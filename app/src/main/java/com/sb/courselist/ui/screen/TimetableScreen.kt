package com.sb.courselist.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.sb.courselist.domain.model.CourseEntry
import com.sb.courselist.domain.model.ParsedSchedule
import com.sb.courselist.ui.theme.ArtHeadlineFamily
import com.sb.courselist.ui.theme.CardTitleFamily
import com.sb.courselist.ui.theme.CardWhite
import com.sb.courselist.ui.theme.PlayfulLabelFamily
import com.sb.courselist.ui.theme.ReadableBodyFamily
import java.text.SimpleDateFormat
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

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
    var easterEggAchievement by remember(schedule.meta.id) { mutableStateOf<EasterEggAchievement?>(null) }
    val flippedCardState = remember(schedule.meta.id) { mutableStateMapOf<String, Boolean>() }

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

    LaunchedEffect(easterEggAchievement?.id) {
        val active = easterEggAchievement ?: return@LaunchedEffect
        delay(2600L)
        if (easterEggAchievement?.id == active.id) {
            easterEggAchievement = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    val compactMode = maxWidth < 430.dp
                    val dayGap = if (compactMode) 3.dp else 8.dp
                    val timeColumnWidth = if (compactMode) 34.dp else 50.dp
                    val adaptiveDayWidth = (maxWidth - timeColumnWidth - dayGap * 7) / 7

                    WeeklyTimetableGrid(
                        courses = visibleCourses,
                        weekDates = weekDates,
                        periodCount = periodCount,
                        highlightedDay = highlightedDay,
                        periodTimes = periodTimes,
                        timeColumnWidth = timeColumnWidth,
                        dayColumnWidth = adaptiveDayWidth,
                        dayColumnGap = dayGap,
                        modifier = Modifier.fillMaxWidth(),
                        onEditCourse = { editingCourse = it },
                        isCourseSkipped = { course ->
                            isWeekMarkedInPattern(course.skipWeekPattern, selectedWeek)
                        },
                        isCourseFlipped = { course ->
                            flippedCardState[buildCourseWeekKey(course.id, selectedWeek)] == true
                        },
                        onToggleFlip = { course ->
                            val key = buildCourseWeekKey(course.id, selectedWeek)
                            val next = !(flippedCardState[key] ?: false)
                            flippedCardState[key] = next
                        },
                        onMarkSkipped = { course ->
                            val teacher = prettyTeacherName(course.teacher)
                            val location = prettyLocationName(course.location)
                            easterEggAchievement = EasterEggAchievement(
                                id = System.currentTimeMillis(),
                                type = EggAchievementType.SKIP,
                                message = buildSkipEasterEggMessage(
                                    teacher = teacher,
                                    location = location,
                                ),
                            )
                            if (!isWeekMarkedInPattern(course.skipWeekPattern, selectedWeek)) {
                                onUpdateCourse(
                                    course.copy(
                                        skipWeekPattern = addWeekToPattern(course.skipWeekPattern, selectedWeek),
                                    ),
                                )
                            }
                            flippedCardState[buildCourseWeekKey(course.id, selectedWeek)] = false
                        },
                        onRestoreSkipped = { course ->
                            easterEggAchievement = EasterEggAchievement(
                                id = System.currentTimeMillis(),
                                type = EggAchievementType.STUDY,
                                message = buildStudyEasterEggMessage(),
                            )
                            if (isWeekMarkedInPattern(course.skipWeekPattern, selectedWeek)) {
                                onUpdateCourse(
                                    course.copy(
                                        skipWeekPattern = removeWeekFromCompactPattern(
                                            pattern = course.skipWeekPattern,
                                            week = selectedWeek,
                                        ),
                                    ),
                                )
                            }
                            flippedCardState[buildCourseWeekKey(course.id, selectedWeek)] = false
                        },
                    )
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
        ) {
            EasterEggAchievementBanner(
                achievement = easterEggAchievement,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = -(maxHeight * 0.2f)),
            )
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
            dialogTitle = "\u65b0\u589e\u6d3b\u52a8",
            confirmLabel = "\u6dfb\u52a0",
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
private fun EasterEggAchievementBanner(
    achievement: EasterEggAchievement?,
    modifier: Modifier = Modifier,
) {
    if (achievement != null) {
        val info = achievement
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier
                    .wrapContentWidth()
                    .widthIn(max = 480.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            ) {
                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = if (info.type == EggAchievementType.SKIP) {
                                    listOf(
                                        Color(0xFFE9F4FF),
                                        Color(0xFFF7EDFF),
                                        Color(0xFFFFF5E3),
                                    )
                                } else {
                                    listOf(
                                        Color(0xFFEAFEF3),
                                        Color(0xFFE9F6FF),
                                        Color(0xFFFFF6EE),
                                    )
                                },
                            ),
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.92f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = info.message,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = ReadableBodyFamily,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = if (info.type == EggAchievementType.SKIP) {
                                Color(0xFF30445A)
                            } else {
                                Color(0xFF2D4E42)
                            },
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
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
                    SummaryChip(label = "\u7b2c${selectedWeek}\u5468")
                    SummaryChip(label = "\u5171${totalWeeks}\u5468")
                    SummaryChip(label = "\u672c\u5468${visibleCount}\u95e8\u8bfe")
                }
                Text(
                    text = "\u6700\u8fd1\u5bfc\u5165\uff1a${formatDate(schedule.meta.importedAt)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = ReadableBodyFamily,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (termStartEpochDay > 0L) {
                    Text(
                        text = "\u5f00\u5b66\u65e5\u671f\uff1a${formatEpochDay(termStartEpochDay)}",
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
                        Text("\u4e0a\u4e00\u5468")
                    }
                    TextButton(
                        onClick = { onSelectWeek(selectedWeek + 1) },
                        enabled = selectedWeek < totalWeeks,
                    ) {
                        Text("\u4e0b\u4e00\u5468")
                    }
                    FilledTonalButton(
                        onClick = { onSelectWeek(currentWeek) },
                        enabled = selectedWeek != currentWeek,
                    ) {
                        Text("\u56de\u5230\u672c\u5468")
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
                contentDescription = "\u624b\u52a8\u6dfb\u52a0\u6d3b\u52a8",
                tint = Color(0xFF765230),
            )
        }
        Text(
            text = "\u624b\u52a8\u6dfb\u52a0",
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
                        text = "\u7b2c${week}\u5468",
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
    isCourseSkipped: (CourseEntry) -> Boolean,
    isCourseFlipped: (CourseEntry) -> Boolean,
    onToggleFlip: (CourseEntry) -> Unit,
    onMarkSkipped: (CourseEntry) -> Unit,
    onRestoreSkipped: (CourseEntry) -> Unit,
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
                    text = "${weekDates.firstOrNull()?.monthValue ?: LocalDate.now().monthValue}\u6708",
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
                        isCourseSkipped = isCourseSkipped,
                        isCourseFlipped = isCourseFlipped,
                        onToggleFlip = onToggleFlip,
                        onMarkSkipped = onMarkSkipped,
                        onRestoreSkipped = onRestoreSkipped,
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
@OptIn(ExperimentalFoundationApi::class)
private fun DayCourseColumn(
    dayCourses: List<CourseEntry>,
    periodCount: Int,
    periodHeight: Dp,
    periodGap: Dp,
    modifier: Modifier = Modifier,
    onEditCourse: (CourseEntry) -> Unit,
    isCourseSkipped: (CourseEntry) -> Boolean,
    isCourseFlipped: (CourseEntry) -> Boolean,
    onToggleFlip: (CourseEntry) -> Unit,
    onMarkSkipped: (CourseEntry) -> Unit,
    onRestoreSkipped: (CourseEntry) -> Unit,
) {
    val unitHeight = periodHeight + periodGap
    val density = LocalDensity.current
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
            val isSkipped = isCourseSkipped(course)
            val isFlipped = isCourseFlipped(course)
            val rotationY by animateFloatAsState(
                targetValue = if (isFlipped) 180f else 0f,
                animationSpec = tween(durationMillis = 380),
                label = "courseFlip",
            )
            val showBack = rotationY >= 90f
            val cardBrush = if (isSkipped) {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFD9DEE6),
                        Color(0xFFC4CCD7),
                    ),
                )
            } else {
                style.brush
            }
            val titleColor = if (isSkipped) Color(0xFF535E6E) else style.titleColor
            val metaColor = if (isSkipped) Color(0xFF667487) else style.metaColor

            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .offset(y = topOffset)
                    .fillMaxWidth()
                    .height(cardHeight)
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBrush)
                    .border(1.dp, Color.White.copy(alpha = 0.88f), RoundedCornerShape(14.dp))
                    .combinedClickable(
                        onClick = {
                            if (isFlipped) {
                                onToggleFlip(course)
                            } else {
                                onEditCourse(course)
                            }
                        },
                        onLongClick = { onToggleFlip(course) },
                    )
                    .graphicsLayer {
                        this.rotationY = rotationY
                        cameraDistance = with(density) { 18.dp.toPx() }
                    }
                    .padding(horizontal = 7.dp, vertical = 7.dp),
            ) {
                if (showBack) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { this.rotationY = 180f },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ColorfulEggLabel()
                            FlipActionButton(
                                label = if (isSkipped) "\u6211\u7231\u5b66\u4e60" else "\u7fd8\u4e86",
                                textPalette = if (isSkipped) {
                                    listOf(
                                        Color(0xFF1B7B5B),
                                        Color(0xFF2C8DE3),
                                        Color(0xFF5469D4),
                                    )
                                } else {
                                    listOf(
                                        Color(0xFFD94887),
                                        Color(0xFF8E53E5),
                                        Color(0xFF2D9AE6),
                                    )
                                },
                                background = if (isSkipped) {
                                    listOf(
                                        Color(0xFFE5FAEA),
                                        Color(0xFFD9F3FF),
                                    )
                                } else {
                                    listOf(
                                        Color(0xFFFFE7F1),
                                        Color(0xFFFFF0D9),
                                    )
                                },
                                onClick = {
                                    if (isSkipped) {
                                        onRestoreSkipped(course)
                                    } else {
                                        onMarkSkipped(course)
                                    }
                                },
                            )
                        }
                    }
                } else {
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
                            color = titleColor,
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
                                    text = course.location.ifBlank { "\u5f85\u8865\u5145" },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = ReadableBodyFamily,
                                    ),
                                    color = metaColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (span >= 2) {
                                    Text(
                                        text = course.teacher.ifBlank { "\u5f85\u8865\u5145" },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = ReadableBodyFamily,
                                        ),
                                        color = metaColor,
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
                                        color = metaColor.copy(alpha = 0.92f),
                                        maxLines = if (span >= 4) 2 else 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                    if (isSkipped) {
                        SkippedStampOverlay(
                            span = span,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = (-2).dp)
                                .fillMaxWidth(0.96f)
                                .zIndex(5f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorfulEggLabel() {
    val label = buildAnnotatedString {
        listOf(
            Color(0xFFFA7A5C),
            Color(0xFF8E53E5),
            Color(0xFF2B94E2),
        ).zip("\u5f69\u86cb".toList()).forEach { (color, ch) ->
            withStyle(
                SpanStyle(
                    color = color,
                    fontWeight = FontWeight.ExtraBold,
                ),
            ) {
                append(ch)
            }
        }
    }
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall.copy(
            fontFamily = ArtHeadlineFamily,
        ),
    )
}

@Composable
private fun FlipActionButton(
    label: String,
    textPalette: List<Color>,
    background: List<Color>,
    onClick: () -> Unit,
) {
    val coloredText = buildAnnotatedString {
        label.forEachIndexed { index, ch ->
            withStyle(
                SpanStyle(
                    color = textPalette[index % textPalette.size],
                    fontWeight = FontWeight.ExtraBold,
                ),
            ) {
                append(ch)
            }
        }
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Brush.horizontalGradient(colors = background))
            .border(1.dp, Color.White.copy(alpha = 0.95f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = coloredText,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = PlayfulLabelFamily,
            ),
        )
    }
}

@Composable
private fun SkippedStampOverlay(
    span: Int,
    modifier: Modifier = Modifier,
) {
    val scale = when {
        span >= 5 -> 1f
        span == 4 -> 0.98f
        span == 3 -> 0.96f
        span == 2 -> 0.92f
        else -> 0.9f
    }
    val overlayAlpha = when {
        span >= 4 -> 0.9f
        span == 3 -> 0.88f
        span == 2 -> 0.84f
        else -> 0.8f
    }
    val stampHeight = when {
        span >= 5 -> 66.dp
        span == 4 -> 62.dp
        span == 3 -> 58.dp
        span == 2 -> 52.dp
        else -> 48.dp
    }
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = overlayAlpha
            }
            .height(stampHeight),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(42.dp)
                .clip(CircleShape)
                .border(3.dp, Color(0xFF4F4F4F), CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(32.dp)
                .clip(CircleShape)
                .border(2.dp, Color(0xFF666666), CircleShape),
        )
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 4.dp, y = ((index - 1) * 10).dp)
                    .width(24.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color(0xFF595959))
                    .graphicsLayer {
                        rotationZ = if (index == 1) 0f else -3f
                    },
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-2).dp)
                .graphicsLayer { rotationZ = -11f }
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFF616161))
                .border(2.dp, Color(0xFF4A4A4A), RoundedCornerShape(9.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "\u7fd8\u6389\u4e86",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = PlayfulLabelFamily,
                ),
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFF7F7F7),
            )
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
                text = "\u7b2c${selectedWeek}\u5468\u6682\u65e0\u8bfe\u7a0b",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "\u8bf7\u5207\u6362\u5176\u4ed6\u5468\u6b21\u67e5\u770b\uff0c\u6216\u68c0\u67e5\u5bfc\u5165\u7ed3\u679c\u3002",
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
                    text = "\u8fd8\u6ca1\u6709\u8bfe\u8868",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "\u5148\u53bb\u5bfc\u5165\u8bfe\u8868 PDF\uff0c\u89e3\u6790\u540e\u5c31\u4f1a\u5728\u8fd9\u91cc\u6309\u5468\u663e\u793a\u3002",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onNavigateImport) {
                    Text("\u53bb\u5bfc\u5165")
                }
            }
        }
    }
}

@Composable
private fun EditPersistedCourseDialog(
    course: CourseEntry,
    dialogTitle: String = "\u7f16\u8f91\u8bfe\u7a0b",
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
    val dialogTitleStyle = MaterialTheme.typography.titleLarge.copy(
        fontFamily = ArtHeadlineFamily,
        fontWeight = FontWeight.Bold,
    )
    val fieldLabelStyle = MaterialTheme.typography.labelLarge.copy(
        fontFamily = PlayfulLabelFamily,
        fontWeight = FontWeight.Bold,
    )
    val fieldTextStyle = MaterialTheme.typography.bodyLarge.copy(
        fontFamily = ReadableBodyFamily,
        fontWeight = FontWeight.SemiBold,
    )
    val actionTextStyle = MaterialTheme.typography.labelLarge.copy(
        fontFamily = PlayfulLabelFamily,
        fontWeight = FontWeight.ExtraBold,
    )
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
        title = {
            Text(
                text = dialogTitle,
                style = dialogTitleStyle,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(text = "\u8bfe\u7a0b\u540d\u79f0", style = fieldLabelStyle) },
                    textStyle = fieldTextStyle,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(text = "\u4e0a\u8bfe\u5730\u70b9", style = fieldLabelStyle) },
                    textStyle = fieldTextStyle,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text(text = "\u4efb\u8bfe\u6559\u5e08", style = fieldLabelStyle) },
                    textStyle = fieldTextStyle,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(text = "\u5907\u6ce8", style = fieldLabelStyle) },
                    textStyle = fieldTextStyle,
                    minLines = 2,
                    maxLines = 4,
                )
                OutlinedTextField(
                    value = weekPattern,
                    onValueChange = { weekPattern = it },
                    label = { Text(text = "\u5468\u6b21\uff08\u5982 1-16\u5468\uff09", style = fieldLabelStyle) },
                    textStyle = fieldTextStyle,
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = day,
                        onValueChange = { day = it },
                        label = { Text(text = "\u661f\u671f(1-7)", style = fieldLabelStyle) },
                        textStyle = fieldTextStyle,
                        modifier = Modifier.width(110.dp),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = { Text(text = "\u5f00\u59cb\u8282", style = fieldLabelStyle) },
                        textStyle = fieldTextStyle,
                        modifier = Modifier.width(110.dp),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        label = { Text(text = "\u7ed3\u675f\u8282", style = fieldLabelStyle) },
                        textStyle = fieldTextStyle,
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
                            style = actionTextStyle,
                            color = Color(0xFFD94141),
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "\u53d6\u6d88",
                        style = actionTextStyle,
                    )
                }
                TextButton(onClick = onSave) {
                    Text(
                        text = confirmLabel,
                        style = actionTextStyle,
                    )
                }
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
        title = { Text("\u5220\u9664\u8bfe\u7a0b") },
        text = {
            Text(
                text = "\u786e\u5b9a\u5220\u9664\u201c${courseName}\u201d\u5728\u7b2c${selectedWeek}\u5468\u7684\u8bb0\u5f55\u5417\uff1f\u5176\u4ed6\u5468\u4e0d\u4f1a\u53d7\u5f71\u54cd\u3002",
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
                Text("\u53d6\u6d88")
            }
        },
    )
}

private fun displayTermName(raw: String): String {
    val normalized = normalizeLegacyMojibake(raw).trim()
    return when (normalized) {
        "Auto Parsed Timetable" -> "\u81ea\u52a8\u8bc6\u522b\u8bfe\u8868"
        "Manual Timetable" -> "\u624b\u52a8\u8bfe\u8868"
        "\u81ea\u52a8\u8bc6\u522b\u8bfe\u8868" -> "\u81ea\u52a8\u8bc6\u522b\u8bfe\u8868"
        "\u624b\u52a8\u8bfe\u8868" -> "\u624b\u52a8\u8bfe\u8868"
        else -> normalized.ifBlank { "\u6211\u7684\u8bfe\u8868" }
    }
}

private fun normalizeLegacyMojibake(text: String): String {
    if (text.isBlank()) return text
    val repaired = runCatching {
        String(text.toByteArray(Charset.forName("GB18030")), Charsets.UTF_8).trim()
    }.getOrNull().orEmpty()
    if (repaired.isBlank()) return text
    val repairedLooksRight =
        repaired.contains("\u8bfe\u8868") ||
            repaired.contains("\u81ea\u52a8") ||
            repaired.contains("\u624b\u52a8")
    return if (repairedLooksRight) repaired else text
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
        name = "\u81ea\u5b9a\u4e49\u6d3b\u52a8",
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

private fun parseCompactWeekPattern(weekPattern: String): Set<Int> {
    if (weekPattern.isBlank()) return emptySet()
    val result = mutableSetOf<Int>()
    weekPattern
        .replace("\uff0c", ",")
        .split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .forEach { segment ->
            val rangeParts = segment.split('-').map { it.trim() }
            if (rangeParts.size == 2) {
                val start = rangeParts[0].toIntOrNull()
                val end = rangeParts[1].toIntOrNull()
                if (start != null && end != null) {
                    val first = minOf(start, end)
                    val last = maxOf(start, end)
                    (first..last).forEach { value ->
                        if (value > 0) result += value
                    }
                }
            } else {
                val value = segment.toIntOrNull()
                if (value != null && value > 0) {
                    result += value
                }
            }
        }
    return result
}

private fun isWeekMarkedInPattern(pattern: String, week: Int): Boolean {
    if (week <= 0) return false
    return parseCompactWeekPattern(pattern).contains(week)
}

private fun addWeekToPattern(pattern: String, week: Int): String {
    if (week <= 0) return pattern
    val weeks = parseCompactWeekPattern(pattern).toMutableSet()
    weeks += week
    return toCompactWeekPattern(weeks.toList())
}

private fun removeWeekFromCompactPattern(pattern: String, week: Int): String {
    if (week <= 0 || pattern.isBlank()) return pattern
    val weeks = parseCompactWeekPattern(pattern).toMutableSet()
    weeks.remove(week)
    return toCompactWeekPattern(weeks.toList())
}

private fun buildCourseWeekKey(courseId: Long, week: Int): String {
    return "$courseId#$week"
}

private fun prettyTeacherName(raw: String): String {
    val value = raw.trim().ifBlank { "\u67d0\u4f4d\u8001\u5e08" }
    return if (value.contains("\u8001\u5e08")) value else "${value}\u8001\u5e08"
}

private fun prettyLocationName(raw: String): String {
    return raw.trim().ifBlank { "\u67d0\u95f4\u6559\u5ba4" }
}

private fun buildStudyEasterEggMessage(): String {
    val lines = listOf(
        "\u8001\u5e08\u8bb0\u4f4f\u4e86\u4f60\u8fd9\u5f20\u5e05\u8138\uff0c\u5e73\u65f6\u5206+1",
        "\u5b66\u4e60\u4f7f\u6211\u5168\u5bb6\u5feb\u4e50",
        "\u4eba\u6c11\u7684\u60c5\u62a5\u5458",
        "\u5bbf\u820d\u538b\u7f29\u5305",
        "\u53bb\u6559\u5ba4\u5077\u5077\u5377",
        "\u4e3b\u52a8\u4e00\u4e2a\u966a\u4f34",
    )
    return lines.random()
}

private fun buildSkipEasterEggMessage(teacher: String, location: String): String {
    val lines = listOf(
        "\u9009\u4fee\u5fc5\u9003\uff0c\u5fc5\u4fee\u9009\u9003",
        "\u8001\u5e08\uff0c\u521a\u624d\u6211\u53bb\u4e0a\u5395\u6240\u4e86...",
        "\u70b9\u540d\u4e86\u53eb\u6211",
        "\u6765\u4e2a\u4e8c\u7ef4\u7801",
        "\u5e2e\u6211\u7b7e\u4e2a\u5230",
        "\u5e73\u65f6\u5206-1\uff0c\u5feb\u4e50\u503c+100",
        "\u4e3b\u516c\u4f55\u5fe7\u6c49\u5ba4\u96be\u5174",
        "\u561a\u561a\u4e86\u561a\u4e86\u561a\u4e86\u561a\u4e86\u561a",
        "\u6c34\u8bfe==\u74e6\u7f57\u5170\u7279",
        "\u539f\u795e\uff0c\u542f\u52a8\uff01",
        "${teacher}\uff1a\u8bf4\u597d\u7684${location}\u4e0d\u89c1\u4e0d\u6563\u5462",
        "\u8001\u5e08\uff0c\u6211\u660e\u5929\u809a\u5b50\u75bc",
    )
    return lines.random()
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
private val DAY_LABELS = listOf("\u4e00", "\u4e8c", "\u4e09", "\u56db", "\u4e94", "\u516d", "\u65e5")
private val DAY_NUMBERS = (1..7).toList()

private data class EasterEggAchievement(
    val id: Long,
    val type: EggAchievementType,
    val message: String,
)

private enum class EggAchievementType {
    SKIP,
    STUDY,
}

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

