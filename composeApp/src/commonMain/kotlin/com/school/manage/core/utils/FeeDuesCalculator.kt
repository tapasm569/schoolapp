package com.school.manage.core.utils

data class StudentDueRecord(
    val studentId: String,
    val studentName: String,
    val gradeClass: String,
    val parentPhone: String,
    val unpaidMonths: List<String>,
    val totalDueAmount: Double
)

object FeeDuesCalculator {

    /**
     * Calculates unpaid tuition months based on the school's billing policy:
     * - BEGINNING_OF_MONTH: The active month is due immediately on day 1.
     * - AFTER_MONTH_COMPLETE: Only past, concluded months are marked overdue.
     */
    fun calculateOverdue(
        studentId: String,
        studentName: String,
        gradeClass: String,
        parentPhone: String,
        monthlyFee: Double,
        admissionDate: String?, // Format: YYYY-MM-DD or DD/MM/YYYY
        paidMonths: List<String>, // e.g. ["10/2026", "09/2026"]
        billingTiming: String,    // "BEGINNING_OF_MONTH" or "AFTER_MONTH_COMPLETE"
        currentYear: Int = 2026,
        currentMonth: Int = 10
    ): StudentDueRecord? {
        // Parse start year & month from admission date or default to current year
        var startYear = currentYear
        var startMonth = 1
        
        if (!admissionDate.isNullOrBlank()) {
            if (admissionDate.contains("-")) {
                val parts = admissionDate.split("-")
                if (parts.size >= 2) {
                    startYear = parts[0].toIntOrNull() ?: currentYear
                    startMonth = parts[1].toIntOrNull() ?: 1
                }
            } else if (admissionDate.contains("/")) {
                val parts = admissionDate.split("/")
                if (parts.size >= 3) {
                    startYear = parts[2].toIntOrNull() ?: currentYear
                    startMonth = parts[1].toIntOrNull() ?: 1
                }
            }
        }

        // Determine cutoff month based on policy
        val (endYear, endMonth) = if (billingTiming == "AFTER_MONTH_COMPLETE") {
            if (currentMonth == 1) Pair(currentYear - 1, 12) else Pair(currentYear, currentMonth - 1)
        } else {
            Pair(currentYear, currentMonth) // Advance billing includes current month
        }

        if (startYear > endYear || (startYear == endYear && startMonth > endMonth)) {
            return null
        }

        // Generate required billing months
        val requiredMonths = mutableListOf<String>()
        var y = startYear
        var m = startMonth

        while (y < endYear || (y == endYear && m <= endMonth)) {
            requiredMonths.add("${m.toString().padStart(2, '0')}/$y")
            m++
            if (m > 12) {
                m = 1
                y++
            }
        }

        val normalizedPaid = paidMonths.map { it.trim() }
        val unpaid = requiredMonths.filter { req -> !normalizedPaid.contains(req) }

        return if (unpaid.isNotEmpty()) {
            StudentDueRecord(
                studentId = studentId,
                studentName = studentName,
                gradeClass = gradeClass,
                parentPhone = parentPhone,
                unpaidMonths = unpaid,
                totalDueAmount = unpaid.size * monthlyFee
            )
        } else {
            null
        }
    }
}
