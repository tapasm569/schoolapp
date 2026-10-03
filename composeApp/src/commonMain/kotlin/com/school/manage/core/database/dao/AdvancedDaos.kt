package com.school.manage.core.database.dao

import androidx.room.*
import com.school.manage.core.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaveDao {
    @Query("SELECT * FROM leave_requests WHERE schoolCode = :schoolCode ORDER BY id DESC")
    fun getLeavesBySchool(schoolCode: String): Flow<List<LeaveRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeave(leave: LeaveRequestEntity): Long

    @Delete
    suspend fun deleteLeave(leave: LeaveRequestEntity)
}

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetables WHERE schoolCode = :schoolCode ORDER BY periodNo ASC")
    fun getTimetableBySchool(schoolCode: String): Flow<List<TimetableEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetable(entry: TimetableEntity): Long

    @Delete
    suspend fun deleteTimetable(entry: TimetableEntity)
}

@Dao
interface OnlineClassDao {
    @Query("SELECT * FROM online_classes WHERE schoolCode = :schoolCode ORDER BY id DESC")
    fun getOnlineClassesBySchool(schoolCode: String): Flow<List<OnlineClassEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOnlineClass(onlineClass: OnlineClassEntity): Long

    @Delete
    suspend fun deleteOnlineClass(onlineClass: OnlineClassEntity)
}

@Dao
interface QuestionBankDao {
    @Query("SELECT * FROM question_bank WHERE schoolCode = :schoolCode ORDER BY id DESC")
    fun getQuestionsBySchool(schoolCode: String): Flow<List<QuestionBankEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionBankEntity): Long

    @Delete
    suspend fun deleteQuestion(question: QuestionBankEntity)
}
