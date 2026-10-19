# SchoolApp Termux Fix Kit

## What this does
`termux_apply_fixes.py` makes conservative edits to:
- remove global FCM topic subscriptions from app startup;
- remove FCM token and push payload logging;
- stop Room from silently using destructive migration fallback;
- align the Gradle wrapper to Gradle 8.9 and make CI use `./gradlew`;
- save backups under `.termux-patch-backups/`.

**Important:** removing global topic subscriptions can stop announcements that currently depend on those topics. Before production, implement school-scoped subscriptions after school selection/login and update the server sender accordingly.

The patch intentionally does not change credentials/login, cloud IDs, schema, date defaults, or Firebase security rules. Those require careful, project-specific changes. Removing destructive fallback also means an unprovided future migration can fail rather than erase local data; create proper Room migrations before changing the database schema.

## Termux steps
From your cloned repository:

```sh
pkg update
pkg install python git openjdk-17
cd ~/schoolapp-main   # change this to your actual repo folder

python termux_apply_fixes.py --dry-run
python termux_apply_fixes.py --apply

python termux_preflight.py
chmod +x gradlew
./gradlew :composeApp:assembleDebug --stacktrace

git diff --check
git status
git diff
```

If the build succeeds and you have reviewed the changes:

```sh
git add .github/workflows/build-android.yml \
  composeApp/src/androidMain/kotlin/com/school/manage/MainActivity.kt \
  composeApp/src/androidMain/kotlin/com/school/manage/SchoolFirebaseMessagingService.kt \
  gradle/wrapper/gradle-wrapper.properties \
  termux_apply_fixes.py termux_preflight.py TERMUX_FIX_KIT_README.md
git commit -m "Fix FCM logging, migration safety and Gradle consistency"
git push
```

If the build fails, do not push blindly. Copy the first meaningful `FAILURE` / `e:` error and review it.

## Rollback
The script saves copies of edited files in `.termux-patch-backups/<timestamp>/`. Restore those files manually if needed, or use Git:

```sh
git diff
git restore path/to/file
```

Only use `git restore` for files whose local edits you are willing to discard.

## Remaining high-priority work
The preflight script flags hard-coded date defaults and numeric local IDs used as Firestore document IDs. Do not replace these automatically: cloud ID changes need a migration/merge plan to prevent overwriting or duplicating existing school records. Authentication/password storage was explicitly left unchanged.
