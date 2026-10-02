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
import com.school.manage.presentation.theme.LocalSchoolColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStaffScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val colors = LocalSchoolColors.current
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
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("Add Teacher / Staff", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgApp)
            )
        },
        bottomBar = {
            Surface(
                color = colors.bgApp,
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
                    colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary)
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
                    color = colors.bgCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Profile photo", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                            Text("This image will be displayed on Profile", fontSize = 12.sp, color = colors.textSecondary)
                        }
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(colors.borderCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👤", fontSize = 24.sp)
                        }
                    }
                }
            }

            item {
                Column {
                    Text("Staff Information", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                    Text("Enter staff personal details here.", fontSize = 12.sp, color = colors.textSecondary)
                }
            }

            item { CustomRoundedInput(value = name, onValueChange = { name = it }, placeholder = "Enter Name") }

            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.inputBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.inputBorder),
                    modifier = Modifier.fillMaxWidth().clickable { showStartDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Start Date: " + startDate, fontSize = 14.sp, color = colors.textPrimary)
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
                    Text("Contact Information", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                    Text("Enter contact details here.", fontSize = 12.sp, color = colors.textSecondary)
                }
            }

            item { CustomRoundedInput(value = whatsapp, onValueChange = { whatsapp = it }, placeholder = "WhatsApp") }
            item { CustomRoundedInput(value = phone, onValueChange = { phone = it }, placeholder = "Mobile number (Acts as login password)") }
            item { CustomRoundedInput(value = address, onValueChange = { address = it }, placeholder = "Enter Address") }

            item {
                Column {
                    Text("Qualification And Salary", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                    Text("Enter Qualification details here.", fontSize = 12.sp, color = colors.textSecondary)
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
                    Text(errorMsg, color = colors.error, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
    val colors = LocalSchoolColors.current
    val staffList by database.staffDao().getStaffBySchool(schoolCode).collectAsState(initial = emptyList())
    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("Staff & Faculty Directory (" + staffList.size + ")", color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgApp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(staffList) { staff ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.bgCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(staff.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
                        Text("Position: " + staff.role + " • Contact: " + staff.phone, color = colors.textSecondary)
                        Text("Salary: ₹" + staff.salary.toInt() + " (" + staff.salaryType + ")", color = colors.brandPrimary, fontWeight = FontWeight.SemiBold)
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
    val colors = LocalSchoolColors.current
    val expenses by database.expenseDao().getExpensesBySchool(schoolCode).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("Expenses (" + schoolCode + ")", color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgApp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("Add Expense", fontWeight = FontWeight.Bold, color = colors.textPrimary) }
            item { CustomRoundedInput(value = title, onValueChange = { title = it }, placeholder = "Title") }
            item { CustomRoundedInput(value = category, onValueChange = { category = it }, placeholder = "Category") }
            item { CustomRoundedInput(value = amount, onValueChange = { amount = it }, placeholder = "Amount (₹)") }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
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
                ) { Text("Save Expense", fontWeight = FontWeight.Bold) }
            }
            items(expenses) { exp ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.bgCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(exp.title, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Text("₹" + exp.amount.toInt(), color = colors.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
