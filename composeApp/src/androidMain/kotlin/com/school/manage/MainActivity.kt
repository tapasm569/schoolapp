package com.school.manage

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.google.firebase.FirebaseApp
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import com.school.manage.core.database.AppDatabase
import com.school.manage.presentation.navigation.AppNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            FirebaseApp.initializeApp(this)
            Firebase.initialize(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "school_announcements_channel",
                "School Announcements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notices, holiday alerts, and fee reminders"
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all_schools")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
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
