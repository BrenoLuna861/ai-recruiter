<template>
  <div class="auth-page">
    <RouterLink to="/" class="back-btn" aria-label="Voltar para o início">
      <span class="back-arrow">←</span>
      <span class="back-label">Início</span>
    </RouterLink>

    <div class="auth-card">
      <div class="auth-brand">
        <RouterLink to="/" class="brand-link">
          <span class="brand-mark">✦</span>
          <span class="brand-text">AI Recruiter</span>
        </RouterLink>
      </div>
      <h1>Criar conta</h1>
      <p class="auth-sub">Junte-se à plataforma de recrutamento inteligente</p>

      <div v-if="error" class="error-box" style="margin-bottom:20px">{{ error }}</div>

      <!-- Role selection (also used for Google sign-up) -->
      <div class="field" style="margin-bottom:16px">
        <label class="label">Eu sou</label>
        <select v-model="form.role" class="input">
          <option value="CANDIDATE">Candidato — Procuro emprego</option>
          <option value="RECRUITER">Recrutador — Procuro talentos</option>
        </select>
      </div>

      <!-- Google sign-up -->
      <GoogleAuthButton mode="register" :role="form.role" @error="onGoogleError" />

      <div class="divider-row">
        <span class="divider-line"></span>
        <span class="divider-label">ou com email</span>
        <span class="divider-line"></span>
      </div>

      <form @submit.prevent="handleRegister" class="auth-form">
        <div class="field">
          <label class="label">Nome completo</label>
          <input v-model="form.name" type="text" class="input" placeholder="Seu Nome" required />
        </div>
        <div class="field">
          <label class="label">Email</label>
          <input v-model="form.email" type="email" class="input" placeholder="seu@email.com" required />
        </div>
        <div class="field">
          <label class="label">Senha</label>
          <input v-model="form.password" type="password" class="input" placeholder="mínimo 8 caracteres" required minlength="8" />
        </div>

        <button type="submit" class="btn btn-primary w-full" :disabled="loading">
          <span v-if="loading" class="spinner" style="width:16px;height:16px;border-width:2px"></span>
          <span>{{ loading ? 'Criando...' : 'Criar conta' }}</span>
        </button>
      </form>

      <p class="auth-footer">
        Já tem conta?
        <RouterLink to="/login">Entrar</RouterLink>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import GoogleAuthButton from '@/components/auth/GoogleAuthButton.vue'

const router = useRouter()
const auth = useAuthStore()
const error = ref('')
const loading = ref(false)
const form = reactive({ name: '', email: '', password: '', role: 'CANDIDATE' })

async function handleRegister() {
  error.value = ''
  loading.value = true
  try {
    await auth.register(form.name, form.email, form.password, form.role)
    router.push('/dashboard')
  } catch (e: any) {
    error.value = e.response?.data?.message || 'Erro ao criar conta'
  } finally {
    loading.value = false
  }
}

function onGoogleError(message: string) {
  error.value = message
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  width: 100%;
  max-width: 100vw;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: var(--bg);
  position: relative;
  overflow-x: hidden;
}
.back-btn {
  position: absolute;
  top: 20px; left: 20px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: var(--text-sm);
  color: var(--text-muted);
  text-decoration: none;
  padding: 8px 14px;
  border-radius: var(--radius);
  border: 1px solid var(--border);
  background: var(--bg-2);
  transition: color 0.15s, border-color 0.15s;
}
.back-btn:hover { color: var(--accent); border-color: var(--accent); }
.back-arrow { font-size: 16px; line-height: 1; }

.auth-card {
  width: 100%;
  max-width: 400px;
  background: var(--bg-2);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 40px;
}
.auth-brand { display: flex; align-items: center; gap: 8px; margin-bottom: 28px; }
.brand-link { display: flex; align-items: center; gap: 8px; text-decoration: none; color: inherit; }
.brand-mark { color: var(--accent); font-size: 18px; }
.brand-text { font-family: var(--font-display); font-size: 1.1rem; letter-spacing: -0.02em; }
h1 { font-size: var(--text-3xl); margin-bottom: 6px; }
.auth-sub { font-size: var(--text-sm); color: var(--text-muted); margin-bottom: 28px; }
.auth-form { display: flex; flex-direction: column; gap: 16px; }
.field { display: flex; flex-direction: column; }
.w-full { width: 100%; justify-content: center; padding: 12px; }
.auth-footer { text-align: center; font-size: var(--text-sm); color: var(--text-muted); margin-top: 20px; }
.auth-footer a { color: var(--accent); text-decoration: none; }

.divider-row { display: flex; align-items: center; gap: 12px; margin: 22px 0; }
.divider-line { flex: 1; height: 1px; background: var(--border); }
.divider-label {
  font-size: 10px; letter-spacing: 0.14em; text-transform: uppercase; color: var(--text-muted);
}

@media (max-width: 480px) {
  .auth-page { padding: 12px; padding-top: 70px; }
  .auth-card { padding: 28px 22px; }
  .back-btn { top: 14px; left: 14px; padding: 8px 10px; }
  .back-label { display: none; }
}
</style>
