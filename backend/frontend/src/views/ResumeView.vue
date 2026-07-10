<template>
  <div>
    <div class="page-header">
      <h1>Meu Currículo</h1>
      <p class="subtitle">Análise inteligente com agente de IA · Otimização para ATS</p>
    </div>

    <div class="upload-zone card" :class="{ dragging, 'has-file': selectedFile }"
      @dragover.prevent="dragging=true" @dragleave="dragging=false"
      @drop.prevent="onDrop" @click="fileInput?.click()">
      <input ref="fileInput" type="file" accept=".pdf,.docx,.txt" hidden @change="onFileChange" />
      <div class="upload-icon">{{ selectedFile ? '▤' : '⊕' }}</div>
      <div class="upload-label">{{ selectedFile ? selectedFile.name : 'Arraste seu currículo aqui' }}</div>
      <div class="upload-sub">{{ selectedFile ? formatSize(selectedFile.size) : 'PDF, DOCX ou TXT · Máx 10MB · Clique para selecionar' }}</div>
    </div>

    <div style="text-align:center;margin:12px 0;font-size:var(--text-xs);color:var(--text-muted);letter-spacing:.06em;text-transform:uppercase">
      ou cole o texto abaixo
    </div>

    <div class="card" style="padding:0;overflow:hidden;margin-bottom:16px">
      <div class="text-area-header">
        <span>▤ Texto do Currículo</span>
        <span class="char-count">{{ resumeText.length.toLocaleString() }} caracteres</span>
      </div>
      <textarea v-model="resumeText" class="text-area" placeholder="Cole o conteúdo do seu currículo aqui..." rows="8"></textarea>
    </div>

    <div class="meta-row">
      <div class="field" style="flex:2">
        <label class="label">Título do Currículo</label>
        <input v-model="title" type="text" class="input" placeholder="ex: curriculo_nome_2025" />
      </div>
    </div>

    <div v-if="error" class="error-box" style="margin-bottom:16px">{{ error }}</div>

    <button class="btn btn-primary analyze-btn" @click="analyze" :disabled="loading || (!selectedFile && !resumeText.trim())">
      <span v-if="loading" class="spinner" style="width:16px;height:16px;border-width:2px;border-color:rgba(0,0,0,0.2);border-top-color:#0a0a0b"></span>
      <span>{{ loading ? 'Analisando com IA...' : '⚡ Analisar com Agente IA' }}</span>
    </button>

    <div v-if="result" class="results">
      <hr class="divider" style="margin:32px 0"/>
      <div class="results-header">
        <h2>Resultado da Análise</h2>
        <span class="score-badge-large" :class="scoreClass(result.overallScore)">{{ result.overallScore }}/100</span>
      </div>

      <div class="score-grid">
        <div class="score-card" v-for="(s, label) in scores" :key="label">
          <div class="sc-label">{{ label }}</div>
          <div class="sc-bar">
            <div class="sc-fill" :style="{ width: s + '%' }" :class="scoreClass(s)"></div>
          </div>
          <div class="sc-value mono" :class="scoreClass(s)">{{ s }}</div>
        </div>
      </div>

      <div class="suggestions-section">
        <button class="btn btn-secondary suggestions-btn" @click="getSuggestions" :disabled="loadingSuggestions">
          <span v-if="loadingSuggestions" class="spinner" style="width:14px;height:14px;border-width:2px;border-color:rgba(255,255,255,0.2);border-top-color:#fff"></span>
          <span>{{ loadingSuggestions ? 'Gerando currículo melhorado...' : '✦ Gerar Currículo Melhorado' }}</span>
        </button>

        <div v-if="suggestions">
          <div class="suggestions-grid">
            <div class="sug-card strengths">
              <div class="sug-title">✓ Pontos Fortes</div>
              <ul>
                <li v-for="(s, i) in suggestions.strengths" :key="i">{{ s }}</li>
              </ul>
            </div>
            <div class="sug-card weaknesses">
              <div class="sug-title">✗ Pontos Fracos</div>
              <ul>
                <li v-for="(s, i) in suggestions.weaknesses" :key="i">{{ s }}</li>
              </ul>
            </div>
          </div>
          <div class="sug-card improvements" style="margin-top:12px">
            <div class="sug-title">⚡ Sugestões</div>
            <ul>
              <li v-for="(s, i) in suggestions.suggestions" :key="i">{{ s }}</li>
            </ul>
          </div>

          <div v-if="suggestions.improvedResume" class="rewritten-box">
            <div class="rewritten-header">
              <div class="sug-title">✦ Currículo Melhorado pela IA</div>
              <div class="rewritten-actions">
                <button class="btn-copy" @click="copy(suggestions.improvedResume)">{{ copied ? '✓ Copiado' : 'Copiar' }}</button>
                <button class="btn-download" @click="downloadPDF">⬇ PDF</button>
                <button class="btn-download" @click="downloadDOCX">⬇ DOCX</button>
              </div>
            </div>
            <div class="rewritten-text">{{ suggestions.improvedResume }}</div>
          </div>
        </div>
      </div>

      <div v-if="result.analysis" class="analysis-box card">
        <div class="analysis-label">Análise Completa</div>
        <div class="analysis-text" v-html="formatText(result.analysis)"></div>
      </div>
    </div>

    <div v-if="resumes.length" class="prev-section">
      <div class="section-title">Análises anteriores</div>
      <div class="resume-table">
        <div class="rt-row rt-head">
          <span>Título</span><span>Score</span><span>ATS</span><span>Data</span>
        </div>
        <div class="rt-row" v-for="r in resumes" :key="r.id" @click="loadResume(r.id)">
          <span>{{ r.title }}</span>
          <span><span class="score-pill" :class="scoreClass(r.overallScore)">{{ r.overallScore ?? '—' }}</span></span>
          <span><span class="score-pill" :class="scoreClass(r.atsScore)">{{ r.atsScore ?? '—' }}</span></span>
          <span class="muted">{{ formatDate(r.createdAt) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { resumeApi, chatApi } from '@/services/api'
import { jsPDF } from 'jspdf'
import { Document, Packer, Paragraph, TextRun, HeadingLevel } from 'docx'
import { saveAs } from 'file-saver'

const fileInput = ref<HTMLInputElement>()
const selectedFile = ref<File | null>(null)
const resumeText = ref('')
const title = ref('')
const dragging = ref(false)
const loading = ref(false)
const loadingSuggestions = ref(false)
const error = ref('')
const result = ref<any>(null)
const resumes = ref<any[]>([])
const suggestions = ref<any>(null)
const copied = ref(false)

const scores = computed(() => result.value ? {
  'Skills': result.value.skillsScore,
  'Experiência': result.value.experienceScore,
  'Formato': result.value.formatScore,
  'ATS': result.value.atsScore,
} : {})

onMounted(async () => {
  try { resumes.value = (await resumeApi.list()).data } catch {}
})

function onFileChange(e: Event) {
  const f = (e.target as HTMLInputElement).files?.[0]
  if (f) { selectedFile.value = f; title.value = title.value || f.name.replace(/\.[^.]+$/, '') }
}

function onDrop(e: DragEvent) {
  dragging.value = false
  const f = e.dataTransfer?.files?.[0]
  if (f) { selectedFile.value = f; title.value = title.value || f.name.replace(/\.[^.]+$/, '') }
}

async function analyze() {
  error.value = ''
  loading.value = true
  result.value = null
  suggestions.value = null
  try {
    let res
    if (selectedFile.value) {
      res = await resumeApi.analyze(selectedFile.value, title.value)
    } else {
      const blob = new Blob([resumeText.value], { type: 'text/plain' })
      const file = new File([blob], title.value || 'curriculo.txt', { type: 'text/plain' })
      res = await resumeApi.analyze(file, title.value)
    }
    if (res.data?.status === 'ERROR') {
      error.value = 'A IA não conseguiu analisar o currículo. Verifique se o arquivo tem conteúdo legível e tente novamente.'
    } else {
      result.value = res.data
      resumes.value = (await resumeApi.list()).data
    }
  } catch (e: any) {
    error.value = e.response?.data?.message || 'Erro ao analisar. Verifique o arquivo e tente novamente.'
  } finally {
    loading.value = false
  }
}

async function getSuggestions() {
  loadingSuggestions.value = true
  try {
    const originalContent = result.value?.content || resumeText.value || ''
    const prompt = `Você é um especialista em currículos. Com base no currículo original abaixo, faça o seguinte:
1. Identifique 3 pontos fortes
2. Identifique 3 pontos fracos
3. Liste 3 sugestões de melhoria
4. Reescreva o currículo COMPLETO melhorado, mantendo TODOS os dados originais (nome, email, telefone, experiências, formação, habilidades) mas com linguagem mais profissional, otimizado para ATS e com as melhorias aplicadas.

Retorne APENAS um JSON válido sem markdown com os campos: strengths (array), weaknesses (array), suggestions (array), improvedResume (string com o currículo completo melhorado).

CURRÍCULO ORIGINAL:
${originalContent}`

    const res = await chatApi.send(prompt)
    const text = res.data?.response || ''
    const clean = text.replace(/```json|```/g, '').trim()
    suggestions.value = JSON.parse(clean)
  } catch {
    suggestions.value = {
      strengths: ['Não foi possível carregar os pontos fortes'],
      weaknesses: ['Não foi possível carregar os pontos fracos'],
      suggestions: ['Tente novamente em instantes'],
      improvedResume: ''
    }
  } finally {
    loadingSuggestions.value = false
  }
}

async function loadResume(id: number) {
  try {
    const res = await resumeApi.get(id)
    result.value = res.data
    suggestions.value = null
    window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' })
  } catch {}
}

async function copy(text: string) {
  await navigator.clipboard.writeText(text)
  copied.value = true
  setTimeout(() => { copied.value = false }, 2000)
}

function downloadPDF() {
  const doc = new jsPDF()
  const fileName = (title.value || 'curriculo') + '_melhorado.pdf'
  const margin = 20
  const maxWidth = doc.internal.pageSize.getWidth() - margin * 2
  let y = 20

  const text = suggestions.value?.improvedResume || ''
  const lines = text.split('\n')

  doc.setFont('helvetica', 'normal')
  doc.setFontSize(11)

  lines.forEach((line: string) => {
    if (y > 270) { doc.addPage(); y = 20 }
    if (line.trim() === '') { y += 4; return }
    const wrapped = doc.splitTextToSize(line, maxWidth)
    doc.text(wrapped, margin, y)
    y += wrapped.length * 6 + 2
  })

  doc.save(fileName)
}

async function downloadDOCX() {
  const fileName = (title.value || 'curriculo') + '_melhorado.docx'
  const text = suggestions.value?.improvedResume || ''
  const lines = text.split('\n')

  const children = lines.map((line: string) => {
    if (line.trim() === '') return new Paragraph({ text: '' })
    return new Paragraph({ children: [new TextRun(line)] })
  })

  const docFile = new Document({ sections: [{ properties: {}, children }] })
  const blob = await Packer.toBlob(docFile)
  saveAs(blob, fileName)
}

function scoreClass(s: number) {
  if (!s) return 'low'
  return s >= 70 ? 'high' : s >= 40 ? 'medium' : 'low'
}

function formatSize(b: number) { return (b / 1024 / 1024).toFixed(2) + ' MB' }
function formatDate(d: string) { return new Date(d).toLocaleDateString('pt-BR') }
function formatText(t: string) { return t.replace(/\n/g, '<br>') }
</script>

<style scoped>
.upload-zone { border: 1.5px dashed var(--border-2); text-align: center; padding: 40px 24px; cursor: pointer; transition: all 0.2s; margin-bottom: 0; }
.upload-zone:hover, .upload-zone.dragging { border-color: var(--accent); background: var(--accent-dim); }
.upload-zone.has-file { border-color: var(--accent); border-style: solid; }
.upload-icon { font-size: 2rem; color: var(--accent); margin-bottom: 10px; }
.upload-label { font-weight: 500; margin-bottom: 4px; }
.upload-sub { font-size: var(--text-xs); color: var(--text-muted); }
.text-area-header { display: flex; justify-content: space-between; padding: 10px 16px; border-bottom: 1px solid var(--border); font-size: var(--text-sm); color: var(--text-muted); }
.char-count { font-family: var(--font-mono); font-size: var(--text-xs); }
.text-area { width: 100%; background: transparent; border: none; color: var(--text); font-family: var(--font-mono); font-size: 12px; padding: 14px 16px; resize: vertical; outline: none; line-height: 1.6; min-height: 120px; }
.meta-row { display: flex; gap: 12px; margin-bottom: 16px; }
.analyze-btn { width: 100%; justify-content: center; padding: 14px; font-size: var(--text-sm); }
.results-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.score-badge-large { font-family: var(--font-mono); font-size: 1.25rem; font-weight: 700; padding: 6px 14px; border-radius: var(--radius); }
.score-badge-large.high { background: rgba(110,231,183,.12); color: var(--accent); }
.score-badge-large.medium { background: rgba(251,191,36,.1); color: var(--warning); }
.score-badge-large.low { background: rgba(248,113,113,.1); color: var(--danger); }
.score-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 24px; }
.score-card { background: var(--bg-2); border: 1px solid var(--border); border-radius: var(--radius); padding: 16px; }
.sc-label { font-size: 10px; letter-spacing: 0.12em; text-transform: uppercase; color: var(--text-muted); margin-bottom: 10px; }
.sc-bar { height: 4px; background: var(--border-2); border-radius: 2px; margin-bottom: 8px; overflow: hidden; }
.sc-fill { height: 100%; border-radius: 2px; transition: width 1s ease; }
.sc-fill.high { background: var(--accent); }
.sc-fill.medium { background: var(--warning); }
.sc-fill.low { background: var(--danger); }
.sc-value { font-family: var(--font-mono); font-size: 1.25rem; font-weight: 600; }
.sc-value.high { color: var(--accent); }
.sc-value.medium { color: var(--warning); }
.sc-value.low { color: var(--danger); }
.mono { font-family: var(--font-mono); }
.suggestions-section { margin-bottom: 24px; }
.suggestions-btn { width: 100%; justify-content: center; padding: 12px; margin-bottom: 16px; }
.suggestions-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.sug-card { background: var(--bg-2); border: 1px solid var(--border); border-radius: var(--radius); padding: 16px; }
.sug-card.strengths { border-color: rgba(52,211,153,.3); }
.sug-card.weaknesses { border-color: rgba(248,113,113,.3); }
.sug-card.improvements { border-color: rgba(251,191,36,.3); }
.sug-title { font-size: 10px; letter-spacing: 0.12em; text-transform: uppercase; color: var(--text-muted); margin-bottom: 10px; font-weight: 600; }
.sug-card ul { margin: 0; padding-left: 16px; }
.sug-card li { font-size: var(--text-sm); line-height: 1.7; color: var(--text); margin-bottom: 4px; }
.rewritten-box { margin-top: 12px; background: var(--bg-2); border: 1px solid var(--accent); border-radius: var(--radius); padding: 16px; }
.rewritten-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; flex-wrap: wrap; gap: 8px; }
.rewritten-actions { display: flex; gap: 8px; }
.rewritten-text { font-size: var(--text-sm); line-height: 1.8; color: var(--text); white-space: pre-wrap; }
.btn-copy, .btn-download { font-size: 12px; padding: 4px 12px; border: 1px solid var(--accent); border-radius: var(--radius); background: transparent; color: var(--accent); cursor: pointer; transition: all 0.15s; }
.btn-copy:hover, .btn-download:hover { background: var(--accent-dim); }
.analysis-box { margin-bottom: 24px; }
.analysis-label { font-size: 10px; letter-spacing: 0.12em; text-transform: uppercase; color: var(--text-muted); margin-bottom: 12px; }
.analysis-text { font-size: var(--text-sm); line-height: 1.8; color: var(--text); }
.prev-section { margin-top: 40px; }
.section-title { font-size: 10px; letter-spacing: 0.14em; text-transform: uppercase; color: var(--text-muted); margin-bottom: 14px; }
.resume-table { background: var(--bg-2); border: 1px solid var(--border); border-radius: var(--radius-lg); overflow: hidden; }
.rt-row { display: grid; grid-template-columns: 2fr 1fr 1fr 1fr; gap: 8px; padding: 12px 16px; font-size: var(--text-sm); border-bottom: 1px solid var(--border); align-items: center; }
.rt-row:last-child { border-bottom: none; }
.rt-row:not(.rt-head) { cursor: pointer; transition: background 0.1s; }
.rt-row:not(.rt-head):hover { background: var(--bg-3); }
.rt-head { font-size: 10px; letter-spacing: 0.1em; text-transform: uppercase; color: var(--text-muted); }
.score-pill { font-family: var(--font-mono); font-size: var(--text-xs); font-weight: 600; padding: 2px 8px; border-radius: 4px; }
.score-pill.high { background: rgba(110,231,183,.12); color: var(--accent); }
.score-pill.medium { background: rgba(251,191,36,.1); color: var(--warning); }
.score-pill.low { background: rgba(248,113,113,.1); color: var(--danger); }
.muted { color: var(--text-muted); }
</style>