from pathlib import Path
from datetime import datetime
import shutil

p = Path(
    "composeApp/src/commonMain/kotlin/com/school/manage/core/firebase/FirestoreSyncService.kt"
)
original = p.read_text()
s = original
changes = []

def replace_once(old, new, label):
    global s

    # Restore-only replacements must match inside restoreAllFromCloud.
    if label.startswith(("Read batch", "Restore batch",
                         "Read student", "Restore student",
                         "Read staff", "Restore staff",
                         "Read fee", "Restore fee")):
        start = s.find("suspend fun restoreAllFromCloud")
        end = s.find("fun startSync", start)
        if start < 0 or end < 0:
            raise SystemExit(
                "STOP: restore function boundaries not found. "
                "Kotlin source unchanged."
            )
        section = s[start:end]
        count = section.count(old)
        if count != 1:
            raise SystemExit(
                f"STOP: {label}: expected 1 match in restore function, "
                f"found {count}. Kotlin source unchanged."
            )
        section = section.replace(old, new, 1)
        s = s[:start] + section + s[end:]
    else:
        count = s.count(old)
        if count != 1:
            raise SystemExit(
                f"STOP: {label}: expected 1 match, found {count}. "
                "Kotlin source unchanged."
            )
        s = s.replace(old, new, 1)

    changes.append(label)

# Batch restore
replace_once(
    'val sections = getSafeStr(doc, "sections")',
    'val sections = getSafeStr(doc, "sections")\n'
    '                        val feeTemplateRaw = getSafeStr(doc, "feeTemplateRaw")\n'
    '                        val collectTuitionAtAdmission = '
    'getSafeStr(doc, "collectTuitionAtAdmission").toBooleanStrictOrNull() ?: true',
    "Read batch fee settings",
)

replace_once(
    '                                    sections = sections\n'
    '                                )',
    '                                    sections = sections,\n'
    '                                    feeTemplateRaw = feeTemplateRaw,\n'
    '                                    collectTuitionAtAdmission = collectTuitionAtAdmission\n'
    '                                )',
    "Restore batch fee settings",
)

# Student restore
replace_once(
    'val address = getSafeStr(doc, "address")',
    'val address = getSafeStr(doc, "address")\n'
    '                        val aadharNumber = getSafeStr(doc, "aadharNumber")\n'
    '                        val photoBase64 = getSafeStr(doc, "photoBase64")',
    "Read student Aadhaar and photo",
)

replace_once(
    '                                    gender = gender,\n'
    '                                    address = address\n'
    '                                )',
    '                                    gender = gender,\n'
    '                                    address = address,\n'
    '                                    aadharNumber = aadharNumber,\n'
    '                                    photoBase64 = photoBase64\n'
    '                                )',
    "Restore student Aadhaar and photo",
)

# Staff restore: keep password = phone unchanged.
replace_once(
    '                        val joinDate = getSafeStr(doc, "joinDate")',
    '                        val joinDate = getSafeStr(doc, "joinDate")\n'
    '                        val qualification = getSafeStr(doc, "qualification")\n'
    '                        val whatsapp = getSafeStr(doc, "whatsapp").ifBlank { phone }\n'
    '                        val salaryType = getSafeStr(doc, "salaryType").ifBlank { "Monthly" }\n'
    '                        val gender = getSafeStr(doc, "gender").ifBlank { "Male" }\n'
    '                        val address = getSafeStr(doc, "address")\n'
    '                        val photoBase64 = getSafeStr(doc, "photoBase64")',
    "Read staff details and photo",
)

replace_once(
    '                                    joinDate = joinDate,\n'
    '                                    password = phone',
    '                                    joinDate = joinDate,\n'
    '                                    gender = gender,\n'
    '                                    whatsapp = whatsapp,\n'
    '                                    address = address,\n'
    '                                    qualification = qualification,\n'
    '                                    salaryType = salaryType,\n'
    '                                    photoBase64 = photoBase64,\n'
    '                                    password = phone',
    "Restore staff details and photo without changing password logic",
)

# Fee restore
replace_once(
    '                        val studentName = getSafeStr(doc, "studentName")',
    '                        val studentName = getSafeStr(doc, "studentName")\n'
    '                        val gradeClass = getSafeStr(doc, "gradeClass")\n'
    '                        val feeMonth = getSafeStr(doc, "feeMonth")',
    "Read fee class and month",
)

replace_once(
    '                                studentName = studentName,\n'
    '                                schoolCode = schoolCode,',
    '                                studentName = studentName,\n'
    '                                gradeClass = gradeClass,\n'
    '                                feeMonth = feeMonth,\n'
    '                                schoolCode = schoolCode,',
    "Restore fee class and month",
)

if s == original:
    raise SystemExit("STOP: no changes produced. Kotlin source unchanged.")

# Write only after every replacement has passed validation.
backup = p.with_name(
    p.name + ".before_restore_" + datetime.now().strftime("%Y%m%d_%H%M%S")
)
shutil.copy2(p, backup)
p.write_text(s)

print("RESTORE PATCH APPLIED")
print("Backup:", backup)
for change in changes:
    print(" -", change)
print("Review the diff before building.")
