<script setup lang="ts">
import { RouterLink, RouterView, useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const authStore = useAuthStore();

const logout = () => {
  authStore.logout();
  router.push('/login');
};

const backToUser = () => {
  router.push('/');
};
</script>

<template>
  <div class="admin-layout">
    <aside class="sider">
      <h2>管理端</h2>
      <nav>
        <RouterLink to="/admin/notices">公告管理</RouterLink>
        <RouterLink to="/admin/users">用户管理</RouterLink>
        <RouterLink to="/admin/depts">部门管理</RouterLink>
        <RouterLink to="/admin/sync-logs">同步日志</RouterLink>
      </nav>
      <div class="actions">
        <button type="button" @click="backToUser">返回用户端</button>
        <button type="button" @click="logout">退出</button>
      </div>
    </aside>
    <main class="admin-content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.admin-layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  min-height: 100vh;
}

.sider {
  background: #f0f2f5;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sider nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.actions {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.admin-content {
  padding: 16px;
}
</style>
