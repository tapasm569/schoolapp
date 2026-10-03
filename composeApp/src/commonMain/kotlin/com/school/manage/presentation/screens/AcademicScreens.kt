package com.school.manage.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.*
import com.school.manage.presentation.theme.LocalSchoolColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamsScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val scope = rememberCoroutineScope()
    val exams by database.examDao().getExamsBySchool(schoolCode).collectAsState(initial = emptyList())
    val batches by database.batchDao().getBatchesBySchool(schoolCode).collectAsState(initial = emptyList())
    val students by database.studentDao().getStudentsBySchool(schoolCode).collectAsState(initial = emptyList())

    var showAddExamModal by remember { mutableStateOf(false) }
    var selectedExamForMarks by remember { mutableStateOf<ExamEntity?>(null) }

    var examTitle by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var maxMarks by remember { mutableStateOf("100") }
    var examDate by remember { mutableStateOf("03/10/2026") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showClassDropdown by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("Exams & Marks (" + exams.size + ")", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgApp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddExamModal = true },
                containerColor = colors.brandPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("+", fontSize = 28.sp)
            }
        }
    ) { padding ->
        if (exams.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No exams scheduled yet. Tap + to create one.", color = colors.textSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                items(exams) { exam ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.bgCard,
                        border = BorderStroke(1.dp, colors.borderCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(exam.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.brandPrimary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = exam.gradeClass,
                                        color = colors.brandPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Subject: " + exam.subject + "  •  Max Marks: " + exam.maxMarks.toInt(), fontSize = 13.sp, color = colors.textSecondary)
                            Text("Date: " + exam.examDate, fontSize = 12.sp, color = colors.textSecondary)

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Button(
                                    onClick = { selectedExamForMarks = exam },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Record / View Marks", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { scope.launch { database.examDao().deleteExam(exam) } },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.error.copy(alpha = 0.15f), contentColor = colors.error),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Delete", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }

        if (showAddExamModal) {
            AlertDialog(
                onDismissRequest = { showAddExamModal = false },
                containerColor = colors.bgCard,
                title = { Text("Schedule New Exam", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CustomRoundedInput(value = examTitle, onValueChange = { examTitle = it }, placeholder = "Exam Title (e.g. Unit Test 1)")

                        Box {
                            DropdownTriggerInput(
                                label = "Select Class",
                                value = if (selectedClass.isEmpty()) "Select Class" else selectedClass,
                                onClick = { showClassDropdown = true }
                            )
                            DropdownMenu(expanded = showClassDropdown, onDismissRequest = { showClassDropdown = false }) {
                                if (batches.isEmpty()) {
                                    listOf("Nursery", "LKG", "UKG", "Class 1", "Class 2", "Class 5").forEach { b ->
                                        DropdownMenuItem(text = { Text(b) }, onClick = { selectedClass = b; showClassDropdown = false })
                                    }
                                } else {
                                    batches.forEach { b ->
                                        DropdownMenuItem(text = { Text(b.batchName) }, onClick = { selectedClass = b.batchName; showClassDropdown = false })
                                    }
                                }
                            }
                        }

                        CustomRoundedInput(value = subject, onValueChange = { subject = it }, placeholder = "Subject (e.g. Mathematics)")
                        CustomRoundedInput(value = maxMarks, onValueChange = { maxMarks = it }, placeholder = "Max Marks (e.g. 100)")

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.inputBg,
                            border = BorderStroke(1.dp, colors.inputBorder),
                            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                        ) {
                            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Exam Date: " + examDate, fontSize = 13.sp, color = colors.textPrimary)
                                Text("📅", fontSize = 16.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                        onClick = {
                            if (examTitle.isNotBlank() && selectedClass.isNotBlank()) {
                                scope.launch {
                                    database.examDao().insertExam(
                                        ExamEntity(
                                            schoolCode = schoolCode,
                                            title = examTitle.trim(),
                                            gradeClass = selectedClass,
                                            subject = subject.trim(),
                                            examDate = examDate,
                                            maxMarks = maxMarks.toDoubleOrNull() ?: 100.0
                                        )
                                    )
                                    examTitle = ""
                                    subject = ""
                                    showAddExamModal = false
                                }
                            }
                        }
                    ) { Text("Save Exam") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddExamModal = false }) { Text("Cancel", color = colors.textSecondary) }
                }
            )
        }

        selectedExamForMarks?.let { exam ->
            val classStudents = students.filter { it.gradeClass.equals(exam.gradeClass, ignoreCase = true) }
            val existingMarks by database.examDao().getMarksByExam(exam.id).collectAsState(initial = emptyList())
            val marksMap = remember(exam.id) { mutableStateMapOf<Long, String>() }

            LaunchedEffect(exam.id, existingMarks) {
                existingMarks.forEach { m ->
                    marksMap[m.studentId] = m.marksObtained.toInt().toString()
                }
            }

            ModalBottomSheet(
                onDismissRequest = { selectedExamForMarks = null },
                containerColor = colors.bgCard,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(exam.title + " - Marks (" + exam.gradeClass + ")", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = colors.textPrimary)
                    Text("Subject: " + exam.subject + " • Max Marks: " + exam.maxMarks.toInt(), fontSize = 12.sp, color = colors.textSecondary)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (classStudents.isEmpty()) {
                        Text("No students enrolled in " + exam.gradeClass + " to grade.", fontSize = 13.sp, color = colors.textSecondary)
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(classStudents) { st ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = colors.bgCardHover,
                                    border = BorderStroke(1.dp, colors.borderCard),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(st.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
                                            Text("Roll No: " + st.rollNo.ifEmpty { "N/A" }, fontSize = 11.sp, color = colors.textSecondary)
                                        }

                                        val currentVal = marksMap[st.id] ?: ""
                                        val marksNum = currentVal.toDoubleOrNull() ?: 0.0
                                        val pct = if (exam.maxMarks > 0) (marksNum / exam.maxMarks) * 100 else 0.0
                                        val gradeStr = when {
                                            pct >= 90 -> "A+"
                                            pct >= 80 -> "A"
                                            pct >= 70 -> "B+"
                                            pct >= 60 -> "B"
                                            pct >= 50 -> "C"
                                            else -> "F"
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedTextField(
                                                value = currentVal,
                                                onValueChange = { marksMap[st.id] = it },
                                                placeholder = { Text("0") },
                                                singleLine = true,
                                                modifier = Modifier.width(76.dp)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (gradeStr != "F") colors.success.copy(alpha = 0.15f) else colors.error.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = gradeStr,
                                                    color = if (gradeStr != "F") colors.success else colors.error,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                val marksList = classStudents.map { st ->
                                    val obt = (marksMap[st.id] ?: "0").toDoubleOrNull() ?: 0.0
                                    val pct = if (exam.maxMarks > 0) (obt / exam.maxMarks) * 100 else 0.0
                                    val gr = when {
                                        pct >= 90 -> "A+"
                                        pct >= 80 -> "A"
                                        pct >= 70 -> "B+"
                                        pct >= 60 -> "B"
                                        pct >= 50 -> "C"
                                        else -> "F"
                                    }
                                    ExamMarksEntity(
                                        examId = exam.id,
                                        studentId = st.id,
                                        studentName = st.name,
                                        marksObtained = obt,
                                        grade = gr
                                    )
                                }
                                database.examDao().insertMarks(marksList)
                                selectedExamForMarks = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary)
                    ) {
                        Text("Save Marks", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (showDatePicker) {
            ModernDatePickerDialog(currentDate = examDate, onDateSelected = { examDate = it }, onDismiss = { showDatePicker = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdaysScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val students by database.studentDao().getStudentsBySchool(schoolCode).collectAsState(initial = emptyList())

    val todayDay = "03"
    val todayMonth = "10"

    val todayBirthdaysStudents = students.filter { 
        it.dob.startsWith(todayDay + "/" + todayMonth) || it.dob.startsWith("3/10") || it.dob.startsWith(todayDay + "-10") || it.dob.startsWith("3-10")
    }
    val otherStudentsWithDob = students.filter { 
        it.dob.isNotBlank() && !(it.dob.startsWith(todayDay + "/" + todayMonth) || it.dob.startsWith("3/10") || it.dob.startsWith(todayDay + "-10") || it.dob.startsWith("3-10"))
    }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("Birthdays 🎂", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
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
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎉", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Today's Celebrations (" + todayBirthdaysStudents.size + ")",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color(0xFF92400E)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (todayBirthdaysStudents.isEmpty()) "No birthdays today (03 Oct). Check upcoming dates below!" else "Wish your students a wonderful birthday today!",
                            fontSize = 12.sp,
                            color = Color(0xFF78350F)
                        )
                    }
                }
            }

            if (todayBirthdaysStudents.isNotEmpty()) {
                item { Text("Today's Birthdays", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary) }
                items(todayBirthdaysStudents) { st ->
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
                                Text(st.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                                Text("Class: " + st.gradeClass + " • Phone: " + st.phone, fontSize = 12.sp, color = colors.textSecondary)
                                Text("DOB: " + st.dob, fontSize = 12.sp, color = colors.brandPrimary, fontWeight = FontWeight.Bold)
                            }
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFDCFCE7)) {
                                Text("🎂 Wish Now", color = Color(0xFF15803D), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }
                }
            }

            item {
                Text("Upcoming Birthdays Directory (" + otherStudentsWithDob.size + ")", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
            }

            if (otherStudentsWithDob.isEmpty() && todayBirthdaysStudents.isEmpty()) {
                item {
                    Text("No date-of-birth records saved yet. Add student DOB when registering.", fontSize = 13.sp, color = colors.textSecondary)
                }
            } else {
                items(otherStudentsWithDob) { st ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.bgCard,
                        border = BorderStroke(1.dp, colors.borderCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(st.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
                                Text("Class: " + st.gradeClass, fontSize = 11.sp, color = colors.textSecondary)
                            }
                            Text("📅 " + st.dob, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.brandPrimary)
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
fun HomeworkScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val scope = rememberCoroutineScope()
    val homeworkList by database.homeworkDao().getHomeworkBySchool(schoolCode).collectAsState(initial = emptyList())
    val batches by database.batchDao().getBatchesBySchool(schoolCode).collectAsState(initial = emptyList())

    var selectedClassFilter by remember { mutableStateOf("ALL") }
    var showAddModal by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("04/10/2026") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showClassMenu by remember { mutableStateOf(false) }

    val filteredList = if (selectedClassFilter == "ALL") homeworkList else homeworkList.filter { it.gradeClass == selectedClassFilter }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("Home Works (" + homeworkList.size + ")", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgApp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddModal = true },
                containerColor = colors.brandPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("+", fontSize = 28.sp)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Surface(
                            onClick = { selectedClassFilter = "ALL" },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedClassFilter == "ALL") colors.brandPrimary else colors.bgCardHover,
                            border = BorderStroke(1.dp, if (selectedClassFilter == "ALL") colors.brandPrimary else colors.borderCard)
                        ) {
                            Text("All Classes", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedClassFilter == "ALL") Color.White else colors.textPrimary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                    items(batches) { b ->
                        val isSel = selectedClassFilter == b.batchName
                        Surface(
                            onClick = { selectedClassFilter = b.batchName },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) colors.brandPrimary else colors.bgCardHover,
                            border = BorderStroke(1.dp, if (isSel) colors.brandPrimary else colors.borderCard)
                        ) {
                            Text(b.batchName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else colors.textPrimary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No homework assigned for this selection.", color = colors.textSecondary)
                    }
                }
            } else {
                items(filteredList) { hw ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.bgCard,
                        border = BorderStroke(1.dp, colors.borderCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(hw.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                                Surface(shape = RoundedCornerShape(6.dp), color = colors.brandPrimary.copy(alpha = 0.15f)) {
                                    Text(hw.gradeClass, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.brandPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Subject: " + hw.subject, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.brandPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(hw.description, fontSize = 13.sp, color = colors.textSecondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Due Date: " + hw.dueDate, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.error)
                                TextButton(onClick = { scope.launch { database.homeworkDao().deleteHomework(hw) } }) {
                                    Text("Delete", color = colors.error, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }

        if (showAddModal) {
            AlertDialog(
                onDismissRequest = { showAddModal = false },
                containerColor = colors.bgCard,
                title = { Text("Assign New Homework", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box {
                            DropdownTriggerInput(
                                label = "Select Class",
                                value = if (selectedClass.isEmpty()) "Select Class" else selectedClass,
                                onClick = { showClassMenu = true }
                            )
                            DropdownMenu(expanded = showClassMenu, onDismissRequest = { showClassMenu = false }) {
                                if (batches.isEmpty()) {
                                    listOf("Nursery", "LKG", "UKG", "Class 1", "Class 2").forEach { b ->
                                        DropdownMenuItem(text = { Text(b) }, onClick = { selectedClass = b; showClassMenu = false })
                                    }
                                } else {
                                    batches.forEach { b ->
                                        DropdownMenuItem(text = { Text(b.batchName) }, onClick = { selectedClass = b.batchName; showClassMenu = false })
                                    }
                                }
                            }
                        }
                        CustomRoundedInput(value = subject, onValueChange = { subject = it }, placeholder = "Subject (e.g. English)")
                        CustomRoundedInput(value = title, onValueChange = { title = it }, placeholder = "Assignment Title")
                        CustomRoundedInput(value = description, onValueChange = { description = it }, placeholder = "Description / Instructions")

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.inputBg,
                            border = BorderStroke(1.dp, colors.inputBorder),
                            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                        ) {
                            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Due Date: " + dueDate, fontSize = 13.sp, color = colors.textPrimary)
                                Text("📅", fontSize = 16.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                        onClick = {
                            if (title.isNotBlank() && selectedClass.isNotBlank()) {
                                scope.launch {
                                    database.homeworkDao().insertHomework(
                                        HomeworkEntity(
                                            schoolCode = schoolCode,
                                            gradeClass = selectedClass,
                                            subject = subject.trim(),
                                            title = title.trim(),
                                            description = description.trim(),
                                            assignedDate = "03/10/2026",
                                            dueDate = dueDate
                                        )
                                    )
                                    title = ""
                                    description = ""
                                    subject = ""
                                    showAddModal = false
                                }
                            }
                        }
                    ) { Text("Assign") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddModal = false }) { Text("Cancel", color = colors.textSecondary) }
                }
            )
        }

        if (showDatePicker) {
            ModernDatePickerDialog(currentDate = dueDate, onDateSelected = { dueDate = it }, onDismiss = { showDatePicker = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassworkScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val scope = rememberCoroutineScope()
    val classworkList by database.classworkDao().getClassworkBySchool(schoolCode).collectAsState(initial = emptyList())
    val batches by database.batchDao().getBatchesBySchool(schoolCode).collectAsState(initial = emptyList())

    var selectedClassFilter by remember { mutableStateOf("ALL") }
    var showAddModal by remember { mutableStateOf(false) }

    var topicTitle by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf("") }
    var logDate by remember { mutableStateOf("03/10/2026") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showClassMenu by remember { mutableStateOf(false) }

    val filteredList = if (selectedClassFilter == "ALL") classworkList else classworkList.filter { it.gradeClass == selectedClassFilter }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("Class Works (Daily Diary)", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgApp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddModal = true },
                containerColor = colors.brandPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("+", fontSize = 28.sp)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Surface(
                            onClick = { selectedClassFilter = "ALL" },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedClassFilter == "ALL") colors.brandPrimary else colors.bgCardHover,
                            border = BorderStroke(1.dp, if (selectedClassFilter == "ALL") colors.brandPrimary else colors.borderCard)
                        ) {
                            Text("All Classes", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedClassFilter == "ALL") Color.White else colors.textPrimary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                    items(batches) { b ->
                        val isSel = selectedClassFilter == b.batchName
                        Surface(
                            onClick = { selectedClassFilter = b.batchName },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) colors.brandPrimary else colors.bgCardHover,
                            border = BorderStroke(1.dp, if (isSel) colors.brandPrimary else colors.borderCard)
                        ) {
                            Text(b.batchName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else colors.textPrimary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No classwork logged yet. Tap + to record today's lesson.", color = colors.textSecondary)
                    }
                }
            } else {
                items(filteredList) { cw ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.bgCard,
                        border = BorderStroke(1.dp, colors.borderCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(cw.topicTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDCFCE7)) {
                                    Text(cw.gradeClass, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Subject: " + cw.subject + "  •  Date: " + cw.date, fontSize = 12.sp, color = colors.brandPrimary, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(cw.summary, fontSize = 13.sp, color = colors.textSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { scope.launch { database.classworkDao().deleteClasswork(cw) } }) {
                                    Text("Remove", color = colors.error, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }

        if (showAddModal) {
            AlertDialog(
                onDismissRequest = { showAddModal = false },
                containerColor = colors.bgCard,
                title = { Text("Log Daily Classwork", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box {
                            DropdownTriggerInput(
                                label = "Select Class",
                                value = if (selectedClass.isEmpty()) "Select Class" else selectedClass,
                                onClick = { showClassMenu = true }
                            )
                            DropdownMenu(expanded = showClassMenu, onDismissRequest = { showClassMenu = false }) {
                                if (batches.isEmpty()) {
                                    listOf("Nursery", "LKG", "UKG", "Class 1", "Class 2").forEach { b ->
                                        DropdownMenuItem(text = { Text(b) }, onClick = { selectedClass = b; showClassMenu = false })
                                    }
                                } else {
                                    batches.forEach { b ->
                                        DropdownMenuItem(text = { Text(b.batchName) }, onClick = { selectedClass = b.batchName; showClassMenu = false })
                                    }
                                }
                            }
                        }
                        CustomRoundedInput(value = subject, onValueChange = { subject = it }, placeholder = "Subject (e.g. Science)")
                        CustomRoundedInput(value = topicTitle, onValueChange = { topicTitle = it }, placeholder = "Topic / Chapter (e.g. Photosynthesis)")
                        CustomRoundedInput(value = summary, onValueChange = { summary = it }, placeholder = "Summary of lessons taught today")

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.inputBg,
                            border = BorderStroke(1.dp, colors.inputBorder),
                            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                        ) {
                            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Lesson Date: " + logDate, fontSize = 13.sp, color = colors.textPrimary)
                                Text("📅", fontSize = 16.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                        onClick = {
                            if (topicTitle.isNotBlank() && selectedClass.isNotBlank()) {
                                scope.launch {
                                    database.classworkDao().insertClasswork(
                                        ClassworkEntity(
                                            schoolCode = schoolCode,
                                            gradeClass = selectedClass,
                                            subject = subject.trim(),
                                            topicTitle = topicTitle.trim(),
                                            summary = summary.trim(),
                                            date = logDate
                                        )
                                    )
                                    topicTitle = ""
                                    summary = ""
                                    subject = ""
                                    showAddModal = false
                                }
                            }
                        }
                    ) { Text("Save Log") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddModal = false }) { Text("Cancel", color = colors.textSecondary) }
                }
            )
        }

        if (showDatePicker) {
            ModernDatePickerDialog(currentDate = logDate, onDateSelected = { logDate = it }, onDismiss = { showDatePicker = false })
        }
    }
}
