#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail

REPO="tapasm569/schoolapp"
WORKFLOW="build-android.yml"
BRANCH="main"
DOWNLOAD_DIR="$HOME/school-management-app/apk-downloads"
ARTIFACT="school-management-debug-apk"

cd "$HOME/school-management-app"

echo "======================================"
echo " School Management App Updater"
echo "======================================"

# Check branch
if [ "$(git branch --show-current)" != "$BRANCH" ]; then
    echo "ERROR: Switch to the main branch first."
    exit 1
fi

# Do not disturb existing staged changes
if ! git diff --cached --quiet; then
    echo "ERROR: Staged changes already exist."
    echo "Review them before continuing."
    git diff --cached --stat
    exit 1
fi

# Check tracked source changes
git diff --check

if git diff --quiet; then
    echo "No tracked code changes found."
    echo "Nothing will be committed or pushed."
    echo
    echo "Existing APK, backups, and helper files are untouched."
    exit 0
fi

echo
echo "Modified tracked files:"
git diff --name-only

echo
echo "Change summary:"
git diff --stat

echo
read -r -p "Continue and prepare these changes? (y/n): " answer
if [[ "$answer" != "y" && "$answer" != "Y" ]]; then
    echo "Cancelled. Your files were not changed by this script."
    exit 0
fi

# Stage tracked modifications only; never add untracked backups
git add -u
git diff --cached --check

echo
echo "Files prepared for commit:"
git diff --cached --name-only

echo
echo "Staged change summary:"
git diff --cached --stat

echo
read -r -p "Commit and push these changes to GitHub? (y/n): " answer
if [[ "$answer" != "y" && "$answer" != "Y" ]]; then
    echo "Cancelled. Changes remain staged."
    exit 0
fi

# Commit and push
git commit -m "Update School Management app"
git push origin "$BRANCH"

HEAD_SHA=$(git rev-parse HEAD)

echo
echo "SUCCESS: Changes pushed."
echo "Commit: $HEAD_SHA"
echo
echo "Waiting for the Android build for this commit..."

# Find the workflow run belonging to this exact commit
RUN_ID=""
for attempt in $(seq 1 60); do
    RUN_ID=$(gh run list \
        --repo "$REPO" \
        --workflow "$WORKFLOW" \
        --branch "$BRANCH" \
        --limit 20 \
        --json databaseId,headSha \
        --jq "[.[] | select(.headSha == \"$HEAD_SHA\")] | first | .databaseId // empty" \
        2>/dev/null || true)

    if [ -n "$RUN_ID" ]; then
        break
    fi

    echo "Build has not appeared yet ($attempt/60)..."
    sleep 10
done

if [ -z "$RUN_ID" ]; then
    echo "ERROR: Could not find the build for this commit."
    echo "Check the Actions page on GitHub."
    exit 1
fi

echo
echo "Build run ID: $RUN_ID"
echo "Waiting for GitHub Actions to finish..."

if ! gh run watch "$RUN_ID" --repo "$REPO" --exit-status; then
    echo
    echo "ERROR: The Android build failed."
    echo "No APK was downloaded from this failed build."
    echo "Check the build logs on GitHub Actions."
    exit 1
fi

CONCLUSION=$(gh run view "$RUN_ID" \
    --repo "$REPO" \
    --json conclusion \
    --jq '.conclusion')

if [ "$CONCLUSION" != "success" ]; then
    echo "ERROR: Build result was $CONCLUSION."
    exit 1
fi

# Download APK from this exact successful run
mkdir -p "$DOWNLOAD_DIR"
TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

echo
echo "Downloading APK from build $RUN_ID..."

gh run download "$RUN_ID" \
    --repo "$REPO" \
    --name "$ARTIFACT" \
    --dir "$TMP_DIR"

APK=$(find "$TMP_DIR" -type f -name '*.apk' -print -quit)

if [ -z "$APK" ]; then
    echo "ERROR: No APK was found in the build artifact."
    exit 1
fi

DEST="$DOWNLOAD_DIR/school-management-latest.apk"
cp "$APK" "$DEST"

echo
echo "======================================"
echo " SUCCESS: APK downloaded!"
echo "======================================"
ls -lh "$DEST"
echo
echo "Opening the Android installer..."

termux-open "$DEST" || {
    echo
    echo "Could not open the installer automatically."
    echo "Open this file manually:"
    echo "$DEST"
}
