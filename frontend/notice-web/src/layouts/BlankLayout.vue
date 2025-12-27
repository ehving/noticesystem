<script setup lang="ts">
import { ref } from 'vue'
import {
  QuestionFilled,
  Monitor,
  DataLine,
  Connection,
  Iphone,
  CloseBold
} from '@element-plus/icons-vue'

const showAbout = ref(false)
</script>

<template>
  <div class="blank-layout">
    <!-- 1. 动态背景系统 -->
    <div class="bg-system">
      <div class="bg-color"></div>
      <!-- 流动的光斑 -->
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
      <!-- 科技网格纹理 -->
      <div class="grid-overlay"></div>
    </div>

    <!-- 2. 顶部操作区 -->
    <div class="top-actions">
      <div class="glass-pill" @click="showAbout = true">
        <div class="icon-wrap">
          <el-icon><QuestionFilled /></el-icon>
        </div>
        <span class="text">About System</span>
      </div>
    </div>

    <!-- 3. 内容容器 (Router View) -->
    <div class="content-stage">
      <router-view v-slot="{ Component }">
        <transition name="zoom-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>

    <!-- 4. 页脚版权 -->
    <div class="layout-footer">
      <span>Notice Management System © 2024</span>
    </div>

    <!-- 5. 关于弹窗 (完全自定义样式) -->
    <el-dialog
      v-model="showAbout"
      width="480px"
      align-center
      class="custom-about-modal"
      :show-close="false"
      destroy-on-close
    >
      <div class="modal-body">
        <!-- 弹窗头部装饰 -->
        <div class="modal-header-banner">
          <div class="logo-ring">
            <el-icon><Monitor /></el-icon>
          </div>
          <button class="close-icon-btn" @click="showAbout = false">
            <el-icon><CloseBold /></el-icon>
          </button>
        </div>

        <div class="modal-content">
          <h2 class="app-title">Notice System <span class="version">v2.0</span></h2>
          <p class="app-slogan">Enterprise Data Sync & Monitoring Solution</p>

          <div class="divider-line"></div>

          <p class="description">
            这是一个集成了多数据库支持（MySQL / PostgreSQL / SQL Server）的综合实践项目，旨在提供高效的数据同步与可视化监控解决方案。
          </p>

          <div class="features-row">
            <div class="feature-item">
              <div class="f-icon i-blue"><el-icon><Connection /></el-icon></div>
              <span class="f-label">实时同步</span>
            </div>
            <div class="feature-item">
              <div class="f-icon i-purple"><el-icon><DataLine /></el-icon></div>
              <span class="f-label">冲突检测</span>
            </div>
            <div class="feature-item">
              <div class="f-icon i-green"><el-icon><Iphone /></el-icon></div>
              <span class="f-label">移动适配</span>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <p>Designed for Developers</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
/* --- 布局核心 --- */
.blank-layout {
  min-height: 100vh;
  width: 100%;
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  font-family: 'Inter', system-ui, sans-serif;
  color: #fff;
}

/* --- 1. 背景系统 (Deep Space Theme) --- */
.bg-system {
  position: absolute;
  inset: 0;
  z-index: 0;
  background-color: #0f172a; /* 深邃蓝黑底色 */
}

.bg-color {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 50% 0%, #1e293b 0%, #0f172a 100%);
  z-index: 1;
}

.grid-overlay {
  position: absolute;
  inset: 0;
  z-index: 3;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px);
  background-size: 50px 50px;
  mask-image: radial-gradient(circle at center, black 40%, transparent 100%);
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  z-index: 2;
  opacity: 0.6;
  animation: floatOrb 20s infinite ease-in-out;
}

.orb-1 {
  top: -10%;
  left: -10%;
  width: 50vw;
  height: 50vw;
  background: #4f46e5; /* Indigo */
  animation-delay: 0s;
}

.orb-2 {
  bottom: -20%;
  right: -10%;
  width: 60vw;
  height: 60vw;
  background: #0ea5e9; /* Sky Blue */
  animation-delay: -5s;
}

.orb-3 {
  top: 40%;
  left: 30%;
  width: 40vw;
  height: 40vw;
  background: #ec4899; /* Pink */
  opacity: 0.3;
  animation-delay: -10s;
}

@keyframes floatOrb {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -50px) scale(1.1); }
  66% { transform: translate(-20px, 20px) scale(0.9); }
}

/* --- 2. 顶部操作区 --- */
.top-actions {
  position: absolute;
  top: 24px;
  right: 24px;
  z-index: 10;
}

.glass-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px 6px 6px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 30px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    background: rgba(255, 255, 255, 0.2);
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
    border-color: rgba(255, 255, 255, 0.4);

    .icon-wrap {
      transform: rotate(15deg) scale(1.1);
      background: #fff;
      color: #0f172a;
    }
  }
}

.icon-wrap {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.glass-pill .text {
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.5px;
}

/* --- 3. 内容舞台 --- */
.content-stage {
  position: relative;
  z-index: 5;
  width: 100%;
  display: flex;
  justify-content: center;
  /* router-view 里的内容通常是登录卡片，这里给它一个居中环境 */
}

/* --- 4. 页脚 --- */
.layout-footer {
  position: absolute;
  bottom: 24px;
  z-index: 5;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  letter-spacing: 1px;
}

/* --- 5. 弹窗自定义样式 --- */
/* 注意：由于 el-dialog 渲染在 body 下，需要使用 global 样式或者 deep */
:global(.custom-about-modal) {
  border-radius: 20px !important;
  background: transparent !important; /* 移除默认背景 */
  box-shadow: none !important; /* 移除默认阴影 */
  overflow: visible !important;

  .el-dialog__header,
  .el-dialog__body {
    padding: 0 !important;
    background: transparent !important;
  }
}

.modal-body {
  background: #fff;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  animation: modalPop 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modalPop {
  from { transform: scale(0.95); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

.modal-header-banner {
  height: 100px;
  background: linear-gradient(135deg, #4f46e5 0%, #0ea5e9 100%);
  position: relative;
  display: flex;
  justify-content: center;
  align-items: flex-end;
}

.close-icon-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  background: rgba(0, 0, 0, 0.2);
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;

  &:hover {
    background: rgba(0, 0, 0, 0.4);
  }
}

.logo-ring {
  width: 64px;
  height: 64px;
  background: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #4f46e5;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  transform: translateY(50%); /* 下沉一半 */
  z-index: 2;
}

.modal-content {
  padding: 40px 32px 24px;
  text-align: center;
  color: #1e293b;
}

.app-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.version {
  font-size: 12px;
  background: #e0e7ff;
  color: #4f46e5;
  padding: 2px 6px;
  border-radius: 4px;
  vertical-align: middle;
}

.app-slogan {
  margin: 8px 0 0;
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

.divider-line {
  height: 1px;
  background: #e2e8f0;
  margin: 20px 0;
}

.description {
  font-size: 14px;
  line-height: 1.6;
  color: #475569;
  margin-bottom: 24px;
  text-align: justify;
  text-align-last: center;
}

.features-row {
  display: flex;
  justify-content: space-around;
  margin-bottom: 12px;
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.f-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  transition: transform 0.2s;

  &:hover { transform: scale(1.1); }

  &.i-blue { background: #eff6ff; color: #3b82f6; }
  &.i-purple { background: #f5f3ff; color: #8b5cf6; }
  &.i-green { background: #f0fdf4; color: #22c55e; }
}

.f-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.modal-footer {
  background: #f8fafc;
  padding: 12px;
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
  border-top: 1px solid #f1f5f9;
}

/* 路由切换动画 */
.zoom-fade-enter-active,
.zoom-fade-leave-active {
  transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

.zoom-fade-enter-from {
  opacity: 0;
  transform: scale(0.95) translateY(20px);
}

.zoom-fade-leave-to {
  opacity: 0;
  transform: scale(1.05);
}
</style>
