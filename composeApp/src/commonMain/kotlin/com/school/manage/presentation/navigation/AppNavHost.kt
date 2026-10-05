package com.school.manage.presentation.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.SessionEntity
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.school.manage.presentation.screens.*
import com.school.manage.presentation.screens.QuestionBankScreen
import com.school.manage.presentation.screens.OnlineClassesScreen
import com.school.manage.presentation.screens.TimetableScreen
import com.school.manage.presentation.screens.LeaveManagementScreen
import com.school.manage.presentation.theme.SchoolAppTheme

@Composable
fun AppNavHost(database: AppDatabase) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    var isDarkTheme by remember { mutableStateOf(false) }

    var currentSchoolCode by remember { mutableStateOf("") }
    var currentSchoolName by remember { mutableStateOf("") }
    var currentStudentId by remember { mutableStateOf(0L) }
    var currentStaffId by remember { mutableStateOf(0L) }
    var initialRoute by remember { mutableStateOf<String?>(null) }

    // Restore saved session from Room (persistent until cleared app data)
    LaunchedEffect(Unit) {
        val session = database.sessionDao().getActiveSession()
        if (session != null) {
            currentSchoolCode = session.schoolCode
            currentSchoolName = session.schoolName
            currentStaffId = session.staffId
            currentStudentId = session.studentId
            when (session.role) {
                "ADMIN" -> initialRoute = "dashboard"
                "STAFF" -> initialRoute = "staff_portal"
                "STUDENT" -> initialRoute = "student_portal"
                else -> initialRoute = "login"
            }
        } else {
            initialRoute = "login"
        }
    }

    if (initialRoute == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    SchoolAppTheme(
        isDark = isDarkTheme,
        onToggleTheme = { isDarkTheme = !isDarkTheme }
    ) {
        NavHost(navController = navController, startDestination = initialRoute!!) {
            composable("login") {
                LoginScreen(
                    database = database,
                    onSchoolLoginSuccess = { code, name ->
                        currentSchoolCode = code
                        currentSchoolName = name
                        scope.launch {
                            database.sessionDao().saveSession(
                                SessionEntity(
                                    role = "ADMIN",
                                    schoolCode = code,
                                    schoolName = name
                                )
                            )
                        }
                        navController.navigate("dashboard") { popUpTo("login") { inclusive = true } }
                    },
                    onStaffLoginSuccess = { staffId, code, name, _ ->
                        currentStaffId = staffId
                        currentSchoolCode = code
                        currentSchoolName = name
                        scope.launch {
                            database.sessionDao().saveSession(
                                SessionEntity(
                                    role = "STAFF",
                                    schoolCode = code,
                                    schoolName = name,
                                    staffId = staffId
                                )
                            )
                        }
                        navController.navigate("staff_portal") { popUpTo("login") { inclusive = true } }
                    },
                    onStudentLoginSuccess = { studentId ->
                        currentStudentId = studentId
                        scope.launch {
                            database.sessionDao().saveSession(
                                SessionEntity(
                                    role = "STUDENT",
                                    studentId = studentId
                                )
                            )
                        }
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
                    onLogout = {
                        scope.launch { database.sessionDao().clearSession() }
                        navController.navigate("login") { popUpTo(0) }
                    }
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
            composable("exams") {
                ExamsScreen(
                    database = database,
                    schoolCode = currentSchoolCode,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("birthdays") {
                BirthdaysScreen(
                    database = database,
                    schoolCode = currentSchoolCode,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("homework") {
                HomeworkScreen(
                    database = database,
                    schoolCode = currentSchoolCode,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("classwork") {
                ClassworkScreen(
                    database = database,
                    schoolCode = currentSchoolCode,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("enquiry") {
                EnquiryScreen(
                    database = database,
                    schoolCode = currentSchoolCode,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateAddStudent = { navController.navigate("add_student") }
                )
            }
            composable("staff_logs") {
                StaffLogsScreen(
                    database = database,
                    schoolCode = currentSchoolCode,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("announcements") {
                AnnouncementsScreen(
                    database = database,
                    schoolCode = currentSchoolCode,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("messages") {
                MessagesScreen(
                    database = database,
                    schoolCode = currentSchoolCode,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("leave_management") {
                LeaveManagementScreen(
                    database = database,
                    schoolCode = currentSchoolCode,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("timetable") {
                TimetableScreen(
                    database = database,
                    schoolCode = currentSchoolCode,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("online_classes") {
                OnlineClassesScreen(
                    database = database,
                    schoolCode = currentSchoolCode,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("question_bank") {
                QuestionBankScreen(
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
                    onLogout = {
                        scope.launch { database.sessionDao().clearSession() }
                        navController.navigate("login") { popUpTo(0) }
                    }
                )
            }
            composable("student_portal") {
                StudentPortalScreen(
                    database = database,
                    studentId = currentStudentId,
                    onNavigate = { route -> navController.navigate(route) },
                    onLogout = {
                        scope.launch { database.sessionDao().clearSession() }
                        navController.navigate("login") { popUpTo(0) }
                    }
                )
            }
        }
    }
}
