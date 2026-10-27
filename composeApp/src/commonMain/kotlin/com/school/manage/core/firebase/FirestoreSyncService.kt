package com.school.manage.core.firebase

import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.BatchEntity
import com.school.manage.core.database.entity.FeeRecordEntity
import com.school.manage.core.database.entity.SchoolEntity
import com.school.manage.core.database.entity.StaffEntity
import com.school.manage.core.database.entity.StudentEntity
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class FirestoreSyncService(private val database: AppDatabase) {

    private val rtdb = Firebase.database
    private val scope = CoroutineScope(Dispatchers.Default)
    private val syncJobs = mutableMapOf<String, Job>()

    /**
     * Pushes all local SQLite data directly to Firebase Realtime Database.
     */
    suspend fun pushAllLocalDataToCloud(schoolCode: String) {
        restoreAllFromCloud(schoolCode)
    }

    /**
     * Compatibility wrapper for AuthScreens: restores all records from RTDB into local SQLite.
     */
    suspend fun restoreAllFromCloud(schoolCode: String) {
        if (schoolCode.isBlank()) return
        try {
            val snapshot = rtdb.reference("schools").child(schoolCode).get()
            if (!snapshot.exists) return

            // Restore School Info
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

            // Restore Students
            val studentsNode = snapshot.child("students")
            if (studentsNode.exists) {
                for (child in studentsNode.children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val sId = map["id"]?.toString()?.toLongOrNull() ?: 0L
                    val sCode = map["schoolCode"]?.toString() ?: schoolCode
                    val roll = map["rollNo"]?.toString() ?: ""
                    val name = map["name"]?.toString() ?: ""
                    val grade = map["gradeClass"]?.toString() ?: ""
                    val sec = map["section"]?.toString() ?: ""
                    val guard = map["guardianName"]?.toString() ?: ""
                    val fName = map["fatherName"]?.toString() ?: ""
                    val mName = map["motherName"]?.toString() ?: ""
                    val phone = map["phone"]?.toString() ?: ""
                    val whatsapp = map["whatsapp"]?.toString() ?: ""
                    val addr = map["address"]?.toString() ?: ""
                    val dob = map["dob"]?.toString() ?: ""
                    val gender = map["gender"]?.toString() ?: ""
                    val caste = map["caste"]?.toString() ?: ""
                    val admDate = map["admissionDate"]?.toString() ?: ""
                    val photo = map["photoBase64"]?.toString() ?: ""

                    database.studentDao().insertStudent(
                        StudentEntity(
                            id = if (sId > 0) sId else 0L,
                            schoolCode = sCode,
                            rollNo = roll,
                            name = name,
                            gradeClass = grade,
                            section = sec,
                            guardianName = guard,
                            fatherName = fName,
                            motherName = mName,
                            phone = phone,
                            whatsapp = whatsapp,
                            address = addr,
                            dob = dob,
                            gender = gender,
                            caste = caste,
                            admissionDate = admDate,
                            photoBase64 = photo
                        )
                    )
                }
            }

            // Restore Staff
            val staffNode = snapshot.child("staff")
            if (staffNode.exists) {
                for (child in staffNode.children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val stId = map["id"]?.toString()?.toLongOrNull() ?: 0L
                    val sCode = map["schoolCode"]?.toString() ?: schoolCode
                    val name = map["name"]?.toString() ?: ""
                    val role = map["role"]?.toString() ?: ""
                    val phone = map["phone"]?.toString() ?: ""
                    val whatsapp = map["whatsapp"]?.toString() ?: ""
                    val salary = map["salary"]?.toString()?.toDoubleOrNull() ?: 0.0
                    val qual = map["qualification"]?.toString() ?: ""
                    val join = map["joinDate"]?.toString() ?: ""
                    val gender = map["gender"]?.toString() ?: ""
                    val addr = map["address"]?.toString() ?: ""
                    val photo = map["photoBase64"]?.toString() ?: ""

                    database.staffDao().insertStaff(
                        StaffEntity(
                            id = if (stId > 0) stId else 0L,
                            schoolCode = sCode,
                            name = name,
                            role = role,
                            phone = phone,
                            whatsapp = whatsapp,
                            salary = salary,
                            qualification = qual,
                            joinDate = join,
                            gender = gender,
                            address = addr,
                            photoBase64 = photo
                        )
                    )
                }
            }

            syncCollectionFromSnapshot(schoolCode, "batches", snapshot.child("batches"))
            syncCollectionFromSnapshot(schoolCode, "students", snapshot.child("students"))
            syncCollectionFromSnapshot(schoolCode, "staff", snapshot.child("staff"))
            syncCollectionFromSnapshot(schoolCode, "fees", snapshot.child("fees"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Restores a single school entity from cloud by schoolCode.
     */
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

    /**
     * Saves newly registered school to cloud.
     */
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

    /**
     * Pushes school profile info (including masterSubjects) to Firebase.
     */
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

    /**
     * Pushes active academic session (e.g. 2026) to Firebase.
     */
    suspend fun pushActiveSession(schoolCode: String, session: String) {
        if (schoolCode.isBlank() || session.isBlank()) return
        try {
            rtdb.reference("schools").child(schoolCode).child("profile").child("activeSession").setValue(session)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Retrieves active academic session from cloud.
     */
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

    /**
     * Saves one class to Firebase without replacing other classes.
     */
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

        rtdb.reference("schools")
            .child(targetSchool)
            .child("fees")
            .child(fee.id.toString())
            .setValue(
                mapOf(
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
            )
    }

    suspend fun deleteBatchRecord(schoolCode: String, batch: BatchEntity) {
        val targetSchool = schoolCode.ifBlank { batch.schoolCode }
        require(targetSchool.isNotBlank()) { "School code is required" }
        require(batch.id > 0L) { "Class ID must be positive" }

        rtdb.reference("schools")
            .child(targetSchool)
            .child("batches")
            .child(batch.id.toString())
            .removeValue()

        database.batchDao().deleteBatch(batch)
    }

    suspend fun deleteStudentRecord(schoolCode: String, student: StudentEntity): Boolean {
        val targetSchool = schoolCode.ifBlank { student.schoolCode }
        require(targetSchool.isNotBlank()) { "School code is required" }
        require(student.id > 0L) { "Student ID must be positive" }

        rtdb.reference("schools")
            .child(targetSchool)
            .child("students")
            .child(student.id.toString())
            .removeValue()

        database.studentDao().deleteStudent(student)
        return true
    }

    suspend fun deleteStaffRecord(schoolCode: String, staff: StaffEntity): Boolean {
        val targetSchool = schoolCode.ifBlank { staff.schoolCode }
        require(targetSchool.isNotBlank()) { "School code is required" }
        require(staff.id > 0L) { "Staff ID must be positive" }

        rtdb.reference("schools")
            .child(targetSchool)
            .child("staff")
            .child(staff.id.toString())
            .removeValue()

        database.staffDao().deleteStaff(staff)
        return true
    }

    /**
     * Reconcile one complete Firebase collection with its local Room records.
     */
    private suspend fun syncCollectionFromSnapshot(
        schoolCode: String,
        collection: String,
        snapshot: dev.gitlive.firebase.database.DataSnapshot
    ) {
        if (!snapshot.exists) return

        val children = snapshot.children.toList()
        if (children.isEmpty()) return

        when (collection) {
            "batches" -> {
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull()
                        ?: child.key?.toLongOrNull()
                        ?: continue
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
                            defaultMonthlyTuition =
                                map["defaultMonthlyTuition"]?.toString()
                                    ?.toDoubleOrNull() ?: 0.0,
                            collectTuitionAtAdmission =
                                map["collectTuitionAtAdmission"]?.toString()
                                    ?.toBooleanStrictOrNull() ?: true
                        )
                    )
                }
            }

            "students" -> {
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull()
                        ?: child.key?.toLongOrNull()
                        ?: continue
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
                    val id = map["id"]?.toString()?.toLongOrNull()
                        ?: child.key?.toLongOrNull()
                        ?: continue
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

            "fees" -> {
                for (child in children) {
                    val map = child.value as? Map<*, *> ?: continue
                    val id = map["id"]?.toString()?.toLongOrNull()
                        ?: child.key?.toLongOrNull()
                        ?: continue
                    if (id <= 0L) continue

                    database.feeDao().insertFeeRecord(
                        FeeRecordEntity(
                            id = id,
                            schoolCode = map["schoolCode"]?.toString() ?: schoolCode,
                            studentId = map["studentId"]?.toString()
                                ?.toLongOrNull() ?: 0L,
                            studentName = map["studentName"]?.toString() ?: "",
                            gradeClass = map["gradeClass"]?.toString() ?: "",
                            amountPaid = map["amountPaid"]?.toString()
                                ?.toDoubleOrNull() ?: 0.0,
                            paymentDate = map["paymentDate"]?.toString() ?: "",
                            feeMonth = map["feeMonth"]?.toString() ?: "",
                            paymentMode = map["paymentMode"]?.toString() ?: "",
                            remarks = map["remarks"]?.toString() ?: ""
                        )
                    )
                }
            }
        }
    }

    /**
     * Keep local Room data synchronized with Firebase changes.
     */
    fun startSync(schoolCode: String) {
        if (schoolCode.isBlank()) return
        if (syncJobs[schoolCode]?.isActive == true) return

        syncJobs[schoolCode] = scope.launch {
            val schoolRef = rtdb.reference("schools").child(schoolCode)

            for (collection in listOf("batches", "students", "staff", "fees")) {
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
