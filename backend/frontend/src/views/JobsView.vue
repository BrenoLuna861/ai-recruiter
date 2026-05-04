<template>
  <div>
    <div class="page-header">
      <h1>Vagas</h1>
      <p class="subtitle">{{ jobs.length }} oportunidades disponíveis</p>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
    </div>

    <div v-else-if="!jobs.length" class="empty-state">
      <div class="empty-icon">◉</div>
      <p>Nenhuma vaga disponível no momento</p>
    </div>

    <div v-else class="jobs-list">
      <div v-for="job in jobs" :key="job.id" class="job-card card" @click="selected = selected?.id === job.id ? null : job">
        <div class="job-header">
          <div>
            <div class="job-title">{{ job.title }}</div>
            <div class="job-company">{{ job.company }}</div>
          </div>
          <div class="job-meta">
            <span class="tag">{{ jobTypeLabel(job.jobType) }}</span>
            <span class="tag" v-if="job.location">{{ job.location }}</span>
          </div>
        </div>

        <div v-if="job.salaryRange" class="job-salary">{{ job.salaryRange }}</div>

        <!-- Expanded -->
        <div v-if="selected?.id === job.id" class="job-details">
          <hr class="divider" />
          <div class="detail-section">
            <div class="detail-label">Descrição</div>
            <div class="detail-text">{{ job.description }}</div>
          </div>
          <div v-if="job.requirements" class="detail-section">
            <div class="detail-label">Requisitos</div>
            <div class="detail-text">{{ job.requirements }}</div>
          </div>

          <!-- Apply (candidate only) -->
          <div v-if="auth.isCandidate" class="apply-section">
            <div v-if="applyError" class="error-box" style="margin-bottom:12px">{{ applyError }}</div>
            <div v-if="applySuccess" style="color:var(--accent);font-size:var(--text-sm);margin-bottom:12px">✓ Candidatura enviada!</div>
            <div class="apply-row">
              <select v-model="selectedResume" class="input" style="flex:1">
                <option value="">Selecione seu currículo</option>
                <option v-for="r in resumes" :key="r.id" :value="r.id">{{ r.title }} ({{ r.overallScore ?? '?' }}/100)</option>
              </select>
              <button class="btn btn-primary" @click.stop="applyToJob(job.id)" :disabled="!selectedResume || applying">
                <span v-if="applying" class="spinner" style="width:14px;height:14px;border-width:2px;border-color:rgba(0,0,0,.2);border-top-color:#0a0a0b"></span>
                <span>{{ applying ? 'Enviando...' : 'Candidatar-se' }}</span>
              </button>
            </div>
          </div>
        </div>

        <div class="job-footer">
          <span class="job-date">{{ formatDate(job.createdAt) }}</span>
          <span class="expand-hint">{{ selected?.id === job.id ? '↑ Fechar' : '↓ Ver detalhes' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { jobApi, resumeApi } from '@/services/api'

const auth = useAuthStore()
const jobs = ref<any[]>([])
const resumes = ref<any[]>([])
const selected = ref<any>(null)
const selectedResume = ref('')
const loading = ref(true)
const applying = ref(false)
const applyError = ref('')
const applySuccess = ref(false)

onMounted(async () => {
  try { jobs.value = (await jobApi.list()).data } catch {} finally { loading.value = false }
  if (auth.isCandidate) {
    try { resumes.value = (await resumeApi.list()).data } catch {}
  }
})

async function applyToJob(jobId: number) {
  applyError.value = ''
  applySuccess.value = false
  applying.value = true
  try {
    await jobApi.apply(jobId, Number(selectedResume.value))
    applySuccess.value = true
  } catch (e: any) {
    applyError.value = e.response?.data?.message || 'Erro ao enviar candidatura'
  } finally {
    applying.value = false
  }
}

function jobTypeLabel(t: string) {
  const m: Record<string, string> = { FULL_TIME: 'Tempo integral', PART_TIME: 'Meio período', CONTRACT: 'Contrato', INTERNSHIP: 'Estágio', REMOTE: 'Remoto' }
  return m[t] || t
}
function formatDate(d: string) { return new Date(d).toLocaleDateString('pt-BR') }
</script>

<style scoped>
.loading-state { display: flex; justify-content: center; padding: 60px; }
.empty-state { text-align: center; padding: 60px; color: var(--text-muted); }
.empty-icon { font-size: 2.5rem; color: var(--border-2); margin-bottom: 16px; }

.jobs-list { display: flex; flex-direction: column; gap: 10px; }
.job-card { cursor: pointer; transition: all 0.15s; }
.job-card:hover { border-color: var(--border-2); }

.job-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; margin-bottom: 8px; }
.job-title { font-family: var(--font-display); font-size: var(--text-xl); letter-spacing: -0.02em; }
.job-company { font-size: var(--text-sm); color: var(--text-muted); margin-top: 2px; }
.job-meta { display: flex; gap: 6px; flex-wrap: wrap; justify-content: flex-end; }
.job-salary { font-family: var(--font-mono); font-size: var(--text-sm); color: var(--accent); margin-bottom: 8px; }

.job-details { margin-top: 8px; }
.detail-section { margin-bottom: 16px; }
.detail-label { font-size: 10px; letter-spacing: 0.12em; text-transform: uppercase; color: var(--text-muted); margin-bottom: 6px; }
.detail-text { font-size: var(--text-sm); line-height: 1.7; white-space: pre-wrap; }

.apply-section { margin-top: 20px; padding-top: 20px; border-top: 1px solid var(--border); }
.apply-row { display: flex; gap: 12px; align-items: center; }

.job-footer { display: flex; justify-content: space-between; margin-top: 12px; font-size: var(--text-xs); color: var(--text-muted); }
.expand-hint { letter-spacing: 0.04em; }
</style>
