#!/bin/bash
set -e

echo "Updating Staff Portal and Navigation..."

# -------------------------------------------------------------
# 1. UPDATE STAFF DAO (Add staff login query)
# -------------------------------------------------------------
cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/core/database/dao/StaffDao.kt
package com.school.manage.core.database.dao

import androidx.room.*
import com.school.manage.core.database.entity.StaffEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff WHERE schoolCode = :schoolCode ORDER BY name ASC")
    fun getStaffBySchool(schoolCode: String): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff WHERE schoolCode = :schoolCode AND phone = :phone LIMIT 1")
    suspend fun loginStaff(schoolCode: String, phone: String): StaffEntity?

    @Query("SELECT * FROM staff WHERE id = :id")
    suspend fun getStaffById(id: Long): StaffEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: StaffEntity): Long

    @Delete
    suspend fun deleteStaff(staff: StaffEntity)
}
EOF

# -------------------------------------------------------------
# 2. UPDATE LOGIN SCREEN (Handle Staff Login Distinctly)
# -------------------------------------------------------------
cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/AuthScreens.kt
package com.school.manage.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.SchoolEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    database: AppDatabase,
    onSchoolLoginSuccess: (String, String) -> Unit,
    onStaffLoginSuccess: (Long, String, String, String) -> Unit,
    onStudentLoginSuccess: (Long) -> Unit,
    onNavigateRegister: () -> Unit
) {
    var activeLoginRole by remember { mutableStateOf<String?>(null) } // "Admin", "Staff", "Student"
    var selectedLanguage by remember { mutableStateOf("English") }
    var showLangMenu by remember { mutableStateOf(false) }

    var schoolCode by remember { mutableStateOf("") }
    var passwordOrPhone by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // 1. Top Logo Badge
            item {
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(65.dp)
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 6.dp, topEnd = 6.dp))
                        .background(Color(0xFFFFC837)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.width(14.dp).height(3.dp).background(Color(0xFF0F172A)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "tuFee",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(modifier = Modifier.width(14.dp).height(3.dp).background(Color(0xFF0F172A)))
                        }
                        Text(
                            text = "Online",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(28.dp)) }

            // 2. Headline
            item {
                Text(
                    text = "Simplify your life and boost\nyour productivity 🚀",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    lineHeight = 30.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF1E293B)
                )
            }

            item { Spacer(modifier = Modifier.height(18.dp)) }

            // 3. Language Selector Pill
            item {
                Box {
                    Surface(
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        color = Color.White,
                        modifier = Modifier.clickable { showLangMenu = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("文A", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Language: $selectedLanguage", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("▾", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                    }

                    DropdownMenu(
                        expanded = showLangMenu,
                        onDismissRequest = { showLangMenu = false }
                    ) {
                        listOf("English", "Hindi", "Bengali").forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang) },
                                onClick = {
                                    selectedLanguage = lang
                                    showLangMenu = false
                                }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(30.dp)) }

            // 4. Section Header
            item {
                Text(
                    text = "Select role",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Please choose your role to continue. Select the role that best describes your position within the organization.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // 5. Admin Card
            item {
                RoleSelectionCard(
                    title = "Login as Admin",
                    subtitle = "Control all administrative tasks and monitor operations.",
                    iconBadgeColor = Color(0xFFE0E7FF),
                    emoji = "👨‍💼",
                    onClick = {
                        schoolCode = ""
                        passwordOrPhone = ""
                        errorMessage = ""
                        activeLoginRole = "Admin"
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }

            // 6. Staff Card
            item {
                RoleSelectionCard(
                    title = "Login as Staff",
                    subtitle = "Manage classes, communicate with students, and track progress.",
                    iconBadgeColor = Color(0xFFDCFCE7),
                    emoji = "👩‍🏫",
                    onClick = {
                        schoolCode = ""
                        passwordOrPhone = ""
                        errorMessage = ""
                        activeLoginRole = "Staff"
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }

            // 7. Student Card
            item {
                RoleSelectionCard(
                    title = "Login as Student",
                    subtitle = "View your classes, fees, homework and more.",
                    iconBadgeColor = Color(0xFFFEF3C7),
                    emoji = "🎒",
                    onClick = {
                        schoolCode = ""
                        passwordOrPhone = ""
                        errorMessage = ""
                        activeLoginRole = "Student"
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(26.dp)) }

            // 8. Divider
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFCBD5E1))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF94A3B8)),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                        Text(
                            text = "OR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp)
                        )
                    }
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFCBD5E1))
                }
            }

            item { Spacer(modifier = Modifier.height(26.dp)) }

            // 9. Create Admin Account Card
            item {
                RoleSelectionCard(
                    title = "Create Admin Account",
                    subtitle = "New here? Step up and create your Admin Account to unlock endless possibilities!",
                    iconBadgeColor = Color(0xFFF1F5F9),
                    emoji = "🏫",
                    onClick = onNavigateRegister
                )
            }

            item { Spacer(modifier = Modifier.height(36.dp)) }
        }

        // Login Modal Dialog
        if (activeLoginRole != null) {
            val role = activeLoginRole!!
            val isStudent = role == "Student"
            val isStaff = role == "Staff"

            AlertDialog(
                onDismissRequest = { activeLoginRole = null },
                title = {
                    Text(
                        text = if (isStudent) "Student Login" else if (isStaff) "Staff Portal Login" else "Admin Login",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = when {
                                isStudent -> "Enter your School Code and registered mobile number."
                                isStaff -> "Enter your School Code and faculty registered mobile number."
                                else -> "Enter your School Code and administrator password."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )

                        OutlinedTextField(
                            value = schoolCode,
                            onValueChange = { schoolCode = it.uppercase() },
                            label = { Text("School Code") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = passwordOrPhone,
                            onValueChange = { passwordOrPhone = it },
                            label = { Text(if (isStudent || isStaff) "Registered Mobile Number" else "Password") },
                            visualTransformation = if (isStudent || isStaff) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (errorMessage.isNotEmpty()) {
                            Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                when {
                                    isStudent -> {
                                        val student = database.studentDao().loginStudent(schoolCode.trim(), passwordOrPhone.trim())
                                        if (student != null) {
                                            val id = student.id
                                            activeLoginRole = null
                                            onStudentLoginSuccess(id)
                                        } else {
                                            errorMessage = "Invalid School Code or Mobile Number"
                                        }
                                    }
                                    isStaff -> {
                                        val staff = database.staffDao().loginStaff(schoolCode.trim(), passwordOrPhone.trim())
                                        if (staff != null) {
                                            val sId = staff.id
                                            val sCode = staff.schoolCode
                                            val sName = staff.name
                                            val sRole = staff.role
                                            activeLoginRole = null
                                            onStaffLoginSuccess(sId, sCode, sName, sRole)
                                        } else {
                                            errorMessage = "Invalid School Code or Staff Mobile Number"
                                        }
                                    }
                                    else -> {
                                        val school = database.schoolDao().loginSchool(schoolCode.trim(), passwordOrPhone)
                                        if (school != null) {
                                            val code = school.schoolCode
                                            val name = school.schoolName
                                            activeLoginRole = null
                                            onSchoolLoginSuccess(code, name)
                                        } else {
                                            errorMessage = "Invalid School Code or Password"
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Sign In")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeLoginRole = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun RoleSelectionCard(
    title: String,
    subtitle: String,
    iconBadgeColor: Color,
    emoji: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBadgeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 32.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterSchoolScreen(
    database: AppDatabase,
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var schoolName by remember { mutableStateOf("") }
    var schoolCode by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Register Your School", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = schoolName,
                onValueChange = { schoolName = it },
                label = { Text("School Full Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = schoolCode,
                onValueChange = { schoolCode = it.uppercase() },
                label = { Text("Create Unique School Code (e.g., SCH01)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Official Contact Number") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Admin Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMsg.isNotEmpty()) {
                Text(errorMsg, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                onClick = {
                    if (schoolName.isBlank() || schoolCode.isBlank() || password.isBlank()) {
                        errorMsg = "Please fill in all mandatory fields"
                        return@Button
                    }
                    scope.launch {
                        val existing = database.schoolDao().getSchoolByCode(schoolCode.trim())
                        if (existing != null) {
                            errorMsg = "School Code '$schoolCode' is already taken. Pick another code."
                        } else {
                            database.schoolDao().registerSchool(
                                SchoolEntity(
                                    schoolCode = schoolCode.trim(),
                                    schoolName = schoolName.trim(),
                                    password = password,
                                    phone = phone
                                )
                            )
                            onRegisterSuccess()
                        }
                    }
                }
            ) {
                Text("Complete School Registration")
            }
        }
    }
}
EOF

# -------------------------------------------------------------
# 3. CREATE DEDICATED MODERN STAFF PORTAL SCREEN
# -------------------------------------------------------------
cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/StaffPortalScreen.kt
package com.school.manage.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.StaffEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffPortalScreen(
    database: AppDatabase,
    staffId: Long,
    schoolCode: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    var staff by remember { mutableStateOf<StaffEntity?>(null) }
    val students by database.studentDao().getStudentsBySchool(schoolCode).collectAsState(initial = emptyList())
    val staffMembers by database.staffDao().getStaffBySchool(schoolCode).collectAsState(initial = emptyList())

    LaunchedEffect(staffId) {
        staff = database.staffDao().getStaffById(staffId)
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(10.dp)) }

            // 1. Top Header with Staff Greeting & Logout
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF059669)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (staff?.name ?: "S").take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 19.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Welcome,", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text(staff?.name ?: "Faculty Member", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                        }
                    }

                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Text("⎋", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE11D48))
                    }
                }
            }

            // 2. Faculty Identity Hero Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF059669))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FACULTY & STAFF CREDENTIALS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA7F3D0),
                                letterSpacing = 1.sp
                            )
                            Surface(shape = RoundedCornerShape(12.dp), color = Color(0x33FFFFFF)) {
                                Text(
                                    text = "School: $schoolCode",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(staff?.name ?: "", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Designation: ${staff?.role ?: "Staff"}  •  Contact: ${staff?.phone ?: "N/A"}",
                            fontSize = 13.sp,
                            color = Color(0xFFD1FAE5)
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0x33FFFFFF))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Joined: ${staff?.joinDate ?: "Active"}", fontSize = 12.sp, color = Color(0xFFA7F3D0))
                            Text("Salary: ₹${staff?.salary?.toInt() ?: 0}/mo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFDE047))
                        }
                    }
                }
            }

            // 3. Quick Stats Overview
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatMetricTile(
                        title = "Enrolled Students",
                        value = "${students.size}",
                        icon = "🎒",
                        bgTint = Color(0xFFEFF6FF),
                        textColor = Color(0xFF1D4ED8),
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricTile(
                        title = "School Faculty",
                        value = "${staffMembers.size}",
                        icon = "👥",
                        bgTint = Color(0xFFF0FDF4),
                        textColor = Color(0xFF15803D),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4. Staff Operations Title
            item {
                Text(
                    text = "Staff Operations & Tasks",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
            }

            // 5. Action Tiles Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ModernActionTile(
                            title = "Daily Attendance",
                            desc = "Mark student register",
                            emoji = "📋",
                            tint = Color(0xFFDCFCE7),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("attendance") }
                        )
                        ModernActionTile(
                            title = "Students Directory",
                            desc = "View classes & details",
                            emoji = "🎓",
                            tint = Color(0xFFE0E7FF),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("student_list") }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ModernActionTile(
                            title = "Fee Collection",
                            desc = "Record fee receipts",
                            emoji = "💳",
                            tint = Color(0xFFFEF3C7),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("fee_collection") }
                        )
                        ModernActionTile(
                            title = "School Expenses",
                            desc = "Log utility/item bills",
                            emoji = "🧾",
                            tint = Color(0xFFFFE4E6),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("expense_list") }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(26.dp)) }
        }
    }
}
EOF

# -------------------------------------------------------------
# 4. UPDATE APP NAV HOST (Connect Staff Portal Route)
# -------------------------------------------------------------
cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/presentation/navigation/AppNavHost.kt
package com.school.manage.presentation.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.school.manage.core.database.AppDatabase
import com.school.manage.presentation.screens.*

@Composable
fun AppNavHost(database: AppDatabase) {
    val navController = rememberNavController()
    var currentSchoolCode by remember { mutableStateOf("") }
    var currentSchoolName by remember { mutableStateOf("") }
    var currentStudentId by remember { mutableStateOf(0L) }
    var currentStaffId by remember { mutableStateOf(0L) }

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
        composable("staff_list") {
            StaffScreen(
                database = database,
                schoolCode = currentSchoolCode,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
EOF

# -------------------------------------------------------------
# 5. REBUILD ZIP ARCHIVE IN DOWNLOADS
# -------------------------------------------------------------
cd ..
zip -r "school-management-app.zip" "school-management-app" -x "*/.git/*"

if [ -d "/sdcard/Download" ]; then
    cp "school-management-app.zip" /sdcard/Download/
    echo "=========================================================="
    echo "SUCCESS: school-management-app.zip updated in Downloads!"
    echo "=========================================================="
fi
cd school-management-app
