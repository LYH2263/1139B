import api from './request'
import type { MindMapResponse, TodayReviewResponse, ReviewRecord, QuizStartResponse, QuizSubmitResponse, StatsResponse, StudyPlan, LeaderboardResponse, Level, LevelStartResponse, LevelSubmitResponse } from '@/types'

export const mindMapApi = {
  getMindMap: (wordId: number, depth: number = 1): Promise<MindMapResponse> => {
    return api.get(`/mindmap/${wordId}`, { params: { depth } })
  }
}

export const reviewApi = {
  getTodayReviews: (): Promise<TodayReviewResponse> => {
    return api.get('/reviews/today')
  },

  submitReview: (wordId: number, result: string): Promise<ReviewRecord> => {
    return api.post('/reviews/submit', { wordId, result })
  }
}

export const quizApi = {
  startQuiz: (count: number = 10): Promise<QuizStartResponse> => {
    return api.post('/quiz/start', null, { params: { count } })
  },

  submitQuiz: (quizId: string, answers: { wordId: number; answer: string }[]): Promise<QuizSubmitResponse> => {
    return api.post('/quiz/submit', { quizId, answers })
  }
}

export const statsApi = {
  getStats: (): Promise<StatsResponse> => {
    return api.get('/stats/me')
  },

  getStudyPlans: (): Promise<StudyPlan[]> => {
    return api.get('/study-records')
  },

  createStudyPlan: (wordId: number, planType: string): Promise<StudyPlan> => {
    return api.post('/study-plans', { wordId, planType })
  }
}

export const leaderboardApi = {
  getLeaderboard: (dimension?: string): Promise<LeaderboardResponse> => {
    const params = dimension ? { dimension } : {}
    return api.get('/leaderboard', { params })
  }
}

export const levelApi = {
  getLevels: (): Promise<Level[]> => {
    return api.get('/levels')
  },

  startLevel: (levelId: number): Promise<LevelStartResponse> => {
    return api.post(`/levels/${levelId}/start`)
  },

  submitLevel: (levelId: number, sessionId: string, answers: { wordId: number; answer: string }[]): Promise<LevelSubmitResponse> => {
    return api.post(`/levels/${levelId}/submit`, { sessionId, answers })
  }
}
