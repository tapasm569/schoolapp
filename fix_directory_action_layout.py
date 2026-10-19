from pathlib import Path
from datetime import datetime
import shutil

files = {
    Path("composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/StudentScreens.kt"):
        ("student.phone", "student", "student"),

    Path("composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/ExpenseAndStaffScreens.kt"):
        ("staff.phone", "staff", "staff"),
}

for path, (phone_marker, entity, kind) in files.items():
    source = path.read_text()

    start = source.find("                            Row(\n", source.find(
        "// Action Bar: Call + WhatsApp + Edit Profile + Remove"
    )) if kind == "student" else source.find(
        "                            Row(\n",
        source.find('messageText = "Hello ${staff.name},"')
    )

    if kind == "staff":
        # Find the action row immediately preceding ContactQuickActions.
        contact = source.find("ContactQuickActions(", source.find(
            'messageText = "Hello ${staff.name},"'
        ))
        start = source.rfind("                            Row(\n", 0, contact)

    if start < 0:
        raise SystemExit(f"STOP: Could not locate {kind} action row in {path}")

    end_marker = (
        "                                    }\n"
        "                                }\n"
    )
    # Find the action-row end by locating the end of its Profile/Remove row.
    profile = source.find('Text("✏️ Profile"', start)
    if profile < 0:
        raise SystemExit(f"STOP: Profile button not found in {path}")

    # Walk backwards to the inner Row and forward through its closing braces.
    inner_row = source.rfind("                                        Row(", start, profile)
    if inner_row < 0:
        inner_row = source.rfind("                                Row(", start, profile)
    if inner_row < 0:
        raise SystemExit(f"STOP: Profile/Remove row not found in {path}")

    # Match braces from the outer action Row, ignoring braces inside strings/comments
    # is unsafe for arbitrary Kotlin, so use known action-row boundary instead.
    # Anchor at the closing sequence immediately before the card content ends.
    after_profile = source.find("Text(\"Remove\"", profile)
    if after_profile < 0:
        raise SystemExit(f"STOP: Remove button not found in {path}")

    # The exact action block is replaced using stable source anchors.
    contact_start = source.rfind("                                    ContactQuickActions(", start, profile)
    if contact_start < 0:
        contact_start = source.find("                                ContactQuickActions(", start)

    if contact_start < 0:
        raise SystemExit(f"STOP: ContactQuickActions not found in {path}")

    # Locate the outer Row's closing point immediately before the card's content
    # closes. The next known line is the card's closing Column/Box structure.
    row_end = source.find(
        "\n                                }\n                            }\n",
        after_profile
    )
    if row_end < 0:
        raise SystemExit(f"STOP: Could not locate action-row end in {path}")

    row_end += len("\n                                }")

    old_block = source[start:row_end]

    if "ContactQuickActions(" not in old_block or 'Text("Remove"' not in old_block:
        raise SystemExit(f"STOP: Unexpected action block in {path}; no files changed")

    # Build the responsive two-row layout while preserving existing click actions.
    if kind == "student":
        contact_call = '''ContactQuickActions(
                                    phone = student.phone,
                                    whatsapp = student.whatsapp.ifBlank { student.phone },
                                    messageText = "Hello Parents of ${student.name} (Class ${student.gradeClass}, Roll #${student.rollNo}),"
                                )'''
        profile_click = "selectedProfileStudent = student"
        delete_call = "scope.launch { database.studentDao().deleteStudent(student) }"
    else:
        contact_call = '''ContactQuickActions(
                                    phone = staff.phone,
                                    whatsapp = staff.whatsapp.ifBlank { staff.phone },
                                    messageText = "Hello ${staff.name},"
                                )'''
        profile_click = "selectedTeacherForEdit = staff"
        delete_call = "scope.launch { database.staffDao().deleteStaff(staff) }"

    new_block = f'''Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {{
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {{
                                    {contact_call}
                                }}

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {{
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = colors.brandPrimary.copy(alpha = 0.12f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clickable {{ {profile_click} }}
                                    ) {{
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {{
                                            Text(
                                                "✏️ Profile",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.brandPrimary
                                            )
                                        }}
                                    }}

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = colors.error.copy(alpha = 0.14f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clickable {{
                                                {delete_call}
                                            }}
                                    ) {{
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {{
                                            Text(
                                                "Remove",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.error
                                            )
                                        }}
                                    }}
                                }}
                            }}'''

    # Validate both files before writing either one.
    files[path] = (phone_marker, entity, kind, old_block, new_block)

# Back up all files before applying changes.
for path, info in files.items():
    backup = path.with_name(
        path.name + ".before_action_layout_" +
        datetime.now().strftime("%Y%m%d_%H%M%S")
    )
    shutil.copy2(path, backup)
    path.write_text(path.read_text().replace(info[3], info[4], 1))
    print(f"PATCHED: {path}")
    print(f"BACKUP:  {backup}")

print("Done. Only the two directory action layouts were changed.")
