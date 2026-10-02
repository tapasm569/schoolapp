#!/bin/bash
set -e

echo "Applying Add New options redesign (Student, Assign Batch, Staff, Class)..."

# -------------------------------------------------------------
# 1. BATCH ENTITY & DAO
# -------------------------------------------------------------
cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/core/database/entity/BatchEntity.kt
package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "batches")
data class BatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolCode: String,
    val batchName: String,
    val subjects: String = "",
    val sections: String = ""
)
EOF

cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/core/database/dao/BatchDao.kt
package com.school.manage.core.database.dao

import androidx.room.*
import com.school.manage.core.database.entity.BatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BatchDao {
    @Query("SELECT * FROM batches WHERE schoolCode = :schoolCode ORDER BY batchName ASC")
    fun getBatchesBySchool(schoolCode: String): Flow<List<BatchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: BatchEntity): Long

    @Delete
    suspend fun deleteBatch(batch: BatchEntity)
}
EOF

# -------------------------------------------------------------
# 2. UPDATE STUDENT & STAFF ENTITIES (EXTENDED PROFILE FIELDS)
# -------------------------------------------------------------
cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/core/database/entity/StudentEntity.kt
package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolCode: String,
    val rollNo: String = "",
    val name: String,
    val gradeClass: String = "",
    val section: String = "",
    val guardianName: String = "",
    val phone: String, // Serves as student password
    val monthlyFee: Double = 0.0,
    val admissionDate: String = "02/10/2026",
    val fatherName: String = "",
    val motherName: String = "",
    val dob: String = "",
    val aadharNumber: String = "",
    val caste: String = "",
    val gender: String = "Male",
    val whatsapp: String = "",
    val address: String = "",
    val admissionFee: Double = 0.0
)
EOF

cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/core/database/entity/StaffEntity.kt
package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolCode: String,
    val name: String,
    val role: String = "Teacher",
    val phone: String,
    val salary: Double = 0.0,
    val joinDate: String = "02/10/2026",
    val gender: String = "Male",
    val whatsapp: String = "",
    val address: String = "",
    val qualification: String = "",
    val salaryType: String = "Monthly",
    val password: String = ""
)
EOF

# -------------------------------------------------------------
# 3. UPDATE DATABASE TO VERSION 3
# -------------------------------------------------------------
cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/core/database/AppDatabase.kt
package com.school.manage.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.school.manage.core.database.dao.*
import com.school.manage.core.database.entity.*

@Database(
    entities = [
        SchoolEntity::class,
        StudentEntity::class,
        AttendanceEntity::class,
        FeeRecordEntity::class,
        ExpenseEntity::class,
        StaffEntity::class,
        BatchEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun schoolDao(): SchoolDao
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun feeDao(): FeeDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun staffDao(): StaffDao
    abstract fun batchDao(): BatchDao
}
EOF

# -------------------------------------------------------------
# 4. REDESIGNED STUDENT & ASSIGN BATCH SCREENS (Images 1, 2, 3)
# -------------------------------------------------------------
cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/StudentScreens.kt
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
EOF

# -------------------------------------------------------------
# 5. REDESIGNED TEACHER / STAFF SCREEN (Image 4)
# -------------------------------------------------------------
cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/ExpenseAndStaffScreens.kt
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.ExpenseEntity
import com.school.manage.core.database.entity.StaffEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val expenses by database.expenseDao().getExpensesBySchool(schoolCode).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses ($schoolCode)") },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("Add Expense", fontWeight = FontWeight.Bold) }
            item { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (₹)") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (title.isNotBlank() && amount.isNotBlank()) {
                            scope.launch {
                                database.expenseDao().insertExpense(
                                    ExpenseEntity(
                                        schoolCode = schoolCode,
                                        title = title,
                                        category = category,
                                        amount = amount.toDoubleOrNull() ?: 0.0,
                                        date = "02/10/2026",
                                        notes = ""
                                    )
                                )
                                title = ""
                                category = ""
                                amount = ""
                            }
                        }
                    }
                ) { Text("Save Expense") }
            }
            items(expenses) { exp ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(exp.title, fontWeight = FontWeight.Bold)
                        Text("₹${exp.amount.toInt()}", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ADD STAFF SCREEN (Image 4)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStaffScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("02/10/2026") }
    var gender by remember { mutableStateOf("Male") }
    var whatsapp by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var qualification by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("Teacher") }
    var salary by remember { mutableStateOf("0.0") }
    var salaryType by remember { mutableStateOf("Monthly") }
    var showSalaryTypeMenu by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = { Text("Add Staff", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                            errorMsg = "Staff Name and Mobile Number are required!"
                            return@Button
                        }
                        scope.launch {
                            database.staffDao().insertStaff(
                                StaffEntity(
                                    schoolCode = schoolCode,
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    joinDate = startDate,
                                    gender = gender,
                                    whatsapp = whatsapp.trim(),
                                    address = address.trim(),
                                    qualification = qualification.trim(),
                                    role = position.ifBlank { "Teacher" },
                                    salary = salary.toDoubleOrNull() ?: 0.0,
                                    salaryType = salaryType,
                                    password = password.trim()
                                )
                            )
                            onNavigateBack()
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

            // Staff Information
            item {
                Column {
                    Text("Staff information", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                    Text("Enter staff personal details here.", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }

            item { CustomRoundedInput(value = name, onValueChange = { name = it }, placeholder = "Enter Name") }
            item {
                CustomRoundedInput(
                    value = startDate,
                    onValueChange = { startDate = it },
                    placeholder = "Start Date",
                    trailingIcon = "📅"
                )
            }

            // Gender
            item {
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

            // Contact Information
            item {
                Column {
                    Text("Contact Information", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                    Text("Enter contact details here.", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }

            item { CustomRoundedInput(value = whatsapp, onValueChange = { whatsapp = it }, placeholder = "WhatsApp") }
            item { CustomRoundedInput(value = phone, onValueChange = { phone = it }, placeholder = "Mobile number") }
            item { CustomRoundedInput(value = address, onValueChange = { address = it }, placeholder = "Enter Address") }

            // Qualification And Salary
            item {
                Column {
                    Text("Qualification And Salary", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                    Text("Enter Qualification details here.", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }

            item { CustomRoundedInput(value = qualification, onValueChange = { qualification = it }, placeholder = "Enter Qualification") }
            item { CustomRoundedInput(value = position, onValueChange = { position = it }, placeholder = "Enter Position") }
            item { CustomRoundedInput(value = salary, onValueChange = { salary = it }, placeholder = "Enter Salary") }

            item {
                Box {
                    DropdownTriggerInput(
                        label = "Select Salary",
                        value = salaryType,
                        onClick = { showSalaryTypeMenu = true }
                    )
                    DropdownMenu(
                        expanded = showSalaryTypeMenu,
                        onDismissRequest = { showSalaryTypeMenu = false }
                    ) {
                        listOf("Monthly", "Annual", "Weekly", "Daily").forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st) },
                                onClick = { salaryType = st; showSalaryTypeMenu = false }
                            )
                        }
                    }
                }
            }

            // Password
            item {
                Column {
                    Text("Staff Password", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                    Text("*Enter Staff Login Password here.", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }
            item {
                CustomRoundedInput(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Enter Password"
                )
            }

            if (errorMsg.isNotEmpty()) {
                item {
                    Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val staffList by database.staffDao().getStaffBySchool(schoolCode).collectAsState(initial = emptyList())
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staff Directory ($schoolCode)") },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(staffList) { staff ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(staff.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Position: ${staff.role} • Contact: ${staff.phone}")
                        Text("Salary: ₹${staff.salary.toInt()} (${staff.salaryType})")
                    }
                }
            }
        }
    }
}
EOF

# -------------------------------------------------------------
# 6. ADD BATCH / CLASS SCREEN (Image 5)
# -------------------------------------------------------------
cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/BatchScreens.kt
package com.school.manage.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.school.manage.core.database.entity.BatchEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBatchScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var batchName by remember { mutableStateOf("") }
    val subjects = remember { mutableStateListOf<String>() }
    val sections = remember { mutableStateListOf<String>() }

    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var newSubject by remember { mutableStateOf("") }

    var showAddSectionDialog by remember { mutableStateOf(false) }
    var newSection by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = { Text("Add Batch", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                        if (batchName.isNotBlank()) {
                            scope.launch {
                                database.batchDao().insertBatch(
                                    BatchEntity(
                                        schoolCode = schoolCode,
                                        batchName = batchName.trim(),
                                        subjects = subjects.joinToString(","),
                                        sections = sections.joinToString(",")
                                    )
                                )
                                onNavigateBack()
                            }
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
            // Profile photo Card
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
                                .background(Color(0xFF6B21A8)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏛️", fontSize = 24.sp)
                        }
                    }
                }
            }

            // Batch Information Card
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Batch Information", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                        Text("Enter batch details here", fontSize = 12.sp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(12.dp))
                        CustomRoundedInput(
                            value = batchName,
                            onValueChange = { batchName = it },
                            placeholder = "Batch name"
                        )
                    }
                }
            }

            // Subjects Card
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Subjects", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                        Text("Add subjects taught in this batch", fontSize = 12.sp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(8.dp))

                        if (subjects.isEmpty()) {
                            Text("No subjects added yet", fontSize = 13.sp, color = Color(0xFF94A3B8))
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                subjects.forEach { sub ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Text(sub, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showAddSubjectDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0), contentColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("+ Add", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Sections Card
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Sections", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                        Text("Add sections for this class (e.g. A, B, C)", fontSize = 12.sp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(8.dp))

                        if (sections.isEmpty()) {
                            Text("No sections added yet", fontSize = 13.sp, color = Color(0xFF94A3B8))
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                sections.forEach { sec ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Text(sec, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showAddSectionDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0), contentColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("+ Add", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Add Subject Dialog
        if (showAddSubjectDialog) {
            AlertDialog(
                onDismissRequest = { showAddSubjectDialog = false },
                title = { Text("Add Subject") },
                text = {
                    OutlinedTextField(
                        value = newSubject,
                        onValueChange = { newSubject = it },
                        label = { Text("Subject Name (e.g. Math)") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (newSubject.isNotBlank()) {
                            subjects.add(newSubject.trim())
                            newSubject = ""
                            showAddSubjectDialog = false
                        }
                    }) { Text("Add") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddSubjectDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Add Section Dialog
        if (showAddSectionDialog) {
            AlertDialog(
                onDismissRequest = { showAddSectionDialog = false },
                title = { Text("Add Section") },
                text = {
                    OutlinedTextField(
                        value = newSection,
                        onValueChange = { newSection = it },
                        label = { Text("Section (e.g. Section A)") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (newSection.isNotBlank()) {
                            sections.add(newSection.trim())
                            newSection = ""
                            showAddSectionDialog = false
                        }
                    }) { Text("Add") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddSectionDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
EOF

# -------------------------------------------------------------
# 7. UPDATE APP NAV HOST (Connect Assign Batch, Staff, Batch)
# -------------------------------------------------------------
cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/presentation/navigation/AppNavHost.kt
package com.school.manage.presentation.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.school.manage.core.database.AppDatabase
import com.school.manage.presentation.screens.*

@Composable
fun AppNavHost(database: AppDatabase) {
    val navController = rememberNavController()
    var currentSchoolCode by remember { mutableStateOf("") }
    var currentSchoolName by remember { mutableStateOf("") }
    var currentStudentId by remember { mutableStateOf(0L) }
    var currentStaffId by remember { mutableStateOf(0L) }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                database = database,
                onSchoolLoginSuccess = { code, name ->
                    currentSchoolCode = code
                    currentSchoolName = name
                    navController.navigate("dashboard") { popUpTo("login") { inclusive = true } }
                },
                onStaffLoginSuccess = { staffId, code, _, _ ->
                    currentStaffId = staffId
                    currentSchoolCode = code
                    navController.navigate("staff_portal") { popUpTo("login") { inclusive = true } }
                },
                onStudentLoginSuccess = { studentId ->
                    currentStudentId = studentId
                    navController.navigate("student_portal") { popUpTo("login") { inclusive = true } }
                },
                onNavigateRegister = { navController.navigate("register_school") }
            )
        }
        composable("register_school") {
            RegisterSchoolScreen(
                database = database,
                onRegisterSuccess = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("dashboard") {
            DashboardScreen(
                database = database,
                schoolCode = currentSchoolCode,
                schoolName = currentSchoolName,
                onNavigate = { route -> navController.navigate(route) },
                onLogout = { navController.navigate("login") { popUpTo(0) } }
            )
        }
        composable("staff_portal") {
            StaffPortalScreen(
                database = database,
                staffId = currentStaffId,
                schoolCode = currentSchoolCode,
                onNavigate = { route -> navController.navigate(route) },
                onLogout = { navController.navigate("login") { popUpTo(0) } }
            )
        }
        composable("student_portal") {
            StudentPortalScreen(
                database = database,
                studentId = currentStudentId,
                onLogout = { navController.navigate("login") { popUpTo(0) } }
            )
        }
        composable("student_list") {
            StudentListScreen(
                database = database,
                schoolCode = currentSchoolCode,
                onNavigateBack = { navController.popBackStack() },
                onNavigateAdd = { navController.navigate("add_student") }
            )
        }
        composable("add_student") {
            AddStudentScreen(
                database = database,
                schoolCode = currentSchoolCode,
                onNavigateBack = { navController.popBackStack() },
                onStudentSaved = { sId, sName ->
                    navController.navigate("assign_batch/$sId/$sName")
                }
            )
        }
        composable(
            route = "assign_batch/{studentId}/{studentName}",
            arguments = listOf(
                navArgument("studentId") { type = NavType.LongType },
                navArgument("studentName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sId = backStackEntry.arguments?.getLong("studentId") ?: 0L
            val sName = backStackEntry.arguments?.getString("studentName") ?: "Student"
            AssignBatchScreen(
                database = database,
                schoolCode = currentSchoolCode,
                studentId = sId,
                studentName = sName,
                onBatchAssigned = {
                    navController.popBackStack("student_list", inclusive = false)
                }
            )
        }
        composable("add_staff") {
            AddStaffScreen(
                database = database,
                schoolCode = currentSchoolCode,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("add_batch") {
            AddBatchScreen(
                database = database,
                schoolCode = currentSchoolCode,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("attendance") {
            AttendanceScreen(
                database = database,
                schoolCode = currentSchoolCode,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("fee_collection") {
            FeeCollectionScreen(
                database = database,
                schoolCode = currentSchoolCode,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("expense_list") {
            ExpenseScreen(
                database = database,
                schoolCode = currentSchoolCode,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("staff_list") {
            StaffScreen(
                database = database,
                schoolCode = currentSchoolCode,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
EOF

# -------------------------------------------------------------
# 8. UPDATE DASHBOARD POPUP DIALOG ROUTES
# -------------------------------------------------------------
cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/DashboardScreen.kt
package com.school.manage.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.school.manage.core.database.AppDatabase

@Composable
fun DashboardScreen(
    database: AppDatabase,
    schoolCode: String,
    schoolName: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val students by database.studentDao().getStudentsBySchool(schoolCode).collectAsState(initial = emptyList())
    val feeRecords by database.feeDao().getFeeRecordsBySchool(schoolCode).collectAsState(initial = emptyList())
    val expenses by database.expenseDao().getExpensesBySchool(schoolCode).collectAsState(initial = emptyList())
    val staffList by database.staffDao().getStaffBySchool(schoolCode).collectAsState(initial = emptyList())

    val distinctClasses = students.map { it.gradeClass }.distinct().size
    val totalFees = feeRecords.sumOf { it.amountPaid }
    val totalExpenses = expenses.sumOf { it.amount }

    var showAddNewDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFFF4F7FB),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddNewDialog = true },
                containerColor = Color(0xFF0D529C),
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Text("+", fontSize = 30.sp, fontWeight = FontWeight.Light)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }

            // 1. Top Header Profile Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onLogout() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👤", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = schoolName.ifEmpty { "TAPAS MONDAL" }.uppercase(),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Change Account",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("▾", fontSize = 10.sp, color = Color(0xFF64748B))
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("🔍", fontSize = 20.sp, color = Color(0xFF334155))
                        Text("⚙", fontSize = 20.sp, color = Color(0xFF334155), modifier = Modifier.clickable { onLogout() })
                    }
                }
            }

            // 2. Top Counter Tri-Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TopCountCard(
                        count = "${students.size}",
                        title = "Students",
                        subtitle = "0 closed",
                        bannerColor = Color(0xFFE0F2FE),
                        icon = "👥",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("student_list") }
                    )
                    TopCountCard(
                        count = "$distinctClasses",
                        title = "Classes",
                        subtitle = "0 closed",
                        bannerColor = Color(0xFFDCFCE7),
                        icon = "🏫",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("add_batch") }
                    )
                    TopCountCard(
                        count = "${staffList.size}",
                        title = "Teacher",
                        subtitle = "0 closed",
                        bannerColor = Color(0xFFFEF3C7),
                        icon = "👨‍🏫",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("staff_list") }
                    )
                }
            }

            // 3. Attendance Summary Graph Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Attendance\nSummary",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F52BA),
                                lineHeight = 18.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("‹", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F52BA))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Oct-2026", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("›", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F52BA))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        ChartGridCanvas(lineColor = Color(0xFF38BDF8))

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            LegendPill("Present", Color(0xFF22C55E))
                            LegendPill("Absent", Color(0xFFEF4444))
                            LegendPill("Leave", Color(0xFFF59E0B))
                            LegendPill("Holiday", Color(0xFF3B82F6))
                        }
                    }
                }
            }

            // 4. Marking Attendance Progress Bars
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AttendanceProgressBarCard(
                        title = "Student",
                        subtitle = "Marking Attendance",
                        pct = "0%",
                        ratio = "0/0",
                        modifier = Modifier.weight(1f)
                    )
                    AttendanceProgressBarCard(
                        title = "Teacher",
                        subtitle = "Marking Attendance",
                        pct = "0%",
                        ratio = "0/0",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. Due Fees Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📑", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Due Fees", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("(0) 0", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F52BA))
                                Text("Active", fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("(0) 0", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F52BA))
                                Text("Close", fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                }
            }

            // 6. Monthly Summary
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Monthly Summary", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F52BA))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("‹", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F52BA))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Oct-2026", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("›", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F52BA))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        ChartGridCanvas(lineColor = Color(0xFFEF4444))

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            LegendPill("Incomes", Color(0xFF22C55E))
                            Spacer(modifier = Modifier.width(16.dp))
                            LegendPill("Expenses", Color(0xFFEF4444))
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Today", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("Monthly", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("Total", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(6.dp))

                        // Incomes
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("₹0", fontSize = 13.sp, color = Color(0xFF22C55E), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("₹${totalFees.toInt()}", fontSize = 13.sp, color = Color(0xFF22C55E), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("₹${totalFees.toInt()}", fontSize = 13.sp, color = Color(0xFF22C55E), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(6.dp))

                        // Expenses
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("₹0", fontSize = 13.sp, color = Color(0xFFEF4444), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("₹${totalExpenses.toInt()}", fontSize = 13.sp, color = Color(0xFFEF4444), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("₹${totalExpenses.toInt()}", fontSize = 13.sp, color = Color(0xFFEF4444), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            // 7. FEATURES 4x3 Grid Section
            item {
                Text("FEATURES", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF475569), letterSpacing = 1.sp)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        FeatureIconItem("Exams", "⏱️", Color(0xFFFFF7ED), onClick = { onNavigate("student_list") })
                        FeatureIconItem("Birthdays", "🎂", Color(0xFFFEF3C7), onClick = { onNavigate("student_list") })
                        FeatureIconItem("Home works", "📋", Color(0xFFECFDF5), onClick = { onNavigate("student_list") })
                        FeatureIconItem("Class works", "📊", Color(0xFFF0FDF4), onClick = { onNavigate("student_list") })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        FeatureIconItem("Enquiry", "👤", Color(0xFFFFF7ED), onClick = { onNavigate("add_student") })
                        FeatureIconItem("Staff Logs", "🔢", Color(0xFFE0F2FE), onClick = { onNavigate("staff_list") })
                        FeatureIconItem("Announcements", "🔔", Color(0xFFFFF7ED), onClick = { onNavigate("student_list") })
                        FeatureIconItem("Messages", "💬", Color(0xFFECFDF5), onClick = { onNavigate("student_list") })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        FeatureIconItem("Leave\nManagement", "➖", Color(0xFFF0FDF4), onClick = { onNavigate("attendance") })
                        FeatureIconItem("Timetable", "📅", Color(0xFFECFDF5), onClick = { onNavigate("add_batch") })
                        FeatureIconItem("Online\nClasses", "📖", Color(0xFFF0FDF4), onClick = { onNavigate("add_batch") })
                        FeatureIconItem("Question\nBank", "📑", Color(0xFFFEF3C7), onClick = { onNavigate("add_batch") })
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(70.dp)) }
        }

        // 8. ADD NEW POPUP DIALOG
        if (showAddNewDialog) {
            Dialog(onDismissRequest = { showAddNewDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Add New", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F52BA))
                            Text(
                                text = "✕",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.padding(4.dp).clickable { showAddNewDialog = false }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        AddNewOptionItem(
                            title = "Student",
                            subtitle = "You can add new student here",
                            icon = "👨‍🎓",
                            onClick = {
                                showAddNewDialog = false
                                onNavigate("add_student")
                            }
                        )
                        AddNewOptionItem(
                            title = "Teacher",
                            subtitle = "You can add new staff here",
                            icon = "👨‍🏫",
                            onClick = {
                                showAddNewDialog = false
                                onNavigate("add_staff")
                            }
                        )
                        AddNewOptionItem(
                            title = "Class",
                            subtitle = "You can add new batch here",
                            icon = "👥",
                            onClick = {
                                showAddNewDialog = false
                                onNavigate("add_batch")
                            }
                        )
                        AddNewOptionItem(
                            title = "Exams",
                            subtitle = "You can add new exam here",
                            icon = "📝",
                            onClick = {
                                showAddNewDialog = false
                                onNavigate("student_list")
                            }
                        )
                        AddNewOptionItem(
                            title = "Expense",
                            subtitle = "You can add expense here",
                            icon = "🧾",
                            onClick = {
                                showAddNewDialog = false
                                onNavigate("expense_list")
                            }
                        )
                        AddNewOptionItem(
                            title = "New Admission",
                            subtitle = "You can add new enquiry here",
                            icon = "👤",
                            onClick = {
                                showAddNewDialog = false
                                onNavigate("add_student")
                            }
                        )
                        AddNewOptionItem(
                            title = "Collect Fee",
                            subtitle = "Record a fee payment from a student",
                            icon = "💰",
                            onClick = {
                                showAddNewDialog = false
                                onNavigate("fee_collection")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopCountCard(
    count: String,
    title: String,
    subtitle: String,
    bannerColor: Color,
    icon: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().height(26.dp).background(bannerColor),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 12.sp)
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(count, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF0F766E))
                Spacer(modifier = Modifier.height(2.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E293B))
                Text(subtitle, fontSize = 10.sp, color = Color(0xFF64748B))
            }
        }
    }
}

@Composable
fun AttendanceProgressBarCard(
    title: String,
    subtitle: String,
    pct: String,
    ratio: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("👥", fontSize = 13.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                    Text(subtitle, fontSize = 9.sp, color = Color(0xFF64748B))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(pct, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F52BA))
                Text(ratio, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F52BA))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFF1F5F9))
                    .border(1.dp, Color(0xFF93C5FD), RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun LegendPill(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color),
        color = Color.White
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1E293B),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun ChartGridCanvas(lineColor: Color) {
    Column {
        Box(
            modifier = Modifier.fillMaxWidth().height(100.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stepY = size.height / 5
                val stepX = size.width / 9

                for (i in 0..5) {
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(0f, i * stepY),
                        end = Offset(size.width, i * stepY),
                        strokeWidth = 1f
                    )
                }
                for (j in 0..9) {
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(j * stepX, 0f),
                        end = Offset(j * stepX, size.height),
                        strokeWidth = 1f
                    )
                }

                val baseline = size.height
                drawLine(
                    color = lineColor,
                    start = Offset(0f, baseline),
                    end = Offset(size.width, baseline),
                    strokeWidth = 3f
                )
                for (k in 0..9) {
                    drawCircle(
                        color = lineColor,
                        radius = 4f,
                        center = Offset(k * stepX, baseline)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            (1..9).forEach {
                Text("$it", fontSize = 10.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun FeatureIconItem(
    title: String,
    iconEmoji: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp).clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(iconEmoji, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            color = Color(0xFF334155),
            textAlign = TextAlign.Center,
            lineHeight = 13.sp,
            maxLines = 2
        )
    }
}

@Composable
fun AddNewOptionItem(
    title: String,
    subtitle: String,
    icon: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFEFF6FF)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF64748B))
        }
    }
}
EOF

echo "All modules successfully updated!"
