package com.school.manage.presentation.screens

import com.school.manage.core.utils.getSystemCurrentDate
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
import com.school.manage.core.database.entity.StudentEntity
import com.school.manage.presentation.theme.LocalSchoolColors
import kotlinx.coroutines.launch

private val attendanceClasses = listOf(
    "Nursery",
    "LKG",
    "UKG",
    "Class 1",
    "Class 2",
    "Class 3",
    "Class 4"
)

private val attendanceGreen = Color(0xFF16A34A)
private val attendanceRed = Color(0xFFDC2626)
private val attendanceAmber = Color(0xFFD97706)

private fun compareRollNumbers(a: StudentEntity, b: StudentEntity): Int {
    val first = a.rollNo.trim()
    val second = b.rollNo.trim()
    val firstNumber = first.toLongOrNull()
    val secondNumber = second.toLongOrNull()

    return if (firstNumber != null && secondNumber != null) {
        firstNumber.compareTo(secondNumber)
    } else {
        first.compareTo(second, ignoreCase = true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val scope = rememberCoroutineScope()

    val students by database.studentDao()
        .getStudentsBySchool(schoolCode)
        .collectAsState(initial = emptyList())

    val attendanceRecords by database.attendanceDao()
        .getAttendanceBySchool(schoolCode)
        .collectAsState(initial = emptyList())

    var date by remember {
        mutableStateOf(getSystemCurrentDate().formatted)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedClass by remember { mutableStateOf<String?>(null) }
    var savedMessage by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val attendanceMap = remember {
        mutableStateMapOf<Long, String>()
    }

    val classStudents = remember(students, selectedClass) {
        students
            .filter {
                selectedClass != null &&
                    it.gradeClass.equals(selectedClass, ignoreCase = true)
            }
            .sortedWith { a, b -> compareRollNumbers(a, b) }
    }

    val dateRecords = remember(attendanceRecords, date) {
        attendanceRecords.filter {
            it.userType == "STUDENT" && it.date == date
        }
    }

    val savedByStudent = remember(dateRecords) {
        dateRecords
            .groupBy { it.studentId }
            .mapValues { (_, records) -> records.last().status }
    }

    // Restore saved choices when opening a class or changing the date.
    LaunchedEffect(selectedClass, date, students, attendanceRecords) {
        attendanceMap.clear()

        val className = selectedClass ?: return@LaunchedEffect

        val matchingStudents = students.filter {
            it.gradeClass.equals(className, ignoreCase = true)
        }

        matchingStudents.forEach { student ->
            val savedStatus = savedByStudent[student.id]
            if (savedStatus in listOf("PRESENT", "ABSENT", "LEAVE")) {
                attendanceMap[student.id] = savedStatus!!
            }
        }
    }

    val classRecords = remember(dateRecords, selectedClass) {
        dateRecords.filter {
            selectedClass != null &&
                it.gradeClass.equals(selectedClass, ignoreCase = true)
        }
    }

    fun classIsCompleted(className: String): Boolean {
        val members = students.filter {
            it.gradeClass.equals(className, ignoreCase = true)
        }

        if (members.isEmpty()) return false

        val records = dateRecords.filter {
            it.gradeClass.equals(className, ignoreCase = true)
        }

        return members.all { student ->
            records.any {
                it.studentId == student.id &&
                    it.status in listOf("PRESENT", "ABSENT", "LEAVE")
            }
        }
    }

    val allStudentsMarked = classStudents.isNotEmpty() &&
        classStudents.all {
            attendanceMap[it.id] in listOf("PRESENT", "ABSENT", "LEAVE")
        }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = selectedClass ?: "Class Attendance",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedClass != null) {
                                selectedClass = null
                                savedMessage = ""
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Text(
                            "←",
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.bgApp
                )
            )
        },
        bottomBar = {
            if (selectedClass != null) {
                Surface(
                    color = colors.bgApp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "${attendanceMap.size.coerceAtMost(classStudents.size)} of ${classStudents.size} students marked",
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )

                        Button(
                            onClick = {
                                if (!allStudentsMarked || isSaving) return@Button

                                scope.launch {
                                    isSaving = true
                                    savedMessage = ""

                                    try {
                                        val list = classStudents.map { student ->
                                            AttendanceEntity(
                                                schoolCode = schoolCode,
                                                studentId = student.id,
                                                studentName = student.name,
                                                gradeClass = student.gradeClass,
                                                staffId = 0L,
                                                userType = "STUDENT",
                                                date = date,
                                                status = attendanceMap.getValue(student.id)
                                            )
                                        }

                                        database.attendanceDao().insertAll(list)

                                        savedMessage =
                                            "Attendance saved successfully for $selectedClass on $date."
                                    } catch (e: Exception) {
                                        savedMessage =
                                            "Could not save attendance: ${e.message ?: "Please try again."}"
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            },
                            enabled = allStudentsMarked && !isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.brandPrimary
                            )
                        ) {
                            Text(
                                if (isSaving) "Saving..." else "Save Class Attendance",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.inputBg,
                    border = BorderStroke(1.dp, colors.inputBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Attendance Date",
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                date,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                        Text("📅", fontSize = 23.sp)
                    }
                }
            }

            if (savedMessage.isNotEmpty()) {
                item {
                    Surface(
                        color = if (savedMessage.startsWith("Could not"))
                            Color(0xFFFFE4E6)
                        else
                            Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            savedMessage,
                            modifier = Modifier.padding(12.dp),
                            color = if (savedMessage.startsWith("Could not"))
                                Color(0xFF9F1239)
                            else
                                Color(0xFF166534),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (selectedClass == null) {
                item {
                    Text(
                        "Select a class",
                        color = colors.textPrimary,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Choose a class to mark attendance for $date.",
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                }

                items(attendanceClasses) { className ->
                    val count = students.count {
                        it.gradeClass.equals(className, ignoreCase = true)
                    }
                    val completed = classIsCompleted(className)

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = colors.bgCard,
                        border = BorderStroke(
                            1.dp,
                            if (completed) attendanceGreen.copy(alpha = 0.5f)
                            else colors.borderCard
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedClass = className
                                savedMessage = ""
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = colors.brandPrimary.copy(alpha = 0.12f),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        when (className) {
                                            "Nursery" -> "N"
                                            "LKG" -> "L"
                                            "UKG" -> "U"
                                            else -> className.removePrefix("Class ")
                                        },
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.brandPrimary
                                    )
                                }
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    className,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "$count ${if (count == 1) "student" else "students"}",
                                    fontSize = 13.sp,
                                    color = colors.textSecondary
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (completed)
                                    attendanceGreen.copy(alpha = 0.13f)
                                else
                                    attendanceAmber.copy(alpha = 0.13f)
                            ) {
                                Text(
                                    if (completed) "Completed" else "Pending",
                                    modifier = Modifier.padding(
                                        horizontal = 10.dp,
                                        vertical = 7.dp
                                    ),
                                    color = if (completed)
                                        attendanceGreen
                                    else
                                        attendanceAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    val completedCount = classStudents.count {
                        attendanceMap[it.id] in listOf("PRESENT", "ABSENT", "LEAVE")
                    }

                    Text(
                        "$selectedClass • ${classStudents.size} students",
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "$completedCount of ${classStudents.size} attendance choices completed",
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                }

                if (classStudents.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No students found in $selectedClass.",
                                color = colors.textSecondary
                            )
                        }
                    }
                } else {
                    items(classStudents, key = { it.id }) { student ->
                        val status = attendanceMap[student.id]

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.bgCard,
                            border = BorderStroke(1.dp, colors.borderCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            student.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            "Roll No: ${student.rollNo.ifBlank { "—" }}",
                                            fontSize = 12.sp,
                                            color = colors.textSecondary
                                        )
                                    }

                                    Text(
                                        when (status) {
                                            "PRESENT" -> "Present"
                                            "ABSENT" -> "Absent"
                                            "LEAVE" -> "Leave"
                                            else -> "Not marked"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (status) {
                                            "PRESENT" -> attendanceGreen
                                            "ABSENT" -> attendanceRed
                                            "LEAVE" -> attendanceAmber
                                            else -> colors.textSecondary
                                        }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AttendanceStatusPill(
                                        "P",
                                        status == "PRESENT",
                                        attendanceGreen
                                    ) {
                                        attendanceMap[student.id] = "PRESENT"
                                    }
                                    AttendanceStatusPill(
                                        "A",
                                        status == "ABSENT",
                                        attendanceRed
                                    ) {
                                        attendanceMap[student.id] = "ABSENT"
                                    }
                                    AttendanceStatusPill(
                                        "L",
                                        status == "LEAVE",
                                        attendanceAmber
                                    ) {
                                        attendanceMap[student.id] = "LEAVE"
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }

        if (showDatePicker) {
            ModernDatePickerDialog(
                currentDate = date,
                onDateSelected = {
                    date = it
                    savedMessage = ""
                },
                onDismiss = { showDatePicker = false }
            )
        }
    }
}

@Composable
fun RowScope.AttendanceStatusPill(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val colors = LocalSchoolColors.current

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) activeColor else colors.borderCard,
        modifier = Modifier
            .weight(1f)
            .height(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = when (label) {
                    "P" -> "Present"
                    "A" -> "Absent"
                    "L" -> "Leave"
                    else -> label
                },
                color = if (isSelected) Color.White else colors.textSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}
