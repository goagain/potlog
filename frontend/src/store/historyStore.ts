import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import type { HistoryItem } from '../types'

interface HistoryState {
  history: HistoryItem[]
  addToHistory: (item: Omit<HistoryItem, 'timestamp'>) => void
  removeFromHistory: (numericId: string) => void
  clearHistory: () => void
}

const MAX_HISTORY_ITEMS = 10

export const useHistoryStore = create<HistoryState>()(
  persist(
    (set) => ({
      history: [],
      
      addToHistory: (item) => set((state) => {
        const filtered = state.history.filter(h => h.numericId !== item.numericId)
        const newItem: HistoryItem = {
          ...item,
          timestamp: Date.now(),
        }
        return {
          history: [newItem, ...filtered].slice(0, MAX_HISTORY_ITEMS),
        }
      }),
      
      removeFromHistory: (numericId) => set((state) => ({
        history: state.history.filter(h => h.numericId !== numericId),
      })),
      
      clearHistory: () => set({ history: [] }),
    }),
    {
      name: 'potlog-history',
      storage: createJSONStorage(() => localStorage),
    }
  )
)
