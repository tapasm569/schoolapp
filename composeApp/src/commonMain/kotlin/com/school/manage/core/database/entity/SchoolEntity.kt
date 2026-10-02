package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schools")
data class SchoolEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolCode: String,
    val schoolName: String,
    val password: String,
    val phone: String
)
