#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

REPO="tapasm569/schoolapp"
WORKFLOW="build-android.yml"
DOWNLOAD_DIR="$HOME/school-management-app/apk-downloads"

mkdir -p "$DOWNLOAD_DIR"

echo "Searching for the latest successful Android build..."

RUN_ID=$(gh run list \
    --repo "$REPO" \
    --workflow "$WORKFLOW" \
    --branch main \
    --status success \
    --limit 1 \
    --json databaseId \
    --jq '.[0].databaseId // empty')

if [ -z "$RUN_ID" ]; then
    echo "ERROR: No successful Android build was found."
    exit 1
fi

echo "Latest successful build ID: $RUN_ID"
echo "Downloading APK artifact..."

TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

gh run download "$RUN_ID" \
    --repo "$REPO" \
    --name school-management-debug-apk \
    --dir "$TMP_DIR"

APK=$(find "$TMP_DIR" -type f -name '*.apk' -print -quit)

if [ -z "$APK" ]; then
    echo "ERROR: No APK was found in the downloaded artifact."
    exit 1
fi

DEST="$DOWNLOAD_DIR/school-management-latest.apk"
cp "$APK" "$DEST"

echo
echo "SUCCESS: APK downloaded."
echo "Location: $DEST"
echo
ls -lh "$DEST"

echo
echo "Open the APK with:"
echo "termux-open '$DEST'"
