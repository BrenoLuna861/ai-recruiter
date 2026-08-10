import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/services/api'

export interface UserInfo {
  id: number
  name: string
  email: string
  role: 'CANDIDATE' | 'RECRUITER' | 'ADMIN'
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const user = ref<UserInfo | null>(
    JSON.parse(localStorage.getItem('user') || 'null')
  )

  const isAuthenticated = computed(() => !!token.value)
  const isCandidate = computed(() => user.value?.role === 'CANDIDATE')
  const isRecruiter = computed(() => user.value?.role === 'RECRUITER')

  function setAuth(data: { token: string; refreshToken: string; user: UserInfo }) {
    token.value = data.token
    user.value = data.user
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(data.user))
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  async function login(email: string, password: string) {
    const res = await authApi.login({ email, password })
    setAuth(res.data)
    return res.data
  }

  /*
    O cadastro NAO autentica mais: a conta so vira utilizavel apos a confirmacao
    por codigo. Guardar o token aqui tornaria o passo opcional na pratica, ja que
    a pessoa entraria no app sem nunca confirmar o e-mail.
  */
  async function register(name: string, email: string, password: string, role: string) {
    const res = await authApi.register({ name, email, password, role })
    return res.data
  }

  async function googleLogin(credential: string, role?: string) {
    const res = await authApi.google({ credential, role })
    setAuth(res.data)
    return res.data
  }

  return {
    token, user,
    isAuthenticated, isCandidate, isRecruiter,
    login, register, googleLogin, logout
  }
})
