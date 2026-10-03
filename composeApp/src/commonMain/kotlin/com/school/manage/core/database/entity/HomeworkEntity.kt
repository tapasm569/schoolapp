package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "homework")
data class HomeworkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolCode: String = "",
    val gradeClass: String = "",
    val subject: String = "",
    val title: String = "",
    val description: String = "",
    val assignedDate: String = "03/10/2026",
    val dueDate: String = "04/10/2026"
)
