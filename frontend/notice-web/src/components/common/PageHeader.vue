<script setup lang="ts">
defineProps<{
  title: string
  subTitle?: string
}>()
</script>

<template>
  <div class="page-header">
    <div class="title-wrap">
      <div class="title">{{ title }}</div>
      <div v-if="subTitle" class="sub">{{ subTitle }}</div>
    </div>
    <div class="actions">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.title {
  font-weight: 600;
  font-size: 16px;
}
.sub {
  color: #909399;
  font-size: 12px;
}
.actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
