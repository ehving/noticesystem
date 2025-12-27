<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminStore } from '@/stores/admin'
import {
  pageSyncLogs,
  getSyncLogDetail,
  cleanSyncLogs,
  listActions,
  listEntityTypes,
  listStatuses,
} from '@/api/modules/admin/sync-logs'
import type { SyncLog, SyncLogVo, SyncAction } from '@/types/models/sync-log'
import { SyncLogStatus, SyncEntityType } from '@/types/enums/sync'
import type { MpPage } from '@/types/page'
import type { DatabaseType } from '@/types/enums/db'
import { DB_OPTIONS } from '@/types/enums/db'
import { formatDateTime } from '@/utils/time'
import PageHeader from '@/components/common/PageHeader.vue'
import TableToolbar from '@/components/common/TableToolbar.vue'
import { useLoading } from '@/hooks/useLoading'
import { useConfirm } from '@/hooks/useConfirm'
import { success } from '@/utils/message'
import { Search, RefreshLeft, Delete, View, Warning, Filter, DataLine, Calendar } from '@element-plus/icons-vue'

const adminStore = useAdminStore()
const router = useRouter()
const { loading, run } = useLoading()
const { confirm } = useConfirm()

const statuses = ref<SyncLogStatus[]>([])
const actions = ref<SyncAction[]>([])
const entityTypes = ref<SyncEntityType[]>([])

const tableData = ref<SyncLog[]>([])
const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0,
})

/**
 * 关键：query 里 beginTime/endTime 在你的 VO 是 LocalDateTime
 * 前端用 string 承载（按你接口约定传 YYYY-MM-DD HH:mm 或 YYYY-MM-DD）。
 * 这里保持 string，但传参时统一转 undefined。
 */
const query = reactive<SyncLogVo>({
  pageNo: 1,
  pageSize: 10,
  entityType: undefined,
  entityId: '',
  action: undefined,
  sourceDb: undefined,
  targetDb: undefined,
  status: undefined,
  beginTime: '' as any,
  endTime: '' as any,
})

const detailVisible = ref(false)
const detail = ref<SyncLog | null>(null)

const cleanDialogVisible = ref(false)
const cleanParams = reactive({
  db: 'ALL' as DatabaseType | 'ALL',
  retainDays: undefined as number | undefined,
  maxCount: undefined as number | undefined,
})

const normalizeDb = (db?: any) => {
  if (!db || db === 'ALL') return undefined
  return db
}

const fetchList = () =>
  run(async () => {
    const res: MpPage<SyncLog> = await pageSyncLogs({
      ...query,
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
      entityId: query.entityId || undefined,
      beginTime: (query as any).beginTime || undefined,
      endTime: (query as any).endTime || undefined,
      sourceDb: normalizeDb(query.sourceDb),
      targetDb: normalizeDb(query.targetDb),
    })
    tableData.value = res.records || []
    pagination.total = res.total || 0
    pagination.pageNo = (res.current as any) || pagination.pageNo
    pagination.pageSize = (res.size as any) || pagination.pageSize
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
  ;(query as any).beginTime = ''
  ;(query as any).endTime = ''
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

const goConflicts = () => {
  router.push('/admin/conflicts')
}

const openCleanDialog = () => {
  cleanDialogVisible.value = true
}

const confirmClean = async () => {
  await run(async () => {
    const msg = await cleanSyncLogs(cleanParams.db, cleanParams.retainDays, cleanParams.maxCount)
    success(msg || '清理完成')
    cleanDialogVisible.value = false
    fetchList()
  })
}

const fetchEnums = async () => {
  statuses.value = (await listStatuses()) || []
  actions.value = (await listActions()) || []
  entityTypes.value = (await listEntityTypes()) || []
}

onMounted(async () => {
  await fetchEnums()
  fetchList()
})

watch(() => adminStore.changeTick, () => {
  fetchList()
})

const getStatusTagType = (status: SyncLogStatus) => {
  switch (status) {
    case SyncLogStatus.SUCCESS:
      return 'success'
    case SyncLogStatus.FAILED:
      return 'danger'
    case SyncLogStatus.CONFLICT:
      return 'warning'
    case SyncLogStatus.ERROR:
      return 'danger'
    default:
      return 'info'
  }
}

const formatJson = (str?: string) => {
  if (!str) return '-'
  try {
    const obj = JSON.parse(str)
    return JSON.stringify(obj, null, 2)
  } catch (e) {
    return str
  }
}
</script>

<template>
  <div class="page">
    <el-card>
      <PageHeader title="同步日志" :sub-title="`当前库：${adminStore.activeDb}`">
        <el-button type="danger" plain :icon="Delete" @click="openCleanDialog">清理日志</el-button>
        <el-button :icon="RefreshLeft" @click="fetchList" :loading="loading">刷新</el-button>
      </PageHeader>

      <TableToolbar>
        <template #filters>
          <el-select v-model="query.entityType" clearable placeholder="实体类型" style="width: 140px" @change="onSearch">
            <template #prefix><el-icon><DataLine /></el-icon></template>
            <el-option v-for="t in entityTypes" :key="t" :label="t" :value="t" />
          </el-select>

          <el-select v-model="query.action" clearable placeholder="操作类型" style="width: 120px" @change="onSearch">
            <template #prefix><el-icon><Filter /></el-icon></template>
            <el-option v-for="a in actions" :key="a" :label="a" :value="a" />
          </el-select>

          <el-select v-model="query.status" clearable placeholder="执行状态" style="width: 120px" @change="onSearch">
            <template #prefix><el-icon><Warning /></el-icon></template>
            <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
          </el-select>

          <el-select v-model="query.sourceDb" clearable placeholder="源库" style="width: 110px" @change="onSearch">
            <el-option v-for="db in DB_OPTIONS" :key="db.value" :label="db.label" :value="db.value" />
          </el-select>

          <el-select v-model="query.targetDb" clearable placeholder="目标库" style="width: 110px" @change="onSearch">
            <el-option v-for="db in DB_OPTIONS" :key="db.value" :label="db.label" :value="db.value" />
          </el-select>

          <div class="date-range">
            <el-date-picker
              v-model="(query as any).beginTime"
              type="datetime"
              placeholder="开始时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm"
              style="width: 190px"
              :prefix-icon="Calendar"
            />
            <span class="date-separator">-</span>
            <el-date-picker
              v-model="(query as any).endTime"
              type="datetime"
              placeholder="结束时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm"
              style="width: 190px"
              :prefix-icon="Calendar"
            />
          </div>

          <el-input v-model="query.entityId" placeholder="搜索实体ID..." clearable style="width: 200px" :prefix-icon="Search" @keyup.enter="onSearch" />
        </template>

        <template #actions>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
        </template>
      </TableToolbar>

      <el-table :data="tableData" v-loading="loading" style="width: 100%" border>
        <el-table-column label="状态" width="160">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small" effect="light" round>
              {{ row.status }}
            </el-tag>
            <span v-if="row.retryCount != null" class="retry">({{ row.retryCount }}次重试)</span>
          </template>
        </el-table-column>
        <el-table-column prop="entityType" label="实体类型" width="140">
          <template #default="{ row }"><el-tag size="small" type="info">{{ row.entityType }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="action" label="操作" width="100" />
        <el-table-column prop="sourceDb" label="源库" width="100" />
        <el-table-column prop="targetDb" label="目标库" width="100" />
        <el-table-column prop="entityId" label="实体ID" min-width="160" show-overflow-tooltip />
        <el-table-column prop="errorMsg" label="错误" min-width="220" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" :icon="View" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === SyncLogStatus.CONFLICT" type="danger" link size="small" :icon="Warning" @click="goConflicts">
              查看冲突
            </el-button>
          </template>
        </el-table-column>
      </el-table>

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
    </el-card>

    <el-drawer v-model="detailVisible" title="日志详情" size="50%">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="实体类型">{{ detail.entityType }}</el-descriptions-item>
        <el-descriptions-item label="实体ID">{{ detail.entityId }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ detail.action }}</el-descriptions-item>
        <el-descriptions-item label="源库">{{ detail.sourceDb }}</el-descriptions-item>
        <el-descriptions-item label="目标库">{{ detail.targetDb }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
        <el-descriptions-item label="重试次数">{{ detail.retryCount }}</el-descriptions-item>
        <el-descriptions-item label="错误信息">
          <pre class="json-content">{{ formatJson(detail.errorMsg) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(detail.updateTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <!-- 清理日志弹窗 -->
    <el-dialog v-model="cleanDialogVisible" title="清理日志" width="420px">
      <el-form label-width="100px">
        <el-form-item label="目标数据库">
          <el-select v-model="cleanParams.db" placeholder="选择库" style="width: 100%">
            <el-option label="全部数据库" value="ALL" />
            <el-option v-for="db in DB_OPTIONS" :key="db.value" :label="db.label" :value="db.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="保留天数">
          <el-input-number v-model="cleanParams.retainDays" :min="0" placeholder="例如：30" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最大条数">
          <el-input-number v-model="cleanParams.maxCount" :min="0" :step="1000" placeholder="例如：10000" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cleanDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmClean" :loading="loading">确认清理</el-button>
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

.date-range {
  display: flex;
  align-items: center;
  gap: 4px;
}

.date-separator {
  color: #909399;
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

.json-content {
  margin: 0;
  font-family: v-mono, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
  background-color: #fafafa;
  padding: 10px;
  border-radius: 4px;
  border: 1px solid #eaeaea;
  max-height: 400px;
  overflow-y: auto;
}
</style>
