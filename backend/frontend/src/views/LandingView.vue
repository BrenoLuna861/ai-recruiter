<template>
  <div class="landing">
    <!-- Top bar -->
    <header class="topbar">
      <div class="brand">
        <span class="brand-mark">✦</span>
        <span class="brand-text">AI Recruiter</span>
      </div>
      <nav class="topbar-nav">
        <RouterLink to="/login" class="link">Entrar</RouterLink>
        <RouterLink to="/register" class="btn btn-primary btn-sm">Criar conta</RouterLink>
      </nav>
    </header>

    <!-- Hero -->
    <section class="hero">
      <!-- AI Core: ilustração SVG animada (rede neural minimalista) -->
      <div class="ai-core" aria-hidden="true">
        <svg viewBox="0 0 320 320" xmlns="http://www.w3.org/2000/svg">
          <!-- Anéis de pulso (radar) -->
          <circle class="ring ring-1" cx="160" cy="160" r="40" />
          <circle class="ring ring-2" cx="160" cy="160" r="60" />
          <circle class="ring ring-3" cx="160" cy="160" r="80" />

          <!-- Linhas de conexão -->
          <g class="links">
            <line x1="160" y1="160" x2="60"  y2="60"  />
            <line x1="160" y1="160" x2="260" y2="60"  />
            <line x1="160" y1="160" x2="40"  y2="160" />
            <line x1="160" y1="160" x2="280" y2="160" />
            <line x1="160" y1="160" x2="60"  y2="260" />
            <line x1="160" y1="160" x2="260" y2="260" />
            <line x1="160" y1="160" x2="160" y2="40"  />
            <line x1="160" y1="160" x2="160" y2="280" />
          </g>

          <!-- Pacotes de dados (pontos fluindo nas linhas) -->
          <g class="packets">
            <circle r="2.5" cx="0" cy="0">
              <animateMotion dur="3s" repeatCount="indefinite"
                path="M 160 160 L 60 60 L 160 160" />
            </circle>
            <circle r="2.5" cx="0" cy="0">
              <animateMotion dur="3.6s" repeatCount="indefinite" begin="0.4s"
                path="M 160 160 L 260 60 L 160 160" />
            </circle>
            <circle r="2.5" cx="0" cy="0">
              <animateMotion dur="2.8s" repeatCount="indefinite" begin="0.8s"
                path="M 160 160 L 40 160 L 160 160" />
            </circle>
            <circle r="2.5" cx="0" cy="0">
              <animateMotion dur="3.2s" repeatCount="indefinite" begin="1.2s"
                path="M 160 160 L 280 160 L 160 160" />
            </circle>
            <circle r="2.5" cx="0" cy="0">
              <animateMotion dur="3.4s" repeatCount="indefinite" begin="1.6s"
                path="M 160 160 L 60 260 L 160 160" />
            </circle>
            <circle r="2.5" cx="0" cy="0">
              <animateMotion dur="2.6s" repeatCount="indefinite" begin="2s"
                path="M 160 160 L 260 260 L 160 160" />
            </circle>
          </g>

          <!-- Nós satélite -->
          <g class="nodes">
            <circle cx="60"  cy="60"  r="6" class="node node-a" />
            <circle cx="260" cy="60"  r="6" class="node node-b" />
            <circle cx="40"  cy="160" r="6" class="node node-c" />
            <circle cx="280" cy="160" r="6" class="node node-d" />
            <circle cx="60"  cy="260" r="6" class="node node-e" />
            <circle cx="260" cy="260" r="6" class="node node-f" />
            <circle cx="160" cy="40"  r="6" class="node node-g" />
            <circle cx="160" cy="280" r="6" class="node node-h" />
          </g>

          <!-- Núcleo central -->
          <circle class="core-glow" cx="160" cy="160" r="22" />
          <circle class="core" cx="160" cy="160" r="11" />
          <circle class="core-dot" cx="160" cy="160" r="3.5" />
        </svg>
      </div>

      <div class="eyebrow">PLATAFORMA INTELIGENTE DE RECRUTAMENTO</div>
      <h1>
        Recrutamento <span class="accent">orientado por IA</span><br />
        para decisões mais assertivas.
      </h1>
      <p class="subtitle">
        O AI Recruiter analisa currículos automaticamente, cruza perfis com
        requisitos de vagas e sugere os candidatos mais aderentes em segundos.
      </p>

      <div class="hero-cta">
        <RouterLink to="/register" class="btn btn-primary btn-lg">
          Começar agora
          <span class="arrow">→</span>
        </RouterLink>
        <RouterLink to="/login" class="btn btn-ghost btn-lg">
          Já tenho conta
        </RouterLink>
      </div>

      <div class="hero-meta">
        <span class="dot"></span>
        Cadastro gratuito · Login com Google disponível
      </div>
    </section>

    <!-- Features -->
    <section class="features">
      <div class="feature-card">
        <div class="feature-icon">▤</div>
        <h3>Análise automática de currículos</h3>
        <p>
          Extração de competências, experiências e qualificações em segundos,
          com score de aderência por vaga.
        </p>
      </div>
      <div class="feature-card">
        <div class="feature-icon">◈</div>
        <h3>Agente IA conversacional</h3>
        <p>
          Converse com o headhunter virtual para refinar buscas, comparar
          candidatos e priorizar shortlists.
        </p>
      </div>
      <div class="feature-card">
        <div class="feature-icon">◉</div>
        <h3>Matching inteligente de vagas</h3>
        <p>
          Algoritmos cruzam perfis e requisitos para sugerir os profissionais
          mais alinhados a cada oportunidade.
        </p>
      </div>
    </section>

    <!-- Personas -->
    <section class="personas">
      <div class="persona">
        <div class="persona-tag">CANDIDATOS</div>
        <h2>Encontre vagas que realmente combinam</h2>
        <p>
          Faça upload do seu currículo, receba um score detalhado e descubra
          oportunidades onde seu perfil se destaca.
        </p>
        <RouterLink to="/register" class="btn btn-ghost">Sou candidato →</RouterLink>
      </div>
      <div class="persona">
        <div class="persona-tag">RECRUTADORES</div>
        <h2>Triagem mais rápida, contratações mais assertivas</h2>
        <p>
          Publique vagas e veja rankings automáticos de candidatos com base em
          aderência real às suas necessidades.
        </p>
        <RouterLink to="/register" class="btn btn-ghost">Sou recrutador →</RouterLink>
      </div>
    </section>

    <!-- Footer CTA -->
    <section class="cta-final">
      <h2>Pronto para modernizar seu processo seletivo?</h2>
      <div class="hero-cta">
        <RouterLink to="/register" class="btn btn-primary btn-lg">Criar conta gratuita</RouterLink>
        <RouterLink to="/login" class="btn btn-ghost btn-lg">Entrar</RouterLink>
      </div>
    </section>

    <footer class="footer">
      <div class="footer-row">
        <span>© {{ new Date().getFullYear() }} AI Recruiter</span>
        <span class="footer-sep">·</span>
        <span>Plataforma Inteligente de Recrutamento</span>
      </div>
      <div class="footer-row footer-credit">
        Desenvolvido por <span class="credit-name">Breno Luna</span>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
// Página pública de abertura — não requer autenticação.
</script>

<style scoped>
.landing {
  min-height: 100vh;
  background:
    radial-gradient(1200px 600px at 80% -10%, rgba(110, 231, 183, 0.08), transparent 60%),
    radial-gradient(900px 500px at -10% 20%, rgba(96, 165, 250, 0.06), transparent 60%),
    var(--bg);
  color: var(--text);
}

/* Top bar */
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 40px;
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  background: rgba(10, 10, 11, 0.7);
  backdrop-filter: blur(8px);
  z-index: 50;
}
.brand { display: flex; align-items: center; gap: 8px; }
.brand-mark { color: var(--accent); font-size: 18px; }
.brand-text { font-family: var(--font-display); font-size: 1.1rem; letter-spacing: -0.02em; }
.topbar-nav { display: flex; align-items: center; gap: 16px; }
.link {
  color: var(--text-muted);
  text-decoration: none;
  font-size: var(--text-sm);
  transition: color 0.15s;
}
.link:hover { color: var(--text); }
.btn-sm { padding: 8px 14px; font-size: var(--text-xs); }

/* Hero */
.hero {
  max-width: 980px;
  margin: 0 auto;
  text-align: center;
  padding: 56px 24px 64px;
}

/* AI Core animation */
.ai-core {
  width: 280px;
  height: 280px;
  margin: 0 auto 40px;
  position: relative;
  filter: drop-shadow(0 0 30px rgba(110, 231, 183, 0.15));
}
.ai-core svg { width: 100%; height: 100%; display: block; }

.ring {
  fill: none;
  stroke: var(--accent);
  stroke-width: 1;
  opacity: 0;
  transform-origin: 160px 160px;
  animation: ring-pulse 4s ease-out infinite;
}
.ring-2 { animation-delay: 1.3s; }
.ring-3 { animation-delay: 2.6s; }

@keyframes ring-pulse {
  0%   { opacity: 0; transform: scale(0.6); }
  20%  { opacity: 0.6; }
  100% { opacity: 0; transform: scale(1.4); }
}

.links line {
  stroke: var(--accent);
  stroke-width: 0.6;
  opacity: 0.18;
  stroke-dasharray: 4 6;
  animation: line-flow 6s linear infinite;
}

@keyframes line-flow {
  to { stroke-dashoffset: -100; }
}

.packets circle {
  fill: var(--accent);
  filter: drop-shadow(0 0 6px var(--accent));
}

.nodes .node {
  fill: var(--bg-3);
  stroke: var(--accent);
  stroke-width: 1.3;
  animation: node-pulse 2.4s ease-in-out infinite;
}
.nodes .node-a { animation-delay: 0s; }
.nodes .node-b { animation-delay: 0.3s; }
.nodes .node-c { animation-delay: 0.6s; }
.nodes .node-d { animation-delay: 0.9s; }
.nodes .node-e { animation-delay: 1.2s; }
.nodes .node-f { animation-delay: 1.5s; }
.nodes .node-g { animation-delay: 1.8s; }
.nodes .node-h { animation-delay: 2.1s; }

@keyframes node-pulse {
  0%, 100% { fill: var(--bg-3); r: 4; }
  50%      { fill: var(--accent); r: 6.5; }
}

.core-glow {
  fill: var(--accent);
  opacity: 0.12;
  animation: glow-pulse 2s ease-in-out infinite;
  transform-origin: 160px 160px;
}
.core {
  fill: var(--accent);
  opacity: 0.85;
}
.core-dot {
  fill: #0a0a0b;
  animation: dot-pulse 1.5s ease-in-out infinite;
}

@keyframes glow-pulse {
  0%, 100% { opacity: 0.10; transform: scale(1); }
  50%      { opacity: 0.22; transform: scale(1.15); }
}
@keyframes dot-pulse {
  0%, 100% { opacity: 1; }
  50%      { opacity: 0.4; }
}

.eyebrow {
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--text-muted);
  margin-bottom: 24px;
}
.hero h1 {
  font-size: clamp(2.4rem, 5vw, 3.6rem);
  line-height: 1.1;
  letter-spacing: -0.035em;
  margin-bottom: 24px;
}
.hero h1 .accent { color: var(--accent); font-style: italic; }
.subtitle {
  font-size: var(--text-lg);
  color: var(--text-muted);
  max-width: 640px;
  margin: 0 auto 36px;
  line-height: 1.6;
}
.hero-cta {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
  margin-bottom: 24px;
}
.btn-lg { padding: 14px 28px; font-size: var(--text-sm); }
.arrow { transition: transform 0.2s var(--ease); }
.btn-primary:hover .arrow { transform: translateX(3px); }
.hero-meta {
  font-size: var(--text-xs);
  color: var(--text-muted);
  letter-spacing: 0.06em;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
  display: inline-block;
  box-shadow: 0 0 10px var(--accent);
  animation: dot-blink 1.6s ease-in-out infinite;
}
@keyframes dot-blink {
  0%, 100% { opacity: 1; }
  50%      { opacity: 0.3; }
}

/* Features */
.features {
  max-width: 1100px;
  margin: 0 auto;
  padding: 32px 24px 80px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}
.feature-card {
  background: var(--bg-2);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 28px;
  transition: border-color 0.2s var(--ease), transform 0.2s var(--ease);
}
.feature-card:hover { border-color: var(--accent); transform: translateY(-2px); }
.feature-icon {
  font-size: 22px;
  color: var(--accent);
  margin-bottom: 16px;
}
.feature-card h3 {
  font-size: var(--text-xl);
  margin-bottom: 10px;
  font-family: var(--font-display);
}
.feature-card p {
  font-size: var(--text-sm);
  color: var(--text-muted);
  line-height: 1.6;
}

/* Personas */
.personas {
  max-width: 1100px;
  margin: 0 auto;
  padding: 32px 24px 80px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 24px;
}
.persona {
  background: var(--bg-2);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 36px;
}
.persona-tag {
  font-size: 10px;
  letter-spacing: 0.16em;
  color: var(--accent);
  margin-bottom: 16px;
}
.persona h2 {
  font-size: var(--text-2xl);
  margin-bottom: 12px;
}
.persona p {
  font-size: var(--text-sm);
  color: var(--text-muted);
  line-height: 1.7;
  margin-bottom: 20px;
}

/* Final CTA */
.cta-final {
  max-width: 720px;
  margin: 0 auto;
  text-align: center;
  padding: 64px 24px 96px;
}
.cta-final h2 {
  font-size: var(--text-3xl);
  margin-bottom: 28px;
}

/* Footer */
.footer {
  border-top: 1px solid var(--border);
  padding: 24px 40px;
  text-align: center;
  font-size: var(--text-xs);
  color: var(--text-muted);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.footer-row {
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}
.footer-sep { opacity: 0.5; }
.footer-credit {
  font-size: 11px;
  letter-spacing: 0.05em;
  color: var(--text-faint);
  margin-top: 2px;
}
.credit-name {
  color: var(--accent);
  font-weight: 600;
  letter-spacing: 0.02em;
}

@media (max-width: 600px) {
  .topbar { padding: 16px 20px; }
  .hero { padding: 40px 20px 40px; }
  .ai-core { width: 220px; height: 220px; margin-bottom: 28px; }
  .feature-card, .persona { padding: 24px; }
}
</style>
