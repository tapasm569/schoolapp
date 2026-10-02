package com.school.manage.presentation.screens

import androidx.compose.foundation.background
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
import com.school.manage.core.database.entity.FeeRecordEntity
import com.school.manage.core.database.entity.StudentEntity
import com.school.manage.presentation.theme.LocalSchoolColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentListScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit,
    onNavigateAdd: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val students by database.studentDao().getStudentsBySchool(schoolCode).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val categorizedStudents = remember(students) {
        students.groupBy { it.gradeClass.ifBlank { "Unassigned Class" } }
    }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("Students Directory (" + students.size + ")", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgApp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateAdd,
                containerColor = colors.brandPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("+", fontSize = 28.sp)
            }
        }
    ) { padding ->
        if (students.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No students enrolled yet. Tap + to register.", color = colors.textSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(modifier = Modifier.height(6.dp)) }

                categorizedStudents.forEach { (className, studentGroup) ->
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.brandPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Class: " + className, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = colors.brandPrimary)
                                Text(studentGroup.size.toString() + " Students", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colors.brandPrimary)
                            }
                        }
                    }

                    items(studentGroup) { student ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.bgCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(student.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                                    Text("Admission Fee: ₹" + student.admissionFee.toInt(), color = colors.brandPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Phone: " + student.phone + "  •  Gender: " + student.gender + "  •  DOB: " + student.dob.ifEmpty { "N/A" }, fontSize = 12.sp, color = colors.textSecondary)
                                Text("Monthly Tuition: ₹" + student.monthlyFee.toInt() + "/mo", fontSize = 12.sp, color = colors.success, fontWeight = FontWeight.Bold)

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Button(
                                        onClick = { scope.launch { database.studentDao().deleteStudent(student) } },
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.error.copy(alpha = 0.15f), contentColor = colors.error),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Remove", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(4.dp)) }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit,
    onStudentSaved: (Long, String) -> Unit
) {
    val colors = LocalSchoolColors.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var motherName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var showDobPicker by remember { mutableStateOf(false) }
    var aadhar by remember { mutableStateOf("") }
    var caste by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var whatsapp by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("Add Student", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgApp)
            )
        },
        bottomBar = {
            Surface(
                color = colors.bgApp,
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
                    colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary)
                ) {
                    Text("Save & Assign Class", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.bgCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Profile photo", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                            Text("This image will be displayed on Profile", fontSize = 12.sp, color = colors.textSecondary)
                        }
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(colors.borderCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👤", fontSize = 24.sp)
                        }
                    }
                }
            }

            item {
                Column {
                    Text("Student Information", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                    Text("Enter student personal details here.", fontSize = 12.sp, color = colors.textSecondary)
                }
            }

            item { CustomRoundedInput(value = name, onValueChange = { name = it }, placeholder = "Student Name (Required)") }
            item { CustomRoundedInput(value = fatherName, onValueChange = { fatherName = it }, placeholder = "Father name") }
            item { CustomRoundedInput(value = motherName, onValueChange = { motherName = it }, placeholder = "Mother name") }

            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.inputBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.inputBorder),
                    modifier = Modifier.fillMaxWidth().clickable { showDobPicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (dob.isEmpty()) "Date of Birth (Tap to select)" else dob,
                            fontSize = 14.sp,
                            color = if (dob.isEmpty()) colors.textSecondary else colors.textPrimary
                        )
                        Text("📅", fontSize = 18.sp)
                    }
                }
            }

            item { CustomRoundedInput(value = aadhar, onValueChange = { aadhar = it }, placeholder = "Aadhar number") }
            item { CustomRoundedInput(value = caste, onValueChange = { caste = it }, placeholder = "Caste") }

            item {
                Column {
                    Text("Gender", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
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

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Column {
                    Text("Contact Information", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                    Text(
                        "Enter student personal details here. (Country Code Required like 91XXXXXXXXXX)",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }

            item { CustomRoundedInput(value = whatsapp, onValueChange = { whatsapp = it }, placeholder = "WhatsApp (Required)") }
            item { CustomRoundedInput(value = phone, onValueChange = { phone = it }, placeholder = "Mobile number (Required)") }
            item { CustomRoundedInput(value = address, onValueChange = { address = it }, placeholder = "Address") }

            if (errorMessage.isNotEmpty()) {
                item {
                    Text(errorMessage, color = colors.error, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }

        if (showDobPicker) {
            ModernDatePickerDialog(
                currentDate = "02/10/2026",
                onDateSelected = { dob = it },
                onDismiss = { showDobPicker = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignBatchScreen(
    database: AppDatabase,
    schoolCode: String,
    studentId: Long,
    studentName: String,
    onBatchAssigned: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val scope = rememberCoroutineScope()
    val batches by database.batchDao().getBatchesBySchool(schoolCode).collectAsState(initial = emptyList())

    var selectedBatch by remember { mutableStateOf("") }
    var showBatchMenu by remember { mutableStateOf(false) }

    var admissionFeeInput by remember { mutableStateOf("") }
    var tuitionFeeInput by remember { mutableStateOf("") }

    val admissionFee = admissionFeeInput.toDoubleOrNull() ?: 0.0
    val monthlyTuition = tuitionFeeInput.toDoubleOrNull() ?: 0.0
    val totalInitialPayable = admissionFee + monthlyTuition

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("Assign Batch & Fee Plan", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBatchAssigned) {
                        Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgApp)
            )
        },
        bottomBar = {
            Surface(
                color = colors.bgApp,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            val existing = database.studentDao().getStudentById(studentId)
                            if (existing != null) {
                                database.studentDao().insertStudent(
                                    existing.copy(
                                        gradeClass = selectedBatch.ifEmpty { "General Class" },
                                        admissionFee = admissionFee,
                                        monthlyFee = monthlyTuition
                                    )
                                )
                                if (totalInitialPayable > 0.0) {
                                    database.feeDao().insertFeeRecord(
                                        FeeRecordEntity(
                                            schoolCode = schoolCode,
                                            studentId = studentId,
                                            studentName = studentName,
                                            gradeClass = selectedBatch.ifEmpty { "General Class" },
                                            amountPaid = totalInitialPayable,
                                            paymentDate = "02/10/2026",
                                            feeMonth = "10/2026",
                                            paymentMode = "CASH",
                                            remarks = "Admission Fee + 1st Month Tuition"
                                        )
                                    )
                                }
                            }
                            onBatchAssigned()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary)
                ) {
                    Text("Confirm & Save", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
            item { FeePlanPillBanner() }

            item {
                Text(
                    text = studentName.ifEmpty { "Student" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }

            item {
                Column {
                    Text("Batch / Class Selection", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                    Text("Select from classes created in School Classes module", fontSize = 12.sp, color = colors.textSecondary)
                }
            }

            item {
                Box {
                    DropdownTriggerInput(
                        label = "Select Class / Batch",
                        value = if (selectedBatch.isEmpty()) "Select Class" else selectedBatch,
                        onClick = { showBatchMenu = true }
                    )
                    DropdownMenu(
                        expanded = showBatchMenu,
                        onDismissRequest = { showBatchMenu = false }
                    ) {
                        if (batches.isEmpty()) {
                            listOf("Nursery", "LKG", "UKG", "Class 1", "Class 2", "Class 5").forEach { b ->
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
                Column {
                    Text("Fee Structure Setup", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                    Text("Set admission and monthly tuition parameters", fontSize = 12.sp, color = colors.textSecondary)
                }
            }

            item {
                CustomRoundedInput(
                    value = admissionFeeInput,
                    onValueChange = { admissionFeeInput = it },
                    placeholder = "ADMISSION FEE (Yearly Payment) e.g. 5000"
                )
            }

            item {
                CustomRoundedInput(
                    value = tuitionFeeInput,
                    onValueChange = { tuitionFeeInput = it },
                    placeholder = "TUITION FEE (Monthly Fee) e.g. 800"
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.brandPrimary.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.brandPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "FEE BREAKDOWN & AUTO-SUM",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.brandPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Admission Fee (Yearly):", fontSize = 13.sp, color = colors.textSecondary)
                            Text("₹" + admissionFee.toInt(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("1st Month Tuition (Auto-Sum):", fontSize = 13.sp, color = colors.textSecondary)
                            Text("₹" + monthlyTuition.toInt(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = colors.brandPrimary.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Initial Due at Admission:", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = colors.brandPrimary)
                            Text("₹" + totalInitialPayable.toInt(), fontSize = 16.sp, fontWeight = FontWeight.Black, color = colors.success)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "ℹ Student pays initial admission total now. Monthly tuition fee (₹" + monthlyTuition.toInt() + ") will be due starting from next month.",
                            fontSize = 11.sp,
                            color = colors.brandPrimary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}
