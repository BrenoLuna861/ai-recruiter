<template>
  <div class="chat-page">
    <div class="chat-container card">
      <!-- Messages -->
      <div class="messages" ref="messagesEl">
        <div class="welcome" v-if="!messages.length">
          <div class="welcome-title">Olá, eu sou a Aria</div>
          <div class="welcome-body">Sua assistente inteligente de recrutamento. Pergunte sobre seu currículo, vagas, entrevistas ou desenvolvimento de carreira.</div>
          <div class="suggestions">
            <button class="suggestion" v-for="s in suggestions" :key="s" @click="sendSuggestion(s)">{{ s }}</button>
          </div>
        </div>

        <div v-for="(msg, i) in messages" :key="i" class="message" :class="msg.role">
          <div class="msg-bubble">
            <div class="msg-author">{{ msg.role === 'user' ? 'Você' : 'Aria' }}</div>
            <div class="msg-text" v-html="formatMessage(msg.content)"></div>
            <div class="msg-time">{{ formatTime(msg.time) }}</div>
          </div>
        </div>

        <div v-if="loading" class="message assistant">
          <div class="msg-bubble">
            <div class="msg-author">Aria</div>
            <div class="typing">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>

      <!-- Input -->
      <div class="chat-input-area">
        <textarea
          v-model="inputText"
          class="chat-input"
          placeholder="Digite sua mensagem..."
          rows="1"
          @keydown.enter.exact.prevent="sendMessage"
          @input="autoResize"
          ref="inputEl"
          :disabled="loading"
        ></textarea>
        <button class="send-btn" @click="sendMessage" :disabled="!inputText.trim() || loading">
          <span v-if="loading" class="spinner" style="width:14px;height:14px;border-width:2px"></span>
          <span v-else>↑</span>
        </button>
      </div>
      <p class="chat-hint">Enter para enviar · Shift+Enter para nova linha</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, computed } from 'vue'
import { chatApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const messages = ref<{ role: string; content: string; time: Date }[]>([])
const inputText = ref('')
const loading = ref(false)
const sessionId = ref('')
const messagesEl = ref<HTMLElement>()
const inputEl = ref<HTMLTextAreaElement>()

const suggestions = computed(() => auth.isCandidate
  ? ['Como melhorar meu currículo?', 'Como me preparar para entrevistas?', 'Quais habilidades estão em alta?']
  : ['Como criar uma boa descrição de vaga?', 'Como avaliar candidatos?', 'Tendências de recrutamento em 2025']
)

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text, time: new Date() })
  inputText.value = ''
  if (inputEl.value) inputEl.value.style.height = 'auto'
  loading.value = true
  await scrollBottom()

  try {
    const res = await chatApi.send(text, sessionId.value)
    sessionId.value = res.data.sessionId
    messages.value.push({ role: 'assistant', content: res.data.response, time: new Date() })
  } catch (e: any) {
    // O catch vazio anterior escondia a causa: qualquer falha virava a mesma
    // frase, e nem o console mostrava o motivo.
    console.error('Falha no chat:', e.response?.status, e.response?.data)
    messages.value.push({ role: 'assistant', content: mensagemDeErro(e), time: new Date() })
  } finally {
    loading.value = false
    await scrollBottom()
  }
}

function sendSuggestion(text: string) {
  inputText.value = text
  sendMessage()
}

function autoResize(e: Event) {
  const el = e.target as HTMLTextAreaElement
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 120) + 'px'
}

async function scrollBottom() {
  await nextTick()
  if (messagesEl.value) messagesEl.value.scrollTop = messagesEl.value.scrollHeight
}

/**
 * Converte a resposta do modelo em HTML seguro para exibicao.
 *
 * A ORDEM IMPORTA: escapamos o HTML ANTES de qualquer coisa. O resultado vai
 * para um v-html, entao sem esse passo bastaria o modelo devolver uma tag para
 * ela ser executada no navegador de quem esta conversando.
 *
 * O prompt ja pede prosa sem titulos, tabelas nem emojis, mas tratamos aqui
 * tambem: o modelo pode escapar da instrucao, e as conversas antigas ficaram
 * salvas no formato anterior.
 */
function formatMessage(text: string) {
  const escapado = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  const semRuido = escapado
    // Emojis e pictogramas
    .replace(/\p{Extended_Pictographic}/gu, '')
    // Linhas divisorias
    .replace(/^\s*[-*_]{3,}\s*$/gm, '')
    // Titulos markdown viram enfase, sem virar secao de documento
    .replace(/^#{1,6}\s*(.+)$/gm, '<strong>$1</strong>')
    .trim()

  const linhas = semRuido.split('\n')
  const partes: string[] = []
  let lista: string[] = []

  const fecharLista = () => {
    if (lista.length) {
      partes.push(`<ul>${lista.map(i => `<li>${i}</li>`).join('')}</ul>`)
      lista = []
    }
  }

  for (const linha of linhas) {
    const item = linha.match(/^\s*(?:[-*•]|\d+\.)\s+(.*)$/)
    if (item) {
      lista.push(inline(item[1]))
    } else if (linha.trim() === '') {
      fecharLista()
    } else {
      fecharLista()
      partes.push(`<p>${inline(linha)}</p>`)
    }
  }
  fecharLista()

  return partes.join('')
}

/** Negrito e italico. Roda depois do escape, entao nao reintroduz risco. */
function inline(s: string) {
  return s
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)/g, '<em>$1</em>')
}

/** Mensagens distintas por causa: cada uma pede uma ação diferente de quem lê. */
function mensagemDeErro(e: any) {
  const status = e.response?.status
  if (status === 401) return 'Sua sessão expirou. Entre novamente para continuar.'
  if (status === 400) return 'Sua mensagem passou do limite de 4.000 caracteres. Tente resumir.'
  if (status === 429) return 'Muitas mensagens em pouco tempo. Aguarde alguns instantes.'
  if (status === 502 || status === 504 || e.code === 'ECONNABORTED') {
    return 'A resposta demorou mais que o esperado e foi interrompida. Tente novamente.'
  }
  if (status >= 500) return 'O servidor falhou ao processar sua mensagem. Tente novamente em instantes.'
  return 'Não consegui responder agora. Verifique sua conexão e tente novamente.'
}

function formatTime(d: Date) {
  return d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  /* Antes era height: calc(100vh - 80px), que assumia ser o unico elemento da
     area de conteudo. Com o rodape abaixo, a soma ultrapassava a viewport e o
     chat ficava cortado. flex:1 faz ele ocupar exatamente o espaco que sobra.
     O min-height:0 e obrigatorio: sem ele um flex item nao encolhe abaixo do
     tamanho do conteudo, e a lista de mensagens deixa de rolar. */
  flex: 1;
  min-height: 0;
  /* Sem margem negativa: o .main-content--full ja entrega a area inteira,
     sem padding para compensar. */
  margin: 0;
  padding: 0;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
  border-radius: 0;
  border-left: none;
  border-right: none;
  border-bottom: none;
  min-height: 0;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 32px 40px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.welcome { text-align: center; margin: auto; max-width: 480px; }
.welcome-title { font-family: var(--font-display); font-size: var(--text-3xl); letter-spacing: -0.02em; margin-bottom: 10px; }
.welcome-body { font-size: var(--text-base); color: var(--text-muted); line-height: 1.7; margin-bottom: 28px; }
.suggestions { display: flex; flex-direction: column; gap: 10px; }
.suggestion {
  background: var(--bg-3); border: 1px solid var(--border);
  color: var(--text-muted); font-family: var(--font-body);
  font-size: var(--text-sm); padding: 12px 18px;
  border-radius: var(--radius); cursor: pointer; text-align: left;
  transition: all 0.15s;
}
.suggestion:hover { border-color: var(--accent); color: var(--text); }

/*
  Sem avatares: quem falou fica claro pelo rotulo de texto e pelo alinhamento.
  Circulos com inicial e simbolo competiam com a leitura sem informar nada que
  o rotulo ja nao diga.
*/
.message { display: flex; }
.message.user { justify-content: flex-end; }

.msg-bubble {
  max-width: 68%;
  background: var(--bg-3);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 14px 18px;
}
.user .msg-bubble { background: var(--accent-dim); border-color: var(--border); }

.msg-author {
  font-size: 10px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-faint);
  margin-bottom: 8px;
}

.msg-text { font-size: var(--text-base); line-height: 1.75; }
.msg-text :deep(p) { margin: 0 0 10px; }
.msg-text :deep(p:last-child) { margin-bottom: 0; }
.msg-text :deep(ul) { margin: 0 0 10px; padding-left: 20px; display: flex; flex-direction: column; gap: 4px; }
.msg-text :deep(ul:last-child) { margin-bottom: 0; }
.msg-text :deep(li)::marker { color: var(--text-faint); }
.msg-text :deep(strong) { font-weight: 600; }
.msg-time { font-size: 10px; color: var(--text-faint); margin-top: 8px; }

.typing { display: flex; gap: 4px; align-items: center; padding: 4px 0; }
.typing span { width: 7px; height: 7px; border-radius: 50%; background: var(--text-muted); animation: bounce 1.2s infinite; }
.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce { 0%, 60%, 100% { transform: translateY(0); } 30% { transform: translateY(-5px); } }

.chat-input-area {
  display: flex; align-items: flex-end; gap: 12px;
  padding: 16px 40px;
  /* Linha de ponta a ponta do painel, igual a do rodape. */
  border-top: 1px solid var(--border);
  background: var(--bg-2);
}

.chat-input {
  flex: 1; font-family: var(--font-body); font-size: var(--text-base);
  background: var(--bg-3); border: 1px solid var(--border);
  border-radius: var(--radius); color: var(--text);
  padding: 12px 16px; resize: none; outline: none;
  transition: border-color 0.15s; line-height: 1.5;
}
.chat-input:focus { border-color: var(--accent); box-shadow: 0 0 0 3px var(--accent-dim); }
.chat-input::placeholder { color: var(--text-faint); }

.send-btn {
  width: 42px; height: 42px; border-radius: var(--radius);
  background: var(--accent); border: none; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; font-weight: 700; color: var(--on-accent);
  flex-shrink: 0; transition: all 0.15s;
}
.send-btn:hover:not(:disabled) { background: var(--accent-hover); transform: scale(1.05); }
.send-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.chat-hint { font-size: 10px; color: var(--text-faint); text-align: center; padding: 6px 0 10px; letter-spacing: 0.05em; background: var(--bg-2); }

@media (max-width: 900px) {
  /* Mesmo motivo da versao desktop: altura fixa somava com o rodape. */
  .chat-page { margin: 0; height: auto; }
  .messages { padding: 20px 16px; }
  .chat-input-area { padding: 12px 16px; }
  .msg-bubble { max-width: 85%; }
}
</style>