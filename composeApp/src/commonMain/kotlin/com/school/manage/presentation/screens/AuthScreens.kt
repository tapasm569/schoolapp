package com.school.manage.presentation.screens
import com.school.manage.core.firebase.FirestoreSyncService

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.SchoolEntity
import kotlinx.coroutines.launch

@Composable
fun AppIconLogoBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(80.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1E3A8A), Color(0xFF2563EB))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(56.dp)) {
            val w = size.width
            val h = size.height

            // 1. Mortarboard Diamond Top
            val capDiamond = Path().apply {
                moveTo(w * 0.50f, h * 0.16f)
                lineTo(w * 0.88f, h * 0.33f)
                lineTo(w * 0.50f, h * 0.50f)
                lineTo(w * 0.12f, h * 0.33f)
                close()
            }
            drawPath(capDiamond, color = Color.White)

            // 2. Cap Skullcap Base
            val capBase = Path().apply {
                moveTo(w * 0.32f, h * 0.40f)
                lineTo(w * 0.32f, h * 0.52f)
                quadraticTo(w * 0.50f, h * 0.63f, w * 0.68f, h * 0.52f)
                lineTo(w * 0.68f, h * 0.40f)
                close()
            }
            drawPath(capBase, color = Color(0xFFE2E8F0))

            // 3. Gold Button on Cap Top
            drawCircle(
                color = Color(0xFFF59E0B),
                radius = w * 0.045f,
                center = Offset(w * 0.50f, h * 0.33f)
            )

            // 4. Gold Tassel Cord & Hanging Ribbon
            drawLine(
                color = Color(0xFFF59E0B),
                start = Offset(w * 0.50f, h * 0.33f),
                end = Offset(w * 0.80f, h * 0.48f),
                strokeWidth = 3f
            )
            drawCircle(
                color = Color(0xFFF59E0B),
                radius = w * 0.035f,
                center = Offset(w * 0.80f, h * 0.52f)
            )

            // 5. Open Knowledge Book - Left Page
            val leftPage = Path().apply {
                moveTo(w * 0.48f, h * 0.65f)
                lineTo(w * 0.18f, h * 0.62f)
                lineTo(w * 0.18f, h * 0.81f)
                lineTo(w * 0.48f, h * 0.85f)
                close()
            }
            drawPath(leftPage, color = Color.White)

            // 6. Open Knowledge Book - Right Page
            val rightPage = Path().apply {
                moveTo(w * 0.52f, h * 0.65f)
                lineTo(w * 0.82f, h * 0.62f)
                lineTo(w * 0.82f, h * 0.81f)
                lineTo(w * 0.52f, h * 0.85f)
                close()
            }
            drawPath(rightPage, color = Color(0xFFF8FAFC))

            // 7. Gold Book Spine
            drawLine(
                color = Color(0xFFF59E0B),
                start = Offset(w * 0.50f, h * 0.64f),
                end = Offset(w * 0.50f, h * 0.87f),
                strokeWidth = 3.5f
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    database: AppDatabase,
    onSchoolLoginSuccess: (String, String) -> Unit,
    onStaffLoginSuccess: (Long, String, String, String) -> Unit,
    onStudentLoginSuccess: (Long) -> Unit,
    onNavigateRegister: () -> Unit
) {
    var activeLoginRole by remember { mutableStateOf<String?>(null) }
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
            item { Spacer(modifier = Modifier.height(20.dp)) }

            // 1. App Icon Logo Badge
            item {
                AppIconLogoBadge()
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }

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

            item { Spacer(modifier = Modifier.height(28.dp)) }

            // 4. Role Selection Header
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

            // 5. Admin Role Card
            item {
                RoleSelectionCard(
                    title = "Login as Admin",
                    subtitle = "Control all administrative tasks and monitor operations.",
                    iconBadgeColor = Color(0xFFE0E7FF),
                    emoji = "👨‍‍💼",
                    onClick = {
                        schoolCode = ""
                        passwordOrPhone = ""
                        errorMessage = ""
                        activeLoginRole = "Admin"
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }

            // 6. Staff Role Card
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

            // 7. Student Role Card
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

        // Login Dialog Modal
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
