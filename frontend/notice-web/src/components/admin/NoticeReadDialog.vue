<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { getReadCount, pageNoticeReads } from '@/api/modules/admin/notice-reads'
import type { NoticeReadUserVo } from '@/types/models/notice-read'
import type { MpPage } from '@/types/page'
import { useAdminStore } from '@/stores/admin'
import { formatDateTime } from '@/utils/time'
import { useLoading } from '@/hooks/useLoading'

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
    readCount.value = await getReadCount(props.noticeId as string)
    const res: MpPage<NoticeReadUserVo> = await pageNoticeReads(props.noticeId as string, {
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
    })
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
</script>

<template>
  <el-dialog v-model="visible" title="阅读统计" width="720px" destroy-on-close @close="close">
    <div class="meta">
      <div>公告ID：{{ noticeId || '-' }}</div>
      <div v-if="noticeTitle">标题：{{ noticeTitle }}</div>
      <div>当前库：{{ adminStore.activeDb }}</div>
      <el-button size="small" @click="fetchAll" :loading="loading">刷新</el-button>
    </div>
    <el-card v-if="readCount !== null" class="summary">已读人数：{{ readCount }}</el-card>
    <el-table :data="tableData" v-loading="loading" style="width: 100%">
      <el-table-column prop="username" label="用户名" min-width="140" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column prop="deptName" label="部门" min-width="160" />
      <el-table-column prop="deviceType" label="设备" width="120" />
      <el-table-column label="阅读时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.readTime) }}</template>
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
    <template #footer>
      <el-button @click="close">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.meta {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.summary {
  margin-bottom: 8px;
}
.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
