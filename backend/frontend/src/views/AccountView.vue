<template>
  <div>
    <div class="page-header">
      <h1>Minha conta</h1>
      <p class="subtitle">Seus dados e preferências</p>
    </div>

    <div class="card dados">
      <div class="linha">
        <span class="rotulo">Nome</span>
        <span class="valor">{{ auth.user?.name || '—' }}</span>
      </div>
      <div class="linha">
        <span class="rotulo">E-mail</span>
        <span class="valor">{{ auth.user?.email || '—' }}</span>
      </div>
      <div class="linha">
        <span class="rotulo">Perfil</span>
        <span class="valor">{{ rotuloPerfil }}</span>
      </div>
    </div>

    <!-- Zona de risco, separada visualmente: ações irreversíveis não devem
         conviver com as comuns. -->
    <section class="zona-risco">
      <h2>Excluir conta</h2>
      <p class="aviso">
        A exclusão é definitiva e não pode ser desfeita. Serão removidos seus
        currículos e análises, suas conversas com a Aria, e
        <template v-if="auth.isCandidate">suas candidaturas — elas deixarão de aparecer para os recrutadores.</template>
        <template v-else>as vagas que você publicou, junto com as candidaturas que recebeu.</template>
      </p>

      <button v-if="!confirmando" class="btn btn-perigo" @click="confirmando = true">
        Quero excluir minha conta
      </button>

      <div v-else class="confirmacao">
        <label class="label" for="confirmar-email">
          Digite <strong>{{ auth.user?.email }}</strong> para confirmar
        </label>
        <input
          id="confirmar-email"
          v-model="confirmacao"
          type="email"
          class="input"
          placeholder="seu@email.com"
          autocomplete="off"
        />

        <div v-if="erro" class="error-box" style="margin-top:12px">{{ erro }}</div>

        <div class="acoes">
          <button class="btn btn-secondary" @click="cancelar" :disabled="excluindo">Cancelar</button>
          <button class="btn btn-perigo" @click="excluir" :disabled="excluindo || !confere">
            <span v-if="excluindo" class="spinner" style="width:14px;height:14px;border-width:2px"></span>
            <span>{{ excluindo ? 'Excluindo...' : 'Excluir definitivamente' }}</span>
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { accountApi } from '@/services/api'

const auth = useAuthStore()
const router = useRouter()

const confirmando = ref(false)
const confirmacao = ref('')
const erro = ref('')
const excluindo = ref(false)

const rotuloPerfil = computed(() => {
  const m: Record<string, string> = {
    CANDIDATE: 'Candidato', RECRUITER: 'Recrutador', ADMIN: 'Administrador'
  }
  return m[auth.user?.role || ''] || auth.user?.role || '—'
})

// O botão só habilita com o e-mail exato: a checagem existe no servidor, mas
// deixar o botão morto até bater evita a frustração de errar e receber erro.
const confere = computed(() =>
  confirmacao.value.trim().toLowerCase() === (auth.user?.email || '').toLowerCase()
)

function cancelar() {
  confirmando.value = false
  confirmacao.value = ''
  erro.value = ''
}

async function excluir() {
  erro.value = ''
  excluindo.value = true
  try {
    await accountApi.excluirConta(confirmacao.value)
    auth.logout()
    router.push('/')
  } catch (e: any) {
    erro.value = e.response?.data?.message || 'Não foi possível excluir a conta. Tente novamente.'
    excluindo.value = false
  }
}
</script>

<style scoped>
.dados { padding: 0; margin-bottom: 40px; }
.linha {
  display: flex; align-items: baseline; gap: 16px;
  padding: 16px 24px; border-bottom: 1px solid var(--border);
}
.linha:last-child { border-bottom: none; }
.rotulo {
  width: 120px; flex-shrink: 0; font-size: var(--text-xs);
  letter-spacing: 0.1em; text-transform: uppercase; color: var(--text-muted);
}
.valor { font-size: var(--text-sm); color: var(--text); word-break: break-word; }

.zona-risco {
  border: 1px solid var(--danger);
  border-radius: var(--radius-lg);
  padding: 28px;
}
.zona-risco h2 { font-size: var(--text-xl); color: var(--danger); margin-bottom: 10px; }
.aviso { font-size: var(--text-sm); color: var(--text-muted); line-height: 1.75; margin-bottom: 22px; }

.confirmacao { display: flex; flex-direction: column; gap: 10px; }
.confirmacao .label { margin-bottom: 2px; text-transform: none; letter-spacing: 0; font-size: var(--text-sm); }
.acoes { display: flex; gap: 10px; margin-top: 6px; flex-wrap: wrap; }

.btn-perigo {
  background: var(--danger); border: 1px solid var(--danger); color: #fff;
  padding: 12px 20px; border-radius: var(--radius); cursor: pointer;
  font-size: var(--text-sm); display: inline-flex; align-items: center; gap: 8px;
}
.btn-perigo:hover:not(:disabled) { opacity: 0.9; }
.btn-perigo:disabled { opacity: 0.45; cursor: not-allowed; }

@media (max-width: 600px) {
  .linha { flex-direction: column; gap: 4px; padding: 14px 18px; }
  .rotulo { width: auto; }
  .zona-risco { padding: 20px; }
}
</style>
