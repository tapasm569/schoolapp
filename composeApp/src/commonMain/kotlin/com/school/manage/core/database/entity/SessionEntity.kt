package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_session")
data class SessionEntity(
    @PrimaryKey val id: Int = 1,
    val role: String, // "ADMIN", "STAFF", "STUDENT"
    val schoolCode: String = "",
    val schoolName: String = "",
    val staffId: Long = 0L,
    val studentId: Long = 0L
)
