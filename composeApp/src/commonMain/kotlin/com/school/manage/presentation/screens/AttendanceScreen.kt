package com.school.manage.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.AttendanceEntity
import com.school.manage.presentation.theme.LocalSchoolColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val students by database.studentDao().getStudentsBySchool(schoolCode).collectAsState(initial = emptyList())
    val attendanceMap = remember { mutableStateMapOf<Long, String>() }
    val scope = rememberCoroutineScope()
    var date by remember { mutableStateOf("03/10/2026") }
    var showDatePicker by remember { mutableStateOf(false) }
    var savedMessage by remember { mutableStateOf("") }

    LaunchedEffect(students) {
        students.forEach { s ->
            if (!attendanceMap.containsKey(s.id)) {
                attendanceMap[s.id] = "PRESENT"
            }
        }
    }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("Mark Attendance", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary) }
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
                        scope.launch {
                            val list = students.map { s ->
                                AttendanceEntity(
                                    schoolCode = schoolCode,
                                    studentId = s.id,
                                    studentName = s.name,
                                    gradeClass = s.gradeClass,
                                    staffId = 0L,
                                    userType = "STUDENT",
                                    date = date,
                                    status = attendanceMap[s.id] ?: "PRESENT"
                                )
                            }
                            database.attendanceDao().insertAll(list)
                            savedMessage = "Attendance saved successfully for " + date + "!"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary)
                ) {
                    Text("Save Attendance", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.inputBg,
                    border = BorderStroke(1.dp, colors.inputBorder),
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Attendance Date: " + date, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Text("📅", fontSize = 18.sp)
                    }
                }
            }

            if (savedMessage.isNotEmpty()) {
                item {
                    Text(savedMessage, color = colors.success, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            if (students.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No students found to mark attendance.", color = colors.textSecondary)
                    }
                }
            } else {
                items(students) { student ->
                    val status = attendanceMap[student.id] ?: "PRESENT"
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.bgCard,
                        border = BorderStroke(1.dp, colors.borderCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(student.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                                Text("Class: " + student.gradeClass.ifEmpty { "General" }, fontSize = 12.sp, color = colors.textSecondary)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                AttendanceStatusPill("P", status == "PRESENT", Color(0xFF22C55E)) {
                                    attendanceMap[student.id] = "PRESENT"
                                }
                                AttendanceStatusPill("A", status == "ABSENT", Color(0xFFEF4444)) {
                                    attendanceMap[student.id] = "ABSENT"
                                }
                                AttendanceStatusPill("L", status == "LEAVE", Color(0xFFF59E0B)) {
                                    attendanceMap[student.id] = "LEAVE"
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }

        if (showDatePicker) {
            ModernDatePickerDialog(
                currentDate = date,
                onDateSelected = { date = it },
                onDismiss = { showDatePicker = false }
            )
        }
    }
}

@Composable
fun AttendanceStatusPill(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val colors = LocalSchoolColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) activeColor else colors.borderCard,
        modifier = Modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (isSelected) Color.White else colors.textSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
