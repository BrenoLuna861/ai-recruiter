<template>
  <div class="google-wrap">
    <!-- O div abaixo é populado pelo Google Identity Services -->
    <div ref="gisContainer" class="gis-container" />

    <!-- Fallback visual enquanto o GIS carrega ou se não houver client ID configurado -->
    <button
      v-if="!ready"
      type="button"
      class="google-btn-fallback"
      :disabled="!clientId"
      @click="onFallbackClick"
    >
      <GoogleIcon />
      <span>{{ fallbackLabel }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, h, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
  mode: 'login' | 'register'
  role?: string
}>()

const emit = defineEmits<{
  (e: 'error', message: string): void
}>()

const router = useRouter()
const auth = useAuthStore()
const gisContainer = ref<HTMLDivElement | null>(null)
const ready = ref(false)

const clientId = (import.meta.env.VITE_GOOGLE_CLIENT_ID || '').trim()

const fallbackLabel = computed(() =>
  !clientId
    ? 'Google indisponível (configure VITE_GOOGLE_CLIENT_ID)'
    : props.mode === 'register'
      ? 'Continuar com Google'
      : 'Entrar com Google'
)

// Pequeno componente de ícone do Google (SVG inline)
const GoogleIcon = () =>
  h(
    'svg',
    { width: 18, height: 18, viewBox: '0 0 48 48', xmlns: 'http://www.w3.org/2000/svg' },
    [
      h('path', {
        fill: '#FFC107',
        d:
          'M43.6 20.5H42V20H24v8h11.3c-1.7 4.6-6 8-11.3 8-6.6 0-12-5.4-12-12s5.4-12 12-12c3 0 5.8 1.1 7.9 3l5.7-5.7C34.5 6 29.5 4 24 4 12.9 4 4 12.9 4 24s8.9 20 20 20c11 0 19.6-8 19.6-20 0-1.2-.1-2.3-.4-3.5z'
      }),
      h('path', {
        fill: '#FF3D00',
        d:
          'M6.3 14.7l6.6 4.8C14.7 16 19 13 24 13c3 0 5.8 1.1 7.9 3l5.7-5.7C34.5 6 29.5 4 24 4 16.3 4 9.6 8.3 6.3 14.7z'
      }),
      h('path', {
        fill: '#4CAF50',
        d:
          'M24 44c5.4 0 10.3-2 14-5.4l-6.4-5.4c-2 1.4-4.6 2.3-7.6 2.3-5.3 0-9.7-3.4-11.3-8l-6.6 5.1C9.4 39.7 16.1 44 24 44z'
      }),
      h('path', {
        fill: '#1976D2',
        d:
          'M43.6 20.5H42V20H24v8h11.3c-.8 2.2-2.2 4.1-4 5.4l6.4 5.4C40.7 36 44 30.5 44 24c0-1.2-.1-2.3-.4-3.5z'
      })
    ]
  )

// ===== Google Identity Services =====
declare global {
  interface Window {
    google?: any
  }
}

function loadGisScript(): Promise<void> {
  return new Promise((resolve, reject) => {
    if (window.google?.accounts?.id) return resolve()
    const existing = document.querySelector<HTMLScriptElement>(
      'script[src="https://accounts.google.com/gsi/client"]'
    )
    if (existing) {
      existing.addEventListener('load', () => resolve())
      existing.addEventListener('error', () => reject(new Error('Falha ao carregar Google Identity')))
      return
    }
    const s = document.createElement('script')
    s.src = 'https://accounts.google.com/gsi/client'
    s.async = true
    s.defer = true
    s.onload = () => resolve()
    s.onerror = () => reject(new Error('Falha ao carregar Google Identity'))
    document.head.appendChild(s)
  })
}

async function handleCredential(response: { credential?: string }) {
  if (!response.credential) {
    emit('error', 'Não foi possível obter credencial do Google.')
    return
  }
  try {
    await auth.googleLogin(response.credential, props.role)
    router.push('/dashboard')
  } catch (e: any) {
    emit('error', e.response?.data?.message || 'Falha ao autenticar com Google.')
  }
}

function onFallbackClick() {
  if (!clientId) {
    emit('error', 'Login com Google ainda não configurado neste ambiente.')
    return
  }
  // Tenta abrir o prompt do GIS
  window.google?.accounts?.id?.prompt()
}

onMounted(async () => {
  if (!clientId) return
  try {
    await loadGisScript()
    if (!window.google?.accounts?.id || !gisContainer.value) return

    window.google.accounts.id.initialize({
      client_id: clientId,
      callback: handleCredential,
      ux_mode: 'popup',
      auto_select: false
    })

    window.google.accounts.id.renderButton(gisContainer.value, {
      type: 'standard',
      theme: 'filled_black',
      size: 'large',
      shape: 'rectangular',
      text: props.mode === 'register' ? 'signup_with' : 'signin_with',
      logo_alignment: 'left',
      width: 320
    })

    ready.value = true
  } catch (e: any) {
    emit('error', e.message || 'Erro ao inicializar Google Sign-In')
  }
})
</script>

<style scoped>
.google-wrap {
  display: flex;
  justify-content: center;
}
.gis-container {
  display: flex;
  justify-content: center;
  width: 100%;
}
.google-btn-fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  padding: 11px 18px;
  background: var(--bg-3);
  border: 1px solid var(--border);
  color: var(--text);
  font-family: var(--font-body);
  font-size: var(--text-sm);
  font-weight: 500;
  border-radius: var(--radius);
  cursor: pointer;
  transition: border-color 0.15s var(--ease), background 0.15s var(--ease);
}
.google-btn-fallback:hover:not(:disabled) {
  border-color: var(--text-muted);
  background: var(--bg-2);
}
.google-btn-fallback:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
</style>
