<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getNoticeDetail } from '@/api/modules/notice'
import type { Notice } from '@/types/models/notice'
import { error as showError } from '@/utils/message'
import { formatDateTime } from '@/utils/time'
import { useAuthStore } from '@/stores/auth'
import { Back, Calendar, Timer, View } from '@element-plus/icons-vue'
import DOMPurify from 'dompurify' // 引入 DOMPurify

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const detail = ref<Notice | null>(null)
const errorMsg = ref('')
// 内部状态，不再暴露给用户切换，而是自动判断
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

    if (data && data.content) {
      // 1. 安全清洗：防止 XSS 攻击
      // 配置允许 target="_blank" 等属性，但移除脚本
      const cleanContent = DOMPurify.sanitize(data.content, {
        ADD_TAGS: ['iframe'], // 如果允许嵌入视频可保留，否则建议移除
        ADD_ATTR: ['target'],
      })

      data.content = cleanContent

      // 2. 智能检测：判断清洗后的内容是否包含 HTML 标签
      // 简单的正则检测：如果有 <tag> 结构，则视为富文本
      const htmlTagRegex = /<[a-z][\s\S]*>/i
      useHtml.value = htmlTagRegex.test(cleanContent)
    }

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
  <div class="detail-container">
    <!-- 悬浮返回按钮 (Cyber Style) -->
    <div class="fab-back" @click="goBack" title="返回列表">
      <el-icon><Back /></el-icon>
    </div>

    <!-- 核心内容卡片 (Reading Pad) -->
    <div class="content-wrapper" v-loading="loading">
      <div v-if="errorMsg" class="error-state">
        <el-empty :description="errorMsg">
          <el-button type="primary" @click="goBack">返回列表</el-button>
        </el-empty>
      </div>

      <template v-else-if="detail">
        <!-- 顶部装饰条 -->
        <div class="tech-bar"></div>

        <!-- 文章头部 -->
        <div class="article-header">
          <div class="tags-row">
            <el-tag
              :type="detail.level === 'URGENT' ? 'danger' : detail.level === 'IMPORTANT' ? 'warning' : 'info'"
              effect="dark"
              round
              class="level-badge"
            >
              {{ detail.level === 'NORMAL' ? '普通公告' : detail.level === 'IMPORTANT' ? '重要通知' : '紧急通告' }}
            </el-tag>
          </div>

          <h1 class="article-title">{{ detail.title }}</h1>

          <div class="article-meta">
            <span class="meta-item">
              <el-icon><Calendar /></el-icon> 发布: {{ formatTime(detail.publishTime) }}
            </span>
            <span class="meta-item">
              <el-icon><Timer /></el-icon> 过期: {{ formatTime(detail.expireTime) }}
            </span>
            <span class="meta-item" v-if="(detail as any).viewCount !== undefined">
              <el-icon><View /></el-icon> {{ (detail as any).viewCount }} 阅读
            </span>
          </div>
        </div>

        <el-divider class="custom-divider" />

        <!-- 内容区域 -->
        <div class="article-body">
          <!-- 安全提示 (仅在检测到富文本时隐晦展示，增加安全感) -->
          <div v-if="useHtml" class="secure-badge">
            <el-icon><ShieldCheck /></el-icon> 内容已通过安全检测
          </div>

          <!-- 正文内容：自动切换显示模式 -->
          <div v-if="useHtml" class="content-html" v-html="detail.content"></div>
          <div v-else class="content-text">
            {{ detail.content }}
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped lang="scss">
.detail-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px 20px 80px;
  position: relative;
}

/* Floating Action Button (Cyber Style) */
.fab-back {
  position: fixed;
  bottom: 60px;
  right: 60px;
  width: 56px;
  height: 56px;
  background: rgba(30, 41, 59, 0.8);
  backdrop-filter: blur(10px);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  z-index: 100;
  font-size: 24px;
  border: 1px solid rgba(255, 255, 255, 0.1);

  &:hover {
    transform: translateY(-4px) scale(1.1);
    background: linear-gradient(135deg, #06b6d4 0%, #3b82f6 100%);
    box-shadow: 0 0 20px rgba(6, 182, 212, 0.5);
    border-color: transparent;
  }
}

/* Reading Pad Card */
.content-wrapper {
  background: rgba(248, 250, 252, 0.96);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  padding: 0;
  box-shadow:
    0 20px 50px rgba(0, 0, 0, 0.5),
    0 0 0 1px rgba(255, 255, 255, 0.1);
  min-height: 500px;
  position: relative;
  overflow: hidden;
}

/* 顶部科技装饰条 */
.tech-bar {
  height: 6px;
  width: 100%;
  background: linear-gradient(90deg, #06b6d4, #8b5cf6);
  position: absolute;
  top: 0;
  left: 0;
}

.article-header {
  text-align: center;
  margin-bottom: 24px;
  padding: 40px 50px 0;
}

.tags-row {
  margin-bottom: 20px;

  .level-badge {
    font-weight: 600;
    letter-spacing: 1px;
    border: none;
  }
}

.article-title {
  font-size: 32px;
  font-weight: 800;
  color: #1e293b;
  line-height: 1.4;
  margin: 0 0 24px;
  letter-spacing: -0.5px;
}

.article-meta {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 32px;
  color: #64748b;
  font-size: 14px;
  font-family: 'Menlo', 'Monaco', monospace;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;

  .el-icon {
    color: #0ea5e9;
  }
}

.custom-divider {
  border-color: #e2e8f0;
  margin: 30px 0;
}

.article-body {
  padding: 0 50px 60px;
  position: relative;
}

/* 安全检测徽章 */
.secure-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #10b981; /* Emerald Green */
  background: rgba(16, 185, 129, 0.1);
  padding: 4px 10px;
  border-radius: 20px;
  margin-bottom: 20px;
  font-weight: 500;
}

/* 核心阅读内容 */
.content-text {
  font-size: 17px;
  line-height: 1.8;
  color: #334155;
  white-space: pre-wrap;
  text-align: justify;
  font-family: 'Georgia', serif;
}

.content-html {
  font-size: 17px;
  line-height: 1.8;
  color: #334155;
  overflow-x: auto;

  /* Reset generic styles inside HTML content */
  :deep(p) { margin-bottom: 1em; }
  :deep(h1), :deep(h2), :deep(h3) { color: #0f172a; margin-top: 1.5em; margin-bottom: 0.5em; font-weight: 700; }
  :deep(ul), :deep(ol) { padding-left: 20px; margin-bottom: 1em; }
  :deep(li) { margin-bottom: 0.5em; }
  :deep(a) { color: #0284c7; text-decoration: underline; transition: color 0.2s; &:hover { color: #0ea5e9; } }
  :deep(img) { max-width: 100%; height: auto; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); margin: 10px 0; }
  :deep(blockquote) {
    border-left: 4px solid #06b6d4;
    padding-left: 16px;
    background: #f1f5f9;
    padding: 12px 16px;
    border-radius: 0 4px 4px 0;
    color: #475569;
    margin: 1.5em 0;
    font-style: italic;
  }
  :deep(pre) {
    background: #1e293b;
    color: #e2e8f0;
    padding: 16px;
    border-radius: 8px;
    overflow-x: auto;
    font-family: 'Menlo', monospace;
    font-size: 14px;
  }
}

.error-state {
  padding: 80px 0;
}

@media (max-width: 768px) {
  .detail-container {
    padding: 20px 16px;
  }
  .content-wrapper {
    padding: 0;
  }
  .article-header {
    padding: 30px 20px 0;
  }
  .article-body {
    padding: 0 20px 40px;
  }
  .article-title {
    font-size: 24px;
  }
  .article-meta {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  .fab-back {
    bottom: 30px;
    right: 20px;
    width: 48px;
    height: 48px;
  }
}
</style>
