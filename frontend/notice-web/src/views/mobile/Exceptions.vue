<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useAdminStore } from '@/stores/admin'
import { pageSyncLogs } from '@/api/modules/admin/sync-logs'
import type { SyncLog } from '@/types/models/sync-log'
import type { EChartsOption } from 'echarts'
import BaseChart from '@/components/charts/BaseChart.vue'
import { DB_OPTIONS } from '@/types/enums/db'
import { useLoading } from '@/hooks/useLoading'
import { aggregateCount } from '@/utils/aggregate'
import { formatDateTime } from '@/utils/time'

const adminStore = useAdminStore()
const { loading, run } = useLoading()

const filter = reactive({
  beginTime: '',
  endTime: '',
  sourceDb: '',
  targetDb: '',
})

const logs = ref<SyncLog[]>([])
const chartOption = ref<EChartsOption>({})

const fetchData = async () => {
  await run(async () => {
    const res = await pageSyncLogs({
      pageNo: 1,
      pageSize: 200, // 仅统计最近 200 条失败
      status: 'FAILED',
      beginTime: filter.beginTime || undefined,
      endTime: filter.endTime || undefined,
      sourceDb: filter.sourceDb || undefined,
      targetDb: filter.targetDb || undefined,
    })
    logs.value = res.records || []
    buildChart()
  })
}

const buildChart = () => {
  const agg = aggregateCount(logs.value, (i) => i.entityType || '未知')
  const x = agg.map((i) => i.name)
  const counts = agg.map((i) => i.count)
  chartOption.value = {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: x },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: counts, name: '失败数' }],
  }
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
        <el-date-picker
          v-model="filter.beginTime"
          type="datetime"
          placeholder="开始时间"
          format="YYYY-MM-DD HH:mm"
          value-format="YYYY-MM-DD HH:mm"
        />
        <el-date-picker
          v-model="filter.endTime"
          type="datetime"
          placeholder="结束时间"
          format="YYYY-MM-DD HH:mm"
          value-format="YYYY-MM-DD HH:mm"
        />
        <el-select v-model="filter.sourceDb" clearable placeholder="源库" style="width: 140px">
          <el-option v-for="db in DB_OPTIONS" :key="db.value" :label="db.label" :value="db.value" />
        </el-select>
        <el-select v-model="filter.targetDb" clearable placeholder="目标库" style="width: 140px">
          <el-option v-for="db in DB_OPTIONS" :key="db.value" :label="db.label" :value="db.value" />
        </el-select>
        <el-button type="primary" @click="fetchData" :loading="loading">查询</el-button>
      </div>
      <BaseChart :option="chartOption" height="300px" />
      <div class="tip">提示：仅统计当前筛选范围内最近 200 条失败日志。</div>
    </el-card>

    <el-card>
      <div class="title">失败明细（最近 200 条）</div>
      <div v-if="!loading && logs.length === 0">
        <el-empty description="暂无失败记录" />
      </div>
      <el-scrollbar height="360px" v-else>
        <div v-for="item in logs" :key="item.id" class="log-card">
          <div class="row">
            <span class="label">实体</span>
            <span>{{ item.entityType }} #{{ item.entityId }}</span>
          </div>
          <div class="row">
            <span class="label">操作</span>
            <span>{{ item.action }}</span>
          </div>
          <div class="row">
            <span class="label">来源/目标</span>
            <span>{{ item.sourceDb }} -> {{ item.targetDb }}</span>
          </div>
          <div class="row">
            <span class="label">错误</span>
            <span class="error">{{ item.errorMsg || '-' }}</span>
          </div>
          <div class="row">
            <span class="label">时间</span>
            <span>{{ formatDateTime(item.createTime) }}</span>
          </div>
        </div>
      </el-scrollbar>
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
  align-items: center;
  margin-bottom: 8px;
}
.title {
  font-weight: 600;
  margin-bottom: 8px;
}
.log-card {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 8px;
}
.row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 13px;
}
.label {
  color: #909399;
}
.error {
  color: #f56c6c;
}
.tip {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
