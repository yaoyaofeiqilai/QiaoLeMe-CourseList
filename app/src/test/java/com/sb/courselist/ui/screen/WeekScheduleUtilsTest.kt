package com.sb.courselist.ui.screen

import com.sb.courselist.domain.model.CourseEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeekScheduleUtilsTest {

    @Test
    fun isCourseInWeek_supportsAllAndRange() {
        assertTrue(isCourseInWeek("all", 6))
        assertTrue(isCourseInWeek("1-16week", 6))
        assertFalse(isCourseInWeek("1-16week", 18))
    }

    @Test
    fun isCourseInWeek_supportsDiscreteWeeks() {
        assertTrue(isCourseInWeek("6,8,10-12", 8))
        assertTrue(isCourseInWeek("6,8,10-12", 11))
        assertFalse(isCourseInWeek("6,8,10-12", 9))
    }

    @Test
    fun isCourseInWeek_supportsOddEven() {
        val odd = "1-16\u5468\u5355"
        val even = "2-16\u5468\u53cc"
        assertTrue(isCourseInWeek(odd, 7))
        assertFalse(isCourseInWeek(odd, 8))
        assertTrue(isCourseInWeek(even, 8))
        assertFalse(isCourseInWeek(even, 7))
    }

    @Test
    fun inferDisplayWeekCount_usesParsedMaxWeek() {
        val courses = listOf(
            course(weekPattern = "1-8"),
            course(weekPattern = "10-18"),
            course(weekPattern = "6,20"),
        )
        assertEquals(20, inferDisplayWeekCount(courses, declaredTotalWeeks = 16))
    }

    @Test
    fun suggestInitialWeek_prefersNearbyWeekWithCourses() {
        val courses = listOf(
            course(weekPattern = "1-3"),
            course(weekPattern = "6-8"),
        )

        assertEquals(
            8,
            suggestInitialWeek(courses = courses, totalWeeks = 20, preferredWeek = 10),
        )
        assertEquals(
            6,
            suggestInitialWeek(courses = courses, totalWeeks = 20, preferredWeek = 6),
        )
    }

    @Test
    fun buildPeriodTimeDisplayMap_fallsBackWhenRangeInvalid() {
        val result = buildPeriodTimeDisplayMap(
            parsed = mapOf(
                1 to "13:35-14:20",
                2 to "18:20",
                10 to "10:05",
            ),
            periodCount = 10,
        )

        assertEquals("13:35-14:20", result[1])
        assertEquals("08:55-09:40", result[2])
        assertEquals("19:55-20:40", result[10])
    }

    @Test
    fun currentWeekFromTermStart_calculatesByDateDelta() {
        val termStart = 20000L
        assertEquals(1, currentWeekFromTermStart(termStart, totalWeeks = 20, todayEpochDay = 20000L))
        assertEquals(2, currentWeekFromTermStart(termStart, totalWeeks = 20, todayEpochDay = 20007L))
        assertEquals(5, currentWeekFromTermStart(termStart, totalWeeks = 20, todayEpochDay = 20031L))
        assertEquals(1, currentWeekFromTermStart(termStart, totalWeeks = 20, todayEpochDay = 19990L))
        assertEquals(20, currentWeekFromTermStart(termStart, totalWeeks = 20, todayEpochDay = 20300L))
    }

    private fun course(weekPattern: String): CourseEntry {
        return CourseEntry(
            id = 1L,
            name = "Course",
            teacher = "Teacher",
            location = "Room",
            dayOfWeek = 1,
            startPeriod = 1,
            endPeriod = 2,
            weekPattern = weekPattern,
        )
    }
}
