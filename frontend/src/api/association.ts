import api from './request'
import type { 
  MemoryAssociation, 
  AssociationListResponse, 
  AssociationCreateRequest,
  AssociationUpvoteResponse 
} from '@/types'

export const associationApi = {
  getAssociations: (wordId: number): Promise<AssociationListResponse> => {
    return api.get(`/words/${wordId}/associations`)
  },

  createAssociation: (wordId: number, data: Partial<AssociationCreateRequest>): Promise<MemoryAssociation> => {
    return api.post(`/words/${wordId}/associations`, data)
  },

  upvoteAssociation: (id: number): Promise<AssociationUpvoteResponse> => {
    return api.post(`/associations/${id}/upvote`)
  }
}
