<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { useAdminStore } from '@/stores/admin'
// 引入新接口和原有基础接口
import {
  pageAdminNoticesWithScope, // 新的分页接口
  createNotice,
  updateNotice,
  recallNotice,
  publishNotice,
  deleteNotice,
  getNoticeTargets // 新的获取可见范围接口
} from '@/api/modules/admin/notices'
import { listDeptOptions } from '@/api/modules/dept'
import type { NoticeAdmin, NoticeAdminPageVo, NoticeAdminSaveVo, NoticeAdminRowVo, NoticeScopeType } from '@/types/models/notice-admin'
import type { NoticeLevel, NoticeStatus } from '@/types/enums/notice.ts'
import type { MpPage } from '@/types/page'
import type { DeptOptionVo } from '@/types/models/dept'
import { success, error as showError } from '@/utils/message'
import { formatDateTime } from '@/utils/time'
import PageHeader from '@/components/common/PageHeader.vue'
import TableToolbar from '@/components/common/TableToolbar.vue'
import { useLoading } from '@/hooks/useLoading'
import NoticeReadDialog from '@/components/admin/NoticeReadDialog.vue'
import {
  Search, RefreshLeft, Plus, Edit, Delete, View,
  Bell, Filter, Timer, DataLine, Hide, Document, Calendar, Promotion, VideoPlay, UserFilled
} from '@element-plus/icons-vue'

const adminStore = useAdminStore()
const { loading, run } = useLoading()

// 表格数据类型改为新的 RowVo
const tableData = ref<NoticeAdminRowVo[]>([])
const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0,
})
const query = reactive<NoticeAdminPageVo>({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  status: undefined,
  level: undefined,
  startTime: '',
  endTime: '',
})

const levelOptions: { label: string; value: NoticeLevel }[] = [
  { label: '普通', value: 'NORMAL' },
  { label: '重要', value: 'IMPORTANT' },
  { label: '紧急', value: 'URGENT' },
]
const statusOptions: { label: string; value: NoticeStatus }[] = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已撤回', value: 'RECALLED' },
]
const levelTagType: Record<NoticeLevel, string> = {
  NORMAL: 'info',
  IMPORTANT: 'warning',
  URGENT: 'danger',
}
const statusTagType: Record<NoticeStatus, string> = {
  DRAFT: 'info',
  PUBLISHED: 'success',
  RECALLED: 'warning',
}

const deptOptions = ref<DeptOptionVo[]>([])

// 格式化可见范围显示 (使用后端返回的 preview 数据)
const formatScopeDisplay = (row: NoticeAdminRowVo) => {
  if (row.scopeType === 'GLOBAL') {
    return '全员可见'
  }
  // DEPT 类型
  const names = row.targetDeptNamesPreview || []
  if (names.length === 0) return '指定部门' // 异常情况兜底

  const displayText = names.join('、')
  // 如果实际数量比预览名字数量多，显示 "等X个部门"
  if (row.targetDeptCount > names.length) {
    return `${displayText} 等 ${row.targetDeptCount} 个部门`
  }
  return displayText
}

const dialogVisible = ref(false)
const dialogTitle = ref('新增公告')
const formRef = ref()
const formLoading = ref(false)

const form = reactive<Omit<NoticeAdminSaveVo, 'status'> & { id?: string; status?: NoticeStatus }>({
  title: '',
  content: '',
  level: 'NORMAL',
  publishTime: '',
  expireTime: '',
  targetDeptIds: [],
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
  level: [{ required: true, message: '请选择级别', trigger: 'change' }],
}

// 修改为调用 pageAdminNoticesWithScope
const fetchList = () =>
  run(async () => {
    const res: MpPage<NoticeAdminRowVo> = await pageAdminNoticesWithScope({
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
      keyword: query.keyword || undefined,
      status: query.status,
      level: query.level,
      startTime: query.startTime || undefined,
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
  query.keyword = ''
  query.status = undefined
  query.level = undefined
  query.startTime = ''
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

const openCreate = () => {
  dialogTitle.value = '新增公告'
  Object.assign(form, {
    id: '',
    title: '',
    content: '',
    level: 'NORMAL' as NoticeLevel,
    publishTime: '',
    expireTime: '',
    targetDeptIds: [] as string[],
  })
  dialogVisible.value = true
}

// 修改 openEdit，使用 getNoticeTargets，移除 getNotice
const openEdit = async (row: NoticeAdminRowVo) => {
  dialogTitle.value = '编辑公告'
  dialogVisible.value = true
  formLoading.value = true

  try {
    // 1. 获取关联部门详情
    const targetsResp = await getNoticeTargets(row.notice.id)

    // 2. 基础详情直接使用 row.notice（假设列表已返回所需字段）
    const detail = row.notice

    Object.assign(form, {
      id: detail.id,
      title: detail.title,
      content: detail.content,
      level: detail.level,
      publishTime: detail.publishTime || '',
      expireTime: detail.expireTime || '',
      // 如果 scopeType 是 GLOBAL，ids 为空；如果是 DEPT，ids 为具体部门
      targetDeptIds: targetsResp.scopeType === 'GLOBAL' ? [] : targetsResp.deptIds,
      status: detail.status
    })
  } catch (e) {
    console.error(e)
    showError('获取公告详情失败')
  } finally {
    formLoading.value = false
  }
}

const readDialogVisible = ref(false)
const currentNoticeId = ref<string>('')
const currentNoticeTitle = ref<string>('')

const openReadStats = (row: NoticeAdminRowVo) => {
  currentNoticeId.value = row.notice.id
  currentNoticeTitle.value = row.notice.title
  readDialogVisible.value = true
}

// --- 部门详情弹窗逻辑 ---
const scopeDialogVisible = ref(false)
const scopeList = ref<string[]>([])
const scopeLoading = ref(false)

const openScopeDetail = async (row: NoticeAdminRowVo) => {
  // 如果是全员可见，无需展示详情
  if (row.scopeType === 'GLOBAL') return

  scopeDialogVisible.value = true
  scopeLoading.value = true
  scopeList.value = [] // 清空旧数据

  try {
    const res = await getNoticeTargets(row.notice.id)
    // 从返回的 depts 数组中提取名称
    if (res.depts && res.depts.length > 0) {
      scopeList.value = res.depts.map(d => d.name)
    } else {
      scopeList.value = []
    }
  } catch (e) {
    showError('获取部门详情失败')
  } finally {
    scopeLoading.value = false
  }
}


// --- 预览相关逻辑 ---
const previewVisible = ref(false)
const previewLoading = ref(false)
const useHtmlPreview = ref(false)
// 独立的预览数据对象
const previewData = reactive({
  title: '',
  level: 'NORMAL' as NoticeLevel,
  publishTime: '',
  expireTime: '',
  content: '',
  targetDeptNames: '',
  scopeType: 'GLOBAL' as NoticeScopeType
})

// 辅助：从ID列表获取名称显示 (用于表单预览)
const getDeptNamesByIds = (ids: string[]) => {
  if (!ids || ids.length === 0) return '全员可见'
  const names = ids.map(id => {
    const d = deptOptions.value.find(opt => opt.id === id)
    return d ? d.name : '未知部门'
  })
  return names.join('、')
}

// 从表单预览
const openFormPreview = () => {
  previewData.title = form.title
  previewData.level = form.level
  previewData.publishTime = form.publishTime || '未定发布时间'
  previewData.expireTime = form.expireTime || '永不过期'
  previewData.content = form.content
  previewData.targetDeptNames = getDeptNamesByIds(form.targetDeptIds)
  // 表单中如果 targetDeptIds 为空，则视为 GLOBAL
  previewData.scopeType = (!form.targetDeptIds || form.targetDeptIds.length === 0) ? 'GLOBAL' : 'DEPT'
  previewVisible.value = true
}

// 从表格行预览 (使用 getNoticeTargets 获取完整可见范围，移除 getNotice)
const openRowPreview = async (row: NoticeAdminRowVo) => {
  previewVisible.value = true
  previewLoading.value = true

  try {
    // 获取可见范围详情
    const targetsResp = await getNoticeTargets(row.notice.id)

    // 基础详情直接使用 row.notice
    const detail = row.notice

    previewData.title = detail.title
    previewData.level = detail.level
    previewData.publishTime = detail.publishTime ? formatDateTime(detail.publishTime) : '未定发布时间'
    previewData.expireTime = detail.expireTime ? formatDateTime(detail.expireTime) : '永不过期'
    previewData.content = detail.content

    // 处理可见范围显示
    previewData.scopeType = targetsResp.scopeType
    if (targetsResp.scopeType === 'GLOBAL') {
      previewData.targetDeptNames = '全员可见'
    } else {
      // 使用 targetsResp.depts (包含 {id, name}) 来拼接
      previewData.targetDeptNames = targetsResp.depts.map(d => d.name).join('、')
    }

  } catch (e) {
    showError('获取详情失败')
    previewVisible.value = false
  } finally {
    previewLoading.value = false
  }
}

// 辅助函数：传入 row.notice
const isScheduledDraft = (notice: NoticeAdmin) => {
  return notice.status === 'DRAFT' && notice.publishTime && dayjs(notice.publishTime).isAfter(dayjs())
}

const onSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    if (form.expireTime && form.publishTime && dayjs(form.expireTime).isBefore(dayjs(form.publishTime))) {
      showError('过期时间需晚于发布时间')
      return
    }

    const payload: NoticeAdminSaveVo = {
      title: form.title,
      content: form.content,
      level: form.level,
      status: 'DRAFT',
      publishTime: form.publishTime || undefined,
      expireTime: form.expireTime || undefined,
      targetDeptIds: form.targetDeptIds,
    }

    if (form.id) {
      await updateNotice(form.id, payload)
      success('草稿更新成功')
    } else {
      await createNotice(payload)
      success('草稿保存成功')
    }
    dialogVisible.value = false
    fetchList()
  })
}

const onPublish = async (row: NoticeAdminRowVo) => {
  const notice = row.notice
  const isScheduled = notice.publishTime && dayjs(notice.publishTime).isAfter(dayjs())
  const tips = isScheduled
    ? `该公告设置了定时时间（${formatDateTime(notice.publishTime)}），确定要现在发布生效吗？\n注意：这将忽略定时设置直接发布。`
    : `确定要立即发布公告【${notice.title}】吗？`

  await ElMessageBox.confirm(tips, '发布确认', {
    type: 'success',
    confirmButtonText: '确认发布',
    cancelButtonText: '取消'
  })

  await publishNotice(notice.id)
  success('发布成功')
  fetchList()
}

const onRecall = async (row: NoticeAdminRowVo) => {
  const notice = row.notice
  await ElMessageBox.confirm(`确定撤回公告【${notice.title}】吗？\n撤回后公告将变为不可见状态。`, '撤回确认', {
    type: 'warning',
    confirmButtonText: '确认撤回',
    cancelButtonText: '取消'
  })
  await recallNotice(notice.id)
  success('撤回成功')
  fetchList()
}

const onDelete = async (row: NoticeAdminRowVo) => {
  const notice = row.notice
  await ElMessageBox.confirm(`确定要永久删除公告【${notice.title}】吗？\n此操作无法恢复。`, '删除确认', {
    type: 'error',
    confirmButtonText: '确认删除',
    cancelButtonText: '取消'
  })
  await deleteNotice(notice.id)
  success('删除成功')
  fetchList()
}

onMounted(async () => {
  deptOptions.value = await listDeptOptions()
  fetchList()
})

watch(
  () => adminStore.changeTick,
  () => {
    fetchList()
  }
)
</script>

<template>
  <div class="page">
    <el-card>
      <PageHeader title="公告管理" :sub-title="`当前库：${adminStore.activeDb}`">
        <el-button type="primary" :icon="Plus" @click="openCreate">新增公告</el-button>
        <el-button :icon="RefreshLeft" @click="fetchList" :loading="loading">刷新</el-button>
      </PageHeader>

      <TableToolbar>
        <template #filters>
          <el-input v-model="query.keyword" placeholder="搜索标题关键字" clearable style="width: 220px" :prefix-icon="Search" @keyup.enter.native="onSearch" />
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px" @change="onSearch">
            <template #prefix><el-icon><Promotion /></el-icon></template>
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-select v-model="query.level" placeholder="级别" clearable style="width: 140px" @change="onSearch">
            <template #prefix><el-icon><Filter /></el-icon></template>
            <el-option v-for="opt in levelOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-date-picker
            v-model="query.startTime"
            type="datetime"
            placeholder="发布开始"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm"
            :prefix-icon="Calendar"
            style="width: 180px"
          />
          <el-date-picker
            v-model="query.endTime"
            type="datetime"
            placeholder="结束时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm"
            :prefix-icon="Calendar"
            style="width: 180px"
          />
        </template>
        <template #actions>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
        </template>
      </TableToolbar>

      <el-table :data="tableData" v-loading="loading" style="width: 100%" border>
        <!-- 注意：row 现在是 NoticeAdminRowVo，基础属性在 row.notice 下 -->
        <el-table-column label="标题" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="title-cell">
              <el-icon class="icon-mr"><Bell /></el-icon>
              {{ row.notice.title }}
            </span>
          </template>
        </el-table-column>

        <!-- 可见范围列：使用 row 上的 scopeType 和 preview 数据 -->
        <el-table-column label="可见范围" width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.scopeType === 'GLOBAL'">
              <el-tag type="info" size="small" effect="plain">全员可见</el-tag>
            </span>
            <span v-else @click="openScopeDetail(row)" class="scope-trigger">
              <el-tag type="primary" size="small" effect="plain" class="cursor-pointer">
                <el-icon class="icon-mr"><UserFilled /></el-icon>
                {{ formatScopeDisplay(row) }}
              </el-tag>
            </span>
          </template>
        </el-table-column>

        <el-table-column label="级别" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="levelTagType[row.notice.level]" size="small" effect="light" round>
              {{ row.notice.level === 'NORMAL' ? '普通' : row.notice.level === 'IMPORTANT' ? '重要' : '紧急' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <div v-if="isScheduledDraft(row.notice)">
              <el-tag type="warning" size="small" effect="plain">
                <el-icon><Timer /></el-icon> 定时
              </el-tag>
            </div>
            <div v-else>
              <el-tag :type="statusTagType[row.notice.status]" size="small" effect="plain">
                {{ row.notice.status === 'DRAFT' ? '草稿' : row.notice.status === 'PUBLISHED' ? '已发布' : '已撤回' }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="160">
          <template #default="{ row }">
            <div class="time-cell" v-if="row.notice.publishTime" :class="{ 'text-scheduled': isScheduledDraft(row.notice) }">
              {{ formatDateTime(row.notice.publishTime) }}
            </div>
            <span v-else class="text-gray">-</span>
          </template>
        </el-table-column>
        <el-table-column label="浏览" width="70" align="center">
          <template #default="{ row }">
            <span class="text-gray" style="font-size: 12px;">{{ row.notice.viewCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right" align="center">
          <template #default="{ row }">
            <el-tooltip content="查看详情" placement="top">
              <el-button type="info" link :icon="View" @click="openRowPreview(row)" />
            </el-tooltip>

            <el-tooltip content="发布公告" placement="top" v-if="['DRAFT', 'RECALLED'].includes(row.notice.status)">
              <el-button type="success" link :icon="VideoPlay" @click="onPublish(row)" />
            </el-tooltip>

            <el-tooltip content="撤回" placement="top" v-if="row.notice.status === 'PUBLISHED'">
              <el-button type="warning" link :icon="Hide" @click="onRecall(row)" />
            </el-tooltip>

            <el-tooltip content="编辑" placement="top" v-if="row.notice.status !== 'PUBLISHED'">
              <el-button type="primary" link :icon="Edit" @click="openEdit(row)" />
            </el-tooltip>

            <el-tooltip content="阅读统计" placement="top">
              <el-button type="primary" link :icon="DataLine" @click="openReadStats(row)" />
            </el-tooltip>

            <el-tooltip content="删除" placement="top" v-if="row.notice.status !== 'PUBLISHED'">
              <el-button type="danger" link :icon="Delete" @click="onDelete(row)" />
            </el-tooltip>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="800px" destroy-on-close top="5vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" v-loading="formLoading">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" :prefix-icon="Bell" placeholder="请输入公告标题" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="级别" prop="level">
              <el-select v-model="form.level" style="width: 100%">
                <template #prefix><el-icon><Filter /></el-icon></template>
                <el-option v-for="opt in levelOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="定时发布">
              <el-date-picker
                v-model="form.publishTime"
                type="datetime"
                placeholder="留空则立即发布(需手动点击)"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DD HH:mm"
                style="width: 100%"
                :disabled-date="(date) => dayjs(date).isBefore(dayjs().startOf('day'))"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="过期时间">
              <el-date-picker
                v-model="form.expireTime"
                type="datetime"
                placeholder="永不过期"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DD HH:mm"
                style="width: 100%"
                :disabled-date="(date) => form.publishTime ? dayjs(date).isBefore(dayjs(form.publishTime).startOf('day')) : false"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="目标部门">
          <el-select v-model="form.targetDeptIds" multiple collapse-tags collapse-tags-tooltip placeholder="可多选，留空则全员可见" style="width: 100%">
            <el-option v-for="d in deptOptions" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="支持 HTML 内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer-left">
           <span class="tips-text" v-if="form.publishTime">
             <el-icon><Timer /></el-icon> 将定时于 {{ formatDateTime(form.publishTime) }} 发布
           </span>
        </div>
        <div class="dialog-footer-right">
          <el-button @click="openFormPreview" :icon="View" :disabled="formLoading">预览效果</el-button>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="onSave" :icon="Document" :loading="formLoading">保存草稿</el-button>
        </div>
      </template>
    </el-dialog>

    <NoticeReadDialog v-model="readDialogVisible" :notice-id="currentNoticeId" :notice-title="currentNoticeTitle" />

    <!-- 可见范围详情弹窗 -->
    <el-dialog v-model="scopeDialogVisible" title="可见部门明细" width="500px" append-to-body>
      <div v-loading="scopeLoading" class="scope-list-container">
        <template v-if="scopeList.length > 0">
          <el-tag v-for="(name, index) in scopeList" :key="index" class="mr-2 mb-2">{{ name }}</el-tag>
        </template>
        <el-empty v-else-if="!scopeLoading" description="暂无具体部门信息" :image-size="60" />
      </div>
      <template #footer>
        <el-button @click="scopeDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 预览弹窗：共用于表单预览和表格行查看 -->
    <el-dialog v-model="previewVisible" title="公告内容预览" width="800px" append-to-body>
      <div class="preview-container" v-loading="previewLoading">
        <div class="article-header">
          <div class="tags-row">
            <el-tag :type="levelTagType[previewData.level]" effect="dark" round>
              {{ previewData.level === 'NORMAL' ? '普通公告' : previewData.level === 'IMPORTANT' ? '重要通知' : '紧急通告' }}
            </el-tag>
            <el-tag type="info" effect="plain" round style="margin-left: 8px;" v-if="previewData.targetDeptNames">
              范围: {{ previewData.targetDeptNames }}
            </el-tag>
          </div>
          <h1 class="article-title">{{ previewData.title || '未命名公告' }}</h1>
          <div class="article-meta">
            <span><el-icon><Calendar /></el-icon> 发布: {{ previewData.publishTime }}</span>
            <span><el-icon><Timer /></el-icon> 过期: {{ previewData.expireTime }}</span>
          </div>
        </div>
        <el-divider border-style="dashed" />
        <div class="preview-controls">
          <el-switch v-model="useHtmlPreview" active-text="富文本预览" inactive-text="纯文本预览" />
        </div>
        <div class="article-body">
          <div v-if="useHtmlPreview" v-html="previewData.content" class="content-html"></div>
          <div v-else class="content-text">{{ previewData.content }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.icon-mr {
  margin-right: 4px;
  vertical-align: middle;
}

.time-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
  font-size: 13px;
}

.text-scheduled {
  color: #e6a23c;
  font-weight: bold;
}

.text-gray {
  color: #909399;
}

.cursor-pointer {
  cursor: pointer;
}
.scope-trigger {
  cursor: pointer;
  display: inline-flex;
}

/* Dialog Footer layout */
:deep(.el-dialog__footer) {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.dialog-footer-left {
  font-size: 12px;
  color: #e6a23c;
}
.tips-text {
  display: flex;
  align-items: center;
  gap: 4px;
}
.dialog-footer-right {
  display: flex;
  gap: 12px;
}

/* Scope List Styles */
.scope-list-container {
  max-height: 400px;
  overflow-y: auto;
  padding: 10px;
}
.mr-2 { margin-right: 8px; }
.mb-2 { margin-bottom: 8px; }

/* Preview Styles */
.preview-container {
  padding: 0 20px 20px;
  min-height: 200px; /* 给loading留出空间 */
}
.article-header {
  text-align: center;
  margin-bottom: 20px;
}
.tags-row {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 10px;
}
.article-title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  margin: 12px 0;
}
.article-meta {
  display: flex;
  justify-content: center;
  gap: 20px;
  color: #909399;
  font-size: 13px;
}
.preview-controls {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 10px;
}
.content-text {
  font-size: 15px;
  line-height: 1.8;
  color: #303133;
  white-space: pre-wrap;
  padding: 10px;
  background-color: #f8f9fa;
  border-radius: 4px;
}
.content-html {
  font-size: 15px;
  line-height: 1.8;
  color: #303133;
  overflow-x: auto;
}
</style>
