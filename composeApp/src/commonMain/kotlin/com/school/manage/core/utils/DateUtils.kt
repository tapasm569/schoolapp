package com.school.manage.core.utils

data class CurrentDateData(
    val day: Int,
    val monthIndex: Int, // 0-based: 0 for Jan, 9 for Oct, etc.
    val year: Int,
    val formatted: String // DD/MM/YYYY
)

expect fun getSystemCurrentDate(): CurrentDateData

expect fun getCurrentEpochMillis(): Long
