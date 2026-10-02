package com.school.manage.domain.model

data class Student(
    val id: Long = 0,
    val rollNo: String,
    val name: String,
    val gradeClass: String,
    val section: String,
    val guardianName: String,
    val phone: String,
    val admissionDate: Long,
    val monthlyFee: Double
)
