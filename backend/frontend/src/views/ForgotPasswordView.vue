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
      <h1>Recuperar senha</h1>
      <p class="auth-sub">Informe seu email e enviaremos as instruções</p>

      <div v-if="message" class="info-box" style="margin-bottom:20px">{{ message }}</div>
      <div v-if="error" class="error-box" style="margin-bottom:20px">{{ error }}</div>

      <form @submit.prevent="handleSubmit" class="auth-form">
        <div class="field">
          <label class="label">Email</label>
          <input v-model="email" type="email" class="input" placeholder="seu@email.com" required />
        </div>
        <button type="submit" class="btn btn-primary w-full" :disabled="loading">
          <span v-if="loading" class="spinner" style="width:16px;height:16px;border-width:2px"></span>
          <span>{{ loading ? 'Enviando...' : 'Enviar instruções' }}</span>
        </button>
      </form>

      <p class="auth-footer">
        Lembrou a senha?
        <RouterLink to="/login">Entrar</RouterLink>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { authApi } from '@/services/api'

const email = ref('')
const message = ref('')
const error = ref('')
const loading = ref(false)

async function handleSubmit() {
  message.value = ''
  error.value = ''
  loading.value = true
  try {
    const res = await authApi.forgotPassword({ email: email.value })
    message.value = res.data?.message || 'Se o email existir, voce recebera as instrucoes.'
  } catch (e: any) {
    error.value = e.response?.data?.error || e.response?.data?.message || 'Nao foi possivel processar o pedido.'
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
@media (max-width: 480px) {
  .auth-page { padding: 12px; padding-top: 70px; }
  .auth-card { padding: 28px 22px; }
  .back-btn { top: 14px; left: 14px; }
  .back-label { display: none; }
  .back-btn { padding: 8px 10px; }
}
</style>
