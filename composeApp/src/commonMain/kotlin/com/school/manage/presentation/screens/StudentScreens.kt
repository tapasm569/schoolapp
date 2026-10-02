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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentListScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit,
    onNavigateAdd: () -> Unit
) {
    val students by database.studentDao().getStudentsBySchool(schoolCode).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Students ($schoolCode)") },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateAdd) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { padding ->
        if (students.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No students registered for $schoolCode. Tap + to add.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(students) { student ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(student.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Roll: ${student.rollNo}", color = MaterialTheme.colorScheme.primary)
                            }
                            Text("Class: ${student.gradeClass} (${student.section})")
                            Text("Phone (Student Login): ${student.phone}")
                            Text("Monthly Fee: ₹${student.monthlyFee.toInt()}")
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { scope.launch { database.studentDao().deleteStudent(student) } },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var rollNo by remember { mutableStateOf("") }
    var gradeClass by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }
    var guardian by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var fee by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admit Student ($schoolCode)") },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("Cancel") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = rollNo, onValueChange = { rollNo = it }, label = { Text("Roll Number") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = gradeClass, onValueChange = { gradeClass = it }, label = { Text("Class / Grade") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = section, onValueChange = { section = it }, label = { Text("Section") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = guardian, onValueChange = { guardian = it }, label = { Text("Guardian Name") }, modifier = Modifier.fillMaxWidth()) }
            item {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number (Used as Student Password)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item { OutlinedTextField(value = fee, onValueChange = { fee = it }, label = { Text("Monthly Fee (₹)") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    onClick = {
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            scope.launch {
                                database.studentDao().insertStudent(
                                    StudentEntity(
                                        schoolCode = schoolCode,
                                        rollNo = rollNo,
                                        name = name,
                                        gradeClass = gradeClass,
                                        section = section,
                                        guardianName = guardian,
                                        phone = phone.trim(),
                                        monthlyFee = fee.toDoubleOrNull() ?: 0.0,
                                        admissionDate = "2026-10-02"
                                    )
                                )
                                onNavigateBack()
                            }
                        }
                    }
                ) {
                    Text("Admit & Save Student")
                }
            }
        }
    }
}
