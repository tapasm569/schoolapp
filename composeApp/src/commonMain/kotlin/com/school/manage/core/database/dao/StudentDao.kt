package com.school.manage.core.database.dao

import androidx.room.*
import com.school.manage.core.database.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY gradeClass ASC, rollNo ASC")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE gradeClass = :gradeClass ORDER BY rollNo ASC")
    fun getStudentsByClass(gradeClass: String): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    @Delete
    suspend fun deleteStudent(student: StudentEntity)
}
