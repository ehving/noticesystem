<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { useAuthStore } from '@/stores/auth'
import { updateProfile, updatePassword } from '@/api/modules/auth'
import { success, error as showError } from '@/utils/message'
import { formatDateTime } from '@/utils/time'

const authStore = useAuthStore()
const profile = computed(() => authStore.profile)

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({
  nickname: '',
  email: '',
  phone: '',
  avatar: '',
})
const rules = {
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

const openEdit = () => {
  if (!profile.value) return
  form.nickname = profile.value.nickname || ''
  form.email = profile.value.email || ''
  form.phone = profile.value.phone || ''
  form.avatar = profile.value.avatar || ''
  dialogVisible.value = true
}

const onSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    await updateProfile({
      nickname: form.nickname || undefined,
      email: form.email || undefined,
      phone: form.phone || undefined,
      avatar: form.avatar || undefined,
    })
    success('更新成功')
    await authStore.fetchProfile()
    dialogVisible.value = false
  })
}

const pwdDialogVisible = ref(false)
const pwdFormRef = ref()
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmNewPassword: '',
})
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmNewPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_: unknown, value: string, callback: (error?: Error) => void) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

const openPwd = () => {
  pwdDialogVisible.value = true
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmNewPassword = ''
}

const onPwdSave = async () => {
  if (!pwdFormRef.value) return
  await pwdFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    if (pwdForm.newPassword !== pwdForm.confirmNewPassword) {
      showError('两次输入的密码不一致')
      return
    }
    await updatePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    success('修改成功')
    pwdDialogVisible.value = false
  })
}

const formatTime = (val?: string) => formatDateTime(val)
</script>

<template>
  <div class="page">
    <el-card>
      <div class="header">
        <h3>个人资料</h3>
        <div class="header-actions">
          <el-button type="primary" size="small" @click="openEdit" :disabled="!profile">编辑资料</el-button>
          <el-button size="small" @click="openPwd" :disabled="!profile">修改密码</el-button>
        </div>
      </div>
      <div v-if="profile">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户名">{{ profile.username }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ profile.nickname }}</el-descriptions-item>
          <el-descriptions-item label="角色">{{ profile.roleName }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ profile.deptName }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ profile.email }}</el-descriptions-item>
          <el-descriptions-item label="手机">{{ profile.phone }}</el-descriptions-item>
          <el-descriptions-item label="最后登录">{{ formatTime(profile.lastLoginTime) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(profile.createTime) }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <div v-else>加载中或未登录</div>
    </el-card>

    <el-dialog v-model="pwdDialogVisible" title="修改密码" width="420px" destroy-on-close>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="110px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmNewPassword">
          <el-input v-model="pwdForm.confirmNewPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onPwdSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogVisible" title="编辑资料" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="头像链接">
          <el-input v-model="form.avatar" placeholder="请输入头像链接" />
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
  padding: 8px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
