package com.school.manage.presentation.screens
import com.school.manage.core.firebase.FirestoreSyncService

import androidx.compose.foundation.BorderStroke
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
import com.school.manage.presentation.theme.LocalSchoolColors

@Composable
fun CustomRoundedInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    trailingIcon: String? = null
) {
    val colors = LocalSchoolColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.inputBg,
        border = BorderStroke(1.dp, colors.inputBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder, color = colors.textSecondary, fontSize = 14.sp) },
                textStyle = LocalTextStyle.current.copy(color = colors.textPrimary),
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
                Text(trailingIcon, fontSize = 18.sp, color = colors.textSecondary)
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
    val colors = LocalSchoolColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.inputBg,
        border = BorderStroke(1.dp, colors.inputBorder),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(label, fontSize = 11.sp, color = colors.textSecondary)
                Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            }
            Text("▾", fontSize = 14.sp, color = colors.textSecondary)
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
    val colors = LocalSchoolColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) colors.brandPrimary.copy(alpha = 0.15f) else colors.bgCard,
        border = BorderStroke(
            1.5.dp,
            if (isSelected) colors.brandPrimary else colors.borderCard
        ),
        modifier = Modifier.width(115.dp).height(46.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(icon, fontSize = 16.sp, color = if (isSelected) colors.brandPrimary else colors.textSecondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSelected) colors.brandPrimary else colors.textPrimary)
        }
    }
}

@Composable
fun FeePlanPillBanner() {
    val colors = LocalSchoolColors.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.brandPrimary.copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⏱", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "FEE PLAN • what this student owes",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.brandPrimary
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
    val colors = LocalSchoolColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = colors.bgCard,
        border = BorderStroke(1.dp, colors.borderCard),
        modifier = modifier
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(bannerColor.copy(alpha = if (colors.isDark) 0.3f else 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 13.sp)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(count, fontWeight = FontWeight.Black, fontSize = 18.sp, color = colors.brandPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colors.textPrimary)
                Text(subtitle, fontSize = 10.sp, color = colors.textSecondary)
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
    val colors = LocalSchoolColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.bgCard,
        border = BorderStroke(1.dp, colors.borderCard),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("👥", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.textPrimary)
                    Text(subtitle, fontSize = 10.sp, color = colors.textSecondary)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(pct, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = colors.brandPrimary)
                Text(ratio, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.brandPrimary)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.borderCard)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(colors.brandPrimary)
                )
            }
        }
    }
}

@Composable
fun LegendPill(label: String, color: Color) {
    val colors = LocalSchoolColors.current
    Surface(
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, color),
        color = colors.bgCard
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun ChartGridCanvas(lineColor: Color) {
    val colors = LocalSchoolColors.current
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
                        color = colors.borderCard.copy(alpha = 0.6f),
                        start = Offset(0f, i * stepY),
                        end = Offset(size.width, i * stepY),
                        strokeWidth = 1f
                    )
                }
                for (j in 0..9) {
                    drawLine(
                        color = colors.borderCard.copy(alpha = 0.6f),
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
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            (1..9).forEach {
                Text(it.toString(), fontSize = 10.sp, color = colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
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
    val colors = LocalSchoolColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bgColor.copy(alpha = if (colors.isDark) 0.25f else 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Text(iconEmoji, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
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
    val colors = LocalSchoolColors.current
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.brandPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                Text(subtitle, fontSize = 12.sp, color = colors.textSecondary)
            }
            Text("›", fontSize = 20.sp, color = colors.textSecondary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatMetricTile(
    title: String = "",
    value: String = "",
    icon: String = "",
    bgTint: Color = Color(0xFFEFF6FF),
    textColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    val colors = LocalSchoolColors.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.bgCard,
        border = BorderStroke(1.dp, colors.borderCard),
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
                    .background(bgTint.copy(alpha = if (colors.isDark) 0.3f else 1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
                Text(title, fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun ModernActionTile(
    title: String = "",
    desc: String = "",
    emoji: String = "",
    tint: Color = Color(0xFFEFF6FF),
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val colors = LocalSchoolColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = colors.bgCard,
        border = BorderStroke(1.dp, colors.borderCard),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = if (colors.isDark) 0.3f else 1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(desc, fontSize = 11.sp, color = colors.textSecondary, lineHeight = 14.sp)
        }
    }
}

@Composable
fun ModernDatePickerDialog(
    currentDate: String = "02/10/2026",
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    var selectedYear by remember { mutableStateOf(2026) }
    var selectedMonthIndex by remember { mutableStateOf(9) }
    var selectedDay by remember { mutableStateOf(2) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.bgCard,
            border = BorderStroke(1.dp, colors.borderCard),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Select Date",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                        Text("‹", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.brandPrimary)
                    }

                    Text(
                        text = months[selectedMonthIndex] + " " + selectedYear,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = colors.textPrimary
                    )

                    IconButton(onClick = {
                        if (selectedMonthIndex < 11) {
                            selectedMonthIndex += 1
                        } else {
                            selectedMonthIndex = 0
                            selectedYear += 1
                        }
                    }) {
                        Text("›", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.brandPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.height(210.dp)
                ) {
                    items(31) { idx ->
                        val day = idx + 1
                        val isSelected = (day == selectedDay)
                        Box(
                            modifier = Modifier
                                .padding(3.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) colors.brandPrimary else Color.Transparent)
                                .clickable { selectedDay = day },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.toString(),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                color = if (isSelected) Color.White else colors.textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val dayStr = if (selectedDay < 10) "0" + selectedDay else selectedDay.toString()
                            val monthNum = selectedMonthIndex + 1
                            val monthStr = if (monthNum < 10) "0" + monthNum else monthNum.toString()
                            onDateSelected(dayStr + "/" + monthStr + "/" + selectedYear)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
