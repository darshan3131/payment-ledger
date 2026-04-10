import { useState } from 'react'
import { register, registerSendOtp } from '../services/api'

const inp = {
  width: '100%', background: 'var(--surface)', border: '1px solid var(--border)',
  borderRadius: 8, padding: '10px 12px', color: 'var(--text)', fontSize: 14,
  boxSizing: 'border-box'
}
const lbl = { fontSize: 12, color: 'var(--muted)', display: 'block', marginBottom: 6 }

export default function Register({ onSwitchToLogin }) {
  // Step 1 fields
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [confirm,  setConfirm]  = useState('')
  const [phone,    setPhone]    = useState('')

  // Step 2 fields
  const [otp, setOtp] = useState('')

  const [step,      setStep]      = useState(1)   // 1 = fill details, 2 = enter OTP
  const [loading,   setLoading]   = useState(false)
  const [cooldown,  setCooldown]  = useState(0)
  const [error,     setError]     = useState('')
  const [success,   setSuccess]   = useState(false)
  const [isDevMode, setIsDevMode] = useState(false) // true when backend returns devOtp

  // ── Step 1 → Step 2: validate then send OTP ───────
  async function handleSendOtp(e) {
    e.preventDefault()
    setError('')
    if (!username.trim())         { setError('Username is required'); return }
    if (password.length < 6)      { setError('Password must be at least 6 characters'); return }
    if (password !== confirm)     { setError('Passwords do not match'); return }
    if (!phone.trim())            { setError('Phone number is required'); return }
    if (!/^\+[1-9]\d{7,14}$/.test(phone.trim())) {
      setError('Phone must be in E.164 format: +91XXXXXXXXXX'); return
    }

    setLoading(true)
    try {
      const res = await registerSendOtp(phone.trim())
      // DEV MODE: backend returns devOtp when OTP_DEV_MODE=true — auto-fill the field
      // registerSendOtp already unwraps r.data, so use res.devOtp (not res.data.devOtp)
      if (res?.devOtp) {
        setOtp(res.devOtp)
        setIsDevMode(true)
      }
      setStep(2)
      startCooldown()
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to send OTP. Try again.')
    } finally { setLoading(false) }
  }

  function startCooldown() {
    setCooldown(30)
    const t = setInterval(() => {
      setCooldown(prev => { if (prev <= 1) { clearInterval(t); return 0 } return prev - 1 })
    }, 1000)
  }

  async function handleResend() {
    if (cooldown > 0) return
    setError('')
    setLoading(true)
    try {
      const res = await registerSendOtp(phone.trim())
      if (res?.devOtp) { setOtp(res.devOtp); setIsDevMode(true) }
      startCooldown()
    } catch (err) {
      setError('Failed to resend OTP.')
    } finally { setLoading(false) }
  }

  // ── Step 2: verify OTP + create account ────────────
  async function handleRegister(e) {
    e.preventDefault()
    setError('')
    if (!/^\d{6}$/.test(otp.trim())) { setError('Enter the 6-digit OTP'); return }

    setLoading(true)
    try {
      await register({
        username: username.trim(),
        password,
        role: 'CUSTOMER',
        phone: phone.trim(),
        otp: otp.trim()
      })
      setSuccess(true)
    } catch (err) {
      setError(err.response?.data?.error || 'Registration failed. Try again.')
    } finally { setLoading(false) }
  }

  // ── Success screen ──────────────────────────────────
  if (success) {
    return (
      <div style={{minHeight:'100vh',display:'flex',alignItems:'center',justifyContent:'center',padding:24}}>
        <div style={{width:'100%',maxWidth:360,textAlign:'center'}}>
          <div style={{width:48,height:48,borderRadius:12,background:'rgba(34,197,94,0.12)',border:'1px solid rgba(34,197,94,0.25)',display:'flex',alignItems:'center',justifyContent:'center',margin:'0 auto 20px'}}>
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#22c55e" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
          </div>
          <h2 style={{fontSize:18,fontWeight:600,marginBottom:8}}>Account created!</h2>
          <p style={{fontSize:13,color:'var(--muted)',marginBottom:8,lineHeight:1.6}}>
            <strong style={{color:'var(--text)'}}>{username}</strong> is verified and ready.
          </p>
          <p style={{fontSize:12,color:'var(--muted)',marginBottom:24,lineHeight:1.6}}>
            Your payment account hasn't been linked yet. Log in and contact back office to open a payment account.
          </p>
          <button onClick={onSwitchToLogin}
            style={{padding:'10px 24px',background:'var(--accent)',color:'white',border:'none',borderRadius:8,fontSize:14,fontWeight:500,cursor:'pointer'}}>
            Sign in
          </button>
        </div>
      </div>
    )
  }

  return (
    <div style={{minHeight:'100vh',display:'flex',alignItems:'center',justifyContent:'center',padding:24}}>
      <div style={{width:'100%',maxWidth:380}}>

        {/* Header */}
        <div style={{marginBottom:28}}>
          <img src="/logo-dark.svg" alt="PayLedger" style={{height:32,display:'block',marginBottom:16}} />
          <h1 style={{fontSize:20,fontWeight:600,letterSpacing:'-0.01em'}}>
            {step === 1 ? 'Create account' : 'Verify your phone'}
          </h1>
          <p style={{fontSize:13,color:'var(--muted)',marginTop:4}}>
            {step === 1
              ? 'Customer Portal'
              : `OTP sent to ${phone}. Enter it below.`}
          </p>
        </div>

        {/* Step indicator */}
        <div style={{display:'flex',gap:6,marginBottom:24}}>
          {[1,2].map(s => (
            <div key={s} style={{flex:1,height:3,borderRadius:3,background: s <= step ? 'var(--accent)' : 'var(--border)'}}/>
          ))}
        </div>

        {error && (
          <div style={{background:'rgba(239,68,68,0.08)',border:'1px solid rgba(239,68,68,0.2)',borderRadius:8,padding:'10px 14px',fontSize:13,color:'var(--red)',marginBottom:16}}>
            {error}
          </div>
        )}

        {/* ── STEP 1 ── */}
        {step === 1 && (
          <form onSubmit={handleSendOtp} style={{display:'flex',flexDirection:'column',gap:14}}>
            <div>
              <label style={lbl}>Username</label>
              <input value={username} onChange={e=>setUsername(e.target.value)} placeholder="choose a username" autoFocus style={inp}/>
            </div>
            <div>
              <label style={lbl}>Password</label>
              <input type="password" value={password} onChange={e=>setPassword(e.target.value)} placeholder="min 6 characters" style={inp}/>
            </div>
            <div>
              <label style={lbl}>Confirm password</label>
              <input type="password" value={confirm} onChange={e=>setConfirm(e.target.value)} placeholder="repeat password" style={inp}/>
            </div>
            <div>
              <label style={lbl}>Phone number <span style={{opacity:.6}}>(E.164 — for OTP verification)</span></label>
              <input value={phone} onChange={e=>setPhone(e.target.value)} placeholder="+91XXXXXXXXXX" style={inp}/>
            </div>
            <button type="submit" disabled={loading}
              style={{marginTop:4,padding:'11px',background:'var(--accent)',color:'white',border:'none',borderRadius:8,fontSize:14,fontWeight:500,opacity:loading?.7:1,cursor:'pointer'}}>
              {loading ? 'Sending OTP…' : 'Send verification code →'}
            </button>
          </form>
        )}

        {/* ── STEP 2 ── */}
        {step === 2 && (
          <form onSubmit={handleRegister} style={{display:'flex',flexDirection:'column',gap:14}}>

            {isDevMode && (
              <div style={{background:'rgba(234,179,8,0.08)',border:'1px solid rgba(234,179,8,0.25)',borderRadius:8,padding:'8px 12px',fontSize:12,color:'#ca8a04',display:'flex',alignItems:'center',gap:6}}>
                <span>⚡</span>
                <span><strong>Dev mode</strong> — OTP auto-filled. Real SMS disabled.</span>
              </div>
            )}

            <div>
              <label style={lbl}>6-digit OTP</label>
              <input
                value={otp} onChange={e=>setOtp(e.target.value.replace(/\D/g,'').slice(0,6))}
                placeholder="000000" maxLength={6} autoFocus
                style={{...inp, letterSpacing:'0.25em', textAlign:'center', fontSize:20, fontWeight:600}}
              />
            </div>

            <button type="submit" disabled={loading}
              style={{padding:'11px',background:'var(--accent)',color:'white',border:'none',borderRadius:8,fontSize:14,fontWeight:500,opacity:loading?.7:1,cursor:'pointer'}}>
              {loading ? 'Creating account…' : 'Verify & create account'}
            </button>

            <div style={{display:'flex',alignItems:'center',justifyContent:'space-between',marginTop:4}}>
              <button type="button" onClick={() => { setStep(1); setOtp(''); setError('') }}
                style={{background:'none',border:'none',color:'var(--muted)',fontSize:13,cursor:'pointer',padding:0}}>
                ← Change details
              </button>
              <button type="button" onClick={handleResend} disabled={cooldown>0||loading}
                style={{background:'none',border:'none',color:cooldown>0?'var(--muted)':'var(--accent)',fontSize:13,cursor:cooldown>0?'default':'pointer',padding:0}}>
                {cooldown > 0 ? `Resend in ${cooldown}s` : 'Resend OTP'}
              </button>
            </div>
          </form>
        )}

        <p style={{textAlign:'center',fontSize:13,color:'var(--muted)',marginTop:24}}>
          Already have an account?{' '}
          <button onClick={onSwitchToLogin}
            style={{background:'none',border:'none',color:'var(--accent)',fontSize:13,cursor:'pointer',padding:0,fontWeight:500}}>
            Sign in
          </button>
        </p>
      </div>
    </div>
  )
}
