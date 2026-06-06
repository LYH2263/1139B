<template>
  <el-dialog
    v-model="visible"
    :title="detailData?.schedule?.name || '计划详情'"
    width="900px"
    top="5vh"
    @closed="handleClosed"
  >
    <div v-loading="loading" class="detail-wrapper">
      <template v-if="detailData">
        <!-- 概览统计 -->
        <div class="overview-section">
          <SectionCard title="学习概览">
            <div class="stats-grid">
              <div class="stat-item">
                <div class="stat-icon total"><el-icon><Collection /></el-icon></div>
                <div class="stat-info">
                  <span class="stat-value">{{ detailData.schedule.totalWords }}</span>
                  <span class="stat-label">总单词数</span>
                </div>
              </div>
              <div class="stat-item">
                <div class="stat-icon learned"><el-icon><Finished /></el-icon></div>
                <div class="stat-info">
                  <span class="stat-value">{{ detailData.schedule.learnedWords }}</span>
                  <span class="stat-label">已掌握</span>
                </div>
              </div>
              <div class="stat-item">
                <div class="stat-icon days"><el-icon><Timer /></el-icon></div>
                <div class="stat-info">
                  <span class="stat-value">{{ detailData.schedule.completedDays }}/{{ detailData.schedule.totalDays }}</span>
                  <span class="stat-label">完成天数</span>
                </div>
              </div>
              <div class="stat-item">
                <div class="stat-icon progress"><el-icon><DataLine /></el-icon></div>
                <div class="stat-info">
                  <span class="stat-value">{{ detailData.schedule.progressPercent }}%</span>
                  <span class="stat-label">完成进度</span>
                </div>
              </div>
            </div>
            <div class="progress-bar-wrap">
              <el-progress
                :percentage="detailData.schedule.progressPercent"
                :stroke-width="12"
                :show-text="false"
              />
            </div>
            <div class="schedule-meta">
              <div class="meta-item">
                <el-icon><Calendar /></el-icon>
                <span>{{ detailData.schedule.startDate }} 至 {{ detailData.schedule.endDate }}</span>
              </div>
              <div class="meta-item">
                <el-icon><Coin /></el-icon>
                <span>每日学习 {{ detailData.schedule.dailyCount }} 个</span>
              </div>
              <div class="meta-item">
                <el-tag
                  :type="statusType"
                  size="small"
                  effect="light"
                >
                  {{ statusText }}
                </el-tag>
              </div>
            </div>
          </SectionCard>
        </div>

        <!-- 甘特图 -->
        <div class="gantt-section">
          <SectionCard title="学习进度甘特图">
            <div v-if="detailData.progressList.length > 0" class="gantt-wrapper">
              <v-chart class="gantt-chart" :option="ganttOption" autoresize />
            </div>
            <EmptyState
              v-else
              title="暂无进度数据"
              description="学习计划还未开始执行"
              icon="Calendar"
            />
          </SectionCard>
        </div>

        <!-- 每日进度列表 -->
        <div class="progress-section">
          <SectionCard title="每日学习记录">
            <div class="progress-table-wrapper" v-if="detailData.progressList.length > 0">
              <el-table
                :data="detailData.progressList"
                max-height="300"
                stripe
              >
                <el-table-column label="日期" width="130" prop="date" />
                <el-table-column label="计划单词" width="100">
                  <template #default="{ row }">
                    <span>{{ row.plannedCount }} 个</span>
                  </template>
                </el-table-column>
                <el-table-column label="已完成" width="100">
                  <template #default="{ row }">
                    <span :class="{ 'text-success': row.isCompleted }">{{ row.completedCount }} 个</span>
                  </template>
                </el-table-column>
                <el-table-column label="完成度">
                  <template #default="{ row }">
                    <el-progress
                      :percentage="row.plannedCount > 0 ? Math.round((row.completedCount / row.plannedCount) * 100) : 0"
                      :stroke-width="8"
                      :status="row.isCompleted ? 'success' : ''"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="90">
                  <template #default="{ row }">
                    <el-tag
                      :type="row.isCompleted ? 'success' : (isPast(row.date) ? 'danger' : 'info')"
                      size="small"
                      effect="light"
                    >
                      {{ row.isCompleted ? '已完成' : (isPast(row.date) ? '未完成' : '待学习') }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <EmptyState
              v-else
              title="暂无学习记录"
              description="开始学习后将在此显示每日记录"
              icon="Document"
            />
          </SectionCard>
        </div>
      </template>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, reactive } from 'vue'
import * as echarts from 'echarts'
import {
  Collection,
  Finished,
  Timer,
  DataLine,
  Calendar,
  Coin
} from '@element-plus/icons-vue'
import type { ScheduleDetailResponse, StudySchedule } from '@/types'
import { scheduleApi } from '@/api/schedule'
import SectionCard from '@/components/ui/SectionCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  DataZoomComponent
} from 'echarts/components'

use([
  CanvasRenderer,
  BarChart,
  LineChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  DataZoomComponent
])

interface Props {
  modelValue: boolean
  scheduleId: number | null
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const detailData = ref<ScheduleDetailResponse | null>(null)

const statusType = computed(() => {
  const status = detailData.value?.schedule?.status
  switch (status) {
    case 'ACTIVE': return 'primary'
    case 'COMPLETED': return 'success'
    case 'PAUSED': return 'warning'
    case 'ARCHIVED': return 'info'
    default: return 'info'
  }
})

const statusText = computed(() => {
  const status = detailData.value?.schedule?.status
  switch (status) {
    case 'ACTIVE': return '进行中'
    case 'COMPLETED': return '已完成'
    case 'PAUSED': return '已暂停'
    case 'ARCHIVED': return '已归档'
    default: return status || '未知'
  }
})

const ganttOption = computed(() => {
  if (!detailData.value || detailData.value.progressList.length === 0) {
    return {}
  }

  const progressList = detailData.value.progressList
  const dates = progressList.map((p) => p.date)
  const planned = progressList.map((p) => p.plannedCount)
  const completed = progressList.map((p) => p.completedCount)
  const completionRates = progressList.map((p) =>
    p.plannedCount > 0 ? Math.round((p.completedCount / p.plannedCount) * 100) : 0
  )

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    legend: {
      data: ['计划单词', '已完成', '完成率'],
      top: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: {
        rotate: dates.length > 15 ? 45 : 0,
        fontSize: 11
      }
    },
    yAxis: [
      {
        type: 'value',
        name: '单词数',
        position: 'left',
        axisLabel: {
          formatter: '{value} 个'
        }
      },
      {
        type: 'value',
        name: '完成率',
        position: 'right',
        axisLabel: {
          formatter: '{value}%'
        },
        max: 100,
        min: 0
      }
    ],
    dataZoom: [
      {
        type: 'inside',
        start: 0,
        end: 100
      },
      {
        type: 'slider',
        start: 0,
        end: 100,
        height: 20,
        bottom: 5
      }
    ],
    series: [
      {
        name: '计划单词',
        type: 'bar',
        data: planned,
        itemStyle: {
          color: 'var(--c-primary-light, #409EFF)',
          borderRadius: [4, 4, 0, 0]
        },
        barMaxWidth: 30
      },
      {
        name: '已完成',
        type: 'bar',
        data: completed,
        itemStyle: {
          color: 'var(--c-success, #67C23A)',
          borderRadius: [4, 4, 0, 0]
        },
        barMaxWidth: 30
      },
      {
        name: '完成率',
        type: 'line',
        yAxisIndex: 1,
        data: completionRates,
        smooth: true,
        itemStyle: {
          color: 'var(--c-warning, #E6A23C)'
        },
        lineStyle: {
          width: 3
        },
        areaStyle: {
          opacity: 0.1
        }
      }
    ]
  } as echarts.EChartsOption
})

const loadDetail = async () => {
  if (!props.scheduleId) return
  loading.value = true
  try {
    detailData.value = await scheduleApi.getScheduleDetail(props.scheduleId)
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const isPast = (dateStr: string) => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const target = new Date(dateStr)
  return target < today
}

const handleClosed = () => {
  detailData.value = null
}

watch(
  () => [visible.value, props.scheduleId],
  ([isVisible, id]) => {
    if (isVisible && id) {
      loadDetail()
    }
  }
)
</script>

<style scoped>
.detail-wrapper {
  max-height: 75vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: white;
}

.stat-icon.total {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.learned {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon.days {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon.progress {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--c-text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: var(--c-text-secondary);
}

.progress-bar-wrap {
  margin-bottom: var(--space-md);
}

.schedule-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-lg);
  padding-top: var(--space-md);
  border-top: 1px solid var(--c-border-light);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--c-text-secondary);
  font-size: var(--font-size-sm);
}

.meta-item :deep(.el-icon) {
  color: var(--c-primary);
}

.gantt-wrapper {
  width: 100%;
}

.gantt-chart {
  width: 100%;
  height: 340px;
}

.progress-table-wrapper {
  border: 1px solid var(--c-border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.text-success {
  color: var(--c-success);
  font-weight: 600;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
