<template>
  <div class="campo-senha">
    <div class="label-row">
      <label class="label" :for="id">{{ label }}</label>
      <slot name="acao" />
    </div>

    <div class="input-wrap">
      <input
        :id="id"
        :type="visivel ? 'text' : 'password'"
        class="input"
        :placeholder="placeholder"
        :required="required"
        :minlength="minlength"
        :autocomplete="autocomplete"
        :value="modelValue"
        @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      />
      <button
        type="button"
        class="olho"
        :aria-label="visivel ? 'Ocultar senha' : 'Mostrar senha'"
        :title="visivel ? 'Ocultar senha' : 'Mostrar senha'"
        @click="visivel = !visivel"
      >
        <!-- Olho aberto quando oculto, olho riscado quando visível: o ícone
             mostra o que o clique VAI fazer, não o estado atual. -->
        <svg v-if="!visivel" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M1.5 12S5 5.5 12 5.5 22.5 12 22.5 12 19 18.5 12 18.5 1.5 12 1.5 12z"/>
          <circle cx="12" cy="12" r="3.2"/>
        </svg>
        <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M9.9 5.7A9.9 9.9 0 0 1 12 5.5c7 0 10.5 6.5 10.5 6.5a17 17 0 0 1-3.2 4"/>
          <path d="M6.3 7.4A16.7 16.7 0 0 0 1.5 12S5 18.5 12 18.5a10 10 0 0 0 4.2-.9"/>
          <path d="M9.9 9.9a3.2 3.2 0 0 0 4.4 4.4"/>
          <path d="M3 3l18 18"/>
        </svg>
      </button>
    </div>

    <!-- Medidor só no cadastro e na redefinição: em tela de login, avaliar a
         força da senha que a pessoa já tem não serve para nada. -->
    <div v-if="mostrarForca && modelValue" class="forca">
      <div class="barras">
        <span v-for="n in 4" :key="n" :class="['barra', n <= forca.nivel ? forca.classe : '']"></span>
      </div>
      <p class="forca-texto" :class="forca.classe">
        {{ forca.rotulo }}<span v-if="forca.dica"> · {{ forca.dica }}</span>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const props = withDefaults(defineProps<{
  modelValue: string
  label?: string
  placeholder?: string
  id?: string
  required?: boolean
  minlength?: number
  autocomplete?: string
  mostrarForca?: boolean
}>(), {
  label: 'Senha',
  placeholder: '••••••••',
  id: 'senha',
  required: true,
  minlength: 8,
  autocomplete: 'current-password',
  mostrarForca: false
})

defineEmits<{ (e: 'update:modelValue', valor: string): void }>()

const visivel = ref(false)

/*
  Medidor simples, por critérios somados. Não é entropia de verdade — para isso
  existem bibliotecas como a zxcvbn, que pesam listas de senhas vazadas. Aqui o
  objetivo é orientar enquanto a pessoa digita, e a dica diz o que falta em vez
  de só dar uma nota.
*/
const forca = computed(() => {
  const s = props.modelValue || ''
  let pontos = 0
  const faltas: string[] = []

  if (s.length >= 8) pontos++; else faltas.push('use ao menos 8 caracteres')
  if (s.length >= 12) pontos++
  if (/[a-z]/.test(s) && /[A-Z]/.test(s)) pontos++; else faltas.push('misture maiúsculas e minúsculas')
  if (/\d/.test(s)) pontos++; else faltas.push('inclua um número')
  if (/[^A-Za-z0-9]/.test(s)) pontos++; else faltas.push('inclua um símbolo')

  // Sequências óbvias derrubam a nota, por mais longa que seja a senha.
  if (/^(.)\1+$/.test(s) || /12345|senha|password|qwerty/i.test(s)) {
    pontos = Math.min(pontos, 1)
    faltas.unshift('evite sequências e palavras comuns')
  }

  const nivel = Math.min(4, Math.max(1, pontos - 1))
  const rotulos = ['', 'Fraca', 'Razoável', 'Boa', 'Forte']
  const classes = ['', 'fraca', 'razoavel', 'boa', 'forte']

  return {
    nivel,
    rotulo: rotulos[nivel],
    classe: classes[nivel],
    dica: nivel < 4 ? faltas[0] : ''
  }
})
</script>

<style scoped>
.campo-senha { display: flex; flex-direction: column; }
.label-row { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; }

.input-wrap { position: relative; }
.input-wrap .input { width: 100%; padding-right: 44px; }

.olho {
  position: absolute; top: 50%; right: 6px; transform: translateY(-50%);
  display: inline-flex; align-items: center; justify-content: center;
  width: 32px; height: 32px; padding: 0;
  background: none; border: none; border-radius: var(--radius);
  color: var(--text-muted); cursor: pointer;
  transition: color 0.15s var(--ease), background 0.15s var(--ease);
}
.olho:hover { color: var(--text); background: var(--bg-3); }
.olho:focus-visible { outline: 2px solid var(--accent); outline-offset: 1px; }

.forca { margin-top: 8px; }
.barras { display: flex; gap: 4px; margin-bottom: 6px; }
.barra { flex: 1; height: 3px; border-radius: 2px; background: var(--border-2); transition: background 0.25s var(--ease); }
.barra.fraca { background: var(--danger); }
.barra.razoavel { background: var(--warning); }
.barra.boa { background: var(--blue); }
.barra.forte { background: var(--accent); }

.forca-texto { font-size: 11px; line-height: 1.5; margin: 0; color: var(--text-muted); }
.forca-texto.fraca { color: var(--danger); }
.forca-texto.razoavel { color: var(--warning); }
.forca-texto.boa { color: var(--blue); }
.forca-texto.forte { color: var(--accent); }
</style>
