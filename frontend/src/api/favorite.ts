import api from './request'
import type { Favorite, FavoriteListResponse, FavoriteStatus } from '@/types'

export const favoriteApi = {
  getFavorites: (params?: { page?: number; size?: number }): Promise<FavoriteListResponse> => {
    return api.get('/favorites', { params })
  },

  getFavoriteWordIds: (): Promise<number[]> => {
    return api.get('/favorites/word-ids')
  },

  getFavoriteStatus: (wordId: number): Promise<FavoriteStatus> => {
    return api.get(`/favorites/status/${wordId}`)
  },

  addFavorite: (wordId: number): Promise<Favorite> => {
    return api.post('/favorites', { wordId })
  },

  removeFavorite: (wordId: number): Promise<void> => {
    return api.delete(`/favorites/${wordId}`)
  }
}
