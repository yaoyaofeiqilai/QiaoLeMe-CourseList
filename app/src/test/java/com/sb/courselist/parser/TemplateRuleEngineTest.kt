package com.sb.courselist.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateRuleEngineTest {
    private val engine = TemplateRuleEngine()

    @Test
    fun parse_buildsCoursesWhenAnchorsAndBlocksExist() {
        val tokens = listOf(
            TextToken("\u4e00", 120f, 20f, 8f, 8f, 1),
            TextToken("\u4e8c", 200f, 20f, 8f, 8f, 1),
            TextToken("\u4e09", 280f, 20f, 8f, 8f, 1),
            TextToken("\u56db", 360f, 20f, 8f, 8f, 1),
            TextToken("\u4e94", 440f, 20f, 8f, 8f, 1),
            TextToken("\u516d", 520f, 20f, 8f, 8f, 1),
            TextToken("\u65e5", 600f, 20f, 8f, 8f, 1),
            TextToken("1", 32f, 80f, 8f, 8f, 1),
            TextToken("2", 32f, 140f, 8f, 8f, 1),
            TextToken("3", 32f, 200f, 8f, 8f, 1),
            TextToken("4", 32f, 260f, 8f, 8f, 1),
            TextToken("LinearAlgebra", 120f, 90f, 40f, 10f, 1),
            TextToken("(1-2\u8282)1-16\u5468/\u573a\u5730:A301/\u6559\u5e08:ZhangSan", 120f, 104f, 60f, 8f, 1),
        )

        val result = engine.parse(tokens, sourceTag = "unit-test")
        assertNotNull(result)
        assertEquals(1, result!!.courses.size)
        assertEquals("LinearAlgebra", result.courses.first().name)
        assertEquals(1, result.courses.first().dayOfWeek)
    }

    @Test
    fun parse_balancesMoocParenthesisInCourseName() {
        val tokens = baseHeaderTokens() + listOf(
            TextToken("\u4eba\u662f\u5982\u4f55\u5b66\u4e60\u7684\uff08\u6155\u8bfe", 340f, 90f, 60f, 10f, 1),
            TextToken(
                "(9-10\u8282)12-13\u5468/\u573a\u5730:\u4e2d\u56fd\u5927\u5b66Mooc/\u6559\u5e08:\u675c\u7389\u971e",
                340f,
                104f,
                80f,
                8f,
                1,
            ),
        )

        val result = engine.parse(tokens, sourceTag = "unit-test")
        assertNotNull(result)
        val target = result!!.courses.first { it.dayOfWeek == 3 && it.startPeriod == 9 && it.endPeriod == 10 }
        assertEquals("\u4eba\u662f\u5982\u4f55\u5b66\u4e60\u7684\uff08\u6155\u8bfe\uff09", target.name)
    }

    @Test
    fun parse_fillsMissingNameByTeacherAndLocation() {
        val tokens = baseHeaderTokens() + listOf(
            TextToken("\u4e91\u5e73\u53f0\u4e0e\u5927\u6570\u636e", 260f, 90f, 40f, 10f, 1),
            TextToken(
                "(1-2\u8282)1-8\u5468/\u573a\u5730:\u6587\u6e0a306/\u6559\u5e08:\u738b\u5065\u96c4",
                260f,
                104f,
                80f,
                8f,
                1,
            ),
            TextToken(
                "(5-7\u8282)1-8\u5468/\u573a\u5730:\u6587\u6e0a306/\u6559\u5e08:\u738b\u5065\u96c4",
                260f,
                160f,
                80f,
                8f,
                1,
            ),
        )

        val result = engine.parse(tokens, sourceTag = "unit-test")
        assertNotNull(result)
        val recovered = result!!.courses.first { it.dayOfWeek == 2 && it.startPeriod == 5 && it.endPeriod == 7 }
        assertEquals("\u4e91\u5e73\u53f0\u4e0e\u5927\u6570\u636e", recovered.name)
        assertEquals("\u6587\u6e0a306", recovered.location)
        assertTrue(recovered.teacher.startsWith("\u738b"))
    }

    @Test
    fun parse_extractsPeriodTimesFromLeftAxis() {
        val tokens = baseHeaderTokens() + listOf(
            TextToken("08:00", 12f, 80f, 20f, 8f, 1),
            TextToken("08:45", 12f, 92f, 20f, 8f, 1),
            TextToken("09:00", 12f, 140f, 20f, 8f, 1),
            TextToken("09:45", 12f, 152f, 20f, 8f, 1),
            TextToken("\u79bb\u6563\u6570\u5b66", 200f, 90f, 40f, 10f, 1),
            TextToken("(1-2\u8282)1-16\u5468/\u573a\u5730:\u6587\u6e0a305/\u6559\u5e08:\u5510\u857e", 200f, 104f, 80f, 8f, 1),
        )

        val result = engine.parse(tokens, sourceTag = "unit-test")
        assertNotNull(result)
        assertEquals("08:00-08:45", result!!.meta.periodTimes[1])
        assertEquals("09:00-09:45", result.meta.periodTimes[2])
    }

    @Test
    fun parse_extractsPeriodTimes_ignoreHeaderNoiseTime() {
        val tokens = baseHeaderTokens() + listOf(
            TextToken("13:30", 10f, 34f, 20f, 8f, 1),
            TextToken("14:05", 10f, 44f, 20f, 8f, 1),
            TextToken("08:00", 12f, 80f, 20f, 8f, 1),
            TextToken("08:45", 12f, 92f, 20f, 8f, 1),
            TextToken("09:00", 12f, 140f, 20f, 8f, 1),
            TextToken("09:45", 12f, 152f, 20f, 8f, 1),
            TextToken("\u8ba1\u7b97\u673a\u89c6\u89c9", 200f, 90f, 40f, 10f, 1),
            TextToken("(1-2\u8282)1-16\u5468/\u573a\u5730:\u6587\u6e0a305/\u6559\u5e08:\u5510\u857e", 200f, 104f, 80f, 8f, 1),
        )

        val result = engine.parse(tokens, sourceTag = "unit-test")
        assertNotNull(result)
        assertEquals("08:00-08:45", result!!.meta.periodTimes[1])
        assertEquals("09:00-09:45", result.meta.periodTimes[2])
    }

    @Test
    fun parse_extractsPeriodTimes_acrossMultiplePages() {
        val page1 = baseHeaderTokens(page = 1) + listOf(
            TextToken("08:30", 12f, 90f, 20f, 8f, 1),
            TextToken("09:15", 12f, 102f, 20f, 8f, 1),
            TextToken("离散数学", 200f, 90f, 40f, 10f, 1),
            TextToken("(1-2节)1-16周/场地:文渊305/教师:唐蕾", 200f, 104f, 80f, 8f, 1),
        )
        val page2 = listOf(
            TextToken("5", 32f, 18f, 8f, 8f, 2),
            TextToken("13:50", 12f, 30f, 20f, 8f, 2),
            TextToken("14:35", 12f, 42f, 20f, 8f, 2),
            TextToken("6", 32f, 196f, 8f, 8f, 2),
            TextToken("14:40", 12f, 208f, 20f, 8f, 2),
            TextToken("15:25", 12f, 220f, 20f, 8f, 2),
            TextToken("编译原理", 200f, 120f, 40f, 10f, 2),
            TextToken("(5-6节)1-16周/场地:文新205/教师:谢延昭", 200f, 134f, 80f, 8f, 2),
        )

        val result = engine.parse(page1 + page2, sourceTag = "unit-test")
        assertNotNull(result)
        assertEquals("08:30-09:15", result!!.meta.periodTimes[1])
        assertEquals("13:50-14:35", result.meta.periodTimes[5])
        assertEquals("14:40-15:25", result.meta.periodTimes[6])
    }

    @Test
    fun parse_extractsFirstPeriod_whenCloseToHeader() {
        val tokens = listOf(
            TextToken("一", 120f, 80f, 8f, 12f, 1),
            TextToken("二", 200f, 80f, 8f, 12f, 1),
            TextToken("三", 280f, 80f, 8f, 12f, 1),
            TextToken("四", 360f, 80f, 8f, 12f, 1),
            TextToken("五", 440f, 80f, 8f, 12f, 1),
            TextToken("六", 520f, 80f, 8f, 12f, 1),
            TextToken("日", 600f, 80f, 8f, 12f, 1),
            TextToken("1", 32f, 98f, 8f, 8f, 1),
            TextToken("08:30", 12f, 112f, 20f, 8f, 1),
            TextToken("09:15", 12f, 124f, 20f, 8f, 1),
            TextToken("2", 32f, 170f, 8f, 8f, 1),
            TextToken("09:20", 12f, 182f, 20f, 8f, 1),
            TextToken("10:05", 12f, 194f, 20f, 8f, 1),
            TextToken("物联网技术", 200f, 108f, 60f, 10f, 1),
            TextToken("(1-2节)1-16周/地点:文新205/教师:唐琳", 200f, 122f, 120f, 8f, 1),
        )

        val result = engine.parse(tokens, sourceTag = "unit-test")
        assertNotNull(result)
        assertEquals("08:30-09:15", result!!.meta.periodTimes[1])
        assertEquals("09:20-10:05", result.meta.periodTimes[2])
    }

    @Test
    fun parse_extractsPeriodTimes_whenAxisIsSplitAcrossPages() {
        val page1 = dayHeaderOnlyTokens(page = 1) + listOf(
            TextToken("1", 32f, 80f, 8f, 8f, 1),
            TextToken("08:30", 12f, 92f, 20f, 8f, 1),
            TextToken("09:15", 12f, 104f, 20f, 8f, 1),
            TextToken("2", 32f, 150f, 8f, 8f, 1),
            TextToken("09:20", 12f, 162f, 20f, 8f, 1),
            TextToken("10:05", 12f, 174f, 20f, 8f, 1),
            TextToken("3", 32f, 220f, 8f, 8f, 1),
            TextToken("10:25", 12f, 232f, 20f, 8f, 1),
            TextToken("11:10", 12f, 244f, 20f, 8f, 1),
            TextToken("4", 32f, 290f, 8f, 8f, 1),
            TextToken("11:15", 12f, 302f, 20f, 8f, 1),
            TextToken("12:00", 12f, 314f, 20f, 8f, 1),
            TextToken("DigitalLogic", 200f, 96f, 50f, 10f, 1),
            TextToken("(1-2\u8282)1-16\u5468/\u573a\u5730:A101/\u6559\u5e08:Li", 200f, 110f, 90f, 8f, 1),
        )
        val page2 = listOf(
            TextToken("5", 32f, 90f, 8f, 8f, 2),
            TextToken("13:50", 12f, 102f, 20f, 8f, 2),
            TextToken("14:35", 12f, 114f, 20f, 8f, 2),
            TextToken("6", 32f, 160f, 8f, 8f, 2),
            TextToken("14:40", 12f, 172f, 20f, 8f, 2),
            TextToken("15:25", 12f, 184f, 20f, 8f, 2),
            TextToken("7", 32f, 230f, 8f, 8f, 2),
            TextToken("15:45", 12f, 366f, 20f, 8f, 2),
            TextToken("16:30", 12f, 378f, 20f, 8f, 2),
            TextToken("placeholder", 280f, 180f, 60f, 8f, 2),
        )
        val page3 = listOf(
            TextToken("2", 132f, 30f, 8f, 8f, 3),
            TextToken("8", 32f, 90f, 8f, 8f, 3),
            TextToken("16:35", 12f, 102f, 20f, 8f, 3),
            TextToken("17:20", 12f, 114f, 20f, 8f, 3),
            TextToken("9", 32f, 160f, 8f, 8f, 3),
            TextToken("18:20", 12f, 172f, 20f, 8f, 3),
            TextToken("19:05", 12f, 184f, 20f, 8f, 3),
            TextToken("10", 32f, 230f, 12f, 8f, 3),
            TextToken("19:10", 12f, 242f, 20f, 8f, 3),
            TextToken("19:55", 12f, 254f, 20f, 8f, 3),
            TextToken("11", 32f, 300f, 12f, 8f, 3),
            TextToken("20:00", 12f, 312f, 20f, 8f, 3),
            TextToken("20:45", 12f, 324f, 20f, 8f, 3),
            TextToken("placeholder", 280f, 210f, 60f, 8f, 3),
        )

        val result = engine.parse(page1 + page2 + page3, sourceTag = "unit-test")
        assertNotNull(result)
        assertEquals("08:30-09:15", result!!.meta.periodTimes[1])
        assertEquals("09:20-10:05", result.meta.periodTimes[2])
        assertEquals("10:25-11:10", result.meta.periodTimes[3])
        assertEquals("11:15-12:00", result.meta.periodTimes[4])
        assertEquals("13:50-14:35", result.meta.periodTimes[5])
        assertEquals("14:40-15:25", result.meta.periodTimes[6])
        assertEquals("15:45-16:30", result.meta.periodTimes[7])
        assertEquals("16:35-17:20", result.meta.periodTimes[8])
        assertEquals("18:20-19:05", result.meta.periodTimes[9])
        assertEquals("19:10-19:55", result.meta.periodTimes[10])
        assertEquals("20:00-20:45", result.meta.periodTimes[11])
    }

    @Test
    fun parse_keepsTopCoursesOnAnchorlessContinuationPage() {
        val page1 = listOf(
            TextToken("\u661f\u671f\u65e5", 133f, 63.4f, 36f, 12f, 1),
            TextToken("\u661f\u671f\u4e00", 236.8f, 63.4f, 36f, 12f, 1),
            TextToken("\u661f\u671f\u4e8c", 340.7f, 63.4f, 36f, 12f, 1),
            TextToken("\u661f\u671f\u4e09", 444.5f, 63.4f, 36f, 12f, 1),
            TextToken("\u661f\u671f\u56db", 548.4f, 63.4f, 36f, 12f, 1),
            TextToken("\u661f\u671f\u4e94", 652.2f, 63.4f, 36f, 12f, 1),
            TextToken("\u661f\u671f\u516d", 756.1f, 63.4f, 36f, 12f, 1),
            TextToken("\u81ea\u7136\u8bed\u8a00\u5904\u7406", 236.8f, 81.6f, 60f, 9f, 1),
            TextToken(
                "(1-2\u8282)5-8\u5468/\u5730\u70b9:\u7535\u5b50\u697c416A/\u6559\u5e08:\u6768\u6714",
                236.8f,
                94.5f,
                120f,
                8f,
                1,
            ),
        )
        val page2 = listOf(
            TextToken("\u5b66\u65f6:12/\u5b66\u5206:1", 548.4f, 21f, 45f, 8f, 2),
            TextToken("7", 78.1f, 33.4f, 8f, 12f, 2),
            TextToken("\u81ea\u7136\u8bed\u8a00\u5904\u7406", 236.8f, 33.6f, 58f, 9f, 2),
            TextToken(
                "(6-8\u8282)1-8\u5468/\u5730\u70b9:\u6587\u6e0a406/\u6559\u5e08:\u6768\u6714",
                236.8f,
                46.5f,
                120f,
                8f,
                2,
            ),
            TextToken("8", 78.1f, 94.2f, 8f, 12f, 2),
        )

        val result = engine.parse(page1 + page2, sourceTag = "unit-test")
        assertNotNull(result)

        val mondayNlp = result!!.courses.filter {
            it.dayOfWeek == 1 && it.name.contains("\u81ea\u7136\u8bed\u8a00\u5904\u7406")
        }
        assertTrue(
            mondayNlp.any { it.startPeriod == 1 && it.endPeriod == 2 && it.weekPattern == "5-8\u5468" },
        )
        assertTrue(
            mondayNlp.any { it.startPeriod == 6 && it.endPeriod == 8 && it.weekPattern == "1-8\u5468" },
        )
    }

    private fun dayHeaderOnlyTokens(page: Int = 1): List<TextToken> {
        return listOf(
            TextToken("\u65e5", 120f, 20f, 8f, 8f, page),
            TextToken("\u4e00", 200f, 20f, 8f, 8f, page),
            TextToken("\u4e8c", 280f, 20f, 8f, 8f, page),
            TextToken("\u4e09", 360f, 20f, 8f, 8f, page),
            TextToken("\u56db", 440f, 20f, 8f, 8f, page),
            TextToken("\u4e94", 520f, 20f, 8f, 8f, page),
            TextToken("\u516d", 600f, 20f, 8f, 8f, page),
        )
    }

    private fun baseHeaderTokens(page: Int = 1): List<TextToken> {
        return listOf(
            TextToken("\u65e5", 120f, 20f, 8f, 8f, page),
            TextToken("\u4e00", 200f, 20f, 8f, 8f, page),
            TextToken("\u4e8c", 280f, 20f, 8f, 8f, page),
            TextToken("\u4e09", 360f, 20f, 8f, 8f, page),
            TextToken("\u56db", 440f, 20f, 8f, 8f, page),
            TextToken("\u4e94", 520f, 20f, 8f, 8f, page),
            TextToken("\u516d", 600f, 20f, 8f, 8f, page),
            TextToken("1", 32f, 80f, 8f, 8f, page),
            TextToken("2", 32f, 140f, 8f, 8f, page),
            TextToken("3", 32f, 200f, 8f, 8f, page),
            TextToken("4", 32f, 260f, 8f, 8f, page),
            TextToken("5", 32f, 320f, 8f, 8f, page),
            TextToken("6", 32f, 380f, 8f, 8f, page),
            TextToken("7", 32f, 440f, 8f, 8f, page),
            TextToken("8", 32f, 500f, 8f, 8f, page),
            TextToken("9", 32f, 560f, 8f, 8f, page),
            TextToken("10", 32f, 620f, 8f, 8f, page),
        )
    }
}
