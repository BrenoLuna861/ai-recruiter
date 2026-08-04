import { ref, computed } from 'vue'

type Theme = 'dark' | 'light'
const STORAGE_KEY = 'theme'

/**
 * Estado no escopo do modulo (fora do composable) = singleton.
 * Todos os componentes que chamarem useTheme() compartilham o MESMO ref,
 * entao o botao da sidebar e o da tela de login ficam sempre em sincronia.
 */
const theme = ref<Theme>('dark')
let initialized = false

function apply(value: Theme) {
  // Escuro e o padrao do :root, entao o tema claro e um atributo adicional.
  if (value === 'light') {
    document.documentElement.setAttribute('data-theme', 'light')
  } else {
    document.documentElement.removeAttribute('data-theme')
  }
}

function detectInitial(): Theme {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved === 'light' || saved === 'dark') return saved
  // Sem preferencia salva: respeita a config do sistema operacional.
  return window.matchMedia?.('(prefers-color-scheme: light)').matches ? 'light' : 'dark'
}

export function useTheme() {
  if (!initialized) {
    initialized = true
    theme.value = detectInitial()
    apply(theme.value)

    // Se o usuario nunca escolheu manualmente, acompanha o SO em tempo real.
    window.matchMedia?.('(prefers-color-scheme: light)')
      .addEventListener('change', (e) => {
        if (localStorage.getItem(STORAGE_KEY)) return
        theme.value = e.matches ? 'light' : 'dark'
        apply(theme.value)
      })
  }

  function setTheme(value: Theme) {
    theme.value = value
    apply(value)
    localStorage.setItem(STORAGE_KEY, value)
  }

  function toggleTheme() {
    setTheme(theme.value === 'dark' ? 'light' : 'dark')
  }

  return {
    theme,
    isDark: computed(() => theme.value === 'dark'),
    setTheme,
    toggleTheme,
  }
}

/**
 * Chamar no main.ts, ANTES do mount.
 * Evita o "flash" de tema escuro por um frame em quem usa o claro.
 */
export function initTheme() {
  apply(detectInitial())
}
