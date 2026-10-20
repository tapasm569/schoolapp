package com.school.manage.core.firebase

import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.BatchEntity
import com.school.manage.core.database.entity.FeeRecordEntity
import com.school.manage.core.database.entity.SchoolEntity
import com.school.manage.core.database.entity.StaffEntity
import com.school.manage.core.database.entity.StudentEntity
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FirestoreSyncService(private val database: AppDatabase) {

    private val rtdb = Firebase.database
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Pushes all local SQLite data directly to Firebase Realtime Database.
     */
    suspend fun pushAllLocalDataToCloud(schoolCode: String) {
        if (schoolCode.isBlank()) return
        println("===> [RTDB SYNC] Pushing all local data to Realtime Database for school: $schoolCode")

        val schoolRef = rtdb.reference("schools").child(schoolCode)

        // 1. School Info
        try {
            val school = database.schoolDao().getSchoolByCode(schoolCode)
            if (school != null) {
                schoolRef.child("info").setValue(
                    mapOf(
                        "schoolCode" to school.schoolCode,
                        "schoolName" to school.schoolName,
                        "password" to school.password,
                        "email" to school.email,
                        "phone" to school.phone,
                        "address" to school.address,
                        "photoBase64" to school.photoBase64
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Batches
        try {
            val batches = database.batchDao().getBatchesBySchool(schoolCode).first()
            val batchMap = mutableMapOf<String, Any>()
            for (item in batches) {
                batchMap[item.id.toString()] = mapOf(
                    "id" to item.id.toString(),
                    "schoolCode" to item.schoolCode,
                    "batchName" to item.batchName,
                    "subjects" to item.subjects,
                    "sections" to item.sections,
                    "feeTemplateRaw" to item.feeTemplateRaw,
                    "defaultMonthlyTuition" to item.defaultMonthlyTuition.toString(),
                    "collectTuitionAtAdmission" to item.collectTuitionAtAdmission.toString()
                )
            }
            if (batchMap.isNotEmpty()) {
                schoolRef.child("batches").setValue(batchMap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Students
        try {
            val students = database.studentDao().getStudentsBySchool(schoolCode).first()
            val studentMap = mutableMapOf<String, Any>()
            for (item in students) {
                studentMap[item.id.toString()] = mapOf(
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
                    "address" to item.address,
                    "dob" to item.dob,
                    "gender" to item.gender,
                    "caste" to item.caste,
                    "admissionDate" to item.admissionDate,
                    "photoBase64" to item.photoBase64
                )
            }
            if (studentMap.isNotEmpty()) {
                schoolRef.child("students").setValue(studentMap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Staff
        try {
            val staffList = database.staffDao().getStaffBySchool(schoolCode).first()
            val staffMap = mutableMapOf<String, Any>()
            for (item in staffList) {
                staffMap[item.id.toString()] = mapOf(
                    "id" to item.id.toString(),
                    "schoolCode" to item.schoolCode,
                    "name" to item.name,
                    "role" to item.role,
                    "phone" to item.phone,
                    "whatsapp" to item.whatsapp,
                    "salary" to item.salary.toString(),
                    "qualification" to item.qualification,
                    "joinDate" to item.joinDate,
                    "gender" to item.gender,
                    "address" to item.address,
                    "photoBase64" to item.photoBase64
                )
            }
            if (staffMap.isNotEmpty()) {
                schoolRef.child("staff").setValue(staffMap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 5. Fees
        try {
            val fees = database.feeDao().getFeeRecordsBySchool(schoolCode).first()
            val feeMap = mutableMapOf<String, Any>()
            for (item in fees) {
                feeMap[item.id.toString()] = mapOf(
                    "id" to item.id.toString(),
                    "schoolCode" to item.schoolCode,
                    "studentId" to item.studentId.toString(),
                    "studentName" to item.studentName,
                    "gradeClass" to item.gradeClass,
                    "amountPaid" to item.amountPaid.toString(),
                    "paymentDate" to item.paymentDate,
                    "feeMonth" to item.feeMonth,
                    "paymentMode" to item.paymentMode,
                    "remarks" to item.remarks
                )
            }
            if (feeMap.isNotEmpty()) {
                schoolRef.child("fees").setValue(feeMap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
            // Cache locally
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

    fun pushStudent(schoolCode: String, student: StudentEntity) {
        val targetSchool = schoolCode.ifBlank { student.schoolCode }
        if (targetSchool.isBlank()) return
        scope.launch {
            try {
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
                            "photoBase64" to student.photoBase64
                        )
                    )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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

    suspend fun deleteStudentRecord(schoolCode: String, student: StudentEntity): Boolean {
        val targetSchool = schoolCode.ifBlank { student.schoolCode }
        try {
            database.studentDao().deleteStudent(student)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (targetSchool.isNotBlank()) {
            scope.launch {
                try {
                    rtdb.reference("schools").child(targetSchool).child("students").child(student.id.toString()).removeValue()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return true
    }

    suspend fun deleteStaffRecord(schoolCode: String, staff: StaffEntity): Boolean {
        val targetSchool = schoolCode.ifBlank { staff.schoolCode }
        try {
            database.staffDao().deleteStaff(staff)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (targetSchool.isNotBlank()) {
            scope.launch {
                try {
                    rtdb.reference("schools").child(targetSchool).child("staff").child(staff.id.toString()).removeValue()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return true
    }

    fun startSync(schoolCode: String) {
        if (schoolCode.isBlank()) return
        scope.launch {
            try {
                pushAllLocalDataToCloud(schoolCode)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
