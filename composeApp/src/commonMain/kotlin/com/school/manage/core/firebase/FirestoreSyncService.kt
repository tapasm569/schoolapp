package com.school.manage.core.firebase

import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.DataSnapshot
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class FirestoreSyncService(private val database: AppDatabase) {

    private val rtdb = Firebase.database
    private val scope = CoroutineScope(Dispatchers.Default)
    private val syncJobs = mutableMapOf<String, Job>()

    private suspend fun resolveSchoolCode(code: String): String {
        if (code.isNotBlank()) return code
        return try {
            database.sessionDao().getActiveSession()?.schoolCode ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun pushAllLocalDataToCloud(schoolCode: String) {
        val sCode = resolveSchoolCode(schoolCode)
        if (sCode.isBlank()) return

        try {
            restoreAllFromCloud(sCode)

            database.batchDao().getBatchesBySchool(sCode).first().forEach { pushBatch(sCode, it) }
            database.studentDao().getStudentsBySchool(sCode).first().forEach { pushStudentRecord(sCode, it) }
            database.staffDao().getStaffBySchool(sCode).first().forEach { pushStaff(sCode, it) }
            database.feeDao().getFeeRecordsBySchool(sCode).first().forEach { pushFeeRecord(sCode, it) }
            database.expenseDao().getExpensesBySchool(sCode).first().forEach { pushExpense(sCode, it) }
            database.attendanceDao().getAttendanceBySchool(sCode).first().forEach { pushAttendance(sCode, it) }

            database.examDao().getExamsBySchool(sCode).first().forEach { pushExam(sCode, it) }
            database.homeworkDao().getHomeworkBySchool(sCode).first().forEach { pushHomework(sCode, it) }
            database.classworkDao().getClassworkBySchool(sCode).first().forEach { pushClasswork(sCode, it) }
            database.enquiryDao().getEnquiriesBySchool(sCode).first().forEach { pushEnquiry(sCode, it) }
            database.staffLogDao().getStaffLogsBySchool(sCode).first().forEach { pushStaffLog(sCode, it) }
            database.announcementDao().getAnnouncementsBySchool(sCode).first().forEach { pushAnnouncement(sCode, it) }
            database.leaveDao().getLeavesBySchool(sCode).first().forEach { pushLeaveRequest(sCode, it) }
            database.timetableDao().getTimetableBySchool(sCode).first().forEach { pushTimetable(sCode, it) }
            database.onlineClassDao().getOnlineClassesBySchool(sCode).first().forEach { pushOnlineClass(sCode, it) }
            database.questionBankDao().getQuestionsBySchool(sCode).first().forEach { pushQuestion(sCode, it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun restoreAllFromCloud(schoolCode: String) {
        val sCode = resolveSchoolCode(schoolCode)
        if (sCode.isBlank()) return
        try {
            val snapshot = rtdb.reference("schools").child(sCode).get()
            if (!snapshot.exists) return

            val infoMap = snapshot.child("info").value as? Map<*, *>
            if (infoMap != null) {
                val sc = infoMap["schoolCode"]?.toString() ?: sCode
                val sName = infoMap["schoolName"]?.toString() ?: ""
                val pass = infoMap["password"]?.toString() ?: ""
                val ph = infoMap["phone"]?.toString() ?: ""
                val em = infoMap["email"]?.toString() ?: ""
                val addr = infoMap["address"]?.toString() ?: ""
                val p64 = infoMap["photoBase64"]?.toString() ?: ""

                val existing = database.schoolDao().getSchoolByCode(sc)
                if (existing == null) {
                    database.schoolDao().registerSchool(
                        SchoolEntity(
                            schoolCode = sc,
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
                syncCollectionFromSnapshot(sCode, col, snapshot.child(col))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    suspend fun restoreSchoolFromCloud(schoolCode: String): SchoolEntity? {
        val sCode = resolveSchoolCode(schoolCode)
        if (sCode.isBlank()) return null
        return try {
            val snapshot = rtdb.reference("schools").child(sCode).child("info").get()
            if (!snapshot.exists) return null
            val map = snapshot.value as? Map<*, *> ?: return null
            val sc = map["schoolCode"]?.toString() ?: sCode
            val sName = map["schoolName"]?.toString() ?: ""
            val pass = map["password"]?.toString() ?: ""
            val ph = map["phone"]?.toString() ?: ""
            val em = map["email"]?.toString() ?: ""
            val addr = map["address"]?.toString() ?: ""
            val p64 = map["photoBase64"]?.toString() ?: ""

            val school = SchoolEntity(
                schoolCode = sc,
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
        val sCode = resolveSchoolCode(school.schoolCode)
        if (sCode.isBlank()) return
        try {
            rtdb.reference("schools").child(sCode).child("info").setValue(
                mapOf(
                    "schoolCode" to sCode,
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
        val sCode = resolveSchoolCode(schoolCode)
        if (sCode.isBlank()) return
        try {
            rtdb.reference("schools").child(sCode).child("profile").updateChildren(
                mapOf(
                    "schoolCode" to sCode,
                    "schoolName" to schoolName,
                    "masterSubjects" to masterSubjects
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun pushActiveSession(schoolCode: String, session: String) {
        val sCode = resolveSchoolCode(schoolCode)
        if (sCode.isBlank() || session.isBlank()) return
        try {
            rtdb.reference("schools").child(sCode).child("profile").child("activeSession").setValue(session)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getActiveSession(schoolCode: String): String? {
        val sCode = resolveSchoolCode(schoolCode)
        if (sCode.isBlank()) return null
        return try {
            val snapshot = rtdb.reference("schools").child(sCode).child("profile").child("activeSession").get()
            if (!snapshot.exists) return null
            snapshot.value?.toString()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteBatchRecord(schoolCode: String, batch: BatchEntity) {
        val targetSchool = resolveSchoolCode(schoolCode.ifBlank { batch.schoolCode })
        if (targetSchool.isNotBlank() && batch.id > 0L) {
            try {
                rtdb.reference("schools").child(targetSchool).child("batches").child(batch.id.toString()).removeValue()
            } catch (_: Exception) {}
        }
        database.batchDao().deleteBatch(batch)
    }

    suspend fun deleteStudentRecord(schoolCode: String, student: StudentEntity): Boolean {
        val targetSchool = resolveSchoolCode(schoolCode.ifBlank { student.schoolCode })
        if (targetSchool.isNotBlank() && student.id > 0L) {
            try {
                rtdb.reference("schools").child(targetSchool).child("students").child(student.id.toString()).removeValue()
            } catch (_: Exception) {}
        }
        database.studentDao().deleteStudent(student)
        return true
    }

    suspend fun deleteStaffRecord(schoolCode: String, staff: StaffEntity): Boolean {
        val targetSchool = resolveSchoolCode(schoolCode.ifBlank { staff.schoolCode })
        if (targetSchool.isNotBlank() && staff.id > 0L) {
            try {
                rtdb.reference("schools").child(targetSchool).child("staff").child(staff.id.toString()).removeValue()
            } catch (_: Exception) {}
        }
        database.staffDao().deleteStaff(staff)
        return true
    }

    suspend fun deleteModuleRecord(schoolCode: String, collection: String, id: Long) {
        val targetSchool = resolveSchoolCode(schoolCode)
        if (targetSchool.isBlank() || id <= 0L) return
        try {
            rtdb.reference("schools").child(targetSchool).child(collection).child(id.toString()).removeValue()
        } catch (_: Exception) {}
    }

    suspend fun pushBatch(schoolCode: String, batch: BatchEntity) {
        val targetSchool = resolveSchoolCode(schoolCode.ifBlank { batch.schoolCode })
        if (targetSchool.isBlank() || batch.id <= 0L || batch.batchName.isBlank()) return

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
        val targetSchool = resolveSchoolCode(schoolCode.ifBlank { student.schoolCode })
        if (targetSchool.isBlank() || student.id <= 0L) return

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
        scope.launch {
            try {
                val targetSchool = resolveSchoolCode(schoolCode.ifBlank { staff.schoolCode })
                if (targetSchool.isBlank() || staff.id <= 0L) return@launch
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
        val targetSchool = resolveSchoolCode(schoolCode.ifBlank { fee.schoolCode })
        if (targetSchool.isBlank() || fee.id <= 0L) return

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

    suspend fun pushExpense(schoolCode: String, exp: ExpenseEntity) {
        val targetSchool = resolveSchoolCode(schoolCode.ifBlank { exp.schoolCode })
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
        val targetSchool = resolveSchoolCode(schoolCode.ifBlank { att.schoolCode })
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

    suspend fun pushExam(schoolCode: String, exam: ExamEntity) {
        val targetSchool = resolveSchoolCode(schoolCode.ifBlank { exam.schoolCode })
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
        val targetSchool = resolveSchoolCode(schoolCode.ifBlank { hw.schoolCode })
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
        val targetSchool = resolveSchoolCode(schoolCode.ifBlank { cw.schoolCode })
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
        val targetSchool = resolveSchoolCode(schoolCode.ifBlank { enq.schoolCode })
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
        val targetSchool = resolveSchoolCode(schoolCode.ifBlank { log.schoolCode })
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
        val targetSchool = resolveSchoolCode(schoolCode.ifBlank { ann.schoolCode })
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
        val targetSchool = resolveSchoolCode(schoolCode.ifBlank { lr.schoolCode })
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
        val targetSchool = resolveSchoolCode(schoolCode.ifBlank { tt.schoolCode })
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
        val targetSchool = resolveSchoolCode(schoolCode.ifBlank { oc.schoolCode })
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
        val targetSchool = resolveSchoolCode(schoolCode.ifBlank { qb.schoolCode })
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
        val children = if (snapshot.exists) snapshot.children.toList() else emptyList()

        when (collection) {
            "batches" -> {
                val cloudIds = mutableSetOf<Long>()
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    val name = map["batchName"]?.toString() ?: ""
                    if (id <= 0L || name.isBlank()) continue
                    cloudIds.add(id)

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
                // Prune local batches that were removed from Firebase
                val localBatches = database.batchDao().getBatchesBySchool(schoolCode).first()
                for (b in localBatches) {
                    if (b.id > 0L && b.id !in cloudIds) {
                        database.batchDao().deleteBatch(b)
                    }
                }
            }

            "students" -> {
                val cloudIds = mutableSetOf<Long>()
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue
                    cloudIds.add(id)

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
                // Prune local students that were removed from Firebase
                val localStudents = database.studentDao().getStudentsBySchool(schoolCode).first()
                for (s in localStudents) {
                    if (s.id > 0L && s.id !in cloudIds) {
                        database.studentDao().deleteStudent(s)
                    }
                }
            }

            "staff" -> {
                val cloudIds = mutableSetOf<Long>()
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue
                    cloudIds.add(id)

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
                // Prune local staff that were removed from Firebase
                val localStaff = database.staffDao().getStaffBySchool(schoolCode).first()
                for (st in localStaff) {
                    if (st.id > 0L && st.id !in cloudIds) {
                        database.staffDao().deleteStaff(st)
                    }
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
                val cloudIds = mutableSetOf<Long>()
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue
                    cloudIds.add(id)
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
                val localList = database.expenseDao().getExpensesBySchool(schoolCode).first()
                for (item in localList) {
                    if (item.id > 0L && item.id !in cloudIds) database.expenseDao().deleteExpense(item)
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
                val cloudIds = mutableSetOf<Long>()
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue
                    cloudIds.add(id)
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
                val localList = database.examDao().getExamsBySchool(schoolCode).first()
                for (item in localList) {
                    if (item.id > 0L && item.id !in cloudIds) database.examDao().deleteExam(item)
                }
            }

            "homework" -> {
                val cloudIds = mutableSetOf<Long>()
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue
                    cloudIds.add(id)
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
                val localList = database.homeworkDao().getHomeworkBySchool(schoolCode).first()
                for (item in localList) {
                    if (item.id > 0L && item.id !in cloudIds) database.homeworkDao().deleteHomework(item)
                }
            }

            "classwork" -> {
                val cloudIds = mutableSetOf<Long>()
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue
                    cloudIds.add(id)
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
                val localList = database.classworkDao().getClassworkBySchool(schoolCode).first()
                for (item in localList) {
                    if (item.id > 0L && item.id !in cloudIds) database.classworkDao().deleteClasswork(item)
                }
            }

            "enquiries" -> {
                val cloudIds = mutableSetOf<Long>()
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue
                    cloudIds.add(id)
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
                val localList = database.enquiryDao().getEnquiriesBySchool(schoolCode).first()
                for (item in localList) {
                    if (item.id > 0L && item.id !in cloudIds) database.enquiryDao().deleteEnquiry(item)
                }
            }

            "staff_logs" -> {
                val cloudIds = mutableSetOf<Long>()
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue
                    cloudIds.add(id)
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
                val localList = database.staffLogDao().getStaffLogsBySchool(schoolCode).first()
                for (item in localList) {
                    if (item.id > 0L && item.id !in cloudIds) database.staffLogDao().deleteStaffLog(item)
                }
            }

            "announcements" -> {
                val cloudIds = mutableSetOf<Long>()
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue
                    cloudIds.add(id)
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
                val localList = database.announcementDao().getAnnouncementsBySchool(schoolCode).first()
                for (item in localList) {
                    if (item.id > 0L && item.id !in cloudIds) database.announcementDao().deleteAnnouncement(item)
                }
            }

            "leave_requests" -> {
                val cloudIds = mutableSetOf<Long>()
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue
                    cloudIds.add(id)
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
                val localList = database.leaveDao().getLeavesBySchool(schoolCode).first()
                for (item in localList) {
                    if (item.id > 0L && item.id !in cloudIds) database.leaveDao().deleteLeave(item)
                }
            }

            "timetables" -> {
                val cloudIds = mutableSetOf<Long>()
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue
                    cloudIds.add(id)
                    database.timetableDao().insertTimetable(
                        TimetableEntity(
                            id = id,
                            schoolCode = schoolCode,
                            gradeClass = map["gradeClass"]?.toString() ?: "",
                            dayOfWeek = map["dayOfWeek"]?.toString() ?: "Monday",
                            periodNo = map["periodNo"]?.toString()?.toIntOrNull() ?: 1,
                            timeSlot = map["timeSlot"]?.toString() ?: "",
                            subject = map["subject"]?.toString() ?: "",
                            teacherName = map["teacherName"]?.toString() ?: ""
                        )
                    )
                }
                val localList = database.timetableDao().getTimetableBySchool(schoolCode).first()
                for (item in localList) {
                    if (item.id > 0L && item.id !in cloudIds) database.timetableDao().deleteTimetable(item)
                }
            }

            "online_classes" -> {
                val cloudIds = mutableSetOf<Long>()
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue
                    cloudIds.add(id)
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
                val localList = database.onlineClassDao().getOnlineClassesBySchool(schoolCode).first()
                for (item in localList) {
                    if (item.id > 0L && item.id !in cloudIds) database.onlineClassDao().deleteOnlineClass(item)
                }
            }

            "question_bank" -> {
                val cloudIds = mutableSetOf<Long>()
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull() ?: child.key?.toLongOrNull() ?: continue
                    if (id <= 0L) continue
                    cloudIds.add(id)
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
                val localList = database.questionBankDao().getQuestionsBySchool(schoolCode).first()
                for (item in localList) {
                    if (item.id > 0L && item.id !in cloudIds) database.questionBankDao().deleteQuestion(item)
                }
            }
        }
    }

    fun startSync(schoolCode: String) {
        scope.launch {
            val sCode = resolveSchoolCode(schoolCode)
            if (sCode.isBlank()) return@launch
            if (syncJobs[sCode]?.isActive == true) return@launch

            pushAllLocalDataToCloud(sCode)

            syncJobs[sCode] = launch {
                val schoolRef = rtdb.reference("schools").child(sCode)
                val collections = listOf(
                    "batches", "students", "staff", "fees", "fee_records", "expenses", "attendance",
                    "exams", "homework", "classwork", "enquiries", "staff_logs",
                    "announcements", "leave_requests", "timetables", "online_classes", "question_bank"
                )

                for (collection in collections) {
                    launch {
                        try {
                            schoolRef.child(collection).valueEvents.collect { snapshot ->
                                try {
                                    syncCollectionFromSnapshot(sCode, collection, snapshot)
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
}
