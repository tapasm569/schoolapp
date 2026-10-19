from pathlib import Path

files = [
    Path("composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/StudentScreens.kt"),
    Path("composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/ExpenseAndStaffScreens.kt"),
]

for path in files:
    text = path.read_text()
    old = 'messageText = "Hello Parents of ${student.name} (Class ${student.gradeClass}, Roll #${student.rollNo}),"\n                                        ,'
    new = '''messageText = "Hello Parents of ${student.name} (Class ${student.gradeClass}, Roll #${student.rollNo}),",
                                        profileAction = { selectedProfileStudent = student },
                                        removeAction = { scope.launch { FirestoreSyncService(database).deleteStudentRecord(schoolCode, student) } }
                                    )'''

    if path.name == "StudentScreens.kt":
        if old not in text:
            print(f"ERROR: Expected broken Student call not found in {path}")
            continue
        text = text.replace(old, new, 1)
    else:
        old_staff = 'messageText = "Hello ${staff.name},"\n                                ,'
        new_staff = '''messageText = "Hello ${staff.name},",
                                    profileAction = { selectedTeacherForEdit = staff },
                                    removeAction = { scope.launch { FirestoreSyncService(database).deleteStaffRecord(schoolCode, staff) } }
                                )'''
        if old_staff not in text:
            print(f"ERROR: Expected broken Teacher call not found in {path}")
            continue
        text = text.replace(old_staff, new_staff, 1)

    path.write_text(text)
    print(f"Repaired: {path}")
