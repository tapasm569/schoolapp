from pathlib import Path

path = Path(
    "composeApp/src/commonMain/kotlin/com/school/manage/core/utils/ProfilePhotoUtils.kt"
)

text = path.read_text(encoding="utf-8")

old = '''                            "https://wa.me/$formattedWa?text=" +
                                java.net.URLEncoder.encode(messageText, "UTF-8")'''

new = '''                            "https://wa.me/$formattedWa?text=" +
                                encodeWhatsAppMessage(messageText)'''

if text.count(old) != 1:
    raise SystemExit(
        "ERROR: Expected exactly one java.net.URLEncoder call. "
        "No changes made."
    )

text = text.replace(old, new, 1)

helper = '''
private fun encodeWhatsAppMessage(message: String): String {
    val bytes = message.encodeToByteArray()
    val hex = "0123456789ABCDEF"
    return buildString {
        for (byte in bytes) {
            val value = byte.toInt() and 0xFF
            val unreserved =
                value in 65..90 ||
                value in 97..122 ||
                value in 48..57 ||
                value == 45 || value == 46 ||
                value == 95 || value == 126

            if (unreserved) {
                append(value.toChar())
            } else {
                append('%')
                append(hex[value shr 4])
                append(hex[value and 15])
            }
        }
    }
}
'''

if "private fun encodeWhatsAppMessage(" in text:
    raise SystemExit(
        "ERROR: Encoder helper already exists. No helper added."
    )

text = text.rstrip() + "\n" + helper
path.write_text(text, encoding="utf-8")

print("Added Kotlin Multiplatform-compatible WhatsApp encoding.")
