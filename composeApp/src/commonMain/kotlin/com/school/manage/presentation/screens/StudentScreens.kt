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
import com.school.manage.core.database.entity.StudentEntity
import com.school.manage.core.firebase.FirestoreSyncService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentListScreen(
    schoolCode: String,
    database: AppDatabase,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val students by database.studentDao().getStudentsBySchool(schoolCode).collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Directory") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Student")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(students) { student ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(student.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Class: ${student.gradeClass} | Phone: ${student.phone}", color = Color.Gray, fontSize = 14.sp)
                            Text("Monthly Fee: ₹${student.monthlyFee}", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        }
                        IconButton(onClick = {
                            scope.launch { database.studentDao().deleteStudent(student) }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddStudentDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, gradeClass, phone, monthlyFee, admissionDate ->
                    scope.launch {
                        val studentToSave = StudentEntity(
                            schoolCode = schoolCode,
                            name = name.trim(),
                            gradeClass = gradeClass.trim(),
                            phone = phone.trim(),
                            monthlyFee = monthlyFee.toDoubleOrNull() ?: 0.0,
                            admissionDate = admissionDate,
                            password = phone.trim()
                        )
                        val newId = database.studentDao().insertStudent(studentToSave)
                        try {
                            FirestoreSyncService(database).syncStudent(schoolCode, studentToSave.copy(id = newId))
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
fun AddStudentDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, gradeClass: String, phone: String, monthlyFee: String, admissionDate: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var gradeClass by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var monthlyFee by remember { mutableStateOf("") }
    var admissionDate by remember { mutableStateOf("01/01/2026") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Admit New Student") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Student Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = gradeClass, onValueChange = { gradeClass = it }, label = { Text("Class / Grade") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Parent's Phone") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = monthlyFee, onValueChange = { monthlyFee = it }, label = { Text("Monthly Fee") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = admissionDate, onValueChange = { admissionDate = it }, label = { Text("Admission Date") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onConfirm(name, gradeClass, phone, monthlyFee, admissionDate)
                    }
                }
            ) {
                Text("Admit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
