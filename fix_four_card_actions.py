from pathlib import Path
import re
import sys

base = Path("composeApp/src/commonMain/kotlin/com/school/manage")

utils = base / "core/utils/ProfilePhotoUtils.kt"
student_file = base / "presentation/screens/StudentScreens.kt"
teacher_file = base / "presentation/screens/ExpenseAndStaffScreens.kt"

# Replace only the ContactQuickActions function.
text = utils.read_text()
start = text.find("@Composable\nfun ContactQuickActions(")

if start < 0:
    sys.exit("ERROR: ContactQuickActions function not found.")

old_function = text[start:]

new_function = r'''@Composable
fun ContactQuickActions(
    phone: String,
    whatsapp: String = "",
    messageText: String = "",
    profileAction: (() -> Unit)? = null,
    removeAction: (() -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    val cleanPhone = phone.filter { it.isDigit() }
    val cleanWa = whatsapp.ifBlank { phone }.filter { it.isDigit() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Call
        Surface(
            shape = RoundedCornerShape(7.dp),
            color = Color(0xFF2563EB).copy(alpha = 0.12f),
            border = BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.3f)),
            modifier = Modifier.weight(1f).height(32.dp)
                .clickable(enabled = cleanPhone.isNotEmpty()) {
                    try {
                        uriHandler.openUri("tel:$cleanPhone")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "📞 Call",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2563EB),
                    maxLines = 1
                )
            }
        }

        // WhatsApp
        Surface(
            shape = RoundedCornerShape(7.dp),
            color = Color(0xFF16A34A).copy(alpha = 0.14f),
            border = BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.35f)),
            modifier = Modifier.weight(1.35f).height(32.dp)
                .clickable(enabled = cleanWa.isNotEmpty()) {
                    try {
                        val formattedWa =
                            if (cleanWa.length == 10) "91$cleanWa" else cleanWa
                        val url = if (messageText.isNotBlank()) {
                            "https://wa.me/$formattedWa?text=" +
                                java.net.URLEncoder.encode(messageText, "UTF-8")
                        } else {
                            "https://wa.me/$formattedWa"
                        }
                        uriHandler.openUri(url)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "💬 WhatsApp",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF15803D),
                    maxLines = 1
                )
            }
        }

        // Profile
        if (profileAction != null) {
            Surface(
                shape = RoundedCornerShape(7.dp),
                color = Color(0xFF2563EB).copy(alpha = 0.12f),
                modifier = Modifier.weight(1.15f).height(32.dp)
                    .clickable { profileAction() }
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "✏️ Profile",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB),
                        maxLines = 1
                    )
                }
            }
        }

        // Remove
        if (removeAction != null) {
            Surface(
                shape = RoundedCornerShape(7.dp),
                color = Color(0xFFDC2626).copy(alpha = 0.12f),
                modifier = Modifier.weight(1f).height(32.dp)
                    .clickable { removeAction() }
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Remove",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
'''

# The function is at the end of this file; preserve everything before it.
if not old_function.rstrip().endswith("}"):
    sys.exit("ERROR: Unexpected end of utility file; no changes made.")

if "BorderStroke" not in text[:start]:
    print("NOTE: Please verify the BorderStroke import if compilation reports one.")

updated_utils = text[:start] + new_function
if updated_utils == text:
    sys.exit("ERROR: Utility function was not changed.")
utils.write_text(updated_utils)
print("Updated:", utils)

def matching_brace(source, opening):
    depth = 0
    for i in range(opening, len(source)):
        if source[i] == "{":
            depth += 1
        elif source[i] == "}":
            depth -= 1
            if depth == 0:
                return i
    raise ValueError("Unmatched brace")

def update_screen(path, profile_expr, remove_expr):
    source = path.read_text()
    call_start = source.find("ContactQuickActions(")
    if call_start < 0:
        raise ValueError(f"ContactQuickActions call not found in {path}")

    # Find the closing parenthesis of the call.
    opening = source.find("(", call_start)
    depth = 0
    call_end = None
    for i in range(opening, len(source)):
        if source[i] == "(":
            depth += 1
        elif source[i] == ")":
            depth -= 1
            if depth == 0:
                call_end = i
                break
    if call_end is None:
        raise ValueError(f"Unmatched ContactQuickActions call in {path}")

    call = source[call_start:call_end + 1]
    if "profileAction =" in call:
        raise ValueError(f"Already patched: {path}")

    # Add the callbacks to the existing call.
    callbacks = (
        ",\n        profileAction = { " + profile_expr + " }," 
        "\n        removeAction = { " + remove_expr + " }\n    "
    )
    new_call = source[call_start:call_end] + callbacks + ")"

    source = source[:call_start] + new_call + source[call_end + 1:]

    # Remove the separate Profile/Remove Row immediately following the call.
    new_call_end = call_start + len(new_call)
    row_start = source.find("\n", new_call_end)
    row_start = source.find("Row(", row_start)
    if row_start < 0 or row_start - new_call_end > 200:
        raise ValueError(f"Could not safely locate the separate action Row in {path}")

    brace = source.find("{", row_start)
    row_end = matching_brace(source, brace)
    # Include indentation/newline around the Row, but leave the enclosing Column.
    line_start = source.rfind("\n", 0, row_start) + 1
    line_end = row_end + 1
    while line_end < len(source) and source[line_end] in " \t":
        line_end += 1
    if line_end < len(source) and source[line_end] == "\n":
        line_end += 1

    source = source[:line_start] + source[line_end:]
    path.write_text(source)
    print("Updated:", path)

try:
    update_screen(
        student_file,
        "selectedProfileStudent = student",
        "scope.launch { FirestoreSyncService(database).deleteStudentRecord(schoolCode, student) }"
    )
    update_screen(
        teacher_file,
        "selectedTeacherForEdit = staff",
        "scope.launch { FirestoreSyncService(database).deleteStaffRecord(schoolCode, staff) }"
    )
except Exception as exc:
    print("ERROR:", exc)
    print("Review the modified files before building.")
    sys.exit(1)

print("\nPatch applied. Review the diff before publishing.")
