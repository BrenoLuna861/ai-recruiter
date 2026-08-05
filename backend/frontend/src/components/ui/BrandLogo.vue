<template>
  <svg
    :viewBox="variant === 'mark' ? '0 0 104 80' : '0 0 300 80'"
    :class="['brand-logo', `is-${variant}`]"
    xmlns="http://www.w3.org/2000/svg"
    role="img"
    :aria-label="title"
  >
    <title>{{ title }}</title>

    <!-- ===== Simbolo: lupa + trilhas de circuito ===== -->
    <g class="mark" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round">
      <!-- Trilhas de circuito entrando pela esquerda -->
      <g stroke-width="2.4">
        <circle cx="6" cy="27" r="3.4" />
        <path d="M9.4 27 h10 l5 5 h6" />

        <circle cx="3" cy="40" r="3.4" />
        <path d="M6.4 40 h22" />

        <circle cx="6" cy="53" r="3.4" />
        <path d="M9.4 53 h10 l5 -5 h6" />
      </g>

      <!-- Aro da lupa -->
      <circle cx="60" cy="40" r="26" stroke-width="5" />

      <!-- Cabo -->
      <path d="M78.5 58.5 L92 72" stroke-width="8" />

      <!-- Letra A -->
      <path d="M48 53 L58 27 L68 53" stroke-width="4.6" />
      <path d="M52.2 45 H63.8" stroke-width="4.6" />

      <!-- Letra i: haste + ponto (a "cabeca" da pessoa) -->
      <path d="M74.5 38.5 V53" stroke-width="4.6" />
      <circle cx="74.5" cy="30" r="3.6" fill="currentColor" stroke="none" />
    </g>

    <!-- ===== Wordmark ===== -->
    <text
      v-if="variant === 'full'"
      class="wordmark"
      x="112"
      y="53"
      font-size="34"
      letter-spacing="-0.5"
    >Ai-Recruiter</text>
  </svg>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  /** 'full' = simbolo + wordmark | 'mark' = so o simbolo */
  variant?: 'full' | 'mark'
  title?: string
}>(), {
  variant: 'full',
  title: 'AI Recruiter'
})
</script>

<style scoped>
/*
  Sem width/height fixos: o tamanho vem da altura definida por quem usa, e o
  viewBox cuida do resto. Assim a marca fica nitida em qualquer densidade de
  tela, do celular ao monitor 4K — que era o ponto de ser SVG.
*/
.brand-logo {
  display: block;
  height: 100%;
  width: auto;
  overflow: visible;
}

/* O simbolo usa a cor de destaque; o texto acompanha a cor do contexto,
   entao a marca funciona igual no tema claro e no escuro. */
.mark { color: var(--accent); }

.wordmark {
  fill: var(--text);
  font-family: var(--font-body), system-ui, sans-serif;
  font-weight: 600;
}
</style>
