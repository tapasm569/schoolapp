package com.school.manage.core.database.dao

import androidx.room.*
import com.school.manage.core.database.entity.StaffEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff WHERE schoolCode = :schoolCode ORDER BY id DESC")
    fun getStaffBySchool(schoolCode: String): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff WHERE id = :id LIMIT 1")
    suspend fun getStaffById(id: Long): StaffEntity?

    @Query("SELECT * FROM staff WHERE phone = :phone LIMIT 1")
    suspend fun getStaffByPhone(phone: String): StaffEntity?

    @Query("SELECT * FROM staff WHERE schoolCode = :schoolCode AND phone = :phone LIMIT 1")
    suspend fun loginStaffByPhone(schoolCode: String, phone: String): StaffEntity?

    @Query("SELECT * FROM staff WHERE schoolCode = :schoolCode AND phone = :phone AND password = :password LIMIT 1")
    suspend fun loginStaff(schoolCode: String, phone: String, password: String): StaffEntity?

    @Query("SELECT * FROM staff WHERE phone = :phone AND password = :password LIMIT 1")
    suspend fun loginStaff(phone: String, password: String): StaffEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: StaffEntity): Long

    @Delete
    suspend fun deleteStaff(staff: StaffEntity)
}
