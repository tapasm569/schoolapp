package com.school.manage.presentation.screens
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.school.manage.core.database.entity.BatchEntity
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.BorderStroke

import com.school.manage.core.utils.getSystemCurrentDate
import com.school.manage.core.utils.rememberPhotoPickerLauncher
import com.school.manage.core.utils.ProfileAvatar
import com.school.manage.core.firebase.FirestoreSyncService

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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        ProfileAvatar(
                                            photoBase64 = student.photoBase64,
                                            name = student.name,
                                            fallbackEmoji = "🎓",
                                            size = 40.dp,
                                            cornerRadius = 10.dp
                                        )
                                        Column {
                                            Text(student.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                                            Text("Roll: ${student.rollNo.ifEmpty { "N/A" }}", fontSize = 11.sp, color = colors.textSecondary)
                                        }
                                    }
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
    var photoBase64 by remember { mutableStateOf("") }
    val pickStudentPhoto = rememberPhotoPickerLauncher { photoBase64 = it }
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

                        item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.bgCard,
                    border = BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier.fillMaxWidth().clickable { pickStudentPhoto() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ProfileAvatar(
                            photoBase64 = photoBase64,
                            name = name,
                            fallbackEmoji = "🎓",
                            size = 56.dp,
                            cornerRadius = 14.dp,
                            showEditBadge = true
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Student Photo", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                            Text(if (photoBase64.isEmpty()) "Tap to upload photo" else "Photo attached successfully", fontSize = 12.sp, color = colors.brandPrimary)
                        }
                    }
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
                currentDate = dob.ifEmpty { getSystemCurrentDate().formatted },
                onDateSelected = { dob = it },
                onDismiss = { showDobPicker = false }
            )
        }
    }
}

data class AdmissionFeePart(
    val name: String,
    val amount: Double,
    val isSelected: Boolean = true
)

private fun parseFeeTemplate(raw: String): List<AdmissionFeePart> {
    if (raw.isBlank()) return emptyList()
    return raw.split("|").mapNotNull { entry ->
        val parts = entry.split(":")
        if (parts.size >= 2) {
            val name = parts[0].trim()
            val amt = parts[1].trim().toDoubleOrNull() ?: 0.0
            if (name.isNotEmpty()) AdmissionFeePart(name, amt, true) else null
        } else null
    }
}

private fun encodeFeeTemplate(items: List<AdmissionFeePart>): String {
    return items.joinToString("|") { "${it.name.replace("|", "").replace(":", "")}:${it.amount}" }
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
    val sysToday = remember { getSystemCurrentDate() }
    val batches by database.batchDao().getBatchesBySchool(schoolCode).collectAsState(initial = emptyList())

    var selectedBatch by remember { mutableStateOf("") }
    var showBatchMenu by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }

    // Starts empty until a class template is loaded or items are manually added
    var feeParts by remember { mutableStateOf<List<AdmissionFeePart>>(emptyList()) }
    var tuitionFeeInput by remember { mutableStateOf("") }
    var collectTuitionAtAdmission by remember { mutableStateOf(true) }

    // Inline add extra item states
    var showInlineAddItem by remember { mutableStateOf(false) }
    var newPartName by remember { mutableStateOf("") }
    var newPartAmount by remember { mutableStateOf("") }

    // Auto-fill saved template whenever selectedBatch changes
    LaunchedEffect(selectedBatch, batches) {
        if (selectedBatch.isNotEmpty()) {
            val matched = batches.find { it.batchName.equals(selectedBatch, ignoreCase = true) }
            if (matched != null) {
                feeParts = parseFeeTemplate(matched.feeTemplateRaw)
                tuitionFeeInput = if (matched.defaultMonthlyTuition > 0.0) {
                    matched.defaultMonthlyTuition.toInt().toString()
                } else {
                    ""
                }
                collectTuitionAtAdmission = matched.collectTuitionAtAdmission
            } else {
                feeParts = emptyList()
                tuitionFeeInput = ""
            }
        }
    }

    val admissionFeeTotal = feeParts.filter { it.isSelected }.sumOf { it.amount }
    val monthlyTuition = tuitionFeeInput.toDoubleOrNull() ?: 0.0
    val firstMonthTuitionDueNow = if (collectTuitionAtAdmission) monthlyTuition else 0.0
    val totalInitialPayable = admissionFeeTotal + firstMonthTuitionDueNow

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Assign Batch & Fee Plan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = colors.textPrimary
                    )
                },
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
                            val targetClass = selectedBatch.ifEmpty { "General Class" }
                            val existing = database.studentDao().getStudentById(studentId)
                            if (existing != null) {
                                database.studentDao().insertStudent(
                                    existing.copy(
                                        gradeClass = targetClass,
                                        admissionFee = admissionFeeTotal,
                                        monthlyFee = monthlyTuition
                                    )
                                )
                                if (totalInitialPayable > 0.0) {
                                    val selectedItemsDesc = feeParts
                                        .filter { it.isSelected }
                                        .joinToString(", ") { "${it.name}(₹${it.amount.toInt()})" }
                                    val tuitionRemark = if (collectTuitionAtAdmission) {
                                        " + 1st Month Tuition(₹${monthlyTuition.toInt()})"
                                    } else {
                                        " [Monthly Tuition ₹${monthlyTuition.toInt()} After Month End]"
                                    }
                                    val monthYearStr = "${(sysToday.monthIndex + 1).toString().padStart(2, '0')}/${sysToday.year}"

                                    database.feeDao().insertFeeRecord(
                                        FeeRecordEntity(
                                            schoolCode = schoolCode,
                                            studentId = studentId,
                                            studentName = studentName,
                                            gradeClass = targetClass,
                                            amountPaid = totalInitialPayable,
                                            paymentDate = sysToday.formatted,
                                            feeMonth = monthYearStr,
                                            paymentMode = "CASH",
                                            remarks = (selectedItemsDesc + tuitionRemark).take(220)
                                        )
                                    )
                                }
                            }
                            onBatchAssigned()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary)
                ) {
                    Text(
                        "Confirm & Collect ₹${totalInitialPayable.toInt()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
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
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                )
            }

            // 1. Class Selection + "+ Fee Template" Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Batch / Class Selection", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                        Text("Selecting a class auto-fills its fee template", fontSize = 12.sp, color = colors.textSecondary)
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.brandPrimary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, colors.brandPrimary),
                        modifier = Modifier.clickable {
                            if (selectedBatch.isEmpty()) {
                                selectedBatch = batches.firstOrNull()?.batchName ?: "Class 1"
                            }
                            showTemplateDialog = true
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("＋", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = colors.brandPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fee Template", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.brandPrimary)
                        }
                    }
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
                        val classOptions = if (batches.isEmpty()) {
                            listOf("Nursery", "LKG", "UKG", "Class 1", "Class 2", "Class 3", "Class 4", "Class 5")
                        } else {
                            batches.map { it.batchName }
                        }
                        classOptions.forEach { bName ->
                            DropdownMenuItem(
                                text = { Text(bName, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    selectedBatch = bName
                                    showBatchMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // 2. Itemized Admission Fee Template Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.bgCard,
                    border = BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (selectedBatch.isEmpty()) "Admission Fee Items" else "$selectedBatch Admission Items",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Check or uncheck items applicable for this student",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary
                                )
                            }
                            Text(
                                text = "₹${admissionFeeTotal.toInt()}",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = colors.brandPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (feeParts.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.bgApp,
                                border = BorderStroke(1.dp, colors.borderCard),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "No fee items configured yet. Tap '＋ Fee Template' above to set class fee heads & amounts, or add an item below.",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            feeParts.forEachIndexed { index, item ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (item.isSelected) colors.brandPrimary.copy(alpha = 0.06f) else colors.bgApp,
                                    border = BorderStroke(
                                        1.dp,
                                        if (item.isSelected) colors.brandPrimary.copy(alpha = 0.3f) else colors.borderCard
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = item.isSelected,
                                            onCheckedChange = { checked ->
                                                feeParts = feeParts.toMutableList().also {
                                                    it[index] = item.copy(isSelected = checked)
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = colors.brandPrimary)
                                        )
                                        Text(
                                            text = item.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "₹${item.amount.toInt()}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.isSelected) colors.brandPrimary else colors.textSecondary
                                        )
                                        IconButton(
                                            onClick = {
                                                feeParts = feeParts.toMutableList().also { it.removeAt(index) }
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Text("✕", fontSize = 12.sp, color = colors.textSecondary)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (showInlineAddItem) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newPartName,
                                    onValueChange = { newPartName = it },
                                    placeholder = { Text("Fee Name", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.4f)
                                )
                                OutlinedTextField(
                                    value = newPartAmount,
                                    onValueChange = { newPartAmount = it },
                                    placeholder = { Text("₹ Amt", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.8f)
                                )
                                Button(
                                    onClick = {
                                        val amt = newPartAmount.toDoubleOrNull() ?: 0.0
                                        if (newPartName.isNotBlank() && amt > 0.0) {
                                            feeParts = feeParts + AdmissionFeePart(newPartName.trim(), amt, true)
                                            newPartName = ""
                                            newPartAmount = ""
                                            showInlineAddItem = false
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary)
                                ) {
                                    Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(onClick = { showInlineAddItem = true }) {
                                    Text("＋ Add Extra Fee Item", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.brandPrimary)
                                }
                                TextButton(
                                    onClick = {
                                        if (selectedBatch.isEmpty()) {
                                            selectedBatch = batches.firstOrNull()?.batchName ?: "Class 1"
                                        }
                                        showTemplateDialog = true
                                    }
                                ) {
                                    Text("⚙ Configure Template", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Monthly Tuition Fee & Collection Timing Mode
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.bgCard,
                    border = BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Monthly Tuition & Collection Rule",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Choose whether 1st month tuition is collected now or after month end",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        CustomRoundedInput(
                            value = tuitionFeeInput,
                            onValueChange = { tuitionFeeInput = it },
                            placeholder = "ENTER MONTHLY TUITION FEE (₹)"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Option A: Collect at Admission
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (collectTuitionAtAdmission) colors.brandPrimary.copy(alpha = 0.1f) else colors.bgApp,
                            border = BorderStroke(
                                1.5.dp,
                                if (collectTuitionAtAdmission) colors.brandPrimary else colors.borderCard
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { collectTuitionAtAdmission = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (collectTuitionAtAdmission) "🔘" else "⚪", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Collect Tuition at Time of Admission",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "Adds 1st month tuition (+₹${monthlyTuition.toInt()}) to today's admission bill",
                                        fontSize = 11.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Option B: Collect After Month End
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (!collectTuitionAtAdmission) colors.brandPrimary.copy(alpha = 0.1f) else colors.bgApp,
                            border = BorderStroke(
                                1.5.dp,
                                if (!collectTuitionAtAdmission) colors.brandPrimary else colors.borderCard
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { collectTuitionAtAdmission = false }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (!collectTuitionAtAdmission) "🔘" else "⚪", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Collect Tuition After Month End",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "Only admission items charged today; ₹${monthlyTuition.toInt()}/mo billed after month end",
                                        fontSize = 11.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Live Fee Breakdown & Auto-Sum Summary Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.brandPrimary.copy(alpha = 0.12f),
                    border = BorderStroke(1.5.dp, colors.brandPrimary.copy(alpha = 0.4f)),
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

                        feeParts.filter { it.isSelected }.forEach { part ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• ${part.name}:", fontSize = 12.sp, color = colors.textSecondary)
                                Text("₹${part.amount.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Admission Items Subtotal:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                            Text("₹${admissionFeeTotal.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = if (collectTuitionAtAdmission) "1st Month Tuition (With Admission):" else "1st Month Tuition (After Month End):",
                                fontSize = 13.sp,
                                color = colors.textSecondary
                            )
                            Text(
                                text = if (collectTuitionAtAdmission) "₹${monthlyTuition.toInt()}" else "₹0 (Post-paid)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = colors.brandPrimary.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Initial Due at Admission:", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = colors.brandPrimary)
                            Text("₹${totalInitialPayable.toInt()}", fontSize = 17.sp, fontWeight = FontWeight.Black, color = colors.success)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (collectTuitionAtAdmission) {
                                "ℹ Includes 1st month tuition. Next monthly fee (₹${monthlyTuition.toInt()}) will be due from next month."
                            } else {
                                "ℹ Only admission fee items are collected now. Monthly tuition (₹${monthlyTuition.toInt()}) will be collected after month end."
                            },
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

    // Modal Dialog to Configure & Save Class-Wise Admission Fee Template (Manual Amounts)
    if (showTemplateDialog) {
        val targetBatchName = selectedBatch.ifEmpty { "Class 1" }
        // Pair of (Fee Head Name, Editable Amount String)
        var dialogEntries by remember(feeParts) {
            mutableStateOf(
                feeParts.map { part ->
                    part.name to (if (part.amount > 0.0) part.amount.toInt().toString() else "")
                }
            )
        }
        var dialogTuition by remember(tuitionFeeInput) { mutableStateOf(tuitionFeeInput) }
        var dialogCollectAtAdmission by remember(collectTuitionAtAdmission) { mutableStateOf(collectTuitionAtAdmission) }
        var customTitleInput by remember { mutableStateOf("") }
        var customAmountInput by remember { mutableStateOf("") }

        // Common fee head names WITHOUT preset amounts
        val quickFeeHeads = listOf(
            "Admission Fee",
            "Cloth / Uniform Fee",
            "Bag Fee",
            "Cultural Program",
            "Saraswati Puja Chanda",
            "Books & Diary",
            "ID Card & Tie",
            "Session / Exam Fee"
        )

        Dialog(onDismissRequest = { showTemplateDialog = false }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = colors.bgCard,
                border = BorderStroke(1.dp, colors.borderCard),
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "$targetBatchName Fee Template",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Tap any fee head below and enter its amount manually",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Quick Select Common Fee Heads:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.brandPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // 2-column grid of fee head chips (no auto-set amounts)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        quickFeeHeads.chunked(2).forEach { rowHeads ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowHeads.forEach { headName ->
                                    val isAdded = dialogEntries.any { it.first.equals(headName, ignoreCase = true) }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isAdded) colors.brandPrimary.copy(alpha = 0.18f) else colors.bgApp,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isAdded) colors.brandPrimary else colors.borderCard
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                if (!isAdded) {
                                                    // Adds fee head with blank amount so admin enters it manually
                                                    dialogEntries = dialogEntries + (headName to "")
                                                }
                                            }
                                    ) {
                                        Text(
                                            text = if (isAdded) "✓ $headName" else "＋ $headName",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isAdded) colors.brandPrimary else colors.textPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Selected Fee Heads with Manual Amount Input Boxes
                    if (dialogEntries.isNotEmpty()) {
                        Text(
                            text = "Enter Amount for Selected Fee Heads:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        dialogEntries.forEachIndexed { idx, (headName, amtStr) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = headName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1.2f)
                                )
                                OutlinedTextField(
                                    value = amtStr,
                                    onValueChange = { newVal ->
                                        dialogEntries = dialogEntries.toMutableList().also {
                                            it[idx] = headName to newVal
                                        }
                                    },
                                    placeholder = { Text("₹ Enter Amt", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        dialogEntries = dialogEntries.toMutableList().also { it.removeAt(idx) }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Text("✕", fontSize = 13.sp, color = colors.error)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Add Custom Fee Head Row
                    Text(
                        text = "Or Add Custom Fee Head:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = customTitleInput,
                            onValueChange = { customTitleInput = it },
                            placeholder = { Text("Custom Fee Name", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = customAmountInput,
                            onValueChange = { customAmountInput = it },
                            placeholder = { Text("₹ Amt", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(0.9f)
                        )
                        Button(
                            onClick = {
                                if (customTitleInput.isNotBlank()) {
                                    dialogEntries = dialogEntries + (customTitleInput.trim() to customAmountInput.trim())
                                    customTitleInput = ""
                                    customAmountInput = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary)
                        ) {
                            Text("＋", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = colors.borderCard)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = dialogTuition,
                        onValueChange = { dialogTuition = it },
                        label = { Text("Monthly Tuition Fee (₹)", fontSize = 12.sp) },
                        placeholder = { Text("Enter monthly tuition manually", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dialogCollectAtAdmission = !dialogCollectAtAdmission }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = dialogCollectAtAdmission,
                            onCheckedChange = { dialogCollectAtAdmission = it },
                            colors = CheckboxDefaults.colors(checkedColor = colors.brandPrimary)
                        )
                        Text(
                            text = "Collect 1st Month Tuition at Admission",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTemplateDialog = false }) {
                            Text("Cancel", color = colors.textSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val validParts = dialogEntries.mapNotNull { (name, amtStr) ->
                                    val amt = amtStr.trim().toDoubleOrNull() ?: 0.0
                                    if (name.isNotBlank() && amt > 0.0) {
                                        AdmissionFeePart(name.trim(), amt, true)
                                    } else null
                                }
                                val tAmt = dialogTuition.trim().toDoubleOrNull() ?: 0.0
                                val encoded = encodeFeeTemplate(validParts)
                                scope.launch {
                                    val existingBatch = batches.find { it.batchName.equals(targetBatchName, ignoreCase = true) }
                                    val updatedBatch = if (existingBatch != null) {
                                        existingBatch.copy(
                                            feeTemplateRaw = encoded,
                                            defaultMonthlyTuition = tAmt,
                                            collectTuitionAtAdmission = dialogCollectAtAdmission
                                        )
                                    } else {
                                        BatchEntity(
                                            schoolCode = schoolCode,
                                            batchName = targetBatchName,
                                            subjects = "General",
                                            sections = "A, B",
                                            feeTemplateRaw = encoded,
                                            defaultMonthlyTuition = tAmt,
                                            collectTuitionAtAdmission = dialogCollectAtAdmission
                                        )
                                    }
                                    database.batchDao().insertBatch(updatedBatch)
                                    feeParts = validParts
                                    tuitionFeeInput = if (tAmt > 0.0) tAmt.toInt().toString() else ""
                                    collectTuitionAtAdmission = dialogCollectAtAdmission
                                    showTemplateDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save Template", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

