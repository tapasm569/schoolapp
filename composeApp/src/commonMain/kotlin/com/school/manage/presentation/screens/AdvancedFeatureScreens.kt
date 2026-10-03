package com.school.manage.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.*
import com.school.manage.presentation.theme.LocalSchoolColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveManagementScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val scope = rememberCoroutineScope()
    val leaves by database.leaveDao().getLeavesBySchool(schoolCode).collectAsState(initial = emptyList())

    var selectedFilter by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }

    var applicantName by remember { mutableStateOf("") }
    var applicantType by remember { mutableStateOf("STUDENT") }
    var startDate by remember { mutableStateOf("03/10/2026") }
    var endDate by remember { mutableStateOf("04/10/2026") }
    var reason by remember { mutableStateOf("") }

    val filtered = if (selectedFilter == "ALL") leaves else leaves.filter { it.status == selectedFilter }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("Leave Management (" + leaves.size + ")", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgApp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = colors.brandPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) { Text("+", fontSize = 28.sp) }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL", "PENDING", "APPROVED", "REJECTED").forEach { st ->
                        Surface(
                            onClick = { selectedFilter = st },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedFilter == st) colors.brandPrimary else colors.bgCardHover,
                            border = BorderStroke(1.dp, if (selectedFilter == st) colors.brandPrimary else colors.borderCard)
                        ) {
                            Text(st, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selectedFilter == st) Color.White else colors.textPrimary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No leave requests in this category.", color = colors.textSecondary)
                    }
                }
            } else {
                items(filtered) { lv ->
                    val statusColor = when (lv.status) {
                        "APPROVED" -> colors.success
                        "REJECTED" -> colors.error
                        else -> Color(0xFFF59E0B)
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.bgCard,
                        border = BorderStroke(1.dp, colors.borderCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(lv.applicantName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                                    Text("Role: " + lv.applicantType, fontSize = 11.sp, color = colors.textSecondary)
                                }
                                Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.15f)) {
                                    Text(lv.status, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Duration: " + lv.startDate + " to " + lv.endDate, fontSize = 12.sp, color = colors.brandPrimary, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Reason: " + lv.reason, fontSize = 12.sp, color = colors.textPrimary)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (lv.status != "APPROVED") {
                                        Button(
                                            onClick = { scope.launch { database.leaveDao().insertLeave(lv.copy(status = "APPROVED")) } },
                                            colors = ButtonDefaults.buttonColors(containerColor = colors.success, contentColor = Color.White),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) { Text("Approve", fontSize = 11.sp) }
                                    }
                                    if (lv.status != "REJECTED") {
                                        Button(
                                            onClick = { scope.launch { database.leaveDao().insertLeave(lv.copy(status = "REJECTED")) } },
                                            colors = ButtonDefaults.buttonColors(containerColor = colors.error, contentColor = Color.White),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) { Text("Reject", fontSize = 11.sp) }
                                    }
                                }
                                TextButton(onClick = { scope.launch { database.leaveDao().deleteLeave(lv) } }) {
                                    Text("Remove", color = colors.error, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                containerColor = colors.bgCard,
                title = { Text("Submit Leave Request", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CustomRoundedInput(value = applicantName, onValueChange = { applicantName = it }, placeholder = "Applicant Name")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("STUDENT", "STAFF").forEach { role ->
                                Surface(
                                    onClick = { applicantType = role },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (applicantType == role) colors.brandPrimary else colors.bgCardHover
                                ) {
                                    Text(role, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (applicantType == role) Color.White else colors.textPrimary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                }
                            }
                        }
                        CustomRoundedInput(value = startDate, onValueChange = { startDate = it }, placeholder = "Start Date (DD/MM/YYYY)")
                        CustomRoundedInput(value = endDate, onValueChange = { endDate = it }, placeholder = "End Date (DD/MM/YYYY)")
                        CustomRoundedInput(value = reason, onValueChange = { reason = it }, placeholder = "Reason for Leave")
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                        onClick = {
                            if (applicantName.isNotBlank() && reason.isNotBlank()) {
                                scope.launch {
                                    database.leaveDao().insertLeave(
                                        LeaveRequestEntity(
                                            schoolCode = schoolCode,
                                            applicantName = applicantName.trim(),
                                            applicantType = applicantType,
                                            startDate = startDate.trim(),
                                            endDate = endDate.trim(),
                                            reason = reason.trim()
                                        )
                                    )
                                    applicantName = ""
                                    reason = ""
                                    showAddDialog = false
                                }
                            }
                        }
                    ) { Text("Submit") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = colors.textSecondary) }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val scope = rememberCoroutineScope()
    val timetableList by database.timetableDao().getTimetableBySchool(schoolCode).collectAsState(initial = emptyList())
    val batches by database.batchDao().getBatchesBySchool(schoolCode).collectAsState(initial = emptyList())

    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    var selectedDay by remember { mutableStateOf("Monday") }
    var selectedClass by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }

    var newClass by remember { mutableStateOf("") }
    var newPeriod by remember { mutableStateOf("1") }
    var newTimeSlot by remember { mutableStateOf("09:00 AM - 09:45 AM") }
    var newSubject by remember { mutableStateOf("") }
    var newTeacher by remember { mutableStateOf("") }

    val filtered = timetableList.filter {
        it.dayOfWeek.equals(selectedDay, ignoreCase = true) && (selectedClass == "ALL" || it.gradeClass.equals(selectedClass, ignoreCase = true))
    }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("Class Timetable", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgApp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = colors.brandPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) { Text("+", fontSize = 28.sp) }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(daysOfWeek) { day ->
                        val isSel = selectedDay == day
                        Surface(
                            onClick = { selectedDay = day },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) colors.brandPrimary else colors.bgCardHover,
                            border = BorderStroke(1.dp, if (isSel) colors.brandPrimary else colors.borderCard)
                        ) {
                            Text(day, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else colors.textPrimary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                        }
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        Surface(
                            onClick = { selectedClass = "ALL" },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedClass == "ALL") colors.brandPrimary.copy(alpha = 0.15f) else colors.bgCardHover
                        ) {
                            Text("All Classes", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selectedClass == "ALL") colors.brandPrimary else colors.textSecondary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                        }
                    }
                    items(batches) { b ->
                        val isSel = selectedClass == b.batchName
                        Surface(
                            onClick = { selectedClass = b.batchName },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) colors.brandPrimary.copy(alpha = 0.15f) else colors.bgCardHover
                        ) {
                            Text(b.batchName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) colors.brandPrimary else colors.textSecondary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No periods scheduled for " + selectedDay + ".", color = colors.textSecondary)
                    }
                }
            } else {
                items(filtered) { slot ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.bgCard,
                        border = BorderStroke(1.dp, colors.borderCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = colors.brandPrimary.copy(alpha = 0.15f)) {
                                        Text("Period " + slot.periodNo, color = colors.brandPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(slot.timeSlot, fontSize = 12.sp, color = colors.textSecondary)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(slot.subject, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                                Text("Class: " + slot.gradeClass + " • Faculty: " + slot.teacherName, fontSize = 12.sp, color = colors.textSecondary)
                            }
                            IconButton(onClick = { scope.launch { database.timetableDao().deleteTimetable(slot) } }) {
                                Text("🗑️️", fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                containerColor = colors.bgCard,
                title = { Text("Add Timetable Slot", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CustomRoundedInput(value = newClass, onValueChange = { newClass = it }, placeholder = "Class (e.g. Class 1)")
                        CustomRoundedInput(value = newPeriod, onValueChange = { newPeriod = it }, placeholder = "Period Number (1-8)")
                        CustomRoundedInput(value = newTimeSlot, onValueChange = { newTimeSlot = it }, placeholder = "Time (e.g. 09:00 AM - 09:45 AM)")
                        CustomRoundedInput(value = newSubject, onValueChange = { newSubject = it }, placeholder = "Subject (e.g. Mathematics)")
                        CustomRoundedInput(value = newTeacher, onValueChange = { newTeacher = it }, placeholder = "Teacher Name")
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                        onClick = {
                            if (newClass.isNotBlank() && newSubject.isNotBlank()) {
                                scope.launch {
                                    database.timetableDao().insertTimetable(
                                        TimetableEntity(
                                            schoolCode = schoolCode,
                                            gradeClass = newClass.trim(),
                                            dayOfWeek = selectedDay,
                                            periodNo = newPeriod.toIntOrNull() ?: 1,
                                            timeSlot = newTimeSlot.trim(),
                                            subject = newSubject.trim(),
                                            teacherName = newTeacher.trim()
                                        )
                                    )
                                    newClass = ""
                                    newSubject = ""
                                    newTeacher = ""
                                    showAddDialog = false
                                }
                            }
                        }
                    ) { Text("Save Slot") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = colors.textSecondary) }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineClassesScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val onlineClasses by database.onlineClassDao().getOnlineClassesBySchool(schoolCode).collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var gradeClass by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var meetingUrl by remember { mutableStateOf("") }
    var classDate by remember { mutableStateOf("03/10/2026") }
    var classTime by remember { mutableStateOf("10:00 AM") }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("Online Classes (" + onlineClasses.size + ")", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgApp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = colors.brandPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) { Text("+", fontSize = 28.sp) }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            if (onlineClasses.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No live virtual classes scheduled yet.", color = colors.textSecondary)
                    }
                }
            } else {
                items(onlineClasses) { cls ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.bgCard,
                        border = BorderStroke(1.dp, colors.borderCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(cls.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
                                Surface(shape = RoundedCornerShape(6.dp), color = colors.brandPrimary.copy(alpha = 0.15f)) {
                                    Text(cls.gradeClass, color = colors.brandPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Subject: " + cls.subject + " • Date: " + cls.classDate + " at " + cls.classTime, fontSize = 12.sp, color = colors.textSecondary)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = {
                                        val cleanUrl = if (cls.meetingUrl.startsWith("http")) cls.meetingUrl else "https://" + cls.meetingUrl
                                        runCatching { uriHandler.openUri(cleanUrl) }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("🔗 Launch / Join Class", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                TextButton(onClick = { scope.launch { database.onlineClassDao().deleteOnlineClass(cls) } }) {
                                    Text("Delete", color = colors.error, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                containerColor = colors.bgCard,
                title = { Text("Schedule Online Class", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CustomRoundedInput(value = title, onValueChange = { title = it }, placeholder = "Session Title (e.g. Science Chapter 2)")
                        CustomRoundedInput(value = gradeClass, onValueChange = { gradeClass = it }, placeholder = "Target Class (e.g. Class 2)")
                        CustomRoundedInput(value = subject, onValueChange = { subject = it }, placeholder = "Subject")
                        CustomRoundedInput(value = meetingUrl, onValueChange = { meetingUrl = it }, placeholder = "Meeting Link (Google Meet / Zoom)")
                        CustomRoundedInput(value = classDate, onValueChange = { classDate = it }, placeholder = "Date (DD/MM/YYYY)")
                        CustomRoundedInput(value = classTime, onValueChange = { classTime = it }, placeholder = "Time (e.g. 10:00 AM)")
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                        onClick = {
                            if (title.isNotBlank() && meetingUrl.isNotBlank()) {
                                scope.launch {
                                    database.onlineClassDao().insertOnlineClass(
                                        OnlineClassEntity(
                                            schoolCode = schoolCode,
                                            title = title.trim(),
                                            gradeClass = gradeClass.trim(),
                                            subject = subject.trim(),
                                            meetingUrl = meetingUrl.trim(),
                                            classDate = classDate.trim(),
                                            classTime = classTime.trim()
                                        )
                                    )
                                    title = ""
                                    meetingUrl = ""
                                    showAddDialog = false
                                }
                            }
                        }
                    ) { Text("Schedule") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = colors.textSecondary) }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionBankScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val scope = rememberCoroutineScope()
    val questions by database.questionBankDao().getQuestionsBySchool(schoolCode).collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedClassFilter by remember { mutableStateOf("ALL") }

    var gradeClass by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var chapter by remember { mutableStateOf("") }
    var questionText by remember { mutableStateOf("") }
    var answerKey by remember { mutableStateOf("") }
    var qType by remember { mutableStateOf("SHORT") }

    val filtered = if (selectedClassFilter == "ALL") questions else questions.filter { it.gradeClass.equals(selectedClassFilter, ignoreCase = true) }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("Question Bank (" + questions.size + ")", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgApp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = colors.brandPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) { Text("+", fontSize = 28.sp) }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            if (filtered.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No questions saved yet. Tap + to add practice questions.", color = colors.textSecondary)
                    }
                }
            } else {
                items(filtered) { q ->
                    var showAnswer by remember(q.id) { mutableStateOf(false) }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.bgCard,
                        border = BorderStroke(1.dp, colors.borderCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Surface(shape = RoundedCornerShape(6.dp), color = colors.brandPrimary.copy(alpha = 0.15f)) {
                                    Text(q.gradeClass + " • " + q.subject, color = colors.brandPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFEF3C7)) {
                                    Text(q.questionType, color = Color(0xFFB45309), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Topic: " + q.chapterTopic, fontSize = 12.sp, color = colors.textSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Q: " + q.questionText, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)

                            if (showAnswer) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.bgCardHover,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Answer / Solution:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.success)
                                        Text(q.answerKey, fontSize = 12.sp, color = colors.textPrimary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { showAnswer = !showAnswer }) {
                                    Text(if (showAnswer) "Hide Solution" else "Show Solution", color = colors.brandPrimary, fontSize = 12.sp)
                                }
                                TextButton(onClick = { scope.launch { database.questionBankDao().deleteQuestion(q) } }) {
                                    Text("Delete", color = colors.error, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                containerColor = colors.bgCard,
                title = { Text("Add Question / Study Note", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CustomRoundedInput(value = gradeClass, onValueChange = { gradeClass = it }, placeholder = "Class (e.g. Class 5)")
                        CustomRoundedInput(value = subject, onValueChange = { subject = it }, placeholder = "Subject (e.g. Science)")
                        CustomRoundedInput(value = chapter, onValueChange = { chapter = it }, placeholder = "Chapter / Topic")
                        CustomRoundedInput(value = questionText, onValueChange = { questionText = it }, placeholder = "Question Text")
                        CustomRoundedInput(value = answerKey, onValueChange = { answerKey = it }, placeholder = "Answer Key / Explanation")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("SHORT", "MCQ", "ESSAY").forEach { tp ->
                                Surface(
                                    onClick = { qType = tp },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (qType == tp) colors.brandPrimary else colors.bgCardHover
                                ) {
                                    Text(tp, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (qType == tp) Color.White else colors.textPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                        onClick = {
                            if (questionText.isNotBlank() && gradeClass.isNotBlank()) {
                                scope.launch {
                                    database.questionBankDao().insertQuestion(
                                        QuestionBankEntity(
                                            schoolCode = schoolCode,
                                            gradeClass = gradeClass.trim(),
                                            subject = subject.trim(),
                                            chapterTopic = chapter.trim(),
                                            questionText = questionText.trim(),
                                            answerKey = answerKey.trim(),
                                            questionType = qType
                                        )
                                    )
                                    questionText = ""
                                    answerKey = ""
                                    showAddDialog = false
                                }
                            }
                        }
                    ) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = colors.textSecondary) }
                }
            )
        }
    }
}
