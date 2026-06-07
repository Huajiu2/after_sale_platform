import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue')
  },
  {
    path: '/orders',
    name: 'Orders',
    component: () => import('../views/Orders.vue')
  },
  {
    path: '/knowledge',
    name: 'KnowledgeBase',
    component: () => import('../views/KnowledgeBase.vue')
  },
  {
    path: '/dlq',
    name: 'Dlq',
    component: () => import('../views/Dlq.vue')
  },
  {
    path: '/report',
    name: 'Report',
    component: () => import('../views/Report.vue')
  },
  {
    path: '/config',
    name: 'Config',
    component: () => import('../views/Config.vue')
  },
  {
    path: '/user-apply',
    name: 'UserApply',
    component: () => import('../views/UserApply.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
