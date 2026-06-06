import api from './request'
import type { SearchSuggestion, SearchHistoryItem, HotKeyword, SearchResponse } from '@/types'

export const searchApi = {
  searchWords: (params?: { keyword?: string; pos?: string; tagIds?: number[]; page?: number; size?: number }): Promise<SearchResponse> => {
    return api.get('/search', { params })
  },

  getSuggestions: (keyword: string): Promise<SearchSuggestion[]> => {
    return api.get('/search/suggestions', { params: { keyword } })
  },

  getSearchHistory: (): Promise<SearchHistoryItem[]> => {
    return api.get('/search/history')
  },

  deleteSearchHistory: (id?: number): Promise<void> => {
    if (id !== undefined) {
      return api.delete(`/search/history/${id}`)
    }
    return api.delete('/search/history')
  },

  deleteSearchHistoryById: (id: number): Promise<void> => {
    return api.delete(`/search/history/${id}`)
  },

  getHotKeywords: (): Promise<HotKeyword[]> => {
    return api.get('/search/hot')
  }
}
