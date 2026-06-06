<template>
  <div class="schedule-page page-container">
    <PageHeader title="学习计划" subtitle="创建专属学习计划，按艾宾浩斯曲线智能安排每日学习。">
      <template #actions>
        <el-button type="primary" size="large" @click="showWizard = true">
          <el-icon><Plus /></el-icon>
          <span>创建计划</span>
        </el-button>
      </template>
    </PageHeader>

    <div class="schedule-content" v-loading="loading">
      <!-- 今日任务区域 -->
      <div class="today-section">
        <SectionCard>
          <template #header>
            <div class="section-title-wrap">
              <el-icon class="title-icon"><Calendar /></el-icon>
              <span>今日任务</span>
              <el-tag v-if="todaySchedules.length > 0" type="primary" size="small" effect="light">
                {{ todaySchedules.length }} 个计划
              </el-tag>
            </div>
          </template>

          <div v-if="todaySchedules.length === 0" class="empty-today">
            <EmptyState
              title="今日暂无任务"
              description="创建一个学习计划开始你的单词学习之旅！"
              icon="Calendar"
            >
              <template #action>
                <el-button type="primary" size="large" @click="showWizard = true">
                  创建学习计划
                </el-button>
              </template>
            </EmptyState>
          </div>

          <div v-else class="today-cards">
            <div
              v-for="todayData in todaySchedules"
              :key="todayData.scheduleId"
              class="today-card"
              :class="{ completed: todayData.isCompleted }"
            >
              <div class="today-card-header">
                <div class="today-card-title">
                  <h3>{{ todayData.scheduleName }}</h3>
                  <el-tag
                    size="small"
                    :type="todayData.isCompleted ? 'success' : 'warning'"
                    effect="light"
                  >
                    {{ todayData.isCompleted ? '已完成' : '进行中' }}
                  </el-tag>
                </div>
                <div class="today-card-progress">
                  <span class="progress-text">{{ todayData.completedCount }}/{{ todayData.totalCount }}</span>
                  <el-progress
                    :percentage="todayData.totalCount > 0 ? Math.round((todayData.completedCount / todayData.totalCount) * 100) : 0"
                    :stroke-width="6"
                    :show-text="false"
                    :status="todayData.isCompleted ? 'success' : ''"
                  />
                </div>
              </div>

              <div class="today-card-stats">
                <div class="stat-tag">
                  <el-icon class="tag-icon new"><Star /></el-icon>
                  <span>新词 {{ todayData.newWords.length }}</span>
                </div>
                <div class="stat-tag">
                  <el-icon class="tag-icon review"><Refresh /></el-icon>
                  <span>复习 {{ todayData.reviewWords.length }}</span>
                </div>
              </div>

              <div class="today-card-words">
                <div class="words-header">
                  <span>单词列表</span>
                  <el-button
                    type="primary"
                    link
                    size="small"
                    @click="completeAllToday(todayData)"
                    :disabled="todayData.isCompleted || completingToday === todayData.scheduleId"
                    :loading="completingToday === todayData.scheduleId"
                  >
                    全部完成
                  </el-button>
                </div>
                <div class="words-list">
                  <div
                    v-for="word in todayData.plannedWords"
                    :key="word.wordId"
                    class="word-item"
                    :class="{ completed: word.isCompleted }"
                    @click="toggleWordComplete(todayData, word)"
                  >
                    <el-checkbox
                      :model-value="word.isCompleted"
                      :disabled="completingToday === todayData.scheduleId"
                    />
                    <div class="word-info">
                      <div class="word-main">
                        <span class="word-text">{{ word.word }}</span>
                        <span v-if="word.phonetic" class="word-phonetic">/{{ word.phonetic }}/</span>
                        <el-tag
                          size="small"
                          effect="dark"
                          :type="word.type === 'NEW' ? 'primary' : 'success'"
                          class="word-type-tag"
                        >
                          {{ word.type === 'NEW' ? '新词' : '复习' }}
                        </el-tag>
                      </div>
                      <span class="word-meaning">{{ word.meaning }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </SectionCard>
      </div>

      <!-- 活跃计划列表 -->
      <div class="schedules-section">
        <SectionCard>
          <template #header>
            <div class="section-title-wrap">
              <el-icon class="title-icon"><Collection /></el-icon>
              <span>我的学习计划</span>
              <el-tag v-if="schedules.length > 0" size="small" effect="light">
                {{ schedules.length }} 个计划
              </el-tag>
            </div>
          </template>

          <div v-if="schedules.length === 0" class="empty-schedules">
            <EmptyState
              title="还没有学习计划"
              description="创建你的第一个学习计划，系统将按艾宾浩斯遗忘曲线自动安排学习。"
              icon="Collection"
            >
              <template #action>
                <el-button type="primary" size="large" @click="showWizard = true">
                  创建学习计划
                </el-button>
              </template>
            </EmptyState>
          </div>

          <div v-else class="schedule-grid">
            <div
              v-for="schedule in schedules"
              :key="schedule.id"
              class="schedule-card"
            >
              <div class="schedule-card-header">
                <div class="schedule-title-row">
                  <h3 class="schedule-name">{{ schedule.name }}</h3>
                  <el-dropdown @command="(cmd) => handleScheduleAction(cmd, schedule)">
                    <el-button text>
                      <el-icon><MoreFilled /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="view">查看详情</el-dropdown-item>
                        <el-dropdown-item command="delete" divided>删除计划</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
                <el-tag
                  size="small"
                  :type="getStatusType(schedule.status)"
                  effect="light"
                >
                  {{ getStatusText(schedule.status) }}
                </el-tag>
              </div>

              <div class="schedule-stats">
                <div class="schedule-stat">
                  <span class="stat-num">{{ schedule.learnedWords }}</span>
                  <span class="stat-label">已学单词</span>
                </div>
                <div class="schedule-stat">
                  <span class="stat-num">{{ schedule.totalWords }}</span>
                  <span class="stat-label">总单词</span>
                </div>
                <div class="schedule-stat">
                  <span class="stat-num">{{ schedule.dailyCount }}</span>
                  <span class="stat-label">每日量</span>
                </div>
              </div>

              <div class="schedule-progress-wrap">
                <div class="progress-header">
                  <span class="progress-label">学习进度</span>
                  <span class="progress-percent">{{ schedule.progressPercent }}%</span>
                </div>
                <el-progress
                  :percentage="schedule.progressPercent"
                  :stroke-width="8"
                  :show-text="false"
                />
              </div>

              <div class="schedule-footer">
                <div class="schedule-date">
                  <el-icon><Calendar /></el-icon>
                  <span>{{ schedule.startDate }} ~ {{ schedule.endDate }}</span>
                </div>
                <el-button
                  type="primary"
                  size="small"
                  @click="viewScheduleDetail(schedule.id)"
                >
                  查看详情
                </el-button>
              </div>
            </div>
          </div>
        </SectionCard>
      </div>
    </div>

    <!-- 计划向导对话框 -->
    <ScheduleWizardDialog
      v-model="showWizard"
      @success="handleScheduleCreated"
    />

    <!-- 计划详情对话框 -->
    <ScheduleDetailDialog
      v-model="showDetail"
      :schedule-id="currentDetailId"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Calendar,
  Collection,
  Star,
  Refresh,
  MoreFilled
} from '@element-plus/icons-vue'
import type { StudySchedule, ScheduleTodayResponse } from '@/types'
import { scheduleApi } from '@/api/schedule'
import PageHeader from '@/components/ui/PageHeader.vue'
import SectionCard from '@/components/ui/SectionCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import ScheduleWizardDialog from '@/components/ScheduleWizardDialog.vue'
import ScheduleDetailDialog from '@/components/ScheduleDetailDialog.vue'

const loading = ref(false)
const schedules = ref<StudySchedule[]>([])
const todaySchedules = ref<ScheduleTodayResponse[]>([])
const showWizard = ref(false)
const showDetail = ref(false)
const currentDetailId = ref<number | null>(null)
const completingToday = ref<number | null>(null)

const loadSchedules = async () => {
  loading.value = true
  try {
    const res = await scheduleApi.getSchedules()
    schedules.value = res.list.sort((a, b) => {
      const statusOrder = { ACTIVE: 0, PAUSED: 1, COMPLETED: 2, ARCHIVED: 3 }
      const orderA = statusOrder[a.status as keyof typeof statusOrder] ?? 99
      const orderB = statusOrder[b.status as keyof typeof statusOrder] ?? 99
      if (orderA !== orderB) return orderA - orderB
      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    })
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const loadTodaySchedules = async () => {
  try {
    const res = await scheduleApi.getSchedules()
    const activeSchedules = res.list.filter((s) => s.status === 'ACTIVE')
    const todayPromises = activeSchedules.map((s) =>
      scheduleApi.getTodaySchedule(s.id).catch(() => null)
    )
    const results = await Promise.all(todayPromises)
    todaySchedules.value = results.filter(
      (r): r is ScheduleTodayResponse => r !== null && r.totalCount > 0
    )
  } catch (error) {
    console.error(error)
  }
}

const toggleWordComplete = async (
  todayData: ScheduleTodayResponse,
  word: ScheduleTodayResponse['plannedWords'][0]
) => {
  if (completingToday.value !== null) return

  const newCompleted = word.isCompleted
    ? todayData.completedWordIds.filter((w) => w.wordId !== word.wordId)
    : [...todayData.completedWordIds, word]

  completingToday.value = todayData.scheduleId
  try {
    const updated = await scheduleApi.completeToday(
      todayData.scheduleId,
      newCompleted.map((w) => w.wordId)
    )
    const idx = todaySchedules.value.findIndex(
      (t) => t.scheduleId === todayData.scheduleId
    )
    if (idx !== -1) {
      todaySchedules.value[idx] = updated
    }
    if (updated.isCompleted && !todayData.isCompleted) {
      ElMessage.success('今日任务已完成！')
    }
  } catch (error) {
    console.error(error)
  } finally {
    completingToday.value = null
  }
}

const completeAllToday = async (todayData: ScheduleTodayResponse) => {
  if (completingToday.value !== null) return
  completingToday.value = todayData.scheduleId
  try {
    const updated = await scheduleApi.completeToday(todayData.scheduleId)
    const idx = todaySchedules.value.findIndex(
      (t) => t.scheduleId === todayData.scheduleId
    )
    if (idx !== -1) {
      todaySchedules.value[idx] = updated
    }
    ElMessage.success('已标记今日任务全部完成！')
  } catch (error) {
    console.error(error)
  } finally {
    completingToday.value = null
  }
}

const viewScheduleDetail = (id: number) => {
  currentDetailId.value = id
  showDetail.value = true
}

const handleScheduleAction = (command: string, schedule: StudySchedule) => {
  switch (command) {
    case 'view':
      viewScheduleDetail(schedule.id)
      break
    case 'delete':
      handleDeleteSchedule(schedule)
      break
  }
}

const handleDeleteSchedule = async (schedule: StudySchedule) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除学习计划"${schedule.name}"吗？此操作不可恢复。`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }
    )
    await scheduleApi.deleteSchedule(schedule.id)
    ElMessage.success('计划已删除')
    loadSchedules()
    loadTodaySchedules()
  } catch {
    // cancelled
  }
}

const handleScheduleCreated = () => {
  loadSchedules()
  loadTodaySchedules()
}

const getStatusType = (status: string) => {
  switch (status) {
    case 'ACTIVE': return 'primary'
    case 'COMPLETED': return 'success'
    case 'PAUSED': return 'warning'
    case 'ARCHIVED': return 'info'
    default: return 'info'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'ACTIVE': return '进行中'
    case 'COMPLETED': return '已完成'
    case 'PAUSED': return '已暂停'
    case 'ARCHIVED': return '已归档'
    default: return status
  }
}

onMounted(() => {
  loadSchedules()
  loadTodaySchedules()
})
</script>

<style scoped>
.schedule-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-xl);
}

.section-title-wrap {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.title-icon {
  color: var(--c-primary);
  font-size: 18px;
}

/* Today Section */
.empty-today {
  padding: var(--space-xl) 0;
}

.today-cards {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.today-card {
  border: 1px solid var(--c-border-light);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  background: var(--c-bg-card);
  transition: all var(--transition-normal);
}

.today-card.completed {
  background: var(--c-success-bg);
  border-color: var(--c-success-light);
}

.today-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-md);
}

.today-card-title {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.today-card-title h3 {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--c-text-primary);
}

.today-card-progress {
  min-width: 180px;
  text-align: right;
}

.progress-text {
  display: block;
  font-size: var(--font-size-sm);
  color: var(--c-text-secondary);
  margin-bottom: 4px;
}

.today-card-stats {
  display: flex;
  gap: var(--space-lg);
  margin-bottom: var(--space-md);
}

.stat-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: var(--c-bg-body);
  border-radius: var(--radius-full);
  font-size: var(--font-size-sm);
  color: var(--c-text-secondary);
}

.tag-icon {
  font-size: 14px;
}

.tag-icon.new {
  color: var(--c-primary);
}

.tag-icon.review {
  color: var(--c-success);
}

.today-card-words {
  border-top: 1px solid var(--c-border-light);
  padding-top: var(--space-md);
}

.words-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-sm);
  font-size: var(--font-size-sm);
  color: var(--c-text-secondary);
}

.words-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  max-height: 320px;
  overflow-y: auto;
}

.word-item {
  display: flex;
  align-items: flex-start;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  background: var(--c-bg-body);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.word-item:hover {
  background: var(--c-primary-bg);
}

.word-item.completed {
  opacity: 0.6;
}

.word-item.completed .word-text {
  text-decoration: line-through;
}

.word-info {
  flex: 1;
  min-width: 0;
}

.word-main {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: 2px;
  flex-wrap: wrap;
}

.word-text {
  font-weight: 600;
  color: var(--c-text-primary);
  font-size: var(--font-size-md);
}

.word-phonetic {
  color: var(--c-text-secondary);
  font-family: serif;
  font-size: var(--font-size-sm);
}

.word-type-tag {
  font-size: 10px;
  padding: 0 6px;
  height: 18px;
  line-height: 16px;
}

.word-meaning {
  display: block;
  color: var(--c-text-secondary);
  font-size: var(--font-size-sm);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Schedules Section */
.empty-schedules {
  padding: var(--space-xl) 0;
}

.schedule-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--space-lg);
}

.schedule-card {
  border: 1px solid var(--c-border-light);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  background: var(--c-bg-card);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  transition: all var(--transition-normal);
}

.schedule-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.schedule-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.schedule-title-row {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  flex: 1;
  min-width: 0;
}

.schedule-name {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--c-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-stats {
  display: flex;
  gap: var(--space-md);
  padding: var(--space-md) 0;
  border-top: 1px solid var(--c-border-light);
  border-bottom: 1px solid var(--c-border-light);
}

.schedule-stat {
  flex: 1;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-num {
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--c-primary);
}

.stat-label {
  font-size: var(--font-size-xs);
  color: var(--c-text-secondary);
}

.schedule-progress-wrap {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-label {
  font-size: var(--font-size-sm);
  color: var(--c-text-secondary);
}

.progress-percent {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--c-text-primary);
}

.schedule-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
}

.schedule-date {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-xs);
  color: var(--c-text-secondary);
}

.schedule-date :deep(.el-icon) {
  color: var(--c-text-tertiary);
}

@media (max-width: 640px) {
  .today-card-progress {
    min-width: 120px;
  }
  
  .schedule-grid {
    grid-template-columns: 1fr;
  }
}
</style>
