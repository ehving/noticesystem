<script setup lang="ts">
import { useRouter, RouterLink, RouterView } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const authStore = useAuthStore();

const logout = () => {
  authStore.logout();
  router.push('/login');
};
</script>

<template>
  <div class="layout">
    <header class="header">
      <nav class="nav">
        <RouterLink to="/notices">公告</RouterLink>
        <RouterLink to="/profile">个人中心</RouterLink>
      </nav>
      <button type="button" class="logout" @click="logout">退出</button>
    </header>
    <main class="content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background-color: #f5f5f5;
}

.nav {
  display: flex;
  gap: 12px;
}

.content {
  flex: 1;
  padding: 16px;
}

.logout {
  padding: 8px 12px;
  cursor: pointer;
}
</style>
