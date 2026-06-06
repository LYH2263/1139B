<template>
  <div class="word-detail-page page-container" v-if="word">
    <PageHeader title="单词详情" @back="$router.back()">
      <template #actions>
        <el-button 
          :type="isFavorite ? 'danger' : 'warning'" 
          :loading="favoriteLoading"
          @click="toggleFavorite"
        >
          <el-icon class="mr-1">
            <component :is="isFavorite ? 'HeartFilled' : 'Heart'" />
          </el-icon> 
          {{ isFavorite ? '已收藏' : '收藏' }}
        </el-button>
        <el-button @click="$router.push(`/mindmap/${word.id}`)">
          <el-icon class="mr-1"><Share /></el-icon> 思维导图
        </el-button>
        <el-button type="primary" @click="addToPlan">
          <el-icon class="mr-1"><Plus /></el-icon> 加入计划
        </el-button>
      </template>
    </PageHeader>
    
    <div class="detail-content" v-loading="loading">
      <!-- Main Word Card -->
      <el-card class="main-card">
        <div class="word-header">
          <h1 class="word-title">{{ word.word }}</h1>
          <div class="word-meta">
            <span v-if="word.phonetic" class="phonetic">/{{ word.phonetic }}/</span>
            <el-tag v-if="word.pos" size="large" effect="dark" type="primary" class="pos-tag">
              {{ formatPos(word.pos) }}
            </el-tag>
          </div>
        </div>
        
        <el-divider />
        
        <div class="word-body">
          <div class="detail-section">
            <h3 class="section-label">释义</h3>
            <p class="meaning-text">{{ word.meaning }}</p>
          </div>
          
          <div class="detail-section" v-if="word.example">
            <h3 class="section-label">例句</h3>
            <div class="example-box">
              <p class="example-text">{{ word.example }}</p>
            </div>
          </div>
          
          <div class="detail-section" v-if="word.memoryTip">
            <h3 class="section-label">记忆提示</h3>
            <el-alert 
              type="success" 
              :title="word.memoryTip" 
              :closable="false" 
              show-icon
              class="memory-alert"
            />
          </div>
        </div>
      </el-card>
      
      <!-- My Notes Section -->
      <el-card class="note-card">
        <div class="note-header" @click="noteExpanded = !noteExpanded">
          <div class="note-title-wrapper">
            <el-icon class="note-icon"><Edit /></el-icon>
            <h3 class="note-title">我的笔记</h3>
            <span v-if="note" class="note-saved-status">
              {{ noteSaving ? '保存中...' : '已保存' }}
            </span>
          </div>
          <el-icon class="expand-icon" :class="{ expanded: noteExpanded }">
            <ArrowDown />
          </el-icon>
        </div>
        
        <div v-show="noteExpanded" class="note-body">
          <div v-if="noteLoading" class="note-loading">
            <el-skeleton :rows="3" />
          </div>
          
          <div v-else class="note-content-wrapper">
            <div v-if="!noteEditing" class="note-preview" @click="startEditing">
              <div 
                v-if="note?.content" 
                class="markdown-body" 
                v-html="renderedContent"
              />
              <div v-else class="note-empty">
                <el-icon><EditPen /></el-icon>
                <p>点击开始记录笔记</p>
                <span class="note-hint">支持 Markdown 格式</span>
              </div>
            </div>
            
            <div v-else class="note-editor-wrapper">
              <div class="editor-toolbar">
                <span class="char-count">{{ noteContent.length }}/1000</span>
                <div class="editor-actions">
                  <el-button size="small" @click="cancelEditing">取消</el-button>
                  <el-button 
                    size="small" 
                    type="primary" 
                    :loading="noteSaving"
                    @click="saveNote"
                  >
                    保存
                  </el-button>
                </div>
              </div>
              
              <div class="editor-tabs">
                <span 
                  class="tab-item" 
                  :class="{ active: editorMode === 'edit' }"
                  @click="editorMode = 'edit'"
                >
                  编辑
                </span>
                <span 
                  class="tab-item" 
                  :class="{ active: editorMode === 'preview' }"
                  @click="editorMode = 'preview'"
                >
                  预览
                </span>
              </div>
              
              <div class="editor-container">
                <el-input
                  v-if="editorMode === 'edit'"
                  v-model="noteContent"
                  type="textarea"
                  :rows="10"
                  placeholder="记录您的学习笔记...&#10;&#10;支持 Markdown 格式：&#10;- **加粗文字**&#10;- *斜体文字*&#10;- `代码`&#10;- - 列表项&#10;- ### 标题"
                  maxlength="1000"
                  show-word-limit
                  resize="vertical"
                  class="note-editor"
                  @input="onContentChange"
                />
                
                <div 
                  v-else 
                  class="markdown-body note-preview-area"
                  v-html="renderedContent"
                />
              </div>
              
              <div class="editor-footer">
                <span class="auto-save-hint">
                  <el-icon v-if="noteSaving" class="saving-icon"><Loading /></el-icon>
                  {{ noteSaving ? '正在自动保存...' : (lastSavedAt ? '上次保存：' + lastSavedAt : '修改后 2 秒自动保存') }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </el-card>
      
      <!-- Memory Associations Section -->
      <el-card class="association-card">
        <div class="association-header" @click="associationsExpanded = !associationsExpanded">
          <div class="association-title-wrapper">
            <el-icon class="association-icon"><Sunny /></el-icon>
            <h3 class="association-title">记忆法</h3>
            <span v-if="associations.length > 0" class="association-count">
              {{ associations.length }} 条联想
            </span>
          </div>
          <div class="association-header-actions">
            <el-button 
              v-if="!showAssociationForm"
              type="primary" 
              size="small" 
              @click.stop="showAssociationForm = true"
            >
              <el-icon class="mr-1"><Plus /></el-icon> 分享我的记忆法
            </el-button>
            <el-icon class="expand-icon" :class="{ expanded: associationsExpanded }">
              <ArrowDown />
            </el-icon>
          </div>
        </div>

        <div v-show="associationsExpanded" class="association-body">
          <div v-if="showAssociationForm" class="association-form">
            <div class="form-title">分享你的记忆法</div>
            <el-select 
              v-model="associationType" class="type-select" size="small">
              <el-option 
                v-for="opt in associationTypeOptions" 
                :key="opt.value" 
                :label="opt.label" 
                :value="opt.value" 
              />
            </el-select>
            <el-input
              v-model="associationContent"
              type="textarea"
              :rows="4"
              placeholder="分享你的记忆联想方法..."
              maxlength="1000"
              show-word-limit
              resize="vertical"
              class="association-editor"
            />
            <div class="form-actions">
              <el-button 
              size="small" 
              @click="showAssociationForm = false"
            >
              取消
            </el-button>
            <el-button 
              size="small" 
              type="primary" 
              :loading="submittingAssociation"
              @click="submitAssociation"
            >
              提交
            </el-button>
          </div>
          </div>

          <div v-if="associationsLoading" class="associations-loading">
            <el-skeleton :rows="4" />
          </div>

          <div v-else-if="associations.length === 0" class="associations-empty">
            <el-icon><Sunny /></el-icon>
            <p>暂无记忆联想</p>
            <span class="hint">点击上方按钮，分享你的记忆法吧</span>
          </div>

          <div v-else class="associations-list">
            <div 
              v-for="assoc in associations" 
              :key="assoc.id" 
              class="association-item"
              :class="{ 'system-generated': assoc.isSystemGenerated }"
            >
              <div class="association-item-header">
                <el-tag 
                size="small" 
                :style="{ backgroundColor: getAssociationTypeColor(assoc.type) + '20', color: getAssociationTypeColor(assoc.type), borderColor: getAssociationTypeColor(assoc.type) + '40' }"
                effect="light"
                class="type-tag"
              >
                <el-icon class="mr-1">
                  <component :is="getAssociationTypeIcon(assoc.type)" />
                </el-icon>
                {{ assoc.type }}
              </el-tag>
              <el-tag 
                v-if="assoc.isSystemGenerated" 
                size="small" 
                type="success"
                effect="light"
                class="source-tag"
              >
                系统生成
              </el-tag>
              <span v-else class="author">
                by {{ assoc.createdBy }}
              </span>
              <el-button 
                class="upvote-btn"
                :loading="upvotingIds.has(assoc.id)"
                size="small"
                @click.stop="upvoteAssociation(assoc.id)"
              >
                <el-icon><Top /></el-icon>
                {{ assoc.upvotes }}
              </el-button>
              </div>
              <div class="association-content">
                <pre class="content-text">{{ assoc.content }}</pre>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- Related Actions -->
      <div class="related-actions">
         <ActionCard 
          title="查看思维导图" 
          description="探索该单词的关联词汇网络"
          @click="$router.push(`/mindmap/${word.id}`)"
        >
          <template #icon><Share /></template>
        </ActionCard>
        
        <ActionCard 
          title="立即加入复习" 
          description="将此单词加入今日复习队列"
          @click="addToPlan"
        >
          <template #icon><Calendar /></template>
        </ActionCard>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Share, Plus, Calendar, Heart, HeartFilled, Edit, ArrowDown, 
  EditPen, Loading, Sunny, Top, ChatDotRound, 
  MagicStick, CollectionTag 
} from '@element-plus/icons-vue'
import { marked } from 'marked'
import type { Word, Note } from '@/types'
import { wordApi } from '@/api/word'
import { statsApi } from '@/api/study'
import { favoriteApi } from '@/api/favorite'
import { noteApi } from '@/api/note'
import { associationApi } from '@/api/association'
import type { MemoryAssociation } from '@/types'
import PageHeader from '@/components/ui/PageHeader.vue'
import ActionCard from '@/components/ui/ActionCard.vue'

marked.setOptions({
  breaks: true,
  gfm: true
})

const route = useRoute()
const word = ref<Word | null>(null)
const loading = ref(false)
const isFavorite = ref(false)
const favoriteLoading = ref(false)

const note = ref<Note | null>(null)
const noteLoading = ref(false)
const noteExpanded = ref(true)
const noteEditing = ref(false)
const noteSaving = ref(false)
const noteContent = ref('')
const editorMode = ref<'edit' | 'preview'>('edit')
const lastSavedAt = ref('')

const associations = ref<MemoryAssociation[]>([])
const associationsLoading = ref(false)
const associationsExpanded = ref(true)
const associationType = ref('词根拆解')
const associationContent = ref('')
const showAssociationForm = ref(false)
const submittingAssociation = ref(false)
const upvotingIds = ref<Set<number>>(new Set())

const associationTypeOptions = [
  { label: '词根拆解', value: '词根拆解' },
  { label: '谐音联想', value: '谐音联想' },
  { label: '字母联想', value: '字母联想' },
  { label: '用户分享', value: '用户分享' }
]

let saveTimer: ReturnType<typeof setTimeout> | null = null

const renderedContent = computed(() => {
  if (!noteContent.value.trim()) {
    return '<p class="empty-preview">暂无内容，切换到编辑模式开始记录...</p>'
  }
  return marked.parse(noteContent.value) as string
})

const fetchWord = async () => {
  const id = Number(route.params.id)
  if (!id) return
  
  loading.value = true
  try {
    word.value = await wordApi.getWordById(id)
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const addToPlan = async () => {
  if (!word.value) return
  try {
    await statsApi.createStudyPlan(word.value.id, 'TODAY')
    ElMessage.success(`已将 "${word.value.word}" 加入今日学习计划`)
  } catch (error) {
    console.error(error)
  }
}

const fetchFavoriteStatus = async () => {
  const id = Number(route.params.id)
  if (!id) return
  try {
    const res = await favoriteApi.getFavoriteStatus(id)
    isFavorite.value = res.isFavorite
  } catch (error) {
    console.error(error)
    ElMessage.error('获取收藏状态失败')
  }
}

const toggleFavorite = async () => {
  if (!word.value || favoriteLoading.value) return
  
  if (isFavorite.value) {
    try {
      await ElMessageBox.confirm(
        `确定要取消收藏 "${word.value.word}" 吗？`,
        '取消收藏',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    } catch {
      return
    }
  }
  
  favoriteLoading.value = true
  try {
    if (isFavorite.value) {
      await favoriteApi.removeFavorite(word.value.id)
      isFavorite.value = false
      ElMessage.success(`已取消收藏 "${word.value.word}"`)
    } else {
      await favoriteApi.addFavorite(word.value.id)
      isFavorite.value = true
      ElMessage.success(`已收藏 "${word.value.word}"`)
    }
  } catch (error) {
    console.error(error)
    ElMessage.error(isFavorite.value ? '取消收藏失败' : '收藏失败')
  } finally {
    favoriteLoading.value = false
  }
}

const fetchNote = async () => {
  const id = Number(route.params.id)
  if (!id) return
  
  noteLoading.value = true
  try {
    const res = await noteApi.getNoteByWordId(id)
    note.value = res
    if (res) {
      noteContent.value = res.content
    }
  } catch (error) {
    console.error(error)
  } finally {
    noteLoading.value = false
  }
}

const startEditing = () => {
  noteEditing.value = true
  editorMode.value = 'edit'
  nextTick(() => {
    const textarea = document.querySelector('.note-editor textarea') as HTMLTextAreaElement
    if (textarea) {
      textarea.focus()
    }
  })
}

const cancelEditing = () => {
  if (note.value) {
    noteContent.value = note.value.content
  } else {
    noteContent.value = ''
  }
  noteEditing.value = false
  if (saveTimer) {
    clearTimeout(saveTimer)
    saveTimer = null
  }
}

const onContentChange = () => {
  if (saveTimer) {
    clearTimeout(saveTimer)
  }
  saveTimer = setTimeout(() => {
    saveNote()
  }, 2000)
}

const saveNote = async () => {
  const id = Number(route.params.id)
  if (!id || !noteContent.value.trim()) return
  
  if (noteContent.value.length > 1000) {
    ElMessage.warning('笔记内容不能超过 1000 个字符')
    return
  }
  
  noteSaving.value = true
  try {
    if (note.value) {
      note.value = await noteApi.updateNote(id, noteContent.value.trim())
    } else {
      note.value = await noteApi.createNote(id, noteContent.value.trim())
    }
    lastSavedAt.value = formatSaveTime(note.value.updatedAt)
    noteEditing.value = false
  } catch (error) {
    console.error(error)
  } finally {
    noteSaving.value = false
  }
}

const formatSaveTime = (dateStr: string) => {
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatPos = (pos: string) => {
  const map: Record<string, string> = {
    noun: '名词',
    verb: '动词',
    adjective: '形容词',
    adverb: '副词'
  }
  return map[pos] || pos
}

const fetchAssociations = async () => {
  const id = Number(route.params.id)
  if (!id) return
  
  associationsLoading.value = true
  try {
    const res = await associationApi.getAssociations(id)
    associations.value = res.list
  } catch (error) {
    console.error(error)
  } finally {
    associationsLoading.value = false
  }
}

const submitAssociation = async () => {
  const id = Number(route.params.id)
  if (!id) return
  
  if (!associationContent.value.trim()) {
    ElMessage.warning('请输入联想内容')
    return
  }
  
  if (associationContent.value.length > 1000) {
    ElMessage.warning('联想内容不能超过 1000 个字符')
    return
  }
  
  submittingAssociation.value = true
  try {
    await associationApi.createAssociation(id, {
      wordId: id,
      type: associationType.value,
      content: associationContent.value.trim()
    })
    ElMessage.success('联想分享成功！')
    associationContent.value = ''
    showAssociationForm.value = false
    await fetchAssociations()
  } catch (error) {
    console.error(error)
  } finally {
    submittingAssociation.value = false
  }
}

const upvoteAssociation = async (id: number) => {
  if (upvotingIds.value.has(id)) return
  
  upvotingIds.value.add(id)
  try {
    const res = await associationApi.upvoteAssociation(id)
    const association = associations.value.find(a => a.id === id)
    if (association) {
      association.upvotes = res.upvotes
    }
    associations.value.sort((a, b) => b.upvotes - a.upvotes)
    ElMessage.success('点赞成功！')
  } catch (error) {
    console.error(error)
  } finally {
    upvotingIds.value.delete(id)
  }
}

const getAssociationTypeIcon = (type: string) => {
  const map: Record<string, any> = {
    '词根拆解': MagicStick,
    '谐音联想': CollectionTag,
    '字母联想': Sunny,
    '用户分享': ChatDotRound
  }
  return map[type] || Sunny
}

const getAssociationTypeColor = (type: string) => {
  const map: Record<string, string> = {
    '词根拆解': '#409eff',
    '谐音联想': '#e6a23c',
    '字母联想': '#67c23a',
    '用户分享': '#909399'
  }
  return map[type] || '#909399'
}

watch(noteContent, (newVal) => {
  if (newVal.length > 1000) {
    noteContent.value = newVal.substring(0, 1000)
  }
})

onMounted(() => {
  fetchWord()
  fetchFavoriteStatus()
  fetchNote()
  fetchAssociations()
})
</script>

<style scoped>
.word-detail-page {
  max-width: 1000px;
}

.main-card {
  margin-bottom: var(--space-xl);
  border-radius: var(--radius-lg);
}

.word-header {
  text-align: center;
  padding: var(--space-lg) 0;
}

.word-title {
  font-size: 48px;
  font-weight: 800;
  color: var(--c-text-primary);
  margin-bottom: var(--space-xs);
  letter-spacing: -1px;
}

.word-meta {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-md);
}

.phonetic {
  font-family: 'Times New Roman', serif;
  font-size: 20px;
  color: var(--c-text-secondary);
  font-style: italic;
}

.pos-tag {
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.word-body {
  padding: 0 var(--space-lg);
}

.detail-section {
  margin-bottom: var(--space-xl);
}

.section-label {
  font-size: var(--font-size-sm);
  text-transform: uppercase;
  color: var(--c-text-tertiary);
  letter-spacing: 1px;
  margin-bottom: var(--space-sm);
  font-weight: 600;
}

.meaning-text {
  font-size: 20px;
  color: var(--c-text-primary);
  line-height: 1.6;
  font-weight: 500;
}

.example-box {
  background: var(--c-bg-body);
  padding: var(--space-lg);
  border-radius: var(--radius-md);
  border-left: 4px solid var(--c-primary);
}

.example-text {
  font-size: 18px;
  color: var(--c-text-secondary);
  font-style: italic;
  line-height: 1.6;
}

/* Note Card Styles */
.note-card {
  margin-bottom: var(--space-xl);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.note-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-md) var(--space-lg);
  cursor: pointer;
  border-bottom: 1px solid var(--c-border-light);
  transition: background-color var(--transition-fast);
}

.note-header:hover {
  background-color: var(--c-bg-body);
}

.note-title-wrapper {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.note-icon {
  color: var(--c-primary);
  font-size: 20px;
}

.note-title {
  margin: 0;
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--c-text-primary);
}

.note-saved-status {
  font-size: var(--font-size-xs);
  color: var(--c-success);
  background-color: var(--c-success-bg);
  padding: 2px 8px;
  border-radius: var(--radius-full);
}

.expand-icon {
  color: var(--c-text-tertiary);
  transition: transform var(--transition-normal);
}

.expand-icon.expanded {
  transform: rotate(180deg);
}

.note-body {
  padding: var(--space-lg);
}

.note-loading {
  padding: var(--space-md);
}

.note-preview {
  padding: var(--space-lg);
  border: 2px dashed var(--c-border-light);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  min-height: 120px;
}

.note-preview:hover {
  border-color: var(--c-primary);
  background-color: var(--c-primary-bg);
}

.note-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--c-text-tertiary);
  padding: var(--space-xl) 0;
}

.note-empty .el-icon {
  font-size: 40px;
  margin-bottom: var(--space-sm);
  opacity: 0.5;
}

.note-empty p {
  margin: 0 0 var(--space-xs) 0;
  font-size: var(--font-size-base);
}

.note-hint {
  font-size: var(--font-size-sm);
  color: var(--c-text-tertiary);
}

.note-editor-wrapper {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.char-count {
  font-size: var(--font-size-sm);
  color: var(--c-text-tertiary);
}

.editor-actions {
  display: flex;
  gap: var(--space-sm);
}

.editor-tabs {
  display: flex;
  gap: var(--space-lg);
  border-bottom: 1px solid var(--c-border-light);
  padding-bottom: var(--space-sm);
}

.tab-item {
  font-size: var(--font-size-sm);
  color: var(--c-text-tertiary);
  cursor: pointer;
  padding-bottom: var(--space-sm);
  border-bottom: 2px solid transparent;
  transition: all var(--transition-fast);
}

.tab-item.active {
  color: var(--c-primary);
  border-bottom-color: var(--c-primary);
  font-weight: 500;
}

.editor-container {
  min-height: 200px;
}

.note-editor :deep(textarea) {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  line-height: 1.8;
}

.note-preview-area {
  padding: var(--space-md);
  background-color: var(--c-bg-body);
  border-radius: var(--radius-md);
  min-height: 200px;
}

.editor-footer {
  text-align: right;
}

.auto-save-hint {
  font-size: var(--font-size-xs);
  color: var(--c-text-tertiary);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
}

.saving-icon {
  animation: spin 1s linear infinite;
  color: var(--c-primary);
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Markdown Styles */
.markdown-body {
  color: var(--c-text-primary);
  line-height: 1.8;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  margin-top: var(--space-md);
  margin-bottom: var(--space-sm);
  font-weight: 600;
  color: var(--c-text-primary);
}

.markdown-body :deep(h1) { font-size: 24px; }
.markdown-body :deep(h2) { font-size: 20px; }
.markdown-body :deep(h3) { font-size: 18px; }
.markdown-body :deep(h4) { font-size: 16px; }

.markdown-body :deep(p) {
  margin-bottom: var(--space-sm);
}

.markdown-body :deep(strong) {
  color: var(--c-text-primary);
  font-weight: 600;
}

.markdown-body :deep(em) {
  color: var(--c-text-secondary);
}

.markdown-body :deep(code) {
  background-color: var(--c-bg-body);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  color: var(--c-primary);
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: var(--space-lg);
  margin-bottom: var(--space-sm);
}

.markdown-body :deep(li) {
  margin-bottom: var(--space-xs);
}

.markdown-body :deep(blockquote) {
  border-left: 4px solid var(--c-primary);
  padding-left: var(--space-md);
  margin: var(--space-sm) 0;
  color: var(--c-text-secondary);
  background-color: var(--c-primary-bg);
  padding: var(--space-sm) var(--space-md);
  border-radius: 0 var(--radius-md) var(--radius-md) 0;
}

.markdown-body :deep(.empty-preview) {
  color: var(--c-text-tertiary);
  text-align: center;
  padding: var(--space-lg);
}

/* Association Card Styles */
.association-card {
  margin-bottom: var(--space-xl);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.association-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-md) var(--space-lg);
  cursor: pointer;
  border-bottom: 1px solid var(--c-border-light);
  transition: background-color var(--transition-fast);
}

.association-header:hover {
  background-color: var(--c-bg-body);
}

.association-title-wrapper {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.association-icon {
  color: var(--c-warning);
  font-size: 20px;
}

.association-title {
  margin: 0;
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--c-text-primary);
}

.association-count {
  font-size: var(--font-size-xs);
  color: var(--c-text-tertiary);
  background-color: var(--c-bg-body);
  padding: 2px 8px;
  border-radius: var(--radius-full);
}

.association-header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.association-body {
  padding: var(--space-lg);
}

.association-form {
  background-color: var(--c-bg-body);
  padding: var(--space-lg);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-lg);
  border: 1px solid var(--c-primary-light);
}

.form-title {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--c-text-primary);
  margin-bottom: var(--space-md);
}

.type-select {
  width: 150px;
  margin-bottom: var(--space-md);
}

.association-editor :deep(textarea) {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  line-height: 1.6;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm);
  margin-top: var(--space-md);
}

.associations-loading {
  padding: var(--space-md);
}

.associations-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--c-text-tertiary);
  padding: var(--space-xl) 0;
}

.associations-empty .el-icon {
  font-size: 40px;
  margin-bottom: var(--space-sm);
  opacity: 0.5;
  color: var(--c-warning);
}

.associations-empty p {
  margin: 0 0 var(--space-xs) 0;
  font-size: var(--font-size-base);
}

.hint {
  font-size: var(--font-size-sm);
  color: var(--c-text-tertiary);
}

.associations-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.association-item {
  background-color: var(--c-bg-body);
  border: 1px solid var(--c-border-light);
  border-radius: var(--radius-md);
  padding: var(--space-md);
  transition: all var(--transition-fast);
}

.association-item:hover {
  border-color: var(--c-primary-light);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.association-item.system-generated {
  background: linear-gradient(135deg, var(--c-success-bg) 0%, var(--c-bg-body) 100%);
  border-color: var(--c-success-light);
}

.association-item-header {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-sm);
  flex-wrap: wrap;
}

.type-tag {
  font-weight: 500;
  display: inline-flex;
  align-items: center;
}

.source-tag {
  font-weight: 500;
}

.author {
  font-size: var(--font-size-xs);
  color: var(--c-text-tertiary);
}

.upvote-btn {
  margin-left: auto !important;
}

.association-content {
  padding: var(--space-sm) 0;
}

.content-text {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.8;
  color: var(--c-text-secondary);
  background: transparent;
  border: none;
  padding: 0;
}

.related-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-lg);
}

.mr-1 { margin-right: 4px; }

/* Responsive */
@media (max-width: 640px) {
  .word-title {
    font-size: 32px;
  }
  
  .related-actions {
    grid-template-columns: 1fr;
  }
  
  .note-body {
    padding: var(--space-md);
  }
}
</style>
