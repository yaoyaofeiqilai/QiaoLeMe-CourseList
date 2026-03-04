package com.sb.courselist.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.sb.courselist.domain.model.CourseEntry
import com.sb.courselist.parser.IssueLevel
import com.sb.courselist.parser.ParseIssue
import com.sb.courselist.ui.state.CourseListUiState
import com.sb.courselist.ui.theme.AccentBlue
import com.sb.courselist.ui.theme.AccentCoral
import com.sb.courselist.ui.theme.AccentGreen
import com.sb.courselist.ui.theme.BorderMint
import com.sb.courselist.ui.theme.CardWhite
import com.sb.courselist.ui.theme.MintBg

@Composable
fun ImportScreen(
    uiState: CourseListUiState,
    onImportSelected: (Uri, String?) -> Unit,
    onSavePreview: () -> Unit,
    onUpdatePreviewCourse: (CourseEntry) -> Unit,
    onClearPreview: () -> Unit,
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        val fileName = DocumentFile.fromSingleUri(context, uri)?.name ?: uri.lastPathSegment
        onImportSelected(uri, fileName)
    }

    var editingCourse by remember { mutableStateOf<CourseEntry?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MintBg, Color(0xFFF6FBFF)),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HeroImportCard(
                fileName = uiState.selectedFileName,
                isParsing = uiState.isParsing,
                onSelectPdf = { launcher.launch(arrayOf("application/pdf")) },
            )

            if (uiState.isParsing) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Text(
                                text = " Parsing timetable...",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            if (uiState.importIssues.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Recognition Tips",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        uiState.importIssues.forEach { issue ->
                            IssueItem(issue)
                        }
                    }
                }
            }

            val preview = uiState.previewSchedule
            if (preview == null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "No preview yet",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Import a PDF file to auto-generate courses and review details.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Preview (editable)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        preview.courses.forEachIndexed { index, course ->
                            PreviewCourseCard(
                                course = course,
                                onEdit = { editingCourse = course },
                            )
                            if (index != preview.courses.lastIndex) {
                                HorizontalDivider(color = BorderMint)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ElevatedButton(
                                onClick = onSavePreview,
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isSaving,
                            ) {
                                Icon(Icons.Rounded.Save, contentDescription = null)
                                Text(" Save")
                            }
                            Button(
                                onClick = onClearPreview,
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isSaving,
                            ) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }
        }
    }

    editingCourse?.let { course ->
        EditCourseDialog(
            course = course,
            onDismiss = { editingCourse = null },
            onConfirm = { updated ->
                onUpdatePreviewCourse(updated)
                editingCourse = null
            },
        )
    }
}

@Composable
private fun HeroImportCard(
    fileName: String?,
    isParsing: Boolean,
    onSelectPdf: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFDFFFF5),
                            Color(0xFFE5F3FF),
                            Color(0xFFFFF1E2),
                        ),
                    ),
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.UploadFile,
                        contentDescription = null,
                        tint = AccentBlue,
                    )
                }
                Column {
                    Text(
                        text = "Import Timetable PDF",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "Text parsing first, OCR fallback.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            ElevatedButton(
                onClick = onSelectPdf,
                enabled = !isParsing,
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Rounded.FileOpen, contentDescription = null)
                Text(" Select PDF")
            }
            if (!fileName.isNullOrBlank()) {
                Text(
                    text = "Current file: $fileName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun IssueItem(issue: ParseIssue) {
    val color = when (issue.level) {
        IssueLevel.INFO -> AccentGreen
        IssueLevel.WARNING -> Color(0xFFE8A92A)
        IssueLevel.ERROR -> AccentCoral
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = issue.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PreviewCourseCard(
    course: CourseEntry,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = course.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Day ${course.dayOfWeek}  ${course.startPeriod}-${course.endPeriod}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Room: ${course.location.ifBlank { "unknown" }}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Teacher: ${course.teacher.ifBlank { "unknown" }}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Rounded.Edit, contentDescription = "Edit")
        }
    }
}

@Composable
private fun EditCourseDialog(
    course: CourseEntry,
    onDismiss: () -> Unit,
    onConfirm: (CourseEntry) -> Unit,
) {
    var name by remember(course.id) { mutableStateOf(course.name) }
    var location by remember(course.id) { mutableStateOf(course.location) }
    var teacher by remember(course.id) { mutableStateOf(course.teacher) }
    var day by remember(course.id) { mutableStateOf(course.dayOfWeek.toString()) }
    var start by remember(course.id) { mutableStateOf(course.startPeriod.toString()) }
    var end by remember(course.id) { mutableStateOf(course.endPeriod.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Course") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Course") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Room") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("Teacher") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = day,
                        onValueChange = { day = it },
                        label = { Text("Day(1-7)") },
                        modifier = Modifier.width(120.dp),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = { Text("Start") },
                        modifier = Modifier.width(120.dp),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        label = { Text("End") },
                        modifier = Modifier.width(120.dp),
                        singleLine = true,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val dayValue = day.toIntOrNull()?.coerceIn(1, 7) ?: course.dayOfWeek
                    val startValue = start.toIntOrNull()?.coerceIn(1, 14) ?: course.startPeriod
                    val endValue = end.toIntOrNull()?.coerceIn(startValue, 14) ?: course.endPeriod
                    onConfirm(
                        course.copy(
                            name = name.ifBlank { course.name },
                            location = location,
                            teacher = teacher,
                            dayOfWeek = dayValue,
                            startPeriod = startValue,
                            endPeriod = endValue,
                        ),
                    )
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

