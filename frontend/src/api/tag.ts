import api from './request'
import type { Tag, TagCreateRequest, TagUpdateRequest } from '@/types'

export const tagApi = {
  getTags: (): Promise<Tag[]> => {
    return api.get('/tags')
  },

  getTagById: (id: number): Promise<Tag> => {
    return api.get(`/tags/${id}`)
  },

  createTag: (data: TagCreateRequest): Promise<Tag> => {
    return api.post('/tags', data)
  },

  updateTag: (id: number, data: TagUpdateRequest): Promise<Tag> => {
    return api.put(`/tags/${id}`, data)
  },

  deleteTag: (id: number): Promise<void> => {
    return api.delete(`/tags/${id}`)
  },

  getPresetColors: (): Promise<string[]> => {
    return api.get('/tags/preset-colors')
  },

  getWordTags: (wordId: number): Promise<Tag[]> => {
    return api.get(`/words/${wordId}/tags`)
  },

  bindTagToWord: (wordId: number, tagId: number): Promise<Tag> => {
    return api.post(`/words/${wordId}/tags`, { tagId })
  },

  unbindTagFromWord: (wordId: number, tagId: number): Promise<void> => {
    return api.delete(`/words/${wordId}/tags/${tagId}`)
  }
}
