<template>
  <div>
    <div class="page-header">
      <h1>Painel Admin</h1>
      <p class="subtitle">{{ users.length }} usuários cadastrados</p>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
    </div>

    <div v-else class="users-table card">
      <table>
        <thead>
          <tr>
            <th>Nome</th>
            <th>Email</th>
            <th>Role</th>
            <th>Status</th>
            <th>Criado em</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td>{{ user.name }}</td>
            <td class="email">{{ user.email }}</td>
            <td>
              <select :value="user.role" @change="updateRole(user.id, ($event.target as HTMLSelectElement).value)" class="role-select">
                <option value="CANDIDATE">Candidato</option>
                <option value="RECRUITER">Recrutador</option>
                <option value="ADMIN">Admin</option>
              </select>
            </td>
            <td>
              <span :class="['status-badge', user.active ? 'active' : 'inactive']">
                {{ user.active ? 'Ativo' : 'Inativo' }}
              </span>
            </td>
            <td class="date">{{ formatDate(user.createdAt) }}</td>
            <td>
              <button
                :class="['btn-action', user.active ? 'btn-danger' : 'btn-success']"
                @click="toggleStatus(user)"
              >
                {{ user.active ? 'Desativar' : 'Ativar' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="message" class="toast" :class="messageType">{{ message }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'

const users = ref<any[]>([])
const loading = ref(true)
const message = ref('')
const messageType = ref('success')

onMounted(async () => {
  try {
    const res = await axios.get('/api/admin/users')
    users.value = res.data
  } catch {
    showMessage('Erro ao carregar usuários', 'error')
  } finally {
    loading.value = false
  }
})

async function updateRole(id: number, role: string) {
  try {
    await axios.patch(`/api/admin/users/${id}/role`, { role })
    showMessage('Role atualizado com sucesso', 'success')
  } catch {
    showMessage('Erro ao atualizar role', 'error')
  }
}

async function toggleStatus(user: any) {
  try {
    await axios.patch(`/api/admin/users/${user.id}/status`, { active: !user.active })
    user.active = !user.active
    showMessage('Status atualizado', 'success')
  } catch {
    showMessage('Erro ao atualizar status', 'error')
  }
}

function showMessage(msg: string, type: string) {
  message.value = msg
  messageType.value = type
  setTimeout(() => { message.value = '' }, 3000)
}

function formatDate(d: string) {
  return new Date(d).toLocaleDateString('pt-BR')
}
</script>

<style scoped>
.users-table { overflow-x: auto; padding: 0; }

table {
  width: 100%;
  border-collapse: collapse;
}

th {
  text-align: left;
  font-size: 10px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--text-muted);
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
}

td {
  padding: 14px 16px;
  font-size: var(--text-sm);
  border-bottom: 1px solid var(--border);
  vertical-align: middle;
}

tr:last-child td { border-bottom: none; }
tr:hover td { background: var(--bg-3); }

.email { color: var(--text-muted); font-size: 12px; }
.date { color: var(--text-muted); font-size: 12px; }

.role-select {
  background: var(--bg-3);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  color: var(--text);
  padding: 4px 8px;
  font-size: var(--text-sm);
  cursor: pointer;
}

.status-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 500;
}
.status-badge.active { background: rgba(52, 211, 153, 0.15); color: #34d399; }
.status-badge.inactive { background: rgba(248, 113, 113, 0.15); color: #f87171; }

.btn-action {
  padding: 4px 12px;
  border-radius: var(--radius);
  font-size: 12px;
  cursor: pointer;
  border: 1px solid;
  background: transparent;
  transition: all 0.15s;
}
.btn-danger { border-color: #f87171; color: #f87171; }
.btn-danger:hover { background: rgba(248, 113, 113, 0.1); }
.btn-success { border-color: #34d399; color: #34d399; }
.btn-success:hover { background: rgba(52, 211, 153, 0.1); }

.loading-state { display: flex; justify-content: center; padding: 60px; }

.toast {
  position: fixed;
  bottom: 24px;
  right: 24px;
  padding: 12px 20px;
  border-radius: var(--radius);
  font-size: var(--text-sm);
  z-index: 999;
}
.toast.success { background: rgba(52, 211, 153, 0.2); color: #34d399; border: 1px solid #34d399; }
.toast.error { background: rgba(248, 113, 113, 0.2); color: #f87171; border: 1px solid #f87171; }
</style>
