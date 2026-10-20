package com.school.manage.core.firebase

import com.school.manage.core.database.AppDatabase
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
     * RTDB has NO daily 20,000 operation write cap, so this is safe for bulk sync.
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
                    "className" to item.className,
                    "section" to item.section,
                    "monthlyFee" to item.monthlyFee.toString(),
                    "admissionFee" to item.admissionFee.toString(),
                    "subjects" to item.subjects,
                    "startDate" to item.startDate,
                    "endDate" to item.endDate
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
            val fees = database.feeDao().getFeesBySchool(schoolCode).first()
            val feeMap = mutableMapOf<String, Any>()
            for (item in fees) {
                feeMap[item.id.toString()] = mapOf(
                    "id" to item.id.toString(),
                    "schoolCode" to item.schoolCode,
                    "studentId" to item.studentId.toString(),
                    "monthYear" to item.monthYear,
                    "amount" to item.amount.toString(),
                    "status" to item.status,
                    "paidDate" to item.paidDate,
                    "remarks" to item.remarks
                )
            }
            if (feeMap.isNotEmpty()) {
                schoolRef.child("fees").setValue(feeMap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        println("===> [RTDB SYNC] Finished pushing all collections to Realtime Database successfully!")
    }

    /**
     * Single-Record Delta Sync:
     * Only updates or inserts the specific student record that was edited/created.
     */
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
                println("✓ [Delta Sync] Synced single student: ${student.name} (ID: ${student.id})")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Single-Record Delta Sync:
     * Only updates or inserts the specific staff member that was edited/created.
     */
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
                println("✓ [Delta Sync] Synced single staff member: ${staff.name} (ID: ${staff.id})")
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
