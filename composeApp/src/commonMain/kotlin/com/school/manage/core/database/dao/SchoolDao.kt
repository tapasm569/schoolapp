package com.school.manage.core.database.dao

import androidx.room.*
import com.school.manage.core.database.entity.SchoolEntity

@Dao
interface SchoolDao {
    @Query("SELECT * FROM schools WHERE schoolCode = :code AND password = :password LIMIT 1")
    suspend fun loginSchool(code: String, password: String): SchoolEntity?

    @Query("SELECT * FROM schools WHERE schoolCode = :code LIMIT 1")
    suspend fun getSchoolByCode(code: String): SchoolEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun registerSchool(school: SchoolEntity): Long
}
