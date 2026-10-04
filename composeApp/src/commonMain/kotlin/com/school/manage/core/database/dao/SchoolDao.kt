package com.school.manage.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.school.manage.core.database.entity.SchoolEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchool(school: SchoolEntity)

    @Query("SELECT * FROM schools WHERE schoolCode = :schoolCode LIMIT 1")
    suspend fun getSchoolByCode(schoolCode: String): SchoolEntity?

    @Query("SELECT * FROM schools")
    fun getAllSchools(): Flow<List<SchoolEntity>>
}
