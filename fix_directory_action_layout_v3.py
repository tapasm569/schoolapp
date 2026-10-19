from pathlib import Path
from datetime import datetime
import shutil

FILES = {
    "student": Path(
        "composeApp/src/commonMain/kotlin/com/school/manage/"
        "presentation/screens/StudentScreens.kt"
    ),
    "staff": Path(
        "composeApp/src/commonMain/kotlin/com/school/manage/"
        "presentation/screens/ExpenseAndStaffScreens.kt"
    ),
}

def find_block(lines, start):
    """Find a Kotlin brace-balanced block, ignoring strings and comments."""
    depth = 0
    in_block_comment = False
    started = False

    for i in range(start, len(lines)):
        line = lines[i]
        j = 0
        in_string = False
        escaped = False

        while j < len(line):
            c = line[j]
            nxt = line[j + 1] if j + 1 < len(line) else ""

            if in_block_comment:
                if c == "*" and nxt == "/":
                    in_block_comment = False
                    j += 2
                    continue
                j += 1
                continue

            if in_string:
                if escaped:
                    escaped = False
                elif c == "\\":
                    escaped = True
                elif c == '"':
                    in_string = False
                j += 1
                continue

            if c == "/" and nxt == "/":
                break

            if c == "/" and nxt == "*":
                in_block_comment = True
                j += 2
                continue

            if c == '"':
                in_string = True
                j += 1
                continue

            if c == "{":
                depth += 1
                started = True
            elif c == "}":
                depth -= 1
                if started and depth == 0:
                    return i + 1

            j += 1

    raise ValueError("Could not find the complete action-row block")

def replacement(indent, person):
    p = " " * indent
    q = " " * (indent + 4)
    r = " " * (indent + 8)
    t = " " * (indent + 12)

    if person == "student":
        contact = '''ContactQuickActions(
PHONE    phone = student.phone,
PHONE    whatsapp = student.whatsapp.ifBlank { student.phone },
PHONE    messageText = "Hello Parents of ${student.name} (Class ${student.gradeClass}, Roll #${student.rollNo}),"
PHONE)'''.replace("PHONE", q)
        profile_click = "selectedProfileStudent = student"
        remove_click = "scope.launch { database.studentDao().deleteStudent(student) }"
    else:
        contact = '''ContactQuickActions(
PHONE    phone = staff.phone,
PHONE    whatsapp = staff.whatsapp.ifBlank { staff.phone },
PHONE    messageText = "Hello ${staff.name},"
PHONE)'''.replace("PHONE", q)
        profile_click = "selectedTeacherForEdit = staff"
        remove_click = "scope.launch { database.staffDao().deleteStaff(staff) }"

    return [
        p + "Column(",
        q + "modifier = Modifier.fillMaxWidth(),",
        q + "verticalArrangement = Arrangement.spacedBy(8.dp)",
        p + ") {",
        q + contact.splitlines()[0],
        *contact.splitlines()[1:],
        "",
        q + "Row(",
        r + "modifier = Modifier.fillMaxWidth(),",
        r + "horizontalArrangement = Arrangement.spacedBy(8.dp),",
        r + "verticalAlignment = Alignment.CenterVertically",
        q + ") {",
        r + "Surface(",
        t + "shape = RoundedCornerShape(8.dp),",
        t + "color = colors.brandPrimary.copy(alpha = 0.12f),",
        t + "modifier = Modifier.weight(1f).height(36.dp).clickable { " + profile_click + " }",
        r + ") {",
        t + "Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {",
        " " * (indent + 16) + 'Text("✏️ Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.brandPrimary)',
        t + "}",
        r + "}",
        "",
        r + "Surface(",
        t + "shape = RoundedCornerShape(8.dp),",
        t + "color = colors.error.copy(alpha = 0.14f),",
        t + "modifier = Modifier.weight(1f).height(36.dp).clickable {",
        " " * (indent + 16) + remove_click,
        t + "}",
        r + ") {",
        t + "Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {",
        " " * (indent + 16) + 'Text("Remove", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.error)',
        t + "}",
        r + "}",
        q + "}",
        p + "}",
    ]

# Validate both targets completely before modifying either file.
prepared = {}

for person, path in FILES.items():
    if not path.exists():
        raise SystemExit(f"STOP: file not found: {path}")

    original = path.read_text()
    lines = original.splitlines()
    
    if person == "student":
        marker = next(
            (i for i, line in enumerate(lines)
             if "// Action Bar: Call + WhatsApp + Edit Profile + Remove" in line),
            None
        )
        if marker is None:
            raise SystemExit("STOP: Student action-row marker not found")
        start = next(
            (i for i in range(marker + 1, min(marker + 5, len(lines)))
             if lines[i].strip() == "Row("),
            None
        )
    else:
        marker = next(
            (i for i, line in enumerate(lines)
             if 'messageText = "Hello ${staff.name},"' in line),
            None
        )
        if marker is None:
            raise SystemExit("STOP: Staff contact action not found")
        start = next(
            (i for i in range(marker - 1, max(-1, marker - 12), -1)
             if lines[i].strip() == "Row("),
            None
        )

    if start is None:
        raise SystemExit(f"STOP: action-row start not found: {path}")

    # Preserve the indentation level of the original outer Row.
    indent = len(lines[start]) - len(lines[start].lstrip())
    end = find_block(lines, start)

    block_text = "\n".join(lines[start:end])
    if "ContactQuickActions(" not in block_text:
        raise SystemExit(f"STOP: unexpected block in {path}")
    if person == "student" and "deleteStudent(student)" not in block_text:
        raise SystemExit("STOP: student Remove action did not validate")
    if person == "staff" and "deleteStaff(staff)" not in block_text:
        raise SystemExit("STOP: staff Remove action did not validate")

    new_lines = lines[:start] + replacement(indent, person) + lines[end:]
    updated = "\n".join(new_lines) + ("\n" if original.endswith("\n") else "")

    if updated == original:
        raise SystemExit(f"STOP: no change produced for {path}")

    prepared[path] = (original, updated)

stamp = datetime.now().strftime("%Y%m%d_%H%M%S")

for path, (original, updated) in prepared.items():
    backup = path.with_name(path.name + f".before_layout_{stamp}")
    shutil.copy2(path, backup)

for path, (original, updated) in prepared.items():
    path.write_text(updated)

print("PATCH APPLIED")
for path in prepared:
    print(f"Updated: {path}")
    print(f"Backup:  {path.name}.before_layout_{stamp}")
print("Both existing Profile and Remove actions were preserved.")
