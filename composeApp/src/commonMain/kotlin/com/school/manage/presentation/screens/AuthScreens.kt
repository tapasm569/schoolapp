package com.school.manage.presentation.screens

import com.school.manage.presentation.localization.AppLanguage
import com.school.manage.presentation.localization.EnglishStrings
import com.school.manage.presentation.localization.BengaliStrings
import com.school.manage.presentation.localization.HindiStrings
import com.school.manage.presentation.localization.LocalAppStrings
import androidx.compose.runtime.CompositionLocalProvider

import com.school.manage.presentation.theme.LocalSchoolColors
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import androidx.compose.material3.CircularProgressIndicator

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

            val capDiamond = Path().apply {
                moveTo(w * 0.50f, h * 0.16f)
                lineTo(w * 0.88f, h * 0.33f)
                lineTo(w * 0.50f, h * 0.50f)
                lineTo(w * 0.12f, h * 0.33f)
                close()
            }
            drawPath(capDiamond, color = Color.White)

            val capBase = Path().apply {
                moveTo(w * 0.32f, h * 0.40f)
                lineTo(w * 0.32f, h * 0.52f)
                quadraticTo(w * 0.50f, h * 0.63f, w * 0.68f, h * 0.52f)
                lineTo(w * 0.68f, h * 0.40f)
                close()
            }
            drawPath(capBase, color = Color(0xFFE2E8F0))

            drawCircle(
                color = Color(0xFFF59E0B),
                radius = w * 0.045f,
                center = Offset(w * 0.50f, h * 0.33f)
            )

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

            val leftPage = Path().apply {
                moveTo(w * 0.48f, h * 0.65f)
                lineTo(w * 0.18f, h * 0.62f)
                lineTo(w * 0.18f, h * 0.81f)
                lineTo(w * 0.48f, h * 0.85f)
                close()
            }
            drawPath(leftPage, color = Color.White)

            val rightPage = Path().apply {
                moveTo(w * 0.52f, h * 0.65f)
                lineTo(w * 0.82f, h * 0.62f)
                lineTo(w * 0.82f, h * 0.81f)
                lineTo(w * 0.52f, h * 0.85f)
                close()
            }
            drawPath(rightPage, color = Color(0xFFF8FAFC))

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

    val strings = when (selectedLanguage) {
        "Bengali" -> BengaliStrings
        "Hindi" -> HindiStrings
        else -> EnglishStrings
    }

    var schoolCode by remember { mutableStateOf("") }
    var passwordOrPhone by remember { mutableStateOf("") }
    var isLoggingIn by remember { mutableStateOf(false) }
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

            item { AppIconLogoBadge() }

            item { Spacer(modifier = Modifier.height(20.dp)) }

            item {
                Text(
                    text = strings.headline,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    lineHeight = 30.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF1E293B)
                )
            }

            item { Spacer(modifier = Modifier.height(18.dp)) }

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
                            val langDisplay = if (selectedLanguage == "Bengali") "বাংলা" else if (selectedLanguage == "Hindi") "हिन्दी" else "English"
                            Text("${strings.languageLabel}: $langDisplay", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("▾", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                    }

                    DropdownMenu(
                        expanded = showLangMenu,
                        onDismissRequest = { showLangMenu = false }
                    ) {
                        listOf(
                            Triple("English", "English", "English"),
                            Triple("Hindi", "Hindi", "हिन्दी"),
                            Triple("Bengali", "Bengali", "বাংলা")
                        ).forEach { (key, engLabel, nativeLabel) ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(nativeLabel, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("($engLabel)", fontSize = 12.sp, color = Color(0xFF64748B))
                                    }
                                },
                                onClick = {
                                    selectedLanguage = key
                                    showLangMenu = false
                                }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(28.dp)) }

            item {
                Text(
                    text = strings.selectRoleTitle,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strings.selectRoleDesc,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                RoleSelectionCard(
                    title = strings.loginAdminTitle,
                    subtitle = strings.loginAdminSubtitle,
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

            item {
                RoleSelectionCard(
                    title = strings.loginStaffTitle,
                    subtitle = strings.loginStaffSubtitle,
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

            item {
                RoleSelectionCard(
                    title = strings.loginStudentTitle,
                    subtitle = strings.loginStudentSubtitle,
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
                            text = strings.orDivider,
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

            item {
                RoleSelectionCard(
                    title = strings.createAdminTitle,
                    subtitle = strings.createAdminSubtitle,
                    iconBadgeColor = Color(0xFFF1F5F9),
                    emoji = "🏫",
                    onClick = onNavigateRegister
                )
            }

            item { Spacer(modifier = Modifier.height(36.dp)) }
        }

        if (activeLoginRole != null) {
            val role = activeLoginRole!!
            val isStudent = role == "Student"
            val isStaff = role == "Staff"

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(18.dp))

                    // 1. Header Navigation: Back
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                activeLoginRole = null
                                errorMessage = ""
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "←",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.back,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // 2. Centered Illustration Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier
                                .width(220.dp)
                                .fillMaxHeight()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isStudent) "🎒" else if (isStaff) "👨‍🏫" else "🏫",
                                    fontSize = 48.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF2563EB).copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = strings.portalAccess,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // 3. Headings
                    Text(
                        text = if (isStudent) "Login as Student" else if (isStaff) "Login as Staff" else "Login as Admin",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when {
                            isStudent -> "View your classes, fees, homework and more."
                            isStaff -> "Manage class registers, homework, and parent updates."
                            else -> "Access administrative controls and school analytics."
                        },
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // 4. Input Fields
                    OutlinedTextField(
                        value = schoolCode,
                        onValueChange = { schoolCode = it.uppercase() },
                        placeholder = { Text(strings.schoolCodeLabel, color = Color(0xFF94A3B8)) },
                        leadingIcon = {
                            Text("🏛️", fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp, end = 4.dp))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedBorderColor = Color(0xFF0284C7)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = passwordOrPhone,
                        onValueChange = { passwordOrPhone = it },
                        placeholder = {
                            Text(
                                text = if (isStudent || isStaff) strings.registeredPhoneLabel else strings.passwordLabel,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        leadingIcon = {
                            Text(
                                text = if (isStudent || isStaff) "📱" else "🔑",
                                fontSize = 16.sp,
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                            )
                        },
                        visualTransformation = if (isStudent || isStaff) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedBorderColor = Color(0xFF0284C7)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 5. Full-Width Sign In Button (Optimized Async)
                    Button(
                        enabled = !isLoggingIn,
                        onClick = {
                            if (isLoggingIn) return@Button
                            isLoggingIn = true
                            errorMessage = ""
                            scope.launch {
                                try {
                                    when {
                                        isStudent -> {
                                            val student = withContext(Dispatchers.IO) {
                                                database.studentDao().loginStudent(schoolCode.trim(), passwordOrPhone.trim())
                                            }
                                            if (student != null) {
                                                activeLoginRole = null
                                                onStudentLoginSuccess(student.id)
                                            } else {
                                                errorMessage = "Invalid School Code or Mobile Number"
                                            }
                                        }
                                        isStaff -> {
                                            var staff = withContext(Dispatchers.IO) {
                                                database.staffDao().loginStaffByPhone(schoolCode.trim(), passwordOrPhone.trim())
                                            }
                                            if (staff == null) {
                                                withContext(Dispatchers.IO) {
                                                    try {
                                                        FirestoreSyncService(database).restoreAllFromCloud(schoolCode.trim())
                                                        staff = database.staffDao().loginStaffByPhone(schoolCode.trim(), passwordOrPhone.trim())
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                            }
                                            if (staff != null) {
                                                activeLoginRole = null
                                                onStaffLoginSuccess(staff.id, staff.schoolCode, staff.name, staff.role)
                                            } else {
                                                errorMessage = "Invalid School Code or Staff Mobile Number"
                                            }
                                        }
                                        else -> {
                                            val sCode = schoolCode.trim()
                                            var school = withContext(Dispatchers.IO) {
                                                database.schoolDao().loginSchool(sCode, passwordOrPhone)
                                            }
                                            var isLocalHit = true
                                            if (school == null) {
                                                isLocalHit = false
                                                val restored = withContext(Dispatchers.IO) {
                                                    try {
                                                        FirestoreSyncService(database).restoreSchoolFromCloud(sCode)
                                                    } catch (e: Exception) { null }
                                                }
                                                if (restored != null && restored.password == passwordOrPhone) {
                                                    school = restored
                                                }
                                            }
                                            if (school != null) {
                                                val code = school.schoolCode
                                                val name = school.schoolName
                                                val syncService = FirestoreSyncService(database)
                                                // If already in local DB, restore in background without delaying login
                                                if (isLocalHit) {
                                                    scope.launch(Dispatchers.IO) {
                                                        try {
                                                            syncService.restoreAllFromCloud(code)
                                                            syncService.startSync(code)
                                                        } catch (e: Exception) { e.printStackTrace() }
                                                    }
                                                } else {
                                                    withContext(Dispatchers.IO) {
                                                        try {
                                                            syncService.restoreAllFromCloud(code)
                                                            syncService.startSync(code)
                                                        } catch (e: Exception) { e.printStackTrace() }
                                                    }
                                                }
                                                activeLoginRole = null
                                                onSchoolLoginSuccess(code, name)
                                            } else {
                                                errorMessage = "Invalid School Code or Password"
                                            }
                                        }
                                    }
                                } finally {
                                    isLoggingIn = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (isLoggingIn) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = "Login",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // 6. Bottom Footer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Terms & conditions",
                            fontSize = 12.sp,
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "  •  ",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "Privacy Policy",
                            fontSize = 12.sp,
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
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
    val colors = LocalSchoolColors.current
    val scope = rememberCoroutineScope()

    var schoolName by remember { mutableStateOf("") }
    var schoolCode by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "School Registration",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgApp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero School Badge / Crest
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                colors.brandPrimary.copy(alpha = 0.18f),
                                colors.brandPrimary.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .border(2.dp, colors.brandPrimary.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(colors.brandPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🏫", fontSize = 40.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Register Your Institution",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = colors.textPrimary
            )
            Text(
                text = "Set up your centralized digital campus in seconds",
                fontSize = 13.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 22.dp)
            )

            // Modern Input Fields
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // School Full Name
                ModernInputField(
                    value = schoolName,
                    onValueChange = { schoolName = it },
                    label = "School Full Name",
                    icon = "🏛️",
                    placeholder = "e.g., Delhi Public Academy"
                )

                // Unique School Code
                ModernInputField(
                    value = schoolCode,
                    onValueChange = { schoolCode = it.uppercase() },
                    label = "Unique School Code",
                    icon = "🏷️",
                    placeholder = "e.g., DPA01 (Used for staff login)"
                )

                // Official Email Address
                ModernInputField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Official Email Address",
                    icon = "✉️",
                    placeholder = "e.g., contact@dpaedu.in"
                )

                // Official Contact Number
                ModernInputField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Official Contact Number",
                    icon = "📞",
                    placeholder = "e.g., +91 9876543210"
                )

                // School Address
                ModernInputField(
                    value = address,
                    onValueChange = { address = it },
                    label = "School Address",
                    icon = "📍",
                    placeholder = "e.g., Main Road, City, PIN"
                )

                // Admin Password
                ModernInputField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Admin Password",
                    icon = "🔒",
                    placeholder = "Enter secure password",
                    isPassword = true,
                    isPasswordVisible = isPasswordVisible,
                    onTogglePassword = { isPasswordVisible = !isPasswordVisible }
                )
            }

            if (errorMsg.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.error.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, colors.error.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMsg,
                        color = colors.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Action Button
            Button(
                onClick = {
                    if (schoolName.isBlank() || schoolCode.isBlank() || password.isBlank()) {
                        errorMsg = "Please fill in School Name, Code, and Password"
                        return@Button
                    }
                    isLoading = true
                    errorMsg = ""
                    scope.launch {
                        val existing = database.schoolDao().getSchoolByCode(schoolCode.trim())
                        if (existing != null) {
                            isLoading = false
                            errorMsg = "School Code '${schoolCode.trim()}' is already taken. Pick another code."
                        } else {
                            val newSchool = SchoolEntity(
                                schoolCode = schoolCode.trim(),
                                schoolName = schoolName.trim(),
                                password = password,
                                phone = phone.trim(),
                                email = email.trim(),
                                address = address.trim()
                            )
                            database.schoolDao().registerSchool(newSchool)
                            try {
                                FirestoreSyncService(database).saveSchoolToCloud(newSchool)
                            } catch (e: Exception) {
                                // Offline registration is still saved in Room
                            }
                            isLoading = false
                            onRegisterSuccess()
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.brandPrimary,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Complete School Registration 🚀",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ModernInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: String,
    placeholder: String = "",
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalSchoolColors.current

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder, fontSize = 13.sp, color = colors.textSecondary.copy(alpha = 0.5f)) }
        } else null,
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.brandPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 17.sp)
            }
        },
        trailingIcon = if (isPassword && onTogglePassword != null) {
            {
                IconButton(onClick = onTogglePassword) {
                    Text(if (isPasswordVisible) "👁️" else "🙈", fontSize = 16.sp)
                }
            }
        } else null,
        visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.inputBg,
            unfocusedContainerColor = colors.inputBg,
            focusedBorderColor = colors.brandPrimary,
            unfocusedBorderColor = colors.inputBorder,
            focusedLabelColor = colors.brandPrimary,
            unfocusedLabelColor = colors.textSecondary,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary
        ),
        modifier = modifier.fillMaxWidth()
    )
}

