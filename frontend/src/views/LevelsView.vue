<template>
  <div class="levels-page page-container">
    <PageHeader title="闯关学习" subtitle="通过挑战关卡，逐步提升你的词汇量！" />

    <div class="levels-content" v-loading="loading">
      <!-- 1. Level Map -->
      <transition name="el-fade-in">
        <div v-if="!levelStarted" class="level-map">
          <div class="map-container">
            <svg class="path-svg" viewBox="0 0 800 600" preserveAspectRatio="xMidYMid meet">
              <defs>
                <linearGradient id="pathGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                  <stop offset="0%" style="stop-color:#6366f1;stop-opacity:0.3" />
                  <stop offset="100%" style="stop-color:#22c55e;stop-opacity:0.3" />
                </linearGradient>
              </defs>
              <path :d="pathD" fill="none" stroke="url(#pathGradient)" stroke-width="4" stroke-dasharray="8,8" />
            </svg>
            
            <div 
              v-for="(level, index) in levels" 
              :key="level.id"
              class="level-node"
              :class="{ 
                unlocked: level.unlocked, 
                locked: !level.unlocked,
                completed: level.progress?.completed 
              }"
              :style="getNodeStyle(index)"
              @click="handleLevelClick(level)"
            >
              <div class="node-circle">
                <span class="node-number">{{ level.order }}</span>
                <div v-if="level.progress?.completed" class="stars-container">
                  <el-icon 
                    v-for="i in 3" 
                    :key="i" 
                    class="star-icon"
                    :class="{ filled: i <= (level.progress?.stars || 0) }"
                  >
                    <Star />
                  </el-icon>
                </div>
                <el-icon v-if="!level.unlocked" class="lock-icon"><Lock /></el-icon>
              </div>
              <div class="node-info">
                <div class="node-name">{{ level.name }}</div>
                <div class="node-difficulty" :class="level.difficulty.toLowerCase()">
                  {{ getDifficultyLabel(level.difficulty) }}
                </div>
              </div>
              <div v-if="level.progress" class="node-progress">
                <span v-if="level.progress.bestScore !== null" class="best-score">
                  最高: {{ level.progress.bestScore }}分
                </span>
              </div>
            </div>
          </div>
        </div>
      </transition>

      <!-- 2. Level Quiz In Progress -->
      <transition name="el-fade-in">
        <div v-if="levelStarted && !levelFinished" class="level-session">
          <el-card class="question-card">
            <div class="quiz-header">
              <div class="quiz-level-info">
                <span class="level-badge">{{ currentLevel?.name }}</span>
                <span class="q-counter">Question {{ currentIndex + 1 }} / {{ questions.length }}</span>
              </div>
              <el-progress 
                :percentage="Math.round(((currentIndex + 1) / questions.length) * 100)" 
                :show-text="false"
                class="q-progress"
              />
            </div>

            <div class="question-body">
              <h1 class="target-word">{{ currentQuestion?.question }}</h1>
              <p class="instruction">请选择正确的中文释义：</p>
              
              <div class="options-list">
                <div 
                  v-for="option in currentQuestion?.options" 
                  :key="option"
                  class="option-item"
                  :class="{ selected: currentAnswer === option }"
                  @click="currentAnswer = option"
                >
                  <div class="radio-circle"></div>
                  <span class="option-text">{{ option }}</span>
                </div>
              </div>
            </div>

            <div class="quiz-footer">
              <el-button 
                type="primary" 
                size="large" 
                class="next-btn"
                :disabled="!currentAnswer"
                @click="nextQuestion"
              >
                {{ isLastQuestion ? '提交答卷' : '下一题' }}
              </el-button>
            </div>
          </el-card>
        </div>
      </transition>

      <!-- 3. Result Screen with Star Animation -->
      <transition name="el-fade-in">
        <div v-if="levelFinished" class="result-screen">
          <el-card class="result-card">
            <div class="score-display" :class="scoreLevelClass">
              <div class="score-circle">
                <span class="score-value">{{ levelResult?.score }}</span>
                <span class="score-label">分</span>
              </div>
              
              <div class="stars-display">
                <el-icon 
                  v-for="i in 3" 
                  :key="i" 
                  class="result-star"
                  :class="{ 
                    'star-filled': i <= (levelResult?.stars || 0),
                    'star-animate': showStarAnimation && i <= (levelResult?.stars || 0)
                  }"
                  :style="{ animationDelay: (i - 1) * 0.3 + 's' }"
                >
                  <Star />
                </el-icon>
              </div>

              <h2 class="result-title">{{ resultTitle }}</h2>
              <p class="result-subtitle">{{ resultSubtitle }}</p>
            </div>

            <div class="stats-row">
               <div class="stat-box">
                 <div class="val">{{ levelResult?.correctCount }}</div>
                 <div class="lbl">答对</div>
               </div>
               <div class="stat-box">
                 <div class="val">{{ levelResult?.totalCount }}</div>
                 <div class="lbl">总题数</div>
               </div>
               <div class="stat-box">
                 <div class="val">{{ levelResult?.duration }}s</div>
                 <div class="lbl">用时</div>
               </div>
            </div>

            <div v-if="levelResult?.newlyCompleted" class="unlock-banner">
              <el-icon><Unlock /></el-icon>
              <span>恭喜通关！下一关卡已解锁</span>
            </div>

            <el-divider v-if="levelResult?.wrongWords?.length">错题回顾</el-divider>
            
            <div v-if="levelResult?.wrongWords?.length" class="wrong-list">
              <div v-for="w in levelResult.wrongWords" :key="w.id" class="wrong-item">
                <span class="w-word">{{ w.word }}</span>
                <span class="w-meaning">{{ w.meaning }}</span>
              </div>
            </div>

            <div class="result-actions">
              <el-button @click="backToMap">返回关卡地图</el-button>
              <el-button v-if="!levelResult?.passed" type="primary" @click="retryLevel">再试一次</el-button>
              <el-button v-if="hasNextLevel && levelResult?.passed" type="primary" @click="nextLevel">下一关</el-button>
            </div>
          </el-card>
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Star, Lock, Unlock } from '@element-plus/icons-vue'
import type { Level, QuizQuestion, LevelSubmitResponse } from '@/types'
import { levelApi } from '@/api/study'
import PageHeader from '@/components/ui/PageHeader.vue'

const loading = ref(false)
const levels = ref<Level[]>([])
const levelStarted = ref(false)
const levelFinished = ref(false)
const currentLevel = ref<Level | null>(null)
const sessionId = ref('')
const questions = ref<QuizQuestion[]>([])
const currentIndex = ref(0)
const currentAnswer = ref('')
const answers = ref<{ wordId: number; answer: string }[]>([])
const levelResult = ref<LevelSubmitResponse | null>(null)
const showStarAnimation = ref(false)

const currentQuestion = computed(() => questions.value[currentIndex.value] || null)
const isLastQuestion = computed(() => currentIndex.value === questions.value.length - 1)

const hasNextLevel = computed(() => {
  if (!currentLevel.value) return false
  const nextOrder = currentLevel.value.order + 1
  return levels.value.some(l => l.order === nextOrder)
})

const scoreLevelClass = computed(() => {
  const s = levelResult.value?.score || 0
  if (s >= 90) return 'level-high'
  if (s >= 80) return 'level-mid'
  return 'level-low'
})

const resultTitle = computed(() => {
  if (!levelResult.value) return ''
  if (levelResult.value.stars === 3) return '完美通关！'
  if (levelResult.value.stars === 2) return '表现出色！'
  if (levelResult.value.stars === 1) return '顺利通过！'
  if (levelResult.value.passed) return '成功通关'
  return '继续努力'
})

const resultSubtitle = computed(() => {
  if (!levelResult.value) return ''
  if (levelResult.value.newlyCompleted) {
    return '你成功解锁了新的关卡，继续前进吧！'
  }
  if (levelResult.value.passed) {
    return '你的表现越来越好了，试试挑战更高星级吧！'
  }
  return '正确率需要达到 80% 才能通关，加油！'
})

const getNodeStyle = (index: number) => {
  const total = levels.value.length
  const cols = 5
  const col = index % cols
  const row = Math.floor(index / cols)
  const x = 100 + col * 140
  const y = 80 + row * 200
  return {
    left: x + 'px',
    top: y + 'px'
  }
}

const pathD = computed(() => {
  const total = levels.value.length
  if (total === 0) return ''
  const cols = 5
  let d = ''
  for (let i = 0; i < total; i++) {
    const col = i % cols
    const row = Math.floor(i / cols)
    const x = 140 + col * 140
    const y = 140 + row * 200
    if (i === 0) {
      d += `M ${x} ${y}`
    } else {
      const prevCol = (i - 1) % cols
      const prevRow = Math.floor((i - 1) / cols)
      const prevX = 140 + prevCol * 140
      const prevY = 140 + prevRow * 200
      d += ` L ${x} ${y}`
    }
  }
  return d
})

const getDifficultyLabel = (difficulty: string) => {
  const map: Record<string, string> = {
    EASY: '简单',
    MEDIUM: '中等',
    HARD: '困难'
  }
  return map[difficulty] || difficulty
}

const loadLevels = async () => {
  loading.value = true
  try {
    levels.value = await levelApi.getLevels()
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleLevelClick = (level: Level) => {
  if (!level.unlocked) {
    ElMessage.warning('该关卡尚未解锁，请先通过前一关')
    return
  }
  currentLevel.value = level
  startLevel()
}

const startLevel = async () => {
  if (!currentLevel.value) return
  loading.value = true
  try {
    const res = await levelApi.startLevel(currentLevel.value.id)
    sessionId.value = res.sessionId
    questions.value = res.questions
    levelStarted.value = true
    levelFinished.value = false
    currentIndex.value = 0
    currentAnswer.value = ''
    answers.value = []
    levelResult.value = null
    showStarAnimation.value = false
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const nextQuestion = () => {
  if (!currentQuestion.value || !currentAnswer.value) return
  
  answers.value.push({
    wordId: currentQuestion.value.wordId,
    answer: currentAnswer.value
  })
  
  if (isLastQuestion.value) {
    submitLevel()
  } else {
    currentIndex.value++
    currentAnswer.value = ''
  }
}

const submitLevel = async () => {
  if (!currentLevel.value) return
  loading.value = true
  try {
    const res = await levelApi.submitLevel(
      currentLevel.value.id, 
      sessionId.value, 
      answers.value
    )
    levelResult.value = res
    levelFinished.value = true
    
    setTimeout(() => {
      showStarAnimation.value = true
    }, 300)
    
    if (res.passed) {
      ElMessage.success(res.newlyCompleted ? '恭喜通关！' : '挑战成功！')
    } else {
      ElMessage.info('未能通关，再试一次吧！')
    }
    
    await loadLevels()
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const retryLevel = () => {
  levelFinished.value = false
  startLevel()
}

const nextLevel = () => {
  if (!currentLevel.value) return
  const nextOrder = currentLevel.value.order + 1
  const next = levels.value.find(l => l.order === nextOrder)
  if (next) {
    currentLevel.value = next
    levelFinished.value = false
    startLevel()
  }
}

const backToMap = () => {
  levelStarted.value = false
  levelFinished.value = false
  currentLevel.value = null
  sessionId.value = ''
  questions.value = []
  currentIndex.value = 0
  currentAnswer.value = ''
  answers.value = []
  levelResult.value = null
  showStarAnimation.value = false
}

onMounted(() => {
  loadLevels()
})
</script>

<style scoped>
.levels-content {
  max-width: 900px;
  margin: 0 auto;
}

/* Level Map */
.level-map {
  padding: var(--space-lg) 0;
}

.map-container {
  position: relative;
  min-height: 500px;
  background: linear-gradient(135deg, #f0f9ff 0%, #f0fdf4 100%);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  overflow: hidden;
}

.path-svg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.level-node {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  transition: transform 0.3s ease;
  z-index: 1;
}

.level-node:hover {
  transform: scale(1.1);
}

.level-node.locked {
  cursor: not-allowed;
  opacity: 0.5;
}

.level-node.locked:hover {
  transform: none;
}

.node-circle {
  width: 70px;
  height: 70px;
  border-radius: 50%;
  background: white;
  border: 3px solid var(--c-border);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  box-shadow: var(--shadow-md);
  transition: all 0.3s ease;
}

.level-node.unlocked .node-circle {
  border-color: var(--c-primary);
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);
}

.level-node.completed .node-circle {
  border-color: var(--c-success);
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
}

.node-number {
  font-size: 24px;
  font-weight: 700;
  color: var(--c-text-primary);
}

.level-node.unlocked .node-number {
  color: var(--c-primary);
}

.level-node.completed .node-number {
  color: var(--c-success);
}

.stars-container {
  position: absolute;
  bottom: -12px;
  display: flex;
  gap: 2px;
}

.star-icon {
  font-size: 14px;
  color: #d1d5db;
}

.star-icon.filled {
  color: #fbbf24;
}

.lock-icon {
  position: absolute;
  font-size: 20px;
  color: #9ca3af;
}

.node-info {
  margin-top: var(--space-md);
  text-align: center;
}

.node-name {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--c-text-primary);
  white-space: nowrap;
}

.node-difficulty {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  margin-top: 4px;
  display: inline-block;
}

.node-difficulty.easy {
  background: #dcfce7;
  color: #16a34a;
}

.node-difficulty.medium {
  background: #fef3c7;
  color: #d97706;
}

.node-difficulty.hard {
  background: #fee2e2;
  color: #dc2626;
}

.node-progress {
  margin-top: 4px;
}

.best-score {
  font-size: 11px;
  color: var(--c-text-secondary);
}

/* Quiz Session */
.level-session {
  max-width: 700px;
  margin: 0 auto;
}

.question-card {
  min-height: 500px;
  display: flex;
  flex-direction: column;
}

.quiz-header {
  margin-bottom: var(--space-lg);
}

.quiz-level-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.level-badge {
  background: var(--c-primary);
  color: white;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: var(--font-size-sm);
  font-weight: 600;
}

.q-counter {
  font-size: var(--font-size-xs);
  color: var(--c-text-tertiary);
}

.question-body {
  flex: 1;
}

.target-word {
  font-size: 36px;
  color: var(--c-text-primary);
  text-align: center;
  margin-bottom: var(--space-md);
}

.instruction {
  text-align: center;
  color: var(--c-text-secondary);
  margin-bottom: var(--space-lg);
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.option-item {
  display: flex;
  align-items: center;
  padding: var(--space-md) var(--space-lg);
  border: 2px solid var(--c-border-light);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
}

.option-item:hover {
  border-color: var(--c-primary-light);
  background: var(--c-bg-body);
}

.option-item.selected {
  border-color: var(--c-primary);
  background: var(--c-primary-bg);
}

.radio-circle {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid var(--c-border);
  margin-right: var(--space-md);
  flex-shrink: 0;
}

.selected .radio-circle {
  border-color: var(--c-primary);
  background: var(--c-primary);
  box-shadow: inset 0 0 0 4px white;
}

.option-text {
  font-size: var(--font-size-base);
  color: var(--c-text-primary);
}

.quiz-footer {
  margin-top: var(--space-xl);
  text-align: center;
}

.next-btn {
  width: 100%;
}

/* Result Screen */
.result-screen {
  max-width: 700px;
  margin: 0 auto;
  text-align: center;
}

.score-display {
  padding: var(--space-lg);
  background: var(--c-bg-body);
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-lg);
}

.score-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: 8px solid currentColor;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto var(--space-md);
}

.score-value {
  font-size: 48px;
  font-weight: 800;
  line-height: 1;
}

.score-label {
  font-size: 16px;
  margin-top: 12px;
  margin-left: 2px;
}

.score-display.level-high { color: var(--c-success); }
.score-display.level-mid { color: var(--c-warning); }
.score-display.level-low { color: var(--c-danger); }

.stars-display {
  display: flex;
  justify-content: center;
  gap: var(--space-md);
  margin-bottom: var(--space-md);
}

.result-star {
  font-size: 40px;
  color: #d1d5db;
  opacity: 0;
  transform: scale(0);
}

.result-star.star-filled {
  color: #fbbf24;
}

.result-star.star-animate {
  animation: starPop 0.6s ease forwards;
}

@keyframes starPop {
  0% {
    opacity: 0;
    transform: scale(0) rotate(-180deg);
  }
  50% {
    transform: scale(1.3) rotate(10deg);
  }
  100% {
    opacity: 1;
    transform: scale(1) rotate(0deg);
  }
}

.result-title {
  margin: 0 0 8px;
  font-size: 28px;
}

.result-subtitle {
  color: var(--c-text-secondary);
  margin: 0;
}

.stats-row {
  display: flex;
  justify-content: space-around;
  margin-bottom: var(--space-lg);
}

.stat-box .val {
  font-size: 24px;
  font-weight: 700;
  color: var(--c-text-primary);
}

.stat-box .lbl {
  font-size: 12px;
  color: var(--c-text-tertiary);
}

.unlock-banner {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #92400e;
  padding: var(--space-md);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: var(--space-lg);
  font-weight: 600;
}

.wrong-list {
  text-align: left;
  background: #fef2f2;
  padding: var(--space-md);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-lg);
}

.wrong-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid rgba(0,0,0,0.05);
}

.w-word { font-weight: 600; color: var(--c-danger); }
.w-meaning { color: var(--c-text-secondary); }

.result-actions {
  display: flex;
  justify-content: center;
  gap: var(--space-md);
}

/* Responsive */
@media (max-width: 640px) {
  .map-container {
    min-height: 400px;
  }
  
  .node-circle {
    width: 50px;
    height: 50px;
  }
  
  .node-number {
    font-size: 18px;
  }
  
  .node-name {
    font-size: 11px;
  }
  
  .target-word { font-size: 28px; }
  .result-title { font-size: 22px; }
  
  .level-node {
    transform: scale(0.8);
    transform-origin: top center;
  }
  
  .level-node:hover {
    transform: scale(0.9);
  }
}
</style>
