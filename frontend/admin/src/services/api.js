import axios from 'axios'

// In development: Vite proxy handles /api/v1 → localhost:8080
// In production:  VITE_API_URL = https://your-railway-app.railway.app
const api = axios.create({
  baseURL: `${import.meta.env.VITE_API_URL || ''}/api/v1`,
  headers: { 'Content-Type': 'application/json' }
})

export const saveToken  = (token) => localStorage.setItem('ad_token', token)
export const getToken   = ()      => localStorage.getItem('ad_token')
export const clearToken = ()      => localStorage.removeItem('ad_token')
export const saveUser   = (user)  => localStorage.setItem('ad_user', JSON.stringify(user))
export const getUser    = ()      => { try { return JSON.parse(localStorage.getItem('ad_user')) } catch { return null } }
export const clearUser  = ()      => localStorage.removeItem('ad_user')

api.interceptors.request.use(config => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) { clearToken(); clearUser(); window.location.reload() }
    return Promise.reject(err)
  }
)

export const login          = (body) => api.post('/auth/login', body).then(r => r.data)
export const forgotPassword = (body) => api.post('/auth/forgot-password', body).then(r => r.data)
export const resetPassword  = (body) => api.post('/auth/reset-password', body).then(r => r.data)
export const changePassword = (body) => api.post('/auth/change-password', body).then(r => r.data)

// Paginated: returns { content, page, size, totalElements, totalPages, first, last }
// page and size are 0-indexed. Example: getAllAccounts(0, 20) = first page, 20 items.
export const getAllAccounts      = (page=0, size=20, sort='createdAt,desc') =>
  api.get(`/accounts?page=${page}&size=${size}&sort=${sort}`).then(r => r.data)

export const getAllTransactions   = (page=0, size=20, sort='createdAt,desc') =>
  api.get(`/transactions?page=${page}&size=${size}&sort=${sort}`).then(r => r.data)

export const createAccount       = (body) => api.post('/accounts', body).then(r => r.data)
export const updateStatus        = (id,s) => api.patch(`/accounts/${id}/status?status=${s}`).then(r => r.data)
export const getLedgerStatement  = (id)   => api.get(`/accounts/${id}/ledger`).then(r => r.data)

// Server-side analytics — pre-aggregated by AnalyticsService, never sends all rows to frontend
export const getAnalytics        = ()     => api.get('/analytics').then(r => r.data)

// ── USER MANAGEMENT (ADMIN only) ──────────────────
export const getUsers       = (role)    => api.get(`/users${role ? `?role=${role}` : ''}`).then(r => r.data)
export const createUser     = (body)    => api.post('/users', body).then(r => r.data)
export const updateUser     = (id,body) => api.patch(`/users/${id}`, body).then(r => r.data)
export const setUserStatus  = (id, en)  => api.patch(`/users/${id}/status?enabled=${en}`).then(r => r.data)
export const deleteUser     = (id)      => api.delete(`/users/${id}`).then(r => r.data)
