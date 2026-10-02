package com.school.manage.presentation.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object StudentList : Screen("student_list")
    data object AddStudent : Screen("add_student")
    data object Attendance : Screen("attendance")
    data object FeeCollection : Screen("fee_collection")
    data object ExpenseList : Screen("expense_list")
    data object StaffList : Screen("staff_list")
}
