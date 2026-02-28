package com.potlog

import com.potlog.models.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ModelsTest {
    
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    @Test
    fun `test Player serialization`() {
        val player = Player(
            id = "test-id",
            name = "Alice",
            buyIn = 200_00,
            cashOut = 350_00,
            net = 150_00,
            userId = "user-123"
        )
        
        val jsonString = json.encodeToString(player)
        val decoded = json.decodeFromString<Player>(jsonString)
        
        assertEquals(player.id, decoded.id)
        assertEquals(player.name, decoded.name)
        assertEquals(player.buyIn, decoded.buyIn)
        assertEquals(player.cashOut, decoded.cashOut)
        assertEquals(player.net, decoded.net)
        assertEquals(player.userId, decoded.userId)
    }
    
    @Test
    fun `test Player default values`() {
        val player = Player(id = "1", name = "Bob")
        
        assertEquals(0L, player.buyIn)
        assertEquals(0L, player.cashOut)
        assertEquals(0L, player.net)
        assertEquals(null, player.userId)
    }
    
    @Test
    fun `test TransactionLog creation`() {
        val log = TransactionLog(
            playerId = "player-1",
            type = TransactionType.BUY_IN,
            amount = 100_00
        )
        
        assertNotNull(log.id)
        assertTrue(log.timestamp > 0)
        assertEquals("player-1", log.playerId)
        assertEquals(TransactionType.BUY_IN, log.type)
        assertEquals(100_00L, log.amount)
    }
    
    @Test
    fun `test Debt default values`() {
        val debt = Debt(
            fromPlayerId = "player-1",
            toPlayerId = "player-2",
            amount = 50_00
        )
        
        assertNotNull(debt.id)
        assertEquals(false, debt.settled)
        assertEquals(0L, debt.settledAmount)
    }
    
    @Test
    fun `test SessionStatus enum values`() {
        assertEquals(2, SessionStatus.values().size)
        assertTrue(SessionStatus.values().contains(SessionStatus.ACTIVE))
        assertTrue(SessionStatus.values().contains(SessionStatus.SETTLED))
    }
    
    @Test
    fun `test TransactionType enum values`() {
        assertEquals(4, TransactionType.values().size)
        assertTrue(TransactionType.values().contains(TransactionType.BUY_IN))
        assertTrue(TransactionType.values().contains(TransactionType.REBUY))
        assertTrue(TransactionType.values().contains(TransactionType.MANUAL_TRANSFER))
        assertTrue(TransactionType.values().contains(TransactionType.CASH_OUT))
    }
    
    @Test
    fun `test BalanceMode enum values`() {
        assertEquals(2, BalanceMode.values().size)
        assertTrue(BalanceMode.values().contains(BalanceMode.MAX_WINNER))
        assertTrue(BalanceMode.values().contains(BalanceMode.PROPORTIONAL))
    }
    
    @Test
    fun `test CreateSessionRequest serialization`() {
        val request = CreateSessionRequest(stakes = "1/2")
        val jsonString = json.encodeToString(request)
        val decoded = json.decodeFromString<CreateSessionRequest>(jsonString)
        
        assertEquals("1/2", decoded.stakes)
    }
    
    @Test
    fun `test AddPlayerRequest serialization`() {
        val request = AddPlayerRequest(
            name = "Charlie",
            initialBuyIn = 200_00,
            userId = "user-456"
        )
        
        val jsonString = json.encodeToString(request)
        val decoded = json.decodeFromString<AddPlayerRequest>(jsonString)
        
        assertEquals("Charlie", decoded.name)
        assertEquals(200_00L, decoded.initialBuyIn)
        assertEquals("user-456", decoded.userId)
    }
    
    @Test
    fun `test SettleRequest with default balance mode`() {
        val cashOuts = mapOf("p1" to 100_00L, "p2" to 200_00L)
        val request = SettleRequest(cashOuts = cashOuts)
        
        assertEquals(BalanceMode.MAX_WINNER, request.balanceMode)
    }
    
    @Test
    fun `test UserStats aggregation`() {
        val stats = UserStats(
            userId = "user-123",
            totalNet = 500_00,
            sessionCount = 5,
            sessions = listOf(
                SessionSummary("123456", "1/2", 200_00, System.currentTimeMillis()),
                SessionSummary("789012", "2/5", 300_00, System.currentTimeMillis())
            )
        )
        
        assertEquals("user-123", stats.userId)
        assertEquals(500_00L, stats.totalNet)
        assertEquals(5, stats.sessionCount)
        assertEquals(2, stats.sessions.size)
    }
    
    @Test
    fun `test amount in cents conversion`() {
        val buyInDollars = 200.00
        val buyInCents = (buyInDollars * 100).toLong()
        
        assertEquals(200_00L, buyInCents)
        assertEquals(200.00, buyInCents / 100.0)
    }
    
    @Test
    fun `test cents precision for display`() {
        val amountCents = 12345L
        val displayAmount = "$${amountCents / 100}.${(amountCents % 100).toString().padStart(2, '0')}"
        
        assertEquals("$123.45", displayAmount)
    }
}
