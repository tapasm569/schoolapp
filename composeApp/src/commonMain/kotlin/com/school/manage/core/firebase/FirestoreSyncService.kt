package com.school.manage.core.firebase

import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.*
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class FirestoreSyncService(private val database: AppDatabase) {

    private val firestore = Firebase.firestore
    private val scope = CoroutineScope(Dispatchers.IO)

    private fun getSafeStr(doc: dev.gitlive.firebase.firestore.DocumentSnapshot, field: String): String {
        return try {
            if (doc.contains(field)) doc.get<String?>(field) ?: "" else ""
        } catch (e: Exception) {
            ""
        }
    }

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
                            "email" to school.email,
                            "address" to school.address,
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
                    val email = getSafeStr(doc, "email")
                    val address = getSafeStr(doc, "address")
                    val pass = getSafeStr(doc, "password")

                    val restored = SchoolEntity(
                        schoolCode = code,
                        schoolName = name,
                        phone = phone,
                        email = email,
                        address = address,
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

    suspend fun pushAllLocalDataToCloud(schoolCode: String) {
        if (schoolCode.isBlank()) return
        val schoolRef = firestore.collection("schools").document(schoolCode)

        // 1. Classes / Batches
        try {
            val batches = database.batchDao().getBatchesBySchool(schoolCode).first()
            for (item in batches) {
                try {
                    schoolRef.collection("classes").document(item.id.toString()).set(
                        mapOf(
                            "id" to item.id.toString(),
                            "schoolCode" to item.schoolCode,
                            "batchName" to item.batchName,
                            "defaultMonthlyTuition" to item.defaultMonthlyTuition.toString(),
                            "subjects" to item.subjects,
                            "sections" to item.sections
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Students
        try {
            val students = database.studentDao().getStudentsBySchool(schoolCode).first()
            for (item in students) {
                try {
                    schoolRef.collection("students").document(item.id.toString()).set(
                        mapOf(
                            "id" to item.id.toString(),
                            "schoolCode" to item.schoolCode,
                            "rollNo" to item.rollNo,
                            "name" to item.name,
                            "gradeClass" to item.gradeClass,
                            "section" to item.section,
                            "guardianName" to item.guardianName,
                            "fatherName" to item.fatherName,
                            "motherName" to item.motherName,
                            "phone" to item.phone,
                            "whatsapp" to item.whatsapp,
                            "monthlyFee" to item.monthlyFee.toString(),
                            "admissionFee" to item.admissionFee.toString(),
                            "admissionDate" to item.admissionDate,
                            "dob" to item.dob,
                            "caste" to item.caste,
                            "gender" to item.gender,
                            "address" to item.address
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Staff
        try {
            val staffList = database.staffDao().getStaffBySchool(schoolCode).first()
            for (item in staffList) {
                try {
                    schoolRef.collection("staff").document(item.id.toString()).set(
                        mapOf(
                            "id" to item.id.toString(),
                            "schoolCode" to item.schoolCode,
                            "name" to item.name,
                            "role" to item.role,
                            "qualification" to item.qualification,
                            "phone" to item.phone,
                            "whatsapp" to item.whatsapp,
                            "salary" to item.salary.toString(),
                            "salaryType" to item.salaryType,
                            "joinDate" to item.joinDate,
                            "gender" to item.gender,
                            "address" to item.address
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Fees
        try {
            val fees = database.feeDao().getFeeRecordsBySchool(schoolCode).first()
            for (item in fees) {
                try {
                    schoolRef.collection("fees").document(item.id.toString()).set(
                        mapOf(
                            "id" to item.id.toString(),
                            "schoolCode" to item.schoolCode,
                            "studentId" to item.studentId.toString(),
                            "studentName" to item.studentName,
                            "amountPaid" to item.amountPaid.toString(),
                            "paymentDate" to item.paymentDate,
                            "paymentMode" to item.paymentMode,
                            "remarks" to item.remarks
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun restoreAllFromCloud(schoolCode: String) {
        if (schoolCode.isBlank()) return
        try {
            withTimeout(30000L) {
                val schoolRef = firestore.collection("schools").document(schoolCode)

                try {
                    val classesDocs = schoolRef.collection("classes").get().documents
                    for (doc in classesDocs) {
                        val id = doc.id.toLongOrNull() ?: 0L
                        val batchName = getSafeStr(doc, "batchName")
                        val feeStr = getSafeStr(doc, "defaultMonthlyTuition")
                        val subjects = getSafeStr(doc, "subjects")
                        val sections = getSafeStr(doc, "sections")
                        if (batchName.isNotBlank()) {
                            database.batchDao().insertBatch(
                                BatchEntity(
                                    id = id,
                                    schoolCode = schoolCode,
                                    batchName = batchName,
                                    defaultMonthlyTuition = feeStr.toDoubleOrNull() ?: 0.0,
                                    subjects = subjects,
                                    sections = sections
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    val studentDocs = schoolRef.collection("students").get().documents
                    for (doc in studentDocs) {
                        val id = doc.id.toLongOrNull() ?: 0L
                        val rollNo = getSafeStr(doc, "rollNo")
                        val name = getSafeStr(doc, "name")
                        val gradeClass = getSafeStr(doc, "gradeClass")
                        val section = getSafeStr(doc, "section")
                        val guardianName = getSafeStr(doc, "guardianName")
                        val fatherName = getSafeStr(doc, "fatherName").ifBlank { guardianName }
                        val motherName = getSafeStr(doc, "motherName")
                        val phone = getSafeStr(doc, "phone")
                        val whatsapp = getSafeStr(doc, "whatsapp").ifBlank { phone }
                        val feeStr = getSafeStr(doc, "monthlyFee")
                        val admFeeStr = getSafeStr(doc, "admissionFee")
                        val admissionDate = getSafeStr(doc, "admissionDate")
                        val dob = getSafeStr(doc, "dob")
                        val caste = getSafeStr(doc, "caste")
                        val gender = getSafeStr(doc, "gender").ifBlank { "Male" }
                        val address = getSafeStr(doc, "address")

                        if (name.isNotBlank()) {
                            database.studentDao().insertStudent(
                                StudentEntity(
                                    id = id,
                                    schoolCode = schoolCode,
                                    rollNo = rollNo,
                                    name = name,
                                    gradeClass = gradeClass,
                                    section = section,
                                    guardianName = guardianName.ifBlank { fatherName },
                                    fatherName = fatherName,
                                    motherName = motherName,
                                    phone = phone,
                                    whatsapp = whatsapp,
                                    monthlyFee = feeStr.toDoubleOrNull() ?: 0.0,
                                    admissionFee = admFeeStr.toDoubleOrNull() ?: 0.0,
                                    admissionDate = admissionDate,
                                    dob = dob,
                                    caste = caste,
                                    gender = gender,
                                    address = address
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    val staffDocs = schoolRef.collection("staff").get().documents
                    for (doc in staffDocs) {
                        val id = doc.id.toLongOrNull() ?: 0L
                        val name = getSafeStr(doc, "name")
                        val role = getSafeStr(doc, "role").ifBlank { "Teacher" }
                        val phone = getSafeStr(doc, "phone")
                        val salStr = getSafeStr(doc, "salary")
                        val joinDate = getSafeStr(doc, "joinDate")
                        if (name.isNotBlank()) {
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
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    val feeDocs = schoolRef.collection("fees").get().documents
                    for (doc in feeDocs) {
                        val id = doc.id.toLongOrNull() ?: 0L
                        val studentId = getSafeStr(doc, "studentId").toLongOrNull() ?: 0L
                        val studentName = getSafeStr(doc, "studentName")
                        val amountStr = getSafeStr(doc, "amountPaid")
                        val paymentDate = getSafeStr(doc, "paymentDate")
                        val paymentMode = getSafeStr(doc, "paymentMode").ifBlank { "Cash" }
                        val remarks = getSafeStr(doc, "remarks")
                        database.feeDao().insertFee(
                            FeeRecordEntity(
                                id = id,
                                studentId = studentId,
                                studentName = studentName,
                                schoolCode = schoolCode,
                                amountPaid = amountStr.toDoubleOrNull() ?: 0.0,
                                paymentDate = paymentDate,
                                paymentMode = paymentMode,
                                remarks = remarks
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

    fun startSync(schoolCode: String) {
        if (schoolCode.isBlank()) return

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
                                    "defaultMonthlyTuition" to item.defaultMonthlyTuition.toString(),
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
                                    "rollNo" to item.rollNo,
                                    "name" to item.name,
                                    "gradeClass" to item.gradeClass,
                                    "section" to item.section,
                                    "guardianName" to item.guardianName,
                                    "fatherName" to item.fatherName,
                                    "motherName" to item.motherName,
                                    "phone" to item.phone,
                                    "whatsapp" to item.whatsapp,
                                    "monthlyFee" to item.monthlyFee.toString(),
                                    "admissionFee" to item.admissionFee.toString(),
                                    "admissionDate" to item.admissionDate,
                                    "dob" to item.dob,
                                    "caste" to item.caste,
                                    "gender" to item.gender,
                                    "address" to item.address
                                )
                            )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

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

        scope.launch {
            database.feeDao().getFeeRecordsBySchool(schoolCode).collectLatest { list ->
                for (item in list) {
                    try {
                        firestore.collection("schools").document(schoolCode)
                            .collection("fees").document(item.id.toString())
                            .set(
                                mapOf(
                                    "id" to item.id.toString(),
                                    "schoolCode" to item.schoolCode,
                                    "studentId" to item.studentId.toString(),
                                    "studentName" to item.studentName,
                                    "amountPaid" to item.amountPaid.toString(),
                                    "paymentDate" to item.paymentDate,
                                    "paymentMode" to item.paymentMode,
                                    "remarks" to item.remarks
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
