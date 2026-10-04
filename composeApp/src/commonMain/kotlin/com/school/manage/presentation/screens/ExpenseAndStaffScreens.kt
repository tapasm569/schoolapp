package com.school.manage.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.ExpenseEntity
import com.school.manage.core.database.entity.StaffEntity
import com.school.manage.core.firebase.FirestoreSyncService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementScreen(
    schoolCode: String,
    database: AppDatabase,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val staffList by database.staffDao().getStaffBySchool(schoolCode).collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staff & Teachers") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Staff")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(staffList) { staff ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(staff.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Role: ${staff.role} | Phone: ${staff.phone}", color = Color.Gray, fontSize = 14.sp)
                            Text("Salary: ₹${staff.salary}", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        }
                        IconButton(onClick = {
                            scope.launch { database.staffDao().deleteStaff(staff) }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddStaffDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, phone, role, salary, joinDate ->
                    scope.launch {
                        val staffToSave = StaffEntity(
                            schoolCode = schoolCode,
                            name = name.trim(),
                            phone = phone.trim(),
                            role = role.ifBlank { "Teacher" },
                            salary = salary.toDoubleOrNull() ?: 0.0,
                            joinDate = joinDate,
                            password = phone.trim()
                        )
                        val newId = database.staffDao().insertStaff(staffToSave)
                        try {
                            FirestoreSyncService(database).syncStaff(schoolCode, staffToSave.copy(id = newId))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun AddStaffDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, role: String, salary: String, joinDate: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var joinDate by remember { mutableStateOf("01/01/2026") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Staff Member") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role (Teacher/Admin)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = salary, onValueChange = { salary = it }, label = { Text("Salary") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = joinDate, onValueChange = { joinDate = it }, label = { Text("Join Date") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onConfirm(name, phone, role, salary, joinDate)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrackerScreen(
    schoolCode: String,
    database: AppDatabase,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val expenses by database.expenseDao().getExpensesBySchool(schoolCode).collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses Tracker") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(expenses) { exp ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(exp.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Category: ${exp.category} | Date: ${exp.date}", color = Color.Gray, fontSize = 14.sp)
                            Text("Amount: ₹${exp.amount}", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = {
                            scope.launch { database.expenseDao().deleteExpense(exp) }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddExpenseDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, category, amount, date ->
                    scope.launch {
                        val expToSave = ExpenseEntity(
                            schoolCode = schoolCode,
                            title = title.trim(),
                            category = category.trim(),
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            date = date
                        )
                        val newId = database.expenseDao().insertExpense(expToSave)
                        try {
                            FirestoreSyncService(database).syncExpense(schoolCode, expToSave.copy(id = newId))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, category: String, amount: String, date: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("01/01/2026") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && amount.isNotBlank()) {
                        onConfirm(title, category, amount, date)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
