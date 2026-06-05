<template>
  <div class="notes-page page-container">
    <PageHeader 
      title="我的笔记" 
      subtitle="浏览和管理您记录的所有学习笔记。"
    />
    
    <!-- Search Bar -->
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索笔记内容..."
        clearable
        class="search-input"
        @input="handleSearch"
        @clear="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>
    
    <!-- Notes Table -->
    <SectionCard class="table-section" :body-style="{ padding: '0' }">
      <el-table 
        :data="noteList" 
        v-loading="loading" 
        stripe
        style="width: 100%"
      >
        <el-table-column prop="word" label="单词" min-width="120">
          <template #default="{ row }">
            <el-link type="primary" class="word-link" @click="$router.push(`/words/${row.wordId}`)">
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
        
        <el-table-column prop="meaning" label="释义" min-width="180" show-overflow-tooltip />
        
        <el-table-column prop="contentSummary" label="笔记摘要" min-width="250" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="note-summary">{{ row.contentSummary }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="updatedAt" label="最后修改" width="180">
          <template #default="{ row }">
            <span class="date-text">{{ formatDate(row.updatedAt) }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="$router.push(`/words/${row.wordId}`)">
              <el-icon class="mr-1"><View /></el-icon> 查看
            </el-button>
          </template>
        </el-table-column>
        
        <template #empty>
          <EmptyState 
            title="暂无笔记" 
            description="去单词详情页记录您的学习笔记吧。"
            :icon="Edit"
          >
            <template #action>
              <el-button type="primary" @click="$router.push('/words')">
                去浏览单词
              </el-button>
            </template>
          </EmptyState>
        </template>
      </el-table>
      
      <div class="pagination-wrapper" v-if="pagination.total > 0">
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, View, Edit } from '@element-plus/icons-vue'
import type { Note } from '@/types'
import { noteApi } from '@/api/note'
import PageHeader from '@/components/ui/PageHeader.vue'
import SectionCard from '@/components/ui/SectionCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

const router = useRouter()
const loading = ref(false)
const noteList = ref<Note[]>([])
const searchKeyword = ref('')
let searchTimer: ReturnType<typeof setTimeout> | null = null

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const fetchNotes = async () => {
  loading.value = true
  try {
    const res = await noteApi.getNotes({
      page: pagination.page,
      size: pagination.size,
      keyword: searchKeyword.value.trim() || undefined
    })
    noteList.value = res.list
    pagination.total = res.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
  searchTimer = setTimeout(() => {
    pagination.page = 1
    fetchNotes()
  }, 300)
}

const handlePageChange = (page: number) => {
  pagination.page = page
  fetchNotes()
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  fetchNotes()
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

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  fetchNotes()
})
</script>

<style scoped>
.search-bar {
  margin-bottom: var(--space-md);
}

.search-input {
  max-width: 400px;
}

.table-section {
  margin-bottom: var(--space-md);
}

.word-link {
  font-weight: 600;
  font-size: var(--font-size-base);
}

.phonetic-text {
  font-family: 'Times New Roman', serif;
  color: var(--c-text-secondary);
}

.note-summary {
  color: var(--c-text-secondary);
  line-height: 1.5;
}

.date-text {
  color: var(--c-text-tertiary);
  font-size: var(--font-size-sm);
}

.pagination-wrapper {
  padding: var(--space-md);
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--c-border-light);
}

.mr-1 { margin-right: 4px; }
</style>
