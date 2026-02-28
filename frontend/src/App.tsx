import { Routes, Route } from 'react-router-dom'
import HomePage from './pages/HomePage'
import SessionPage from './pages/SessionPage'
import SettlePage from './pages/SettlePage'
import LanguageSwitch from './components/LanguageSwitch'

function App() {
  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-900 via-gray-900 to-poker-green">
      <LanguageSwitch />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/:numericId" element={<SessionPage />} />
        <Route path="/:numericId/settle" element={<SettlePage />} />
      </Routes>
    </div>
  )
}

export default App
