import type { NavigationGuardNext, RouteLocationNormalized, Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { error as showError } from '@/utils/message'
import { setNavigate } from '@/api/http'
import { getDeviceType, isMobileByWidth } from '@/utils/device'

const getHomeRedirect = (authStore: ReturnType<typeof useAuthStore>) => {
  const isMobile = isMobileByWidth()
  if (isMobile) {
    if (!authStore.isLoggedIn) return '/login?redirect=/m/dashboard'
    return authStore.isAdmin ? '/m/dashboard' : '/mobile-not-supported'
  }
  return authStore.isAdmin ? '/admin/notices' : '/notices'
}

export const setupGuards = (router: Router) => {
  setNavigate((path: string) => {
    router.push(path)
  })

  router.beforeEach(async (to: RouteLocationNormalized, _from: RouteLocationNormalized, next: NavigationGuardNext) => {
    const authStore = useAuthStore()
    const isMobile = isMobileByWidth()

    // 根路径分流
    if (to.path === '/') {
      return next(getHomeRedirect(authStore))
    }

    // PC 页在移动端访问 -> 提示
    if (to.meta?.desktopOnly && isMobile) {
      return next({ path: '/mobile-not-supported', query: { from: to.fullPath } })
    }

    // 移动端页在 PC 访问 -> 跳 PC 报表或登录
    if (to.meta?.mobileOnly && !isMobile) {
      return next('/admin/reports')
    }

    // 登录页已登录
    if (to.path === '/login' && authStore.isLoggedIn) {
      return next(getHomeRedirect(authStore))
    }

    const requiresAuth = to.meta?.requiresAuth
    const roles = (to.meta?.roles as string[] | undefined) ?? []

    if (requiresAuth) {
      if (!authStore.token) {
        return next({ path: '/login', query: { redirect: to.fullPath } })
      }
      if (!authStore.profile && !authStore.loadingProfile) {
        try {
          await authStore.fetchProfile()
        } catch {
          return next({ path: '/login', query: { redirect: to.fullPath } })
        }
      }
    }

    if (roles.length > 0) {
      const roleName = authStore.roleName
      const allowed = roles.includes(roleName)
      if (!allowed) {
        showError('无访问权限')
        // 移动端角色失败也提示页
        return next(isMobile ? '/mobile-not-supported' : '/notices')
      }
    }

    return next()
  })

  router.afterEach((to) => {
    const title = (to.meta?.title as string | undefined) || '公告管理系统'
    document.title = `${title} - 公告管理系统`
  })

  // 设备变化监听：窗口尺寸变化自动分流
  let lastType = getDeviceType()
  window.addEventListener('resize', () => {
    const current = getDeviceType()
    if (current === lastType) return
    lastType = current
    const route = router.currentRoute.value
    const isMobile = current === 'mobile'
    if (route.meta?.desktopOnly && isMobile) {
      router.replace({ path: '/mobile-not-supported', query: { from: route.fullPath } })
      return
    }
    if (route.meta?.mobileOnly && !isMobile) {
      router.replace('/admin/reports')
      return
    }
    if (route.path === '/mobile-not-supported' && !isMobile) {
      const authStore = useAuthStore()
      router.replace(authStore.isAdmin ? '/admin/notices' : '/notices')
    }
  })
}
