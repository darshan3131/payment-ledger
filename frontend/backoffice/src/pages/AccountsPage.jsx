import { useState, useEffect, useCallback } from 'react'
import {
  getAllAccounts, getAvailableCustomers, createAccount,
  updateStatus, getLedgerStatement, deposit, withdraw
} from '../services/api'

const inp = {
  background:'var(--surface2)', border:'1px solid var(--border)', borderRadius:8,
  padding:'9px 12px', color:'var(--text)', fontSize:13, outline:'none', width:'100%', marginBottom:12,
  boxSizing:'border-box'
}
const lbl = { fontSize:12, color:'var(--muted)', display:'block', marginBottom:5 }
const th  = { padding:'10px 16px', textAlign:'left', color:'var(--muted)', fontWeight:500, fontSize:11, textTransform:'uppercase', letterSpacing:'0.06em', borderBottom:'1px solid var(--border)', whiteSpace:'nowrap' }
const td  = { padding:'12px 16px', borderBottom:'1px solid var(--border)', fontSize:13 }

const fmt = dt => dt ? new Date(dt).toLocaleDateString('en-IN',{day:'2-digit',month:'short',year:'numeric'}) : '—'

const CURRENCY_SYMBOLS   = { INR:'₹', USD:'$', EUR:'€', GBP:'£', JPY:'¥', AED:'AED', SGD:'SGD', SAR:'SAR', CAD:'CAD', AUD:'AUD' }
const CURRENCY_SUBUNITS  = { INR:'paise', USD:'cents', EUR:'cents', GBP:'pence', JPY:'sen', AED:'fils', SGD:'cents', SAR:'halalah', CAD:'cents', AUD:'cents' }
const currSym    = (code) => CURRENCY_SYMBOLS[code]  || code || '₹'
const currSubunit = (code) => CURRENCY_SUBUNITS[code] || 'subunits'

function Badge({ status }) {
  const map = {
    ACTIVE:  { bg:'rgba(34,197,94,0.1)',  color:'#22c55e' },
    FROZEN:  { bg:'rgba(215,119,6,0.1)',  color:'#d97706' },
    CLOSED:  { bg:'rgba(239,68,68,0.1)',  color:'#ef4444' },
  }
  const s = map[status] || { bg:'rgba(94,106,210,0.1)', color:'var(--accent)' }
  return <span style={{display:'inline-block',padding:'2px 8px',borderRadius:4,fontSize:11,fontWeight:500,...s}}>{status}</span>
}

function Btn({ variant='primary', children, ...props }) {
  const styles = {
    primary:   { background:'var(--accent)',              color:'white',         border:'none' },
    secondary: { background:'none',                       color:'var(--muted)',  border:'1px solid var(--border)' },
    danger:    { background:'rgba(239,68,68,0.08)',        color:'var(--red)',    border:'1px solid rgba(239,68,68,0.2)' },
    green:     { background:'#22c55e',                    color:'white',         border:'none' },
    amber:     { background:'rgba(215,119,6,0.12)',        color:'#d97706',       border:'1px solid rgba(215,119,6,0.25)' },
  }
  return (
    <button {...props} style={{padding:'9px 16px',borderRadius:8,fontSize:13,fontWeight:500,cursor:'pointer',...styles[variant],opacity:props.disabled?0.6:1,...props.style}}>
      {children}
    </button>
  )
}

// ── Cash operation modal (shared by Deposit and Withdraw) ───────
function CashModal({ account, mode, onClose, onDone }) {
  const [amount,  setAmount]  = useState('')
  const [desc,    setDesc]    = useState('')
  const [saving,  setSaving]  = useState(false)
  const [error,   setError]   = useState('')

  async function handleSubmit(e) {
    e.preventDefault()
    const amt = parseInt(amount)
    if (!amt || amt <= 0) { setError('Amount must be > 0'); return }
    setSaving(true); setError('')
    try {
      await (mode === 'deposit' ? deposit : withdraw)({
        accountNumber: account.accountNumber,
        amount: amt,
        description: desc || undefined,
      })
      onDone()
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data?.error || 'Operation failed.')
    } finally { setSaving(false) }
  }

  const isDeposit = mode === 'deposit'
  const accent    = isDeposit ? '#22c55e' : '#d97706'

  return (
    <div style={{position:'fixed',inset:0,background:'rgba(0,0,0,0.5)',display:'flex',alignItems:'center',justifyContent:'center',zIndex:100}}>
      <div style={{background:'var(--surface)',border:'1px solid var(--border)',borderRadius:12,padding:24,maxWidth:380,width:'90%'}}>
        <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:16}}>
          <div>
            <div style={{fontSize:15,fontWeight:600}}>{isDeposit ? '↓ Deposit' : '↑ Withdraw'}</div>
            <div style={{fontSize:12,color:'var(--muted)',marginTop:2,fontFamily:'monospace'}}>{account.holderName} · {account.accountNumber}</div>
          </div>
          <button onClick={onClose} style={{background:'none',border:'none',color:'var(--muted)',cursor:'pointer',fontSize:20,lineHeight:1}}>×</button>
        </div>

        <div style={{background:'var(--surface2)',borderRadius:8,padding:'10px 14px',marginBottom:16,fontSize:13}}>
          Current balance: <strong>{account.formattedBalance}</strong>
        </div>

        {error && (
          <div style={{background:'rgba(239,68,68,0.08)',border:'1px solid rgba(239,68,68,0.2)',borderRadius:8,padding:'8px 12px',fontSize:13,color:'var(--red)',marginBottom:12}}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} style={{display:'flex',flexDirection:'column',gap:12}}>
          <div>
            <label style={lbl}>Amount ({currSubunit(account.currency)})</label>
            <input type="number" min="1" value={amount} onChange={e=>setAmount(e.target.value)} placeholder={`e.g. 100000 = ${currSym(account.currency)}1,000`} style={inp}/>
            {amount > 0 && (
              <div style={{fontSize:11,color:accent,marginTop:-8,marginBottom:8}}>
                = {currSym(account.currency)}{(parseInt(amount)/100).toLocaleString('en-IN',{minimumFractionDigits:2})}
              </div>
            )}
          </div>
          <div>
            <label style={lbl}>Description <span style={{opacity:.6}}>(optional)</span></label>
            <input value={desc} onChange={e=>setDesc(e.target.value)} placeholder={isDeposit ? 'e.g. Account opening deposit' : 'e.g. Customer withdrawal request'} style={inp}/>
          </div>
          <div style={{display:'flex',gap:8,justifyContent:'flex-end',marginTop:4}}>
            <Btn variant="secondary" type="button" style={{padding:'8px 16px'}} onClick={onClose}>Cancel</Btn>
            <button type="submit" disabled={saving}
              style={{padding:'8px 20px',background:accent,color:'white',border:'none',borderRadius:8,fontSize:13,fontWeight:500,opacity:saving?.7:1,cursor:'pointer'}}>
              {saving ? 'Processing…' : isDeposit ? 'Deposit' : 'Withdraw'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

// ── Main page ───────────────────────────────────────
export default function AccountsPage() {
  const [accounts,      setAccounts]      = useState([])
  const [customers,     setCustomers]     = useState([])
  const [unlinkedOnly,  setUnlinkedOnly]  = useState(true)  // default: hide customers who already have accounts
  const [loading,       setLoading]       = useState(true)
  const [currentPage,   setCurrentPage]   = useState(0)
  const [totalPages,    setTotalPages]    = useState(0)
  const [totalElements, setTotalElements] = useState(0)

  // Create account form
  const [selectedUserId, setSelectedUserId] = useState('')
  const [cur,            setCur]            = useState('INR')
  const [creating,       setCreating]       = useState(false)
  const [createError,    setCreateError]    = useState('')

  // Status update form
  const [statusId,   setStatusId]   = useState('')
  const [newStatus,  setNewStatus]  = useState('FROZEN')
  const [updating,   setUpdating]   = useState(false)

  // Ledger drawer
  const [drawerAccount,  setDrawerAccount]  = useState(null)
  const [ledgerEntries,  setLedgerEntries]  = useState([])
  const [loadingLedger,  setLoadingLedger]  = useState(false)

  // Cash op modal
  const [cashModal, setCashModal] = useState(null) // { account, mode: 'deposit'|'withdraw' }

  const load = useCallback(async (page = 0) => {
    // eslint-disable-next-line react-hooks/exhaustive-deps
    setLoading(true)
    try {
      const [paged, availCust] = await Promise.all([
        getAllAccounts(page, 20),
        getAvailableCustomers(unlinkedOnly),
      ])
      // Filter out SYSTEM_CASH from display
      setAccounts(paged.content.filter(a => a.accountNumber !== 'SYSTEM_CASH'))
      setCurrentPage(paged.page)
      setTotalPages(paged.totalPages)
      setTotalElements(Math.max(0, paged.totalElements - 1)) // subtract SYSTEM_CASH
      setCustomers(availCust)
    } catch(e) { console.error(e) }
    finally { setLoading(false) }
  }, [unlinkedOnly])

  useEffect(() => { load(0) }, [load])

  async function handleCreate(e) {
    e.preventDefault()
    setCreateError('')
    if (!selectedUserId) { setCreateError('Please select a customer'); return }
    setCreating(true)
    try {
      await createAccount({ userId: parseInt(selectedUserId), currency: cur })
      setSelectedUserId('')
      await load(0)
    } catch(e) {
      setCreateError(e.response?.data?.error || e.response?.data?.message || 'Create failed')
    } finally { setCreating(false) }
  }

  async function handleStatusUpdate(e) {
    e.preventDefault()
    if (!statusId) return
    setUpdating(true)
    try {
      await updateStatus(parseInt(statusId), newStatus)
      setStatusId('')
      await load(currentPage)
    } catch(e) { alert('Update failed: ' + (e.response?.data?.error || e.message)) }
    finally { setUpdating(false) }
  }

  async function openLedger(account) {
    setDrawerAccount(account)
    setLoadingLedger(true)
    try { setLedgerEntries(await getLedgerStatement(account.id)) }
    catch(e) { alert('Error: ' + e.message) }
    finally { setLoadingLedger(false) }
  }

  async function handleCashDone() {
    setCashModal(null)
    await load(currentPage)
  }

  return (
    <div>
      <div style={{marginBottom:24}}>
        <h1 style={{fontSize:20,fontWeight:600,letterSpacing:'-0.01em'}}>Accounts</h1>
        <p style={{fontSize:13,color:'var(--muted)',marginTop:3}}>Create and manage customer payment accounts</p>
      </div>

      <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:16,marginBottom:24}}>

        {/* ── Create account ── */}
        <div style={{background:'var(--surface)',border:'1px solid var(--border)',borderRadius:10,padding:20}}>
          <div style={{fontSize:14,fontWeight:500,marginBottom:4}}>Open new account</div>
          <div style={{fontSize:12,color:'var(--muted)',marginBottom:16}}>
            Select a verified customer. Account number is auto-generated. Balance starts at 0 — use Deposit to add funds.
          </div>
          {createError && (
            <div style={{background:'rgba(239,68,68,0.08)',border:'1px solid rgba(239,68,68,0.2)',borderRadius:8,padding:'8px 12px',fontSize:13,color:'var(--red)',marginBottom:12}}>
              {createError}
            </div>
          )}
          <form onSubmit={handleCreate}>
            <div style={{display:'flex',alignItems:'center',justifyContent:'space-between',marginBottom:5}}>
              <label style={{...lbl,marginBottom:0}}>Customer</label>
              <div style={{display:'flex',alignItems:'center',gap:8}}>
                <label style={{display:'flex',alignItems:'center',gap:4,fontSize:11,color: unlinkedOnly ? 'var(--accent)' : 'var(--muted)',cursor:'pointer',fontWeight: unlinkedOnly ? 600 : 400}}>
                  <input
                    type="checkbox"
                    checked={unlinkedOnly}
                    onChange={e => setUnlinkedOnly(e.target.checked)}
                    style={{cursor:'pointer',accentColor:'var(--accent)'}}
                  />
                  New customers only
                </label>
                <button type="button" onClick={() => load(currentPage)}
                  style={{fontSize:11,background:'none',border:'1px solid var(--border)',borderRadius:5,padding:'2px 8px',color:'var(--muted)',cursor:'pointer'}}>
                  ⟳
                </button>
              </div>
            </div>
            <select style={inp} value={selectedUserId} onChange={e=>setSelectedUserId(e.target.value)}>
              <option value="">— Select a customer —</option>
              {customers.map(c => (
                <option key={c.id} value={c.id}>{c.username}</option>
              ))}
            </select>
            {customers.length === 0 && (
              <div style={{fontSize:11,color:'var(--muted)',marginTop:-8,marginBottom:12}}>
                No customers found. Create a CUSTOMER user in the Admin portal first.
              </div>
            )}
            <label style={lbl}>Currency</label>
            <select style={inp} value={cur} onChange={e=>setCur(e.target.value)}>
              {['INR','USD','EUR','GBP','SGD'].map(c=><option key={c}>{c}</option>)}
            </select>
            <Btn variant="green" disabled={creating || !selectedUserId}>
              {creating ? 'Creating…' : 'Open account'}
            </Btn>
          </form>
        </div>

        {/* ── Update status ── */}
        <div style={{background:'var(--surface)',border:'1px solid var(--border)',borderRadius:10,padding:20}}>
          <div style={{fontSize:14,fontWeight:500,marginBottom:16}}>Update status</div>
          <form onSubmit={handleStatusUpdate}>
            <label style={lbl}>Account ID <span style={{color:'var(--accent)',fontWeight:600}}>(click any row to select)</span></label>
            <input style={{...inp, background: statusId ? 'rgba(245,158,11,0.08)' : undefined}} type="number" value={statusId} onChange={e=>setStatusId(e.target.value)} placeholder="Click a row or enter numeric ID" />
            <label style={lbl}>New status</label>
            <select style={inp} value={newStatus} onChange={e=>setNewStatus(e.target.value)}>
              <option>ACTIVE</option><option>FROZEN</option><option>CLOSED</option>
            </select>
            <Btn variant="danger" disabled={updating}>{updating?'Updating…':'Update status'}</Btn>
            <p style={{fontSize:11,color:'var(--muted)',marginTop:10}}>FROZEN blocks all transactions. CLOSED is permanent.</p>
          </form>
        </div>
      </div>

      {/* ── Accounts table ── */}
      <div style={{background:'var(--surface)',border:'1px solid var(--border)',borderRadius:10}}>
        <div style={{padding:'14px 20px',borderBottom:'1px solid var(--border)',display:'flex',alignItems:'center',justifyContent:'space-between'}}>
          <span style={{fontSize:14,fontWeight:500}}>All accounts</span>
          <span style={{fontSize:12,color:'var(--muted)'}}>{totalElements} total</span>
        </div>
        <div style={{overflowX:'auto'}}>
          <table style={{width:'100%',borderCollapse:'collapse'}}>
            <thead>
              <tr>
                {['ID','Account #','Holder','Currency','Balance','Status','Created','Actions'].map(h=>(
                  <th key={h} style={{...th, textAlign: h==='Actions'?'center':th.textAlign}}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {loading
                ? <tr><td colSpan={8} style={{...td,textAlign:'center',color:'var(--muted)',padding:32}}>Loading…</td></tr>
                : accounts.length === 0
                ? <tr><td colSpan={8} style={{...td,textAlign:'center',color:'var(--muted)',padding:32}}>No accounts yet</td></tr>
                : accounts.map(a => (
                  <tr key={a.id}
                    onClick={() => setStatusId(String(a.id))}
                    title="Click to select for status update"
                    style={{cursor:'pointer'}}>
                    <td style={{...td,color:'var(--muted)'}}>{a.id}</td>
                    <td style={td}><span style={{fontFamily:'monospace',fontSize:12,color:'#818cf8'}}>{a.accountNumber}</span></td>
                    <td style={td}>{a.holderName}</td>
                    <td style={{...td,color:'var(--muted)'}}>{a.currency}</td>
                    <td style={{...td,fontWeight:600}}>{a.formattedBalance}</td>
                    <td style={td}><Badge status={a.status}/></td>
                    <td style={{...td,color:'var(--muted)',fontSize:12}}>{fmt(a.createdAt)}</td>
                    <td style={{...td,textAlign:'center'}}>
                      <div style={{display:'flex',gap:5,justifyContent:'center'}}>
                        <button
                          onClick={() => setCashModal({ account: a, mode: 'deposit' })}
                          disabled={a.status !== 'ACTIVE'}
                          title="Deposit cash"
                          style={{padding:'4px 10px',background:'rgba(34,197,94,0.1)',border:'1px solid rgba(34,197,94,0.25)',borderRadius:6,color:'#22c55e',fontSize:12,cursor:a.status!=='ACTIVE'?'not-allowed':'pointer',opacity:a.status!=='ACTIVE'?0.4:1}}>
                          ↓ Dep
                        </button>
                        <button
                          onClick={() => setCashModal({ account: a, mode: 'withdraw' })}
                          disabled={a.status !== 'ACTIVE'}
                          title="Withdraw cash"
                          style={{padding:'4px 10px',background:'rgba(215,119,6,0.1)',border:'1px solid rgba(215,119,6,0.25)',borderRadius:6,color:'#d97706',fontSize:12,cursor:a.status!=='ACTIVE'?'not-allowed':'pointer',opacity:a.status!=='ACTIVE'?0.4:1}}>
                          ↑ Wdw
                        </button>
                        <Btn variant="secondary" style={{padding:'4px 10px',fontSize:12}} onClick={()=>openLedger(a)}>
                          Ledger
                        </Btn>
                      </div>
                    </td>
                  </tr>
                ))
              }
            </tbody>
          </table>
        </div>
        {totalPages > 1 && (
          <div style={{display:'flex',justifyContent:'center',alignItems:'center',gap:12,padding:14,borderTop:'1px solid var(--border)'}}>
            <Btn variant="secondary" style={{padding:'5px 14px',fontSize:12}} disabled={currentPage===0} onClick={()=>load(currentPage-1)}>← Prev</Btn>
            <span style={{fontSize:13,color:'var(--muted)'}}>Page {currentPage+1} of {totalPages}</span>
            <Btn variant="secondary" style={{padding:'5px 14px',fontSize:12}} disabled={currentPage>=totalPages-1} onClick={()=>load(currentPage+1)}>Next →</Btn>
          </div>
        )}
      </div>

      {/* ── Cash operation modal ── */}
      {cashModal && (
        <CashModal
          account={cashModal.account}
          mode={cashModal.mode}
          onClose={() => setCashModal(null)}
          onDone={handleCashDone}
        />
      )}

      {/* ── Ledger drawer ── */}
      {drawerAccount && (
        <div style={{position:'fixed',inset:0,background:'rgba(0,0,0,0.6)',zIndex:100,display:'flex',alignItems:'flex-end'}} onClick={()=>setDrawerAccount(null)}>
          <div
            style={{background:'var(--surface)',border:'1px solid var(--border)',borderRadius:'12px 12px 0 0',width:'100%',maxHeight:'72vh',overflow:'auto',padding:24}}
            onClick={e=>e.stopPropagation()}
          >
            <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:20}}>
              <div>
                <div style={{fontSize:15,fontWeight:600}}>{drawerAccount.holderName}</div>
                <div style={{fontSize:12,color:'var(--muted)',fontFamily:'monospace',marginTop:2}}>{drawerAccount.accountNumber} · {drawerAccount.formattedBalance}</div>
              </div>
              <button onClick={()=>setDrawerAccount(null)} style={{background:'none',border:'1px solid var(--border)',borderRadius:6,padding:'5px 12px',color:'var(--muted)',fontSize:12}}>Close</button>
            </div>

            {loadingLedger
              ? <div style={{textAlign:'center',padding:40,color:'var(--muted)',fontSize:13}}>Loading ledger…</div>
              : ledgerEntries.length === 0
              ? <div style={{textAlign:'center',padding:40,color:'var(--muted)',fontSize:13}}>No entries for this account</div>
              : (
                <table style={{width:'100%',borderCollapse:'collapse',fontSize:13}}>
                  <thead><tr>
                    {['Date','Type','Ref ID','Counterparty','Debit','Credit'].map(h=>(
                      <th key={h} style={{...th,textAlign:['Debit','Credit'].includes(h)?'right':'left'}}>{h}</th>
                    ))}
                  </tr></thead>
                  <tbody>
                    {ledgerEntries.map(e=>(
                      <tr key={e.id}>
                        <td style={{...td,fontSize:12,color:'var(--muted)'}}>{fmt(e.createdAt)}</td>
                        <td style={td}>
                          <span style={{display:'inline-block',padding:'2px 7px',borderRadius:4,fontSize:11,fontWeight:500,background:e.entryType==='DEBIT'?'rgba(239,68,68,0.1)':'rgba(34,197,94,0.1)',color:e.entryType==='DEBIT'?'var(--red)':'var(--green)'}}>
                            {e.entryType}
                          </span>
                        </td>
                        <td style={td}><span style={{fontFamily:'monospace',fontSize:10,color:'#818cf8'}}>{e.referenceId}</span></td>
                        <td style={td}><span style={{fontFamily:'monospace',fontSize:11}}>{e.counterpartyAccountNumber}</span></td>
                        <td style={{...td,textAlign:'right',color:'var(--red)',fontWeight:500}}>{e.entryType==='DEBIT'?e.formattedAmount:''}</td>
                        <td style={{...td,textAlign:'right',color:'var(--green)',fontWeight:500}}>{e.entryType==='CREDIT'?e.formattedAmount:''}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )
            }
          </div>
        </div>
      )}
    </div>
  )
}
