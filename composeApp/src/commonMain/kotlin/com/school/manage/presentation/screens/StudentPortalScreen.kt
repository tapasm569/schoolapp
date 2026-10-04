package com.school.manage.presentation.screens

import androidx.compose.foundation.BorderStroke
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
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.SchoolEntity
import com.school.manage.core.database.entity.StudentEntity
import com.school.manage.presentation.theme.LocalSchoolColors

@Composable
fun StudentPortalScreen(
    database: AppDatabase,
    studentId: Long,
    onLogout: () -> Unit
) {
    val colors = LocalSchoolColors.current
    var student by remember { mutableStateOf(null as StudentEntity?) }
    var school by remember { mutableStateOf(null as SchoolEntity?) }
    val attendanceList by database.attendanceDao().getAttendanceByStudent(studentId).collectAsState(initial = emptyList())
    val feeRecords by database.feeDao().getFeeRecordsByStudent(studentId).collectAsState(initial = emptyList())

    LaunchedEffect(studentId) {
        val s = database.studentDao().getStudentById(studentId)
        student = s
        if (s != null) {
            school = database.schoolDao().getSchoolByCode(s.schoolCode)
        }
    }

    val totalPaid = feeRecords.sumOf { it.amountPaid }.toInt()
    val presentDays = attendanceList.count { it.status == "PRESENT" }
    val absentDays = attendanceList.count { it.status == "ABSENT" }
    val leaveDays = attendanceList.count { it.status == "LEAVE" }

    Scaffold(
        containerColor = colors.bgApp
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(10.dp)) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = student?.name ?: "Student",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = colors.textPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onLogout() }
                        ) {
                            Text(
                                text = "Change Account",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("▾", fontSize = 11.sp, color = colors.textSecondary)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔲", fontSize = 18.sp, color = colors.brandPrimary)
                        Text("💬", fontSize = 18.sp, color = colors.brandPrimary)
                        Text("🔔", fontSize = 18.sp, color = colors.brandPrimary)
                        Text(
                            text = "⎋",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = colors.error,
                            modifier = Modifier.clickable { onLogout() }
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StudentActionTile("Fees", "💰", Color(0xFFDCFCE7))
                        StudentActionTile("Exams", "📝", Color(0xFFE0F2FE))
                        StudentActionTile("Classwork", "🖥️", Color(0xFFFEF3C7))
                        StudentActionTile("Homework", "📖", Color(0xFFFFE4E6))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(22.dp)
                    ) {
                        StudentActionTile("Live Class", "⏱️", Color(0xFFFFEDD5))
                        StudentActionTile("Leave", "➖", Color(0xFFFEF9C3))
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BATCH INFORMATION",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    HorizontalDivider(color = colors.borderCard, modifier = Modifier.weight(1f))
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.bgCard,
                    border = BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.brandPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👥", fontSize = 22.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = student?.gradeClass?.ifEmpty { "General" } ?: "General",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = colors.textPrimary
                                )
                                Text("Monthly Tuition", fontSize = 12.sp, color = colors.textSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = colors.borderCard.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Joined", fontSize = 11.sp, color = colors.textSecondary)
                                Text(
                                    text = student?.admissionDate ?: "03/10/2026",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Paid", fontSize = 11.sp, color = colors.textSecondary)
                                Text(
                                    text = totalPaid.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = colors.success
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Status", fontSize = 11.sp, color = colors.textSecondary)
                                Text("Active", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.success)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Due", fontSize = 11.sp, color = colors.textSecondary)
                                Text("0", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.success)
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
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
                                Text("Attendance Summary", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.brandPrimary)
                                Text(student?.gradeClass?.ifEmpty { "General" } ?: "General", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
                            }
                            Text("Oct-2026", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatusCountPill("✓", presentDays.toString(), Color(0xFF86EFAC), Color(0xFF15803D))
                            StatusCountPill("✕", absentDays.toString(), Color(0xFFFCA5A5), Color(0xFFB91C1C))
                            StatusCountPill("−", leaveDays.toString(), Color(0xFFFDE68A), Color(0xFFB45309))
                            StatusCountPill("🏃", "0", Color(0xFF93C5FD), Color(0xFF1D4ED8))
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.bgCard,
                    border = BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(colors.borderCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏫", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = school?.schoolName?.ifEmpty { "SCHOOL APP" } ?: "SCHOOL APP",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Institute Code: " + (school?.schoolCode ?: "SCHOOL"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.brandPrimary
                            )
                            Text(
                                text = school?.phone?.ifEmpty { "N/A" } ?: "N/A",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun StudentActionTile(
    label: String,
    emoji: String,
    bgColor: Color
) {
    val colors = LocalSchoolColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bgColor.copy(alpha = if (colors.isDark) 0.3f else 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StatusCountPill(
    symbol: String,
    count: String,
    bgColor: Color,
    textColor: Color
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        modifier = Modifier.width(68.dp).height(30.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(symbol, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
            Spacer(modifier = Modifier.width(6.dp))
            Text(count, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
        }
    }
}
