package com.school.manage.presentation.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.school.manage.core.database.AppDatabase
import com.school.manage.presentation.screens.*
import com.school.manage.presentation.theme.SchoolAppTheme

@Composable
fun AppNavHost(database: AppDatabase) {
    val navController = rememberNavController()
    var isDarkTheme by remember { mutableStateOf(false) }

    var currentSchoolCode by remember { mutableStateOf("") }
    var currentSchoolName by remember { mutableStateOf("") }
    var currentStudentId by remember { mutableStateOf(0L) }
    var currentStaffId by remember { mutableStateOf(0L) }

    SchoolAppTheme(
        isDark = isDarkTheme,
        onToggleTheme = { isDarkTheme = !isDarkTheme }
    ) {
        NavHost(navController = navController, startDestination = "login") {
            composable("login") {
                LoginScreen(
                    database = database,
                    onSchoolLoginSuccess = { code, name ->
                        currentSchoolCode = code
                        currentSchoolName = name
                        navController.navigate("dashboard") { popUpTo("login") { inclusive = true } }
                    },
                    onStaffLoginSuccess = { staffId, code, _, _ ->
                        currentStaffId = staffId
                        currentSchoolCode = code
                        navController.navigate("staff_portal") { popUpTo("login") { inclusive = true } }
                    },
                    onStudentLoginSuccess = { studentId ->
                        currentStudentId = studentId
                        navController.navigate("student_portal") { popUpTo("login") { inclusive = true } }
                    },
                    onNavigateRegister = { navController.navigate("register_school") }
                )
            }
            composable("register_school") {
                RegisterSchoolScreen(
                    database = database,
                    onRegisterSuccess = { navController.popBackStack() },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("dashboard") {
                DashboardScreen(
                    database = database,
                    schoolCode = currentSchoolCode,
                    schoolName = currentSchoolName,
                    onNavigate = { route -> navController.navigate(route) },
                    onLogout = { navController.navigate("login") { popUpTo(0) } }
                )
            }
            composable("batch_list") {
                BatchListScreen(
                    database = database,
                    schoolCode = currentSchoolCode,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateAdd = { navController.navigate("add_batch") }
                )
            }
            composable("add_batch") {
                AddBatchScreen(
                    database = database,
                    schoolCode = currentSchoolCode,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("student_list") {
                StudentListScreen(
                    database = database,
                    schoolCode = currentSchoolCode,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateAdd = { navController.navigate("add_student") }
                )
            }
            composable("add_student") {
                AddStudentScreen(
                    database = database,
                    schoolCode = currentSchoolCode,
                    onNavigateBack = { navController.popBackStack() },
                    onStudentSaved = { sId, sName ->
                        navController.navigate("assign_batch/" + sId + "/" + sName)
                    }
                )
            }
            composable(
                route = "assign_batch/{studentId}/{studentName}",
                arguments = listOf(
                    navArgument("studentId") { type = NavType.LongType },
                    navArgument("studentName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val sId = backStackEntry.arguments?.getLong("studentId") ?: 0L
                val sName = backStackEntry.arguments?.getString("studentName") ?: "Student"
                AssignBatchScreen(
                    database = database,
                    schoolCode = currentSchoolCode,
                    studentId = sId,
                    studentName = sName,
                    onBatchAssigned = {
                        if (!navController.popBackStack("student_list", inclusive = false)) {
                            navController.navigate("dashboard") {
                                popUpTo("dashboard") { inclusive = true }
                            }
                        }
                    }
                )
            }
            composable("staff_list") {
                StaffScreen(
                    database = database,
                    schoolCode = currentSchoolCode,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("add_staff") {
                AddStaffScreen(
                    database = database,
                    schoolCode = currentSchoolCode,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("attendance") {
                AttendanceScreen(
                    database = database,
                    schoolCode = currentSchoolCode,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("fee_collection") {
                FeeCollectionScreen(
                    database = database,
                    schoolCode = currentSchoolCode,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("expense_list") {
                ExpenseScreen(
                    database = database,
                    schoolCode = currentSchoolCode,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("staff_portal") {
                StaffPortalScreen(
                    database = database,
                    staffId = currentStaffId,
                    schoolCode = currentSchoolCode,
                    onNavigate = { route -> navController.navigate(route) },
                    onLogout = { navController.navigate("login") { popUpTo(0) } }
                )
            }
            composable("student_portal") {
                StudentPortalScreen(
                    database = database,
                    studentId = currentStudentId,
                    onLogout = { navController.navigate("login") { popUpTo(0) } }
                )
            }
        }
    }
}
