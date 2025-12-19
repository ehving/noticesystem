<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { useAdminStore } from '@/stores/admin'
import { pageAdminNotices, createNotice, updateNotice, recallNotice } from '@/api/modules/admin/notices'
import { listDeptOptions } from '@/api/modules/dept'
import type { NoticeAdmin, NoticeAdminPageVo, NoticeAdminSaveVo, NoticeLevel, NoticeStatus } from '@/types/models/notice-admin'
import type { MpPage } from '@/types/page'
import type { DeptOptionVo } from '@/types/models/dept'
import { success, error as showError } from '@/utils/message'
import { formatDateTime } from '@/utils/time'
import PageHeader from '@/components/common/PageHeader.vue'
import TableToolbar from '@/components/common/TableToolbar.vue'
import { useLoading } from '@/hooks/useLoading'
import NoticeReadDialog from '@/components/admin/NoticeReadDialog.vue'

const adminStore = useAdminStore()
const { loading, run } = useLoading()

const tableData = ref<NoticeAdmin[]>([])
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

const dialogVisible = ref(false)
const dialogTitle = ref('新增公告')
const formRef = ref()
const form = reactive<NoticeAdminSaveVo>({
  title: '',
  content: '',
  level: 'NORMAL',
  status: 'DRAFT',
  publishTime: '',
  expireTime: '',
  targetDeptIds: [],
})
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
}

const fetchList = () =>
  run(async () => {
    const res: MpPage<NoticeAdmin> = await pageAdminNotices({
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
    status: 'DRAFT' as NoticeStatus,
    publishTime: '',
    expireTime: '',
    targetDeptIds: [] as string[],
  })
  dialogVisible.value = true
}

const openEdit = (row: NoticeAdmin) => {
  dialogTitle.value = '编辑公告'
  Object.assign(form, {
    id: row.id,
    title: row.title,
    content: row.content,
    level: row.level,
    status: row.status,
    publishTime: row.publishTime || '',
    expireTime: row.expireTime || '',
    targetDeptIds: [],
  })
  dialogVisible.value = true
}

const readDialogVisible = ref(false)
const currentNoticeId = ref<string>('')
const currentNoticeTitle = ref<string>('')

const openReadStats = (row: NoticeAdmin) => {
  currentNoticeId.value = row.id
  currentNoticeTitle.value = row.title
  readDialogVisible.value = true
}

const onSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    if (form.publishTime && dayjs(form.publishTime).isBefore(dayjs())) {
      showError('发布时间不能早于当前时间')
      return
    }
    if (form.expireTime && form.publishTime && dayjs(form.expireTime).isBefore(dayjs(form.publishTime))) {
      showError('过期时间需晚于发布时间')
      return
    }
    const payload: NoticeAdminSaveVo = {
      title: form.title,
      content: form.content,
      level: form.level,
      status: form.status,
      publishTime: form.publishTime || undefined,
      expireTime: form.expireTime || undefined,
      targetDeptIds: form.targetDeptIds,
    }
    if (form.id) {
      await updateNotice(form.id, payload)
      success('更新成功')
    } else {
      await createNotice(payload)
      success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  })
}

const onRecall = async (row: NoticeAdmin) => {
  await ElMessageBox.confirm(`确定撤回公告【${row.title}】吗？`, '提示', { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' })
  await recallNotice(row.id)
  success('撤回成功')
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
        <el-button type="primary" @click="openCreate">新增公告</el-button>
        <el-button @click="fetchList" :loading="loading">刷新</el-button>
      </PageHeader>

      <TableToolbar>
        <template #filters>
          <el-input v-model="query.keyword" placeholder="标题关键字" clearable style="width: 220px" @keyup.enter.native="onSearch" />
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px" @change="onSearch">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-select v-model="query.level" placeholder="级别" clearable style="width: 140px" @change="onSearch">
            <el-option v-for="opt in levelOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-date-picker
            v-model="query.startTime"
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

      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="level" label="级别" width="120">
          <template #default="{ row }">
            <el-tag :type="levelTagType[row.level]" size="small">
              {{ row.level === 'NORMAL' ? '普通' : row.level === 'IMPORTANT' ? '重要' : '紧急' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType[row.status]" size="small">
              {{ row.status === 'DRAFT' ? '草稿' : row.status === 'PUBLISHED' ? '已发布' : '已撤回' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.publishTime) }}</template>
        </el-table-column>
        <el-table-column label="过期时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.expireTime) }}</template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览量" width="100" />
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
            <el-button type="warning" link size="small" @click="onRecall(row)">撤回</el-button>
            <el-button type="info" link size="small" @click="openReadStats(row)">阅读统计</el-button>
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
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" />
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="form.level" style="width: 180px">
            <el-option v-for="opt in levelOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 180px">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布时间">
          <el-date-picker
            v-model="form.publishTime"
            type="datetime"
            placeholder="选填"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm"
            :disabled-date="(date) => dayjs(date).isBefore(dayjs().startOf('day'))"
          />
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="form.expireTime"
            type="datetime"
            placeholder="选填"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm"
            :disabled-date="(date) =>
              form.publishTime ? dayjs(date).isBefore(dayjs(form.publishTime).startOf('day')) : false"
          />
        </el-form-item>
        <el-form-item label="目标部门">
          <el-select v-model="form.targetDeptIds" multiple placeholder="可多选，空为全体">
            <el-option v-for="d in deptOptions" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <NoticeReadDialog v-model="readDialogVisible" :notice-id="currentNoticeId" :notice-title="currentNoticeTitle" />
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
</style>
