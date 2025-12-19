<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const toLogin = () => {
  router.push({ path: '/login', query: { redirect: route.query.from || '/' } })
}

const toMobileReport = () => {
  router.push('/m/dashboard')
}

const logout = () => {
  authStore.logout()
  toLogin()
}
</script>

<template>
  <div class="page">
    <el-result
      icon="warning"
      title="移动端仅提供管理员报表"
      sub-title="公告/个人中心/管理端请在电脑端访问"
    >
      <template #extra>
        <div class="actions">
          <el-button v-if="!authStore.isLoggedIn" type="primary" @click="toLogin">去登录</el-button>
          <template v-else-if="authStore.isAdmin">
            <el-button type="primary" @click="toMobileReport">打开移动端报表</el-button>
            <el-button @click="logout">退出登录</el-button>
          </template>
          <template v-else>
            <el-button type="primary" @click="logout">退出登录</el-button>
          </template>
        </div>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.page {
  min-height: 70vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>
