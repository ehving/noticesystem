<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { success } from '@/utils/message'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loading = ref(false)
const formRef = ref()
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
    } catch (e) {
      console.error(e)
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
    <div class="card">
      <h2 class="title">登录</h2>
      <el-form ref="formRef" label-position="top" :model="form" :rules="rules" @submit.prevent="onSubmit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="onSubmit">登 录</el-button>
        </el-form-item>
        <div class="footer">
          <span>还没有账号？</span>
          <el-link type="primary" @click="goRegister">去注册</el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}

.card {
  width: 380px;
  padding: 24px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.title {
  margin: 0 0 16px;
  text-align: center;
}

.footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #909399;
}
</style>
