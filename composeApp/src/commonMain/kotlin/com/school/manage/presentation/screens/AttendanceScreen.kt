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
import com.school.manage.core.database.entity.AttendanceEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val students by database.studentDao().getStudentsBySchool(schoolCode).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val attendanceMap = remember { mutableStateMapOf<Long, String>() }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(students) {
        students.forEach { s ->
            if (!attendanceMap.containsKey(s.id)) {
                attendanceMap[s.id] = "PRESENT"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance ($schoolCode)") },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            Text("Date: 2026-10-02", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(10.dp))

            if (submitted) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text("Attendance records saved!", modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(students) { student ->
                    val status = attendanceMap[student.id] ?: "PRESENT"
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column {
                                Text(student.name, fontWeight = FontWeight.Bold)
                                Text("Class: ${student.gradeClass} • Roll: ${student.rollNo}")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = { attendanceMap[student.id] = "PRESENT" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (status == "PRESENT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) { Text("P") }
                                Button(
                                    onClick = { attendanceMap[student.id] = "ABSENT" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (status == "ABSENT") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) { Text("A") }
                            }
                        }
                    }
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                onClick = {
                    scope.launch {
                        val records = students.map { s ->
                            AttendanceEntity(
                                schoolCode = schoolCode,
                                studentId = s.id,
                                studentName = s.name,
                                gradeClass = s.gradeClass,
                                date = "2026-10-02",
                                status = attendanceMap[s.id] ?: "PRESENT"
                            )
                        }
                        database.attendanceDao().insertAll(records)
                        submitted = true
                    }
                }
            ) {
                Text("Submit Attendance")
            }
        }
    }
}
