package com.school.manage.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.school.manage.core.database.dao.*
import com.school.manage.core.database.entity.*

@Database(
    entities = [
        SchoolEntity::class,
        StudentEntity::class,
        AttendanceEntity::class,
        FeeRecordEntity::class,
        ExpenseEntity::class,
        StaffEntity::class,
        BatchEntity::class,
        ExamEntity::class,
        ExamMarksEntity::class,
        HomeworkEntity::class,
        ClassworkEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun schoolDao(): SchoolDao
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun feeDao(): FeeDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun staffDao(): StaffDao
    abstract fun batchDao(): BatchDao
    abstract fun examDao(): ExamDao
    abstract fun homeworkDao(): HomeworkDao
    abstract fun classworkDao(): ClassworkDao
}
