<template>
  <div class="leaderboard-page page-container">
    <PageHeader 
      title="学习排行榜" 
      subtitle="与其他学习者一较高下，激励自己持续进步！"
    >
      <template #actions>
        <el-button type="primary" @click="fetchLeaderboard()" :loading="loading">
          <el-icon class="mr-1"><Refresh /></el-icon> 刷新数据
        </el-button>
      </template>
    </PageHeader>

    <div class="leaderboard-content">
      <el-tabs v-model="activeTab" class="leaderboard-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="掌握单词数" name="masteredWords">
          <div class="tab-content" v-loading="loading" element-loading-text="加载中...">
            <template v-if="!loading">
              <EmptyState 
                v-if="!masteredWordsData?.topList?.length" 
                title="暂无数据"
                description="还没有用户掌握足够的单词，快来成为第一名吧！"
                :icon="Trophy"
              />
              <div v-else class="table-wrapper">
                <el-table :data="masteredWordsData?.topList || []" class="leaderboard-table" stripe>
                  <el-table-column prop="rank" label="排名" width="100" align="center">
                    <template #default="{ row }">
                      <div class="rank-cell">
                        <span v-if="row.rank === 1" class="medal gold">🥇</span>
                        <span v-else-if="row.rank === 2" class="medal silver">🥈</span>
                        <span v-else-if="row.rank === 3" class="medal bronze">🥉</span>
                        <span v-else class="rank-number">{{ row.rank }}</span>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="username" label="用户名">
                    <template #default="{ row }">
                      <div class="username-cell" :class="{ 'current-user': isCurrentUser(row) }">
                        <el-avatar :size="32" class="user-avatar" :class="{ 'current-avatar': isCurrentUser(row) }">
                          {{ row.username?.charAt(0).toUpperCase() }}
                        </el-avatar>
                        <span>{{ row.username }}</span>
                        <el-tag v-if="isCurrentUser(row)" type="primary" size="small" class="self-tag">我</el-tag>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="value" label="掌握单词数" width="150" align="center">
                    <template #default="{ row }">
                      <span class="value-cell">{{ row.value }} 个</span>
                    </template>
                  </el-table-column>
                </el-table>

                <div v-if="showCurrentUserRow('masteredWords')" class="current-user-row">
                  <el-table :data="[masteredWordsData?.currentUser]" class="leaderboard-table current-user-table" :show-header="false">
                    <el-table-column prop="rank" width="100" align="center">
                      <template #default="{ row }">
                        <div class="rank-cell">
                          <span class="rank-number current-rank">{{ row?.rank }}</span>
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column prop="username">
                      <template #default="{ row }">
                        <div class="username-cell current-user">
                          <el-avatar :size="32" class="user-avatar current-avatar">
                            {{ row?.username?.charAt(0).toUpperCase() }}
                          </el-avatar>
                          <span>{{ row?.username }}</span>
                          <el-tag type="primary" size="small" class="self-tag">我</el-tag>
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column prop="value" width="150" align="center">
                      <template #default="{ row }">
                        <span class="value-cell">{{ row?.value }} 个</span>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </div>
            </template>
          </div>
        </el-tab-pane>

        <el-tab-pane label="测验最高分" name="quizScore">
          <div class="tab-content" v-loading="loading" element-loading-text="加载中...">
            <template v-if="!loading">
              <EmptyState 
                v-if="!quizScoreData?.topList?.length" 
                title="暂无数据"
                description="还没有用户完成测验，快来创造第一个高分记录！"
                :icon="Trophy"
              />
              <div v-else class="table-wrapper">
                <el-table :data="quizScoreData?.topList || []" class="leaderboard-table" stripe>
                  <el-table-column prop="rank" label="排名" width="100" align="center">
                    <template #default="{ row }">
                      <div class="rank-cell">
                        <span v-if="row.rank === 1" class="medal gold">🥇</span>
                        <span v-else-if="row.rank === 2" class="medal silver">🥈</span>
                        <span v-else-if="row.rank === 3" class="medal bronze">🥉</span>
                        <span v-else class="rank-number">{{ row.rank }}</span>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="username" label="用户名">
                    <template #default="{ row }">
                      <div class="username-cell" :class="{ 'current-user': isCurrentUser(row) }">
                        <el-avatar :size="32" class="user-avatar" :class="{ 'current-avatar': isCurrentUser(row) }">
                          {{ row.username?.charAt(0).toUpperCase() }}
                        </el-avatar>
                        <span>{{ row.username }}</span>
                        <el-tag v-if="isCurrentUser(row)" type="primary" size="small" class="self-tag">我</el-tag>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="value" label="最高分" width="150" align="center">
                    <template #default="{ row }">
                      <span class="value-cell">{{ row.value }} 分</span>
                    </template>
                  </el-table-column>
                </el-table>

                <div v-if="showCurrentUserRow('quizScore')" class="current-user-row">
                  <el-table :data="[quizScoreData?.currentUser]" class="leaderboard-table current-user-table" :show-header="false">
                    <el-table-column prop="rank" width="100" align="center">
                      <template #default="{ row }">
                        <div class="rank-cell">
                          <span class="rank-number current-rank">{{ row?.rank }}</span>
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column prop="username">
                      <template #default="{ row }">
                        <div class="username-cell current-user">
                          <el-avatar :size="32" class="user-avatar current-avatar">
                            {{ row?.username?.charAt(0).toUpperCase() }}
                          </el-avatar>
                          <span>{{ row?.username }}</span>
                          <el-tag type="primary" size="small" class="self-tag">我</el-tag>
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column prop="value" width="150" align="center">
                      <template #default="{ row }">
                        <span class="value-cell">{{ row?.value }} 分</span>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </div>
            </template>
          </div>
        </el-tab-pane>

        <el-tab-pane label="连续学习天数" name="streakDays">
          <div class="tab-content" v-loading="loading" element-loading-text="加载中...">
            <template v-if="!loading">
              <EmptyState 
                v-if="!streakDaysData?.topList?.length" 
                title="暂无数据"
                description="连续学习记录从今天开始，坚持就是胜利！"
                :icon="Trophy"
              />
              <div v-else class="table-wrapper">
                <el-table :data="streakDaysData?.topList || []" class="leaderboard-table" stripe>
                  <el-table-column prop="rank" label="排名" width="100" align="center">
                    <template #default="{ row }">
                      <div class="rank-cell">
                        <span v-if="row.rank === 1" class="medal gold">🥇</span>
                        <span v-else-if="row.rank === 2" class="medal silver">🥈</span>
                        <span v-else-if="row.rank === 3" class="medal bronze">🥉</span>
                        <span v-else class="rank-number">{{ row.rank }}</span>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="username" label="用户名">
                    <template #default="{ row }">
                      <div class="username-cell" :class="{ 'current-user': isCurrentUser(row) }">
                        <el-avatar :size="32" class="user-avatar" :class="{ 'current-avatar': isCurrentUser(row) }">
                          {{ row.username?.charAt(0).toUpperCase() }}
                        </el-avatar>
                        <span>{{ row.username }}</span>
                        <el-tag v-if="isCurrentUser(row)" type="primary" size="small" class="self-tag">我</el-tag>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="value" label="连续天数" width="150" align="center">
                    <template #default="{ row }">
                      <span class="value-cell">{{ row.value }} 天</span>
                    </template>
                  </el-table-column>
                </el-table>

                <div v-if="showCurrentUserRow('streakDays')" class="current-user-row">
                  <el-table :data="[streakDaysData?.currentUser]" class="leaderboard-table current-user-table" :show-header="false">
                    <el-table-column prop="rank" width="100" align="center">
                      <template #default="{ row }">
                        <div class="rank-cell">
                          <span class="rank-number current-rank">{{ row?.rank }}</span>
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column prop="username">
                      <template #default="{ row }">
                        <div class="username-cell current-user">
                          <el-avatar :size="32" class="user-avatar current-avatar">
                            {{ row?.username?.charAt(0).toUpperCase() }}
                          </el-avatar>
                          <span>{{ row?.username }}</span>
                          <el-tag type="primary" size="small" class="self-tag">我</el-tag>
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column prop="value" width="150" align="center">
                      <template #default="{ row }">
                        <span class="value-cell">{{ row?.value }} 天</span>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </div>
            </template>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Refresh, Trophy } from '@element-plus/icons-vue'
import type { LeaderboardResponse, LeaderboardDimension, LeaderboardEntry } from '@/types'
import { leaderboardApi } from '@/api/study'
import { useUserStore } from '@/stores/user'
import PageHeader from '@/components/ui/PageHeader.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

const userStore = useUserStore()
const activeTab = ref('masteredWords')
const loading = ref(false)
const leaderboardData = ref<LeaderboardResponse | null>(null)

const masteredWordsData = computed<LeaderboardDimension | null>(() => leaderboardData.value?.masteredWords || null)
const quizScoreData = computed<LeaderboardDimension | null>(() => leaderboardData.value?.quizScore || null)
const streakDaysData = computed<LeaderboardDimension | null>(() => leaderboardData.value?.streakDays || null)

const VALID_DIMENSIONS = ['masteredWords', 'quizScore', 'streakDays'] as const

const fetchLeaderboard = async (dimension?: unknown) => {
  loading.value = true
  try {
    let dim: string
    if (typeof dimension === 'string' && VALID_DIMENSIONS.includes(dimension as typeof VALID_DIMENSIONS[number])) {
      dim = dimension
    } else {
      dim = activeTab.value
    }
    leaderboardData.value = await leaderboardApi.getLeaderboard(dim)
  } catch (error) {
    console.error('Failed to fetch leaderboard:', error)
  } finally {
    loading.value = false
  }
}

const handleTabChange = (tabName: string) => {
  if (VALID_DIMENSIONS.includes(tabName as typeof VALID_DIMENSIONS[number])) {
    fetchLeaderboard(tabName)
  }
}

const isCurrentUser = (row: LeaderboardEntry): boolean => {
  return row?.userId === userStore.userInfo?.id
}

const isCurrentUserInTopList = (dimension: string): boolean => {
  const dimData = getDimensionData(dimension)
  if (!dimData?.topList || !userStore.userInfo) return false
  return dimData.topList.some(entry => entry.userId === userStore.userInfo!.id)
}

const showCurrentUserRow = (dimension: string): boolean => {
  const dimData = getDimensionData(dimension)
  if (!dimData?.currentUser || !userStore.userInfo) return false
  return !isCurrentUserInTopList(dimension) && dimData.currentUser.userId === userStore.userInfo.id
}

const getDimensionData = (dimension: string): LeaderboardDimension | null => {
  switch (dimension) {
    case 'masteredWords':
      return masteredWordsData.value
    case 'quizScore':
      return quizScoreData.value
    case 'streakDays':
      return streakDaysData.value
    default:
      return null
  }
}

onMounted(() => {
  fetchLeaderboard()
})
</script>

<style scoped>
.leaderboard-content {
  background: var(--c-bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: var(--space-lg);
}

.leaderboard-tabs :deep(.el-tabs__header) {
  margin-bottom: var(--space-lg);
}

.leaderboard-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: var(--c-border-light);
}

.leaderboard-tabs :deep(.el-tabs__item) {
  font-size: var(--font-size-base);
  font-weight: 500;
  color: var(--c-text-secondary);
  padding: 0 var(--space-xl);
}

.leaderboard-tabs :deep(.el-tabs__item.is-active) {
  color: var(--c-primary);
  font-weight: 600;
}

.leaderboard-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--c-primary);
  height: 3px;
}

.tab-content {
  min-height: 400px;
}

.table-wrapper {
  width: 100%;
}

.leaderboard-table {
  width: 100%;
}

.leaderboard-table :deep(.el-table__header th) {
  background-color: var(--c-bg-body);
  color: var(--c-text-primary);
  font-weight: 600;
  font-size: var(--font-size-sm);
}

.leaderboard-table :deep(.el-table__row:hover > td) {
  background-color: var(--c-primary-bg);
}

.rank-cell {
  display: flex;
  align-items: center;
  justify-content: center;
}

.medal {
  font-size: 24px;
  display: inline-block;
  animation: bounce 1s ease infinite;
}

.medal.gold {
  animation-delay: 0s;
}

.medal.silver {
  animation-delay: 0.2s;
}

.medal.bronze {
  animation-delay: 0.4s;
}

@keyframes bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-3px);
  }
}

.rank-number {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--c-bg-body);
  border-radius: var(--radius-full);
  font-weight: 600;
  color: var(--c-text-secondary);
  font-size: var(--font-size-sm);
}

.rank-number.current-rank {
  background: var(--c-primary);
  color: white;
}

.username-cell {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-xs) 0;
}

.username-cell.current-user {
  color: var(--c-primary);
  font-weight: 600;
}

.user-avatar {
  background-color: var(--c-primary-light);
  color: white;
  font-weight: 600;
  font-size: var(--font-size-sm);
}

.user-avatar.current-avatar {
  background-color: var(--c-primary);
  box-shadow: 0 0 0 2px var(--c-primary-bg);
}

.self-tag {
  margin-left: var(--space-xs);
  font-weight: 500;
}

.value-cell {
  font-weight: 600;
  color: var(--c-text-primary);
  font-size: var(--font-size-base);
}

.current-user-row {
  margin-top: var(--space-md);
  padding-top: var(--space-md);
  border-top: 2px dashed var(--c-primary-light);
}

.current-user-table :deep(.el-table__body tr) {
  background-color: var(--c-primary-bg);
}

.current-user-table :deep(.el-table__body tr:hover > td) {
  background-color: var(--c-primary-bg);
}

.mr-1 {
  margin-right: 4px;
}

/* Responsive */
@media (max-width: 768px) {
  .leaderboard-content {
    padding: var(--space-md);
  }

  .leaderboard-tabs :deep(.el-tabs__item) {
    padding: 0 var(--space-md);
    font-size: var(--font-size-sm);
  }

  .value-cell {
    font-size: var(--font-size-sm);
  }
}
</style>
