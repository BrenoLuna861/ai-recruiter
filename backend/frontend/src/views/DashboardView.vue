<template>
  <div>
    <div class="page-header">
      <h1>Dashboard</h1>
      <p class="subtitle">Bem-vindo de volta, {{ auth.user?.name?.split(' ')[0] }}</p>
    </div>

    <!-- Candidate Dashboard -->
    <div v-if="auth.isCandidate" class="grid">
      <div class="card stat-card">
        <div class="stat-label">Score Geral</div>
        <div class="stat-value mono">{{ bestScore ?? '—' }}</div>
        <div class="stat-sub">do seu melhor currículo</div>
      </div>
      <div class="card stat-card">
        <div class="stat-label">Currículos</div>
        <div class="stat-value mono">{{ resumes.length }}</div>
        <div class="stat-sub">analisados pelo agente</div>
      </div>
      <div class="card stat-card">
        <div class="stat-label">Vagas abertas</div>
        <div class="stat-value mono">{{ jobs.length }}</div>
        <div class="stat-sub">disponíveis agora</div>
      </div>
    </div>

    <!-- Recruiter Dashboard -->
    <div v-if="auth.isRecruiter" class="grid">
      <div class="card stat-card">
        <div class="stat-label">Vagas ativas</div>
        <div class="stat-value mono">{{ myJobs.length }}</div>
        <div class="stat-sub">publicadas por você</div>
      </div>
    </div>

    <!-- Quick actions -->
    <div class="section-title">Ações rápidas</div>
    <div class="actions">
      <RouterLink v-if="auth.isCandidate" to="/resume" class="action-card">
        <span class="action-icon">▤</span>
        <span class="action-label">Analisar Currículo</span>
        <span class="action-sub">Upload e análise com IA</span>
      </RouterLink>
      <RouterLink to="/chat" class="action-card">
        <span class="action-icon">◈</span>
        <span class="action-label">Agente Alex</span>
        <span class="action-sub">Converse com o headhunter IA</span>
      </RouterLink>
      <RouterLink to="/jobs" class="action-card">
        <span class="action-icon">◉</span>
        <span class="action-label">Explorar Vagas</span>
        <span class="action-sub">{{ jobs.length }} oportunidades</span>
      </RouterLink>
      <RouterLink v-if="auth.isRecruiter" to="/recruiter" class="action-card">
        <span class="action-icon">◎</span>
        <span class="action-label">Painel Recrutador</span>
        <span class="action-sub">Gerencie vagas e candidatos</span>
      </RouterLink>
    </div>

    <!-- Recent resumes -->
    <div v-if="auth.isCandidate && resumes.length" class="section-title">Currículos recentes</div>
    <div v-if="auth.isCandidate && resumes.length" class="resume-list">
      <div v-for="r in resumes.slice(0,3)" :key="r.id" class="resume-row card">
        <div class="resume-info">
          <div class="resume-title">{{ r.title }}</div>
          <div class="resume-meta">{{ r.fileType?.split('/')[1]?.toUpperCase() }} · {{ formatDate(r.createdAt) }}</div>
        </div>
        <div class="resume-scores">
          <span class="score-pill" :class="scoreClass(r.overallScore)">{{ r.overallScore ?? '—' }}</span>
        </div>
      </div>
    </div>
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
.grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; margin-bottom: 36px; }
.stat-card { text-align: center; padding: 24px 16px; }
.stat-label { font-size: 10px; letter-spacing: 0.14em; text-transform: uppercase; color: var(--text-muted); margin-bottom: 8px; }
.stat-value { font-family: var(--font-mono); font-size: 2.5rem; font-weight: 600; color: var(--accent); line-height: 1; }
.stat-sub { font-size: var(--text-xs); color: var(--text-muted); margin-top: 6px; }
.mono { font-family: var(--font-mono); }

.section-title { font-size: 10px; letter-spacing: 0.14em; text-transform: uppercase; color: var(--text-muted); margin: 32px 0 14px; }

.actions { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; margin-bottom: 36px; }
.action-card {
  display: flex; flex-direction: column; gap: 4px;
  background: var(--bg-2); border: 1px solid var(--border);
  border-radius: var(--radius-lg); padding: 20px;
  text-decoration: none; color: var(--text);
  transition: all 0.15s var(--ease);
}
.action-card:hover { border-color: var(--accent); transform: translateY(-2px); }
.action-icon { font-size: 20px; color: var(--accent); }
.action-label { font-weight: 600; font-size: var(--text-sm); margin-top: 8px; }
.action-sub { font-size: var(--text-xs); color: var(--text-muted); }

.resume-list { display: flex; flex-direction: column; gap: 8px; }
.resume-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 16px 20px; }
.resume-title { font-size: var(--text-sm); font-weight: 500; }
.resume-meta { font-size: var(--text-xs); color: var(--text-muted); margin-top: 2px; }

.score-pill {
  font-family: var(--font-mono); font-size: var(--text-sm); font-weight: 600;
  padding: 4px 10px; border-radius: 4px;
}
.score-pill.high   { background: rgba(110,231,183,.12); color: var(--accent); }
.score-pill.medium { background: rgba(251,191,36,.1);   color: var(--warning); }
.score-pill.low    { background: rgba(248,113,113,.1);  color: var(--danger); }
</style>
