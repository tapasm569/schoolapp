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
    val date: String = "",
    val attachmentUrl: String = "",
    val attachmentType: String = "NONE",
    val attachmentName: String = "",
    val assignedBy: String = "",
    val createdAt: Long = 0L
)
