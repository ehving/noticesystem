import type { RouteRecordRaw } from 'vue-router'
import MobileLayout from '@/layouts/MobileLayout.vue'

export const mobileRoutes: RouteRecordRaw[] = [
  {
    path: '/m',
    component: MobileLayout,
    redirect: '/m/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'mobile-dashboard',
        component: () => import('@/views/mobile/Dashboard.vue'),
        meta: { requiresAuth: true, roles: ['管理员'], title: '同步统计', mobileOnly: true },
      },
      {
        path: 'exceptions',
        name: 'mobile-exceptions',
        component: () => import('@/views/mobile/Exceptions.vue'),
        meta: { requiresAuth: true, roles: ['管理员'], title: '异常报表', mobileOnly: true },
      },
      {
        path: 'me',
        name: 'mobile-me',
        component: () => import('@/views/mobile/Me.vue'),
        meta: { requiresAuth: true, roles: ['管理员'], title: '我的', mobileOnly: true },
      },
    ],
  },
  {
    path: '/m/preview',
    name: 'mobile-preview',
    component: () => import('@/views/mobile/DesktopPreview.vue'),
    meta: { title: '移动端预览' },
  },
]
