<template>
  <div>
    <div class="page-header">
      <h1>Vagas</h1>
      <p class="subtitle">{{ totalVisivel }} oportunidades disponíveis</p>
    </div>

    <!-- Filtros -->
    <form class="filtros" @submit.prevent="buscar">
      <input v-model="filtroTermo" type="text" class="input" placeholder="Cargo, tecnologia ou palavra-chave" />
      <input v-model="filtroLocal" type="text" class="input filtro-local" placeholder="Cidade ou estado" />
      <label class="filtro-check">
        <input type="checkbox" v-model="filtroRemotas" @change="buscar" />
        <span>Só remotas</span>
      </label>
      <button type="submit" class="btn btn-primary" :disabled="loadingExternas">
        {{ loadingExternas ? 'Buscando...' : 'Buscar' }}
      </button>
    </form>

    <!-- A origem fica aqui, junto do filtro, e nao repetida em cada card:
         é informação sobre a busca, não sobre a vaga individual. -->
    <p v-if="fonte" class="aviso-fonte">
      <template v-if="fonte === 'Adzuna'">
        As vagas de outros portais vêm do <strong>Adzuna</strong>, que agrega
        anúncios de diversos sites de emprego. Ao abrir uma delas você é levado ao
        anúncio original, onde a candidatura acontece.
      </template>
      <template v-else>
        Exibindo vagas remotas internacionais do <strong>Remotive</strong>. Para
        vagas brasileiras, configure as chaves da Adzuna nas variáveis de ambiente.
      </template>
    </p>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
    </div>

    <div v-else-if="!totalVisivel" class="empty-state">
      <p>Nenhuma vaga encontrada{{ filtroTermo ? ` para "${filtroTermo}"` : '' }}</p>
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

    <!-- Vagas externas: nao aceitam candidatura pela plataforma, entao levam ao
         anuncio original. O selo de origem deixa a diferenca explicita. -->
    <div v-if="externas.length" class="jobs-list externas">
      <h2 class="secao-externas">Vagas de outros portais</h2>

      <a
        v-for="vaga in externas"
        :key="vaga.id"
        :href="vaga.url"
        target="_blank"
        rel="noopener noreferrer"
        class="job-card card externa"
      >
        <div class="job-header">
          <div>
            <div class="job-title">{{ vaga.title }}</div>
            <div class="job-company">{{ vaga.company || 'Empresa não informada' }}</div>
          </div>
          <div class="job-meta">
            <span class="tag" v-if="vaga.remote">Remoto</span>
            <span class="tag" v-if="vaga.location">{{ vaga.location }}</span>
          </div>
        </div>

        <p v-if="vaga.description" class="externa-desc">{{ vaga.description }}</p>

        <div class="job-footer">
          <span class="job-date">{{ vaga.publishedAt ? formatDate(vaga.publishedAt) : '' }}</span>
          <span class="expand-hint">Ver no {{ vaga.source }} ↗</span>
        </div>
      </a>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
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

// ----- vagas externas -----
const externas = ref<any[]>([])
const fonte = ref('')
const loadingExternas = ref(false)
const filtroTermo = ref('')
const filtroLocal = ref('')
const filtroRemotas = ref(false)

const totalVisivel = computed(() => jobs.value.length + externas.value.length)

onMounted(async () => {
  try { jobs.value = (await jobApi.list()).data } catch {} finally { loading.value = false }
  if (auth.isCandidate) {
    try { resumes.value = (await resumeApi.list()).data } catch {}
  }
  buscar()
})

async function buscar() {
  loadingExternas.value = true
  try {
    const res = await jobApi.external({
      q: filtroTermo.value || undefined,
      local: filtroLocal.value || undefined,
      remotas: filtroRemotas.value
    })
    externas.value = res.data?.jobs || []
    fonte.value = res.data?.source || ''
  } catch (e) {
    // A busca externa e um complemento: se a fonte cair, as vagas internas
    // continuam na tela em vez de a pagina inteira falhar.
    console.error('Falha ao buscar vagas externas:', e)
    externas.value = []
  } finally {
    loadingExternas.value = false
  }
}

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

/* ----- filtros ----- */
.filtros { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-bottom: 20px; }
.filtros .input { flex: 1; min-width: 200px; }
.filtro-local { max-width: 220px; }
.filtro-check { display: flex; align-items: center; gap: 7px; font-size: var(--text-sm); color: var(--text-muted); white-space: nowrap; cursor: pointer; }
.filtro-check input { accent-color: var(--accent); width: 15px; height: 15px; cursor: pointer; }

.aviso-fonte {
  font-size: var(--text-xs); color: var(--text-muted); line-height: 1.6;
  background: var(--bg-2); border: 1px solid var(--border);
  border-radius: var(--radius); padding: 10px 14px; margin-bottom: 20px;
}

/* ----- vagas externas ----- */
.secao-externas { font-size: var(--text-lg); margin: 32px 0 4px; }
.job-card.externa { display: block; text-decoration: none; color: inherit; }
.job-card.externa:hover { border-color: var(--accent); }
.tag.fonte { background: var(--accent-dim); color: var(--accent); border-color: var(--accent); }
.externa-desc {
  font-size: var(--text-sm); line-height: 1.65; color: var(--text-muted);
  margin: 10px 0 0; display: -webkit-box; -webkit-line-clamp: 2;
  -webkit-box-orient: vertical; overflow: hidden;
}

@media (max-width: 600px) {
  .filtros { flex-direction: column; align-items: stretch; }
  .filtro-local { max-width: 100%; }
}
</style>
