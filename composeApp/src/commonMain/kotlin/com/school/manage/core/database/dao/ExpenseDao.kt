package com.school.manage.core.database.dao

import androidx.room.*
import com.school.manage.core.database.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE schoolCode = :schoolCode ORDER BY id DESC")
    fun getExpensesBySchool(schoolCode: String): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
}
