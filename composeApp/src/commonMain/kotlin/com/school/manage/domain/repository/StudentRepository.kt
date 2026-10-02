package com.school.manage.domain.repository

import com.school.manage.domain.model.Student
import kotlinx.coroutines.flow.Flow

interface StudentRepository {
    fun getStudents(): Flow<List<Student>>
    suspend fun getStudentById(id: Long): Student?
    suspend fun saveStudent(student: Student): Long
    suspend fun removeStudent(student: Student)
}
