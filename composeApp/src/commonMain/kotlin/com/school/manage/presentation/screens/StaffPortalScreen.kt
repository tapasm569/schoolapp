package com.school.manage.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.SchoolEntity
import com.school.manage.core.database.entity.StaffEntity
import com.school.manage.core.database.entity.StudentEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffPortalScreen(
    database: AppDatabase,
    staffId: Long,
    schoolCode: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val uriHandler = LocalUriHandler.current

    var staff by remember { mutableStateOf<StaffEntity?>(null) }
    var school by remember { mutableStateOf<SchoolEntity?>(null) }
    val students by database.studentDao().getStudentsBySchool(schoolCode).collectAsState(initial = emptyList())
    val announcements by database.announcementDao().getAnnouncementsBySchool(schoolCode).collectAsState(initial = emptyList())

    // WhatsApp Connect State
    var showWhatsAppDialog by remember { mutableStateOf(false) }
    var selectedStudentForWhatsApp by remember { mutableStateOf<StudentEntity?>(null) }
    var customMessage by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(staffId) {
        staff = database.staffDao().getStaffById(staffId)
        school = database.schoolDao().getSchoolByCode(schoolCode)
    }

    val teacherName = staff?.name ?: "Faculty Member"
    val teacherRole = staff?.role ?: "Teacher"
    val schoolTitle = school?.schoolName ?: schoolCode

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Faculty Portal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = Color(0xFFFFCDD2))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryColor)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Hero Identity Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(primaryColor, primaryColor.copy(alpha = 0.85f))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("👨‍🏫", fontSize = 28.sp)
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        teacherName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        "$teacherRole • $schoolTitle",
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.88f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.16f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4ADE80))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Faculty Session Active",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    "Code: $schoolCode",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 2. School Circulars / Announcements Banner
            val latestNotice = announcements.firstOrNull()
            if (latestNotice != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("announcements") },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📢", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    latestNotice.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    latestNotice.message,
                                    fontSize = 12.sp,
                                    color = Color(0xFF78350F),
                                    maxLines = 1
                                )
                            }
                            Text("›", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                        }
                    }
                }
            }

            // Section Label
            item {
                Text(
                    "TEACHER WORKSPACE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.8.sp
                )
            }

            // Row 1: Student Attendance & WhatsApp Connect
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    FacultyTile(
                        title = "Student\nAttendance",
                        subtitle = "Daily Register",
                        icon = "📋",
                        badgeColor = Color(0xFFDBEAFE),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("attendance") }
                    )
                    FacultyTile(
                        title = "WhatsApp\nConnect",
                        subtitle = "Message Parents",
                        icon = "💬",
                        badgeColor = Color(0xFFDCFCE7),
                        modifier = Modifier.weight(1f),
                        onClick = { showWhatsAppDialog = true }
                    )
                }
            }

            // Row 2: Homework & Classwork
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    FacultyTile(
                        title = "Assign\nHomework",
                        subtitle = "Daily Tasks",
                        icon = "📝",
                        badgeColor = Color(0xFFFEF3C7),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("homework") }
                    )
                    FacultyTile(
                        title = "Classwork\nNotes",
                        subtitle = "Daily Topics",
                        icon = "📖",
                        badgeColor = Color(0xFFF3E8FF),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("classwork") }
                    )
                }
            }

            // Row 3: Timetable Routine & Exams
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    FacultyTile(
                        title = "Routine &\nTimetable",
                        subtitle = "Period Schedule",
                        icon = "📅",
                        badgeColor = Color(0xFFE0E7FF),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("timetable") }
                    )
                    FacultyTile(
                        title = "Examinations\n& Results",
                        subtitle = "Score Sheet",
                        icon = "📊",
                        badgeColor = Color(0xFFFFEDD5),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("exams") }
                    )
                }
            }

            // Row 4: Question Bank & Leave Application
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    FacultyTile(
                        title = "Question\nBank",
                        subtitle = "Study Vault",
                        icon = "📑",
                        badgeColor = Color(0xFFCFFAFE),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("question_bank") }
                    )
                    FacultyTile(
                        title = "Leave\nApplication",
                        subtitle = "Apply & Track",
                        icon = "➖",
                        badgeColor = Color(0xFFFEE2E2),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("leave_management") }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(28.dp)) }
        }

        // WhatsApp Parent Communication Dialog
        if (showWhatsAppDialog) {
            AlertDialog(
                onDismissRequest = {
                    showWhatsAppDialog = false
                    selectedStudentForWhatsApp = null
                    customMessage = ""
                },
                shape = RoundedCornerShape(22.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💬", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("WhatsApp Parent Connect", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (selectedStudentForWhatsApp == null) {
                            Text(
                                "Select a student to message their parent:",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search name or class...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            val filtered = students.filter {
                                it.name.contains(searchQuery, ignoreCase = true) ||
                                it.gradeClass.contains(searchQuery, ignoreCase = true)
                            }
                            LazyColumn(
                                modifier = Modifier.height(260.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filtered) { s ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedStudentForWhatsApp = s },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(s.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("Class: ${s.gradeClass}  •  📞 ${s.phone}", fontSize = 12.sp, color = Color.Gray)
                                            }
                                            Text("›", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        } else {
                            val student = selectedStudentForWhatsApp!!
                            Text(
                                "To parent of ${student.name} (${student.gradeClass}):",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )

                            Text("QUICK TEMPLATES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            customMessage = "Dear Parent, this is to inform you that ${student.name} was marked absent today. Kindly let us know the reason. - $teacherName"
                                        },
                                        label = { Text("Absent Alert") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            customMessage = "Dear Parent, please ensure that ${student.name} completes today's assigned homework. - $teacherName"
                                        },
                                        label = { Text("Homework Alert") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            customMessage = "Dear Parent, proud to inform you that ${student.name} performed exceptionally well in class class today! - $teacherName"
                                        },
                                        label = { Text("Appreciation") }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = customMessage,
                                onValueChange = { customMessage = it },
                                label = { Text("Message") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    if (selectedStudentForWhatsApp != null) {
                        Button(
                            onClick = {
                                val s = selectedStudentForWhatsApp!!
                                val cleanPhone = s.phone.replace("[^0-9]".toRegex(), "")
                                val formattedPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
                                val encoded = customMessage.replace(" ", "%20")
                                val url = "https://wa.me/$formattedPhone?text=$encoded"
                                try {
                                    uriHandler.openUri(url)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                showWhatsAppDialog = false
                                selectedStudentForWhatsApp = null
                                customMessage = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Send WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        if (selectedStudentForWhatsApp != null) {
                            selectedStudentForWhatsApp = null
                        } else {
                            showWhatsAppDialog = false
                        }
                    }) {
                        Text(if (selectedStudentForWhatsApp != null) "Back to List" else "Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun FacultyTile(
    title: String,
    subtitle: String,
    icon: String,
    badgeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                subtitle,
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                maxLines = 1
            )
        }
    }
}
