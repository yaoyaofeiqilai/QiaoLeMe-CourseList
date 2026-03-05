package com.sb.courselist.ui.screen

import com.sb.courselist.domain.model.CourseEntry
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

internal fun filterCoursesByWeek(
    courses: List<CourseEntry>,
    week: Int,
): List<CourseEntry> {
    return courses.filter { isCourseInWeek(it.weekPattern, week) }
}

internal fun inferDisplayWeekCount(
    courses: List<CourseEntry>,
    declaredTotalWeeks: Int,
): Int {
    var maxWeek = if (declaredTotalWeeks > 0) declaredTotalWeeks else 20
    courses.forEach { course ->
        parseWeekRanges(course.weekPattern).forEach { range ->
            maxWeek = max(maxWeek, range.last)
        }
    }
    return maxWeek.coerceIn(1, 30)
}

internal fun defaultCurrentWeek(totalWeeks: Int): Int {
    if (totalWeeks <= 0) return 1
    val weekOfYear = LocalDate.now()
        .get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())
    return ((weekOfYear - 1) % totalWeeks) + 1
}

internal fun currentWeekFromTermStart(
    termStartEpochDay: Long,
    totalWeeks: Int,
    todayEpochDay: Long = LocalDate.now().toEpochDay(),
): Int? {
    if (termStartEpochDay <= 0L || totalWeeks <= 0) return null
    val diffDays = todayEpochDay - termStartEpochDay
    val rawWeek = if (diffDays >= 0) {
        (diffDays / 7L) + 1L
    } else {
        1L
    }
    return rawWeek.toInt().coerceIn(1, totalWeeks)
}

internal fun suggestInitialWeek(
    courses: List<CourseEntry>,
    totalWeeks: Int,
    preferredWeek: Int,
): Int {
    if (totalWeeks <= 0) return 1
    val target = preferredWeek.coerceIn(1, totalWeeks)
    if (filterCoursesByWeek(courses, target).isNotEmpty()) return target

    return (1..totalWeeks)
        .sortedBy { week -> abs(week - target) }
        .firstOrNull { week -> filterCoursesByWeek(courses, week).isNotEmpty() }
        ?: target
}

internal fun weekDates(
    selectedWeek: Int,
    currentWeek: Int,
): List<LocalDate> {
    val mondayThisWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val offset = (selectedWeek - currentWeek).toLong()
    val targetMonday = mondayThisWeek.plusWeeks(offset)
    return (0L..6L).map { targetMonday.plusDays(it) }
}

internal fun buildPeriodTimeDisplayMap(
    parsed: Map<Int, String>,
    periodCount: Int,
): Map<Int, String> {
    val safeParsed = parsed
        .filterKeys { key -> key in 1..14 }
        .mapValues { (_, value) -> value.trim() }
        .toMutableMap()
    if (safeParsed.isEmpty()) {
        safeParsed.putAll(DEFAULT_PERIOD_TIME_MAP)
    } else {
        (1..periodCount).forEach { period ->
            val current = safeParsed[period].orEmpty()
            if (!isValidTimeRange(current) && DEFAULT_PERIOD_TIME_MAP.containsKey(period)) {
                safeParsed[period] = DEFAULT_PERIOD_TIME_MAP[period].orEmpty()
            }
        }
    }
    return (1..periodCount).associateWith { period -> safeParsed[period].orEmpty() }
}

private fun isValidTimeRange(text: String): Boolean {
    val values = TIME_VALUE_REGEX.findAll(text).map { it.value }.toList()
    if (values.size != 2) return false
    val start = parseMinutes(values[0]) ?: return false
    val end = parseMinutes(values[1]) ?: return false
    return end > start
}

private fun parseMinutes(text: String): Int? {
    val parts = text.split(':')
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

internal fun isCourseInWeek(
    weekPattern: String,
    week: Int,
): Boolean {
    if (week <= 0) return false
    val normalized = normalizeWeekPattern(weekPattern)
    if (normalized.isBlank() || normalized == "all") return true

    val oddOnly = ODD_MARKERS.any { marker -> normalized.contains(marker) }
    val evenOnly = EVEN_MARKERS.any { marker -> normalized.contains(marker) }
    if (oddOnly && week % 2 == 0) return false
    if (evenOnly && week % 2 != 0) return false

    val ranges = parseWeekRanges(normalized)
    if (ranges.isEmpty()) return true
    return ranges.any { range -> week in range }
}

private fun parseWeekRanges(weekPattern: String): List<IntRange> {
    val normalized = normalizeWeekPattern(weekPattern)
    return WEEK_NUMBER_REGEX.findAll(normalized).mapNotNull { match ->
        val start = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
        val endRaw = match.groupValues.getOrNull(2)?.toIntOrNull() ?: start
        val end = max(start, endRaw)
        start..end
    }.toList()
}

private fun normalizeWeekPattern(raw: String): String {
    return raw
        .lowercase(Locale.ROOT)
        .replace(" ", "")
        .replace("\u3000", "")
        .replace("，", ",")
        .replace("、", ",")
        .replace("；", ",")
        .replace(";", ",")
        .replace("至", "-")
        .replace("—", "-")
        .replace("–", "-")
        .replace("－", "-")
        .replace("~", "-")
}

private val WEEK_NUMBER_REGEX = Regex("(\\d{1,2})(?:-(\\d{1,2}))?")
private val ODD_MARKERS = listOf("\u5355", "odd")
private val EVEN_MARKERS = listOf("\u53cc", "even")
private val TIME_VALUE_REGEX = Regex("\\d{1,2}:\\d{2}")

private val DEFAULT_PERIOD_TIME_MAP = mapOf(
    1 to "08:00-08:45",
    2 to "08:55-09:40",
    3 to "10:05-10:50",
    4 to "11:00-11:45",
    5 to "14:00-14:45",
    6 to "14:55-15:40",
    7 to "16:05-16:50",
    8 to "17:00-17:45",
    9 to "19:00-19:45",
    10 to "19:55-20:40",
    11 to "20:50-21:35",
    12 to "21:40-22:25",
    13 to "22:30-23:15",
    14 to "23:20-00:05",
)
