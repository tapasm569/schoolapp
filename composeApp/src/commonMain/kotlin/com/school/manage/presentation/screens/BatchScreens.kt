package com.school.manage.presentation.screens

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
import com.school.manage.core.database.AppDatabase
import com.school.manage.core.database.entity.BatchEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchListScreen(
    database: AppDatabase,
    schoolCode: String,
    onNavigateBack: () -> Unit,
    onNavigateAdd: () -> Unit
) {
    val batches by database.batchDao().getBatchesBySchool(schoolCode).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = { Text("School Classes (${batches.size})", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateAdd,
                containerColor = Color(0xFF0D529C),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("+", fontSize = 28.sp)
            }
        }
    ) { padding ->
        if (batches.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No classes added yet. Tap + to add.", color = Color(0xFF64748B))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(batches) { batch ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(batch.batchName, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF0D529C))
                                Button(
                                    onClick = { scope.launch { database.batchDao().deleteBatch(batch) } },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2), contentColor = Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Delete", fontSize = 11.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Subjects: ${batch.subjects.ifEmpty { "None" }}", fontSize = 12.sp, color = Color(0xFF475569))
                            Text("Sections: ${batch.sections.ifEmpty { "Default" }}", fontSize = 12.sp, color = Color(0xFF64748B))
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
    val scope = rememberCoroutineScope()
    var batchName by remember { mutableStateOf("") }
    val subjects = remember { mutableStateListOf<String>() }
    val sections = remember { mutableStateListOf<String>() }

    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var newSubject by remember { mutableStateOf("") }

    var showAddSectionDialog by remember { mutableStateOf(false) }
    var newSection by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = { Text("Add Batch / Class", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8FAFC))
            )
        },
        bottomBar = {
            Surface(
                color = Color(0xFFF8FAFC),
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D529C))
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
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Profile photo", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                            Text("This image will be displayed on Profile", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6B21A8)),
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
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Batch Information", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                        Text("Enter class/batch name here (e.g. Class 1)", fontSize = 12.sp, color = Color(0xFF64748B))
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
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Subjects", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                        Text("Add subjects taught in this batch", fontSize = 12.sp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(8.dp))

                        if (subjects.isEmpty()) {
                            Text("No subjects added yet", fontSize = 13.sp, color = Color(0xFF94A3B8))
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                subjects.forEach { sub ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Text(sub, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showAddSubjectDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0), contentColor = Color(0xFF0F172A)),
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
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Sections", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                        Text("Add sections for this class (e.g. A, B, C)", fontSize = 12.sp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(8.dp))

                        if (sections.isEmpty()) {
                            Text("No sections added yet", fontSize = 13.sp, color = Color(0xFF94A3B8))
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                sections.forEach { sec ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Text(sec, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showAddSectionDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0), contentColor = Color(0xFF0F172A)),
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
            AlertDialog(
                onDismissRequest = { showAddSubjectDialog = false },
                title = { Text("Add Subject") },
                text = {
                    OutlinedTextField(
                        value = newSubject,
                        onValueChange = { newSubject = it },
                        label = { Text("Subject Name (e.g. Mathematics)") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (newSubject.isNotBlank()) {
                            subjects.add(newSubject.trim())
                            newSubject = ""
                            showAddSubjectDialog = false
                        }
                    }) { Text("Add") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddSubjectDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showAddSectionDialog) {
            AlertDialog(
                onDismissRequest = { showAddSectionDialog = false },
                title = { Text("Add Section") },
                text = {
                    OutlinedTextField(
                        value = newSection,
                        onValueChange = { newSection = it },
                        label = { Text("Section (e.g. Section A)") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (newSection.isNotBlank()) {
                            sections.add(newSection.trim())
                            newSection = ""
                            showAddSectionDialog = false
                        }
                    }) { Text("Add") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddSectionDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
