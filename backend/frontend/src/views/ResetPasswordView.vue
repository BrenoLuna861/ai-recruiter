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
      <h1>Nova senha</h1>
      <p class="auth-sub">Defina uma nova senha para sua conta</p>

      <div v-if="!token" class="error-box" style="margin-bottom:20px">
        Link inválido ou expirado. <RouterLink to="/forgot-password">Solicitar novo link</RouterLink>
      </div>
      <div v-if="error" class="error-box" style="margin-bottom:20px">{{ error }}</div>
      <div v-if="success" class="info-box" style="margin-bottom:20px">
        Senha redefinida com sucesso! Redirecionando para o login...
      </div>

      <form v-if="token && !success" @submit.prevent="handleSubmit" class="auth-form">
        <div class="field">
          <label class="label">Nova senha</label>
          <input v-model="password" type="password" class="input"
                 placeholder="mínimo 8 caracteres" required minlength="8" />
        </div>
        <div class="field">
          <label class="label">Confirmar senha</label>
          <input v-model="confirm" type="password" class="input"
                 placeholder="repita a senha" required minlength="8" />
        </div>
        <button type="submit" class="btn btn-primary w-full" :disabled="loading">
          <span v-if="loading" class="spinner" style="width:16px;height:16px;border-width:2px"></span>
          <span>{{ loading ? 'Salvando...' : 'Redefinir senha' }}</span>
        </button>
      </form>

      <p class="auth-footer">
        <RouterLink to="/login">Voltar ao login</RouterLink>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authApi } from '@/services/api'

const route = useRoute()
const router = useRouter()
const token = computed(() => (route.query.token as string) || '')

const password = ref('')
const confirm = ref('')
const error = ref('')
const success = ref(false)
const loading = ref(false)

async function handleSubmit() {
  error.value = ''
  if (password.value !== confirm.value) {
    error.value = 'As senhas não coincidem.'
    return
  }
  if (password.value.length < 8) {
    error.value = 'A senha precisa ter pelo menos 8 caracteres.'
    return
  }
  loading.value = true
  try {
    await authApi.resetPassword({ token: token.value, newPassword: password.value })
    success.value = true
    setTimeout(() => router.push('/login'), 1800)
  } catch (e: any) {
    error.value = e.response?.data?.error || e.response?.data?.message
      || 'Não foi possível redefinir a senha. O link pode ter expirado.'
  } finally {
    loading.value = false
  }
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
  position: relative;
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
.info-box {
  background: var(--accent-dim);
  border: 1px solid var(--accent);
  border-radius: var(--radius);
  color: var(--accent);
  padding: 12px 16px;
  font-size: var(--text-sm);
  line-height: 1.5;
}
.error-box a { color: var(--danger); text-decoration: underline; }
@media (max-width: 480px) {
  .auth-page { padding: 12px; padding-top: 70px; }
  .auth-card { padding: 28px 22px; }
  .back-btn { top: 14px; left: 14px; padding: 8px 10px; }
  .back-label { display: none; }
}
</style>
