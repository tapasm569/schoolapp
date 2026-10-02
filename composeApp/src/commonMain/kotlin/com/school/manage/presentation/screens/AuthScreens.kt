package com.school.manage.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.SchoolEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    database: AppDatabase,
    onSchoolLoginSuccess: (String, String) -> Unit,
    onStudentLoginSuccess: (Long) -> Unit,
    onNavigateRegister: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var schoolCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var studentPhone by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("School Management System", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0; errorMessage = "" }, text = { Text("School Admin") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1; errorMessage = "" }, text = { Text("Student Portal") })
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedTab == 0) {
                // School Admin Login
                Text("School Administrator Login", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = schoolCode,
                    onValueChange = { schoolCode = it.uppercase() },
                    label = { Text("School Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    onClick = {
                        scope.launch {
                            val school = database.schoolDao().loginSchool(schoolCode.trim(), password)
                            if (school != null) {
                                onSchoolLoginSuccess(school.schoolCode, school.schoolName)
                            } else {
                                errorMessage = "Invalid School Code or Password"
                            }
                        }
                    }
                ) {
                    Text("Login as School")
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onNavigateRegister) {
                    Text("Register New School")
                }
            } else {
                // Student Login
                Text("Student / Guardian Login", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Password is your mobile number registered during admission.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = schoolCode,
                    onValueChange = { schoolCode = it.uppercase() },
                    label = { Text("School Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = studentPhone,
                    onValueChange = { studentPhone = it },
                    label = { Text("Registered Mobile Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    onClick = {
                        scope.launch {
                            val student = database.studentDao().loginStudent(schoolCode.trim(), studentPhone.trim())
                            if (student != null) {
                                onStudentLoginSuccess(student.id)
                            } else {
                                errorMessage = "No student found matching this School Code & Mobile Number"
                            }
                        }
                    }
                ) {
                    Text("Login as Student")
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterSchoolScreen(
    database: AppDatabase,
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var schoolName by remember { mutableStateOf("") }
    var schoolCode by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Register Your School") },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = schoolName,
                onValueChange = { schoolName = it },
                label = { Text("School Full Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = schoolCode,
                onValueChange = { schoolCode = it.uppercase() },
                label = { Text("Create Unique School Code (e.g., SCH01)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Official Contact Number") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Admin Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMsg.isNotEmpty()) {
                Text(errorMsg, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                onClick = {
                    if (schoolName.isBlank() || schoolCode.isBlank() || password.isBlank()) {
                        errorMsg = "Please fill in all mandatory fields"
                        return@Button
                    }
                    scope.launch {
                        val existing = database.schoolDao().getSchoolByCode(schoolCode.trim())
                        if (existing != null) {
                            errorMsg = "School Code '$schoolCode' is already taken. Pick another code."
                        } else {
                            database.schoolDao().registerSchool(
                                SchoolEntity(
                                    schoolCode = schoolCode.trim(),
                                    schoolName = schoolName.trim(),
                                    password = password,
                                    phone = phone
                                )
                            )
                            onRegisterSuccess()
                        }
                    }
                }
            ) {
                Text("Complete School Registration")
            }
        }
    }
}
