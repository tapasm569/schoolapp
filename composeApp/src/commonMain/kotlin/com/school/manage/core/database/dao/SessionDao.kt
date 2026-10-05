package com.school.manage.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.school.manage.core.database.entity.SessionEntity

@Dao
interface SessionDao {
    @Query("SELECT * FROM app_session WHERE id = 1 LIMIT 1")
    suspend fun getActiveSession(): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: SessionEntity)

    @Query("DELETE FROM app_session")
    suspend fun clearSession()
}
