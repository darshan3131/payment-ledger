import { useState, useEffect, useCallback } from 'react'
import { getAllTransactions, reverseTransaction } from '../services/api'

const th = { padding:'10px 16px', textAlign:'left', color:'var(--muted)', fontWeight:500, fontSize:11, textTransform:'uppercase', letterSpacing:'0.06em', borderBottom:'1px solid var(--border)', whiteSpace:'nowrap' }
const td = { padding:'12px 16px', borderBottom:'1px solid var(--border)', fontSize:13 }

const fmt = dt => dt ? new Date(dt).toLocaleString('en-IN',{day:'2-digit',month:'short',hour:'2-digit',minute:'2-digit'}) : '—'

function StatusBadge({ status }) {
  const map = {
    COMPLETED: { bg:'rgba(34,197,94,0.1)',  color:'#22c55e' },
    PENDING:   { bg:'rgba(215,119,6,0.1)',  color:'#d97706' },
    FAILED:    { bg:'rgba(239,68,68,0.1)',  color:'#ef4444' },
    REVERSED:  { bg:'rgba(148,163,184,0.1)', color:'#94a3b8' },
  }
  const s = map[status] || { bg:'rgba(94,106,210,0.1)', color:'var(--accent)' }
  return <span style={{display:'inline-block',padding:'2px 8px',borderRadius:4,fontSize:11,fontWeight:500,...s}}>{status}</span>
}

function TypeBadge({ type }) {
  return <span style={{display:'inline-block',padding:'2px 8px',borderRadius:4,fontSize:11,fontWeight:500,background:'rgba(94,106,210,0.1)',color:'var(--accent)'}}>{type}</span>
}

export default function TransactionsPage() {
  const [txns, setTxns]           = useState([])
  const [loading, setLoading]     = useState(true)
  const [filter, setFilter]       = useState('')
  const [statusF, setStatusF]     = useState('')
  const [currentPage, setCurrentPage]     = useState(0)
  const [totalPages, setTotalPages]       = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [reverseModal, setReverseModal]   = useState(null) // { referenceId }
  const [reverseReason, setReverseReason] = useState('')
  const [reversing, setReversing]         = useState(false)
  const [reverseError, setReverseError]   = useState('')

  const load = useCallback(async (page = 0) => {
    setLoading(true)
    try {
      const paged = await getAllTransactions(page, 20)
      setTxns(paged.content)
      setCurrentPage(paged.page)
      setTotalPages(paged.totalPages)
      setTotalElements(paged.totalElements)
    } catch(e) { console.error(e) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { load(0) }, [load])

  async function handleReverse() {
    setReversing(true); setReverseError('')
    try {
      await reverseTransaction(reverseModal.referenceId, reverseReason)
      setReverseModal(null); setReverseReason('')
      await load(currentPage)
    } catch (err) {
      setReverseError(err.response?.data?.error || 'Reversal failed.')
    } finally { setReversing(false) }
  }

  const filtered = txns.filter(t => {
    const q = filter.toLowerCase()
    const matchQ = !q || t.referenceId?.toLowerCase().includes(q)
      || t.sourceAccountNumber?.toLowerCase().includes(q)
      || t.destinationAccountNumber?.toLowerCase().includes(q)
      || (t.description||'').toLowerCase().includes(q)
    return matchQ && (!statusF || t.status === statusF)
  })

  return (
    <div>
      <div style={{marginBottom:24}}>
        <h1 style={{fontSize:20,fontWeight:600,letterSpacing:'-0.01em'}}>Transactions</h1>
        <p style={{fontSize:13,color:'var(--muted)',marginTop:3}}>Full audit trail of all money movements</p>
      </div>

      <div style={{background:'var(--surface)',border:'1px solid var(--border)',borderRadius:10}}>
        {/* Filters */}
        <div style={{padding:'12px 16px',borderBottom:'1px solid var(--border)',display:'flex',gap:10,alignItems:'center'}}>
          <input
            value={filter} onChange={e=>setFilter(e.target.value)}
            placeholder="Search ref ID, account, description…"
            style={{flex:1,background:'var(--surface2)',border:'1px solid var(--border)',borderRadius:7,padding:'8px 12px',color:'var(--text)',fontSize:13}}
          />
          <select
            value={statusF} onChange={e=>setStatusF(e.target.value)}
            style={{background:'var(--surface2)',border:'1px solid var(--border)',borderRadius:7,padding:'8px 12px',color:'var(--text)',fontSize:13}}
          >
            <option value=''>All statuses</option>
            <option>COMPLETED</option><option>PENDING</option><option>FAILED</option>
          </select>
          <button onClick={()=>load(currentPage)} style={{padding:'8px 12px',background:'none',border:'1px solid var(--border)',borderRadius:7,color:'var(--muted)',fontSize:12}}>
            Refresh
          </button>
          <span style={{fontSize:12,color:'var(--muted)',whiteSpace:'nowrap'}}>{filtered.length} / {totalElements}</span>
        </div>

        <div style={{overflowX:'auto'}}>
          <table style={{width:'100%',borderCollapse:'collapse'}}>
            <thead>
              <tr>
                {['Ref ID','From','To','Amount','Type','Status','Date','Description','Action'].map(h=><th key={h} style={th}>{h}</th>)}
              </tr>
            </thead>
            <tbody>
              {loading
                ? <tr><td colSpan={8} style={{...td,textAlign:'center',color:'var(--muted)',padding:32}}>Loading…</td></tr>
                : filtered.length === 0
                ? <tr><td colSpan={8} style={{...td,textAlign:'center',color:'var(--muted)',padding:32}}>No transactions found</td></tr>
                : filtered.map(t => (
                  <tr key={t.id}>
                    <td style={td}><span style={{fontFamily:'monospace',fontSize:11,color:'#818cf8'}}>{t.referenceId}</span></td>
                    <td style={td}><span style={{fontFamily:'monospace',fontSize:11}}>{t.sourceAccountNumber}</span></td>
                    <td style={td}><span style={{fontFamily:'monospace',fontSize:11}}>{t.destinationAccountNumber}</span></td>
                    <td style={{...td,fontWeight:600}}>{t.formattedAmount}</td>
                    <td style={td}><TypeBadge type={t.type}/></td>
                    <td style={td}><StatusBadge status={t.status}/></td>
                    <td style={{...td,fontSize:12,color:'var(--muted)'}}>{fmt(t.createdAt)}</td>
                    <td style={{...td,maxWidth:140,overflow:'hidden',textOverflow:'ellipsis',whiteSpace:'nowrap',color:'var(--muted)'}}>{t.description||'—'}</td>
                    <td style={td}>
                      {t.status === 'COMPLETED' && (t.type === 'TRANSFER' || t.type === 'PAYMENT') && (
                        <button
                          onClick={() => { setReverseModal({referenceId: t.referenceId}); setReverseReason(''); setReverseError('') }}
                          style={{padding:'4px 10px',background:'rgba(239,68,68,0.08)',border:'1px solid rgba(239,68,68,0.25)',borderRadius:6,color:'#ef4444',fontSize:12,cursor:'pointer'}}
                        >Reverse</button>
                      )}
                    </td>
                  </tr>
                ))
              }
            </tbody>
          </table>
        </div>

        {totalPages > 1 && (
          <div style={{display:'flex',justifyContent:'center',alignItems:'center',gap:12,padding:14,borderTop:'1px solid var(--border)'}}>
            <button disabled={currentPage===0} onClick={()=>load(currentPage-1)} style={{padding:'5px 14px',background:'none',border:'1px solid var(--border)',borderRadius:6,color:'var(--muted)',fontSize:12,opacity:currentPage===0?0.4:1}}>← Prev</button>
            <span style={{fontSize:13,color:'var(--muted)'}}>Page {currentPage+1} of {totalPages}</span>
            <button disabled={currentPage>=totalPages-1} onClick={()=>load(currentPage+1)} style={{padding:'5px 14px',background:'none',border:'1px solid var(--border)',borderRadius:6,color:'var(--muted)',fontSize:12,opacity:currentPage>=totalPages-1?0.4:1}}>Next →</button>
          </div>
        )}
      </div>

      {/* Reverse Transaction Modal */}
      {reverseModal && (
        <div style={{position:'fixed',inset:0,background:'rgba(0,0,0,0.55)',display:'flex',alignItems:'center',justifyContent:'center',zIndex:1000}}>
          <div style={{background:'var(--surface)',border:'1px solid var(--border)',borderRadius:12,padding:28,width:420,maxWidth:'90vw'}}>
            <h3 style={{fontSize:16,fontWeight:600,marginBottom:6}}>Reverse Transaction</h3>
            <p style={{fontSize:13,color:'var(--muted)',marginBottom:16}}>
              Ref: <span style={{fontFamily:'monospace',color:'#818cf8'}}>{reverseModal.referenceId}</span>
            </p>
            <label style={{fontSize:12,color:'var(--muted)',display:'block',marginBottom:6}}>Reason (optional)</label>
            <textarea
              value={reverseReason}
              onChange={e=>setReverseReason(e.target.value)}
              placeholder="e.g. Customer dispute, duplicate charge…"
              rows={3}
              style={{width:'100%',background:'var(--surface2)',border:'1px solid var(--border)',borderRadius:7,padding:'8px 12px',color:'var(--text)',fontSize:13,resize:'vertical',boxSizing:'border-box'}}
            />
            {reverseError && <p style={{color:'#ef4444',fontSize:12,marginTop:8}}>{reverseError}</p>}
            <div style={{display:'flex',gap:10,marginTop:18,justifyContent:'flex-end'}}>
              <button
                onClick={()=>{ setReverseModal(null); setReverseReason(''); setReverseError('') }}
                style={{padding:'8px 18px',background:'none',border:'1px solid var(--border)',borderRadius:7,color:'var(--muted)',fontSize:13,cursor:'pointer'}}
              >Cancel</button>
              <button
                onClick={handleReverse}
                disabled={reversing}
                style={{padding:'8px 18px',background:'rgba(239,68,68,0.12)',border:'1px solid rgba(239,68,68,0.35)',borderRadius:7,color:'#ef4444',fontSize:13,fontWeight:600,cursor:'pointer',opacity:reversing?0.6:1}}
              >{reversing ? 'Reversing…' : 'Confirm Reversal'}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
