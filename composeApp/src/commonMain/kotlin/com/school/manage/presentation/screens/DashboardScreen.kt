package com.school.manage.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
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

    val distinctClasses = students.map { it.gradeClass }.distinct().size
    val totalFees = feeRecords.sumOf { it.amountPaid }
    val totalExpenses = expenses.sumOf { it.amount }

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

            // 1. Top Header Profile Row
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
                        subtitle = "0 closed",
                        bannerColor = Color(0xFFE0F2FE),
                        icon = "👥",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("student_list") }
                    )
                    TopCountCard(
                        count = "$distinctClasses",
                        title = "Classes",
                        subtitle = "0 closed",
                        bannerColor = Color(0xFFDCFCE7),
                        icon = "🏫",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("student_list") }
                    )
                    TopCountCard(
                        count = "${staffList.size}",
                        title = "Teacher",
                        subtitle = "0 closed",
                        bannerColor = Color(0xFFFEF3C7),
                        icon = "👨‍🏫",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("staff_list") }
                    )
                }
            }

            // 3. Attendance Summary Graph Card
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
                                Text("‹", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F52BA))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Oct-2026", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("›", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F52BA))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        ChartGridCanvas(lineColor = Color(0xFF38BDF8))

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            LegendPill("Present", Color(0xFF22C55E))
                            LegendPill("Absent", Color(0xFFEF4444))
                            LegendPill("Leave", Color(0xFFF59E0B))
                            LegendPill("Holiday", Color(0xFF3B82F6))
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
                        pct = "0%",
                        ratio = "0/0",
                        modifier = Modifier.weight(1f)
                    )
                    AttendanceProgressBarCard(
                        title = "Teacher",
                        subtitle = "Marking Attendance",
                        pct = "0%",
                        ratio = "0/0",
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
                                Text("(0) 0", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F52BA))
                                Text("Active", fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("(0) 0", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F52BA))
                                Text("Close", fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                }
            }

            // 6. Monthly Summary (Income vs Expense Table + Chart)
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
                                "Monthly Summary",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F52BA)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("‹", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F52BA))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Oct-2026", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("›", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F52BA))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        ChartGridCanvas(lineColor = Color(0xFFEF4444))

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
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

                        // Incomes Row
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("₹0", fontSize = 13.sp, color = Color(0xFF22C55E), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("₹${totalFees.toInt()}", fontSize = 13.sp, color = Color(0xFF22C55E), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("₹${totalFees.toInt()}", fontSize = 13.sp, color = Color(0xFF22C55E), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(6.dp))

                        // Expenses Row
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("₹0", fontSize = 13.sp, color = Color(0xFFEF4444), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("₹${totalExpenses.toInt()}", fontSize = 13.sp, color = Color(0xFFEF4444), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("₹${totalExpenses.toInt()}", fontSize = 13.sp, color = Color(0xFFEF4444), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            // 7. FEATURES 4x3 Grid Section
            item {
                Text(
                    text = "FEATURES",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    letterSpacing = 1.sp
                )
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
                        FeatureIconItem("Timetable", "📅", Color(0xFFECFDF5), onClick = { onNavigate("student_list") })
                        FeatureIconItem("Online\nClasses", "📖", Color(0xFFF0FDF4), onClick = { onNavigate("student_list") })
                        FeatureIconItem("Question\nBank", "📑", Color(0xFFFEF3C7), onClick = { onNavigate("student_list") })
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
                            Text(
                                text = "Add New",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF0F52BA)
                            )
                            Text(
                                text = "✕",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626),
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clickable { showAddNewDialog = false }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        AddNewOptionItem(
                            title = "Student",
                            subtitle = "You can add new student here",
                            icon = "👨‍‍🎓",
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
                                onNavigate("staff_list")
                            }
                        )
                        AddNewOptionItem(
                            title = "Class",
                            subtitle = "You can add new batch here",
                            icon = "👥",
                            onClick = {
                                showAddNewDialog = false
                                onNavigate("student_list")
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

@Composable
fun TopCountCard(
    count: String,
    title: String,
    subtitle: String,
    bannerColor: Color,
    icon: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .background(bannerColor),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 12.sp)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(count, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF0F766E))
                Spacer(modifier = Modifier.height(2.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E293B))
                Text(subtitle, fontSize = 10.sp, color = Color(0xFF64748B))
            }
        }
    }
}

@Composable
fun AttendanceProgressBarCard(
    title: String,
    subtitle: String,
    pct: String,
    ratio: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("👥", fontSize = 13.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                    Text(subtitle, fontSize = 9.sp, color = Color(0xFF64748B))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(pct, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F52BA))
                Text(ratio, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F52BA))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFF1F5F9))
                    .border(1.dp, Color(0xFF93C5FD), RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun LegendPill(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color),
        color = Color.White
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1E293B),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun ChartGridCanvas(lineColor: Color) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stepY = size.height / 5
                val stepX = size.width / 9

                // Grid Lines
                for (i in 0..5) {
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(0f, i * stepY),
                        end = Offset(size.width, i * stepY),
                        strokeWidth = 1f
                    )
                }
                for (j in 0..9) {
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(j * stepX, 0f),
                        end = Offset(j * stepX, size.height),
                        strokeWidth = 1f
                    )
                }

                // Data Line at 0 baseline
                val baseline = size.height
                drawLine(
                    color = lineColor,
                    start = Offset(0f, baseline),
                    end = Offset(size.width, baseline),
                    strokeWidth = 3f
                )
                for (k in 0..9) {
                    drawCircle(
                        color = lineColor,
                        radius = 4f,
                        center = Offset(k * stepX, baseline)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            (1..9).forEach {
                Text("$it", fontSize = 10.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun FeatureIconItem(
    title: String,
    iconEmoji: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(iconEmoji, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            color = Color(0xFF334155),
            textAlign = TextAlign.Center,
            lineHeight = 13.sp,
            maxLines = 2
        )
    }
}

@Composable
fun AddNewOptionItem(
    title: String,
    subtitle: String,
    icon: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFFEFF6FF)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF64748B))
        }
    }
}
