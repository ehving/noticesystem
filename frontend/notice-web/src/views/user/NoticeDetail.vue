<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getNoticeDetail } from '@/api/modules/notice'
import type { Notice } from '@/types/models/notice'
import { error as showError } from '@/utils/message'
import { formatDateTime } from '@/utils/time'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const detail = ref<Notice | null>(null)
const errorMsg = ref('')
const useHtml = ref(false)

const formatTime = (val?: string) => formatDateTime(val)

const loadDetail = async (id: string) => {
  if (!authStore.isLoggedIn) {
    errorMsg.value = '请登录后查看公告'
    return
  }
  loading.value = true
  errorMsg.value = ''
  detail.value = null
  try {
    const data = await getNoticeDetail(id)
    detail.value = data
  } catch (e) {
    console.error(e)
    errorMsg.value = '公告不存在或不可查看'
    showError(errorMsg.value)
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push({ path: '/notices', query: route.query })
}

onMounted(() => {
  if (route.params.id) {
    loadDetail(String(route.params.id))
  }
})

watch(
  () => route.params.id,
  (id) => {
    if (id) loadDetail(String(id))
  }
)
</script>

<template>
  <div class="page">
    <el-card v-loading="loading">
      <template #header>
        <div class="header">
          <div>
            <div class="title">{{ detail?.title || '公告详情' }}</div>
            <div class="meta">
              <el-tag v-if="detail" size="small" :type="detail.level === 'URGENT' ? 'danger' : detail.level === 'IMPORTANT' ? 'warning' : 'info'">
                {{ detail?.level === 'NORMAL' ? '普通' : detail?.level === 'IMPORTANT' ? '重要' : '紧急' }}
              </el-tag>
              <span class="time">发布时间：{{ formatTime(detail?.publishTime) }}</span>
              <span class="time">过期时间：{{ formatTime(detail?.expireTime) }}</span>
            </div>
          </div>
          <el-button type="primary" link @click="goBack">返回列表</el-button>
        </div>
      </template>

      <div v-if="errorMsg" class="error">
        {{ errorMsg }}
        <el-button type="primary" text @click="goBack">返回列表</el-button>
      </div>
      <div v-else-if="detail">
        <div class="switch">
          <el-switch v-model="useHtml" active-text="富文本预览" inactive-text="纯文本（安全）" />
          <div class="tip" v-if="useHtml">注意：请确保后端已进行内容过滤，谨慎开启。</div>
        </div>
        <div v-if="useHtml" class="content" v-html="detail.content"></div>
        <div v-else class="content">{{ detail.content }}</div>
      </div>
      <div v-else>加载中...</div>
    </el-card>
  </div>
</template>

<style scoped>
.page {
  padding: 8px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 18px;
  font-weight: 600;
}

.meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #909399;
  margin-top: 6px;
  font-size: 13px;
}

.time {
  display: inline-block;
}

.content {
  padding-top: 12px;
  line-height: 1.7;
  color: #303133;
}

.error {
  color: #f56c6c;
}

.switch {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.tip {
  color: #f56c6c;
  font-size: 12px;
}
</style>
