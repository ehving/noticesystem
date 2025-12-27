<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import DbSelect from "@/components/admin/DbSelect.vue";
import { DataLine, Filter as FilterIcon, User, SwitchButton, Monitor } from '@element-plus/icons-vue'

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
      <div class="header-left">
        <div class="logo-icon">
          <el-icon><Monitor /></el-icon>
        </div>
        <span class="title">数据看板</span>
      </div>

      <div class="header-right">
        <div class="db-select-box">
          <DbSelect />
        </div>
        <el-button
          v-if="authStore.isLoggedIn"
          circle
          text
          :icon="SwitchButton"
          class="logout-btn"
          @click="logout"
        />
        <el-button
          v-else
          size="small"
          type="primary"
          round
          @click="router.push('/login')"
        >
          登录
        </el-button>
      </div>
    </header>

    <main class="content">
      <router-view />
    </main>

    <footer class="tabbar">
      <el-menu
        mode="horizontal"
        :default-active="active"
        @select="to"
        class="mobile-menu"
        :ellipsis="false"
      >
        <el-menu-item index="/m/MSyncLogReport">
          <div class="tab-item">
            <el-icon class="tab-icon"><DataLine /></el-icon>
            <span class="tab-text">同步</span>
          </div>
        </el-menu-item>
        <el-menu-item index="/m/MConflicts">
          <div class="tab-item">
            <el-icon class="tab-icon"><FilterIcon /></el-icon>
            <span class="tab-text">冲突</span>
          </div>
        </el-menu-item>
        <el-menu-item index="/m/Me">
          <div class="tab-item">
            <el-icon class="tab-icon"><User /></el-icon>
            <span class="tab-text">我的</span>
          </div>
        </el-menu-item>
      </el-menu>
    </footer>
  </div>
</template>

<style scoped>
.mobile-layout {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 56px;
  z-index: 100;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 16px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo-icon {
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, #409eff 0%, #36cfc9 100%);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
}

.title {
  font-weight: 600;
  font-size: 16px;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logout-btn {
  font-size: 18px;
  color: #909399;
  margin-left: 4px;
}

.content {
  padding: 68px 12px 70px; /* Top: header+12, Bottom: footer+12 */
  min-height: 100vh;
  box-sizing: border-box;
}

.tabbar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: #fff;
  box-shadow: 0 -1px 4px rgba(0, 0, 0, 0.05);
}

.mobile-menu {
  border-bottom: none;
  display: flex;
  justify-content: space-around;
  height: 56px;
}

:deep(.el-menu-item) {
  padding: 0 12px;
  height: 100%;
  line-height: normal;
  background: transparent !important;
}

:deep(.el-menu-item.is-active) {
  border-bottom: none !important;
  color: #409eff !important;
}

:deep(.el-menu-item:hover) {
  background-color: transparent !important;
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 4px;
}

.tab-icon {
  font-size: 20px;
  margin: 0 !important;
}

.tab-text {
  font-size: 10px;
  line-height: 1;
}
</style>
