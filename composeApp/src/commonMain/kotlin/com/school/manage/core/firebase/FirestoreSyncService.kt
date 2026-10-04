package com.school.manage.core.firebase

import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class FirestoreSyncService(private val database: AppDatabase) {

    private val firestore = Firebase.firestore
    private val scope = CoroutineScope(Dispatchers.IO)

    // Cloud school credentials backup and restore
    suspend fun saveSchoolToCloud(school: SchoolEntity): Boolean {
        return try {
            withTimeout(10000L) {
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
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun restoreSchoolFromCloud(schoolCode: String): SchoolEntity? {
        return try {
            withTimeout(10000L) {
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
            }
        } catch (e: Exception) {
            null
        }
    }

    // Continuous Real-Time Observers for All Database Entities
    fun startSync(schoolCode: String) {
        if (schoolCode.isBlank()) return

        scope.launch {
            try {
                database.schoolDao().getSchoolByCode(schoolCode)?.let { saveSchoolToCloud(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 1. Batches / Classes
        scope.launch {
            database.batchDao().getBatchesBySchool(schoolCode).collectLatest { list ->
                for (item in list) {
                    try {
                        firestore.collection("schools").document(schoolCode)
                            .collection("classes").document(item.id.toString())
                            .set(
                                mapOf(
                                    "id" to item.id.toString(),
                                    "schoolCode" to item.schoolCode,
                                    "name" to item.name,
                                    "gradeClass" to item.gradeClass,
                                    "section" to item.section,
                                    "stream" to item.stream
                                )
                            )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // 2. Students
        scope.launch {
            database.studentDao().getStudentsBySchool(schoolCode).collectLatest { list ->
                for (item in list) {
                    try {
                        firestore.collection("schools").document(schoolCode)
                            .collection("students").document(item.id.toString())
                            .set(
                                mapOf(
                                    "id" to item.id.toString(),
                                    "schoolCode" to item.schoolCode,
                                    "name" to item.name,
                                    "gradeClass" to item.gradeClass,
                                    "phone" to item.phone,
                                    "monthlyFee" to item.monthlyFee.toString(),
                                    "admissionDate" to item.admissionDate
                                )
                            )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // 3. Staff / Teachers
        scope.launch {
            database.staffDao().getStaffBySchool(schoolCode).collectLatest { list ->
                for (item in list) {
                    try {
                        firestore.collection("schools").document(schoolCode)
                            .collection("staff").document(item.id.toString())
                            .set(
                                mapOf(
                                    "id" to item.id.toString(),
                                    "schoolCode" to item.schoolCode,
                                    "name" to item.name,
                                    "role" to item.role,
                                    "phone" to item.phone,
                                    "salary" to item.salary.toString(),
                                    "joinDate" to item.joinDate
                                )
                            )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // 4. Fees
        scope.launch {
            database.feeDao().getFeeRecordsBySchool(schoolCode).collectLatest { list ->
                for (item in list) {
                    try {
                        firestore.collection("schools").document(schoolCode)
                            .collection("fees").document(item.id.toString())
                            .set(
                                mapOf(
                                    "id" to item.id.toString(),
                                    "studentId" to item.studentId.toString(),
                                    "schoolCode" to item.schoolCode,
                                    "amountPaid" to item.amountPaid.toString(),
                                    "paymentDate" to item.paymentDate,
                                    "remarks" to item.remarks
                                )
                            )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // 5. Expenses
        scope.launch {
            database.expenseDao().getExpensesBySchool(schoolCode).collectLatest { list ->
                for (item in list) {
                    try {
                        firestore.collection("schools").document(schoolCode)
                            .collection("expenses").document(item.id.toString())
                            .set(
                                mapOf(
                                    "id" to item.id.toString(),
                                    "schoolCode" to item.schoolCode,
                                    "title" to item.title,
                                    "category" to item.category,
                                    "amount" to item.amount.toString(),
                                    "date" to item.date
                                )
                            )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // 6. Attendance
        scope.launch {
            database.attendanceDao().getAttendanceBySchool(schoolCode).collectLatest { list ->
                for (item in list) {
                    try {
                        firestore.collection("schools").document(schoolCode)
                            .collection("attendance").document(item.id.toString())
                            .set(
                                mapOf(
                                    "id" to item.id.toString(),
                                    "studentId" to item.studentId.toString(),
                                    "schoolCode" to item.schoolCode,
                                    "date" to item.date,
                                    "status" to item.status
                                )
                            )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // 7. Exams
        scope.launch {
            database.examDao().getExamsBySchool(schoolCode).collectLatest { list ->
                for (item in list) {
                    try {
                        firestore.collection("schools").document(schoolCode)
                            .collection("exams").document(item.id.toString())
                            .set(
                                mapOf(
                                    "id" to item.id.toString(),
                                    "schoolCode" to item.schoolCode,
                                    "title" to item.title,
                                    "gradeClass" to item.gradeClass,
                                    "examDate" to item.examDate,
                                    "maxMarks" to item.maxMarks.toString()
                                )
                            )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // 8. Homework
        scope.launch {
            database.homeworkDao().getHomeworkBySchool(schoolCode).collectLatest { list ->
                for (item in list) {
                    try {
                        firestore.collection("schools").document(schoolCode)
                            .collection("homework").document(item.id.toString())
                            .set(
                                mapOf(
                                    "id" to item.id.toString(),
                                    "schoolCode" to item.schoolCode,
                                    "gradeClass" to item.gradeClass,
                                    "subject" to item.subject,
                                    "title" to item.title,
                                    "description" to item.description,
                                    "dueDate" to item.dueDate
                                )
                            )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // 9. Classwork
        scope.launch {
            database.classworkDao().getClassworkBySchool(schoolCode).collectLatest { list ->
                for (item in list) {
                    try {
                        firestore.collection("schools").document(schoolCode)
                            .collection("classwork").document(item.id.toString())
                            .set(
                                mapOf(
                                    "id" to item.id.toString(),
                                    "schoolCode" to item.schoolCode,
                                    "gradeClass" to item.gradeClass,
                                    "subject" to item.subject,
                                    "topicTitle" to item.topicTitle,
                                    "summary" to item.summary,
                                    "date" to item.date
                                )
                            )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
