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
      <span v-if="loading" class="spinner" style="width:16px;height:16px;border-width:2px;border-color:rgba(0,0,0,0.2);border-top-color:var(--on-accent)"></span>
      <span>{{ loading ? 'Analisando com IA...' : '⚡ Analisar com Agente IA' }}</span>
    </button>

    <!-- Painel de progresso: a analise leva de 20 a 60 segundos e antes nao havia
         nenhum sinal de vida nesse intervalo, o que passava impressao de travamento. -->
    <div v-if="loading" class="progress-panel">
      <div class="progress-track"><div class="progress-fill"></div></div>
      <ul class="progress-steps">
        <li v-for="(etapa, i) in etapas" :key="i" :class="{ done: i < etapaAtual, current: i === etapaAtual }">
          <span class="step-dot"></span>{{ etapa }}
        </li>
      </ul>
      <p class="progress-note">
        Tempo decorrido: {{ segundos }}s · costuma levar entre 20 e 60 segundos.
      </p>
    </div>

    <div v-if="result" class="results">
      <hr class="divider" style="margin:32px 0"/>
      <div class="results-header">
        <h2>Resultado da Análise</h2>
        <span class="score-badge-large" :class="scoreClass(result.overallScore)">{{ result.overallScore }}/100</span>
      </div>

      <div class="score-grid">
        <div class="score-card" v-for="d in dimensoes" :key="d.chave">
          <div class="sc-head">
            <span class="sc-label">{{ d.rotulo }}</span>
            <span class="sc-weight">peso {{ d.peso }}%</span>
          </div>
          <div class="sc-bar">
            <div class="sc-fill" :style="{ width: d.nota + '%' }" :class="scoreClass(d.nota)"></div>
          </div>
          <div class="sc-value mono" :class="scoreClass(d.nota)">{{ d.nota }}</div>
          <p v-if="d.justificativa" class="sc-rationale">{{ d.justificativa }}</p>
        </div>
      </div>

      <!-- Como a nota geral foi obtida. Antes o numero vinha da IA sem relacao com
           as quatro dimensoes e nao havia como explicar de onde saia. -->
      <details class="formula-box">
        <summary>Como chegamos em {{ result.overallScore }}/100</summary>
        <p class="formula-intro">
          A nota geral é a média ponderada das quatro dimensões. Os pesos são fixos,
          então o mesmo currículo sempre recebe a mesma nota.
        </p>
        <div class="formula-calc mono">
          <div v-for="d in dimensoes" :key="d.chave">
            {{ d.rotulo }}: {{ d.nota }} × {{ d.peso }}% = {{ (d.nota * d.peso / 100).toFixed(1) }}
          </div>
          <div class="formula-total">Total = {{ result.overallScore }}</div>
        </div>
      </details>

      <!-- Pontos fortes, fracos e sugestoes vem da propria analise, sem uma
           segunda chamada a IA: eles ja foram gerados e gravados no analysisJson. -->
      <div v-if="analise" class="suggestions-section">
        <div class="suggestions-grid">
          <div class="sug-card strengths">
            <div class="sug-title">Pontos fortes</div>
            <ul>
              <li v-for="(s, i) in analise.strengths" :key="i">{{ s }}</li>
            </ul>
          </div>
          <div class="sug-card weaknesses">
            <div class="sug-title">Pontos fracos</div>
            <ul>
              <li v-for="(s, i) in analise.weaknesses" :key="i">{{ s }}</li>
            </ul>
          </div>
        </div>
        <div class="sug-card improvements" style="margin-top:12px">
          <div class="sug-title">Sugestões</div>
          <ul>
            <li v-for="(s, i) in analise.suggestions" :key="i">{{ s }}</li>
          </ul>
        </div>
      </div>

      <div class="suggestions-section">
        <button class="btn btn-secondary suggestions-btn" @click="getSuggestions" :disabled="loadingSuggestions">
          <span v-if="loadingSuggestions" class="spinner" style="width:14px;height:14px;border-width:2px"></span>
          <span>{{ loadingSuggestions ? 'Reescrevendo o currículo...' : 'Gerar currículo melhorado' }}</span>
        </button>

        <div v-if="improveError" class="error-box">{{ improveError }}</div>

        <div v-if="improvedResume" class="rewritten-box">
          <div class="rewritten-header">
            <div class="sug-title">Currículo reescrito</div>
            <div class="rewritten-actions">
              <button class="btn-copy" @click="copy(improvedResume)">{{ copied ? 'Copiado' : 'Copiar' }}</button>
              <button class="btn-download" @click="downloadPDF">PDF</button>
              <button class="btn-download" @click="downloadDOCX">DOCX</button>
              <!-- Salvar transforma a reescrita num currículo de verdade, que passa
                   a aparecer no seletor de candidatura. Sem isso ela só existe nesta tela. -->
              <button class="btn-salvar" @click="salvarMelhorado" :disabled="salvandoMelhorado">
                {{ salvandoMelhorado ? 'Salvando...' : (salvouMelhorado ? 'Salvo' : 'Salvar como currículo') }}
              </button>
            </div>
          </div>
          <div class="rewritten-text">{{ improvedResume }}</div>
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
import { resumeApi } from '@/services/api'
import { jsPDF } from 'jspdf'
import { Document, Packer, Paragraph, TextRun, BorderStyle } from 'docx'
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
const improvedResume = ref('')
const improveError = ref('')
const salvandoMelhorado = ref(false)
const salvouMelhorado = ref(false)
const copied = ref(false)

// Analise qualitativa: vem do MySQL (analysisJson), entao nao depende do MongoDB.
const analise = computed<any>(() => {
  if (!result.value?.analysisJson) return null
  try { return JSON.parse(result.value.analysisJson) } catch { return null }
})

// Uma linha por dimensao: nota, peso usado no calculo e a justificativa da IA.
const dimensoes = computed(() => {
  if (!result.value) return []
  const pesos = result.value.scoreWeights || { skills: 35, experience: 30, ats: 20, format: 15 }
  const a = analise.value || {}
  return [
    { chave: 'skills',     rotulo: 'Skills',     nota: result.value.skillsScore,     peso: pesos.skills,     justificativa: a.skillsRationale },
    { chave: 'experience', rotulo: 'Experiência', nota: result.value.experienceScore, peso: pesos.experience, justificativa: a.experienceRationale },
    { chave: 'ats',        rotulo: 'ATS',        nota: result.value.atsScore,        peso: pesos.ats,        justificativa: a.atsRationale },
    { chave: 'format',     rotulo: 'Formato',    nota: result.value.formatScore,     peso: pesos.format,     justificativa: a.formatRationale }
  ]
})

// ----- Progresso da analise -----
const etapas = [
  'Extraindo o texto do arquivo',
  'Enviando para o agente de IA',
  'Avaliando skills e experiência',
  'Calculando a nota final'
]
const etapaAtual = ref(0)
const segundos = ref(0)
let cronometro: number | undefined

function iniciarProgresso() {
  segundos.value = 0
  etapaAtual.value = 0
  cronometro = window.setInterval(() => {
    segundos.value++
    // As etapas sao estimadas pelo tempo: a API nao reporta progresso real,
    // e prometer precisao que nao temos seria pior do que uma estimativa honesta.
    if (segundos.value > 3 && etapaAtual.value < 1) etapaAtual.value = 1
    if (segundos.value > 10 && etapaAtual.value < 2) etapaAtual.value = 2
    if (segundos.value > 25 && etapaAtual.value < 3) etapaAtual.value = 3
  }, 1000)
}

function pararProgresso() {
  if (cronometro) { clearInterval(cronometro); cronometro = undefined }
}

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
  improvedResume.value = ''
  iniciarProgresso()
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
    // Mensagens distintas por causa: antes qualquer falha virava o mesmo texto,
    // e um timeout de 2 minutos ficava indistinguivel de um arquivo invalido.
    if (e.code === 'ECONNABORTED') {
      error.value = 'A análise passou de 2 minutos e foi interrompida. Currículos muito longos podem estourar esse limite — tente um arquivo menor.'
    } else if (e.response?.status === 500) {
      error.value = 'O servidor falhou ao processar a análise. Tente novamente em instantes.'
    } else {
      error.value = e.response?.data?.message || 'Erro ao analisar. Verifique o arquivo e tente novamente.'
    }
  } finally {
    loading.value = false
    pararProgresso()
  }
}

/*
  Antes esta funcao montava um prompt gigante com o curriculo inteiro e mandava
  para /api/chat/message, que tem @Size(max = 4000) — qualquer curriculo real
  estourava e voltava 400, produzindo os "Nao foi possivel carregar".

  Pior: ela pedia a IA pontos fortes, fracos e sugestoes que a analise JA tinha
  devolvido e gravado no analysisJson. Era uma segunda cobranca da Anthropic
  para obter dados que ja estavam na tela.

  Agora esses tres vem da analise (computed `analise`), e aqui sobrou apenas a
  reescrita — num endpoint proprio, que le o texto do banco pelo id.
*/
/*
  Salva a reescrita como um curriculo novo, reaproveitando o endpoint de analise:
  o texto vira um arquivo .txt em memoria e segue o mesmo caminho de um upload.
  Assim ele recebe notas proprias e passa a existir no seletor de candidatura,
  sem precisar de um endpoint separado no backend.
*/
async function salvarMelhorado() {
  if (!improvedResume.value) return
  salvandoMelhorado.value = true
  try {
    const nomeBase = (result.value?.title || 'curriculo').replace(/\.[^.]+$/, '')
    const nome = `${nomeBase}_melhorado`
    const blob = new Blob([improvedResume.value], { type: 'text/plain' })
    const arquivo = new File([blob], `${nome}.txt`, { type: 'text/plain' })

    await resumeApi.analyze(arquivo, nome)
    resumes.value = (await resumeApi.list()).data
    salvouMelhorado.value = true
  } catch (e: any) {
    improveError.value = e.response?.data?.message || 'Não foi possível salvar. Tente novamente.'
  } finally {
    salvandoMelhorado.value = false
  }
}

async function getSuggestions() {
  if (!result.value?.id) return
  loadingSuggestions.value = true
  improveError.value = ''
  try {
    const res = await resumeApi.improve(result.value.id)
    improvedResume.value = res.data?.improvedResume || ''
    salvouMelhorado.value = false
  } catch (e: any) {
    improveError.value = e.code === 'ECONNABORTED'
      ? 'A reescrita passou de 2 minutos e foi interrompida. Tente com um currículo menor.'
      : (e.response?.data?.message || 'Não foi possível gerar a versão melhorada. Tente novamente.')
  } finally {
    loadingSuggestions.value = false
  }
}

async function loadResume(id: number) {
  try {
    const res = await resumeApi.get(id)
    result.value = res.data
    improvedResume.value = ''
    window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' })
  } catch {}
}

async function copy(text: string) {
  await navigator.clipboard.writeText(text)
  copied.value = true
  setTimeout(() => { copied.value = false }, 2000)
}

/*
  PDF de UMA página, sempre.

  A versão anterior chamava addPage() quando o texto passava do fim — o currículo
  virava duas ou três folhas, que é justamente o que faz recrutador descartar.

  Aqui o layout é medido antes de desenhar: se não couber, a fonte e o
  espaçamento diminuem até caber, dentro de um piso legível. Só se nem no piso
  couber é que a última linha é cortada — e aí o problema é excesso de conteúdo,
  não de formatação.
*/
function downloadPDF() {
  const doc = new jsPDF({ unit: 'mm', format: 'a4' })
  const fileName = (title.value || 'curriculo') + '.pdf'

  const larguraPagina = doc.internal.pageSize.getWidth()
  const alturaPagina = doc.internal.pageSize.getHeight()
  const margem = 16
  const larguraUtil = larguraPagina - margem * 2
  const alturaUtil = alturaPagina - margem * 2

  const linhas = improvedResume.value.split('\n')

  const blocos = montarBlocos(improvedResume.value)

  /** Simula o desenho e devolve a altura total que o conteúdo ocuparia. */
  function medir(f: number, el: number, esp: number) {
    let altura = 0
    blocos.forEach((b, i) => {
      if (b.tipo === 'nome') { doc.setFontSize(f + 5); altura += el + 2.5 }
      else if (b.tipo === 'contato') { doc.setFontSize(f - 1); altura += el - 0.6 }
      else if (b.tipo === 'secao') { doc.setFontSize(f); altura += (i === 0 ? 0 : esp) + el + 1.6 }
      else { doc.setFontSize(f) }
      if (b.tipo !== 'nome' && b.tipo !== 'secao') {
        const largura = b.tipo === 'item' ? larguraUtil - 4 : larguraUtil
        altura += doc.splitTextToSize(b.texto, largura).length * el
      }
      if (b.tipo === 'item') altura += 0.6
    })
    return altura
  }

  // Busca a maior combinação que ainda caiba. 8.5pt é o piso: abaixo disso o
  // currículo fica desconfortável de ler e o esforço passa a ser contraproducente.
  let fonte = 10.5
  let entrelinha = 4.5
  let espacoSecao = 4
  while (medir(fonte, entrelinha, espacoSecao) > alturaUtil && fonte > 8.5) {
    fonte -= 0.25
    entrelinha -= 0.1
    espacoSecao -= 0.15
  }

  let y = margem
  const fim = alturaPagina - margem

  blocos.forEach((b, i) => {
    if (y > fim) return   // uma folha, sem exceção

    if (b.tipo === 'nome') {
      doc.setFont('helvetica', 'bold')
      doc.setFontSize(fonte + 5)
      doc.setTextColor(20)
      doc.text(b.texto, margem, y)
      y += entrelinha + 2.5
      return
    }

    if (b.tipo === 'contato') {
      doc.setFont('helvetica', 'normal')
      doc.setFontSize(fonte - 1)
      doc.setTextColor(90)
      doc.splitTextToSize(b.texto, larguraUtil).forEach((p: string) => {
        doc.text(p, margem, y); y += entrelinha - 0.6
      })
      return
    }

    if (b.tipo === 'secao') {
      if (i !== 0) y += espacoSecao
      doc.setFont('helvetica', 'bold')
      doc.setFontSize(fonte)
      doc.setTextColor(20)
      doc.text(b.texto.toUpperCase(), margem, y)
      y += 1.6
      doc.setDrawColor(170)
      doc.setLineWidth(0.25)
      doc.line(margem, y, larguraPagina - margem, y)
      y += entrelinha
      return
    }

    // Item de lista: marcador e recuo pendente, para a segunda linha alinhar
    // com a primeira em vez de voltar à margem.
    doc.setFont('helvetica', 'normal')
    doc.setFontSize(fonte)
    doc.setTextColor(40)

    if (b.tipo === 'item') {
      const partes = doc.splitTextToSize(b.texto, larguraUtil - 4)
      partes.forEach((p: string, k: number) => {
        if (y > fim) return
        if (k === 0) doc.text('•', margem, y)
        doc.text(p, margem + 4, y)
        y += entrelinha
      })
      y += 0.6
    } else {
      doc.splitTextToSize(b.texto, larguraUtil).forEach((p: string) => {
        if (y > fim) return
        doc.text(p, margem, y); y += entrelinha
      })
    }
  })

  doc.save(fileName)
}

/** Nomes de seção conhecidos, para reconhecer o título mesmo sem maiúsculas. */
const SECOES = [
  'resumo profissional', 'resumo', 'perfil profissional', 'perfil', 'objetivo',
  'experiencia profissional', 'experiencia', 'projetos', 'formacao academica',
  'formacao', 'habilidades tecnicas', 'habilidades', 'competencias',
  'idiomas', 'certificacoes', 'cursos', 'contato'
]

function semAcento(s: string) {
  return s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim()
}

/*
  Converte o texto corrido em blocos com papel definido. Antes o gerador só
  distinguia "título" de "linha", e a detecção exigia MAIÚSCULAS — se o modelo
  escrevesse "Resumo Profissional", nada ficava em negrito. Agora compara com uma
  lista de seções conhecidas, sem acento e sem caixa, e o resto do currículo ganha
  hierarquia: nome, contato, seções e itens de lista.
*/
function montarBlocos(texto: string) {
  const linhas = texto.split('\n')
  const blocos: { tipo: 'nome' | 'contato' | 'secao' | 'item' | 'texto'; texto: string }[] = []
  let viuNome = false
  let viuSecao = false

  for (const bruta of linhas) {
    const linha = bruta.trim()
    if (!linha) continue

    const chave = semAcento(linha).replace(/[:]+$/, '')
    const ehSecao = SECOES.includes(chave) ||
      (linha.length < 45 && linha === linha.toUpperCase() && /[A-ZÀ-Ú]{3}/.test(linha))

    if (ehSecao) { blocos.push({ tipo: 'secao', texto: linha.replace(/[:]+$/, '') }); viuSecao = true; continue }
    if (!viuNome) { blocos.push({ tipo: 'nome', texto: linha }); viuNome = true; continue }
    if (!viuSecao) { blocos.push({ tipo: 'contato', texto: linha }); continue }

    if (/^[-•*]\s+/.test(linha)) {
      blocos.push({ tipo: 'item', texto: linha.replace(/^[-•*]\s+/, '') })
    } else {
      blocos.push({ tipo: 'texto', texto: linha })
    }
  }
  return blocos
}

/** Mesma estrutura do PDF: nome, contato, seções em negrito e itens com marcador. */
async function downloadDOCX() {
  const fileName = (title.value || 'curriculo') + '.docx'
  const blocos = montarBlocos(improvedResume.value)

  const children = blocos.map(b => {
    if (b.tipo === 'nome') {
      return new Paragraph({
        children: [new TextRun({ text: b.texto, bold: true, size: 30 })],
        spacing: { after: 60 }
      })
    }
    if (b.tipo === 'contato') {
      return new Paragraph({
        children: [new TextRun({ text: b.texto, size: 18, color: '555555' })],
        spacing: { after: 20 }
      })
    }
    if (b.tipo === 'secao') {
      return new Paragraph({
        children: [new TextRun({ text: b.texto.toUpperCase(), bold: true, size: 21 })],
        spacing: { before: 220, after: 80 },
        border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: 'AAAAAA', space: 2 } }
      })
    }
    if (b.tipo === 'item') {
      return new Paragraph({
        children: [new TextRun({ text: b.texto, size: 20 })],
        bullet: { level: 0 },
        spacing: { after: 40 }
      })
    }
    return new Paragraph({
      children: [new TextRun({ text: b.texto, size: 20 })],
      spacing: { after: 60 }
    })
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
/* ----- progresso da analise ----- */
.progress-panel { margin-top: 16px; padding: 18px 20px; border: 1px solid var(--border); border-radius: var(--radius); background: var(--bg-2); }
.progress-track { height: 3px; background: var(--border-2); border-radius: 2px; overflow: hidden; margin-bottom: 16px; }
.progress-fill { height: 100%; width: 40%; background: var(--accent); border-radius: 2px; animation: slide 1.6s ease-in-out infinite; }
@keyframes slide { 0% { transform: translateX(-100%); } 100% { transform: translateX(250%); } }
.progress-steps { list-style: none; padding: 0; margin: 0 0 14px; display: flex; flex-direction: column; gap: 8px; }
.progress-steps li { display: flex; align-items: center; gap: 10px; font-size: var(--text-xs); color: var(--text-faint); transition: color 0.3s var(--ease); }
.progress-steps li.done { color: var(--text-muted); }
.progress-steps li.current { color: var(--accent); }
.step-dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; flex-shrink: 0; }
.progress-note { font-size: 11px; color: var(--text-faint); margin: 0; }

/* ----- justificativa e formula ----- */
.sc-head { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; margin-bottom: 8px; }
.sc-weight { font-size: 10px; color: var(--text-faint); letter-spacing: 0.04em; }
.sc-rationale { font-size: 11px; line-height: 1.6; color: var(--text-muted); margin: 8px 0 0; }

.formula-box { margin-top: 16px; border: 1px solid var(--border); border-radius: var(--radius); background: var(--bg-2); padding: 14px 18px; }
.formula-box summary { cursor: pointer; font-size: var(--text-sm); color: var(--text-muted); }
.formula-box summary:hover { color: var(--accent); }
.formula-intro { font-size: var(--text-xs); color: var(--text-muted); line-height: 1.7; margin: 12px 0; }
.formula-calc { font-size: 12px; color: var(--text-muted); display: flex; flex-direction: column; gap: 4px; }
.formula-total { color: var(--accent); border-top: 1px solid var(--border); padding-top: 6px; margin-top: 4px; }

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
.btn-salvar { font-size: 12px; padding: 4px 12px; border: 1px solid var(--accent); border-radius: var(--radius); background: var(--accent); color: var(--on-accent); cursor: pointer; transition: opacity 0.15s; }
.btn-salvar:hover:not(:disabled) { opacity: 0.88; }
.btn-salvar:disabled { opacity: 0.5; cursor: default; }
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