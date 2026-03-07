package com.potlog

import com.potlog.models.PokerSession
import com.potlog.services.SessionService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AdminPinTest {

    private val sessionService = SessionService()

    private fun session(adminOnly: Boolean, adminPassword: String?) = PokerSession(
        numericId = "123456",
        stakes = "1/2",
        adminOnly = adminOnly,
        adminPassword = adminPassword
    )

    @Test
    fun `verifyAdmin - non adminOnly session always returns true`() {
        val s = session(adminOnly = false, adminPassword = null)
        assertEquals(true, sessionService.verifyAdmin(s, null))
        assertEquals(true, sessionService.verifyAdmin(s, ""))
        assertEquals(true, sessionService.verifyAdmin(s, "1234"))
    }

    @Test
    fun `verifyAdmin - adminOnly with correct PIN returns true`() {
        val s = session(adminOnly = true, adminPassword = "1234")
        assertEquals(true, sessionService.verifyAdmin(s, "1234"))
    }

    @Test
    fun `verifyAdmin - adminOnly with wrong PIN returns false`() {
        val s = session(adminOnly = true, adminPassword = "1234")
        assertEquals(false, sessionService.verifyAdmin(s, "0000"))
        assertEquals(false, sessionService.verifyAdmin(s, null))
        assertEquals(false, sessionService.verifyAdmin(s, ""))
    }

    @Test
    fun `verifyAdmin - PIN 0000 accepts 0, 00, 000, 0000`() {
        val s = session(adminOnly = true, adminPassword = "0000")
        assertEquals(true, sessionService.verifyAdmin(s, "0"))
        assertEquals(true, sessionService.verifyAdmin(s, "00"))
        assertEquals(true, sessionService.verifyAdmin(s, "000"))
        assertEquals(true, sessionService.verifyAdmin(s, "0000"))
    }

    @Test
    fun `verifyAdmin - stored 0 accepts 0000 format`() {
        val s = session(adminOnly = true, adminPassword = "0")
        assertEquals(true, sessionService.verifyAdmin(s, "0000"))
        assertEquals(true, sessionService.verifyAdmin(s, "0"))
    }

    @Test
    fun `verifyAdmin - PIN 0042 accepts 42 and 0042`() {
        val s = session(adminOnly = true, adminPassword = "0042")
        assertEquals(true, sessionService.verifyAdmin(s, "42"))
        assertEquals(true, sessionService.verifyAdmin(s, "0042"))
    }

    @Test
    fun `requireAdmin - adminOnly with correct PIN does not throw`() {
        val s = session(adminOnly = true, adminPassword = "1234")
        sessionService.requireAdmin(s, "1234")
    }

    @Test
    fun `requireAdmin - adminOnly with wrong PIN throws IllegalAccessException`() {
        val s = session(adminOnly = true, adminPassword = "1234")
        assertFailsWith<IllegalAccessException> {
            sessionService.requireAdmin(s, "0000")
        }
    }

    @Test
    fun `requireAdmin - adminOnly with null throws IllegalAccessException`() {
        val s = session(adminOnly = true, adminPassword = "1234")
        assertFailsWith<IllegalAccessException> {
            sessionService.requireAdmin(s, null)
        }
    }
}
