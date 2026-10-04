package com.school.manage.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.LeaveRequestEntity
import com.school.manage.core.database.entity.SchoolEntity
import com.school.manage.core.database.entity.StudentEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentPortalScreen(
    database: AppDatabase,
    studentId: Long,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    var student by remember { mutableStateOf<StudentEntity?>(null) }
    var school by remember { mutableStateOf<SchoolEntity?>(null) }

    val attendanceList by database.attendanceDao().getAttendanceByStudent(studentId).collectAsState(initial = emptyList())
    val feeRecords by database.feeDao().getFeeRecordsByStudent(studentId).collectAsState(initial = emptyList())

    LaunchedEffect(studentId) {
        val s = database.studentDao().getStudentById(studentId)
        student = s
        if (s != null) {
            school = database.schoolDao().getSchoolByCode(s.schoolCode)
        }
    }

    val schoolCode = student?.schoolCode ?: ""
    val studentClass = student?.gradeClass ?: ""
    val studentName = student?.name ?: "Student"

    // Dynamic data flows for student's class
    val homeworkList by database.homeworkDao().getHomeworkBySchool(schoolCode).collectAsState(initial = emptyList())
    val classworkList by database.classworkDao().getClassworkBySchool(schoolCode).collectAsState(initial = emptyList())
    val examList by database.examDao().getExamsBySchool(schoolCode).collectAsState(initial = emptyList())
    val onlineClasses by database.onlineClassDao().getOnlineClassesBySchool(schoolCode).collectAsState(initial = emptyList())
    val leaves by database.leaveDao().getLeavesBySchool(schoolCode).collectAsState(initial = emptyList())

    // Filter by student class
    val studentHomework = homeworkList.filter { it.gradeClass.equals(studentClass, ignoreCase = true) || it.gradeClass.isBlank() }
    val studentClasswork = classworkList.filter { it.gradeClass.equals(studentClass, ignoreCase = true) || it.gradeClass.isBlank() }
    val studentExams = examList.filter { it.gradeClass.equals(studentClass, ignoreCase = true) || it.gradeClass.isBlank() }
    val studentOnlineClasses = onlineClasses.filter { it.gradeClass.equals(studentClass, ignoreCase = true) || it.gradeClass.isBlank() }
    val myLeaves = leaves.filter { it.applicantName.equals(studentName, ignoreCase = true) }

    // Active bottom-sheet / modal state
    var activeModal by remember { mutableStateOf<String?>(null) }

    // Leave form state
    var leaveStartDate by remember { mutableStateOf("05/10/2026") }
    var leaveEndDate by remember { mutableStateOf("06/10/2026") }
    var leaveReason by remember { mutableStateOf("") }
    var leaveSubmittedMsg by remember { mutableStateOf("") }

    val totalPaid = feeRecords.sumOf { it.amountPaid }.toInt()
    val totalFee = (student?.monthlyFee ?: 0.0).toInt()
    val dueAmount = maxOf(0, totalFee - totalPaid)

    val presentDays = attendanceList.count { it.status.equals("PRESENT", ignoreCase = true) }
    val absentDays = attendanceList.count { it.status.equals("ABSENT", ignoreCase = true) }
    val leaveDays = attendanceList.count { it.status.equals("LEAVE", ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            studentName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            "Class: $studentClass  •  ID: #$studentId",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                    }
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

            // 1. Profile Banner Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(primaryColor, primaryColor.copy(alpha = 0.82f))
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎓", fontSize = 26.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        studentName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        school?.schoolName ?: schoolCode,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF22C55E).copy(alpha = 0.9f)
                            ) {
                                Text(
                                    "Active",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Action Grid (6 Fully Functional Options)
            item {
                Text(
                    "STUDENT DASHBOARD",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.8.sp
                )
            }

            // Row 1: Fees, Exams, Classwork, Homework
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StudentModernActionTile("Fees", "💰", Color(0xFFDCFCE7), onClick = { activeModal = "FEES" })
                    StudentModernActionTile("Exams", "📝", Color(0xFFE0F2FE), onClick = { activeModal = "EXAMS" })
                    StudentModernActionTile("Classwork", "🖥️", Color(0xFFFEF3C7), onClick = { activeModal = "CLASSWORK" })
                    StudentModernActionTile("Homework", "📖", Color(0xFFFFE4E6), onClick = { activeModal = "HOMEWORK" })
                }
            }

            // Row 2: Live Class & Leave
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StudentModernActionTile("Live Class", "⏱️", Color(0xFFFFEDD5), onClick = { activeModal = "LIVE" })
                    StudentModernActionTile("Leave", "➖", Color(0xFFFEF9C3), onClick = { activeModal = "LEAVE" })
                }
            }

            // 3. Batch Information Card
            item {
                Text(
                    "BATCH INFORMATION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.8.sp
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(primaryColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👥", fontSize = 22.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Class: $studentClass", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E293B))
                                Text("Monthly Tuition & Academic Batch", fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Joined", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                Text(student?.admissionDate ?: "Active", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF334155))
                            }
                            Column {
                                Text("Status", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                Text("Active", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF16A34A))
                            }
                            Column {
                                Text("Paid", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                Text("₹$totalPaid", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF16A34A))
                            }
                            Column {
                                Text("Due", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                Text("₹$dueAmount", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (dueAmount > 0) Color(0xFFDC2626) else Color(0xFF16A34A))
                            }
                        }
                    }
                }
            }

            // 4. Attendance Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Attendance Summary", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = primaryColor)
                            Text("Current Session", fontSize = 12.sp, color = Color(0xFF64748B))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AttendancePill(label = "Present", count = presentDays, color = Color(0xFF86EFAC), textColor = Color(0xFF166534), modifier = Modifier.weight(1f))
                            AttendancePill(label = "Absent", count = absentDays, color = Color(0xFFFCA5A5), textColor = Color(0xFF991B1B), modifier = Modifier.weight(1f))
                            AttendancePill(label = "Leave", count = leaveDays, color = Color(0xFFFDE047), textColor = Color(0xFF854D0E), modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // 5. School Profile Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE0E7FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏫", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(school?.schoolName ?: "SCHOOL PORTAL", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E293B))
                            Text("Institute Code: $schoolCode", fontSize = 12.sp, color = primaryColor, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(30.dp)) }
        }

        // ================= POPUP MODALS FOR THE 6 ACTIONS =================

        // 1. FEES MODAL
        if (activeModal == "FEES") {
            ModalBottomSheet(
                onDismissRequest = { activeModal = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Fee Ledger & Status", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E293B))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatBox("Total Fee", "₹$totalFee", Color(0xFFF1F5F9), Color(0xFF334155))
                        StatBox("Paid", "₹$totalPaid", Color(0xFFDCFCE7), Color(0xFF166534))
                        StatBox("Due", "₹$dueAmount", Color(0xFFFEE2E2), if (dueAmount > 0) Color(0xFFDC2626) else Color(0xFF166534))
                    }
                    Text("PAYMENT RECORDS", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    if (feeRecords.isEmpty()) {
                        Text("No payment transactions recorded yet.", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(feeRecords) { fee ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
        Text(fee.feeMonth, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text("Paid on: ${fee.paymentDate}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Text("₹${fee.amountPaid.toInt()}", fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                    }
                                }
                            }
                        }
                    }
                    Button(
                        onClick = { activeModal = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        }

        // 2. EXAMS MODAL
        if (activeModal == "EXAMS") {
            ModalBottomSheet(
                onDismissRequest = { activeModal = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Scheduled Examinations", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E293B))
                    if (studentExams.isEmpty()) {
                        Text("No examinations currently scheduled for Class $studentClass.", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(studentExams) { exam ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(exam.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Subject: ${exam.subject}  •  Max Marks: ${exam.maxMarks.toInt()}", fontSize = 12.sp, color = Color(0xFF64748B))
                                        Text("Date: ${exam.examDate}", fontSize = 12.sp, color = primaryColor, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                    Button(
                        onClick = { activeModal = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        }

        // 3. CLASSWORK MODAL
        if (activeModal == "CLASSWORK") {
            ModalBottomSheet(
                onDismissRequest = { activeModal = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Daily Classwork & Notes", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E293B))
                    if (studentClasswork.isEmpty()) {
                        Text("No classwork topics posted yet for Class $studentClass.", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(studentClasswork) { cw ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(cw.topicTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Subject: ${cw.subject}  •  ${cw.date}", fontSize = 12.sp, color = Color(0xFF64748B))
                                        if (cw.summary.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(cw.summary, fontSize = 13.sp, color = Color(0xFF334155))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Button(
                        onClick = { activeModal = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        }

        // 4. HOMEWORK MODAL
        if (activeModal == "HOMEWORK") {
            ModalBottomSheet(
                onDismissRequest = { activeModal = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Assigned Homework", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E293B))
                    if (studentHomework.isEmpty()) {
                        Text("No homework assignments active for Class $studentClass.", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(studentHomework) { hw ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(hw.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
                                        Text("Subject: ${hw.subject}  •  Due: ${hw.dueDate}", fontSize = 12.sp, color = Color(0xFFE11D48), fontWeight = FontWeight.SemiBold)
                                        if (hw.description.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(hw.description, fontSize = 13.sp, color = Color(0xFF334155))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Button(
                        onClick = { activeModal = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        }

        // 5. LIVE CLASS MODAL
        if (activeModal == "LIVE") {
            ModalBottomSheet(
                onDismissRequest = { activeModal = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Live Online Sessions", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E293B))
                    if (studentOnlineClasses.isEmpty()) {
                        Text("No live classes scheduled for Class $studentClass.", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(studentOnlineClasses) { live ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(live.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
                                        Text("Subject: ${live.subject}  •  ${live.classDate} at ${live.classTime}", fontSize = 12.sp, color = Color(0xFF64748B))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                if (live.meetingUrl.isNotBlank()) {
                                                    try {
                                                        uriHandler.openUri(live.meetingUrl)
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                        ) {
                                            Text("Join Live Meeting 🔗", fontSize = 12.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Button(
                        onClick = { activeModal = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        }

        // 6. LEAVE REQUEST MODAL
        if (activeModal == "LEAVE") {
            ModalBottomSheet(
                onDismissRequest = {
                    activeModal = null
                    leaveSubmittedMsg = ""
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Apply for Leave", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E293B))

                    OutlinedTextField(
                        value = leaveStartDate,
                        onValueChange = { leaveStartDate = it },
                        label = { Text("From Date (DD/MM/YYYY)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = leaveEndDate,
                        onValueChange = { leaveEndDate = it },
                        label = { Text("To Date (DD/MM/YYYY)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = leaveReason,
                        onValueChange = { leaveReason = it },
                        label = { Text("Reason for Leave") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (leaveSubmittedMsg.isNotBlank()) {
                        Text(leaveSubmittedMsg, color = Color(0xFF16A34A), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            if (leaveReason.isNotBlank()) {
                                scope.launch {
                                    database.leaveDao().insertLeave(
                                        LeaveRequestEntity(
                                            schoolCode = schoolCode,
                                            applicantName = studentName,
                                            applicantType = "STUDENT",
                                            startDate = leaveStartDate,
                                            endDate = leaveEndDate,
                                            reason = leaveReason,
                                            status = "PENDING"
                                        )
                                    )
                                    leaveSubmittedMsg = "✓ Leave application submitted successfully!"
                                    leaveReason = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Submit Application")
                    }

                    if (myLeaves.isNotEmpty()) {
                        Text("MY RECENT APPLICATIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        LazyColumn(modifier = Modifier.heightIn(max = 140.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(myLeaves) { l ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("${l.startDate} to ${l.endDate}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            Text(l.reason, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                                        }
                                        Text(
                                            l.status,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (l.status == "APPROVED") Color(0xFF16A34A) else Color(0xFFEAB308)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentModernActionTile(
    title: String,
    icon: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(badgeColor),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 28.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = Color(0xFF334155),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AttendancePill(
    label: String,
    count: Int,
    color: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$label $count",
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun StatBox(label: String, value: String, bgColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 11.sp, color = Color.Gray)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = textColor)
        }
    }
}
