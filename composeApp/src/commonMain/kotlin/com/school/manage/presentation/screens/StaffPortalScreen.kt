package com.school.manage.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.StaffEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffPortalScreen(
    database: AppDatabase,
    staffId: Long,
    schoolCode: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    var staff by remember { mutableStateOf<StaffEntity?>(null) }
    val students by database.studentDao().getStudentsBySchool(schoolCode).collectAsState(initial = emptyList())
    val staffMembers by database.staffDao().getStaffBySchool(schoolCode).collectAsState(initial = emptyList())

    LaunchedEffect(staffId) {
        staff = database.staffDao().getStaffById(staffId)
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(10.dp)) }

            // 1. Top Header with Staff Greeting & Logout
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF059669)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (staff?.name ?: "S").take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 19.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Welcome,", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text(staff?.name ?: "Faculty Member", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                        }
                    }

                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Text("⎋", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE11D48))
                    }
                }
            }

            // 2. Faculty Identity Hero Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF059669))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FACULTY & STAFF CREDENTIALS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA7F3D0),
                                letterSpacing = 1.sp
                            )
                            Surface(shape = RoundedCornerShape(12.dp), color = Color(0x33FFFFFF)) {
                                Text(
                                    text = "School: $schoolCode",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(staff?.name ?: "", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Designation: ${staff?.role ?: "Staff"}  •  Contact: ${staff?.phone ?: "N/A"}",
                            fontSize = 13.sp,
                            color = Color(0xFFD1FAE5)
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0x33FFFFFF))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Joined: ${staff?.joinDate ?: "Active"}", fontSize = 12.sp, color = Color(0xFFA7F3D0))
                            Text("Salary: ₹${staff?.salary?.toInt() ?: 0}/mo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFDE047))
                        }
                    }
                }
            }

            // 3. Quick Stats Overview
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatMetricTile(
                        title = "Enrolled Students",
                        value = "${students.size}",
                        icon = "🎒",
                        bgTint = Color(0xFFEFF6FF),
                        textColor = Color(0xFF1D4ED8),
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricTile(
                        title = "School Faculty",
                        value = "${staffMembers.size}",
                        icon = "👥",
                        bgTint = Color(0xFFF0FDF4),
                        textColor = Color(0xFF15803D),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4. Staff Operations Title
            item {
                Text(
                    text = "Staff Operations & Tasks",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
            }

            // 5. Action Tiles Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ModernActionTile(
                            title = "Daily Attendance",
                            desc = "Mark student register",
                            emoji = "📋",
                            tint = Color(0xFFDCFCE7),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("attendance") }
                        )
                        ModernActionTile(
                            title = "Students Directory",
                            desc = "View classes & details",
                            emoji = "🎓",
                            tint = Color(0xFFE0E7FF),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("student_list") }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ModernActionTile(
                            title = "Fee Collection",
                            desc = "Record fee receipts",
                            emoji = "💳",
                            tint = Color(0xFFFEF3C7),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("fee_collection") }
                        )
                        ModernActionTile(
                            title = "School Expenses",
                            desc = "Log utility/item bills",
                            emoji = "🧾",
                            tint = Color(0xFFFFE4E6),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("expense_list") }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(26.dp)) }
        }
    }
}
