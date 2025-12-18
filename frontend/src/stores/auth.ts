import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { request } from '../api';
import type { UserProfileVo } from '../types/vo';
import { clearToken, getToken, setToken as persistToken } from '../utils/storage';

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getToken() ?? '');
  const profile = ref<UserProfileVo | null>(null);

  const isLoggedIn = computed(() => Boolean(token.value));
  const isAdmin = computed(() => profile.value?.roleName === '管理员');

  const setToken = (value: string) => {
    token.value = value;
    persistToken(value);
  };

  const logout = () => {
    token.value = '';
    profile.value = null;
    clearToken();
  };

  const fetchProfile = async () => {
    if (!token.value) {
      profile.value = null;
      return null;
    }
    try {
      const data = await request<UserProfileVo>({ url: '/api/user/profile', method: 'get' });
      profile.value = data;
      return data;
    } catch (error) {
      logout();
      throw error;
    }
  };

  return {
    token,
    profile,
    isLoggedIn,
    isAdmin,
    setToken,
    logout,
    fetchProfile,
  };
});
