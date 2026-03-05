package com.sb.courselist.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sb.courselist.domain.model.CourseEntry
import com.sb.courselist.domain.model.ParsedSchedule
import com.sb.courselist.ui.theme.ArtHeadlineFamily
import com.sb.courselist.ui.theme.CardTitleFamily
import com.sb.courselist.ui.theme.CardWhite
import com.sb.courselist.ui.theme.PlayfulLabelFamily
import com.sb.courselist.ui.theme.ReadableBodyFamily
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek
import java.util.Date
import java.util.Locale

@Composable
fun TimetableScreen(
    schedule: ParsedSchedule?,
    onNavigateImport: () -> Unit,
    onAddCourse: (CourseEntry) -> Unit,
    onDeleteCourse: (Long) -> Unit,
    onUpdateCourse: (CourseEntry) -> Unit,
) {
    var editingCourse by remember(schedule?.meta?.id) { mutableStateOf<CourseEntry?>(null) }

    if (schedule == null) {
        EmptyTimetable(onNavigateImport = onNavigateImport)
        return
    }

    val totalWeeks = remember(schedule.meta.id, schedule.courses) {
        inferDisplayWeekCount(
            courses = schedule.courses,
            declaredTotalWeeks = schedule.meta.totalWeeks,
        )
    }
    val currentWeek = remember(schedule.meta.id, schedule.meta.termStartEpochDay, totalWeeks) {
        currentWeekFromTermStart(
            termStartEpochDay = schedule.meta.termStartEpochDay,
            totalWeeks = totalWeeks,
        ) ?: defaultCurrentWeek(totalWeeks)
    }
    var selectedWeek by rememberSaveable(schedule.meta.id, schedule.meta.termStartEpochDay, totalWeeks) {
        mutableStateOf(currentWeek)
    }
    var creatingCourse by remember(schedule.meta.id) { mutableStateOf<CourseEntry?>(null) }
    var deletingCourse by remember(schedule.meta.id) { mutableStateOf<CourseEntry?>(null) }

    val visibleCourses = remember(schedule.courses, selectedWeek) {
        filterCoursesByWeek(schedule.courses, selectedWeek)
            .sortedWith(compareBy<CourseEntry> { it.dayOfWeek }.thenBy { it.startPeriod })
    }
    val weekDates = remember(selectedWeek, currentWeek) {
        weekDates(selectedWeek = selectedWeek, currentWeek = currentWeek)
    }
    val periodCount = remember(schedule.courses) {
        (schedule.courses.maxOfOrNull { it.endPeriod } ?: DEFAULT_PERIOD_COUNT).coerceIn(10, 14)
    }
    val todayDayOfWeek = remember { mapToMondayFirst(LocalDate.now().dayOfWeek) }
    val highlightedDay = if (selectedWeek == currentWeek) todayDayOfWeek else null
    val periodTimes = remember(schedule.meta.periodTimes, periodCount) {
        buildPeriodTimeDisplayMap(
            parsed = schedule.meta.periodTimes,
            periodCount = periodCount,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        WeekHeroCard(
            schedule = schedule,
            selectedWeek = selectedWeek,
            totalWeeks = totalWeeks,
            currentWeek = currentWeek,
            visibleCount = visibleCourses.size,
            termStartEpochDay = schedule.meta.termStartEpochDay,
            onSelectWeek = { selectedWeek = it.coerceIn(1, totalWeeks) },
            onAddManualCourse = {
                creatingCourse = buildManualCourseDraft(
                    scheduleId = schedule.meta.id,
                    selectedWeek = selectedWeek,
                    defaultDayOfWeek = highlightedDay ?: 1,
                )
            },
        )

        WeekPickerRow(
            totalWeeks = totalWeeks,
            selectedWeek = selectedWeek,
            onSelectWeek = { selectedWeek = it },
        )

        if (visibleCourses.isEmpty()) {
            EmptyWeekCard(selectedWeek = selectedWeek)
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val dayGap = 8.dp
                val timeColumnWidth = 50.dp
                val minDayColumnWidth = 94.dp
                val adaptiveDayWidth = ((maxWidth - timeColumnWidth - dayGap * 7) / 7).coerceAtLeast(minDayColumnWidth)
                val tableWidth = timeColumnWidth + adaptiveDayWidth * 7 + dayGap * 7
                val needHorizontalScroll = tableWidth > maxWidth
                val wrapperModifier = if (needHorizontalScroll) {
                    Modifier.horizontalScroll(rememberScrollState())
                } else {
                    Modifier
                }

                Box(modifier = wrapperModifier.fillMaxWidth()) {
                    WeeklyTimetableGrid(
                        courses = visibleCourses,
                        weekDates = weekDates,
                        periodCount = periodCount,
                        highlightedDay = highlightedDay,
                        periodTimes = periodTimes,
                        timeColumnWidth = timeColumnWidth,
                        dayColumnWidth = adaptiveDayWidth,
                        dayColumnGap = dayGap,
                        modifier = Modifier.width(tableWidth),
                        onEditCourse = { editingCourse = it },
                    )
                }
            }
        }
    }

    editingCourse?.let { course ->
        EditPersistedCourseDialog(
            course = course,
            showDeleteAction = true,
            onDismiss = { editingCourse = null },
            onRequestDelete = {
                deletingCourse = course
                editingCourse = null
            },
            onConfirm = { updated ->
                onUpdateCourse(updated)
                editingCourse = null
            },
        )
    }

    creatingCourse?.let { draft ->
        EditPersistedCourseDialog(
            course = draft,
            dialogTitle = "新增活动",
            confirmLabel = "添加",
            onDismiss = { creatingCourse = null },
            onConfirm = { created ->
                onAddCourse(
                    created.copy(
                        id = 0L,
                        scheduleId = schedule.meta.id,
                        rawText = "manual-edit",
                        sourceConfidence = 1f,
                    ),
                )
                creatingCourse = null
            },
        )
    }

    deletingCourse?.let { course ->
        DeleteWeekCourseDialog(
            courseName = course.name,
            selectedWeek = selectedWeek,
            onDismiss = { deletingCourse = null },
            onConfirmDelete = {
                val updatedWeekPattern = removeWeekFromPattern(
                    weekPattern = course.weekPattern,
                    weekToRemove = selectedWeek,
                    totalWeeks = totalWeeks,
                )
                if (updatedWeekPattern == null) {
                    onDeleteCourse(course.id)
                } else {
                    onUpdateCourse(course.copy(weekPattern = updatedWeekPattern))
                }
                deletingCourse = null
            },
        )
    }
}

@Composable
private fun WeekHeroCard(
    schedule: ParsedSchedule,
    selectedWeek: Int,
    totalWeeks: Int,
    currentWeek: Int,
    visibleCount: Int,
    termStartEpochDay: Long,
    onSelectWeek: (Int) -> Unit,
    onAddManualCourse: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE8FFF8),
                            Color(0xFFEAF4FF),
                            Color(0xFFFFF0E7),
                        ),
                    ),
                )
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 72.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = displayTermName(schedule.meta.termName),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = ArtHeadlineFamily,
                    ),
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SummaryChip(label = "第${selectedWeek}周")
                    SummaryChip(label = "共${totalWeeks}周")
                    SummaryChip(label = "本周${visibleCount}门课")
                }
                Text(
                    text = "最近导入：${formatDate(schedule.meta.importedAt)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = ReadableBodyFamily,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (termStartEpochDay > 0L) {
                    Text(
                        text = "开学日期：${formatEpochDay(termStartEpochDay)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = ReadableBodyFamily,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { onSelectWeek(selectedWeek - 1) },
                        enabled = selectedWeek > 1,
                    ) {
                        Text("上一周")
                    }
                    TextButton(
                        onClick = { onSelectWeek(selectedWeek + 1) },
                        enabled = selectedWeek < totalWeeks,
                    ) {
                        Text("下一周")
                    }
                    FilledTonalButton(
                        onClick = { onSelectWeek(currentWeek) },
                        enabled = selectedWeek != currentWeek,
                    ) {
                        Text("回到本周")
                    }
                }
            }
            ManualEditPencilButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 2.dp, bottom = 2.dp),
                onClick = onAddManualCourse,
            )
        }
    }
}

@Composable
private fun ManualEditPencilButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFF1AE),
                            Color(0xFFFFD3B0),
                        ),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.88f), RoundedCornerShape(14.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = "手动添加活动",
                tint = Color(0xFF765230),
            )
        }
        Text(
            text = "手动添加",
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = PlayfulLabelFamily,
            ),
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF5E4B3E),
        )
    }
}

@Composable
private fun SummaryChip(label: String) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xF8FFFFFF),
                        Color(0xFFF3F8FF),
                    ),
                ),
            )
            .border(1.dp, Color(0xFFE2ECFB), CircleShape)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = PlayfulLabelFamily,
            ),
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF36566A),
        )
    }
}

@Composable
private fun WeekPickerRow(
    totalWeeks: Int,
    selectedWeek: Int,
    onSelectWeek: (Int) -> Unit,
) {
    val weeks = remember(totalWeeks) { (1..totalWeeks).toList() }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFEFF)),
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(weeks) { week ->
                val selected = week == selectedWeek
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (selected) {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF5FA8FF),
                                        Color(0xFF79D2FF),
                                    ),
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFF6FAFF),
                                        Color(0xFFEEF5FF),
                                    ),
                                )
                            },
                        )
                        .clickable { onSelectWeek(week) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = "第${week}周",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = PlayfulLabelFamily,
                        ),
                        color = if (selected) Color.White else Color(0xFF496176),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyTimetableGrid(
    courses: List<CourseEntry>,
    weekDates: List<LocalDate>,
    periodCount: Int,
    highlightedDay: Int?,
    periodTimes: Map<Int, String>,
    timeColumnWidth: Dp,
    dayColumnWidth: Dp,
    dayColumnGap: Dp,
    modifier: Modifier = Modifier,
    onEditCourse: (CourseEntry) -> Unit,
) {
    val periodHeight = 70.dp
    val periodGap = 6.dp
    val totalHeight = (periodHeight * periodCount) + (periodGap * (periodCount - 1))

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(dayColumnGap)) {
                HeaderCell(
                    text = "${weekDates.firstOrNull()?.monthValue ?: LocalDate.now().monthValue}月",
                    modifier = Modifier.width(timeColumnWidth),
                )
                DAY_LABELS.forEachIndexed { index, day ->
                    val date = weekDates.getOrNull(index)?.dayOfMonth ?: 0
                    val dayNumber = index + 1
                    HeaderCell(
                        text = "$day\n$date",
                        highlighted = highlightedDay == dayNumber,
                        modifier = Modifier.width(dayColumnWidth),
                    )
                }
            }

            Row(
                modifier = Modifier.height(totalHeight),
                horizontalArrangement = Arrangement.spacedBy(dayColumnGap),
            ) {
                TimeAxisColumn(
                    periodCount = periodCount,
                    periodTimes = periodTimes,
                    periodHeight = periodHeight,
                    periodGap = periodGap,
                    modifier = Modifier.width(timeColumnWidth),
                )
                DAY_NUMBERS.forEach { day ->
                    DayCourseColumn(
                        dayCourses = courses.filter { it.dayOfWeek == day },
                        periodCount = periodCount,
                        periodHeight = periodHeight,
                        periodGap = periodGap,
                        modifier = Modifier.width(dayColumnWidth),
                        onEditCourse = onEditCourse,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeAxisColumn(
    periodCount: Int,
    periodTimes: Map<Int, String>,
    periodHeight: Dp,
    periodGap: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(periodGap),
    ) {
        (1..periodCount).forEach { period ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(periodHeight)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color(0xFFF2F7FF)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = period.toString(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = PlayfulLabelFamily,
                        ),
                        color = Color(0xFF4A627B),
                        fontWeight = FontWeight.Bold,
                    )
                    val timeText = periodTimes[period].orEmpty()
                    if (timeText.isNotBlank()) {
                        Text(
                            text = axisTimeLabel(timeText),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = ReadableBodyFamily,
                            ),
                            color = Color(0xFF637A91),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCourseColumn(
    dayCourses: List<CourseEntry>,
    periodCount: Int,
    periodHeight: Dp,
    periodGap: Dp,
    modifier: Modifier = Modifier,
    onEditCourse: (CourseEntry) -> Unit,
) {
    val unitHeight = periodHeight + periodGap
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF9FCFF)),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(periodGap),
        ) {
            (1..periodCount).forEach {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(periodHeight)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF1F6FB)),
                )
            }
        }

        dayCourses.sortedBy { it.startPeriod }.forEach { course ->
            val start = course.startPeriod.coerceIn(1, periodCount)
            val end = course.endPeriod.coerceIn(start, periodCount)
            val span = end - start + 1
            val topOffset = unitHeight * (start - 1)
            val cardHeight = (periodHeight * span) + (periodGap * (span - 1))
            val style = courseCardStyle(course.name)

            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .offset(y = topOffset)
                    .fillMaxWidth()
                    .height(cardHeight)
                    .clip(RoundedCornerShape(14.dp))
                    .background(style.brush)
                    .border(1.dp, Color.White.copy(alpha = 0.88f), RoundedCornerShape(14.dp))
                    .clickable { onEditCourse(course) }
                    .padding(horizontal = 7.dp, vertical = 7.dp),
            ) {
                CardDecorationOverlay(
                    profile = style.decorationProfile,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 12.dp, y = (-12).dp),
                )
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = CardTitleFamily,
                        ),
                        fontWeight = FontWeight.Bold,
                        color = style.titleColor,
                        maxLines = if (span >= 3) 3 else 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = if (span == 1) TextAlign.Center else TextAlign.Start,
                        modifier = if (span == 1) {
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(top = 6.dp)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .padding(top = if (span >= 2) 8.dp else 2.dp)
                        },
                    )
                    if (span > 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = course.location.ifBlank { "待补充" },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = ReadableBodyFamily,
                                ),
                                color = style.metaColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (span >= 2) {
                                Text(
                                    text = course.teacher.ifBlank { "待补充" },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = ReadableBodyFamily,
                                    ),
                                    color = style.metaColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (course.note.isNotBlank()) {
                                Text(
                                    text = course.note,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = ReadableBodyFamily,
                                    ),
                                    color = style.metaColor.copy(alpha = 0.92f),
                                    maxLines = if (span >= 4) 2 else 1,
                                    overflow = TextOverflow.Ellipsis,
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
private fun CardDecorationOverlay(
    profile: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(56.dp)
            .height(56.dp),
    ) {
        when (profile % 5) {
            0 -> {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .width(42.dp)
                        .height(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .width(20.dp)
                        .height(20.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                )
            }

            1 -> {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .width(40.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.22f)),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .width(24.dp)
                        .height(24.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                )
            }

            2 -> {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .width(34.dp)
                        .height(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(18.dp)
                        .height(18.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.28f)),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .width(10.dp)
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.26f)),
                )
            }

            3 -> {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .width(42.dp)
                        .height(42.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(14.dp)
                        .height(14.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                )
            }

            else -> {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .width(40.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .width(26.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.3f)),
                )
            }
        }
    }
}

@Composable
private fun HeaderCell(
    text: String,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val backgroundBrush = if (highlighted) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFE4B8),
                Color(0xFFFFCFA6),
            ),
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFEAF4FF), Color(0xFFEAF4FF)),
        )
    }
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundBrush)
            .then(
                if (highlighted) {
                    Modifier.border(1.dp, Color(0xFFF7A75A), RoundedCornerShape(12.dp))
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = PlayfulLabelFamily,
            ),
            fontWeight = FontWeight.Bold,
            color = if (highlighted) Color(0xFF7A3D13) else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun EmptyWeekCard(selectedWeek: Int) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "第${selectedWeek}周暂无课程",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "请切换其他周次查看，或检查导入结果。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .width(300.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "还没有课表",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "先去导入课表 PDF，解析后就会在这里按周显示。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onNavigateImport) {
                    Text("去导入")
                }
            }
        }
    }
}

@Composable
private fun EditPersistedCourseDialog(
    course: CourseEntry,
    dialogTitle: String = "编辑课程",
    confirmLabel: String = "\u4fdd\u5b58",
    showDeleteAction: Boolean = false,
    onDismiss: () -> Unit,
    onRequestDelete: (() -> Unit)? = null,
    onConfirm: (CourseEntry) -> Unit,
) {
    var name by remember(course.id) { mutableStateOf(course.name) }
    var location by remember(course.id) { mutableStateOf(course.location) }
    var teacher by remember(course.id) { mutableStateOf(course.teacher) }
    var note by remember(course.id) { mutableStateOf(course.note) }
    var day by remember(course.id) { mutableStateOf(course.dayOfWeek.toString()) }
    var start by remember(course.id) { mutableStateOf(course.startPeriod.toString()) }
    var end by remember(course.id) { mutableStateOf(course.endPeriod.toString()) }
    var weekPattern by remember(course.id) { mutableStateOf(course.weekPattern) }
    val onSave = {
        val dayValue = day.toIntOrNull()?.coerceIn(1, 7) ?: course.dayOfWeek
        val startValue = start.toIntOrNull()?.coerceIn(1, 14) ?: course.startPeriod
        val endValue = end.toIntOrNull()?.coerceIn(startValue, 14) ?: course.endPeriod
        onConfirm(
            course.copy(
                name = name.ifBlank { course.name },
                location = location,
                teacher = teacher,
                note = note.trim(),
                weekPattern = weekPattern.ifBlank { course.weekPattern },
                dayOfWeek = dayValue,
                startPeriod = startValue,
                endPeriod = endValue,
            ),
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
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
        confirmButton = {},
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (showDeleteAction && onRequestDelete != null) {
                    TextButton(onClick = onRequestDelete) {
                        Text(
                            text = "\u5220\u9664",
                            color = Color(0xFFD94141),
                        )
                    }
                }
                TextButton(onClick = onDismiss) { Text("\u53d6\u6d88") }
                TextButton(onClick = onSave) { Text(confirmLabel) }
            }
        },
    )
}

@Composable
private fun DeleteWeekCourseDialog(
    courseName: String,
    selectedWeek: Int,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除课程") },
        text = {
            Text(
                text = "确定删除“$courseName”在第${selectedWeek}周的记录吗？其他周不会受影响。",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirmDelete) {
                Text("\u5220\u9664")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

private fun displayTermName(raw: String): String {
    return when (raw) {
        "Auto Parsed Timetable" -> "自动识别课表"
        "Manual Timetable" -> "手动课表"
        else -> raw.ifBlank { "我的课表" }
    }
}

private fun courseCardStyle(name: String): CourseCardStyle {
    val index = kotlin.math.abs(name.hashCode()) % CARD_STYLE_SEEDS.size
    val seed = CARD_STYLE_SEEDS[index]
    return CourseCardStyle(
        brush = Brush.linearGradient(colors = listOf(seed.from, seed.to)),
        titleColor = Color(0xFF243244),
        metaColor = Color(0xFF30485F),
        decorationProfile = index,
    )
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

private fun formatEpochDay(epochDay: Long): String {
    if (epochDay <= 0L) return "-"
    return LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ISO_LOCAL_DATE)
}

private fun mapToMondayFirst(dayOfWeek: DayOfWeek): Int {
    return when (dayOfWeek) {
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
        DayOfWeek.SUNDAY -> 7
    }
}

private fun buildManualCourseDraft(
    scheduleId: Long,
    selectedWeek: Int,
    defaultDayOfWeek: Int,
): CourseEntry {
    return CourseEntry(
        id = 0L,
        scheduleId = scheduleId,
        name = "自定义活动",
        teacher = "",
        location = "",
        note = "",
        dayOfWeek = defaultDayOfWeek.coerceIn(1, 7),
        startPeriod = 1,
        endPeriod = 2,
        weekPattern = selectedWeek.toString(),
        rawText = "manual-edit",
        sourceConfidence = 1f,
    )
}

private fun removeWeekFromPattern(
    weekPattern: String,
    weekToRemove: Int,
    totalWeeks: Int,
): String? {
    if (weekToRemove <= 0 || totalWeeks <= 0) return weekPattern
    val keptWeeks = (1..totalWeeks)
        .filter { week -> week != weekToRemove && isCourseInWeek(weekPattern, week) }
    if (keptWeeks.isEmpty()) return null
    if (keptWeeks.size == totalWeeks) return "all"
    return toCompactWeekPattern(keptWeeks)
}

private fun toCompactWeekPattern(weeks: List<Int>): String {
    if (weeks.isEmpty()) return ""
    val sorted = weeks.distinct().sorted()
    val parts = mutableListOf<String>()
    var start = sorted.first()
    var end = sorted.first()

    sorted.drop(1).forEach { week ->
        if (week == end + 1) {
            end = week
        } else {
            parts += if (start == end) start.toString() else "$start-$end"
            start = week
            end = week
        }
    }
    parts += if (start == end) start.toString() else "$start-$end"
    return parts.joinToString(separator = ",")
}

private fun axisTimeLabel(raw: String): String {
    val parts = raw.split('-')
    return if (parts.size == 2) {
        "${parts[0]}\n${parts[1]}"
    } else {
        raw
    }
}

private const val DEFAULT_PERIOD_COUNT = 12
private val DAY_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")
private val DAY_NUMBERS = (1..7).toList()

private data class CourseCardStyle(
    val brush: Brush,
    val titleColor: Color,
    val metaColor: Color,
    val decorationProfile: Int,
)

private data class CardStyleSeed(
    val from: Color,
    val to: Color,
)

private val CARD_STYLE_SEEDS = listOf(
    CardStyleSeed(Color(0xFFFFD6BF), Color(0xFFFFF0A7)),
    CardStyleSeed(Color(0xFFCDE9FF), Color(0xFF9EDBFF)),
    CardStyleSeed(Color(0xFFCFF5DF), Color(0xFFA9F0CF)),
    CardStyleSeed(Color(0xFFE7D0FF), Color(0xFFD9C5FF)),
    CardStyleSeed(Color(0xFFFFD5E8), Color(0xFFFFC7CF)),
    CardStyleSeed(Color(0xFFD5E7FF), Color(0xFFBEEBFF)),
    CardStyleSeed(Color(0xFFFFEDC3), Color(0xFFFFD9A0)),
    CardStyleSeed(Color(0xFFCFF3FF), Color(0xFFB2EAF2)),
    CardStyleSeed(Color(0xFFFFD1DA), Color(0xFFFFC3A4)),
    CardStyleSeed(Color(0xFFE1D8FF), Color(0xFFC9D4FF)),
    CardStyleSeed(Color(0xFFFFE1B8), Color(0xFFFFC6AE)),
    CardStyleSeed(Color(0xFFCAE6FF), Color(0xFFC4DBFF)),
    CardStyleSeed(Color(0xFFCFF3D2), Color(0xFFE2F8B8)),
    CardStyleSeed(Color(0xFFD7D8FF), Color(0xFFEBC8FF)),
)
