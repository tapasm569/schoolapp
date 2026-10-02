package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolCode: String,
    val name: String,
    val role: String = "Teacher",
    val phone: String,
    val salary: Double = 0.0,
    val joinDate: String = "02/10/2026",
    val gender: String = "Male",
    val whatsapp: String = "",
    val address: String = "",
    val qualification: String = "",
    val salaryType: String = "Monthly",
    val password: String = ""
)
