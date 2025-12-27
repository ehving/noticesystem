<script setup lang="ts">
defineProps<{
  title: string
  subTitle?: string
}>()
</script>

<template>
  <div class="page-header">
    <div class="header-left">
      <!-- 品牌色装饰竖条：量子能量柱 -->
      <div class="decoration-bar"></div>
      <div class="title-group">
        <div class="title">{{ title }}</div>
        <div v-if="subTitle" class="sub-title">{{ subTitle }}</div>
      </div>
    </div>
    <div class="actions">
      <slot />
    </div>
  </div>
</template>

<style scoped lang="scss">
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding-bottom: 16px;
  position: relative;

  /* 底部精细分割线 */
  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%;
    height: 1px;
    background: linear-gradient(90deg, rgba(6, 182, 212, 0.3) 0%, rgba(203, 213, 225, 0.3) 50%, transparent 100%);
  }
}

.header-left {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

/* 渐变装饰条 */
.decoration-bar {
  width: 4px;
  height: 24px;
  /* 调整为管理端的青蓝配色 */
  background: linear-gradient(180deg, #06b6d4 0%, #3b82f6 100%);
  border-radius: 2px;
  flex-shrink: 0;
  margin-top: 3px;
  /* 增加微弱光晕 */
  box-shadow: 0 0 8px rgba(6, 182, 212, 0.4);
}

.title-group {
  display: flex;
  flex-direction: column;
}

.title {
  font-weight: 700;
  font-size: 20px; /* 略微加大 */
  color: #1e293b; /* Slate 800 - 更深邃的灰 */
  line-height: 1.2;
  letter-spacing: 0.5px;
}

.sub-title {
  margin-top: 4px;
  color: #64748b; /* Slate 500 */
  font-size: 12px;
  line-height: 1.4;
  font-weight: 500;
}

.actions {
  display: flex;
  gap: 12px;
  align-items: center;
}
</style>
