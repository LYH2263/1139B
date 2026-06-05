import api from './request'
import type {
  Notification,
  NotificationListResponse,
  UnreadCountResponse,
  BroadcastRequest,
  MarkReadRequest
} from '@/types'

export const notificationApi = {
  getNotifications: (params?: {
    page?: number
    size?: number
    read?: boolean
  }): Promise<NotificationListResponse> => {
    return api.get('/notifications', { params })
  },

  getRecentNotifications: (params?: { limit?: number }): Promise<Notification[]> => {
    return api.get('/notifications/recent', { params })
  },

  getUnreadCount: (): Promise<UnreadCountResponse> => {
    return api.get('/notifications/unread-count')
  },

  markAsRead: (id: number): Promise<void> => {
    return api.put(`/notifications/${id}/read`)
  },

  markAllAsRead: (): Promise<void> => {
    return api.put('/notifications/read-all')
  },

  markSelectedAsRead: (data: MarkReadRequest): Promise<void> => {
    return api.put('/notifications/read-selected', data)
  },

  broadcastNotification: (data: BroadcastRequest): Promise<{ sentCount: number }> => {
    return api.post('/admin/notifications/broadcast', data)
  }
}
