package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // Salary, Utilities, Maintenance, Books, Miscellaneous
    val amount: Double,
    val date: String,
    val notes: String
)
