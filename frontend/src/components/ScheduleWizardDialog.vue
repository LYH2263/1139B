<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="680px"
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <!-- 步骤指示器 -->
    <div class="wizard-steps">
      <el-steps :active="currentStep" finish-status="success" align-center>
        <el-step title="选择单词" icon="Document" />
        <el-step title="设置数量" icon="Coin" />
        <el-step title="选择日期" icon="Calendar" />
        <el-step title="确认创建" icon="CircleCheck" />
      </el-steps>
    </div>

    <!-- 步骤1: 选择单词 -->
    <div v-show="currentStep === 0" class="step-content">
      <div class="step-search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索单词或释义..."
          clearable
          :prefix-icon="Search"
          size="large"
          @input="handleSearch"
        />
      </div>
      <div class="word-list-wrapper">
        <el-table
          ref="wordTableRef"
          :data="filteredWords"
          height="320"
          @selection-change="handleWordSelectionChange"
          row-key="id"
        >
          <el-table-column type="selection" width="50" reserve-selection />
          <el-table-column prop="word" label="单词" width="140" />
          <el-table-column prop="phonetic" label="音标" width="120" />
          <el-table-column prop="meaning" label="释义" show-overflow-tooltip />
        </el-table>
      </div>
      <div class="selection-info">
        <span>已选择 <strong>{{ selectedWords.length }}</strong> 个单词</span>
      </div>
    </div>

    <!-- 步骤2: 设置数量 -->
    <div v-show="currentStep === 1" class="step-content">
      <div class="form-row">
        <el-form label-position="top">
          <el-form-item label="计划名称">
            <el-input v-model="formData.name" placeholder="例如：四级核心词汇" size="large" />
          </el-form-item>
        </el-form>
      </div>
      <div class="form-row">
        <el-form label-position="top">
          <el-form-item :label="`每日学习单词数（当前共 ${selectedWords.length} 个单词）`">
            <el-input-number
              v-model="formData.dailyCount"
              :min="1"
              :max="Math.max(selectedWords.length, 1)"
              size="large"
              :step="5"
              style="width: 100%"
            />
          </el-form-item>
        </el-form>
      </div>
      <div class="preview-calc">
        <SectionCard title="预估学习进度">
          <div class="calc-grid">
            <div class="calc-item">
              <span class="calc-label">总单词数</span>
              <span class="calc-value">{{ selectedWords.length }}</span>
            </div>
            <div class="calc-item">
              <span class="calc-label">每日学习</span>
              <span class="calc-value">{{ formData.dailyCount }} 个</span>
            </div>
            <div class="calc-item">
              <span class="calc-label">预计天数</span>
              <span class="calc-value">{{ estimatedDays }} 天</span>
            </div>
          </div>
          <div class="calc-tip">
            <el-icon><InfoFilled /></el-icon>
            <span>系统将按艾宾浩斯遗忘曲线自动安排复习，实际每天学习量包含新词和复习词。</span>
          </div>
        </SectionCard>
      </div>
    </div>

    <!-- 步骤3: 选择日期 -->
    <div v-show="currentStep === 2" class="step-content">
      <div class="form-row">
        <el-form label-position="top">
          <el-form-item label="学习时间范围">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              size="large"
              value-format="YYYY-MM-DD"
              :disabled-date="disabledDate"
              style="width: 100%"
            />
          </el-form-item>
        </el-form>
      </div>
      <div class="preview-calc" v-if="dateRange && dateRange.length === 2">
        <SectionCard title="时间安排预览">
          <div class="calc-grid">
            <div class="calc-item">
              <span class="calc-label">开始日期</span>
              <span class="calc-value">{{ dateRange[0] }}</span>
            </div>
            <div class="calc-item">
              <span class="calc-label">结束日期</span>
              <span class="calc-value">{{ dateRange[1] }}</span>
            </div>
            <div class="calc-item">
              <span class="calc-label">总天数</span>
              <span class="calc-value">{{ totalDays }} 天</span>
            </div>
          </div>
          <div v-if="dailyWordsPerDay" class="calc-tip warning">
            <el-icon><Warning /></el-icon>
            <span>按此安排，每天需学习约 {{ dailyWordsPerDay }} 个新词（复习词另外安排）。</span>
          </div>
        </SectionCard>
      </div>
    </div>

    <!-- 步骤4: 确认创建 -->
    <div v-show="currentStep === 3" class="step-content">
      <div class="confirm-section">
        <SectionCard title="计划概览">
          <div class="confirm-grid">
            <div class="confirm-item">
              <span class="confirm-label">计划名称</span>
              <span class="confirm-value">{{ formData.name || '未命名计划' }}</span>
            </div>
            <div class="confirm-item">
              <span class="confirm-label">目标单词</span>
              <span class="confirm-value">{{ selectedWords.length }} 个</span>
            </div>
            <div class="confirm-item">
              <span class="confirm-label">每日学习量</span>
              <span class="confirm-value">{{ formData.dailyCount }} 个/天</span>
            </div>
            <div class="confirm-item">
              <span class="confirm-label">学习周期</span>
              <span class="confirm-value">{{ dateRange?.[0] || '-' }} 至 {{ dateRange?.[1] || '-' }}</span>
            </div>
          </div>
        </SectionCard>

        <SectionCard title="学习模式说明">
          <div class="mode-list">
            <div class="mode-item">
              <div class="mode-icon new"><el-icon><Star /></el-icon></div>
              <div class="mode-text">
                <strong>新词学习</strong>
                <span>每天按设定数量安排新单词学习</span>
              </div>
            </div>
            <div class="mode-item">
              <div class="mode-icon review"><el-icon><Refresh /></el-icon></div>
              <div class="mode-text">
                <strong>智能复习</strong>
                <span>按艾宾浩斯曲线：第1、2、4、7、15、30天自动安排复习</span>
              </div>
            </div>
          </div>
        </SectionCard>
      </div>
    </div>

    <!-- 底部操作按钮 -->
    <template #footer>
      <div class="wizard-footer">
        <el-button @click="handlePrev" :disabled="currentStep === 0 || loading">
          上一步
        </el-button>
        <el-button
          v-if="currentStep < 3"
          type="primary"
          @click="handleNext"
          :disabled="!canProceed || loading"
        >
          下一步
        </el-button>
        <el-button
          v-else
          type="primary"
          @click="handleConfirm"
          :loading="loading"
        >
          创建计划
        </el-button>
        <el-button @click="visible = false" :disabled="loading">取消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search,
  InfoFilled,
  Warning,
  Star,
  Refresh
} from '@element-plus/icons-vue'
import type { Word } from '@/types'
import { wordApi } from '@/api/word'
import { scheduleApi } from '@/api/schedule'
import SectionCard from '@/components/ui/SectionCard.vue'

interface Props {
  modelValue: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const dialogTitle = '创建学习计划'
const currentStep = ref(0)
const loading = ref(false)
const searchKeyword = ref('')
const allWords = ref<Word[]>([])
const selectedWords = ref<Word[]>([])
const wordTableRef = ref()

const formData = reactive({
  name: '',
  dailyCount: 20
})

const dateRange = ref<string[]>([])

const filteredWords = computed(() => {
  if (!searchKeyword.value) return allWords.value
  const kw = searchKeyword.value.toLowerCase()
  return allWords.value.filter(
    (w) =>
      w.word.toLowerCase().includes(kw) ||
      w.meaning.toLowerCase().includes(kw)
  )
})

const estimatedDays = computed(() => {
  if (selectedWords.value.length === 0 || formData.dailyCount === 0) return 0
  return Math.ceil(selectedWords.value.length / formData.dailyCount)
})

const totalDays = computed(() => {
  if (!dateRange.value || dateRange.value.length !== 2) return 0
  const start = new Date(dateRange.value[0])
  const end = new Date(dateRange.value[1])
  return Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1
})

const dailyWordsPerDay = computed(() => {
  if (totalDays.value === 0 || selectedWords.value.length === 0) return 0
  return Math.ceil(selectedWords.value.length / totalDays.value)
})

const canProceed = computed(() => {
  switch (currentStep.value) {
    case 0:
      return selectedWords.value.length > 0
    case 1:
      return formData.name.trim() !== '' && formData.dailyCount > 0
    case 2:
      return dateRange.value && dateRange.value.length === 2
    default:
      return true
  }
})

const disabledDate = (time: Date) => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return time.getTime() < today.getTime() - 24 * 60 * 60 * 1000
}

const loadWords = async () => {
  try {
    const res = await wordApi.getWords({ size: 500 })
    allWords.value = res.list
  } catch (error) {
    console.error(error)
  }
}

const handleSearch = () => {
  // handled by computed
}

const handleWordSelectionChange = (selection: Word[]) => {
  selectedWords.value = selection
}

const handleNext = () => {
  if (currentStep.value < 3) {
    currentStep.value++
  }
}

const handlePrev = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

const handleConfirm = async () => {
  if (!formData.name.trim()) {
    ElMessage.warning('请输入计划名称')
    return
  }
  if (selectedWords.value.length === 0) {
    ElMessage.warning('请选择要学习的单词')
    return
  }
  if (!dateRange.value || dateRange.value.length !== 2) {
    ElMessage.warning('请选择学习时间范围')
    return
  }

  loading.value = true
  try {
    await scheduleApi.createSchedule({
      name: formData.name.trim(),
      targetWordIds: selectedWords.value.map((w) => w.id),
      dailyCount: formData.dailyCount,
      startDate: dateRange.value[0],
      endDate: dateRange.value[1]
    })
    ElMessage.success('学习计划创建成功！')
    emit('success')
    visible.value = false
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleClosed = () => {
  currentStep.value = 0
  formData.name = ''
  formData.dailyCount = 20
  dateRange.value = []
  selectedWords.value = []
  searchKeyword.value = ''
}

watch(visible, (val) => {
  if (val) {
    loadWords()
    const today = new Date()
    const endDate = new Date()
    endDate.setDate(endDate.getDate() + 30)
    dateRange.value = [
      today.toISOString().split('T')[0],
      endDate.toISOString().split('T')[0]
    ]
  }
})

onMounted(() => {
  // lazy load on open
})
</script>

<style scoped>
.wizard-steps {
  margin-bottom: var(--space-xl);
  padding: 0 var(--space-md);
}

.step-content {
  min-height: 380px;
  display: flex;
  flex-direction: column;
}

.step-search {
  margin-bottom: var(--space-md);
}

.word-list-wrapper {
  flex: 1;
  border: 1px solid var(--c-border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.selection-info {
  margin-top: var(--space-sm);
  text-align: right;
  color: var(--c-text-secondary);
  font-size: var(--font-size-sm);
}

.selection-info strong {
  color: var(--c-primary);
  font-size: var(--font-size-lg);
}

.form-row {
  margin-bottom: var(--space-lg);
}

.preview-calc {
  margin-top: var(--space-lg);
}

.calc-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-md);
  margin-bottom: var(--space-md);
}

.calc-item {
  text-align: center;
  padding: var(--space-sm);
  background: var(--c-bg-body);
  border-radius: var(--radius-md);
}

.calc-label {
  display: block;
  font-size: var(--font-size-xs);
  color: var(--c-text-secondary);
  margin-bottom: 4px;
}

.calc-value {
  display: block;
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--c-text-primary);
}

.calc-tip {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: var(--space-sm);
  background: var(--c-primary-bg);
  border-radius: var(--radius-md);
  color: var(--c-primary);
  font-size: var(--font-size-sm);
}

.calc-tip.warning {
  background: var(--c-warning-bg);
  color: var(--c-warning);
}

.confirm-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.confirm-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-md);
}

.confirm-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-sm) var(--space-md);
  background: var(--c-bg-body);
  border-radius: var(--radius-md);
}

.confirm-label {
  color: var(--c-text-secondary);
  font-size: var(--font-size-sm);
}

.confirm-value {
  color: var(--c-text-primary);
  font-weight: 600;
}

.mode-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.mode-item {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.mode-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.mode-icon.new {
  background: var(--c-primary);
}

.mode-icon.review {
  background: var(--c-success);
}

.mode-text {
  display: flex;
  flex-direction: column;
}

.mode-text strong {
  color: var(--c-text-primary);
  font-size: var(--font-size-md);
}

.mode-text span {
  color: var(--c-text-secondary);
  font-size: var(--font-size-sm);
}

.wizard-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm);
}

@media (max-width: 640px) {
  .calc-grid {
    grid-template-columns: 1fr;
  }
  .confirm-grid {
    grid-template-columns: 1fr;
  }
}
</style>
