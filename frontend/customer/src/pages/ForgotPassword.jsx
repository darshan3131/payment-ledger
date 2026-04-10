import { useState } from 'react'
import { forgotPassword, resetPassword } from '../services/api'

// Two-step flow:
// Step 1 — enter phone → POST /api/v1/auth/forgot-password → OTP sent via SMS
// Step 2 — enter OTP + new password → POST /api/v1/auth/reset-password → success

export default function ForgotPassword({ onSwitchToLogin, accentColor = 'var(--accent)' }) {
  const [step,        setStep]        = useState(1)   // 1 = enter phone, 2 = enter OTP + new password
  const [phone,       setPhone]       = useState('')
  const [otp,         setOtp]         = useState('123456') // DEV MODE: pre-filled, real SMS disabled
  const [newPassword, setNewPassword] = useState('')
  const [confirm,     setConfirm]     = useState('')
  const [loading,     setLoading]     = useState(false)
  const [error,       setError]       = useState('')
  const [success,     setSuccess]     = useState(false)
  const [resendCool,  setResendCool]  = useState(false)

  const inputStyle = {
    width: '100%', background: 'var(--surface)', border: '1px solid var(--border)',
    borderRadius: 8, padding: '10px 12px', color: 'var(--text)', fontSize: 14,
    boxSizing: 'border-box'
  }

  async function handleSendOtp(e) {
    e.preventDefault()
    if (!/^\+[1-9]\d{7,14}$/.test(phone.trim())) {
      setError('Enter phone in E.164 format: +91XXXXXXXXXX'); return
    }
    setLoading(true); setError('')
    try {
      await forgotPassword({ phone: phone.trim() })
      setStep(2)
      startResendCooldown()
    } catch {
      setError('Something went wrong. Please try again.')
    } finally { setLoading(false) }
  }

  async function handleResetPassword(e) {
    e.preventDefault()
    if (!/^\d{6}$/.test(otp.trim())) { setError('OTP must be exactly 6 digits'); return }
    if (newPassword.length < 6)      { setError('Password must be at least 6 characters'); return }
    if (newPassword !== confirm)      { setError('Passwords do not match'); return }
    setLoading(true); setError('')
    try {
      await resetPassword({ phone: phone.trim(), otp: otp.trim(), newPassword })
      setSuccess(true)
    } catch (err) {
      setError(err.response?.data?.error || 'Invalid or expired OTP. Try again.')
    } finally { setLoading(false) }
  }

  async function handleResend() {
    setError(''); setOtp('')
    try {
      await forgotPassword({ phone: phone.trim() })
      startResendCooldown()
    } catch { setError('Failed to resend. Try again.') }
  }

  function startResendCooldown() {
    setResendCool(true)
    setTimeout(() => setResendCool(false), 30_000) // 30s cooldown
  }

  if (success) {
    return (
      <div style={{minHeight:'100vh',display:'flex',alignItems:'center',justifyContent:'center',padding:24}}>
        <div style={{width:'100%',maxWidth:360,textAlign:'center'}}>
          <div style={{width:48,height:48,borderRadius:12,background:'rgba(34,197,94,0.12)',border:'1px solid rgba(34,197,94,0.25)',display:'flex',alignItems:'center',justifyContent:'center',margin:'0 auto 20px'}}>
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#22c55e" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
          </div>
          <h2 style={{fontSize:18,fontWeight:600,marginBottom:8}}>Password reset</h2>
          <p style={{fontSize:13,color:'var(--muted)',marginBottom:24}}>
            Your password has been updated. Sign in with your new password.
          </p>
          <button onClick={onSwitchToLogin}
            style={{padding:'10px 24px',background:accentColor,color:'white',border:'none',borderRadius:8,fontSize:14,fontWeight:500,cursor:'pointer'}}>
            Sign in
          </button>
        </div>
      </div>
    )
  }

  return (
    <div style={{minHeight:'100vh',display:'flex',alignItems:'center',justifyContent:'center',padding:24}}>
      <div style={{width:'100%',maxWidth:360}}>

        <div style={{marginBottom:32}}>
          <div style={{width:36,height:36,background:accentColor,borderRadius:8,marginBottom:20,display:'flex',alignItems:'center',justifyContent:'center'}}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
            </svg>
          </div>
          <h1 style={{fontSize:20,fontWeight:600,letterSpacing:'-0.01em'}}>Reset password</h1>
          <p style={{fontSize:13,color:'var(--muted)',marginTop:4}}>
            {step === 1 ? 'Enter your registered phone number' : `OTP sent to ${phone}`}
          </p>
          {step === 2 && (
            <div style={{marginTop:8,background:'rgba(234,179,8,0.08)',border:'1px solid rgba(234,179,8,0.25)',borderRadius:6,padding:'6px 10px',fontSize:12,color:'#ca8a04',display:'inline-flex',alignItems:'center',gap:5}}>
              <span>⚡</span><span><strong>Dev mode</strong> — OTP auto-filled (123456). Real SMS disabled.</span>
            </div>
          )}
        </div>

        {/* Step indicator */}
        <div style={{display:'flex',gap:6,marginBottom:24}}>
          {[1,2].map(s => (
            <div key={s} style={{height:3,flex:1,borderRadius:2,background: s <= step ? accentColor : 'var(--border)',transition:'background 0.3s'}}/>
          ))}
        </div>

        {error && (
          <div style={{background:'rgba(239,68,68,0.08)',border:'1px solid rgba(239,68,68,0.2)',borderRadius:8,padding:'10px 14px',fontSize:13,color:'var(--red)',marginBottom:16}}>
            {error}
          </div>
        )}

        {step === 1 && (
          <form onSubmit={handleSendOtp} style={{display:'flex',flexDirection:'column',gap:12}}>
            <div>
              <label style={{fontSize:12,color:'var(--muted)',display:'block',marginBottom:6}}>Phone number</label>
              <input value={phone} onChange={e => setPhone(e.target.value)}
                placeholder="+91XXXXXXXXXX" autoFocus style={inputStyle}/>
              <p style={{fontSize:11,color:'var(--muted)',marginTop:5}}>
                Must match the phone number on your account.
              </p>
            </div>
            <button type="submit" disabled={loading}
              style={{marginTop:4,padding:'11px',background:accentColor,color:'white',border:'none',borderRadius:8,fontSize:14,fontWeight:500,opacity:loading?0.7:1,cursor:'pointer'}}>
              {loading ? 'Sending OTP…' : 'Send OTP'}
            </button>
          </form>
        )}

        {step === 2 && (
          <form onSubmit={handleResetPassword} style={{display:'flex',flexDirection:'column',gap:12}}>
            <div>
              <label style={{fontSize:12,color:'var(--muted)',display:'block',marginBottom:6}}>6-digit OTP</label>
              <input value={otp} onChange={e => setOtp(e.target.value.replace(/\D/g,'').slice(0,6))}
                placeholder="_ _ _ _ _ _" autoFocus maxLength={6}
                style={{...inputStyle, letterSpacing:6, fontSize:18, textAlign:'center'}}/>
              <div style={{display:'flex',justifyContent:'space-between',marginTop:5}}>
                <p style={{fontSize:11,color:'var(--muted)'}}>Valid for 5 minutes.</p>
                <button type="button" onClick={handleResend} disabled={resendCool}
                  style={{background:'none',border:'none',fontSize:11,color:resendCool?'var(--muted)':accentColor,cursor:resendCool?'default':'pointer',padding:0}}>
                  {resendCool ? 'Resend in 30s' : 'Resend OTP'}
                </button>
              </div>
            </div>
            <div>
              <label style={{fontSize:12,color:'var(--muted)',display:'block',marginBottom:6}}>New password</label>
              <input type="password" value={newPassword} onChange={e => setNewPassword(e.target.value)}
                placeholder="min 6 characters" style={inputStyle}/>
            </div>
            <div>
              <label style={{fontSize:12,color:'var(--muted)',display:'block',marginBottom:6}}>Confirm new password</label>
              <input type="password" value={confirm} onChange={e => setConfirm(e.target.value)}
                placeholder="repeat password" style={inputStyle}/>
            </div>
            <button type="submit" disabled={loading}
              style={{marginTop:4,padding:'11px',background:accentColor,color:'white',border:'none',borderRadius:8,fontSize:14,fontWeight:500,opacity:loading?0.7:1,cursor:'pointer'}}>
              {loading ? 'Resetting…' : 'Reset password'}
            </button>
          </form>
        )}

        <p style={{textAlign:'center',fontSize:13,color:'var(--muted)',marginTop:20}}>
          <button onClick={onSwitchToLogin}
            style={{background:'none',border:'none',color:accentColor,fontSize:13,cursor:'pointer',padding:0,fontWeight:500}}>
            ← Back to sign in
          </button>
        </p>
      </div>
    </div>
  )
}
