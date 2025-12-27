<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { listRoles } from '@/api/modules/admin/roles'
import type { Role } from '@/types/models/role'
import { useAdminStore } from '@/stores/admin'
import { formatDateTime } from '@/utils/time'
import PageHeader from '@/components/common/PageHeader.vue'
import { useLoading } from '@/hooks/useLoading'
import { RefreshLeft, Stamp, Clock, Operation } from '@element-plus/icons-vue'

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
  <div class="page">
    <el-card>
      <PageHeader title="角色列表" :sub-title="`当前库：${adminStore.activeDb}`">
        <el-button :icon="RefreshLeft" @click="fetchList" :loading="loading">刷新</el-button>
      </PageHeader>

      <el-table :data="data" v-loading="loading" style="width: 100%" border>
        <el-table-column prop="name" label="角色名称" min-width="160">
          <template #default="{ row }">
            <el-tag effect="plain" round>
              <el-icon style="margin-right: 4px; vertical-align: middle"><Stamp /></el-icon>
              {{ row.name }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="200">
          <template #default="{ row }">
            <div class="time-cell">
              <el-icon><Clock /></el-icon>
              <span>{{ formatTime(row.createTime) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="200">
          <template #default="{ row }">
            <div class="time-cell">
              <el-icon><Clock /></el-icon>
              <span>{{ formatTime(row.updateTime) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default>
            <el-tooltip content="权限管理 (暂未开放)" placement="top">
              <el-button type="primary" link :icon="Operation" disabled />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.time-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
  font-size: 13px;
}
</style>
