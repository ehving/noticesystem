import type { RouteRecordRaw } from 'vue-router'
import BlankLayout from '@/layouts/BlankLayout.vue'

export const publicRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    component: BlankLayout,
    meta: {
      requiresAuth: false,
      title: '登录',
    },
    children: [
      {
        path: '',
        name: 'login',
        component: () => import('@/views/auth/Login.vue'),
      },
    ],
  },
  {
    path: '/register',
    component: BlankLayout,
    meta: {
      requiresAuth: false,
      title: '注册',
    },
    children: [
      {
        path: '',
        name: 'register',
        component: () => import('@/views/auth/Register.vue'),
      },
    ],
  },
]
