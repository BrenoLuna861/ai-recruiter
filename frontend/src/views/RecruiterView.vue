<template>
  <div>
    <div class="page-header" style="display:flex;justify-content:space-between;align-items:flex-end">
      <div>
        <h1>Recrutamento</h1>
        <p class="subtitle">Gerencie vagas e visualize candidatos rankeados por IA</p>
      </div>
      <button class="btn btn-primary" @click="showForm = !showForm">{{ showForm ? '✕ Fechar' : '+ Nova Vaga' }}</button>
    </div>

    <!-- Create Job Form -->
    <div v-if="showForm" class="card create-form">
      <h3 style="font-family:var(--font-display);margin-bottom:20px">Nova Vaga</h3>
      <div v-if="formError" class="error-box" style="margin-bottom:16px">{{ formError }}</div>
      <div class="form-grid">
        <div class="field"><label class="label">Título da Vaga</label>
          <input v-model="form.title" type="text" class="input" placeholder="ex: Desenvolvedor Full Stack Senior" /></div>
        <div class="field"><label class="label">Empresa</label>
          <input v-model="form.company" type="text" class="input" placeholder="Nome da empresa" /></div>
        <div class="field"><label class="label">Localização</label>
          <input v-model="form.location" type="text" class="input" placeholder="ex: São Paulo / Remoto" /></div>
        <div class="field"><label class="label">Faixa Salarial</label>
          <input v-model="form.salaryRange" type="text" class="input" placeholder="ex: R$8.000 – R$12.000" /></div>
        <div class="field"><label class="label">Tipo</label>
          <select v-model="form.jobType" class="input">
            <option value="FULL_TIME">Tempo integral</option>
            <option value="PART_TIME">Meio período</option>
            <option value="CONTRACT">Contrato</option>
            <option value="INTERNSHIP">Estágio</option>
            <option value="REMOTE">Remoto</option>
          </select></div>
      </div>
      <div class="field" style="margin-top:12px"><label class="label">Descrição</label>
        <textarea v-model="form.description" class="input" rows="4" placeholder="Descreva a vaga em detalhes..." style="resize:vertical"></textarea></div>
      <div class="field" style="margin-top:12px"><label class="label">Requisitos</label>
        <textarea v-model="form.requirements" class="input" rows="3" placeholder="Liste os requisitos..." style="resize:vertical"></textarea></div>
      <button class="btn btn-primary" style="margin-top:16px" @click="createJob" :disabled="creating">
        <span v-if="creating" class="spinner" style="width:14px;height:14px;border-width:2px;border-color:rgba(0,0,0,.2);border-top-color:#0a0a0b"></span>
        <span>{{ creating ? 'Publicando...' : 'Publicar Vaga' }}</span>
      </button>
    </div>

    <!-- My Jobs -->
    <div class="section-title">Minhas Vagas ({{ myJobs.length }})</div>
    <div v-if="!myJobs.length" class="empty-card card" style="text-align:center;padding:40px;color:var(--text-muted)">
      Nenhuma vaga publicada ainda
    </div>

    <div v-for="job in myJobs" :key="job.id" class="job-block card">
      <div class="jb-header">
        <div>
          <div class="jb-title">{{ job.title }}</div>
          <div class="jb-sub">{{ job.company }} · {{ job.location || 'Sem localização' }}</div>
        </div>
        <button class="btn btn-ghost btn-sm" @click="toggleRanking(job.id)">
          {{ rankingOpen === job.id ? 'Fechar ranking' : '◈ Ver ranking IA' }}
        </button>
      </div>

      <!-- Ranking -->
      <div v-if="rankingOpen === job.id" class="ranking">
        <div v-if="loadingRanking" style="display:flex;justify-content:center;padding:24px"><div class="spinner"></div></div>
        <div v-else-if="!rankings[job.id]?.length" style="color:var(--text-muted);font-size:var(--text-sm);padding:16px">
          Nenhuma candidatura recebida ainda
        </div>
        <div v-else>
          <div class="rank-head">
            <span>#</span><span>Candidato</span><span>Match IA</span><span>Status</span>
          </div>
          <div v-for="(app, i) in rankings[job.id]" :key="app.id" class="rank-row">
            <span class="rank-pos" :class="i < 3 ? 'top' : ''">{{ i + 1 }}</span>
            <span class="rank-name">{{ app.candidate?.name || '—' }}</span>
            <span>
              <span class="score-pill" :class="scoreClass(app.matchScore)">{{ app.matchScore ?? '—' }}</span>
            </span>
            <span class="rank-status" :class="app.status.toLowerCase()">{{ app.status }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { jobApi } from '@/services/api'

const myJobs = ref<any[]>([])
const showForm = ref(false)
const creating = ref(false)
const formError = ref('')
const rankingOpen = ref<number | null>(null)
const rankings = ref<Record<number, any[]>>({})
const loadingRanking = ref(false)

const form = reactive({ title: '', company: '', location: '', salaryRange: '', jobType: 'FULL_TIME', description: '', requirements: '' })

onMounted(async () => {
  try { myJobs.value = (await jobApi.myJobs()).data } catch {}
})

async function createJob() {
  formError.value = ''
  if (!form.title || !form.company || !form.description) { formError.value = 'Preencha título, empresa e descrição'; return }
  creating.value = true
  try {
    const res = await jobApi.create(form)
    myJobs.value.unshift(res.data)
    showForm.value = false
    Object.keys(form).forEach(k => (form as any)[k] = k === 'jobType' ? 'FULL_TIME' : '')
  } catch (e: any) {
    formError.value = e.response?.data?.message || 'Erro ao criar vaga'
  } finally {
    creating.value = false
  }
}

async function toggleRanking(jobId: number) {
  if (rankingOpen.value === jobId) { rankingOpen.value = null; return }
  rankingOpen.value = jobId
  if (rankings.value[jobId]) return
  loadingRanking.value = true
  try {
    rankings.value[jobId] = (await jobApi.ranking(jobId)).data
  } catch {} finally {
    loadingRanking.value = false
  }
}

function scoreClass(s: number) { return !s ? 'low' : s >= 70 ? 'high' : s >= 40 ? 'medium' : 'low' }
</script>

<style scoped>
.section-title { font-size: 10px; letter-spacing: 0.14em; text-transform: uppercase; color: var(--text-muted); margin: 28px 0 14px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.field { display: flex; flex-direction: column; }
.create-form { margin-bottom: 24px; }

.job-block { margin-bottom: 10px; }
.jb-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.jb-title { font-family: var(--font-display); font-size: var(--text-xl); letter-spacing: -0.02em; }
.jb-sub { font-size: var(--text-sm); color: var(--text-muted); margin-top: 2px; }
.btn-sm { font-size: 11px; padding: 6px 12px; }

.ranking { margin-top: 20px; padding-top: 20px; border-top: 1px solid var(--border); }
.rank-head { display: grid; grid-template-columns: 36px 1fr 80px 100px; gap: 8px; padding: 8px 12px; font-size: 10px; letter-spacing: 0.1em; text-transform: uppercase; color: var(--text-muted); }
.rank-row { display: grid; grid-template-columns: 36px 1fr 80px 100px; gap: 8px; padding: 10px 12px; border-top: 1px solid var(--border); font-size: var(--text-sm); align-items: center; transition: background 0.1s; }
.rank-row:hover { background: var(--bg-3); }
.rank-pos { font-family: var(--font-mono); font-weight: 600; font-size: var(--text-sm); color: var(--text-muted); }
.rank-pos.top { color: var(--accent); }
.rank-name { font-weight: 500; }
.score-pill { font-family: var(--font-mono); font-size: var(--text-xs); font-weight: 600; padding: 2px 8px; border-radius: 4px; }
.score-pill.high   { background: rgba(110,231,183,.12); color: var(--accent); }
.score-pill.medium { background: rgba(251,191,36,.1);   color: var(--warning); }
.score-pill.low    { background: rgba(248,113,113,.1);  color: var(--danger); }
.rank-status { font-size: 10px; letter-spacing: 0.1em; text-transform: uppercase; }
.rank-status.pending { color: var(--text-muted); }
.rank-status.shortlisted { color: var(--accent); }
.rank-status.rejected { color: var(--danger); }
</style>
