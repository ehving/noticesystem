<script setup lang="ts">
import { computed } from 'vue'
import { useAdminStore } from '@/stores/admin'
// 关键修复：去掉 type 关键字，导入 DatabaseType 实体以便在下方作为对象 Key 使用
import { DB_OPTIONS, DatabaseType } from '@/types/enums/db'
import { Coin } from '@element-plus/icons-vue'

const adminStore = useAdminStore()

// 数据库品牌色配置
// 使用 [DatabaseType.MYSQL] 动态 Key，确保与 activeDb 的真实值完全一致
const dbConfig = {
  [DatabaseType.MYSQL]: { color: '#16a085', bg: '#e8f8f5', label: 'MySQL' },
  [DatabaseType.PG]: { color: '#2980b9', bg: '#eaf2f8', label: 'PostgreSQL' },
  [DatabaseType.SQLSERVER]: { color: '#c0392b', bg: '#f9ebea', label: 'SQL Server' },
}

const defaultStyle = { color: '#909399', bg: '#f4f6f7', label: 'Unknown' }

// 获取当前选中数据库的样式配置
const activeStyle = computed(() => {
  const type = adminStore.activeDb
  // @ts-ignore: 忽略枚举索引检查，确保类型安全
  return dbConfig[type] || defaultStyle
})

// 获取指定选项的样式配置
const getOptionStyle = (val: DatabaseType) => {
  // @ts-ignore: 忽略枚举索引检查
  return dbConfig[val] || defaultStyle
}

const handleChange = (val: DatabaseType) => {
  adminStore.setDb(val)
}
</script>

<template>
  <div class="db-select-wrapper" :style="{ '--active-color': activeStyle.color, '--active-bg': activeStyle.bg }">
    <el-select
      v-model="adminStore.activeDb"
      size="default"
      class="custom-db-select"
      popper-class="custom-db-dropdown"
      @change="handleChange"
      :teleported="false"
      placeholder="Select DB"
    >
      <!-- 自定义 Prefix 图标：显示当前数据库品牌色 -->
      <template #prefix>
        <div class="db-icon-box">
          <el-icon :size="14" :color="activeStyle.color"><Coin /></el-icon>
        </div>
      </template>

      <!-- 自定义 Option 选项 -->
      <el-option
        v-for="opt in DB_OPTIONS"
        :key="opt.value"
        :label="opt.label"
        :value="opt.value"
        class="custom-option"
      >
        <div class="option-content">
          <!-- 左侧色条指示器 -->
          <span class="color-indicator" :style="{ background: getOptionStyle(opt.value).color }"></span>
          <span class="option-label" :style="{ color: adminStore.activeDb === opt.value ? getOptionStyle(opt.value).color : '' }">
            {{ opt.label }}
          </span>
          <!-- 选中状态下的微标 -->
          <span v-if="adminStore.activeDb === opt.value" class="active-badge" :style="{ background: getOptionStyle(opt.value).color }">
            Current
          </span>
        </div>
      </el-option>
    </el-select>
  </div>
</template>

<style scoped lang="scss">
.db-select-wrapper {
  display: inline-block;

  /* 动态 CSS 变量，用于控制 hover 和 focus 时的颜色 */
  --el-select-input-focus-border-color: var(--active-color);
  --el-color-primary: var(--active-color);
}

:deep(.custom-db-select) {
  width: 160px;

  /* 输入框样式重写 */
  .el-input__wrapper {
    background-color: #fff;
    border-radius: 8px;
    box-shadow: 0 0 0 1px #dcdfe6 inset;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    padding-left: 8px;

    /* 选中后的背景色变为淡淡的品牌色 */
    &.is-focus, &:hover {
      box-shadow: 0 0 0 1px var(--active-color) inset !important;
      background-color: var(--active-bg);
    }
  }

  .el-input__inner {
    font-weight: 600;
    color: #303133;
    font-size: 13px;
    height: 32px;
  }

  /* 下拉箭头颜色 */
  .el-select__caret {
    color: var(--active-color);
    font-weight: bold;
  }
}

.db-icon-box {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 4px;
  transition: transform 0.3s;
}

/* 下拉菜单样式 */
:deep(.custom-db-dropdown) {
  border: none !important;
  border-radius: 12px !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12) !important;
  overflow: hidden;
  padding: 6px !important;

  .el-select-dropdown__item {
    border-radius: 6px;
    margin-bottom: 2px;
    padding: 0 8px 0 0 !important;
    height: 36px;
    line-height: 36px;

    &.hover, &:hover {
      background-color: #f5f7fa;
    }

    &.selected {
      background-color: #fff;
      font-weight: 600;
    }
  }
}

.custom-option {
  .option-content {
    display: flex;
    align-items: center;
    width: 100%;
    position: relative;

    .color-indicator {
      width: 4px;
      height: 16px;
      border-radius: 0 4px 4px 0;
      margin-right: 12px;
      opacity: 0.6;
      transition: opacity 0.2s;
    }

    .option-label {
      font-size: 13px;
      color: #606266;
      transition: color 0.2s;
    }

    .active-badge {
      margin-left: auto;
      font-size: 10px;
      color: #fff;
      padding: 0 6px;
      height: 18px;
      line-height: 18px;
      border-radius: 4px;
      transform: scale(0.9);
    }
  }

  /* Hover 时加深色条颜色 */
  &:hover .color-indicator {
    opacity: 1;
  }
}
</style>
