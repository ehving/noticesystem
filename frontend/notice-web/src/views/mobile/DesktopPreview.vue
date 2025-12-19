<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const target = computed(() => {
  const path = (route.query.path as string) || '/m/dashboard'
  const url = new URL(path, window.location.origin)
  url.searchParams.set('embed', '1')
  return url.toString()
})
</script>

<template>
  <div class="wrapper">
    <div class="info">
      <h3>这是移动端报表</h3>
      <p>请在手机访问，或点击下方预览查看手机效果。</p>
    </div>
    <div class="phone-frame">
      <iframe :src="target" frameborder="0"></iframe>
    </div>
  </div>
</template>

<style scoped>
.wrapper {
  min-height: 80vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 24px;
}
.info {
  text-align: center;
}
.phone-frame {
  width: 390px;
  height: 844px;
  border: 12px solid #111;
  border-radius: 28px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  background: #000;
}
.phone-frame iframe {
  width: 100%;
  height: 100%;
}
</style>
