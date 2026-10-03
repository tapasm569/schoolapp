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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.SchoolEntity
import com.school.manage.core.database.entity.StaffEntity
import com.school.manage.presentation.theme.LocalSchoolColors

@Composable
fun StaffPortalScreen(
    database: AppDatabase,
    staffId: Long,
    schoolCode: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val colors = LocalSchoolColors.current
    var staff by remember { mutableStateOf<StaffEntity?>(null) }
    var school by remember { mutableStateOf<SchoolEntity?>(null) }

    LaunchedEffect(staffId) {
        val s = database.staffDao().getStaffById(staffId)
        staff = s
        school = database.schoolDao().getSchoolByCode(schoolCode)
    }

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
                            text = staff?.name ?: "Faculty Member",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Role: " + (staff?.role ?: "Teacher"),
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.bgCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Faculty Profile", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.brandPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Contact: " + (staff?.phone ?: "N/A"), fontSize = 13.sp, color = colors.textPrimary)
                        Text("Joined: " + (staff?.joinDate ?: "N/A"), fontSize = 13.sp, color = colors.textSecondary)
                        Text("Qualification: " + (staff?.qualification?.ifEmpty { "N/A" } ?: "N/A"), fontSize = 13.sp, color = colors.textSecondary)
                        Text("Salary: ₹" + (staff?.salary?.toInt() ?: 0) + " (" + (staff?.salaryType ?: "Monthly") + ")", fontSize = 13.sp, color = colors.success, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Text("QUICK ACTIONS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colors.textSecondary, letterSpacing = 1.sp)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModernActionTile(
                        title = "Mark Attendance",
                        desc = "Record student attendance",
                        emoji = "📝",
                        tint = Color(0xFFDCFCE7),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("attendance") }
                    )
                    ModernActionTile(
                        title = "Classes",
                        desc = "View school classes",
                        emoji = "🏫",
                        tint = Color(0xFFE0F2FE),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("batch_list") }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}
