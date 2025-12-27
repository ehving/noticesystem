<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { success } from '@/utils/message'
import type { FormInstance } from 'element-plus'
import { User, Lock, Monitor, Right, Star } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const onSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    loading.value = true
    try {
      await authStore.login(form.username, form.password)
      success('登录成功')
      const redirect = (route.query.redirect as string) || (authStore.isAdmin ? '/admin' : '/notices')
      router.replace(redirect)
    } finally {
      loading.value = false
    }
  })
}

const goRegister = () => {
  router.push({ path: '/register', query: route.query })
}
</script>

<template>
  <div class="login-wrapper">
    <div class="login-card">
      <!-- 左侧：全息投影风格品牌区 -->
      <div class="visual-side">
        <div class="visual-bg"></div>
        <div class="brand-container">
          <div class="logo-holo">
            <div class="holo-ring"></div>
            <el-icon class="holo-icon"><Monitor /></el-icon>
          </div>
          <div class="brand-title">公告管理系统</div>
          <div class="brand-slogan">
            <span>数据同步</span>
            <span class="dot">·</span>
            <span>实时监控</span>
            <span class="dot">·</span>
            <span>安全可靠</span>
          </div>
        </div>

        <!-- 装饰性数据流 -->
        <div class="data-stream">
          <div class="stream-line s1"></div>
          <div class="stream-line s2"></div>
          <div class="stream-line s3"></div>
        </div>

        <div class="glass-reflection"></div>
      </div>

      <!-- 右侧：控制台风格表单 -->
      <div class="form-side">
        <div class="form-header">
          <h3 class="title">欢迎回来</h3>
          <p class="subtitle">请输入您的凭证以访问终端</p>
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
              placeholder="用户名 / 账号"
              :prefix-icon="User"
              class="cyber-input"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              show-password
              :prefix-icon="Lock"
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
              <span>启动登录程序</span>
              <el-icon class="btn-icon"><Right /></el-icon>
              <div class="btn-glitch"></div>
            </el-button>
          </el-form-item>

          <div class="form-footer">
            <span class="footer-text">还没有账号？</span>
            <span class="register-link" @click="goRegister">
              立即注册 <el-icon><Star /></el-icon>
            </span>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
/* --- 容器布局 --- */
.login-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  perspective: 1000px;
}

.login-card {
  width: 960px;
  height: 560px;
  background: rgba(15, 23, 42, 0.6); /* 深色半透明背景 */
  backdrop-filter: blur(20px); /* 强磨砂效果 */
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

/* --- 左侧视觉区 --- */
.visual-side {
  width: 45%;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  overflow: hidden;
  border-right: 1px solid rgba(255, 255, 255, 0.05);
}

/* 动态背景光效 */
.visual-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 0% 0%, rgba(79, 70, 229, 0.2) 0%, transparent 50%),
    radial-gradient(circle at 100% 100%, rgba(14, 165, 233, 0.2) 0%, transparent 50%);
  z-index: 0;
}

.brand-container {
  position: relative;
  z-index: 2;
  text-align: center;
}

/* 全息 Logo */
.logo-holo {
  width: 100px;
  height: 100px;
  margin: 0 auto 24px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.holo-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 2px solid rgba(56, 189, 248, 0.3);
  border-top-color: #38bdf8;
  border-bottom-color: #38bdf8;
  animation: spin 6s linear infinite;
  box-shadow: 0 0 15px rgba(56, 189, 248, 0.3);
}

.holo-icon {
  font-size: 48px;
  color: #fff;
  filter: drop-shadow(0 0 10px rgba(56, 189, 248, 0.8));
  animation: float 4s ease-in-out infinite;
}

.brand-title {
  font-size: 32px;
  font-weight: 800;
  color: #fff;
  letter-spacing: 2px;
  margin-bottom: 12px;
  text-transform: uppercase;
  background: linear-gradient(to right, #fff, #94a3b8);
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
  color: #38bdf8;
  font-weight: bold;
}

/* 装饰线条 */
.data-stream {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}

.stream-line {
  position: absolute;
  background: linear-gradient(to bottom, transparent, rgba(56, 189, 248, 0.1), transparent);
  width: 1px;
  height: 100%;
  opacity: 0.5;
}

.s1 { left: 20%; animation: stream 5s infinite; }
.s2 { left: 50%; animation: stream 7s infinite 1s; }
.s3 { left: 80%; animation: stream 4s infinite 2s; }

/* --- 右侧表单区 --- */
.form-side {
  flex: 1;
  padding: 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  z-index: 2;
}

.form-header {
  margin-bottom: 48px;
}

.title {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 8px;
  letter-spacing: 1px;
}

.subtitle {
  color: #94a3b8;
  font-size: 14px;
}

/* 深度定制 Element Input */
:deep(.cyber-input) {
  --el-input-text-color: #fff;
  --el-input-bg-color: transparent;
  --el-input-placeholder-color: #64748b;
  --el-input-hover-border-color: rgba(56, 189, 248, 0.5);
  --el-input-focus-border-color: #38bdf8;

  .el-input__wrapper {
    /* 完全透明背景，依靠 box-shadow 绘制细腻边框 */
    background-color: transparent !important;
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.15) inset;
    border-radius: 12px;
    padding: 8px 16px;
    transition: all 0.3s ease;

    &:hover {
      box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.3) inset;
    }

    &.is-focus {
      background-color: rgba(255, 255, 255, 0.02) !important; /* 极微弱的聚焦背景 */
      box-shadow: 0 0 0 1px #38bdf8 inset, 0 0 15px rgba(56, 189, 248, 0.2);
    }
  }

  .el-input__inner {
    height: 40px;
    font-size: 15px;

    /* 关键修复：利用 transition 延迟背景色变化，
      解决 Chrome 自动填充导致出现不透明背景块的问题
    */
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

  /* 聚焦时图标变亮 */
  .el-input__wrapper.is-focus .el-input__prefix-inner {
    color: #38bdf8;
  }
}

/* 赛博风格按钮 */
.cyber-btn {
  width: 100%;
  height: 50px;
  background: linear-gradient(90deg, #4f46e5, #0ea5e9);
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 1.5px;
  color: #fff;
  margin-top: 16px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s;
  box-shadow: 0 4px 15px rgba(79, 70, 229, 0.3);

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 20px rgba(79, 70, 229, 0.4);
    background: linear-gradient(90deg, #4338ca, #0284c7);
  }

  /* 按钮光效扫过动画 */
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
  margin-top: 32px;
  text-align: center;
  font-size: 14px;
}

.footer-text {
  color: #64748b;
  margin-right: 8px;
}

.register-link {
  color: #38bdf8;
  cursor: pointer;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: all 0.2s;

  &:hover {
    color: #7dd3fc;
    text-shadow: 0 0 10px rgba(56, 189, 248, 0.4);
  }
}

/* 动画定义 */
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

@keyframes stream {
  0% { transform: translateY(-100%); opacity: 0; }
  50% { opacity: 0.5; }
  100% { transform: translateY(100%); opacity: 0; }
}

@keyframes shine {
  0% { left: -100%; }
  20% { left: 100%; }
  100% { left: 100%; }
}

/* 响应式适配 */
@media (max-width: 992px) {
  .login-card {
    width: 90%;
    height: auto;
    flex-direction: column;
    max-width: 500px;
  }

  .visual-side {
    width: 100%;
    padding: 30px;
    border-right: none;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }

  .logo-holo {
    width: 80px;
    height: 80px;
    margin-bottom: 16px;
  }

  .holo-icon {
    font-size: 36px;
  }

  .form-side {
    padding: 40px 30px;
  }
}
</style>
