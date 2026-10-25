cd ~/school-management-app && python3 - << 'EOF'
import os

# ==============================================================================
# 1. CREATE FeeDuesCalculator.kt (Dynamic Billing Timing Engine)
# ==============================================================================
utils_dir = "composeApp/src/commonMain/kotlin/com/school/manage/core/utils"
os.makedirs(utils_dir, exist_ok=True)

dues_calc_code = """package com.school.manage.core.utils

data class StudentDueRecord(
    val studentId: String,
    val studentName: String,
    val gradeClass: String,
    val parentPhone: String,
    val unpaidMonths: List<String>,
    val totalDueAmount: Double
)

object FeeDuesCalculator {

    /**
     * Calculates unpaid tuition months based on the school's billing policy:
     * - BEGINNING_OF_MONTH: The active month is due immediately on day 1.
     * - AFTER_MONTH_COMPLETE: Only past, concluded months are marked overdue.
     */
    fun calculateOverdue(
        studentId: String,
        studentName: String,
        gradeClass: String,
        parentPhone: String,
        monthlyFee: Double,
        admissionDate: String?, // Format: YYYY-MM-DD or DD/MM/YYYY
        paidMonths: List<String>, // e.g. ["10/2026", "09/2026"]
        billingTiming: String,    // "BEGINNING_OF_MONTH" or "AFTER_MONTH_COMPLETE"
        currentYear: Int = 2026,
        currentMonth: Int = 10
    ): StudentDueRecord? {
        // Parse start year & month from admission date or default to current year
        var startYear = currentYear
        var startMonth = 1
        
        if (!admissionDate.isNullOrBlank()) {
            if (admissionDate.contains("-")) {
                val parts = admissionDate.split("-")
                if (parts.size >= 2) {
                    startYear = parts[0].toIntOrNull() ?: currentYear
                    startMonth = parts[1].toIntOrNull() ?: 1
                }
            } else if (admissionDate.contains("/")) {
                val parts = admissionDate.split("/")
                if (parts.size >= 3) {
                    startYear = parts[2].toIntOrNull() ?: currentYear
                    startMonth = parts[1].toIntOrNull() ?: 1
                }
            }
        }

        // Determine cutoff month based on policy
        val (endYear, endMonth) = if (billingTiming == "AFTER_MONTH_COMPLETE") {
            if (currentMonth == 1) Pair(currentYear - 1, 12) else Pair(currentYear, currentMonth - 1)
        } else {
            Pair(currentYear, currentMonth) // Advance billing includes current month
        }

        if (startYear > endYear || (startYear == endYear && startMonth > endMonth)) {
            return null
        }

        // Generate required billing months
        val requiredMonths = mutableListOf<String>()
        var y = startYear
        var m = startMonth

        while (y < endYear || (y == endYear && m <= endMonth)) {
            requiredMonths.add("${m.toString().padStart(2, '0')}/$y")
            m++
            if (m > 12) {
                m = 1
                y++
            }
        }

        val normalizedPaid = paidMonths.map { it.trim() }
        val unpaid = requiredMonths.filter { req -> !normalizedPaid.contains(req) }

        return if (unpaid.isNotEmpty()) {
            StudentDueRecord(
                studentId = studentId,
                studentName = studentName,
                gradeClass = gradeClass,
                parentPhone = parentPhone,
                unpaidMonths = unpaid,
                totalDueAmount = unpaid.size * monthlyFee
            )
        } else {
            null
        }
    }
}
"""

with open(f"{utils_dir}/FeeDuesCalculator.kt", "w", encoding="utf-8") as f:
    f.write(dues_calc_code)
print("✓ Created FeeDuesCalculator.kt")


# ==============================================================================
# 2. CREATE SchoolSetupWizardScreen.kt (4-Step Sequential Admin Setup)
# ==============================================================================
screens_dir = "composeApp/src/commonMain/kotlin/com/school/manage/presentation/screens"
os.makedirs(screens_dir, exist_ok=True)

wizard_screen_code = """package com.school.manage.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.launch

@Composable
fun SchoolSetupWizardScreen(
    schoolCode: String,
    onSetupCompleted: () -> Unit
) {
    var currentStep by remember { mutableStateOf(1) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Step 1: Classes state
    var className by remember { mutableStateOf("") }
    var classSection by remember { mutableStateOf("A") }
    val createdClasses = remember { mutableStateListOf<String>() }

    // Step 2: Subjects state
    var subjectName by remember { mutableStateOf("") }
    val createdSubjects = remember { mutableStateListOf<String>() }

    // Step 3: Fee Policy state
    var admissionFee by remember { mutableStateOf("1500") }
    var monthlyFee by remember { mutableStateOf("500") }
    var feeTiming by remember { mutableStateOf("BEGINNING_OF_MONTH") }

    // Step 4: Teacher state
    var teacherName by remember { mutableStateOf("") }
    var teacherPhone by remember { mutableStateOf("") }
    var teacherRole by remember { mutableStateOf("Teacher") }
    val createdTeachers = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stepper Header
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "STEP $currentStep OF 4: " + when (currentStep) {
                        1 -> "SETUP CLASSES"
                        2 -> "CURRICULUM SUBJECTS"
                        3 -> "ADMISSION & FEE POLICY"
                        else -> "ADD TEACHERS"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1D4ED8)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { currentStep / 4f },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Color(0xFF1D4ED8),
                    trackColor = Color(0xFFE2E8F0)
                )
            }
        }

        // STEP 1: CLASS SETUP
        if (currentStep == 1) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("1. Create School Classes", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text("Define classes before configuring subjects.", fontSize = 12.sp, color = Color.Gray)

                    OutlinedTextField(
                        value = className,
                        onValueChange = { className = it },
                        label = { Text("Class Name (e.g. Class 1)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = classSection,
                        onValueChange = { classSection = it },
                        label = { Text("Section (e.g. A)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (className.isNotBlank()) {
                                val item = "$className ($classSection)"
                                createdClasses.add(item)
                                val id = className.replace(" ", "_")
                                scope.launch {
                                    try {
                                        Firebase.database.reference("schools")
                                            .child(schoolCode).child("batches").child(id)
                                            .setValue(mapOf("id" to id, "name" to className, "section" to classSection))
                                    } catch (_: Exception) {}
                                }
                                className = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ Add Class", fontWeight = FontWeight.Bold)
                    }

                    if (createdClasses.isNotEmpty()) {
                        Text("Active Classes: ${createdClasses.joinToString(", ")}", fontSize = 12.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { currentStep = 2 },
                        enabled = createdClasses.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Text("Next: Setup Subjects ➔", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // STEP 2: SUBJECT SETUP
        if (currentStep == 2) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("2. Add School Subjects", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text("Register subjects taught across classes.", fontSize = 12.sp, color = Color.Gray)

                    OutlinedTextField(
                        value = subjectName,
                        onValueChange = { subjectName = it },
                        label = { Text("Subject (e.g. Mathematics)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (subjectName.isNotBlank()) {
                                createdSubjects.add(subjectName)
                                val id = subjectName.replace(" ", "_")
                                scope.launch {
                                    try {
                                        Firebase.database.reference("schools")
                                            .child(schoolCode).child("subjects").child(id)
                                            .setValue(mapOf("id" to id, "name" to subjectName))
                                    } catch (_: Exception) {}
                                }
                                subjectName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ Add Subject", fontWeight = FontWeight.Bold)
                    }

                    if (createdSubjects.isNotEmpty()) {
                        Text("Subjects: ${createdSubjects.joinToString(", ")}", fontSize = 12.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { currentStep = 3 },
                        enabled = createdSubjects.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Text("Next: Admission & Fee Policy ➔", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // STEP 3: FEE POLICY
        if (currentStep == 3) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("3. Admission & Tuition Policy", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)

                    OutlinedTextField(
                        value = admissionFee,
                        onValueChange = { admissionFee = it },
                        label = { Text("Default Admission Fee (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = monthlyFee,
                        onValueChange = { monthlyFee = it },
                        label = { Text("Default Monthly Fee (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("When are monthly tuition dues generated?", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)).padding(8.dp)
                    ) {
                        RadioButton(
                            selected = feeTiming == "BEGINNING_OF_MONTH",
                            onClick = { feeTiming = "BEGINNING_OF_MONTH" }
                        )
                        Column {
                            Text("Beginning of Month", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Fees due on 1st of month. Unpaid students show in Dues immediately.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)).padding(8.dp)
                    ) {
                        RadioButton(
                            selected = feeTiming == "AFTER_MONTH_COMPLETE",
                            onClick = { feeTiming = "AFTER_MONTH_COMPLETE" }
                        )
                        Column {
                            Text("After Month Complete", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Active month is not overdue. Dues trigger only after month ends.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    Firebase.database.reference("schools").child(schoolCode).child("info").updateChildren(
                                        mapOf(
                                            "defaultAdmissionFee" to (admissionFee.toDoubleOrNull() ?: 1500.0),
                                            "defaultMonthlyFee" to (monthlyFee.toDoubleOrNull() ?: 500.0),
                                            "feeBillingTiming" to feeTiming
                                        )
                                    )
                                } catch (_: Exception) {}
                            }
                            currentStep = 4
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Text("Next: Add Teachers ➔", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // STEP 4: TEACHER SETUP & FINISH
        if (currentStep == 4) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("4. Register Teaching Staff", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text("Add at least one teacher to complete initial setup.", fontSize = 12.sp, color = Color.Gray)

                    OutlinedTextField(
                        value = teacherName,
                        onValueChange = { teacherName = it },
                        label = { Text("Teacher Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = teacherPhone,
                        onValueChange = { teacherPhone = it },
                        label = { Text("Mobile Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = teacherRole,
                        onValueChange = { teacherRole = it },
                        label = { Text("Role / Subject (e.g. Mathematics)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (teacherName.isNotBlank() && teacherPhone.isNotBlank()) {
                                createdTeachers.add(teacherName)
                                val id = teacherPhone.trim()
                                scope.launch {
                                    try {
                                        Firebase.database.reference("schools")
                                            .child(schoolCode).child("staff").child(id)
                                            .setValue(mapOf(
                                                "id" to id,
                                                "name" to teacherName,
                                                "phone" to teacherPhone,
                                                "role" to teacherRole
                                            ))
                                    } catch (_: Exception) {}
                                }
                                teacherName = ""
                                teacherPhone = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ Add Teacher", fontWeight = FontWeight.Bold)
                    }

                    if (createdTeachers.isNotEmpty()) {
                        Text("Faculty: ${createdTeachers.joinToString(", ")}", fontSize = 12.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    Firebase.database.reference("schools").child(schoolCode).child("info").updateChildren(
                                        mapOf("setupCompleted" to true, "setupStep" to 5)
                                    )
                                } catch (_: Exception) {}
                                onSetupCompleted()
                            }
                        },
                        enabled = createdTeachers.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                    ) {
                        Text("✓ Finish School Setup & Open Dashboard", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
"""

with open(f"{screens_dir}/SchoolSetupWizardScreen.kt", "w", encoding="utf-8") as f:
    f.write(wizard_screen_code)
print("✓ Created SchoolSetupWizardScreen.kt")

EOF