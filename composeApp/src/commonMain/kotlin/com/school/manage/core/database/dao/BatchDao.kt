package com.school.manage.core.database.dao

import androidx.room.*
import com.school.manage.core.database.entity.BatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BatchDao {
    @Query("SELECT * FROM batches WHERE schoolCode = :schoolCode ORDER BY id DESC")
    fun getBatchesBySchool(schoolCode: String): Flow<List<BatchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: BatchEntity): Long

    @Delete
    suspend fun deleteBatch(batch: BatchEntity)

    @Delete
    suspend fun delete(batch: BatchEntity)
}
