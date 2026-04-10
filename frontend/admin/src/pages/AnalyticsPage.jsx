import { useState, useEffect } from 'react'
import { getAnalytics } from '../services/api'

// WHY call GET /api/v1/analytics instead of fetching raw data?
// AnalyticsService on the backend pre-aggregates everything in Java streams.
// The DB returns ~6 numbers + 3 maps + 2 lists of 5 items max.
// The old approach fetched ALL transactions/accounts to the browser — breaks at 10k+ rows.
// Server-side aggregation = O(n) DB scan vs O(n) network transfer. At scale, n differs by orders of magnitude.

const fmtAmt = (str) => str || '₹0.00'
const badge = (color, text) => (
  <span style={{display:'inline-block',padding:'2px 8px',borderRadius:99,fontSize:11,fontWeight:600,background:`rgba(${color},.15)`,color:`rgb(${color})`}}>{text}</span>
)

export default function AnalyticsPage() {
  const [data, setData]       = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState(null)

  useEffect(() => {
    getAnalytics()
      .then(d => setData(d))
      .catch(e => setError(e.message || 'Failed to load analytics'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div style={{textAlign:'center',padding:80,color:'var(--muted)'}}>Loading analytics...</div>
  if (error)   return <div style={{textAlign:'center',padding:80,color:'rgb(239,68,68)'}}>{error}</div>
  if (!data)   return null

  const {
    totalAccounts, activeAccounts,
    totalTransactions, completedTransactions, pendingTransactions, failedTransactions,
    formattedTotalVolume, formattedAverageSize,
    volumeByType, countByStatus,
    topSenders, topReceivers
  } = data

  const statCards = [
    { label:'Total Accounts',  value: totalAccounts,          sub:`${activeAccounts} active`,           color:'139,92,246' },
    { label:'Total Volume',    value: formattedTotalVolume,   sub:'completed txns only',                color:'34,197,94'  },
    { label:'Completed Txns',  value: completedTransactions,  sub:`of ${totalTransactions} total`,      color:'34,197,94'  },
    { label:'Pending',         value: pendingTransactions,    sub:'may need attention',                 color:'245,158,11' },
    { label:'Failed',          value: failedTransactions,     sub:'check logs',                        color:'239,68,68'  },
    { label:'Avg Txn Size',    value: formattedAverageSize,   sub:'per completed txn',                  color:'99,102,241' },
  ]

  const th = { padding:'10px 14px', textAlign:'left', color:'var(--muted)', fontWeight:500, fontSize:11, textTransform:'uppercase', letterSpacing:0.5, borderBottom:'1px solid var(--border)' }
  const td = { padding:'12px 14px', borderBottom:'1px solid var(--border)', fontSize:13 }

  return (
    <div>
      <h2 style={{fontSize:22,fontWeight:700,marginBottom:4}}>Analytics</h2>
      <p style={{fontSize:13,color:'var(--muted)',marginBottom:24}}>System-wide metrics — pre-aggregated server-side</p>

      {/* STAT CARDS */}
      <div style={{display:'grid',gridTemplateColumns:'repeat(3,1fr)',gap:16,marginBottom:28}}>
        {statCards.map((s,i)=>(
          <div key={i} style={{background:'var(--surface)',border:'1px solid var(--border)',borderRadius:12,padding:20,position:'relative',overflow:'hidden'}}>
            <div style={{position:'absolute',top:0,left:0,right:0,height:3,background:`rgb(${s.color})`}}/>
            <div style={{fontSize:11,color:'var(--muted)',textTransform:'uppercase',letterSpacing:1}}>{s.label}</div>
            <div style={{fontSize:26,fontWeight:700,marginTop:8}}>{s.value}</div>
            <div style={{fontSize:12,color:'var(--muted)',marginTop:4}}>{s.sub}</div>
          </div>
        ))}
      </div>

      <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:20,marginBottom:20}}>
        {/* Status breakdown */}
        <div style={{background:'var(--surface)',border:'1px solid var(--border)',borderRadius:12}}>
          <div style={{padding:'16px 20px',borderBottom:'1px solid var(--border)',fontSize:15,fontWeight:600}}>Transaction Status Breakdown</div>
          <div style={{padding:20}}>
            {[
              {label:'COMPLETED', count: completedTransactions, color:'34,197,94'},
              {label:'PENDING',   count: pendingTransactions,   color:'245,158,11'},
              {label:'FAILED',    count: failedTransactions,    color:'239,68,68'},
            ].map(({label,count,color})=>(
              <div key={label} style={{marginBottom:16}}>
                <div style={{display:'flex',justifyContent:'space-between',fontSize:13,marginBottom:6}}>
                  <span>{label}</span>
                  <span style={{color:'var(--muted)'}}>
                    {count} ({totalTransactions ? ((count/totalTransactions)*100).toFixed(1) : 0}%)
                  </span>
                </div>
                <div style={{height:8,background:'var(--surface2)',borderRadius:4,overflow:'hidden'}}>
                  <div style={{height:'100%',width:`${totalTransactions ? (count/totalTransactions)*100 : 0}%`,background:`rgb(${color})`,borderRadius:4,transition:'width 0.5s'}}/>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Transaction type breakdown */}
        <div style={{background:'var(--surface)',border:'1px solid var(--border)',borderRadius:12}}>
          <div style={{padding:'16px 20px',borderBottom:'1px solid var(--border)',fontSize:15,fontWeight:600}}>Volume by Type</div>
          <div style={{padding:20}}>
            {!volumeByType || Object.entries(volumeByType).length === 0
              ? <div style={{color:'var(--muted)',textAlign:'center',padding:20}}>No data</div>
              : Object.entries(volumeByType).map(([type, amount])=>(
                <div key={type} style={{display:'flex',justifyContent:'space-between',alignItems:'center',padding:'10px 0',borderBottom:'1px solid var(--border)'}}>
                  <span style={{fontSize:13}}>{type}</span>
                  <div style={{display:'flex',alignItems:'center',gap:8}}>
                    <span style={{fontSize:13,color:'var(--muted)'}}>{countByStatus?.[type] || ''}</span>
                    <span style={{fontSize:13,fontWeight:600,color:'rgb(99,102,241)'}}>{'₹' + (amount/100).toLocaleString('en-IN',{minimumFractionDigits:2})}</span>
                  </div>
                </div>
              ))
            }
          </div>
        </div>
      </div>

      {/* Top senders + receivers */}
      <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:20}}>
        {[
          { title:'Top Senders',   data: topSenders   },
          { title:'Top Receivers', data: topReceivers },
        ].map(({title,data})=>(
          <div key={title} style={{background:'var(--surface)',border:'1px solid var(--border)',borderRadius:12}}>
            <div style={{padding:'16px 20px',borderBottom:'1px solid var(--border)',fontSize:15,fontWeight:600}}>{title}</div>
            <table style={{width:'100%',borderCollapse:'collapse'}}>
              <thead><tr>
                {['Account','Holder','Volume','Count'].map(h=><th key={h} style={th}>{h}</th>)}
              </tr></thead>
              <tbody>
                {!data || data.length === 0
                  ? <tr><td colSpan={4} style={{...td,textAlign:'center',color:'var(--muted)'}}>No data</td></tr>
                  : data.map((acct)=>(
                    <tr key={acct.accountNumber}>
                      <td style={td}><span style={{fontFamily:'monospace',fontSize:11,color:'var(--blue2)'}}>{acct.accountNumber}</span></td>
                      <td style={td}>{acct.holderName}</td>
                      <td style={{...td,fontWeight:700}}>{acct.formattedAmount}</td>
                      <td style={{...td,color:'var(--muted)'}}>{acct.count}</td>
                    </tr>
                  ))
                }
              </tbody>
            </table>
          </div>
        ))}
      </div>
    </div>
  )
}
