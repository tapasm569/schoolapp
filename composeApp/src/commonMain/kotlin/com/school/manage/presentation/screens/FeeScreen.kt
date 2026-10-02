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
