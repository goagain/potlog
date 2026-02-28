import type { PokerSession, CreateSessionResponse, BalanceMode, Debt, DirectTransfer } from '../types'

const API_BASE = '/api'

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${url}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  })
  
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Request failed' }))
    throw new Error(error.message || `HTTP ${response.status}`)
  }
  
  return response.json()
}

export const api = {
  createSession: (stakes: string): Promise<CreateSessionResponse> =>
    request('/sessions', {
      method: 'POST',
      body: JSON.stringify({ stakes }),
    }),

  getSession: (numericId: string): Promise<PokerSession> =>
    request(`/sessions/${numericId}`),

  addPlayer: (numericId: string, name: string, initialBuyIn: number): Promise<PokerSession> =>
    request(`/sessions/${numericId}/players`, {
      method: 'POST',
      body: JSON.stringify({ name, initialBuyIn }),
    }),

  rebuy: (numericId: string, playerId: string, amount: number): Promise<PokerSession> =>
    request(`/sessions/${numericId}/rebuy`, {
      method: 'POST',
      body: JSON.stringify({ playerId, amount }),
    }),

  settle: (
    numericId: string, 
    cashOuts: Record<string, number>, 
    balanceMode: BalanceMode
  ): Promise<PokerSession> =>
    request(`/sessions/${numericId}/settle`, {
      method: 'POST',
      body: JSON.stringify({ cashOuts, balanceMode }),
    }),

  calculateDiff: (numericId: string, cashOuts: Record<string, number>): Promise<{ diff: number }> =>
    request(`/sessions/${numericId}/diff`, {
      method: 'POST',
      body: JSON.stringify(cashOuts),
    }),

  markDebtSettled: (numericId: string, debtId: string, settledAmount: number): Promise<PokerSession> =>
    request(`/sessions/${numericId}/debts/settle`, {
      method: 'POST',
      body: JSON.stringify({ debtId, settledAmount }),
    }),

  addTransfer: (
    numericId: string,
    fromPlayerId: string,
    toPlayerId: string,
    amount: number,
    note?: string
  ): Promise<PokerSession> =>
    request(`/sessions/${numericId}/transfers`, {
      method: 'POST',
      body: JSON.stringify({ fromPlayerId, toPlayerId, amount, note }),
    }),

  removeTransfer: (numericId: string, transferId: string): Promise<PokerSession> =>
    request(`/sessions/${numericId}/transfers/${transferId}`, {
      method: 'DELETE',
    }),

  previewSettlement: (
    numericId: string,
    cashOuts: Record<string, number>,
    balanceMode: BalanceMode
  ): Promise<{ debts: Debt[]; transfers: DirectTransfer[] }> =>
    request(`/sessions/${numericId}/preview`, {
      method: 'POST',
      body: JSON.stringify({ cashOuts, balanceMode }),
    }),

  reopenSession: (numericId: string): Promise<PokerSession> =>
    request(`/sessions/${numericId}/reopen`, {
      method: 'POST',
    }),
}
