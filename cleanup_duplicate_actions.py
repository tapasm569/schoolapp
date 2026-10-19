from pathlib import Path

repairs = {
    Path("composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/StudentScreens.kt"): '''        profileAction = { selectedProfileStudent = student },
        removeAction = { scope.launch { FirestoreSyncService(database).deleteStudentRecord(schoolCode, student) } }
    )
''',
    Path("composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/ExpenseAndStaffScreens.kt"): '''        profileAction = { selectedTeacherForEdit = staff },
        removeAction = { scope.launch { FirestoreSyncService(database).deleteStaffRecord(schoolCode, staff) } }
    )
''',
}

for path, duplicate in repairs.items():
    text = path.read_text()
    count = text.count(duplicate)

    if count != 1:
        print(f"ERROR: Expected exactly one duplicate in {path}; found {count}. No changes made to this file.")
        continue

    path.write_text(text.replace(duplicate, "", 1))
    print(f"Removed duplicate callback block: {path}")
