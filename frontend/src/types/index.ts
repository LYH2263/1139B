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

export type NotificationType = 'SYSTEM_ANNOUNCEMENT' | 'REVIEW_REMINDER' | 'ACHIEVEMENT' | 'QUIZ_RESULT'

export interface Notification {
  id: number
  userId: number
  type: NotificationType
  title: string
  content: string
  read: boolean
  createdAt: string
}

export interface NotificationListResponse {
  list: Notification[]
  total: number
  unreadCount: number
  page: number
  size: number
}

export interface UnreadCountResponse {
  count: number
}

export interface BroadcastRequest {
  title: string
  content: string
}

export interface MarkReadRequest {
  ids: number[]
}

export interface MemoryAssociation {
  id: number
  wordId: number
  type: string
  content: string
  upvotes: number
  createdBy: string
  isSystemGenerated: boolean
  createdAt: string
}

export interface AssociationListResponse {
  list: MemoryAssociation[]
  total: number
}

export interface AssociationCreateRequest {
  wordId: number
  type: string
  content: string
}

export interface AssociationUpvoteResponse {
  id: number
  upvotes: number
}

export interface SearchSuggestion {
  id: number
  word: string
  meaning: string
  matchType: string
}

export interface SearchHistoryItem {
  id: number
  keyword: string
  searchedAt: string
}

export interface HotKeyword {
  keyword: string
  count: number
}

export interface SearchResponse {
  list: Word[]
  total: number
  page: number
  size: number
}

export interface StudySchedule {
  id: number
  userId: number
  name: string
  targetWordIds: number[]
  dailyCount: number
  startDate: string
  endDate: string
  status: string
  createdAt: string
  totalDays: number
  completedDays: number
  totalWords: number
  learnedWords: number
  progressPercent: number
}

export interface ScheduleWordItem {
  wordId: number
  word: string
  phonetic?: string
  meaning: string
  example?: string
  type: 'NEW' | 'REVIEW'
  isCompleted: boolean
}

export interface ScheduleTodayResponse {
  scheduleId: number
  scheduleName: string
  date: string
  plannedWords: ScheduleWordItem[]
  completedWords: ScheduleWordItem[]
  newWords: ScheduleWordItem[]
  reviewWords: ScheduleWordItem[]
  totalCount: number
  completedCount: number
  isCompleted: boolean
}

export interface ScheduleProgressDetail {
  date: string
  plannedWordIds: number[]
  completedWordIds: number[]
  plannedCount: number
  completedCount: number
  isCompleted: boolean
}

export interface ScheduleDetailResponse {
  schedule: StudySchedule
  progressList: ScheduleProgressDetail[]
  ganttData: number[]
}

export interface ScheduleListResponse {
  list: StudySchedule[]
  total: number
}

export interface CreateScheduleRequest {
  name: string
  targetWordIds: number[]
  dailyCount: number
  startDate: string
  endDate: string
}

export interface Tag {
  id: number
  name: string
  color: string
  createdAt?: string
  wordCount?: number
}

export interface TagCreateRequest {
  name: string
  color: string
}

export interface TagUpdateRequest {
  name?: string
  color?: string
}

export interface WordTagBindRequest {
  tagId: number
}
