<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useAdminStore } from '@/stores/admin'
import { dailyReport } from '@/api/modules/admin/sync-logs'
import type { SyncLogDailyReportVo } from '@/types/models/sync-log'
import type { EChartsOption } from 'echarts'
import { DB_OPTIONS } from '@/types/enums/db'
import BaseChart from '@/components/charts/BaseChart.vue'
import { useLoading } from '@/hooks/useLoading'
import { formatDateTime } from '@/utils/time'

const adminStore = useAdminStore()
const { loading, run } = useLoading()

const filter = reactive({
  beginTime: '',
  endTime: '',
  sourceDb: '',
  targetDb: '',
})

const chartOption = ref<EChartsOption>({})
const tableData = ref<SyncLogDailyReportVo[]>([])

const buildOption = (data: SyncLogDailyReportVo[]) => {
  const x = data.map((i) => i.statDate)
  const success = data.map((i) => i.successCount)
  const failed = data.map((i) => i.failedCount)
  const rate = data.map((i) => Number((i.failedRate ?? 0) * 100).toFixed(2))

  chartOption.value = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['成功', '失败', '失败率'] },
    xAxis: { type: 'category', data: x },
    yAxis: [
      { type: 'value', name: '次数' },
      { type: 'value', name: '失败率(%)' },
    ],
    series: [
      { name: '成功', type: 'bar', data: success },
      { name: '失败', type: 'bar', data: failed },
      { name: '失败率', type: 'line', yAxisIndex: 1, data: rate },
    ],
  }
}

const fetchData = async () => {
  await run(async () => {
    const res = await dailyReport({
      pageNo: 1,
      pageSize: 100,
      beginTime: filter.beginTime || undefined,
      endTime: filter.endTime || undefined,
      sourceDb: filter.sourceDb || undefined,
      targetDb: filter.targetDb || undefined,
    })
    tableData.value = res
    buildOption(res)
  })
}

watch(
  () => adminStore.changeTick,
  () => fetchData()
)

fetchData()
</script>

<template>
  <div class="page">
    <el-card>
      <div class="filters">
        <el-date-picker v-model="filter.beginTime" type="date" placeholder="开始日期" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
        <el-date-picker v-model="filter.endTime" type="date" placeholder="结束日期" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
        <el-select v-model="filter.sourceDb" clearable placeholder="源库" style="width: 140px">
          <el-option v-for="db in DB_OPTIONS" :key="db.value" :label="db.label" :value="db.value" />
        </el-select>
        <el-select v-model="filter.targetDb" clearable placeholder="目标库" style="width: 140px">
          <el-option v-for="db in DB_OPTIONS" :key="db.value" :label="db.label" :value="db.value" />
        </el-select>
        <el-button type="primary" @click="fetchData" :loading="loading">查询</el-button>
      </div>
      <BaseChart :option="chartOption" height="320px" />
    </el-card>

    <el-card>
      <div class="title">日报明细（当前库：{{ adminStore.activeDb }}）</div>
      <el-table :data="tableData" v-loading="loading" style="width: 100%" size="small">
        <el-table-column prop="statDate" label="日期" width="120" />
        <el-table-column prop="sourceDb" label="源库" width="100" />
        <el-table-column prop="targetDb" label="目标库" width="100" />
        <el-table-column prop="totalCount" label="总数" width="80" />
        <el-table-column prop="successCount" label="成功" width="80" />
        <el-table-column prop="failedCount" label="失败" width="80" />
        <el-table-column label="失败率" width="100">
          <template #default="{ row }">{{ row.failedRate != null ? (row.failedRate * 100).toFixed(2) + '%' : '-' }}</template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.statDate) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && tableData.length === 0" description="暂无数据" />
    </el-card>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}
.title {
  margin-bottom: 8px;
  font-weight: 600;
}
</style>
