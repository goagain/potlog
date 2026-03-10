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
    val note: String? = null,
    /** 转账时使用：收款人 playerId */
    val toPlayerId: String? = null,
    /** 关联的 DirectTransfer id，删除转账时可移除对应 log */
    val transferId: String? = null
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
    val adminOnly: Boolean = false,
    val adminPassword: String? = null,
    val players: MutableList<Player> = mutableListOf(),
    val logs: MutableList<TransactionLog> = mutableListOf(),
    val transfers: MutableList<DirectTransfer> = mutableListOf(),
    val debts: MutableList<Debt> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis(),
    var settledAt: Long? = null,
    /** 结算时使用的转账模式（MINIMAL/CENTRAL），结算后添加/删除转账时重算债务沿用 */
    val settlementTransferMode: String? = null,
    /** 庄家转账模式下的庄家玩家 ID */
    val settlementDealerPlayerId: String? = null
) {
    /** 返回不包含管理员密码的副本，用于 API 响应 */
    fun withoutAdminPassword() = this.copy(adminPassword = null)
}

@Serializable
data class CreateSessionRequest(
    val stakes: String,
    val adminOnly: Boolean = false
)

@Serializable
data class CreateSessionResponse(
    val numericId: String,
    val session: PokerSession,
    val adminPassword: String? = null
)

@Serializable
data class AddPlayerRequest(
    val name: String,
    val initialBuyIn: Long,
    val userId: String? = null,
    val adminPassword: String? = null
)

@Serializable
data class RebuyRequest(
    val playerId: String,
    val amount: Long,
    val adminPassword: String? = null
)

@Serializable
data class CashOutRequest(
    val playerId: String,
    val amount: Long
)

@Serializable
data class RevokeCashOutRequest(
    val playerId: String
)

@Serializable
data class SettleRequest(
    val cashOuts: Map<String, Long>,
    val balanceMode: BalanceMode = BalanceMode.MAX_WINNER,
    val transferMode: TransferMode = TransferMode.MINIMAL,
    val dealerPlayerId: String? = null,
    val adminPassword: String? = null
)

/** 平账：diff!=0 时差额由谁承担 */
enum class BalanceMode {
    MAX_WINNER,   // 最大赢家承担
    PROPORTIONAL, // 赢家按比例分担（精确到1分）
    DEALER        // 庄家承担
}

/** 转账模式：债务如何转化为转账 */
enum class TransferMode {
    MINIMAL,  // 最少转账次数
    CENTRAL   // 庄家转账：输家→庄家→赢家
}

@Serializable
data class ManualSettleRequest(
    val debtId: String,
    val settledAmount: Long,
    val adminPassword: String? = null
)

@Serializable
data class AddTransferRequest(
    val fromPlayerId: String,
    val toPlayerId: String,
    val amount: Long,
    val note: String? = null,
    val adminPassword: String? = null
)

@Serializable
data class RemoveTransferRequest(
    val transferId: String,
    val adminPassword: String? = null
)

@Serializable
data class AdminAuthBody(
    val adminPassword: String? = null
)

@Serializable
data class ReopenRequest(
    val adminPassword: String? = null
)

@Serializable
data class VerifyAdminRequest(
    val adminPassword: String
)

@Serializable
data class VerifyAdminResponse(
    val valid: Boolean
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
