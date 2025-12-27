<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { updateProfile } from '@/api/modules/auth'
import { success } from '@/utils/message'
import { formatDateTime } from '@/utils/time'
import type { FormInstance } from 'element-plus'
import {
  User, Message, Iphone, OfficeBuilding, Edit, Key,
  Calendar, Postcard, Avatar
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const profile = computed(() => authStore.profile)

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  nickname: '',
  email: '',
  phone: '',
  avatar: '',
})
const rules = {
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

const openEdit = () => {
  if (!profile.value) return
  form.nickname = profile.value.nickname || ''
  form.email = profile.value.email || ''
  form.phone = profile.value.phone || ''
  form.avatar = profile.value.avatar || ''
  dialogVisible.value = true
}

const onSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    await updateProfile({
      nickname: form.nickname || undefined,
      email: form.email || undefined,
      phone: form.phone || undefined,
      avatar: form.avatar || undefined,
    })
    success('档案更新成功')
    await authStore.fetchProfile()
    dialogVisible.value = false
  })
}

const openPwd = () => {
  router.push('/password')
}

const formatTime = (val?: string) => formatDateTime(val)
</script>

<template>
  <div class="profile-container">
    <div class="profile-card">
      <!-- 装饰：顶部扫描线 -->
      <div class="scan-line"></div>

      <!-- 顶部能量封面 -->
      <div class="profile-cover">
        <div class="cover-pattern"></div>
        <div class="cover-content">
          <h2 class="greeting">
            Welcome back, <span class="highlight">{{ profile?.nickname || profile?.username }}</span>
          </h2>
          <p class="welcome-text">:: SYSTEM ACCESS GRANTED ::</p>
        </div>
      </div>

      <div class="profile-body">
        <!-- 左侧：战术头像区 -->
        <div class="profile-sidebar">
          <div class="avatar-holo-container">
            <div class="holo-ring rotating"></div>
            <div class="holo-ring static"></div>
            <el-avatar :size="100" :src="profile?.avatar" class="main-avatar">
              {{ profile?.nickname?.[0]?.toUpperCase() || profile?.username?.[0]?.toUpperCase() }}
            </el-avatar>
            <div class="role-tag">
              <span class="status-dot"></span>
              {{ profile?.roleName || 'OPERATOR' }}
            </div>
          </div>

          <div class="identity-block">
            <h3 class="username">{{ profile?.nickname || profile?.username }}</h3>
            <p class="user-id">UID: {{ profile?.username }}</p>
          </div>

          <div class="action-buttons">
            <el-button class="cyber-btn primary" @click="openEdit">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button class="cyber-btn secondary" @click="openPwd">
              <el-icon><Key /></el-icon> 密保
            </el-button>
          </div>
        </div>

        <!-- 右侧：数据仪表盘 -->
        <div class="profile-content">
          <div class="section-header">
            <h4 class="section-title">BASIC INFO // 基础信息</h4>
            <div class="header-line"></div>
          </div>

          <div class="info-grid">
            <div class="info-card">
              <div class="label"><el-icon><OfficeBuilding /></el-icon> 隶属部门</div>
              <!-- 兼容 dept 对象和 deptName 字段 -->
              <div class="value">{{  profile?.deptName || '未分配' }}</div>
              <div class="card-bg-icon"><OfficeBuilding /></div>
            </div>

            <div class="info-card">
              <div class="label"><el-icon><Message /></el-icon> 通讯邮箱</div>
              <div class="value">{{ profile?.email || 'N/A' }}</div>
              <div class="card-bg-icon"><Message /></div>
            </div>

            <div class="info-card">
              <div class="label"><el-icon><Iphone /></el-icon> 移动终端</div>
              <div class="value">{{ profile?.phone || 'N/A' }}</div>
              <div class="card-bg-icon"><Iphone /></div>
            </div>

            <div class="info-card">
              <div class="label"><el-icon><Calendar /></el-icon> 注册时间</div>
              <div class="value">{{ formatTime(profile?.createTime) }}</div>
              <div class="card-bg-icon"><Calendar /></div>
            </div>

            <div class="info-card full-width">
              <div class="label"><el-icon><Postcard /></el-icon> 上次登录记录</div>
              <div class="value mono">{{ formatTime(profile?.lastLoginTime) }}</div>
              <div class="card-bg-icon"><Postcard /></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑资料弹窗 (深色适配) -->
    <el-dialog
      v-model="dialogVisible"
      title="更新档案信息"
      width="480px"
      destroy-on-close
      class="cyber-dialog"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="cyber-form">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="输入昵称" :prefix-icon="User" class="cyber-input" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="输入邮箱" :prefix-icon="Message" class="cyber-input" />
        </el-form-item>
        <el-form-item label="手机" prop="phone">
          <el-input v-model="form.phone" placeholder="输入手机号" :prefix-icon="Iphone" class="cyber-input" />
        </el-form-item>
        <!-- <el-form-item label="头像" prop="avatar">
          <el-input v-model="form.avatar" placeholder="网络图片URL" :prefix-icon="Avatar" class="cyber-input" />
        </el-form-item> -->
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="cyber-btn secondary" @click="dialogVisible = false">取消</el-button>
          <el-button class="cyber-btn primary" @click="onSave">保存更改</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.profile-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
  position: relative;
}

.profile-card {
  background: rgba(30, 41, 59, 0.6);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.3);
  position: relative;
}

/* 顶部装饰线 */
.scan-line {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, transparent, #06b6d4, transparent);
  box-shadow: 0 0 10px #06b6d4;
  z-index: 10;
}

/* Cover */
.profile-cover {
  height: 180px;
  position: relative;
  display: flex;
  align-items: center;
  padding: 0 40px;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.8) 0%, rgba(30, 41, 59, 0.9) 100%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.cover-pattern {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(6, 182, 212, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(6, 182, 212, 0.05) 1px, transparent 1px);
  background-size: 40px 40px;
  mask-image: linear-gradient(to bottom, black 0%, transparent 100%);
  pointer-events: none;
}

.cover-content {
  margin-left: 240px; /* Offset for avatar - Adjusted to match wider sidebar */
  z-index: 2;
}

.greeting {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 8px;
  letter-spacing: 0.5px;

  .highlight {
    background: linear-gradient(90deg, #06b6d4, #22d3ee);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

.welcome-text {
  font-size: 12px;
  color: #06b6d4;
  font-family: 'Menlo', monospace;
  letter-spacing: 2px;
  opacity: 0.8;
}

/* Body */
.profile-body {
  display: flex;
  padding: 0 40px 60px;
  gap: 40px;
}

/* Sidebar */
.profile-sidebar {
  width: 240px; /* Widened to fit side-by-side buttons */
  text-align: center;
  position: relative;
  top: -60px; /* Pull up */
  flex-shrink: 0;
}

.avatar-holo-container {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.holo-ring {
  position: absolute;
  inset: -10px;
  border-radius: 50%;
  border: 1px solid rgba(6, 182, 212, 0.3);

  &.rotating {
    border-top-color: #06b6d4;
    animation: spin 8s linear infinite;
  }
  &.static {
    border: 1px dashed rgba(255, 255, 255, 0.1);
    inset: -5px;
  }
}

.main-avatar {
  border: 4px solid #1e293b;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.5);
  background: #0f172a;
  color: #fff;
  font-size: 32px;
  font-weight: 700;
}

.role-tag {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(15, 23, 42, 0.9);
  border: 1px solid #06b6d4;
  color: #06b6d4;
  font-size: 10px;
  padding: 2px 10px;
  border-radius: 12px;
  white-space: nowrap;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
}

.status-dot {
  width: 6px;
  height: 6px;
  background: #06b6d4;
  border-radius: 50%;
  box-shadow: 0 0 5px #06b6d4;
}

.identity-block {
  margin-bottom: 24px;
}

.username {
  font-size: 20px;
  font-weight: 600;
  color: #f1f5f9;
  margin: 0 0 4px;
}

.user-id {
  font-size: 12px;
  color: #64748b;
  font-family: monospace;
}

.action-buttons {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  width: 100%;
}

/* Cyber Buttons */
.cyber-btn {
  width: 100%;
  height: 36px;
  border-radius: 6px;
  font-weight: 500;
  letter-spacing: 1px;
  transition: all 0.3s;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  font-size: 13px;

  .el-icon {
    margin-right: 4px;
  }

  &.primary {
    background: linear-gradient(90deg, #06b6d4, #3b82f6);
    color: #fff;
    &:hover {
      box-shadow: 0 0 15px rgba(6, 182, 212, 0.4);
    }
  }

  &.secondary {
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.1);
    color: #94a3b8;
    &:hover {
      background: rgba(255, 255, 255, 0.1);
      color: #fff;
    }
  }
}

/* Content */
.profile-content {
  flex: 1;
  padding-top: 20px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.section-title {
  font-size: 14px;
  font-weight: 700;
  color: #06b6d4;
  letter-spacing: 1px;
  margin: 0;
}

.header-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, rgba(6, 182, 212, 0.3), transparent);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.info-card {
  background: rgba(15, 23, 42, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 16px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s;

  &:hover {
    border-color: rgba(6, 182, 212, 0.3);
    background: rgba(15, 23, 42, 0.6);
  }
}

.info-card.full-width {
  grid-column: span 2;
}

.label {
  font-size: 12px;
  color: #94a3b8;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}

.value {
  font-size: 16px;
  color: #f1f5f9;
  font-weight: 500;
  position: relative;
  z-index: 2;

  &.mono {
    font-family: 'Menlo', monospace;
  }
}

.card-bg-icon {
  position: absolute;
  right: 10px;
  bottom: 5px;
  font-size: 40px;
  color: rgba(255, 255, 255, 0.03);
  z-index: 1;
  pointer-events: none;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .profile-cover {
    padding: 0 20px;
    justify-content: center;
  }
  .cover-content {
    margin-left: 0;
    text-align: center;
  }
  .profile-body {
    flex-direction: column;
    padding: 0 20px 30px;
    gap: 20px;
  }
  .profile-sidebar {
    width: 100%;
    top: -40px;
    margin-bottom: -20px;
  }
  .info-grid {
    grid-template-columns: 1fr;
  }
  .info-card.full-width {
    grid-column: span 1;
  }
}
</style>

<style lang="scss">
/* 全局 Dialog 样式适配 */
.cyber-dialog {
  background: rgba(15, 23, 42, 0.95) !important;
  backdrop-filter: blur(16px) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  border-radius: 12px !important;
  box-shadow: 0 20px 60px rgba(0,0,0,0.5) !important;

  .el-dialog__header {
    margin-right: 0 !important;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
    padding-bottom: 15px;
  }

  .el-dialog__title {
    color: #f1f5f9 !important;
    font-weight: 600;
  }

  .el-dialog__body {
    padding: 30px 20px !important;
  }

  .el-dialog__footer {
    border-top: 1px solid rgba(255, 255, 255, 0.05);
    padding-top: 15px;
  }
}

.cyber-form {
  .el-form-item__label {
    color: #94a3b8 !important;
  }
}

/* 复用之前的 cyber-input 样式，如果没有全局定义，这里需要再次确保 */
.cyber-input {
  .el-input__wrapper {
    background-color: rgba(0, 0, 0, 0.2) !important;
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1) inset !important;

    &.is-focus {
      box-shadow: 0 0 0 1px #06b6d4 inset !important;
    }
  }
  .el-input__inner {
    color: #fff !important;
  }
}
</style>
