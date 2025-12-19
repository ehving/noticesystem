<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { listRoles } from '@/api/modules/admin/roles'
import type { Role } from '@/types/models/role'
import { useAdminStore } from '@/stores/admin'
import { formatDateTime } from '@/utils/time'
import PageHeader from '@/components/common/PageHeader.vue'
import TableToolbar from '@/components/common/TableToolbar.vue'
import { useLoading } from '@/hooks/useLoading'

const { loading, run } = useLoading()
const data = ref<Role[]>([])
const adminStore = useAdminStore()

const fetchList = () =>
  run(async () => {
    data.value = await listRoles()
  })

const formatTime = (val?: string) => formatDateTime(val)

onMounted(fetchList)

watch(
  () => adminStore.changeTick,
  () => fetchList()
)
</script>

<template>
  <el-card>
    <PageHeader title="角色列表" :sub-title="`当前库：${adminStore.activeDb}`">
      <el-button type="primary" size="small" @click="fetchList" :loading="loading">刷新</el-button>
    </PageHeader>
    <el-table :data="data" v-loading="loading" style="width: 100%">
      <el-table-column prop="name" label="角色名称" min-width="160" />
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="更新时间" width="180">
        <template #default="{ row }">{{ formatTime(row.updateTime) }}</template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && data.length === 0" description="暂无数据" />
  </el-card>
</template>
