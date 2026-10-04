package com.school.manage

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.room.Room
import com.school.manage.core.database.AppDatabase
import com.school.manage.presentation.navigation.AppNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

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
