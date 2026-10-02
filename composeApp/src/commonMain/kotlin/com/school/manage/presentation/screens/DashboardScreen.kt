package com.school.manage.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.window.Dialog
import com.school.manage.core.database.AppDatabase

@Composable
fun DashboardScreen(
    database: AppDatabase,
    schoolCode: String,
    schoolName: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val students by database.studentDao().getStudentsBySchool(schoolCode).collectAsState(initial = emptyList())
    val feeRecords by database.feeDao().getFeeRecordsBySchool(schoolCode).collectAsState(initial = emptyList())
    val expenses by database.expenseDao().getExpensesBySchool(schoolCode).collectAsState(initial = emptyList())
    val staffList by database.staffDao().getStaffBySchool(schoolCode).collectAsState(initial = emptyList())
    val batches by database.batchDao().getBatchesBySchool(schoolCode).collectAsState(initial = emptyList())
    val attendanceList by database.attendanceDao().getAttendanceBySchool(schoolCode).collectAsState(initial = emptyList())

    val monthsList = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    var selectedMonthIndex by remember { mutableStateOf(9) } // 9 = October
    var selectedYear by remember { mutableStateOf(2026) }

    val currentMonthLabel = "${monthsList[selectedMonthIndex]}-$selectedYear"
    val monthNumberStr = if (selectedMonthIndex + 1 < 10) "0${selectedMonthIndex + 1}" else "${selectedMonthIndex + 1}"
    val monthFilterPattern = "$monthNumberStr/$selectedYear"

    // Filter fees & expenses for selected month
    val monthlyFees = feeRecords.filter { it.feeMonth.contains(monthFilterPattern) || it.paymentDate.contains(monthFilterPattern) }.sumOf { it.amountPaid }
    val totalFees = feeRecords.sumOf { it.amountPaid }
    val monthlyExpenses = expenses.filter { it.date.contains(monthFilterPattern) }.sumOf { it.amount }
    val totalExpenses = expenses.sumOf { it.amount }

    // Filter Attendance for selected month
    val studentAttendance = attendanceList.filter { it.userType == "STUDENT" && it.date.contains(monthFilterPattern) }
    val teacherAttendance = attendanceList.filter { it.userType == "STAFF" && it.date.contains(monthFilterPattern) }

    val presentCount = studentAttendance.count { it.status == "PRESENT" }
    val absentCount = studentAttendance.count { it.status == "ABSENT" }
    val leaveCount = studentAttendance.count { it.status == "LEAVE" }
    val holidayCount = studentAttendance.count { it.status == "HOLIDAY" }

    val studentTotalMarked = presentCount + absentCount + leaveCount
    val studentPct = if (studentTotalMarked > 0) (presentCount * 100 / studentTotalMarked) else 0

    val teacherPresent = teacherAttendance.count { it.status == "PRESENT" }
    val teacherTotalMarked = teacherAttendance.count { it.status in listOf("PRESENT", "ABSENT", "LEAVE") }
    val teacherPct = if (teacherTotalMarked > 0) (teacherPresent * 100 / teacherTotalMarked) else 0

    var showAddNewDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFFF4F7FB),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddNewDialog = true },
                containerColor = Color(0xFF0D529C),
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Text("+", fontSize = 30.sp, fontWeight = FontWeight.Light)
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

            // 1. Top Header Profile
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
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B)),
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
                                color = Color(0xFF0F172A)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Change Account",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("▾", fontSize = 10.sp, color = Color(0xFF64748B))
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("🔍", fontSize = 20.sp, color = Color(0xFF334155))
                        Text("⚙", fontSize = 20.sp, color = Color(0xFF334155), modifier = Modifier.clickable { onLogout() })
                    }
                }
            }

            // 2. Top Counter Tri-Cards (Students, Classes, Teacher)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TopCountCard(
                        count = "${students.size}",
                        title = "Students",
                        subtitle = "${students.size} active",
                        bannerColor = Color(0xFFE0F2FE),
                        icon = "👥",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("student_list") }
                    )
                    TopCountCard(
                        count = "${batches.size}",
                        title = "Classes",
                        subtitle = "${batches.size} active",
                        bannerColor = Color(0xFFDCFCE7),
                        icon = "🏫",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("batch_list") }
                    )
                    TopCountCard(
                        count = "${staffList.size}",
                        title = "Teacher",
                        subtitle = "${staffList.size} active",
                        bannerColor = Color(0xFFFEF3C7),
                        icon = "👨‍🏫",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("staff_list") }
                    )
                }
            }

            // 3. Attendance Summary Graph Card (Interactive Month Switcher)
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Attendance\nSummary",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F52BA),
                                lineHeight = 18.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "‹",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F52BA),
                                    modifier = Modifier.clickable {
                                        if (selectedMonthIndex > 0) selectedMonthIndex -= 1 else { selectedMonthIndex = 11; selectedYear -= 1 }
                                    }.padding(horizontal = 6.dp)
                                )
                                Text(currentMonthLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text(
                                    text = "›",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F52BA),
                                    modifier = Modifier.clickable {
                                        if (selectedMonthIndex < 11) selectedMonthIndex += 1 else { selectedMonthIndex = 0; selectedYear += 1 }
                                    }.padding(horizontal = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        ChartGridCanvas(lineColor = Color(0xFF38BDF8))

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            LegendPill("Present: $presentCount", Color(0xFF22C55E))
                            LegendPill("Absent: $absentCount", Color(0xFFEF4444))
                            LegendPill("Leave: $leaveCount", Color(0xFFF59E0B))
                            LegendPill("Holiday: $holidayCount", Color(0xFF3B82F6))
                        }
                    }
                }
            }

            // 4. Marking Attendance Progress Bars (Percentages & Ratios)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AttendanceProgressBarCard(
                        title = "Student",
                        subtitle = "Marking Attendance",
                        pct = "$studentPct%",
                        ratio = "$presentCount/$studentTotalMarked",
                        progress = studentPct / 100f,
                        modifier = Modifier.weight(1f)
                    )
                    AttendanceProgressBarCard(
                        title = "Teacher",
                        subtitle = "Marking Attendance",
                        pct = "$teacherPct%",
                        ratio = "$teacherPresent/$teacherTotalMarked",
                        progress = teacherPct / 100f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. Due Fees Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📑", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Due Fees", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("(0) ₹0", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F52BA))
                                Text("Active", fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("(0) ₹0", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F52BA))
                                Text("Close", fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                }
            }

            // 6. Monthly Summary (Working Month Switcher & Totals)
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Monthly Summary", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F52BA))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "‹",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F52BA),
                                    modifier = Modifier.clickable {
                                        if (selectedMonthIndex > 0) selectedMonthIndex -= 1 else { selectedMonthIndex = 11; selectedYear -= 1 }
                                    }.padding(horizontal = 6.dp)
                                )
                                Text(currentMonthLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text(
                                    text = "›",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F52BA),
                                    modifier = Modifier.clickable {
                                        if (selectedMonthIndex < 11) selectedMonthIndex += 1 else { selectedMonthIndex = 0; selectedYear += 1 }
                                    }.padding(horizontal = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        ChartGridCanvas(lineColor = Color(0xFFEF4444))

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            LegendPill("Incomes", Color(0xFF22C55E))
                            Spacer(modifier = Modifier.width(16.dp))
                            LegendPill("Expenses", Color(0xFFEF4444))
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Today", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("Monthly", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("Total", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(6.dp))

                        // Incomes
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("₹0", fontSize = 13.sp, color = Color(0xFF22C55E), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("₹${monthlyFees.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("₹${totalFees.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(6.dp))

                        // Expenses
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("₹0", fontSize = 13.sp, color = Color(0xFFEF4444), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("₹${monthlyExpenses.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("₹${totalExpenses.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            // 7. FEATURES Section
            item {
                Text("FEATURES", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF475569), letterSpacing = 1.sp)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        FeatureIconItem("Exams", "⏱️", Color(0xFFFFF7ED), onClick = { onNavigate("student_list") })
                        FeatureIconItem("Birthdays", "🎂", Color(0xFFFEF3C7), onClick = { onNavigate("student_list") })
                        FeatureIconItem("Home works", "📋", Color(0xFFECFDF5), onClick = { onNavigate("student_list") })
                        FeatureIconItem("Class works", "📊", Color(0xFFF0FDF4), onClick = { onNavigate("student_list") })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        FeatureIconItem("Enquiry", "👤", Color(0xFFFFF7ED), onClick = { onNavigate("add_student") })
                        FeatureIconItem("Staff Logs", "🔢", Color(0xFFE0F2FE), onClick = { onNavigate("staff_list") })
                        FeatureIconItem("Announcements", "🔔", Color(0xFFFFF7ED), onClick = { onNavigate("student_list") })
                        FeatureIconItem("Messages", "💬", Color(0xFFECFDF5), onClick = { onNavigate("student_list") })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        FeatureIconItem("Leave\nManagement", "➖", Color(0xFFF0FDF4), onClick = { onNavigate("attendance") })
                        FeatureIconItem("Timetable", "📅", Color(0xFFECFDF5), onClick = { onNavigate("batch_list") })
                        FeatureIconItem("Online\nClasses", "📖", Color(0xFFF0FDF4), onClick = { onNavigate("batch_list") })
                        FeatureIconItem("Question\nBank", "📑", Color(0xFFFEF3C7), onClick = { onNavigate("batch_list") })
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(70.dp)) }
        }

        // 8. ADD NEW POPUP DIALOG
        if (showAddNewDialog) {
            Dialog(onDismissRequest = { showAddNewDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Add New", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F52BA))
                            Text(
                                text = "✕",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.padding(4.dp).clickable { showAddNewDialog = false }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        AddNewOptionItem(
                            title = "Student",
                            subtitle = "You can add new student here",
                            icon = "👨‍🎓",
                            onClick = {
                                showAddNewDialog = false
                                onNavigate("add_student")
                            }
                        )
                        AddNewOptionItem(
                            title = "Teacher",
                            subtitle = "You can add new staff here",
                            icon = "👨‍🏫",
                            onClick = {
                                showAddNewDialog = false
                                onNavigate("add_staff")
                            }
                        )
                        AddNewOptionItem(
                            title = "Class",
                            subtitle = "You can add new school class here",
                            icon = "👥",
                            onClick = {
                                showAddNewDialog = false
                                onNavigate("add_batch")
                            }
                        )
                        AddNewOptionItem(
                            title = "Exams",
                            subtitle = "You can add new exam here",
                            icon = "📝",
                            onClick = {
                                showAddNewDialog = false
                                onNavigate("student_list")
                            }
                        )
                        AddNewOptionItem(
                            title = "Expense",
                            subtitle = "You can add expense here",
                            icon = "🧾",
                            onClick = {
                                showAddNewDialog = false
                                onNavigate("expense_list")
                            }
                        )
                        AddNewOptionItem(
                            title = "New Admission",
                            subtitle = "You can add new enquiry here",
                            icon = "👤",
                            onClick = {
                                showAddNewDialog = false
                                onNavigate("add_student")
                            }
                        )
                        AddNewOptionItem(
                            title = "Collect Fee",
                            subtitle = "Record a fee payment from a student",
                            icon = "💰",
                            onClick = {
                                showAddNewDialog = false
                                onNavigate("fee_collection")
                            }
                        )
                    }
                }
            }
        }
    }
}
