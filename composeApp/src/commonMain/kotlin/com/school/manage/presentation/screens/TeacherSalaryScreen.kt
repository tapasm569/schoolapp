package com.school.manage.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.ExpenseEntity
import com.school.manage.core.database.entity.StaffEntity
import com.school.manage.core.utils.getSystemCurrentDate
import com.school.manage.presentation.theme.LocalSchoolColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherSalaryScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val sysToday = remember { getSystemCurrentDate() }

    val staffList by database.staffDao().getStaffBySchool(schoolCode).collectAsState(initial = emptyList())
    val allExpenses by database.expenseDao().getExpensesBySchool(schoolCode).collectAsState(initial = emptyList())

    val currentMonthKey = "${(sysToday.monthIndex + 1).toString().padStart(2, '0')}/${sysToday.year}"

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("Teacher & Staff Salaries", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgApp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Active Staff & Teachers", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                        Text("${staffList.size} Members Registered", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF1E40AF))
                    }
                    val totalMonthlyPayroll = staffList.sumOf { it.salary }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Monthly Payroll", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                        Text("₹${totalMonthlyPayroll.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF1E40AF))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (staffList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No teachers or staff added yet.", fontWeight = FontWeight.Bold, color = colors.textSecondary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(staffList) { staff ->
                        // Check if salary for current month is already paid via expense records
                        val isPaidThisMonth = allExpenses.any { exp ->
                            exp.category.equals("Salary", ignoreCase = true) &&
                            exp.title.contains(staff.name, ignoreCase = true) &&
                            exp.date.contains(currentMonthKey)
                        }

                        StylishStaffSalaryCard(
                            staff = staff,
                            isPaid = isPaidThisMonth,
                            currentMonth = currentMonthKey,
                            onPaySalary = {
                                scope.launch {
                                    val payoutAmount = staff.salary
                                    if (payoutAmount > 0.0) {
                                        database.expenseDao().insertExpense(
                                            ExpenseEntity(
                                                schoolCode = schoolCode,
                                                title = "Salary Payout: ${staff.name}",
                                                category = "Salary",
                                                amount = payoutAmount,
                                                date = sysToday.formatted,
                                                notes = "Monthly salary for $currentMonthKey"
                                            )
                                        )
                                    }
                                }
                            },
                            onCall = {
                                if (staff.phone.isNotBlank()) {
                                    uriHandler.openUri("tel:${staff.phone}")
                                }
                            },
                            onWhatsApp = {
                                val cleanPhone = staff.phone.replace("+", "").trim()
                                val msg = "Dear ${staff.name}, this is a notification from school administration regarding your monthly salary for $currentMonthKey (₹${staff.salary.toInt()})."
                                uriHandler.openUri("https://wa.me/$cleanPhone?text=${urlEncode(msg)}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StylishStaffSalaryCard(
    staff: StaffEntity,
    isPaid: Boolean,
    currentMonth: String,
    onPaySalary: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    val colors = LocalSchoolColors.current

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👩‍🏫", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = staff.name.ifEmpty { "Staff Member" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Role: ${staff.role.ifEmpty { "Teacher" }} | Phone: ${staff.phone.ifEmpty { "N/A" }}",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isPaid) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                ) {
                    Text(
                        text = if (isPaid) "Paid ✓" else "Due ($currentMonth)",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = if (isPaid) Color(0xFF16A34A) else Color(0xFFD97706),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = colors.borderCard)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly Salary: ₹${staff.salary.toInt()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Status for $currentMonth",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!isPaid) {
                        Button(
                            onClick = onPaySalary,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.success),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Pay Salary", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    IconButton(
                        onClick = onCall,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F2FE))
                    ) {
                        Text("📞", fontSize = 15.sp)
                    }

                    IconButton(
                        onClick = onWhatsApp,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7))
                    ) {
                        Text("💬", fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

private fun urlEncode(s: String): String {
    return s.replace(" ", "%20").replace("\n", "%0A")
}
