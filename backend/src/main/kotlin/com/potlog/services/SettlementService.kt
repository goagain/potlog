package com.potlog.services

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.potlog.database.MongoDB
import com.potlog.models.*
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import kotlin.math.abs

class SettlementService(private val sessionService: SessionService) {
    private val logger = LoggerFactory.getLogger(SettlementService::class.java)
    
    /**
     * 执行结算
     * 1. 记录每个玩家的 cashOut
     * 2. 计算 diff 并根据 balanceMode 平账
     * 3. 使用贪心算法生成最小化债务列表
     */
    suspend fun settle(numericId: String, request: SettleRequest): PokerSession {
        val session = sessionService.getSession(numericId)
            ?: throw IllegalArgumentException("Session not found: $numericId")
        
        if (session.status != SessionStatus.ACTIVE) {
            throw IllegalStateException("Session already settled")
        }
        
        val updatedPlayers = session.players.map { player ->
            val cashOut = request.cashOuts[player.id]
                ?: throw IllegalArgumentException("Missing cashOut for player: ${player.name}")
            player.copy(cashOut = cashOut)
        }.toMutableList()
        
        val totalBuyIn = updatedPlayers.sumOf { it.buyIn }
        val totalCashOut = updatedPlayers.sumOf { it.cashOut }
        val diff = totalBuyIn - totalCashOut
        
        logger.info("Settlement diff: $diff (buyIn: $totalBuyIn, cashOut: $totalCashOut)")
        
        val balancedPlayers = if (diff != 0L) {
            when (request.balanceMode) {
                BalanceMode.MAX_WINNER -> balanceWithMaxWinner(updatedPlayers, diff)
                BalanceMode.PROPORTIONAL -> balanceProportionally(updatedPlayers, diff)
            }
        } else {
            updatedPlayers.map { it.copy(net = it.cashOut - it.buyIn) }.toMutableList()
        }
        
        val debts = generateMinimalDebtsWithTransfers(balancedPlayers, session.transfers)
        
        MongoDB.sessions.updateOne(
            Filters.eq("numericId", numericId),
            Updates.combine(
                Updates.set("status", SessionStatus.SETTLED.name),
                Updates.set("players", balancedPlayers),
                Updates.set("debts", debts),
                Updates.set("settledAt", System.currentTimeMillis())
            )
        )
        
        return sessionService.getSession(numericId)!!
    }
    
    /**
     * 最大赢家承担差额
     * 将 diff 补偿给 net 最高的玩家
     */
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
    
    /**
     * 比例分摊（最大余数法）
     * 仅针对 net > 0 的赢家，按盈利比例分摊 diff
     * 使用最大余数法确保分摊后为整数且 Sum(adjustments) == diff
     */
    private fun balanceProportionally(players: MutableList<Player>, diff: Long): MutableList<Player> {
        val playersWithNet = players.map { it.copy(net = it.cashOut - it.buyIn) }.toMutableList()
        
        val winners = playersWithNet.filter { it.net > 0 }
        
        if (winners.isEmpty()) {
            return balanceWithMaxWinner(players, diff)
        }
        
        val totalWinnings = winners.sumOf { it.net }
        
        val adjustments = applyLargestRemainderMethod(winners, diff, totalWinnings)
        
        return playersWithNet.map { player ->
            val adjustment = adjustments[player.id] ?: 0L
            player.copy(net = player.net + adjustment)
        }.toMutableList()
    }
    
    /**
     * 最大余数法实现
     * 确保分配后各部分为整数，且总和精确等于目标值
     * 
     * @param winners 赢家列表
     * @param totalToDistribute 需要分配的总金额 (diff)
     * @param totalWinnings 赢家的总盈利
     * @return Map<playerId, adjustment>
     */
    private fun applyLargestRemainderMethod(
        winners: List<Player>,
        totalToDistribute: Long,
        totalWinnings: Long
    ): Map<String, Long> {
        data class QuotaResult(
            val playerId: String,
            val quota: Double,
            val integerPart: Long,
            val remainder: Double
        )
        
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
        
        val finalSum = adjustments.values.sum()
        require(finalSum == totalToDistribute) {
            "Largest remainder method failed: expected $totalToDistribute, got $finalSum"
        }
        
        return adjustments
    }
    
    /**
     * 贪心算法生成最小化债务列表（考虑已有的定向转账）
     * 
     * 1. 先计算每对玩家之间的净转账额
     * 2. 将净转账应用到债务关系中
     * 3. 用贪心算法生成剩余的最小债务列表
     */
    private fun generateMinimalDebtsWithTransfers(
        players: List<Player>, 
        transfers: List<DirectTransfer>
    ): MutableList<Debt> {
        val netBalances = mutableMapOf<String, Long>()
        players.forEach { netBalances[it.id] = it.net }
        
        val transferMatrix = mutableMapOf<Pair<String, String>, Long>()
        
        for (transfer in transfers) {
            val key = transfer.fromPlayerId to transfer.toPlayerId
            val reverseKey = transfer.toPlayerId to transfer.fromPlayerId
            
            if (transferMatrix.containsKey(reverseKey)) {
                val existing = transferMatrix[reverseKey]!!
                if (transfer.amount >= existing) {
                    transferMatrix.remove(reverseKey)
                    if (transfer.amount > existing) {
                        transferMatrix[key] = transfer.amount - existing
                    }
                } else {
                    transferMatrix[reverseKey] = existing - transfer.amount
                }
            } else {
                transferMatrix[key] = (transferMatrix[key] ?: 0L) + transfer.amount
            }
        }
        
        logger.info("Transfer matrix after consolidation: $transferMatrix")
        
        val adjustedBalances = netBalances.toMutableMap()
        
        for ((pair, amount) in transferMatrix) {
            val (from, to) = pair
            adjustedBalances[from] = (adjustedBalances[from] ?: 0L) + amount
            adjustedBalances[to] = (adjustedBalances[to] ?: 0L) - amount
        }
        
        logger.info("Adjusted balances: $adjustedBalances")
        
        val debtors = adjustedBalances
            .filter { it.value < 0 }
            .map { it.key to abs(it.value) }
            .toMutableList()
        
        val creditors = adjustedBalances
            .filter { it.value > 0 }
            .map { it.key to it.value }
            .toMutableList()
        
        val debts = mutableListOf<Debt>()
        
        var debtorIndex = 0
        var creditorIndex = 0
        
        while (debtorIndex < debtors.size && creditorIndex < creditors.size) {
            val (debtorId, debtorOwes) = debtors[debtorIndex]
            val (creditorId, creditorOwed) = creditors[creditorIndex]
            
            val transferAmount = minOf(debtorOwes, creditorOwed)
            
            if (transferAmount > 0) {
                debts.add(
                    Debt(
                        id = ObjectId().toHexString(),
                        fromPlayerId = debtorId,
                        toPlayerId = creditorId,
                        amount = transferAmount
                    )
                )
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
    
    /**
     * 预览结算结果（不实际保存）
     * 用于前端实时显示转账方案
     */
    fun previewSettlement(
        session: PokerSession,
        cashOuts: Map<String, Long>,
        balanceMode: BalanceMode
    ): List<Debt> {
        val updatedPlayers = session.players.map { player ->
            val cashOut = cashOuts[player.id] ?: 0L
            player.copy(cashOut = cashOut)
        }.toMutableList()
        
        val totalBuyIn = updatedPlayers.sumOf { it.buyIn }
        val totalCashOut = updatedPlayers.sumOf { it.cashOut }
        val diff = totalBuyIn - totalCashOut
        
        val balancedPlayers = if (diff != 0L) {
            when (balanceMode) {
                BalanceMode.MAX_WINNER -> balanceWithMaxWinner(updatedPlayers, diff)
                BalanceMode.PROPORTIONAL -> balanceProportionally(updatedPlayers, diff)
            }
        } else {
            updatedPlayers.map { it.copy(net = it.cashOut - it.buyIn) }.toMutableList()
        }
        
        return generateMinimalDebtsWithTransfers(balancedPlayers, session.transfers)
    }
    
    /**
     * 手动标记部分/全部债务已结清
     * 同时将已结清的金额记录为定向转账，以便重新结算时自动扣除
     */
    suspend fun markDebtSettled(numericId: String, request: ManualSettleRequest): PokerSession {
        val session = sessionService.getSession(numericId)
            ?: throw IllegalArgumentException("Session not found: $numericId")
        
        val debt = session.debts.find { it.id == request.debtId }
            ?: throw IllegalArgumentException("Debt not found: ${request.debtId}")
        
        val actualSettleAmount = minOf(request.settledAmount, debt.amount - debt.settledAmount)
        if (actualSettleAmount <= 0) {
            return session
        }
        
        val newSettledAmount = debt.settledAmount + actualSettleAmount
        val isFullySettled = newSettledAmount >= debt.amount
        
        val transfer = DirectTransfer(
            fromPlayerId = debt.fromPlayerId,
            toPlayerId = debt.toPlayerId,
            amount = actualSettleAmount,
            note = "结算转账"
        )
        
        MongoDB.sessions.updateOne(
            Filters.and(
                Filters.eq("numericId", numericId),
                Filters.eq("debts.id", request.debtId)
            ),
            Updates.combine(
                Updates.set("debts.$.settledAmount", newSettledAmount),
                Updates.set("debts.$.settled", isFullySettled),
                Updates.push("transfers", transfer)
            )
        )
        
        logger.info("Debt settled: ${debt.fromPlayerId} -> ${debt.toPlayerId} = $actualSettleAmount, recorded as transfer")
        
        return sessionService.getSession(numericId)!!
    }
    
    /**
     * 重新打开已结算的 Session，进行重新结算
     * 保留所有定向转账记录（包括之前结算时标记的已结清转账）
     */
    suspend fun reopenSession(numericId: String): PokerSession {
        val session = sessionService.getSession(numericId)
            ?: throw IllegalArgumentException("Session not found: $numericId")
        
        if (session.status != SessionStatus.SETTLED) {
            throw IllegalStateException("Session is not settled, cannot reopen")
        }
        
        val resetPlayers = session.players.map { player ->
            player.copy(cashOut = 0, net = 0)
        }
        
        MongoDB.sessions.updateOne(
            Filters.eq("numericId", numericId),
            Updates.combine(
                Updates.set("status", SessionStatus.ACTIVE.name),
                Updates.set("players", resetPlayers),
                Updates.set("debts", emptyList<Debt>()),
                Updates.unset("settledAt")
            )
        )
        
        logger.info("Session reopened: $numericId, transfers preserved: ${session.transfers.size}")
        
        return sessionService.getSession(numericId)!!
    }
    
    /**
     * 计算账目差额（用于前端实时显示）
     */
    fun calculateDiff(session: PokerSession, cashOuts: Map<String, Long>): Long {
        val totalBuyIn = session.players.sumOf { it.buyIn }
        val totalCashOut = cashOuts.values.sum()
        return totalBuyIn - totalCashOut
    }
}
