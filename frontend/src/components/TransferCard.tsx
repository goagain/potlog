import type { DirectTransfer, Player } from '../types'
import { formatCents } from '../utils/format'
import { useI18n } from '../i18n'

interface TransferCardProps {
  transfer: DirectTransfer
  players: Player[]
  onRemove?: () => void
  isSettled?: boolean
}

export default function TransferCard({ transfer, players, onRemove, isSettled }: TransferCardProps) {
  const { t } = useI18n()
  const fromPlayer = players.find(p => p.id === transfer.fromPlayerId)
  const toPlayer = players.find(p => p.id === transfer.toPlayerId)

  return (
    <div className="p-3 rounded-lg bg-blue-900/30 border border-blue-700/50 flex items-center justify-between">
      <div className="flex items-center gap-2">
        <div className="chip text-xs bg-blue-700">{fromPlayer?.name.charAt(0) || '?'}</div>
        <span className="text-blue-300">→</span>
        <div className="chip text-xs bg-green-700">{toPlayer?.name.charAt(0) || '?'}</div>
        <div className="ml-2">
          <span className="font-medium text-blue-300">{fromPlayer?.name}</span>
          <span className="text-gray-400 mx-2">{t.transfer.transferTo}</span>
          <span className="font-medium text-green-300">{toPlayer?.name}</span>
        </div>
      </div>
      
      <div className="flex items-center gap-3">
        <span className="text-lg font-semibold text-white">
          {formatCents(transfer.amount)}
        </span>
        {transfer.note && (
          <span className="text-xs text-gray-500 max-w-20 truncate" title={transfer.note}>
            ({transfer.note})
          </span>
        )}
        {!isSettled && onRemove && (
          <button
            onClick={onRemove}
            className="text-gray-500 hover:text-red-400 p-1"
            title={t.transfer.deleteTitle}
          >
            ✕
          </button>
        )}
      </div>
    </div>
  )
}
