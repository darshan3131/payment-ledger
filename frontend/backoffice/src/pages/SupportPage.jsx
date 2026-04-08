import { useState, useEffect, useCallback } from 'react'
import { getAllTickets, updateTicket } from '../services/api'

const th = { padding:'10px 16px', textAlign:'left', color:'var(--muted)', fontWeight:500, fontSize:11, textTransform:'uppercase', letterSpacing:'0.06em', borderBottom:'1px solid var(--border)', whiteSpace:'nowrap' }
const td = { padding:'12px 16px', borderBottom:'1px solid var(--border)', fontSize:13 }

const STATUS_COLORS = {
  OPEN:        { bg:'rgba(215,119,6,0.1)',   color:'#d97706' },
  IN_PROGRESS: { bg:'rgba(129,140,248,0.1)', color:'#818cf8' },
  RESOLVED:    { bg:'rgba(34,197,94,0.1)',   color:'#22c55e' },
  CLOSED:      { bg:'rgba(148,163,184,0.1)', color:'#94a3b8' },
}

function StatusBadge({ status }) {
  const s = STATUS_COLORS[status] || STATUS_COLORS.OPEN
  return <span style={{display:'inline-block',padding:'2px 8px',borderRadius:4,fontSize:11,fontWeight:500,...s}}>{status}</span>
}

export default function SupportPage() {
  const [tickets,      setTickets]      = useState([])
  const [loading,      setLoading]      = useState(true)
  const [statusFilter, setStatusFilter] = useState('')
  const [currentPage,  setCurrentPage]  = useState(0)
  const [totalPages,   setTotalPages]   = useState(0)
  const [totalElements, setTotalElements] = useState(0)

  // Edit modal
  const [modal,      setModal]      = useState(null) // ticket object
  const [newStatus,  setNewStatus]  = useState('')
  const [resolution, setResolution] = useState('')
  const [saving,     setSaving]     = useState(false)
  const [saveError,  setSaveError]  = useState('')

  const load = useCallback(async (page = 0, status = statusFilter) => {
    setLoading(true)
    try {
      const data = await getAllTickets(page, 20, status)
      setTickets(data.content || [])
      setCurrentPage(data.page)
      setTotalPages(data.totalPages)
      setTotalElements(data.totalElements)
    } catch(e) { console.error(e) }
    finally { setLoading(false) }
  }, [statusFilter])

  useEffect(() => { load(0, statusFilter) }, [statusFilter])

  function openModal(t) {
    setModal(t)
    setNewStatus(t.status)
    setResolution(t.resolution || '')
    setSaveError('')
  }

  async function handleSave() {
    setSaving(true); setSaveError('')
    try {
      await updateTicket(modal.id, { status: newStatus, resolution })
      setModal(null)
      load(currentPage)
    } catch(err) {
      setSaveError(err.response?.data?.error || 'Update failed')
    } finally { setSaving(false) }
  }

  return (
    <div>
      <div style={{marginBottom:24}}>
        <h1 style={{fontSize:20,fontWeight:600,letterSpacing:'-0.01em'}}>Support Tickets</h1>
        <p style={{fontSize:13,color:'var(--muted)',marginTop:3}}>Customer helpdesk — view and resolve open issues</p>
      </div>

      <div style={{background:'var(--surface)',border:'1px solid var(--border)',borderRadius:10}}>
        {/* Filters */}
        <div style={{padding:'12px 16px',borderBottom:'1px solid var(--border)',display:'flex',gap:10,alignItems:'center'}}>
          <select
            value={statusFilter} onChange={e=>{setStatusFilter(e.target.value);setCurrentPage(0)}}
            style={{background:'var(--surface2)',border:'1px solid var(--border)',borderRadius:7,padding:'8px 12px',color:'var(--text)',fontSize:13}}
          >
            <option value=''>All statuses</option>
            <option>OPEN</option>
            <option>IN_PROGRESS</option>
            <option>RESOLVED</option>
            <option>CLOSED</option>
          </select>
          <button onClick={()=>load(currentPage)} style={{padding:'8px 12px',background:'none',border:'1px solid var(--border)',borderRadius:7,color:'var(--muted)',fontSize:12}}>
            Refresh
          </button>
          <span style={{fontSize:12,color:'var(--muted)',marginLeft:'auto'}}>{totalElements} tickets</span>
        </div>

        <div style={{overflowX:'auto'}}>
          <table style={{width:'100%',borderCollapse:'collapse'}}>
            <thead>
              <tr>
                {['#','Customer','Subject','Ref','Status','Created','Action'].map(h=><th key={h} style={th}>{h}</th>)}
              </tr>
            </thead>
            <tbody>
              {loading
                ? <tr><td colSpan={7} style={{...td,textAlign:'center',color:'var(--muted)',padding:32}}>Loading…</td></tr>
                : tickets.length === 0
                ? <tr><td colSpan={7} style={{...td,textAlign:'center',color:'var(--muted)',padding:32}}>No tickets found</td></tr>
                : tickets.map(t => (
                  <tr key={t.id}>
                    <td style={{...td,fontFamily:'monospace',fontSize:11,color:'#818cf8'}}>#{t.id}</td>
                    <td style={td}>{t.username}</td>
                    <td style={{...td,maxWidth:220,overflow:'hidden',textOverflow:'ellipsis',whiteSpace:'nowrap'}}>{t.subject}</td>
                    <td style={{...td,fontFamily:'monospace',fontSize:11,color:'var(--muted)'}}>{t.referenceId || '—'}</td>
                    <td style={td}><StatusBadge status={t.status}/></td>
                    <td style={{...td,fontSize:12,color:'var(--muted)'}}>{new Date(t.createdAt).toLocaleDateString('en-IN')}</td>
                    <td style={td}>
                      <button
                        onClick={() => openModal(t)}
                        style={{padding:'4px 12px',background:'rgba(94,106,210,0.08)',border:'1px solid rgba(94,106,210,0.25)',borderRadius:6,color:'var(--accent)',fontSize:12,cursor:'pointer'}}
                      >Manage</button>
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

      {/* Manage Modal */}
      {modal && (
        <div style={{position:'fixed',inset:0,background:'rgba(0,0,0,0.55)',display:'flex',alignItems:'center',justifyContent:'center',zIndex:1000}}>
          <div style={{background:'var(--surface)',border:'1px solid var(--border)',borderRadius:12,padding:28,width:480,maxWidth:'90vw',maxHeight:'80vh',overflowY:'auto'}}>
            <h3 style={{fontSize:16,fontWeight:600,marginBottom:4}}>Ticket #{modal.id}</h3>
            <p style={{fontSize:13,color:'var(--muted)',marginBottom:16}}>{modal.username} · {new Date(modal.createdAt).toLocaleDateString('en-IN')}</p>

            <div style={{background:'var(--surface2)',borderRadius:8,padding:14,marginBottom:20}}>
              <div style={{fontSize:13,fontWeight:500,marginBottom:6}}>{modal.subject}</div>
              <div style={{fontSize:13,color:'var(--muted)',lineHeight:1.6}}>{modal.description}</div>
              {modal.referenceId && (
                <div style={{marginTop:8,fontSize:12,color:'var(--accent)',fontFamily:'monospace'}}>Txn ref: {modal.referenceId}</div>
              )}
            </div>

            <div style={{marginBottom:14}}>
              <label style={{fontSize:12,color:'var(--muted)',display:'block',marginBottom:6}}>Status</label>
              <select
                value={newStatus}
                onChange={e=>setNewStatus(e.target.value)}
                style={{width:'100%',background:'var(--surface2)',border:'1px solid var(--border)',borderRadius:7,padding:'8px 12px',color:'var(--text)',fontSize:13}}
              >
                <option>OPEN</option>
                <option>IN_PROGRESS</option>
                <option>RESOLVED</option>
                <option>CLOSED</option>
              </select>
            </div>

            <div style={{marginBottom:16}}>
              <label style={{fontSize:12,color:'var(--muted)',display:'block',marginBottom:6}}>Resolution / notes</label>
              <textarea
                value={resolution}
                onChange={e=>setResolution(e.target.value)}
                rows={4}
                placeholder="Explain what was done or what the customer should do…"
                style={{width:'100%',background:'var(--surface2)',border:'1px solid var(--border)',borderRadius:7,padding:'8px 12px',color:'var(--text)',fontSize:13,resize:'vertical',boxSizing:'border-box'}}
              />
            </div>

            {saveError && <p style={{color:'#ef4444',fontSize:12,marginBottom:10}}>{saveError}</p>}

            <div style={{display:'flex',gap:10,justifyContent:'flex-end'}}>
              <button
                onClick={()=>setModal(null)}
                style={{padding:'8px 18px',background:'none',border:'1px solid var(--border)',borderRadius:7,color:'var(--muted)',fontSize:13,cursor:'pointer'}}
              >Cancel</button>
              <button
                onClick={handleSave}
                disabled={saving}
                style={{padding:'8px 18px',background:'var(--accent)',border:'none',borderRadius:7,color:'white',fontSize:13,fontWeight:600,cursor:'pointer',opacity:saving?0.6:1}}
              >{saving ? 'Saving…' : 'Save changes'}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
