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
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    android.util.Log.d("FCM_TEST", "Successfully subscribed to all_schools")
                } else {
                    android.util.Log.e("FCM_TEST", "Topic subscription failed", task.exception)
                }
            }

        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            runOnUiThread {
                if (!task.isSuccessful) {
                    val errorMsg = task.exception?.localizedMessage ?: "Unknown FCM Error"
                    android.util.Log.e("FCM_TEST", "Fetching FCM registration token failed: $errorMsg", task.exception)
                    AlertDialog.Builder(this)
                        .setTitle("FCM Error ⚠️️")
                        .setMessage("Failed to retrieve FCM token:\n$errorMsg\n\nPlease verify Google Play Services and internet connection.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@runOnUiThread
                }

                val token = task.result
                if (token.isNullOrEmpty()) {
                    AlertDialog.Builder(this)
                        .setTitle("FCM Warning ⚠️")
                        .setMessage("Token was empty. Please restart the app.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@runOnUiThread
                }

                android.util.Log.d("FCM_TEST", "FCM REGISTRATION TOKEN: $token")

                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = ClipData.newPlainText("FCM Token", token)
                clipboard?.setPrimaryClip(clip)

                AlertDialog.Builder(this)
                    .setTitle("FCM Token Copied! 🔔")
                    .setMessage("Your FCM token has been copied to your clipboard!\n\nToken starts with:\n${token.take(25)}...\n\nPaste this into Firebase Console -> 'Send test message'.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
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
