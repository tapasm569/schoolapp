package com.school.manage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.room.Room
import com.school.manage.core.database.AppDatabase
import com.school.manage.presentation.navigation.AppNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all_schools") } catch (e: Exception) {}
        
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "school_management.db"
        )
            .fallbackToDestructiveMigration()
            .build()

        setContent {
            AppNavHost(database = database)
        }
    }
}
