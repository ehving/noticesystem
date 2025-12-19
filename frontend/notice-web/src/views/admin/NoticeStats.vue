<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getReadCount, pageNoticeReads } from '@/api/modules/admin/notice-reads'
import type { NoticeReadUserVo } from '@/types/models/notice-read'
import type { MpPage } from '@/types/page'
import { useAdminStore } from '@/stores/admin'
import { error as showError } from '@/utils/message'
import { formatDateTime } from '@/utils/time'
import PageHeader from '@/components/common/PageHeader.vue'
import { useLoading } from '@/hooks/useLoading'

const route = useRoute()
const adminStore = useAdminStore()
const { loading, run } = useLoading()

const noticeId = ref<string>((route.query.noticeId as string) || '')
const readCount = ref<number | null>(null)
const tableData = ref<NoticeReadUserVo[]>([])
const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0,
})

const formatTime = (val?: string) => formatDateTime(val)

const fetchAll = async () => {
  if (!noticeId.value) {
    showError('请先输入公告ID')
    return
  }
  await run(async () => {
    readCount.value = await getReadCount(noticeId.value)
    const res: MpPage<NoticeReadUserVo> = await pageNoticeReads(noticeId.value, {
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
    })
    tableData.value = res.records || []
    pagination.total = res.total || 0
    pagination.pageNo = res.current || pagination.pageNo
    pagination.pageSize = res.size || pagination.pageSize
  })
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

onMounted(() => {
  if (noticeId.value) fetchAll()
})

watch(
  () => adminStore.changeTick,
  () => {
    if (noticeId.value) fetchAll()
  }
)
</script>

<template>
  <div class="page">
    <el-card>
      <PageHeader title="公告阅读统计" :sub-title="`当前库：${adminStore.activeDb}`">
        <el-input v-model="noticeId" placeholder="公告ID" style="width: 240px" />
        <el-button type="primary" @click="fetchAll">查询</el-button>
      </PageHeader>

      <el-card class="summary" v-if="readCount !== null">
        已读人数：{{ readCount }}
      </el-card>

      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="nickname" label="昵称" min-width="140" />
        <el-table-column prop="deptName" label="部门" min-width="160" />
        <el-table-column prop="deviceType" label="设备" width="120" />
        <el-table-column label="阅读时间" width="180">
          <template #default="{ row }">{{ formatTime(row.readTime) }}</template>
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
    </el-card>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.summary {
  margin-bottom: 12px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
