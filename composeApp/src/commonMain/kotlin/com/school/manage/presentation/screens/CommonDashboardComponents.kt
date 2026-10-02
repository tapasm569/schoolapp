package com.school.manage.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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

@Composable
fun CustomRoundedInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    trailingIcon: String? = null
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder, color = Color(0xFF94A3B8), fontSize = 14.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            if (trailingIcon != null) {
                Text(trailingIcon, fontSize = 18.sp, color = Color(0xFF475569))
            }
        }
    }
}

@Composable
fun DropdownTriggerInput(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(label, fontSize = 10.sp, color = Color(0xFF64748B))
                Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
            }
            Text("▾", fontSize = 14.sp, color = Color(0xFF475569))
        }
    }
}

@Composable
fun GenderOptionButton(
    label: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isSelected) Color(0xFFF97316) else Color(0xFFE2E8F0)
        ),
        modifier = Modifier.width(110.dp).height(44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(icon, fontSize = 14.sp, color = if (isSelected) Color(0xFFF97316) else Color(0xFF64748B), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color(0xFFF97316) else Color(0xFF0F172A))
        }
    }
}

@Composable
fun FeePlanPillBanner() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFDBEAFE),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⏱", fontSize = 12.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "FEE PLAN • what this student owes",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E40AF)
            )
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
    progress: Float = 0f,
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
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(Color(0xFF2563EB))
                )
            }
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

@Composable
fun StatMetricTile(
    title: String,
    value: String,
    icon: String,
    bgTint: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgTint),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                Text(title, fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun ModernActionTile(
    title: String,
    desc: String,
    emoji: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
            Spacer(modifier = Modifier.height(2.dp))
            Text(desc, fontSize = 11.sp, color = Color(0xFF64748B), lineHeight = 14.sp)
        }
    }
}

@Composable
fun ModernDatePickerDialog(
    currentDate: String = "02/10/2026",
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    var selectedYear by remember { mutableStateOf(2026) }
    var selectedMonthIndex by remember { mutableStateOf(9) }
    var selectedDay by remember { mutableStateOf(2) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(10.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Select Date",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (selectedMonthIndex > 0) {
                            selectedMonthIndex -= 1
                        } else {
                            selectedMonthIndex = 11
                            selectedYear -= 1
                        }
                    }) {
                        Text("‹", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D529C))
                    }

                    Text(
                        text = "${months[selectedMonthIndex]} $selectedYear",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A)
                    )

                    IconButton(onClick = {
                        if (selectedMonthIndex < 11) {
                            selectedMonthIndex += 1
                        } else {
                            selectedMonthIndex = 0
                            selectedYear += 1
                        }
                    }) {
                        Text("›", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D529C))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(31) { idx ->
                        val day = idx + 1
                        val isSelected = (day == selectedDay)
                        Box(
                            modifier = Modifier
                                .padding(3.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFF0D529C) else Color.Transparent)
                                .clickable { selectedDay = day },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$day",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFF1E293B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val dayStr = if (selectedDay < 10) "0$selectedDay" else "$selectedDay"
                            val monthNum = selectedMonthIndex + 1
                            val monthStr = if (monthNum < 10) "0$monthNum" else "$monthNum"
                            onDateSelected("$dayStr/$monthStr/$selectedYear")
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D529C))
                    ) {
                        Text("Select")
                    }
                }
            }
        }
    }
}
