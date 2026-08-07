import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 60000,
  headers: { 'Content-Type': 'application/json' }
})

// Attach JWT token on every request
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Handle 401 globally
api.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export default api

// Auth
export const authApi = {
  register: (data: { name: string; email: string; password: string; role?: string }) =>
    api.post('/auth/register', data),
  login: (data: { email: string; password: string }) =>
    api.post('/auth/login', data),
  google: (data: { credential: string; role?: string }) =>
    api.post('/auth/google', data),
  forgotPassword: (data: { email: string }) =>
    api.post('/auth/forgot-password', data),
  resetPassword: (data: { token: string; newPassword: string }) =>
    api.post('/auth/reset-password', data),
  validateResetToken: (token: string) =>
    api.get('/auth/reset-password/validate', { params: { token } }),
  health: () => api.get('/auth/health')
}

// Resumes
export const resumeApi = {
  analyze: (file: File, title?: string) => {
    const form = new FormData()
    form.append('file', file)
    if (title) form.append('title', title)
    return api.post('/resumes/analyze', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000 // 2 min — AI analysis can take a while
    })
  },
  list: () => api.get('/resumes'),
  get: (id: number) => api.get(`/resumes/${id}`),
  /** Reescrita do currículo. O texto sai do banco — só enviamos o id. */
  improve: (id: number) => api.post(`/resumes/${id}/improve`, {}, { timeout: 120000 })
}

// Chat
export const chatApi = {
  send: (message: string, sessionId?: string) =>
    api.post('/chat/message', { message, sessionId }),
  history: (sessionId: string) =>
    api.get(`/chat/history/${sessionId}`)
}

// Admin
export const adminApi = {
  listUsers: () => api.get('/admin/users'),
  updateRole: (id: number, role: string) =>
    api.patch(`/admin/users/${id}/role`, { role }),
  updateStatus: (id: number, active: boolean) =>
    api.patch(`/admin/users/${id}/status`, { active })
}

// Jobs
export const jobApi = {
  list: () => api.get('/jobs'),
  myJobs: () => api.get('/jobs/my-jobs'),
  create: (data: object) => api.post('/jobs', data),
  apply: (jobId: number, resumeId: number) =>
    api.post(`/jobs/${jobId}/apply?resumeId=${resumeId}`),
  ranking: (jobId: number) => api.get(`/jobs/${jobId}/ranking`),
  /** Vagas de fontes externas (Adzuna/Remotive). O backend intermedia e faz cache. */
  external: (params: { q?: string; local?: string; remotas?: boolean; pagina?: number }) =>
    api.get('/jobs/external', { params })
}
