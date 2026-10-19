package com.school.manage

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class SchoolFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        android.util.Log.d("FCM_TEST", "NEW TOKEN RECEIVED: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        android.util.Log.d("FCM_TEST", "PUSH RECEIVED! from: ${remoteMessage.from}, data: ${remoteMessage.data}")

        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: "School Notice"

        val body = remoteMessage.notification?.body 
            ?: remoteMessage.data["message"] 
            ?: "You have a new announcement from your school."

        showSystemNotification(title, body)
    }

    private fun showSystemNotification(title: String, body: String) {
        val channelId = "school_announcements_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "School Announcements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notices, holiday alerts, and fee reminders"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
