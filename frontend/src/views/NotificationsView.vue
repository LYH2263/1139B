<template>
  <div class="notifications-page page-container">
    <PageHeader
      title="消息通知"
      subtitle="查看您的所有通知消息，包括系统公告、复习提醒、成就达成和测验成绩。"
    />

    <SectionCard class="filter-section">
      <div class="filter-bar">
        <div class="filter-left">
          <el-radio-group v-model="filterRead" @change="handleFilterChange">
            <el-radio-button :label="null">全部</el-radio-button>
            <el-radio-button :label="false">
              未读
              <el-badge v-if="unreadCount > 0" :value="unreadCount" class="unread-badge" />
            </el-radio-button>
            <el-radio-button :label="true">已读</el-radio-button>
          </el-radio-group>
        </div>
        <div class="filter-right">
          <el-button
            type="primary"
            :disabled="selectedIds.length === 0"
            @click="handleMarkSelectedRead"
          >
            <el-icon><Check /></el-icon> 标记选中已读
          </el-button>
          <el-button
            type="success"
            :disabled="unreadCount === 0"
            @click="handleMarkAllRead"
          >
            <el-icon><Check /></el-icon> 全部标记已读
          </el-button>
        </div>
      </div>
    </SectionCard>

    <SectionCard class="table-section" :body-style="{ padding: '0' }">
      <el-table
        :data="notificationList"
        v-loading="loading"
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTagType(row.type)" size="small">
              {{ getTypeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="title" label="标题" min-width="180">
          <template #default="{ row }">
            <span class="notification-title" :class="{ unread: !row.read }">
              {{ row.title }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.read ? 'info' : 'warning'" size="small">
              {{ row.read ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="时间" width="180">
          <template #default="{ row }">
            <span class="date-text">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="!row.read"
              link
              type="primary"
              size="small"
              @click="handleMarkSingleRead(row)"
            >
              <el-icon><Check /></el-icon> 标记已读
            </el-button>
            <el-button link type="info" size="small" @click="showDetail(row)">
              <el-icon><View /></el-icon> 查看
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <EmptyState
            title="暂无通知消息"
            description="当有新的系统公告、复习提醒、成就达成或测验成绩时，会在这里显示。"
            :icon="Bell"
          />
        </template>
      </el-table>

      <div class="pagination-wrapper" v-if="pagination.total > 0">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </SectionCard>

    <el-dialog v-model="detailVisible" title="通知详情" width="500px">
      <div v-if="currentNotification" class="notification-detail">
        <div class="detail-header">
          <el-tag :type="getTagType(currentNotification.type)" size="default">
            {{ getTypeLabel(currentNotification.type) }}
          </el-tag>
          <span class="detail-time">{{ formatDate(currentNotification.createdAt) }}</span>
        </div>
        <h3 class="detail-title">{{ currentNotification.title }}</h3>
        <p class="detail-content">{{ currentNotification.content }}</p>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="currentNotification && !currentNotification.read"
          type="primary"
          @click="handleMarkSingleRead(currentNotification); detailVisible = false"
        >
          标记已读
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, View, Bell } from '@element-plus/icons-vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import SectionCard from '@/components/ui/SectionCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { notificationApi } from '@/api/notification'
import type { Notification } from '@/types'

const loading = ref(false)
const filterRead = ref<boolean | null>(null)
const notificationList = ref<Notification[]>([])
const selectedIds = ref<number[]>([])
const unreadCount = ref(0)
const detailVisible = ref(false)
const currentNotification = ref<Notification | null>(null)

const pagination = ref({
  page: 1,
  size: 20,
  total: 0
})

const fetchNotifications = async () => {
  loading.value = true
  try {
    const response = await notificationApi.getNotifications({
      page: pagination.value.page,
      size: pagination.value.size,
      read: filterRead.value ?? undefined
    })
    notificationList.value = response.list
    pagination.value.total = response.total
    unreadCount.value = response.unreadCount
  } catch (error) {
    ElMessage.error('获取通知列表失败')
  } finally {
    loading.value = false
  }
}

const handleFilterChange = () => {
  pagination.value.page = 1
  selectedIds.value = []
  fetchNotifications()
}

const handlePageChange = (page: number) => {
  pagination.value.page = page
  fetchNotifications()
}

const handleSizeChange = (size: number) => {
  pagination.value.size = size
  pagination.value.page = 1
  fetchNotifications()
}

const handleSelectionChange = (selection: Notification[]) => {
  selectedIds.value = selection
    .filter(n => !n.read)
    .map(n => n.id)
}

const handleMarkSingleRead = async (notification: Notification) => {
  try {
    await notificationApi.markAsRead(notification.id)
    notification.read = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
    selectedIds.value = selectedIds.value.filter(id => id !== notification.id)
    ElMessage.success('已标记为已读')
  } catch (error) {
    ElMessage.error('标记失败')
  }
}

const handleMarkSelectedRead = async () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请选择要标记的通知')
    return
  }
  try {
    await notificationApi.markSelectedAsRead({ ids: selectedIds.value })
    notificationList.value.forEach(n => {
      if (selectedIds.value.includes(n.id)) {
        n.read = true
      }
    })
    unreadCount.value = Math.max(0, unreadCount.value - selectedIds.value.length)
    selectedIds.value = []
    ElMessage.success('已标记选中为已读')
  } catch (error) {
    ElMessage.error('标记失败')
  }
}

const handleMarkAllRead = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要将所有未读通知标记为已读吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await notificationApi.markAllAsRead()
    notificationList.value.forEach(n => {
      n.read = true
    })
    unreadCount.value = 0
    selectedIds.value = []
    ElMessage.success('已全部标记为已读')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('标记失败')
    }
  }
}

const showDetail = (notification: Notification) => {
  currentNotification.value = notification
  detailVisible.value = true
  if (!notification.read) {
    handleMarkSingleRead(notification)
  }
}

const getTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    SYSTEM_ANNOUNCEMENT: '系统公告',
    REVIEW_REMINDER: '复习提醒',
    ACHIEVEMENT: '成就达成',
    QUIZ_RESULT: '测验成绩'
  }
  return labels[type] || '通知'
}

const getTagType = (type: string) => {
  const types: Record<string, string> = {
    SYSTEM_ANNOUNCEMENT: 'danger',
    REVIEW_REMINDER: 'primary',
    ACHIEVEMENT: 'warning',
    QUIZ_RESULT: 'success'
  }
  return types[type] || 'info'
}

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  fetchNotifications()
})
</script>

<style scoped>
.filter-section {
  margin-bottom: var(--space-md);
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-md);
}

.filter-left {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.filter-right {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.unread-badge {
  margin-left: 4px;
}

.notification-title {
  font-weight: 500;
  color: var(--c-text-primary);
}

.notification-title.unread {
  font-weight: 600;
  color: var(--c-primary);
}

.date-text {
  color: var(--c-text-secondary);
  font-size: var(--font-size-sm);
}

.table-section {
  margin-bottom: var(--space-md);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: var(--space-lg);
  border-top: 1px solid var(--c-border-light);
}

.notification-detail {
  padding: var(--space-md) 0;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-md);
}

.detail-time {
  color: var(--c-text-secondary);
  font-size: var(--font-size-sm);
}

.detail-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--c-text-primary);
  margin-bottom: var(--space-md);
}

.detail-content {
  font-size: 14px;
  line-height: 1.8;
  color: var(--c-text-secondary);
  background-color: var(--c-bg-body);
  padding: var(--space-md);
  border-radius: var(--radius-md);
}
</style>
