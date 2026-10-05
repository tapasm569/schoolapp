package com.school.manage.presentation.screens
import com.school.manage.core.firebase.FirestoreSyncService
import kotlinx.coroutines.launch

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
    LaunchedEffect(schoolCode) {
        FirestoreSyncService(database).startSync(schoolCode)
    }

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

    // Navigation Drawer / Hamburger Menu State
    var showHamburgerMenu by remember { mutableStateOf(false) }
    var showSchoolProfileSheet by remember { mutableStateOf(false) }
    var showBatchSessionDialog by remember { mutableStateOf(false) }
    var showResetPasswordDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    var currentBatchSession by remember { mutableStateOf("2026 - 2027") }
    var newAdminPassword by remember { mutableStateOf("") }
    var passwordChangeSuccessMsg by remember { mutableStateOf("") }

    var schoolRecord by remember { mutableStateOf<com.school.manage.core.database.entity.SchoolEntity?>(null) }
    LaunchedEffect(schoolCode) {
        schoolRecord = database.schoolDao().getSchoolByCode(schoolCode)
    }
    val scope = rememberCoroutineScope()

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

            // 1. Modern Header: School Profile & Hamburger Menu
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // School Name Card - Clicking opens School Profile
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showSchoolProfileSheet = true }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.brandPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏫", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = schoolName.ifEmpty { "SCHOOL PORTAL" }.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ⓘ", fontSize = 12.sp, color = colors.brandPrimary, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "Code: $schoolCode • Tap for profile",
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    // Hamburger Menu Button (☰)
                    Surface(
                        onClick = { showHamburgerMenu = true },
                        shape = RoundedCornerShape(12.dp),
                        color = colors.bgCard,
                        border = BorderStroke(1.dp, colors.borderCard),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("☰", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
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
                                label = "Holiday",
                                count = holidayCount.toString(),
                                isSelected = selectedStatusFilter == "HOLIDAY",
                                activeColor = Color(0xFF3B82F6),
                                modifier = Modifier.weight(1f),
                                onClick = { selectedStatusFilter = if (selectedStatusFilter == "HOLIDAY") "ALL" else "HOLIDAY" }
                            )
                        }

                        if (selectedStatusFilter != "ALL" && activeAttendanceRecords.isNotEmpty()) {
                            val filteredList = activeAttendanceRecords.filter { it.status == selectedStatusFilter }
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.bgCardHover,
                                border = BorderStroke(1.dp, colors.borderCard),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = selectedStatusFilter + " Students (" + filteredList.size + ")",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    if (filteredList.isEmpty()) {
                                        Text("No students marked " + selectedStatusFilter.lowercase() + " on this date.", fontSize = 11.sp, color = colors.textSecondary)
                                    } else {
                                        filteredList.forEach { att ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = att.studentName.ifEmpty { "Student #" + att.studentId },
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = colors.textPrimary
                                                )
                                                Text(
                                                    text = att.gradeClass.ifEmpty { "General" },
                                                    fontSize = 11.sp,
                                                    color = colors.textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Marking Attendance Progress Bars
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AttendanceProgressBarCard(
                        title = "Student",
                        subtitle = "Marking Attendance",
                        pct = studentPct.toString() + "%",
                        ratio = presentCount.toString() + "/" + studentTotalMarked.toString(),
                        progress = studentPct / 100f,
                        modifier = Modifier.weight(1f)
                    )
                    AttendanceProgressBarCard(
                        title = "Teacher",
                        subtitle = "Marking Attendance",
                        pct = teacherPct.toString() + "%",
                        ratio = teacherPresent.toString() + "/" + teacherTotalMarked.toString(),
                        progress = teacherPct / 100f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. Monthly Financial Summary
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
                            Text("Monthly Summary", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = colors.brandPrimary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "‹",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.brandPrimary,
                                    modifier = Modifier.clickable {
                                        if (selectedMonthIndex > 0) selectedMonthIndex -= 1 else { selectedMonthIndex = 11; selectedYear -= 1 }
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
                                    }.padding(horizontal = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val monthNetPrefix = if (netMonthCashFlow > 0) "+₹" else if (netMonthCashFlow < 0) "-₹" else "₹"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (netMonthCashFlow > 0) colors.success.copy(alpha = 0.12f) else if (netMonthCashFlow < 0) colors.error.copy(alpha = 0.12f) else colors.borderCard.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, if (netMonthCashFlow > 0) colors.success.copy(alpha = 0.4f) else if (netMonthCashFlow < 0) colors.error.copy(alpha = 0.4f) else colors.borderCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("MONTH NET BALANCE", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = colors.textSecondary, letterSpacing = 0.5.sp)
                                    Text(
                                        text = monthNetPrefix + abs(netMonthCashFlow).toInt(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = if (netMonthCashFlow > 0) colors.success else if (netMonthCashFlow < 0) colors.error else colors.textPrimary
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (netMonthCashFlow > 0) colors.success else if (netMonthCashFlow < 0) colors.error else colors.brandPrimary
                                ) {
                                    Text(
                                        text = if (netMonthCashFlow > 0) "Surplus" else if (netMonthCashFlow < 0) "Deficit" else "Balanced",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("DAILY TRANSACTIONS (SCROLLABLE)", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = colors.textSecondary, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items((1..daysInMonth).toList()) { day ->
                                val dStr = if (day < 10) "0" + day else day.toString()
                                val dayDateStr = dStr + "/" + monthNumberStr + "/" + selectedYear
                                val altDateStr = day.toString() + "/" + monthNumberStr + "/" + selectedYear

                                val dayIncome = feeRecords.filter { 
                                    it.paymentDate == dayDateStr || it.paymentDate == altDateStr || (it.feeMonth == (monthNumberStr + "/" + selectedYear) && it.paymentDate.startsWith(dStr + "/"))
                                }.sumOf { it.amountPaid }

                                val dayExpense = expenses.filter { 
                                    it.date == dayDateStr || it.date == altDateStr 
                                }.sumOf { it.amount }

                                val dayNet = dayIncome - dayExpense
                                val netPrefix = if (dayNet > 0) "+₹" else if (dayNet < 0) "-₹" else "₹"

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = colors.bgCardHover,
                                    border = BorderStroke(1.dp, colors.borderCard),
                                    modifier = Modifier.width(130.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            dStr + " " + monthsList[selectedMonthIndex],
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = colors.textPrimary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("In:", fontSize = 10.sp, color = colors.textSecondary)
                                            Text("+₹" + dayIncome.toInt(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.success)
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Out:", fontSize = 10.sp, color = colors.textSecondary)
                                            Text("-₹" + dayExpense.toInt(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.error)
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        HorizontalDivider(color = colors.borderCard.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Net:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                            Text(
                                                text = netPrefix + abs(dayNet).toInt(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (dayNet > 0) colors.success else if (dayNet < 0) colors.error else colors.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = colors.borderCard.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Monthly Incomes", fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("Monthly Expenses", fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("All-Time Incomes", fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("₹" + monthlyFees.toInt(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.success, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("₹" + monthlyExpenses.toInt(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.error, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("₹" + totalFees.toInt(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.brandPrimary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            // 6. FEATURES Grid
            item {
                Text("FEATURES", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = colors.textSecondary, letterSpacing = 1.sp)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        FeatureIconItem("Exams", "⏱️", Color(0xFFFFF7ED), onClick = { onNavigate("exams") })
                        FeatureIconItem("Birthdays", "🎂", Color(0xFFFEF3C7), onClick = { onNavigate("birthdays") })
                        FeatureIconItem("Home works", "📋", Color(0xFFECFDF5), onClick = { onNavigate("homework") })
                        FeatureIconItem("Class works", "📊", Color(0xFFF0FDF4), onClick = { onNavigate("classwork") })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        FeatureIconItem("Enquiry", "👤", Color(0xFFFFF7ED), onClick = { onNavigate("enquiry") })
                        FeatureIconItem("Staff Logs", "🔢", Color(0xFFE0F2FE), onClick = { onNavigate("staff_logs") })
                        FeatureIconItem("Announcements", "🔔", Color(0xFFFFF7ED), onClick = { onNavigate("announcements") })
                        FeatureIconItem("Messages", "💬", Color(0xFFECFDF5), onClick = { onNavigate("messages") })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        FeatureIconItem("Leave\nManagement", "➖", Color(0xFFF0FDF4), onClick = { onNavigate("leave_management") })
                        FeatureIconItem("Timetable", "📅", Color(0xFFECFDF5), onClick = { onNavigate("timetable") })
                        FeatureIconItem("Online\nClasses", "📖", Color(0xFFF0FDF4), onClick = { onNavigate("online_classes") })
                        FeatureIconItem("Question\nBank", "📑", Color(0xFFFEF3C7), onClick = { onNavigate("question_bank") })
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(70.dp)) }
        }

        // 7. Modal Bottom Sheet
        if (showAddNewBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddNewBottomSheet = false },
                containerColor = colors.bgCard,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .size(width = 44.dp, height = 5.dp)
                            .clip(CircleShape)
                            .background(colors.borderCard)
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 30.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Add New", fontWeight = FontWeight.Black, fontSize = 20.sp, color = colors.textPrimary)
                        Text(
                            text = "✕",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                            modifier = Modifier.clickable { showAddNewBottomSheet = false }.padding(6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = colors.borderCard.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(6.dp))

                    AddNewOptionItem(
                        title = "Student",
                        subtitle = "You can add new student here",
                        icon = "👨‍🎓",
                        onClick = {
                            showAddNewBottomSheet = false
                            onNavigate("add_student")
                        }
                    )
                    AddNewOptionItem(
                        title = "Teacher",
                        subtitle = "You can add new staff here",
                        icon = "👨‍🏫",
                        onClick = {
                            showAddNewBottomSheet = false
                            onNavigate("add_staff")
                        }
                    )
                    AddNewOptionItem(
                        title = "Class",
                        subtitle = "You can add new school class here",
                        icon = "👥",
                        onClick = {
                            showAddNewBottomSheet = false
                            onNavigate("add_batch")
                        }
                    )
                    AddNewOptionItem(
                        title = "Exams",
                        subtitle = "Schedule exams and input marks",
                        icon = "📝",
                        onClick = {
                            showAddNewBottomSheet = false
                            onNavigate("exams")
                        }
                    )
                    AddNewOptionItem(
                        title = "Homework",
                        subtitle = "Assign tasks to classes",
                        icon = "📋",
                        onClick = {
                            showAddNewBottomSheet = false
                            onNavigate("homework")
                        }
                    )
                    AddNewOptionItem(
                        title = "Expense",
                        subtitle = "You can add expense here",
                        icon = "🧾",
                        onClick = {
                            showAddNewBottomSheet = false
                            onNavigate("expense_list")
                        }
                    )
                    AddNewOptionItem(
                        title = "Collect Fee",
                        subtitle = "Record a fee payment from a student",
                        icon = "💰",
                        onClick = {
                            showAddNewBottomSheet = false
                            onNavigate("fee_collection")
                        }
                    )
                }
            }
        }

// ================= 1. HAMBURGER SLIDE MENU =================
        if (showHamburgerMenu) {
            ModalBottomSheet(
                onDismissRequest = { showHamburgerMenu = false },
                containerColor = colors.bgCard,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.brandPrimary.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(colors.brandPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🎓", fontSize = 22.sp)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Welcome to",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = schoolName.ifEmpty { "School Portal" },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Option 1: Set Batch Season
                    HamburgerOptionItem(
                        icon = "📅",
                        title = "Set Batch Session",
                        subtitle = "Current: $currentBatchSession",
                        onClick = {
                            showHamburgerMenu = false
                            showBatchSessionDialog = true
                        }
                    )

                    // Option 2: Reset Password
                    HamburgerOptionItem(
                        icon = "🔑",
                        title = "Reset Password",
                        subtitle = "Update Admin security credentials",
                        onClick = {
                            showHamburgerMenu = false
                            showResetPasswordDialog = true
                        }
                    )

                    // Option 3: Theme Toggle (Dark / Light Mode)
                    HamburgerOptionItem(
                        icon = if (colors.isDark) "☀️" else "🌙",
                        title = if (colors.isDark) "Switch to Light Mode" else "Switch to Dark Mode",
                        subtitle = if (colors.isDark) "Currently Dark theme active" else "Currently Light theme active",
                        onClick = {
                            toggleTheme()
                        }
                    )

                    // Option 4: Help & Support
                    HamburgerOptionItem(
                        icon = "💬",
                        title = "Help & Support",
                        subtitle = "Guides, FAQs, and assistance",
                        onClick = {
                            showHamburgerMenu = false
                            showHelpDialog = true
                        }
                    )

                    HorizontalDivider(color = colors.borderCard, modifier = Modifier.padding(vertical = 4.dp))

                    // Option 5: Log Out
                    Surface(
                        onClick = {
                            showHamburgerMenu = false
                            onLogout()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFEE2E2).copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🚪", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Log Out",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // ================= 2. SCHOOL PROFILE BOTTOM SHEET =================
        if (showSchoolProfileSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSchoolProfileSheet = false },
                containerColor = colors.bgCard,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.brandPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏫", fontSize = 28.sp)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = schoolName.ifEmpty { "School Portal" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Institute Code: $schoolCode",
                                fontSize = 12.sp,
                                color = colors.brandPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    HorizontalDivider(color = colors.borderCard)

                    // Profile Details Cards
                    ProfileDetailRow(label = "School Name", value = schoolName.ifEmpty { "N/A" }, icon = "🏛️")
                    ProfileDetailRow(label = "Staff Count", value = "${staffList.size} Active Members", icon = "👨‍🏫")
                    ProfileDetailRow(label = "Students Count", value = "${students.size} Enrolled", icon = "🎒")
                    ProfileDetailRow(label = "Batch Season", value = currentBatchSession, icon = "📅")
                    ProfileDetailRow(
                        label = "Contact Number",
                        value = schoolRecord?.phone?.ifEmpty { "Not registered" } ?: "Not registered",
                        icon = "📞",
                        colors = colors
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { showSchoolProfileSheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close Profile", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }

        // ================= 3. SET BATCH SESSION DIALOG =================
        if (showBatchSessionDialog) {
            var tempSession by remember { mutableStateOf(currentBatchSession) }
            AlertDialog(
                onDismissRequest = { showBatchSessionDialog = false },
                title = { Text("Set Academic Batch Season", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Define the active academic year for student attendance, fees, and grading.", fontSize = 13.sp, color = colors.textSecondary)
                        OutlinedTextField(
                            value = tempSession,
                            onValueChange = { tempSession = it },
                            label = { Text("Academic Session (e.g. 2026 - 2027)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (tempSession.isNotBlank()) {
                            currentBatchSession = tempSession.trim()
                        }
                        showBatchSessionDialog = false
                    }) {
                        Text("Save Session")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBatchSessionDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // ================= 4. RESET PASSWORD DIALOG =================
        if (showResetPasswordDialog) {
            AlertDialog(
                onDismissRequest = {
                    showResetPasswordDialog = false
                    newAdminPassword = ""
                    passwordChangeSuccessMsg = ""
                },
                title = { Text("Reset Admin Password", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Enter a new administrator password for School Code $schoolCode:", fontSize = 13.sp, color = colors.textSecondary)
                        OutlinedTextField(
                            value = newAdminPassword,
                            onValueChange = { newAdminPassword = it },
                            label = { Text("New Password") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (passwordChangeSuccessMsg.isNotEmpty()) {
                            Text(passwordChangeSuccessMsg, color = Color(0xFF16A34A), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (newAdminPassword.length >= 4 && schoolRecord != null) {
                            scope.launch {
                                val updated = schoolRecord!!.copy(password = newAdminPassword.trim())
                                database.schoolDao().insertSchool(updated)
                                passwordChangeSuccessMsg = "✓ Password updated successfully!"
                            }
                        }
                    }) {
                        Text("Update Password")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showResetPasswordDialog = false
                        newAdminPassword = ""
                        passwordChangeSuccessMsg = ""
                    }) {
                        Text("Close")
                    }
                }
            )
        }

        // ================= 5. HELP & SUPPORT DIALOG =================
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                title = { Text("Help & Support", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("• Manage Students & Staff via bottom navigation.", fontSize = 13.sp)
                        Text("• Tapping school name reveals institutional statistics and directory counts.", fontSize = 13.sp)
                        Text("• All changes synchronize directly to local offline cache and cloud database.", fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("For technical support contact: support@schoolapp.io", fontSize = 12.sp, color = colors.brandPrimary, fontWeight = FontWeight.SemiBold)
                    }
                },
                confirmButton = {
                    Button(onClick = { showHelpDialog = false }) {
                        Text("Got it")
                    }
                }
            )
        }
    }
}

@Composable
fun AttendanceCountTile(
    label: String,
    count: String,
    isSelected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalSchoolColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) activeColor.copy(alpha = 0.15f) else colors.bgCardHover,
        border = BorderStroke(1.5.dp, if (isSelected) activeColor else colors.borderCard),
        modifier = modifier.height(64.dp)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(count, fontWeight = FontWeight.Black, fontSize = 16.sp, color = if (isSelected) activeColor else colors.textPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) activeColor else colors.textSecondary)
        }
    }
}

@Composable
fun HamburgerOptionItem(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = LocalSchoolColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = colors.bgApp,
        border = BorderStroke(1.dp, colors.borderCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.brandPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }
            Text("›", fontSize = 20.sp, color = colors.textSecondary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfileDetailRow(
    label: String,
    value: String,
    icon: String
) {
    val colors = LocalSchoolColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.bgApp),
        border = BorderStroke(1.dp, colors.borderCard)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.brandPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, fontSize = 11.sp, color = colors.textSecondary)
                Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            }
        }
    }
}
