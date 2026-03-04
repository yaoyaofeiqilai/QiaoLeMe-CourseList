package com.sb.courselist.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sb.courselist.domain.model.CourseEntry
import com.sb.courselist.domain.model.ParsedSchedule
import com.sb.courselist.ui.theme.CardWhite
import com.sb.courselist.ui.theme.CoursePalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimetableScreen(
    schedule: ParsedSchedule?,
    onNavigateImport: () -> Unit,
    onUpdateCourse: (CourseEntry) -> Unit,
) {
    var editingCourse by remember(schedule?.meta?.id) { mutableStateOf<CourseEntry?>(null) }

    if (schedule == null) {
        EmptyTimetable(onNavigateImport = onNavigateImport)
        return
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "My Timetable",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = schedule.meta.termName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Last import: ${formatDate(schedule.meta.importedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        TimetableGrid(
            courses = schedule.courses,
            onEditCourse = { editingCourse = it },
        )
    }

    editingCourse?.let { course ->
        EditPersistedCourseDialog(
            course = course,
            onDismiss = { editingCourse = null },
            onConfirm = { updated ->
                onUpdateCourse(updated)
                editingCourse = null
            },
        )
    }
}

@Composable
private fun EmptyTimetable(
    onNavigateImport: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "No timetable yet",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Import a timetable PDF first.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = onNavigateImport) {
                    Text("Go Import")
                }
            }
        }
    }
}

@Composable
private fun TimetableGrid(
    courses: List<CourseEntry>,
    onEditCourse: (CourseEntry) -> Unit,
) {
    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val periods = (1..12).toList()

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                HeaderCell(
                    text = "P",
                    modifier = Modifier.width(40.dp),
                )
                dayLabels.forEach { label ->
                    HeaderCell(
                        text = label,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            periods.forEach { period ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Cell(
                        modifier = Modifier
                            .width(40.dp)
                            .fillMaxSize(),
                        color = Color(0xFFEFF7FF),
                    ) {
                        Text(
                            text = period.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    (1..7).forEach { day ->
                        val course = courses.firstOrNull { entry ->
                            entry.dayOfWeek == day &&
                                period in entry.startPeriod..entry.endPeriod
                        }

                        val courseColor = course?.let { courseCardColor(it.name) } ?: Color.Transparent
                        val clickableModifier = if (course != null && course.startPeriod == period) {
                            Modifier.clickable { onEditCourse(course) }
                        } else {
                            Modifier
                        }

                        Cell(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .then(clickableModifier),
                            color = if (course == null) Color(0xFFF9FCFF) else courseColor.copy(alpha = 0.85f),
                        ) {
                            if (course == null) return@Cell
                            if (course.startPeriod == period) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(1.dp),
                                ) {
                                    Text(
                                        text = course.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = course.location.ifBlank { "room unknown" },
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = course.teacher.ifBlank { "teacher unknown" },
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                    )
                                }
                            } else {
                                Text(
                                    text = "<->",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE8F6FF)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun Cell(
    modifier: Modifier = Modifier,
    color: Color,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun EditPersistedCourseDialog(
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
        title = { Text("Edit Saved Course") },
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

private fun courseCardColor(name: String): Color {
    val index = kotlin.math.abs(name.hashCode()) % CoursePalette.size
    return CoursePalette[index]
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

