<template>
  <el-popover
    v-model:visible="popoverVisible"
    placement="bottom-end"
    :width="360"
    trigger="click"
    popper-class="notification-popover"
    @show="handlePopoverShow"
  >
    <template #reference>
      <div class="notification-bell" @click="togglePopover">
        <el-icon :size="20" class="bell-icon"><Bell /></el-icon>
        <span v-if="unreadCount > 0" class="unread-badge">
          {{ unreadCount > 99 ? '99+' : unreadCount }}
        </span>
      </div>
    </template>

    <div class="notification-panel">
      <div class="panel-header">
        <span class="panel-title">消息通知</span>
        <span v-if="unreadCount > 0" class="unread-text">{{ unreadCount }} 条未读</span>
      </div>

      <div class="notification-list">
        <div
          v-for="notification in recentNotifications"
          :key="notification.id"
          class="notification-item"
          :class="{ unread: !notification.read }"
          @click="handleNotificationClick(notification)"
        >
          <div class="notification-icon" :class="getTypeClass(notification.type)">
            <el-icon :size="16">
              <component :is="getTypeIcon(notification.type)" />
            </el-icon>
          </div>
          <div class="notification-content">
            <div class="notification-title">
              {{ notification.title }}
              <span class="notification-type">{{ getTypeLabel(notification.type) }}</span>
            </div>
            <div class="notification-body">{{ notification.content }}</div>
            <div class="notification-time">{{ formatTime(notification.createdAt) }}</div>
          </div>
          <div v-if="!notification.read" class="unread-dot"></div>
        </div>

        <div v-if="recentNotifications.length === 0" class="empty-state">
          <el-icon :size="48" class="empty-icon"><Bell /></el-icon>
          <p>暂无通知消息</p>
        </div>
      </div>

      <div class="panel-footer">
        <el-button type="primary" link @click="goToNotifications">
          查看全部 <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Bell,
  BellFilled,
  Trophy,
  Edit,
  Calendar,
  Warning,
  ArrowRight
} from '@element-plus/icons-vue'
import { notificationApi } from '@/api/notification'
import type { Notification } from '@/types'

const router = useRouter()
const popoverVisible = ref(false)
const unreadCount = ref(0)
const recentNotifications = ref<Notification[]>([])
let pollingTimer: number | null = null

const POLLING_INTERVAL = 30000

const fetchUnreadCount = async () => {
  try {
    const response = await notificationApi.getUnreadCount()
    unreadCount.value = response.count
  } catch (error) {
    console.error('Failed to fetch unread count:', error)
  }
}

const fetchRecentNotifications = async () => {
  try {
    const notifications = await notificationApi.getRecentNotifications({ limit: 5 })
    recentNotifications.value = notifications
  } catch (error) {
    console.error('Failed to fetch recent notifications:', error)
  }
}

const handlePopoverShow = () => {
  fetchRecentNotifications()
}

const togglePopover = () => {
  popoverVisible.value = !popoverVisible.value
}

const handleNotificationClick = async (notification: Notification) => {
  if (!notification.read) {
    try {
      await notificationApi.markAsRead(notification.id)
      notification.read = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch (error) {
      ElMessage.error('标记已读失败')
    }
  }
}

const goToNotifications = () => {
  popoverVisible.value = false
  router.push('/notifications')
}

const getTypeClass = (type: string) => {
  const classes: Record<string, string> = {
    SYSTEM_ANNOUNCEMENT: 'type-system',
    REVIEW_REMINDER: 'type-review',
    ACHIEVEMENT: 'type-achievement',
    QUIZ_RESULT: 'type-quiz'
  }
  return classes[type] || 'type-default'
}

const getTypeIcon = (type: string) => {
  const icons: Record<string, any> = {
    SYSTEM_ANNOUNCEMENT: Warning,
    REVIEW_REMINDER: Calendar,
    ACHIEVEMENT: Trophy,
    QUIZ_RESULT: Edit
  }
  return icons[type] || BellFilled
}

const getTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    SYSTEM_ANNOUNCEMENT: '系统',
    REVIEW_REMINDER: '复习',
    ACHIEVEMENT: '成就',
    QUIZ_RESULT: '测验'
  }
  return labels[type] || '通知'
}

const formatTime = (dateStr: string) => {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  if (hours < 24) return `${hours} 小时前`
  if (days < 7) return `${days} 天前`
  return date.toLocaleDateString()
}

const startPolling = () => {
  pollingTimer = window.setInterval(() => {
    fetchUnreadCount()
  }, POLLING_INTERVAL)
}

const stopPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

onMounted(() => {
  fetchUnreadCount()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})

defineExpose({
  refreshUnreadCount: fetchUnreadCount
})
</script>

<style scoped>
.notification-bell {
  position: relative;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color var(--transition-fast);
}

.notification-bell:hover {
  background-color: var(--c-bg-body);
}

.bell-icon {
  color: var(--c-text-secondary);
}

.unread-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  background-color: var(--c-danger);
  color: white;
  font-size: 11px;
  font-weight: 600;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.notification-panel {
  display: flex;
  flex-direction: column;
  max-height: 480px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--c-border-light);
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--c-text-primary);
}

.unread-text {
  font-size: 12px;
  color: var(--c-primary);
  font-weight: 500;
}

.notification-list {
  flex: 1;
  overflow-y: auto;
  max-height: 360px;
}

.notification-item {
  display: flex;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid var(--c-border-light);
  transition: background-color var(--transition-fast);
  position: relative;
}

.notification-item:hover {
  background-color: var(--c-bg-body);
}

.notification-item.unread {
  background-color: var(--c-primary-bg);
}

.notification-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  flex-shrink: 0;
}

.notification-icon.type-system {
  background-color: #fee2e2;
  color: var(--c-danger);
}

.notification-icon.type-review {
  background-color: #dbeafe;
  color: #2563eb;
}

.notification-icon.type-achievement {
  background-color: #fef3c7;
  color: #d97706;
}

.notification-icon.type-quiz {
  background-color: #d1fae5;
  color: #059669;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--c-text-primary);
  margin-bottom: 4px;
}

.notification-type {
  font-size: 10px;
  padding: 2px 6px;
  background-color: var(--c-bg-body);
  color: var(--c-text-secondary);
  border-radius: 4px;
  font-weight: 500;
}

.notification-body {
  font-size: 12px;
  color: var(--c-text-secondary);
  line-height: 1.5;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notification-time {
  font-size: 11px;
  color: var(--c-text-tertiary);
}

.unread-dot {
  width: 8px;
  height: 8px;
  background-color: var(--c-primary);
  border-radius: 50%;
  margin-left: 8px;
  flex-shrink: 0;
  align-self: flex-start;
  margin-top: 6px;
}

.empty-state {
  padding: 40px 20px;
  text-align: center;
  color: var(--c-text-tertiary);
}

.empty-icon {
  margin-bottom: 12px;
  opacity: 0.5;
}

.panel-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--c-border-light);
  text-align: center;
}

:deep(.notification-popover) {
  padding: 0;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
}
</style>
