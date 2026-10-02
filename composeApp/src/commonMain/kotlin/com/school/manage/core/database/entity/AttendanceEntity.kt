package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolCode: String = "",
    val studentId: Long = 0L,
    val studentName: String = "",
    val gradeClass: String = "",
    val staffId: Long = 0L,
    val userType: String = "STUDENT",
    val date: String = "",
    val status: String = "PRESENT"
)
