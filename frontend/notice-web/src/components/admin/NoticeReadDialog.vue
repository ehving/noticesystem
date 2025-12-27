<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { getReadCount, pageNoticeReads } from '@/api/modules/admin/notice-reads'
import type { NoticeReadUserVo } from '@/types/models/notice-read'
import type { MpPage } from '@/types/page'
import { useAdminStore } from '@/stores/admin'
import { formatDateTime } from '@/utils/time'
import { useLoading } from '@/hooks/useLoading'
import {
  DataLine,
  Refresh,
  Close,
  User,
  OfficeBuilding,
  Monitor,
  Iphone,
  Timer,
  Coin,
  View
} from '@element-plus/icons-vue'

const props = defineProps<{
  modelValue: boolean
  noticeId?: string
  noticeTitle?: string
}>()
const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
}>()

const adminStore = useAdminStore()
const { loading, run } = useLoading()
const visible = ref(props.modelValue)

watch(
  () => props.modelValue,
  (v) => {
    visible.value = v
    if (v) {
      init()
    }
  }
)
watch(visible, (v) => emit('update:modelValue', v))

const readCount = ref<number | null>(null)
const tableData = ref<NoticeReadUserVo[]>([])
const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0,
})

const resetState = () => {
  readCount.value = null
  tableData.value = []
  pagination.pageNo = 1
  pagination.pageSize = 10
  pagination.total = 0
}

const fetchAll = async () => {
  if (!props.noticeId) return
  await run(async () => {
    // 并行请求数据
    const [count, res] = await Promise.all([
      getReadCount(props.noticeId as string),
      pageNoticeReads(props.noticeId as string, {
        pageNo: pagination.pageNo,
        pageSize: pagination.pageSize,
      })
    ])

    readCount.value = count
    tableData.value = res.records || []
    pagination.total = res.total || 0
    pagination.pageNo = res.current || pagination.pageNo
    pagination.pageSize = res.size || pagination.pageSize
  })
}

const init = () => {
  resetState()
  fetchAll()
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.pageNo = 1
  fetchAll()
}

const handleCurrentChange = (page: number) => {
  pagination.pageNo = page
  fetchAll()
}

const close = () => {
  visible.value = false
  resetState()
}

// 辅助：简单的设备图标判断
const getDeviceIcon = (device?: string) => {
  if (!device) return Monitor
  const d = device.toLowerCase()
  if (d.includes('mobile') || d.includes('phone') || d.includes('android') || d.includes('ios')) {
    return Iphone
  }
  return Monitor
}
</script>

<template>
  <el-dialog
    v-model="visible"
    width="780px"
    class="read-stats-dialog"
    destroy-on-close
    :show-close="false"
    @close="close"
  >
    <!-- 自定义 Header -->
    <template #header>
      <div class="dialog-header">
        <div class="header-left">
          <div class="icon-box">
            <el-icon><DataLine /></el-icon>
          </div>
          <div class="header-titles">
            <span class="main-title">阅读数据统计</span>
            <el-tooltip :content="noticeTitle" placement="bottom-start" :disabled="!noticeTitle">
              <span class="sub-title">{{ noticeTitle || `Notice ID: ${noticeId}` }}</span>
            </el-tooltip>
          </div>
        </div>
        <div class="header-right">
          <el-button circle :icon="Close" @click="close" class="close-btn" />
        </div>
      </div>
    </template>

    <div class="dialog-body">
      <!-- 顶部仪表盘区 -->
      <div class="dashboard-row">
        <!-- 核心指标卡片 -->
        <div class="stat-card">
          <div class="stat-icon-bg">
            <el-icon><View /></el-icon>
          </div>
          <div class="stat-content">
            <div class="label">累计已读人数</div>
            <div class="value-row">
              <span class="number">{{ readCount !== null ? readCount : '-' }}</span>
              <span class="unit">人</span>
            </div>
          </div>
        </div>

        <!-- 过滤器/操作区 -->
        <div class="actions-panel">
          <div class="info-item">
            <span class="label">数据来源</span>
            <el-tag type="info" effect="plain" round size="small">
              <el-icon style="margin-right: 4px"><Coin /></el-icon>
              {{ adminStore.activeDb }}
            </el-tag>
          </div>
          <div class="divider"></div>
          <el-button :icon="Refresh" circle @click="fetchAll" :loading="loading" title="刷新数据" />
        </div>
      </div>

      <!-- 数据表格 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        style="width: 100%"
        class="custom-table"
        :header-cell-style="{ background: '#f8f9fb', color: '#606266', fontWeight: 600 }"
      >
        <el-table-column label="用户" min-width="160">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="avatar-placeholder">
                <el-icon><User /></el-icon>
              </div>
              <div class="user-info">
                <div class="nickname">{{ row.nickname }}</div>
                <div class="username">@{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="部门" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="dept-cell">
              <el-icon class="dept-icon"><OfficeBuilding /></el-icon>
              <span>{{ row.deptName || '暂无部门' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="阅读设备" width="120" align="center">
          <template #default="{ row }">
            <div class="device-cell">
              <el-icon :class="['device-icon', getDeviceIcon(row.deviceType) === Iphone ? 'mobile' : 'pc']">
                <component :is="getDeviceIcon(row.deviceType)" />
              </el-icon>
              <span class="device-name">{{ row.deviceType || '未知' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="阅读时间" width="160">
          <template #default="{ row }">
            <div class="time-cell">
              <span class="main-time">{{ formatDateTime(row.readTime).split(' ')[1] }}</span>
              <span class="sub-time"><el-icon><Timer /></el-icon> {{ formatDateTime(row.readTime).split(' ')[0] }}</span>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty :image-size="80" description="暂无阅读记录" />
        </template>
      </el-table>

      <div class="pagination-footer">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="pagination.total"
          :page-size="pagination.pageSize"
          :current-page="pagination.pageNo"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
/* 覆盖 Element Dialog 样式 */
:deep(.read-stats-dialog) {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);

  .el-dialog__header {
    padding: 0;
    margin: 0;
  }
  .el-dialog__body {
    padding: 0;
  }
}

/* Header 样式 */
.dialog-header {
  background: linear-gradient(135deg, #fdfbfb 0%, #ebedee 100%);
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e4e7ed;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
    overflow: hidden; /* 防止长标题溢出 */

    .icon-box {
      width: 36px;
      height: 36px;
      background: #fff;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 2px 6px rgba(0,0,0,0.05);
      color: #409eff;
      font-size: 18px;
      flex-shrink: 0;
    }

    .header-titles {
      display: flex;
      flex-direction: column;
      overflow: hidden;

      .main-title {
        font-size: 16px;
        font-weight: 700;
        color: #303133;
        line-height: 1.2;
      }
      .sub-title {
        font-size: 12px;
        color: #909399;
        margin-top: 2px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        max-width: 400px;
      }
    }
  }

  .header-right {
    .close-btn {
      border: none;
      background: transparent;
      font-size: 18px;
      &:hover {
        background: rgba(0,0,0,0.05);
      }
    }
  }
}

.dialog-body {
  padding: 24px;
  background-color: #fff;
}

/* 仪表盘区域 */
.dashboard-row {
  display: flex;
  justify-content: space-between;
  align-items: stretch;
  margin-bottom: 20px;
  gap: 16px;

  /* 指标卡片 */
  .stat-card {
    flex: 1;
    background: linear-gradient(135deg, #ecf5ff 0%, #ffffff 100%);
    border: 1px solid #d9ecff;
    border-radius: 10px;
    padding: 16px 20px;
    display: flex;
    align-items: center;
    gap: 16px;

    .stat-icon-bg {
      width: 48px;
      height: 48px;
      background: #fff;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #409eff;
      font-size: 24px;
      box-shadow: 0 4px 10px rgba(64, 158, 255, 0.15);
    }

    .stat-content {
      .label {
        font-size: 13px;
        color: #606266;
        margin-bottom: 4px;
      }
      .value-row {
        display: flex;
        align-items: baseline;
        gap: 4px;

        .number {
          font-size: 24px;
          font-weight: 700;
          color: #303133;
          font-family: 'Segoe UI', sans-serif;
        }
        .unit {
          font-size: 12px;
          color: #909399;
        }
      }
    }
  }

  /* 操作面板 */
  .actions-panel {
    display: flex;
    align-items: center;
    background: #f8f9fb;
    border-radius: 10px;
    padding: 0 20px;
    gap: 16px;

    .info-item {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 4px;

      .label {
        font-size: 12px;
        color: #909399;
      }
    }

    .divider {
      width: 1px;
      height: 24px;
      background: #e4e7ed;
    }
  }
}

/* 表格样式优化 */
.custom-table {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 0 0 1px #ebeef5;

  .user-cell {
    display: flex;
    align-items: center;
    gap: 10px;

    .avatar-placeholder {
      width: 32px;
      height: 32px;
      background: #e5eaf3;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #909399;
    }

    .user-info {
      display: flex;
      flex-direction: column;
      line-height: 1.2;

      .nickname {
        font-size: 13px;
        color: #303133;
        font-weight: 500;
      }
      .username {
        font-size: 12px;
        color: #909399;
      }
    }
  }

  .dept-cell {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #606266;

    .dept-icon { color: #a8abb2; }
  }

  .device-cell {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;

    .device-icon {
      font-size: 16px;
      &.mobile { color: #e6a23c; }
      &.pc { color: #409eff; }
    }
    .device-name {
      font-size: 12px;
      color: #909399;
      transform: scale(0.9);
    }
  }

  .time-cell {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    line-height: 1.3;

    .main-time {
      font-size: 13px;
      color: #303133;
      font-weight: 500;
    }
    .sub-time {
      font-size: 12px;
      color: #909399;
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }
}

.pagination-footer {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
