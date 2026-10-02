package com.school.manage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.room.Room
import com.school.manage.core.database.AppDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "school_operations.db"
        ).fallbackToDestructiveMigration().build()

        setContent {
            App(database = database)
        }
    }
}
