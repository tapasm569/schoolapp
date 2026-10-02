package com.school.manage.core.database.dao

import androidx.room.*
import com.school.manage.core.database.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE schoolCode = :schoolCode AND date = :date")
    fun getAttendanceByDate(schoolCode: String, date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId ORDER BY date DESC")
    fun getAttendanceByStudent(studentId: Long): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<AttendanceEntity>)
}
