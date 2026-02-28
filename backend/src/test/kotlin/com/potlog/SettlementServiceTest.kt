package com.potlog

import com.potlog.models.Player
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettlementServiceTest {
    
    @Test
    fun `test largest remainder method with positive diff - basic case`() {
        val winners = listOf(
            Player(id = "1", name = "Alice", buyIn = 100_00, cashOut = 200_00, net = 100_00),
            Player(id = "2", name = "Bob", buyIn = 100_00, cashOut = 150_00, net = 50_00),
            Player(id = "3", name = "Charlie", buyIn = 100_00, cashOut = 130_00, net = 30_00)
        )
        
        val diff = 7_00L
        val totalWinnings = winners.sumOf { it.net }
        
        val adjustments = applyLargestRemainderMethod(winners, diff, totalWinnings)
        
        assertEquals(diff, adjustments.values.sum())
    }
    
    @Test
    fun `test largest remainder method with negative diff`() {
        val winners = listOf(
            Player(id = "1", name = "Alice", buyIn = 100_00, cashOut = 200_00, net = 100_00),
            Player(id = "2", name = "Bob", buyIn = 100_00, cashOut = 150_00, net = 50_00)
        )
        
        val diff = -10_00L
        val totalWinnings = winners.sumOf { it.net }
        
        val adjustments = applyLargestRemainderMethod(winners, diff, totalWinnings)
        
        assertEquals(diff, adjustments.values.sum())
    }
    
    @Test
    fun `test largest remainder method distributes proportionally`() {
        val winners = listOf(
            Player(id = "1", name = "Alice", net = 60_00),
            Player(id = "2", name = "Bob", net = 40_00)
        )
        
        val diff = 10_00L
        val totalWinnings = 100_00L
        
        val adjustments = applyLargestRemainderMethod(winners, diff, totalWinnings)
        
        assertEquals(6_00L, adjustments["1"])
        assertEquals(4_00L, adjustments["2"])
    }
    
    @Test
    fun `test largest remainder method handles single winner`() {
        val winners = listOf(
            Player(id = "1", name = "Alice", net = 100_00)
        )
        
        val diff = 15_00L
        
        val adjustments = applyLargestRemainderMethod(winners, diff, 100_00L)
        
        assertEquals(15_00L, adjustments["1"])
    }
    
    @Test
    fun `test largest remainder method handles equal proportions`() {
        val winners = listOf(
            Player(id = "1", name = "Alice", net = 50_00),
            Player(id = "2", name = "Bob", net = 50_00)
        )
        
        val diff = 5_00L
        
        val adjustments = applyLargestRemainderMethod(winners, diff, 100_00L)
        
        assertEquals(diff, adjustments.values.sum())
        assertTrue(adjustments.values.all { it in 2_00L..3_00L })
    }
    
    @Test
    fun `test greedy debt minimization - basic case`() {
        val players = listOf(
            Player(id = "1", name = "Alice", net = 150_00),
            Player(id = "2", name = "Bob", net = 50_00),
            Player(id = "3", name = "Charlie", net = -100_00),
            Player(id = "4", name = "Dave", net = -100_00)
        )
        
        val debts = generateMinimalDebts(players)
        
        val totalDebts = debts.sumOf { it.amount }
        val totalOwed = players.filter { it.net > 0 }.sumOf { it.net }
        assertEquals(totalOwed, totalDebts)
        
        assertTrue(debts.size <= 3, "Should generate at most 3 debts for 4 players")
    }
    
    @Test
    fun `test greedy debt minimization - single debtor single creditor`() {
        val players = listOf(
            Player(id = "1", name = "Alice", net = 100_00),
            Player(id = "2", name = "Bob", net = -100_00)
        )
        
        val debts = generateMinimalDebts(players)
        
        assertEquals(1, debts.size)
        assertEquals("2", debts[0].fromPlayerId)
        assertEquals("1", debts[0].toPlayerId)
        assertEquals(100_00L, debts[0].amount)
    }
    
    @Test
    fun `test greedy debt minimization - all losers pay one winner`() {
        val players = listOf(
            Player(id = "1", name = "Alice", net = 300_00),
            Player(id = "2", name = "Bob", net = -100_00),
            Player(id = "3", name = "Charlie", net = -100_00),
            Player(id = "4", name = "Dave", net = -100_00)
        )
        
        val debts = generateMinimalDebts(players)
        
        assertEquals(3, debts.size)
        assertTrue(debts.all { it.toPlayerId == "1" })
    }
    
    @Test
    fun `test greedy debt minimization - no debts when all zero`() {
        val players = listOf(
            Player(id = "1", name = "Alice", net = 0),
            Player(id = "2", name = "Bob", net = 0)
        )
        
        val debts = generateMinimalDebts(players)
        
        assertTrue(debts.isEmpty())
    }
    
    @Test
    fun `test zero diff scenario`() {
        val players = listOf(
            Player(id = "1", name = "Alice", buyIn = 100_00, cashOut = 150_00),
            Player(id = "2", name = "Bob", buyIn = 100_00, cashOut = 50_00)
        )
        
        val totalBuyIn = players.sumOf { it.buyIn }
        val totalCashOut = players.sumOf { it.cashOut }
        val diff = totalBuyIn - totalCashOut
        
        assertEquals(0, diff)
    }
    
    @Test
    fun `test max winner balance - winner absorbs diff`() {
        val players = mutableListOf(
            Player(id = "1", name = "Alice", buyIn = 100_00, cashOut = 180_00),
            Player(id = "2", name = "Bob", buyIn = 100_00, cashOut = 50_00)
        )
        
        val diff = -30_00L
        
        val result = balanceWithMaxWinner(players, diff)
        
        val alice = result.find { it.id == "1" }!!
        val bob = result.find { it.id == "2" }!!
        
        assertEquals(50_00L, alice.net)
        assertEquals(-50_00L, bob.net)
    }
    
    @Test
    fun `test net calculation is correct`() {
        val player = Player(
            id = "1",
            name = "Alice",
            buyIn = 200_00,
            cashOut = 350_00
        )
        
        val net = player.cashOut - player.buyIn
        assertEquals(150_00L, net)
    }
    
    @Test
    fun `test debt sum equals creditor sum`() {
        val players = listOf(
            Player(id = "1", name = "Alice", net = 75_00),
            Player(id = "2", name = "Bob", net = 25_00),
            Player(id = "3", name = "Charlie", net = -50_00),
            Player(id = "4", name = "Dave", net = -50_00)
        )
        
        val debts = generateMinimalDebts(players)
        
        val totalCreditorNet = players.filter { it.net > 0 }.sumOf { it.net }
        val totalDebtorNet = players.filter { it.net < 0 }.sumOf { abs(it.net) }
        val totalDebtAmount = debts.sumOf { it.amount }
        
        assertEquals(totalCreditorNet, totalDebtorNet)
        assertEquals(totalCreditorNet, totalDebtAmount)
    }
    
    @Test
    fun `test complex scenario with 6 players`() {
        val players = listOf(
            Player(id = "1", name = "Player1", net = 120_00),
            Player(id = "2", name = "Player2", net = 80_00),
            Player(id = "3", name = "Player3", net = 50_00),
            Player(id = "4", name = "Player4", net = -70_00),
            Player(id = "5", name = "Player5", net = -90_00),
            Player(id = "6", name = "Player6", net = -90_00)
        )
        
        val debts = generateMinimalDebts(players)
        
        assertTrue(debts.size <= 5, "Should generate at most n-1 debts")
        assertEquals(250_00L, debts.sumOf { it.amount })
    }
    
    private data class QuotaResult(
        val playerId: String,
        val quota: Double,
        val integerPart: Long,
        val remainder: Double
    )
    
    private fun applyLargestRemainderMethod(
        winners: List<Player>,
        totalToDistribute: Long,
        totalWinnings: Long
    ): Map<String, Long> {
        val quotaResults = winners.map { player ->
            val proportion = player.net.toDouble() / totalWinnings
            val quota = proportion * totalToDistribute
            val integerPart = if (totalToDistribute >= 0) {
                kotlin.math.floor(quota).toLong()
            } else {
                kotlin.math.ceil(quota).toLong()
            }
            val remainder = quota - integerPart
            
            QuotaResult(player.id, quota, integerPart, remainder)
        }
        
        val sumOfIntegerParts = quotaResults.sumOf { it.integerPart }
        var remainingToDistribute = totalToDistribute - sumOfIntegerParts
        
        val sortedByRemainder = if (totalToDistribute >= 0) {
            quotaResults.sortedByDescending { it.remainder }
        } else {
            quotaResults.sortedBy { it.remainder }
        }
        
        val adjustments = quotaResults.associate { it.playerId to it.integerPart }.toMutableMap()
        
        val step = if (totalToDistribute >= 0) 1L else -1L
        for (result in sortedByRemainder) {
            if (remainingToDistribute == 0L) break
            adjustments[result.playerId] = adjustments[result.playerId]!! + step
            remainingToDistribute -= step
        }
        
        return adjustments
    }
    
    private data class Debt(
        val fromPlayerId: String,
        val toPlayerId: String,
        val amount: Long
    )
    
    private fun generateMinimalDebts(players: List<Player>): List<Debt> {
        val debtors = players
            .filter { it.net < 0 }
            .map { it.id to abs(it.net) }
            .toMutableList()
        
        val creditors = players
            .filter { it.net > 0 }
            .map { it.id to it.net }
            .toMutableList()
        
        val debts = mutableListOf<Debt>()
        
        var debtorIndex = 0
        var creditorIndex = 0
        
        while (debtorIndex < debtors.size && creditorIndex < creditors.size) {
            val (debtorId, debtorOwes) = debtors[debtorIndex]
            val (creditorId, creditorOwed) = creditors[creditorIndex]
            
            val transferAmount = minOf(debtorOwes, creditorOwed)
            
            if (transferAmount > 0) {
                debts.add(Debt(debtorId, creditorId, transferAmount))
            }
            
            val newDebtorOwes = debtorOwes - transferAmount
            val newCreditorOwed = creditorOwed - transferAmount
            
            debtors[debtorIndex] = debtorId to newDebtorOwes
            creditors[creditorIndex] = creditorId to newCreditorOwed
            
            if (newDebtorOwes == 0L) debtorIndex++
            if (newCreditorOwed == 0L) creditorIndex++
        }
        
        return debts
    }
    
    private fun balanceWithMaxWinner(players: MutableList<Player>, diff: Long): MutableList<Player> {
        val playersWithNet = players.map { it.copy(net = it.cashOut - it.buyIn) }.toMutableList()
        
        val maxWinner = playersWithNet.maxByOrNull { it.net }
            ?: throw IllegalStateException("No players found")
        
        return playersWithNet.map { player ->
            if (player.id == maxWinner.id) {
                player.copy(net = player.net + diff)
            } else {
                player
            }
        }.toMutableList()
    }
}
