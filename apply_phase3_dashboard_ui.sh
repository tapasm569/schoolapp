#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail

ROOT="$HOME/school-management-app"
FILE="composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/DashboardScreen.kt"
STAMP="$(date +%Y%m%d-%H%M%S)"

cd "$ROOT" || { echo "ERROR: Project folder not found: $ROOT"; exit 1; }
if [[ ! -f "$FILE" || ! -d .git ]]; then
  echo "ERROR: This does not look like the school-management-app repository."
  exit 1
fi

echo "Checking repository state..."
git fetch origin
if [[ "$(git branch --show-current)" != "main" ]]; then
  echo "ERROR: Please switch to main before running. No files changed."
  exit 1
fi
if ! git merge-base --is-ancestor origin/main HEAD; then
  echo "ERROR: Local main is behind/diverged from origin/main. Sync first; no files changed."
  exit 1
fi
if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "ERROR: Tracked/staged changes already exist. No files changed."
  exit 1
fi

BACKUP="${FILE}.before-dashboard-ui-${STAMP}.bak"
cp -p "$FILE" "$BACKUP"
echo "Backup created: $BACKUP"

export UI_FILE="$FILE"
python - <<'PY'
import os
from pathlib import Path

path = Path(os.environ["UI_FILE"])
text = path.read_text(encoding="utf-8")
original = text

replacements = [
    (
        '.padding(horizontal = 14.dp),\n            verticalArrangement = Arrangement.spacedBy(14.dp)',
        '.padding(horizontal = 16.dp),\n            contentPadding = PaddingValues(bottom = 20.dp),\n            verticalArrangement = Arrangement.spacedBy(16.dp)'
    ),
    (
        'shape = RoundedCornerShape(18.dp),\n                    color = colors.bgCard,\n                    border = BorderStroke(1.dp, colors.borderCard),\n                    modifier = Modifier.fillMaxWidth()',
        'shape = RoundedCornerShape(20.dp),\n                    color = colors.bgCard,\n                    border = BorderStroke(1.dp, colors.borderCard),\n                    shadowElevation = if (colors.isDark) 0.dp else 2.dp,\n                    modifier = Modifier.fillMaxWidth()'
    ),
]

# Validate all expected blocks before writing anything.
expected_counts = [1, 2]
for (old, new), expected in zip(replacements, expected_counts):
    count = text.count(old)
    if count != expected:
        raise SystemExit(
            f"PATCH ABORTED: expected {expected} occurrence(s), found {count}. "
            "No file was written; send this output for adjustment."
        )

for (old, new), expected in zip(replacements, expected_counts):
    text = text.replace(old, new)

if text == original:
    raise SystemExit("PATCH ABORTED: no changes produced.")

path.write_text(text, encoding="utf-8")
print(f"Updated dashboard layout and card styling in {path}")
PY

echo
echo "Checking patch formatting..."
if ! git diff --check; then
  cp -p "$BACKUP" "$FILE"
  echo "ERROR: Diff check failed; original file restored. Backup preserved: $BACKUP"
  exit 1
fi

echo
echo "Reviewing exact changes..."
git diff -- "$FILE"

echo
read -r -p "If the diff looks correct, type PUSH to commit and trigger the APK build: " CONFIRM
if [[ "$CONFIRM" != "PUSH" ]]; then
  cp -p "$BACKUP" "$FILE"
  echo "Cancelled. Original file restored. Backup preserved: $BACKUP"
  exit 0
fi

git add -- "$FILE"
git commit -m "Polish dashboard layout and summary cards"
git push origin HEAD:main

echo
echo "SUCCESS: Dashboard UI polish pushed to GitHub main."
echo "Check the Android build at https://github.com/tapasm569/schoolapp/actions"
echo "Backup preserved: $BACKUP"
