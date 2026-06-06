<template>
  <div class="words-page page-container">
    <PageHeader 
      title="单词列表" 
      subtitle="浏览并管理您的个人词库。"
    >
      <template #actions>
        <el-button @click="resetSearch">
          <el-icon class="mr-1"><Refresh /></el-icon> 重置
        </el-button>
        <el-button type="primary" @click="handleSearch">
          <el-icon class="mr-1"><Search /></el-icon> 搜索
        </el-button>
      </template>
    </PageHeader>
    
    <!-- Search Section -->
    <SectionCard class="search-section">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="关键词">
          <el-popover
            ref="searchPopover"
            placement="bottom-start"
            :width="420"
            trigger="manual"
            v-model:visible="popoverVisible"
            popper-class="search-popover"
          >
            <template #reference>
              <el-input 
                v-model="searchForm.keyword" 
                placeholder="搜索单词/释义/拼音(如 app*、pg)" 
                clearable 
                @focus="handleSearchFocus"
                @blur="handleSearchBlur"
                @input="handleKeywordInput"
                @keyup.enter="handleSearch"
                style="width: 360px"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </template>

            <div class="search-dropdown" v-loading="suggestionsLoading || historyLoading || hotLoading">
              <template v-if="searchForm.keyword && suggestions.length > 0">
                <div class="dropdown-section">
                  <div class="section-title">
                    <el-icon><Search /></el-icon>
                    <span>搜索建议</span>
                  </div>
                  <ul class="suggestion-list">
                    <li 
                      v-for="(item, idx) in suggestions" 
                      :key="'sug-' + idx"
                      class="suggestion-item"
                      @mousedown="selectSuggestion(item)"
                    >
                      <span class="suggestion-word">{{ item.word }}</span>
                      <span class="suggestion-meaning">{{ item.meaning }}</span>
                      <el-tag size="small" :type="getMatchTypeTag(item.matchType)" effect="plain">
                        {{ getMatchTypeLabel(item.matchType) }}
                      </el-tag>
                    </li>
                  </ul>
                </div>
              </template>

              <template v-else-if="!searchForm.keyword">
                <div v-if="searchHistory.length > 0" class="dropdown-section">
                  <div class="section-title">
                    <el-icon><Clock /></el-icon>
                    <span>搜索历史</span>
                    <el-button 
                      link 
                      type="danger" 
                      size="small" 
                      class="clear-btn"
                      @mousedown.stop="clearAllHistory"
                    >清空</el-button>
                  </div>
                  <div class="tag-cloud">
                    <el-tag
                      v-for="item in searchHistory"
                      :key="'h-' + item.id"
                      class="history-tag"
                      closable
                      effect="light"
                      @close="deleteHistoryItem(item.id)"
                      @click="selectKeyword(item.keyword)"
                    >
                      {{ item.keyword }}
                    </el-tag>
                  </div>
                </div>

                <div v-if="hotKeywords.length > 0" class="dropdown-section">
                  <div class="section-title">
                    <el-icon><TrendCharts /></el-icon>
                    <span>热门搜索</span>
                  </div>
                  <div class="tag-cloud">
                    <el-tag
                      v-for="(item, idx) in hotKeywords"
                      :key="'hot-' + idx"
                      class="hot-tag"
                      :type="idx < 3 ? 'danger' : (idx < 6 ? 'warning' : 'info')"
                      effect="light"
                      @click="selectKeyword(item.keyword)"
                    >
                      <span class="hot-rank">{{ idx + 1 }}</span>
                      {{ item.keyword }}
                      <span class="hot-count">({{ item.count }})</span>
                    </el-tag>
                  </div>
                </div>

                <div v-if="searchHistory.length === 0 && hotKeywords.length === 0" class="empty-tip">
                  暂无搜索历史和热门搜索
                </div>
              </template>

              <template v-else>
                <div class="empty-tip">未找到匹配的建议</div>
              </template>
            </div>
          </el-popover>
        </el-form-item>
        
        <el-form-item label="词性">
          <el-select v-model="searchForm.pos" placeholder="全部词性" clearable style="width: 140px">
            <el-option label="名词 (Noun)" value="noun" />
            <el-option label="动词 (Verb)" value="verb" />
            <el-option label="形容词 (Adjective)" value="adjective" />
            <el-option label="副词 (Adverb)" value="adverb" />
          </el-select>
        </el-form-item>
      </el-form>
    </SectionCard>
    
    <!-- Words Table -->
    <SectionCard class="table-section" :body-style="{ padding: '0' }">
      <el-table 
        :data="wordList" 
        v-loading="loading" 
        stripe
        style="width: 100%"
      >
        <el-table-column prop="word" label="单词" min-width="120">
          <template #default="{ row }">
            <el-link type="primary" class="word-link" @click="$router.push(`/words/${row.id}`)">
              {{ row.word }}
            </el-link>
          </template>
        </el-table-column>
        
        <el-table-column prop="phonetic" label="音标" width="140">
           <template #default="{ row }">
            <span class="phonetic-text">{{ row.phonetic }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="pos" label="词性" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.pos" size="small" effect="plain">{{ formatPos(row.pos) }}</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="meaning" label="释义" min-width="200" show-overflow-tooltip />
        
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button 
              link 
              :type="isFavorite(row.id) ? 'danger' : 'warning'" 
              size="small" 
              :loading="favoriteLoading[row.id]"
              @click.stop="toggleFavorite(row)"
            >
              <el-icon class="mr-1">
                <component :is="isFavorite(row.id) ? 'HeartFilled' : 'Heart'" />
              </el-icon> 
              {{ isFavorite(row.id) ? '已收藏' : '收藏' }}
            </el-button>
            <el-button link type="primary" size="small" @click="$router.push(`/mindmap/${row.id}`)">
              <el-icon class="mr-1"><Share /></el-icon> 导图
            </el-button>
            <el-button link type="success" size="small" @click="addToPlan(row)">
              <el-icon class="mr-1"><Plus /></el-icon> 加入计划
            </el-button>
            <el-button link type="info" size="small" @click="$router.push(`/words/${row.id}`)">
              <el-icon class="mr-1"><View /></el-icon> 详情
            </el-button>
          </template>
        </el-table-column>
        
        <template #empty>
          <EmptyState 
            title="未找到单词" 
            description="请尝试更换搜索条件，或联系管理员添加。"
          />
        </template>
      </el-table>
      
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </SectionCard>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Search, Refresh, Share, Plus, View, Heart, HeartFilled, 
  Clock, TrendCharts 
} from '@element-plus/icons-vue'
import type { Word, SearchSuggestion, SearchHistoryItem, HotKeyword } from '@/types'
import { wordApi } from '@/api/word'
import { searchApi } from '@/api/search'
import { statsApi } from '@/api/study'
import { favoriteApi } from '@/api/favorite'
import PageHeader from '@/components/ui/PageHeader.vue'
import SectionCard from '@/components/ui/SectionCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

const router = useRouter()
const loading = ref(false)
const wordList = ref<Word[]>([])
const favoriteWordIds = ref<number[]>([])
const favoriteLoading = reactive<Record<number, boolean>>({})

const searchPopover = ref()
const popoverVisible = ref(false)
const suggestions = ref<SearchSuggestion[]>([])
const searchHistory = ref<SearchHistoryItem[]>([])
const hotKeywords = ref<HotKeyword[]>([])
const suggestionsLoading = ref(false)
const historyLoading = ref(false)
const hotLoading = ref(false)

let debounceTimer: number | null = null
let blurTimer: number | null = null

const searchForm = reactive({
  keyword: '',
  pos: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const fetchWords = async () => {
  loading.value = true
  try {
    const res = await searchApi.searchWords({
      keyword: searchForm.keyword || undefined,
      pos: searchForm.pos || undefined,
      page: pagination.page,
      size: pagination.size
    })
    wordList.value = res.list
    pagination.total = res.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const fetchFavoriteWordIds = async () => {
  try {
    favoriteWordIds.value = await favoriteApi.getFavoriteWordIds()
  } catch (error) {
    console.error(error)
    ElMessage.error('获取收藏列表失败')
  }
}

const fetchSearchHistory = async () => {
  historyLoading.value = true
  try {
    searchHistory.value = await searchApi.getSearchHistory()
  } catch (error) {
    console.error(error)
  } finally {
    historyLoading.value = false
  }
}

const fetchHotKeywords = async () => {
  hotLoading.value = true
  try {
    hotKeywords.value = await searchApi.getHotKeywords()
  } catch (error) {
    console.error(error)
  } finally {
    hotLoading.value = false
  }
}

const fetchSuggestions = async () => {
  if (!searchForm.keyword.trim()) {
    suggestions.value = []
    return
  }
  suggestionsLoading.value = true
  try {
    suggestions.value = await searchApi.getSuggestions(searchForm.keyword.trim())
  } catch (error) {
    console.error(error)
  } finally {
    suggestionsLoading.value = false
  }
}

const debouncedFetchSuggestions = () => {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  debounceTimer = window.setTimeout(() => {
    fetchSuggestions()
  }, 300)
}

const handleKeywordInput = () => {
  debouncedFetchSuggestions()
}

const handleSearchFocus = () => {
  if (blurTimer) {
    clearTimeout(blurTimer)
    blurTimer = null
  }
  popoverVisible.value = true
  if (!searchForm.keyword) {
    fetchSearchHistory()
    fetchHotKeywords()
  }
}

const handleSearchBlur = () => {
  blurTimer = window.setTimeout(() => {
    popoverVisible.value = false
  }, 200)
}

const selectKeyword = (keyword: string) => {
  searchForm.keyword = keyword
  popoverVisible.value = false
  handleSearch()
}

const selectSuggestion = (item: SearchSuggestion) => {
  searchForm.keyword = item.word
  popoverVisible.value = false
  handleSearch()
}

const deleteHistoryItem = async (id: number) => {
  try {
    await searchApi.deleteSearchHistoryById(id)
    searchHistory.value = searchHistory.value.filter(item => item.id !== id)
  } catch (error) {
    console.error(error)
  }
}

const clearAllHistory = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有搜索历史吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await searchApi.deleteSearchHistory()
    searchHistory.value = []
    ElMessage.success('已清空搜索历史')
  } catch (e) {
    // 用户取消
  }
}

const getMatchTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    word_prefix: '前缀',
    word_contains: '包含',
    wildcard: '通配',
    meaning_contains: '释义',
    pinyin: '拼音'
  }
  return map[type] || type
}

const getMatchTypeTag = (type: string) => {
  const map: Record<string, string> = {
    word_prefix: 'primary',
    word_contains: 'success',
    wildcard: 'warning',
    meaning_contains: 'info',
    pinyin: 'danger'
  }
  return (map[type] as any) || 'info'
}

const isFavorite = (wordId: number) => {
  return favoriteWordIds.value.includes(wordId)
}

const toggleFavorite = async (word: Word) => {
  if (favoriteLoading[word.id]) return
  
  if (isFavorite(word.id)) {
    try {
      await ElMessageBox.confirm(
        `确定要取消收藏 "${word.word}" 吗？`,
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
  
  favoriteLoading[word.id] = true
  try {
    if (isFavorite(word.id)) {
      await favoriteApi.removeFavorite(word.id)
      favoriteWordIds.value = favoriteWordIds.value.filter(id => id !== word.id)
      ElMessage.success(`已取消收藏 "${word.word}"`)
    } else {
      await favoriteApi.addFavorite(word.id)
      favoriteWordIds.value.push(word.id)
      ElMessage.success(`已收藏 "${word.word}"`)
    }
  } catch (error) {
    console.error(error)
    ElMessage.error(isFavorite(word.id) ? '取消收藏失败' : '收藏失败')
  } finally {
    favoriteLoading[word.id] = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  popoverVisible.value = false
  fetchWords()
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.pos = ''
  pagination.page = 1
  suggestions.value = []
  fetchWords()
}

const handlePageChange = (page: number) => {
  pagination.page = page
  fetchWords()
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  fetchWords()
}

const addToPlan = async (word: Word) => {
  try {
    await statsApi.createStudyPlan(word.id, 'TODAY')
    ElMessage.success(`已将 "${word.word}" 加入今日学习计划`)
  } catch (error) {
    console.error(error)
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
  fetchWords()
  fetchFavoriteWordIds()
})
</script>

<style scoped>
.search-section {
  margin-bottom: var(--space-md);
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-md);
  align-items: flex-start;
}

.word-link {
  font-weight: 600;
  font-size: var(--font-size-base);
}

.phonetic-text {
  font-family: 'Times New Roman', serif;
  color: var(--c-text-secondary);
}

.pagination-wrapper {
  padding: var(--space-md);
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--c-border-light);
}

.mr-1 { margin-right: 4px; }

:deep(.search-popover) {
  padding: 0 !important;
}

.search-dropdown {
  padding: 8px 0;
  max-height: 420px;
  overflow-y: auto;
}

.dropdown-section {
  padding: 8px 16px;
}

.dropdown-section + .dropdown-section {
  border-top: 1px solid var(--c-border-light);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--c-text-secondary);
  margin-bottom: 10px;
  font-weight: 600;
}

.section-title .clear-btn {
  margin-left: auto;
  font-size: 12px;
  padding: 0;
}

.suggestion-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 4px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.suggestion-item:hover {
  background-color: var(--c-bg-hover, #f5f7fa);
}

.suggestion-word {
  font-weight: 600;
  font-size: 14px;
  color: var(--c-primary);
  min-width: 100px;
}

.suggestion-meaning {
  flex: 1;
  font-size: 13px;
  color: var(--c-text-regular);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.history-tag {
  cursor: pointer;
  transition: all 0.15s;
}

.history-tag:hover {
  opacity: 0.8;
}

.hot-tag {
  cursor: pointer;
  transition: all 0.15s;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.hot-tag:hover {
  opacity: 0.85;
}

.hot-rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 700;
}

.hot-count {
  font-size: 11px;
  opacity: 0.75;
}

.empty-tip {
  padding: 32px;
  text-align: center;
  color: var(--c-text-secondary);
  font-size: 13px;
}
</style>
