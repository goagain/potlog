import type { Player } from '../types'
import { formatCents, formatNet, formatNetClass } from '../utils/format'
import { useI18n } from '../i18n'

interface PlayerCardProps {
  player: Player
  showNet?: boolean
  onRebuy?: () => void
  onCashOut?: () => void
  onRevokeCashOut?: () => void
  isSettled?: boolean
}

export default function PlayerCard({ player, showNet, onRebuy, onCashOut, onRevokeCashOut, isSettled }: PlayerCardProps) {
  const { t } = useI18n()
  
  return (
    <div className="bg-gray-700/50 rounded-lg p-4 flex items-center justify-between">
      <div className="flex items-center gap-3">
        <div className="chip">{player.name.charAt(0).toUpperCase()}</div>
        <div>
          <div className="font-medium text-white">{player.name}</div>
          <div className="text-sm text-gray-400">
            {t.player.buyIn}: {formatCents(player.buyIn)}
            {player.cashOut > 0 && !isSettled && (
              <span className="ml-2 text-green-400">
                · {t.player.cashOut}: {formatCents(player.cashOut)}
                {onRevokeCashOut && (
                  <button
                    onClick={(e) => { e.stopPropagation(); onRevokeCashOut() }}
                    className="ml-2 text-xs text-gray-400 hover:text-amber-400"
                    title={t.player.revokeCashOut}
                  >
                    ({t.player.revokeCashOut})
                  </button>
                )}
              </span>
            )}
          </div>
        </div>
      </div>
      
      <div className="flex items-center gap-3">
        {showNet && isSettled && (
          <div className={`text-lg font-semibold ${formatNetClass(player.net)}`}>
            {formatNet(player.net)}
          </div>
        )}
        
        {!isSettled && (
          <div className="flex gap-2">
            {onRebuy && (
              <button
                onClick={onRebuy}
                className="bg-poker-gold/20 text-poker-gold px-3 py-1 rounded-lg text-sm hover:bg-poker-gold/30 transition-colors"
              >
                + {t.player.rebuy}
              </button>
            )}
            {onCashOut && (
              <button
                onClick={onCashOut}
                className="bg-green-700/30 text-green-400 px-3 py-1 rounded-lg text-sm hover:bg-green-700/50 transition-colors"
              >
                {t.player.earlyCashOut}
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
