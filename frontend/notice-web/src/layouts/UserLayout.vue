<script setup lang="ts">
import {useRoute, useRouter} from 'vue-router'
import { ArrowDown, Bell, UserFilled, SwitchButton, Monitor, HomeFilled, Grid } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const goHome = () => router.push('/notices')

const goProfile = () => router.push('/profile')
const goAdmin = () => router.push('/admin')

const logout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="user-layout">
    <!-- 1. 深空环境层 -->
    <div class="space-bg">
      <div class="nebula nebula-1"></div>
      <div class="nebula nebula-2"></div>
      <div class="star-grid"></div>
    </div>

    <header class="header">
      <div class="header-inner">
        <!-- Logo区：全息投影感 -->
        <div class="left" @click="goHome">
          <div class="logo-wrapper">
            <div class="logo-ring"></div>
            <div class="logo-icon">
              <el-icon><Bell /></el-icon>
            </div>
          </div>
          <div class="brand-info">
            <div class="logo-text">公告管理系统</div>
            <div class="logo-sub">NOTICE COMMAND CENTER</div>
          </div>
        </div>

        <div class="right">
          <el-dropdown v-if="authStore.isLoggedIn" trigger="click" popper-class="cyber-user-dropdown">
            <div class="user-trigger">
              <div class="avatar-frame">
                <el-avatar :size="32" class="user-avatar" :icon="UserFilled" :src="authStore.profile?.avatar || ''" />
                <div class="status-light online"></div>
              </div>
              <div class="user-info">
                <span class="nickname">{{ authStore.profile?.nickname || authStore.profile?.username }}</span>
                <span class="role-badge" :class="{ 'admin-badge': authStore.isAdmin }">
                  {{ authStore.profile?.roleName || 'OPERATOR' }}
                </span>
              </div>
              <el-icon class="arrow"><ArrowDown /></el-icon>
            </div>

            <template #dropdown>
              <el-dropdown-menu class="user-menu">
                <div class="menu-header">
                  <span class="menu-label">ACCESS CONTROL</span>
                </div>
                <el-dropdown-item v-if="route.path === '/profile'" @click="goHome" :icon="HomeFilled">
                  主控台
                </el-dropdown-item>
                <el-dropdown-item v-else @click="goProfile" :icon="UserFilled">
                  个人档案
                </el-dropdown-item>

                <el-dropdown-item v-if="authStore.isAdmin" divided @click="goAdmin" :icon="Monitor" class="admin-item">
                  系统管理后台
                </el-dropdown-item>
                <el-dropdown-item divided @click="logout" :icon="SwitchButton" class="danger-item">
                  断开连接
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>

          </el-dropdown>

          <el-button v-else round class="login-btn" @click="router.push('/login')">
            接入系统
          </el-button>
        </div>
      </div>
    </header>

    <main class="content">
      <div class="content-inner">
        <router-view v-slot="{ Component }">
          <transition name="holo-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>

    <footer class="footer">
      <div class="footer-line"></div>
      <p>SYSTEM STATUS: NORMAL | © 2024 NOTICE SYSTEM</p>
    </footer>
  </div>
</template>

<script lang="ts">
export default { name: 'UserLayout' }
</script>

<style scoped lang="scss">
.user-layout {
  --header-height: 70px;
  --bg-color: #0f172a;
  --text-main: #f8fafc;
  --text-sub: #94a3b8;
  --accent-cyan: #06b6d4;
  --accent-purple: #8b5cf6;
  --glass-bg: rgba(15, 23, 42, 0.75);
  --glass-border: rgba(255, 255, 255, 0.08);

  min-height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
  background-color: var(--bg-color);
  font-family: 'Inter', 'Segoe UI', sans-serif;
  color: var(--text-main);
  overflow-x: hidden;
}

/* 1. 深空环境背景 */
.space-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100vh;
  z-index: 0;
  pointer-events: none;
  background: radial-gradient(circle at center, #1e293b 0%, #0f172a 100%);
}

.star-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px);
  background-size: 40px 40px;
  mask-image: radial-gradient(circle at 50% 0%, black 40%, transparent 100%);
  z-index: 1;
}

.nebula {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.4;
  animation: floatNebula 20s infinite ease-in-out;
  z-index: 0;
}

.nebula-1 {
  top: -20%;
  left: 20%;
  width: 600px;
  height: 600px;
  background: #4f46e5;
}

.nebula-2 {
  bottom: -20%;
  right: 10%;
  width: 500px;
  height: 500px;
  background: #06b6d4;
}

@keyframes floatNebula {
  0%, 100% { transform: translate(0, 0); opacity: 0.4; }
  50% { transform: translate(20px, 30px); opacity: 0.2; }
}

/* 2. HUD 风格顶栏 */
.header {
  height: var(--header-height);
  background: var(--glass-bg);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--glass-border);
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.3);
}

.header-inner {
  max-width: 1400px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
}

/* Logo 区 */
.left {
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;

  &:hover .logo-ring {
    transform: rotate(180deg) scale(1.1);
    border-color: var(--accent-cyan);
  }
}

.logo-wrapper {
  position: relative;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-ring {
  position: absolute;
  inset: 0;
  border: 2px dashed rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.logo-icon {
  font-size: 20px;
  color: var(--accent-cyan);
  filter: drop-shadow(0 0 8px rgba(6, 182, 212, 0.6));
}

.brand-info {
  display: flex;
  flex-direction: column;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 1px;
  background: linear-gradient(90deg, #fff, #94a3b8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.logo-sub {
  font-size: 10px;
  font-weight: 500;
  color: var(--accent-cyan);
  letter-spacing: 2px;
  opacity: 0.8;
}

/* 用户区 */
.right {
  display: flex;
  align-items: center;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 6px 16px 6px 6px;
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--glass-border);
  transition: all 0.3s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.08);
    border-color: var(--accent-purple);
    box-shadow: 0 0 15px rgba(139, 92, 246, 0.2);
  }
}

.avatar-frame {
  position: relative;
  padding: 2px;
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 50%;
}

.user-avatar {
  background: #1e293b;
  color: #fff;
}

.status-light {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
  box-shadow: 0 0 5px #10b981;
  border: 1px solid #0f172a;
}

.user-info {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
  text-align: left;
  margin-right: 4px;
}

.nickname {
  font-size: 14px;
  font-weight: 600;
  color: #e2e8f0;
}

.role-badge {
  font-size: 10px;
  color: #94a3b8;
  background: rgba(255,255,255,0.05);
  padding: 0px 6px;
  border-radius: 4px;
  align-self: flex-start;
  text-transform: uppercase;
  letter-spacing: 0.5px;

  &.admin-badge {
    color: #fbbf24;
    background: rgba(251, 191, 36, 0.1);
    box-shadow: 0 0 5px rgba(251, 191, 36, 0.2);
  }
}

.arrow {
  font-size: 12px;
  color: #64748b;
  transition: transform 0.3s;
}

.user-trigger:hover .arrow {
  transform: rotate(180deg);
  color: var(--accent-purple);
}

.login-btn {
  background: linear-gradient(90deg, #4f46e5, #06b6d4);
  border: none;
  color: #fff;
  font-weight: 600;
  letter-spacing: 1px;
  padding: 10px 24px;

  &:hover {
    opacity: 0.9;
    box-shadow: 0 0 15px rgba(6, 182, 212, 0.4);
  }
}

/* 3. 内容区 */
.content {
  flex: 1;
  padding: 32px 40px;
  width: 100%;
  position: relative;
  z-index: 10;
}

.content-inner {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

.footer {
  text-align: center;
  padding: 24px 0;
  color: #475569;
  font-size: 11px;
  letter-spacing: 1px;
  position: relative;
  z-index: 10;
}

.footer-line {
  width: 60px;
  height: 2px;
  background: #334155;
  margin: 0 auto 12px;
}

/* 路由动画: Holo Fade */
.holo-fade-enter-active,
.holo-fade-leave-active {
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.holo-fade-enter-from {
  opacity: 0;
  transform: scale(0.98) translateY(10px);
  filter: blur(4px);
}

.holo-fade-leave-to {
  opacity: 0;
  transform: scale(1.02);
  filter: blur(4px);
}

/* 移动端适配 */
@media (max-width: 768px) {
  .header-inner { padding: 0 16px; }
  .content { padding: 20px 16px; }
  .logo-sub { display: none; }
  .user-info { display: none; }
  .user-trigger {
    padding: 6px;
    background: transparent;
    border: none;
    &:hover { background: rgba(255,255,255,0.1); }
  }
  .arrow { display: none; }
  .avatar-frame { border: none; padding: 0; }
}
</style>

<style lang="scss">
/* 全局覆盖 - 深色下拉菜单 */
.cyber-user-dropdown {
  background: rgba(15, 23, 42, 0.9) !important;
  backdrop-filter: blur(16px) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  border-radius: 12px !important;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5) !important;
  padding: 8px !important;

  .el-dropdown-menu__item {
    color: #cbd5e1 !important;
    border-radius: 6px;
    margin-bottom: 2px;
    padding: 10px 16px;
    font-weight: 500;

    &:hover, &:focus {
      background-color: rgba(6, 182, 212, 0.15) !important;
      color: #fff !important;
    }

    .el-icon {
      margin-right: 8px;
      font-size: 16px;
    }
  }

  .menu-header {
    padding: 8px 16px;
    font-size: 10px;
    color: #64748b;
    font-weight: 700;
    letter-spacing: 1px;
    border-bottom: 1px solid rgba(255,255,255,0.05);
    margin-bottom: 6px;
  }

  .admin-item {
    color: #fbbf24 !important;
    &:hover { background-color: rgba(251, 191, 36, 0.15) !important; }
  }

  .danger-item {
    color: #f87171 !important;
    &:hover { background-color: rgba(248, 113, 113, 0.15) !important; }
  }

  .el-dropdown-menu__item--divided {
    margin-top: 6px;
    border-top-color: rgba(255, 255, 255, 0.1) !important;
  }

  /* 隐藏默认箭头 */
  .el-popper__arrow { display: none; }
}
</style>
