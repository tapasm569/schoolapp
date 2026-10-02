package com.school.manage.domain.model

data class Student(
    val id: Long = 0,
    val schoolCode: String = "",
    val rollNo: String,
    val name: String,
    val gradeClass: String,
    val section: String,
    val guardianName: String,
    val phone: String,
    val monthlyFee: Double,
    val admissionDate: String
)
