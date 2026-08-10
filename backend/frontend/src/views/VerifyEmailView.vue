<template>
  <div class="auth-page">
    <AuthBackdrop />
    <RouterLink to="/" class="back-btn" aria-label="Voltar para o início">
      <span class="back-arrow">←</span>
      <span class="back-label">Início</span>
    </RouterLink>

    <ThemeToggle variant="icon" floating />

    <div class="auth-card">
      <div class="auth-brand">
        <RouterLink to="/" class="brand-link">
          <BrandLogo variant="full" style="height: 34px;" />
        </RouterLink>
      </div>

      <h1>Confirme seu e-mail</h1>
      <p class="auth-sub">
        Enviamos um código de 6 dígitos para
        <strong v-if="email">{{ email }}</strong>
        <span v-else>o e-mail do seu cadastro</span>.
      </p>

      <div v-if="erro" class="error-box" style="margin-bottom:20px">{{ erro }}</div>
      <div v-if="aviso" class="info-box" style="margin-bottom:20px">{{ aviso }}</div>

      <form v-if="!confirmado" @submit.prevent="confirmar" class="auth-form">
        <div v-if="!email" class="field">
          <label class="label">E-mail</label>
          <input v-model="emailManual" type="email" class="input" placeholder="seu@email.com" required />
        </div>

        <div class="field">
          <label class="label">Código de confirmação</label>
          <input
            v-model="codigo"
            type="text"
            inputmode="numeric"
            autocomplete="one-time-code"
            class="input codigo-input"
            placeholder="000000"
            maxlength="6"
            required
            @input="apenasDigitos"
          />
        </div>

        <button type="submit" class="btn btn-primary w-full" :disabled="carregando || codigo.length < 6">
          <span v-if="carregando" class="spinner" style="width:16px;height:16px;border-width:2px"></span>
          <span>{{ carregando ? 'Confirmando...' : 'Confirmar conta' }}</span>
        </button>
      </form>

      <div v-else class="info-box">
        Conta confirmada. Redirecionando para o login...
      </div>

      <p v-if="!confirmado" class="auth-footer">
        Não recebeu?
        <button class="link-btn" @click="reenviar" :disabled="reenviando || segundosEspera > 0">
          {{ segundosEspera > 0 ? `Reenviar em ${segundosEspera}s` : (reenviando ? 'Enviando...' : 'Reenviar código') }}
        </button>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authApi } from '@/services/api'
import ThemeToggle from '@/components/ui/ThemeToggle.vue'
import BrandLogo from '@/components/ui/BrandLogo.vue'
import AuthBackdrop from '@/components/ui/AuthBackdrop.vue'

const route = useRoute()
const router = useRouter()

// O e-mail chega pela URL logo apos o cadastro. Se a pessoa abrir a tela direto,
// o campo aparece para ela digitar.
const emailManual = ref('')
const email = computed(() => (route.query.email as string) || emailManual.value)

const codigo = ref('')
const erro = ref('')
const aviso = ref('')
const carregando = ref(false)
const reenviando = ref(false)
const confirmado = ref(false)

// Espera entre reenvios: sem isso, o botao vira um gerador de e-mails.
const segundosEspera = ref(0)
let cronometro: number | undefined

function apenasDigitos() {
  codigo.value = codigo.value.replace(/\D/g, '').slice(0, 6)
}

async function confirmar() {
  erro.value = ''
  aviso.value = ''
  carregando.value = true
  try {
    await authApi.verifyEmail({ email: email.value, code: codigo.value })
    confirmado.value = true
    setTimeout(() => router.push('/login'), 1800)
  } catch (e: any) {
    erro.value = e.response?.data?.message || 'Não foi possível confirmar. Tente novamente.'
  } finally {
    carregando.value = false
  }
}

async function reenviar() {
  if (!email.value) { erro.value = 'Informe o e-mail do cadastro.'; return }
  erro.value = ''
  reenviando.value = true
  try {
    await authApi.resendCode({ email: email.value })
    aviso.value = 'Se houver cadastro pendente, um novo código foi enviado.'
    iniciarEspera(60)
  } catch {
    erro.value = 'Não foi possível reenviar agora. Tente em instantes.'
  } finally {
    reenviando.value = false
  }
}

function iniciarEspera(segundos: number) {
  segundosEspera.value = segundos
  cronometro = window.setInterval(() => {
    segundosEspera.value--
    if (segundosEspera.value <= 0 && cronometro) clearInterval(cronometro)
  }, 1000)
}

onUnmounted(() => { if (cronometro) clearInterval(cronometro) })
</script>

<style scoped>
.auth-page {
  min-height: 100vh; width: 100%; max-width: 100vw;
  display: flex; align-items: center; justify-content: center;
  padding: 24px; background: var(--bg); position: relative; overflow-x: hidden;
}
.auth-card {
  width: 100%; max-width: 400px; background: var(--bg-2);
  border: 1px solid var(--border); border-radius: var(--radius-lg); padding: 40px;
}
.auth-brand { margin-bottom: 24px; }
.brand-link { text-decoration: none; display: inline-block; }
.auth-sub { font-size: var(--text-sm); color: var(--text-muted); margin-bottom: 24px; line-height: 1.6; }
.auth-form { display: flex; flex-direction: column; gap: 16px; }
.w-full { width: 100%; justify-content: center; }

/* Espaçamento largo e fonte monoespaçada: o código é lido dígito a dígito. */
.codigo-input {
  font-family: var(--font-mono); font-size: 1.6rem;
  letter-spacing: 0.5em; text-align: center; padding: 14px 0 14px 0.5em;
}

.info-box {
  background: var(--accent-dim); border: 1px solid var(--accent);
  border-radius: var(--radius); color: var(--accent);
  padding: 12px 16px; font-size: var(--text-sm); line-height: 1.5;
}

.auth-footer { margin-top: 20px; text-align: center; font-size: var(--text-sm); color: var(--text-muted); }
.link-btn {
  background: none; border: none; color: var(--accent); cursor: pointer;
  font-size: var(--text-sm); padding: 0; text-decoration: underline;
}
.link-btn:disabled { color: var(--text-faint); cursor: default; text-decoration: none; }

.back-btn {
  position: absolute; top: 24px; left: 24px; display: inline-flex; align-items: center; gap: 8px;
  padding: 10px 14px; background: var(--bg-2); border: 1px solid var(--border);
  border-radius: var(--radius); color: var(--text-muted); text-decoration: none; font-size: var(--text-sm);
}
.back-btn:hover { border-color: var(--accent); color: var(--text); }

@media (max-width: 600px) {
  .auth-page { padding: 12px; padding-top: 70px; }
  .auth-card { padding: 28px 22px; }
  .back-btn { top: 14px; left: 14px; padding: 8px 10px; }
  .back-label { display: none; }
}
</style>
