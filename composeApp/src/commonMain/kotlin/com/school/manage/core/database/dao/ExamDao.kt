package com.school.manage.core.database.dao

import androidx.room.*
import com.school.manage.core.database.entity.ExamEntity
import com.school.manage.core.database.entity.ExamMarksEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams WHERE schoolCode = :schoolCode ORDER BY id DESC")
    fun getExamsBySchool(schoolCode: String): Flow<List<ExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity): Long

    @Delete
    suspend fun deleteExam(exam: ExamEntity)

    @Query("SELECT * FROM exam_marks WHERE examId = :examId")
    fun getMarksByExam(examId: Long): Flow<List<ExamMarksEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarks(marks: List<ExamMarksEntity>)
}
