package com.school.manage.core.database.dao

import androidx.room.*
import com.school.manage.core.database.entity.HomeworkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeworkDao {
    @Query("SELECT * FROM homework WHERE schoolCode = :schoolCode ORDER BY id DESC")
    fun getHomeworkBySchool(schoolCode: String): Flow<List<HomeworkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomework(homework: HomeworkEntity): Long

    @Delete
    suspend fun deleteHomework(homework: HomeworkEntity)
}
