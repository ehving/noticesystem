<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { updatePassword } from '@/api/modules/auth'
import { success, error as showError } from '@/utils/message'
import type { FormInstance } from 'element-plus'
import { Lock, Key, Check, Close } from '@element-plus/icons-vue'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmNewPassword: '',
})

const rules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmNewPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_: unknown, value: string, callback: (error?: Error) => void) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

const onSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    if (form.newPassword !== form.confirmNewPassword) {
      showError('两次输入的密码不一致')
      return
    }
    loading.value = true
    try {
      await updatePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
      success('密钥更新成功')
      form.oldPassword = ''
      form.newPassword = ''
      form.confirmNewPassword = ''
      // 可选：修改成功后跳转回档案页
      // setTimeout(() => onCancel(), 1000)
    } finally {
      loading.value = false
    }
  })
}

const onCancel = () => {
  router.push('/profile')
}
</script>

<template>
  <div class="pwd-container">
    <div class="pwd-card">
      <!-- 顶部安全警示条 -->
      <div class="security-bar"></div>

      <div class="card-header">
        <div class="header-icon-box">
          <el-icon><ShieldCheck /></el-icon>
        </div>
        <div class="header-text">
          <div class="title">SECURITY PROTOCOL</div>
          <div class="subtitle">密钥重置终端 // ENCRYPTION LEVEL: HIGH</div>
        </div>
      </div>

      <div class="form-wrapper">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large" class="cyber-form">

          <div class="form-group">
            <label class="cyber-label">CURRENT ACCESS KEY // 旧密码</label>
            <el-form-item prop="oldPassword">
              <el-input
                v-model="form.oldPassword"
                type="password"
                show-password
                placeholder="请输入当前使用的密码"
                :prefix-icon="Lock"
                class="cyber-input"
              />
            </el-form-item>
          </div>

          <div class="form-group">
            <label class="cyber-label">NEW ACCESS KEY // 新密码</label>
            <el-form-item prop="newPassword">
              <el-input
                v-model="form.newPassword"
                type="password"
                show-password
                placeholder="请输入新的安全密码"
                :prefix-icon="Key"
                class="cyber-input"
              />
            </el-form-item>
          </div>

          <div class="form-group">
            <label class="cyber-label">VERIFY ACCESS KEY // 确认新密码</label>
            <el-form-item prop="confirmNewPassword">
              <el-input
                v-model="form.confirmNewPassword"
                type="password"
                show-password
                placeholder="请再次输入以确认"
                :prefix-icon="Check"
                class="cyber-input"
              />
            </el-form-item>
          </div>

          <el-form-item class="actions-item">
            <div class="actions">
              <el-button
                type="primary"
                :loading="loading"
                @click="onSubmit"
                class="cyber-btn primary"
              >
                <el-icon><Check /></el-icon> UPDATE KEY
              </el-button>
              <el-button @click="onCancel" class="cyber-btn secondary">
                <el-icon><Close /></el-icon> CANCEL
              </el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.pwd-container {
  max-width: 600px;
  margin: 40px auto;
  padding: 20px;
  position: relative;
}

.pwd-card {
  background: rgba(30, 41, 59, 0.6);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.3);
  position: relative;
}

/* 顶部安全条 */
.security-bar {
  height: 4px;
  width: 100%;
  background: linear-gradient(90deg, #f59e0b, #ef4444); /* Amber to Red for security vibe */
  box-shadow: 0 0 10px rgba(239, 68, 68, 0.5);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 32px 40px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.header-icon-box {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid rgba(245, 158, 11, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #f59e0b;
  box-shadow: 0 0 15px rgba(245, 158, 11, 0.2);
}

.header-text {
  .title {
    font-size: 20px;
    font-weight: 700;
    color: #f1f5f9;
    letter-spacing: 1px;
  }
  .subtitle {
    font-size: 10px;
    color: #f59e0b;
    font-family: 'Menlo', monospace;
    margin-top: 4px;
    opacity: 0.8;
  }
}

.form-wrapper {
  padding: 32px 40px 40px;
}

.form-group {
  margin-bottom: 24px;
}

.cyber-label {
  display: block;
  font-size: 11px;
  color: #94a3b8;
  margin-bottom: 8px;
  font-family: 'Menlo', monospace;
  letter-spacing: 0.5px;
}

/* Custom Input Styling (Reusable Cyber Style) */
:deep(.cyber-input) {
  .el-input__wrapper {
    background-color: rgba(15, 23, 42, 0.4) !important;
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1) inset !important;
    border-radius: 8px;
    padding: 8px 12px;
    transition: all 0.3s ease;

    &.is-focus {
      box-shadow: 0 0 0 1px #f59e0b inset !important; /* Amber glow for security */
      background-color: rgba(245, 158, 11, 0.05) !important;
    }
  }

  .el-input__inner {
    color: #f1f5f9 !important;
    height: 36px;
    &::placeholder {
      color: #475569;
    }
  }

  .el-input__prefix-inner {
    color: #64748b;
  }

  .is-focus .el-input__prefix-inner {
    color: #f59e0b;
  }
}

.actions {
  display: flex;
  gap: 16px;
  width: 100%;
  margin-top: 16px;
}

.cyber-btn {
  flex: 1;
  height: 44px;
  border-radius: 8px;
  font-weight: 600;
  letter-spacing: 1px;
  border: none;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s;

  &.primary {
    background: linear-gradient(90deg, #f59e0b, #ea580c); /* Amber to Orange */
    color: #fff;
    &:hover {
      box-shadow: 0 0 20px rgba(245, 158, 11, 0.4);
      transform: translateY(-1px);
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

.actions-item {
  margin-bottom: 0;
}
</style>
