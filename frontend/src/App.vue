<template>
  <div class="app-layout" v-if="auth.isAuthenticated">
    <AppSidebar />
    <main class="main-content">
      <RouterView />
    </main>
  </div>
  <RouterView v-else />
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import AppSidebar from '@/components/layout/AppSidebar.vue'

const auth = useAuthStore()
</script>
