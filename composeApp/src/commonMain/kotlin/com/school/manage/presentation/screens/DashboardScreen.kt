package com.school.manage.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.school.manage.core.database.AppDatabase
import com.school.manage.presentation.theme.LocalSchoolColors
import com.school.manage.presentation.theme.LocalThemeToggle
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    database: AppDatabase,
    schoolCode: String,
    schoolName: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val toggleTheme = LocalThemeToggle.current

    val students by database.studentDao().getStudentsBySchool(schoolCode).collectAsState(initial = emptyList())
    val feeRecords by database.feeDao().getFeeRecordsBySchool(schoolCode).collectAsState(initial = emptyList())
    val expenses by database.expenseDao().getExpensesBySchool(schoolCode).collectAsState(initial = emptyList())
    val staffList by database.staffDao().getStaffBySchool(schoolCode).collectAsState(initial = emptyList())
    val batches by database.batchDao().getBatchesBySchool(schoolCode).collectAsState(initial = emptyList())
    val attendanceList by database.attendanceDao().getAttendanceBySchool(schoolCode).collectAsState(initial = emptyList())

    val monthsList = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    var selectedMonthIndex by remember { mutableStateOf(9) }
    var selectedYear by remember { mutableStateOf(2026) }

    var selectedAttendanceDay by remember { mutableStateOf<Int?>(null) }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }

    val currentMonthLabel = monthsList[selectedMonthIndex] + "-" + selectedYear
    val monthNumVal = selectedMonthIndex + 1
    val monthNumberStr = if (monthNumVal < 10) "0" + monthNumVal else monthNumVal.toString()
    val monthFilterPattern = monthNumberStr + "/" + selectedYear

    val daysInMonth = remember(selectedMonthIndex, selectedYear) {
        when (selectedMonthIndex) {
            1 -> if (selectedYear % 4 == 0 && (selectedYear % 100 != 0 || selectedYear % 400 == 0)) 29 else 28
            3, 5, 8, 10 -> 30
            else -> 31
        }
    }

    val monthlyFees = feeRecords.filter { it.feeMonth.contains(monthFilterPattern) || it.paymentDate.contains(monthFilterPattern) }.sumOf { it.amountPaid }
    val totalFees = feeRecords.sumOf { it.amountPaid }
    val monthlyExpenses = expenses.filter { it.date.contains(monthFilterPattern) }.sumOf { it.amount }
    val totalExpenses = expenses.sumOf { it.amount }
    val netMonthCashFlow = monthlyFees - monthlyExpenses

    val studentMonthAttendance = attendanceList.filter { it.userType == "STUDENT" && it.date.contains(monthFilterPattern) }
    val teacherMonthAttendance = attendanceList.filter { it.userType == "STAFF" && it.date.contains(monthFilterPattern) }

    val activeAttendanceRecords = remember(selectedAttendanceDay, studentMonthAttendance, selectedMonthIndex, selectedYear) {
        if (selectedAttendanceDay == null) {
            studentMonthAttendance
        } else {
            val dStr = if (selectedAttendanceDay!! < 10) "0" + selectedAttendanceDay else selectedAttendanceDay.toString()
            val fullTargetDate = dStr + "/" + monthNumberStr + "/" + selectedYear
            val altTargetDate = selectedAttendanceDay.toString() + "/" + monthNumberStr + "/" + selectedYear
            studentMonthAttendance.filter { it.date == fullTargetDate || it.date == altTargetDate }
        }
    }

    val presentCount = activeAttendanceRecords.count { it.status == "PRESENT" }
    val absentCount = activeAttendanceRecords.count { it.status == "ABSENT" }
    val leaveCount = activeAttendanceRecords.count { it.status == "LEAVE" }
    val holidayCount = activeAttendanceRecords.count { it.status == "HOLIDAY" }

    val studentTotalMarked = presentCount + absentCount + leaveCount
    val studentPct = if (studentTotalMarked > 0) (presentCount * 100 / studentTotalMarked) else 0

    val teacherPresent = teacherMonthAttendance.count { it.status == "PRESENT" }
    val teacherTotalMarked = teacherMonthAttendance.count { it.status in listOf("PRESENT", "ABSENT", "LEAVE") }
    val teacherPct = if (teacherTotalMarked > 0) (teacherPresent * 100 / teacherTotalMarked) else 0

    var showAddNewBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = colors.bgApp,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddNewBottomSheet = true },
                containerColor = colors.brandPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Text("+", fontSize = 32.sp, fontWeight = FontWeight.Light)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }

            // 1. Header Profile & Theme Switcher
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onLogout() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.brandPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👤", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = schoolName.ifEmpty { "TAPAS MONDAL" }.uppercase(),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = colors.textPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Change Account",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("▾", fontSize = 10.sp, color = colors.textSecondary)
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = toggleTheme,
                            shape = CircleShape,
                            color = colors.bgCard,
                            border = BorderStroke(1.dp, colors.borderCard),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(if (colors.isDark) "☀️" else "🌙", fontSize = 16.sp)
                            }
                        }

                        Surface(
                            onClick = { onLogout() },
                            shape = CircleShape,
                            color = colors.bgCard,
                            border = BorderStroke(1.dp, colors.borderCard),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("⚙", fontSize = 16.sp, color = colors.textPrimary)
                            }
                        }
                    }
                }
            }

            // 2. Directory Counts
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TopCountCard(
                        count = students.size.toString(),
                        title = "Students",
                        subtitle = students.size.toString() + " active",
                        bannerColor = Color(0xFFE0F2FE),
                        icon = "👥",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("student_list") }
                    )
                    TopCountCard(
                        count = batches.size.toString(),
                        title = "Classes",
                        subtitle = batches.size.toString() + " active",
                        bannerColor = Color(0xFFDCFCE7),
                        icon = "🏫",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("batch_list") }
                    )
                    TopCountCard(
                        count = staffList.size.toString(),
                        title = "Teacher",
                        subtitle = staffList.size.toString() + " active",
                        bannerColor = Color(0xFFFEF3C7),
                        icon = "👨‍🏫",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("staff_list") }
                    )
                }
            }

            // 3. Attendance Summary (Scrollable Timeline + Dynamic Tiles)
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.bgCard,
                    border = BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Attendance Summary", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = colors.brandPrimary)
                                Text(
                                    text = if (selectedAttendanceDay == null) "All Month Overview" else "Day: " + (if (selectedAttendanceDay!! < 10) "0" + selectedAttendanceDay else selectedAttendanceDay.toString()) + "/" + monthNumberStr + "/" + selectedYear,
                                    fontSize = 11.sp,
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "‹",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.brandPrimary,
                                    modifier = Modifier.clickable {
                                        if (selectedMonthIndex > 0) selectedMonthIndex -= 1 else { selectedMonthIndex = 11; selectedYear -= 1 }
                                        selectedAttendanceDay = null
                                    }.padding(horizontal = 6.dp)
                                )
                                Text(currentMonthLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                Text(
                                    text = "›",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.brandPrimary,
                                    modifier = Modifier.clickable {
                                        if (selectedMonthIndex < 11) selectedMonthIndex += 1 else { selectedMonthIndex = 0; selectedYear += 1 }
                                        selectedAttendanceDay = null
                                    }.padding(horizontal = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                Surface(
                                    onClick = { selectedAttendanceDay = null },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedAttendanceDay == null) colors.brandPrimary else colors.bgCardHover,
                                    border = BorderStroke(1.dp, if (selectedAttendanceDay == null) colors.brandPrimary else colors.borderCard),
                                    modifier = Modifier.height(46.dp)
                                ) {
                                    Box(modifier = Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            "All Month",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedAttendanceDay == null) Color.White else colors.textPrimary
                                        )
                                    }
                                }
                            }

                            items((1..daysInMonth).toList()) { day ->
                                val isSelected = (selectedAttendanceDay == day)
                                val dStr = if (day < 10) "0" + day else day.toString()
                                val dayDateStr = dStr + "/" + monthNumberStr + "/" + selectedYear
                                val altDateStr = day.toString() + "/" + monthNumberStr + "/" + selectedYear
                                val hasRecords = studentMonthAttendance.any { it.date == dayDateStr || it.date == altDateStr }

                                Surface(
                                    onClick = { selectedAttendanceDay = day },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) colors.brandPrimary else colors.bgCardHover,
                                    border = BorderStroke(1.dp, if (isSelected) colors.brandPrimary else colors.borderCard),
                                    modifier = Modifier.width(46.dp).height(46.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            dStr,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = if (isSelected) Color.White else colors.textPrimary
                                        )
                                        if (hasRecords) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 2.dp)
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) Color.White else colors.brandPrimary)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AttendanceCountTile(
                                label = "Present",
                                count = presentCount.toString(),
                                isSelected = selectedStatusFilter == "PRESENT",
                                activeColor = Color(0xFF22C55E),
                                modifier = Modifier.weight(1f),
                                onClick = { selectedStatusFilter = if (selectedStatusFilter == "PRESENT") "ALL" else "PRESENT" }
                            )
                            AttendanceCountTile(
                                label = "Absent",
                                count = absentCount.toString(),
                                isSelected = selectedStatusFilter == "ABSENT",
                                activeColor = Color(0xFFEF4444),
                                modifier = Modifier.weight(1f),
                                onClick = { selectedStatusFilter = if (selectedStatusFilter == "ABSENT") "ALL" else "ABSENT" }
                            )
                            AttendanceCountTile(
                                label = "Leave",
                                count = leaveCount.toString(),
                                isSelected = selectedStatusFilter == "LEAVE",
                                activeColor = Color(0xFFF59E0B),
                                modifier = Modifier.weight(1f),
                                onClick = { selectedStatusFilter = if (selectedStatusFilter == "LEAVE") "ALL" else "LEAVE" }
                            )
                            AttendanceCountTile(
clear
