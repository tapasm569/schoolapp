from pathlib import Path
from datetime import datetime
import shutil

p = Path(
    "composeApp/src/commonMain/kotlin/com/school/manage/core/firebase/FirestoreSyncService.kt"
)

original = p.read_text()
s = original
changes = []

def patch_section(start_marker, end_marker, replacements, label):
    global s

    if label.startswith("Automatic"):
        start = s.find(start_marker, sync_start)
    else:
        start = s.find(start_marker)
    if start < 0:
        raise SystemExit(f"STOP: cannot find section start: {label}")

    end = s.find(end_marker, start + len(start_marker))
    if end < 0:
        raise SystemExit(f"STOP: cannot find section end: {label}")

    section = s[start:end]

    for old, new, description in replacements:
        count = section.count(old)
        if count != 1:
            raise SystemExit(
                f"STOP: {label}/{description}: expected 1 match, "
                f"found {count}. Source file unchanged."
            )
        section = section.replace(old, new, 1)
        changes.append(f"{label}: {description}")

    s = s[:start] + section + s[end:]


# 1. Initial cloud upload: batches
patch_section(
    "suspend fun pushAllLocalDataToCloud",
    "// 2. Students",
    [
        (
            '"sections" to item.sections',
            '"sections" to item.sections,\n'
            '                            "feeTemplateRaw" to item.feeTemplateRaw,\n'
            '                            "collectTuitionAtAdmission" to item.collectTuitionAtAdmission',
            "fee-template settings",
        ),
    ],
    "Initial batch upload",
)

# 2. Initial cloud upload: students
patch_section(
    "// 2. Students",
    "// 3. Staff",
    [
        (
            '"address" to item.address',
            '"address" to item.address,\n'
            '                            "aadharNumber" to item.aadharNumber,\n'
            '                            "photoBase64" to item.photoBase64',
            "Aadhaar and profile photo",
        ),
    ],
    "Initial student upload",
)

# 3. Initial cloud upload: staff
patch_section(
    "// 3. Staff",
    "// 4. Fees",
    [
        (
            '"address" to item.address',
            '"address" to item.address,\n'
            '                            "photoBase64" to item.photoBase64',
            "profile photo",
        ),
    ],
    "Initial staff upload",
)

# 4. Initial cloud upload: fees
patch_section(
    "// 4. Fees",
    "suspend fun restoreAllFromCloud",
    [
        (
            '"studentName" to item.studentName,\n'
            '                            "amountPaid" to item.amountPaid.toString(),',
            '"studentName" to item.studentName,\n'
            '                            "gradeClass" to item.gradeClass,\n'
            '                            "feeMonth" to item.feeMonth,\n'
            '                            "amountPaid" to item.amountPaid.toString(),',
            "class and fee month",
        ),
    ],
    "Initial fee upload",
)

# Locate automatic-sync function.
sync_start = s.find("fun startSync(schoolCode: String)")
if sync_start < 0:
    raise SystemExit("STOP: startSync function not found. Source unchanged.")

# 5. Automatic batch sync
patch_section(
    "database.batchDao().getBatchesBySchool",
    "database.studentDao().getStudentsBySchool",
    [
        (
            '"sections" to item.sections',
            '"sections" to item.sections,\n'
            '                                    "feeTemplateRaw" to item.feeTemplateRaw,\n'
            '                                    "collectTuitionAtAdmission" to item.collectTuitionAtAdmission',
            "fee-template settings",
        ),
    ],
    "Automatic batch sync",
)

# 6. Automatic student sync
patch_section(
    "database.studentDao().getStudentsBySchool",
    "database.staffDao().getStaffBySchool",
    [
        (
            '"address" to item.address',
            '"address" to item.address,\n'
            '                                    "aadharNumber" to item.aadharNumber,\n'
            '                                    "photoBase64" to item.photoBase64',
            "Aadhaar and profile photo",
        ),
    ],
    "Automatic student sync",
)

# 7. Automatic staff sync
patch_section(
    "database.staffDao().getStaffBySchool",
    "database.feeDao().getFeeRecordsBySchool",
    [
        (
            '"salary" to item.salary.toString(),\n'
            '                                    "joinDate" to item.joinDate',
            '"salary" to item.salary.toString(),\n'
            '                                    "qualification" to item.qualification,\n'
            '                                    "whatsapp" to item.whatsapp,\n'
            '                                    "salaryType" to item.salaryType,\n'
            '                                    "gender" to item.gender,\n'
            '                                    "address" to item.address,\n'
            '                                    "photoBase64" to item.photoBase64,\n'
            '                                    "joinDate" to item.joinDate',
            "missing staff fields and profile photo",
        ),
    ],
    "Automatic staff sync",
)

# 8. Automatic fee sync
patch_section(
    "database.feeDao().getFeeRecordsBySchool",
    "\n    }\n}",
    [
        (
            '"studentName" to item.studentName,\n'
            '                                    "amountPaid" to item.amountPaid.toString(),',
            '"studentName" to item.studentName,\n'
            '                                    "gradeClass" to item.gradeClass,\n'
            '                                    "feeMonth" to item.feeMonth,\n'
            '                                    "amountPaid" to item.amountPaid.toString(),',
            "class and fee month",
        ),
    ],
    "Automatic fee sync",
)

if s == original:
    raise SystemExit("STOP: no changes generated. Source unchanged.")

# All checks passed. Only now create a backup and write.
backup = p.with_name(
    p.name + ".before_safe_fields_" + datetime.now().strftime("%Y%m%d_%H%M%S")
)
shutil.copy2(p, backup)
p.write_text(s)

print("PATCH APPLIED")
print("Backup:", backup)
print("Changes:")
for item in changes:
    print(" -", item)
print("Next: inspect git diff before building.")
