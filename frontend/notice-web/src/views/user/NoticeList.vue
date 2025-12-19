<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { pageNotices } from '@/api/modules/notice'
import type { Notice, NoticeLevel } from '@/types/models/notice'
import { formatDateTime } from '@/utils/time'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const tableData = ref<Notice[]>([])
const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0,
})
const query = reactive<{
  keyword: string
  level: NoticeLevel | '' | 'ALL'
}>({
  keyword: '',
  level: 'ALL',
})

const levelOptions: { label: string; value: NoticeLevel | 'ALL' }[] = [
  { label: '全部', value: 'ALL' },
  { label: '普通', value: 'NORMAL' },
  { label: '重要', value: 'IMPORTANT' },
  { label: '紧急', value: 'URGENT' },
]

const levelTagType: Record<NoticeLevel, string> = {
  NORMAL: 'info',
  IMPORTANT: 'warning',
  URGENT: 'danger',
}

const fetchList = async () => {
  if (!authStore.isLoggedIn) return
  loading.value = true
  try {
    const res = await pageNotices({
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
      keyword: query.keyword || undefined,
      level: query.level === 'ALL' ? undefined : query.level,
    })
    tableData.value = res.records || []
    pagination.total = res.total || 0
    pagination.pageNo = res.current || pagination.pageNo
    pagination.pageSize = res.size || pagination.pageSize
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const onSearch = () => {
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

const gotoDetail = (row: Notice) => {
  router.push(`/notices/${row.id}`)
}

const formatTime = (val?: string) => formatDateTime(val)

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div class="page">
    <div v-if="!authStore.isLoggedIn" class="login-empty">
      <el-empty description="请登录后查看公告">
        <el-button type="primary" @click="router.push('/login')">去登录</el-button>
      </el-empty>
    </div>
    <template v-else>
    <el-card class="toolbar">
      <el-form inline @submit.prevent>
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="标题/内容关键字"
            clearable
            @keyup.enter.native="onSearch"
          />
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="query.level" placeholder="选择级别" style="width: 140px" @change="onSearch">
            <el-option v-for="opt in levelOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="() => { query.keyword=''; query.level='ALL'; onSearch() }">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="200">
          <template #default="{ row }">
            <el-link type="primary" @click="gotoDetail(row)">{{ row.title }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="级别" width="120">
          <template #default="{ row }">
            <el-tag :type="levelTagType[row.level]" size="small">
              {{ row.level === 'NORMAL' ? '普通' : row.level === 'IMPORTANT' ? '重要' : '紧急' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.publishTime) }}
          </template>
        </el-table-column>
        <el-table-column label="过期时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.expireTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览量" width="100" />
      </el-table>
      <el-empty v-if="!loading && tableData.length === 0" description="暂无数据" />
      <div class="pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="pagination.total"
          :page-size="pagination.pageSize"
          :current-page="pagination.pageNo"
          :page-sizes="[5, 10, 20, 50]"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
    </template>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.toolbar {
  padding-bottom: 0;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.login-empty {
  padding-top: 40px;
}
</style>
