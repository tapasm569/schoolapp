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
