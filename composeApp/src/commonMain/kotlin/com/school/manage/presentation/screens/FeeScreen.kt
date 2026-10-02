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
