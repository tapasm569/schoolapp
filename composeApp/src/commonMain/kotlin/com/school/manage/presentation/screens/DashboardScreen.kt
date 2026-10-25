package com.school.manage.presentation.screens
import com.school.manage.presentation.screens.SchoolSetupWizardScreen
import dev.gitlive.firebase.database.database
import dev.gitlive.firebase.Firebase

import androidx.compose.ui.window.Dialog

import androidx.compose.runtime.rememberCoroutineScope

import com.school.manage.core.utils.getSystemCurrentDate
import com.school.manage.core.utils.rememberPhotoPickerLauncher
import com.school.manage.core.utils.rememberCsvFilePickerLauncher
import com.school.manage.core.utils.ProfileAvatar
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.school.manage.core.utils.SchoolBackupManager


import com.school.manage.presentation.components.StylishAttendanceSummaryCard
import com.school.manage.core.firebase.FirestoreSyncService
import kotlinx.coroutines.launch

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.school.manage.core.database.entity.BatchEntity
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
    // ================= MANDATORY CHRONOLOGICAL SETUP GATEKEEPER =================
    var isSchoolSetupDone by remember { mutableStateOf<Boolean?>(null) }
    val activeSchoolCode = schoolCode

    LaunchedEffect(activeSchoolCode) {
        try {
            val code = activeSchoolCode.ifBlank { "12345" }
            val root = Firebase.database.reference("schools").child(code)
            val infoSnap = root.child("info").get()
            val isDone = infoSnap.child("setupCompleted").value as? Boolean ?: false
            val batchesSnap = root.child("batches").get()
            val hasBatches = batchesSnap.children.toList().isNotEmpty()
            
            isSchoolSetupDone = isDone && hasBatches
        } catch (e: Exception) {
            isSchoolSetupDone = true
        }
    }

    if (isSchoolSetupDone == false) {
        SchoolSetupWizardScreen(
            schoolCode = activeSchoolCode.ifBlank { "12345" },
            onSetupCompleted = {
                isSchoolSetupDone = true
            }
        )
        return
    }
    // ============================================================================

    // Financial Dues & Payroll Calculations for Dashboard Cards
    val allStudentsList by database.studentDao().getStudentsBySchool(schoolCode).collectAsState(initial = emptyList())
    val allSchoolFeeRecords by database.feeDao().getFeeRecordsBySchool(schoolCode).collectAsState(initial = emptyList())
    val allStaffList by database.staffDao().getStaffBySchool(schoolCode).collectAsState(initial = emptyList())
    val allExpensesList by database.expenseDao().getExpensesBySchool(schoolCode).collectAsState(initial = emptyList())
    val dashSysDate = remember { getSystemCurrentDate() }
    val dashCurrentMonthKey = "${(dashSysDate.monthIndex + 1).toString().padStart(2, '0')}/${dashSysDate.year}"

    val (totalStudentDues, dueStudentsCount) = remember(allStudentsList, allSchoolFeeRecords, dashSysDate) {
        var duesSum = 0.0
        var count = 0
        allStudentsList.forEach { student ->
            val paid = allSchoolFeeRecords.filter { it.studentId == student.id }.sumOf { it.amountPaid }
            val parts = student.admissionDate.split("/")
            val admMonth = parts.getOrNull(1)?.toIntOrNull()?.minus(1) ?: dashSysDate.monthIndex
            val admYear = parts.getOrNull(2)?.toIntOrNull() ?: dashSysDate.year
            val elapsedMonths = maxOf(1, (dashSysDate.year - admYear) * 12 + (dashSysDate.monthIndex - admMonth) + 1)
            val expected = (student.monthlyFee * elapsedMonths) + student.admissionFee
            val due = maxOf(0.0, expected - paid)
            if (due > 0.0) {
                duesSum += due
                count++
            }
        }
        Pair(duesSum, count)
    }

    val (totalUnpaidSalaries, unpaidTeachersCount) = remember(allStaffList, allExpensesList, dashCurrentMonthKey) {
        var unpaidSum = 0.0
        var count = 0
        allStaffList.forEach { staff ->
            val isPaid = allExpensesList.any { exp ->
                exp.category.equals("Salary", ignoreCase = true) &&
                exp.title.contains(staff.name, ignoreCase = true) &&
                exp.date.contains(dashCurrentMonthKey)
            }
            if (!isPaid) {
                unpaidSum += staff.salary
                count++
            }
        }
        Pair(unpaidSum, count)
    }

    LaunchedEffect(schoolCode) {
        FirestoreSyncService(database).startSync(schoolCode)
    }

    val colors = LocalSchoolColors.current
    var fcmTokenText by remember { mutableStateOf<String?>(null) }
    var showTokenDialog by remember { mutableStateOf(false) }
    val toggleTheme = LocalThemeToggle.current

    val students by database.studentDao().getStudentsBySchool(schoolCode).collectAsState(initial = emptyList())
    val feeRecords by database.feeDao().getFeeRecordsBySchool(schoolCode).collectAsState(initial = emptyList())
    val expenses by database.expenseDao().getExpensesBySchool(schoolCode).collectAsState(initial = emptyList())
    val staffList by database.staffDao().getStaffBySchool(schoolCode).collectAsState(initial = emptyList())
    val batches by database.batchDao().getBatchesBySchool(schoolCode).collectAsState(initial = emptyList())
    val attendanceList by database.attendanceDao().getAttendanceBySchool(schoolCode).collectAsState(initial = emptyList())

    val monthsList = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val today = remember { getSystemCurrentDate() }
    var selectedMonthIndex by remember { mutableStateOf(today.monthIndex) }
    var selectedYear by remember { mutableStateOf(today.year) }

    var selectedAttendanceDay by remember { mutableStateOf<Int?>(today.day) }
    var selectedAttendanceDate by remember { mutableStateOf(today.formatted) }
    var showAttendanceCalendar by remember { mutableStateOf(false) }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var selectedClassAttendanceFilter by remember { mutableStateOf("ALL") }

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

    val monthlyFees = remember(feeRecords, monthFilterPattern) {
        feeRecords.filter { it.feeMonth.contains(monthFilterPattern) || it.paymentDate.contains(monthFilterPattern) }.sumOf { it.amountPaid }
    }
    val totalFees = remember(feeRecords) { feeRecords.sumOf { it.amountPaid } }
    val monthlyExpenses = remember(expenses, monthFilterPattern) {
        expenses.filter { it.date.contains(monthFilterPattern) }.sumOf { it.amount }
    }
    val totalExpenses = remember(expenses) { expenses.sumOf { it.amount } }
    val netMonthCashFlow = remember(monthlyFees, monthlyExpenses) { monthlyFees - monthlyExpenses }

    val studentMonthAttendance = remember(attendanceList, monthFilterPattern) {
        attendanceList.filter { it.userType == "STUDENT" && it.date.contains(monthFilterPattern) }
    }
    val teacherMonthAttendance = remember(attendanceList, monthFilterPattern) {
        attendanceList.filter { it.userType == "STAFF" && it.date.contains(monthFilterPattern) }
    }

    // Precomputed daily cash-flow map for instant O(1) day-chip lookup
    val dailyCashFlowMap = remember(feeRecords, expenses, monthNumberStr, selectedYear, daysInMonth) {
        (1..daysInMonth).associateWith { day ->
            val dStr = if (day < 10) "0$day" else day.toString()
            val dayDateStr = "$dStr/$monthNumberStr/$selectedYear"
            val altDateStr = "$day/$monthNumberStr/$selectedYear"
            val income = feeRecords.filter {
                it.paymentDate == dayDateStr || it.paymentDate == altDateStr || (it.feeMonth == "$monthNumberStr/$selectedYear" && it.paymentDate.startsWith("$dStr/"))
            }.sumOf { it.amountPaid }
            val expense = expenses.filter { it.date == dayDateStr || it.date == altDateStr }.sumOf { it.amount }
            Triple(income.toDouble(), expense.toDouble(), (income - expense).toDouble())
        }
    }

    fun normalizeDateStr(raw: String): String {
        val p = raw.trim().split("/")
        return if (p.size == 3) "${p[0].padStart(2, '0')}/${p[1].padStart(2, '0')}/${p[2]}" else raw.trim()
    }

    val activeAttendanceRecords = remember(selectedAttendanceDate, attendanceList) {
        val targetNorm = normalizeDateStr(selectedAttendanceDate)
        attendanceList.filter { it.userType == "STUDENT" && normalizeDateStr(it.date) == targetNorm }
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
    var showSetAdmissionFeeDialog by remember { mutableStateOf(false) }
    var showManageSubjectsDialog by remember { mutableStateOf(false) }

    // Navigation Drawer / Hamburger Menu State
    var showHamburgerMenu by remember { mutableStateOf(false) }
    var showSchoolProfileSheet by remember { mutableStateOf(false) }
    var showBatchSessionDialog by remember { mutableStateOf(false) }
    var showResetPasswordDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val backupManager = remember { SchoolBackupManager(database) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showBulkStudentImportDialog by remember { mutableStateOf(false) }
    var bulkStudentExcelInput by remember { mutableStateOf("") }
    var bulkImportStatusMessage by remember { mutableStateOf<String?>(null) }
    val pickExcelCsvFile = rememberCsvFilePickerLauncher { fileText ->
        bulkStudentExcelInput = fileText
        bulkImportStatusMessage = "✓ Excel/CSV file loaded! Tap 'Import Students' below."
    }
    var exportBackupJson by remember { mutableStateOf("") }
    var exportSummaryText by remember { mutableStateOf("") }
    var importInputJson by remember { mutableStateOf("") }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }
    var isBackupLoading by remember { mutableStateOf(false) }


    var currentBatchSession by remember { mutableStateOf("2026 - 2027") }
    var newAdminPassword by remember { mutableStateOf("") }
    var passwordChangeSuccessMsg by remember { mutableStateOf("") }

    var schoolRecord by remember { mutableStateOf<com.school.manage.core.database.entity.SchoolEntity?>(null) }
    LaunchedEffect(schoolCode) {
        schoolRecord = database.schoolDao().getSchoolByCode(schoolCode)
    }
    val scope = rememberCoroutineScope()
    val dashboardListState = rememberLazyListState()
    var isCloudSyncing by remember { mutableStateOf(false) }
    var cloudSyncStatusMessage by remember { mutableStateOf<String?>(null) }

    val pickSchoolLogo = rememberPhotoPickerLauncher { encodedBase64 ->
        scope.launch {
            val current = schoolRecord ?: database.schoolDao().getSchoolByCode(schoolCode)
            if (current != null) {
                val updated = current.copy(photoBase64 = encodedBase64)
                database.schoolDao().insertSchool(updated)
                schoolRecord = updated
            }
        }
    }

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
            state = dashboardListState,
            modifier = Modifier
                
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            .weight(1f)
                            .padding(end = 8.dp)
                            .clickable { showSchoolProfileSheet = true }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        ProfileAvatar(
                            photoBase64 = schoolRecord?.photoBase64.orEmpty(),
                            name = schoolName,
                            fallbackEmoji = "🏫",
                            size = 44.dp,
                            cornerRadius = 12.dp,
                            backgroundColor = colors.brandPrimary.copy(alpha = 0.15f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = schoolName.ifEmpty { "SCHOOL PORTAL" }.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colors.textPrimary,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
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

            // 3. Attendance Summary (Current Date + Calendar Picker + Horizontal Class-Wise Tiles)
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.bgCard,
                    border = BorderStroke(1.dp, colors.borderCard),
                    shadowElevation = if (colors.isDark) 0.dp else 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Attendance Summary", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = colors.brandPrimary)
                                Text(
                                    text = if (normalizeDateStr(selectedAttendanceDate) == normalizeDateStr(today.formatted))
                                        "Today: ${today.formatted}"
                                    else
                                        "Selected Date: $selectedAttendanceDate",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (normalizeDateStr(selectedAttendanceDate) != normalizeDateStr(today.formatted)) {
                                    Surface(
                                        onClick = { selectedAttendanceDate = today.formatted },
                                        shape = RoundedCornerShape(10.dp),
                                        color = colors.brandPrimary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "Today",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.brandPrimary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                Surface(
                                    onClick = { showAttendanceCalendar = true },
                                    shape = RoundedCornerShape(10.dp),
                                    color = colors.bgCardHover,
                                    border = BorderStroke(1.dp, colors.brandPrimary.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("📅", fontSize = 14.sp)
                                        Text(
                                            text = selectedAttendanceDate,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.brandPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⇄ Swipe left/right for All Classes & Class-Wise Attendance",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val classNamesList = remember(batches, students) {
                            val fromBatches = batches.map { it.batchName.trim() }.filter { it.isNotEmpty() }
                            val fromStudents = students.map { it.gradeClass.trim() }.filter { it.isNotEmpty() }
                            (fromBatches + fromStudents).distinctBy { it.uppercase() }
                        }

                        // Horizontal Scrollable Tiles: First "All Classes", then each Class Tile
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Tile 0: ALL CLASSES SUMMARY
                            item {
                                val isAllSelected = selectedClassAttendanceFilter == "ALL"
                                Surface(
                                    onClick = { selectedClassAttendanceFilter = "ALL" },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isAllSelected) colors.brandPrimary.copy(alpha = 0.06f) else colors.bgCardHover,
                                    border = BorderStroke(if (isAllSelected) 1.5.dp else 1.dp, if (isAllSelected) colors.brandPrimary else colors.borderCard),
                                    modifier = Modifier.width(290.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("🏫", fontSize = 16.sp)
                                                Column {
                                                    Text("All Classes", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = colors.textPrimary)
                                                    Text("${students.size} Total Students", fontSize = 10.sp, color = colors.textSecondary)
                                                }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = colors.brandPrimary.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "$studentPct% Present",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.brandPrimary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        StylishAttendanceSummaryCard(
                                            presentCount = presentCount,
                                            absentCount = absentCount,
                                            leaveCount = leaveCount,
                                            holidayCount = holidayCount,
                                            selectedFilter = if (isAllSelected) selectedStatusFilter else "ALL",
                                            onFilterSelected = { status ->
                                                selectedClassAttendanceFilter = "ALL"
                                                selectedStatusFilter = if (selectedStatusFilter == status && isAllSelected) "ALL" else status
                                            }
                                        )
                                    }
                                }
                            }

                            // Subsequent Tiles: Individual Class-Wise Summary Tiles
                            items(classNamesList) { cName ->
                                val isClassSelected = selectedClassAttendanceFilter.equals(cName, ignoreCase = true)
                                val classStudentsCount = students.count { it.gradeClass.equals(cName, ignoreCase = true) }
                                val classRecords = activeAttendanceRecords.filter { it.gradeClass.equals(cName, ignoreCase = true) }
                                val cPresent = classRecords.count { it.status == "PRESENT" }
                                val cAbsent = classRecords.count { it.status == "ABSENT" }
                                val cLeave = classRecords.count { it.status == "LEAVE" }
                                val cHoliday = classRecords.count { it.status == "HOLIDAY" }
                                val cTotalMarked = cPresent + cAbsent + cLeave
                                val cPct = if (cTotalMarked > 0) (cPresent * 100 / cTotalMarked) else 0

                                Surface(
                                    onClick = { selectedClassAttendanceFilter = cName },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isClassSelected) colors.brandPrimary.copy(alpha = 0.06f) else colors.bgCardHover,
                                    border = BorderStroke(if (isClassSelected) 1.5.dp else 1.dp, if (isClassSelected) colors.brandPrimary else colors.borderCard),
                                    modifier = Modifier.width(280.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("📚", fontSize = 16.sp)
                                                Column {
                                                    Text(cName, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = colors.textPrimary)
                                                    Text("$classStudentsCount Enrolled", fontSize = 10.sp, color = colors.textSecondary)
                                                }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = colors.success.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "$cPct% Present",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.success,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        StylishAttendanceSummaryCard(
                                            presentCount = cPresent,
                                            absentCount = cAbsent,
                                            leaveCount = cLeave,
                                            holidayCount = cHoliday,
                                            selectedFilter = if (isClassSelected) selectedStatusFilter else "ALL",
                                            onFilterSelected = { status ->
                                                selectedClassAttendanceFilter = cName
                                                selectedStatusFilter = if (selectedStatusFilter == status && isClassSelected) "ALL" else status
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (selectedStatusFilter != "ALL" && activeAttendanceRecords.isNotEmpty()) {
                            val filteredList = activeAttendanceRecords.filter {
                                it.status == selectedStatusFilter &&
                                (selectedClassAttendanceFilter == "ALL" || it.gradeClass.equals(selectedClassAttendanceFilter, ignoreCase = true))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.bgCardHover,
                                border = BorderStroke(1.dp, colors.borderCard),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    val scopeTitle = if (selectedClassAttendanceFilter == "ALL") "All Classes" else selectedClassAttendanceFilter
                                    Text(
                                        text = "$scopeTitle • $selectedStatusFilter Students (${filteredList.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    if (filteredList.isEmpty()) {
                                        Text("No students marked ${selectedStatusFilter.lowercase()} on $selectedAttendanceDate.", fontSize = 11.sp, color = colors.textSecondary)
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

            // 4. Financial Liabilities (Student Dues & Teacher Salary)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Student Dues Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate("student_dues") }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Student Dues", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                Text("🎓", fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("₹${totalStudentDues.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF991B1B))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("$dueStudentsCount Due Students", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFFB91C1C))
                        }
                    }

                    // Teacher Unpaid Salary Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate("teacher_salaries") }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Teacher Salary", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                                Text("👩‍🏫", fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("₹${totalUnpaidSalaries.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF1E40AF))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("$unpaidTeachersCount Unpaid Teachers", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1D4ED8))
                        }
                    }
                }
            }

            // 5. Monthly Financial Summary
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.bgCard,
                    border = BorderStroke(1.dp, colors.borderCard),
                    shadowElevation = if (colors.isDark) 0.dp else 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Monthly Summary", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = colors.brandPrimary)
                                Text("Date: " + today.formatted, fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.SemiBold)
                            }
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

                                val flow = dailyCashFlowMap[day] ?: Triple(0.0, 0.0, 0.0)
                                val dayIncome = flow.first
                                val dayExpense = flow.second
                                val dayNet = flow.third
                                val netPrefix = if (dayNet > 0.0) "+₹" else if (dayNet < 0.0) "-₹" else "₹"
                                val absNet = if (dayNet < 0.0) -dayNet else dayNet

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
                                                text = netPrefix + absNet.toInt(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (dayNet > 0.0) colors.success else if (dayNet < 0.0) colors.error else colors.textSecondary
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

        if (showAttendanceCalendar) {
            ModernDatePickerDialog(
                currentDate = selectedAttendanceDate,
                onDateSelected = { picked ->
                    selectedAttendanceDate = normalizeDateStr(picked)
                },
                onDismiss = { showAttendanceCalendar = false }
            )
        }

        if (showAttendanceCalendar) {
            ModernDatePickerDialog(
                currentDate = selectedAttendanceDate,
                onDateSelected = { picked ->
                    selectedAttendanceDate = normalizeDateStr(picked)
                },
                onDismiss = { showAttendanceCalendar = false }
            )
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
                        title = "Set Admission Fee",
                        subtitle = "Configure class fee breakdown & tuition rules",
                        icon = "💳",
                        onClick = {
                            showAddNewBottomSheet = false
                            showSetAdmissionFeeDialog = true
                        }
                    )
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
                        title = "Subjects",
                        subtitle = "Add & manage all school subjects",
                        icon = "📚",
                        onClick = {
                            showAddNewBottomSheet = false
                            showManageSubjectsDialog = true
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
                        .verticalScroll(rememberScrollState()).imePadding()
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

                    // Cloud Auto-Push Sync Button
                    Surface(
                        onClick = {
                            if (!isCloudSyncing) {
                                isCloudSyncing = true
                                cloudSyncStatusMessage = "Syncing with cloud..."
                                scope.launch {
                                    try {
                                        FirestoreSyncService(database).restoreAllFromCloud(schoolCode)
                                        cloudSyncStatusMessage = "✓ Cloud data refreshed!"
                                    } catch (e: Exception) {
                                        cloudSyncStatusMessage = "Sync error: ${e.message}"
                                    } finally {
                                        isCloudSyncing = false
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = colors.brandPrimary.copy(alpha = 0.12f),
                        border = BorderStroke(1.2.dp, colors.brandPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (isCloudSyncing) "⏳" else "☁️", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isCloudSyncing) "Syncing to Firebase..." else "Sync All Data to Cloud",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = colors.brandPrimary
                                    )
                                    Text(
                                        text = cloudSyncStatusMessage ?: "Tap to upload students, staff & fees",
                                        fontSize = 11.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                            if (isCloudSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.brandPrimary
                                )
                            } else {
                                Text("➔", fontSize = 16.sp, color = colors.brandPrimary, fontWeight = FontWeight.Bold)
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

                    // Option: Export School Data (Backup)
                    HamburgerOptionItem(
                        icon = "📤",
                        title = "Export Data (Backup)",
                        subtitle = "Save students, staff & fees as JSON backup",
                        onClick = {
                            showHamburgerMenu = false
                            isBackupLoading = true
                            scope.launch {
                                val (jsonStr, summary) = backupManager.generateBackupJson(schoolCode)
                                exportBackupJson = jsonStr
                                exportSummaryText = summary
                                isBackupLoading = false
                                showExportDialog = true
                            }
                        }
                    )

                    // Option: Import School Data (Restore)
                    HamburgerOptionItem(
                        icon = "📥",
                        title = "Import Data (Restore)",
                        subtitle = "Restore database from your JSON backup",
                        onClick = {
                            showHamburgerMenu = false
                            importInputJson = ""
                            importStatusMessage = null
                            showImportDialog = true
                        }
                    )

                    // Option: Bulk Import Admitted Students (Excel / CSV)
                    HamburgerOptionItem(
                        icon = "📊",
                        title = "Import Students (Excel / CSV)",
                        subtitle = "Upload past 3 years students from Excel .csv file",
                        onClick = {
                            showHamburgerMenu = false
                            bulkStudentExcelInput = ""
                            bulkImportStatusMessage = null
                            showBulkStudentImportDialog = true
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
                        .verticalScroll(rememberScrollState()).imePadding()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileAvatar(
                            photoBase64 = schoolRecord?.photoBase64.orEmpty(),
                            name = schoolName,
                            fallbackEmoji = "🏫",
                            size = 56.dp,
                            cornerRadius = 14.dp,
                            backgroundColor = colors.brandPrimary.copy(alpha = 0.15f),
                            showEditBadge = true,
                            onClick = { pickSchoolLogo() }
                        )
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { pickSchoolLogo() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (schoolRecord?.photoBase64.isNullOrEmpty()) "📷 Upload School Logo" else "📷 Change School Logo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (!schoolRecord?.photoBase64.isNullOrEmpty()) {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        schoolRecord?.let { current ->
                                            val updated = current.copy(photoBase64 = "")
                                            database.schoolDao().insertSchool(updated)
                                            schoolRecord = updated
                                        }
                                    }
                                }
                            ) {
                                Text("Remove", fontSize = 12.sp, color = Color(0xFFDC2626))
                            }
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

        // ================= EXPORT DATA DIALOG =================
        if (showExportDialog) {
            var copiedMessage by remember { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                containerColor = colors.bgCard,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📤", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("School Data Backup", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()).imePadding(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.brandPrimary.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = exportSummaryText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.brandPrimary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(exportBackupJson))
                                copiedMessage = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (copiedMessage) "✓ Backup Copied to Clipboard!" else "📋 Copy JSON to Clipboard")
                        }

                        if (copiedMessage) {
                            Text(
                                text = "Paste this JSON into WhatsApp, Google Keep, or an Email draft to keep a permanent backup safe.",
                                fontSize = 11.sp,
                                color = colors.success,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text("Preview JSON:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.bgApp,
                            border = BorderStroke(1.dp, colors.borderCard),
                            modifier = Modifier.fillMaxWidth().height(140.dp)
                        ) {
                            Text(
                                text = if (exportBackupJson.length > 800) exportBackupJson.take(800) + "... [more]" else exportBackupJson,
                                fontSize = 10.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Done", color = colors.brandPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // ================= IMPORT DATA DIALOG =================
        if (showImportDialog) {
            var isRestoring by remember { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = { if (!isRestoring) showImportDialog = false },
                containerColor = colors.bgCard,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📥", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restore School Backup", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()).imePadding(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Paste your previously exported JSON backup below. This will restore all your classes, students, staff, fee payments, and expenses.",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Backup JSON:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            TextButton(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text.orEmpty()
                                    if (clip.isNotBlank()) importInputJson = clip
                                }
                            ) {
                                Text("📋 Paste Clipboard", fontSize = 12.sp, color = colors.brandPrimary)
                            }
                        }

                        OutlinedTextField(
                            value = importInputJson,
                            onValueChange = { importInputJson = it },
                            placeholder = { Text("Paste JSON string here...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            shape = RoundedCornerShape(10.dp)
                        )

                        val currentStatus = importStatusMessage
                        if (currentStatus != null) {
                            val isSuccess = currentStatus.startsWith("Restored successfully")
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSuccess) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = currentStatus,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSuccess) Color(0xFF15803D) else Color(0xFFB91C1C),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = importInputJson.isNotBlank() && !isRestoring,
                        onClick = {
                            isRestoring = true
                            scope.launch {
                                val result = backupManager.restoreBackupJson(schoolCode, importInputJson)
                                if (result.isSuccess) {
                                    importStatusMessage = result.getOrNull()
                                } else {
                                    importStatusMessage = "Error: " + (result.exceptionOrNull()?.message ?: "Failed to restore data")
                                }
                                isRestoring = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isRestoring) "Restoring..." else "⚡ Restore Data")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                }
            )
        }

        // ================= BULK STUDENT IMPORT (EXCEL / CSV) DIALOG =================
        if (showBulkStudentImportDialog) {
            var isImportingBulk by remember { mutableStateOf(false) }
            var settlePastMonths by remember { mutableStateOf(true) }
            val sampleCsvTemplate = "Roll No,Student Name,Class,Section,Father Name,Mother Name,Phone,Monthly Fee,Pending Due,Gender,Admission Date,DOB,Caste,Address\n1,Rahul Das,Class 5,A,Ramesh Das,Sita Das,9876543210,500,0,Male,10/01/2024,15/05/2015,General,Ranirhat\n2,Priya Roy,Class 6,A,Subir Roy,Mina Roy,9123456789,600,1200,Female,15/01/2025,20/08/2014,OBC,Mekhliganj"

            AlertDialog(
                onDismissRequest = { if (!isImportingBulk) showBulkStudentImportDialog = false },
                containerColor = colors.bgCard,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📊", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Import Admitted Students", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = colors.textPrimary)
                            Text("Bulk upload 3 years school register from Excel (.csv)", fontSize = 11.sp, color = colors.textSecondary)
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()).imePadding(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.brandPrimary.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Supported Excel Columns:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colors.brandPrimary)
                                Text(
                                    text = "Roll No | Student Name | Class | Section | Father Name | Mother Name | Phone | Monthly Fee | Pending Due | Gender | Admission Date | DOB | Caste | Address",
                                    fontSize = 11.sp,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "• Leave Roll No blank to auto-assign 1, 2, 3... per class.\n• Put any old unpaid balance in 'Pending Due'.",
                                    fontSize = 10.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { pickExcelCsvFile() },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("📂 Pick Excel (.csv)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(sampleCsvTemplate))
                                    bulkStudentExcelInput = sampleCsvTemplate
                                    bulkImportStatusMessage = "✓ 14-Column Excel Template loaded & copied!"
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("📋 Copy Template", fontSize = 12.sp)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.bgApp)
                                .clickable { settlePastMonths = !settlePastMonths }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Checkbox(
                                checked = settlePastMonths,
                                onCheckedChange = { settlePastMonths = it }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Auto-settle past months (2023–2026)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Prevents old Admission Dates from creating false unpaid dues. Only 'Pending Due' column + current month will be owed.",
                                    fontSize = 10.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        OutlinedTextField(
                            value = bulkStudentExcelInput,
                            onValueChange = { bulkStudentExcelInput = it },
                            label = { Text("Excel CSV Content (or Paste directly from Excel)", fontSize = 11.sp) },
                            placeholder = { Text("Roll No,Student Name,Class,Section,Father Name...", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().height(140.dp),
                            shape = RoundedCornerShape(10.dp)
                        )

                        val status = bulkImportStatusMessage
                        if (status != null) {
                            val isError = status.startsWith("Error:")
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isError) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = status,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isError) Color(0xFFB91C1C) else Color(0xFF15803D),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = bulkStudentExcelInput.isNotBlank() && !isImportingBulk,
                        onClick = {
                            isImportingBulk = true
                            scope.launch {
                                val res = backupManager.importStudentsFromExcelCsv(
                                    schoolCode = schoolCode,
                                    rawText = bulkStudentExcelInput,
                                    settlePastMonthsAutomatically = settlePastMonths
                                )
                                if (res.isSuccess) {
                                    bulkImportStatusMessage = res.getOrNull()
                                } else {
                                    bulkImportStatusMessage = "Error: " + (res.exceptionOrNull()?.message ?: "Could not parse Excel file")
                                }
                                isImportingBulk = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isImportingBulk) "Importing..." else "⚡ Import Students")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBulkStudentImportDialog = false }) {
                        Text("Close", color = colors.textSecondary)
                    }
                }
            )
        }


    }

    // Manage School Master Subjects Dialog
            // ================= SET ADMISSION FEE DIALOG =================
        if (showSetAdmissionFeeDialog) {
            val dialogScope = rememberCoroutineScope()
            // Keep the chosen class selected when the Room Flow emits the saved batch.
            // remember(batches) reset this value after every save and could jump back to
            // the first class, making the entered fee appear to have been lost.
            var selectedBatchName by remember {
                mutableStateOf(batches.firstOrNull()?.batchName ?: "")
            }
            LaunchedEffect(batches) {
                if (batches.isNotEmpty() &&
                    (selectedBatchName.isBlank() || batches.none {
                        it.batchName.equals(selectedBatchName, ignoreCase = true)
                    })
                ) {
                    selectedBatchName = batches.first().batchName
                }
            }
            val currentBatch: BatchEntity? = batches.find {
                it.batchName.equals(selectedBatchName, ignoreCase = true)
            } ?: batches.firstOrNull()

            var tuitionInput by remember(currentBatch) {
                val fee = currentBatch?.defaultMonthlyTuition ?: 0.0
                mutableStateOf(if (fee > 0.0) fee.toInt().toString() else "")
            }
            var feeParts by remember(currentBatch) {
                mutableStateOf(parseFeeTemplate(currentBatch?.feeTemplateRaw ?: ""))
            }
            var collectTuitionNow by remember(currentBatch) {
                mutableStateOf(currentBatch?.collectTuitionAtAdmission ?: true)
            }
            var newPartName by remember { mutableStateOf("") }
            var newPartAmount by remember { mutableStateOf("") }
            var isSavingFeeRules by remember { mutableStateOf(false) }
            var feeSaveMessage by remember { mutableStateOf<String?>(null) }

            Dialog(onDismissRequest = { if (!isSavingFeeRules) showSetAdmissionFeeDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = colors.bgCard,
                    border = BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.88f)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Surface(
                            color = colors.brandPrimary.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Set Admission Fee", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = colors.brandPrimary)
                                    Text("Rules prefill automatically when assigning students", fontSize = 11.sp, color = colors.textSecondary)
                                }
                                TextButton(onClick = { if (!isSavingFeeRules) showSetAdmissionFeeDialog = false }) {
                                    Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()).imePadding()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("1. Select Class to Configure:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.textPrimary)

                            if (batches.isEmpty()) {
                                Text("No classes added yet. Please add a class first.", fontSize = 12.sp, color = colors.error)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    batches.chunked(3).forEach { rowBatches ->
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            rowBatches.forEach { b ->
                                                val isSelected = b.batchName.equals(selectedBatchName, ignoreCase = true)
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (isSelected) colors.brandPrimary else colors.bgApp,
                                                    border = BorderStroke(1.dp, if (isSelected) colors.brandPrimary else colors.borderCard),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable {
                                                            selectedBatchName = b.batchName
                                                            feeSaveMessage = null
                                                        }
                                                ) {
                                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)) {
                                                        Text(
                                                            text = b.batchName,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = if (isSelected) Color.White else colors.textPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text("2. Default Monthly Tuition (₹):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.textPrimary)
                            OutlinedTextField(
                                value = tuitionInput,
                                onValueChange = { tuitionInput = it },
                                label = { Text("Monthly Tuition (e.g. 400)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Collect 1st month tuition at admission?", fontSize = 13.sp, color = colors.textPrimary)
                                Checkbox(
                                    checked = collectTuitionNow,
                                    onCheckedChange = { collectTuitionNow = it },
                                    colors = CheckboxDefaults.colors(checkedColor = colors.brandPrimary)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text("3. Admission Fee Breakdown Items:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.textPrimary)

                            feeParts.forEachIndexed { index, part ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = colors.bgApp,
                                    border = BorderStroke(1.dp, colors.borderCard),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Checkbox(
                                                checked = part.isSelected,
                                                onCheckedChange = { isChecked ->
                                                    feeParts = feeParts.toMutableList().also {
                                                        it[index] = it[index].copy(isSelected = isChecked)
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = colors.brandPrimary)
                                            )
                                            Text(part.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.textPrimary)
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = if (part.amount > 0) part.amount.toInt().toString() else "",
                                                onValueChange = { newVal ->
                                                    val parsed = newVal.toDoubleOrNull() ?: 0.0
                                                    feeParts = feeParts.toMutableList().also {
                                                        it[index] = it[index].copy(amount = parsed)
                                                    }
                                                },
                                                placeholder = { Text("₹") },
                                                singleLine = true,
                                                modifier = Modifier.width(90.dp).height(50.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            IconButton(
                                                onClick = {
                                                    feeParts = feeParts.toMutableList().also { it.removeAt(index) }
                                                }
                                            ) {
                                                Text("✕", color = colors.error, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = newPartName,
                                    onValueChange = { newPartName = it },
                                    label = { Text("New Fee Head") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.4f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                OutlinedTextField(
                                    value = newPartAmount,
                                    onValueChange = { newPartAmount = it },
                                    label = { Text("₹") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.8f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Button(
                                    onClick = {
                                        val amount = newPartAmount.trim().toDoubleOrNull()
                                        if (newPartName.isBlank() || amount == null || amount < 0.0) {
                                            feeSaveMessage = "Error: Enter a fee name and a valid non-negative amount."
                                        } else {
                                            feeParts = feeParts + AdmissionFeePart(
                                                name = newPartName.trim(),
                                                amount = amount,
                                                isSelected = true
                                            )
                                            newPartName = ""
                                            newPartAmount = ""
                                            feeSaveMessage = null
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("+")
                                }
                            }
                        }

                        Surface(
                            color = colors.bgCard,
                            border = BorderStroke(1.dp, colors.borderCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                feeSaveMessage?.let { message ->
                                    val isError = message.startsWith("Error:")
                                    Text(
                                        text = message,
                                        modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 10.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isError) colors.error else Color(0xFF15803D)
                                    )
                                }
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        enabled = !isSavingFeeRules,
                                        onClick = { showSetAdmissionFeeDialog = false },
                                        modifier = Modifier.weight(1f).height(46.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Cancel")
                                    }
                                    Button(
                                        enabled = currentBatch != null && !isSavingFeeRules,
                                        onClick = {
                                            val tuitionAmount = if (tuitionInput.isBlank()) {
                                                0.0
                                            } else {
                                                tuitionInput.trim().toDoubleOrNull()
                                            }
                                            if (currentBatch == null) {
                                                feeSaveMessage = "Error: Add a class before setting its admission fee."
                                            } else if (tuitionAmount == null || tuitionAmount < 0.0) {
                                                feeSaveMessage = "Error: Enter a valid non-negative monthly tuition amount."
                                            } else {
                                                isSavingFeeRules = true
                                                feeSaveMessage = null
                                                dialogScope.launch {
                                                    try {
                                                        val updatedBatch = currentBatch.copy(
                                                            defaultMonthlyTuition = tuitionAmount,
                                                            feeTemplateRaw = encodeFeeTemplate(feeParts),
                                                            collectTuitionAtAdmission = collectTuitionNow
                                                        )
                                                        val savedId = database.batchDao().insertBatch(updatedBatch)
                                                        if (savedId > 0L) {
                                                            feeSaveMessage = "Saved admission fee rules for ${updatedBatch.batchName}."
                                                            // The dashboard's Room Flow sync uploads the changed class.
                                                            // Keep this dialog open so the selected class and saved values remain visible.
                                                        } else {
                                                            feeSaveMessage = "Error: The fee settings were not saved. Please try again."
                                                        }
                                                    } catch (e: Exception) {
                                                        feeSaveMessage = "Error: Could not save fee rules. ${e.message ?: "Please try again."}"
                                                    } finally {
                                                        isSavingFeeRules = false
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1.6f).height(46.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary)
                                    ) {
                                        Text(if (isSavingFeeRules) "Saving..." else "💾 Save Fee Rules", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showManageSubjectsDialog) {
        var schoolData by remember { mutableStateOf<com.school.manage.core.database.entity.SchoolEntity?>(null) }
        var subjectsStr by remember { mutableStateOf("") }
        var newSubInput by remember { mutableStateOf("") }

        LaunchedEffect(schoolCode) {
            schoolData = database.schoolDao().getSchoolByCode(schoolCode)
            subjectsStr = schoolData?.masterSubjects ?: "Bengali, English, Mathematics, Science, Geography, History, Computer, Drawing, General Knowledge, Physical Education"
        }

        Dialog(onDismissRequest = { showManageSubjectsDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.bgCard,
                border = BorderStroke(1.dp, colors.borderCard),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Manage School Subjects", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)
                    Text("Add or remove subjects available across your school.", fontSize = 12.sp, color = colors.textSecondary)

                    val currentList = remember(subjectsStr) {
                        subjectsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableStateList()
                    }

                    // Existing Subjects Chips
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        currentList.forEach { sub ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.bgCardHover,
                                border = BorderStroke(1.dp, colors.borderCard)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(sub, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "✕",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.error,
                                        modifier = Modifier.clickable {
                                            currentList.remove(sub)
                                            subjectsStr = currentList.joinToString(", ")
                                        }
                                    )
                                }
                            }
                        }
                    }

                    CustomRoundedInput(
                        value = newSubInput,
                        onValueChange = { newSubInput = it },
                        placeholder = "New Subject Name (e.g. Sanskrit)"
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Button(
                            onClick = {
                                if (newSubInput.isNotBlank() && !currentList.contains(newSubInput.trim())) {
                                    currentList.add(newSubInput.trim())
                                    subjectsStr = currentList.joinToString(", ")
                                    newSubInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary)
                        ) {
                            Text("+ Add Subject")
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    val currentSchool = database.schoolDao().getSchoolByCode(schoolCode)
                                    if (currentSchool != null) {
                                        val updated = currentSchool.copy(masterSubjects = subjectsStr)
                                        database.schoolDao().insertSchool(updated)
                                    }
                                    showManageSubjectsDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.success)
                        ) {
                            Text("Save Changes")
                        }
                    }
                }
            }
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
