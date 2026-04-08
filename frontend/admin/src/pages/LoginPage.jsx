import { useState } from 'react'
import { login, saveToken, saveUser } from '../services/api'

export default function LoginPage({ onLogin, onForgotPassword }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading]   = useState(false)
  const [error, setError]       = useState('')

  async function handleSubmit(e) {
    e.preventDefault()
    setLoading(true); setError('')
    try {
      const data = await login({ username: username.trim(), password })
      if (data.role !== 'ADMIN') {
        setError('Access denied. Admin role required.')
        return
      }
      saveToken(data.token); saveUser(data)
      onLogin(data)
    } catch (err) {
      setError(err.response?.data?.error || 'Invalid credentials')
    } finally { setLoading(false) }
  }

  return (
    <div style={{minHeight:'100vh',display:'flex',alignItems:'center',justifyContent:'center',padding:24}}>
      <div style={{width:'100%',maxWidth:360}}>
        <div style={{marginBottom:32}}>
          <div style={{width:36,height:36,background:'#d97706',borderRadius:8,marginBottom:20,display:'flex',alignItems:'center',justifyContent:'center'}}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="3"/>
              <path d="M19.07 4.93l-1.41 1.41M4.93 4.93l1.41 1.41M12 2v2M12 20v2M20 12h2M2 12h2M17.66 17.66l-1.41-1.41M6.34 17.66l1.41-1.41"/>
            </svg>
          </div>
          <h1 style={{fontSize:20,fontWeight:600,letterSpacing:'-0.01em'}}>Admin console</h1>
          <p style={{fontSize:13,color:'var(--muted)',marginTop:4}}>PayLedger · System Administration</p>
        </div>

        {error && (
          <div style={{background:'rgba(239,68,68,0.08)',border:'1px solid rgba(239,68,68,0.2)',borderRadius:8,padding:'10px 14px',fontSize:13,color:'var(--red)',marginBottom:16}}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} style={{display:'flex',flexDirection:'column',gap:12}}>
          <div>
            <label style={{fontSize:12,color:'var(--muted)',display:'block',marginBottom:6}}>Username</label>
            <input
              value={username} onChange={e=>setUsername(e.target.value)}
              placeholder="admin username" autoFocus
              style={{width:'100%',background:'var(--surface)',border:'1px solid var(--border)',borderRadius:8,padding:'10px 12px',color:'var(--text)',fontSize:14}}
            />
          </div>
          <div>
            <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:6}}>
              <label style={{fontSize:12,color:'var(--muted)'}}>Password</label>
              <button type="button" onClick={onForgotPassword}
                style={{background:'none',border:'none',fontSize:11,color:'#d97706',cursor:'pointer',padding:0}}>
                Forgot password?
              </button>
            </div>
            <input
              type="password" value={password} onChange={e=>setPassword(e.target.value)}
              placeholder="••••••••"
              style={{width:'100%',background:'var(--surface)',border:'1px solid var(--border)',borderRadius:8,padding:'10px 12px',color:'var(--text)',fontSize:14}}
            />
          </div>
          <button
            type="submit" disabled={loading}
            style={{marginTop:4,padding:'11px',background:'#d97706',color:'white',border:'none',borderRadius:8,fontSize:14,fontWeight:500,opacity:loading?0.7:1}}
          >
            {loading ? 'Signing in…' : 'Continue'}
          </button>
        </form>
      </div>
    </div>
  )
}
