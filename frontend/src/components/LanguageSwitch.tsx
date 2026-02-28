import { useI18n, type Locale } from '../i18n'

export default function LanguageSwitch() {
  const { locale, setLocale } = useI18n()

  const toggleLocale = () => {
    const newLocale: Locale = locale === 'en' ? 'zh' : 'en'
    setLocale(newLocale)
  }

  return (
    <button
      onClick={toggleLocale}
      className="fixed top-4 right-4 z-50 bg-gray-700/80 hover:bg-gray-600 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors flex items-center gap-2"
      title={locale === 'en' ? 'Switch to Chinese' : '切换到英文'}
    >
      <span className="text-base">🌐</span>
      <span>{locale === 'en' ? '中文' : 'EN'}</span>
    </button>
  )
}
