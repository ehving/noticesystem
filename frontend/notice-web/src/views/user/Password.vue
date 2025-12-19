<script setup lang="ts">
import { reactive, ref } from 'vue'
import { updatePassword } from '@/api/modules/auth'
import { success, error as showError } from '@/utils/message'

const formRef = ref()
const loading = ref(false)
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmNewPassword: '',
})

const rules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmNewPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_: unknown, value: string, callback: (error?: Error) => void) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

const onSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    if (form.newPassword !== form.confirmNewPassword) {
      showError('两次输入的密码不一致')
      return
    }
    loading.value = true
    try {
      await updatePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
      success('修改成功')
      form.oldPassword = ''
      form.newPassword = ''
      form.confirmNewPassword = ''
    } catch (e) {
      console.error(e)
    } finally {
      loading.value = false
    }
  })
}
</script>

<template>
  <div class="page">
    <el-card>
      <h3>修改密码</h3>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 520px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="form.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmNewPassword">
          <el-input v-model="form.confirmNewPassword" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="onSubmit">提交</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.page {
  padding: 8px;
  max-width: 640px;
}
</style>
