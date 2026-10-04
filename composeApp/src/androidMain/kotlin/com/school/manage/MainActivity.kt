package com.school.manage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.firebase.messaging.FirebaseMessaging
import com.school.manage.core.database.DatabaseDriverFactory
import com.school.manage.core.database.createAppDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Automatically subscribe to school broadcasts
        FirebaseMessaging.getInstance().subscribeToTopic("all_schools")

        val database = createAppDatabase(DatabaseDriverFactory(applicationContext))

        setContent {
            App(database)
        }
    }
}
