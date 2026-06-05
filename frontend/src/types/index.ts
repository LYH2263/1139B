export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  traceId?: string
}

export interface UserInfo {
  id: number
  username: string
  email?: string
  role: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  user: UserInfo
}

export interface RegisterRequest {
  username: string
  password: string
  email?: string
}

export interface Word {
  id: number
  word: string
  phonetic?: string
  pos?: string
  meaning: string
  example?: string
  memoryTip?: string
  createdAt?: string
}

export interface WordListResponse {
  list: Word[]
  total: number
  page: number
  size: number
}

export interface MindMapNode {
  id: number
  word: string
  meaning: string
  category?: string
  depth: number
}

export interface MindMapEdge {
  source: number
  target: number
  relationType: string
  label: string
}

export interface MindMapResponse {
  centerWord: MindMapNode
  nodes: MindMapNode[]
  edges: MindMapEdge[]
}

export interface ReviewRecord {
  id: number
  wordId: number
  word: string
  meaning: string
  result: string
  proficiency: number
  nextReviewAt?: string
  createdAt?: string
}

export interface TodayReviewResponse {
  list: ReviewRecord[]
  total: number
}

export interface QuizQuestion {
  wordId: number
  word: string
  type: string
  question: string
  options: string[]
  correctAnswer: string
}

export interface QuizStartResponse {
  quizId: string
  questions: QuizQuestion[]
}

export interface QuizSubmitResponse {
  score: number
  correctCount: number
  totalCount: number
  duration: number
  wrongWords: Word[]
}

export interface StatsResponse {
  totalWords: number
  todayReviewCount: number
  accuracy: number
  streakDays: number
}

export interface StudyPlan {
  id: number
  wordId: number
  word: string
  meaning: string
  planType: string
  createdAt: string
}

export interface Favorite {
  id: number
  wordId: number
  word: string
  phonetic?: string
  pos?: string
  meaning: string
  createdAt: string
}

export interface FavoriteListResponse {
  list: Favorite[]
  total: number
  page: number
  size: number
}

export interface FavoriteStatus {
  wordId: number
  isFavorite: boolean
}

export interface LeaderboardEntry {
  rank: number
  userId: number
  username: string
  value: number
}

export interface LeaderboardDimension {
  dimension: string
  topList: LeaderboardEntry[]
  currentUser: LeaderboardEntry | null
}

export interface LeaderboardResponse {
  masteredWords: LeaderboardDimension
  quizScore: LeaderboardDimension
  streakDays: LeaderboardDimension
}

export interface Note {
  id: number
  userId: number
  wordId: number
  word: string
  phonetic?: string
  pos?: string
  meaning: string
  content: string
  contentSummary: string
  createdAt: string
  updatedAt: string
}

export interface NoteListResponse {
  list: Note[]
  total: number
  page: number
  size: number
}

export interface LevelProgress {
  id: number
  bestScore: number
  stars: number
  completed: boolean
  attempts: number
  lastAttemptAt?: string
}

export interface Level {
  id: number
  name: string
  description: string
  difficulty: string
  passingScore: number
  order: number
  wordCount: number
  unlocked: boolean
  progress?: LevelProgress
}

export interface LevelStartResponse {
  sessionId: string
  questions: QuizQuestion[]
}

export interface LevelSubmitRequest {
  sessionId: string
  answers: { wordId: number; answer: string }[]
}

export interface LevelSubmitResponse {
  levelId: number
  score: number
  correctCount: number
  totalCount: number
  stars: number
  passed: boolean
  newlyCompleted: boolean
  duration: number
  wrongWords: Word[]
  progress: LevelProgress
}
