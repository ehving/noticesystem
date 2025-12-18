<script setup lang="ts">
import { ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const username = ref('');
const password = ref('');

const handleLogin = () => {
  // 这里只做占位，实际登录逻辑后续实现
  authStore.setToken('mock-token');
  const redirect = (route.query.redirect as string) || '/';
  router.push(redirect);
};
</script>

<template>
  <div class="page">
    <h1>登录</h1>
    <p>此处为登录页面占位。提交后将写入 token 并跳转。</p>
    <form @submit.prevent="handleLogin">
      <div class="field">
        <label>用户名</label>
        <input v-model="username" type="text" />
      </div>
      <div class="field">
        <label>密码</label>
        <input v-model="password" type="password" />
      </div>
      <button type="submit">登录</button>
    </form>
  </div>
</template>

<style scoped>
.page {
  max-width: 420px;
  margin: 40px auto;
  padding: 24px;
  border: 1px solid #ddd;
  border-radius: 8px;
}

.field {
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
</style>
