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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.ExpenseEntity
import com.school.manage.core.database.entity.StaffEntity
import kotlinx.coroutines.launch

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
    var showStartDatePicker by remember { mutableStateOf(false) }

    var gender by remember { mutableStateOf("Male") }
    var whatsapp by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var qualification by remember { mutableStateOf("") }

    var position by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }

    var salaryType by remember { mutableStateOf("Monthly") }
    var showSalaryTypeMenu by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = { Text("Add Teacher / Staff", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                                    password = phone.trim()
                                )
                            )
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D529C))
                ) {
                    Text("Save Teacher", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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

            item {
                Column {
                    Text("Staff Information", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                    Text("Enter staff personal details here.", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }

            item { CustomRoundedInput(value = name, onValueChange = { name = it }, placeholder = "Enter Name") }

            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth().clickable { showStartDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Start Date: $startDate", fontSize = 14.sp, color = Color(0xFF0F172A))
                        Text("📅", fontSize = 18.sp)
                    }
                }
            }

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

            item {
                Column {
                    Text("Contact Information", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                    Text("Enter contact details here.", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }

            item { CustomRoundedInput(value = whatsapp, onValueChange = { whatsapp = it }, placeholder = "WhatsApp") }
            item { CustomRoundedInput(value = phone, onValueChange = { phone = it }, placeholder = "Mobile number (Acts as login password)") }
            item { CustomRoundedInput(value = address, onValueChange = { address = it }, placeholder = "Enter Address") }

            item {
                Column {
                    Text("Qualification And Salary", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                    Text("Enter Qualification details here.", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }

            item { CustomRoundedInput(value = qualification, onValueChange = { qualification = it }, placeholder = "Enter Qualification") }
            item { CustomRoundedInput(value = position, onValueChange = { position = it }, placeholder = "Enter Position (e.g. Math Teacher)") }
            item { CustomRoundedInput(value = salary, onValueChange = { salary = it }, placeholder = "Enter Salary (e.g. 15000)") }

            item {
                Box {
                    DropdownTriggerInput(
                        label = "Select Salary Frequency",
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

            if (errorMsg.isNotEmpty()) {
                item {
                    Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }

        if (showStartDatePicker) {
            ModernDatePickerDialog(
                currentDate = startDate,
                onDateSelected = { startDate = it },
                onDismiss = { showStartDatePicker = false }
            )
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
                title = { Text("Staff & Faculty Directory (${staffList.size})") },
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
