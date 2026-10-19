#!/usr/bin/env python3
"""Lightweight source preflight for SchoolApp before pushing from Termux.
Run from repository root: python termux_preflight.py
It does not replace a successful Gradle build or runtime tests.
"""
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path.cwd().resolve()
if not (ROOT / "gradlew").exists() or not (ROOT / "composeApp").is_dir():
    sys.exit("ERROR: Run this script from the SchoolApp repository root.")

checks = [
    ("global FCM topic subscriptions", re.compile(r'subscribeToTopic\("(?:all_schools|school_announcements)"\)'), ["*.kt"]),
    ("FCM token logging", re.compile(r'Log\.[divew]\([^\n]*(?:FCM Token|NEW TOKEN RECEIVED|token: \\$token)'), ["*.kt"]),
    ("FCM payload logging", re.compile(r'Log\.[divew]\([^\n]*remoteMessage\.data'), ["*.kt"]),
    ("destructive Room migration", re.compile(r'fallbackToDestructiveMigration\s*\('), ["*.kt"]),
    ("hard-coded date default", re.compile(r'"02/10/2026"'), ["*.kt"]),
    ("numeric Room IDs used as cloud doc IDs", re.compile(r'document\(item\.id\.toString\(\)\)'), ["*.kt"]),
]
files = list((ROOT / "composeApp/src").rglob("*.kt"))
failed = False
print(f"Scanning {len(files)} Kotlin files under {ROOT / 'composeApp/src'}\n")
for label, pattern, _ in checks:
    hits = []
    for path in files:
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        for line_no, line in enumerate(text.splitlines(), 1):
            if pattern.search(line):
                hits.append(f"  {path.relative_to(ROOT)}:{line_no}: {line.strip()[:180]}")
    if hits:
        failed = True
        print(f"[REVIEW] {label}: {len(hits)} occurrence(s)")
        print("\n".join(hits[:12]))
        if len(hits) > 12:
            print(f"  ... plus {len(hits)-12} more")
    else:
        print(f"[OK] {label}: no match found")

print("\nGit whitespace check:")
try:
    result = subprocess.run(["git", "diff", "--check"], cwd=ROOT, text=True, capture_output=True)
    if result.returncode == 0:
        print("[OK] git diff --check")
    else:
        failed = True
        print(result.stdout + result.stderr)
except FileNotFoundError:
    print("[SKIP] git is not installed")

print("\nBuild check (run separately; needs Android SDK and network/cache):")
print("  ./gradlew :composeApp:assembleDebug --stacktrace")
print("\nNote: findings are heuristic. A clean scan does not prove the app is bug-free.")
sys.exit(1 if failed else 0)
