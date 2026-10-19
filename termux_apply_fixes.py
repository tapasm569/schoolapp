#!/usr/bin/env python3
"""Conservative, repeatable fixes for SchoolApp. Run from repository root.

Usage:
  python termux_apply_fixes.py --dry-run
  python termux_apply_fixes.py --apply

This intentionally does NOT change authentication/password handling, cloud record IDs,
Room schema/version, date defaults, or Firestore rules; those need project-specific design
and/or deployed Firebase configuration.
"""
from __future__ import annotations
import argparse
import datetime as dt
import shutil
import sys
from pathlib import Path


def find_root(start: Path) -> Path:
    for p in (start, *start.parents):
        if (p / "gradlew").exists() and (p / "composeApp").is_dir():
            return p
    raise SystemExit("ERROR: Could not find project root (gradlew + composeApp). Run this from inside your SchoolApp repo.")


def replace_once(path: Path, old: str, new: str, *, label: str, dry_run: bool, backup_dir: Path, changed: list[str]) -> None:
    if not path.exists():
        raise SystemExit(f"ERROR: expected file not found: {path}")
    data = path.read_text(encoding="utf-8")
    if old not in data:
        if new in data:
            print(f"[already fixed] {label}")
            return
        raise SystemExit(f"ERROR: could not safely locate expected code for: {label}\nFile: {path}\nNo changes applied to this file.")
    if dry_run:
        print(f"[would patch] {label}")
        return
    backup_dir.mkdir(parents=True, exist_ok=True)
    backup = backup_dir / path.relative_to(ROOT)
    backup.parent.mkdir(parents=True, exist_ok=True)
    if not backup.exists():
        shutil.copy2(path, backup)
    path.write_text(data.replace(old, new, 1), encoding="utf-8")
    changed.append(str(path.relative_to(ROOT)))
    print(f"[patched] {label}")


def main() -> int:
    global ROOT
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--dry-run", action="store_true", help="show changes without editing files")
    mode.add_argument("--apply", action="store_true", help="apply fixes and save backups")
    args = parser.parse_args()
    ROOT = find_root(Path.cwd().resolve())
    dry = args.dry_run
    stamp = dt.datetime.now().strftime("%Y%m%d-%H%M%S")
    backup_dir = ROOT / ".termux-patch-backups" / stamp
    changed: list[str] = []

    main_activity = ROOT / "composeApp/src/androidMain/kotlin/com/school/manage/MainActivity.kt"
    old_notifications = '''        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all_schools")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    android.util.Log.d("FCM_TEST", "Successfully subscribed to all_schools")
                } else {
                    android.util.Log.e("FCM_TEST", "Topic subscription failed", task.exception)
                }
            }

        val fcm = com.google.firebase.messaging.FirebaseMessaging.getInstance()
        fcm.token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                android.util.Log.d("FCM_TEST", "FCM Token active: ${task.result}")
            }
        }
        fcm.subscribeToTopic("school_announcements")
'''
    replace_once(main_activity, old_notifications, '''        // Do not subscribe every installation to global topics. School-specific
        // subscriptions must be managed after authenticated school selection.
        // Do not log FCM registration tokens.
''', label="remove global FCM topic subscriptions and token logging", dry_run=dry, backup_dir=backup_dir, changed=changed)

    replace_once(main_activity, '            .fallbackToDestructiveMigration()\n', '', label="stop Room from silently deleting the local database on missing migrations", dry_run=dry, backup_dir=backup_dir, changed=changed)

    messaging = ROOT / "composeApp/src/androidMain/kotlin/com/school/manage/SchoolFirebaseMessagingService.kt"
    replace_once(messaging, '        android.util.Log.d("FCM_TEST", "NEW TOKEN RECEIVED: $token")\n', '        // Token deliberately not logged; it is a sensitive registration identifier.\n', label="remove FCM token logging in messaging service", dry_run=dry, backup_dir=backup_dir, changed=changed)
    replace_once(messaging, '        android.util.Log.d("FCM_TEST", "PUSH RECEIVED! from: ${remoteMessage.from}, data: ${remoteMessage.data}")\n', '        // Do not log message payloads; they may contain private school information.\n', label="remove push payload logging", dry_run=dry, backup_dir=backup_dir, changed=changed)

    wrapper = ROOT / "gradle/wrapper/gradle-wrapper.properties"
    replace_once(wrapper, 'distributionUrl=https\\://services.gradle.org/distributions/gradle-9.8.1-bin.zip', 'distributionUrl=https\\://services.gradle.org/distributions/gradle-8.9-bin.zip', label="align Gradle wrapper with CI/AGP-compatible Gradle 8.9", dry_run=dry, backup_dir=backup_dir, changed=changed)

    workflow = ROOT / ".github/workflows/build-android.yml"
    old_ci = '''      - name: Setup Gradle 8.9
        uses: gradle/actions/setup-gradle@v3
        with:
          gradle-version: '8.9'

      - name: Build Debug APK
        run: gradle :composeApp:assembleDebug --stacktrace
'''
    new_ci = '''      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3

      - name: Ensure Gradle wrapper is executable
        run: chmod +x gradlew

      - name: Build Debug APK
        run: ./gradlew :composeApp:assembleDebug --stacktrace
'''
    replace_once(workflow, old_ci, new_ci, label="make CI use the repository Gradle wrapper consistently", dry_run=dry, backup_dir=backup_dir, changed=changed)

    if dry:
        print("\nDry run only. To apply: python termux_apply_fixes.py --apply")
    else:
        gradlew = ROOT / "gradlew"
        try:
            gradlew.chmod(gradlew.stat().st_mode | 0o111)
            print("[patched] set executable permission on gradlew")
        except OSError as exc:
            print(f"[warning] could not chmod gradlew: {exc}")
        print(f"\nApplied {len(changed)} file edit(s). Backups: {backup_dir.relative_to(ROOT)}")
        print("Review with: git diff --check && git diff")
        print("Then build with: ./gradlew :composeApp:assembleDebug")
    print("\nImportant: this is a conservative patch, not a claim that every bug is fixed.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
