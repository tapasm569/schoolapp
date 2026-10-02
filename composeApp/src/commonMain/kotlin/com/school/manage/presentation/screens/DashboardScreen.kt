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
