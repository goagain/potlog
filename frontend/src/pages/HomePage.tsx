import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { useHistoryStore } from '../store/historyStore'
import { useI18n } from '../i18n'
import ChipLoader from '../components/ChipLoader'

export default function HomePage() {
  const navigate = useNavigate()
  const { history, removeFromHistory } = useHistoryStore()
  const { t } = useI18n()
  
  const [stakes, setStakes] = useState('1/2')
  const [joinId, setJoinId] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleCreate = async () => {
    if (!stakes.trim()) return
    
    setLoading(true)
    setError('')
    
    try {
      const response = await api.createSession(stakes)
      navigate(`/${response.numericId}`)
    } catch (err) {
      setError(err instanceof Error ? err.message : t.home.createFailed)
    } finally {
      setLoading(false)
    }
  }

  const handleJoin = () => {
    const id = joinId.trim()
    if (id.length === 6 && /^\d+$/.test(id)) {
      navigate(`/${id}`)
    } else {
      setError(t.home.invalidId)
    }
  }

  return (
    <div className="min-h-screen flex flex-col items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-20 h-20 rounded-full bg-poker-red border-4 border-poker-gold mb-4">
            <span className="text-3xl font-bold text-white">P</span>
          </div>
          <h1 className="text-4xl font-bold text-white mb-2">PotLog</h1>
          <p className="text-gray-400">{t.home.subtitle}</p>
        </div>

        {/* Create New Session */}
        <div className="card mb-6">
          <h2 className="text-xl font-semibold mb-4">{t.home.createNew}</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm text-gray-400 mb-2">{t.home.blindLevel}</label>
              <input
                type="text"
                value={stakes}
                onChange={(e) => setStakes(e.target.value)}
                placeholder={t.home.blindPlaceholder}
                className="input-field"
              />
            </div>
            <button
              onClick={handleCreate}
              disabled={loading || !stakes.trim()}
              className="btn-primary w-full flex items-center justify-center gap-2"
            >
              {loading ? <ChipLoader size="sm" /> : null}
              {loading ? t.home.creating : t.home.startSession}
            </button>
          </div>
        </div>

        {/* Join Existing Session */}
        <div className="card mb-6">
          <h2 className="text-xl font-semibold mb-4">{t.home.joinExisting}</h2>
          <div className="flex gap-3">
            <input
              type="text"
              value={joinId}
              onChange={(e) => setJoinId(e.target.value.replace(/\D/g, '').slice(0, 6))}
              placeholder={t.home.enterIdPlaceholder}
              className="input-field flex-1"
              maxLength={6}
            />
            <button
              onClick={handleJoin}
              disabled={joinId.length !== 6}
              className="btn-secondary"
            >
              {t.home.join}
            </button>
          </div>
        </div>

        {/* Error Message */}
        {error && (
          <div className="bg-red-900/50 border border-red-700 text-red-300 rounded-lg p-3 mb-6 text-center">
            {error}
          </div>
        )}

        {/* Recent Sessions */}
        {history.length > 0 && (
          <div className="card">
            <h2 className="text-xl font-semibold mb-4">{t.home.recentSessions}</h2>
            <div className="space-y-2">
              {history.map((item) => (
                <div
                  key={item.numericId}
                  className="flex items-center justify-between p-3 bg-gray-700/50 rounded-lg hover:bg-gray-700 transition-colors cursor-pointer"
                  onClick={() => navigate(`/${item.numericId}`)}
                >
                  <div>
                    <span className="font-mono text-lg text-poker-gold">{item.numericId}</span>
                    <span className="text-gray-400 ml-3">{item.stakes}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-sm text-gray-500">
                      {new Date(item.timestamp).toLocaleDateString()}
                    </span>
                    <button
                      onClick={(e) => {
                        e.stopPropagation()
                        removeFromHistory(item.numericId)
                      }}
                      className="text-gray-500 hover:text-red-400 p-1"
                    >
                      ✕
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
