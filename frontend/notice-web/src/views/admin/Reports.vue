<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useAdminStore } from '@/stores/admin'
import { dailyReport } from '@/api/modules/admin/sync-logs'
import { pageSyncLogs } from '@/api/modules/admin/sync-logs'
import type { SyncLogDailyReportVo, SyncLog } from '@/types/models/sync-log'
import type { EChartsOption } from 'echarts'
import BaseChart from '@/components/charts/BaseChart.vue'
import { DB_OPTIONS } from '@/types/enums/db'
import { useLoading } from '@/hooks/useLoading'
import { aggregateCount } from '@/utils/aggregate'
import { formatDateTime } from '@/utils/time'

const adminStore = useAdminStore()
const activeTab = ref('daily')

const dailyFilter = reactive({
  beginTime: '',
  endTime: '',
  sourceDb: '',
  targetDb: '',
})
const { loading: dailyLoading, run: runDaily } = useLoading()
const dailyData = ref<SyncLogDailyReportVo[]>([])
const dailyChartOption = ref<EChartsOption>({})

const fetchDaily = async () => {
  await runDaily(async () => {
    const res = await dailyReport({
      pageNo: 1,
      pageSize: 200,
      beginTime: dailyFilter.beginTime || undefined,
      endTime: dailyFilter.endTime || undefined,
      sourceDb: dailyFilter.sourceDb || undefined,
      targetDb: dailyFilter.targetDb || undefined,
    })
    dailyData.value = res
    const x = res.map((i) => i.statDate)
    const success = res.map((i) => i.successCount)
    const failed = res.map((i) => i.failedCount)
    const rate = res.map((i) => Number((i.failedRate ?? 0) * 100).toFixed(2))
    dailyChartOption.value = {
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
  })
}

const exceptionFilter = reactive({
  beginTime: '',
  endTime: '',
  sourceDb: '',
  targetDb: '',
})
const { loading: exLoading, run: runEx } = useLoading()
const exLogs = ref<SyncLog[]>([])
const exChartOption = ref<EChartsOption>({})

const fetchExceptions = async () => {
  await runEx(async () => {
    const res = await pageSyncLogs({
      pageNo: 1,
      pageSize: 200,
      status: 'FAILED',
      beginTime: exceptionFilter.beginTime || undefined,
      endTime: exceptionFilter.endTime || undefined,
      sourceDb: exceptionFilter.sourceDb || undefined,
      targetDb: exceptionFilter.targetDb || undefined,
    })
    exLogs.value = res.records || []
    const agg = aggregateCount(exLogs.value, (i) => i.entityType || '未知')
    exChartOption.value = {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: agg.map((i) => i.name) },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: agg.map((i) => i.count), name: '失败数' }],
    }
  })
}

watch(
  () => adminStore.changeTick,
  () => {
    if (activeTab.value === 'daily') fetchDaily()
    else fetchExceptions()
  }
)

fetchDaily()
fetchExceptions()
</script>

<template>
  <div class="page">
    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="同步日报" name="daily">
          <div class="filters">
            <el-date-picker v-model="dailyFilter.beginTime" type="date" placeholder="开始日期" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
            <el-date-picker v-model="dailyFilter.endTime" type="date" placeholder="结束日期" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
            <el-select v-model="dailyFilter.sourceDb" clearable placeholder="源库" style="width: 140px">
              <el-option v-for="db in DB_OPTIONS" :key="db.value" :label="db.label" :value="db.value" />
            </el-select>
            <el-select v-model="dailyFilter.targetDb" clearable placeholder="目标库" style="width: 140px">
              <el-option v-for="db in DB_OPTIONS" :key="db.value" :label="db.label" :value="db.value" />
            </el-select>
            <el-button type="primary" @click="fetchDaily" :loading="dailyLoading">查询</el-button>
          </div>
          <BaseChart :option="dailyChartOption" height="360px" />
          <el-table :data="dailyData" v-loading="dailyLoading" style="width: 100%" size="small">
            <el-table-column prop="statDate" label="日期" width="120" />
            <el-table-column prop="sourceDb" label="源库" width="100" />
            <el-table-column prop="targetDb" label="目标库" width="100" />
            <el-table-column prop="totalCount" label="总数" width="80" />
            <el-table-column prop="successCount" label="成功" width="80" />
            <el-table-column prop="failedCount" label="失败" width="80" />
            <el-table-column label="失败率" width="100">
              <template #default="{ row }">{{ row.failedRate != null ? (row.failedRate * 100).toFixed(2) + '%' : '-' }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!dailyLoading && dailyData.length === 0" description="暂无数据" />
        </el-tab-pane>

        <el-tab-pane label="异常概览" name="exceptions">
          <div class="filters">
            <el-date-picker
              v-model="exceptionFilter.beginTime"
              type="datetime"
              placeholder="开始时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm"
            />
            <el-date-picker
              v-model="exceptionFilter.endTime"
              type="datetime"
              placeholder="结束时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm"
            />
            <el-select v-model="exceptionFilter.sourceDb" clearable placeholder="源库" style="width: 140px">
              <el-option v-for="db in DB_OPTIONS" :key="db.value" :label="db.label" :value="db.value" />
            </el-select>
            <el-select v-model="exceptionFilter.targetDb" clearable placeholder="目标库" style="width: 140px">
              <el-option v-for="db in DB_OPTIONS" :key="db.value" :label="db.label" :value="db.value" />
            </el-select>
            <el-button type="primary" @click="fetchExceptions" :loading="exLoading">查询</el-button>
          </div>
          <BaseChart :option="exChartOption" height="320px" />
          <div class="tip">仅统计当前筛选内最近 200 条失败记录。</div>
          <el-empty v-if="!exLoading && exLogs.length === 0" description="暂无失败记录" />
          <el-scrollbar height="360px" v-else>
            <div v-for="item in exLogs" :key="item.id" class="log-card">
              <div class="row"><span class="label">实体</span><span>{{ item.entityType }} #{{ item.entityId }}</span></div>
              <div class="row"><span class="label">操作</span><span>{{ item.action }}</span></div>
              <div class="row"><span class="label">来源/目标</span><span>{{ item.sourceDb }} -> {{ item.targetDb }}</span></div>
              <div class="row"><span class="label">错误</span><span class="error">{{ item.errorMsg || '-' }}</span></div>
              <div class="row"><span class="label">时间</span><span>{{ formatDateTime(item.createTime) }}</span></div>
            </div>
          </el-scrollbar>
        </el-tab-pane>
      </el-tabs>
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
.tip {
  color: #909399;
  font-size: 12px;
  margin: 6px 0;
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
  font-size: 13px;
  margin-bottom: 6px;
}
.label {
  color: #909399;
}
.error {
  color: #f56c6c;
}
</style>
