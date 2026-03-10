export interface Player {
  id: string
  name: string
  buyIn: number
  cashOut: number
  net: number
  userId?: string
}

export interface TransactionLog {
  id: string
  playerId: string
  type: 'BUY_IN' | 'REBUY' | 'MANUAL_TRANSFER' | 'CASH_OUT'
  amount: number
  timestamp: number
  note?: string
  /** 转账时使用：收款人 playerId */
  toPlayerId?: string
}

export interface Debt {
  id: string
  fromPlayerId: string
  toPlayerId: string
  amount: number
  settled: boolean
  settledAmount: number
}

export interface DirectTransfer {
  id: string
  fromPlayerId: string
  toPlayerId: string
  amount: number
  timestamp: number
  note?: string
}

export interface PokerSession {
  numericId: string
  status: 'ACTIVE' | 'SETTLED'
  stakes: string
  adminOnly?: boolean
  players: Player[]
  logs: TransactionLog[]
  transfers: DirectTransfer[]
  debts: Debt[]
  createdAt: number
  settledAt?: number
  /** 结算时使用的转账模式，结算后添加/删除转账时重算沿用 */
  settlementTransferMode?: 'MINIMAL' | 'CENTRAL'
  /** 庄家转账模式下的庄家玩家 ID */
  settlementDealerPlayerId?: string | null
}

export interface CreateSessionResponse {
  numericId: string
  session: PokerSession
  adminPassword?: string
}

export interface HistoryItem {
  numericId: string
  stakes: string
  timestamp: number
}

export type BalanceMode = 'MAX_WINNER' | 'PROPORTIONAL' | 'DEALER'
export type TransferMode = 'MINIMAL' | 'CENTRAL'
