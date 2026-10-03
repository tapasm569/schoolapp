package com.school.manage.core.database.dao

import androidx.room.*
import com.school.manage.core.database.entity.ClassworkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassworkDao {
    @Query("SELECT * FROM classwork WHERE schoolCode = :schoolCode ORDER BY id DESC")
    fun getClassworkBySchool(schoolCode: String): Flow<List<ClassworkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClasswork(classwork: ClassworkEntity): Long

    @Delete
    suspend fun deleteClasswork(classwork: ClassworkEntity)
}
