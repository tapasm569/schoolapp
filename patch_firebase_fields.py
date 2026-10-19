from pathlib import Path
import shutil
from datetime import datetime

p = Path(
    "composeApp/src/commonMain/kotlin/com/school/manage/core/firebase/FirestoreSyncService.kt"
)
original = p.read_text()
s = original

def replace_once(old, new, label):
    global s
    count = s.count(old)
    if count != 1:
        raise SystemExit(
            f"STOP: {label}: expected 1 match, found {count}. "
            "Source file unchanged."
        )
    s = s.replace(old, new, 1)

# Batch upload in pushAllLocalDataToCloud
replace_once(
    '"sections" to item.sections',
    '"sections" to item.sections,\n'
    '                            "feeTemplateRaw" to item.feeTemplateRaw,\n'
    '                            "collectTuitionAtAdmission" to item.collectTuitionAtAdmission',
    "batch upload",
)

# Student upload: insert only in the first student-map occurrence.
replace_once(
    '"address" to item.address',
    '"address" to item.address,\n'
    '                            "aadharNumber" to item.aadharNumber,\n'
    '                            "photoBase64" to item.photoBase64',
    "student upload",
)

# Staff upload: identify the staff map using its salaryType field.
replace_once(
    '"salaryType" to item.salaryType,\n'
    '                            "joinDate" to item.joinDate,',
    '"salaryType" to item.salaryType,\n'
    '                            "photoBase64" to item.photoBase64,\n'
    '                            "joinDate" to item.joinDate,',
    "staff upload",
)

# Fee upload.
replace_once(
    '"studentName" to item.studentName,\n'
    '                            "amountPaid" to item.amountPaid.toString(),',
    '"studentName" to item.studentName,\n'
    '                            "gradeClass" to item.gradeClass,\n'
    '                            "feeMonth" to item.feeMonth,\n'
    '                            "amountPaid" to item.amountPaid.toString(),',
    "fee upload",
)

# Automatic batch sync map.
replace_once(
    '"sections" to item.sections\n'
    '                                )',
    '"sections" to item.sections,\n'
    '                                    "feeTemplateRaw" to item.feeTemplateRaw,\n'
    '                                    "collectTuitionAtAdmission" to item.collectTuitionAtAdmission\n'
    '                                )',
    "batch automatic sync",
)

# Automatic student sync map: target its map using the Aadhaar field
# already added to the initial upload map.
student_marker = (
    'schoolRef.collection("students").document(item.id.toString())'
)
if s.count(student_marker) != 1:
    raise SystemExit("STOP: student watcher location not unique; source unchanged.")
pos = s.index(student_marker)
end = s.index("                            )", pos)
block = s[pos:end]
if '"photoBase64" to item.photoBase64' in block:
    raise SystemExit("STOP: unexpected existing photo field in watcher; source unchanged.")
old = '"address" to item.address'
if block.count(old) != 1:
    raise SystemExit("STOP: student watcher address not unique; source unchanged.")
block = block.replace(
    old,
    '"address" to item.address,\n'
    '                                    "aadharNumber" to item.aadharNumber,\n'
    '                                    "photoBase64" to item.photoBase64',
    1,
)
s = s[:pos] + block + s[end:]

# Automatic staff sync map.
replace_once(
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
    "staff automatic sync",
)

# Automatic fee sync map.
replace_once(
    '"studentName" to item.studentName,\n'
    '                                    "amountPaid" to item.amountPaid.toString(),',
    '"studentName" to item.studentName,\n'
    '                                    "gradeClass" to item.gradeClass,\n'
    '                                    "feeMonth" to item.feeMonth,\n'
    '                                    "amountPaid" to item.amountPaid.toString(),',
    "fee automatic sync",
)

if s == original:
    raise SystemExit("STOP: no changes produced; source unchanged.")

backup = p.with_name(
    p.name + ".before_fields_" + datetime.now().strftime("%Y%m%d_%H%M%S")
)
shutil.copy2(p, backup)
p.write_text(s)
print("Patch applied:", p)
print("Backup created:", backup)
print("Review the diff before building.")
