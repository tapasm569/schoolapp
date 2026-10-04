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

        // 1. Upload local students to Cloud Firestore (Backup)
        scope.launch {
            database.studentDao().getStudentsBySchool(schoolCode).collectLatest { studentList ->
                val studentsCollection = firestore
                    .collection("schools")
                    .document(schoolCode)
                    .collection("students")

                for (student in studentList) {
                    studentsCollection.document(student.id.toString()).set(
                        mapOf(
                            "id" to student.id,
                            "schoolCode" to student.schoolCode,
                            "name" to student.name,
                            "gradeClass" to student.gradeClass,
                            "phone" to student.phone,
                            "monthlyFee" to student.monthlyFee,
                            "admissionDate" to student.admissionDate
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
                        val data = docChange.document.data(Map::class) as? Map<*, *> ?: continue
                        val studentId = (data["id"] as? Number)?.toLong() ?: 0L
                        val name = data["name"] as? String ?: ""
                        val gradeClass = data["gradeClass"] as? String ?: ""
                        val phone = data["phone"] as? String ?: ""
                        val fee = (data["monthlyFee"] as? Number)?.toDouble() ?: 0.0

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
                    }
                }
        }
    }
}
