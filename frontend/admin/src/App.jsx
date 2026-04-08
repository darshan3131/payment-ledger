import { useState } from 'react'
import './index.css'
import LoginPage      from './pages/LoginPage'
import ForgotPassword from './pages/ForgotPassword'
import AnalyticsPage  from './pages/AnalyticsPage'
import HealthPage     from './pages/HealthPage'
import UsersPage      from './pages/UsersPage'
import { getToken, getUser, clearToken, clearUser, changePassword } from './services/api'

const NAV = [
  {
    key: 'analytics', label: 'Analytics',
    icon: <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>
  },
  {
    key: 'users', label: 'Users',
    icon: <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
  },
  {
    key: 'health', label: 'System health',
    icon: <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
  },
]

export default function App() {
  const [user, setUser] = useState(() => {
    const token = getToken(); const u = getUser()
    return (token && u) ? u : null
  })
  const [page,     setPage]     = useState('analytics')
  const [authPage, setAuthPage] = useState('login') // 'login' | 'forgot'
  const [showCp,    setShowCp]    = useState(false)
  const [cpCurrent, setCpCurrent] = useState('')
  const [cpNew,     setCpNew]     = useState('')
  const [cpConfirm, setCpConfirm] = useState('')
  const [cpSaving,  setCpSaving]  = useState(false)
  const [cpError,   setCpError]   = useState('')
  const [cpOk,      setCpOk]      = useState(false)

  function handleLogout() { clearToken(); clearUser(); setUser(null); setAuthPage('login') }

  function openCp() { setShowCp(true); setCpCurrent(''); setCpNew(''); setCpConfirm(''); setCpError(''); setCpOk(false) }

  async function handleCpSubmit(e) {
    e.preventDefault(); setCpError(''); setCpOk(false)
    if (cpNew.length < 6)    { setCpError('Min 6 characters'); return }
    if (cpNew !== cpConfirm) { setCpError('Passwords do not match'); return }
    setCpSaving(true)
    try {
      await changePassword({ currentPassword: cpCurrent, newPassword: cpNew })
      setCpOk(true); setCpCurrent(''); setCpNew(''); setCpConfirm('')
    } catch (err) { setCpError(err.response?.data?.error || 'Failed to change password.') }
    finally { setCpSaving(false) }
  }

  if (!user) {
    if (authPage === 'forgot') return <ForgotPassword onSwitchToLogin={() => setAuthPage('login')} accentColor="#d97706" />
    return <LoginPage onLogin={setUser} onForgotPassword={() => setAuthPage('forgot')} />
  }

  return (
    <div style={{display:'flex',minHeight:'100vh'}}>
      <aside style={{
        width:216, background:'var(--surface)', borderRight:'1px solid var(--border)',
        padding:'16px 0', position:'fixed', height:'100vh',
        display:'flex', flexDirection:'column'
      }}>
        <div style={{padding:'8px 16px 20px',borderBottom:'1px solid var(--border)'}}>
          <div style={{display:'flex',alignItems:'center',gap:8,marginBottom:12}}>
            <div style={{width:24,height:24,background:'#d97706',borderRadius:5,display:'flex',alignItems:'center',justifyContent:'center'}}>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="3"/>
                <path d="M12 2v2M12 20v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M2 12h2M20 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
              </svg>
            </div>
            <span style={{fontSize:13,fontWeight:600}}>PayLedger</span>
          </div>
          <div style={{display:'flex',alignItems:'center',gap:8}}>
            <div style={{width:26,height:26,borderRadius:'50%',background:'var(--surface2)',border:'1px solid var(--border)',display:'flex',alignItems:'center',justifyContent:'center',fontSize:11,fontWeight:600,color:'var(--muted)'}}>
              {user.username[0].toUpperCase()}
            </div>
            <div>
              <div style={{fontSize:12,fontWeight:500}}>{user.username}</div>
              <div style={{fontSize:10,color:'var(--muted)'}}>{user.role}</div>
            </div>
          </div>
        </div>

        <nav style={{marginTop:8,flex:1,padding:'0 8px'}}>
          {NAV.map(n => (
            <button key={n.key} onClick={() => setPage(n.key)} style={{
              display:'flex', alignItems:'center', gap:9, width:'100%',
              padding:'8px 10px', background: page===n.key ? 'var(--surface2)' : 'none',
              border:'none', borderRadius:6,
              color: page===n.key ? 'var(--text)' : 'var(--muted)',
              fontSize:13, fontWeight: page===n.key ? 500 : 400,
              cursor:'pointer', textAlign:'left', marginBottom:2
            }}>
              {n.icon}
              {n.label}
            </button>
          ))}
        </nav>

        <div style={{padding:'12px 8px',borderTop:'1px solid var(--border)'}}>
          <button onClick={openCp} style={{
            width:'100%', padding:'8px 10px', background:'none',
            border:'none', borderRadius:6, color:'var(--muted)',
            fontSize:13, cursor:'pointer', textAlign:'left',
            display:'flex', alignItems:'center', gap:9, marginBottom:2
          }}>
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/>
            </svg>
            Change password
          </button>
          <button onClick={handleLogout} style={{
            width:'100%', padding:'8px 10px', background:'none',
            border:'none', borderRadius:6, color:'var(--muted)',
            fontSize:13, cursor:'pointer', textAlign:'left',
            display:'flex', alignItems:'center', gap:9
          }}>
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
              <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>
            </svg>
            Sign out
          </button>
        </div>
      </aside>

      <main style={{marginLeft:216,flex:1,padding:28,minWidth:0}}>
        {page==='analytics' && <AnalyticsPage />}
        {page==='users'     && <UsersPage />}
        {page==='health'    && <HealthPage />}
      </main>

      {/* CHANGE PASSWORD MODAL */}
      {showCp && (
        <div style={{position:'fixed',inset:0,background:'rgba(0,0,0,0.5)',display:'flex',alignItems:'center',justifyContent:'center',zIndex:100}}>
          <div style={{background:'var(--surface)',borderRadius:12,padding:24,maxWidth:380,width:'90%',border:'1px solid var(--border)'}}>
            <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:16}}>
              <h3 style={{fontSize:15,fontWeight:600}}>Change password</h3>
              <button onClick={() => setShowCp(false)} style={{background:'none',border:'none',color:'var(--muted)',cursor:'pointer',fontSize:20,lineHeight:1}}>×</button>
            </div>
            {cpOk && <div style={{background:'rgba(34,197,94,0.06)',border:'1px solid rgba(34,197,94,0.2)',borderRadius:8,padding:'10px 12px',fontSize:13,color:'#22c55e',marginBottom:12}}>Password updated successfully.</div>}
            {cpError && <div style={{background:'rgba(239,68,68,0.08)',border:'1px solid rgba(239,68,68,0.2)',borderRadius:8,padding:'10px 12px',fontSize:13,color:'var(--red)',marginBottom:12}}>{cpError}</div>}
            <form onSubmit={handleCpSubmit} style={{display:'flex',flexDirection:'column',gap:12}}>
              {[
                ['Current password', cpCurrent, setCpCurrent],
                ['New password',     cpNew,     setCpNew],
                ['Confirm new',      cpConfirm, setCpConfirm],
              ].map(([lbl, val, setter]) => (
                <div key={lbl}>
                  <label style={{fontSize:11,color:'var(--muted)',display:'block',marginBottom:5}}>{lbl}</label>
                  <input type="password" value={val} onChange={e=>setter(e.target.value)}
                    style={{width:'100%',background:'var(--surface)',border:'1px solid var(--border)',borderRadius:8,padding:'9px 12px',color:'var(--text)',fontSize:13,boxSizing:'border-box'}}/>
                </div>
              ))}
              <div style={{display:'flex',gap:8,justifyContent:'flex-end',marginTop:4}}>
                <button type="button" onClick={() => setShowCp(false)}
                  style={{padding:'8px 16px',background:'none',border:'1px solid var(--border)',borderRadius:8,color:'var(--muted)',fontSize:13,cursor:'pointer'}}>
                  Cancel
                </button>
                <button type="submit" disabled={cpSaving}
                  style={{padding:'8px 16px',background:'#d97706',color:'white',border:'none',borderRadius:8,fontSize:13,fontWeight:500,opacity:cpSaving?.7:1,cursor:'pointer'}}>
                  {cpSaving ? 'Saving…' : 'Update'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
