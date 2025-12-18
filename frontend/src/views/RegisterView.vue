<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();

const handleRegister = () => {
  // 占位注册逻辑
  router.push('/login');
};
</script>

<template>
  <div class="page">
    <h1>注册</h1>
    <p>此处为注册页面占位。</p>
    <button type="button" @click="handleRegister">注册完成</button>
  </div>
</template>

<style scoped>
.page {
  max-width: 420px;
  margin: 40px auto;
  padding: 24px;
  border: 1px solid #ddd;
  border-radius: 8px;
}
</style>
