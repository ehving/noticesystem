<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import { pageUsers, createUser, updateUser, deleteUser, resetPassword } from '@/api/modules/admin/users'
import { listRoles } from '@/api/modules/admin/roles'
import { listDeptOptions } from '@/api/modules/dept'
import type { UserAdminListVo, UserAdminPageVo } from '@/types/models/user-admin'
import type { Role } from '@/types/models/role'
import type { DeptOptionVo } from '@/types/models/dept'
import type { MpPage } from '@/types/page'
import { useAdminStore } from '@/stores/admin'
import { formatDateTime } from '@/utils/time'
import PageHeader from '@/components/common/PageHeader.vue'
import TableToolbar from '@/components/common/TableToolbar.vue'
import { useLoading } from '@/hooks/useLoading'

const adminStore = useAdminStore()
const { loading, run } = useLoading()
const tableData = ref<UserAdminListVo[]>([])
const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0,
})
const query = reactive<UserAdminPageVo>({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  roleId: '',
  deptId: '',
  status: undefined,
})

const roleOptions = ref<Role[]>([])
const deptOptions = ref<DeptOptionVo[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const formRef = ref()
const form = reactive({
  id: '',
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  avatar: '',
  roleId: '',
  deptId: '',
  status: 1,
})
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const resetDialog = () => {
  dialogTitle.value = '新增用户'
  Object.assign(form, {
    id: '',
    username: '',
    password: '',
    nickname: '',
    email: '',
    phone: '',
    avatar: '',
    roleId: '',
    deptId: '',
    status: 1,
  })
}

const fetchRolesAndDepts = async () => {
  roleOptions.value = await listRoles()
  deptOptions.value = await listDeptOptions()
}

const fetchList = () =>
  run(async () => {
    const res: MpPage<UserAdminListVo> = await pageUsers({
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
      keyword: query.keyword || undefined,
      roleId: query.roleId || undefined,
      deptId: query.deptId || undefined,
      status: query.status,
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
  query.roleId = ''
  query.deptId = ''
  query.status = undefined
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
  resetDialog()
  dialogVisible.value = true
}

const openEdit = (row: UserAdminListVo) => {
  dialogTitle.value = '编辑用户'
  Object.assign(form, {
    id: row.id,
    username: row.username,
    password: '',
    nickname: row.nickname,
    email: row.email,
    phone: row.phone,
    avatar: row.avatar,
    roleId: row.roleId,
    deptId: row.deptId,
    status: row.status ?? 1,
  })
  dialogVisible.value = true
}

const onSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    if (form.id) {
      await updateUser(form.id, {
        nickname: form.nickname || undefined,
        email: form.email || undefined,
        phone: form.phone || undefined,
        avatar: form.avatar || undefined,
        roleId: form.roleId || undefined,
        deptId: form.deptId || undefined,
        status: form.status,
      })
    } else {
      await createUser({
        username: form.username,
        password: form.password,
        nickname: form.nickname || undefined,
        email: form.email || undefined,
        phone: form.phone || undefined,
        avatar: form.avatar || undefined,
        roleId: form.roleId || undefined,
        deptId: form.deptId || undefined,
        status: form.status,
      })
    }
    dialogVisible.value = false
    fetchList()
  })
}

const onDelete = async (row: UserAdminListVo) => {
  await ElMessageBox.confirm(`确定删除用户【${row.username}】吗？`, '提示', { type: 'warning' })
  await deleteUser(row.id)
  fetchList()
}

const resetPwdDialogVisible = ref(false)
const resetPwdRef = ref()
const resetPwdForm = reactive({
  id: '',
  newPassword: '',
})
const resetPwdRules = {
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
}

const openResetPwd = (row: UserAdminListVo) => {
  resetPwdForm.id = row.id
  resetPwdForm.newPassword = ''
  resetPwdDialogVisible.value = true
}

const onResetPwd = async () => {
  if (!resetPwdRef.value) return
  await resetPwdRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    await resetPassword(resetPwdForm.id, { newPassword: resetPwdForm.newPassword })
    resetPwdDialogVisible.value = false
  })
}

const formatTime = (val?: string) => formatDateTime(val)

onMounted(async () => {
  await fetchRolesAndDepts()
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
      <PageHeader title="用户管理" :sub-title="`当前库：${adminStore.activeDb}`">
        <el-button type="primary" @click="openCreate">新增用户</el-button>
        <el-button @click="fetchList" :loading="loading">刷新</el-button>
      </PageHeader>

      <TableToolbar>
        <template #filters>
          <el-input
            v-model="query.keyword"
            placeholder="用户名/昵称/邮箱"
            clearable
            style="width: 220px"
            @keyup.enter.native="onSearch"
          />
          <el-select v-model="query.roleId" placeholder="角色" clearable style="width: 160px" @change="onSearch">
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
          <el-select v-model="query.deptId" placeholder="部门" clearable style="width: 180px" @change="onSearch">
            <el-option v-for="d in deptOptions" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px" @change="onSearch">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </template>
        <template #actions>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </template>
      </TableToolbar>

      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="roleName" label="角色" min-width="120" />
        <el-table-column prop="deptName" label="部门" min-width="140" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
            <el-button type="warning" link size="small" @click="openResetPwd(row)">重置密码</el-button>
            <el-button type="danger" link size="small" @click="onDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="头像">
          <el-input v-model="form.avatar" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleId" clearable placeholder="请选择角色">
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="form.deptId" clearable placeholder="请选择部门">
            <el-option v-for="d in deptOptions" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetPwdDialogVisible" title="重置密码" width="420px" destroy-on-close>
      <el-form ref="resetPwdRef" :model="resetPwdForm" :rules="resetPwdRules" label-width="110px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetPwdForm.newPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onResetPwd">重置</el-button>
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
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.filters {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
