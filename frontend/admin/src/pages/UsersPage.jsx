import { useState, useEffect } from 'react'
import { getUsers, createUser, updateUser, setUserStatus, deleteUser } from '../services/api'

const ROLES = ['BACKOFFICE', 'ADMIN', 'CUSTOMER']

const roleColor = {
  ADMIN:      { bg: 'rgba(217,119,6,0.12)',  text: '#d97706', border: 'rgba(217,119,6,0.25)'  },
  BACKOFFICE: { bg: 'rgba(34,197,94,0.10)',  text: '#16a34a', border: 'rgba(34,197,94,0.25)'  },
  CUSTOMER:   { bg: 'rgba(99,102,241,0.10)', text: '#6366f1', border: 'rgba(99,102,241,0.25)' },
}

export default function UsersPage() {
  const [users,     setUsers]     = useState([])
  const [loading,   setLoading]   = useState(true)
  const [error,     setError]     = useState('')
  const [roleFilter, setRoleFilter] = useState('ALL')
  const [showForm,  setShowForm]  = useState(false)
  const [saving,    setSaving]    = useState(false)
  const [formError, setFormError] = useState('')
  const [confirm,   setConfirm]   = useState(null) // { id, username }
  const [editUser,  setEditUser]  = useState(null) // user object being edited
  const [editForm,  setEditForm]  = useState({ phone: '', role: '' })
  const [editSaving, setEditSaving] = useState(false)
  const [editError,  setEditError]  = useState('')

  // Create-user form state
  const [form, setForm] = useState({ username: '', password: '', role: 'BACKOFFICE', phone: '' })

  useEffect(() => { load() }, [])

  async function load() {
    setLoading(true); setError('')
    try {
      const data = await getUsers()
      setUsers(data)
    } catch { setError('Failed to load users.') }
    finally { setLoading(false) }
  }

  async function handleCreate(e) {
    e.preventDefault()
    if (!form.username.trim() || !form.password.trim()) { setFormError('Username and password required'); return }
    if (form.password.length < 6) { setFormError('Password must be at least 6 characters'); return }
    if (form.phone && !/^\+[1-9]\d{7,14}$/.test(form.phone)) { setFormError('Phone must be +91XXXXXXXXXX format'); return }
    setSaving(true); setFormError('')
    try {
      const created = await createUser({
        username: form.username.trim(),
        password: form.password,
        role:     form.role,
        phone:    form.phone.trim() || null,
      })
      setUsers(prev => [created, ...prev])
      setShowForm(false)
      setForm({ username: '', password: '', role: 'BACKOFFICE', phone: '' })
    } catch (err) {
      setFormError(err.response?.data?.error || 'Failed to create user.')
    } finally { setSaving(false) }
  }

  async function handleToggle(user) {
    try {
      const updated = await setUserStatus(user.id, !user.enabled)
      setUsers(prev => prev.map(u => u.id === updated.id ? updated : u))
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to update status.')
    }
  }

  function openEdit(u) {
    setEditUser(u)
    setEditForm({ phone: u.phone || '', role: u.role })
    setEditError('')
  }

  async function handleEditSave(e) {
    e.preventDefault()
    if (editForm.phone && !/^\+[1-9]\d{7,14}$/.test(editForm.phone)) {
      setEditError('Phone must be +91XXXXXXXXXX format'); return
    }
    setEditSaving(true); setEditError('')
    try {
      const updated = await updateUser(editUser.id, {
        phone: editForm.phone.trim() || null,
        role:  editForm.role,
      })
      setUsers(prev => prev.map(u => u.id === updated.id ? updated : u))
      setEditUser(null)
    } catch (err) {
      setEditError(err.response?.data?.error || 'Failed to update user.')
    } finally { setEditSaving(false) }
  }

  async function handleDelete(id) {
    try {
      await deleteUser(id)
      setUsers(prev => prev.filter(u => u.id !== id))
      setConfirm(null)
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to delete user.')
      setConfirm(null)
    }
  }

  const filtered = roleFilter === 'ALL' ? users : users.filter(u => u.role === roleFilter)

  const inp = {
    width: '100%', background: 'var(--surface)', border: '1px solid var(--border)',
    borderRadius: 8, padding: '9px 12px', color: 'var(--text)', fontSize: 13,
    boxSizing: 'border-box'
  }

  return (
    <div>
      {/* Header */}
      <div style={{display:'flex',justifyContent:'space-between',alignItems:'flex-start',marginBottom:24}}>
        <div>
          <h1 style={{fontSize:18,fontWeight:600,marginBottom:4}}>Users</h1>
          <p style={{fontSize:13,color:'var(--muted)'}}>Manage staff and admin accounts. Customers self-register.</p>
        </div>
        <button onClick={() => { setShowForm(true); setFormError('') }}
          style={{display:'flex',alignItems:'center',gap:7,padding:'8px 14px',background:'#d97706',color:'white',border:'none',borderRadius:8,fontSize:13,fontWeight:500,cursor:'pointer'}}>
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          New user
        </button>
      </div>

      {/* Role filter pills */}
      <div style={{display:'flex',gap:6,marginBottom:20}}>
        {['ALL',...ROLES].map(r => (
          <button key={r} onClick={() => setRoleFilter(r)}
            style={{padding:'5px 12px',borderRadius:20,fontSize:12,fontWeight:500,cursor:'pointer',
              background: roleFilter===r ? '#d97706' : 'var(--surface)',
              color:      roleFilter===r ? 'white'    : 'var(--muted)',
              border:     roleFilter===r ? 'none'     : '1px solid var(--border)'}}>
            {r}
          </button>
        ))}
      </div>

      {/* Error banner */}
      {error && (
        <div style={{background:'rgba(239,68,68,0.08)',border:'1px solid rgba(239,68,68,0.2)',borderRadius:8,padding:'10px 14px',fontSize:13,color:'var(--red)',marginBottom:16}}>
          {error}
        </div>
      )}

      {/* Create user form */}
      {showForm && (
        <div style={{background:'var(--surface)',border:'1px solid var(--border)',borderRadius:10,padding:20,marginBottom:20}}>
          <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:16}}>
            <h3 style={{fontSize:14,fontWeight:600}}>Create user</h3>
            <button onClick={() => setShowForm(false)}
              style={{background:'none',border:'none',color:'var(--muted)',cursor:'pointer',fontSize:18,lineHeight:1}}>×</button>
          </div>
          {formError && (
            <div style={{background:'rgba(239,68,68,0.08)',border:'1px solid rgba(239,68,68,0.2)',borderRadius:8,padding:'8px 12px',fontSize:13,color:'var(--red)',marginBottom:12}}>
              {formError}
            </div>
          )}
          <form onSubmit={handleCreate}>
            <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:12,marginBottom:12}}>
              <div>
                <label style={{fontSize:11,color:'var(--muted)',display:'block',marginBottom:5}}>Username *</label>
                <input value={form.username} onChange={e=>setForm(f=>({...f,username:e.target.value}))} placeholder="e.g. john_ops" style={inp}/>
              </div>
              <div>
                <label style={{fontSize:11,color:'var(--muted)',display:'block',marginBottom:5}}>Password *</label>
                <input type="password" value={form.password} onChange={e=>setForm(f=>({...f,password:e.target.value}))} placeholder="min 6 chars" style={inp}/>
              </div>
              <div>
                <label style={{fontSize:11,color:'var(--muted)',display:'block',marginBottom:5}}>Role *</label>
                <select value={form.role} onChange={e=>setForm(f=>({...f,role:e.target.value}))}
                  style={{...inp,appearance:'none'}}>
                  {ROLES.map(r => <option key={r} value={r}>{r}</option>)}
                </select>
              </div>
              <div>
                <label style={{fontSize:11,color:'var(--muted)',display:'block',marginBottom:5}}>Phone <span style={{opacity:.6}}>(optional)</span></label>
                <input value={form.phone} onChange={e=>setForm(f=>({...f,phone:e.target.value}))} placeholder="+91XXXXXXXXXX" style={inp}/>
              </div>
              {form.role === 'CUSTOMER' && (
                <div style={{gridColumn:'span 2',padding:'8px 12px',background:'var(--surface2)',borderRadius:8,fontSize:12,color:'var(--muted)'}}>
                  ℹ️ Payment account is auto-linked when backoffice opens an account for this user.
                </div>
              )}
            </div>
            <div style={{display:'flex',gap:8,justifyContent:'flex-end'}}>
              <button type="button" onClick={() => setShowForm(false)}
                style={{padding:'8px 16px',background:'none',border:'1px solid var(--border)',borderRadius:8,color:'var(--muted)',fontSize:13,cursor:'pointer'}}>
                Cancel
              </button>
              <button type="submit" disabled={saving}
                style={{padding:'8px 16px',background:'#d97706',color:'white',border:'none',borderRadius:8,fontSize:13,fontWeight:500,opacity:saving?.7:1,cursor:'pointer'}}>
                {saving ? 'Creating…' : 'Create user'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Edit user modal */}
      {editUser && (
        <div style={{position:'fixed',inset:0,background:'rgba(0,0,0,0.5)',display:'flex',alignItems:'center',justifyContent:'center',zIndex:50}}>
          <div style={{background:'var(--surface)',borderRadius:12,padding:24,maxWidth:420,width:'90%',border:'1px solid var(--border)'}}>
            <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:16}}>
              <h3 style={{fontSize:15,fontWeight:600}}>Edit — {editUser.username}</h3>
              <button onClick={() => setEditUser(null)}
                style={{background:'none',border:'none',color:'var(--muted)',cursor:'pointer',fontSize:20,lineHeight:1}}>×</button>
            </div>
            {editError && (
              <div style={{background:'rgba(239,68,68,0.08)',border:'1px solid rgba(239,68,68,0.2)',borderRadius:8,padding:'8px 12px',fontSize:13,color:'var(--red)',marginBottom:12}}>
                {editError}
              </div>
            )}
            <form onSubmit={handleEditSave}>
              <div style={{display:'grid',gap:12,marginBottom:16}}>
                <div>
                  <label style={{fontSize:11,color:'var(--muted)',display:'block',marginBottom:5}}>Role</label>
                  <select value={editForm.role} onChange={e => setEditForm(f => ({...f, role: e.target.value}))}
                    style={{...inp, appearance:'none'}}>
                    {ROLES.map(r => <option key={r} value={r}>{r}</option>)}
                  </select>
                </div>
                <div>
                  <label style={{fontSize:11,color:'var(--muted)',display:'block',marginBottom:5}}>Phone <span style={{opacity:.6}}>(E.164 format)</span></label>
                  <input value={editForm.phone} onChange={e => setEditForm(f => ({...f, phone: e.target.value}))}
                    placeholder="+91XXXXXXXXXX" style={inp}/>
                </div>
                <div>
                  <label style={{fontSize:11,color:'var(--muted)',display:'block',marginBottom:5}}>
                    Account number <span style={{opacity:.6}}>(auto-linked by backoffice — read only)</span>
                  </label>
                  <div style={{...inp, fontFamily:'monospace', opacity:.6, cursor:'default', color: editUser?.accountNumber ? 'var(--text)' : 'var(--muted)'}}>
                    {editUser?.accountNumber || 'Not linked yet'}
                  </div>
                </div>
              </div>
              <div style={{display:'flex',gap:8,justifyContent:'flex-end'}}>
                <button type="button" onClick={() => setEditUser(null)}
                  style={{padding:'8px 16px',background:'none',border:'1px solid var(--border)',borderRadius:8,color:'var(--muted)',fontSize:13,cursor:'pointer'}}>
                  Cancel
                </button>
                <button type="submit" disabled={editSaving}
                  style={{padding:'8px 16px',background:'#d97706',color:'white',border:'none',borderRadius:8,fontSize:13,fontWeight:500,opacity:editSaving?.7:1,cursor:'pointer'}}>
                  {editSaving ? 'Saving…' : 'Save changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Confirm delete modal */}
      {confirm && (
        <div style={{position:'fixed',inset:0,background:'rgba(0,0,0,0.5)',display:'flex',alignItems:'center',justifyContent:'center',zIndex:50}}>
          <div style={{background:'var(--surface)',borderRadius:12,padding:24,maxWidth:360,width:'90%',border:'1px solid var(--border)'}}>
            <h3 style={{fontSize:15,fontWeight:600,marginBottom:8}}>Delete user?</h3>
            <p style={{fontSize:13,color:'var(--muted)',marginBottom:20}}>
              This permanently deletes <strong style={{color:'var(--text)'}}>{confirm.username}</strong>. This cannot be undone. Consider disabling instead.
            </p>
            <div style={{display:'flex',gap:8,justifyContent:'flex-end'}}>
              <button onClick={() => setConfirm(null)}
                style={{padding:'8px 16px',background:'none',border:'1px solid var(--border)',borderRadius:8,color:'var(--muted)',fontSize:13,cursor:'pointer'}}>
                Cancel
              </button>
              <button onClick={() => handleDelete(confirm.id)}
                style={{padding:'8px 16px',background:'var(--red)',color:'white',border:'none',borderRadius:8,fontSize:13,fontWeight:500,cursor:'pointer'}}>
                Delete
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Users table */}
      {loading ? (
        <div style={{textAlign:'center',padding:40,color:'var(--muted)',fontSize:13}}>Loading users…</div>
      ) : filtered.length === 0 ? (
        <div style={{textAlign:'center',padding:40,color:'var(--muted)',fontSize:13}}>
          No {roleFilter !== 'ALL' ? roleFilter : ''} users found.
        </div>
      ) : (
        <div style={{background:'var(--surface)',border:'1px solid var(--border)',borderRadius:10,overflow:'hidden'}}>
          <table style={{width:'100%',borderCollapse:'collapse'}}>
            <thead>
              <tr style={{borderBottom:'1px solid var(--border)'}}>
                {['Username','Role','Phone','Account','Status','Actions'].map(h => (
                  <th key={h} style={{padding:'10px 14px',textAlign:'left',fontSize:11,fontWeight:500,color:'var(--muted)',textTransform:'uppercase',letterSpacing:'0.04em'}}>
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {filtered.map((u, i) => {
                const rc = roleColor[u.role] || roleColor.CUSTOMER
                return (
                  <tr key={u.id} style={{borderBottom: i < filtered.length-1 ? '1px solid var(--border)' : 'none', opacity: u.enabled ? 1 : 0.5}}>
                    <td style={{padding:'12px 14px'}}>
                      <div style={{display:'flex',alignItems:'center',gap:9}}>
                        <div style={{width:28,height:28,borderRadius:'50%',background:'var(--surface2)',border:'1px solid var(--border)',display:'flex',alignItems:'center',justifyContent:'center',fontSize:11,fontWeight:600,color:'var(--muted)',flexShrink:0}}>
                          {u.username[0].toUpperCase()}
                        </div>
                        <div>
                          <div style={{fontSize:13,fontWeight:500}}>{u.username}</div>
                          <div style={{fontSize:11,color:'var(--muted)'}}>{u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '—'}</div>
                        </div>
                      </div>
                    </td>
                    <td style={{padding:'12px 14px'}}>
                      <span style={{display:'inline-block',padding:'2px 8px',borderRadius:4,fontSize:11,fontWeight:500,background:rc.bg,color:rc.text,border:`1px solid ${rc.border}`}}>
                        {u.role}
                      </span>
                    </td>
                    <td style={{padding:'12px 14px',fontSize:13,color:'var(--muted)'}}>{u.phone || '—'}</td>
                    <td style={{padding:'12px 14px',fontSize:13,color:'var(--muted)',fontFamily:'monospace'}}>{u.accountNumber || '—'}</td>
                    <td style={{padding:'12px 14px'}}>
                      <span style={{display:'inline-block',padding:'2px 8px',borderRadius:4,fontSize:11,fontWeight:500,
                        background: u.enabled ? 'rgba(34,197,94,0.1)' : 'rgba(239,68,68,0.08)',
                        color:      u.enabled ? '#16a34a'             : 'var(--red)',
                        border:     `1px solid ${u.enabled ? 'rgba(34,197,94,0.25)' : 'rgba(239,68,68,0.2)'}`}}>
                        {u.enabled ? 'Active' : 'Disabled'}
                      </span>
                    </td>
                    <td style={{padding:'12px 14px'}}>
                      <div style={{display:'flex',gap:6}}>
                        <button onClick={() => openEdit(u)} title="Edit"
                          style={{padding:'5px 10px',background:'var(--surface2)',border:'1px solid var(--border)',borderRadius:6,fontSize:12,color:'var(--muted)',cursor:'pointer'}}>
                          Edit
                        </button>
                        <button onClick={() => handleToggle(u)} title={u.enabled ? 'Disable' : 'Enable'}
                          style={{padding:'5px 10px',background:'var(--surface2)',border:'1px solid var(--border)',borderRadius:6,fontSize:12,color:'var(--muted)',cursor:'pointer'}}>
                          {u.enabled ? 'Disable' : 'Enable'}
                        </button>
                        <button onClick={() => setConfirm({id:u.id, username:u.username})} title="Delete"
                          style={{padding:'5px 8px',background:'rgba(239,68,68,0.08)',border:'1px solid rgba(239,68,68,0.2)',borderRadius:6,cursor:'pointer',color:'var(--red)'}}>
                          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/><path d="M10 11v6M14 11v6"/><path d="M9 6V4h6v2"/>
                          </svg>
                        </button>
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
          <div style={{padding:'10px 14px',borderTop:'1px solid var(--border)',fontSize:12,color:'var(--muted)'}}>
            {filtered.length} user{filtered.length !== 1 ? 's' : ''}
          </div>
        </div>
      )}
    </div>
  )
}
