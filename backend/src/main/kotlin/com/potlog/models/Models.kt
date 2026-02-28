package com.potlog.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

enum class SessionStatus {
    ACTIVE,
    SETTLED
}

enum class TransactionType {
    BUY_IN,
    REBUY,
    MANUAL_TRANSFER,
    CASH_OUT
}

@Serializable
data class Player(
    val id: String,
    val name: String,
    var buyIn: Long = 0,
    var cashOut: Long = 0,
    var net: Long = 0,
    val userId: String? = null
)

@Serializable
data class TransactionLog(
    val id: String = ObjectId().toHexString(),
    val playerId: String,
    val type: TransactionType,
    val amount: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null
)

@Serializable
data class Debt(
    val id: String = ObjectId().toHexString(),
    val fromPlayerId: String,
    val toPlayerId: String,
    var amount: Long,
    var settled: Boolean = false,
    var settledAmount: Long = 0
)

@Serializable
data class DirectTransfer(
    val id: String = ObjectId().toHexString(),
    val fromPlayerId: String,
    val toPlayerId: String,
    val amount: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null
)

@Serializable
data class PokerSession(
    @SerialName("_id")
    val id: String = ObjectId().toHexString(),
    val numericId: String,
    var status: SessionStatus = SessionStatus.ACTIVE,
    val stakes: String,
    val players: MutableList<Player> = mutableListOf(),
    val logs: MutableList<TransactionLog> = mutableListOf(),
    val transfers: MutableList<DirectTransfer> = mutableListOf(),
    val debts: MutableList<Debt> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis(),
    var settledAt: Long? = null
)

@Serializable
data class CreateSessionRequest(
    val stakes: String
)

@Serializable
data class CreateSessionResponse(
    val numericId: String,
    val session: PokerSession
)

@Serializable
data class AddPlayerRequest(
    val name: String,
    val initialBuyIn: Long,
    val userId: String? = null
)

@Serializable
data class RebuyRequest(
    val playerId: String,
    val amount: Long
)

@Serializable
data class CashOutRequest(
    val playerId: String,
    val amount: Long
)

@Serializable
data class SettleRequest(
    val cashOuts: Map<String, Long>,
    val balanceMode: BalanceMode = BalanceMode.MAX_WINNER
)

enum class BalanceMode {
    MAX_WINNER,
    PROPORTIONAL
}

@Serializable
data class ManualSettleRequest(
    val debtId: String,
    val settledAmount: Long
)

@Serializable
data class AddTransferRequest(
    val fromPlayerId: String,
    val toPlayerId: String,
    val amount: Long,
    val note: String? = null
)

@Serializable
data class RemoveTransferRequest(
    val transferId: String
)

@Serializable
data class UserStats(
    val userId: String,
    val totalNet: Long,
    val sessionCount: Int,
    val sessions: List<SessionSummary>
)

@Serializable
data class SessionSummary(
    val numericId: String,
    val stakes: String,
    val net: Long,
    val settledAt: Long
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String
)
