package com.school.manage.core.database.dao

import androidx.room.*
import com.school.manage.core.database.entity.AnnouncementEntity
import com.school.manage.core.database.entity.EnquiryEntity
import com.school.manage.core.database.entity.StaffLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EnquiryDao {
    @Query("SELECT * FROM enquiries WHERE schoolCode = :schoolCode ORDER BY id DESC")
    fun getEnquiriesBySchool(schoolCode: String): Flow<List<EnquiryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnquiry(enquiry: EnquiryEntity): Long

    @Delete
    suspend fun deleteEnquiry(enquiry: EnquiryEntity)
}

@Dao
interface StaffLogDao {
    @Query("SELECT * FROM staff_logs WHERE schoolCode = :schoolCode ORDER BY id DESC")
    fun getStaffLogsBySchool(schoolCode: String): Flow<List<StaffLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaffLog(log: StaffLogEntity): Long

    @Delete
    suspend fun deleteStaffLog(log: StaffLogEntity)
}

@Dao
interface AnnouncementDao {
    @Query("SELECT * FROM announcements WHERE schoolCode = :schoolCode ORDER BY id DESC")
    fun getAnnouncementsBySchool(schoolCode: String): Flow<List<AnnouncementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: AnnouncementEntity): Long

    @Delete
    suspend fun deleteAnnouncement(announcement: AnnouncementEntity)
}
