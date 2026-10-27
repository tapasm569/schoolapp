package com.school.manage.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.FeeRecordEntity
import com.school.manage.core.database.entity.StudentEntity
import com.school.manage.core.firebase.FirestoreSyncService
import com.school.manage.core.utils.ProfileAvatar
import com.school.manage.presentation.theme.LocalSchoolColors
import kotlinx.coroutines.launch

data class StudentDueItem(
    val student: StudentEntity,
    val monthlyRate: Double,
    val elapsedMonths: Int,
    val totalExpected: Double,
    val totalPaid: Double,
    val dueAmount: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDuesScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    val students by database.studentDao().getStudentsBySchool(schoolCode).collectAsState(initial = emptyList())
    val allFeeRecords by database.feeDao().getFeeRecordsBySchool(schoolCode).collectAsState(initial = emptyList())
    val batches by database.batchDao().getBatchesBySchool(schoolCode).collectAsState(initial = emptyList())

    val sysDate = remember { getSystemCurrentDate() }
    val currentMonthKey = "${(sysDate.monthIndex + 1).toString().padStart(2, '0')}/${sysDate.year}"
    val todayFormatted = try {
        "${sysDate.day.toString().padStart(2, '0')}/${(sysDate.monthIndex + 1).toString().padStart(2, '0')}/${sysDate.year}"
    } catch (_: Exception) {
        "26/10/2026"
    }

    var selectedClassFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var studentForPayment by remember { mutableStateOf<StudentDueItem?>(null) }

    // Real-time calculation of student dues
    val allDuesList = remember(students, allFeeRecords, batches, sysDate) {
        students.mapNotNull { student ->
            val paid = allFeeRecords
                .filter { it.studentId == student.id }
                .sumOf { it.amountPaid }

            val parts = student.admissionDate.split("/")
            val admMonth = parts.getOrNull(1)?.toIntOrNull()?.minus(1) ?: sysDate.monthIndex
            val admYear = parts.getOrNull(2)?.toIntOrNull() ?: sysDate.year
            val elapsedMonths = maxOf(1, (sysDate.year - admYear) * 12 + (sysDate.monthIndex - admMonth) + 1)

            val batch = batches.find { it.batchName.equals(student.gradeClass, ignoreCase = true) }
            val monthlyRate = if (student.monthlyFee > 0.0) student.monthlyFee else (batch?.defaultMonthlyTuition ?: 0.0)
            val expected = (monthlyRate * elapsedMonths) + student.admissionFee
            val due = maxOf(0.0, expected - paid)

            if (due > 0.0) {
                StudentDueItem(
                    student = student,
                    monthlyRate = monthlyRate,
                    elapsedMonths = elapsedMonths,
                    totalExpected = expected,
                    totalPaid = paid,
                    dueAmount = due
                )
            } else null
        }
    }

    val filteredDues = remember(allDuesList, selectedClassFilter, searchQuery) {
        allDuesList.filter { item ->
            val matchesClass = selectedClassFilter == "ALL" || item.student.gradeClass.equals(selectedClassFilter, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                item.student.name.contains(searchQuery, ignoreCase = true) ||
                item.student.rollNo.contains(searchQuery, ignoreCase = true) ||
                item.student.fatherName.contains(searchQuery, ignoreCase = true)
            matchesClass && matchesSearch
        }
    }

    val totalDuesSum = remember(allDuesList) { allDuesList.sumOf { it.dueAmount } }
    val classOptions = remember(batches, students) {
        listOf("ALL") + (batches.map { it.batchName } + students.map { it.gradeClass }).filter { it.isNotBlank() }.distinct()
    }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Student Dues", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)
                        Text(
                            "${allDuesList.size} Students • Total Due: ₹${totalDuesSum.toInt()}",
                            fontSize = 11.sp,
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
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
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("🔍 Search name, roll, father...", fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = colors.brandPrimary,
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            // Class Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(classOptions) { cls ->
                    val isSelected = selectedClassFilter == cls
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) colors.brandPrimary else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) colors.brandPrimary else Color(0xFFE2E8F0)),
                        modifier = Modifier.clickable { selectedClassFilter = cls }
                    ) {
                        Text(
                            text = if (cls == "ALL") "All Classes" else cls,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Defaulters List
            if (filteredDues.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎉", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No Pending Dues!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            "All students are up-to-date with tuition fees.",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredDues, key = { it.student.id }) { item ->
                        val st = item.student
                        val cleanPhone = st.phone.filter { it.isDigit() }
                        val father = st.fatherName.ifBlank { st.guardianName.ifBlank { "N/A" } }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            border = BorderStroke(1.2.dp, Color(0xFFF1F5F9)),
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Student Top Header: Avatar + Name + Roll
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    ProfileAvatar(
                                        photoBase64 = st.photoBase64,
                                        name = st.name,
                                        fallbackEmoji = "🎓",
                                        size = 54.dp,
                                        cornerRadius = 14.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                st.name,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFEFF6FF),
                                                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                                            ) {
                                                Text(
                                                    "Roll #${st.rollNo.ifBlank { "-" }}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1D4ED8),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            "👨 Father: $father",
                                            fontSize = 12.sp,
                                            color = colors.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "📚 Class: ${st.gradeClass.ifBlank { "General" }}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.brandPrimary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Dues Highlight Box
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFFEF2F2),
                                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("DUE AMOUNT", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626), letterSpacing = 0.5.sp)
                                            Text("₹${item.dueAmount.toInt()}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF991B1B))
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("${item.elapsedMonths} mo payable", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF7F1D1D))
                                            Text("Paid: ₹${item.totalPaid.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Action Buttons (Call, WhatsApp, Collect Payment)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Call Button
                                    OutlinedButton(
                                        onClick = {
                                            if (cleanPhone.isNotBlank()) {
                                                uriHandler.openUri("tel:$cleanPhone")
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1E293B)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier.weight(1f).height(40.dp)
                                    ) {
                                        Text("📞 Call", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // WhatsApp Button
                                    Button(
                                        onClick = {
                                            if (cleanPhone.isNotBlank()) {
                                                val reminderMsg = "Dear Parent, this is a fee reminder for ${st.name} (Roll #${st.rollNo}, ${st.gradeClass}). Total pending due is ₹${item.dueAmount.toInt()}. Please clear the dues at your earliest convenience."
                                                val encoded = reminderMsg.replace(" ", "%20").replace("\n", "%0A").replace("#", "%23")
                                                uriHandler.openUri("https://wa.me/91$cleanPhone?text=$encoded")
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier.weight(1.2f).height(40.dp)
                                    ) {
                                        Text("💬 WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Collect Payment Button
                                Button(
                                    onClick = { studentForPayment = item },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary, contentColor = Color.White),
                                    modifier = Modifier.fillMaxWidth().height(42.dp)
                                ) {
                                    Text("💳 Collect Payment", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ================= COLLECT PAYMENT DIALOG =================
    if (studentForPayment != null) {
        val currentItem = studentForPayment!!
        val student = currentItem.student
        var isFullPayment by remember { mutableStateOf(true) }
        var amountText by remember { mutableStateOf(currentItem.dueAmount.toInt().toString()) }
        var feeMonthInput by remember { mutableStateOf(currentMonthKey) }
        var paymentDateInput by remember { mutableStateOf(todayFormatted) }
        var paymentMode by remember { mutableStateOf("CASH") }
        var isSubmitting by remember { mutableStateOf(false) }

        val enteredVal = amountText.toDoubleOrNull() ?: 0.0
        val remainingBalance = maxOf(0.0, currentItem.dueAmount - enteredVal)

        Dialog(onDismissRequest = { if (!isSubmitting) studentForPayment = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Collect Payment", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        IconButton(onClick = { studentForPayment = null }) {
                            Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                    }

                    // Student Details Snapshot
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            ProfileAvatar(
                                photoBase64 = student.photoBase64,
                                name = student.name,
                                fallbackEmoji = "🎓",
                                size = 44.dp,
                                cornerRadius = 10.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(student.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
                                Text("${student.gradeClass} • Roll #${student.rollNo}", fontSize = 11.sp, color = colors.textSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Outstanding Banner
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Outstanding Due:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                            Text("₹${currentItem.dueAmount.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Full vs Partial Payment Radio Buttons
                    Text("PAYMENT OPTION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isFullPayment) colors.brandPrimary.copy(alpha = 0.12f) else Color.White,
                            border = BorderStroke(1.5.dp, if (isFullPayment) colors.brandPrimary else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    isFullPayment = true
                                    amountText = currentItem.dueAmount.toInt().toString()
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                RadioButton(
                                    selected = isFullPayment,
                                    onClick = {
                                        isFullPayment = true
                                        amountText = currentItem.dueAmount.toInt().toString()
                                    }
                                )
                                Text("Full Pay", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (!isFullPayment) colors.brandPrimary.copy(alpha = 0.12f) else Color.White,
                            border = BorderStroke(1.5.dp, if (!isFullPayment) colors.brandPrimary else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    isFullPayment = false
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                RadioButton(
                                    selected = !isFullPayment,
                                    onClick = { isFullPayment = false }
                                )
                                Text("Partial Pay", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Amount Field
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            amountText = input
                            if (isFullPayment && input.toDoubleOrNull() != currentItem.dueAmount) {
                                isFullPayment = false
                            }
                        },
                        label = { Text("Amount to Pay (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Carry-Forward Live Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Remaining Unpaid Balance:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                                Text("₹${remainingBalance.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF1D4ED8))
                            }
                            if (remainingBalance > 0.0) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "✓ This unpaid balance automatically carries over and will be added to next month's tuition fee.",
                                    fontSize = 10.sp,
                                    color = Color(0xFF2563EB),
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Fee Month & Date Fields
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = feeMonthInput,
                            onValueChange = { feeMonthInput = it },
                            label = { Text("Fee Month") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = paymentDateInput,
                            onValueChange = { paymentDateInput = it },
                            label = { Text("Pay Date") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            val payVal = amountText.toDoubleOrNull() ?: 0.0
                            if (payVal > 0.0 && !isSubmitting) {
                                isSubmitting = true
                                scope.launch {
                                    try {
                                        val recordId = System.currentTimeMillis()
                                        val newRecord = FeeRecordEntity(
                                            id = 0L,
                                            schoolCode = schoolCode,
                                            studentId = student.id,
                                            studentName = student.name,
                                            gradeClass = student.gradeClass,
                                            amountPaid = payVal,
                                            paymentDate = paymentDateInput.trim(),
                                            feeMonth = feeMonthInput.trim(),
                                            paymentMode = paymentMode,
                                            remarks = if (payVal < currentItem.dueAmount) {
                                                "Partial fee payment. Remaining ₹${(currentItem.dueAmount - payVal).toInt()} carried over."
                                            } else {
                                                "Full fee payment settlement"
                                            }
                                        )
                                        val savedId = database.feeDao().insertFeeRecord(newRecord)
                                        try {
                                            val toPush = if (savedId > 0L) newRecord.copy(id = savedId) else newRecord
                                            FirestoreSyncService(database).pushFeeRecord(schoolCode, toPush)
                                        } catch (_: Exception) {}
                                        studentForPayment = null
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isSubmitting = false
                                    }
                                }
                            }
                        },
                        enabled = !isSubmitting && (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            if (isSubmitting) "Processing..." else "Confirm & Collect ₹${enteredVal.toInt()}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
