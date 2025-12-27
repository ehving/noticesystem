<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import dayjs from 'dayjs'
import type { EChartsCoreOption } from 'echarts/core'
import { Monitor, DataLine, PieChart, Calendar, Filter, RefreshLeft, Search } from '@element-plus/icons-vue'
import BaseChart from '@/components/charts/BaseChart.vue'
import { useAdminStore } from '@/stores/admin'
import { useLoading } from '@/hooks/useLoading'

import { aggLogs } from '@/api/modules/admin/sync-logs'
import { aggConflicts } from '@/api/modules/admin/conflicts'

import type { AggVo } from '@/types/models/agg'
import { SyncLogAggBy, SyncConflictAggBy } from '@/types/enums/agg-by'
import { SyncLogStatus, ConflictStatus } from '@/types/enums/sync'
import { toLocalDateTimeParam } from '@/utils/time'

// ======================= State =======================
const adminStore = useAdminStore()
const { loading, run } = useLoading()

const activeTab = ref<'logs' | 'conflicts'>('logs')

// 时间筛选模式：近七天 / 全局
const timeMode = ref<'last7' | 'global'>('last7')

// 近七天：柱形图堆叠/分组切换（左右共用同一个）
const last7BarMode = ref<'stack' | 'group'>('stack')

// 近七天：选择维度（多选，左右共用；去掉原来的右侧单选）
const last7ByListLogs = ref<SyncLogAggBy[]>([SyncLogAggBy.STATUS])
const last7ByListConflicts = ref<SyncConflictAggBy[]>([SyncConflictAggBy.STATUS])

// 近七天：右侧“当天”选择
const selectedDay = ref<string>('') // YYYY-MM-DD

// 全局：多选维度（每个维度一行：柱+饼）
const globalByListLogs = ref<SyncLogAggBy[]>([SyncLogAggBy.STATUS])
const globalByListConflicts = ref<SyncConflictAggBy[]>([SyncConflictAggBy.STATUS])

// filters
const globalFilter = reactive({
  beginTime: '',
  endTime: '',
})

// ======================= Options lists =======================
const logByOptions = [
  { label: '按状态', value: SyncLogAggBy.STATUS },
  { label: '按实体类型', value: SyncLogAggBy.ENTITY_TYPE },
  { label: '按动作', value: SyncLogAggBy.ACTION },
  { label: '按源库', value: SyncLogAggBy.SOURCE_DB },
  { label: '按目标库', value: SyncLogAggBy.TARGET_DB },
] as const

const conflictByOptions = [
  { label: '按状态', value: SyncConflictAggBy.STATUS },
  { label: '按冲突类型', value: SyncConflictAggBy.CONFLICT_TYPE },
  { label: '按实体类型', value: SyncConflictAggBy.ENTITY_TYPE },
] as const

const currentByOptions = computed(() => (activeTab.value === 'logs' ? logByOptions : conflictByOptions))
const tabLabel = computed(() => (activeTab.value === 'logs' ? '同步日志' : '冲突处理'))

// v-model proxies
const last7ByList = computed({
  get() {
    return activeTab.value === 'logs'
      ? (last7ByListLogs.value as unknown as string[])
      : (last7ByListConflicts.value as unknown as string[])
  },
  set(v: any) {
    if (activeTab.value === 'logs') last7ByListLogs.value = (v as any) as SyncLogAggBy[]
    else last7ByListConflicts.value = (v as any) as SyncConflictAggBy[]
  },
})

const globalByList = computed({
  get() {
    return activeTab.value === 'logs'
      ? (globalByListLogs.value as unknown as string[])
      : (globalByListConflicts.value as unknown as string[])
  },
  set(v: any) {
    if (activeTab.value === 'logs') globalByListLogs.value = (v as any) as SyncLogAggBy[]
    else globalByListConflicts.value = (v as any) as SyncConflictAggBy[]
  },
})

// ======================= Data containers =======================
// 近七天：左侧趋势图（每个 by 一张堆叠/分组柱）
const last7Blocks = ref<{ by: string; option: EChartsCoreOption }[]>([])

// 近七天：右侧 “当天” —— 每个 by 一张饼（同一组选项）
const last7PieBlocks = ref<{ by: string; option: EChartsCoreOption }[]>([])
const last7Days = ref<string[]>([])

// 全局：每个 by 一行（柱+饼）
const globalBlocks = ref<{ by: string; bar: EChartsCoreOption; pie: EChartsCoreOption }[]>([])

// ======================= Color =======================
const palette = [
  '#67C23A', // green
  '#409EFF', // blue
  '#E6A23C', // orange
  '#F56C6C', // red
  '#909399', // gray
  '#9B59B6', // purple
  '#16A085', // teal
  '#E84393', // pink
  '#2D3436', // dark
  '#00B894', // green2
]

const logColorMap: Record<string, string> = {
  [SyncLogStatus.SUCCESS]: '#67C23A',
  [SyncLogStatus.CONFLICT]: '#E6A23C',
  [SyncLogStatus.FAILED]: '#F56C6C',
  [SyncLogStatus.ERROR]: '#E84393', // 错误粉色
}

const conflictStatusColorMap: Record<string, string> = {
  [ConflictStatus.RESOLVED]: '#67C23A', // 解决绿色
  [ConflictStatus.OPEN]: '#E6A23C',
  [ConflictStatus.IGNORED]: '#909399',
}

const colorForKey = (k: string, preferMap?: Record<string, string>) => {
  if (preferMap && preferMap[k]) return preferMap[k]
  let h = 0
  for (let i = 0; i < k.length; i++) h = (h * 31 + k.charCodeAt(i)) >>> 0
  return palette[h % palette.length]
}

const currentColorFn = computed(() => {
  if (activeTab.value === 'logs') return (k: string) => colorForKey(k, logColorMap)
  return (k: string) => colorForKey(k, conflictStatusColorMap)
})

// ======================= Helpers =======================
const sortAggByOrder = (list: AggVo[], order: string[]) => {
  const map = new Map(list.map((i) => [i.key, i.count]))
  return order
    .filter((k) => map.has(k))
    .map((k) => ({ key: k, count: map.get(k) ?? 0 }))
    .concat(list.filter((i) => !order.includes(i.key)))
}

const canonicalOrder = (by: string) => {
  if (activeTab.value === 'logs' && by === SyncLogAggBy.STATUS) {
    return [SyncLogStatus.SUCCESS, SyncLogStatus.CONFLICT, SyncLogStatus.FAILED, SyncLogStatus.ERROR]
  }
  if (activeTab.value === 'conflicts' && by === SyncConflictAggBy.STATUS) {
    return [ConflictStatus.OPEN, ConflictStatus.RESOLVED, ConflictStatus.IGNORED]
  }
  return []
}

const byLabel = (by: string) => {
  const logMap: Record<string, string> = {
    [SyncLogAggBy.STATUS]: '按状态',
    [SyncLogAggBy.ENTITY_TYPE]: '按实体类型',
    [SyncLogAggBy.ACTION]: '按动作',
    [SyncLogAggBy.SOURCE_DB]: '按源库',
    [SyncLogAggBy.TARGET_DB]: '按目标库',
  }
  const conflictMap: Record<string, string> = {
    [SyncConflictAggBy.STATUS]: '按状态',
    [SyncConflictAggBy.CONFLICT_TYPE]: '按冲突类型',
    [SyncConflictAggBy.ENTITY_TYPE]: '按实体类型',
  }
  return activeTab.value === 'logs' ? logMap[by] ?? '统计' : conflictMap[by] ?? '统计'
}

// 近七天：生成 days（YYYY-MM-DD）
const buildLast7Days = () => {
  const days: string[] = []
  for (let i = 6; i >= 0; i--) {
    days.push(dayjs().subtract(i, 'day').format('YYYY-MM-DD'))
  }
  last7Days.value = days
  if (!selectedDay.value) selectedDay.value = days[days.length - 1]
}

// day -> begin/end LocalDateTime
const dayToRange = (day: string) => {
  const begin = `${day}T00:00:00`
  const end = `${day}T23:59:59`
  return { begin, end }
}

const last7Range = () => {
  const days = last7Days.value
  const begin = `${days[0]}T00:00:00`
  const end = `${days[days.length - 1]}T23:59:59`
  return { begin, end }
}

// ======================= Chart builders =======================
const buildBarByAgg = (title: string, list: AggVo[], colorFn: (k: string) => string): EChartsCoreOption => ({
  title: { text: title, left: 'left' },
  tooltip: { trigger: 'axis' },
  grid: { top: 52, left: 42, right: 18, bottom: 38, containLabel: true },
  xAxis: {
    type: 'category',
    data: list.map((i) => i.key),
    axisLabel: { interval: 0, rotate: 20 },
  },
  yAxis: { type: 'value', minInterval: 1 }, // 整数刻度
  series: [
    {
      name: '数量',
      type: 'bar',
      data: list.map((i) => ({
        value: i.count,
        itemStyle: { color: colorFn(i.key) },
      })),
    },
  ],
})

const buildPieByAgg = (title: string, list: AggVo[], colorFn: (k: string) => string): EChartsCoreOption => ({
  title: { text: title, left: 'left' },
  tooltip: {
    trigger: 'item',
    formatter: (p: any) => `${p.name}：${p.value}（${p.percent}%）`,
  },
  legend: { top: 'bottom' },
  series: [
    {
      name: '数量',
      type: 'pie',
      radius: '55%',
      data: list.map((i) => ({
        name: i.key,
        value: i.count,
        itemStyle: { color: colorFn(i.key) },
      })),
    },
  ],
})

const buildLast7Bar = (
  title: string,
  days: string[],
  raw: Record<string, number[]>,
  keys: string[],
  colorFn: (k: string) => string,
  mode: 'group' | 'stack'
): EChartsCoreOption => ({
  title: { text: title, left: 'left' },
  tooltip: { trigger: 'axis' },
  legend: { top: 'bottom' },
  grid: { top: 52, left: 42, right: 18, bottom: 52, containLabel: true },
  xAxis: { type: 'category', data: days },
  yAxis: { type: 'value', minInterval: 1 },
  series: keys.map((k) => ({
    name: k,
    type: 'bar',
    ...(mode === 'stack' ? { stack: 'total' } : {}),
    emphasis: { focus: 'series' },
    itemStyle: { color: colorFn(k) },
    data: raw[k] ?? new Array(days.length).fill(0),
  })),
})

// ======================= API wrapper =======================
const fetchAggOnce = async (by: string, begin: string | undefined, end: string | undefined, bodyFilter: any): Promise<AggVo[]> => {
  if (activeTab.value === 'logs') {
    const res = await aggLogs(begin, end, by as any, bodyFilter ?? ({} as any))
    return (res || []) as AggVo[]
  }
  const res = await aggConflicts(begin, end, by as any, bodyFilter ?? ({} as any))
  return (res || []) as AggVo[]
}

// ======================= Build: last7 left (bars) =======================
const buildLast7Left = async () => {
  buildLast7Days()
  const { begin, end } = last7Range()
  const byList = (last7ByList.value || []) as string[]
  if (byList.length === 0) {
    last7Blocks.value = []
    return
  }

  const days = last7Days.value
  const blocks: { by: string; option: EChartsCoreOption }[] = []

  for (const by of byList) {
    const seriesSet = new Set<string>()
    const raw: Record<string, number[]> = {}

    const dayResList = await Promise.all(
      days.map(async (d) => {
        const r = dayToRange(d)
        const list = await fetchAggOnce(by, r.begin, r.end, {})
        const order = canonicalOrder(by)
        return order.length ? sortAggByOrder(list, order) : list
      })
    )

    dayResList.forEach((list, idx) => {
      list.forEach((it) => {
        seriesSet.add(it.key)
        if (!raw[it.key]) raw[it.key] = new Array(days.length).fill(0)
        raw[it.key][idx] = it.count
      })
    })

    const keys = Array.from(seriesSet)
    const order = canonicalOrder(by)
    const sortedKeys = order.length ? order.filter((k) => keys.includes(k)).concat(keys.filter((k) => !order.includes(k))) : keys

    blocks.push({
      by,
      option: buildLast7Bar(
        `${tabLabel.value} · 近七天趋势（${byLabel(by)}）`,
        days,
        raw,
        sortedKeys,
        currentColorFn.value,
        last7BarMode.value
      ),
    })
  }

  last7Blocks.value = blocks
}

// ======================= Build: last7 right (pies, same by list) =======================
const buildLast7Right = async () => {
  buildLast7Days()
  const day = selectedDay.value
  if (!day) {
    last7PieBlocks.value = []
    return
  }

  const byList = (last7ByList.value || []) as string[]
  if (byList.length === 0) {
    last7PieBlocks.value = []
    return
  }

  const r = dayToRange(day)
  const blocks = await Promise.all(
    byList.map(async (by) => {
      const list0 = await fetchAggOnce(by, r.begin, r.end, {})
      const order = canonicalOrder(by)
      const list = order.length ? sortAggByOrder(list0, order) : list0
      return {
        by,
        option: buildPieByAgg(`${tabLabel.value} · ${day}（${byLabel(by)}）`, list, currentColorFn.value),
      }
    })
  )

  last7PieBlocks.value = blocks
}

// ======================= Build: global =======================
const buildGlobal = async () => {
  const begin = toLocalDateTimeParam(globalFilter.beginTime)
  const end = toLocalDateTimeParam(globalFilter.endTime)

  const byList = (globalByList.value || []) as string[]
  if (byList.length === 0) {
    globalBlocks.value = []
    return
  }

  const blocks = await Promise.all(
    byList.map(async (by) => {
      const list0 = await fetchAggOnce(by, begin, end, {})
      const order = canonicalOrder(by)
      const list = order.length ? sortAggByOrder(list0, order) : list0

      return {
        by,
        bar: buildBarByAgg(`${tabLabel.value} · ${byLabel(by)}（全局）`, list, currentColorFn.value),
        pie: buildPieByAgg(`${tabLabel.value} · ${byLabel(by)}占比（全局）`, list, currentColorFn.value),
      }
    })
  )

  globalBlocks.value = blocks
}

// ======================= Main fetch =======================
const fetchAll = async () => {
  await run(async () => {
    if (timeMode.value === 'last7') {
      await Promise.all([buildLast7Left(), buildLast7Right()])
    } else {
      await buildGlobal()
    }
  })
}

const onResetGlobal = () => {
  globalFilter.beginTime = ''
  globalFilter.endTime = ''
  fetchAll()
}

const onResetLast7 = () => {
  if (activeTab.value === 'logs') last7ByListLogs.value = [SyncLogAggBy.STATUS]
  else last7ByListConflicts.value = [SyncConflictAggBy.STATUS]
  last7BarMode.value = 'stack'
  selectedDay.value = ''
  buildLast7Days()
  fetchAll()
}

// ======================= Watchers =======================
watch(
  () => adminStore.changeTick,
  () => fetchAll()
)

watch(activeTab, () => {
  buildLast7Days()
  fetchAll()
})

watch(timeMode, () => fetchAll())

watch(last7ByListLogs, () => {
  if (timeMode.value === 'last7' && activeTab.value === 'logs') fetchAll()
})

watch(last7ByListConflicts, () => {
  if (timeMode.value === 'last7' && activeTab.value === 'conflicts') fetchAll()
})

watch(last7BarMode, () => {
  if (timeMode.value === 'last7') fetchAll()
})

watch(selectedDay, () => {
  if (timeMode.value === 'last7') buildLast7Right()
})

// 全局：多选 by 变化自动刷新
watch(globalByListLogs, () => {
  if (timeMode.value === 'global' && activeTab.value === 'logs') fetchAll()
})
watch(globalByListConflicts, () => {
  if (timeMode.value === 'global' && activeTab.value === 'conflicts') fetchAll()
})

// init
buildLast7Days()
fetchAll()
</script>

<template>
  <div class="dashboard-container" v-loading="loading">
    <!-- 1. 顶部标题与概览 -->
    <div class="dashboard-header">
      <div class="header-left">
        <h2 class="page-title">
          <el-icon class="icon-mr"><Monitor /></el-icon>
          数据同步监控看板
        </h2>
        <p class="page-desc">实时监控多数据库间的数据流转状态、异常日志与冲突处理情况。</p>
      </div>
      <div class="header-right">
        <el-radio-group v-model="activeTab" size="large">
          <el-radio-button label="logs">
            <el-icon class="icon-mr"><DataLine /></el-icon>同步日志
          </el-radio-button>
          <el-radio-button label="conflicts">
            <el-icon class="icon-mr"><Filter /></el-icon>冲突处理
          </el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 2. 筛选控制栏 -->
    <el-card class="control-bar" shadow="hover">
      <div class="control-row">
        <div class="control-group">
          <span class="label">时间跨度：</span>
          <el-radio-group v-model="timeMode">
            <el-radio-button label="last7">近七天趋势</el-radio-button>
            <el-radio-button label="global">历史全量统计</el-radio-button>
          </el-radio-group>
        </div>

        <!-- 全局模式筛选 -->
        <div v-if="timeMode === 'global'" class="control-group">
          <el-date-picker
            v-model="globalFilter.beginTime"
            type="datetime"
            placeholder="开始时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm"
            style="width: 200px"
          />
          <span class="separator">至</span>
          <el-date-picker
            v-model="globalFilter.endTime"
            type="datetime"
            placeholder="结束时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm"
            style="width: 200px"
          />
          <el-select
            v-model="globalByList"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="统计维度（多选）"
            style="width: 240px"
          >
            <el-option v-for="o in currentByOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </div>

        <!-- 近七天模式筛选 -->
        <div v-else class="control-group">
          <el-select
            v-model="last7ByList"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="统计维度（多选）"
            style="width: 240px"
          >
            <el-option v-for="o in currentByOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </div>

        <div class="actions">
          <el-button type="primary" :icon="Search" @click="fetchAll">查询</el-button>
          <el-button :icon="RefreshLeft" @click="timeMode === 'global' ? onResetGlobal() : onResetLast7()">重置</el-button>
        </div>
      </div>
    </el-card>

    <!-- 3. 图表展示区 -->
    <div class="charts-area">
      <!-- =================== 近七天模式 =================== -->
      <template v-if="timeMode === 'last7'">
        <el-row :gutter="20">
          <!-- 左侧：趋势图 -->
          <el-col :span="14">
            <el-card class="chart-panel" shadow="hover">
              <template #header>
                <div class="panel-header">
                  <span class="panel-title">
                    <el-icon class="icon-mr"><DataLine /></el-icon>
                    {{ tabLabel }} · 趋势分析
                  </span>
                  <el-radio-group v-model="last7BarMode" size="small">
                    <el-radio-button label="stack">堆叠视图</el-radio-button>
                    <el-radio-button label="group">分组视图</el-radio-button>
                  </el-radio-group>
                </div>
              </template>

              <el-empty v-if="last7Blocks.length === 0" description="请在上方选择至少一个统计维度" />
              <div v-for="blk in last7Blocks" :key="blk.by" class="chart-block">
                <BaseChart :option="blk.option" height="350px" />
              </div>
            </el-card>
          </el-col>

          <!-- 右侧：每日明细 -->
          <el-col :span="10">
            <el-card class="chart-panel" shadow="hover">
              <template #header>
                <div class="panel-header">
                  <span class="panel-title">
                    <el-icon class="icon-mr"><PieChart /></el-icon>
                    每日构成明细
                  </span>
                  <div class="date-selector">
                    <el-icon class="icon-mr"><Calendar /></el-icon>
                    {{ selectedDay }}
                  </div>
                </div>
              </template>

              <!-- 日期选择器（胶囊样式） -->
              <div class="day-capsules">
                <div
                  v-for="d in last7Days"
                  :key="d"
                  class="day-capsule"
                  :class="{ active: selectedDay === d }"
                  @click="selectedDay = d"
                >
                  <div class="day-label">{{ dayjs(d).format('MM-DD') }}</div>
                  <div class="day-sub">{{ d === dayjs().format('YYYY-MM-DD') ? '今天' : dayjs(d).format('ddd') }}</div>
                </div>
              </div>

              <el-divider style="margin: 12px 0" />

              <el-empty v-if="last7PieBlocks.length === 0" description="请在上方选择至少一个统计维度" />
              <div v-for="blk in last7PieBlocks" :key="blk.by" class="chart-block">
                <BaseChart :option="blk.option" height="300px" />
              </div>
            </el-card>
          </el-col>
        </el-row>
      </template>

      <!-- =================== 全局模式 =================== -->
      <template v-else>
        <el-empty v-if="globalBlocks.length === 0" description="请选择至少一个统计维度进行分析" />
        <div v-for="blk in globalBlocks" :key="blk.by" class="global-row">
          <el-row :gutter="20">
            <el-col :span="16">
              <el-card shadow="hover">
                <template #header>
                  <span class="panel-title">{{ tabLabel }} · {{ byLabel(blk.by) }}（总量分布）</span>
                </template>
                <BaseChart :option="blk.bar" height="350px" />
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card shadow="hover">
                <template #header>
                  <span class="panel-title">占比分析</span>
                </template>
                <BaseChart :option="blk.pie" height="350px" />
              </el-card>
            </el-col>
          </el-row>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.dashboard-container {
  padding: 0;
  min-height: 100%;
}

/* 1. Header */
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
}

.page-desc {
  color: #909399;
  font-size: 14px;
  margin: 0;
}

.icon-mr {
  margin-right: 8px;
  vertical-align: middle;
}

/* 2. Control Bar */
.control-bar {
  margin-bottom: 20px;
  border: none;
}

.control-row {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  align-items: center;
}

.control-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.separator {
  color: #909399;
}

.actions {
  margin-left: auto;
  display: flex;
  gap: 12px;
}

/* 3. Charts Area */
.chart-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
}

.chart-block {
  margin-bottom: 24px;
}

.chart-block:last-child {
  margin-bottom: 0;
}

/* Day Capsules */
.day-capsules {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.day-capsule {
  flex: 1;
  min-width: 50px;
  background-color: #f5f7fa;
  border-radius: 8px;
  padding: 8px 4px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.day-capsule:hover {
  background-color: #ecf5ff;
  color: #409eff;
}

.day-capsule.active {
  background-color: #409eff;
  color: white;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.day-label {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 2px;
}

.day-sub {
  font-size: 12px;
  opacity: 0.8;
}

.date-selector {
  font-size: 13px;
  color: #909399;
  display: flex;
  align-items: center;
}

.global-row {
  margin-bottom: 20px;
}
</style>
