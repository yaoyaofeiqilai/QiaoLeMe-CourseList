package com.sb.courselist.parser

import android.util.Log
import com.sb.courselist.domain.model.CourseEntry
import com.sb.courselist.domain.model.ParsedSchedule
import com.sb.courselist.domain.model.ScheduleMeta

class TemplateRuleEngine {
    private var previewIdSeed: Long = -1L

    fun parse(tokens: List<TextToken>, sourceTag: String): ParsedSchedule? {
        if (tokens.isEmpty()) return null

        val normalizedTokens = tokens
            .map { token -> token.copy(text = normalize(token.text)) }
            .filter { it.text.isNotBlank() }
        if (normalizedTokens.isEmpty()) return null

        val byPage = normalizedTokens.groupBy { it.page }.toSortedMap()
        if (byPage.isEmpty()) return null

        var referenceAnchors = byPage.values
            .asSequence()
            .map { extractDayAnchors(it) }
            .firstOrNull { it.size >= 5 }
            .orEmpty()

        val referenceHeaderBottom = if (referenceAnchors.size >= 5) {
            referenceAnchors.maxOf { it.bottom } + HEADER_BOTTOM_PAD
        } else {
            referenceAnchors = synthesizeDayAnchors(normalizedTokens)
            Float.NEGATIVE_INFINITY
        }

        val referenceDayBounds = buildDayBounds(referenceAnchors)
        if (referenceDayBounds.size < 5) {
            logWarn("parse: insufficient day bounds, tokens=${normalizedTokens.size}")
            return null
        }

        val parsed = mutableListOf<MutableCourse>()
        byPage.forEach { (page, pageTokens) ->
            val anchors = extractDayAnchors(pageTokens)
            val dayBounds = if (anchors.size >= 5) buildDayBounds(anchors) else referenceDayBounds
            if (dayBounds.size < 5) return@forEach

            val headerBottom = if (anchors.size >= 5) {
                anchors.maxOf { it.bottom } + HEADER_BOTTOM_PAD
            } else {
                referenceHeaderBottom
            }

            val dayWords = assignWordsByDay(pageTokens, dayBounds, headerBottom)
            dayBounds.forEach { dayBound ->
                val lines = wordsToLines(dayWords[dayBound.day].orEmpty())
                parsed += parseDayLines(page = page, day = dayBound.day, lines = lines)
            }
        }

        if (parsed.isEmpty()) return null

        val enriched = fillMissingFields(parsed)
        val courses = enriched
            .filter { it.startPeriod in 1..14 && it.endPeriod in 1..14 && it.endPeriod >= it.startPeriod }
            .filter { it.name.isNotBlank() }
            .map { course ->
                CourseEntry(
                    id = nextPreviewId(),
                    name = course.name,
                    teacher = course.teacher,
                    location = course.location,
                    dayOfWeek = course.day,
                    startPeriod = course.startPeriod,
                    endPeriod = course.endPeriod,
                    weekPattern = course.weekPattern.ifBlank { WEEK_ALL },
                    rawText = course.rawText,
                    sourceConfidence = score(course),
                )
            }
            .distinctBy {
                "${it.dayOfWeek}-${it.startPeriod}-${it.endPeriod}-${it.weekPattern}-${it.name}-${it.location}-${it.teacher}"
            }
            .sortedWith(compareBy<CourseEntry> { it.dayOfWeek }.thenBy { it.startPeriod })

        if (courses.isEmpty()) return null

        logInfo(
            "parse: source=$sourceTag tokens=${normalizedTokens.size} pages=${byPage.size} courses=${courses.size}",
        )

        return ParsedSchedule(
            meta = ScheduleMeta(
                termName = "Auto Parsed Timetable",
                totalWeeks = 20,
                importedAt = System.currentTimeMillis(),
                templateVersion = "cn-campus-v2",
                sourceTag = sourceTag,
            ),
            courses = courses,
        )
    }

    private fun nextPreviewId(): Long {
        val current = previewIdSeed
        previewIdSeed -= 1L
        return current
    }

    private fun extractDayAnchors(tokens: List<TextToken>): List<DayAnchor> {
        val bestByDay = mutableMapOf<Int, TextToken>()
        tokens.forEach { token ->
            val day = parseDay(token.text) ?: return@forEach
            val existing = bestByDay[day]
            if (existing == null || token.y < existing.y) {
                bestByDay[day] = token
            }
        }
        return bestByDay
            .map { (day, token) ->
                DayAnchor(
                    day = day,
                    x = token.x + token.width * 0.5f,
                    top = token.y,
                    bottom = token.y + token.height,
                )
            }
            .sortedBy { it.x }
    }

    private fun synthesizeDayAnchors(tokens: List<TextToken>): List<DayAnchor> {
        if (tokens.size < 20) return emptyList()

        val minX = tokens.minOf { it.x }
        val maxX = tokens.maxOf { it.x + it.width }
        val minY = tokens.minOf { it.y }
        val width = maxX - minX
        if (width < 140f) return emptyList()

        val dayStart = minX + width * 0.18f
        val usableWidth = (maxX - dayStart).coerceAtLeast(70f)
        val step = usableWidth / 7f
        val anchorY = minY + 12f

        return (1..7).map { day ->
            DayAnchor(
                day = day,
                x = dayStart + step * (day - 0.5f),
                top = anchorY,
                bottom = anchorY + 8f,
            )
        }
    }

    private fun buildDayBounds(dayAnchors: List<DayAnchor>): List<DayBound> {
        val sorted = dayAnchors.sortedBy { it.x }
        if (sorted.size < 5) return emptyList()

        return sorted.mapIndexed { index, anchor ->
            val left = if (index == 0) {
                val next = sorted.getOrNull(index + 1)?.x ?: anchor.x + 80f
                anchor.x - (next - anchor.x) / 2f
            } else {
                val prev = sorted[index - 1].x
                (prev + anchor.x) / 2f
            }
            val right = if (index == sorted.lastIndex) {
                val prev = sorted.getOrNull(index - 1)?.x ?: anchor.x - 80f
                anchor.x + (anchor.x - prev) / 2f
            } else {
                val next = sorted[index + 1].x
                (anchor.x + next) / 2f
            }
            DayBound(day = anchor.day, left = left, right = right)
        }
    }

    private fun assignWordsByDay(
        tokens: List<TextToken>,
        dayBounds: List<DayBound>,
        headerBottom: Float,
    ): Map<Int, List<TextToken>> {
        val assigned = dayBounds.associate { it.day to mutableListOf<TextToken>() }
        tokens.forEach { token ->
            if (token.y <= headerBottom) return@forEach
            val centerX = token.x + token.width * 0.5f
            dayBounds.firstOrNull { bound -> centerX >= bound.left && centerX < bound.right }
                ?.let { bound -> assigned[bound.day]?.add(token) }
        }
        return assigned.mapValues { (_, list) -> list.toList() }
    }

    private fun wordsToLines(tokens: List<TextToken>): List<LineItem> {
        if (tokens.isEmpty()) return emptyList()

        val sorted = tokens.sortedWith(compareBy<TextToken> { it.y }.thenBy { it.x })
        val lines = mutableListOf<MutableList<TextToken>>()

        sorted.forEach { token ->
            val current = lines.lastOrNull()
            if (current == null) {
                lines += mutableListOf(token)
                return@forEach
            }

            val currentTop = current.first().y
            if (kotlin.math.abs(token.y - currentTop) <= LINE_Y_TOLERANCE) {
                current += token
            } else {
                lines += mutableListOf(token)
            }
        }

        return lines.mapNotNull { lineTokens ->
            val text = cleanValue(lineTokens.sortedBy { it.x }.joinToString(separator = "") { it.text })
            if (text.isBlank()) {
                null
            } else {
                LineItem(
                    top = lineTokens.minOf { it.y },
                    bottom = lineTokens.maxOf { it.y + it.height },
                    text = text,
                )
            }
        }
    }

    private fun parseDayLines(page: Int, day: Int, lines: List<LineItem>): List<MutableCourse> {
        if (lines.isEmpty()) return emptyList()

        val detailIndices = lines
            .mapIndexedNotNull { index, line -> if (PERIOD_REGEX.containsMatchIn(line.text)) index else null }
        if (detailIndices.isEmpty()) return emptyList()

        val parsed = mutableListOf<MutableCourse>()
        detailIndices.forEachIndexed { position, detailIndex ->
            val prevIndex = if (position > 0) detailIndices[position - 1] else -1
            val nextIndex = if (position + 1 < detailIndices.size) detailIndices[position + 1] else lines.size

            val prelude = lines.subList(prevIndex + 1, detailIndex).map { it.text }
            val block = lines.subList(detailIndex, nextIndex)
            if (block.isEmpty()) return@forEachIndexed

            val detailLine = block.first().text
            val joined = block.joinToString(separator = "") { it.text }
            val periodMatch = PERIOD_REGEX.find(joined) ?: return@forEachIndexed

            val rawStart = periodMatch.groupValues.getOrNull(1)?.toIntOrNull() ?: return@forEachIndexed
            val rawEnd = periodMatch.groupValues.getOrNull(2)?.toIntOrNull() ?: return@forEachIndexed
            val startPeriod = minOf(rawStart, rawEnd).coerceIn(1, 14)
            val endPeriod = maxOf(rawStart, rawEnd).coerceIn(startPeriod, 14)

            val nameHint = pickNameFromPrelude(prelude)
            val baseName = if (nameHint.isNotBlank()) {
                nameHint
            } else {
                extractNameFromJoined(joined, periodMatch.range.first)
            }
            val name = cleanName(baseName).ifBlank { UNKNOWN_COURSE }

            val weeks = extractWeeks(detailLine, joined).ifBlank { WEEK_ALL }
            val location = extractSegment(joined, LOCATION_LABEL_REGEX)
            val teacher = extractSegment(joined, TEACHER_LABEL_REGEX)

            parsed += MutableCourse(
                page = page,
                day = day,
                startPeriod = startPeriod,
                endPeriod = endPeriod,
                weekPattern = weeks,
                name = name,
                location = cleanValue(location),
                teacher = cleanValue(teacher),
                rawText = joined,
            )
        }

        return parsed
    }

    private fun fillMissingFields(courses: List<MutableCourse>): List<MutableCourse> {
        val byNameTeacher = mutableMapOf<String, MutableMap<String, Int>>()
        val byNameLocation = mutableMapOf<String, MutableMap<String, Int>>()
        val byTeacherName = mutableMapOf<Triple<String, Int, Int>, MutableMap<String, Int>>()
        val byTeacherAnyName = mutableMapOf<String, MutableMap<String, Int>>()
        val byTeacherLocationName = mutableMapOf<Pair<String, String>, MutableMap<String, Int>>()

        courses.forEach { course ->
            if (course.name.isBlank() || course.name == UNKNOWN_COURSE) return@forEach
            if (course.teacher.isNotBlank()) {
                increment(byNameTeacher, course.name, course.teacher)
                increment(byTeacherName, Triple(course.teacher, course.startPeriod, course.endPeriod), course.name)
                increment(byTeacherAnyName, course.teacher, course.name)
            }
            if (course.location.isNotBlank()) {
                increment(byNameLocation, course.name, course.location)
            }
            if (course.teacher.isNotBlank() && course.location.isNotBlank()) {
                increment(byTeacherLocationName, Pair(course.teacher, course.location), course.name)
            }
        }

        courses.forEach { course ->
            if (course.name == UNKNOWN_COURSE) {
                var guessed = ""
                if (course.teacher.isNotBlank() && course.location.isNotBlank()) {
                    guessed = pickStable(byTeacherLocationName[Pair(course.teacher, course.location)])
                }
                if (guessed.isBlank() && course.teacher.isNotBlank()) {
                    guessed = pickStable(
                        byTeacherName[Triple(course.teacher, course.startPeriod, course.endPeriod)],
                    )
                }
                if (guessed.isBlank() && course.teacher.isNotBlank()) {
                    guessed = pickStable(byTeacherAnyName[course.teacher])
                }
                if (guessed.isNotBlank()) {
                    course.name = guessed
                }
            }

            if (course.name != UNKNOWN_COURSE) {
                if (course.teacher.isBlank()) {
                    val guessedTeacher = pickStable(byNameTeacher[course.name])
                    if (guessedTeacher.isNotBlank()) {
                        course.teacher = guessedTeacher
                    }
                } else if (course.teacher.length <= 1) {
                    val candidates = byNameTeacher[course.name].orEmpty()
                    val better = candidates.keys
                        .filter { candidate ->
                            candidate.length > course.teacher.length &&
                                candidate.startsWith(course.teacher)
                        }
                        .sortedWith(
                            compareByDescending<String> { candidates[it] ?: 0 }
                                .thenByDescending { it.length },
                        )
                    if (better.isNotEmpty()) {
                        course.teacher = better.first()
                    }
                }

                if (course.location.isBlank()) {
                    val guessedLocation = pickStable(byNameLocation[course.name])
                    if (guessedLocation.isNotBlank()) {
                        course.location = guessedLocation
                    }
                }
            }

            if (course.name == UNKNOWN_COURSE) {
                val tailGuess = extractTailName(course.rawText)
                if (tailGuess.isNotBlank()) {
                    course.name = tailGuess
                }
            }

            if (
                course.name != UNKNOWN_COURSE &&
                !course.name.endsWith(CH_LAB_SUFFIX) &&
                course.rawText.contains(CH_HOURS_LAB_ONLY) &&
                !course.rawText.contains(CH_THEORY)
            ) {
                course.name += CH_LAB_SUFFIX
            }
        }

        return courses
    }

    private fun extractWeeks(detailText: String, joined: String): String {
        val detailMatch = PERIOD_REGEX.find(detailText)
        if (detailMatch != null) {
            val start = (detailMatch.range.last + 1).coerceAtMost(detailText.length)
            val tail = detailText.substring(start)
            val week = WEEK_REGEX.find(tail)?.groupValues?.getOrNull(1).orEmpty()
            if (week.isNotBlank()) return cleanValue(week)
        }

        val joinedMatch = PERIOD_REGEX.find(joined)
        if (joinedMatch != null) {
            val start = (joinedMatch.range.last + 1).coerceAtMost(joined.length)
            val end = (start + 160).coerceAtMost(joined.length)
            val tail = joined.substring(start, end)
            val week = WEEK_REGEX.find(tail)?.groupValues?.getOrNull(1).orEmpty()
            if (week.isNotBlank()) return cleanValue(week)
        }

        return ""
    }

    private fun extractSegment(text: String, labelRegex: Regex): String {
        val match = labelRegex.find(text) ?: return ""
        val start = (match.range.last + 1).coerceAtMost(text.length)
        val tail = text.substring(start)

        var end = tail.length
        FIELD_STOP_MARKERS.forEach { marker ->
            val idx = tail.indexOf(marker)
            if (idx >= 0 && idx < end) {
                end = idx
            }
        }

        return cleanValue(tail.substring(0, end))
    }

    private fun pickNameFromPrelude(lines: List<String>): String {
        val cleaned = lines
            .map { cleanName(it) }
            .filter { it.isNotBlank() }
        if (cleaned.isEmpty()) return ""

        for (index in cleaned.indices.reversed()) {
            val current = cleaned[index]
            if (index > 0) {
                val merged = cleanName(cleaned[index - 1] + current)
                if (looksLikeName(cleaned[index - 1]) && looksLikeName(merged)) {
                    return merged
                }
            }
            if (looksLikeName(current)) {
                return current
            }
        }

        if (cleaned.size >= 2) {
            val merged = cleanName(cleaned[cleaned.size - 2] + cleaned.last())
            if (looksLikeName(merged)) return merged
        }

        return ""
    }

    private fun extractNameFromJoined(joined: String, periodStart: Int): String {
        val start = periodStart.coerceIn(0, joined.length)
        val prefix = joined.substring(0, start)
        val chunks = prefix
            .split('/', '\uff0f')
            .map { cleanName(it) }
            .filter { it.isNotBlank() }

        chunks.asReversed().forEach { chunk ->
            if (looksLikeName(chunk)) return chunk
        }

        val fallback = cleanName(prefix)
        return if (looksLikeName(fallback)) fallback else UNKNOWN_COURSE
    }

    private fun extractTailName(text: String): String {
        var tailStart = 0
        TAIL_MARKERS.forEach { marker ->
            val index = text.lastIndexOf(marker)
            if (index >= 0) {
                tailStart = maxOf(tailStart, index + marker.length)
            }
        }

        var tail = cleanName(text.substring(tailStart.coerceIn(0, text.length)))
        tail = tail.replace(PREFIX_NOISE_REGEX, "")
        tail = cleanName(tail)
        if (looksLikeName(tail)) return tail

        val parts = text
            .split('/', '\uff0f')
            .map { cleanName(it) }
            .filter { it.isNotBlank() }

        parts.asReversed().forEach { part ->
            if (looksLikeName(part)) return part
        }
        return ""
    }

    private fun looksLikeName(text: String): Boolean {
        if (text.length < 2) return false
        if (PERIOD_REGEX.containsMatchIn(text)) return false
        if (text.contains('/') || text.contains(':') || text.contains('\uff1a')) return false
        if (TIME_OR_NUMBER_REGEX.matches(text)) return false
        if (!text.any { it.isLetterOrDigit() || isCjk(it) }) return false
        if (NAME_NOISE.any { token -> text.contains(token) }) return false
        if (isWeakName(text)) return false
        return true
    }

    private fun isWeakName(text: String): Boolean {
        val cleaned = cleanName(text)
        if (cleaned.isBlank()) return true
        if (cleaned in WEAK_NAME_EXACT) return true
        if (NOISE_ONLY_REGEX.matches(cleaned)) return true
        if (cleaned.length <= 3 && SHORT_CJK_OR_NUM_REGEX.matches(cleaned)) {
            if (
                cleaned.endsWith(CH_SUFFIX_COURSE) ||
                cleaned.endsWith(CH_SUFFIX_LAB) ||
                cleaned.endsWith(CH_SUFFIX_PRACTICE) ||
                cleaned.endsWith(CH_SUFFIX_GUIDE) ||
                cleaned.endsWith(CH_SUFFIX_THEORY)
            ) {
                return true
            }
        }
        return false
    }

    private fun parseDay(text: String): Int? {
        val normalized = normalize(text).lowercase()
        if (normalized.isBlank()) return null

        if (normalized.startsWith(CH_WEEKDAY_PREFIX) && normalized.length >= 3) {
            return mapDayChar(normalized[2])
        }
        if (normalized.startsWith(CH_WEEK) && normalized.length >= 2) {
            return mapDayChar(normalized[1])
        }
        if (normalized.startsWith(CH_WEEK_PREFIX_ALT) && normalized.length >= 3) {
            return mapDayChar(normalized[2])
        }
        if (normalized.length == 1) {
            return mapDayChar(normalized[0])
        }

        return when {
            normalized.startsWith(EN_MON) -> 1
            normalized.startsWith(EN_TUE) -> 2
            normalized.startsWith(EN_WED) -> 3
            normalized.startsWith(EN_THU) -> 4
            normalized.startsWith(EN_FRI) -> 5
            normalized.startsWith(EN_SAT) -> 6
            normalized.startsWith(EN_SUN) -> 7
            else -> null
        }
    }

    private fun mapDayChar(ch: Char): Int? {
        return when (ch) {
            '\u4e00', '1' -> 1
            '\u4e8c', '2' -> 2
            '\u4e09', '3' -> 3
            '\u56db', '4' -> 4
            '\u4e94', '5' -> 5
            '\u516d', '6' -> 6
            '\u65e5', '\u5929', '7' -> 7
            else -> null
        }
    }

    private fun cleanName(text: String): String {
        var value = normalize(text)
        value = LEADING_NOISE_REGEX.replace(value, "")
        value = TRAILING_SYMBOL_REGEX.replace(value, "").trim()

        val fullOpen = value.count { it == '\uff08' }
        val fullClose = value.count { it == '\uff09' }
        if (fullOpen > fullClose) {
            value += "\uff09".repeat(fullOpen - fullClose)
        }

        val asciiOpen = value.count { it == '(' }
        val asciiClose = value.count { it == ')' }
        if (asciiOpen > asciiClose) {
            value += ")".repeat(asciiOpen - asciiClose)
        }
        return value
    }

    private fun cleanValue(text: String): String {
        return normalize(text)
            .trim('/')
            .replace('\uff0f', '/')
    }

    private fun score(course: MutableCourse): Float {
        var value = 0.55f
        if (course.name != UNKNOWN_COURSE) value += 0.2f
        if (course.location.isNotBlank()) value += 0.1f
        if (course.teacher.isNotBlank()) value += 0.1f
        if (course.endPeriod > course.startPeriod) value += 0.05f
        return value.coerceIn(0.05f, 0.99f)
    }

    private fun logInfo(message: String) {
        runCatching { Log.i(TAG, message) }
    }

    private fun logWarn(message: String) {
        runCatching { Log.w(TAG, message) }
    }

    private fun pickStable(counter: Map<String, Int>?): String {
        if (counter.isNullOrEmpty()) return ""
        val sorted = counter.entries.sortedByDescending { it.value }
        if (sorted.size == 1) return sorted.first().key
        val first = sorted[0]
        val second = sorted[1]
        return if (first.value >= second.value + 2) first.key else ""
    }

    private fun <K> increment(
        store: MutableMap<K, MutableMap<String, Int>>,
        key: K,
        value: String,
    ) {
        if (value.isBlank()) return
        val counter = store.getOrPut(key) { mutableMapOf() }
        counter[value] = (counter[value] ?: 0) + 1
    }

    private fun normalize(raw: String): String {
        return raw
            .replace('\u3000', ' ')
            .replace(WHITESPACE_REGEX, "")
            .replace("|", "")
            .trim()
    }

    private fun isCjk(ch: Char): Boolean {
        return ch in '\u4e00'..'\u9fff'
    }

    companion object {
        private const val TAG = "ScheduleParser"

        private const val UNKNOWN_COURSE = "unknown_course"
        private const val WEEK_ALL = "all"
        private const val LINE_Y_TOLERANCE = 3.4f
        private const val HEADER_BOTTOM_PAD = 1.5f

        private const val EN_MON = "mon"
        private const val EN_TUE = "tue"
        private const val EN_WED = "wed"
        private const val EN_THU = "thu"
        private const val EN_FRI = "fri"
        private const val EN_SAT = "sat"
        private const val EN_SUN = "sun"

        private const val CH_WEEK = "\u5468"
        private const val CH_WEEKDAY_PREFIX = "\u661f\u671f"
        private const val CH_WEEK_PREFIX_ALT = "\u793c\u62dc"
        private const val CH_THEORY = "\u7406\u8bba"
        private const val CH_LAB_SUFFIX = "\u5b9e\u9a8c"
        private const val CH_HOURS_LAB_ONLY = "\u8bfe\u7a0b\u5b66\u65f6\u7ec4\u6210:\u5b9e\u9a8c"

        private const val CH_SUFFIX_COURSE = "\u8bfe"
        private const val CH_SUFFIX_LAB = "\u9a8c"
        private const val CH_SUFFIX_PRACTICE = "\u8df5"
        private const val CH_SUFFIX_GUIDE = "\u5bfc"
        private const val CH_SUFFIX_THEORY = "\u8bba"

        private val PERIOD_REGEX = Regex("[\\(\uff08]\\s*(\\d{1,2})\\s*[-~\u301c\u2013\u2014\u2015\uff0d\u81f3]\\s*(\\d{1,2})\\s*\u8282\\s*[\\)\uff09]")
        private val WEEK_REGEX = Regex("(\\d{1,2}(?:-\\d{1,2})?\u5468(?:[,，]\\d{1,2}(?:-\\d{1,2})?\u5468)*)")

        private val LOCATION_LABEL_REGEX = Regex("(?:\u573a\u5730|\u5730\u70b9|\u6559\u5ba4)[:\uff1a]\\s*")
        private val TEACHER_LABEL_REGEX = Regex("(?:\u6559\u5e08|\u8001\u5e08)[:\uff1a]?\\s*")

        private val FIELD_STOP_MARKERS = listOf(
            "/",
            "\uff0f",
            "\u6559\u5e08",
            "\u8001\u5e08",
            "\u6559\u5b66\u73ed",
            "\u8bfe\u7a0b\u5b66\u65f6",
            "\u8003\u6838\u65b9\u5f0f",
            "\u9009\u8bfe\u5907\u6ce8",
        )

        private val TAIL_MARKERS = listOf(
            "\u8bfe\u7a0b\u5b66\u65f6",
            "\u9009\u8bfe\u5907\u6ce8",
            "\u8003\u6838\u65b9\u5f0f",
            "\u6559\u5b66\u73ed\u7ec4\u6210",
            "\u6559\u5b66\u73ed",
            "\u5b66\u5206",
            "\u603b\u5b66\u65f6",
            "\u5468\u5b66\u65f6",
        )

        private val NAME_NOISE = listOf(
            "\u8bfe\u7a0b\u5b66\u65f6",
            "\u8003\u6838\u65b9\u5f0f",
            "\u6559\u5b66\u73ed",
            "\u9009\u8bfe\u5907\u6ce8",
            "\u5468\u5b66\u65f6",
            "\u5b66\u5206",
            "\u6821\u533a",
            "\u573a\u5730",
            "\u6559\u5e08",
            "\u8001\u5e08",
        )

        private val WEAK_NAME_EXACT = setOf(
            "\u5b9e\u9a8c",
            "\u5b9e\u8df5",
            "\u7406\u8bba",
            "\u8df5\u8bfe",
            "\u6307\u5bfc",
            "\u6307\u5bfc1",
            "\u6307\u5bfc2",
            "\u8bfe\u7a0b",
            "\u5b66\u5206",
            "\u5b66\u65f6",
        )

        private val WHITESPACE_REGEX = Regex("\\s+")
        private val TIME_OR_NUMBER_REGEX = Regex("^\\d{1,2}(?::\\d{2})?$")
        private val LEADING_NOISE_REGEX = Regex("^[^\\u4e00-\\u9fffA-Za-z0-9]+")
        private val TRAILING_SYMBOL_REGEX = Regex("[*&@]+$")
        private val PREFIX_NOISE_REGEX = Regex("^[:\uff1a0-9.,，]+")
        private val NOISE_ONLY_REGEX = Regex("^[:\uff1a0-9.,，]+$")
        private val SHORT_CJK_OR_NUM_REGEX = Regex("^[\\u4e00-\\u9fff0-9]+$")
    }
}

private data class DayAnchor(
    val day: Int,
    val x: Float,
    val top: Float,
    val bottom: Float,
)

private data class DayBound(
    val day: Int,
    val left: Float,
    val right: Float,
)

private data class LineItem(
    val top: Float,
    val bottom: Float,
    val text: String,
)

private data class MutableCourse(
    val page: Int,
    val day: Int,
    val startPeriod: Int,
    val endPeriod: Int,
    var weekPattern: String,
    var name: String,
    var location: String,
    var teacher: String,
    var rawText: String,
)
