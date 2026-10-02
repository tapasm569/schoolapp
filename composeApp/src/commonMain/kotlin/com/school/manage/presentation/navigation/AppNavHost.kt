package com.school.manage.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.school.manage.core.database.AppDatabase
import com.school.manage.presentation.screens.*

@Composable
fun AppNavHost(database: AppDatabase) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(database = database, onNavigate = { route -> navController.navigate(route) })
        }
        composable(Screen.StudentList.route) {
            StudentListScreen(
                database = database,
                onNavigateBack = { navController.popBackStack() },
                onNavigateAdd = { navController.navigate(Screen.AddStudent.route) }
            )
        }
        composable(Screen.AddStudent.route) {
            AddStudentScreen(database = database, onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Attendance.route) {
            AttendanceScreen(database = database, onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.FeeCollection.route) {
            FeeCollectionScreen(database = database, onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.ExpenseList.route) {
            ExpenseScreen(database = database, onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.StaffList.route) {
            StaffScreen(database = database, onNavigateBack = { navController.popBackStack() })
        }
    }
}
