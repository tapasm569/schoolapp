package com.school.manage.core.database.dao

import androidx.room.*
import com.school.manage.core.database.entity.StaffEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff WHERE schoolCode = :schoolCode ORDER BY name ASC")
    fun getStaffBySchool(schoolCode: String): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff WHERE schoolCode = :schoolCode AND phone = :phone LIMIT 1")
    suspend fun loginStaff(schoolCode: String, phone: String): StaffEntity?

    @Query("SELECT * FROM staff WHERE id = :id")
    suspend fun getStaffById(id: Long): StaffEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: StaffEntity): Long

    @Delete
    suspend fun deleteStaff(staff: StaffEntity)
}
