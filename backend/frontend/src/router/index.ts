import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Landing',
      component: () => import('@/views/LandingView.vue'),
      meta: { public: true, landing: true }
    },
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
      path: '/forgot-password',
      name: 'ForgotPassword',
      component: () => import('@/views/ForgotPasswordView.vue'),
      meta: { public: true }
    },
    {
      path: '/confirmar-email',
      name: 'VerifyEmail',
      component: () => import('@/views/VerifyEmailView.vue'),
      meta: { public: true }
    },
    {
      path: '/reset-password',
      name: 'ResetPassword',
      component: () => import('@/views/ResetPasswordView.vue'),
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
    {
      path: '/admin',
      name: 'Admin',
      component: () => import('@/views/AdminView.vue'),
      meta: { role: 'ADMIN' }
    },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach((to, _from, next) => {
  const auth = useAuthStore()

  if (!to.meta.public && !auth.isAuthenticated) return next('/')

  if (auth.isAuthenticated && (to.name === 'Login' || to.name === 'Register')) {
    return next('/dashboard')
  }

  if (auth.isAuthenticated && to.meta.landing) {
    return next('/dashboard')
  }

  if (to.meta.role && auth.user?.role !== to.meta.role) return next('/dashboard')

  next()
})

export default router