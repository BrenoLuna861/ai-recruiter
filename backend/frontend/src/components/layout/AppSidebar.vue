<template>
  <aside class="sidebar" :class="{ 'is-open': open }">
    <!-- Brand -->
    <div class="sidebar-brand">
      <img src="@/assets/logo.png" alt="AI Recruiter" style="height: 28px; object-fit: contain;" />
      <div class="brand-role">{{ auth.user?.role }}</div>
      <button class="close-btn" @click="$emit('close')" aria-label="Fechar menu">×</button>
    </div>

    <!-- Nav -->
    <nav class="sidebar-nav">
      <RouterLink to="/dashboard" class="nav-item" active-class="active">
        <span class="nav-icon">▦</span> Dashboard
      </RouterLink>
      <RouterLink to="/chat" class="nav-item" active-class="active">
        <span class="nav-icon">◈</span> Agente IA
      </RouterLink>
      <RouterLink v-if="auth.isCandidate" to="/resume" class="nav-item" active-class="active">
        <span class="nav-icon">▤</span> Meu Currículo
      </RouterLink>
      <RouterLink to="/jobs" class="nav-item" active-class="active">
        <span class="nav-icon">◉</span> Vagas
      </RouterLink>
      <RouterLink v-if="auth.isRecruiter" to="/recruiter" class="nav-item" active-class="active">
        <span class="nav-icon">◎</span> Recrutamento
      </RouterLink>
      <RouterLink v-if="auth.user?.role === 'ADMIN'" to="/admin" class="nav-item" active-class="active">
        <span class="nav-icon">⚙</span> Admin
      </RouterLink>
    </nav>

    <!-- Score (candidate only) -->
    <div v-if="auth.isCandidate && resumeScore" class="sidebar-score">
      <div class="score-ring-label">SCORE DO CURRÍCULO</div>
      <div class="score-ring">
        <svg viewBox="0 0 60 60" class="ring-svg">
          <circle cx="30" cy="30" r="24" fill="none" stroke="var(--border-2)" stroke-width="3"/>
          <circle cx="30" cy="30" r="24" fill="none" stroke="var(--accent)" stroke-width="3"
            stroke-dasharray="150.8"
            :stroke-dashoffset="150.8 - (150.8 * resumeScore / 100)"
            stroke-linecap="round"
            transform="rotate(-90 30 30)"
            style="transition: stroke-dashoffset 1s ease"/>
        </svg>
        <span class="ring-value">{{ resumeScore }}</span>
      </div>
      <div class="ring-sublabel">de 100 pontos</div>
    </div>

    <!-- Theme Toggle -->
    <div class="theme-toggle-row">
      <span class="theme-label">{{ isDark ? '🌙 Escuro' : '☀️ Claro' }}</span>
      <button class="theme-toggle" @click="toggleTheme" :aria-label="isDark ? 'Mudar para tema claro' : 'Mudar para tema escuro'">
        <span class="toggle-thumb" :class="{ active: !isDark }"></span>
      </button>
    </div>

    <!-- User -->
    <div class="sidebar-user">
      <div class="user-avatar">{{ initials }}</div>
      <div class="user-info">
        <div class="user-name">{{ auth.user?.name }}</div>
        <div class="user-email">{{ auth.user?.email }}</div>
      </div>
      <button class="logout-btn" @click="handleLogout" title="Sair">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
          <polyline points="16 17 21 12 16 7"/>
          <line x1="21" y1="12" x2="9" y2="12"/>
  </svg>
</button>
    </div>

    <button class="sidebar-logout" @click="handleLogout">
      <span class="logout-icon">⏻</span>
      <span>Sair da conta</span>
    </button>
  </aside>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { resumeApi } from '@/services/api'

defineProps<{ open?: boolean }>()
defineEmits<{ (e: 'close'): void }>()

const auth = useAuthStore()
const router = useRouter()
const resumeScore = ref<number | null>(null)
const isDark = ref(true)

const initials = computed(() => {
  return auth.user?.name?.split(' ').map(n => n[0]).slice(0, 2).join('').toUpperCase() || '?'
})

onMounted(async () => {
  const saved = localStorage.getItem('theme')
  if (saved === 'light') {
    isDark.value = false
    document.documentElement.setAttribute('data-theme', 'light')
  } else {
    isDark.value = true
    document.documentElement.removeAttribute('data-theme')
  }

  if (auth.isCandidate) {
    try {
      const res = await resumeApi.list()
      if (res.data?.length) {
        const best = res.data.reduce((a: any, b: any) => (b.overallScore > (a.overallScore || 0) ? b : a), {})
        resumeScore.value = best.overallScore
      }
    } catch {}
  }
})

function toggleTheme() {
  isDark.value = !isDark.value
  if (isDark.value) {
    document.documentElement.removeAttribute('data-theme')
    localStorage.setItem('theme', 'dark')
  } else {
    document.documentElement.setAttribute('data-theme', 'light')
    localStorage.setItem('theme', 'light')
  }
}

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.sidebar {
  position: fixed;
  top: 0; left: 0;
  width: var(--sidebar-w);
  height: 100vh;
  background: var(--bg-2);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  padding: 0;
  z-index: 100;
  overflow-y: auto;
  transition: transform 0.25s var(--ease);
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 24px 20px 20px;
  border-bottom: 1px solid var(--border);
  position: relative;
}

.close-btn {
  display: none;
  position: absolute;
  top: 12px; right: 12px;
  background: transparent;
  border: none;
  color: var(--text-muted);
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
  width: 28px; height: 28px;
  border-radius: 4px;
}
.close-btn:hover { color: var(--text); background: var(--bg-3); }

.brand-role {
  font-size: 9px;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: var(--text-muted);
  margin-top: 2px;
  flex: 1;
}

.sidebar-nav {
  flex: 1;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 10px;
  border-radius: var(--radius);
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--text-muted);
  text-decoration: none;
  letter-spacing: 0.01em;
  transition: all 0.15s var(--ease);
}
.nav-item:hover { color: var(--text); background: var(--bg-3); }
.nav-item.active { color: var(--accent); background: var(--accent-dim); }
.nav-icon { font-size: 14px; width: 16px; text-align: center; }

/* Score */
.sidebar-score { padding: 20px; border-top: 1px solid var(--border); text-align: center; }
.score-ring-label { font-size: 9px; letter-spacing: 0.14em; text-transform: uppercase; color: var(--text-muted); margin-bottom: 12px; }
.score-ring { position: relative; display: inline-flex; align-items: center; justify-content: center; width: 70px; height: 70px; }
.ring-svg { width: 100%; height: 100%; }
.ring-value { position: absolute; font-family: var(--font-mono); font-size: 1.1rem; font-weight: 600; color: var(--accent); }
.ring-sublabel { font-size: 10px; color: var(--text-muted); margin-top: 6px; }

/* Theme Toggle */
.theme-toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-top: 1px solid var(--border);
}
.theme-label { font-size: var(--text-xs); color: var(--text-muted); }
.theme-toggle {
  width: 36px; height: 20px;
  background: var(--border-2);
  border: none;
  border-radius: 10px;
  cursor: pointer;
  position: relative;
  transition: background 0.2s;
  padding: 0;
}
.theme-toggle:hover { background: var(--accent-dim); }
.toggle-thumb {
  position: absolute;
  top: 3px; left: 3px;
  width: 14px; height: 14px;
  background: var(--text-muted);
  border-radius: 50%;
  transition: transform 0.2s, background 0.2s;
}
.toggle-thumb.active {
  transform: translateX(16px);
  background: var(--accent);
}

/* User */
.sidebar-user { display: flex; align-items: center; gap: 10px; padding: 16px 16px 20px; border-top: 1px solid var(--border); }
.user-avatar { width: 32px; height: 32px; border-radius: 50%; background: var(--accent-dim); border: 1px solid var(--accent); color: var(--accent); font-size: 11px; font-weight: 600; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.user-info { flex: 1; min-width: 0; }
.user-name { font-size: var(--text-sm); font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.user-email { font-size: 10px; color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.logout-btn { background: none; border: none; color: var(--text-muted); cursor: pointer; font-size: 14px; padding: 4px; border-radius: 4px; transition: color 0.15s; }
.logout-btn:hover { color: var(--danger); }

.sidebar-logout { display: flex; align-items: center; gap: 8px; width: calc(100% - 24px); margin: 0 12px 16px; padding: 10px 14px; background: none; border: 1px solid var(--border); border-radius: var(--radius); color: var(--text-muted); font-size: var(--text-sm); cursor: pointer; transition: color 0.15s, border-color 0.15s, background 0.15s; text-align: left; }
.sidebar-logout:hover { color: var(--danger); border-color: var(--danger); background: rgba(248, 113, 113, 0.05); }
.logout-icon { font-size: 14px; }

@media (max-width: 900px) {
  .sidebar { transform: translateX(-100%); width: 280px; box-shadow: 0 0 40px rgba(0, 0, 0, 0.5); }
  .sidebar.is-open { transform: translateX(0); }
  .close-btn { display: inline-flex; align-items: center; justify-content: center; }
  .nav-item { padding: 13px 12px; font-size: var(--text-base); }
}
</style>
