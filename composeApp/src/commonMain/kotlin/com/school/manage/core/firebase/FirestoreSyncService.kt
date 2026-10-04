package com.school.manage.core.firebase

import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class FirestoreSyncService(private val database: AppDatabase) {

    private val firestore = Firebase.firestore
    private val scope = CoroutineScope(Dispatchers.IO)

    suspend fun saveSchoolToCloud(school: SchoolEntity): Boolean {
        return try {
            withTimeout(10000L) {
                firestore.collection("schools")
                    .document(school.schoolCode)
                    .set(
                        mapOf(
                            "id" to school.id,
                            "schoolCode" to school.schoolCode,
                            "schoolName" to school.schoolName,
                            "password" to school.password,
                            "phone" to school.phone
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

    fun syncStudent(schoolCode: String, student: StudentEntity) {
        scope.launch {
            try {
                firestore.collection("schools").document(schoolCode)
                    .collection("students").document(student.id.toString())
                    .set(
                        mapOf(
                            "id" to student.id.toString(),
                            "schoolCode" to student.schoolCode.toString(),
                            "rollNo" to student.rollNo.toString(),
                            "name" to student.name.toString(),
                            "gradeClass" to student.gradeClass.toString(),
                            "section" to student.section.toString(),
                            "guardianName" to student.guardianName.toString(),
                            "phone" to student.phone.toString(),
                            "monthlyFee" to student.monthlyFee.toString(),
                            "admissionDate" to student.admissionDate.toString(),
                            "fatherName" to student.fatherName.toString(),
                            "motherName" to student.motherName.toString(),
                            "dob" to student.dob.toString(),
                            "aadharNumber" to student.aadharNumber.toString(),
                            "caste" to student.caste.toString(),
                            "gender" to student.gender.toString(),
                            "whatsapp" to student.whatsapp.toString(),
                            "address" to student.address.toString(),
                            "admissionFee" to student.admissionFee.toString()
                        )
                    )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun syncStaff(schoolCode: String, staff: StaffEntity) {
        scope.launch {
            try {
                firestore.collection("schools").document(schoolCode)
                    .collection("staff").document(staff.id.toString())
                    .set(
                        mapOf(
                            "id" to staff.id.toString(),
                            "schoolCode" to staff.schoolCode.toString(),
                            "name" to staff.name.toString(),
                            "role" to staff.role.toString(),
                            "phone" to staff.phone.toString(),
                            "salary" to staff.salary.toString(),
                            "joinDate" to staff.joinDate.toString(),
                            "gender" to staff.gender.toString(),
                            "whatsapp" to staff.whatsapp.toString(),
                            "address" to staff.address.toString(),
                            "qualification" to staff.qualification.toString(),
                            "salaryType" to staff.salaryType.toString(),
                            "password" to staff.password.toString()
                        )
                    )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun syncFeeRecord(schoolCode: String, fee: FeeRecordEntity) {
        scope.launch {
            try {
                firestore.collection("schools").document(schoolCode)
                    .collection("fees").document(fee.id.toString())
                    .set(
                        mapOf(
                            "id" to fee.id.toString(),
                            "schoolCode" to fee.schoolCode.toString(),
                            "studentId" to fee.studentId.toString(),
                            "studentName" to fee.studentName.toString(),
                            "gradeClass" to fee.gradeClass.toString(),
                            "amountPaid" to fee.amountPaid.toString(),
                            "paymentDate" to fee.paymentDate.toString(),
                            "feeMonth" to fee.feeMonth.toString(),
                            "paymentMode" to fee.paymentMode.toString(),
                            "remarks" to fee.remarks.toString()
                        )
                    )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun syncAttendance(schoolCode: String, att: AttendanceEntity) {
        scope.launch {
            try {
                firestore.collection("schools").document(schoolCode)
                    .collection("attendance").document(att.id.toString())
                    .set(
                        mapOf(
                            "id" to att.id.toString(),
                            "schoolCode" to att.schoolCode.toString(),
                            "studentId" to att.studentId.toString(),
                            "studentName" to att.studentName.toString(),
                            "gradeClass" to att.gradeClass.toString(),
                            "staffId" to att.staffId.toString(),
                            "userType" to att.userType.toString(),
                            "date" to att.date.toString(),
                            "status" to att.status.toString()
                        )
                    )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun syncExam(schoolCode: String, exam: ExamEntity) {
        scope.launch {
            try {
                firestore.collection("schools").document(schoolCode)
                    .collection("exams").document(exam.id.toString())
                    .set(
                        mapOf(
                            "id" to exam.id.toString(),
                            "schoolCode" to exam.schoolCode.toString(),
                            "title" to exam.title.toString(),
                            "gradeClass" to exam.gradeClass.toString(),
                            "subject" to exam.subject.toString(),
                            "examDate" to exam.examDate.toString(),
                            "maxMarks" to exam.maxMarks.toString(),
                            "examId" to exam.examId.toString(),
                            "studentId" to exam.studentId.toString(),
                            "studentName" to exam.studentName.toString(),
                            "marksObtained" to exam.marksObtained.toString(),
                            "grade" to exam.grade.toString()
                        )
                    )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun syncHomework(schoolCode: String, hw: HomeworkEntity) {
        scope.launch {
            try {
                firestore.collection("schools").document(schoolCode)
                    .collection("homework").document(hw.id.toString())
                    .set(
                        mapOf(
                            "id" to hw.id.toString(),
                            "schoolCode" to hw.schoolCode.toString(),
                            "gradeClass" to hw.gradeClass.toString(),
                            "subject" to hw.subject.toString(),
                            "title" to hw.title.toString(),
                            "description" to hw.description.toString(),
                            "assignedDate" to hw.assignedDate.toString(),
                            "dueDate" to hw.dueDate.toString()
                        )
                    )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun syncClasswork(schoolCode: String, cw: ClassworkEntity) {
        scope.launch {
            try {
                firestore.collection("schools").document(schoolCode)
                    .collection("classwork").document(cw.id.toString())
                    .set(
                        mapOf(
                            "id" to cw.id.toString(),
                            "schoolCode" to cw.schoolCode.toString(),
                            "gradeClass" to cw.gradeClass.toString(),
                            "subject" to cw.subject.toString(),
                            "topicTitle" to cw.topicTitle.toString(),
                            "summary" to cw.summary.toString(),
                            "date" to cw.date.toString()
                        )
                    )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startSync(schoolCode: String) {
        if (schoolCode.isBlank()) return
        scope.launch {
            try {
                database.schoolDao().getSchoolByCode(schoolCode)?.let { saveSchoolToCloud(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
