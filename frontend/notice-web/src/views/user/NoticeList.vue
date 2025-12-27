<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { pageNotices } from '@/api/modules/notice'
import type { Notice } from '@/types/models/notice'
import type {NoticeLevel} from "@/types/enums/notice.ts"
import { formatDateTime } from '@/utils/time'
import { useAuthStore } from '@/stores/auth'
import { Search, RefreshLeft, View, Timer, BellFilled, ArrowRight, Filter } from '@element-plus/icons-vue'

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
  } finally {
    loading.value = false
  }
}

const onSearch = () => {
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
  <div class="notice-container">
    <div v-if="!authStore.isLoggedIn" class="login-empty">
      <el-empty description="请登录后查看公告">
        <el-button type="primary" round @click="router.push('/login')">去登录</el-button>
      </el-empty>
    </div>
    <template v-else>
      <!-- 顶部搜索与筛选栏 -->
      <div class="filter-bar">
        <div class="page-header">
          <div class="icon-wrapper">
            <el-icon class="header-icon"><BellFilled /></el-icon>
          </div>
          <span class="header-title">公告列表</span>
        </div>

        <div class="filter-actions">
          <div class="search-input-wrapper">
            <el-input
              v-model="query.keyword"
              placeholder="搜索公告标题..."
              clearable
              :prefix-icon="Search"
              class="cyber-input"
              @keyup.enter="onSearch"
            />
          </div>

          <el-select
            v-model="query.level"
            placeholder="公告级别"
            style="width: 120px"
            class="cyber-select"
            popper-class="cyber-dropdown"
            @change="onSearch"
          >
            <template #prefix><el-icon><Filter /></el-icon></template>
            <el-option v-for="opt in levelOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>

          <el-button type="primary" :icon="Search" @click="onSearch" circle class="action-btn" />
          <el-button :icon="RefreshLeft" circle @click="() => { query.keyword = ''; query.level = 'ALL'; onSearch() }" class="action-btn" />
        </div>
      </div>

      <!-- 公告卡片网格 -->
      <div class="notice-list" v-loading="loading" element-loading-background="rgba(0,0,0,0.5)">
        <el-empty v-if="!loading && tableData.length === 0" description="暂无公告数据" />

        <div class="notice-grid">
          <div
            v-for="item in tableData"
            :key="item.id"
            class="notice-card"
            @click="gotoDetail(item)"
          >
            <!-- 装饰角标 -->
            <div class="card-corner"></div>

            <div class="card-top">
              <el-tag :type="levelTagType[item.level]" effect="dark" round size="small" class="level-tag">
                {{ item.level === 'NORMAL' ? '普通' : item.level === 'IMPORTANT' ? '重要' : '紧急' }}
              </el-tag>
              <span class="publish-time">
                <el-icon><Timer /></el-icon> {{ formatTime(item.publishTime)?.split(' ')[0] }}
              </span>
            </div>

            <h3 class="card-title" :title="item.title">{{ item.title }}</h3>

            <div class="card-bottom">
              <div class="meta-info">
                <span class="views"><el-icon><View /></el-icon> {{ item.viewCount }}</span>
              </div>
              <div class="read-more">
                详情 <el-icon><ArrowRight /></el-icon>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="tableData.length > 0">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="pagination.total"
            :page-size="pagination.pageSize"
            :current-page="pagination.pageNo"
            @current-change="handleCurrentChange"
            class="cyber-pagination"
          />
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.notice-container {
  max-width: 1200px;
  margin: 0 auto;
  padding-bottom: 40px;
  color: #e2e8f0;
}

/* Filter Bar */
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  background: rgba(30, 41, 59, 0.6);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 16px 24px;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.icon-wrapper {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(6, 182, 212, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(6, 182, 212, 0.3);
}

.header-icon {
  color: #06b6d4;
  font-size: 20px;
}

.header-title {
  font-size: 18px;
  font-weight: 700;
  color: #f1f5f9;
  letter-spacing: 0.5px;
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-input-wrapper {
  width: 240px;
}

/* Custom Inputs for Dark Theme */
:deep(.cyber-input) {
  .el-input__wrapper {
    background-color: rgba(15, 23, 42, 0.6);
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1) inset;
    border-radius: 8px;
    color: #fff;

    &.is-focus {
      box-shadow: 0 0 0 1px #06b6d4 inset;
    }
  }
  .el-input__inner {
    color: #f1f5f9;
    &::placeholder {
      color: #64748b;
    }
  }
}

:deep(.cyber-select) {
  .el-input__wrapper {
    background-color: rgba(15, 23, 42, 0.6);
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1) inset;
    border-radius: 8px;
  }
  .el-input__inner {
    color: #f1f5f9;
  }
}

.action-btn {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #94a3b8;

  &:hover {
    background: rgba(6, 182, 212, 0.2);
    border-color: #06b6d4;
    color: #fff;
  }
}

/* Notice Grid */
.notice-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

.notice-card {
  background: rgba(30, 41, 59, 0.4);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(255, 255, 255, 0.05);
  position: relative;
  display: flex;
  flex-direction: column;
  height: 180px;
  overflow: hidden;

  /* Shine effect */
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
    opacity: 0;
    transition: opacity 0.3s;
  }

  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 15px 30px rgba(0, 0, 0, 0.4);
    border-color: rgba(6, 182, 212, 0.3);
    background: rgba(30, 41, 59, 0.7);

    &::before {
      opacity: 1;
    }

    .card-corner {
      opacity: 1;
    }
  }
}

.card-corner {
  position: absolute;
  top: 0;
  right: 0;
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, transparent 50%, rgba(6, 182, 212, 0.1) 50%);
  opacity: 0;
  transition: opacity 0.3s;
  pointer-events: none;
  border-top-right-radius: 16px;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.publish-time {
  font-size: 12px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(0, 0, 0, 0.2);
  padding: 4px 8px;
  border-radius: 6px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #e2e8f0;
  margin: 0 0 auto;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.3s;
}

.notice-card:hover .card-title {
  color: #38bdf8;
}

.card-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.meta-info {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: #94a3b8;
}

.views {
  display: flex;
  align-items: center;
  gap: 6px;
}

.read-more {
  font-size: 13px;
  color: #06b6d4;
  display: flex;
  align-items: center;
  gap: 4px;
  opacity: 0;
  transform: translateX(-10px);
  transition: all 0.3s ease;
  font-weight: 600;
}

.notice-card:hover .read-more {
  opacity: 1;
  transform: translateX(0);
}

/* Pagination */
.pagination-wrapper {
  margin-top: 40px;
  display: flex;
  justify-content: center;
}

:deep(.cyber-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: #94a3b8;
  --el-pagination-button-color: #94a3b8;
  --el-pagination-button-bg-color: rgba(30, 41, 59, 0.6);
  --el-pagination-hover-color: #06b6d4;

  .el-pager li {
    background: rgba(30, 41, 59, 0.6);
    border: 1px solid rgba(255, 255, 255, 0.05);
    margin: 0 4px;
    border-radius: 6px;

    &.is-active {
      background: rgba(6, 182, 212, 0.2);
      border-color: #06b6d4;
      color: #fff;
    }

    &:hover:not(.is-active) {
      color: #38bdf8;
    }
  }

  button {
    background: rgba(30, 41, 59, 0.6);
    border: 1px solid rgba(255, 255, 255, 0.05);
    border-radius: 6px;

    &:disabled {
      background: transparent;
      opacity: 0.5;
    }
  }
}

.login-empty {
  padding-top: 80px;
  :deep(.el-empty__description) {
    color: #94a3b8;
  }
}

/* Responsive */
@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
    padding: 16px;
  }

  .filter-actions {
    flex-wrap: wrap;
    gap: 10px;
  }

  .search-input-wrapper {
    width: 100%;
    order: -1;
  }

  .notice-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .notice-card {
    height: auto;
    min-height: 160px;
  }
}
</style>

<style lang="scss">
/* Global dropdown style for this page */
.cyber-dropdown {
  background: rgba(15, 23, 42, 0.95) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;

  .el-select-dropdown__item {
    color: #cbd5e1;
    &.hover, &:hover {
      background-color: rgba(6, 182, 212, 0.15);
      color: #fff;
    }
    &.selected {
      color: #06b6d4;
      font-weight: 600;
    }
  }
}
</style>
