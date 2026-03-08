import { useState, useEffect } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { api } from '../api/client'
import { useHistoryStore } from '../store/historyStore'
import { useI18n } from '../i18n'
import type { PokerSession } from '../types'
import { formatCents } from '../utils/format'
import ChipLoader from '../components/ChipLoader'
import PlayerCard from '../components/PlayerCard'
import AddPlayerModal from '../components/AddPlayerModal'
import RebuyModal from '../components/RebuyModal'
import DebtCard from '../components/DebtCard'
import TransferModal from '../components/TransferModal'
import TransferCard from '../components/TransferCard'
import BecomeAdminModal from '../components/BecomeAdminModal'
import AdminPasswordInput from '../components/AdminPasswordInput'
import { canModify, getStoredAdminPassword } from '../store/adminStore'

export default function SessionPage() {
  const { numericId } = useParams<{ numericId: string }>()
  const navigate = useNavigate()
  const { addToHistory } = useHistoryStore()
  const { t } = useI18n()
  
  const [session, setSession] = useState<PokerSession | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  
  const [showAddPlayer, setShowAddPlayer] = useState(false)
  const [rebuyPlayerId, setRebuyPlayerId] = useState<string | null>(null)
  const [showTransfer, setShowTransfer] = useState(false)
  const [showBecomeAdmin, setShowBecomeAdmin] = useState(false)

  const isAdmin = numericId ? canModify(numericId, session?.adminOnly) : false
  const adminPassword = numericId ? getStoredAdminPassword(numericId) : null

  const handleVerifyAdmin = async (password: string): Promise<boolean> => {
    if (!numericId) return false
    const res = await api.verifyAdmin(numericId, password)
    if (res.valid) {
      const { setAdmin } = await import('../store/adminStore')
      setAdmin(numericId, password)
      return true
    }
    return false
  }

  useEffect(() => {
    if (!numericId) return
    
    const fetchSession = async () => {
      try {
        const data = await api.getSession(numericId)
        setSession(data)
        addToHistory({ numericId: data.numericId, stakes: data.stakes })
      } catch (err) {
        setError(err instanceof Error ? err.message : t.session.loadFailed)
      } finally {
        setLoading(false)
      }
    }
    
    fetchSession()
  }, [numericId, addToHistory])

  const handleAddPlayer = async (name: string, buyIn: number) => {
    if (!numericId) return
    
    try {
      const updated = await api.addPlayer(numericId, name, buyIn, adminPassword)
      setSession(updated)
      setShowAddPlayer(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : t.session.addPlayerFailed)
    }
  }

  const handleRebuy = async (amount: number) => {
    if (!numericId || !rebuyPlayerId) return
    
    try {
      const updated = await api.rebuy(numericId, rebuyPlayerId, amount, adminPassword)
      setSession(updated)
      setRebuyPlayerId(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : t.session.rebuyFailed)
    }
  }

  const handleCopyLink = () => {
    const url = window.location.href
    navigator.clipboard.writeText(url)
  }

  const handleAddTransfer = async (fromPlayerId: string, toPlayerId: string, amount: number, note?: string) => {
    if (!numericId) return
    
    try {
      const updated = await api.addTransfer(numericId, fromPlayerId, toPlayerId, amount, note, adminPassword)
      setSession(updated)
      setShowTransfer(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : t.session.addTransferFailed)
    }
  }

  const handleRemoveTransfer = async (transferId: string) => {
    if (!numericId) return
    
    try {
      const updated = await api.removeTransfer(numericId, transferId, adminPassword)
      setSession(updated)
    } catch (err) {
      setError(err instanceof Error ? err.message : t.session.removeTransferFailed)
    }
  }

  const handleReopen = async () => {
    if (!numericId) return
    
    if (!confirm(t.session.reSettleConfirm)) {
      return
    }
    
    try {
      const updated = await api.reopenSession(numericId, adminPassword)
      setSession(updated)
    } catch (err) {
      setError(err instanceof Error ? err.message : t.session.reopenFailed)
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <ChipLoader size="lg" />
      </div>
    )
  }

  if (error || !session) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center p-4">
        <div className="text-red-400 text-xl mb-4">{error || t.session.sessionNotFound}</div>
        <Link to="/" className="btn-secondary">{t.common.home}</Link>
      </div>
    )
  }

  const totalPot = session.players.reduce((sum, p) => sum + p.buyIn, 0)
  const isSettled = session.status === 'SETTLED'

  return (
    <div className="min-h-screen p-4 pb-24">
      <div className="max-w-lg mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between gap-3 mb-6 flex-shrink-0">
          <Link to="/" className="text-gray-400 hover:text-white shrink-0">
            ← {t.common.back}
          </Link>
          <button
            onClick={handleCopyLink}
            className="text-sm bg-gray-700 px-3 py-1 rounded-lg hover:bg-gray-600 shrink-0"
          >
            {t.common.copyLink}
          </button>
        </div>

        {session.adminOnly && !isAdmin && (
          <div className="mb-4 p-3 bg-amber-900/30 border border-amber-700/50 rounded-lg text-amber-200 text-sm text-center">
            {t.admin.viewOnly}
          </div>
        )}

        {/* Session Info */}
        <div className="card mb-6">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-4">
            <div>
              <div className="text-3xl font-mono text-poker-gold">{session.numericId}</div>
              <div className="text-gray-400">{t.session.blinds}: {session.stakes}</div>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              {session.adminOnly && !isAdmin && (
                <button
                  onClick={() => setShowBecomeAdmin(true)}
                  className="text-sm bg-poker-gold/20 text-poker-gold px-3 py-1 rounded-lg hover:bg-poker-gold/30"
                >
                  {t.admin.becomeAdmin}
                </button>
              )}
              <div className={`px-3 py-1 rounded-full text-sm ${
                isSettled ? 'bg-green-900/50 text-green-400' : 'bg-blue-900/50 text-blue-400'
              }`}>
                {isSettled ? t.session.settled : t.session.active}
              </div>
              {isSettled && isAdmin && (
                <button
                  onClick={handleReopen}
                  className="text-sm bg-yellow-700/30 text-yellow-400 px-3 py-1 rounded-lg hover:bg-yellow-700/50"
                >
                  {t.session.reSettle}
                </button>
              )}
            </div>
          </div>
          
          <div className="bg-poker-green/30 rounded-lg p-4 text-center">
            <div className="text-sm text-gray-400 mb-1">Total Pot</div>
            <div className="text-3xl font-bold text-white">{formatCents(totalPot)}</div>
          </div>

          {session.adminOnly && isAdmin && adminPassword && (
            <div className="mt-4 pt-4 border-t border-gray-600">
              <label className="block text-xs text-gray-500 mb-2">{t.admin.adminPassword}</label>
              <AdminPasswordInput value={adminPassword} readOnly />
            </div>
          )}
        </div>

        {/* Players */}
        <div className="card mb-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-semibold">
              {t.session.players} ({session.players.length})
            </h2>
            {!isSettled && isAdmin && (
              <button
                onClick={() => setShowAddPlayer(true)}
                className="bg-poker-gold/20 text-poker-gold px-4 py-2 rounded-lg hover:bg-poker-gold/30 transition-colors"
              >
                + {t.session.addPlayer}
              </button>
            )}
          </div>
          
          {session.players.length === 0 ? (
            <div className="text-center text-gray-500 py-8">
              {t.session.noPlayers}
            </div>
          ) : (
            <div className="space-y-3">
              {session.players.map((player) => (
                <PlayerCard
                  key={player.id}
                  player={player}
                  showNet={isSettled}
                  isSettled={isSettled}
                  onRebuy={isAdmin ? () => setRebuyPlayerId(player.id) : undefined}
                />
              ))}
            </div>
          )}
        </div>

        {/* Direct Transfers */}
        {!isSettled && session.players.length >= 2 && (
          <div className="card mb-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold">
                {t.session.directTransfers} {session.transfers.length > 0 && `(${session.transfers.length})`}
              </h2>
              {isAdmin && (
              <button
                onClick={() => setShowTransfer(true)}
                className="bg-blue-700/30 text-blue-400 px-4 py-2 rounded-lg hover:bg-blue-700/50 transition-colors"
              >
                + {t.session.addTransfer}
              </button>
              )}
            </div>
            
            {session.transfers.length === 0 ? (
              <div className="text-center text-gray-500 py-4 text-sm">
                {t.session.transferHint}
              </div>
            ) : (
              <div className="space-y-2">
                {session.transfers.map((transfer) => (
                  <TransferCard
                    key={transfer.id}
                    transfer={transfer}
                    players={session.players}
                    onRemove={isAdmin ? () => handleRemoveTransfer(transfer.id) : undefined}
                  />
                ))}
              </div>
            )}
          </div>
        )}

        {/* Settled: Recorded Transfers */}
        {isSettled && session.transfers.length > 0 && (
          <div className="card mb-6">
            <h2 className="text-xl font-semibold mb-4">{t.session.recordedTransfers}</h2>
            <div className="space-y-2">
              {session.transfers.map((transfer) => (
                <TransferCard
                  key={transfer.id}
                  transfer={transfer}
                  players={session.players}
                  isSettled={true}
                />
              ))}
            </div>
          </div>
        )}

        {/* Settled: Remaining Transfers */}
        {isSettled && session.debts.length > 0 && (
          <div className="card mb-6">
            <h2 className="text-xl font-semibold mb-4">{t.session.remainingTransfers}</h2>
            <div className="space-y-3">
              {session.debts.map((debt) => (
                <DebtCard
                  key={debt.id}
                  debt={debt}
                  players={session.players}
                  onSettle={isAdmin ? async (settledAmount) => {
                    if (!numericId) return
                    const updated = await api.markDebtSettled(numericId, debt.id, settledAmount, adminPassword)
                    setSession(updated)
                  } : undefined}
                />
              ))}
            </div>
          </div>
        )}

        {/* Bottom Action Bar */}
        {!isSettled && session.players.length >= 2 && isAdmin && (
          <div className="fixed bottom-0 left-0 right-0 p-4 bg-gray-900/95 border-t border-gray-700">
            <div className="max-w-lg mx-auto">
              <button
                onClick={() => navigate(`/${numericId}/settle`)}
                className="btn-primary w-full"
              >
                {t.session.startSettle}
              </button>
            </div>
          </div>
        )}
      </div>

      {/* 添加玩家模态框 */}
      {showAddPlayer && (
        <AddPlayerModal
          onClose={() => setShowAddPlayer(false)}
          onAdd={handleAddPlayer}
        />
      )}

      {/* 加买模态框 */}
      {rebuyPlayerId && (
        <RebuyModal
          playerName={session.players.find(p => p.id === rebuyPlayerId)?.name || ''}
          onClose={() => setRebuyPlayerId(null)}
          onRebuy={handleRebuy}
        />
      )}

      {/* 成为管理员模态框 */}
      {showBecomeAdmin && (
        <BecomeAdminModal
          onClose={() => setShowBecomeAdmin(false)}
          onVerify={handleVerifyAdmin}
        />
      )}

      {/* 转账模态框 */}
      {showTransfer && (
        <TransferModal
          players={session.players}
          onClose={() => setShowTransfer(false)}
          onTransfer={handleAddTransfer}
        />
      )}
    </div>
  )
}
