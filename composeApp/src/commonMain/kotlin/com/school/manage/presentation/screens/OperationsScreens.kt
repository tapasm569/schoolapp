package com.school.manage.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.window.Dialog
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.AnnouncementEntity
import com.school.manage.core.database.entity.EnquiryEntity
import com.school.manage.core.database.entity.StaffLogEntity
import com.school.manage.core.database.entity.StudentEntity
import com.school.manage.presentation.theme.LocalSchoolColors
import kotlinx.coroutines.launch

// 1. ENQUIRY SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnquiryScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit,
    onNavigateAddStudent: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val enquiries by database.enquiryDao().getEnquiriesBySchool(schoolCode).collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var studentName by remember { mutableStateOf("") }
    var parentName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var gradeClass by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filtered = if (selectedFilter == "ALL") enquiries else enquiries.filter { it.status == selectedFilter }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("Admission Enquiries (" + enquiries.size + ")", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
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
                    for (st in listOf("ALL", "NEW", "FOLLOW_UP", "ADMITTED")) {
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
                        Text("No admission enquiries recorded yet.", color = colors.textSecondary)
                    }
                }
            } else {
                items(filtered) { enq ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.bgCard,
                        border = BorderStroke(1.dp, colors.borderCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(enq.studentName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (enq.status == "ADMITTED") colors.success.copy(alpha = 0.15f) else Color(0xFFFEF3C7)
                                ) {
                                    Text(
                                        text = enq.status,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (enq.status == "ADMITTED") colors.success else Color(0xFFB45309),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Parent: " + enq.parentName + " • Target Class: " + enq.gradeClass, fontSize = 12.sp, color = colors.textSecondary)
                            Text("Phone: " + enq.phone + " • Date: " + enq.date, fontSize = 12.sp, color = colors.brandPrimary, fontWeight = FontWeight.SemiBold)
                            if (enq.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Notes: " + enq.notes, fontSize = 11.sp, color = colors.textSecondary)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val cleanPhone = enq.phone.replace("+", "").replace(" ", "").trim()
                                            uriHandler.openUri("https://wa.me/91" + cleanPhone)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDCFCE7), contentColor = Color(0xFF15803D)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) { Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold) }

                                    if (enq.status != "ADMITTED") {
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    database.enquiryDao().insertEnquiry(enq.copy(status = "ADMITTED"))
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) { Text("Admit", fontSize = 11.sp) }
                                    }
                                }

                                TextButton(onClick = { scope.launch { database.enquiryDao().deleteEnquiry(enq) } }) {
                                    Text("Delete", color = colors.error, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }

        if (showAddDialog) {
            Dialog(onDismissRequest = { showAddDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.bgCard,
                    border = BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("New Admission Enquiry", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
                        CustomRoundedInput(value = studentName, onValueChange = { studentName = it }, placeholder = "Student Name")
                        CustomRoundedInput(value = parentName, onValueChange = { parentName = it }, placeholder = "Parent / Guardian Name")
                        CustomRoundedInput(value = phone, onValueChange = { phone = it }, placeholder = "Mobile Number")
                        CustomRoundedInput(value = gradeClass, onValueChange = { gradeClass = it }, placeholder = "Seeking Class (e.g. Nursery)")
                        CustomRoundedInput(value = notes, onValueChange = { notes = it }, placeholder = "Discussion / Background Notes")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = colors.textSecondary) }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                                onClick = {
                                    if (studentName.isNotBlank() && phone.isNotBlank()) {
                                        scope.launch {
                                            database.enquiryDao().insertEnquiry(
                                                EnquiryEntity(
                                                    schoolCode = schoolCode,
                                                    studentName = studentName.trim(),
                                                    parentName = parentName.trim(),
                                                    phone = phone.trim(),
                                                    gradeClass = gradeClass.trim(),
                                                    notes = notes.trim()
                                                )
                                            )
                                            studentName = ""
                                            parentName = ""
                                            phone = ""
                                            notes = ""
                                            showAddDialog = false
                                        }
                                    }
                                }
                            ) { Text("Save Lead") }
                        }
                    }
                }
            }
        }
    }
}

// 2. STAFF LOGS SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffLogsScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val scope = rememberCoroutineScope()
    val logs by database.staffLogDao().getStaffLogsBySchool(schoolCode).collectAsState(initial = emptyList())
    val staffList by database.staffDao().getStaffBySchool(schoolCode).collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedStaffName by remember { mutableStateOf("") }
    var selectedStaffId by remember { mutableStateOf(0L) }
    var showStaffDropdown by remember { mutableStateOf(false) }
    var checkIn by remember { mutableStateOf("09:00 AM") }
    var checkOut by remember { mutableStateOf("03:30 PM") }
    var note by remember { mutableStateOf("Conducted assigned lectures") }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("Staff Activity Logs (" + logs.size + ")", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
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

            if (logs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No staff check-in logs recorded yet. Tap + to record.", color = colors.textSecondary)
                    }
                }
            } else {
                items(logs) { log ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.bgCard,
                        border = BorderStroke(1.dp, colors.borderCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(log.staffName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                                Surface(shape = RoundedCornerShape(6.dp), color = colors.brandPrimary.copy(alpha = 0.15f)) {
                                    Text(log.status, color = colors.brandPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Check-In: " + log.checkIn + " • Check-Out: " + log.checkOut, fontSize = 12.sp, color = colors.textSecondary)
                            Text("Date: " + log.date + " • Activity: " + log.activityNote, fontSize = 12.sp, color = colors.brandPrimary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { scope.launch { database.staffLogDao().deleteStaffLog(log) } }) {
                                    Text("Remove", color = colors.error, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }

        if (showAddDialog) {
            Dialog(onDismissRequest = { showAddDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.bgCard,
                    border = BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Log Staff Check-In", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
                        Box {
                            DropdownTriggerInput(
                                label = "Select Faculty",
                                value = if (selectedStaffName.isEmpty()) "Select Teacher" else selectedStaffName,
                                onClick = { showStaffDropdown = true }
                            )
                            DropdownMenu(expanded = showStaffDropdown, onDismissRequest = { showStaffDropdown = false }) {
                                for (st in staffList) {
                                    DropdownMenuItem(
                                        text = { Text(st.name + " (" + st.role + ")") },
                                        onClick = {
                                            selectedStaffId = st.id
                                            selectedStaffName = st.name
                                            showStaffDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                        CustomRoundedInput(value = checkIn, onValueChange = { checkIn = it }, placeholder = "Check-In (e.g. 09:00 AM)")
                        CustomRoundedInput(value = checkOut, onValueChange = { checkOut = it }, placeholder = "Check-Out (e.g. 03:30 PM)")
                        CustomRoundedInput(value = note, onValueChange = { note = it }, placeholder = "Activity Note")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = colors.textSecondary) }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                                onClick = {
                                    if (selectedStaffName.isNotBlank()) {
                                        scope.launch {
                                            database.staffLogDao().insertStaffLog(
                                                StaffLogEntity(
                                                    schoolCode = schoolCode,
                                                    staffId = selectedStaffId,
                                                    staffName = selectedStaffName,
                                                    checkIn = checkIn.trim(),
                                                    checkOut = checkOut.trim(),
                                                    activityNote = note.trim()
                                                )
                                            )
                                            showAddDialog = false
                                        }
                                    }
                                }
                            ) { Text("Save Log") }
                        }
                    }
                }
            }
        }
    }
}

// 3. ANNOUNCEMENTS SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val scope = rememberCoroutineScope()
    val announcements by database.announcementDao().getAnnouncementsBySchool(schoolCode).collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var audience by remember { mutableStateOf("ALL") }
    var priority by remember { mutableStateOf("NORMAL") }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("School Announcements (" + announcements.size + ")", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
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

            if (announcements.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No notices published yet. Tap + to post a circular.", color = colors.textSecondary)
                    }
                }
            } else {
                items(announcements) { notice ->
                    val badgeColor = when (notice.priority) {
                        "URGENT" -> colors.error
                        "EVENT" -> Color(0xFFF59E0B)
                        else -> colors.brandPrimary
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.bgCard,
                        border = BorderStroke(1.dp, colors.borderCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(notice.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
                                Surface(shape = RoundedCornerShape(6.dp), color = badgeColor.copy(alpha = 0.15f)) {
                                    Text(notice.priority, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Audience: " + notice.targetAudience + " • Date: " + notice.date, fontSize = 11.sp, color = colors.textSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(notice.message, fontSize = 13.sp, color = colors.textPrimary, lineHeight = 17.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { scope.launch { database.announcementDao().deleteAnnouncement(notice) } }) {
                                    Text("Delete Notice", color = colors.error, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }

        if (showAddDialog) {
            Dialog(onDismissRequest = { showAddDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.bgCard,
                    border = BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Publish Announcement", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
                        CustomRoundedInput(value = title, onValueChange = { title = it }, placeholder = "Title (e.g. Diwali Holiday Notice)")
                        CustomRoundedInput(value = message, onValueChange = { message = it }, placeholder = "Circular content / instructions")

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (pr in listOf("NORMAL", "URGENT", "EVENT")) {
                                Surface(
                                    onClick = { priority = pr },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (priority == pr) colors.brandPrimary else colors.bgCardHover
                                ) {
                                    Text(pr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (priority == pr) Color.White else colors.textPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = colors.textSecondary) }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                                onClick = {
                                    if (title.isNotBlank() && message.isNotBlank()) {
                                        scope.launch {
                                            database.announcementDao().insertAnnouncement(
                                                AnnouncementEntity(
                                                    schoolCode = schoolCode,
                                                    title = title.trim(),
                                                    message = message.trim(),
                                                    priority = priority,
                                                    targetAudience = audience
                                                )
                                            )
                                            title = ""
                                            message = ""
                                            showAddDialog = false
                                        }
                                    }
                                }
                            ) { Text("Publish") }
                        }
                    }
                }
            }
        }
    }
}

// 4. MESSAGES SCREEN (Standard itemsIndexed in LazyRow)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val uriHandler = LocalUriHandler.current
    val students by database.studentDao().getStudentsBySchool(schoolCode).collectAsState(initial = emptyList())

    var selectedStudent by remember { mutableStateOf(null) }
    var showStudentDropdown by remember { mutableStateOf(false) }
    var customMessage by remember { mutableStateOf("") }
    var selectedTemplateIndex by remember { mutableStateOf(0) }

    val templateList = remember {
        listOf("Fee Due Reminder", "Absence Alert", "Exam Schedule", "General Circular")
    }

    LaunchedEffect(selectedTemplateIndex, selectedStudent) {
        val stName = selectedStudent?.name ?: "Student"
        customMessage = when (selectedTemplateIndex) {
            0 -> "Dear Parent, this is a friendly reminder that tuition fee for " + stName + " is due. Kindly clear dues at your earliest."
            1 -> "Dear Parent, your child " + stName + " was marked absent today without prior notice. Please reach out to the school office."
            2 -> "Dear Parent, term exams for " + stName + " are commencing soon. Please ensure regular study."
            else -> "Dear Parent of " + stName + ", please review our latest official circular on the school notice board."
        }
    }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("Parent Messages (WhatsApp)", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgApp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.bgCard,
                    border = BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("1. Select Student / Parent", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
                        Box {
                            DropdownTriggerInput(
                                label = "Select Enrolled Student",
                                value = if (selectedStudent == null) "Tap to choose student" else (selectedStudent!!.name + " (" + selectedStudent!!.gradeClass + ")"),
                                onClick = { showStudentDropdown = true }
                            )
                            DropdownMenu(expanded = showStudentDropdown, onDismissRequest = { showStudentDropdown = false }) {
                                for (s in students) {
                                    DropdownMenuItem(
                                        text = { Text(s.name + " - Class " + s.gradeClass + " (" + s.phone + ")") },
                                        onClick = {
                                            selectedStudent = s
                                            showStudentDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("2. Quick WhatsApp Templates", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(templateList) { idx, label ->
                        val isSel = selectedTemplateIndex == idx
                        Surface(
                            onClick = { selectedTemplateIndex = idx },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) colors.brandPrimary else colors.bgCardHover,
                            border = BorderStroke(1.dp, if (isSel) colors.brandPrimary else colors.borderCard)
                        ) {
                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else colors.textPrimary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.bgCard,
                    border = BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("3. Message Preview", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
                        CustomRoundedInput(
                            value = customMessage,
                            onValueChange = { customMessage = it },
                            placeholder = "Type message to parent..."
                        )

                        Button(
                            onClick = {
                                val currentStudent = selectedStudent
                                val rawPhone = if (currentStudent != null) {
                                    if (currentStudent.whatsapp.isNotBlank()) currentStudent.whatsapp else currentStudent.phone
                                } else ""

                                val cleanPhone = rawPhone.replace("+", "").replace(" ", "").trim()
                                val encoded = customMessage
                                    .replace(" ", "%20")
                                    .replace("\n", "%0A")
                                    .replace("&", "%26")

                                if (cleanPhone.isNotBlank()) {
                                    uriHandler.openUri("https://wa.me/91" + cleanPhone + "?text=" + encoded)
                                }
                            },
                            enabled = selectedStudent != null && customMessage.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
                        ) {
                            Text("💬 Send via WhatsApp", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}
