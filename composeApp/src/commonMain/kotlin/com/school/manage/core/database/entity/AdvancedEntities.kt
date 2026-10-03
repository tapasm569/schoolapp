package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leave_requests")
data class LeaveRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolCode: String = "",
    val applicantName: String = "",
    val applicantType: String = "STUDENT",
    val startDate: String = "03/10/2026",
    val endDate: String = "04/10/2026",
    val reason: String = "",
    val status: String = "PENDING"
)

@Entity(tableName = "timetables")
data class TimetableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolCode: String = "",
    val gradeClass: String = "",
    val dayOfWeek: String = "Monday",
    val periodNo: Int = 1,
    val timeSlot: String = "09:00 AM - 09:45 AM",
    val subject: String = "",
    val teacherName: String = ""
)

@Entity(tableName = "online_classes")
data class OnlineClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolCode: String = "",
    val gradeClass: String = "",
    val subject: String = "",
    val title: String = "",
    val meetingUrl: String = "",
    val classDate: String = "03/10/2026",
    val classTime: String = "10:00 AM"
)

@Entity(tableName = "question_bank")
data class QuestionBankEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolCode: String = "",
    val gradeClass: String = "",
    val subject: String = "",
    val chapterTopic: String = "",
    val questionText: String = "",
    val answerKey: String = "",
    val questionType: String = "SHORT"
)
