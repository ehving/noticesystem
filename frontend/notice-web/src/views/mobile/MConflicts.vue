<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import dayjs from 'dayjs'
import BaseChart from '@/components/charts/BaseChart.vue'
import { useAdminStore } from '@/stores/admin'
import { useLoading } from '@/hooks/useLoading'
import { aggConflicts } from '@/api/modules/admin/conflicts'
import { SyncConflictAggBy } from '@/types/enums/agg-by'
import { ConflictStatus } from '@/types/enums/sync'
import type { AggVo } from '@/types/models/agg'
import type { EChartsCoreOption } from 'echarts/core'
import { toLocalDateTimeParam } from '@/utils/time'

const adminStore = useAdminStore()
const { loading, run } = useLoading()

type Mode = '7d' | 'global'
const mode = ref<Mode>('7d')

// ✅ 移动端：by 单选
const by = ref<SyncConflictAggBy>(SyncConflictAggBy.STATUS)

// 你有哪些 by，就在这填（必须和后端枚举一致）
const BY_OPTIONS = [
  { label: '按状态', value: SyncConflictAggBy.STATUS },
  { label: '按冲突类型', value: SyncConflictAggBy.CONFLICT_TYPE },
  { label: '按实体类型', value: SyncConflictAggBy.ENTITY_TYPE },
  // { label: '按源库', value: SyncConflictAggBy.SOURCE_DB },
  // { label: '按目标库', value: SyncConflictAggBy.TARGET_DB },
]

// ✅ 全局模式：日历选日期（不选=全历史）
const filter = reactive({
  day: '', // YYYY-MM-DD
  // 你要额外筛选（比如只看 OPEN）可在这里扩展：
  // status: '' as '' | ConflictStatus,
})

// ✅ 近七天选择
const days7 = computed(() =>
  Array.from({ length: 7 })
    .map((_, i) => dayjs().subtract(6 - i, 'day'))
    .map((d) => d.format('YYYY-MM-DD'))
)
const activeDay = ref(days7.value[6]) // 默认今天

// 图表
const barOption = ref<EChartsCoreOption>({})
const pieOption = ref<EChartsCoreOption>({})
const barRef = ref<InstanceType<typeof BaseChart> | null>(null)
const pieRef = ref<InstanceType<typeof BaseChart> | null>(null)

// ===================== ✅ 统一配色：柱状图 & 饼图一致 =====================

// 状态固定色（按你要求：解决绿，错误蓝；其它补齐）
const STATUS_COLOR: Record<string, string> = {
  [ConflictStatus.RESOLVED]: '#67C23A', // ✅ 已解决：绿
  // 你后端如果有 ERROR/FAILED 这种冲突状态就映射到蓝；否则用作“其它错误类”
  ERROR: '#409EFF', // ✅ 错误：蓝（兜底占位）
  [ConflictStatus.OPEN]: '#E6A23C', // OPEN：橙（处理态）
  [ConflictStatus.IGNORED]: '#909399', // 忽略：灰
}

// 更大、更均衡的 48 色调色盘（偏高饱和 + 易区分）
// 直接替换你现有的 PALETTE 即可
const PALETTE = [
  '#1F77B4', '#FF7F0E', '#2CA02C', '#D62728', '#9467BD', '#8C564B',
  '#E377C2', '#7F7F7F', '#BCBD22', '#17BECF', '#AEC7E8', '#FFBB78',
  '#98DF8A', '#FF9896', '#C5B0D5', '#C49C94', '#F7B6D2', '#C7C7C7',
  '#DBDB8D', '#9EDAE5', '#393B79', '#637939', '#8C6D31', '#843C39',
  '#7B4173', '#3182BD', '#E6550D', '#31A354', '#756BB1', '#636363',
  '#6BAED6', '#FD8D3C', '#74C476', '#9E9AC8', '#969696', '#9ECAE1',
  '#FDAE6B', '#A1D99B', '#BCBDDC', '#BDBDBD', '#BDD7E7', '#FDD0A2',
  '#C7E9C0', '#DADAEB', '#D9D9D9', '#C6DBEF', '#FEE6CE', '#E5F5E0',
]


const colorCache = new Map<string, string>()

const hashString = (s: string) => {
  let h = 0
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0
  return h
}

const getColorForKey = (key: string, isStatus: boolean) => {
  if (isStatus && STATUS_COLOR[key]) return STATUS_COLOR[key]

  const k = `${String(by.value)}::${key}`
  const cached = colorCache.get(k)
  if (cached) return cached

  const idx = hashString(k) % PALETTE.length
  const color = PALETTE[idx]
  colorCache.set(k, color)
  return color
}

// ===================== 数据处理 =====================

const STATUS_ORDER = [ConflictStatus.OPEN, ConflictStatus.RESOLVED, ConflictStatus.IGNORED] as string[]

const normalizeOrder = (list: AggVo[], order: string[]) => {
  const map = new Map(list.map((i) => [i.key, i.count]))
  const fixed = order.map((k) => ({ key: k, count: map.get(k) ?? 0 }))
  const rest = list.filter((i) => !order.includes(i.key))
  return fixed.concat(rest)
}

const buildBar = (title: string, list: AggVo[], isStatus: boolean): EChartsCoreOption => {
  const x = list.map((i) => i.key)
  const y = list.map((i) => i.count)

  return {
    title: { text: title, left: 'left' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: x, axisLabel: { interval: 0, rotate: 20 } },
    yAxis: { type: 'value', minInterval: 1 }, // ✅ 整数刻度
    series: [
      {
        name: '数量',
        type: 'bar',
        data: y.map((v, idx) => ({
          value: v,
          itemStyle: { color: getColorForKey(x[idx], isStatus) },
        })),
      },
    ],
  }
}

const buildPie = (title: string, list: AggVo[], isStatus: boolean): EChartsCoreOption => {
  return {
    title: { text: title, left: 'left' },
    tooltip: {
      trigger: 'item',
      formatter: (p: any) => `${p?.name ?? '-'}<br/>数量：${p?.value ?? 0}<br/>占比：${p?.percent ?? 0}%`,
    },
    legend: { top: 'bottom' },
    series: [
      {
        name: '占比',
        type: 'pie',
        radius: '60%',
        itemStyle: { borderColor: '#fff', borderWidth: 1 },
        data: list.map((i) => ({
          name: i.key,
          value: i.count,
          itemStyle: { color: getColorForKey(i.key, isStatus) },
        })),
      },
    ],
  }
}

// ✅ 统一生成 begin/end（按“每日查看”）
const getBeginEnd = () => {
  if (mode.value === '7d') {
    const d = dayjs(activeDay.value)
    return {
      begin: toLocalDateTimeParam(d.startOf('day').toDate()),
      end: toLocalDateTimeParam(d.endOf('day').toDate()),
      label: activeDay.value,
    }
  }

  if (filter.day) {
    const d = dayjs(filter.day)
    return {
      begin: toLocalDateTimeParam(d.startOf('day').toDate()),
      end: toLocalDateTimeParam(d.endOf('day').toDate()),
      label: filter.day,
    }
  }

  return { begin: undefined as string | undefined, end: undefined as string | undefined, label: '全历史' }
}

// ✅ 未来你要加筛选（status/entityType/conflictType/sourceDb/targetDb/limitToOpenOnly）就在这里扩展
const buildBodyFilter = () => {
  // 示例：如果你想默认只看 OPEN：
  // return { status: ConflictStatus.OPEN } as any
  return {} as any
}

const fetchData = async () => {
  await run(async () => {
    const { begin, end, label } = getBeginEnd()
    const body = buildBodyFilter()

    const res = await aggConflicts(begin, end, by.value, body)
    let list = (res || []).filter((i) => (i.count ?? 0) >= 0)

    const isStatus = by.value === SyncConflictAggBy.STATUS
    if (isStatus) list = normalizeOrder(list, STATUS_ORDER)

    const byLabel = BY_OPTIONS.find((x) => x.value === by.value)?.label ?? '统计'
    const barTitle = `${byLabel}（${label}）`

    barOption.value = buildBar(barTitle, list, isStatus)
    pieOption.value = buildPie(`${byLabel}占比`, list, isStatus)

    barRef.value?.resize()
    pieRef.value?.resize()
  })
}

const onPickDay = (d: string) => {
  activeDay.value = d
  fetchData()
}

const onReset = () => {
  if (mode.value === 'global') filter.day = ''
  else activeDay.value = days7.value[6]
  fetchData()
}

watch(mode, () => fetchData())
watch(by, () => fetchData())
watch(
  () => adminStore.changeTick,
  () => fetchData()
)

fetchData()
</script>

<template>
  <div class="page">
    <el-card>
      <div class="top">
        <el-segmented
          v-model="mode"
          :options="[
            { label: '近七天', value: '7d' },
            { label: '全局', value: 'global' },
          ]"
        />

        <div class="row">
          <span class="label">统计维度</span>
          <el-select v-model="by" size="small" style="flex: 1">
            <el-option v-for="o in BY_OPTIONS" :key="String(o.value)" :label="o.label" :value="o.value" />
          </el-select>
        </div>

        <div v-if="mode === '7d'" class="days">
          <el-scrollbar>
            <div class="day-row">
              <el-button
                v-for="d in days7"
                :key="d"
                size="small"
                :type="d === activeDay ? 'primary' : 'default'"
                @click="onPickDay(d)"
              >
                {{ d.slice(5) }}
              </el-button>
            </div>
          </el-scrollbar>
        </div>

        <div v-else class="row">
          <span class="label">选择日期</span>
          <el-date-picker
            v-model="filter.day"
            type="date"
            placeholder="不选=全历史"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="flex: 1"
            @change="fetchData"
          />
        </div>

        <div class="actions">
          <el-button type="primary" :loading="loading" @click="fetchData">查询</el-button>
          <el-button @click="onReset">复位</el-button>
        </div>
      </div>

      <div class="charts" v-loading="loading">
        <BaseChart ref="barRef" :option="barOption" height="280px" />
        <BaseChart ref="pieRef" :option="pieOption" height="280px" />
      </div>

      <div class="hint">当前库：{{ adminStore.activeDb }}</div>
    </el-card>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.top {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.row {
  display: flex;
  gap: 8px;
  align-items: center;
}
.label {
  color: #606266;
  font-size: 12px;
  white-space: nowrap;
}
.days .day-row {
  display: flex;
  gap: 8px;
  padding-bottom: 6px;
}
.actions {
  display: flex;
  gap: 8px;
}
.charts {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 10px;
}
.hint {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}
</style>

