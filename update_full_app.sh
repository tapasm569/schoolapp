#!/bin/bash
set -e

echo "Generating all core school management modules..."

# Create directory structure
mkdir -p composeApp/src/commonMain/kotlin/com/school/manage/core/database/entity
mkdir -p composeApp/src/commonMain/kotlin/com/school/manage/core/database/dao
mkdir -p composeApp/src/commonMain/kotlin/com/school/manage/domain/model
mkdir -p composeApp/src/commonMain/kotlin/com/school/manage/domain/repository
mkdir -p composeApp/src/commonMain/kotlin/com/school/manage/data/repository
mkdir -p composeApp/src/commonMain/kotlin/com/school/manage/presentation/navigation
mkdir -p composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens

# -------------------------------------------------------------
# 1. Database Entities
# -------------------------------------------------------------
cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/core/database/entity/StudentEntity.kt
package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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

cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/core/database/entity/AttendanceEntity.kt
package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val studentName: String,
    val gradeClass: String,
    val date: String,
    val status: String // PRESENT, ABSENT, LEAVE
)
EOF

cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/core/database/entity/FeeRecordEntity.kt
package com.school.manage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fee_records")
data class FeeRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val studentName: String,
    val gradeClass: String,
    val amountPaid: Double,
    val paymentDate: String,
    val paymentMode: String, // Cash, UPI, Bank Transfer
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
    val title: String,
    val category: String, // Salary, Utilities, Maintenance, Books, Miscellaneous
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
    val name: String,
    val role: String, // Teacher, Admin, Accountant, Support
    val phone: String,
    val salary: Double,
    val joinDate: String
)
EOF

# -------------------------------------------------------------
# 2. Database DAOs & AppDatabase
# -------------------------------------------------------------
cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/core/database/dao/StudentDao.kt
package com.school.manage.core.database.dao

import androidx.room.*
import com.school.manage.core.database.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY gradeClass ASC, rollNo ASC")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE gradeClass = :gradeClass ORDER BY rollNo ASC")
    fun getStudentsByClass(gradeClass: String): Flow<List<StudentEntity>>

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
    @Query("SELECT * FROM attendance WHERE date = :date")
    fun getAttendanceByDate(date: String): Flow<List<AttendanceEntity>>

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
    @Query("SELECT * FROM fee_records ORDER BY id DESC")
    fun getAllFeeRecords(): Flow<List<FeeRecordEntity>>

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
    @Query("SELECT * FROM expenses ORDER BY id DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

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
    @Query("SELECT * FROM staff ORDER BY name ASC")
    fun getAllStaff(): Flow<List<StaffEntity>>

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
        StudentEntity::class,
        AttendanceEntity::class,
        FeeRecordEntity::class,
        ExpenseEntity::class,
        StaffEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun feeDao(): FeeDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun staffDao(): StaffDao
}
EOF

# -------------------------------------------------------------
# 3. Android Entry Initialization
# -------------------------------------------------------------
cat << 'EOF' > composeApp/src/androidMain/kotlin/com/school/manage/MainActivity.kt
package com.school.manage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.room.Room
import com.school.manage.core.database.AppDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "school_operations.db"
        ).fallbackToDestructiveMigration().build()

        setContent {
            App(database = database)
        }
    }
}
EOF

cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/App.kt
package com.school.manage

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.school.manage.core.database.AppDatabase
import com.school.manage.presentation.navigation.AppNavHost

@Composable
fun App(database: AppDatabase) {
    MaterialTheme {
        AppNavHost(database = database)
    }
}
EOF

# -------------------------------------------------------------
# 4. Presentation & Functional Screens
# -------------------------------------------------------------
cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/presentation/navigation/Screen.kt
package com.school.manage.presentation.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object StudentList : Screen("student_list")
    data object AddStudent : Screen("add_student")
    data object Attendance : Screen("attendance")
    data object FeeCollection : Screen("fee_collection")
    data object ExpenseList : Screen("expense_list")
    data object StaffList : Screen("staff_list")
}
EOF

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
    onNavigate: (String) -> Unit
) {
    val students by database.studentDao().getAllStudents().collectAsState(initial = emptyList())
    val feeRecords by database.feeDao().getAllFeeRecords().collectAsState(initial = emptyList())
    val expenses by database.expenseDao().getAllExpenses().collectAsState(initial = emptyList())
    val staffList by database.staffDao().getAllStaff().collectAsState(initial = emptyList())

    val totalFees = feeRecords.sumOf { it.amountPaid }
    val totalExpenses = expenses.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("School Dashboard", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard("Students", "${students.size}", Modifier.weight(1f))
                    MetricCard("Staff", "${staffList.size}", Modifier.weight(1f))
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard("Collected", "₹${totalFees.toInt()}", Modifier.weight(1f))
                    MetricCard("Expenses", "₹${totalExpenses.toInt()}", Modifier.weight(1f))
                }
            }
            item {
                Text(
                    text = "Modules & Operations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item { ActionCard("Students & Admissions", "Register, edit, and view students") { onNavigate("student_list") } }
            item { ActionCard("Attendance Register", "Mark daily attendance by class") { onNavigate("attendance") } }
            item { ActionCard("Fee Collection", "Record payments and track receipts") { onNavigate("fee_collection") } }
            item { ActionCard("Expense Tracker", "Record and manage school expenditures") { onNavigate("expense_list") } }
            item { ActionCard("Staff Directory", "View teachers and staff members") { onNavigate("staff_list") } }
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ActionCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
    onNavigateBack: () -> Unit,
    onNavigateAdd: () -> Unit
) {
    val students by database.studentDao().getAllStudents().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Students Directory (${students.size})") },
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
                Text("No students admitted yet. Tap + to register.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(students) { student ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(student.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Roll: ${student.rollNo}", color = MaterialTheme.colorScheme.primary)
                            }
                            Text("Class: ${student.gradeClass} (${student.section})")
                            Text("Guardian: ${student.guardianName} • Phone: ${student.phone}")
                            Text("Monthly Fee: ₹${student.monthlyFee.toInt()}")
                            Spacer(modifier = Modifier.height(8.dp))
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
                title = { Text("Admit New Student") },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("Cancel") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Student Full Name") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = rollNo, onValueChange = { rollNo = it }, label = { Text("Roll Number") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = gradeClass, onValueChange = { gradeClass = it }, label = { Text("Class (e.g. 5, 8, 10)") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = section, onValueChange = { section = it }, label = { Text("Section (A, B, C)") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = guardian, onValueChange = { guardian = it }, label = { Text("Guardian Name") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Mobile Phone") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = fee, onValueChange = { fee = it }, label = { Text("Monthly Tuition Fee (₹)") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    onClick = {
                        if (name.isNotBlank() && rollNo.isNotBlank()) {
                            scope.launch {
                                database.studentDao().insertStudent(
                                    
                   StudentEntity(
                                        rollNo = rollNo,
                                        name = name,
                                        gradeClass = gradeClass,
                                        section = section,
                                        guardianName = guardian,
                                        phone = phone,
                                        monthlyFee = fee.toDoubleOrNull() ?: 0.0,
                                        admissionDate = "2026-10-02"
                                    )
                                )
                                onNavigateBack()
                            }
                        }
                    }
                ) {
                    Text("Save Admission Record")
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
    onNavigateBack: () -> Unit
) {
    val students by database.studentDao().getAllStudents().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val attendanceMap = remember { mutableStateMapOf<Long, String>() }
    var submitted by remember { mutableStateOf(false) }

    // Default status to PRESENT
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
                title = { Text("Daily Attendance") },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Date: 2026-10-02", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            if (submitted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        "Attendance saved successfully for today!",
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
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
                                Text("Roll: ${student.rollNo} • Class: ${student.gradeClass}")
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
                modifier = Modifier.fillMaxWidth().height(50.dp),
                onClick = {
                    scope.launch {
                        val records = students.map { s ->
                            AttendanceEntity(
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
    onNavigateBack: () -> Unit
) {
    val feeRecords by database.feeDao().getAllFeeRecords().collectAsState(initial = emptyList())
    val students by database.studentDao().getAllStudents().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var studentName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("Cash") }
    var remarks by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fee Collection & Ledger") },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Record New Payment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item { OutlinedTextField(value = studentName, onValueChange = { studentName = it }, label = { Text("Student Name / Roll No") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount Paid (₹)") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = paymentMode, onValueChange = { paymentMode = it }, label = { Text("Payment Mode (Cash, UPI, Cheque)") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = remarks, onValueChange = { remarks = it }, label = { Text("Remarks (e.g. October Fee)") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    onClick = {
                        if (studentName.isNotBlank() && amount.isNotBlank()) {
                            scope.launch {
                                database.feeDao().insertFeeRecord(
                                    FeeRecordEntity(
                                        studentId = 0,
                                        studentName = studentName,
                                        gradeClass = "General",
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
                ) {
                    Text("Collect & Generate Receipt")
                }
            }
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Payment Records (${feeRecords.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(feeRecords) { fee ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(fee.studentName, fontWeight = FontWeight.Bold)
                            Text("₹${fee.amountPaid.toInt()}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Text("Mode: ${fee.paymentMode} • Date: ${fee.paymentDate}")
                        if (fee.remarks.isNotBlank()) Text("Note: ${fee.remarks}", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    onNavigateBack: () -> Unit
) {
    val expenses by database.expenseDao().getAllExpenses().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("School Expenses") },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("Add Expense", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) }
            item { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Expense Title (e.g. Electric Bill)") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (Maintenance, Salary, Utilities)") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (₹)") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (title.isNotBlank() && amount.isNotBlank()) {
                            scope.launch {
                                database.expenseDao().insertExpense(
                                    ExpenseEntity(
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
            item { Spacer(modifier = Modifier.height(10.dp)); Text("Expense Log (${expenses.size})", fontWeight = FontWeight.Bold) }
            items(expenses) { exp ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(exp.title, fontWeight = FontWeight.Bold)
                            Text(exp.category, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("₹${exp.amount.toInt()}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
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
    onNavigateBack: () -> Unit
) {
    val staffList by database.staffDao().getAllStaff().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staff & Faculty") },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("Add Staff Member", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) }
            item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Staff Full Name") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role (Teacher, Accountant, Staff)") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Contact Phone") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = salary, onValueChange = { salary = it }, label = { Text("Monthly Salary (₹)") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (name.isNotBlank()) {
                            scope.launch {
                                database.staffDao().insertStaff(
                                    StaffEntity(
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
                ) { Text("Save Staff Record") }
            }
            item { Spacer(modifier = Modifier.height(10.dp)); Text("Staff Directory (${staffList.size})", fontWeight = FontWeight.Bold) }
            items(staffList) { staff ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(staff.name, fontWeight = FontWeight.Bold)
                            Text("₹${staff.salary.toInt()}", color = MaterialTheme.colorScheme.primary)
                        }
                        Text("Role: ${staff.role} • Phone: ${staff.phone}")
                    }
                }
            }
        }
    }
}
EOF

cat << 'EOF' > composeApp/src/commonMain/kotlin/com/school/manage/presentation/navigation/AppNavHost.kt
package com.school.manage.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.school.manage.core.database.AppDatabase
import com.school.manage.presentation.screens.*

@Composable
fun AppNavHost(database: AppDatabase) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(database = database, onNavigate = { route -> navController.navigate(route) })
        }
        composable(Screen.StudentList.route) {
            StudentListScreen(
                database = database,
                onNavigateBack = { navController.popBackStack() },
                onNavigateAdd = { navController.navigate(Screen.AddStudent.route) }
            )
        }
        composable(Screen.AddStudent.route) {
            AddStudentScreen(database = database, onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Attendance.route) {
            AttendanceScreen(database = database, onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.FeeCollection.route) {
            FeeCollectionScreen(database = database, onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.ExpenseList.route) {
            ExpenseScreen(database = database, onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.StaffList.route) {
            StaffScreen(database = database, onNavigateBack = { navController.popBackStack() })
        }
    }
}
EOF

# -------------------------------------------------------------
# 5. Build Updated ZIP & Copy to Downloads
# -------------------------------------------------------------
cd ..
zip -r "school-management-app.zip" "school-management-app" -x "*/.git/*"

if [ -d "/sdcard/Download" ]; then
    cp "school-management-app.zip" /sdcard/Download/
    echo "========================================================"
    echo "SUCCESS: school-management-app.zip is ready in Downloads!"
    echo "========================================================"
fi
cd school-management-app
