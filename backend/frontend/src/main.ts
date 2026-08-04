import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { initTheme } from './composables/useTheme'
import './styles/main.css'

// Antes do mount: evita o flash de tema escuro em quem usa o claro.
initTheme()

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
