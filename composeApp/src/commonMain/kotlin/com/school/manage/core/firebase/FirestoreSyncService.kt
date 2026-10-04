package com.school.manage.core.firebase

import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.StudentEntity
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FirestoreSyncService(private val database: AppDatabase) {

    private val firestore = Firebase.firestore
    private val scope = CoroutineScope(Dispatchers.IO)

    fun startSync(schoolCode: String) {
        if (schoolCode.isBlank()) return

        // 1. Upload local Room students to Cloud Firestore (Backup)
        scope.launch {
            database.studentDao().getStudentsBySchool(schoolCode).collectLatest { studentList ->
                val col = firestore
                    .collection("schools")
                    .document(schoolCode)
                    .collection("students")

                for (student in studentList) {
                    col.document(student.id.toString()).set(
                        mapOf(
                            "id" to student.id.toString(),
                            "schoolCode" to student.schoolCode,
                            "name" to student.name,
                            "gradeClass" to student.gradeClass,
                            "phone" to student.phone,
                            "monthlyFee" to student.monthlyFee.toString()
                        )
                    )
                }
            }
        }

        // 2. Download cloud updates into local Room Database
        scope.launch {
            firestore.collection("schools")
                .document(schoolCode)
                .collection("students")
                .snapshots
                .collectLatest { snapshot ->
                    for (docChange in snapshot.documentChanges) {
                        val doc = docChange.document
                        try {
                            val idStr: String = if (doc.contains("id")) doc.get("id") else ""
                            val studentId = idStr.toLongOrNull() ?: 0L
                            val name: String = if (doc.contains("name")) doc.get("name") else ""
                            val gradeClass: String = if (doc.contains("gradeClass")) doc.get("gradeClass") else ""
                            val phone: String = if (doc.contains("phone")) doc.get("phone") else ""
                            val feeStr: String = if (doc.contains("monthlyFee")) doc.get("monthlyFee") else "0"
                            val fee = feeStr.toDoubleOrNull() ?: 0.0

                            if (studentId != 0L && name.isNotBlank()) {
                                database.studentDao().insertStudent(
                                    StudentEntity(
                                        id = studentId,
                                        schoolCode = schoolCode,
                                        name = name,
                                        gradeClass = gradeClass,
                                        phone = phone,
                                        monthlyFee = fee
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            // ignore missing or malformed records
                        }
                    }
                }
        }
    }
}
