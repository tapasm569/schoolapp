package com.school.manage.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.FeeRecordEntity
import com.school.manage.presentation.theme.LocalSchoolColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeCollectionScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val students by database.studentDao().getStudentsBySchool(schoolCode).collectAsState(initial = emptyList())
    val feeRecords by database.feeDao().getFeeRecordsBySchool(schoolCode).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var selectedStudentId by remember { mutableStateOf(null as Long?) }
    var selectedStudentName by remember { mutableStateOf("") }
    var selectedStudentClass by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var feeMonth by remember { mutableStateOf("10/2026") }
    var remarks by remember { mutableStateOf("Monthly Tuition Fee") }
    var showStudentDropdown by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("Fee Collection", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgApp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.bgCard,
                    border = BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Collect Student Fee", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)

                        Box {
                            DropdownTriggerInput(
                                label = "Select Student",
                                value = if (selectedStudentName.isEmpty()) "Select Student" else selectedStudentName + " (" + selectedStudentClass + ")",
                                onClick = { showStudentDropdown = true }
                            )
                            DropdownMenu(
                                expanded = showStudentDropdown,
                                onDismissRequest = { showStudentDropdown = false }
                            ) {
                                for (s in students) {
                                    DropdownMenuItem(
                                        text = { Text(s.name + " - Class " + s.gradeClass) },
                                        onClick = {
                                            selectedStudentId = s.id
                                            selectedStudentName = s.name
                                            selectedStudentClass = s.gradeClass
                                            amount = s.monthlyFee.toInt().toString()
                                            showStudentDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        CustomRoundedInput(
                            value = amount,
                            onValueChange = { amount = it },
                            placeholder = "Amount Paid (₹)"
                        )

                        CustomRoundedInput(
                            value = feeMonth,
                            onValueChange = { feeMonth = it },
                            placeholder = "Fee Month (e.g. 10/2026)"
                        )

                        CustomRoundedInput(
                            value = remarks,
                            onValueChange = { remarks = it },
                            placeholder = "Remarks (e.g. Monthly Tuition Fee)"
                        )

                        Button(
                            onClick = {
                                val sId = selectedStudentId
                                val amt = amount.toDoubleOrNull() ?: 0.0
                                if (sId != null && amt > 0.0) {
                                    scope.launch {
                                        database.feeDao().insertFeeRecord(
                                            FeeRecordEntity(
                                                schoolCode = schoolCode,
                                                studentId = sId,
                                                studentName = selectedStudentName,
                                                gradeClass = selectedStudentClass,
                                                amountPaid = amt,
                                                paymentDate = "03/10/2026",
                                                feeMonth = feeMonth,
                                                paymentMode = "CASH",
                                                remarks = remarks
                                            )
                                        )
                                        amount = ""
                                        selectedStudentId = null
                                        selectedStudentName = ""
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary)
                        ) {
                            Text("Record Payment", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Text("Recent Payments (" + feeRecords.size + ")", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
            }

            items(feeRecords) { fee ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.bgCard,
                    border = BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(fee.studentName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                            Text("Month: " + fee.feeMonth + "  •  " + fee.paymentDate, fontSize = 12.sp, color = colors.textSecondary)
                            if (fee.remarks.isNotEmpty()) {
                                Text(fee.remarks, fontSize = 11.sp, color = colors.brandPrimary)
                            }
                        }
                        Text("₹" + fee.amountPaid.toInt(), fontWeight = FontWeight.Black, fontSize = 16.sp, color = colors.success)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}
