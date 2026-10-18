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

            val targetCode = root["schoolCode"] as? String ?: schoolCode

            // Restore Batches
            val batches = (root["batches"] as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()
            batches.forEach { b ->
                database.batchDao().insertBatch(
                    BatchEntity(
                        id = (b["id"] as? Number)?.toLong() ?: 0L,
                        schoolCode = targetCode,
                        batchName = b["batchName"]?.toString().orEmpty(),
                        subjects = b["subjects"]?.toString().orEmpty(),
                        sections = b["sections"]?.toString().orEmpty(),
                        feeTemplateRaw = b["feeTemplateRaw"]?.toString().orEmpty(),
                        defaultMonthlyTuition = (b["defaultMonthlyTuition"] as? Number)?.toDouble() ?: 0.0,
                        collectTuitionAtAdmission = (b["collectTuitionAtAdmission"] as? Boolean) ?: true
                    )
                )
            }

            // Restore Students
            val students = (root["students"] as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()
            students.forEach { s ->
                database.studentDao().insertStudent(
                    StudentEntity(
                        id = (s["id"] as? Number)?.toLong() ?: 0L,
                        schoolCode = targetCode,
                        rollNo = s["rollNo"]?.toString().orEmpty(),
                        name = s["name"]?.toString().orEmpty(),
                        gradeClass = s["gradeClass"]?.toString().orEmpty(),
                        section = s["section"]?.toString().orEmpty(),
                        guardianName = s["guardianName"]?.toString().orEmpty(),
                        phone = s["phone"]?.toString().orEmpty(),
                        monthlyFee = (s["monthlyFee"] as? Number)?.toDouble() ?: 0.0,
                        admissionDate = s["admissionDate"]?.toString().ifEmpty { "02/10/2026" },
                        fatherName = s["fatherName"]?.toString().orEmpty(),
                        motherName = s["motherName"]?.toString().orEmpty(),
                        dob = s["dob"]?.toString().orEmpty(),
                        aadharNumber = s["aadharNumber"]?.toString().orEmpty(),
                        caste = s["caste"]?.toString().orEmpty(),
                        gender = s["gender"]?.toString().ifEmpty { "Male" },
                        whatsapp = s["whatsapp"]?.toString().orEmpty(),
                        address = s["address"]?.toString().orEmpty(),
                        admissionFee = (s["admissionFee"] as? Number)?.toDouble() ?: 0.0
                    )
                )
            }

            // Restore Staff
            val staff = (root["staff"] as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()
            staff.forEach { st ->
                database.staffDao().insertStaff(
                    StaffEntity(
                        id = (st["id"] as? Number)?.toLong() ?: 0L,
                        schoolCode = targetCode,
                        name = st["name"]?.toString().orEmpty(),
                        role = st["role"]?.toString().ifEmpty { "Teacher" },
                        phone = st["phone"]?.toString().orEmpty(),
                        salary = (st["salary"] as? Number)?.toDouble() ?: 0.0,
                        joinDate = st["joinDate"]?.toString().ifEmpty { "02/10/2026" },
                        gender = st["gender"]?.toString().ifEmpty { "Male" },
                        whatsapp = st["whatsapp"]?.toString().orEmpty(),
                        address = st["address"]?.toString().orEmpty(),
                        qualification = st["qualification"]?.toString().orEmpty(),
                        salaryType = st["salaryType"]?.toString().ifEmpty { "Monthly" },
                        password = st["password"]?.toString().orEmpty()
                    )
                )
            }

            // Restore Fees
            val fees = (root["fees"] as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()
            fees.forEach { f ->
                database.feeDao().insertFeeRecord(
                    FeeRecordEntity(
                        id = (f["id"] as? Number)?.toLong() ?: 0L,
                        schoolCode = targetCode,
                        studentId = (f["studentId"] as? Number)?.toLong() ?: 0L,
                        studentName = f["studentName"]?.toString().orEmpty(),
                        gradeClass = f["gradeClass"]?.toString().orEmpty(),
                        amountPaid = (f["amountPaid"] as? Number)?.toDouble() ?: 0.0,
                        paymentDate = f["paymentDate"]?.toString().orEmpty(),
                        feeMonth = f["feeMonth"]?.toString().orEmpty(),
                        paymentMode = f["paymentMode"]?.toString().ifEmpty { "CASH" },
                        remarks = f["remarks"]?.toString().orEmpty()
                    )
                )
            }

            // Restore Expenses
            val expenses = (root["expenses"] as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()
            expenses.forEach { e ->
                database.expenseDao().insertExpense(
                    ExpenseEntity(
                        id = (e["id"] as? Number)?.toLong() ?: 0L,
                        schoolCode = targetCode,
                        title = e["title"]?.toString().orEmpty(),
                        category = e["category"]?.toString().orEmpty(),
                        amount = (e["amount"] as? Number)?.toDouble() ?: 0.0,
                        date = e["date"]?.toString().orEmpty(),
                        notes = e["notes"]?.toString().orEmpty()
                    )
                )
            }

            // Restore Attendance
            val attendance = (root["attendance"] as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()
            if (attendance.isNotEmpty()) {
                val attEntities = attendance.map { a ->
                    AttendanceEntity(
                        id = (a["id"] as? Number)?.toLong() ?: 0L,
                        schoolCode = targetCode,
                        studentId = (a["studentId"] as? Number)?.toLong() ?: 0L,
                        studentName = a["studentName"]?.toString().orEmpty(),
                        gradeClass = a["gradeClass"]?.toString().orEmpty(),
                        staffId = (a["staffId"] as? Number)?.toLong() ?: 0L,
                        userType = a["userType"]?.toString().ifEmpty { "STUDENT" },
                        date = a["date"]?.toString().orEmpty(),
                        status = a["status"]?.toString().ifEmpty { "PRESENT" }
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
