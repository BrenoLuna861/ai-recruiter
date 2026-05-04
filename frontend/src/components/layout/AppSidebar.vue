<template>
  <aside class="sidebar">
    <!-- Brand -->
    <div class="sidebar-brand">
      <div class="brand-icon">✦</div>
      <div>
        <div class="brand-name">AI Recruiter</div>
        <div class="brand-role">{{ auth.user?.role }}</div>
      </div>
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

    <!-- User -->
    <div class="sidebar-user">
      <div class="user-avatar">{{ initials }}</div>
      <div class="user-info">
        <div class="user-name">{{ auth.user?.name }}</div>
        <div class="user-email">{{ auth.user?.email }}</div>
      </div>
      <button class="logout-btn" @click="handleLogout" title="Sair">⏻</button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { resumeApi } from '@/services/api'

const auth = useAuthStore()
const router = useRouter()
const resumeScore = ref<number | null>(null)

const initials = computed(() => {
  return auth.user?.name?.split(' ').map(n => n[0]).slice(0, 2).join('').toUpperCase() || '?'
})

onMounted(async () => {
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
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 24px 20px 20px;
  border-bottom: 1px solid var(--border);
}

.brand-icon {
  font-size: 18px;
  color: var(--accent);
  line-height: 1;
}

.brand-name {
  font-family: var(--font-display);
  font-size: 1rem;
  letter-spacing: -0.02em;
  line-height: 1.1;
}

.brand-role {
  font-size: 9px;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: var(--text-muted);
  margin-top: 2px;
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
  padding: 9px 10px;
  border-radius: var(--radius);
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--text-muted);
  text-decoration: none;
  letter-spacing: 0.01em;
  transition: all 0.15s var(--ease);
}

.nav-item:hover {
  color: var(--text);
  background: var(--bg-3);
}

.nav-item.active {
  color: var(--accent);
  background: var(--accent-dim);
}

.nav-icon {
  font-size: 14px;
  width: 16px;
  text-align: center;
}

/* Score */
.sidebar-score {
  padding: 20px;
  border-top: 1px solid var(--border);
  text-align: center;
}

.score-ring-label {
  font-size: 9px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--text-muted);
  margin-bottom: 12px;
}

.score-ring {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 70px;
  height: 70px;
}

.ring-svg { width: 100%; height: 100%; }

.ring-value {
  position: absolute;
  font-family: var(--font-mono);
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--accent);
}

.ring-sublabel {
  font-size: 10px;
  color: var(--text-muted);
  margin-top: 6px;
}

/* User */
.sidebar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 16px 20px;
  border-top: 1px solid var(--border);
}

.user-avatar {
  width: 32px; height: 32px;
  border-radius: 50%;
  background: var(--accent-dim);
  border: 1px solid var(--accent);
  color: var(--accent);
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-info { flex: 1; min-width: 0; }

.user-name {
  font-size: var(--text-sm);
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-email {
  font-size: 10px;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.logout-btn {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 14px;
  padding: 4px;
  border-radius: 4px;
  transition: color 0.15s;
}

.logout-btn:hover { color: var(--danger); }
</style>
