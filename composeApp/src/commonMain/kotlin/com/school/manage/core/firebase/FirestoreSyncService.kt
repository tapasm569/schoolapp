package com.school.manage.core.firebase

import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.DataSnapshot
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class FirestoreSyncService(private val database: AppDatabase) {

    private val rtdb = Firebase.database
    private val scope = CoroutineScope(Dispatchers.Default)
    private val syncJobs = mutableMapOf<String, Job>()

    suspend fun pushAllLocalDataToCloud(schoolCode: String) {
        restoreAllFromCloud(schoolCode)
    }

    suspend fun restoreAllFromCloud(schoolCode: String) {
        if (schoolCode.isBlank()) return
        try {
            val snapshot = rtdb.reference("schools").child(schoolCode).get()
            if (!snapshot.exists) return

            val infoMap = snapshot.child("info").value as? Map<*, *>
            if (infoMap != null) {
                val sCode = infoMap["schoolCode"]?.toString() ?: schoolCode
                val sName = infoMap["schoolName"]?.toString() ?: ""
                val pass = infoMap["password"]?.toString() ?: ""
                val ph = infoMap["phone"]?.toString() ?: ""
                val em = infoMap["email"]?.toString() ?: ""
                val addr = infoMap["address"]?.toString() ?: ""
                val p64 = infoMap["photoBase64"]?.toString() ?: ""

                val existing = database.schoolDao().getSchoolByCode(sCode)
                if (existing == null) {
                    database.schoolDao().registerSchool(
                        SchoolEntity(
                            schoolCode = sCode,
                            schoolName = sName,
                            password = pass,
                            phone = ph,
                            email = em,
                            address = addr,
                            photoBase64 = p64
                        )
                    )
                }
            }

            val allCollections = listOf(
                "batches", "students", "staff", "fees", "fee_records", "expenses", "attendance",
                "exams", "homework", "classwork", "enquiries", "staff_logs",
                "announcements", "leave_requests", "timetables", "online_classes", "question_bank"
            )
            for (col in allCollections) {
                syncCollectionFromSnapshot(schoolCode, col, snapshot.child(col))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun restoreSchoolFromCloud(schoolCode: String): SchoolEntity? {
        if (schoolCode.isBlank()) return null
        return try {
            val snapshot = rtdb.reference("schools").child(schoolCode).child("info").get()
            if (!snapshot.exists) return null
            val map = snapshot.value as? Map<*, *> ?: return null
            val sCode = map["schoolCode"]?.toString() ?: schoolCode
            val sName = map["schoolName"]?.toString() ?: ""
            val pass = map["password"]?.toString() ?: ""
            val ph = map["phone"]?.toString() ?: ""
            val em = map["email"]?.toString() ?: ""
            val addr = map["address"]?.toString() ?: ""
            val p64 = map["photoBase64"]?.toString() ?: ""

            val school = SchoolEntity(
                schoolCode = sCode,
                schoolName = sName,
                password = pass,
                phone = ph,
                email = em,
                address = addr,
                photoBase64 = p64
            )
            database.schoolDao().registerSchool(school)
            school
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveSchoolToCloud(school: SchoolEntity) {
        if (school.schoolCode.isBlank()) return
        try {
            rtdb.reference("schools").child(school.schoolCode).child("info").setValue(
                mapOf(
                    "schoolCode" to school.schoolCode,
                    "schoolName" to school.schoolName,
                    "password" to school.password,
                    "phone" to school.phone,
                    "email" to school.email,
                    "address" to school.address,
                    "photoBase64" to school.photoBase64
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun pushSchoolProfile(schoolCode: String, schoolName: String, masterSubjects: String) {
        if (schoolCode.isBlank()) return
        try {
            rtdb.reference("schools").child(schoolCode).child("profile").updateChildren(
                mapOf(
                    "schoolCode" to schoolCode,
                    "schoolName" to schoolName,
                    "masterSubjects" to masterSubjects
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun pushActiveSession(schoolCode: String, session: String) {
        if (schoolCode.isBlank() || session.isBlank()) return
        try {
            rtdb.reference("schools").child(schoolCode).child("profile").child("activeSession").setValue(session)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getActiveSession(schoolCode: String): String? {
        if (schoolCode.isBlank()) return null
        return try {
            val snapshot = rtdb.reference("schools").child(schoolCode).child("profile").child("activeSession").get()
            if (!snapshot.exists) return null
            snapshot.value?.toString()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun pushBatch(schoolCode: String, batch: BatchEntity) {
        val targetSchool = schoolCode.ifBlank { batch.schoolCode }
        if (targetSchool.isBlank() || batch.id <= 0L || batch.batchName.isBlank()) {
            throw IllegalArgumentException("Invalid school code or class details")
        }

        rtdb.reference("schools")
            .child(targetSchool)
            .child("batches")
            .child(batch.id.toString())
            .setValue(
                mapOf(
                    "id" to batch.id.toString(),
                    "schoolCode" to targetSchool,
                    "batchName" to batch.batchName,
                    "subjects" to batch.subjects,
                    "sections" to batch.sections,
                    "feeTemplateRaw" to batch.feeTemplateRaw,
                    "defaultMonthlyTuition" to batch.defaultMonthlyTuition.toString(),
                    "collectTuitionAtAdmission" to batch.collectTuitionAtAdmission.toString()
                )
            )
    }

    fun pushStudent(schoolCode: String, student: StudentEntity) {
        scope.launch {
            try {
                pushStudentRecord(schoolCode, student)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun pushStudentRecord(schoolCode: String, student: StudentEntity) {
        val targetSchool = schoolCode.ifBlank { student.schoolCode }
        require(targetSchool.isNotBlank()) { "School code is required" }
        require(student.id > 0L) { "Student ID must be positive" }

        rtdb.reference("schools")
            .child(targetSchool)
            .child("students")
            .child(student.id.toString())
            .setValue(
                mapOf(
                    "id" to student.id.toString(),
                    "schoolCode" to targetSchool,
                    "rollNo" to student.rollNo,
                    "name" to student.name,
                    "gradeClass" to student.gradeClass,
                    "section" to student.section,
                    "guardianName" to student.guardianName,
                    "fatherName" to student.fatherName,
                    "motherName" to student.motherName,
                    "phone" to student.phone,
                    "whatsapp" to student.whatsapp,
                    "address" to student.address,
                    "dob" to student.dob,
                    "gender" to student.gender,
                    "caste" to student.caste,
                    "admissionDate" to student.admissionDate,
                    "monthlyFee" to student.monthlyFee.toString(),
                    "admissionFee" to student.admissionFee.toString(),
                    "aadharNumber" to student.aadharNumber,
                    "photoBase64" to student.photoBase64
                )
            )
    }

    fun pushStaff(schoolCode: String, staff: StaffEntity) {
        val targetSchool = schoolCode.ifBlank { staff.schoolCode }
        if (targetSchool.isBlank()) return
        scope.launch {
            try {
                rtdb.reference("schools")
                    .child(targetSchool)
                    .child("staff")
                    .child(staff.id.toString())
                    .setValue(
                        mapOf(
                            "id" to staff.id.toString(),
                            "schoolCode" to targetSchool,
                            "name" to staff.name,
                            "role" to staff.role,
                            "phone" to staff.phone,
                            "whatsapp" to staff.whatsapp,
                            "salary" to staff.salary.toString(),
                            "salaryType" to staff.salaryType,
                            "password" to staff.password,
                            "qualification" to staff.qualification,
                            "joinDate" to staff.joinDate,
                            "gender" to staff.gender,
                            "address" to staff.address,
                            "photoBase64" to staff.photoBase64
                        )
                    )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun pushFeeRecord(schoolCode: String, fee: FeeRecordEntity) {
        val targetSchool = schoolCode.ifBlank { fee.schoolCode }
        require(targetSchool.isNotBlank()) { "School code is required" }
        require(fee.id > 0L) { "Fee record ID must be positive" }

        val payload = mapOf(
            "id" to fee.id.toString(),
            "schoolCode" to targetSchool,
            "studentId" to fee.studentId.toString(),
            "studentName" to fee.studentName,
            "gradeClass" to fee.gradeClass,
            "amountPaid" to fee.amountPaid.toString(),
            "paymentDate" to fee.paymentDate,
            "feeMonth" to fee.feeMonth,
            "paymentMode" to fee.paymentMode,
            "remarks" to fee.remarks
        )
        rtdb.reference("schools").child(targetSchool).child("fees").child(fee.id.toString()).setValue(payload)
        rtdb.reference("schools").child(targetSchool).child("fee_records").child(fee.id.toString()).setValue(payload)
    }

    suspend fun deleteBatchRecord(schoolCode: String, batch: BatchEntity) {
        val targetSchool = schoolCode.ifBlank { batch.schoolCode }
        require(targetSchool.isNotBlank()) { "School code is required" }
        require(batch.id > 0L) { "Class ID must be positive" }

        rtdb.reference("schools").child(targetSchool).child("batches").child(batch.id.toString()).removeValue()
        database.batchDao().deleteBatch(batch)
    }

    suspend fun deleteStudentRecord(schoolCode: String, student: StudentEntity): Boolean {
        val targetSchool = schoolCode.ifBlank { student.schoolCode }
        require(targetSchool.isNotBlank()) { "School code is required" }
        require(student.id > 0L) { "Student ID must be positive" }

        rtdb.reference("schools").child(targetSchool).child("students").child(student.id.toString()).removeValue()
        database.studentDao().deleteStudent(student)
        return true
    }

    suspend fun deleteStaffRecord(schoolCode: String, staff: StaffEntity): Boolean {
        val targetSchool = schoolCode.ifBlank { staff.schoolCode }
        require(targetSchool.isNotBlank()) { "School code is required" }
        require(staff.id > 0L) { "Staff ID must be positive" }

        rtdb.reference("schools").child(targetSchool).child("staff").child(staff.id.toString()).removeValue()
        database.staffDao().deleteStaff(staff)
        return true
    }

    suspend fun deleteModuleRecord(schoolCode: String, collection: String, id: Long) {
        if (schoolCode.isBlank() || id <= 0L) return
        try {
            rtdb.reference("schools").child(schoolCode).child(collection).child(id.toString()).removeValue()
        } catch (_: Exception) {}
    }

    suspend fun pushExpense(schoolCode: String, exp: ExpenseEntity) {
        val targetSchool = schoolCode.ifBlank { exp.schoolCode }
        if (targetSchool.isBlank() || exp.id <= 0L) return
        rtdb.reference("schools").child(targetSchool).child("expenses").child(exp.id.toString()).setValue(
            mapOf(
                "id" to exp.id.toString(), "schoolCode" to targetSchool, "title" to exp.title,
                "category" to exp.category, "amount" to exp.amount.toString(),
                "date" to exp.date, "notes" to exp.notes
            )
        )
    }

    suspend fun pushAttendance(schoolCode: String, att: AttendanceEntity) {
        val targetSchool = schoolCode.ifBlank { att.schoolCode }
        if (targetSchool.isBlank() || att.id <= 0L) return
        rtdb.reference("schools").child(targetSchool).child("attendance").child(att.id.toString()).setValue(
            mapOf(
                "id" to att.id.toString(), "schoolCode" to targetSchool,
                "studentId" to att.studentId.toString(), "studentName" to att.studentName,
                "gradeClass" to att.gradeClass, "staffId" to att.staffId.toString(),
                "userType" to att.userType, "date" to att.date, "status" to att.status
            )
        )
    }

    // --- 12 MODULE PUSH FUNCTIONS ---
    suspend fun pushExam(schoolCode: String, exam: ExamEntity) {
        val targetSchool = schoolCode.ifBlank { exam.schoolCode }
        if (targetSchool.isBlank() || exam.id <= 0L) return
        rtdb.reference("schools").child(targetSchool).child("exams").child(exam.id.toString()).setValue(
            mapOf(
                "id" to exam.id.toString(), "schoolCode" to targetSchool, "title" to exam.title,
                "gradeClass" to exam.gradeClass, "subject" to exam.subject,
                "examDate" to exam.examDate, "maxMarks" to exam.maxMarks.toString()
            )
        )
    }

    suspend fun pushHomework(schoolCode: String, hw: HomeworkEntity) {
        val targetSchool = schoolCode.ifBlank { hw.schoolCode }
        if (targetSchool.isBlank() || hw.id <= 0L) return
        rtdb.reference("schools").child(targetSchool).child("homework").child(hw.id.toString()).setValue(
            mapOf(
                "id" to hw.id.toString(), "schoolCode" to targetSchool, "title" to hw.title,
                "description" to hw.description, "gradeClass" to hw.gradeClass,
                "subject" to hw.subject, "assignedDate" to hw.assignedDate, "dueDate" to hw.dueDate
            )
        )
    }

    suspend fun pushClasswork(schoolCode: String, cw: ClassworkEntity) {
        val targetSchool = schoolCode.ifBlank { cw.schoolCode }
        if (targetSchool.isBlank() || cw.id <= 0L) return
        rtdb.reference("schools").child(targetSchool).child("classwork").child(cw.id.toString()).setValue(
            mapOf(
                "id" to cw.id.toString(), "schoolCode" to targetSchool, "topicTitle" to cw.topicTitle,
                "summary" to cw.summary, "gradeClass" to cw.gradeClass,
                "subject" to cw.subject, "date" to cw.date
            )
        )
    }

    suspend fun pushEnquiry(schoolCode: String, enq: EnquiryEntity) {
        val targetSchool = schoolCode.ifBlank { enq.schoolCode }
        if (targetSchool.isBlank() || enq.id <= 0L) return
        rtdb.reference("schools").child(targetSchool).child("enquiries").child(enq.id.toString()).setValue(
            mapOf(
                "id" to enq.id.toString(), "schoolCode" to targetSchool, "studentName" to enq.studentName,
                "parentName" to enq.parentName, "phone" to enq.phone, "gradeClass" to enq.gradeClass,
                "date" to enq.date, "status" to enq.status, "notes" to enq.notes
            )
        )
    }

    suspend fun pushStaffLog(schoolCode: String, log: StaffLogEntity) {
        val targetSchool = schoolCode.ifBlank { log.schoolCode }
        if (targetSchool.isBlank() || log.id <= 0L) return
        rtdb.reference("schools").child(targetSchool).child("staff_logs").child(log.id.toString()).setValue(
            mapOf(
                "id" to log.id.toString(), "schoolCode" to targetSchool, "staffName" to log.staffName,
                "staffId" to log.staffId.toString(), "date" to log.date, "checkIn" to log.checkIn,
                "checkOut" to log.checkOut, "activityNote" to log.activityNote, "status" to log.status
            )
        )
    }

    suspend fun pushAnnouncement(schoolCode: String, ann: AnnouncementEntity) {
        val targetSchool = schoolCode.ifBlank { ann.schoolCode }
        if (targetSchool.isBlank() || ann.id <= 0L) return
        rtdb.reference("schools").child(targetSchool).child("announcements").child(ann.id.toString()).setValue(
            mapOf(
                "id" to ann.id.toString(), "schoolCode" to targetSchool, "title" to ann.title,
                "message" to ann.message, "targetAudience" to ann.targetAudience,
                "date" to ann.date, "priority" to ann.priority
            )
        )
    }

    suspend fun pushLeaveRequest(schoolCode: String, lr: LeaveRequestEntity) {
        val targetSchool = schoolCode.ifBlank { lr.schoolCode }
        if (targetSchool.isBlank() || lr.id <= 0L) return
        rtdb.reference("schools").child(targetSchool).child("leave_requests").child(lr.id.toString()).setValue(
            mapOf(
                "id" to lr.id.toString(), "schoolCode" to targetSchool, "applicantName" to lr.applicantName,
                "applicantType" to lr.applicantType, "startDate" to lr.startDate,
                "endDate" to lr.endDate, "reason" to lr.reason, "status" to lr.status
            )
        )
    }

    suspend fun pushTimetable(schoolCode: String, tt: TimetableEntity) {
        val targetSchool = schoolCode.ifBlank { tt.schoolCode }
        if (targetSchool.isBlank() || tt.id <= 0L) return
        rtdb.reference("schools").child(targetSchool).child("timetables").child(tt.id.toString()).setValue(
            mapOf(
                "id" to tt.id.toString(), "schoolCode" to targetSchool, "gradeClass" to tt.gradeClass,
                "dayOfWeek" to tt.dayOfWeek, "periodNo" to tt.periodNo.toString(),
                "timeSlot" to tt.timeSlot, "subject" to tt.subject, "teacherName" to tt.teacherName
            )
        )
    }

    suspend fun pushOnlineClass(schoolCode: String, oc: OnlineClassEntity) {
        val targetSchool = schoolCode.ifBlank { oc.schoolCode }
        if (targetSchool.isBlank() || oc.id <= 0L) return
        rtdb.reference("schools").child(targetSchool).child("online_classes").child(oc.id.toString()).setValue(
            mapOf(
                "id" to oc.id.toString(), "schoolCode" to targetSchool, "title" to oc.title,
                "gradeClass" to oc.gradeClass, "subject" to oc.subject,
                "meetingUrl" to oc.meetingUrl, "classDate" to oc.classDate, "classTime" to oc.classTime
   )
        )
    }

    suspend fun pushQuestion(schoolCode: String, qb: QuestionBankEntity) {
        val targetSchool = schoolCode.ifBlank { qb.schoolCode }
        if (targetSchool.isBlank() || qb.id <= 0L) return
        rtdb.reference("schools").child(targetSchool).child("question_bank").child(qb.id.toString()).setValue(
            mapOf(
                "id" to qb.id.toString(), "schoolCode" to targetSchool, "gradeClass" to qb.gradeClass,
                "subject" to qb.subject, "chapterTopic" to qb.chapterTopic,
                "questionText" to qb.questionText, "answerKey" to qb.answerKey, "questionType" to qb.questionType
            )
        )
    }

    private suspend fun syncCollectionFromSnapshot(
        schoolCode: String,
        collection: String,
        snapshot: DataSnapshot
    ) {
        if (!snapshot.exists) return

        val children = snapshot.children.toList()
        if (children.isEmpty()) return

        when (collection) {
            "batches" -> {
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    val name = map["batchName"]?.toString() ?: ""
                    if (id <= 0L || name.isBlank()) continue

                    database.batchDao().insertBatch(
                        BatchEntity(
                            id = id,
                            schoolCode = schoolCode,
                            batchName = name,
                            subjects = map["subjects"]?.toString() ?: "",
                            sections = map["sections"]?.toString() ?: "",
                            feeTemplateRaw = map["feeTemplateRaw"]?.toString() ?: "",
                            defaultMonthlyTuition = map["defaultMonthlyTuition"]?.toString()?.toDoubleOrNull() ?: 0.0,
                            collectTuitionAtAdmission = map["collectTuitionAtAdmission"]?.toString()?.toBooleanStrictOrNull() ?: true
                        )
                    )
                }
            }

            "students" -> {
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue

                    database.studentDao().insertStudent(
                        StudentEntity(
                            id = id,
                            schoolCode = map["schoolCode"]?.toString() ?: schoolCode,
                            rollNo = map["rollNo"]?.toString() ?: "",
                            name = map["name"]?.toString() ?: "",
                            gradeClass = map["gradeClass"]?.toString() ?: "",
                            section = map["section"]?.toString() ?: "",
                            guardianName = map["guardianName"]?.toString() ?: "",
                            fatherName = map["fatherName"]?.toString() ?: "",
                            motherName = map["motherName"]?.toString() ?: "",
                            phone = map["phone"]?.toString() ?: "",
                            monthlyFee = map["monthlyFee"]?.toString()?.toDoubleOrNull() ?: 0.0,
                            admissionFee = map["admissionFee"]?.toString()?.toDoubleOrNull() ?: 0.0,
                            aadharNumber = map["aadharNumber"]?.toString() ?: "",
                            whatsapp = map["whatsapp"]?.toString() ?: "",
                            address = map["address"]?.toString() ?: "",
                            dob = map["dob"]?.toString() ?: "",
                            gender = map["gender"]?.toString() ?: "",
                            caste = map["caste"]?.toString() ?: "",
                            admissionDate = map["admissionDate"]?.toString() ?: "",
                            photoBase64 = map["photoBase64"]?.toString() ?: ""
                        )
                    )
                }
            }

            "staff" -> {
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue

                    database.staffDao().insertStaff(
                        StaffEntity(
                            id = id,
                            schoolCode = map["schoolCode"]?.toString() ?: schoolCode,
                            name = map["name"]?.toString() ?: "",
                            role = map["role"]?.toString() ?: "",
                            phone = map["phone"]?.toString() ?: "",
                            whatsapp = map["whatsapp"]?.toString() ?: "",
                            salary = map["salary"]?.toString()?.toDoubleOrNull() ?: 0.0,
                            salaryType = map["salaryType"]?.toString() ?: "Monthly",
                            password = map["password"]?.toString() ?: "",
                            qualification = map["qualification"]?.toString() ?: "",
                            joinDate = map["joinDate"]?.toString() ?: "",
                            gender = map["gender"]?.toString() ?: "",
                            address = map["address"]?.toString() ?: "",
                            photoBase64 = map["photoBase64"]?.toString() ?: ""
                        )
                    )
                }
            }

            "fees", "fee_records" -> {
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue

                    database.feeDao().insertFeeRecord(
                        FeeRecordEntity(
                            id = id,
                            schoolCode = map["schoolCode"]?.toString() ?: schoolCode,
                            studentId = map["studentId"]?.toString()?.toLongOrNull() ?: 0L,
                            studentName = map["studentName"]?.toString() ?: "",
                            gradeClass = map["gradeClass"]?.toString() ?: "",
                            amountPaid = map["amountPaid"]?.toString()?.toDoubleOrNull() ?: 0.0,
                            paymentDate = map["paymentDate"]?.toString() ?: "",
                            feeMonth = map["feeMonth"]?.toString() ?: "",
                            paymentMode = map["paymentMode"]?.toString() ?: "",
                            remarks = map["remarks"]?.toString() ?: ""
                        )
                    )
                }
            }

            "expenses" -> {
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue
                    database.expenseDao().insertExpense(
                        ExpenseEntity(
                            id = id,
                            schoolCode = map["schoolCode"]?.toString() ?: schoolCode,
                            title = map["title"]?.toString() ?: "",
                            category = map["category"]?.toString() ?: "General",
                            amount = map["amount"]?.toString()?.toDoubleOrNull() ?: 0.0,
                            date = map["date"]?.toString() ?: "",
                            notes = map["notes"]?.toString() ?: ""
                        )
                    )
                }
            }

            "attendance" -> {
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue
                    database.attendanceDao().markAttendance(
                        AttendanceEntity(
                            id = id,
                            schoolCode = map["schoolCode"]?.toString() ?: schoolCode,
                            studentId = map["studentId"]?.toString()?.toLongOrNull() ?: 0L,
                            studentName = map["studentName"]?.toString() ?: "",
                            gradeClass = map["gradeClass"]?.toString() ?: "",
                            staffId = map["staffId"]?.toString()?.toLongOrNull() ?: 0L,
                            userType = map["userType"]?.toString() ?: "STUDENT",
                            date = map["date"]?.toString() ?: "",
                            status = map["status"]?.toString() ?: "PRESENT"
                        )
                    )
                }
            }

            "exams" -> {
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue
                    database.examDao().insertExam(
                        ExamEntity(
                            id = id,
                            schoolCode = map["schoolCode"]?.toString() ?: schoolCode,
                            title = map["title"]?.toString() ?: "",
                            gradeClass = map["gradeClass"]?.toString() ?: "",
                            subject = map["subject"]?.toString() ?: "",
                            examDate = map["examDate"]?.toString() ?: "",
                            maxMarks = map["maxMarks"]?.toString()?.toDoubleOrNull() ?: 100.0
                        )
                    )
                }
            }

            "homework" -> {
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue
                    database.homeworkDao().insertHomework(
                        HomeworkEntity(
                            id = id,
                            schoolCode = map["schoolCode"]?.toString() ?: schoolCode,
                            gradeClass = map["gradeClass"]?.toString() ?: "",
                            subject = map["subject"]?.toString() ?: "",
                            title = map["title"]?.toString() ?: "",
                            description = map["description"]?.toString() ?: "",
                            assignedDate = map["assignedDate"]?.toString() ?: "",
                            dueDate = map["dueDate"]?.toString() ?: ""
                        )
                    )
                }
            }

            "classwork" -> {
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue
                    database.classworkDao().insertClasswork(
                        ClassworkEntity(
                            id = id,
                            schoolCode = map["schoolCode"]?.toString() ?: schoolCode,
                            gradeClass = map["gradeClass"]?.toString() ?: "",
                            subject = map["subject"]?.toString() ?: "",
                            topicTitle = map["topicTitle"]?.toString() ?: "",
                            summary = map["summary"]?.toString() ?: "",
                            date = map["date"]?.toString() ?: ""
                        )
                    )
                }
            }

            "enquiries" -> {
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue
                    database.enquiryDao().insertEnquiry(
                        EnquiryEntity(
                            id = id,
                            schoolCode = map["schoolCode"]?.toString() ?: schoolCode,
                            studentName = map["studentName"]?.toString() ?: "",
                            parentName = map["parentName"]?.toString() ?: "",
                            phone = map["phone"]?.toString() ?: "",
                            gradeClass = map["gradeClass"]?.toString() ?: "",
                            date = map["date"]?.toString() ?: "",
                            status = map["status"]?.toString() ?: "NEW",
                            notes = map["notes"]?.toString() ?: ""
                        )
                    )
                }
            }

            "staff_logs" -> {
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue
                    database.staffLogDao().insertStaffLog(
                        StaffLogEntity(
                            id = id,
                            schoolCode = map["schoolCode"]?.toString() ?: schoolCode,
                            staffId = map["staffId"]?.toString()?.toLongOrNull() ?: 0L,
                            staffName = map["staffName"]?.toString() ?: "",
                            date = map["date"]?.toString() ?: "",
                            checkIn = map["checkIn"]?.toString() ?: "",
                            checkOut = map["checkOut"]?.toString() ?: "",
                            activityNote = map["activityNote"]?.toString() ?: "",
                            status = map["status"]?.toString() ?: "ON_TIME"
                        )
                    )
                }
            }

            "announcements" -> {
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue
                    database.announcementDao().insertAnnouncement(
                        AnnouncementEntity(
                            id = id,
                            schoolCode = map["schoolCode"]?.toString() ?: schoolCode,
                            title = map["title"]?.toString() ?: "",
                            message = map["message"]?.toString() ?: "",
                            targetAudience = map["targetAudience"]?.toString() ?: "ALL",
                            priority = map["priority"]?.toString() ?: "NORMAL",
                            date = map["date"]?.toString() ?: ""
                        )
                    )
                }
            }

            "leave_requests" -> {
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue
                    database.leaveDao().insertLeave(
                        LeaveRequestEntity(
                            id = id,
                            schoolCode = map["schoolCode"]?.toString() ?: schoolCode,
                            applicantName = map["applicantName"]?.toString() ?: "",
                            applicantType = map["applicantType"]?.toString() ?: "STUDENT",
                            startDate = map["startDate"]?.toString() ?: "",
                            endDate = map["endDate"]?.toString() ?: "",
                            reason = map["reason"]?.toString() ?: "",
                            status = map["status"]?.toString() ?: "PENDING"
                        )
                    )
                }
            }

            "timetables" -> {
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue
                    database.timetableDao().insertTimetable(
                        TimetableEntity(
                            id = id,
                            schoolCode = map["schoolCode"]?.toString() ?: schoolCode,
                            gradeClass = map["gradeClass"]?.toString() ?: "",
                            dayOfWeek = map["dayOfWeek"]?.toString() ?: "Monday",
                            periodNo = map["periodNo"]?.toString()?.toIntOrNull() ?: 1,
                            timeSlot = map["timeSlot"]?.toString() ?: "",
                            subject = map["subject"]?.toString() ?: "",
                            teacherName = map["teacherName"]?.toString() ?: ""
                        )
                    )
                }
            }

            "online_classes" -> {
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue
                    database.onlineClassDao().insertOnlineClass(
                        OnlineClassEntity(
                            id = id,
                            schoolCode = map["schoolCode"]?.toString() ?: schoolCode,
                            gradeClass = map["gradeClass"]?.toString() ?: "",
                            subject = map["subject"]?.toString() ?: "",
                            title = map["title"]?.toString() ?: "",
                            meetingUrl = map["meetingUrl"]?.toString() ?: "",
                            classDate = map["classDate"]?.toString() ?: "",
                            classTime = map["classTime"]?.toString() ?: ""
                        )
                    )
                }
            }

            "question_bank" -> {
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue
                    database.questionBankDao().insertQuestion(
                        QuestionBankEntity(
                            id = id,
                            schoolCode = map["schoolCode"]?.toString() ?: schoolCode,
                            gradeClass = map["gradeClass"]?.toString() ?: "",
                            subject = map["subject"]?.toString() ?: "",
                            chapterTopic = map["chapterTopic"]?.toString() ?: "",
                            questionText = map["questionText"]?.toString() ?: "",
                            answerKey = map["answerKey"]?.toString() ?: "",
                            questionType = map["questionType"]?.toString() ?: "SHORT"
                        )
                    )
                }
            }
        }
    }

    fun startSync(schoolCode: String) {
        if (schoolCode.isBlank()) return
        if (syncJobs[schoolCode]?.isActive == true) return

        syncJobs[schoolCode] = scope.launch {
            val schoolRef = rtdb.reference("schools").child(schoolCode)
            val collections = listOf(
                "batches", "students", "staff", "fees", "fee_records", "expenses", "attendance",
                "exams", "homework", "classwork", "enquiries", "staff_logs",
                "announcements", "leave_requests", "timetables", "online_classes", "question_bank")

            for (collection in collections) {
                launch {
                    try {
                        schoolRef.child(collection).valueEvents.collect { snapshot ->
                            try {
                                syncCollectionFromSnapshot(
                                    schoolCode,
                                    collection,
                                    snapshot
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
