from pathlib import Path

service_path = Path(
    "composeApp/src/commonMain/kotlin/com/school/manage/core/firebase/FirestoreSyncService.kt"
)
student_path = Path(
    "composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/StudentScreens.kt"
)
staff_path = Path(
    "composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/ExpenseAndStaffScreens.kt"
)

files = {
    service_path: service_path.read_text(),
    student_path: student_path.read_text(),
    staff_path: staff_path.read_text(),
}

def replace_once(path, old, new, label):
    source = files[path]
    count = source.count(old)
    if count != 1:
        raise SystemExit(
            f"STOP: {label}: expected exactly 1 match, found {count}. "
            "No source files have been written."
        )
    files[path] = source.replace(old, new, 1)
    print(f"Prepared: {label}")

# Add deletion methods before startSync().
old = """    fun startSync(schoolCode: String) {
"""
new = """    /**
     * A tombstone prevents a deleted student from being recreated by
     * background synchronization or cloud restoration.
     */
    suspend fun deleteStudentRecord(
        schoolCode: String,
        student: StudentEntity
    ): Boolean {
        if (schoolCode.isBlank() || student.schoolCode != schoolCode) {
            return false
        }

        return try {
            val schoolRef = firestore.collection("schools").document(schoolCode)
            schoolRef.collection("deletedStudents")
                .document(student.id.toString())
                .set(
                    mapOf(
                        "id" to student.id.toString(),
                        "deleted" to true
                    )
                )

            // The marker is already saved. A failed cloud-document deletion
            // must not allow this record to be restored or uploaded again.
            try {
                schoolRef.collection("students")
                    .document(student.id.toString())
                    .delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            database.studentDao().deleteStudent(student)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * A tombstone prevents a deleted staff member from being recreated by
     * background synchronization or cloud restoration.
     */
    suspend fun deleteStaffRecord(
        schoolCode: String,
        staff: StaffEntity
    ): Boolean {
        if (schoolCode.isBlank() || staff.schoolCode != schoolCode) {
            return false
        }

        return try {
            val schoolRef = firestore.collection("schools").document(schoolCode)
            schoolRef.collection("deletedStaff")
                .document(staff.id.toString())
                .set(
                    mapOf(
                        "id" to staff.id.toString(),
                        "deleted" to true
                    )
                )

            // The marker is already saved, so a cloud deletion error cannot
            // cause this record to be restored or uploaded again.
            try {
                schoolRef.collection("staff")
                    .document(staff.id.toString())
                    .delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            database.staffDao().deleteStaff(staff)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun startSync(schoolCode: String) {
"""
replace_once(service_path, old, new, "cloud deletion methods")

# Skip tombstoned students during cloud restoration.
old = """                    for (doc in studentDocs) {
                        val id = doc.id.toLongOrNull() ?: 0L
"""
new = """                    for (doc in studentDocs) {
                        val deletionMarker = schoolRef
                            .collection("deletedStudents")
                            .document(doc.id)
                            .get()
                        if (deletionMarker.exists) continue

                        val id = doc.id.toLongOrNull() ?: 0L
"""
replace_once(service_path, old, new, "student restore tombstone check")

# Skip tombstoned staff during cloud restoration.
old = """                    for (doc in staffDocs) {
                        val id = doc.id.toLongOrNull() ?: 0L
"""
new = """                    for (doc in staffDocs) {
                        val deletionMarker = schoolRef
                            .collection("deletedStaff")
                            .document(doc.id)
                            .get()
                        if (deletionMarker.exists) continue

                        val id = doc.id.toLongOrNull() ?: 0L
"""
replace_once(service_path, old, new, "staff restore tombstone check")

# Skip tombstoned students during background upload.
old = """        scope.launch {
            database.studentDao().getStudentsBySchool(schoolCode).collectLatest { list ->
                for (item in list) {
                    try {
                        firestore.collection("schools").document(schoolCode)
                            .collection("students").document(item.id.toString())
"""
new = """        scope.launch {
            database.studentDao().getStudentsBySchool(schoolCode).collectLatest { list ->
                for (item in list) {
                    try {
                        val deletionMarker = firestore.collection("schools")
                            .document(schoolCode)
                            .collection("deletedStudents")
                            .document(item.id.toString())
                            .get()
                        if (deletionMarker.exists) continue

                        firestore.collection("schools").document(schoolCode)
                            .collection("students").document(item.id.toString())
"""
replace_once(service_path, old, new, "student sync tombstone check")

# Skip tombstoned staff during background upload.
old = """        scope.launch {
            database.staffDao().getStaffBySchool(schoolCode).collectLatest { list ->
                for (item in list) {
                    try {
                        firestore.collection("schools").document(schoolCode)
                            .collection("staff").document(item.id.toString())
"""
new = """        scope.launch {
            database.staffDao().getStaffBySchool(schoolCode).collectLatest { list ->
                for (item in list) {
                    try {
                        val deletionMarker = firestore.collection("schools")
                            .document(schoolCode)
                            .collection("deletedStaff")
                            .document(item.id.toString())
                            .get()
                        if (deletionMarker.exists) continue

                        firestore.collection("schools").document(schoolCode)
                            .collection("staff").document(item.id.toString())
"""
replace_once(service_path, old, new, "staff sync tombstone check")

# Route the Student Directory Remove button through the cloud-aware method.
old = """        scope.launch { database.studentDao().deleteStudent(student) }
"""
new = """        scope.launch {
            FirestoreSyncService(database).deleteStudentRecord(schoolCode, student)
        }
"""
replace_once(student_path, old, new, "student Remove button")

# Route the Staff Directory Remove button through the cloud-aware method.
old = """                                            scope.launch { database.staffDao().deleteStaff(staff) }
"""
new = """                                            scope.launch {
                                                FirestoreSyncService(database).deleteStaffRecord(schoolCode, staff)
                                            }
"""
replace_once(staff_path, old, new, "staff Remove button")

# All validations passed. Write backups and then the patched sources.
backups = {}
for path in files:
    backup = path.with_name(
        path.name + ".before_cloud_delete_20261019"
    )
    if backup.exists():
        raise SystemExit(
            f"STOP: backup already exists: {backup}. "
            "No source files have been written."
        )
    backups[path] = backup

for path, backup in backups.items():
    backup.write_text(path.read_text())

for path, content in files.items():
    path.write_text(content)

print("\nPATCH APPLIED")
print("Backups created:")
for backup in backups.values():
    print(" -", backup)
print("\nChanged source files:")
for path in files:
    print(" -", path)
