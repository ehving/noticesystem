<template>
  <div class="toolbar-container">
    <!-- 左侧筛选区：自动处理间距和换行 -->
    <div class="filters-section">
      <slot name="filters" />
    </div>

    <!-- 右侧操作区：靠右对齐 -->
    <div class="actions-section">
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.toolbar-container {
  /* 容器样式：模拟精密仪器面板 */
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #e2e8f0; /* Slate 200 */
  border-radius: 12px; /* 更圆润的角 */
  padding: 16px 20px;
  margin-bottom: 24px;

  /* 布局：两端对齐，垂直居中 */
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;

  /* 阴影：细腻的悬浮感 */
  box-shadow:
    0 1px 3px 0 rgba(0, 0, 0, 0.05),
    0 1px 2px -1px rgba(0, 0, 0, 0.03);

  /* 微交互：悬停时边框呈现微弱的青色光晕 */
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    border-color: #cbd5e1;
    box-shadow:
      0 4px 6px -1px rgba(0, 0, 0, 0.05),
      0 2px 4px -1px rgba(0, 0, 0, 0.03),
      0 0 0 1px rgba(6, 182, 212, 0.1); /* Cyan glow hint */
  }
}

.filters-section {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px; /* 统一控件间距 */
  flex: 1; /* 占据剩余空间 */

  /* * 深度定制 Element Plus 输入框
   * 目标：让输入框像嵌入面板的“凹槽”，增加对比度
   */
  :deep(.el-input),
  :deep(.el-select) {
    --el-input-text-color: #334155; /* Slate 700 */
    --el-input-placeholder-color: #94a3b8;
    --el-input-border-color: transparent; /* 默认无边框，靠背景区分 */
    --el-input-hover-border-color: #cbd5e1;
    --el-input-focus-border-color: #06b6d4; /* Cyan focus */

    .el-input__wrapper {
      background-color: #f1f5f9; /* Slate 100 - 浅灰填充，形成对比 */
      box-shadow: none !important; /* 移除默认阴影 */
      border: 1px solid transparent;
      border-radius: 8px;
      padding: 4px 11px;
      transition: all 0.2s ease;

      &:hover {
        background-color: #e2e8f0; /* Slate 200 */
      }

      &.is-focus {
        background-color: #ffffff; /* 聚焦时变白，凸显内容 */
        border-color: #06b6d4;
        box-shadow: 0 0 0 3px rgba(6, 182, 212, 0.1) !important; /* Cyan Glow */
      }
    }

    .el-input__inner {
      font-weight: 500;
    }
  }

  /* 定制按钮样式 (如果插槽里有按钮) */
  :deep(.el-button) {
    border-radius: 8px;
    font-weight: 600;
  }
}

.actions-section {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  flex-shrink: 0; /* 防止按钮被压缩 */
}

/* 移动端适配优化 */
@media (max-width: 768px) {
  .toolbar-container {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
    padding: 16px;
  }

  .actions-section {
    justify-content: flex-end;
    border-top: 1px dashed #e2e8f0;
    padding-top: 12px;
    width: 100%;
  }
}
</style>
