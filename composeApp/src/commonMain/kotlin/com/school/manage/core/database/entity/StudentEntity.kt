package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolCode: String,
    val rollNo: String = "",
    val name: String,
    val gradeClass: String = "",
    val section: String = "",
    val guardianName: String = "",
    val phone: String, // Serves as student password
    val monthlyFee: Double = 0.0,
    val admissionDate: String = "02/10/2026",
    val fatherName: String = "",
    val motherName: String = "",
    val dob: String = "",
    val aadharNumber: String = "",
    val caste: String = "",
    val gender: String = "Male",
    val whatsapp: String = "",
    val address: String = "",
    val admissionFee: Double = 0.0
)
