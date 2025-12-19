<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const active = computed(() => route.path)

const to = (path: string) => {
  if (path === active.value) return
  router.push(path)
}

const logout = () => {
  authStore.logout()
  router.push('/login')
}

</script>

<template>
  <div class="mobile-layout">
    <header class="header">
      <div class="title">移动端</div>
      <div class="actions">
        <el-button v-if="authStore.isAdmin" size="small" @click="router.push('/admin')">管理端</el-button>
        <el-button v-if="authStore.isLoggedIn" size="small" @click="logout">退出</el-button>
        <el-button v-else size="small" type="primary" @click="router.push('/login')">登录</el-button>
      </div>
    </header>
    <main class="content">
      <router-view />
    </main>
    <footer class="tabbar">
      <el-menu mode="horizontal" :default-active="active" @select="to">
        <el-menu-item index="/m/dashboard">同步统计</el-menu-item>
        <el-menu-item index="/m/exceptions">异常报表</el-menu-item>
        <el-menu-item index="/m/me">我的</el-menu-item>
      </el-menu>
    </footer>
  </div>
</template>

<style scoped>
.mobile-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}
.title {
  font-weight: 600;
}
.actions {
  display: flex;
  gap: 8px;
}
.content {
  flex: 1;
  padding: 12px;
}
.tabbar {
  border-top: 1px solid #ebeef5;
}
.tabbar :deep(.el-menu) {
  border-bottom: none;
  justify-content: space-around;
}
</style>
