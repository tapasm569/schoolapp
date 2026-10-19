@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
package com.school.manage.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.BatchEntity
import com.school.manage.core.firebase.FirestoreSyncService
import com.school.manage.core.utils.ProfileAvatar
import com.school.manage.presentation.theme.LocalSchoolColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BatchListScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit,
    onNavigateAdd: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val batches by database.batchDao().getBatchesBySchool(schoolCode).collectAsState(initial = emptyList())
    val students by database.studentDao().getStudentsBySchool(schoolCode).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var selectedBatchForEdit by remember { mutableStateOf<BatchEntity?>(null) }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("School Classes (${batches.size})", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = colors.textPrimary)
                        Text("Tap any class card to view details & update subjects/fees", fontSize = 11.sp, color = colors.textSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgApp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateAdd() },
                containerColor = colors.brandPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("+", fontSize = 28.sp)
            }
        }
    ) { padding ->
        if (batches.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No classes added yet. Tap + to add.", color = colors.textSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(batches) { batch ->
                    val studentCount = students.count { it.gradeClass.equals(batch.batchName, ignoreCase = true) }
                    val subjectList = batch.subjects.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val sectionList = batch.sections.ifEmpty { "A, B" }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.bgCard,
                        border = BorderStroke(1.dp, colors.borderCard),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedBatchForEdit = batch }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    ProfileAvatar(
                                        photoBase64 = "",
                                        name = batch.batchName,
                                        fallbackEmoji = "🏛️",
                                        size = 48.dp,
                                        cornerRadius = 14.dp
                                    )
                                    Column {
                                        Text(
                                            text = batch.batchName,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp,
                                            color = colors.brandPrimary
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 3.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = colors.brandPrimary.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = "👥 $studentCount Students",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.brandPrimary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = colors.borderCard.copy(alpha = 0.6f)
                                            ) {
                                                Text(
                                                    text = "Sec: $sectionList",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.textSecondary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (batch.defaultMonthlyTuition > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = colors.success.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = "₹${batch.defaultMonthlyTuition.toInt()}/mo",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = colors.success,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = colors.borderCard.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "📚 Assigned Subjects (${subjectList.size}):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            if (subjectList.isEmpty()) {
                                Text("No subjects assigned yet — tap card to configure", fontSize = 12.sp, color = colors.textSecondary)
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    subjectList.forEach { subj ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = colors.brandPrimary.copy(alpha = 0.08f),
                                            border = BorderStroke(1.dp, colors.brandPrimary.copy(alpha = 0.2f))
                                        ) {
                                            Text(
                                                text = subj,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.textPrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.brandPrimary.copy(alpha = 0.12f),
                                    modifier = Modifier
                                        .height(30.dp)
                                        .clickable { selectedBatchForEdit = batch }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                                        Text("✏️ Edit Class", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.brandPrimary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.error.copy(alpha = 0.15f),
                                    modifier = Modifier
                                        .height(30.dp)
                                        .clickable {
                                            scope.launch { database.batchDao().deleteBatch(batch) }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                                        Text("Delete", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Class Details & Update Modal
    if (selectedBatchForEdit != null) {
        val batch = selectedBatchForEdit!!
        var editBatchName by remember(batch) { mutableStateOf(batch.batchName) }
        var editSections by remember(batch) { mutableStateOf(batch.sections.ifEmpty { "A, B" }) }
        var editMonthlyTuition by remember(batch) { mutableStateOf(batch.defaultMonthlyTuition.toInt().toString()) }
        var newCustomSubj by remember(batch) { mutableStateOf("") }

        val selectedSubjs = remember(batch) {
            mutableStateListOf<String>().apply {
                addAll(batch.subjects.split(",").map { it.trim() }.filter { it.isNotEmpty() })
            }
        }
        val defaultPool = listOf(
            "Bengali", "English", "Mathematics", "General Science", "Social Studies",
            "Environmental Awareness", "Computer Studies", "General Knowledge",
            "Hindi", "History", "Geography", "Art & Craft / Activities", "Rhymes & Storytelling"
        )
        val allAvailableSubjs = (defaultPool + selectedSubjs).distinct()

        Dialog(onDismissRequest = { selectedBatchForEdit = null }) {
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
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Class Details & Update", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = colors.brandPrimary)
                                Text("Modify class name, sections, tuition fee & subjects", fontSize = 11.sp, color = colors.textSecondary)
                            }
                            TextButton(onClick = { selectedBatchForEdit = null }) {
                                Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = editBatchName,
                            onValueChange = { editBatchName = it },
                            label = { Text("Class / Batch Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editSections,
                                onValueChange = { editSections = it },
                                label = { Text("Sections (e.g. A, B)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = editMonthlyTuition,
                                onValueChange = { editMonthlyTuition = it },
                                label = { Text("Default Tuition (₹)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Text("Select Subjects Taught:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            allAvailableSubjs.forEach { subj ->
                                val isSel = subj in selectedSubjs
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        if (isSel) selectedSubjs.remove(subj) else selectedSubjs.add(subj)
                                    },
                                    label = { Text(subj, fontSize = 12.sp) }
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newCustomSubj,
                                onValueChange = { newCustomSubj = it },
                                label = { Text("Add Custom Subject") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Button(
                                onClick = {
                                    if (newCustomSubj.isNotBlank()) {
                                        val s = newCustomSubj.trim()
                                        if (s !in selectedSubjs) selectedSubjs.add(s)
                                        newCustomSubj = ""
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary)
                            ) {
                                Text("+ Add")
                            }
                        }
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
                                onClick = { selectedBatchForEdit = null },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    if (editBatchName.isNotBlank()) {
                                        scope.launch {
                                            val updatedBatch = batch.copy(
                                                batchName = editBatchName.trim(),
                                                sections = editSections.trim().ifEmpty { "A, B" },
                                                defaultMonthlyTuition = editMonthlyTuition.toDoubleOrNull() ?: batch.defaultMonthlyTuition,
                                                subjects = selectedSubjs.joinToString(", ")
                                            )
                                            database.batchDao().insertBatch(updatedBatch)
                                            try {
                                                FirestoreSyncService(database).pushAllLocalDataToCloud(schoolCode)
                                            } catch (_: Exception) {}
                                            selectedBatchForEdit = null
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1.5f).height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary)
                            ) {
                                Text("💾 Update Class", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddBatchScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val scope = rememberCoroutineScope()
    var batchName by remember { mutableStateOf("") }
    val subjects = remember { mutableStateListOf<String>() }
    val masterSubjects = remember { mutableStateListOf<String>() }

    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var newSubject by remember { mutableStateOf("") }

    LaunchedEffect(schoolCode) {
        val defaults = listOf(
            "Bengali", "English", "Mathematics", "Science",
            "Geography", "History", "Computer", "Drawing",
            "General Knowledge", "Physical Education"
        )
        val school = database.schoolDao().getSchoolByCode(schoolCode)
        val saved = school?.masterSubjects
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            .orEmpty()
        masterSubjects.clear()
        masterSubjects.addAll(saved.ifEmpty { defaults })
    }

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("Add Batch / Class", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary) },
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
                        if (batchName.isNotBlank()) {
                            scope.launch {
                                database.batchDao().insertBatch(
                                    BatchEntity(
                                        schoolCode = schoolCode,
                                        batchName = batchName.trim(),
                                        subjects = subjects.joinToString(", "),
                                        sections = "A, B"
                                    )
                                )
                                onNavigateBack()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary)
                ) {
                    Text("Save Class", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                    border = BorderStroke(1.dp, colors.borderCard),
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
                            Text("🏛️", fontSize = 24.sp)
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.bgCard,
                    border = BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Batch Information", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                        Text("Enter class/batch name here (e.g. Class 1)", fontSize = 12.sp, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(12.dp))
                        CustomRoundedInput(
                            value = batchName,
                            onValueChange = { batchName = it },
                            placeholder = "Batch name"
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.bgCard,
                    border = BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Subjects",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.textPrimary
                        )
                        Text(
                            "Select subjects taught in this class",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val availableSubjects =
                            (masterSubjects + subjects).distinct()

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            availableSubjects.forEach { subject ->
                                FilterChip(
                                    selected = subject in subjects,
                                    onClick = {
                                        if (subject in subjects) {
                                            subjects.remove(subject)
                                        } else {
                                            subjects.add(subject)
                                        }
                                    },
                                    label = { Text(subject) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showAddSubjectDialog = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+ Add Custom Subject")
                        }
                    }
                }
            }


        }

        if (showAddSubjectDialog) {
            Dialog(onDismissRequest = { showAddSubjectDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.bgCard,
                    border = BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Add Subject", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
                        CustomRoundedInput(
                            value = newSubject,
                            onValueChange = { newSubject = it },
                            placeholder = "Subject Name (e.g. Mathematics)"
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAddSubjectDialog = false }) {
                                Text("Cancel", color = colors.textSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newSubject.isNotBlank()) {
                                        subjects.add(newSubject.trim())
                                        newSubject = ""
                                        showAddSubjectDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary)
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }
            }
        }


    }
}
