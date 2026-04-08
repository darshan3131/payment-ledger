const fmt = dt => dt
  ? new Date(dt).toLocaleString('en-IN', { day:'2-digit', month:'short', year:'numeric', hour:'2-digit', minute:'2-digit' })
  : '—'

const th = {
  padding:'10px 16px', textAlign:'left', color:'var(--muted)', fontWeight:500,
  fontSize:11, textTransform:'uppercase', letterSpacing:'0.06em',
  borderBottom:'1px solid var(--border)', whiteSpace:'nowrap'
}
const td = { padding:'12px 16px', borderBottom:'1px solid var(--border)', fontSize:13 }

export default function LedgerTable({ entries, accountNumber }) {
  if (!entries || entries.length === 0) {
    return (
      <div style={{textAlign:'center',padding:48,color:'var(--muted)',fontSize:13}}>
        No entries yet. Make your first transfer to see it here.
      </div>
    )
  }

  const sorted   = [...entries].sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
  let running    = 0
  const withBal  = sorted.map(e => {
    running += e.entryType === 'CREDIT' ? e.amount : -e.amount
    return { ...e, runningBalance: running }
  }).reverse()

  return (
    <div style={{overflowX:'auto'}}>
      <table style={{width:'100%',borderCollapse:'collapse',fontSize:13}}>
        <thead>
          <tr>
            <th style={th}>Date</th>
            <th style={th}>Type</th>
            <th style={th}>Description / Counterparty</th>
            <th style={{...th,textAlign:'right'}}>Debit</th>
            <th style={{...th,textAlign:'right'}}>Credit</th>
            <th style={{...th,textAlign:'right'}}>Balance</th>
          </tr>
        </thead>
        <tbody>
          {withBal.map(entry => {
            const isDebit = entry.entryType === 'DEBIT'
            return (
              <tr key={entry.id}>
                <td style={{...td,color:'var(--muted)',fontSize:12}}>{fmt(entry.createdAt)}</td>
                <td style={td}>
                  <span style={{
                    display:'inline-block', padding:'2px 7px', borderRadius:4, fontSize:11, fontWeight:500,
                    background: isDebit ? 'rgba(239,68,68,0.1)' : 'rgba(34,197,94,0.1)',
                    color:      isDebit ? 'var(--red)' : 'var(--green)'
                  }}>
                    {entry.entryType}
                  </span>
                </td>
                <td style={td}>
                  <div>{entry.description || entry.transactionType}</div>
                  <div style={{fontSize:11,color:'var(--muted)',marginTop:2,fontFamily:'monospace'}}>
                    {isDebit ? 'To' : 'From'} {entry.counterpartyAccountNumber}
                    <span style={{opacity:0.5}}> · {entry.referenceId}</span>
                  </div>
                </td>
                <td style={{...td,textAlign:'right',color:'var(--red)',fontWeight:500}}>
                  {isDebit ? `−${entry.formattedAmount}` : ''}
                </td>
                <td style={{...td,textAlign:'right',color:'var(--green)',fontWeight:500}}>
                  {!isDebit ? `+${entry.formattedAmount}` : ''}
                </td>
                <td style={{...td,textAlign:'right',fontWeight:600,fontFamily:'monospace',fontSize:12}}>
                  {entry.currency === 'INR' ? '₹' : entry.currency === 'USD' ? '$' : '€'}
                  {(entry.runningBalance / 100).toLocaleString('en-IN', {minimumFractionDigits:2})}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
