<template>
  <div class="app-layout" v-if="auth.isAuthenticated">
    <!-- Mobile top bar (visivel apenas <= 900px) -->
    <header class="mobile-bar">
      <button class="hamburger" @click="menuOpen = !menuOpen"
              :aria-label="menuOpen ? 'Fechar menu' : 'Abrir menu'"
              :class="{ active: menuOpen }">
        <span></span><span></span><span></span>
      </button>
      <div class="mobile-brand">
        <BrandLogo variant="full" style="height: 26px;" />
      </div>
    </header>

    <!-- Backdrop atras da sidebar quando aberta no mobile -->
    <div v-if="menuOpen" class="sidebar-backdrop" @click="menuOpen = false"></div>

    <AppSidebar :open="menuOpen" @close="menuOpen = false" />

    <main class="main-content">
      <RouterView />
    </main>

    <!-- Fora do .main-content de proposito: a barra e fixa na viewport, entao
         nao pode herdar o padding nem o max-width da coluna de conteudo. -->
    <AppFooter variant="compact" />
  </div>

  <!-- Telas publicas: nao tem sidebar, entao o wrapper cuida de empurrar
       o rodape para o fim mesmo quando a pagina e curta. -->
  <div v-else class="public-layout">
    <RouterView />
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import BrandLogo from '@/components/ui/BrandLogo.vue'

const auth = useAuthStore()
const route = useRoute()
const menuOpen = ref(false)

// Fechar menu ao trocar de rota (UX mobile)
watch(() => route.fullPath, () => { menuOpen.value = false })
</script>

<style scoped>
/* Top bar mobile */
.mobile-bar {
  display: none;
  position: fixed;
  top: 0; left: 0; right: 0;
  height: 56px;
  z-index: 80;
  background: var(--bg-overlay);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--border);
  padding: 0 16px;
  align-items: center;
  gap: 14px;
}

.mobile-brand {
  display: flex;
  align-items: center;
  gap: 8px;
}
.mobile-brand .brand-mark { color: var(--accent); font-size: 18px; }
.mobile-brand .brand-text {
  font-family: var(--font-display);
  font-size: 1rem;
  letter-spacing: -0.02em;
}

.hamburger {
  width: 34px; height: 34px;
  display: inline-flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 4px;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  cursor: pointer;
  padding: 0;
  transition: border-color 0.15s;
}
.hamburger:hover, .hamburger.active { border-color: var(--accent); }
.hamburger span {
  width: 16px; height: 2px;
  background: var(--text);
  border-radius: 1px;
  transition: transform 0.2s var(--ease), opacity 0.2s var(--ease);
}
.hamburger.active span:nth-child(1) { transform: translateY(6px) rotate(45deg); }
.hamburger.active span:nth-child(2) { opacity: 0; }
.hamburger.active span:nth-child(3) { transform: translateY(-6px) rotate(-45deg); }

.sidebar-backdrop {
  display: none;
  position: fixed;
  inset: 0;
  background: var(--backdrop);
  z-index: 90;
  animation: fade-in 0.18s ease;
}
@keyframes fade-in { from { opacity: 0; } to { opacity: 1; } }

@media (max-width: 900px) {
  .mobile-bar { display: flex; }
  .sidebar-backdrop { display: block; }
}
</style>
