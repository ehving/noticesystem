<script setup lang="ts">
import { useAdminStore } from '@/stores/admin'
import { DB_OPTIONS, type DatabaseType } from '@/types/enums/db'

const adminStore = useAdminStore()

const handleChange = (val: DatabaseType) => {
  adminStore.setDb(val)
}
</script>

<template>
  <el-select v-model="adminStore.activeDb" size="small" style="width: 140px" @change="handleChange">
    <el-option v-for="opt in DB_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
  </el-select>
</template>
