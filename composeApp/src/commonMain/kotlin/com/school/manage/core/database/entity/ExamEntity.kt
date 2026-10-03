package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolCode: String = "",
    val title: String = "",
    val gradeClass: String = "",
    val subject: String = "",
    val examDate: String = "03/10/2026",
    val maxMarks: Double = 100.0
)

@Entity(tableName = "exam_marks", primaryKeys = ["examId", "studentId"])
data class ExamMarksEntity(
    val examId: Long = 0L,
    val studentId: Long = 0L,
    val studentName: String = "",
    val marksObtained: Double = 0.0,
    val grade: String = "A"
)
