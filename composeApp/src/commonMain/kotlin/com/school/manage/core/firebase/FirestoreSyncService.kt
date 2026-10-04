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

    // Helper to extract string safely without throwing serialization errors
    private fun getSafeStr(doc: dev.gitlive.firebase.firestore.DocumentSnapshot, field: String): String {
        return try {
            if (doc.contains(field)) doc.get<String?>(field) ?: "" else ""
        } catch (e: Exception) {
            ""
        }
    }

    // 1. School Master Backup & Restore
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
                    val code = getSafeStr(doc, "schoolCode").ifBlank { schoolCode }
                    val name = getSafeStr(doc, "schoolName")
                    val phone = getSafeStr(doc, "phone")
                    val pass = getSafeStr(doc, "password")

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

    // 2. Full Two-Way Restore (Cloud -> Room SQLite)
    suspend fun restoreAllFromCloud(schoolCode: String) {
        if (schoolCode.isBlank()) return
        try {
            withTimeout(25000L) {
                val schoolRef = firestore.collection("schools").document(schoolCode)

                // Classes / Batches
                try {
                    val classesDocs = schoolRef.collection("classes").get().documents
                    for (doc in classesDocs) {
                        val id = doc.id.toLongOrNull() ?: 0L
                        val batchName = getSafeStr(doc, "batchName")
                        val subjects = getSafeStr(doc, "subjects")
                        val sections = getSafeStr(doc, "sections")
                        database.batchDao().insertBatch(
                            BatchEntity(id = id, schoolCode = schoolCode, batchName = batchName, subjects = subjects, sections = sections)
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Students
                try {
                    val studentDocs = schoolRef.collection("students").get().documents
                    for (doc in studentDocs) {
                        val id = doc.id.toLongOrNull() ?: 0L
                        val name = getSafeStr(doc, "name")
                        val gradeClass = getSafeStr(doc, "gradeClass")
                        val phone = getSafeStr(doc, "phone")
                        val feeStr = getSafeStr(doc, "monthlyFee")
                        val admissionDate = getSafeStr(doc, "admissionDate")
                        database.studentDao().insertStudent(
                            StudentEntity(
                                id = id,
                                schoolCode = schoolCode,
                                name = name,
                                gradeClass = gradeClass,
                                phone = phone,
                                monthlyFee = feeStr.toDoubleOrNull() ?: 0.0,
                                admissionDate = admissionDate
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Staff
                try {
                    val staffDocs = schoolRef.collection("staff").get().documents
                    for (doc in staffDocs) {
                        val id = doc.id.toLongOrNull() ?: 0L
                        val name = getSafeStr(doc, "name")
                        val role = getSafeStr(doc, "role").ifBlank { "Teacher" }
                        val phone = getSafeStr(doc, "phone")
                        val salStr = getSafeStr(doc, "salary")
                        val joinDate = getSafeStr(doc, "joinDate")
                        database.staffDao().insertStaff(
                            StaffEntity(
                                id = id,
                                schoolCode = schoolCode,
                                name = name,
                                role = role,
                                phone = phone,
                                salary = salStr.toDoubleOrNull() ?: 0.0,
                                joinDate = joinDate,
                                password = phone
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Fees
                try {
                    val feeDocs = schoolRef.collection("fees").get().documents
                    for (doc in feeDocs) {
                        val id = doc.id.toLongOrNull() ?: 0L
                        val studentId = getSafeStr(doc, "studentId").toLongOrNull() ?: 0L
                        val amountStr = getSafeStr(doc, "amountPaid")
                        val paymentDate = getSafeStr(doc, "paymentDate")
                        val remarks = getSafeStr(doc, "remarks")
                        database.feeDao().insertFee(
                            FeeRecordEntity(
                                id = id,
                                studentId = studentId,
                                schoolCode = schoolCode,
                                amountPaid = amountStr.toDoubleOrNull() ?: 0.0,
                                paymentDate = paymentDate,
                                remarks = remarks
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Expenses
                try {
                    val expDocs = schoolRef.collection("expenses").get().documents
                    for (doc in expDocs) {
                        val id = doc.id.toLongOrNull() ?: 0L
                        val title = getSafeStr(doc, "title")
                        val category = getSafeStr(doc, "category")
                        val amountStr = getSafeStr(doc, "amount")
                        val date = getSafeStr(doc, "date")
                        database.expenseDao().insertExpense(
                            ExpenseEntity(
                                id = id,
                                schoolCode = schoolCode,
                                title = title,
                                category = category,
                                amount = amountStr.toDoubleOrNull() ?: 0.0,
                                date = date,
                                notes = ""
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Attendance
                try {
                    val attDocs = schoolRef.collection("attendance").get().documents
                    for (doc in attDocs) {
                        val id = doc.id.toLongOrNull() ?: 0L
                        val studentId = getSafeStr(doc, "studentId").toLongOrNull() ?: 0L
                        val date = getSafeStr(doc, "date")
                        val status = getSafeStr(doc, "status").ifBlank { "Present" }
                        database.attendanceDao().markAttendance(
                            AttendanceEntity(
                                id = id,
                                studentId = studentId,
                                schoolCode = schoolCode,
                                date = date,
                                status = status
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Exams
                try {
                    val examDocs = schoolRef.collection("exams").get().documents
                    for (doc in examDocs) {
                        val id = doc.id.toLongOrNull() ?: 0L
                        val title = getSafeStr(doc, "title")
                        val gradeClass = getSafeStr(doc, "gradeClass")
                        val examDate = getSafeStr(doc, "examDate")
                        val maxMarksStr = getSafeStr(doc, "maxMarks")
                        database.examDao().insertExam(
                            ExamEntity(
                                id = id,
                                schoolCode = schoolCode,
                                title = title,
                                gradeClass = gradeClass,
                                examDate = examDate,
                                maxMarks = maxMarksStr.toDoubleOrNull() ?: 100.0
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Homework
                try {
                    val hwDocs = schoolRef.collection("homework").get().documents
                    for (doc in hwDocs) {
                        val id = doc.id.toLongOrNull() ?: 0L
                        val gradeClass = getSafeStr(doc, "gradeClass")
                        val subject = getSafeStr(doc, "subject")
                        val title = getSafeStr(doc, "title")
                        val description = getSafeStr(doc, "description")
                        val dueDate = getSafeStr(doc, "dueDate")
                        database.homeworkDao().insertHomework(
                            HomeworkEntity(
                                id = id,
                                schoolCode = schoolCode,
                                gradeClass = gradeClass,
                                subject = subject,
                                title = title,
                                description = description,
                                dueDate = dueDate
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Classwork
                try {
                    val cwDocs = schoolRef.collection("classwork").get().documents
                    for (doc in cwDocs) {
                        val id = doc.id.toLongOrNull() ?: 0L
                        val gradeClass = getSafeStr(doc, "gradeClass")
                        val subject = getSafeStr(doc, "subject")
                        val topicTitle = getSafeStr(doc, "topicTitle")
                        val summary = getSafeStr(doc, "summary")
                        val date = getSafeStr(doc, "date")
                        database.classworkDao().insertClasswork(
                            ClassworkEntity(
                                id = id,
                                schoolCode = schoolCode,
                                gradeClass = gradeClass,
                                subject = subject,
                                topicTitle = topicTitle,
                                summary = summary,
                                date = date
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 3. MASTER START SYNC: Restores Cloud -> SQLite, then listens SQLite -> Cloud
    fun startSync(schoolCode: String) {
        if (schoolCode.isBlank()) return

        scope.launch {
            // Pull cloud records into local SQLite if local database was cleared
            restoreAllFromCloud(schoolCode)

            // Ensure School credentials are saved
            try {
                database.schoolDao().getSchoolByCode(schoolCode)?.let { saveSchoolToCloud(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Classes / Batches
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
                                    "batchName" to item.batchName,
                                    "subjects" to item.subjects,
                                    "sections" to item.sections
                                )
                            )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // Students
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

        // Staff
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

        // Fees
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

        // Expenses
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

        // Attendance
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

        // Exams
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

        // Homework
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

        // Classwork
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
