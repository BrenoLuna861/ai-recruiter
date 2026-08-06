<template>
  <!--
    Fundo decorativo das telas publicas.

    aria-hidden e pointer-events:none porque isto e ornamento: leitores de tela
    devem ignorar, e nenhum clique pode ser interceptado por ele.

    A linguagem visual segue a referencia que voce passou: muito espaco negativo,
    geometria discreta, nada competindo com o conteudo. Todas as cores saem das
    variaveis do tema, entao funciona no claro e no escuro sem duplicar nada.
  -->
  <div class="auth-backdrop" aria-hidden="true">
    <svg viewBox="0 0 1440 900" preserveAspectRatio="xMidYMid slice" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <!-- Malha fina, quase imperceptivel: da textura sem virar assunto -->
        <pattern id="malha" width="48" height="48" patternUnits="userSpaceOnUse">
          <path d="M48 0 H0 V48" fill="none" stroke="currentColor" stroke-width="0.5" opacity="0.055" />
        </pattern>

        <radialGradient id="brilho" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="var(--accent)" stop-opacity="0.10" />
          <stop offset="100%" stop-color="var(--accent)" stop-opacity="0" />
        </radialGradient>

        <radialGradient id="brilho2" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="var(--blue)" stop-opacity="0.07" />
          <stop offset="100%" stop-color="var(--blue)" stop-opacity="0" />
        </radialGradient>
      </defs>

      <rect width="1440" height="900" fill="url(#malha)" />

      <!-- Dois halos amplos e difusos, nos cantos opostos -->
      <circle cx="1180" cy="130" r="420" fill="url(#brilho)" />
      <circle cx="200" cy="820" r="380" fill="url(#brilho2)" />

      <!-- Arcos concentricos: eco do aro da lupa da marca -->
      <g fill="none" stroke="currentColor" stroke-linecap="round">
        <circle cx="1180" cy="130" r="150" stroke-width="1" opacity="0.10" />
        <circle cx="1180" cy="130" r="230" stroke-width="1" opacity="0.06" />
        <circle cx="200" cy="820" r="120" stroke-width="1" opacity="0.08" />
      </g>

      <!-- Nos de conexao, referencia ao cruzamento de perfis e vagas -->
      <g class="nos" fill="var(--accent)">
        <circle cx="1180" cy="130" r="3" opacity="0.5" />
        <circle cx="1030" cy="130" r="2.5" opacity="0.35" />
        <circle cx="1180" cy="360" r="2.5" opacity="0.3" />
        <circle cx="1310" cy="52" r="2" opacity="0.25" />
        <circle cx="200" cy="700" r="2.5" opacity="0.3" />
        <circle cx="320" cy="820" r="2" opacity="0.25" />
      </g>

      <g stroke="var(--accent)" stroke-width="0.75" opacity="0.14">
        <line x1="1180" y1="130" x2="1030" y2="130" />
        <line x1="1180" y1="130" x2="1180" y2="360" />
        <line x1="1180" y1="130" x2="1310" y2="52" />
        <line x1="200" y1="820" x2="200" y2="700" />
        <line x1="200" y1="820" x2="320" y2="820" />
      </g>
    </svg>
  </div>
</template>

<style scoped>
.auth-backdrop {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
  color: var(--text);
}

.auth-backdrop svg {
  width: 100%;
  height: 100%;
  display: block;
}

/* Respiracao muito lenta nos nos. Quem prefere menos movimento nao ve nada. */
.nos circle {
  animation: pulsa 5s ease-in-out infinite;
  transform-origin: center;
}
.nos circle:nth-child(2) { animation-delay: 0.9s; }
.nos circle:nth-child(3) { animation-delay: 1.8s; }
.nos circle:nth-child(4) { animation-delay: 2.7s; }
.nos circle:nth-child(5) { animation-delay: 3.4s; }
.nos circle:nth-child(6) { animation-delay: 4.1s; }

@keyframes pulsa {
  0%, 100% { opacity: 0.18; }
  50%      { opacity: 0.55; }
}

@media (prefers-reduced-motion: reduce) {
  .nos circle { animation: none; }
}
</style>
