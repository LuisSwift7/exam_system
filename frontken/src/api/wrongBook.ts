import { http } from './http'

export interface WrongBookItem {
  id: number
  studentId: number
  questionId: number
  examId: number
  wrongCount: number
  practiceCount: number
  practiceCorrectCount: number
  createTime: string
  updateTime: string
  // Question details
  questionContent: string
  questionType: number // 1: Single, 2: Multiple, 3: True/False
  questionOptions: string[]
  questionAnswer: string
  questionAnalysis: string
}

export interface WrongBookStats {
  totalCount: number
  practiceCount: number
  practiceAccuracy: number
  typeDistribution: Record<string, number>
}

export const wrongBookApi = {
  list: (params: { page: number; size: number; keyword?: string; type?: number }) =>
    http.get('/api/student/wrong-book', { params }),
  
  practice: (id: number, correct: boolean) =>
    http.post(`/api/student/wrong-book/${id}/practice`, { correct }),
  
  getStats: () =>
    http.get('/api/student/wrong-book/stats'),
}
