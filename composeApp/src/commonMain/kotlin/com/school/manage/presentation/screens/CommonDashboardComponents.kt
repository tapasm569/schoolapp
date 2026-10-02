package com.school.manage.presentation.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun ModernDatePickerDialog(
    currentDate: String = "02/10/2026",
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    var selectedYear by remember { mutableStateOf(2026) }
    var selectedMonthIndex by remember { mutableStateOf(9) } // October
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

                // Year & Month Switcher
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

                // Day numbers 1 to 31
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
