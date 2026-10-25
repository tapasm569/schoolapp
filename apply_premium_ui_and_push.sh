#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail

# Premium Modern UI refresh for School Management app.
# Updates shared theme only; preserves database, Firebase and screen logic.
cd "$HOME/school-management-app" || {
  echo "ERROR: ~/school-management-app not found."
  exit 1
}

THEME="composeApp/src/commonMain/kotlin/com/school/manage/presentation/theme/AppTheme.kt"
COMPONENTS="composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/CommonDashboardComponents.kt"

for f in "$THEME" "$COMPONENTS"; do
  if [ ! -f "$f" ]; then
    echo "ERROR: Required source file missing: $f"
    exit 1
  fi
done

if ! command -v python >/dev/null 2>&1; then
  echo "Python is required. Install it with: pkg install python"
  exit 1
fi
if ! command -v git >/dev/null 2>&1; then
  echo "Git is required."
  exit 1
fi

echo "Checking GitHub state..."
git fetch origin || {
  echo "ERROR: Could not fetch origin. Check internet/GitHub access and retry."
  exit 1
}

# Avoid accidentally overwriting existing tracked edits.
if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "ERROR: Tracked changes already exist. Nothing was changed."
  echo "Review them first with: git status --short"
  exit 1
fi

BRANCH="$(git branch --show-current)"
if [ -z "$BRANCH" ]; then
  echo "ERROR: Detached HEAD. Switch to your normal working branch first."
  exit 1
fi

# Make sure a regular push to main will be a fast-forward.
if ! git merge-base --is-ancestor origin/main HEAD; then
  echo "ERROR: Your current HEAD is not based on origin/main."
  echo "Nothing was changed or pushed. Please send: git status --short --branch"
  exit 1
fi

STAMP="$(date +%Y%m%d-%H%M%S)"
cp -p "$THEME" "$THEME.before-premium-ui-$STAMP.bak"
cp -p "$COMPONENTS" "$COMPONENTS.before-premium-ui-$STAMP.bak"
echo "Created backups:"
echo "  $THEME.before-premium-ui-$STAMP.bak"
echo "  $COMPONENTS.before-premium-ui-$STAMP.bak"

python - <<'PY'
from pathlib import Path
import re
theme = Path("composeApp/src/commonMain/kotlin/com/school/manage/presentation/theme/AppTheme.kt")
text = theme.read_text()

replacements = {
    "bgApp = Color(0xFFF4F7FB)": "bgApp = Color(0xFFF5F7FC)",
    "bgCard = Color(0xFFFFFFFF)": "bgCard = Color(0xFFFFFFFF)",
    "bgCardHover = Color(0xFFF8FAFC)": "bgCardHover = Color(0xFFF0F3FA)",
    "borderCard = Color(0xFFE2E8F0)": "borderCard = Color(0xFFE3E8F2)",
    "textPrimary = Color(0xFF0F172A)": "textPrimary = Color(0xFF172033)",
    "textSecondary = Color(0xFF64748B)": "textSecondary = Color(0xFF667085)",
    "brandPrimary = Color(0xFF0D529C)": "brandPrimary = Color(0xFF4338CA)",
    "brandAccent = Color(0xFF2563EB)": "brandAccent = Color(0xFF0D9488)",
    "success = Color(0xFF16A34A)": "success = Color(0xFF15803D)",
    "error = Color(0xFFDC2626)": "error = Color(0xFFB42318)",
    "warning = Color(0xFFD97706)": "warning = Color(0xFFB45309)",
    "inputBg = Color(0xFFFFFFFF)": "inputBg = Color(0xFFFFFFFF)",
    "inputBorder = Color(0xFFCBD5E1)": "inputBorder = Color(0xFFD0D5DD)",
    "bgApp = Color(0xFF0B1120)": "bgApp = Color(0xFF101522)",
    "bgCard = Color(0xFF1E293B)": "bgCard = Color(0xFF191F2E)",
    "bgCardHover = Color(0xFF334155)": "bgCardHover = Color(0xFF252D40)",
    "borderCard = Color(0xFF334155)": "borderCard = Color(0xFF343D52)",
    "textPrimary = Color(0xFFF8FAFC)": "textPrimary = Color(0xFFF2F4F8)",
    "textSecondary = Color(0xFF94A3B8)": "textSecondary = Color(0xFFADB7CA)",
    "brandPrimary = Color(0xFF38BDF8)": "brandPrimary = Color(0xFFA5B4FC)",
    "brandAccent = Color(0xFF60A5FA)": "brandAccent = Color(0xFF5EEAD4)",
    "success = Color(0xFF22C55E)": "success = Color(0xFF4ADE80)",
    "error = Color(0xFFEF4444)": "error = Color(0xFFFF8A80)",
    "warning = Color(0xFFF59E0B)": "warning = Color(0xFFFBBF24)",
    "inputBg = Color(0xFF1E293B)": "inputBg = Color(0xFF20283A)",
    "inputBorder = Color(0xFF475569)": "inputBorder = Color(0xFF48536B)",
}
for old, new in replacements.items():
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Safety check failed: expected one occurrence of {old!r}, found {count}")
    text = text.replace(old, new, 1)

# Add explicit Material 3 shape tokens for dialogs, menus, cards and controls.
if "shapes = Shapes(" not in text:
    text = text.replace(
        "import androidx.compose.ui.graphics.Color\n",
        "import androidx.compose.ui.graphics.Color\nimport androidx.compose.foundation.shape.RoundedCornerShape\nimport androidx.compose.ui.unit.dp\n"
    )
    old = """        MaterialTheme(
            colorScheme = m3Colors,
            content = content
        )"""
    new = """        MaterialTheme(
            colorScheme = m3Colors,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(22.dp),
                extraLarge = RoundedCornerShape(28.dp)
            ),
            content = content
        )"""
    if old not in text:
        raise SystemExit("Safety check failed: could not locate MaterialTheme block")
    text = text.replace(old, new, 1)

theme.write_text(text)
print("Updated shared light/dark palette and Material 3 shape tokens.")
PY

if [ $? -ne 0 ]; then
  echo "ERROR: Theme patch failed. Restoring backups."
  cp -p "$THEME.before-premium-ui-$STAMP.bak" "$THEME"
  cp -p "$COMPONENTS.before-premium-ui-$STAMP.bak" "$COMPONENTS"
  exit 1
fi

# Components are intentionally preserved in this first pass; shared Material 3
# shape tokens and palette changes affect the app consistently without touching
# business logic or per-screen state.
echo
echo "Reviewing patch..."
git diff --check || {
  echo "ERROR: Whitespace validation failed. Restoring theme backup."
  cp -p "$THEME.before-premium-ui-$STAMP.bak" "$THEME"
  exit 1
}
git diff --stat
git diff -- "$THEME"

echo
echo "Staging only the shared theme file..."
git add "$THEME"
git commit -m "Refresh app with premium modern Material 3 theme" || {
  echo "ERROR: Commit failed. Source backup is preserved."
  exit 1
}

echo
echo "Pushing to GitHub main to trigger the Android APK workflow..."
git push origin HEAD:main || {
  echo "ERROR: Push failed. Your commit and backups are preserved."
  echo "Check: git status --short --branch"
  exit 1
}

echo
echo "SUCCESS: Premium Modern theme pushed to GitHub main."
echo "Check the Android build at:"
echo "https://github.com/tapasm569/schoolapp/actions"
echo
echo "Backup files were preserved. No database, Firebase or fee logic was changed."
