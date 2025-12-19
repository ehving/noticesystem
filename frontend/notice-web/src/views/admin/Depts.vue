<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import { listDepts, createDept, updateDept, deleteDept, updateDeptStatus } from '@/api/modules/admin/dept'
import type { DeptAdminVo } from '@/types/models/dept-admin'
import { useAdminStore } from '@/stores/admin'
import { formatDateTime } from '@/utils/time'
import PageHeader from '@/components/common/PageHeader.vue'
import TableToolbar from '@/components/common/TableToolbar.vue'
import { useLoading } from '@/hooks/useLoading'

const { loading, run } = useLoading()
const adminStore = useAdminStore()
const list = ref<DeptAdminVo[]>([])
const query = reactive({
  name: '',
  status: 'ALL' as 'ALL' | '1' | '0',
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增部门')
const formRef = ref()
const form = reactive<DeptAdminVo>({
  id: '',
  name: '',
  parentId: undefined,
  description: '',
  sortOrder: 0,
  status: 1,
})

const rules = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
}

const parentOptions = computed(() => {
  return list.value.map((item) => ({
    label: item.name,
    value: item.id as string,
  }))
})

const formatTime = (val?: string) => formatDateTime(val)

const fetchList = () =>
  run(async () => {
    list.value = await listDepts({
      name: query.name || undefined,
      status: query.status === 'ALL' ? undefined : Number(query.status),
    })
  })

const resetQuery = () => {
  query.name = ''
  query.status = 'ALL'
  fetchList()
}

const openCreate = () => {
  dialogTitle.value = '新增部门'
  Object.assign(form, {
    id: '',
    name: '',
    parentId: undefined,
    description: '',
    sortOrder: 0,
    status: 1,
  })
  dialogVisible.value = true
}

const openEdit = (row: DeptAdminVo) => {
  dialogTitle.value = '编辑部门'
  Object.assign(form, row)
  dialogVisible.value = true
}

const onSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    if (form.id) {
      await updateDept(form.id, form)
    } else {
      await createDept(form)
    }
    dialogVisible.value = false
    fetchList()
  })
}

const onDelete = async (row: DeptAdminVo) => {
  await ElMessageBox.confirm(`确定删除部门【${row.name}】吗？`, '提示', { type: 'warning' })
  await deleteDept(row.id as string)
  fetchList()
}

const toggleStatus = async (row: DeptAdminVo) => {
  const targetStatus = row.status === 1 ? 0 : 1
  await updateDeptStatus(row.id as string, targetStatus)
  fetchList()
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
      <PageHeader title="部门管理" :sub-title="`当前库：${adminStore.activeDb}`">
        <el-button type="primary" @click="openCreate">新增部门</el-button>
        <el-button @click="fetchList" :loading="loading">刷新</el-button>
      </PageHeader>
      <TableToolbar>
        <template #filters>
          <el-input v-model="query.name" placeholder="部门名称" clearable style="width: 200px" @keyup.enter.native="fetchList" />
          <el-select v-model="query.status" placeholder="状态" style="width: 140px" @change="fetchList">
            <el-option label="全部" value="ALL" />
            <el-option label="启用" value="1" />
            <el-option label="停用" value="0" />
          </el-select>
        </template>
        <template #actions>
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </template>
      </TableToolbar>

      <el-table :data="list" v-loading="loading" style="width: 100%">
        <el-table-column prop="name" label="部门名称" min-width="160" />
        <el-table-column prop="parentName" label="上级部门" min-width="140" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="onDelete(row)">删除</el-button>
            <el-button type="info" link size="small" @click="toggleStatus(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无数据" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="上级部门">
          <el-select v-model="form.parentId" clearable placeholder="请选择上级部门">
            <el-option v-for="opt in parentOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
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

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.filters {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
