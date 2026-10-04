package com.school.manage.core.firebase

import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.SchoolEntity
import com.school.manage.core.database.entity.StudentEntity
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class FirestoreSyncService(private val database: AppDatabase) {

    private val firestore = Firebase.firestore
    private val scope = CoroutineScope(Dispatchers.IO)

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
                database.schoolDao().insertSchool(restored)
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

        scope.launch {
            val localSchool = database.schoolDao().getSchoolByCode(schoolCode)
            if (localSchool != null) {
                saveSchoolToCloud(localSchool)
            }

            val students = database.studentDao().getStudentsBySchool(schoolCode).firstOrNull() ?: emptyList()
            val col = firestore.collection("schools").document(schoolCode).collection("students")
            for (st in students) {
                col.document(st.id.toString()).set(
                    mapOf(
                        "id" to st.id.toString(),
                        "schoolCode" to st.schoolCode,
                        "name" to st.name,
                        "gradeClass" to st.gradeClass,
                        "phone" to st.phone,
                        "monthlyFee" to st.monthlyFee.toString(),
                        "admissionDate" to st.admissionDate
                    )
                )
            }

            col.snapshots.collect { snapshot ->
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
