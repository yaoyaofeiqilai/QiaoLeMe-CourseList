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

    private fun baseHeaderTokens(): List<TextToken> {
        return listOf(
            TextToken("\u65e5", 120f, 20f, 8f, 8f, 1),
            TextToken("\u4e00", 200f, 20f, 8f, 8f, 1),
            TextToken("\u4e8c", 280f, 20f, 8f, 8f, 1),
            TextToken("\u4e09", 360f, 20f, 8f, 8f, 1),
            TextToken("\u56db", 440f, 20f, 8f, 8f, 1),
            TextToken("\u4e94", 520f, 20f, 8f, 8f, 1),
            TextToken("\u516d", 600f, 20f, 8f, 8f, 1),
            TextToken("1", 32f, 80f, 8f, 8f, 1),
            TextToken("2", 32f, 140f, 8f, 8f, 1),
            TextToken("3", 32f, 200f, 8f, 8f, 1),
            TextToken("4", 32f, 260f, 8f, 8f, 1),
            TextToken("5", 32f, 320f, 8f, 8f, 1),
            TextToken("6", 32f, 380f, 8f, 8f, 1),
            TextToken("7", 32f, 440f, 8f, 8f, 1),
            TextToken("8", 32f, 500f, 8f, 8f, 1),
            TextToken("9", 32f, 560f, 8f, 8f, 1),
            TextToken("10", 32f, 620f, 8f, 8f, 1),
        )
    }
}
