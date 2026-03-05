package com.sb.courselist.ui.screen

import android.app.DatePickerDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ImportScreen(
    uiState: CourseListUiState,
    onImportSelected: (Uri, String?) -> Unit,
    onSavePreview: (Long?) -> Unit,
    onUpdatePreviewCourse: (CourseEntry) -> Unit,
    onClearPreview: () -> Unit,
) {
    val context = LocalContext.current
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()) }
    var termStartEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingCourse by remember { mutableStateOf<CourseEntry?>(null) }

    val preview = uiState.previewSchedule
    LaunchedEffect(preview?.meta?.termStartEpochDay) {
        val importedTermStart = preview?.meta?.termStartEpochDay ?: 0L
        if (termStartEpochDay == null && importedTermStart > 0L) {
            termStartEpochDay = importedTermStart
        }
    }

    fun showTermStartDateDialog() {
        val initial = termStartEpochDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                termStartEpochDay = LocalDate.of(year, month + 1, day).toEpochDay()
            },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth,
        ).show()
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(MintBg, Color(0xFFF6FBFF)))),
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
                termStartDateText = termStartEpochDay?.let { LocalDate.ofEpochDay(it).format(dateFormatter) },
                onPickTermStartDate = ::showTermStartDateDialog,
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
                                text = " 正在解析课表...",
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
                            text = "识别提示",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        uiState.importIssues.forEach { issue ->
                            IssueItem(issue)
                        }
                    }
                }
            }

            if (preview == null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "暂无预览",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "先导入 PDF 识别课程，再设置开学日期并保存。",
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
                            text = "预览（可编辑）",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (termStartEpochDay == null) {
                                    "开学日期：未设置"
                                } else {
                                    "开学日期：${LocalDate.ofEpochDay(termStartEpochDay!!).format(dateFormatter)}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (termStartEpochDay == null) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            TextButton(
                                onClick = ::showTermStartDateDialog,
                                enabled = !uiState.isSaving,
                            ) {
                                Text("设置开学日期")
                            }
                        }

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
                                onClick = { onSavePreview(termStartEpochDay) },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isSaving && termStartEpochDay != null,
                            ) {
                                Icon(Icons.Rounded.Save, contentDescription = null)
                                Text(" 保存")
                            }
                            Button(
                                onClick = onClearPreview,
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isSaving,
                            ) {
                                Text("清空")
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
    termStartDateText: String?,
    onPickTermStartDate: () -> Unit,
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
                        text = "导入课表 PDF",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "先识别课程，再设置开学日期并保存。",
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
                Text(" 选择 PDF")
            }

            ElevatedButton(
                onClick = onPickTermStartDate,
                enabled = !isParsing,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = if (termStartDateText == null) "设置开学日期（可稍后）" else "开学日期：$termStartDateText",
                )
            }

            if (!fileName.isNullOrBlank()) {
                Text(
                    text = "当前文件：$fileName",
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
                text = "星期${course.dayOfWeek}  第${course.startPeriod}-${course.endPeriod}节",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "地点：${course.location.ifBlank { "待补充" }}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "教师：${course.teacher.ifBlank { "待补充" }}",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (course.note.isNotBlank()) {
                Text(
                    text = "备注：${course.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Rounded.Edit, contentDescription = "编辑")
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
    var note by remember(course.id) { mutableStateOf(course.note) }
    var weekPattern by remember(course.id) { mutableStateOf(course.weekPattern) }
    var day by remember(course.id) { mutableStateOf(course.dayOfWeek.toString()) }
    var start by remember(course.id) { mutableStateOf(course.startPeriod.toString()) }
    var end by remember(course.id) { mutableStateOf(course.endPeriod.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑课程") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("课程名称") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("上课地点") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("任课教师") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注") },
                    minLines = 2,
                    maxLines = 4,
                )
                OutlinedTextField(
                    value = weekPattern,
                    onValueChange = { weekPattern = it },
                    label = { Text("周次（如 1-16周）") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = day,
                        onValueChange = { day = it },
                        label = { Text("星期(1-7)") },
                        modifier = Modifier.width(110.dp),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = { Text("开始节") },
                        modifier = Modifier.width(110.dp),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        label = { Text("结束节") },
                        modifier = Modifier.width(110.dp),
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
                            location = location.trim(),
                            teacher = teacher.trim(),
                            note = note.trim(),
                            weekPattern = weekPattern.ifBlank { course.weekPattern },
                            dayOfWeek = dayValue,
                            startPeriod = startValue,
                            endPeriod = endValue,
                        ),
                    )
                },
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
