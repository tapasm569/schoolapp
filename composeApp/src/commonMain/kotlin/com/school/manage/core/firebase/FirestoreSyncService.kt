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

    // 1. School Admin Sync
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

    // 2. Student Cloud Sync
    fun syncStudent(schoolCode: String, student: StudentEntity) {
        scope.launch {
            try {
                firestore.collection("schools").document(schoolCode)
                    .collection("students").document(student.id.toString())
                    .set(
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 3. Staff / Teacher Cloud Sync
    fun syncStaff(schoolCode: String, staff: StaffEntity) {
        scope.launch {
            try {
                firestore.collection("schools").document(schoolCode)
                    .collection("staff").document(staff.id.toString())
                    .set(
                        mapOf(
                            "id" to staff.id.toString(),
                            "schoolCode" to staff.schoolCode,
                            "name" to staff.name,
                            "role" to staff.role,
                            "phone" to staff.phone,
                            "salary" to staff.salary.toString(),
                            "joinDate" to staff.joinDate
                        )
                    )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 4. Fee Record Cloud Sync
    fun syncFee(schoolCode: String, fee: FeeRecordEntity) {
        scope.launch {
            try {
                firestore.collection("schools").document(schoolCode)
                    .collection("fees").document(fee.id.toString())
                    .set(
                        mapOf(
                            "id" to fee.id.toString(),
                            "studentId" to fee.studentId.toString(),
                            "schoolCode" to fee.schoolCode,
                            "amountPaid" to fee.amountPaid.toString(),
                            "paymentDate" to fee.paymentDate,
                            "remarks" to fee.remarks
                        )
                    )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 5. Attendance Cloud Sync
    fun syncAttendance(schoolCode: String, att: AttendanceEntity) {
        scope.launch {
            try {
                firestore.collection("schools").document(schoolCode)
                    .collection("attendance").document(att.id.toString())
                    .set(
                        mapOf(
                            "id" to att.id.toString(),
                            "studentId" to att.studentId.toString(),
                            "schoolCode" to att.schoolCode,
                            "date" to att.date,
                            "status" to att.status
                        )
                    )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 6. Exam Cloud Sync
    fun syncExam(schoolCode: String, exam: ExamEntity) {
        scope.launch {
            try {
                firestore.collection("schools").document(schoolCode)
                    .collection("exams").document(exam.id.toString())
                    .set(
                        mapOf(
                            "id" to exam.id.toString(),
                            "schoolCode" to exam.schoolCode,
                            "title" to exam.title,
                            "gradeClass" to exam.gradeClass,
                            "examDate" to exam.examDate,
                            "maxMarks" to exam.maxMarks.toString()
                        )
                    )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 7. Homework Cloud Sync
    fun syncHomework(schoolCode: String, hw: HomeworkEntity) {
        scope.launch {
            try {
                firestore.collection("schools").document(schoolCode)
                    .collection("homework").document(hw.id.toString())
                    .set(
                        mapOf(
                            "id" to hw.id.toString(),
                            "schoolCode" to hw.schoolCode,
                            "gradeClass" to hw.gradeClass,
                            "subject" to hw.subject,
                            "title" to hw.title,
                            "description" to hw.description,
                            "dueDate" to hw.dueDate
                        )
                    )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 8. Classwork Cloud Sync
    fun syncClasswork(schoolCode: String, cw: ClassworkEntity) {
        scope.launch {
            try {
                firestore.collection("schools").document(schoolCode)
                    .collection("classwork").document(cw.id.toString())
                    .set(
                        mapOf(
                            "id" to cw.id.toString(),
                            "schoolCode" to cw.schoolCode,
                            "gradeClass" to cw.gradeClass,
                            "subject" to cw.subject,
                            "task" to cw.task,
                            "date" to cw.date
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
