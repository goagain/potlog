import { useState } from 'react'
import type { Player } from '../types'
import { parseDollarsToCents } from '../utils/format'
import { useI18n } from '../i18n'

interface TransferModalProps {
  players: Player[]
  onClose: () => void
  onTransfer: (fromPlayerId: string, toPlayerId: string, amount: number, note?: string) => void
}

export default function TransferModal({ players, onClose, onTransfer }: TransferModalProps) {
  const { t } = useI18n()
  const [fromPlayerId, setFromPlayerId] = useState('')
  const [toPlayerId, setToPlayerId] = useState('')
  const [amount, setAmount] = useState('')
  const [note, setNote] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!fromPlayerId || !toPlayerId || !amount) return
    if (fromPlayerId === toPlayerId) return
    
    setLoading(true)
    await onTransfer(fromPlayerId, toPlayerId, parseDollarsToCents(amount), note || undefined)
    setLoading(false)
  }

  const quickAmounts = [20, 50, 100, 200]

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center p-4 z-50">
      <div className="bg-gray-800 rounded-xl p-6 w-full max-w-sm border border-gray-700">
        <h3 className="text-xl font-semibold mb-4">{t.transfer.addTransfer}</h3>
        <p className="text-gray-400 text-sm mb-4">
          {t.transfer.transferHint}
        </p>
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm text-gray-400 mb-2">{t.transfer.payer}</label>
            <select
              value={fromPlayerId}
              onChange={(e) => setFromPlayerId(e.target.value)}
              className="input-field"
            >
              <option value="">{t.transfer.selectPayer}</option>
              {players.map((p) => (
                <option key={p.id} value={p.id} disabled={p.id === toPlayerId}>
                  {p.name}
                </option>
              ))}
            </select>
          </div>
          
          <div className="flex justify-center">
            <span className="text-2xl text-gray-500">↓</span>
          </div>
          
          <div>
            <label className="block text-sm text-gray-400 mb-2">{t.transfer.payee}</label>
            <select
              value={toPlayerId}
              onChange={(e) => setToPlayerId(e.target.value)}
              className="input-field"
            >
              <option value="">{t.transfer.selectPayee}</option>
              {players.map((p) => (
                <option key={p.id} value={p.id} disabled={p.id === fromPlayerId}>
                  {p.name}
                </option>
              ))}
            </select>
          </div>
          
          <div>
            <label className="block text-sm text-gray-400 mb-2">{t.transfer.amount}</label>
            <input
              type="number"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              placeholder={t.transfer.amountPlaceholder}
              className="input-field"
              min="0"
              step="0.01"
            />
          </div>
          
          <div className="flex flex-wrap gap-2">
            {quickAmounts.map((qa) => (
              <button
                key={qa}
                type="button"
                onClick={() => setAmount(qa.toString())}
                className="bg-gray-700 px-3 py-1 rounded-lg text-sm hover:bg-gray-600"
              >
                ${qa}
              </button>
            ))}
          </div>
          
          <div>
            <label className="block text-sm text-gray-400 mb-2">{t.transfer.note}</label>
            <input
              type="text"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder={t.transfer.notePlaceholder}
              className="input-field"
            />
          </div>
          
          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="btn-secondary flex-1"
            >
              {t.common.cancel}
            </button>
            <button
              type="submit"
              disabled={!fromPlayerId || !toPlayerId || !amount || fromPlayerId === toPlayerId || loading}
              className="btn-primary flex-1"
            >
              {loading ? t.transfer.adding : t.transfer.add}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
