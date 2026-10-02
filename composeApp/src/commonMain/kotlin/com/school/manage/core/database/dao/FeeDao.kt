package com.school.manage.core.database.dao

import androidx.room.*
import com.school.manage.core.database.entity.FeeRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeeDao {
    @Query("SELECT * FROM fee_records ORDER BY id DESC")
    fun getAllFeeRecords(): Flow<List<FeeRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeeRecord(record: FeeRecordEntity): Long
}
