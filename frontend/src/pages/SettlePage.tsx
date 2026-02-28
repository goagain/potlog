import { useState, useEffect, useMemo } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { api } from '../api/client'
import { useI18n } from '../i18n'
import type { PokerSession, BalanceMode } from '../types'
import { formatCents, parseDollarsToCents } from '../utils/format'
import ChipLoader from '../components/ChipLoader'

export default function SettlePage() {
  const { numericId } = useParams<{ numericId: string }>()
  const navigate = useNavigate()
  const { t } = useI18n()
  
  const [session, setSession] = useState<PokerSession | null>(null)
  const [loading, setLoading] = useState(true)
  const [settling, setSettling] = useState(false)
  const [error, setError] = useState('')
  
  const [cashOuts, setCashOuts] = useState<Record<string, string>>({})
  const [balanceMode, setBalanceMode] = useState<BalanceMode>('MAX_WINNER')

  useEffect(() => {
    if (!numericId) return
    
    const fetchSession = async () => {
      try {
        const data = await api.getSession(numericId)
        if (data.status === 'SETTLED') {
          navigate(`/${numericId}`)
          return
        }
        setSession(data)
        const initial: Record<string, string> = {}
        data.players.forEach(p => {
          initial[p.id] = ''
        })
        setCashOuts(initial)
      } catch (err) {
        setError(err instanceof Error ? err.message : t.session.loadFailed)
      } finally {
        setLoading(false)
      }
    }
    
    fetchSession()
  }, [numericId, navigate])

  const { totalBuyIn, totalCashOut, diff, allFilled } = useMemo(() => {
    if (!session) return { totalBuyIn: 0, totalCashOut: 0, diff: 0, allFilled: false }
    
    const totalBuyIn = session.players.reduce((sum, p) => sum + p.buyIn, 0)
    let totalCashOut = 0
    let allFilled = true
    
    for (const player of session.players) {
      const value = cashOuts[player.id]
      if (value === '' || value === undefined) {
        allFilled = false
      } else {
        totalCashOut += parseDollarsToCents(value)
      }
    }
    
    return {
      totalBuyIn,
      totalCashOut,
      diff: totalBuyIn - totalCashOut,
      allFilled,
    }
  }, [session, cashOuts])

  const handleCashOutChange = (playerId: string, value: string) => {
    setCashOuts(prev => ({
      ...prev,
      [playerId]: value,
    }))
  }

  const handleSettle = async () => {
    if (!numericId || !session) return
    
    setSettling(true)
    setError('')
    
    try {
      const cashOutsCents: Record<string, number> = {}
      for (const player of session.players) {
        cashOutsCents[player.id] = parseDollarsToCents(cashOuts[player.id] || '0')
      }
      
      await api.settle(numericId, cashOutsCents, balanceMode)
      navigate(`/${numericId}`)
    } catch (err) {
      setError(err instanceof Error ? err.message : t.settle.settleFailed)
    } finally {
      setSettling(false)
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <ChipLoader size="lg" />
      </div>
    )
  }

  if (error && !session) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center p-4">
        <div className="text-red-400 text-xl mb-4">{error}</div>
        <Link to="/" className="btn-secondary">{t.common.home}</Link>
      </div>
    )
  }

  if (!session) return null

  const canSettle = allFilled && (diff === 0 || diff !== 0)

  return (
    <div className="min-h-screen p-4 pb-32">
      <div className="max-w-lg mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <Link to={`/${numericId}`} className="text-gray-400 hover:text-white">
            ← {t.common.back}
          </Link>
          <span className="text-gray-400">{t.settle.title}</span>
        </div>

        {/* Summary */}
        <div className="card mb-6">
          <div className="grid grid-cols-3 gap-4 text-center">
            <div>
              <div className="text-sm text-gray-400 mb-1">{t.settle.totalBuyIn}</div>
              <div className="text-xl font-semibold text-white">{formatCents(totalBuyIn)}</div>
            </div>
            <div>
              <div className="text-sm text-gray-400 mb-1">{t.settle.totalCashOut}</div>
              <div className="text-xl font-semibold text-white">{formatCents(totalCashOut)}</div>
            </div>
            <div>
              <div className="text-sm text-gray-400 mb-1">{t.settle.difference}</div>
              <div className={`text-xl font-semibold ${
                diff === 0 ? 'text-green-400' : 'text-yellow-400'
              }`}>
                {diff >= 0 ? '+' : ''}{formatCents(diff)}
              </div>
            </div>
          </div>
          
          {diff !== 0 && allFilled && (
            <div className="mt-4 p-3 bg-yellow-900/30 border border-yellow-700/50 rounded-lg">
              <div className="text-yellow-400 text-sm mb-2">
                {t.settle.diffWarning.replace('{amount}', formatCents(Math.abs(diff)))}
              </div>
              <div className="flex gap-3">
                <button
                  onClick={() => setBalanceMode('MAX_WINNER')}
                  className={`flex-1 py-2 px-3 rounded-lg text-sm transition-colors ${
                    balanceMode === 'MAX_WINNER'
                      ? 'bg-poker-gold text-black'
                      : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                  }`}
                >
                  {t.settle.maxWinner}
                </button>
                <button
                  onClick={() => setBalanceMode('PROPORTIONAL')}
                  className={`flex-1 py-2 px-3 rounded-lg text-sm transition-colors ${
                    balanceMode === 'PROPORTIONAL'
                      ? 'bg-poker-gold text-black'
                      : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                  }`}
                >
                  {t.settle.proportional}
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Cash Out Input */}
        <div className="card mb-6">
          <h2 className="text-xl font-semibold mb-4">{t.settle.enterCashOut}</h2>
          <div className="space-y-4">
            {session.players.map((player) => (
              <div key={player.id} className="flex items-center gap-4">
                <div className="flex items-center gap-3 flex-1">
                  <div className="chip">{player.name.charAt(0).toUpperCase()}</div>
                  <div>
                    <div className="font-medium">{player.name}</div>
                    <div className="text-sm text-gray-400">{t.player.buyIn}: {formatCents(player.buyIn)}</div>
                  </div>
                </div>
                <div className="w-32">
                  <div className="relative">
                    <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">$</span>
                    <input
                      type="number"
                      value={cashOuts[player.id] || ''}
                      onChange={(e) => handleCashOutChange(player.id, e.target.value)}
                      placeholder="0"
                      className="input-field pl-7 text-right"
                      min="0"
                      step="0.01"
                    />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* 错误提示 */}
        {error && (
          <div className="bg-red-900/50 border border-red-700 text-red-300 rounded-lg p-3 mb-6 text-center">
            {error}
          </div>
        )}

        {/* Settle Button */}
        <div className="fixed bottom-0 left-0 right-0 p-4 bg-gray-900/95 border-t border-gray-700">
          <div className="max-w-lg mx-auto">
            <button
              onClick={handleSettle}
              disabled={!canSettle || settling}
              className="btn-primary w-full flex items-center justify-center gap-2"
            >
              {settling ? <ChipLoader size="sm" /> : null}
              {settling ? t.settle.settling : t.settle.complete}
            </button>
            {!allFilled && (
              <p className="text-center text-gray-500 text-sm mt-2">
                {t.settle.enterAllCashOuts}
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
