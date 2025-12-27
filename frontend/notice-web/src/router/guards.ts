import type { NavigationGuardNext, RouteLocationNormalized, Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { error as showError } from '@/utils/message'
import { setNavigate } from '@/api/http'
import { getDeviceType, isMobileByWidth, onDeviceChange } from '@/utils/device'

const getHomeRedirect = (authStore: ReturnType<typeof useAuthStore>) => {
  const isMobile = isMobileByWidth()
  if (isMobile) {
    if (!authStore.isLoggedIn) return '/login?redirect=/m/MSyncLogReport'
    return authStore.isAdmin ? '/m/MSyncLogReport' : '/mobile-not-supported'
  }
  return authStore.isAdmin ? '/admin/notices' : '/notices'
}

const ensureProfile = async (authStore: ReturnType<typeof useAuthStore>) => {
  if (!authStore.token) return false
  if (authStore.profile || authStore.loadingProfile) return true
  try {
    await authStore.fetchProfile()
    return true
  } catch {
    authStore.clearAuth()
    return false
  }
}

export const setupGuards = (router: Router) => {
  setNavigate((path: string) => {
    router.push(path)
  })

  router.beforeEach(async (to: RouteLocationNormalized, _from: RouteLocationNormalized, next: NavigationGuardNext) => {
    const authStore = useAuthStore()
    const isMobile = isMobileByWidth()

    if (to.path === '/') {
      return next(getHomeRedirect(authStore))
    }

    if (to.meta?.desktopOnly && isMobile) {
      return next({ path: '/mobile-not-supported', query: { from: to.fullPath } })
    }

    if (to.meta?.mobileOnly && !isMobile) {
      return next('/admin/reports')
    }

    if (to.path === '/login' && authStore.isLoggedIn) {
      return next(getHomeRedirect(authStore))
    }

    const requiresAuth = to.meta?.requiresAuth
    const roles = (to.meta?.roles as string[] | undefined) ?? []

    if (requiresAuth && !authStore.token) {
      return next({ path: '/login', query: { redirect: to.fullPath } })
    }

    // 确保 profile 已加载（roles 校验前）
    if (authStore.token && (requiresAuth || roles.length > 0)) {
      const ok = await ensureProfile(authStore)
      if (!ok) {
        return next({ path: '/login', query: { redirect: to.fullPath } })
      }
    }

    if (roles.length > 0) {
      const roleName = authStore.roleName
      const allowed = roles.includes(roleName)
      if (!allowed) {
        showError('无访问权限')
        return next(isMobile ? '/mobile-not-supported' : '/notices')
      }
    }

    return next()
  })

  router.afterEach((to) => {
    const title = (to.meta?.title as string | undefined) || '公告管理系统'
    document.title = `${title} - 公告管理系统`
  })

  let lastType = getDeviceType()
  onDeviceChange(() => {
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
