package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fee_records")
data class FeeRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolCode: String,
    val studentId: Long,
    val studentName: String,
    val gradeClass: String,
    val amountPaid: Double,
    val paymentDate: String,
    val paymentMode: String,
    val remarks: String
)
