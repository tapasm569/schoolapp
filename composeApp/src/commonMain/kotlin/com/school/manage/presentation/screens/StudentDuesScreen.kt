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
import com.school.manage.core.database.entity.StudentEntity
import com.school.manage.presentation.theme.LocalSchoolColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDuesScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val uriHandler = LocalUriHandler.current

    val students by database.studentDao().getStudentsBySchool(schoolCode).collectAsState(initial = emptyList())
    val allFeeRecords by database.feeDao().getAllFeeRecordsForSchool(schoolCode).collectAsState(initial = emptyList())

    val defaulters = remember(students, allFeeRecords) {
        students.map { student ->
            val totalPaid = allFeeRecords
                .filter { it.studentId == student.id }
                .sumOf { it.amountPaid }
            
            // Expected vs Paid calculation
            val dueAmount = maxOf(0.0, student.monthlyFee - totalPaid)
            Triple(student, totalPaid, dueAmount)
        }.filter { it.third > 0.0 }
    }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("Pending Fee Defaulters", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary) },
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
                color = Color(0xFFFEF2F2),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Defaulters", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        Text("${defaulters.size} Students Unpaid", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF991B1B))
                    }
                    val totalDueSum = defaulters.sumOf { it.third }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Outstanding", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        Text("₹${totalDueSum.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF991B1B))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (defaulters.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("🎉 All student fees are fully paid up!", fontWeight = FontWeight.Bold, color = colors.textSecondary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(defaulters) { (student, _, dueAmount) ->
                        StylishDefaulterCard(
                            student = student,
                            dueAmount = dueAmount,
                            onCall = {
                                if (student.phone.isNotBlank()) {
                                    uriHandler.openUri("tel:${student.phone}")
                                }
                            },
                            onWhatsApp = {
                                val cleanPhone = student.phone.replace("+", "").trim()
                                val msg = "Dear Parent, gentle reminder from school administration that a monthly tuition fee balance of ₹${dueAmount.toInt()} is pending for ${student.name} (Class: ${student.gradeClass}). Kindly clear it at your earliest convenience."
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
fun StylishDefaulterCard(
    student: StudentEntity,
    dueAmount: Double,
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
                            .background(Color(0xFFFEF2F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎓", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = student.name.ifEmpty { "Unnamed Student" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Class: ${student.gradeClass.ifEmpty { "General" }} | Roll: ${student.rollNo.ifEmpty { "N/A" }}",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEE2E2)
                ) {
                    Text(
                        text = "Due: ₹${dueAmount.toInt()}",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = Color(0xFFDC2626),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Father: ${student.fatherName.ifEmpty { "Not Provided" }}",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "Mother: ${student.motherName.ifEmpty { "Not Provided" }}",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "Phone: ${student.phone.ifEmpty { "No Phone" }}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F2FE))
                    ) {
                        Text("📞", fontSize = 16.sp)
                    }

                    IconButton(
                        onClick = onWhatsApp,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7))
                    ) {
                        Text("💬", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

private fun urlEncode(s: String): String {
    return s.replace(" ", "%20").replace("\n", "%0A")
}
