import type { RouteRecordRaw } from 'vue-router'
import MobileLayout from '@/layouts/MobileLayout.vue'

export const mobileRoutes: RouteRecordRaw[] = [
  {
    path: '/m',
    component: MobileLayout,
    redirect: '/m/MSyncLogReport',
    children: [
      {
        path: 'MSyncLogReport',
        name: 'mobile-sync-report',
        component: () => import('@/views/mobile/MSyncLogReport.vue'),
        meta: { requiresAuth: true, roles: ['管理员'], title: '同步统计', mobileOnly: true },
      },
      {
        path: 'MConflicts',
        name: 'mobile-conflicts',
        component: () => import('@/views/mobile/MConflicts.vue'),
        meta: { requiresAuth: true, roles: ['管理员'], title: '异常报表', mobileOnly: true },
      },
      {
        path: 'Me',
        name: 'mobile-me',
        component: () => import('@/views/mobile/Me.vue'),
        meta: { requiresAuth: true, roles: ['管理员'], title: '我的', mobileOnly: true },
      },
    ],
  },

]
