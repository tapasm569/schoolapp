package com.school.manage

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.content.ClipData
import android.content.ClipboardManager
import android.app.AlertDialog
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
        try { val f = Class.forName("com.school.manage.core.utils.ProfilePhotoUtils_androidKt").getDeclaredField("appContextRef"); f.isAccessible = true; f.set(null, applicationContext) } catch (_: Exception) {}

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

        // Do not subscribe every installation to global topics. School-specific
        // subscriptions must be managed after authenticated school selection.
        // Do not log FCM registration tokens.
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "school_management.db"
        )
            .fallbackToDestructiveMigration().build()

        setContent {
            AppNavHost(database = database)
        }
    }
}
