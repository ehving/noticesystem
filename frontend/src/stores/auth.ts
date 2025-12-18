import { defineStore } from 'pinia';
import { request } from '../api';
import type { UserProfileVo } from '../types/vo';
import { clearToken, getToken, setToken as persistToken } from '../utils/storage';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken() ?? '',
    profile: null as UserProfileVo | null,
  }),
  actions: {
    setToken(token: string) {
      this.token = token;
      persistToken(token);
    },
    logout() {
      this.token = '';
      this.profile = null;
      clearToken();
    },
    async fetchProfile() {
      if (!this.token) {
        this.profile = null;
        return null;
      }
      try {
        const profile = await request<UserProfileVo>({ url: '/api/user/profile', method: 'get' });
        this.profile = profile;
        return profile;
      } catch (error) {
        this.logout();
        throw error;
      }
    },
  },
});
