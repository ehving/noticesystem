<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime } from '@/utils/time'

const authStore = useAuthStore()
const profile = computed(() => authStore.profile)
</script>

<template>
  <div class="page">
    <el-card>
      <div class="header">
        <div class="title">个人信息</div>
      </div>
      <div v-if="profile">
        <p>用户名：{{ profile.username }}</p>
        <p>昵称：{{ profile.nickname || '-' }}</p>
        <p>角色：{{ profile.roleName || '-' }}</p>
        <p>部门：{{ profile.deptName || '未设置' }}</p>
        <p>最后登录：{{ formatDateTime(profile.lastLoginTime) }}</p>
      </div>
      <div v-else>未登录或加载中...</div>
    </el-card>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.title {
  font-weight: 600;
}
</style>
