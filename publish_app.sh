#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

cd "$HOME/school-management-app"

if [ "$(git branch --show-current)" != "main" ]; then
    echo "ERROR: You must be on main."
    exit 1
fi

if ! git diff --cached --quiet; then
    echo "ERROR: Staged changes already exist."
    echo "Review them before publishing:"
    git diff --cached --stat
    exit 1
fi

echo "Checking tracked changes..."
git diff --check

if git diff --quiet; then
    echo "No tracked changes to publish."
    echo "Backups and untracked files have been left untouched."
    exit 0
fi

echo
echo "Modified tracked files:"
git diff --name-only

echo
echo "Change summary:"
git diff --stat

echo
read -r -p "Stage these tracked changes? (y/n): " answer
if [[ "$answer" != "y" && "$answer" != "Y" ]]; then
    echo "Cancelled."
    exit 0
fi

git add -u
git diff --cached --check

echo
echo "Review staged files carefully:"
git diff --cached --name-only
git diff --cached --stat

echo
read -r -p "Commit and push to main? (y/n): " answer
if [[ "$answer" != "y" && "$answer" != "Y" ]]; then
    echo "Cancelled. Changes remain staged."
    exit 0
fi

git commit -m "Update School Management app"
git push origin main

echo
echo "SUCCESS: Changes pushed."
echo "GitHub Actions will build the APK."O

