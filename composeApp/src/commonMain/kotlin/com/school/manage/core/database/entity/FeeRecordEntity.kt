package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fee_records")
data class FeeRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolCode: String = "",
    val studentId: Long = 0L,
    val studentName: String = "",
    val gradeClass: String = "",
    val amountPaid: Double = 0.0,
    val paymentDate: String = "",
    val feeMonth: String = "",
    val paymentMode: String = "CASH",
    val remarks: String = ""
)
