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
