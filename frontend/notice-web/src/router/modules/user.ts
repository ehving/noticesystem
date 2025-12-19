import type { RouteRecordRaw } from 'vue-router'
import UserLayout from '@/layouts/UserLayout.vue'

export const userRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    component: UserLayout,
    meta: { requiresAuth: true, title: '公告列表', desktopOnly: true },
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/user/NoticeList.vue'),
        meta: {
          requiresAuth: true,
          desktopOnly: true,
          title: '公告列表',
        },
      },
      {
        path: 'notices',
        name: 'notices',
        component: () => import('@/views/user/NoticeList.vue'),
        meta: {
          requiresAuth: true,
          desktopOnly: true,
          title: '公告列表',
        },
      },
      {
        path: 'notices/:id',
        name: 'notice-detail',
        component: () => import('@/views/user/NoticeDetail.vue'),
        meta: {
          requiresAuth: true,
          desktopOnly: true,
          title: '公告详情',
        },
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/user/Profile.vue'),
        meta: {
          requiresAuth: true,
          desktopOnly: true,
          title: '个人资料',
        },
      },
      {
        path: 'password',
        name: 'password',
        component: () => import('@/views/user/Password.vue'),
        meta: {
          requiresAuth: true,
          desktopOnly: true,
          title: '修改密码',
        },
      },
    ],
  },
]
