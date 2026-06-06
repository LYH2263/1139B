import api from './request'
import type {
  ScheduleListResponse,
  StudySchedule,
  ScheduleTodayResponse,
  ScheduleDetailResponse,
  CreateScheduleRequest
} from '@/types'

export const scheduleApi = {
  getSchedules: (): Promise<ScheduleListResponse> => {
    return api.get('/schedules')
  },

  getSchedule: (id: number): Promise<StudySchedule> => {
    return api.get(`/schedules/${id}`)
  },

  getScheduleDetail: (id: number): Promise<ScheduleDetailResponse> => {
    return api.get(`/schedules/${id}/detail`)
  },

  createSchedule: (data: CreateScheduleRequest): Promise<StudySchedule> => {
    return api.post('/schedules', data)
  },

  updateSchedule: (id: number, data: CreateScheduleRequest & { status?: string }): Promise<StudySchedule> => {
    return api.put(`/schedules/${id}`, data)
  },

  deleteSchedule: (id: number): Promise<void> => {
    return api.delete(`/schedules/${id}`)
  },

  getTodaySchedule: (id: number): Promise<ScheduleTodayResponse> => {
    return api.get(`/schedules/${id}/today`)
  },

  completeToday: (id: number, completedWordIds?: number[]): Promise<ScheduleTodayResponse> => {
    return api.post(`/schedules/${id}/complete`, completedWordIds ? { completedWordIds } : {})
  }
}
