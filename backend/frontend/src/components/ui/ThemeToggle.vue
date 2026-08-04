<template>
  <!-- variant="switch": trilho com rotulo, usado na sidebar -->
  <div v-if="variant === 'switch'" class="theme-row">
    <span class="theme-label">{{ isDark ? '🌙 Escuro' : '☀️ Claro' }}</span>
    <button
      class="theme-switch"
      type="button"
      role="switch"
      :aria-checked="!isDark"
      :aria-label="isDark ? 'Mudar para tema claro' : 'Mudar para tema escuro'"
      @click="toggleTheme"
    >
      <span class="thumb" :class="{ active: !isDark }"></span>
    </button>
  </div>

  <!-- variant="icon": botao redondo, usado na landing e nas telas de auth -->
  <button
    v-else
    class="theme-icon"
    :class="{ floating: floating }"
    type="button"
    :aria-label="isDark ? 'Mudar para tema claro' : 'Mudar para tema escuro'"
    :title="isDark ? 'Mudar para tema claro' : 'Mudar para tema escuro'"
    @click="toggleTheme"
  >
    {{ isDark ? '☀️' : '🌙' }}
  </button>
</template>

<script setup lang="ts">
import { useTheme } from '@/composables/useTheme'

withDefaults(defineProps<{
  /** 'switch' = trilho com rotulo | 'icon' = botao redondo */
  variant?: 'switch' | 'icon'
  /** posiciona o botao fixo no canto superior direito (telas sem header) */
  floating?: boolean
}>(), {
  variant: 'icon',
  floating: false,
})

const { isDark, toggleTheme } = useTheme()
</script>

<style scoped>
/* ---------- variant: switch ---------- */
/* Sem padding proprio: quem usa (a sidebar) define o espacamento e a borda. */
.theme-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
}

.theme-label {
  font-size: var(--text-xs);
  color: var(--text-muted);
  white-space: nowrap;
}

.theme-switch {
  position: relative;
  width: 36px;
  height: 20px;
  flex-shrink: 0;
  border: none;
  border-radius: 10px;
  background: var(--border-2);
  cursor: pointer;
  padding: 0;
  transition: background 0.2s var(--ease);
}

.theme-switch:hover { background: var(--accent-dim); }

.theme-switch .thumb {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--text-muted);
  transition: transform 0.2s var(--ease), background 0.2s var(--ease);
}

.theme-switch .thumb.active {
  transform: translateX(16px);
  background: var(--accent);
}

/* ---------- variant: icon ---------- */
.theme-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  font-size: 15px;
  line-height: 1;
  border: 1px solid var(--border);
  border-radius: 50%;
  background: var(--bg-2);
  cursor: pointer;
  transition: border-color 0.2s var(--ease), background 0.2s var(--ease), transform 0.2s var(--ease);
}

.theme-icon:hover {
  border-color: var(--accent);
  background: var(--bg-3);
  transform: scale(1.05);
}

.theme-icon:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.theme-icon.floating {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 90;
}

@media (max-width: 600px) {
  .theme-icon.floating { top: 14px; right: 14px; }
}
</style>
