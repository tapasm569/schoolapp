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
    onNavigateBack: () -> Unit,
    onNavigateAdd: () -> Unit
) {
    val students by database.studentDao().getAllStudents().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Students Directory (${students.size})") },
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
                Text("No students admitted yet. Tap + to register.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(students) { student ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(student.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Roll: ${student.rollNo}", color = MaterialTheme.colorScheme.primary)
                            }
                            Text("Class: ${student.gradeClass} (${student.section})")
                            Text("Guardian: ${student.guardianName} • Phone: ${student.phone}")
                            Text("Monthly Fee: ₹${student.monthlyFee.toInt()}")
                            Spacer(modifier = Modifier.height(8.dp))
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
                title = { Text("Admit New Student") },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("Cancel") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Student Full Name") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = rollNo, onValueChange = { rollNo = it }, label = { Text("Roll Number") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = gradeClass, onValueChange = { gradeClass = it }, label = { Text("Class (e.g. 5, 8, 10)") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = section, onValueChange = { section = it }, label = { Text("Section (A, B, C)") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = guardian, onValueChange = { guardian = it }, label = { Text("Guardian Name") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Mobile Phone") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = fee, onValueChange = { fee = it }, label = { Text("Monthly Tuition Fee (₹)") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    onClick = {
                        if (name.isNotBlank() && rollNo.isNotBlank()) {
                            scope.launch {
                                database.studentDao().insertStudent(
                                    
                   StudentEntity(
                                        rollNo = rollNo,
                                        name = name,
                                        gradeClass = gradeClass,
                                        section = section,
                                        guardianName = guardian,
                                        phone = phone,
                                        monthlyFee = fee.toDoubleOrNull() ?: 0.0,
                                        admissionDate = "2026-10-02"
                                    )
                                )
                                onNavigateBack()
                            }
                        }
                    }
                ) {
                    Text("Save Admission Record")
                }
            }
        }
    }
}
