package com.school.manage.core.database.dao

import androidx.room.*
import com.school.manage.core.database.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE schoolCode = :schoolCode ORDER BY id DESC")
    fun getStudentsBySchool(schoolCode: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudentById(id: Long): StudentEntity?

    @Query("SELECT * FROM students WHERE phone = :phone LIMIT 1")
    suspend fun getStudentByPhone(phone: String): StudentEntity?

    @Query("SELECT * FROM students WHERE phone = :phone LIMIT 1")
    suspend fun loginStudent(phone: String): StudentEntity?

    @Query("SELECT * FROM students WHERE schoolCode = :schoolCode AND phone = :phone LIMIT 1")
    suspend fun loginStudent(schoolCode: String, phone: String): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    @Delete
    suspend fun deleteStudent(student: StudentEntity)

    @Delete
    suspend fun delete(student: StudentEntity)
}
