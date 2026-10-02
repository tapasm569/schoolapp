package com.school.manage.data.repository

import com.school.manage.core.database.dao.StudentDao
import com.school.manage.core.database.entity.StudentEntity
import com.school.manage.domain.model.Student
import com.school.manage.domain.repository.StudentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StudentRepositoryImpl(
    private val studentDao: StudentDao
) : StudentRepository {

    override fun getStudents(): Flow<List<Student>> =
        studentDao.getStudentsBySchool("").map { list -> list.map { it.toDomain() } }

    override suspend fun getStudentById(id: Long): Student? =
        studentDao.getStudentById(id)?.toDomain()

    override suspend fun saveStudent(student: Student): Long =
        studentDao.insertStudent(student.toEntity())

    override suspend fun removeStudent(student: Student) =
        studentDao.deleteStudent(student.toEntity())

    private fun StudentEntity.toDomain() = Student(
        id = id,
        schoolCode = schoolCode,
        rollNo = rollNo,
        name = name,
        gradeClass = gradeClass,
        section = section,
        guardianName = guardianName,
        phone = phone,
        monthlyFee = monthlyFee,
        admissionDate = admissionDate
    )

    private fun Student.toEntity() = StudentEntity(
        id = id,
        schoolCode = schoolCode,
        rollNo = rollNo,
        name = name,
        gradeClass = gradeClass,
        section = section,
        guardianName = guardianName,
        phone = phone,
        monthlyFee = monthlyFee,
        admissionDate = admissionDate
    )
}
