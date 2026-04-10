import { useState } from 'react'
import { login, saveToken, saveUser } from '../services/api'

export default function Login({ onLogin, onSwitchToRegister, onForgotPassword }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading]   = useState(false)
  const [error, setError]       = useState('')

  async function handleSubmit(e) {
    e.preventDefault()
    if (!username.trim() || !password.trim()) { setError('All fields required'); return }
    setLoading(true); setError('')
    try {
      const data = await login({ username: username.trim(), password })
      saveToken(data.token)
      saveUser(data)
      onLogin(data)
    } catch (err) {
      setError(err.response?.data?.error || 'Invalid credentials')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{minHeight:'100vh',display:'flex',alignItems:'center',justifyContent:'center',padding:24}}>
      <div style={{width:'100%',maxWidth:360}}>

        <div style={{marginBottom:32}}>
          <img src="/logo-dark.svg" alt="PayLedger" style={{height:36,marginBottom:20,display:'block'}} />
          <h1 style={{fontSize:20,fontWeight:600,letterSpacing:'-0.01em'}}>Sign in</h1>
          <p style={{fontSize:13,color:'var(--muted)',marginTop:4}}>Customer Portal</p>
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
              value={username}
              onChange={e => setUsername(e.target.value)}
              placeholder="your username"
              autoFocus
              style={{width:'100%',background:'var(--surface)',border:'1px solid var(--border)',borderRadius:8,padding:'10px 12px',color:'var(--text)',fontSize:14}}
            />
          </div>
          <div>
            <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:6}}>
              <label style={{fontSize:12,color:'var(--muted)'}}>Password</label>
              <button type="button" onClick={onForgotPassword}
                style={{background:'none',border:'none',fontSize:11,color:'var(--accent)',cursor:'pointer',padding:0}}>
                Forgot password?
              </button>
            </div>
            <input
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              placeholder="••••••••"
              style={{width:'100%',background:'var(--surface)',border:'1px solid var(--border)',borderRadius:8,padding:'10px 12px',color:'var(--text)',fontSize:14}}
            />
          </div>
          <button
            type="submit"
            disabled={loading}
            style={{marginTop:4,padding:'11px',background:'var(--accent)',color:'white',border:'none',borderRadius:8,fontSize:14,fontWeight:500,opacity:loading?0.7:1}}
          >
            {loading ? 'Signing in…' : 'Continue'}
          </button>
        </form>

        <p style={{textAlign:'center',fontSize:13,color:'var(--muted)',marginTop:20}}>
          New here?{' '}
          <button
            onClick={onSwitchToRegister}
            style={{background:'none',border:'none',color:'var(--accent)',fontSize:13,cursor:'pointer',padding:0,fontWeight:500}}
          >
            Create account
          </button>
        </p>
      </div>
    </div>
  )
}
