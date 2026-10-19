package com.school.manage.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.school.manage.core.database.entity.ExpenseEntity
import com.school.manage.core.database.entity.StaffEntity
import com.school.manage.core.firebase.FirestoreSyncService
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import com.school.manage.core.utils.ProfileAvatar
import com.school.manage.core.utils.ContactQuickActions
import com.school.manage.core.utils.rememberPhotoPickerLauncher
import com.school.manage.presentation.theme.LocalSchoolColors
import kotlinx.coroutines.launch



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStaffScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var photoBase64 by remember { mutableStateOf("") }
    val pickStaffPhoto = rememberPhotoPickerLauncher { photoBase64 = it }
    var startDate by remember { mutableStateOf("02/10/2026") }
    var showStartDatePicker by remember { mutableStateOf(false) }

    var gender by remember { mutableStateOf("Male") }
    var whatsapp by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var qualification by remember { mutableStateOf("") }

    var position by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }

    var salaryType by remember { mutableStateOf("Monthly") }
    var showSalaryTypeMenu by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("Add Teacher / Staff", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgApp)
            )
        },
        bottomBar = {
            Surface(
                color = colors.bgApp,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Button(
                    onClick = {
                        if (name.isBlank() || phone.isBlank()) {
                            errorMsg = "Staff Name and Mobile Number are required!"
                            return@Button
                        }
                        scope.launch {
                            database.staffDao().insertStaff(
                                StaffEntity(
                                    schoolCode = schoolCode,
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    joinDate = startDate,
                                    gender = gender,
                                    whatsapp = whatsapp.trim(),
                                    address = address.trim(),
                                    qualification = qualification.trim(),
                                    role = position.ifBlank { "Teacher" },
                                    salary = salary.toDoubleOrNull() ?: 0.0,
                                    salaryType = salaryType,
                                    password = phone.trim(),
                                    photoBase64 = photoBase64
                                )
                            )
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary)
                ) {
                    Text("Save Teacher", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.bgCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Profile photo", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                            Text("This image will be displayed on Profile", fontSize = 12.sp, color = colors.textSecondary)
                        }
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(colors.borderCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👤", fontSize = 24.sp)
                        }
                    }
                }
            }

            item {
                Column {
                    Text("Staff Information", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                    Text("Enter staff personal details here.", fontSize = 12.sp, color = colors.textSecondary)
                }
            }

                        item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.bgCard,
                    border = BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier.fillMaxWidth().clickable { pickStaffPhoto() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ProfileAvatar(
                            photoBase64 = photoBase64,
                            name = name,
                            fallbackEmoji = "👨‍🏫",
                            size = 56.dp,
                            cornerRadius = 14.dp,
                            showEditBadge = true
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Staff / Teacher Photo", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                            Text(if (photoBase64.isEmpty()) "Tap to upload photo" else "Photo attached successfully", fontSize = 12.sp, color = colors.brandPrimary)
                        }
                    }
                }
            }

            item { CustomRoundedInput(value = name, onValueChange = { name = it }, placeholder = "Enter Name") }

            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.inputBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.inputBorder),
                    modifier = Modifier.fillMaxWidth().clickable { showStartDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Start Date: " + startDate, fontSize = 14.sp, color = colors.textPrimary)
                        Text("📅", fontSize = 18.sp)
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GenderOptionButton(
                        label = "Male",
                        icon = "♂",
                        isSelected = gender == "Male",
                        onClick = { gender = "Male" }
                    )
                    GenderOptionButton(
                        label = "Female",
                        icon = "♀",
                        isSelected = gender == "Female",
                        onClick = { gender = "Female" }
                    )
                }
            }

            item {
                Column {
                    Text("Contact Information", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                    Text("Enter contact details here.", fontSize = 12.sp, color = colors.textSecondary)
                }
            }

            item { CustomRoundedInput(value = whatsapp, onValueChange = { whatsapp = it }, placeholder = "WhatsApp") }
            item { CustomRoundedInput(value = phone, onValueChange = { phone = it }, placeholder = "Mobile number (Acts as login password)") }
            item { CustomRoundedInput(value = address, onValueChange = { address = it }, placeholder = "Enter Address") }

            item {
                Column {
                    Text("Qualification And Salary", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                    Text("Enter Qualification details here.", fontSize = 12.sp, color = colors.textSecondary)
                }
            }

            item { CustomRoundedInput(value = qualification, onValueChange = { qualification = it }, placeholder = "Enter Qualification") }
            item { CustomRoundedInput(value = position, onValueChange = { position = it }, placeholder = "Enter Position (e.g. Math Teacher)") }
            item { CustomRoundedInput(value = salary, onValueChange = { salary = it }, placeholder = "Enter Salary (e.g. 15000)") }

            item {
                Box {
                    DropdownTriggerInput(
                        label = "Select Salary Frequency",
                        value = salaryType,
                        onClick = { showSalaryTypeMenu = true }
                    )
                    DropdownMenu(
                        expanded = showSalaryTypeMenu,
                        onDismissRequest = { showSalaryTypeMenu = false }
                    ) {
                        listOf("Monthly", "Annual", "Weekly", "Daily").forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st) },
                                onClick = { salaryType = st; showSalaryTypeMenu = false }
                            )
                        }
                    }
                }
            }

            if (errorMsg.isNotEmpty()) {
                item {
                    Text(errorMsg, color = colors.error, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }

        if (showStartDatePicker) {
            ModernDatePickerDialog(
                currentDate = startDate,
                onDateSelected = { startDate = it },
                onDismiss = { showStartDatePicker = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val staffList by database.staffDao().getStaffBySchool(schoolCode).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var selectedTeacherForEdit by remember { mutableStateOf<StaffEntity?>(null) }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Staff & Faculty Directory (${staffList.size})", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = colors.textPrimary)
                        Text("Tap any teacher card to view & update profile", fontSize = 11.sp, color = colors.textSecondary)
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
        if (staffList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No teachers or staff added yet.", color = colors.textSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(staffList) { staff ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.bgCard,
                        border = BorderStroke(1.dp, colors.borderCard),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTeacherForEdit = staff }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    ProfileAvatar(
                                        photoBase64 = staff.photoBase64,
                                        name = staff.name,
                                        fallbackEmoji = "👨‍🏫",
                                        size = 54.dp,
                                        cornerRadius = 14.dp,
                                        showEditBadge = true,
                                        onClick = { selectedTeacherForEdit = staff }
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            text = staff.name,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            color = colors.textPrimary
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = colors.brandPrimary.copy(alpha = 0.14f)
                                            ) {
                                                Text(
                                                    text = staff.role.ifBlank { "Teacher" },
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.brandPrimary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                            if (staff.qualification.isNotBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = colors.borderCard.copy(alpha = 0.6f)
                                                ) {
                                                    Text(
                                                        text = "🎓 ${staff.qualification}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = colors.textSecondary,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.success.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = "₹${staff.salary.toInt()}/${staff.salaryType.take(2).lowercase()}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.success,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = colors.borderCard.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "📱 Phone: ${staff.phone.ifEmpty { "N/A" }}   •   📅 Joined: ${staff.joinDate.ifEmpty { "N/A" }}   •   🚻 ${staff.gender}",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                            if (staff.address.isNotBlank()) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "📍 Address: ${staff.address}",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ContactQuickActions(
                                    phone = staff.phone,
                                    whatsapp = staff.whatsapp.ifBlank { staff.phone },
                                    messageText = "Hello ${staff.name},",
                                    profileAction = { selectedTeacherForEdit = staff },
                                    removeAction = { scope.launch { FirestoreSyncService(database).deleteStaffRecord(schoolCode, staff) } }
                                )

                            }
                        }
                    }
                }
            }
        }
    }

    // Teacher Profile & Update Modal
    if (selectedTeacherForEdit != null) {
        val teacher = selectedTeacherForEdit!!
        var editPhoto by remember(teacher) { mutableStateOf(teacher.photoBase64) }
        val pickTeacherPhoto = rememberPhotoPickerLauncher { editPhoto = it }

        var editName by remember(teacher) { mutableStateOf(teacher.name) }
        var editRole by remember(teacher) { mutableStateOf(teacher.role) }
        var editQual by remember(teacher) { mutableStateOf(teacher.qualification) }
        var editPhone by remember(teacher) { mutableStateOf(teacher.phone) }
        var editWhatsapp by remember(teacher) { mutableStateOf(teacher.whatsapp.ifBlank { teacher.phone }) }
        var editSalary by remember(teacher) { mutableStateOf(teacher.salary.toInt().toString()) }
        var editSalaryType by remember(teacher) { mutableStateOf(teacher.salaryType.ifBlank { "Monthly" }) }
        var editJoinDate by remember(teacher) { mutableStateOf(teacher.joinDate) }
        var editGender by remember(teacher) { mutableStateOf(teacher.gender.ifBlank { "Male" }) }
        var editAddress by remember(teacher) { mutableStateOf(teacher.address) }
        var editPassword by remember(teacher) { mutableStateOf(teacher.password.ifBlank { teacher.phone }) }

        Dialog(onDismissRequest = { selectedTeacherForEdit = null }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = colors.bgCard,
                border = BorderStroke(1.dp, colors.borderCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        color = colors.brandPrimary.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Teacher / Staff Profile", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = colors.brandPrimary)
                                TextButton(onClick = { selectedTeacherForEdit = null }) {
                                    Text("✕ Close", fontWeight = FontWeight.Bold, color = colors.textSecondary)
                                }
                            }
                            ProfileAvatar(
                                photoBase64 = editPhoto,
                                name = editName,
                                fallbackEmoji = "👨‍🏫",
                                size = 82.dp,
                                showEditBadge = true,
                                onClick = { pickTeacherPhoto() }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap photo to upload / change picture",
                                fontSize = 11.sp,
                                color = colors.brandPrimary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { pickTeacherPhoto() }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ContactQuickActions(
                                phone = editPhone,
                                whatsapp = editWhatsapp,
                                messageText = "Hello $editName,"
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Teacher Full Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editRole,
                                onValueChange = { editRole = it },
                                label = { Text("Role / Designation") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = editQual,
                                onValueChange = { editQual = it },
                                label = { Text("Qualification") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editPhone,
                                onValueChange = { editPhone = it },
                                label = { Text("Mobile Number") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = editWhatsapp,
                                onValueChange = { editWhatsapp = it },
                                label = { Text("WhatsApp Number") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editSalary,
                                onValueChange = { editSalary = it },
                                label = { Text("Salary (₹)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = editSalaryType,
                                onValueChange = { editSalaryType = it },
                                label = { Text("Frequency (Monthly)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editJoinDate,
                                onValueChange = { editJoinDate = it },
                                label = { Text("Joining Date") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = editGender,
                                onValueChange = { editGender = it },
                                label = { Text("Gender") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        OutlinedTextField(
                            value = editPassword,
                            onValueChange = { editPassword = it },
                            label = { Text("Staff Portal Login Password") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = editAddress,
                            onValueChange = { editAddress = it },
                            label = { Text("Residential Address") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Surface(
                        color = colors.bgCard,
                        border = BorderStroke(1.dp, colors.borderCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { selectedTeacherForEdit = null },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    if (editName.isNotBlank()) {
                                        scope.launch {
                                            val updatedStaff = teacher.copy(
                                                name = editName.trim(),
                                                role = editRole.trim().ifEmpty { "Teacher" },
                                                qualification = editQual.trim(),
                                                phone = editPhone.trim(),
                                                whatsapp = editWhatsapp.trim().ifEmpty { editPhone.trim() },
                                                salary = editSalary.toDoubleOrNull() ?: teacher.salary,
                                                salaryType = editSalaryType.trim().ifEmpty { "Monthly" },
                                                joinDate = editJoinDate.trim(),
                                                gender = editGender.trim().ifEmpty { "Male" },
                                                address = editAddress.trim(),
                                                password = editPassword.trim().ifEmpty { editPhone.trim() },
                                                photoBase64 = editPhoto
                                            )
                                            database.staffDao().insertStaff(updatedStaff)
                                            selectedTeacherForEdit = null
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1.5f).height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary)
                            ) {
                                Text("💾 Update Profile", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val expenses by database.expenseDao().getExpensesBySchool(schoolCode).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("Expenses (" + schoolCode + ")", color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgApp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("Add Expense", fontWeight = FontWeight.Bold, color = colors.textPrimary) }
            item { CustomRoundedInput(value = title, onValueChange = { title = it }, placeholder = "Title") }
            item { CustomRoundedInput(value = category, onValueChange = { category = it }, placeholder = "Category") }
            item { CustomRoundedInput(value = amount, onValueChange = { amount = it }, placeholder = "Amount (₹)") }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                    onClick = {
                        if (title.isNotBlank() && amount.isNotBlank()) {
                            scope.launch {
                                database.expenseDao().insertExpense(
                                    ExpenseEntity(
                                        schoolCode = schoolCode,
                                        title = title,
                                        category = category,
                                        amount = amount.toDoubleOrNull() ?: 0.0,
                                        date = "02/10/2026",
                                        notes = ""
                                    )
                                )
                                title = ""
                                category = ""
                                amount = ""
                            }
                        }
                    }
                ) { Text("Save Expense", fontWeight = FontWeight.Bold) }
            }
            items(expenses) { exp ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.bgCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(exp.title, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Text("₹" + exp.amount.toInt(), color = colors.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
