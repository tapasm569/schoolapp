package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rollNo: String,
    val name: String,
    val gradeClass: String,
    val section: String,
    val guardianName: String,
    val phone: String,
    val monthlyFee: Double,
    val admissionDate: String
)
