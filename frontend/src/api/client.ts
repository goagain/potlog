import type { PokerSession, CreateSessionResponse, BalanceMode, Debt, DirectTransfer } from '../types'

const API_BASE = '/api'

async function request<T>(url: string, options?: RequestInit & { adminPassword?: string | null }): Promise<T> {
  const { adminPassword, ...restOptions } = options ?? {}
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(restOptions.headers as Record<string, string>),
  }
  if (adminPassword) headers['X-Admin-Password'] = adminPassword
  const response = await fetch(`${API_BASE}${url}`, {
    ...restOptions,
    headers,
  })
  
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Request failed' }))
    throw new Error(error.message || `HTTP ${response.status}`)
  }
  
  return response.json()
}

function withAdminPassword(body: Record<string, unknown>, adminPassword?: string | null) {
  if (adminPassword != null) {
    return { ...body, adminPassword }
  }
  return body
}

export const api = {
  createSession: (stakes: string, adminOnly = false): Promise<CreateSessionResponse> =>
    request('/sessions', {
      method: 'POST',
      body: JSON.stringify({ stakes, adminOnly }),
    }),

  verifyAdmin: (numericId: string, adminPassword: string): Promise<{ valid: boolean }> =>
    request(`/sessions/${numericId}/verify-admin`, {
      method: 'POST',
      body: JSON.stringify({ adminPassword }),
    }),

  getSession: (numericId: string): Promise<PokerSession> =>
    request(`/sessions/${numericId}`),

  addPlayer: (numericId: string, name: string, initialBuyIn: number, adminPassword?: string | null): Promise<PokerSession> =>
    request(`/sessions/${numericId}/players`, {
      method: 'POST',
      body: JSON.stringify(withAdminPassword({ name, initialBuyIn }, adminPassword)),
    }),

  rebuy: (numericId: string, playerId: string, amount: number, adminPassword?: string | null): Promise<PokerSession> =>
    request(`/sessions/${numericId}/rebuy`, {
      method: 'POST',
      body: JSON.stringify(withAdminPassword({ playerId, amount }, adminPassword)),
    }),

  cashOut: (numericId: string, playerId: string, amount: number): Promise<PokerSession> =>
    request(`/sessions/${numericId}/cash-out`, {
      method: 'POST',
      body: JSON.stringify({ playerId, amount }),
    }),

  revokeCashOut: (numericId: string, playerId: string): Promise<PokerSession> =>
    request(`/sessions/${numericId}/cash-out/revoke`, {
      method: 'POST',
      body: JSON.stringify({ playerId }),
    }),

  settle: (
    numericId: string, 
    cashOuts: Record<string, number>, 
    balanceMode: BalanceMode,
    adminPassword?: string | null
  ): Promise<PokerSession> =>
    request(`/sessions/${numericId}/settle`, {
      method: 'POST',
      body: JSON.stringify(withAdminPassword({ cashOuts, balanceMode }, adminPassword)),
    }),

  calculateDiff: (numericId: string, cashOuts: Record<string, number>): Promise<{ diff: number }> =>
    request(`/sessions/${numericId}/diff`, {
      method: 'POST',
      body: JSON.stringify(cashOuts),
    }),

  markDebtSettled: (numericId: string, debtId: string, settledAmount: number, adminPassword?: string | null): Promise<PokerSession> =>
    request(`/sessions/${numericId}/debts/settle`, {
      method: 'POST',
      body: JSON.stringify(withAdminPassword({ debtId, settledAmount }, adminPassword)),
    }),

  addTransfer: (
    numericId: string,
    fromPlayerId: string,
    toPlayerId: string,
    amount: number,
    note?: string,
    adminPassword?: string | null
  ): Promise<PokerSession> =>
    request(`/sessions/${numericId}/transfers`, {
      method: 'POST',
      body: JSON.stringify(withAdminPassword({ fromPlayerId, toPlayerId, amount, note }, adminPassword)),
    }),

  removeTransfer: (numericId: string, transferId: string, adminPassword?: string | null): Promise<PokerSession> =>
    request(`/sessions/${numericId}/transfers/${transferId}`, {
      method: 'DELETE',
      adminPassword,
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

  reopenSession: (numericId: string, adminPassword?: string | null): Promise<PokerSession> =>
    request(`/sessions/${numericId}/reopen`, {
      method: 'POST',
      body: JSON.stringify(withAdminPassword({}, adminPassword)),
    }),
}
