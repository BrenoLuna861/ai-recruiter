<template>
  <!-- Variante compacta: barra fina fixa no rodape da viewport, usada nas telas
       internas. Nao empurra conteudo nem cria scroll — o .main-content reserva
       a altura dela via padding-bottom. -->
  <footer v-if="variant === 'compact'" class="app-footer is-compact">
    <span class="compact-copy">© {{ year }} AI Recruiter</span>
    <nav class="compact-links">
      <a :href="mailUrl" target="_blank" rel="noopener noreferrer">{{ email }}</a>
      <a
        v-for="s in activeSocials"
        :key="s.label"
        :href="s.url"
        target="_blank"
        rel="noopener noreferrer"
        :aria-label="s.label"
        :title="s.label"
        class="compact-social"
      ><span v-html="s.icon"></span></a>
    </nav>
    <span class="compact-credit">Desenvolvido por <span class="credit-name">Breno Luna</span></span>
  </footer>

  <footer v-else class="app-footer">
    <div class="footer-inner">
      <!-- Marca -->
      <div class="footer-brand">
        <RouterLink to="/" class="brand-link" aria-label="AI Recruiter — página inicial">
          <BrandLogo variant="full" style="height: 30px;" />
        </RouterLink>
        <p class="brand-tagline">Plataforma Inteligente de Recrutamento</p>
      </div>

      <!-- Navegacao -->
      <nav class="footer-nav" aria-label="Links do rodapé">
        <span class="nav-title">Navegação</span>
        <RouterLink to="/jobs">Vagas</RouterLink>
        <RouterLink v-if="!auth.isAuthenticated" to="/login">Entrar</RouterLink>
        <RouterLink v-if="!auth.isAuthenticated" to="/register">Criar conta</RouterLink>
        <RouterLink v-if="auth.isAuthenticated" to="/dashboard">Dashboard</RouterLink>
        <RouterLink v-if="auth.isAuthenticated" to="/chat">Agente IA</RouterLink>
      </nav>

      <!-- Contato -->
      <div class="footer-contact">
        <span class="nav-title">Contato</span>
        <a :href="mailUrl" target="_blank" rel="noopener noreferrer">{{ email }}</a>

        <!-- TODO: preencher com as redes sociais reais.
             Cada item com url vazia simplesmente nao e renderizado, entao da
             para ir preenchendo aos poucos sem quebrar o layout. -->
        <div class="social-row">
          <a
            v-for="s in activeSocials"
            :key="s.label"
            :href="s.url"
            class="social-link"
            target="_blank"
            rel="noopener noreferrer"
            :aria-label="s.label"
            :title="s.label"
          >
            <span v-html="s.icon"></span>
          </a>
        </div>
      </div>
    </div>

    <div class="footer-bottom">
      <span>© {{ year }} AI Recruiter. Todos os direitos reservados.</span>
      <span class="footer-credit">
        Desenvolvido por <span class="credit-name">Breno Luna</span>
      </span>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import BrandLogo from '@/components/ui/BrandLogo.vue'

withDefaults(defineProps<{
  /** 'full' = tres colunas (telas publicas) | 'compact' = barra fixa (telas internas) */
  variant?: 'full' | 'compact'
}>(), { variant: 'full' })

const auth = useAuthStore()
const year = new Date().getFullYear()
const email = 'valenexo05@gmail.com'

/*
  Abre a janela de composicao do Gmail ja com destinatario e assunto.
  Preferi isto a um mailto: puro porque o mailto depende de haver um cliente de
  e-mail configurado na maquina — e quando nao ha, o clique simplesmente nao faz
  nada, que era o comportamento reclamado. O link do Gmail funciona no navegador.
*/
const mailUrl = computed(() => {
  const params = new URLSearchParams({
    view: 'cm',
    fs: '1',
    to: email,
    su: 'Contato pelo site AI Recruiter'
  })
  return `https://mail.google.com/mail/?${params.toString()}`
})

// Deixe a url vazia enquanto nao tiver o link — o item some do rodape sozinho.
// (E assim que o WhatsApp entra depois: basta preencher a url.)
const socials = [
  {
    label: 'LinkedIn',
    url: 'https://www.linkedin.com/in/br%C3%AA/',
    icon: `<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" aria-hidden="true"><path d="M4.98 3.5a2.5 2.5 0 1 1 0 5 2.5 2.5 0 0 1 0-5zM3 9h4v12H3zM9 9h3.8v1.7h.05c.53-1 1.83-2.05 3.77-2.05C20.3 8.65 21 11 21 14.1V21h-4v-6.1c0-1.45-.03-3.32-2.02-3.32-2.02 0-2.33 1.58-2.33 3.21V21H9z"/></svg>`
  },
  {
    label: 'Instagram',
    url: 'https://www.instagram.com/valenexoo/',
    icon: `<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><rect x="2" y="2" width="20" height="20" rx="5"/><circle cx="12" cy="12" r="4"/><circle cx="17.5" cy="6.5" r="1.2" fill="currentColor" stroke="none"/></svg>`
  },
  {
    label: 'WhatsApp',
    url: '', // preencher com https://wa.me/55DDDNUMERO
    icon: `<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" aria-hidden="true"><path d="M12.04 2C6.58 2 2.13 6.45 2.13 11.91c0 1.75.46 3.45 1.32 4.95L2 22l5.25-1.38a9.9 9.9 0 0 0 4.79 1.22h.01c5.46 0 9.91-4.45 9.91-9.91 0-2.65-1.03-5.14-2.9-7.01A9.82 9.82 0 0 0 12.04 2zm0 18.15h-.01a8.2 8.2 0 0 1-4.19-1.15l-.3-.18-3.12.82.83-3.04-.2-.31a8.18 8.18 0 0 1-1.26-4.38c0-4.54 3.7-8.24 8.25-8.24 2.2 0 4.27.86 5.83 2.42a8.19 8.19 0 0 1 2.41 5.83c0 4.54-3.7 8.23-8.24 8.23zm4.52-6.16c-.25-.13-1.47-.72-1.69-.81-.23-.08-.39-.12-.56.13-.16.24-.64.8-.78.97-.15.16-.29.18-.53.06-.25-.13-1.05-.39-1.99-1.23-.74-.66-1.23-1.47-1.38-1.72-.14-.25-.01-.38.11-.5.11-.11.25-.29.37-.44.13-.15.17-.25.25-.41.08-.17.04-.31-.02-.44-.06-.12-.56-1.34-.76-1.84-.2-.48-.4-.42-.56-.43h-.47c-.16 0-.43.06-.65.31-.23.25-.86.84-.86 2.05s.88 2.38 1 2.54c.13.17 1.74 2.65 4.21 3.71.59.26 1.05.41 1.4.52.59.19 1.13.16 1.55.1.48-.07 1.47-.6 1.67-1.18.21-.58.21-1.08.15-1.18-.06-.11-.23-.17-.48-.29z"/></svg>`
  }
]

const activeSocials = computed(() => socials.filter(s => s.url))
</script>

<style scoped>
.app-footer {
  border-top: 1px solid var(--border);
  background: var(--bg-2);
  color: var(--text-muted);
  font-size: var(--text-sm);
  /* margin-top: auto faz o rodape colar no fim mesmo em paginas curtas,
     desde que o container pai seja flex column com min-height: 100vh. */
  margin-top: auto;
}

/* ---------- variante compacta: barra fixa ---------- */
.app-footer.is-compact {
  position: fixed;
  bottom: 0;
  left: var(--sidebar-w);
  right: 0;
  z-index: 60;
  height: var(--footer-h);
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 24px;
  font-size: var(--text-xs);
  color: var(--text-faint);
  background: var(--bg-overlay);
  backdrop-filter: blur(8px);
}

.compact-links { display: flex; align-items: center; gap: 14px; }
.compact-links a { color: var(--text-muted); text-decoration: none; transition: color 0.15s var(--ease); }
.compact-links a:hover { color: var(--accent); }
.compact-social { display: inline-flex; align-items: center; }
.compact-social :deep(svg) { width: 15px; height: 15px; }
.compact-credit { white-space: nowrap; }

@media (max-width: 900px) {
  .app-footer.is-compact { left: 0; padding: 0 14px; gap: 10px; }
  .compact-credit { display: none; }
}

@media (max-width: 600px) {
  .app-footer.is-compact { justify-content: center; }
  .compact-copy { display: none; }
}

.footer-inner {
  max-width: 1080px;
  margin: 0 auto;
  padding: 40px 32px 28px;
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: 40px;
}

/* ----- marca ----- */
.brand-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: var(--text);
}
.brand-mark { color: var(--accent); font-size: 16px; }
.brand-text { font-weight: 600; font-size: var(--text-base); }
.brand-tagline {
  margin-top: 8px;
  font-size: var(--text-xs);
  color: var(--text-muted);
  max-width: 34ch;
  line-height: 1.6;
}

/* ----- colunas de links ----- */
.nav-title {
  display: block;
  font-size: 10px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--text-faint);
  margin-bottom: 12px;
}

.footer-nav,
.footer-contact {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.footer-nav a,
.footer-contact a {
  color: var(--text-muted);
  text-decoration: none;
  font-size: var(--text-xs);
  width: fit-content;
  transition: color 0.15s var(--ease);
}
.footer-nav a:hover,
.footer-contact a:hover { color: var(--accent); }

/* ----- redes sociais ----- */
.social-row { display: flex; gap: 10px; margin-top: 6px; }

.social-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--border);
  border-radius: 50%;
  color: var(--text-muted);
  transition: color 0.15s var(--ease), border-color 0.15s var(--ease), transform 0.15s var(--ease);
}
.social-link:hover {
  color: var(--accent);
  border-color: var(--accent);
  transform: translateY(-2px);
}
.social-link:focus-visible { outline: 2px solid var(--accent); outline-offset: 2px; }

/* ----- barra inferior ----- */
/*
  A linha vem de um ::before, e nao de border-top.

  A borda acompanharia a caixa inteira do elemento, padding incluso, ficando
  32px mais larga de cada lado que o texto que ela separa — desalinhada com a
  logo acima e com o copyright abaixo. O pseudo-elemento e recuado exatamente
  pelo mesmo padding, entao a linha comeca e termina onde o conteudo comeca e
  termina.
*/
.footer-bottom {
  position: relative;
  padding: 16px 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  max-width: 1080px;
  margin: 0 auto;
  font-size: var(--text-xs);
  color: var(--text-faint);
}
.footer-bottom::before {
  content: '';
  position: absolute;
  top: 0;
  left: 32px;
  right: 32px;
  height: 1px;
  background: var(--border);
}

.footer-credit { letter-spacing: 0.04em; }
.credit-name { color: var(--text-muted); }

@media (max-width: 800px) {
  .footer-inner {
    grid-template-columns: 1fr;
    gap: 28px;
    padding: 32px 24px 24px;
  }
  .footer-bottom {
    flex-direction: column;
    text-align: center;
    padding: 16px 24px;
  }
  /* Acompanha o padding menor do mobile, para a linha continuar alinhada. */
  .footer-bottom::before { left: 24px; right: 24px; }
}
</style>
