import type { Player } from '../types'
import { formatCents, formatNet, formatNetClass } from '../utils/format'
import { useI18n } from '../i18n'

interface PlayerCardProps {
  player: Player
  showNet?: boolean
  onRebuy?: () => void
  isSettled?: boolean
}

export default function PlayerCard({ player, showNet, onRebuy, isSettled }: PlayerCardProps) {
  const { t } = useI18n()
  
  return (
    <div className="bg-gray-700/50 rounded-lg p-4 flex items-center justify-between">
      <div className="flex items-center gap-3">
        <div className="chip">{player.name.charAt(0).toUpperCase()}</div>
        <div>
          <div className="font-medium text-white">{player.name}</div>
          <div className="text-sm text-gray-400">
            {t.player.buyIn}: {formatCents(player.buyIn)}
          </div>
        </div>
      </div>
      
      <div className="flex items-center gap-3">
        {showNet && isSettled && (
          <div className={`text-lg font-semibold ${formatNetClass(player.net)}`}>
            {formatNet(player.net)}
          </div>
        )}
        
        {!isSettled && onRebuy && (
          <button
            onClick={onRebuy}
            className="bg-poker-gold/20 text-poker-gold px-3 py-1 rounded-lg text-sm hover:bg-poker-gold/30 transition-colors"
          >
            + {t.player.rebuy}
          </button>
        )}
      </div>
    </div>
  )
}
