package com.school.manage.core.firebase

import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.SchoolEntity
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

    // Save School Credentials to Cloud Firestore on Registration
    suspend fun saveSchoolToCloud(school: SchoolEntity) {
        try {
            firestore.collection("schools")
                .document(school.schoolCode)
                .set(
                    mapOf(
                        "schoolCode" to school.schoolCode,
                        "schoolName" to school.schoolName,
                        "phone" to school.phone,
                        "password" to school.password
                    )
                )
        } catch (e: Exception) {
            // Offline fallback
        }
    }

    // Verify and restore School from Cloud when Local Room DB is wiped (e.g. after Clear Data)
    suspend fun restoreSchoolFromCloud(schoolCode: String): SchoolEntity? {
        return try {
            val doc = firestore.collection("schools").document(schoolCode).get()
            if (doc.exists) {
                val code: String = if (doc.contains("schoolCode")) doc.get("schoolCode") else schoolCode
                val name: String = if (doc.contains("schoolName")) doc.get("schoolName") else ""
                val phone: String = if (doc.contains("phone")) doc.get("phone") else ""
                val pass: String = if (doc.contains("password")) doc.get("password") else ""

                val restored = SchoolEntity(
                    schoolCode = code,
                    schoolName = name,
                    phone = phone,
                    password = pass
                )
                database.schoolDao().registerSchool(restored)
                restored
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun startSync(schoolCode: String) {
        if (schoolCode.isBlank()) return

        // 1. Sync Local School Profile -> Cloud
        scope.launch {
            val localSchool = database.schoolDao().getSchoolByCode(schoolCode)
            if (localSchool != null) {
                saveSchoolToCloud(localSchool)
            }
        }

        // 2. Upload local Room students to Cloud Firestore (Backup)
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
                            "monthlyFee" to student.monthlyFee.toString(),
                            "admissionDate" to student.admissionDate
                        )
                    )
                }
            }
        }

        // 3. Download cloud updates into local Room Database
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
                            val admissionDate = if (doc.contains("admissionDate")) doc.get("admissionDate") else "03/10/2026"

                            if (studentId != 0L && name.isNotBlank()) {
                                database.studentDao().insertStudent(
                                    StudentEntity(
                                        id = studentId,
                                        schoolCode = schoolCode,
                                        name = name,
                                        gradeClass = gradeClass,
                                        phone = phone,
                                        monthlyFee = fee,
                                        admissionDate = admissionDate
                                    )
                                )
                            }
                        } catch (e: Exception) {}
                    }
                }
        }
    }
}
