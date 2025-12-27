<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useAdminStore } from '@/stores/admin'
import { useLoading } from '@/hooks/useLoading'
import { useConfirm } from '@/hooks/useConfirm'
import { error, success } from '@/utils/message'
import {
  pageConflicts,
  getConflictDetail,
  resolveByDb,
  ignoreConflict,
  reopenConflict,
  recheckConflict,
} from '@/api/modules/admin/conflicts'
import { ConflictStatus, ConflictType, SyncEntityType } from '@/types/enums/sync'
import { DB_OPTIONS, type DatabaseType } from '@/types/enums/db'
import type { MpPage } from '@/types/page'
import type { SyncConflictListVo, SyncConflictDetailVo, SyncConflictQuery } from '@/types/models/sync-conflict'
import { formatDateTime } from '@/utils/time'
import PageHeader from '@/components/common/PageHeader.vue'
import TableToolbar from '@/components/common/TableToolbar.vue'
import ConflictDialog from '@/components/admin/ConflictDialog.vue'
import {
  Search, RefreshLeft, View, MagicStick, Hide,
  VideoPlay, Refresh, Filter, Connection, Calendar, Warning
} from '@element-plus/icons-vue'

const adminStore = useAdminStore()
const { loading, run } = useLoading()
const { loading: actionLoading, run: runAction } = useLoading()
const { confirm } = useConfirm()

const list = ref<SyncConflictListVo[]>([])
const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0,
})

const query = reactive({
  status: '' as ConflictStatus | '',
  conflictType: '' as ConflictType | '',
  entityType: undefined as SyncEntityType | undefined,
  sourceDb: undefined as DatabaseType | undefined,
  beginTime: '',
  endTime: '',
})

const dialogVisible = ref(false)
const selectedConflict = ref<SyncConflictDetailVo | null>(null)

const resolveDialogVisible = ref(false)
const resolveForm = reactive({
  conflictId: '',
  sourceDb: undefined as DatabaseType | undefined,
  note: '',
})

const buildQuery = (): SyncConflictQuery => ({
  pageNo: pagination.pageNo,
  pageSize: pagination.pageSize,
  status: query.status || undefined,
  conflictType: query.conflictType || undefined,
  entityType: query.entityType || undefined,
  sourceDb: query.sourceDb || undefined,
  beginTime: query.beginTime || undefined,
  endTime: query.endTime || undefined,
})

const fetchList = () =>
  run(async () => {
    const res: MpPage<SyncConflictListVo> = await pageConflicts(buildQuery())
    list.value = res.records || []
    pagination.total = res.total || 0
    pagination.pageNo = res.current || pagination.pageNo
    pagination.pageSize = res.size || pagination.pageSize
  })

const onSearch = () => {
  pagination.pageNo = 1
  fetchList()
}

const onReset = () => {
  query.status = ''
  query.conflictType = ''
  query.entityType = undefined
  query.sourceDb = undefined
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

const handleShowDetail = async (row: SyncConflictListVo) => {
  await runAction(async () => {
    selectedConflict.value = await getConflictDetail(row.id)
    dialogVisible.value = true
  })
}

const openResolve = (row: SyncConflictListVo) => {
  resolveForm.conflictId = row.id
  resolveForm.sourceDb = undefined
  resolveForm.note = ''
  resolveDialogVisible.value = true
}

const submitResolve = async () => {
  const db = resolveForm.sourceDb
  if (!db) {
    error('请选择来源库')
    return
  }
  await runAction(async () => {
    await resolveByDb(resolveForm.conflictId, db, resolveForm.note || undefined)
    success('已提交修复')
    resolveDialogVisible.value = false
    fetchList()
  })
}


const handleIgnore = async (row: SyncConflictListVo) => {
  const ok = await confirm('确认要忽略该冲突吗？', '提示')
  if (!ok) return
  await runAction(async () => {
    await ignoreConflict(row.id)
    success('已忽略')
    fetchList()
  })
}

const handleReopen = async (row: SyncConflictListVo) => {
  const ok = await confirm('确认要重开该冲突吗？', '提示')
  if (!ok) return
  await runAction(async () => {
    await reopenConflict(row.id)
    success('已重开')
    fetchList()
  })
}

const handleRecheck = async (row: SyncConflictListVo) => {
  const ok = await confirm('确认要重新检测该冲突吗？', '提示')
  if (!ok) return
  await runAction(async () => {
    await recheckConflict(row.id)
    success('已提交重新检测')
    fetchList()
  })
}

const handleDialogClose = () => {
  dialogVisible.value = false
  selectedConflict.value = null
}

const handleResolveClose = () => {
  resolveDialogVisible.value = false
  resolveForm.conflictId = ''
  resolveForm.sourceDb = undefined
  resolveForm.note = ''
}


onMounted(fetchList)

watch(
  () => adminStore.changeTick,
  () => fetchList()
)
</script>

<template>
  <div class="page">
    <el-card>
      <PageHeader title="冲突管理" :sub-title="`当前库：${adminStore.activeDb}`">
        <el-button :icon="RefreshLeft" @click="fetchList" :loading="loading">刷新</el-button>
      </PageHeader>

      <TableToolbar>
        <template #filters>
          <el-select v-model="query.status" clearable placeholder="处理状态" style="width: 120px" @change="onSearch">
            <template #prefix><el-icon><Warning /></el-icon></template>
            <el-option v-for="s in Object.values(ConflictStatus)" :key="s" :label="s" :value="s" />
          </el-select>
          <el-select v-model="query.conflictType" clearable placeholder="冲突类型" style="width: 120px" @change="onSearch">
            <template #prefix><el-icon><Filter /></el-icon></template>
            <el-option v-for="t in Object.values(ConflictType)" :key="t" :label="t" :value="t" />
          </el-select>
          <el-select v-model="query.entityType" clearable placeholder="实体类型" style="width: 140px" @change="onSearch">
            <el-option v-for="e in Object.values(SyncEntityType)" :key="e" :label="e" :value="e" />
          </el-select>
          <el-select v-model="query.sourceDb" clearable placeholder="来源库" style="width: 120px" @change="onSearch">
            <template #prefix><el-icon><Connection /></el-icon></template>
            <el-option v-for="db in DB_OPTIONS" :key="db.value" :label="db.label" :value="db.value" />
          </el-select>

          <div class="date-range">
            <el-date-picker
              v-model="query.beginTime"
              type="datetime"
              placeholder="开始时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm"
              style="width: 190px"
              :prefix-icon="Calendar"
            />
            <span class="date-separator">-</span>
            <el-date-picker
              v-model="query.endTime"
              type="datetime"
              placeholder="结束时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm"
              style="width: 190px"
              :prefix-icon="Calendar"
            />
          </div>
        </template>
        <template #actions>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="RefreshLeft" @click="onReset">重置</el-button>
        </template>
      </TableToolbar>

      <el-table :data="list" v-loading="loading" border style="width: 100%">
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag
              :type="row.status === ConflictStatus.OPEN ? 'danger' : row.status === ConflictStatus.RESOLVED ? 'success' : 'info'"
              effect="light"
              round
            >
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="conflictType" label="冲突类型" width="120">
          <template #default="{ row }">
            <el-tag :type="row.conflictType === ConflictType.MISSING ? 'warning' : 'danger'" effect="plain">
              {{ row.conflictType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="entityType" label="实体类型" width="140" />
        <el-table-column prop="entityId" label="实体ID" min-width="160" show-overflow-tooltip />
        <el-table-column label="首次出现" width="170">
          <template #default="{ row }">{{ formatDateTime(row.firstSeenAt) }}</template>
        </el-table-column>
        <el-table-column label="最后出现" width="170">
          <template #default="{ row }">{{ formatDateTime(row.lastSeenAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-tooltip content="查看详情" placement="top">
              <el-button size="small" circle :icon="View" @click="handleShowDetail(row)" />
            </el-tooltip>

            <template v-if="row.status === ConflictStatus.OPEN">
              <el-tooltip content="修复冲突" placement="top">
                <el-button size="small" type="primary" circle :icon="MagicStick" @click="openResolve(row)" />
              </el-tooltip>
              <el-tooltip content="忽略冲突" placement="top">
                <el-button size="small" type="warning" circle :icon="Hide" @click="handleIgnore(row)" />
              </el-tooltip>
            </template>

            <template v-else>
              <el-tooltip content="重新打开" placement="top">
                <el-button size="small" type="danger" circle :icon="VideoPlay" @click="handleReopen(row)" />
              </el-tooltip>
            </template>

            <el-tooltip content="重新检测" placement="top">
              <el-button size="small" circle :icon="Refresh" @click="handleRecheck(row)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-if="pagination.total > 0"
          v-model:current-page="pagination.pageNo"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <ConflictDialog
      v-if="dialogVisible"
      :visible="dialogVisible"
      :conflict="selectedConflict"
      @close="handleDialogClose"
    />

    <el-dialog v-model="resolveDialogVisible" title="修复冲突" width="420px" destroy-on-close @closed="handleResolveClose">
      <el-form label-width="110px">
        <el-form-item label="来源库" required>
          <el-select v-model="resolveForm.sourceDb" placeholder="请选择来源库">
            <el-option v-for="db in DB_OPTIONS" :key="db.value" :label="db.label" :value="db.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="resolveForm.note" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="submitResolve">确认修复</el-button>
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
</style>
