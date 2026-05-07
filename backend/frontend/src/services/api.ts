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
  health: () => api.get('/auth/health')
}

// Resumes
export const resumeApi = {
  analyze: (file: File, title?: string) => {
    const form = new FormData()
    form.append('file', file)
    if (title) form.append('title', title)
    return api.post('/resumes/analyze', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  list: () => api.get('/resumes'),
  get: (id: number) => api.get(`/resumes/${id}`)
}

// Chat
export const chatApi = {
  send: (message: string, sessionId?: string) =>
    api.post('/chat/message', { message, sessionId }),
  history: (sessionId: string) =>
    api.get(`/chat/history/${sessionId}`)
}

// Jobs
export const jobApi = {
  list: () => api.get('/jobs'),
  myJobs: () => api.get('/jobs/my-jobs'),
  create: (data: object) => api.post('/jobs', data),
  apply: (jobId: number, resumeId: number) =>
    api.post(`/jobs/${jobId}/apply?resumeId=${resumeId}`),
  ranking: (jobId: number) => api.get(`/jobs/${jobId}/ranking`)
}
