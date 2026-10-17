package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "batches")
data class BatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolCode: String = "",
    val batchName: String = "",
    val subjects: String = "",
    val sections: String = "",
    val feeTemplateRaw: String = "",
    val defaultMonthlyTuition: Double = 0.0,
    val collectTuitionAtAdmission: Boolean = true
)
