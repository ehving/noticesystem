<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useAdminStore } from '@/stores/admin'
import {
  pageSyncLogs,
  getSyncLogDetail,
  retrySyncLog,
  cleanSyncLogs,
  listActions,
  listEntityTypes,
  listStatuses,
  dailyReport,
} from '@/api/modules/admin/sync-logs'
import type {
  SyncLog,
  SyncLogVo,
  SyncAction,
  SyncEntityType,
  SyncStatus,
  SyncLogDailyReportVo,
} from '@/types/models/sync-log'
import type { MpPage } from '@/types/page'
import type { DatabaseType } from '@/types/enums/db'
import { DB_OPTIONS } from '@/types/enums/db'
import { formatDateTime } from '@/utils/time'
import PageHeader from '@/components/common/PageHeader.vue'
import TableToolbar from '@/components/common/TableToolbar.vue'
import { useLoading } from '@/hooks/useLoading'

const adminStore = useAdminStore()
const { loading, run } = useLoading()

const activeTab = ref('list')

const statuses = ref<SyncStatus[]>([])
const actions = ref<SyncAction[]>([])
const entityTypes = ref<SyncEntityType[]>([])

const tableData = ref<SyncLog[]>([])
const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0,
})
const query = reactive<SyncLogVo>({
  pageNo: 1,
  pageSize: 10,
  entityType: undefined,
  entityId: '',
  action: undefined,
  sourceDb: undefined,
  targetDb: undefined,
  status: undefined,
  beginTime: '',
  endTime: '',
})

const detailVisible = ref(false)
const detail = ref<SyncLog | null>(null)
const repairDialogVisible = ref(false)
const repairDb = ref<string>('')
const currentRow = ref<SyncLog | null>(null)

const cleanParams = reactive({
  db: 'ALL' as DatabaseType | 'ALL',
  retainDays: undefined as number | undefined,
  maxCount: undefined as number | undefined,
})

const reportLoading = ref(false)
const reportData = ref<SyncLogDailyReportVo[]>([])

const formatTime = (val?: string) => formatDateTime(val)

const fetchList = () =>
  run(async () => {
    const res: MpPage<SyncLog> = await pageSyncLogs({
      ...query,
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
      entityId: query.entityId || undefined,
      beginTime: query.beginTime || undefined,
      endTime: query.endTime || undefined,
    })
    tableData.value = res.records || []
    pagination.total = res.total || 0
    pagination.pageNo = res.current || pagination.pageNo
    pagination.pageSize = res.size || pagination.pageSize
  })

const onSearch = () => {
  pagination.pageNo = 1
  fetchList()
}

const resetQuery = () => {
  query.entityType = undefined
  query.entityId = ''
  query.action = undefined
  query.sourceDb = undefined
  query.targetDb = undefined
  query.status = undefined
  query.beginTime = ''
  query.endTime = ''
  pagination.pageNo = 1
  fetchList()
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.pageNo = 1
  fetchList()
}

const handleCurrentChange = (page: number) => {
  pagination.pageNo = page
  fetchList()
}

const openDetail = async (row: SyncLog) => {
  detail.value = await getSyncLogDetail(row.id)
  detailVisible.value = true
}

const openRepair = (row: SyncLog) => {
  currentRow.value = row
  repairDb.value = row.sourceDb || adminStore.activeDb
  repairDialogVisible.value = true
}

const onRetry = async () => {
  if (!currentRow.value) return
  const ok = await ElMessageBox.confirm(
    `将以【${repairDb.value}】作为来源库进行修复（实际调用重试接口）。是否继续？`,
    '提示',
    { type: 'warning' }
  ).then(
    () => true,
    () => false
  )
  if (!ok) return
  await retrySyncLog(currentRow.value.id)
  repairDialogVisible.value = false
  fetchList()
}

const onClean = async () => {
  await cleanSyncLogs(cleanParams.db, cleanParams.retainDays, cleanParams.maxCount)
  fetchList()
}

const fetchEnums = async () => {
  statuses.value = await listStatuses()
  actions.value = await listActions()
  entityTypes.value = await listEntityTypes()
}

const fetchReport = async () => {
  reportLoading.value = true
  try {
    const res = await dailyReport({
      ...query,
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
      entityId: query.entityId || undefined,
      beginTime: query.beginTime || undefined,
      endTime: query.endTime || undefined,
    })
    reportData.value = res
  } finally {
    reportLoading.value = false
  }
}

onMounted(async () => {
  await fetchEnums()
  fetchList()
})

watch(
  () => adminStore.changeTick,
  () => {
    if (activeTab.value === 'list') fetchList()
    else fetchReport()
  }
)
</script>

<template>
  <div class="page">
    <el-card>
      <PageHeader title="同步日志" :sub-title="`当前库：${adminStore.activeDb}`" />
      <el-tabs v-model="activeTab">
        <el-tab-pane label="日志列表" name="list">
          <TableToolbar>
            <template #filters>
              <el-select v-model="query.entityType" clearable placeholder="实体类型" style="width: 150px" @change="onSearch">
                <el-option v-for="t in entityTypes" :key="t" :label="t" :value="t" />
              </el-select>
              <el-input v-model="query.entityId" placeholder="实体ID" clearable style="width: 180px" @keyup.enter.native="onSearch" />
              <el-select v-model="query.action" clearable placeholder="操作" style="width: 140px" @change="onSearch">
                <el-option v-for="a in actions" :key="a" :label="a" :value="a" />
              </el-select>
              <el-select v-model="query.status" clearable placeholder="状态" style="width: 140px" @change="onSearch">
                <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
              </el-select>
              <el-select v-model="query.sourceDb" clearable placeholder="源库" style="width: 140px" @change="onSearch">
                <el-option label="ALL" value="ALL" />
                <el-option v-for="db in DB_OPTIONS" :key="db.value" :label="db.label" :value="db.value" />
              </el-select>
              <el-select v-model="query.targetDb" clearable placeholder="目标库" style="width: 140px" @change="onSearch">
                <el-option label="ALL" value="ALL" />
                <el-option v-for="db in DB_OPTIONS" :key="db.value" :label="db.label" :value="db.value" />
              </el-select>
            <el-date-picker
              v-model="query.beginTime"
              type="datetime"
              placeholder="开始时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm"
            />
            <el-date-picker
              v-model="query.endTime"
              type="datetime"
              placeholder="结束时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm"
            />
            </template>
            <template #actions>
              <el-button type="primary" @click="onSearch">查询</el-button>
              <el-button @click="resetQuery">重置</el-button>
            </template>
          </TableToolbar>

          <div class="clean">
            <div class="label">清理日志：</div>
            <el-select v-model="cleanParams.db" placeholder="选择库" style="width: 140px">
              <el-option label="全部" value="ALL" />
              <el-option v-for="db in DB_OPTIONS" :key="db.value" :label="db.label" :value="db.value" />
            </el-select>
            <el-input-number v-model="cleanParams.retainDays" :min="0" placeholder="保留天数" />
            <el-input-number v-model="cleanParams.maxCount" :min="0" placeholder="最大条数" />
            <el-button type="warning" @click="onClean">执行清理</el-button>
          </div>

          <el-table :data="tableData" v-loading="loading" style="width: 100%">
            <el-table-column prop="createTime" label="时间" width="180">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column prop="entityType" label="实体类型" width="140" />
            <el-table-column prop="entityId" label="实体ID" min-width="160" />
            <el-table-column prop="action" label="操作" width="120" />
            <el-table-column prop="sourceDb" label="源库" width="120" />
            <el-table-column prop="targetDb" label="目标库" width="120" />
            <el-table-column label="状态" width="140">
              <template #default="{ row }">
                <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
                  {{ row.status }}
                </el-tag>
                <span v-if="row.retryCount !== undefined" class="retry">({{ row.retryCount }}次重试)</span>
              </template>
            </el-table-column>
            <el-table-column prop="errorMsg" label="错误" min-width="200" show-overflow-tooltip />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openDetail(row)">详情</el-button>
                <el-button v-if="row.status === 'FAILED'" type="warning" link size="small" @click="openRepair(row)">
                  选择来源库修复
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && tableData.length === 0" description="暂无数据" />

          <div class="pagination">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next, jumper"
              :total="pagination.total"
              :page-size="pagination.pageSize"
              :current-page="pagination.pageNo"
              :page-sizes="[10, 20, 50]"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="日报统计" name="report">
          <TableToolbar>
            <template #filters>
              <el-select v-model="query.entityType" clearable placeholder="实体类型" style="width: 150px">
                <el-option v-for="t in entityTypes" :key="t" :label="t" :value="t" />
              </el-select>
              <el-select v-model="query.action" clearable placeholder="操作" style="width: 140px">
                <el-option v-for="a in actions" :key="a" :label="a" :value="a" />
              </el-select>
              <el-select v-model="query.status" clearable placeholder="状态" style="width: 140px">
                <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
              </el-select>
              <el-select v-model="query.sourceDb" clearable placeholder="源库" style="width: 140px">
                <el-option label="ALL" value="ALL" />
                <el-option v-for="db in DB_OPTIONS" :key="db.value" :label="db.label" :value="db.value" />
              </el-select>
              <el-select v-model="query.targetDb" clearable placeholder="目标库" style="width: 140px">
                <el-option label="ALL" value="ALL" />
                <el-option v-for="db in DB_OPTIONS" :key="db.value" :label="db.label" :value="db.value" />
              </el-select>
            <el-date-picker
              v-model="query.beginTime"
              type="date"
              placeholder="开始日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
            <el-date-picker
              v-model="query.endTime"
              type="date"
              placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
            </template>
            <template #actions>
              <el-button type="primary" @click="fetchReport">查询</el-button>
            </template>
          </TableToolbar>

          <el-table :data="reportData" v-loading="reportLoading" style="width: 100%">
            <el-table-column prop="statDate" label="日期" width="140" />
            <el-table-column prop="sourceDb" label="源库" width="120" />
            <el-table-column prop="targetDb" label="目标库" width="120" />
            <el-table-column prop="totalCount" label="总数" width="100" />
            <el-table-column prop="successCount" label="成功" width="100" />
            <el-table-column prop="failedCount" label="失败" width="100" />
            <el-table-column label="失败率" width="120">
              <template #default="{ row }">
                {{ row.failedRate != null ? (row.failedRate * 100).toFixed(2) + '%' : '-' }}
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!reportLoading && reportData.length === 0" description="暂无数据" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-drawer v-model="detailVisible" title="日志详情" size="40%">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="实体类型">{{ detail.entityType }}</el-descriptions-item>
        <el-descriptions-item label="实体ID">{{ detail.entityId }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ detail.action }}</el-descriptions-item>
        <el-descriptions-item label="源库">{{ detail.sourceDb }}</el-descriptions-item>
        <el-descriptions-item label="目标库">{{ detail.targetDb }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
        <el-descriptions-item label="重试次数">{{ detail.retryCount }}</el-descriptions-item>
        <el-descriptions-item label="错误信息">{{ detail.errorMsg }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatTime(detail.updateTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog v-model="repairDialogVisible" title="选择来源库修复" width="420px" destroy-on-close>
      <div v-if="currentRow">
        <p>实体：{{ currentRow.entityType }} #{{ currentRow.entityId }}</p>
        <p>操作：{{ currentRow.action }}</p>
        <p>源->目标：{{ currentRow.sourceDb }} -> {{ currentRow.targetDb }}</p>
        <p>错误：{{ currentRow.errorMsg || '-' }}</p>
        <p class="tip">提示：前端选择来源库用于演示，后端实际以日志/接口策略为准。</p>
      </div>
      <el-form label-width="120px">
        <el-form-item label="修复来源库">
          <el-select v-model="repairDb" placeholder="请选择">
            <el-option v-for="db in DB_OPTIONS" :key="db.value" :label="db.label" :value="db.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="repairDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onRetry">确认修复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  font-weight: 600;
}

.sub {
  color: #909399;
  font-size: 12px;
}

.filters {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
  margin: 12px 0;
}

.clean {
  display: flex;
  gap: 8px;
  align-items: center;
  margin: 8px 0;
}

.label {
  color: #606266;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.retry {
  margin-left: 4px;
  color: #909399;
}
</style>
