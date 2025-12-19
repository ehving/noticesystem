<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import DbSelect from '@/components/admin/DbSelect.vue'
import PageHeader from '@/components/common/PageHeader.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const activePath = computed(() => route.path)

const handleSelect = (index: string) => {
  router.push(index)
}

const logout = () => {
  authStore.clearAuth()
  router.push('/login')
}

const toUserSide = () => {
  router.push('/notices')
}
</script>

<template>
  <div class="admin-layout">
    <aside class="sider">
      <div class="logo" @click="router.push('/admin')">Admin</div>
      <el-menu :default-active="activePath" class="menu" @select="handleSelect" router>
        <el-menu-item index="/admin/users">用户管理</el-menu-item>
        <el-menu-item index="/admin/depts">部门管理</el-menu-item>
        <el-menu-item index="/admin/roles">角色列表</el-menu-item>
        <el-menu-item index="/admin/notices">公告管理</el-menu-item>
        <el-menu-item index="/admin/sync-logs">同步日志</el-menu-item>
        <el-menu-item index="/admin/reports">报表分析</el-menu-item>
      </el-menu>
    </aside>
    <div class="main">
      <header class="topbar">
        <PageHeader title="后台管理">
          <DbSelect />
          <el-button size="small" @click="toUserSide">返回用户端</el-button>
          <span v-if="authStore.profile" class="user">
            {{ authStore.profile.nickname || authStore.profile.username }}（{{ authStore.profile.roleName }}）
          </span>
          <el-button size="small" type="text" @click="logout">退出</el-button>
        </PageHeader>
      </header>
      <section class="content">
        <router-view />
      </section>
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: flex;
}

.sider {
  width: 200px;
  background: #fff;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
}

.menu {
  border-right: 0;
  flex: 1;
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.topbar {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.content {
  flex: 1;
  padding: 16px;
  background: #f5f7fa;
}

.user {
  margin-right: 12px;
  color: #606266;
}
</style>
