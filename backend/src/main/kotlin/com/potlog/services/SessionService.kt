package com.potlog.services

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.potlog.database.MongoDB
import com.potlog.models.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import kotlin.random.Random

class SessionService {
    private val logger = LoggerFactory.getLogger(SessionService::class.java)
    
    companion object {
        private const val NUMERIC_ID_LENGTH = 6
        private const val MAX_RETRY_ATTEMPTS = 10
        private const val MIN_NUMERIC_ID = 100000
        private const val MAX_NUMERIC_ID = 999999
    }
    
    /**
     * 生成唯一的 6 位数字 ID
     * 使用随机生成 + MongoDB 唯一索引冲突重试机制
     */
    private fun generateNumericId(): String {
        val randomNumber = Random.nextInt(MIN_NUMERIC_ID, MAX_NUMERIC_ID + 1)
        return randomNumber.toString()
    }
    
    /**
     * 创建新的扑克 Session
     * 处理 MongoDB 唯一索引冲突，自动重试生成新 ID
     */
    suspend fun createSession(stakes: String): PokerSession {
        var attempts = 0
        
        while (attempts < MAX_RETRY_ATTEMPTS) {
            attempts++
            val numericId = generateNumericId()
            
            val session = PokerSession(
                numericId = numericId,
                stakes = stakes
            )
            
            try {
                MongoDB.sessions.insertOne(session)
                logger.info("Created session with numericId: $numericId (attempt: $attempts)")
                return session
            } catch (e: Exception) {
                if (MongoDB.isDuplicateKeyException(e)) {
                    logger.warn("Duplicate numericId collision: $numericId, retrying... (attempt: $attempts)")
                    continue
                }
                throw e
            }
        }
        
        throw IllegalStateException("Failed to generate unique numericId after $MAX_RETRY_ATTEMPTS attempts")
    }
    
    /**
     * 根据 numericId 获取 Session
     */
    suspend fun getSession(numericId: String): PokerSession? {
        return MongoDB.sessions
            .find(Filters.eq("numericId", numericId))
            .firstOrNull()
    }
    
    /**
     * 添加玩家到 Session
     */
    suspend fun addPlayer(numericId: String, request: AddPlayerRequest): PokerSession? {
        val session = getSession(numericId) ?: return null
        
        if (session.status != SessionStatus.ACTIVE) {
            throw IllegalStateException("Cannot add player to settled session")
        }
        
        val playerId = ObjectId().toHexString()
        val player = Player(
            id = playerId,
            name = request.name,
            buyIn = request.initialBuyIn,
            userId = request.userId
        )
        
        val log = TransactionLog(
            playerId = playerId,
            type = TransactionType.BUY_IN,
            amount = request.initialBuyIn
        )
        
        MongoDB.sessions.updateOne(
            Filters.eq("numericId", numericId),
            Updates.combine(
                Updates.push("players", player),
                Updates.push("logs", log)
            )
        )
        
        return getSession(numericId)
    }
    
    /**
     * 玩家加买 (Re-buy)
     */
    suspend fun rebuy(numericId: String, request: RebuyRequest): PokerSession? {
        val session = getSession(numericId) ?: return null
        
        if (session.status != SessionStatus.ACTIVE) {
            throw IllegalStateException("Cannot rebuy in settled session")
        }
        
        val player = session.players.find { it.id == request.playerId }
            ?: throw IllegalArgumentException("Player not found: ${request.playerId}")
        
        val newBuyIn = player.buyIn + request.amount
        
        val log = TransactionLog(
            playerId = request.playerId,
            type = TransactionType.REBUY,
            amount = request.amount
        )
        
        MongoDB.sessions.updateOne(
            Filters.and(
                Filters.eq("numericId", numericId),
                Filters.eq("players.id", request.playerId)
            ),
            Updates.combine(
                Updates.set("players.$.buyIn", newBuyIn),
                Updates.push("logs", log)
            )
        )
        
        return getSession(numericId)
    }
    
    /**
     * 添加定向转账记录
     * 用于记录玩家之间的私下转账，结算时会自动扣除
     */
    suspend fun addTransfer(numericId: String, request: AddTransferRequest): PokerSession? {
        val session = getSession(numericId) ?: return null
        
        if (session.status != SessionStatus.ACTIVE) {
            throw IllegalStateException("Cannot add transfer to settled session")
        }
        
        val fromPlayer = session.players.find { it.id == request.fromPlayerId }
            ?: throw IllegalArgumentException("From player not found: ${request.fromPlayerId}")
        val toPlayer = session.players.find { it.id == request.toPlayerId }
            ?: throw IllegalArgumentException("To player not found: ${request.toPlayerId}")
        
        if (request.fromPlayerId == request.toPlayerId) {
            throw IllegalArgumentException("Cannot transfer to self")
        }
        
        if (request.amount <= 0) {
            throw IllegalArgumentException("Transfer amount must be positive")
        }
        
        val transfer = DirectTransfer(
            fromPlayerId = request.fromPlayerId,
            toPlayerId = request.toPlayerId,
            amount = request.amount,
            note = request.note
        )
        
        MongoDB.sessions.updateOne(
            Filters.eq("numericId", numericId),
            Updates.push("transfers", transfer)
        )
        
        logger.info("Added transfer: ${fromPlayer.name} -> ${toPlayer.name} = ${request.amount}")
        
        return getSession(numericId)
    }
    
    /**
     * 删除定向转账记录
     */
    suspend fun removeTransfer(numericId: String, transferId: String): PokerSession? {
        val session = getSession(numericId) ?: return null
        
        if (session.status != SessionStatus.ACTIVE) {
            throw IllegalStateException("Cannot remove transfer from settled session")
        }
        
        val transfer = session.transfers.find { it.id == transferId }
            ?: throw IllegalArgumentException("Transfer not found: $transferId")
        
        MongoDB.sessions.updateOne(
            Filters.eq("numericId", numericId),
            Updates.pull("transfers", Filters.eq("id", transferId))
        )
        
        logger.info("Removed transfer: $transferId")
        
        return getSession(numericId)
    }
    
    /**
     * 获取用户的历史统计数据
     */
    suspend fun getUserStats(userId: String): UserStats {
        val sessions = MongoDB.sessions
            .find(
                Filters.and(
                    Filters.eq("status", SessionStatus.SETTLED.name),
                    Filters.eq("players.userId", userId)
                )
            )
            .toList()
        
        var totalNet = 0L
        val summaries = mutableListOf<SessionSummary>()
        
        for (session in sessions) {
            val player = session.players.find { it.userId == userId }
            if (player != null) {
                totalNet += player.net
                summaries.add(
                    SessionSummary(
                        numericId = session.numericId,
                        stakes = session.stakes,
                        net = player.net,
                        settledAt = session.settledAt ?: session.createdAt
                    )
                )
            }
        }
        
        return UserStats(
            userId = userId,
            totalNet = totalNet,
            sessionCount = summaries.size,
            sessions = summaries.sortedByDescending { it.settledAt }
        )
    }
}
