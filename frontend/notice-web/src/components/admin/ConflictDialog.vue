<script setup lang="ts">
import { computed } from 'vue'
import type { PropType } from 'vue'
import { ElMessage } from 'element-plus'
import type { SyncConflictDetailVo } from '@/types/models/sync-conflict'
import { DatabaseType, DatabaseTypeMap } from '@/types/enums/db'
import { formatDateTime } from '@/utils/time'
import {
  Warning,
  Check,
  Close,
  CopyDocument,
  Platform,
  Timer,
  Connection,
  Refresh,
  Coin,
  View,
  Document
} from '@element-plus/icons-vue'

const props = defineProps({
  visible: {
    type: Boolean,
    required: true,
  },
  conflict: {
    type: Object as PropType<SyncConflictDetailVo | null>,
    required: true,
  },
})

const emit = defineEmits(['close'])

// 数据库品牌色配置
const dbConfig = {
  [DatabaseType.MYSQL]: { color: '#16a085', bg: '#e8f8f5', label: 'MySQL' },
  [DatabaseType.PG]: { color: '#2980b9', bg: '#eaf2f8', label: 'PostgreSQL' },
  [DatabaseType.SQLSERVER]: { color: '#c0392b', bg: '#f9ebea', label: 'SQL Server' },
  DEFAULT: { color: '#7f8c8d', bg: '#f4f6f7', label: 'Unknown' }
}

const getDbStyle = (dbType: DatabaseType) => {
  return dbConfig[dbType] || dbConfig.DEFAULT
}

const copyToClipboard = (text: string) => {
  if (!text) return
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制')
  })
}

const statusTagType = computed(() => {
  if (!props.conflict) return 'info'
  switch (props.conflict.status) {
    case 'RESOLVED': return 'success'
    case 'IGNORED': return 'info'
    default: return 'danger'
  }
})

// --- JSON 对比核心逻辑 ---

// 1. 获取所有存在的数据库项
const activeItems = computed(() => {
  return props.conflict?.items?.filter(item => item.existsFlag === 1) || []
})

// 2. 解析 JSON 并提取所有唯一的 Keys
const diffData = computed(() => {
  const items = activeItems.value
  if (items.length === 0) return { keys: [], rows: {} }

  const parsedMaps: Record<string, any>[] = items.map(item => {
    try {
      return item.rowJson ? JSON.parse(item.rowJson) : {}
    } catch {
      return {}
    }
  })

  // 收集所有出现的字段名 (Key)
  const allKeys = new Set<string>()
  parsedMaps.forEach(obj => Object.keys(obj).forEach(k => allKeys.add(k)))
  const sortedKeys = Array.from(allKeys).sort()

  return {
    keys: sortedKeys,
    parsedMaps
  }
})

// 3. 判断某个字段在不同库之间是否有差异
const isFieldDiff = (key: string, parsedMaps: any[]) => {
  if (parsedMaps.length < 2) return false // 只有一个库有数据，没法比对差异
  const firstVal = JSON.stringify(parsedMaps[0][key])
  for (let i = 1; i < parsedMaps.length; i++) {
    if (JSON.stringify(parsedMaps[i][key]) !== firstVal) {
      return true
    }
  }
  return false
}

// 4. 格式化显示值 (处理对象或null)
const formatValue = (val: any) => {
  if (val === null || val === undefined) return '<span class="text-gray-300">NULL</span>'
  if (typeof val === 'object') return JSON.stringify(val)
  return String(val)
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    width="900px"
    class="conflict-dialog"
    destroy-on-close
    :show-close="false"
    @update:model-value="(val) => !val && emit('close')"
  >
    <!-- Header -->
    <template #header>
      <div class="dialog-header">
        <div class="header-left">
          <div class="icon-box">
            <el-icon><Connection /></el-icon>
          </div>
          <div class="header-titles">
            <span class="main-title">数据冲突详情</span>
            <span class="sub-title">ID: {{ conflict?.id }}</span>
          </div>
        </div>
        <div class="header-right">
          <el-tag :type="statusTagType" effect="dark" round size="large" class="status-badge">
            {{ conflict?.status }}
          </el-tag>
          <el-button circle :icon="Close" @click="emit('close')" class="close-btn" />
        </div>
      </div>
    </template>

    <div v-if="conflict" class="dialog-body">
      <!-- 顶部关键指标卡片 -->
      <div class="metrics-grid">
        <div class="metric-card">
          <div class="label"><el-icon><Platform /></el-icon> 实体信息</div>
          <div class="value highlight">{{ conflict.entityType }}</div>
          <div class="sub-value">ID: {{ conflict.entityId }}</div>
        </div>
        <div class="metric-card">
          <div class="label"><el-icon><Warning /></el-icon> 冲突类型</div>
          <div class="value">{{ conflict.conflictType }}</div>
        </div>
        <div class="metric-card">
          <div class="label"><el-icon><View /></el-icon> 监控数据</div>
          <div class="value">{{ conflict.notifyCount || 0 }} <span class="unit">次通知</span></div>
        </div>
        <div class="metric-card">
          <div class="label"><el-icon><Timer /></el-icon> 持续时间</div>
          <div class="value time-text">{{ formatDateTime(conflict.firstSeenAt).split(' ')[0] }}</div>
          <div class="sub-value">至 {{ formatDateTime(conflict.lastSeenAt).split(' ')[0] }}</div>
        </div>
      </div>

      <!-- 数据一致性对比 (Metadata) -->
      <div class="comparison-section">
        <div class="section-title">
          <el-icon><Coin /></el-icon> 数据库状态概览
        </div>

        <el-table
          :data="conflict.items"
          border
          class="custom-table"
          :header-cell-style="{ background: '#f8f9fb', color: '#606266' }"
        >
          <el-table-column label="数据库" width="140">
            <template #default="{ row }">
              <div class="db-cell" :style="{ borderLeftColor: getDbStyle(row.dbType).color }">
                <span class="db-name" :style="{ color: getDbStyle(row.dbType).color }">
                  {{ DatabaseTypeMap[row.dbType] }}
                </span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <div v-if="row.existsFlag" class="status-icon success">
                <el-icon><Check /></el-icon> 存在
              </div>
              <div v-else class="status-icon error">
                <el-icon><Close /></el-icon> 缺失
              </div>
            </template>
          </el-table-column>

          <el-table-column label="Row Hash" min-width="200">
            <template #default="{ row }">
              <div class="hash-wrapper" @click="copyToClipboard(row.rowHash)">
                <span class="hash-text">{{ row.rowHash || '-' }}</span>
                <el-icon class="copy-icon" v-if="row.rowHash"><CopyDocument /></el-icon>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="更新时间" width="170">
            <template #default="{ row }">
              <div class="time-cell">
                <span class="main-time">{{ formatDateTime(row.rowUpdateTime).split(' ')[1] }}</span>
                <span class="sub-time">{{ formatDateTime(row.rowUpdateTime).split(' ')[0] }}</span>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- JSON 字段深度对比 (新增模块) -->
      <div class="diff-section" v-if="activeItems.length > 0">
        <div class="section-title diff-title">
          <el-icon><Document /></el-icon> 字段内容深度比对
          <span class="tip-badge">自动标红差异项</span>
        </div>

        <div class="diff-table-wrapper">
          <table class="diff-table">
            <thead>
            <tr>
              <th class="field-col">Field Name</th>
              <th v-for="item in activeItems" :key="item.id" :style="{ color: getDbStyle(item.dbType).color }">
                {{ DatabaseTypeMap[item.dbType] }}
              </th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="key in diffData.keys" :key="key">
              <td class="field-name">{{ key }}</td>
              <td
                v-for="(item, index) in activeItems"
                :key="item.id"
                :class="{ 'diff-cell': isFieldDiff(key, diffData.parsedMaps) }"
              >
                <div class="cell-content" v-html="formatValue(diffData.parsedMaps[index][key])"></div>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>
      <div v-else-if="conflict.items?.some(i => !i.existsFlag)" class="empty-diff">
        <el-empty description="部分数据缺失，无法进行字段级比对" :image-size="80" />
      </div>

    </div>

    <div v-else class="loading-state">
      <el-icon class="is-loading"><Refresh /></el-icon> 数据加载中...
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
/* Dialog Base */
:deep(.conflict-dialog) {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);

  .el-dialog__header { padding: 0; margin: 0; }
  .el-dialog__body { padding: 0; }
}

/* Header */
.dialog-header {
  background: linear-gradient(135deg, #fdfbfb 0%, #ebedee 100%);
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e4e7ed;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .icon-box {
      width: 40px;
      height: 40px;
      background: #fff;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 2px 8px rgba(0,0,0,0.05);
      color: #409eff;
      font-size: 20px;
    }

    .header-titles {
      display: flex;
      flex-direction: column;
      .main-title { font-size: 18px; font-weight: 700; color: #303133; }
      .sub-title { font-size: 12px; color: #909399; font-family: monospace; }
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;
    .close-btn { border: none; background: transparent; font-size: 18px; &:hover { background: rgba(0,0,0,0.05); } }
  }
}

.dialog-body {
  padding: 24px;
  background-color: #fff;
  max-height: 80vh;
  overflow-y: auto;
}

/* Metrics Grid */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;

  .metric-card {
    background: #f8f9fb;
    border-radius: 8px;
    padding: 16px;
    border: 1px solid #ebeef5;

    .label { font-size: 12px; color: #909399; display: flex; align-items: center; gap: 6px; margin-bottom: 8px; }
    .value { font-size: 16px; font-weight: 700; color: #303133; &.highlight { color: #409eff; } .unit { font-size: 12px; font-weight: 400; color: #909399; } }
    .sub-value { font-size: 12px; color: #c0c4cc; margin-top: 4px; }
  }
}

/* Comparison Section */
.comparison-section, .diff-section {
  margin-bottom: 24px;

  .section-title {
    font-size: 15px;
    font-weight: 700;
    color: #303133;
    margin-bottom: 12px;
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

/* Table Styles from previous version */
.custom-table {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 0 0 1px #ebeef5;

  .db-cell { padding-left: 10px; border-left: 3px solid transparent; font-weight: 600; }
  .status-icon { display: flex; align-items: center; justify-content: center; gap: 4px; font-size: 12px; &.success { color: #67c23a; } &.error { color: #f56c6c; } }
  .hash-wrapper { background: #f4f4f5; padding: 4px 8px; border-radius: 4px; font-family: 'Consolas', monospace; font-size: 12px; color: #606266; cursor: pointer; display: flex; justify-content: space-between; align-items: center; &:hover { background: #e9e9eb; .copy-icon { opacity: 1; } } .hash-text { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; } .copy-icon { opacity: 0; transition: opacity 0.2s; color: #409eff; } }
  .time-cell { display: flex; flex-direction: column; line-height: 1.2; .main-time { font-size: 13px; color: #303133; } .sub-time { font-size: 12px; color: #909399; } }
}

/* --- JSON Diff Table Styles --- */
.diff-title .tip-badge {
  font-size: 10px;
  background: #fef0f0;
  color: #f56c6c;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: normal;
}

.diff-table-wrapper {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  /* 横向滚动支持 */
  overflow-x: auto;
}

.diff-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  table-layout: fixed; /* 保持列宽稳定 */

  th, td {
    padding: 10px 12px;
    border-bottom: 1px solid #ebeef5;
    border-right: 1px solid #ebeef5;
    text-align: left;
    word-break: break-all; /* 防止长字符串撑破 */

    &:last-child { border-right: none; }
  }

  th {
    background: #f8f9fb;
    color: #606266;
    font-weight: 600;
    position: sticky;
    top: 0;
  }

  .field-col {
    width: 120px;
    background: #fafafa;
    color: #909399;
  }

  .field-name {
    font-family: 'Menlo', monospace;
    font-weight: 600;
    color: #606266;
    background: #fafafa;
  }

  .cell-content {
    max-height: 100px;
    overflow-y: auto;
    white-space: pre-wrap; /* 保持 JSON 格式 */
  }

  /* 差异高亮核心样式 */
  .diff-cell {
    background-color: #fef0f0; /* 浅红背景 */
    color: #f56c6c; /* 红色字体 */
    font-weight: 500;
    position: relative;

    &::after {
      content: '';
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 3px;
      background-color: #f56c6c;
    }
  }
}

.loading-state {
  padding: 40px;
  text-align: center;
  color: #909399;
}

.empty-diff {
  padding: 20px;
  text-align: center;
}
</style>
