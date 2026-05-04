import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/dashboard' },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true }
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { public: true }
    },
    {
      path: '/dashboard',
      name: 'Dashboard',
      component: () => import('@/views/DashboardView.vue')
    },
    {
      path: '/chat',
      name: 'Chat',
      component: () => import('@/views/ChatView.vue')
    },
    {
      path: '/resume',
      name: 'Resume',
      component: () => import('@/views/ResumeView.vue'),
      meta: { role: 'CANDIDATE' }
    },
    {
      path: '/jobs',
      name: 'Jobs',
      component: () => import('@/views/JobsView.vue')
    },
    {
      path: '/recruiter',
      name: 'Recruiter',
      component: () => import('@/views/RecruiterView.vue'),
      meta: { role: 'RECRUITER' }
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
  ]
})

router.beforeEach((to, _from, next) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isAuthenticated) return next('/login')
  if (to.meta.public && auth.isAuthenticated) return next('/dashboard')
  if (to.meta.role && auth.user?.role !== to.meta.role) return next('/dashboard')
  next()
})

export default router
