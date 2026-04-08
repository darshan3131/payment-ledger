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
      if (data.role !== 'BACKOFFICE' && data.role !== 'ADMIN') {
        setError('Access denied. Backoffice role required.')
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
          <div style={{width:36,height:36,background:'#22c55e',borderRadius:8,marginBottom:20,display:'flex',alignItems:'center',justifyContent:'center'}}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>
            </svg>
          </div>
          <h1 style={{fontSize:20,fontWeight:600,letterSpacing:'-0.01em'}}>Back office</h1>
          <p style={{fontSize:13,color:'var(--muted)',marginTop:4}}>PayLedger · Operations</p>
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
              placeholder="staff username" autoFocus
              style={{width:'100%',background:'var(--surface)',border:'1px solid var(--border)',borderRadius:8,padding:'10px 12px',color:'var(--text)',fontSize:14}}
            />
          </div>
          <div>
            <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:6}}>
              <label style={{fontSize:12,color:'var(--muted)'}}>Password</label>
              <button type="button" onClick={onForgotPassword}
                style={{background:'none',border:'none',fontSize:11,color:'#22c55e',cursor:'pointer',padding:0}}>
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
            style={{marginTop:4,padding:'11px',background:'#22c55e',color:'white',border:'none',borderRadius:8,fontSize:14,fontWeight:500,opacity:loading?0.7:1}}
          >
            {loading ? 'Signing in…' : 'Continue'}
          </button>
        </form>
      </div>
    </div>
  )
}
