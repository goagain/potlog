import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { locales, type Locale, type Translations } from './locales'

interface I18nState {
  locale: Locale
  setLocale: (locale: Locale) => void
  t: Translations
}

export const useI18n = create<I18nState>()(
  persist(
    (set) => ({
      locale: 'en',
      setLocale: (locale: Locale) => set({ locale, t: locales[locale] }),
      t: locales.en,
    }),
    {
      name: 'potlog-locale',
      onRehydrateStorage: () => (state) => {
        if (state) {
          state.t = locales[state.locale]
        }
      },
    }
  )
)

export { type Locale } from './locales'
