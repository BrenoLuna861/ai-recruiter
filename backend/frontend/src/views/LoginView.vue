<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-brand">
        <span class="brand-mark">✦</span>
        <span class="brand-text">AI Recruiter</span>
      </div>
      <h1>Entrar</h1>
      <p class="auth-sub">Acesse sua conta para continuar</p>

      <div v-if="error" class="error-box" style="margin-bottom:20px">{{ error }}</div>

      <form @submit.prevent="handleLogin" class="auth-form">
        <div class="field">
          <label class="label">Email</label>
          <input v-model="email" type="email" class="input" placeholder="seu@email.com" required />
        </div>
        <div class="field">
          <label class="label">Senha</label>
          <input v-model="password" type="password" class="input" placeholder="••••••••" required />
        </div>

        <button type="submit" class="btn btn-primary w-full" :disabled="loading">
          <span v-if="loading" class="spinner" style="width:16px;height:16px;border-width:2px"></span>
          <span>{{ loading ? 'Entrando...' : 'Entrar' }}</span>
        </button>
      </form>

      <p class="auth-footer">
        Não tem conta?
        <RouterLink to="/register">Criar conta</RouterLink>
      </p>

      <div class="demo-box">
        <p class="demo-label">Contas demo</p>
        <button class="demo-btn" @click="fillDemo('candidato@demo.com')">Candidato</button>
        <button class="demo-btn" @click="fillDemo('recrutador@demo.com')">Recrutador</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const email = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(email.value, password.value)
    router.push('/dashboard')
  } catch (e: any) {
    error.value = e.response?.data?.message || 'Email ou senha incorretos'
  } finally {
    loading.value = false
  }
}

function fillDemo(demoEmail: string) {
  email.value = demoEmail
  password.value = 'demo123'
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: var(--bg);
}

.auth-card {
  width: 100%;
  max-width: 400px;
  background: var(--bg-2);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 40px;
}

.auth-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 28px;
}

.brand-mark { color: var(--accent); font-size: 18px; }
.brand-text { font-family: var(--font-display); font-size: 1.1rem; letter-spacing: -0.02em; }

h1 { font-size: var(--text-3xl); margin-bottom: 6px; }

.auth-sub {
  font-size: var(--text-sm);
  color: var(--text-muted);
  margin-bottom: 28px;
}

.auth-form { display: flex; flex-direction: column; gap: 16px; }
.field { display: flex; flex-direction: column; }

.w-full { width: 100%; justify-content: center; padding: 12px; font-size: var(--text-sm); }

.auth-footer {
  text-align: center;
  font-size: var(--text-sm);
  color: var(--text-muted);
  margin-top: 20px;
}

.auth-footer a { color: var(--accent); text-decoration: none; }
.auth-footer a:hover { text-decoration: underline; }

.demo-box {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--border);
  text-align: center;
}

.demo-label {
  font-size: 10px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-muted);
  margin-bottom: 10px;
}

.demo-btn {
  background: var(--bg-3);
  border: 1px solid var(--border);
  color: var(--text-muted);
  font-size: var(--text-xs);
  font-family: var(--font-body);
  letter-spacing: 0.06em;
  text-transform: uppercase;
  padding: 6px 14px;
  border-radius: var(--radius);
  cursor: pointer;
  margin: 0 4px;
  transition: all 0.15s;
}
.demo-btn:hover { border-color: var(--accent); color: var(--accent); }
</style>
