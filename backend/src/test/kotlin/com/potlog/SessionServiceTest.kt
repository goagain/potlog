package com.potlog

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionServiceTest {

    companion object {
        private const val MIN_NUMERIC_ID = 100_000L
        private const val MAX_NUMERIC_ID = 9_999_999_999L
    }

    @Test
    fun `test numeric id generation is 6 to 10 digits`() {
        repeat(100) {
            val id = generateNumericId()
            assertTrue(id.length in 6..10, "ID should be 6-10 digits, got ${id.length}")
            assertTrue(id.all { it.isDigit() }, "ID should contain only digits")
        }
    }

    @Test
    fun `test numeric id is within valid range`() {
        repeat(100) {
            val id = generateNumericId()
            val numericValue = id.toLong()
            assertTrue(numericValue in MIN_NUMERIC_ID..MAX_NUMERIC_ID)
        }
    }

    @Test
    fun `test numeric id generation randomness`() {
        val ids = (1..1000).map { generateNumericId() }.toSet()
        assertTrue(ids.size > 900, "Should generate mostly unique IDs")
    }

    @Test
    fun `test numeric id does not start with zero`() {
        repeat(100) {
            val id = generateNumericId()
            assertTrue(id[0] != '0', "ID should not start with zero")
        }
    }

    private fun generateNumericId(): String {
        val randomNumber = Random.nextLong(MIN_NUMERIC_ID, MAX_NUMERIC_ID + 1)
        return randomNumber.toString()
    }
}
