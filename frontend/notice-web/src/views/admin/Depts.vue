<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import { listDepts, getDeptTree, createDept, updateDept, deleteDept, updateDeptStatus,getDeptParentOptions } from '@/api/modules/admin/dept'
import type { DeptAdminVo } from '@/types/models/dept-admin'
import { useAdminStore } from '@/stores/admin'
import { formatDateTime } from '@/utils/time'
import PageHeader from '@/components/common/PageHeader.vue'
import TableToolbar from '@/components/common/TableToolbar.vue'
import { useLoading } from '@/hooks/useLoading'
import { success } from '@/utils/message'
import {
  Search, RefreshLeft, Plus, Edit, Delete, Operation, Menu,
  OfficeBuilding, CircleCheck, Clock, Sort, SwitchButton
} from '@element-plus/icons-vue'

type RuleItem = { required: boolean; message: string; trigger: 'blur' | 'change' }

const { loading, run } = useLoading()
const adminStore = useAdminStore()
const list = ref<DeptAdminVo[]>([])
const parentOptions = ref<{ label: string; value: string }[]>([])
const viewMode = ref<'tree' | 'list'>('tree')
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

const rules: Record<string, RuleItem[]> = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

const formatTime = (val?: string) => formatDateTime(val)

const fetchList = () =>
  run(async () => {
    // 2. 根据视图模式获取表格数据
    if (viewMode.value === 'tree') {
      // 树形模式：调用树形接口 (通常不支持复杂筛选，或者由后端处理)
      const treeRes = await getDeptTree()
      list.value = (treeRes as unknown as DeptAdminVo[]) || []
    } else {
      // 列表模式：调用扁平接口，支持筛选
      const flatRes = await listDepts({
        name: query.name || undefined,
        status: query.status === 'ALL' ? undefined : Number(query.status),
      })
      list.value = flatRes || []
    }
  })

const resetQuery = () => {
  query.name = ''
  query.status = 'ALL'
  fetchList()
}

const openCreate = async () => {
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

  // 获取父部门选项（排除环路）
  try {
    const res = await getDeptParentOptions(undefined)
    parentOptions.value = (res || []).map((item) => ({ label: item.name, value: item.id }))
  } catch (e) {
    console.error(e)
  }
}

const openEdit = async (row: DeptAdminVo) => {
  dialogTitle.value = '编辑部门'
  // 排除 children 属性，避免污染 form 和后续提交数据
  const { children, ...rest } = row as any
  Object.assign(form, rest)

  dialogVisible.value = true

  // 获取父部门选项（排除环路）
  try {
    const res = await getDeptParentOptions( { ChildDeptId: row.id as string } )
    parentOptions.value = (res || []).map((item) => ({ label: item.name, value: item.id }))
  } catch (e) {
    console.error(e)
  }
}

const onSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    if (form.id) {
      await updateDept(form.id, form)
      success('保存成功')
    } else {
      await createDept(form)
      success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  })
}

const onDelete = async (row: DeptAdminVo) => {
  await ElMessageBox.confirm(`确定删除部门【${row.name}】吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '确认',
    cancelButtonText: '取消',
  })
  await deleteDept(row.id as string)
  success('删除成功')
  fetchList()
}

const toggleStatus = async (row: DeptAdminVo) => {
  const targetStatus = row.status === 1 ? 0 : 1
  await updateDeptStatus(row.id as string, targetStatus)
  success('状态已更新')
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
        <el-radio-group v-model="viewMode" size="small" @change="fetchList" class="view-switch">
          <el-radio-button label="tree"><el-icon class="icon-mr"><Operation /></el-icon>树形</el-radio-button>
          <el-radio-button label="list"><el-icon class="icon-mr"><Menu /></el-icon>列表</el-radio-button>
        </el-radio-group>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增部门</el-button>
        <el-button :icon="RefreshLeft" @click="fetchList" :loading="loading">刷新</el-button>
      </PageHeader>

      <!-- 仅在列表模式下显示筛选栏，因为树形接口通常返回全量结构 -->
      <TableToolbar v-if="viewMode === 'list'">
        <template #filters>
          <el-input v-model="query.name" placeholder="搜索部门名称" clearable style="width: 200px" :prefix-icon="Search" @keyup.enter.native="fetchList" />
          <el-select v-model="query.status" placeholder="状态" style="width: 140px" @change="fetchList">
            <template #prefix><el-icon><CircleCheck /></el-icon></template>
            <el-option label="全部" value="ALL" />
            <el-option label="启用" value="1" />
            <el-option label="停用" value="0" />
          </el-select>
        </template>
        <template #actions>
          <el-button type="primary" :icon="Search" @click="fetchList">查询</el-button>
          <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
        </template>
      </TableToolbar>

      <el-table
        :data="list"
        v-loading="loading"
        style="width: 100%"
        row-key="id"
        border
        default-expand-all
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      >
        <el-table-column prop="name" label="部门名称" min-width="200">
          <template #default="{ row }">
            <el-icon class="icon-mr"><OfficeBuilding /></el-icon>
            {{ row.name }}
          </template>
        </el-table-column>
        <el-table-column prop="parentName" label="上级部门" min-width="140" />
        <el-table-column prop="sortOrder" label="排序" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="info" effect="plain" size="small"><el-icon class="icon-mr"><Sort /></el-icon>{{ row.sortOrder }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="light" round>
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">
            <div class="time-cell">
              <el-icon><Clock /></el-icon>
              <span>{{ formatTime(row.createTime) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-tooltip content="编辑" placement="top">
              <el-button type="primary" link :icon="Edit" @click="openEdit(row)" />
            </el-tooltip>
            <el-tooltip :content="row.status === 1 ? '停用' : '启用'" placement="top">
              <el-button :type="row.status === 1 ? 'warning' : 'success'" link :icon="SwitchButton" @click="toggleStatus(row)" />
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button type="danger" link :icon="Delete" @click="onDelete(row)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="form.name" :prefix-icon="OfficeBuilding" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="上级部门">
          <el-select v-model="form.parentId" clearable placeholder="请选择上级部门" style="width: 100%">
            <el-option v-for="opt in parentOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="部门描述信息" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
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

.view-switch {
  margin-right: 12px;
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
</style>
