package com.school.manage.core.firebase

import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.*
import com.school.manage.core.database.entity.AnnouncementEntity
import com.school.manage.core.database.entity.EnquiryEntity
import com.school.manage.core.database.entity.LeaveRequestEntity
import com.school.manage.core.database.entity.OnlineClassEntity
import com.school.manage.core.database.entity.QuestionBankEntity
import com.school.manage.core.database.entity.StaffLogEntity
import com.school.manage.core.database.entity.TimetableEntity
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

                // Enquiries
                try {
                    val enqDocs = schoolRef.collection("enquiries").get().documents
                    for (doc in enqDocs) {
                        val id = doc.id.toLongOrNull() ?: 0L
                        val studentName = getSafeStr(doc, "studentName")
                        val parentName = getSafeStr(doc, "parentName")
                        val phone = getSafeStr(doc, "phone")
                        val gradeClass = getSafeStr(doc, "gradeClass")
                        val date = getSafeStr(doc, "date")
                        val status = getSafeStr(doc, "status")
                        val notes = getSafeStr(doc, "notes")
                        database.enquiryDao().insertEnquiry(
                            EnquiryEntity(
                                id = id,
                                schoolCode = schoolCode,
                                studentName = studentName,
                                parentName = parentName,
                                phone = phone,
                                gradeClass = gradeClass,
                                date = date,
                                status = if (status.isNotBlank()) status else "NEW",
                                notes = notes
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Staff Logs
                try {
                    val logDocs = schoolRef.collection("staff_logs").get().documents
                    for (doc in logDocs) {
                        val id = doc.id.toLongOrNull() ?: 0L
                        val staffId = getSafeStr(doc, "staffId").toLongOrNull() ?: 0L
                        val staffName = getSafeStr(doc, "staffName")
                        val date = getSafeStr(doc, "date")
                        val checkIn = getSafeStr(doc, "checkIn")
                        val checkOut = getSafeStr(doc, "checkOut")
                        val activityNote = getSafeStr(doc, "activityNote")
                        val status = getSafeStr(doc, "status")
                        database.staffLogDao().insertStaffLog(
                            StaffLogEntity(
                                id = id,
                                schoolCode = schoolCode,
                                staffId = staffId,
                                staffName = staffName,
                                date = date,
                                checkIn = if (checkIn.isNotBlank()) checkIn else "09:00 AM",
                                checkOut = if (checkOut.isNotBlank()) checkOut else "03:30 PM",
                                activityNote = activityNote,
                                status = if (status.isNotBlank()) status else "ON_TIME"
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Announcements
                try {
                    val annDocs = schoolRef.collection("announcements").get().documents
                    for (doc in annDocs) {
                        val id = doc.id.toLongOrNull() ?: 0L
                        val title = getSafeStr(doc, "title")
                        val message = getSafeStr(doc, "message")
                        val targetAudience = getSafeStr(doc, "targetAudience")
                        val priority = getSafeStr(doc, "priority")
                        val date = getSafeStr(doc, "date")
                        database.announcementDao().insertAnnouncement(
                            AnnouncementEntity(
                                id = id,
                                schoolCode = schoolCode,
                                title = title,
                                message = message,
                                targetAudience = if (targetAudience.isNotBlank()) targetAudience else "ALL",
                                priority = if (priority.isNotBlank()) priority else "NORMAL",
                                date = date
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Leave Requests
                try {
                    val leaveDocs = schoolRef.collection("leave_requests").get().documents
                    for (doc in leaveDocs) {
                        val id = doc.id.toLongOrNull() ?: 0L
                        val applicantName = getSafeStr(doc, "applicantName")
                        val applicantType = getSafeStr(doc, "applicantType")
                        val startDate = getSafeStr(doc, "startDate")
                        val endDate = getSafeStr(doc, "endDate")
                        val reason = getSafeStr(doc, "reason")
                        val status = getSafeStr(doc, "status")
                        database.leaveDao().insertLeave(
                            LeaveRequestEntity(
                                id = id,
                                schoolCode = schoolCode,
                                applicantName = applicantName,
                                applicantType = if (applicantType.isNotBlank()) applicantType else "STUDENT",
                                startDate = startDate,
                                endDate = endDate,
                                reason = reason,
                                status = if (status.isNotBlank()) status else "PENDING"
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Timetables
                try {
                    val ttDocs = schoolRef.collection("timetables").get().documents
                    for (doc in ttDocs) {
                        val id = doc.id.toLongOrNull() ?: 0L
                        val gradeClass = getSafeStr(doc, "gradeClass")
                        val dayOfWeek = getSafeStr(doc, "dayOfWeek")
                        val periodNo = getSafeStr(doc, "periodNo").toIntOrNull() ?: 1
                        val timeSlot = getSafeStr(doc, "timeSlot")
                        val subject = getSafeStr(doc, "subject")
                        val teacherName = getSafeStr(doc, "teacherName")
                        database.timetableDao().insertTimetable(
                            TimetableEntity(
                                id = id,
                                schoolCode = schoolCode,
                                gradeClass = gradeClass,
                                dayOfWeek = dayOfWeek,
                                periodNo = periodNo,
                                timeSlot = timeSlot,
                                subject = subject,
                                teacherName = teacherName
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Online Classes
                try {
                    val ocDocs = schoolRef.collection("online_classes").get().documents
                    for (doc in ocDocs) {
                        val id = doc.id.toLongOrNull() ?: 0L
                        val gradeClass = getSafeStr(doc, "gradeClass")
                        val subject = getSafeStr(doc, "subject")
                        val title = getSafeStr(doc, "title")
                        val meetingUrl = getSafeStr(doc, "meetingUrl")
                        val classDate = getSafeStr(doc, "classDate")
                        val classTime = getSafeStr(doc, "classTime")
                        database.onlineClassDao().insertOnlineClass(
                            OnlineClassEntity(
                                id = id,
                                schoolCode = schoolCode,
                                gradeClass = gradeClass,
                                subject = subject,
                                title = title,
                                meetingUrl = meetingUrl,
                                classDate = classDate,
                                classTime = classTime
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Question Bank
                try {
                    val qbDocs = schoolRef.collection("question_bank").get().documents
                    for (doc in qbDocs) {
                        val id = doc.id.toLongOrNull() ?: 0L
                        val gradeClass = getSafeStr(doc, "gradeClass")
                        val subject = getSafeStr(doc, "subject")
                        val chapterTopic = getSafeStr(doc, "chapterTopic")
                        val questionText = getSafeStr(doc, "questionText")
                        val answerKey = getSafeStr(doc, "answerKey")
                        val questionType = getSafeStr(doc, "questionType")
                        database.questionBankDao().insertQuestion(
                            QuestionBankEntity(
                                id = id,
                                schoolCode = schoolCode,
                                gradeClass = gradeClass,
                                subject = subject,
                                chapterTopic = chapterTopic,
                                questionText = questionText,
                                answerKey = answerKey,
                                questionType = if (questionType.isNotBlank()) questionType else "SHORT"
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

        // Enquiries
        scope.launch {
            database.enquiryDao().getEnquiriesBySchool(schoolCode).collectLatest { list ->
                for (item in list) {
                    try {
                        firestore.collection("schools").document(schoolCode)
                            .collection("enquiries").document(item.id.toString())
                            .set(
                                mapOf(
                                    "id" to item.id.toString(),
                                    "schoolCode" to item.schoolCode,
                                    "studentName" to item.studentName,
                                    "parentName" to item.parentName,
                                    "phone" to item.phone,
                                    "gradeClass" to item.gradeClass,
                                    "date" to item.date,
                                    "status" to item.status,
                                    "notes" to item.notes
                                )
                            )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // Staff Logs
        scope.launch {
            database.staffLogDao().getStaffLogsBySchool(schoolCode).collectLatest { list ->
                for (item in list) {
                    try {
                        firestore.collection("schools").document(schoolCode)
                            .collection("staff_logs").document(item.id.toString())
                            .set(
                                mapOf(
                                    "id" to item.id.toString(),
                                    "schoolCode" to item.schoolCode,
                                    "staffId" to item.staffId.toString(),
                                    "staffName" to item.staffName,
                                    "date" to item.date,
                                    "checkIn" to item.checkIn,
                                    "checkOut" to item.checkOut,
                                    "activityNote" to item.activityNote,
                                    "status" to item.status
                                )
                            )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // Announcements
        scope.launch {
            database.announcementDao().getAnnouncementsBySchool(schoolCode).collectLatest { list ->
                for (item in list) {
                    try {
                        firestore.collection("schools").document(schoolCode)
                            .collection("announcements").document(item.id.toString())
                            .set(
                                mapOf(
                                    "id" to item.id.toString(),
                                    "schoolCode" to item.schoolCode,
                                    "title" to item.title,
                                    "message" to item.message,
                                    "targetAudience" to item.targetAudience,
                                    "priority" to item.priority,
                                    "date" to item.date
                                )
                            )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // Leave Requests
        scope.launch {
            database.leaveDao().getLeavesBySchool(schoolCode).collectLatest { list ->
                for (item in list) {
                    try {
                        firestore.collection("schools").document(schoolCode)
                            .collection("leave_requests").document(item.id.toString())
                            .set(
                                mapOf(
                                    "id" to item.id.toString(),
                                    "schoolCode" to item.schoolCode,
                                    "applicantName" to item.applicantName,
                                    "applicantType" to item.applicantType,
                                    "startDate" to item.startDate,
                                    "endDate" to item.endDate,
                                    "reason" to item.reason,
                                    "status" to item.status
                                )
                            )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // Timetables
        scope.launch {
            database.timetableDao().getTimetableBySchool(schoolCode).collectLatest { list ->
                for (item in list) {
                    try {
                        firestore.collection("schools").document(schoolCode)
                            .collection("timetables").document(item.id.toString())
                            .set(
                                mapOf(
                                    "id" to item.id.toString(),
                                    "schoolCode" to item.schoolCode,
                                    "gradeClass" to item.gradeClass,
                                    "dayOfWeek" to item.dayOfWeek,
                                    "periodNo" to item.periodNo.toString(),
                                    "timeSlot" to item.timeSlot,
                                    "subject" to item.subject,
                                    "teacherName" to item.teacherName
                                )
                            )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // Online Classes
        scope.launch {
            database.onlineClassDao().getOnlineClassesBySchool(schoolCode).collectLatest { list ->
                for (item in list) {
                    try {
                        firestore.collection("schools").document(schoolCode)
                            .collection("online_classes").document(item.id.toString())
                            .set(
                                mapOf(
                                    "id" to item.id.toString(),
                                    "schoolCode" to item.schoolCode,
                                    "gradeClass" to item.gradeClass,
                                    "subject" to item.subject,
                                    "title" to item.title,
                                    "meetingUrl" to item.meetingUrl,
                                    "classDate" to item.classDate,
                                    "classTime" to item.classTime
                                )
                            )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // Question Bank
        scope.launch {
            database.questionBankDao().getQuestionsBySchool(schoolCode).collectLatest { list ->
                for (item in list) {
                    try {
                        firestore.collection("schools").document(schoolCode)
                            .collection("question_bank").document(item.id.toString())
                            .set(
                                mapOf(
                                    "id" to item.id.toString(),
                                    "schoolCode" to item.schoolCode,
                                    "gradeClass" to item.gradeClass,
                                    "subject" to item.subject,
                                    "chapterTopic" to item.chapterTopic,
                                    "questionText" to item.questionText,
                                    "answerKey" to item.answerKey,
                                    "questionType" to item.questionType
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