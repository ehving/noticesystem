import { createRouter, createWebHistory } from 'vue-router'
import { publicRoutes } from './modules/public'
import { userRoutes } from './modules/user'
import { adminRoutes } from './modules/admin'
import { mobileRoutes } from './modules/mobile'
import { setupGuards } from './guards'
import AdminDesktopOnly from '@/views/common/AdminDesktopOnly.vue'
import MobileNotSupported from '@/views/common/MobileNotSupported.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/notices',
    },
    ...publicRoutes,
    ...userRoutes,
    ...adminRoutes,
    ...mobileRoutes,
    {
      path: '/admin/desktop-only',
      name: 'admin-desktop-only',
      component: AdminDesktopOnly,
      meta: { title: '仅支持电脑端' },
    },
    {
      path: '/mobile-not-supported',
      name: 'mobile-not-supported',
      component: MobileNotSupported,
      meta: { title: '移动端访问提示' },
    },
  ],
})

setupGuards(router)

export default router
