package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "enquiries")
data class EnquiryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolCode: String = "",
    val studentName: String = "",
    val parentName: String = "",
    val phone: String = "",
    val gradeClass: String = "",
    val date: String = "03/10/2026",
    val status: String = "NEW",
    val notes: String = ""
)

@Entity(tableName = "staff_logs")
data class StaffLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolCode: String = "",
    val staffId: Long = 0L,
    val staffName: String = "",
    val date: String = "03/10/2026",
    val checkIn: String = "09:00 AM",
    val checkOut: String = "03:30 PM",
    val activityNote: String = "Classes conducted as scheduled",
    val status: String = "ON_TIME"
)

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolCode: String = "",
    val title: String = "",
    val message: String = "",
    val targetAudience: String = "ALL",
    val priority: String = "NORMAL",
    val date: String = "03/10/2026"
)
