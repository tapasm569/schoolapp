package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classwork")
data class ClassworkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolCode: String = "",
    val gradeClass: String = "",
    val subject: String = "",
    val topicTitle: String = "",
    val summary: String = "",
    val date: String = "03/10/2026"
)
