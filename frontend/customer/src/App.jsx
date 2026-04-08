import { useState } from 'react'
import './index.css'
import Login          from './pages/Login'
import Register       from './pages/Register'
import ForgotPassword from './pages/ForgotPassword'
import Dashboard      from './pages/Dashboard'
import { getToken, getUser, getMyAccounts, clearToken, clearUser } from './services/api'

export default function App() {
  const [authUser, setAuthUser] = useState(() => {
    const token = getToken()
    const user  = getUser()
    return (token && user) ? user : null
  })
  const [account,   setAccount]   = useState(null)
  const [allAccounts, setAllAccounts] = useState([])
  const [page,      setPage]      = useState('login') // 'login' | 'register' | 'forgot'

  async function handleLogin(data) {
    setAuthUser(data)
    try {
      const accounts = await getMyAccounts()
      setAllAccounts(accounts)
      if (accounts.length > 0) setAccount(accounts[0])
      else setAccount(null)
    } catch {
      setAccount(null)
    }
  }

  function handleLogout() {
    clearToken(); clearUser()
    setAuthUser(null); setAccount(null); setAllAccounts([]); setPage('login')
  }

  if (!authUser) {
    if (page === 'register') return <Register onSwitchToLogin={() => setPage('login')} />
    if (page === 'forgot')   return <ForgotPassword onSwitchToLogin={() => setPage('login')} />
    return <Login onLogin={handleLogin} onSwitchToRegister={() => setPage('register')} onForgotPassword={() => setPage('forgot')} />
  }

  if (!account) return <NoAccount user={authUser} onLogout={handleLogout} />

  return (
    <Dashboard
      account={account}
      allAccounts={allAccounts}
      onSwitchAccount={setAccount}
      onLogout={handleLogout}
    />
  )
}

function NoAccount({ user, onLogout }) {
  return (
    <div style={{minHeight:'100vh',display:'flex',alignItems:'center',justifyContent:'center',flexDirection:'column',gap:12,padding:24}}>
      <div style={{width:40,height:40,borderRadius:8,background:'rgba(215,119,6,0.12)',border:'1px solid rgba(215,119,6,0.25)',display:'flex',alignItems:'center',justifyContent:'center',marginBottom:4}}>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#d97706" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
      </div>
      <div style={{fontSize:16,fontWeight:600}}>No account linked</div>
      <div style={{fontSize:13,color:'var(--muted)',textAlign:'center',maxWidth:300}}>
        User <strong style={{color:'var(--text)'}}>{user.username}</strong> has no account attached. Contact back office to create one.
      </div>
      <button
        onClick={onLogout}
        style={{marginTop:8,padding:'8px 18px',borderRadius:8,border:'1px solid var(--border)',background:'none',color:'var(--muted)',fontSize:13}}
      >
        Sign out
      </button>
    </div>
  )
}
