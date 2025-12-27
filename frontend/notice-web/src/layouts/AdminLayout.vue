<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import DbSelect from '@/components/admin/DbSelect.vue'
import {
  ArrowDown,
  Monitor,
  DataLine,
  Filter,
  User,
  Stamp,
  OfficeBuilding,
  Bell,
  PieChart,
  SwitchButton,
  HomeFilled,
  Operation,
  Management,
  Menu as IconMenu
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const activePath = computed(() => route.path)

const logout = () => {
  authStore.logout()
  router.push('/login')
}

const toUserSide = () => {
  router.push('/notices')
}

const toAdminHome = () => {
  router.push('/admin')
}
</script>

<template>
  <div class="admin-layout">
    <!-- 背景装饰：量子网格 -->
    <div class="quantum-grid"></div>

    <!-- 侧边栏：深色控制台风格 -->
    <aside class="sider">
      <div class="sider-logo" @click="toAdminHome">
        <div class="logo-box">
          <el-icon><Monitor /></el-icon>
        </div>
        <div class="logo-text-group">
          <span class="main-text">管理中枢</span>
          <span class="sub-text">ADMIN CONTROL</span>
        </div>
      </div>

      <div class="menu-container">
        <el-menu
          :default-active="activePath"
          class="cyber-menu"
          router
          :unique-opened="true"
          background-color="transparent"
          text-color="#94a3b8"
          active-text-color="#fff"
        >
          <el-sub-menu index="group-maintain">
            <template #title>
              <el-icon><Operation /></el-icon>
              <span>系统运维</span>
            </template>
            <el-menu-item index="/admin/reports">
              <div class="menu-dot"></div>
              <el-icon><PieChart /></el-icon>数据罗盘
            </el-menu-item>
            <el-menu-item index="/admin/sync-logs">
              <div class="menu-dot"></div>
              <el-icon><DataLine /></el-icon>同步日志
            </el-menu-item>
            <el-menu-item index="/admin/conflicts">
              <div class="menu-dot"></div>
              <el-icon><Filter /></el-icon>冲突解决
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="group-info">
            <template #title>
              <el-icon><Management /></el-icon>
              <span>组织架构</span>
            </template>
            <el-menu-item index="/admin/users">
              <div class="menu-dot"></div>
              <el-icon><User /></el-icon>用户档案
            </el-menu-item>
            <el-menu-item index="/admin/roles">
              <div class="menu-dot"></div>
              <el-icon><Stamp /></el-icon>权限矩阵
            </el-menu-item>
            <el-menu-item index="/admin/depts">
              <div class="menu-dot"></div>
              <el-icon><OfficeBuilding /></el-icon>部门单元
            </el-menu-item>
            <el-menu-item index="/admin/notices">
              <div class="menu-dot"></div>
              <el-icon><Bell /></el-icon>全域广播
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </div>

      <!-- 侧边栏底部装饰 -->
      <div class="sider-footer">
        <div class="sys-status">
          <span class="status-indicator"></span>
          SYSTEM ONLINE
        </div>
      </div>
    </aside>

    <!-- 主体区域 -->
    <div class="main-viewport">
      <!-- 顶栏：晶体玻璃风格 -->
      <header class="topbar">
        <div class="topbar-left">
          <!-- 面包屑区域 (预留) -->
          <div class="current-context">
            <el-icon><IconMenu /></el-icon>
            <span>Dashboard</span>
          </div>
        </div>

        <div class="topbar-right">
          <!-- 数据源切换器 -->
          <div class="db-module">
            <span class="module-label">DATA SOURCE</span>
            <div class="db-select-wrap">
              <DbSelect />
            </div>
          </div>

          <!-- 管理员信息 -->
          <el-dropdown trigger="click" popper-class="admin-dropdown-popover">
            <div class="admin-profile-trigger">
              <div class="avatar-ring">
                <el-avatar :size="34" :src="authStore.profile?.avatar || ''" class="admin-avatar">
                  {{ authStore.profile?.nickname?.[0]?.toUpperCase() || 'A' }}
                </el-avatar>
              </div>
              <div class="admin-info">
                <span class="name">{{ authStore.profile?.nickname || 'Administrator' }}</span>
                <span class="role">Level 9 Access</span>
              </div>
              <el-icon class="caret"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="toUserSide" :icon="HomeFilled">切换至用户视角</el-dropdown-item>
                <el-dropdown-item divided @click="logout" :icon="SwitchButton" class="danger-item">安全登出</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 内容区 -->
      <section class="content-stage">
        <router-view v-slot="{ Component }">
          <transition name="slide-up" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </section>
    </div>
  </div>
</template>

<script lang="ts">
export default { name: 'AdminLayout' }
</script>

<style scoped lang="scss">
.admin-layout {
  --sider-width: 260px;
  --header-height: 64px;
  --primary-color: #06b6d4; /* Cyan */
  --bg-color: #f1f5f9;      /* Light Slate */
  --sider-bg: #0f172a;      /* Dark Slate (matching User layout bg) */

  min-height: 100vh;
  display: flex;
  background-color: var(--bg-color);
  font-family: 'Inter', system-ui, sans-serif;
  overflow: hidden;
}

/* 1. 背景装饰 (亮色科技感) */
.quantum-grid {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.1) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.1) 1px, transparent 1px);
  background-size: 40px 40px;
  /* 柔和的光晕 */
  background:
    radial-gradient(circle at 80% 20%, rgba(6, 182, 212, 0.08), transparent 40%),
    radial-gradient(circle at 20% 80%, rgba(79, 70, 229, 0.08), transparent 40%);
}

/* 2. 侧边栏 (Dark Anchor) */
.sider {
  width: var(--sider-width);
  background: var(--sider-bg);
  display: flex;
  flex-direction: column;
  z-index: 20;
  box-shadow: 4px 0 20px rgba(0, 0, 0, 0.1);
  border-right: 1px solid rgba(255, 255, 255, 0.05);
}

.sider-logo {
  height: 80px;
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  cursor: pointer;
  transition: background 0.3s;

  &:hover {
    background: rgba(255, 255, 255, 0.02);
  }
}

.logo-box {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #06b6d4, #3b82f6);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  box-shadow: 0 0 15px rgba(6, 182, 212, 0.4);
}

.logo-text-group {
  display: flex;
  flex-direction: column;

  .main-text {
    font-size: 16px;
    font-weight: 700;
    color: #f8fafc;
    letter-spacing: 1px;
  }
  .sub-text {
    font-size: 10px;
    color: #64748b;
    font-weight: 600;
    letter-spacing: 2px;
  }
}

.menu-container {
  flex: 1;
  padding: 16px 0;
  overflow-y: auto;

  &::-webkit-scrollbar { width: 0; }
}

/* Cyber Menu Styles */
.cyber-menu {
  border-right: none;

  :deep(.el-sub-menu__title) {
    height: 50px;
    line-height: 50px;
    font-weight: 600;

    &:hover {
      background-color: rgba(255, 255, 255, 0.03) !important;
      color: #fff !important;
    }
  }

  :deep(.el-menu-item) {
    height: 46px;
    line-height: 46px;
    margin: 4px 12px;
    border-radius: 8px;
    position: relative;
    padding-left: 48px !important; /* Indent fix */

    .menu-dot {
      position: absolute;
      left: 24px;
      width: 4px;
      height: 4px;
      background: #475569;
      border-radius: 50%;
      transition: all 0.3s;
    }

    &:hover {
      background-color: rgba(255, 255, 255, 0.05) !important;
      color: #e2e8f0 !important;

      .menu-dot { background: #94a3b8; transform: scale(1.5); }
    }

    &.is-active {
      background: linear-gradient(90deg, rgba(6, 182, 212, 0.15), transparent) !important;
      color: #06b6d4 !important;

      .menu-dot {
        background: #06b6d4;
        box-shadow: 0 0 8px #06b6d4;
        transform: scale(1.5);
      }

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 10%;
        height: 80%;
        width: 3px;
        background: #06b6d4;
        border-radius: 0 4px 4px 0;
      }
    }
  }
}

.sider-footer {
  padding: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);

  .sys-status {
    font-size: 10px;
    color: #475569;
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
    letter-spacing: 1px;

    .status-indicator {
      width: 6px;
      height: 6px;
      background: #10b981;
      border-radius: 50%;
      box-shadow: 0 0 5px #10b981;
    }
  }
}

/* 3. 主视口 */
.main-viewport {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 10;
  overflow: hidden;
}

.topbar {
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  z-index: 10;
}

.current-context {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-size: 14px;
  font-weight: 500;

  .el-icon { font-size: 18px; }
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

/* 数据库模块 */
.db-module {
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.5);
  padding: 4px 6px 4px 16px;
  border-radius: 30px;
  border: 1px solid #e2e8f0;
  transition: all 0.3s;

  &:hover {
    background: #fff;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
    border-color: #cbd5e1;
  }

  .module-label {
    font-size: 10px;
    color: #94a3b8;
    font-weight: 700;
    letter-spacing: 0.5px;
  }
}

/* 管理员 Profile */
.admin-profile-trigger {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 4px;
  padding-right: 12px;
  border-radius: 40px;
  transition: background 0.3s;

  &:hover {
    background: rgba(0, 0, 0, 0.03);
  }
}

.avatar-ring {
  padding: 2px;
  border: 1px solid #e2e8f0;
  border-radius: 50%;
  background: #fff;
}

.admin-avatar {
  background: #0f172a;
  color: #fff;
  font-weight: 700;
}

.admin-info {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
  text-align: left;

  .name {
    font-size: 14px;
    font-weight: 600;
    color: #334155;
  }
  .role {
    font-size: 10px;
    color: #06b6d4;
    font-weight: 600;
    text-transform: uppercase;
  }
}

.caret {
  font-size: 12px;
  color: #94a3b8;
}

/* 内容舞台 */
.content-stage {
  flex: 1;
  padding: 24px 32px;
  overflow-y: auto;
  overflow-x: hidden;
}

/* 动画 */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.slide-up-enter-from {
  opacity: 0;
  transform: translateY(15px);
}

.slide-up-leave-to {
  opacity: 0;
  transform: translateY(-15px);
}
</style>

<style lang="scss">
/* 下拉菜单适配 */
.admin-dropdown-popover {
  border-radius: 12px !important;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1) !important;
  border: none !important;
  padding: 6px !important;

  .el-dropdown-menu__item {
    border-radius: 6px;
    margin-bottom: 2px;
    padding: 10px 16px;
    color: #475569;
    font-weight: 500;

    &:hover {
      background-color: #f1f5f9;
      color: #06b6d4;
    }

    .el-icon { margin-right: 8px; }
  }

  .danger-item {
    color: #ef4444;
    &:hover { background-color: #fef2f2; }
  }

  .el-dropdown-menu__item--divided {
    margin-top: 6px;
    border-top-color: #f1f5f9;
  }
}
</style>
