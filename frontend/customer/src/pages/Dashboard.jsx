import { useState, useEffect } from 'react'
import { getLedgerStatement, getAccountById, processTransaction, requestTransferOtp, changePassword, createTicket, getMyTickets } from '../services/api'
import LedgerTable from '../components/LedgerTable'

const input = {
  width:'100%', background:'var(--surface)', border:'1px solid var(--border)',
  borderRadius:8, padding:'10px 12px', color:'var(--text)', fontSize:13
}
const label = { fontSize:12, color:'var(--muted)', display:'block', marginBottom:6 }
const card  = { background:'var(--surface)', border:'1px solid var(--border)', borderRadius:10, padding:20 }

function PrimaryBtn({ children, ...props }) {
  return (
    <button {...props} style={{
      width:'100%', padding:'11px', background:'var(--accent)', color:'white',
      border:'none', borderRadius:8, fontSize:13, fontWeight:500,
      opacity: props.disabled ? 0.6 : 1, ...props.style
    }}>{children}</button>
  )
}

function Badge({ status }) {
  const colors = {
    ACTIVE:    { bg:'rgba(34,197,94,0.1)',  text:'#22c55e' },
    FROZEN:    { bg:'rgba(215,119,6,0.1)',  text:'#d97706' },
    CLOSED:    { bg:'rgba(239,68,68,0.1)',  text:'#ef4444' },
    COMPLETED: { bg:'rgba(34,197,94,0.1)',  text:'#22c55e' },
    PENDING:   { bg:'rgba(215,119,6,0.1)',  text:'#d97706' },
    FAILED:    { bg:'rgba(239,68,68,0.1)',  text:'#ef4444' },
  }
  const c = colors[status] || { bg:'rgba(94,106,210,0.1)', text:'var(--accent)' }
  return (
    <span style={{display:'inline-block',padding:'2px 8px',borderRadius:4,fontSize:11,fontWeight:500,background:c.bg,color:c.text}}>
      {status}
    </span>
  )
}

export default function Dashboard({ account: initialAccount, allAccounts = [], onSwitchAccount, onLogout }) {
  const [account, setAccount] = useState(initialAccount)
  const [tab, setTab]         = useState('statement')
  const [entries, setEntries] = useState([])
  const [loading, setLoading] = useState(true)

  const [dst, setDst]         = useState('')
  const [amount, setAmount]   = useState('')
  const [txType, setTxType]   = useState('TRANSFER')
  const [desc, setDesc]       = useState('')
  const [idem, setIdem]       = useState('')
  const [sending, setSending] = useState(false)
  const [txResult, setTxResult] = useState(null)
  const [txError, setTxError]   = useState('')
  const [otpRequired, setOtpRequired] = useState(false)
  const [txOtp, setTxOtp]             = useState('')
  const [otpSending, setOtpSending]   = useState(false)
  const [otpCooldown, setOtpCooldown] = useState(0)

  useEffect(() => { loadData() }, [account.id])
  useEffect(() => { if (tab === 'support') loadTickets() }, [tab])

  async function loadData() {
    setLoading(true)
    try {
      const [freshAccount, ledger] = await Promise.all([
        getAccountById(account.id),
        getLedgerStatement(account.id)
      ])
      setAccount(freshAccount)
      setEntries(ledger)
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  const debits  = entries.filter(e => e.entryType === 'DEBIT')
  const credits = entries.filter(e => e.entryType === 'CREDIT')
  const totalSent     = debits.reduce((s, e) => s + e.amount, 0)
  const totalReceived = credits.reduce((s, e) => s + e.amount, 0)

  function startOtpCooldown() {
    setOtpCooldown(30)
    const t = setInterval(() => setOtpCooldown(p => { if (p<=1){clearInterval(t);return 0} return p-1 }), 1000)
  }

  async function handleRequestOtp() {
    setOtpSending(true); setTxError('')
    try {
      await requestTransferOtp(account.accountNumber)
      setOtpRequired(true)
      startOtpCooldown()
    } catch (err) {
      setTxError(err.response?.data?.error || 'Failed to send OTP')
    } finally { setOtpSending(false) }
  }

  async function handleTransfer(e) {
    e.preventDefault()
    setTxError(''); setTxResult(null)
    if (!dst.trim())                              { setTxError('Destination account required'); return }
    if (dst.trim().toUpperCase() === account.accountNumber) { setTxError('Cannot transfer to yourself'); return }
    const parsedAmount = parseInt(amount)
    if (!parsedAmount || parsedAmount <= 0)       { setTxError('Amount must be greater than 0'); return }
    if (parsedAmount > account.balance)           { setTxError('Insufficient balance'); return }

    setSending(true)
    try {
      const txn = await processTransaction({
        sourceAccountNumber:      account.accountNumber,
        destinationAccountNumber: dst.trim().toUpperCase(),
        amount:           parsedAmount,
        type:             txType,
        description:      desc || undefined,
        idempotencyKey:   idem || undefined,
        otp:              txOtp || undefined,
      })
      setTxResult(txn)
      setDst(''); setAmount(''); setDesc(''); setIdem(''); setTxOtp(''); setOtpRequired(false)
      await loadData()
    } catch (err) {
      if (err.response?.status === 428) {
        // High-value transfer — OTP required
        setOtpRequired(true)
        setTxError('This transfer requires OTP verification. An OTP has been sent to your phone.')
        handleRequestOtp()
      } else {
        setTxError(err.response?.data?.message || err.response?.data?.error || 'Transfer failed')
      }
    } finally {
      setSending(false)
    }
  }

  // ── change password state ────────────────────────────
  const [cpCurrent,  setCpCurrent]  = useState('')
  const [cpNew,      setCpNew]      = useState('')
  const [cpConfirm,  setCpConfirm]  = useState('')
  const [cpSaving,   setCpSaving]   = useState(false)
  const [cpError,    setCpError]    = useState('')
  const [cpSuccess,  setCpSuccess]  = useState(false)

  async function handleChangePassword(e) {
    e.preventDefault()
    setCpError(''); setCpSuccess(false)
    if (cpNew.length < 6)       { setCpError('New password must be at least 6 characters'); return }
    if (cpNew !== cpConfirm)    { setCpError('Passwords do not match'); return }
    if (cpNew === cpCurrent)    { setCpError('New password must differ from current'); return }
    setCpSaving(true)
    try {
      await changePassword({ currentPassword: cpCurrent, newPassword: cpNew })
      setCpSuccess(true)
      setCpCurrent(''); setCpNew(''); setCpConfirm('')
    } catch (err) {
      setCpError(err.response?.data?.error || 'Failed to change password.')
    } finally { setCpSaving(false) }
  }

  // ── support ticket state ─────────────────────────────
  const [tickets,      setTickets]      = useState([])
  const [ticketsLoading, setTicketsLoading] = useState(false)
  const [tkSubject,    setTkSubject]    = useState('')
  const [tkDesc,       setTkDesc]       = useState('')
  const [tkRef,        setTkRef]        = useState('')
  const [tkSending,    setTkSending]    = useState(false)
  const [tkError,      setTkError]      = useState('')
  const [tkSuccess,    setTkSuccess]    = useState(false)

  async function loadTickets() {
    setTicketsLoading(true)
    try {
      const data = await getMyTickets()
      setTickets(data.content || [])
    } catch(e) { console.error(e) }
    finally { setTicketsLoading(false) }
  }

  async function handleCreateTicket(e) {
    e.preventDefault()
    setTkError(''); setTkSuccess(false)
    if (!tkSubject.trim()) { setTkError('Subject is required'); return }
    if (!tkDesc.trim())    { setTkError('Description is required'); return }
    setTkSending(true)
    try {
      await createTicket({ subject: tkSubject, description: tkDesc, referenceId: tkRef || undefined })
      setTkSuccess(true)
      setTkSubject(''); setTkDesc(''); setTkRef('')
      loadTickets()
    } catch(err) {
      setTkError(err.response?.data?.error || 'Failed to submit ticket')
    } finally { setTkSending(false) }
  }

  const tabs = [
    { key:'statement', label:'Statement' },
    { key:'transfer',  label:'Send money' },
    { key:'support',   label:'Support'    },
    { key:'security',  label:'Security'   },
  ]

  return (
    <div style={{minHeight:'100vh',display:'flex',flexDirection:'column'}}>

      {/* TOP NAV */}
      <header style={{
        height:52, background:'var(--surface)', borderBottom:'1px solid var(--border)',
        padding:'0 24px', display:'flex', alignItems:'center', justifyContent:'space-between',
        position:'sticky', top:0, zIndex:10
      }}>
        <div style={{display:'flex',alignItems:'center',gap:8}}>
          <div style={{width:24,height:24,background:'var(--accent)',borderRadius:5,display:'flex',alignItems:'center',justifyContent:'center'}}>
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <rect x="2" y="5" width="20" height="14" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/>
            </svg>
          </div>
          <span style={{fontSize:14,fontWeight:600,letterSpacing:'-0.01em'}}>PayLedger</span>
        </div>
        <div style={{display:'flex',alignItems:'center',gap:12}}>
          {allAccounts.length > 1
            ? (
              <div style={{textAlign:'right'}}>
                <div style={{fontSize:11,color:'var(--muted)',marginBottom:3}}>Active account</div>
                <select
                  value={account.id}
                  onChange={e => {
                    const chosen = allAccounts.find(a => a.id === parseInt(e.target.value))
                    if (chosen) { setAccount(chosen); if (onSwitchAccount) onSwitchAccount(chosen) }
                  }}
                  style={{background:'var(--surface2)',border:'1px solid var(--border)',borderRadius:6,padding:'4px 8px',color:'var(--text)',fontSize:12,fontFamily:'monospace'}}
                >
                  {allAccounts.map(a => (
                    <option key={a.id} value={a.id}>{a.accountNumber} ({a.currency})</option>
                  ))}
                </select>
              </div>
            )
            : (
              <div style={{textAlign:'right'}}>
                <div style={{fontSize:13,fontWeight:500}}>{account.holderName}</div>
                <div style={{fontSize:11,color:'var(--muted)',fontFamily:'monospace'}}>{account.accountNumber}</div>
              </div>
            )
          }
          <button
            onClick={onLogout}
            style={{padding:'5px 12px',background:'none',border:'1px solid var(--border)',borderRadius:6,color:'var(--muted)',fontSize:12}}
          >
            Sign out
          </button>
        </div>
      </header>

      <main style={{flex:1,maxWidth:960,margin:'0 auto',padding:'28px 24px',width:'100%'}}>

        {/* BALANCE + STATS */}
        <div style={{display:'grid',gridTemplateColumns:'1fr 1fr 1fr',gap:16,marginBottom:28}}>
          <div style={{...card,gridColumn:'span 1'}}>
            <div style={{fontSize:11,color:'var(--muted)',textTransform:'uppercase',letterSpacing:'0.06em',marginBottom:8}}>Available Balance</div>
            <div style={{fontSize:30,fontWeight:700,letterSpacing:'-0.02em'}}>{account.formattedBalance}</div>
            <div style={{display:'flex',alignItems:'center',gap:8,marginTop:10}}>
              <Badge status={account.status}/>
              <span style={{fontSize:12,color:'var(--muted)'}}>{account.currency}</span>
            </div>
          </div>
          <div style={card}>
            <div style={{fontSize:11,color:'var(--muted)',textTransform:'uppercase',letterSpacing:'0.06em',marginBottom:8}}>Total Sent</div>
            <div style={{fontSize:22,fontWeight:600,color:'var(--red)'}}>
              −{(totalSent/100).toLocaleString('en-IN',{minimumFractionDigits:2})}
            </div>
            <div style={{fontSize:12,color:'var(--muted)',marginTop:6}}>{debits.length} transactions</div>
          </div>
          <div style={card}>
            <div style={{fontSize:11,color:'var(--muted)',textTransform:'uppercase',letterSpacing:'0.06em',marginBottom:8}}>Total Received</div>
            <div style={{fontSize:22,fontWeight:600,color:'var(--green)'}}>
              +{(totalReceived/100).toLocaleString('en-IN',{minimumFractionDigits:2})}
            </div>
            <div style={{fontSize:12,color:'var(--muted)',marginTop:6}}>{credits.length} transactions</div>
          </div>
        </div>

        {/* TABS */}
        <div style={{display:'flex',gap:0,borderBottom:'1px solid var(--border)',marginBottom:20}}>
          {tabs.map(t => (
            <button key={t.key} onClick={() => setTab(t.key)} style={{
              padding:'9px 16px', fontSize:13, fontWeight:500, background:'none',
              border:'none', borderBottom: tab===t.key ? '2px solid var(--accent)' : '2px solid transparent',
              color: tab===t.key ? 'var(--text)' : 'var(--muted)',
              marginBottom:'-1px'
            }}>
              {t.label}
            </button>
          ))}
        </div>

        {/* STATEMENT TAB */}
        {tab === 'statement' && (
          <div style={{background:'var(--surface)',border:'1px solid var(--border)',borderRadius:10}}>
            <div style={{padding:'14px 20px',borderBottom:'1px solid var(--border)',display:'flex',alignItems:'center',justifyContent:'space-between'}}>
              <span style={{fontSize:14,fontWeight:500}}>Account statement</span>
              <button
                onClick={loadData}
                style={{padding:'5px 12px',background:'none',border:'1px solid var(--border)',borderRadius:6,color:'var(--muted)',fontSize:12}}
              >
                Refresh
              </button>
            </div>
            {loading
              ? <div style={{textAlign:'center',padding:48,color:'var(--muted)',fontSize:13}}>Loading…</div>
              : <LedgerTable entries={entries} accountNumber={account.accountNumber} />
            }
          </div>
        )}

        {/* TRANSFER TAB */}
        {tab === 'transfer' && (
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:20}}>

            <div style={{...card}}>
              <div style={{fontSize:14,fontWeight:500,marginBottom:20}}>New transfer</div>
              <form onSubmit={handleTransfer} style={{display:'flex',flexDirection:'column',gap:14}}>
                {txError && (
                  <div style={{background:'rgba(239,68,68,0.08)',border:'1px solid rgba(239,68,68,0.2)',borderRadius:8,padding:'10px 12px',fontSize:13,color:'var(--red)'}}>
                    {txError}
                  </div>
                )}
                <div>
                  <label style={label}>Destination account</label>
                  <input style={input} value={dst} onChange={e=>setDst(e.target.value)} placeholder="ACC…" />
                </div>
                <div>
                  <label style={label}>Amount (paise)</label>
                  <input style={input} type="number" value={amount} onChange={e=>setAmount(e.target.value)} placeholder="e.g. 50000 = ₹500.00" min="1" />
                  {amount > 0 && <div style={{fontSize:11,color:'var(--accent)',marginTop:5}}>= ₹{(parseInt(amount)/100).toFixed(2)}</div>}
                </div>
                <div>
                  <label style={label}>Type</label>
                  <select style={input} value={txType} onChange={e=>setTxType(e.target.value)}>
                    <option value="TRANSFER">Transfer</option>
                    <option value="PAYMENT">Payment</option>
                  </select>
                </div>
                <div>
                  <label style={label}>Description (optional)</label>
                  <input style={input} value={desc} onChange={e=>setDesc(e.target.value)} placeholder="e.g. April rent" />
                </div>
                <div>
                  <label style={label}>Idempotency key (optional)</label>
                  <div style={{display:'flex',gap:8}}>
                    <input style={{...input,flex:1}} value={idem} onChange={e=>setIdem(e.target.value)} placeholder="Prevents duplicate sends" />
                    <button
                      type="button"
                      onClick={() => setIdem('cust-' + Date.now())}
                      style={{padding:'10px 12px',background:'var(--surface2)',border:'1px solid var(--border)',borderRadius:8,color:'var(--muted)',fontSize:12,whiteSpace:'nowrap'}}
                    >
                      Generate
                    </button>
                  </div>
                </div>
                {otpRequired && (
                  <div style={{background:'rgba(215,119,6,0.06)',border:'1px solid rgba(215,119,6,0.2)',borderRadius:8,padding:12}}>
                    <div style={{fontSize:12,color:'#d97706',marginBottom:8,fontWeight:500}}>
                      High-value transfer — OTP required
                    </div>
                    <input
                      value={txOtp}
                      onChange={e=>setTxOtp(e.target.value.replace(/\D/g,'').slice(0,6))}
                      placeholder="6-digit OTP"
                      maxLength={6}
                      style={{...input,letterSpacing:'0.2em',textAlign:'center',fontWeight:600}}
                    />
                    <button type="button" onClick={handleRequestOtp} disabled={otpCooldown>0||otpSending}
                      style={{background:'none',border:'none',color:otpCooldown>0?'var(--muted)':'var(--accent)',fontSize:12,cursor:otpCooldown>0?'default':'pointer',padding:'4px 0'}}>
                      {otpCooldown>0 ? `Resend in ${otpCooldown}s` : 'Resend OTP'}
                    </button>
                  </div>
                )}
                <PrimaryBtn type="submit" disabled={sending} style={{marginTop:4}}>
                  {sending ? 'Sending…' : 'Send transfer'}
                </PrimaryBtn>
              </form>

              {txResult && (
                <div style={{marginTop:16,background:'rgba(34,197,94,0.06)',border:'1px solid rgba(34,197,94,0.2)',borderRadius:8,padding:14}}>
                  <div style={{fontSize:13,fontWeight:500,color:'var(--green)',marginBottom:10}}>Transfer complete</div>
                  <table style={{width:'100%',borderCollapse:'collapse'}}>
                    {[
                      ['Reference', txResult.referenceId],
                      ['Amount', txResult.formattedAmount],
                      ['Status', txResult.status],
                    ].map(([k,v]) => (
                      <tr key={k}>
                        <td style={{fontSize:12,color:'var(--muted)',padding:'3px 0',paddingRight:16}}>{k}</td>
                        <td style={{fontSize:12,fontFamily:'monospace',color:'var(--text)'}}>{v}</td>
                      </tr>
                    ))}
                  </table>
                </div>
              )}
            </div>

            <div style={card}>
              <div style={{fontSize:14,fontWeight:500,marginBottom:20}}>Account details</div>
              <table style={{width:'100%',borderCollapse:'collapse'}}>
                {[
                  ['Account number', account.accountNumber],
                  ['Holder', account.holderName],
                  ['Currency', account.currency],
                  ['Balance', account.formattedBalance],
                  ['Status', account.status],
                  ['Member since', account.createdAt ? new Date(account.createdAt).toLocaleDateString('en-IN') : '—'],
                ].map(([k,v]) => (
                  <tr key={k} style={{borderBottom:'1px solid var(--border)'}}>
                    <td style={{fontSize:12,color:'var(--muted)',padding:'10px 0',paddingRight:16,whiteSpace:'nowrap'}}>{k}</td>
                    <td style={{fontSize:13,fontFamily: k==='Account number'?'monospace':'inherit',color:'var(--text)',textAlign:'right'}}>{v}</td>
                  </tr>
                ))}
              </table>
              <div style={{marginTop:16,padding:'12px',background:'var(--surface2)',borderRadius:8,fontSize:12,color:'var(--muted)',lineHeight:1.7}}>
                100 paise = ₹1.00 · Transfers are instant and irreversible
              </div>
            </div>

          </div>
        )}

        {/* SUPPORT TAB */}
        {tab === 'support' && (
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:20,alignItems:'start'}}>

            {/* New ticket form */}
            <div style={card}>
              <div style={{fontSize:14,fontWeight:500,marginBottom:4}}>Open a support ticket</div>
              <div style={{fontSize:12,color:'var(--muted)',marginBottom:18}}>Describe your issue and our team will get back to you.</div>

              {tkSuccess && (
                <div style={{background:'rgba(34,197,94,0.06)',border:'1px solid rgba(34,197,94,0.2)',borderRadius:8,padding:'10px 14px',fontSize:13,color:'var(--green)',marginBottom:14}}>
                  Ticket submitted successfully.
                </div>
              )}
              {tkError && (
                <div style={{background:'rgba(239,68,68,0.08)',border:'1px solid rgba(239,68,68,0.2)',borderRadius:8,padding:'10px 14px',fontSize:13,color:'var(--red)',marginBottom:14}}>
                  {tkError}
                </div>
              )}

              <form onSubmit={handleCreateTicket} style={{display:'flex',flexDirection:'column',gap:14}}>
                <div>
                  <label style={label}>Subject</label>
                  <input style={input} value={tkSubject} onChange={e=>setTkSubject(e.target.value)} placeholder="e.g. Transaction not received" />
                </div>
                <div>
                  <label style={label}>Description</label>
                  <textarea
                    style={{...input,resize:'vertical',minHeight:100}}
                    value={tkDesc} onChange={e=>setTkDesc(e.target.value)}
                    placeholder="Explain what happened in detail…"
                  />
                </div>
                <div>
                  <label style={label}>Transaction reference (optional)</label>
                  <input style={input} value={tkRef} onChange={e=>setTkRef(e.target.value)} placeholder="REF-…" />
                </div>
                <PrimaryBtn type="submit" disabled={tkSending}>
                  {tkSending ? 'Submitting…' : 'Submit ticket'}
                </PrimaryBtn>
              </form>
            </div>

            {/* Ticket history */}
            <div style={card}>
              <div style={{display:'flex',alignItems:'center',justifyContent:'space-between',marginBottom:16}}>
                <div style={{fontSize:14,fontWeight:500}}>My tickets</div>
                <button onClick={loadTickets} style={{padding:'4px 10px',background:'none',border:'1px solid var(--border)',borderRadius:6,color:'var(--muted)',fontSize:12}}>
                  Refresh
                </button>
              </div>
              {ticketsLoading
                ? <div style={{color:'var(--muted)',fontSize:13,textAlign:'center',padding:24}}>Loading…</div>
                : tickets.length === 0
                ? <div style={{color:'var(--muted)',fontSize:13,textAlign:'center',padding:24}}>No tickets yet</div>
                : tickets.map(t => {
                    const statusColor = {OPEN:'#d97706',IN_PROGRESS:'#818cf8',RESOLVED:'#22c55e',CLOSED:'#94a3b8'}[t.status] || 'var(--muted)'
                    return (
                      <div key={t.id} style={{borderBottom:'1px solid var(--border)',padding:'12px 0'}}>
                        <div style={{display:'flex',alignItems:'center',justifyContent:'space-between',marginBottom:4}}>
                          <span style={{fontSize:13,fontWeight:500}}>{t.subject}</span>
                          <span style={{fontSize:11,fontWeight:500,color:statusColor,background:`${statusColor}22`,padding:'2px 8px',borderRadius:4}}>{t.status}</span>
                        </div>
                        <div style={{fontSize:12,color:'var(--muted)',marginBottom:t.resolution?6:0}}>{t.description}</div>
                        {t.resolution && (
                          <div style={{fontSize:12,color:'var(--green)',background:'rgba(34,197,94,0.06)',border:'1px solid rgba(34,197,94,0.15)',borderRadius:6,padding:'6px 10px',marginTop:6}}>
                            <strong>Resolution:</strong> {t.resolution}
                          </div>
                        )}
                        <div style={{fontSize:11,color:'var(--muted)',marginTop:4}}>
                          {t.referenceId && <span>Ref: {t.referenceId} · </span>}
                          {new Date(t.createdAt).toLocaleDateString('en-IN')}
                        </div>
                      </div>
                    )
                  })
              }
            </div>

          </div>
        )}

        {/* SECURITY TAB */}
        {tab === 'security' && (
          <div style={{maxWidth:440}}>
            <div style={{...card}}>
              <div style={{fontSize:14,fontWeight:500,marginBottom:4}}>Change password</div>
              <div style={{fontSize:12,color:'var(--muted)',marginBottom:20}}>Choose a strong password you haven't used before.</div>

              {cpSuccess && (
                <div style={{background:'rgba(34,197,94,0.06)',border:'1px solid rgba(34,197,94,0.2)',borderRadius:8,padding:'10px 14px',fontSize:13,color:'var(--green)',marginBottom:16}}>
                  Password changed successfully.
                </div>
              )}
              {cpError && (
                <div style={{background:'rgba(239,68,68,0.08)',border:'1px solid rgba(239,68,68,0.2)',borderRadius:8,padding:'10px 14px',fontSize:13,color:'var(--red)',marginBottom:16}}>
                  {cpError}
                </div>
              )}

              <form onSubmit={handleChangePassword} style={{display:'flex',flexDirection:'column',gap:14}}>
                <div>
                  <label style={label}>Current password</label>
                  <input type="password" style={input} value={cpCurrent} onChange={e=>setCpCurrent(e.target.value)} placeholder="Enter current password" />
                </div>
                <div>
                  <label style={label}>New password</label>
                  <input type="password" style={input} value={cpNew} onChange={e=>setCpNew(e.target.value)} placeholder="Min 6 characters" />
                </div>
                <div>
                  <label style={label}>Confirm new password</label>
                  <input type="password" style={input} value={cpConfirm} onChange={e=>setCpConfirm(e.target.value)} placeholder="Repeat new password" />
                </div>
                <PrimaryBtn type="submit" disabled={cpSaving} style={{marginTop:4}}>
                  {cpSaving ? 'Saving…' : 'Update password'}
                </PrimaryBtn>
              </form>
            </div>
          </div>
        )}

      </main>
    </div>
  )
}
