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
  players: Player[]
  logs: TransactionLog[]
  transfers: DirectTransfer[]
  debts: Debt[]
  createdAt: number
  settledAt?: number
}

export interface CreateSessionResponse {
  numericId: string
  session: PokerSession
}

export interface HistoryItem {
  numericId: string
  stakes: string
  timestamp: number
}

export type BalanceMode = 'MAX_WINNER' | 'PROPORTIONAL'
