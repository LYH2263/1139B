import api from './request'
import type { Note, NoteListResponse } from '@/types'

export const noteApi = {
  getNotes: (params?: { page?: number; size?: number; keyword?: string }): Promise<NoteListResponse> => {
    return api.get('/notes', { params })
  },

  getNoteByWordId: (wordId: number): Promise<Note | null> => {
    return api.get(`/words/${wordId}/notes`)
  },

  createNote: (wordId: number, content: string): Promise<Note> => {
    return api.post(`/words/${wordId}/notes`, { content })
  },

  updateNote: (wordId: number, content: string): Promise<Note> => {
    return api.put(`/words/${wordId}/notes`, { content })
  },

  deleteNote: (wordId: number): Promise<void> => {
    return api.delete(`/words/${wordId}/notes`)
  }
}
