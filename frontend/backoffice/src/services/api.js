import axios from 'axios'

// In development: Vite proxy handles /api/v1 → localhost:8080
// In production:  VITE_API_URL = https://your-railway-app.railway.app
const api = axios.create({
  baseURL: `${import.meta.env.VITE_API_URL || ''}/api/v1`,
  headers: { 'Content-Type': 'application/json' }
})

export const saveToken  = (token) => localStorage.setItem('bo_token', token)
export const getToken   = ()      => localStorage.getItem('bo_token')
export const clearToken = ()      => localStorage.removeItem('bo_token')
export const saveUser   = (user)  => localStorage.setItem('bo_user', JSON.stringify(user))
export const getUser    = ()      => { try { return JSON.parse(localStorage.getItem('bo_user')) } catch { return null } }
export const clearUser  = ()      => localStorage.removeItem('bo_user')

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
export const getAllAccounts       = (page=0, size=20, sort='createdAt,desc') =>
  api.get(`/accounts?page=${page}&size=${size}&sort=${sort}`).then(r => r.data)

export const getAvailableCustomers = (unlinked = false) =>
  api.get(`/accounts/available-customers?unlinked=${unlinked}`).then(r => r.data)
export const getAccountById        = (id)        => api.get(`/accounts/${id}`).then(r => r.data)
export const getAccountByNumber    = (num)       => api.get(`/accounts/number/${num}`).then(r => r.data)
export const createAccount         = (body)      => api.post('/accounts', body).then(r => r.data)
export const updateStatus          = (id,status) => api.patch(`/accounts/${id}/status?status=${status}`).then(r => r.data)
export const deposit               = (body)      => api.post('/transactions/deposit', body).then(r => r.data)
export const withdraw              = (body)      => api.post('/transactions/withdraw', body).then(r => r.data)

export const getAllTransactions        = (page=0, size=20, sort='createdAt,desc') =>
  api.get(`/transactions?page=${page}&size=${size}&sort=${sort}`).then(r => r.data)

// Paginated account transactions: returns PagedResponse
export const getTransactionsByAccount = (id, page=0, size=10) =>
  api.get(`/transactions/account/${id}?page=${page}&size=${size}&sort=createdAt,desc`).then(r => r.data)

export const getTransactionByRef      = (ref) => api.get(`/transactions/${ref}`).then(r => r.data)

export const getLedgerStatement      = (id)   => api.get(`/accounts/${id}/ledger`).then(r => r.data)
export const getLedgerForTransaction = (txId) => api.get(`/transactions/${txId}/ledger`).then(r => r.data)
export const reverseTransaction      = (ref, reason) => api.post(`/transactions/${ref}/reverse`, { reason }).then(r => r.data)

// Support Tickets
export const getAllTickets   = (page=0, size=20, status='') =>
  api.get(`/support?page=${page}&size=${size}&sort=createdAt,desc${status ? '&status='+status : ''}`).then(r => r.data)
export const updateTicket   = (id, body) => api.patch(`/support/${id}`, body).then(r => r.data)
