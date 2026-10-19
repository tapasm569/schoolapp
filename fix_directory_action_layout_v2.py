from pathlib import Path
from datetime import datetime
import shutil

configs = [
    {
        "path": Path("composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/StudentScreens.kt"),
        "marker": "// Action Bar: Call + WhatsApp + Edit Profile + Remove",
        "entity": "student",
        "contact": '''ContactQuickActions(
                                            phone = student.phone,
                                            whatsapp = student.whatsapp.ifBlank { student.phone },
                                            messageText = "Hello Parents of ${student.name} (Class ${student.gradeClass}, Roll #${student.rollNo}),"
                                        )''',
        "profile": "selectedProfileStudent = student",
        "delete": "scope.launch { database.studentDao().deleteStudent(student) }",
    },
    {
        "path": Path("composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/ExpenseAndStaffScreens.kt"),
        "marker": 'messageText = "Hello ${staff.name},"',
        "entity": "staff",
        "contact": '''ContactQuickActions(
                                    phone = staff.phone,
                                    whatsapp = staff.whatsapp.ifBlank { staff.phone },
                                    messageText = "Hello ${staff.name},"
                                )''',
        "profile": "selectedTeacherForEdit = staff",
        "delete": "scope.launch { database.staffDao().deleteStaff(staff) }",
    },
]

replacements = []

for c in configs:
    p = c["path"]
    s = p.read_text()
    entity = c["entity"]

    if entity == "student":
        contact_pos = s.find(c["marker"])
        if contact_pos < 0:
            raise SystemExit(f"STOP: Student action marker not found: {p}")
        start = s.rfind("                                    Row(", 0, contact_pos)
    else:
        contact_pos = s.find(c["marker"])
        if contact_pos < 0:
            raise SystemExit(f"STOP: Staff action marker not found: {p}")
        contact_pos = s.rfind("                                ContactQuickActions(", 0, contact_pos)
        if contact_pos < 0:
            raise SystemExit(f"STOP: Staff ContactQuickActions not found: {p}")
        start = s.rfind("                            Row(", 0, contact_pos)

    if start < 0:
        raise SystemExit(f"STOP: Outer action row not found: {p}")

    profile_pos = s.find("Profile", contact_pos)
    remove_pos = s.find('"Remove"', contact_pos)
    if profile_pos < 0 or remove_pos < 0:
        raise SystemExit(f"STOP: Profile/Remove controls not found: {p}")

    # Find the end of the outer action Row by tracking Kotlin braces/parens
    # while ignoring braces inside quoted strings and comments.
    i = start
    depth = 0
    paren_depth = 0
    in_string = False
    escaped = False
    line_comment = False
    block_comment = False
    started = False
    end = None

    while i < len(s):
        ch = s[i]
        nxt = s[i + 1] if i + 1 < len(s) else ""

        if line_comment:
            if ch == "\n":
                line_comment = False
            i += 1
            continue
        if block_comment:
            if ch == "*" and nxt == "/":
                block_comment = False
                i += 2
            else:
                i += 1
            continue
        if in_string:
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == '"':
                in_string = False
            i += 1
            continue
        if ch == "/" and nxt == "/":
            line_comment = True
            i += 2
            continue
        if ch == "/" and nxt == "*":
            block_comment = True
            i += 2
            continue
        if ch == '"':
            in_string = True
            i += 1
            continue

        if ch == "(":
            paren_depth += 1
            started = True
        elif ch == ")":
            paren_depth -= 1
            if started and paren_depth == 0:
                end = i + 1
                break
        i += 1

    if end is None or not (start < contact_pos < profile_pos < remove_pos < end):
        raise SystemExit(f"STOP: Could not validate full action row: {p}")

    block = s[start:end]
    if "ContactQuickActions(" not in block or "Remove" not in block:
        raise SystemExit(f"STOP: Action row validation failed: {p}")

    if entity == "student":
        contact = '''ContactQuickActions(
                                        phone = student.phone,
                                        whatsapp = student.whatsapp.ifBlank { student.phone },
                                        messageText = "Hello Parents of ${student.name} (Class ${student.gradeClass}, Roll #${student.rollNo}),"
                                    )'''
    else:
        contact = '''ContactQuickActions(
                                    phone = staff.phone,
                                    whatsapp = staff.whatsapp.ifBlank { staff.phone },
                                    messageText = "Hello ${staff.name},"
                                )'''

    new = f'''Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {{
                                {contact}

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
                                            .clickable {{ {c["profile"]} }}
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
                                                {c["delete"]}
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

    replacements.append((p, s, start, end, new))

# No file is changed until both source sections pass validation.
for p, s, start, end, new in replacements:
    backup = p.with_name(
        p.name + ".before_action_layout_" +
        datetime.now().strftime("%Y%m%d_%H%M%S")
    )
    shutil.copy2(p, backup)
    p.write_text(s[:start] + new + s[end:])
    print("PATCHED:", p)
    print("BACKUP:", backup)

print("PATCH APPLIED: both directory action layouts")
