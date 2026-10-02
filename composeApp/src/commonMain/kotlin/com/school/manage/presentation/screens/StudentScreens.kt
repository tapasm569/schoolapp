package com.school.manage.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.StudentEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentListScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit,
    onNavigateAdd: () -> Unit
) {
    val students by database.studentDao().getStudentsBySchool(schoolCode).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = { Text("Students ($schoolCode)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateAdd,
                containerColor = Color(0xFF0D529C),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("+", fontSize = 28.sp)
            }
        }
    ) { padding ->
        if (students.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No students registered yet. Tap + to add.", color = Color(0xFF64748B))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(students) { student ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(student.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                                Text(student.gradeClass.ifEmpty { "Unassigned" }, color = Color(0xFF0D529C), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Mobile: ${student.phone}  •  Gender: ${student.gender}", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text("Monthly Fee: ₹${student.monthlyFee.toInt()}", fontSize = 12.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { scope.launch { database.studentDao().deleteStudent(student) } },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2), contentColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Remove", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ADD STUDENT SCREEN (Images 1 & 2)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit,
    onStudentSaved: (Long, String) -> Unit
) {
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var motherName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var aadhar by remember { mutableStateOf("") }
    var caste by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var whatsapp by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = { Text("Add Student", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8FAFC))
            )
        },
        bottomBar = {
            Surface(
                color = Color(0xFFF8FAFC),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Button(
                    onClick = {
                        if (name.isBlank() || phone.isBlank()) {
                            errorMessage = "Student Name and Mobile Number are required!"
                            return@Button
                        }
                        scope.launch {
                            val newId = database.studentDao().insertStudent(
                                StudentEntity(
                                    schoolCode = schoolCode,
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    fatherName = fatherName.trim(),
                                    motherName = motherName.trim(),
                                    dob = dob.trim(),
                                    aadharNumber = aadhar.trim(),
                                    caste = caste.trim(),
                                    gender = gender,
                                    whatsapp = whatsapp.trim(),
                                    address = address.trim(),
                                    guardianName = fatherName.ifBlank { motherName },
                                    admissionDate = "02/10/2026"
                                )
                            )
                            onStudentSaved(newId, name.trim())
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D529C))
                ) {
                    Text("Save", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile photo card
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Profile photo", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                            Text("This image will be displayed on Profile", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👤", fontSize = 24.sp)
                        }
                    }
                }
            }

            // Student Information Header
            item {
                Column {
                    Text("Student Information", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                    Text("Enter student personal details here.", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }

            item { CustomRoundedInput(value = name, onValueChange = { name = it }, placeholder = "Student Name (Required)") }
            item { CustomRoundedInput(value = fatherName, onValueChange = { fatherName = it }, placeholder = "Father name") }
            item { CustomRoundedInput(value = motherName, onValueChange = { motherName = it }, placeholder = "Mother name") }
            item {
                CustomRoundedInput(
                    value = dob,
                    onValueChange = { dob = it },
                    placeholder = "Date of Birth",
                    trailingIcon = "📅"
                )
            }
            item { CustomRoundedInput(value = aadhar, onValueChange = { aadhar = it }, placeholder = "Aadhar number") }
            item { CustomRoundedInput(value = caste, onValueChange = { caste = it }, placeholder = "Caste") }

            // Gender Selector
            item {
                Column {
                    Text("Gender", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GenderOptionButton(
                            label = "Male",
                            icon = "♂",
                            isSelected = gender == "Male",
                            onClick = { gender = "Male" }
                        )
                        GenderOptionButton(
                            label = "Female",
                            icon = "♀",
                            isSelected = gender == "Female",
                            onClick = { gender = "Female" }
                        )
                    }
                }
            }

            // Contact Information Header
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Column {
                    Text("Contact Information", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                    Text(
                        "Enter student personal details here. (Country Code Required like 91XXXXXXXXXX)",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            item { CustomRoundedInput(value = whatsapp, onValueChange = { whatsapp = it }, placeholder = "WhatsApp (Required)") }
            item { CustomRoundedInput(value = phone, onValueChange = { phone = it }, placeholder = "Mobile number (Required)") }
            item { CustomRoundedInput(value = address, onValueChange = { address = it }, placeholder = "Address") }

            if (errorMessage.isNotEmpty()) {
                item {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

// -------------------------------------------------------------
// ASSIGN BATCH SCREEN (Image 3)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignBatchScreen(
    database: AppDatabase,
    schoolCode: String,
    studentId: Long,
    studentName: String,
    onBatchAssigned: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val batches by database.batchDao().getBatchesBySchool(schoolCode).collectAsState(initial = emptyList())

    var selectedBatch by remember { mutableStateOf("") }
    var showBatchMenu by remember { mutableStateOf(false) }
    var joinDate by remember { mutableStateOf("02/10/2026") }

    var feeCategory by remember { mutableStateOf("Select Fee Category") }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var feeType by remember { mutableStateOf("Monthly") }
    var showFeeTypeMenu by remember { mutableStateOf(false) }

    var feeStartsFrom by remember { mutableStateOf("02/10/2026") }
    var feeEnds by remember { mutableStateOf("") }
    var feeAmount by remember { mutableStateOf("0") }

    var partialFeeSupported by remember { mutableStateOf(false) }
    var collectFeeOnMonthStart by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = { Text("Assign Batch", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBatchAssigned) {
                        Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8FAFC))
            )
        },
        bottomBar = {
            Surface(
                color = Color(0xFFF8FAFC),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            val existing = database.studentDao().getStudentById(studentId)
                            if (existing != null) {
                                database.studentDao().insertStudent(
                                    existing.copy(
                                        gradeClass = selectedBatch.ifEmpty { "General Batch" },
                                        monthlyFee = feeAmount.toDoubleOrNull() ?: 0.0
                                    )
                                )
                            }
                            onBatchAssigned()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D529C))
                ) {
                    Text("Save", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Blue Fee Plan Banners
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FeePlanPillBanner()
                    FeePlanPillBanner()
                }
            }

            // Student Name Centered
            item {
                Text(
                    text = studentName.ifEmpty { "Student" },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }

            // Batch Information
            item {
                Column {
                    Text("Batch Information", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                    Text("Enter batch details here", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }

            // Select Batch Dropdown
            item {
                Box {
                    DropdownTriggerInput(
                        label = "Select Batch",
                        value = if (selectedBatch.isEmpty()) "Select Batch" else selectedBatch,
                        onClick = { showBatchMenu = true }
                    )
                    DropdownMenu(
                        expanded = showBatchMenu,
                        onDismissRequest = { showBatchMenu = false }
                    ) {
                        if (batches.isEmpty()) {
                            listOf("Class 1", "Class 2", "Class 3", "General Batch").forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b) },
                                    onClick = { selectedBatch = b; showBatchMenu = false }
                                )
                            }
                        } else {
                            batches.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b.batchName) },
                                    onClick = { selectedBatch = b.batchName; showBatchMenu = false }
                                )
                            }
                        }
                    }
                }
            }

            item {
                CustomRoundedInput(
                    value = joinDate,
                    onValueChange = { joinDate = it },
                    placeholder = "Join Date",
                    trailingIcon = "📅"
                )
            }

            // Fee Item #1 Card Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fee Item #1", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                    Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                }
            }

            // Select Fee Category
            item {
                Box {
                    DropdownTriggerInput(
                        label = "Select Fee Category",
                        value = feeCategory,
                        onClick = { showCategoryMenu = true }
                    )
                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        listOf("Tuition Fee", "Admission Fee", "Monthly Fee", "Exam Fee").forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = { feeCategory = cat; showCategoryMenu = false }
                            )
                        }
                    }
                }
            }

            // Fee Type
            item {
                Box {
                    DropdownTriggerInput(
                        label = "Fee type",
                        value = feeType,
                        onClick = { showFeeTypeMenu = true }
                    )
                    DropdownMenu(
                        expanded = showFeeTypeMenu,
                        onDismissRequest = { showFeeTypeMenu = false }
                    ) {
                        listOf("Monthly", "One Time", "Annual", "Quarterly").forEach { ft ->
                            DropdownMenuItem(
                                text = { Text(ft) },
                                onClick = { feeType = ft; showFeeTypeMenu = false }
                            )
                        }
                    }
                }
            }

            // Dates Row (Starts From & Ends)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        CustomRoundedInput(
                            value = feeStartsFrom,
                            onValueChange = { feeStartsFrom = it },
                            placeholder = "Fee starts from",
                            trailingIcon = "📅"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        CustomRoundedInput(
                            value = feeEnds,
                            onValueChange = { feeEnds = it },
                            placeholder = "Ends (optional)",
                            trailingIcon = "📅"
                        )
                    }
                }
            }

            // Fee Amount
            item {
                CustomRoundedInput(
                    value = feeAmount,
                    onValueChange = { feeAmount = it },
                    placeholder = "Fee Amount"
                )
            }

            // Switches
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Partial Fee Supported", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                    Switch(
                        checked = partialFeeSupported,
                        onCheckedChange = { partialFeeSupported = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0D529C))
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Collect Fee On Month Start", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                    Switch(
                        checked = collectFeeOnMonthStart,
                        onCheckedChange = { collectFeeOnMonthStart = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0D529C))
                    )
                }
            }

            // Add Fee Item Button
            item {
                OutlinedButton(
                    onClick = { /* Add additional fee item */ },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0D529C))
                ) {
                    Text("Add Fee Item", color = Color(0xFF0D529C), fontWeight = FontWeight.Bold)
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

// -------------------------------------------------------------
// REUSABLE UI HELPERS FOR FORM INPUTS
// -------------------------------------------------------------
@Composable
fun CustomRoundedInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    trailingIcon: String? = null
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder, color = Color(0xFF94A3B8), fontSize = 14.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            if (trailingIcon != null) {
                Text(trailingIcon, fontSize = 18.sp, color = Color(0xFF475569))
            }
        }
    }
}

@Composable
fun DropdownTriggerInput(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(label, fontSize = 10.sp, color = Color(0xFF64748B))
                Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
            }
            Text("▾", fontSize = 14.sp, color = Color(0xFF475569))
        }
    }
}

@Composable
fun GenderOptionButton(
    label: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isSelected) Color(0xFFF97316) else Color(0xFFE2E8F0)
        ),
        modifier = Modifier.width(110.dp).height(44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(icon, fontSize = 14.sp, color = if (isSelected) Color(0xFFF97316) else Color(0xFF64748B), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color(0xFFF97316) else Color(0xFF0F172A))
        }
    }
}

@Composable
fun FeePlanPillBanner() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFDBEAFE),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⏱", fontSize = 12.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "FEE PLAN • what this student owes",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E40AF)
            )
        }
    }
}
