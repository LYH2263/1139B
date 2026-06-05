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
            <component :is="isFavorite ? 'StarFilled' : 'Star'" />
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
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Share, Plus, Calendar, Star, StarFilled } from '@element-plus/icons-vue'
import type { Word } from '@/types'
import { wordApi } from '@/api/word'
import { statsApi } from '@/api/study'
import { favoriteApi } from '@/api/favorite'
import PageHeader from '@/components/ui/PageHeader.vue'
import ActionCard from '@/components/ui/ActionCard.vue'

const route = useRoute()
const word = ref<Word | null>(null)
const loading = ref(false)
const isFavorite = ref(false)
const favoriteLoading = ref(false)

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
  } finally {
    favoriteLoading.value = false
  }
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

onMounted(() => {
  fetchWord()
  fetchFavoriteStatus()
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
}
</style>
