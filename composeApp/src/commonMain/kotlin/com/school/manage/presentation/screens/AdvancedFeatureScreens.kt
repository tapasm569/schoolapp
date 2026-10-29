package com.school.manage.presentation.screens

import androidx.compose.foundation.clickable
import com.school.manage.core.utils.getSystemCurrentDate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var selectedRoleFilter by remember { mutableStateOf("ALL") }

    val pendingCount = leaves.count { it.status == "PENDING" }
    val filtered = leaves.filter { lv ->
        (selectedStatusFilter == "ALL" || lv.status.equals(selectedStatusFilter, ignoreCase = true)) &&
        (selectedRoleFilter == "ALL" || lv.applicantType.equals(selectedRoleFilter, ignoreCase = true))
    }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Leave Verification (${leaves.size})", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Text("Verify Student & Teacher leave applications", fontSize = 11.sp, color = colors.textSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgApp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (pendingCount > 0) Color(0xFFFEF3C7) else Color(0xFFDCFCE7),
                    border = BorderStroke(1.dp, if (pendingCount > 0) Color(0xFFF59E0B) else Color(0xFF22C55E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (pendingCount > 0) "⏳ $pendingCount Pending Leave Applications" else "✓ All Leave Requests Verified",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = if (pendingCount > 0) Color(0xFF92400E) else Color(0xFF166534)
                            )
                            Text(
                                text = "Students and Teachers submit leave from their portals for Admin verification.",
                                fontSize = 11.sp,
                                color = if (pendingCount > 0) Color(0xFF78350F) else Color(0xFF15803D)
                            )
                        }
                    }
                }
            }

            // Role Filter: ALL / STUDENT / STAFF
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL" to "All Roles", "STUDENT" to "🎓 Students", "STAFF" to "👨‍🏫 Teachers").forEach { (roleKey, label) ->
                        Surface(
                            onClick = { selectedRoleFilter = roleKey },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedRoleFilter == roleKey) colors.brandPrimary else colors.bgCardHover,
                            border = BorderStroke(1.dp, if (selectedRoleFilter == roleKey) colors.brandPrimary else colors.borderCard)
                        ) {
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selectedRoleFilter == roleKey) Color.White else colors.textPrimary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }
            }

            // Status Filter: ALL / PENDING / APPROVED / REJECTED
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL", "PENDING", "APPROVED", "REJECTED").forEach { st ->
                        Surface(
                            onClick = { selectedStatusFilter = st },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedStatusFilter == st) colors.brandPrimary.copy(alpha = 0.16f) else colors.bgCardHover,
                            border = BorderStroke(1.dp, if (selectedStatusFilter == st) colors.brandPrimary else colors.borderCard)
                        ) {
                            Text(st, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selectedStatusFilter == st) colors.brandPrimary else colors.textSecondary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No leave applications in this category.", color = colors.textSecondary)
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
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(lv.applicantName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                                    Text(
                                        text = if (lv.applicantType.equals("STAFF", ignoreCase = true)) "👨‍🏫 Teacher / Staff" else "🎓 Student",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.brandPrimary
                                    )
                                }
                                Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.15f)) {
                                    Text(lv.status, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("📅 Duration: " + lv.startDate + " to " + lv.endDate, fontSize = 12.sp, color = colors.brandPrimary, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Reason: " + lv.reason, fontSize = 12.sp, color = colors.textPrimary)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (lv.status != "APPROVED") {
                                        Button(
                                            onClick = { scope.launch { run { val _ent = lv.copy(status = "APPROVED"); val _id = database.leaveDao().insertLeave(_ent); val _final = if (_id > 0L) _ent.copy(id = _id) else _ent; try { com.school.manage.core.firebase.FirestoreSyncService(database).pushLeaveRequest(schoolCode, _final) } catch (_: Exception) {}; _id } } },
                                            colors = ButtonDefaults.buttonColors(containerColor = colors.success, contentColor = Color.White),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) { Text("✓ Approve", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    }
                                    if (lv.status != "REJECTED") {
                                        Button(
                                            onClick = { scope.launch { run { val _ent = lv.copy(status = "REJECTED"); val _id = database.leaveDao().insertLeave(_ent); val _final = if (_id > 0L) _ent.copy(id = _id) else _ent; try { com.school.manage.core.firebase.FirestoreSyncService(database).pushLeaveRequest(schoolCode, _final) } catch (_: Exception) {}; _id } } },
                                            colors = ButtonDefaults.buttonColors(containerColor = colors.error, contentColor = Color.White),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) { Text("✕ Reject", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    }
                                }
                                TextButton(onClick = { scope.launch { run { val _ent = lv; database.leaveDao().deleteLeave(_ent); try { com.school.manage.core.firebase.FirestoreSyncService(database).deleteModuleRecord(schoolCode, "leave_requests", _ent.id) } catch (_: Exception) {} } } }) {
                                    Text("Delete", color = colors.error, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
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
                            IconButton(onClick = { scope.launch { run { val _ent = slot; database.timetableDao().deleteTimetable(_ent); try { com.school.manage.core.firebase.FirestoreSyncService(database).deleteModuleRecord(schoolCode, "timetables", _ent.id) } catch (_: Exception) {} } } }) {
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
                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CustomRoundedInput(value = newClass, onValueChange = { newClass = it }, placeholder = "Class (e.g. Class 1)")
                        CustomRoundedInput(value = newPeriod, onValueChange = { newPeriod = it }, placeholder = "Period Number (1-8)")
                        CustomRoundedInput(value = newTimeSlot, onValueChange = { newTimeSlot = it }, placeholder = "Time (e.g. 09:00 AM - 09:45 AM)")
                        SubjectDropdownSelector(selectedClass = newClass, selectedSubject = newSubject, batches = batches, onSubjectSelected = { newSubject = it })
                        CustomRoundedInput(value = newTeacher, onValueChange = { newTeacher = it }, placeholder = "Teacher Name")
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                        onClick = {
                            if (newClass.isNotBlank() && newSubject.isNotBlank()) {
                                scope.launch {
                                    run { val _ent = TimetableEntity(
                                            schoolCode = schoolCode,
                                            gradeClass = newClass.trim(),
                                            dayOfWeek = selectedDay,
                                            periodNo = newPeriod.toIntOrNull() ?: 1,
                                            timeSlot = newTimeSlot.trim(),
                                            subject = newSubject.trim(),
                                            teacherName = newTeacher.trim()
                                        ); val _id = database.timetableDao().insertTimetable(_ent); val _final = if (_id > 0L) _ent.copy(id = _id) else _ent; try { com.school.manage.core.firebase.FirestoreSyncService(database).pushTimetable(schoolCode, _final) } catch (_: Exception) {}; _id }
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
    val currentSysDate = remember { getSystemCurrentDate().formatted }
    var classDate by remember { mutableStateOf(currentSysDate) }
    var showDatePicker by remember { mutableStateOf(false) }
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
                                Text(cls.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary, modifier = Modifier.weight(1f).padding(end = 8.dp))
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
                                TextButton(onClick = { scope.launch { run { val _ent = cls; database.onlineClassDao().deleteOnlineClass(_ent); try { com.school.manage.core.firebase.FirestoreSyncService(database).deleteModuleRecord(schoolCode, "online_classes", _ent.id) } catch (_: Exception) {} } } }) {
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
                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CustomRoundedInput(value = title, onValueChange = { title = it }, placeholder = "Session Title (e.g. Science Chapter 2)")
                        CustomRoundedInput(value = gradeClass, onValueChange = { gradeClass = it }, placeholder = "Target Class (e.g. Class 2)")
                        CustomRoundedInput(value = subject, onValueChange = { subject = it }, placeholder = "Subject")
                        CustomRoundedInput(value = meetingUrl, onValueChange = { meetingUrl = it }, placeholder = "Meeting Link (Google Meet / Zoom)")
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, colors.borderCard),
                            color = colors.bgCard,
                            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📅 Class Date: $classDate", fontSize = 13.sp, color = colors.textPrimary)
                                Text("Change ▾", fontSize = 12.sp, color = colors.brandPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                        CustomRoundedInput(value = classTime, onValueChange = { classTime = it }, placeholder = "Time (e.g. 10:00 AM)")
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                        onClick = {
                            if (title.isNotBlank() && meetingUrl.isNotBlank()) {
                                scope.launch {
                                    run { val _ent = OnlineClassEntity(
                                            schoolCode = schoolCode,
                                            title = title.trim(),
                                            gradeClass = gradeClass.trim(),
                                            subject = subject.trim(),
                                            meetingUrl = meetingUrl.trim(),
                                            classDate = classDate.trim(),
                                            classTime = classTime.trim()
                                        ); val _id = database.onlineClassDao().insertOnlineClass(_ent); val _final = if (_id > 0L) _ent.copy(id = _id) else _ent; try { com.school.manage.core.firebase.FirestoreSyncService(database).pushOnlineClass(schoolCode, _final) } catch (_: Exception) {}; _id }
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
    val batches by database.batchDao().getBatchesBySchool(schoolCode).collectAsState(initial = emptyList())
    var showQbClassMenu by remember { mutableStateOf(false) }

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
                                TextButton(onClick = { scope.launch { run { val _ent = q; database.questionBankDao().deleteQuestion(_ent); try { com.school.manage.core.firebase.FirestoreSyncService(database).deleteModuleRecord(schoolCode, "question_bank", _ent.id) } catch (_: Exception) {} } } }) {
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
                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box {
                            DropdownTriggerInput(
                                label = "Select Class",
                                value = if (gradeClass.isEmpty()) "SELECT CLASS" else gradeClass,
                                onClick = { showQbClassMenu = true }
                            )
                            DropdownMenu(expanded = showQbClassMenu, onDismissRequest = { showQbClassMenu = false }) {
                                val classList = if (batches.isEmpty()) listOf("Nursery", "LKG", "UKG", "Class 1", "Class 2", "Class 3", "Class 4") else batches.map { it.batchName }
                                classList.forEach { bName ->
                                    DropdownMenuItem(
                                        text = { Text(bName, fontWeight = FontWeight.SemiBold) },
                                        onClick = { gradeClass = bName; showQbClassMenu = false }
                                    )
                                }
                            }
                        }
                        SubjectDropdownSelector(
                            selectedClass = gradeClass,
                            selectedSubject = subject,
                            batches = batches,
                            onSubjectSelected = { subject = it }
                        )
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
                                    run { val _ent = QuestionBankEntity(
                                            schoolCode = schoolCode,
                                            gradeClass = gradeClass.trim(),
                                            subject = subject.trim(),
                                            chapterTopic = chapter.trim(),
                                            questionText = questionText.trim(),
                                            answerKey = answerKey.trim(),
                                            questionType = qType
                                        ); val _id = database.questionBankDao().insertQuestion(_ent); val _final = if (_id > 0L) _ent.copy(id = _id) else _ent; try { com.school.manage.core.firebase.FirestoreSyncService(database).pushQuestion(schoolCode, _final) } catch (_: Exception) {}; _id }
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
