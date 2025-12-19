<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const logout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="user-layout">
    <header class="header">
      <div class="logo" @click="router.push('/')">Notice System</div>
      <nav class="nav">
        <a @click.prevent="router.push('/notices')">公告</a>
        <a @click.prevent="router.push('/profile')">个人资料</a>
        <a v-if="authStore.isAdmin" @click.prevent="router.push('/admin')">管理端</a>
      </nav>
      <div class="actions">
        <span v-if="authStore.profile" class="user">
          {{ authStore.profile.nickname || authStore.profile.username }}（{{ authStore.profile.roleName }}）
        </span>
        <el-button v-if="authStore.isLoggedIn" size="small" type="text" @click="logout">退出</el-button>
        <el-button v-else size="small" type="primary" @click="router.push('/login')">登录</el-button>
      </div>
    </header>
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.user-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.logo {
  font-weight: 600;
  cursor: pointer;
}

.nav a {
  margin: 0 12px;
  cursor: pointer;
  color: #606266;
}

.nav a:hover {
  color: #409eff;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user {
  color: #606266;
}

.content {
  flex: 1;
  padding: 16px 24px;
}
</style>
