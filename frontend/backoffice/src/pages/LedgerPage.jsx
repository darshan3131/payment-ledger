import { useState } from 'react'
import { getAccountByNumber, getLedgerStatement } from '../services/api'

const th  = { padding:'10px 16px', textAlign:'left', color:'var(--muted)', fontWeight:500, fontSize:11, textTransform:'uppercase', letterSpacing:'0.06em', borderBottom:'1px solid var(--border)', whiteSpace:'nowrap' }
const td  = { padding:'12px 16px', borderBottom:'1px solid var(--border)', fontSize:13 }
const fmt = dt => dt ? new Date(dt).toLocaleString('en-IN',{day:'2-digit',month:'short',year:'numeric',hour:'2-digit',minute:'2-digit'}) : '—'

export default function LedgerPage() {
  const [search, setSearch]   = useState('')
  const [account, setAccount] = useState(null)
  const [entries, setEntries] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError]     = useState('')

  async function handleSearch(e) {
    e.preventDefault()
    const val = search.trim().toUpperCase()
    if (!val) return
    setLoading(true); setError(''); setAccount(null); setEntries([])
    try {
      const acct   = await getAccountByNumber(val)
      const ledger = await getLedgerStatement(acct.id)
      setAccount(acct)
      setEntries(ledger)
    } catch {
      setError('Account not found: ' + val)
    } finally { setLoading(false) }
  }

  const totalDebits  = entries.filter(e=>e.entryType==='DEBIT').reduce((s,e)=>s+e.amount,0)
  const totalCredits = entries.filter(e=>e.entryType==='CREDIT').reduce((s,e)=>s+e.amount,0)
  const computedBalance = totalCredits - totalDebits
  const balanceMatch = account ? computedBalance === account.balance : null

  return (
    <div>
      <div style={{marginBottom:24}}>
        <h1 style={{fontSize:20,fontWeight:600,letterSpacing:'-0.01em'}}>Ledger</h1>
        <p style={{fontSize:13,color:'var(--muted)',marginTop:3}}>Double-entry statement for any account</p>
      </div>

      <form onSubmit={handleSearch} style={{display:'flex',gap:10,marginBottom:24}}>
        <input
          value={search} onChange={e=>setSearch(e.target.value)}
          placeholder="Account number (e.g. ACC1A2B3C4D5E6)"
          style={{flex:1,background:'var(--surface)',border:'1px solid var(--border)',borderRadius:8,padding:'10px 14px',color:'var(--text)',fontSize:13}}
        />
        <button
          style={{padding:'10px 20px',background:'var(--accent)',color:'white',border:'none',borderRadius:8,fontSize:13,fontWeight:500}}
        >
          {loading ? 'Loading…' : 'Load statement'}
        </button>
      </form>

      {error && (
        <div style={{background:'rgba(239,68,68,0.08)',border:'1px solid rgba(239,68,68,0.2)',borderRadius:8,padding:'12px 16px',color:'var(--red)',marginBottom:16,fontSize:13}}>
          {error}
        </div>
      )}

      {account && (
        <>
          {/* Summary cards */}
          <div style={{display:'grid',gridTemplateColumns:'repeat(4,1fr)',gap:14,marginBottom:20}}>
            {[
              { label:'Account holder', value: account.holderName },
              { label:'Reported balance', value: account.formattedBalance },
              {
                label:'Computed balance',
                value: account.currency==='INR' ? `₹${(computedBalance/100).toFixed(2)}` : `${(computedBalance/100).toFixed(2)}`,
                ok: balanceMatch
              },
              { label:'Total entries', value: entries.length },
            ].map((s,i) => (
              <div key={i} style={{
                background:'var(--surface)', borderRadius:10, padding:16,
                border: s.ok===false ? '1px solid rgba(239,68,68,0.35)' : s.ok===true ? '1px solid rgba(34,197,94,0.25)' : '1px solid var(--border)'
              }}>
                <div style={{fontSize:11,color:'var(--muted)',textTransform:'uppercase',letterSpacing:'0.06em',marginBottom:6}}>{s.label}</div>
                <div style={{fontSize:17,fontWeight:600,color: s.ok===false?'var(--red)':s.ok===true?'var(--green)':'var(--text)'}}>{s.value}</div>
                {s.ok===true  && <div style={{fontSize:11,color:'var(--green)',marginTop:4}}>Matches reported</div>}
                {s.ok===false && <div style={{fontSize:11,color:'var(--red)',marginTop:4}}>Mismatch — reconcile</div>}
              </div>
            ))}
          </div>

          {/* Ledger table */}
          <div style={{background:'var(--surface)',border:'1px solid var(--border)',borderRadius:10}}>
            <div style={{padding:'14px 20px',borderBottom:'1px solid var(--border)',display:'flex',justifyContent:'space-between',alignItems:'center'}}>
              <span style={{fontSize:14,fontWeight:500}}>{account.accountNumber}</span>
              <div style={{display:'flex',gap:20,fontSize:12}}>
                <span style={{color:'var(--red)'}}>Debits: <strong>₹{(totalDebits/100).toFixed(2)}</strong></span>
                <span style={{color:'var(--green)'}}>Credits: <strong>₹{(totalCredits/100).toFixed(2)}</strong></span>
              </div>
            </div>
            {entries.length === 0
              ? <div style={{textAlign:'center',padding:40,color:'var(--muted)',fontSize:13}}>No entries</div>
              : (
                <div style={{overflowX:'auto'}}>
                  <table style={{width:'100%',borderCollapse:'collapse'}}>
                    <thead><tr>
                      {['Date','Type','Ref ID','Tx Type','Counterparty','Description','Amount'].map(h=>(
                        <th key={h} style={{...th,textAlign:h==='Amount'?'right':'left'}}>{h}</th>
                      ))}
                    </tr></thead>
                    <tbody>
                      {entries.map(e => (
                        <tr key={e.id}>
                          <td style={{...td,fontSize:11,color:'var(--muted)'}}>{fmt(e.createdAt)}</td>
                          <td style={td}>
                            <span style={{display:'inline-block',padding:'2px 7px',borderRadius:4,fontSize:11,fontWeight:500,background:e.entryType==='DEBIT'?'rgba(239,68,68,0.1)':'rgba(34,197,94,0.1)',color:e.entryType==='DEBIT'?'var(--red)':'var(--green)'}}>
                              {e.entryType}
                            </span>
                          </td>
                          <td style={td}><span style={{fontFamily:'monospace',fontSize:10,color:'#818cf8'}}>{e.referenceId}</span></td>
                          <td style={{...td,fontSize:11,color:'var(--muted)'}}>{e.transactionType}</td>
                          <td style={td}><span style={{fontFamily:'monospace',fontSize:11}}>{e.counterpartyAccountNumber}</span></td>
                          <td style={{...td,maxWidth:120,overflow:'hidden',textOverflow:'ellipsis',whiteSpace:'nowrap',color:'var(--muted)'}}>{e.description||'—'}</td>
                          <td style={{...td,textAlign:'right',fontWeight:600,color:e.entryType==='DEBIT'?'var(--red)':'var(--green)'}}>
                            {e.entryType==='DEBIT'?'−':'+'}{e.formattedAmount}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )
            }
          </div>
        </>
      )}
    </div>
  )
}
