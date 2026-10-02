package com.school.manage.core.database.dao

import androidx.room.*
import com.school.manage.core.database.entity.FeeRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeeDao {
    @Query("SELECT * FROM fee_records WHERE schoolCode = :schoolCode ORDER BY id DESC")
    fun getFeeRecordsBySchool(schoolCode: String): Flow<List<FeeRecordEntity>>

    @Query("SELECT * FROM fee_records WHERE studentId = :studentId ORDER BY id DESC")
    fun getFeeRecordsByStudent(studentId: Long): Flow<List<FeeRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeeRecord(record: FeeRecordEntity): Long
}
