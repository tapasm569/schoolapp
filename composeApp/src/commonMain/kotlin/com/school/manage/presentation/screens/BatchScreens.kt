package com.school.manage.presentation.screens
import com.school.manage.core.firebase.FirestoreSyncService

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.window.Dialog
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.BatchEntity
import com.school.manage.presentation.theme.LocalSchoolColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchListScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit,
    onNavigateAdd: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val batches by database.batchDao().getBatchesBySchool(schoolCode).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = colors.bgApp,
        topBar = {
            TopAppBar(
                title = { Text("School Classes (" + batches.size + ")", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary) }
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(batches) { batch ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.bgCard,
                        border = BorderStroke(1.dp, colors.borderCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(batch.batchName, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = colors.brandPrimary)
                                Button(
                                    onClick = { scope.launch { database.batchDao().deleteBatch(batch) } },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.error.copy(alpha = 0.15f), contentColor = colors.error),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Delete", fontSize = 11.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Subjects: " + batch.subjects.ifEmpty { "None" }, fontSize = 12.sp, color = colors.textPrimary)
                            Text("Sections: " + batch.sections.ifEmpty { "Default" }, fontSize = 12.sp, color = colors.textSecondary)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBatchScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit
) {
    val colors = LocalSchoolColors.current
    val scope = rememberCoroutineScope()
    var batchName by remember { mutableStateOf("") }
    val subjects = remember { listOf("").toMutableStateList().apply { clear() } }
    val sections = remember { listOf("").toMutableStateList().apply { clear() } }

    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var newSubject by remember { mutableStateOf("") }

    var showAddSectionDialog by remember { mutableStateOf(false) }
    var newSection by remember { mutableStateOf("") }

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
                                        sections = sections.joinToString(", ")
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
                        Text("Subjects", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                        Text("Add subjects taught in this batch", fontSize = 12.sp, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (subjects.isEmpty()) {
                            Text("No subjects added yet", fontSize = 13.sp, color = colors.textSecondary)
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (sub in subjects) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = colors.bgCardHover,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Text(sub, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showAddSubjectDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.bgCardHover, contentColor = colors.textPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("+ Add", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                        Text("Sections", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                        Text("Add sections for this class (e.g. A, B, C)", fontSize = 12.sp, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (sections.isEmpty()) {
                            Text("No sections added yet", fontSize = 13.sp, color = colors.textSecondary)
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (sec in sections) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = colors.bgCardHover,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Text(sec, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showAddSectionDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.bgCardHover, contentColor = colors.textPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("+ Add", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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

        if (showAddSectionDialog) {
            Dialog(onDismissRequest = { showAddSectionDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.bgCard,
                    border = BorderStroke(1.dp, colors.borderCard),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Add Section", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
                        CustomRoundedInput(
                            value = newSection,
                            onValueChange = { newSection = it },
                            placeholder = "Section (e.g. Section A)"
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAddSectionDialog = false }) {
                                Text("Cancel", color = colors.textSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newSection.isNotBlank()) {
                                        sections.add(newSection.trim())
                                        newSection = ""
                                        showAddSectionDialog = false
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
