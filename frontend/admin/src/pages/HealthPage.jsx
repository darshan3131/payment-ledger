import { useState, useEffect } from 'react'
import { getAnalytics } from '../services/api'

function Dot({ ok }) {
  const color = ok === null ? 'var(--muted)' : ok ? 'var(--green)' : 'var(--red)'
  return <div style={{width:8,height:8,borderRadius:'50%',background:color,boxShadow:ok?`0 0 6px ${color}`:'none'}} />
}

function ServiceRow({ name, sub, ok }) {
  return (
    <div style={{display:'flex',alignItems:'center',justifyContent:'space-between',padding:'12px 0',borderBottom:'1px solid var(--border)'}}>
      <div>
        <div style={{fontSize:13,fontWeight:500}}>{name}</div>
        <div style={{fontSize:11,color:'var(--muted)',marginTop:1}}>{sub}</div>
      </div>
      {ok === null
        ? <span style={{fontSize:11,color:'var(--muted)'}}>Checking…</span>
        : <div style={{display:'flex',alignItems:'center',gap:6}}>
            <Dot ok={ok}/>
            <span style={{fontSize:11,color:ok?'var(--green)':'var(--red)',fontWeight:500}}>{ok?'Healthy':'Unreachable'}</span>
          </div>
      }
    </div>
  )
}

export default function HealthPage() {
  const [data, setData]       = useState(null)
  const [apiOk, setApiOk]     = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getAnalytics()
      .then(d => { setData(d); setApiOk(true) })
      .catch(() => setApiOk(false))
      .finally(() => setLoading(false))
  }, [])

  const pending = data?.pendingTransactions ?? 0
  const failed  = data?.failedTransactions  ?? 0

  const patterns = [
    ['Optimistic locking', '@Version on Account. Hibernate detects version conflicts on concurrent writes and throws an exception — no row locks needed.'],
    ['Outbox pattern', 'Events are written to outbox_events inside the same DB transaction as the payment. OutboxPoller publishes to Kafka every 10s. No event is lost even if Kafka crashes.'],
    ['Idempotency', 'Each request carries an idempotency key. On replay, Redis returns the cached referenceId — the DB write never runs twice.'],
    ['Double-entry ledger', 'Every transfer creates 2 LedgerEntry rows: DEBIT on source, CREDIT on destination. Balance = SUM(CREDITs) − SUM(DEBITs).'],
    ['Reconciliation', 'Background job runs every 5 min. PENDING transactions older than 5 min are marked FAILED — these are mid-flight crashes.'],
  ]

  return (
    <div>
      <div style={{marginBottom:24}}>
        <h1 style={{fontSize:20,fontWeight:600,letterSpacing:'-0.01em'}}>System health</h1>
        <p style={{fontSize:13,color:'var(--muted)',marginTop:3}}>Infrastructure and pipeline status</p>
      </div>

      <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:16,marginBottom:20}}>

        {/* Service status */}
        <div style={{background:'var(--surface)',border:'1px solid var(--border)',borderRadius:10,padding:20}}>
          <div style={{fontSize:14,fontWeight:500,marginBottom:4}}>Services</div>
          <ServiceRow name="Spring Boot API" sub="port 8080"  ok={apiOk}/>
          <ServiceRow name="MySQL"           sub="port 3306"  ok={apiOk}/>
          <ServiceRow name="Redis"           sub="port 6379"  ok={apiOk}/>
          <ServiceRow name="Kafka"           sub="port 9092"  ok={apiOk}/>
        </div>

        {/* Pipeline */}
        <div style={{background:'var(--surface)',border:'1px solid var(--border)',borderRadius:10,padding:20}}>
          <div style={{fontSize:14,fontWeight:500,marginBottom:16}}>Outbox pipeline</div>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:16,marginBottom:16}}>
            <div>
              <div style={{fontSize:11,color:'var(--muted)',textTransform:'uppercase',letterSpacing:'0.06em',marginBottom:4}}>Pending txns</div>
              <div style={{fontSize:28,fontWeight:700,color: loading?'var(--muted)': pending>0?'var(--yellow)':'var(--green)'}}>
                {loading ? '—' : pending}
              </div>
            </div>
            <div>
              <div style={{fontSize:11,color:'var(--muted)',textTransform:'uppercase',letterSpacing:'0.06em',marginBottom:4}}>Failed txns</div>
              <div style={{fontSize:28,fontWeight:700,color: loading?'var(--muted)': failed>0?'var(--red)':'var(--green)'}}>
                {loading ? '—' : failed}
              </div>
            </div>
          </div>
          {!loading && pending > 0 && (
            <div style={{background:'rgba(215,119,6,0.08)',border:'1px solid rgba(215,119,6,0.2)',borderRadius:8,padding:'10px 12px',fontSize:12,color:'var(--yellow)'}}>
              {pending} transaction(s) still pending. ReconciliationService auto-heals after 5 min.
            </div>
          )}
          {!loading && pending === 0 && apiOk && (
            <div style={{background:'rgba(34,197,94,0.06)',border:'1px solid rgba(34,197,94,0.2)',borderRadius:8,padding:'10px 12px',fontSize:12,color:'var(--green)'}}>
              No stuck transactions. Pipeline is healthy.
            </div>
          )}
        </div>
      </div>

      {/* Architecture reference */}
      <div style={{background:'var(--surface)',border:'1px solid var(--border)',borderRadius:10,padding:20}}>
        <div style={{fontSize:14,fontWeight:500,marginBottom:16}}>Architecture patterns</div>
        <div style={{display:'flex',flexDirection:'column',gap:1}}>
          {patterns.map(([title, desc]) => (
            <div key={title} style={{display:'flex',gap:16,padding:'12px 0',borderBottom:'1px solid var(--border)'}}>
              <div style={{fontSize:12,fontWeight:600,minWidth:160,color:'var(--text)',paddingTop:1}}>{title}</div>
              <div style={{fontSize:12,color:'var(--muted)',lineHeight:1.65}}>{desc}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
