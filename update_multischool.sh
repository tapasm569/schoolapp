#!/bin/bash
set -e

echo "Applying Multi-School Administration updates..."

# -------------------------------------------------------------
# 1. ENTITIES
# -------------------------------------------------------------
cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/core/database/entity/SchoolEntity.kt
package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schools")
data class SchoolEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolCode: String,
    val schoolName: String,
    val password: String,
    val phone: String
)
EOF

cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/core/database/entity/StudentEntity.kt
package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolCode: String,
    val rollNo: String,
    val name: String,
    val gradeClass: String,
    val section: String,
    val guardianName: String,
    val phone: String, // Serves as student password
    val monthlyFee: Double,
    val admissionDate: String
)
EOF

cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/core/database/entity/AttendanceEntity.kt
package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolCode: String,
    val studentId: Long,
    val studentName: String,
    val gradeClass: String,
    val date: String,
    val status: String // PRESENT, ABSENT
)
EOF

cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/core/database/entity/FeeRecordEntity.kt
package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fee_records")
data class FeeRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolCode: String,
    val studentId: Long,
    val studentName: String,
    val gradeClass: String,
    val amountPaid: Double,
    val paymentDate: String,
    val paymentMode: String,
    val remarks: String
)
EOF

cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/core/database/entity/ExpenseEntity.kt
package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolCode: String,
    val title: String,
    val category: String,
    val amount: Double,
    val date: String,
    val notes: String
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
    val role: String,
    val phone: String,
    val salary: Double,
    val joinDate: String
)
EOF

# -------------------------------------------------------------
# 2. DAOS & DATABASE
# -------------------------------------------------------------
cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/core/database/dao/SchoolDao.kt
package com.school.manage.core.database.dao

import androidx.room.*
import com.school.manage.core.database.entity.SchoolEntity

@Dao
interface SchoolDao {
    @Query("SELECT * FROM schools WHERE schoolCode = :code AND password = :password LIMIT 1")
    suspend fun loginSchool(code: String, password: String): SchoolEntity?

    @Query("SELECT * FROM schools WHERE schoolCode = :code LIMIT 1")
    suspend fun getSchoolByCode(code: String): SchoolEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun registerSchool(school: SchoolEntity): Long
}
EOF

cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/core/database/dao/StudentDao.kt
package com.school.manage.core.database.dao

import androidx.room.*
import com.school.manage.core.database.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE schoolCode = :schoolCode ORDER BY gradeClass ASC, rollNo ASC")
    fun getStudentsBySchool(schoolCode: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE schoolCode = :schoolCode AND phone = :phone LIMIT 1")
    suspend fun loginStudent(schoolCode: String, phone: String): StudentEntity?

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getStudentById(id: Long): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    @Delete
    suspend fun deleteStudent(student: StudentEntity)
}
EOF

cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/core/database/dao/AttendanceDao.kt
package com.school.manage.core.database.dao

import androidx.room.*
import com.school.manage.core.database.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE schoolCode = :schoolCode AND date = :date")
    fun getAttendanceByDate(schoolCode: String, date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId ORDER BY date DESC")
    fun getAttendanceByStudent(studentId: Long): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<AttendanceEntity>)
}
EOF

cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/core/database/dao/FeeDao.kt
package com.school.manage.core.database.dao

import androidx.room.*
import com.school.manage.core.database.entity.FeeRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeeDao {
    @Query("SELECT * FROM fee_records WHERE schoolCode = :schoolCode ORDER BY id DESC")
    fun getFeeRecordsBySchool(schoolCode: String): Flow<List<FeeRecordEntity>>

    @Query("SELECT * FROM fee_records WHERE studentId = :studentId ORDER BY id DESC")
    fun getFeeRecordsByStudent(studentId: Long): Flow<List<FeeRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeeRecord(record: FeeRecordEntity): Long
}
EOF

cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/core/database/dao/ExpenseDao.kt
package com.school.manage.core.database.dao

import androidx.room.*
import com.school.manage.core.database.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE schoolCode = :schoolCode ORDER BY id DESC")
    fun getExpensesBySchool(schoolCode: String): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
}
EOF

cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/core/database/dao/StaffDao.kt
package com.school.manage.core.database.dao

import androidx.room.*
import com.school.manage.core.database.entity.StaffEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff WHERE schoolCode = :schoolCode ORDER BY name ASC")
    fun getStaffBySchool(schoolCode: String): Flow<List<StaffEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: StaffEntity): Long

    @Delete
    suspend fun deleteStaff(staff: StaffEntity)
}
EOF

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
        StaffEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun schoolDao(): SchoolDao
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun feeDao(): FeeDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun staffDao(): StaffDao
}
EOF

# -------------------------------------------------------------
# 3. DOMAIN MODEL & REPOSITORY ALIGNMENT
# -------------------------------------------------------------
cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/domain/model/Student.kt
package com.school.manage.domain.model

data class Student(
    val id: Long = 0,
    val schoolCode: String = "",
    val rollNo: String,
    val name: String,
    val gradeClass: String,
    val section: String,
    val guardianName: String,
    val phone: String,
    val monthlyFee: Double,
    val admissionDate: String
)
EOF

cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/data/repository/StudentRepositoryImpl.kt
package com.school.manage.data.repository

import com.school.manage.core.database.dao.StudentDao
import com.school.manage.core.database.entity.StudentEntity
import com.school.manage.domain.model.Student
import com.school.manage.domain.repository.StudentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StudentRepositoryImpl(
    private val studentDao: StudentDao
) : StudentRepository {

    override fun getStudents(): Flow<List<Student>> =
        studentDao.getStudentsBySchool("").map { list -> list.map { it.toDomain() } }

    override suspend fun getStudentById(id: Long): Student? =
        studentDao.getStudentById(id)?.toDomain()

    override suspend fun saveStudent(student: Student): Long =
        studentDao.insertStudent(student.toEntity())

    override suspend fun removeStudent(student: Student) =
        studentDao.deleteStudent(student.toEntity())

    private fun StudentEntity.toDomain() = Student(
        id = id,
        schoolCode = schoolCode,
        rollNo = rollNo,
        name = name,
        gradeClass = gradeClass,
        section = section,
        guardianName = guardianName,
        phone = phone,
        monthlyFee = monthlyFee,
        admissionDate = admissionDate
    )

    private fun Student.toEntity() = StudentEntity(
        id = id,
        schoolCode = schoolCode,
        rollNo = rollNo,
        name = name,
        gradeClass = gradeClass,
        section = section,
        guardianName = guardianName,
        phone = phone,
        monthlyFee = monthlyFee,
        admissionDate = admissionDate
    )
}
EOF

# -------------------------------------------------------------
# 4. AUTH SCREENS (LOGIN & REGISTER)
# -------------------------------------------------------------
cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/AuthScreens.kt
package com.school.manage.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.SchoolEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    database: AppDatabase,
    onSchoolLoginSuccess: (String, String) -> Unit,
    onStudentLoginSuccess: (Long) -> Unit,
    onNavigateRegister: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var schoolCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var studentPhone by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("School Management System", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0; errorMessage = "" }, text = { Text("School Admin") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1; errorMessage = "" }, text = { Text("Student Portal") })
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedTab == 0) {
                // School Admin Login
                Text("School Administrator Login", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = schoolCode,
                    onValueChange = { schoolCode = it.uppercase() },
                    label = { Text("School Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    onClick = {
                        scope.launch {
                            val school = database.schoolDao().loginSchool(schoolCode.trim(), password)
                            if (school != null) {
                                onSchoolLoginSuccess(school.schoolCode, school.schoolName)
                            } else {
                                errorMessage = "Invalid School Code or Password"
                            }
                        }
                    }
                ) {
                    Text("Login as School")
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onNavigateRegister) {
                    Text("Register New School")
                }
            } else {
                // Student Login
                Text("Student / Guardian Login", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Password is your mobile number registered during admission.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = schoolCode,
                    onValueChange = { schoolCode = it.uppercase() },
                    label = { Text("School Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = studentPhone,
                    onValueChange = { studentPhone = it },
                    label = { Text("Registered Mobile Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    onClick = {
                        scope.launch {
                            val student = database.studentDao().loginStudent(schoolCode.trim(), studentPhone.trim())
                            if (student != null) {
                                onStudentLoginSuccess(student.id)
                            } else {
                                errorMessage = "No student found matching this School Code & Mobile Number"
                            }
                        }
                    }
                ) {
                    Text("Login as Student")
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterSchoolScreen(
    database: AppDatabase,
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var schoolName by remember { mutableStateOf("") }
    var schoolCode by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Register Your School") },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = schoolName,
                onValueChange = { schoolName = it },
                label = { Text("School Full Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = schoolCode,
                onValueChange = { schoolCode = it.uppercase() },
                label = { Text("Create Unique School Code (e.g., SCH01)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Official Contact Number") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Admin Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMsg.isNotEmpty()) {
                Text(errorMsg, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                onClick = {
                    if (schoolName.isBlank() || schoolCode.isBlank() || password.isBlank()) {
                        errorMsg = "Please fill in all mandatory fields"
                        return@Button
                    }
                    scope.launch {
                        val existing = database.schoolDao().getSchoolByCode(schoolCode.trim())
                        if (existing != null) {
                            errorMsg = "School Code '$schoolCode' is already taken. Pick another code."
                        } else {
                            database.schoolDao().registerSchool(
                                SchoolEntity(
                                    schoolCode = schoolCode.trim(),
                                    schoolName = schoolName.trim(),
                                    password = password,
                                    phone = phone
                                )
                            )
                            onRegisterSuccess()
                        }
                    }
                }
            ) {
                Text("Complete School Registration")
            }
        }
    }
}
EOF

# -------------------------------------------------------------
# 5. STUDENT PORTAL (STUDENT DASHBOARD)
# -------------------------------------------------------------
cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/StudentPortalScreen.kt
package com.school.manage.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.StudentEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentPortalScreen(
    database: AppDatabase,
    studentId: Long,
    onLogout: () -> Unit
) {
    var student by remember { mutableStateOf<StudentEntity?>(null) }
    val attendanceList by database.attendanceDao().getAttendanceByStudent(studentId).collectAsState(initial = emptyList())
    val feeRecords by database.feeDao().getFeeRecordsByStudent(studentId).collectAsState(initial = emptyList())

    LaunchedEffect(studentId) {
        student = database.studentDao().getStudentById(studentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(student?.name ?: "Student Portal") },
                actions = {
                    TextButton(onClick = onLogout) { Text("Logout") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(student?.name ?: "Student", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Roll No: ${student?.rollNo} • Class: ${student?.gradeClass} (${student?.section})")
                        Text("School Code: ${student?.schoolCode}")
                        Text("Guardian: ${student?.guardianName}")
                        Text("Monthly Fee: ₹${student?.monthlyFee?.toInt() ?: 0}")
                    }
                }
            }

            item {
                Text("Attendance History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (attendanceList.isEmpty()) {
                item { Text("No attendance records logged yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(attendanceList) { att ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(att.date)
                            Text(
                                att.status,
                                fontWeight = FontWeight.Bold,
                                color = if (att.status == "PRESENT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            item {
                Text("Fee Payment History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (feeRecords.isEmpty()) {
                item { Text("No fee payment records found.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(feeRecords) { fee ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Amount: ₹${fee.amountPaid.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(fee.paymentDate)
                            }
                            Text("Payment Mode: ${fee.paymentMode}")
                            if (fee.remarks.isNotBlank()) Text("Notes: ${fee.remarks}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
EOF

# -------------------------------------------------------------
# 6. ADMIN DASHBOARD & MODULE SCREENS (SCOPED TO SCHOOL CODE)
# -------------------------------------------------------------
cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/DashboardScreen.kt
package com.school.manage.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.school.manage.core.database.AppDatabase

@OptIn(ExperimentalMaterial3Api::class)
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

    val totalFees = feeRecords.sumOf { it.amountPaid }
    val totalExpenses = expenses.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(schoolName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Code: $schoolCode", style = MaterialTheme.typography.bodySmall)
                    }
                },
                actions = {
                    TextButton(onClick = onLogout) { Text("Logout") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard("Students", "${students.size}", Modifier.weight(1f))
                    MetricCard("Staff", "${staffList.size}", Modifier.weight(1f))
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard("Fees Collected", "₹${totalFees.toInt()}", Modifier.weight(1f))
                    MetricCard("Expenses", "₹${totalExpenses.toInt()}", Modifier.weight(1f))
                }
            }
            item {
                Text(
                    text = "School Administration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            item { ActionCard("Students & Admissions", "Manage students in $schoolCode") { onNavigate("student_list") } }
            item { ActionCard("Attendance Register", "Mark daily attendance") { onNavigate("attendance") } }
            item { ActionCard("Fee Collection", "Record student fee receipts") { onNavigate("fee_collection") } }
            item { ActionCard("School Expenses", "Log and track expenditures") { onNavigate("expense_list") } }
            item { ActionCard("Staff & Teachers", "Manage faculty directory") { onNavigate("staff_list") } }
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ActionCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("→", style = MaterialTheme.typography.headlineSmall)
        }
    }
}
EOF

cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/StudentScreens.kt
package com.school.manage.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        topBar = {
            TopAppBar(
                title = { Text("Students ($schoolCode)") },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateAdd) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { padding ->
        if (students.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No students registered for $schoolCode. Tap + to add.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(students) { student ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(student.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Roll: ${student.rollNo}", color = MaterialTheme.colorScheme.primary)
                            }
                            Text("Class: ${student.gradeClass} (${student.section})")
                            Text("Phone (Student Login): ${student.phone}")
                            Text("Monthly Fee: ₹${student.monthlyFee.toInt()}")
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { scope.launch { database.studentDao().deleteStudent(student) } },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var rollNo by remember { mutableStateOf("") }
    var gradeClass by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }
    var guardian by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var fee by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admit Student ($schoolCode)") },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("Cancel") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = rollNo, onValueChange = { rollNo = it }, label = { Text("Roll Number") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = gradeClass, onValueChange = { gradeClass = it }, label = { Text("Class / Grade") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = section, onValueChange = { section = it }, label = { Text("Section") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = guardian, onValueChange = { guardian = it }, label = { Text("Guardian Name") }, modifier = Modifier.fillMaxWidth()) }
            item {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number (Used as Student Password)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item { OutlinedTextField(value = fee, onValueChange = { fee = it }, label = { Text("Monthly Fee (₹)") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    onClick = {
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            scope.launch {
                                database.studentDao().insertStudent(
                                    StudentEntity(
                                        schoolCode = schoolCode,
                                        rollNo = rollNo,
                                        name = name,
                                        gradeClass = gradeClass,
                                        section = section,
                                        guardianName = guardian,
                                        phone = phone.trim(),
                                        monthlyFee = fee.toDoubleOrNull() ?: 0.0,
                                        admissionDate = "2026-10-02"
                                    )
                                )
                                onNavigateBack()
                            }
                        }
                    }
                ) {
                    Text("Admit & Save Student")
                }
            }
        }
    }
}
EOF

cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/AttendanceScreen.kt
package com.school.manage.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.AttendanceEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val students by database.studentDao().getStudentsBySchool(schoolCode).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val attendanceMap = remember { mutableStateMapOf<Long, String>() }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(students) {
        students.forEach { s ->
            if (!attendanceMap.containsKey(s.id)) {
                attendanceMap[s.id] = "PRESENT"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance ($schoolCode)") },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            Text("Date: 2026-10-02", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(10.dp))

            if (submitted) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text("Attendance records saved!", modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(students) { student ->
                    val status = attendanceMap[student.id] ?: "PRESENT"
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column {
                                Text(student.name, fontWeight = FontWeight.Bold)
                                Text("Class: ${student.gradeClass} • Roll: ${student.rollNo}")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = { attendanceMap[student.id] = "PRESENT" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (status == "PRESENT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) { Text("P") }
                                Button(
                                    onClick = { attendanceMap[student.id] = "ABSENT" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (status == "ABSENT") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) { Text("A") }
                            }
                        }
                    }
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                onClick = {
                    scope.launch {
                        val records = students.map { s ->
                            AttendanceEntity(
                                schoolCode = schoolCode,
                                studentId = s.id,
                                studentName = s.name,
                                gradeClass = s.gradeClass,
                                date = "2026-10-02",
                                status = attendanceMap[s.id] ?: "PRESENT"
                            )
                        }
                        database.attendanceDao().insertAll(records)
                        submitted = true
                    }
                }
            ) {
                Text("Submit Attendance")
            }
        }
    }
}
EOF

cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/FeeScreen.kt
package com.school.manage.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.FeeRecordEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeCollectionScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val feeRecords by database.feeDao().getFeeRecordsBySchool(schoolCode).collectAsState(initial = emptyList())
    val students by database.studentDao().getStudentsBySchool(schoolCode).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var studentName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("Cash") }
    var remarks by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fees ($schoolCode)") },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("Record Fee Receipt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item { OutlinedTextField(value = studentName, onValueChange = { studentName = it }, label = { Text("Student Name") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (₹)") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = paymentMode, onValueChange = { paymentMode = it }, label = { Text("Payment Mode") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = remarks, onValueChange = { remarks = it }, label = { Text("Remarks") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (studentName.isNotBlank() && amount.isNotBlank()) {
                            scope.launch {
                                val matchedStudent = students.find { it.name.contains(studentName, ignoreCase = true) }
                                database.feeDao().insertFeeRecord(
                                    FeeRecordEntity(
                                        schoolCode = schoolCode,
                                        studentId = matchedStudent?.id ?: 0,
                                        studentName = studentName,
                                        gradeClass = matchedStudent?.gradeClass ?: "General",
                                        amountPaid = amount.toDoubleOrNull() ?: 0.0,
                                        paymentDate = "2026-10-02",
                                        paymentMode = paymentMode,
                                        remarks = remarks
                                    )
                                )
                                studentName = ""
                                amount = ""
                                remarks = ""
                            }
                        }
                    }
                ) { Text("Record Payment") }
            }

            item { Spacer(modifier = Modifier.height(10.dp)); Text("Receipts Log (${feeRecords.size})", fontWeight = FontWeight.Bold) }
            items(feeRecords) { fee ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(fee.studentName, fontWeight = FontWeight.Bold)
                            Text("₹${fee.amountPaid.toInt()}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Text("Date: ${fee.paymentDate} • Mode: ${fee.paymentMode}")
                    }
                }
            }
        }
    }
}
EOF

cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens/ExpenseAndStaffScreens.kt
package com.school.manage.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                                        date = "2026-10-02",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val staffList by database.staffDao().getStaffBySchool(schoolCode).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staff ($schoolCode)") },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("Add Staff Member", fontWeight = FontWeight.Bold) }
            item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = salary, onValueChange = { salary = it }, label = { Text("Salary") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (name.isNotBlank()) {
                            scope.launch {
                                database.staffDao().insertStaff(
                                    StaffEntity(
                                        schoolCode = schoolCode,
                                        name = name,
                                        role = role,
                                        phone = phone,
                                        salary = salary.toDoubleOrNull() ?: 0.0,
                                        joinDate = "2026-10-02"
                                    )
                                )
                                name = ""
                                role = ""
                                phone = ""
                                salary = ""
                            }
                        }
                    }
                ) { Text("Save Staff") }
            }
            items(staffList) { staff ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(staff.name, fontWeight = FontWeight.Bold)
                        Text("Role: ${staff.role} • Phone: ${staff.phone}")
                    }
                }
            }
        }
    }
}
EOF

# -------------------------------------------------------------
# 7. ROUTING NAVIGATION GRAPH
# -------------------------------------------------------------
cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/presentation/navigation/AppNavHost.kt
package com.school.manage.presentation.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.school.manage.core.database.AppDatabase
import com.school.manage.presentation.screens.*

@Composable
fun AppNavHost(database: AppDatabase) {
    val navController = rememberNavController()
    var currentSchoolCode by remember { mutableStateOf("") }
    var currentSchoolName by remember { mutableStateOf("") }
    var currentStudentId by remember { mutableStateOf(0L) }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                database = database,
                onSchoolLoginSuccess = { code, name ->
                    currentSchoolCode = code
                    currentSchoolName = name
                    navController.navigate("dashboard") { popUpTo("login") { inclusive = true } }
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
# 8. UPDATE ZIP IN DOWNLOADS
# -------------------------------------------------------------
cd ..
zip -r "school-management-app.zip" "school-management-app" -x "*/.git/*"

if [ -d "/sdcard/Download" ]; then
    cp "school-management-app.zip" /sdcard/Download/
    echo "=========================================================="
    echo "SUCCESS: school-management-app.zip updated in Downloads!"
    echo "=========================================================="
fi
cd school-management-app
