package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val role: String, // Teacher, Admin, Accountant, Support
    val phone: String,
    val salary: Double,
    val joinDate: String
)
