package com.school.manage.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    onStudentLoginSuccess: (Long) -> Unit,
    onNavigateRegister: () -> Unit
) {
    var activeLoginRole by remember { mutableStateOf<String?>(null) } // "Admin", "Staff", "Student"
    var selectedLanguage by remember { mutableStateOf("English") }
    var showLangMenu by remember { mutableStateOf(false) }

    // Login Form State
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

            // 4. Section Title & Subtitle
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

            // 5. Role Card: Admin
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

            // 6. Role Card: Staff
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

            // 7. Role Card: Student
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

            // 8. "OR" Divider
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

            // 9. Card: Create Admin Account
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

        // Login Modal Dialog when tapping any Role
        if (activeLoginRole != null) {
            AlertDialog(
                onDismissRequest = { activeLoginRole = null },
                title = {
                    Text(
                        text = if (activeLoginRole == "Student") "Student Login" else "$activeLoginRole Login",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (activeLoginRole == "Student") {
                                "Enter your School Code and registered mobile number."
                            } else {
                                "Enter your School Code and Password to access your portal."
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
                            label = { Text(if (activeLoginRole == "Student") "Registered Mobile Number" else "Password") },
                            visualTransformation = if (activeLoginRole == "Student") androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
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
                                if (activeLoginRole == "Student") {
                                    val student = database.studentDao().loginStudent(schoolCode.trim(), passwordOrPhone.trim())
                                    if (student != null) {
                                        val id = student.id
                                        activeLoginRole = null
                                        onStudentLoginSuccess(id)
                                    } else {
                                        errorMessage = "Invalid School Code or Mobile Number"
                                    }
                                } else {
                                    // Admin & Staff Authentication
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
            // Left Illustration Badge
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

            // Right Text Column
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
