package com.school.manage.core.utils

import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SchoolBackupManager(private val database: AppDatabase) {

    private fun escapeJson(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun getString(map: Map<String, Any?>, key: String, default: String = ""): String {
        val v = map[key]?.toString()?.trim()
        return if (v.isNullOrEmpty()) default else v
    }

    private fun getDouble(map: Map<String, Any?>, key: String, default: Double = 0.0): Double {
        val v = map[key]
        return when (v) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull() ?: default
            else -> default
        }
    }

    private fun getLong(map: Map<String, Any?>, key: String, default: Long = 0L): Long {
        val v = map[key]
        return when (v) {
            is Number -> v.toLong()
            is String -> v.toLongOrNull() ?: default
            else -> default
        }
    }

    private fun getBoolean(map: Map<String, Any?>, key: String, default: Boolean = true): Boolean {
        val v = map[key]
        return when (v) {
            is Boolean -> v
            is String -> v.toBooleanStrictOrNull() ?: default
            else -> default
        }
    }

    suspend fun generateBackupJson(schoolCode: String): Pair<String, String> = withContext(Dispatchers.IO) {
        val batches = database.batchDao().getBatchesBySchool(schoolCode).first()
        val students = database.studentDao().getStudentsBySchool(schoolCode).first()
        val staffList = database.staffDao().getStaffBySchool(schoolCode).first()
        val fees = database.feeDao().getFeeRecordsBySchool(schoolCode).first()
        val expenses = database.expenseDao().getExpensesBySchool(schoolCode).first()
        val attendance = database.attendanceDao().getAttendanceBySchool(schoolCode).first()

        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"version\": 1,\n")
        sb.append("  \"schoolCode\": \"${escapeJson(schoolCode)}\",\n")

        // 1. Batches
        sb.append("  \"batches\": [\n")
        batches.forEachIndexed { i, b ->
            sb.append("    {\"id\": ${b.id}, \"batchName\": \"${escapeJson(b.batchName)}\", \"subjects\": \"${escapeJson(b.subjects)}\", \"sections\": \"${escapeJson(b.sections)}\", \"feeTemplateRaw\": \"${escapeJson(b.feeTemplateRaw)}\", \"defaultMonthlyTuition\": ${b.defaultMonthlyTuition}, \"collectTuitionAtAdmission\": ${b.collectTuitionAtAdmission}}")
            if (i < batches.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ],\n")

        // 2. Students
        sb.append("  \"students\": [\n")
        students.forEachIndexed { i, s ->
            sb.append("    {\"id\": ${s.id}, \"rollNo\": \"${escapeJson(s.rollNo)}\", \"name\": \"${escapeJson(s.name)}\", \"gradeClass\": \"${escapeJson(s.gradeClass)}\", \"section\": \"${escapeJson(s.section)}\", \"guardianName\": \"${escapeJson(s.guardianName)}\", \"phone\": \"${escapeJson(s.phone)}\", \"monthlyFee\": ${s.monthlyFee}, \"admissionDate\": \"${escapeJson(s.admissionDate)}\", \"fatherName\": \"${escapeJson(s.fatherName)}\", \"motherName\": \"${escapeJson(s.motherName)}\", \"dob\": \"${escapeJson(s.dob)}\", \"aadharNumber\": \"${escapeJson(s.aadharNumber)}\", \"caste\": \"${escapeJson(s.caste)}\", \"gender\": \"${escapeJson(s.gender)}\", \"whatsapp\": \"${escapeJson(s.whatsapp)}\", \"address\": \"${escapeJson(s.address)}\", \"admissionFee\": ${s.admissionFee}}")
            if (i < students.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ],\n")

        // 3. Staff
        sb.append("  \"staff\": [\n")
        staffList.forEachIndexed { i, st ->
            sb.append("    {\"id\": ${st.id}, \"name\": \"${escapeJson(st.name)}\", \"role\": \"${escapeJson(st.role)}\", \"phone\": \"${escapeJson(st.phone)}\", \"salary\": ${st.salary}, \"joinDate\": \"${escapeJson(st.joinDate)}\", \"gender\": \"${escapeJson(st.gender)}\", \"whatsapp\": \"${escapeJson(st.whatsapp)}\", \"address\": \"${escapeJson(st.address)}\", \"qualification\": \"${escapeJson(st.qualification)}\", \"salaryType\": \"${escapeJson(st.salaryType)}\", \"password\": \"${escapeJson(st.password)}\"}")
            if (i < staffList.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ],\n")

        // 4. Fees
        sb.append("  \"fees\": [\n")
        fees.forEachIndexed { i, f ->
            sb.append("    {\"id\": ${f.id}, \"studentId\": ${f.studentId}, \"studentName\": \"${escapeJson(f.studentName)}\", \"gradeClass\": \"${escapeJson(f.gradeClass)}\", \"amountPaid\": ${f.amountPaid}, \"paymentDate\": \"${escapeJson(f.paymentDate)}\", \"feeMonth\": \"${escapeJson(f.feeMonth)}\", \"paymentMode\": \"${escapeJson(f.paymentMode)}\", \"remarks\": \"${escapeJson(f.remarks)}\"}")
            if (i < fees.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ],\n")

        // 5. Expenses
        sb.append("  \"expenses\": [\n")
        expenses.forEachIndexed { i, e ->
            sb.append("    {\"id\": ${e.id}, \"title\": \"${escapeJson(e.title)}\", \"category\": \"${escapeJson(e.category)}\", \"amount\": ${e.amount}, \"date\": \"${escapeJson(e.date)}\", \"notes\": \"${escapeJson(e.notes)}\"}")
            if (i < expenses.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ],\n")

        // 6. Attendance
        sb.append("  \"attendance\": [\n")
        attendance.forEachIndexed { i, a ->
            sb.append("    {\"id\": ${a.id}, \"studentId\": ${a.studentId}, \"studentName\": \"${escapeJson(a.studentName)}\", \"gradeClass\": \"${escapeJson(a.gradeClass)}\", \"staffId\": ${a.staffId}, \"userType\": \"${escapeJson(a.userType)}\", \"date\": \"${escapeJson(a.date)}\", \"status\": \"${escapeJson(a.status)}\"}")
            if (i < attendance.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ]\n")
        sb.append("}")

        val summary = "✓ Exported: ${students.size} Students, ${staffList.size} Staff, ${batches.size} Classes, ${fees.size} Fee Records, ${expenses.size} Expenses, ${attendance.size} Attendance entries."
        Pair(sb.toString(), summary)
    }

    suspend fun restoreBackupJson(schoolCode: String, jsonString: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val parser = MiniJsonParser(jsonString)
            val root = parser.parse() as? Map<String, Any?>
                ?: return@withContext Result.failure(Exception("Invalid JSON backup format"))

            val targetCode = getString(root, "schoolCode", schoolCode)

            // Restore Batches
            val batches = (root["batches"] as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()
            batches.forEach { b ->
                database.batchDao().insertBatch(
                    BatchEntity(
                        id = getLong(b, "id", 0L),
                        schoolCode = targetCode,
                        batchName = getString(b, "batchName"),
                        subjects = getString(b, "subjects"),
                        sections = getString(b, "sections"),
                        feeTemplateRaw = getString(b, "feeTemplateRaw"),
                        defaultMonthlyTuition = getDouble(b, "defaultMonthlyTuition", 0.0),
                        collectTuitionAtAdmission = getBoolean(b, "collectTuitionAtAdmission", true)
                    )
                )
            }

            // Restore Students
            val students = (root["students"] as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()
            students.forEach { s ->
                database.studentDao().insertStudent(
                    StudentEntity(
                        id = getLong(s, "id", 0L),
                        schoolCode = targetCode,
                        rollNo = getString(s, "rollNo"),
                        name = getString(s, "name"),
                        gradeClass = getString(s, "gradeClass"),
                        section = getString(s, "section"),
                        guardianName = getString(s, "guardianName"),
                        phone = getString(s, "phone"),
                        monthlyFee = getDouble(s, "monthlyFee", 0.0),
                        admissionDate = getString(s, "admissionDate", "02/10/2026"),
                        fatherName = getString(s, "fatherName"),
                        motherName = getString(s, "motherName"),
                        dob = getString(s, "dob"),
                        aadharNumber = getString(s, "aadharNumber"),
                        caste = getString(s, "caste"),
                        gender = getString(s, "gender", "Male"),
                        whatsapp = getString(s, "whatsapp"),
                        address = getString(s, "address"),
                        admissionFee = getDouble(s, "admissionFee", 0.0)
                    )
                )
            }

            // Restore Staff
            val staff = (root["staff"] as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()
            staff.forEach { st ->
                database.staffDao().insertStaff(
                    StaffEntity(
                        id = getLong(st, "id", 0L),
                        schoolCode = targetCode,
                        name = getString(st, "name"),
                        role = getString(st, "role", "Teacher"),
                        phone = getString(st, "phone"),
                        salary = getDouble(st, "salary", 0.0),
                        joinDate = getString(st, "joinDate", "02/10/2026"),
                        gender = getString(st, "gender", "Male"),
                        whatsapp = getString(st, "whatsapp"),
                        address = getString(st, "address"),
                        qualification = getString(st, "qualification"),
                        salaryType = getString(st, "salaryType", "Monthly"),
                        password = getString(st, "password")
                    )
                )
            }

            // Restore Fees
            val fees = (root["fees"] as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()
            fees.forEach { f ->
                database.feeDao().insertFeeRecord(
                    FeeRecordEntity(
                        id = getLong(f, "id", 0L),
                        schoolCode = targetCode,
                        studentId = getLong(f, "studentId", 0L),
                        studentName = getString(f, "studentName"),
                        gradeClass = getString(f, "gradeClass"),
                        amountPaid = getDouble(f, "amountPaid", 0.0),
                        paymentDate = getString(f, "paymentDate"),
                        feeMonth = getString(f, "feeMonth"),
                        paymentMode = getString(f, "paymentMode", "CASH"),
                        remarks = getString(f, "remarks")
                    )
                )
            }

            // Restore Expenses
            val expenses = (root["expenses"] as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()
            expenses.forEach { e ->
                database.expenseDao().insertExpense(
                    ExpenseEntity(
                        id = getLong(e, "id", 0L),
                        schoolCode = targetCode,
                        title = getString(e, "title"),
                        category = getString(e, "category"),
                        amount = getDouble(e, "amount", 0.0),
                        date = getString(e, "date"),
                        notes = getString(e, "notes")
                    )
                )
            }

            // Restore Attendance
            val attendance = (root["attendance"] as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()
            if (attendance.isNotEmpty()) {
                val attEntities = attendance.map { a ->
                    AttendanceEntity(
                        id = getLong(a, "id", 0L),
                        schoolCode = targetCode,
                        studentId = getLong(a, "studentId", 0L),
                        studentName = getString(a, "studentName"),
                        gradeClass = getString(a, "gradeClass"),
                        staffId = getLong(a, "staffId", 0L),
                        userType = getString(a, "userType", "STUDENT"),
                        date = getString(a, "date"),
                        status = getString(a, "status", "PRESENT")
                    )
                }
                database.attendanceDao().insertAll(attEntities)
            }

            Result.success("Restored successfully: ${students.size} Students, ${staff.size} Staff, ${batches.size} Classes, ${fees.size} Fee Records, and ${expenses.size} Expenses.")
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }
}

class MiniJsonParser(private val json: String) {
    private var i = 0

    private fun skip() {
        while (i < json.length && json[i].isWhitespace()) i++
    }

    fun parse(): Any? {
        skip()
        if (i >= json.length) return null
        return when (json[i]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't', 'f' -> parseBoolean()
            'n' -> parseNull()
            else -> parseNumber()
        }
    }

    private fun parseObject(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        i++ // consume '{'
        skip()
        if (i < json.length && json[i] == '}') {
            i++
            return map
        }
        while (i < json.length) {
            skip()
            val key = parseString()
            skip()
            if (i < json.length && json[i] == ':') i++
            skip()
            val value = parse()
            map[key] = value
            skip()
            if (i < json.length && json[i] == ',') {
                i++
            } else if (i < json.length && json[i] == '}') {
                i++
                break
            }
        }
        return map
    }

    private fun parseArray(): List<Any?> {
        val list = mutableListOf<Any?>()
        i++ // consume '['
        skip()
        if (i < json.length && json[i] == ']') {
            i++
            return list
        }
        while (i < json.length) {
            val item = parse()
            list.add(item)
            skip()
            if (i < json.length && json[i] == ',') {
                i++
            } else if (i < json.length && json[i] == ']') {
                i++
                break
            }
        }
        return list
    }

    private fun parseString(): String {
        i++ // consume '"'
        val sb = StringBuilder()
        while (i < json.length) {
            val c = json[i++]
            if (c == '"') break
            if (c == '\\' && i < json.length) {
                when (val esc = json[i++]) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    '/' -> sb.append('/')
                    else -> sb.append(esc)
                }
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun parseBoolean(): Boolean {
        if (json.startsWith("true", i)) {
            i += 4
            return true
        }
        if (json.startsWith("false", i)) {
            i += 5
            return false
        }
        return false
    }

    private fun parseNull(): Any? {
        if (json.startsWith("null", i)) i += 4
        return null
    }

    private fun parseNumber(): Number {
        val start = i
        if (i < json.length && (json[i] == '-' || json[i] == '+')) i++
        while (i < json.length && (json[i].isDigit() || json[i] == '.' || json[i] == 'e' || json[i] == 'E')) i++
        val numStr = json.substring(start, i)
        return if (numStr.contains('.')) {
            numStr.toDoubleOrNull() ?: 0.0
        } else {
            numStr.toLongOrNull() ?: 0L
        }
    }
}
