<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent, DatasetComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { EChartsType, EChartsCoreOption } from 'echarts/core'

echarts.use([
  BarChart,
  LineChart,
  PieChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  DatasetComponent,
  CanvasRenderer,
])

const props = defineProps<{
  option: EChartsCoreOption
  height?: string
}>()

const chartRef = ref<HTMLDivElement>()
const isReady = ref(false) // 新增：用于控制加载动画
let chart: EChartsType | null = null
let observer: ResizeObserver | null = null
let initAttempts = 0
const maxAttempts = 10

const ensureInit = () => {
  if (!chartRef.value) return
  const { clientWidth, clientHeight } = chartRef.value
  if (clientWidth > 0 && clientHeight > 0) {
    chart = echarts.init(chartRef.value)
    chart.setOption(props.option, true)
    isReady.value = true // 标记初始化完成
    return
  }
  if (initAttempts < maxAttempts) {
    initAttempts += 1
    requestAnimationFrame(ensureInit)
  }
}

const resize = () => {
  chart?.resize()
}

defineExpose({ resize })

watch(
  () => props.option,
  (opt) => {
    if (chart) {
      chart.setOption(opt, true)
    }
  },
  { deep: true }
)

onMounted(() => {
  ensureInit()
  if (chartRef.value) {
    observer = new ResizeObserver(() => resize())
    observer.observe(chartRef.value)
  }
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  observer?.disconnect()
  observer = null
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div class="base-chart-pod" :style="{ height: height || '360px' }">
    <!-- 装饰：背景微弱网格 -->
    <div class="pod-grid-bg"></div>

    <!-- 装饰：四角科技光标 -->
    <div class="corner top-left"></div>
    <div class="corner top-right"></div>
    <div class="corner bottom-left"></div>
    <div class="corner bottom-right"></div>

    <!-- 核心图表容器 -->
    <div ref="chartRef" class="chart-canvas"></div>

    <!-- 加载状态遮罩 -->
    <transition name="fade">
      <div v-if="!isReady" class="chart-loader">
        <div class="scanner-line"></div>
        <div class="loader-text">Analyzing Data Stream...</div>
      </div>
    </transition>
  </div>
</template>

<style scoped lang="scss">
.base-chart-pod {
  position: relative;
  width: 100%;
  background: rgba(255, 255, 255, 0.5); /* 默认浅色半透明 */
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.03);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);

  &:hover {
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
    border-color: rgba(64, 158, 255, 0.3); /* 悬停时显现品牌色 */

    .corner {
      opacity: 1;
      transform: scale(1);
    }
  }
}

.chart-canvas {
  width: 100%;
  height: 100%;
  position: relative;
  z-index: 5;
}

/* --- 背景装饰 --- */
.pod-grid-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(64, 158, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(64, 158, 255, 0.03) 1px, transparent 1px);
  background-size: 20px 20px;
  z-index: 1;
  opacity: 0.6;
}

/* --- 四角科技装饰 --- */
.corner {
  position: absolute;
  width: 12px;
  height: 12px;
  border: 2px solid #409eff;
  z-index: 10;
  opacity: 0; /* 默认隐藏，悬停显示 */
  transition: all 0.3s ease;
  transform: scale(0.8);
  pointer-events: none;

  &.top-left {
    top: 6px;
    left: 6px;
    border-right: none;
    border-bottom: none;
    border-top-left-radius: 4px;
  }
  &.top-right {
    top: 6px;
    right: 6px;
    border-left: none;
    border-bottom: none;
    border-top-right-radius: 4px;
  }
  &.bottom-left {
    bottom: 6px;
    left: 6px;
    border-right: none;
    border-top: none;
    border-bottom-left-radius: 4px;
  }
  &.bottom-right {
    bottom: 6px;
    right: 6px;
    border-left: none;
    border-top: none;
    border-bottom-right-radius: 4px;
  }
}

/* --- 加载动画 --- */
.chart-loader {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
  z-index: 20;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.scanner-line {
  width: 48px;
  height: 4px;
  background: #e4e7ed;
  border-radius: 2px;
  position: relative;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    height: 100%;
    width: 20px;
    background: #409eff;
    border-radius: 2px;
    animation: scan 1s infinite cubic-bezier(0.4, 0, 0.2, 1) alternate;
  }
}

.loader-text {
  font-size: 12px;
  color: #909399;
  font-family: monospace;
  letter-spacing: 0.5px;
}

@keyframes scan {
  0% { transform: translateX(0); width: 10px; }
  100% { transform: translateX(38px); width: 10px; }
}

/* Transition */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

