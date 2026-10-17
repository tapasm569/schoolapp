package com.school.manage.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ColorPresent = Color(0xFF10B981) // Emerald Green
private val ColorAbsent = Color(0xFFEF4444)  // Rose Red
private val ColorLeave = Color(0xFFF59E0B)   // Amber Orange
private val ColorHoliday = Color(0xFF3B82F6) // Sky / Indigo Blue

@Composable
fun StylishAttendanceSummaryCard(
    presentCount: Int,
    absentCount: Int,
    leaveCount: Int,
    holidayCount: Int,
    selectedFilter: String = "ALL",
    onFilterSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val total = presentCount + absentCount + leaveCount
    val presentPercent = if (total > 0) (presentCount.toFloat() / total) * 100f else 0f
    val absentPercent = if (total > 0) (absentCount.toFloat() / total) * 100f else 0f
    val leavePercent = if (total > 0) (leaveCount.toFloat() / total) * 100f else 0f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Proportional Segmented Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFF1F5F9))
            ) {
                if (total > 0) {
                    if (presentPercent > 0) Box(Modifier.weight(presentPercent).fillMaxHeight().background(ColorPresent))
                    if (absentPercent > 0) Box(Modifier.weight(absentPercent).fillMaxHeight().background(ColorAbsent))
                    if (leavePercent > 0) Box(Modifier.weight(leavePercent).fillMaxHeight().background(ColorLeave))
                } else {
                    Box(Modifier.fillMaxSize().background(Color(0xFFE2E8F0)))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Donut Chart + Bento Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(CircleShape)
                        .clickable { onFilterSelected("ALL") },
                    contentAlignment = Alignment.Center
                ) {
                    AttendanceDonutChart(
                        presentPct = presentPercent,
                        absentPct = absentPercent,
                        leavePct = leavePercent
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (total > 0) "${presentPercent.toInt()}%" else "0%",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (selectedFilter != "ALL") selectedFilter else "Attendance",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedFilter != "ALL") ColorPresent else Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AttendanceBentoTile(
                            title = "Present",
                            count = presentCount,
                            percentage = presentPercent,
                            accentColor = ColorPresent,
                            bgColor = Color(0xFFECFDF5),
                            isSelected = selectedFilter == "PRESENT",
                            modifier = Modifier.weight(1f),
                            onClick = { onFilterSelected("PRESENT") }
                        )
                        AttendanceBentoTile(
                            title = "Absent",
                            count = absentCount,
                            percentage = absentPercent,
                            accentColor = ColorAbsent,
                            bgColor = Color(0xFFFEF2F2),
                            isSelected = selectedFilter == "ABSENT",
                            modifier = Modifier.weight(1f),
                            onClick = { onFilterSelected("ABSENT") }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AttendanceBentoTile(
                            title = "Leave",
                            count = leaveCount,
                            percentage = leavePercent,
                            accentColor = ColorLeave,
                            bgColor = Color(0xFFFFFBEB),
                            isSelected = selectedFilter == "LEAVE",
                            modifier = Modifier.weight(1f),
                            onClick = { onFilterSelected("LEAVE") }
                        )
                        AttendanceBentoTile(
                            title = "Holiday",
                            count = holidayCount,
                            percentage = null,
                            accentColor = ColorHoliday,
                            bgColor = Color(0xFFEFF6FF),
                            isSelected = selectedFilter == "HOLIDAY",
                            modifier = Modifier.weight(1f),
                            onClick = { onFilterSelected("HOLIDAY") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceBentoTile(
    title: String,
    count: Int,
    percentage: Float?,
    accentColor: Color,
    bgColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) accentColor else accentColor.copy(alpha = 0.25f)
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.15f) else bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp, horizontal = 9.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155),
                    maxLines = 1,
                    softWrap = false
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                if (percentage != null) {
                    Text(
                        text = "${percentage.toInt()}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun AttendanceDonutChart(
    presentPct: Float,
    absentPct: Float,
    leavePct: Float,
    strokeWidth: Float = 20f
) {
    val total = presentPct + absentPct + leavePct

    Canvas(modifier = Modifier.fillMaxSize()) {
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
        val arcSize = Size(diameter, diameter)

        drawArc(
            color = Color(0xFFF1F5F9),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth)
        )

        if (total > 0f) {
            val presentSweep = (presentPct / 100f) * 360f
            val absentSweep = (absentPct / 100f) * 360f
            val leaveSweep = (leavePct / 100f) * 360f

            var currentAngle = -90f

            if (presentSweep > 0f) {
                drawArc(
                    color = ColorPresent,
                    startAngle = currentAngle,
                    sweepAngle = presentSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                currentAngle += presentSweep
            }

            if (absentSweep > 0f) {
                drawArc(
                    color = ColorAbsent,
                    startAngle = currentAngle,
                    sweepAngle = absentSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                currentAngle += absentSweep
            }

            if (leaveSweep > 0f) {
                drawArc(
                    color = ColorLeave,
                    startAngle = currentAngle,
                    sweepAngle = leaveSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
    }
}