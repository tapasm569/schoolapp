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
