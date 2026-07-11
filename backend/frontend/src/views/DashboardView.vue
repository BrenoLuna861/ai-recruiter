<template>
  <div class="dashboard">
    <div class="page-header">
      <h1>Dashboard</h1>
      <p class="subtitle">Bem-vindo de volta, {{ auth.user?.name?.split(' ')[0] }}</p>
    </div>

    <!-- Candidate Stats -->
    <div v-if="auth.isCandidate" class="grid">
      <div class="card stat-card">
        <div class="stat-icon">◎</div>
        <div class="stat-label">Score Geral</div>
        <div class="stat-value">{{ bestScore ?? '—' }}</div>
        <div class="stat-sub">do seu melhor currículo</div>
      </div>
      <div class="card stat-card">
        <div class="stat-icon">▤</div>
        <div class="stat-label">Currículos</div>
        <div class="stat-value">{{ resumes.length }}</div>
        <div class="stat-sub">analisados pelo agente</div>
      </div>
      <div class="card stat-card">
        <div class="stat-icon">◉</div>
        <div class="stat-label">Vagas abertas</div>
        <div class="stat-value">{{ jobs.length }}</div>
        <div class="stat-sub">disponíveis agora</div>
      </div>
    </div>

    <!-- Recruiter Stats -->
    <div v-if="auth.isRecruiter" class="grid">
      <div class="card stat-card">
        <div class="stat-icon">◉</div>
        <div class="stat-label">Vagas ativas</div>
        <div class="stat-value">{{ myJobs.length }}</div>
        <div class="stat-sub">publicadas por você</div>
      </div>
    </div>

    <!-- Quick actions -->
    <div class="section-title">Ações rápidas</div>
    <div class="actions">
      <RouterLink v-if="auth.isCandidate" to="/resume" class="action-card">
        <span class="action-icon">▤</span>
        <div class="action-body">
          <span class="action-label">Analisar Currículo</span>
          <span class="action-sub">Upload e análise com IA</span>
        </div>
        <span class="action-arrow">→</span>
      </RouterLink>
      <RouterLink to="/chat" class="action-card">
        <span class="action-icon">◈</span>
        <div class="action-body">
          <span class="action-label">Agente Aria</span>
          <span class="action-sub">Converse com o headhunter IA</span>
        </div>
        <span class="action-arrow">→</span>
      </RouterLink>
      <RouterLink to="/jobs" class="action-card">
        <span class="action-icon">◉</span>
        <div class="action-body">
          <span class="action-label">Explorar Vagas</span>
          <span class="action-sub">{{ jobs.length }} oportunidades</span>
        </div>
        <span class="action-arrow">→</span>
      </RouterLink>
      <RouterLink v-if="auth.isRecruiter" to="/recruiter" class="action-card">
        <span class="action-icon">◎</span>
        <div class="action-body">
          <span class="action-label">Painel Recrutador</span>
          <span class="action-sub">Gerencie vagas e candidatos</span>
        </div>
        <span class="action-arrow">→</span>
      </RouterLink>
    </div>

    <!-- Recent resumes -->
    <template v-if="auth.isCandidate && resumes.length">
      <div class="section-title">Currículos recentes</div>
      <div class="resume-list">
        <div v-for="r in resumes.slice(0,3)" :key="r.id" class="resume-row card">
          <div class="resume-info">
            <div class="resume-title">{{ r.title }}</div>
            <div class="resume-meta">{{ r.fileType?.split('/')[1]?.toUpperCase() }} · {{ formatDate(r.createdAt) }}</div>
          </div>
          <span class="score-pill" :class="scoreClass(r.overallScore)">{{ r.overallScore ?? '—' }}</span>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { resumeApi, jobApi } from '@/services/api'

const auth = useAuthStore()
const resumes = ref<any[]>([])
const jobs = ref<any[]>([])
const myJobs = ref<any[]>([])

const bestScore = computed(() => {
  if (!resumes.value.length) return null
  return Math.max(...resumes.value.map(r => r.overallScore || 0))
})

onMounted(async () => {
  try { jobs.value = (await jobApi.list()).data } catch {}
  if (auth.isCandidate) {
    try { resumes.value = (await resumeApi.list()).data } catch {}
  }
  if (auth.isRecruiter) {
    try { myJobs.value = (await jobApi.myJobs()).data } catch {}
  }
})

function formatDate(d: string) {
  return new Date(d).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' })
}
function scoreClass(score: number) {
  if (!score) return 'low'
  if (score >= 70) return 'high'
  if (score >= 40) return 'medium'
  return 'low'
}
</script>

<style scoped>
.dashboard { min-height: calc(100vh - 80px); }

/* Stats */
.grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; margin-bottom: 40px; }
.stat-card { text-align: center; padding: 32px 20px; display: flex; flex-direction: column; align-items: center; gap: 6px; }
.stat-icon { font-size: 28px; color: var(--accent); margin-bottom: 4px; }
.stat-label { font-size: 10px; letter-spacing: 0.14em; text-transform: uppercase; color: var(--text-muted); }
.stat-value { font-family: var(--font-mono); font-size: 3rem; font-weight: 700; color: var(--accent); line-height: 1; }
.stat-sub { font-size: var(--text-xs); color: var(--text-muted); }

/* Section title */
.section-title { font-size: 10px; letter-spacing: 0.14em; text-transform: uppercase; color: var(--text-muted); margin: 0 0 16px; }

/* Actions */
.actions { display: flex; flex-direction: column; gap: 10px; margin-bottom: 40px; }
.action-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--bg-2);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  text-decoration: none;
  color: var(--text);
  transition: all 0.15s var(--ease);
}
.action-card:hover { border-color: var(--accent); transform: translateX(4px); }
.action-icon { font-size: 24px; color: var(--accent); flex-shrink: 0; width: 32px; text-align: center; }
.action-body { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.action-label { font-weight: 600; font-size: var(--text-base); }
.action-sub { font-size: var(--text-sm); color: var(--text-muted); }
.action-arrow { font-size: 18px; color: var(--text-muted); transition: color 0.15s, transform 0.15s; }
.action-card:hover .action-arrow { color: var(--accent); transform: translateX(4px); }

/* Resume list */
.resume-list { display: flex; flex-direction: column; gap: 10px; }
.resume-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 18px 24px; }
.resume-title { font-size: var(--text-base); font-weight: 500; }
.resume-meta { font-size: var(--text-xs); color: var(--text-muted); margin-top: 3px; }

.score-pill {
  font-family: var(--font-mono); font-size: var(--text-sm); font-weight: 600;
  padding: 4px 12px; border-radius: 4px; flex-shrink: 0;
}
.score-pill.high   { background: rgba(110,231,183,.12); color: var(--accent); }
.score-pill.medium { background: rgba(251,191,36,.1);   color: var(--warning); }
.score-pill.low    { background: rgba(248,113,113,.1);  color: var(--danger); }

/* Responsive */
@media (max-width: 600px) {
  .stat-value { font-size: 2.2rem; }
  .action-card { padding: 16px 18px; }
  .action-label { font-size: var(--text-sm); }
}
</style>