package com.school.manage

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.school.manage.core.database.AppDatabase
import com.school.manage.presentation.navigation.AppNavHost

@Composable
fun App(database: AppDatabase) {
    MaterialTheme {
        AppNavHost(database = database)
    }
}
