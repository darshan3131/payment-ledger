import axios from 'axios'

// In development: Vite proxy handles /api/v1 → localhost:8080
// In production:  VITE_API_URL = https://your-railway-app.railway.app
const api = axios.create({
  baseURL: `${import.meta.env.VITE_API_URL || ''}/api/v1`,
  headers: { 'Content-Type': 'application/json' }
})

// ── AUTH TOKEN HELPERS ────────────────────────────
export const saveToken  = (token) => localStorage.setItem('pl_token', token)
export const getToken   = ()      => localStorage.getItem('pl_token')
export const clearToken = ()      => localStorage.removeItem('pl_token')
export const saveUser   = (user)  => localStorage.setItem('pl_user', JSON.stringify(user))
export const getUser    = ()      => { try { return JSON.parse(localStorage.getItem('pl_user')) } catch { return null } }
export const clearUser  = ()      => localStorage.removeItem('pl_user')

// REQUEST INTERCEPTOR — attaches JWT to every request
api.interceptors.request.use(config => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// RESPONSE INTERCEPTOR — catches 401, forces re-login
api.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) { clearToken(); clearUser(); window.location.reload() }
    return Promise.reject(err)
  }
)

// ── AUTH ──────────────────────────────────────────
export const login               = (body) => api.post('/auth/login', body).then(r => r.data)
export const register            = (body) => api.post('/auth/register', body).then(r => r.data)
export const registerSendOtp     = (phone) => api.post('/auth/register/send-otp', { phone }).then(r => r.data)
export const forgotPassword      = (body) => api.post('/auth/forgot-password', body).then(r => r.data)
export const resetPassword       = (body) => api.post('/auth/reset-password', body).then(r => r.data)
export const changePassword      = (body) => api.post('/auth/change-password', body).then(r => r.data)

// ── ACCOUNTS ──────────────────────────────────────
export const getAccountByNumber = (accountNumber) =>
  api.get(`/accounts/number/${accountNumber}`).then(r => r.data)

export const getAccountById = (id) =>
  api.get(`/accounts/${id}`).then(r => r.data)

export const getMyAccounts = () =>
  api.get('/accounts/my').then(r => r.data)

// ── LEDGER ────────────────────────────────────────
export const getLedgerStatement = (accountId) =>
  api.get(`/accounts/${accountId}/ledger`).then(r => r.data)

// ── TRANSACTIONS ──────────────────────────────────
export const getTransactionsByAccount = (accountId) =>
  api.get(`/transactions/account/${accountId}`).then(r => r.data)

export const processTransaction    = (payload) => api.post('/transactions', payload).then(r => r.data)
export const requestTransferOtp    = (sourceAccountNumber) =>
  api.post('/transactions/request-otp', { sourceAccountNumber }).then(r => r.data)

export const getTransactionByRef = (referenceId) =>
  api.get(`/transactions/${referenceId}`).then(r => r.data)

// ── SUPPORT TICKETS ───────────────────────────────
export const createTicket  = (body) => api.post('/support', body).then(r => r.data)
export const getMyTickets  = (page=0, size=10) =>
  api.get(`/support/my?page=${page}&size=${size}&sort=createdAt,desc`).then(r => r.data)
