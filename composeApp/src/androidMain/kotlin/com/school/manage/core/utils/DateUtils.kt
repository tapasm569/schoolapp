package com.school.manage.core.utils

import java.util.Calendar

actual fun getSystemCurrentDate(): CurrentDateData {
    val cal = Calendar.getInstance()
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val monthIndex = cal.get(Calendar.MONTH) // 0-based
    val year = cal.get(Calendar.YEAR)
    val dStr = if (day < 10) "0$day" else "$day"
    val mStr = if (monthIndex + 1 < 10) "0${monthIndex + 1}" else "${monthIndex + 1}"
    return CurrentDateData(
        day = day,
        monthIndex = monthIndex,
        year = year,
        formatted = "$dStr/$mStr/$year"
    )
}
