import { defineStore } from 'pinia'
import { getToken, setToken as cacheToken, clearToken as clearCacheToken } from '@/utils/auth'
import * as authApi from '@/api/modules/auth'
import type { UserProfileVo } from '@/types/models/user'

interface AuthState {
  token: string | null
  profile: UserProfileVo | null
  loadingProfile: boolean
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: getToken(),
    profile: null,
    loadingProfile: false,
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    roleName: (state) => state.profile?.roleName ?? '',
    isAdmin(): boolean {
      return this.roleName === '管理员'
    },
  },
  actions: {
    setToken(token: string) {
      this.token = token
      cacheToken(token)
    },
    clearAuth() {
      this.token = null
      this.profile = null
      clearCacheToken()
    },
    async fetchProfile() {
      if (!this.token) {
        this.profile = null
        return null
      }
      this.loadingProfile = true
      try {
        const profile = await authApi.getProfile()
        this.profile = profile
        return profile
      } catch (error) {
        this.clearAuth()
        throw error
      } finally {
        this.loadingProfile = false
      }
    },
    async login(username: string, password: string) {
      const token = await authApi.login({ username, password })
      this.setToken(token)
      await this.fetchProfile()
      return token
    },
    logout() {
      this.clearAuth()
    },
    async init() {
      const token = getToken()
      if (!token) {
        this.clearAuth()
        return
      }
      this.setToken(token)
      try {
        await this.fetchProfile()
      } catch {
        // 已在 fetchProfile 中清理
      }
    },
  },
})
