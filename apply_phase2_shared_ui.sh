#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail

ROOT="$HOME/school-management-app"
FILE="composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/CommonDashboardComponents.kt"
STAMP="$(date +%Y%m%d-%H%M%S)"

cd "$ROOT" || { echo "ERROR: Project folder not found: $ROOT"; exit 1; }
if [[ ! -f "$FILE" || ! -d .git ]]; then
  echo "ERROR: Run this from the school-management-app repository."
  exit 1
fi

echo "Checking repository state..."
git fetch origin
BRANCH="$(git branch --show-current)"
if [[ "$BRANCH" != "main" ]]; then
  echo "ERROR: Current branch is '$BRANCH', not 'main'. Switch to main before running."
  exit 1
fi
if ! git merge-base --is-ancestor origin/main HEAD; then
  echo "ERROR: Local main is behind or diverged from origin/main. Sync first; no files changed."
  exit 1
fi
if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "ERROR: Tracked or staged changes already exist. Commit or safely back them up first."
  exit 1
fi

BACKUP="${FILE}.before-shared-ui-${STAMP}.bak"
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
"""    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.inputBg,
        border = BorderStroke(1.2.dp, colors.inputBorder),
        modifier = Modifier.fillMaxWidth()
    ) {""",
"""    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.inputBg,
        border = BorderStroke(1.2.dp, colors.inputBorder),
        shadowElevation = if (colors.isDark) 0.dp else 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {"""
    ),
    (
"""    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.inputBg,
        border = BorderStroke(1.dp, colors.inputBorder),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {""",
"""    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.inputBg,
        border = BorderStroke(1.dp, colors.inputBorder),
        shadowElevation = if (colors.isDark) 0.dp else 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {"""
    ),
    (
"""    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = colors.bgCard,
        border = BorderStroke(1.dp, colors.borderCard),
        modifier = modifier
    ) {""",
"""    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = colors.bgCard,
        border = BorderStroke(1.dp, colors.borderCard),
        shadowElevation = if (colors.isDark) 0.dp else 2.dp,
        modifier = modifier
    ) {"""
    ),
    (
"""    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.bgCard,
        border = BorderStroke(1.dp, colors.borderCard),
        modifier = modifier
    ) {""",
"""    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.bgCard,
        border = BorderStroke(1.dp, colors.borderCard),
        shadowElevation = if (colors.isDark) 0.dp else 2.dp,
        modifier = modifier
    ) {"""
    ),
    (
"""    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.bgCard,
        border = BorderStroke(1.dp, colors.borderCard),
        modifier = modifier
    ) {""",
"""    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.bgCard,
        border = BorderStroke(1.dp, colors.borderCard),
        shadowElevation = if (colors.isDark) 0.dp else 2.dp,
        modifier = modifier
    ) {"""
    ),
    (
"""    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = colors.bgCard,
        border = BorderStroke(1.dp, colors.borderCard),
        modifier = modifier
    ) {""",
"""    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = colors.bgCard,
        border = BorderStroke(1.dp, colors.borderCard),
        shadowElevation = if (colors.isDark) 0.dp else 2.dp,
        modifier = modifier
    ) {"""
    ),
]

for old, new in replacements:
    count = text.count(old)
    if count != 1:
        raise SystemExit(
            f"PATCH ABORTED: expected exactly one matching block, found {count}. "
            "No file was written. Share this output so the patch can be adjusted."
        )

for old, new in replacements:
    text = text.replace(old, new, 1)

if text == original:
    raise SystemExit("PATCH ABORTED: no changes produced.")

path.write_text(text, encoding="utf-8")
print(f"Updated shared UI components in {path}")
PY

echo
echo "Checking patch formatting..."
if ! git diff --check; then
  cp -p "$BACKUP" "$FILE"
  echo "ERROR: Diff check failed; original file restored from backup."
  exit 1
fi

echo
echo "Reviewing exact changes..."
git diff -- "$FILE"

echo
read -r -p "If the diff looks correct, type PUSH to commit and trigger the APK build: " CONFIRM
if [[ "$CONFIRM" != "PUSH" ]]; then
  cp -p "$BACKUP" "$FILE"
  echo "Cancelled. Original file restored. Backup preserved at $BACKUP"
  exit 0
fi

git add -- "$FILE"
git commit -m "Polish shared cards and input controls"
git push origin HEAD:main

echo
echo "SUCCESS: Shared UI polish pushed to GitHub main."
echo "Check the Android APK workflow at https://github.com/tapasm569/schoolapp/actions"
echo "Backup preserved: $BACKUP"
