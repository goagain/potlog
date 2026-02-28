import { useState } from 'react'
import { parseDollarsToCents } from '../utils/format'
import { useI18n } from '../i18n'

interface RebuyModalProps {
  playerName: string
  onClose: () => void
  onRebuy: (amount: number) => void
}

export default function RebuyModal({ playerName, onClose, onRebuy }: RebuyModalProps) {
  const { t } = useI18n()
  const [amount, setAmount] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!amount) return
    
    setLoading(true)
    await onRebuy(parseDollarsToCents(amount))
    setLoading(false)
  }

  const quickAmounts = [50, 100, 200, 500]

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center p-4 z-50">
      <div className="bg-gray-800 rounded-xl p-6 w-full max-w-sm border border-gray-700">
        <h3 className="text-xl font-semibold mb-2">{t.rebuy.title}</h3>
        <p className="text-gray-400 mb-4">{playerName}</p>
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm text-gray-400 mb-2">{t.rebuy.amount}</label>
            <input
              type="number"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              placeholder={t.rebuy.amountPlaceholder}
              className="input-field"
              min="0"
              step="0.01"
              autoFocus
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
              disabled={!amount || loading}
              className="btn-primary flex-1"
            >
              {loading ? t.common.processing : t.rebuy.confirm}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
