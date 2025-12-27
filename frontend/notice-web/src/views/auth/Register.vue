<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { register as registerApi } from '@/api/modules/auth'
import { success } from '@/utils/message'
import type { FormInstance } from 'element-plus'
import { User, Lock, Message, Iphone, OfficeBuilding, Monitor, Right, Edit, Connection } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  deptId: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

const onSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    loading.value = true
    try {
      await registerApi({
        username: form.username,
        password: form.password,
        nickname: form.nickname || undefined,
        email: form.email || undefined,
        phone: form.phone || undefined,
        deptId: form.deptId || undefined,
      })
      success('注册成功，请登录')
      const redirect = (route.query.redirect as string) || '/login'
      router.replace(redirect)
    } finally {
      loading.value = false
    }
  })
}

const goLogin = () => {
  router.push({ path: '/login', query: route.query })
}
</script>

<template>
  <div class="register-wrapper">
    <div class="register-card">

      <!-- 左侧：控制台风格表单 (Layout Flip) -->
      <div class="form-side">
        <div class="form-header">
          <h3 class="title">创建新身份</h3>
          <p class="subtitle">初始化账户参数以接入网络</p>
        </div>

        <el-form
          ref="formRef"
          size="large"
          :model="form"
          :rules="rules"
          @submit.prevent="onSubmit"
          class="custom-form"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="设定用户名 (必填)"
              :prefix-icon="User"
              class="cyber-input"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="设定密码 (必填)"
              show-password
              :prefix-icon="Lock"
              class="cyber-input"
            />
          </el-form-item>

          <!-- 并排布局 -->
          <div class="form-row">
            <el-form-item prop="nickname" class="half-item">
              <el-input
                v-model="form.nickname"
                placeholder="昵称"
                :prefix-icon="Edit"
                class="cyber-input"
              />
            </el-form-item>
            <el-form-item prop="phone" class="half-item">
              <el-input
                v-model="form.phone"
                placeholder="手机终端"
                :prefix-icon="Iphone"
                class="cyber-input"
              />
            </el-form-item>
          </div>

          <el-form-item prop="email">
            <el-input
              v-model="form.email"
              placeholder="电子邮箱"
              :prefix-icon="Message"
              class="cyber-input"
            />
          </el-form-item>

          <el-form-item prop="deptId">
            <el-input
              v-model="form.deptId"
              placeholder="部门单元 ID (可选)"
              :prefix-icon="OfficeBuilding"
              class="cyber-input"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              class="cyber-btn"
              @click="onSubmit"
            >
              <span>执行注册程序</span>
              <el-icon class="btn-icon"><Right /></el-icon>
              <div class="btn-glitch"></div>
            </el-button>
          </el-form-item>

          <div class="form-footer">
            <span class="footer-text">已拥有访问权限？</span>
            <span class="login-link" @click="goLogin">
              返回登录终端 <el-icon><Connection /></el-icon>
            </span>
          </div>
        </el-form>
      </div>

      <!-- 右侧：全息投影视觉区 (Visual Side) -->
      <div class="visual-side">
        <div class="visual-bg"></div>
        <div class="brand-container">
          <!-- 不同的视觉 Logo：六边形结构代表“构建” -->
          <div class="logo-construct">
            <div class="hex-outer"></div>
            <div class="hex-inner"></div>
            <el-icon class="construct-icon"><Monitor /></el-icon>
          </div>

          <div class="brand-title">加入协作网络</div>
          <div class="brand-slogan">
            <span>高效构建</span>
            <span class="dot">·</span>
            <span>无缝链接</span>
          </div>
        </div>

        <!-- 装饰性网格 -->
        <div class="grid-mesh"></div>
        <div class="glass-reflection"></div>
      </div>

    </div>
  </div>
</template>

<style scoped lang="scss">
/* --- 容器布局 --- */
.register-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  perspective: 1000px;
}

.register-card {
  width: 1000px; /* 比登录页稍宽以容纳更多内容 */
  min-height: 640px;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  box-shadow:
    0 0 0 1px rgba(0, 0, 0, 0.2),
    0 20px 50px rgba(0, 0, 0, 0.4),
    inset 0 0 60px rgba(0, 0, 0, 0.5);
  display: flex;
  overflow: hidden;
  position: relative;
  transition: transform 0.3s ease, box-shadow 0.3s ease;

  &:hover {
    transform: translateY(-5px);
    box-shadow:
      0 0 0 1px rgba(255, 255, 255, 0.15),
      0 30px 60px rgba(0, 0, 0, 0.5),
      inset 0 0 60px rgba(0, 0, 0, 0.5);
  }
}

/* --- 左侧表单区 (Form Side) --- */
.form-side {
  flex: 1;
  padding: 48px 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  z-index: 2;
  /* 表单在左侧，右边框分割 */
  border-right: 1px solid rgba(255, 255, 255, 0.05);
}

.form-header {
  margin-bottom: 32px;
}

.title {
  font-size: 30px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 8px;
  letter-spacing: 1px;
}

.subtitle {
  color: #94a3b8;
  font-size: 14px;
}

.form-row {
  display: flex;
  gap: 16px;
  .half-item {
    flex: 1;
  }
}

/* 深度定制 Element Input (复用登录页风格) */
:deep(.cyber-input) {
  --el-input-text-color: #fff;
  --el-input-bg-color: transparent;
  --el-input-placeholder-color: #64748b;
  --el-input-hover-border-color: rgba(45, 212, 191, 0.5); /* 注册页改用青色 Teal */
  --el-input-focus-border-color: #2dd4bf;

  .el-input__wrapper {
    background-color: transparent !important;
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.15) inset;
    border-radius: 12px;
    padding: 8px 16px;
    transition: all 0.3s ease;

    &:hover {
      box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.3) inset;
    }

    &.is-focus {
      background-color: rgba(45, 212, 191, 0.05) !important;
      box-shadow: 0 0 0 1px #2dd4bf inset, 0 0 15px rgba(45, 212, 191, 0.2);
    }
  }

  .el-input__inner {
    height: 40px;
    font-size: 14px;

    &:-webkit-autofill,
    &:-webkit-autofill:hover,
    &:-webkit-autofill:focus,
    &:-webkit-autofill:active {
      -webkit-text-fill-color: #fff !important;
      transition: background-color 99999s ease-in-out 0s;
      caret-color: #fff;
    }
  }

  .el-input__prefix-inner {
    color: #94a3b8;
    font-size: 18px;
    transition: color 0.3s;
  }

  .el-input__wrapper.is-focus .el-input__prefix-inner {
    color: #2dd4bf;
  }
}

/* 赛博风格按钮 - 注册页变种 (青色/绿色渐变) */
.cyber-btn {
  width: 100%;
  height: 48px;
  background: linear-gradient(90deg, #0d9488, #0ea5e9); /* Teal to Blue */
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 1.5px;
  color: #fff;
  margin-top: 8px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s;
  box-shadow: 0 4px 15px rgba(13, 148, 136, 0.3);

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 20px rgba(13, 148, 136, 0.4);
    background: linear-gradient(90deg, #0f766e, #0284c7);
  }

  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
    animation: shine 3s infinite;
  }

  .btn-icon {
    margin-left: 8px;
    transition: transform 0.3s;
  }

  &:hover .btn-icon {
    transform: translateX(4px);
  }
}

.form-footer {
  margin-top: 24px;
  text-align: center;
  font-size: 14px;
}

.footer-text {
  color: #64748b;
  margin-right: 8px;
}

.login-link {
  color: #2dd4bf;
  cursor: pointer;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: all 0.2s;

  &:hover {
    color: #5eead4;
    text-shadow: 0 0 10px rgba(45, 212, 191, 0.4);
  }
}

/* --- 右侧视觉区 (Visual Side) --- */
.visual-side {
  width: 40%;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  overflow: hidden;
}

.visual-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 100% 0%, rgba(45, 212, 191, 0.15) 0%, transparent 50%),
    radial-gradient(circle at 0% 100%, rgba(14, 165, 233, 0.15) 0%, transparent 50%);
  z-index: 0;
}

.brand-container {
  position: relative;
  z-index: 2;
  text-align: center;
}

/* 构造 Logo (Hexagon) */
.logo-construct {
  width: 100px;
  height: 100px;
  margin: 0 auto 24px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hex-outer {
  position: absolute;
  inset: 0;
  clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
  background: rgba(45, 212, 191, 0.1);
  border: 1px solid rgba(45, 212, 191, 0.3);
  animation: pulse 4s ease-in-out infinite;
}

.hex-inner {
  position: absolute;
  inset: 15px;
  clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
  border: 2px solid #2dd4bf;
  box-shadow: 0 0 15px rgba(45, 212, 191, 0.3);
  animation: spin-reverse 10s linear infinite;
}

.construct-icon {
  font-size: 36px;
  color: #fff;
  filter: drop-shadow(0 0 10px rgba(45, 212, 191, 0.8));
  z-index: 2;
}

.brand-title {
  font-size: 28px;
  font-weight: 800;
  color: #fff;
  letter-spacing: 2px;
  margin-bottom: 12px;
  background: linear-gradient(to right, #fff, #99f6e4);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.brand-slogan {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  color: #94a3b8;
  font-weight: 500;
  letter-spacing: 1px;
}

.dot {
  color: #2dd4bf;
}

/* 装饰网格 */
.grid-mesh {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(45, 212, 191, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(45, 212, 191, 0.05) 1px, transparent 1px);
  background-size: 30px 30px;
  mask-image: radial-gradient(circle at center, black 30%, transparent 80%);
  z-index: 1;
  animation: moveGrid 20s linear infinite;
}

/* 动画定义 */
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
@keyframes spin-reverse { from { transform: rotate(360deg); } to { transform: rotate(0deg); } }

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(1.1); opacity: 0.8; }
}

@keyframes shine {
  0% { left: -100%; }
  20% { left: 100%; }
  100% { left: 100%; }
}

@keyframes moveGrid {
  0% { background-position: 0 0; }
  100% { background-position: 30px 30px; }
}

/* 响应式适配 */
@media (max-width: 992px) {
  .register-card {
    width: 90%;
    min-height: auto;
    flex-direction: column-reverse; /* 移动端先显示视觉区，再显示表单 */
    max-width: 500px;
  }

  .form-side {
    padding: 40px 30px;
    border-right: none;
    border-top: 1px solid rgba(255, 255, 255, 0.1);
  }

  .visual-side {
    width: 100%;
    padding: 30px;
    min-height: 200px;
  }

  .logo-construct {
    width: 70px;
    height: 70px;
  }

  .construct-icon { font-size: 28px; }
}
</style>
