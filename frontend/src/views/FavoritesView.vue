<template>
  <div class="favorites-page page-container">
    <PageHeader 
      title="我的收藏" 
      subtitle="管理您收藏的单词，随时查看和复习。"
    />
    
    <!-- Favorites Table -->
    <SectionCard class="table-section" :body-style="{ padding: '0' }">
      <el-table 
        :data="favoriteList" 
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
        
        <el-table-column prop="meaning" label="释义" min-width="200" show-overflow-tooltip />
        
        <el-table-column prop="createdAt" label="收藏时间" width="180">
          <template #default="{ row }">
            <span class="date-text">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button 
              link 
              type="danger" 
              size="small" 
              :loading="removeLoading[row.id]"
              @click="handleRemove(row)"
            >
              <el-icon class="mr-1"><StarFilled /></el-icon> 取消收藏
            </el-button>
            <el-button link type="info" size="small" @click="$router.push(`/words/${row.wordId}`)">
              <el-icon class="mr-1"><View /></el-icon> 详情
            </el-button>
          </template>
        </el-table-column>
        
        <template #empty>
          <EmptyState 
            title="暂无收藏的单词" 
            description="去单词列表逛逛，收藏您感兴趣的单词吧。"
            :icon="Star"
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { StarFilled, View, Star } from '@element-plus/icons-vue'
import type { Favorite } from '@/types'
import { favoriteApi } from '@/api/favorite'
import PageHeader from '@/components/ui/PageHeader.vue'
import SectionCard from '@/components/ui/SectionCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

const router = useRouter()
const loading = ref(false)
const favoriteList = ref<Favorite[]>([])
const removeLoading = reactive<Record<number, boolean>>({})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const fetchFavorites = async () => {
  loading.value = true
  try {
    const res = await favoriteApi.getFavorites({
      page: pagination.page,
      size: pagination.size
    })
    favoriteList.value = res.list
    pagination.total = res.total
  } catch (error) {
    console.error(error)
    ElMessage.error('加载收藏列表失败')
  } finally {
    loading.value = false
  }
}

const handleRemove = async (favorite: Favorite) => {
  if (removeLoading[favorite.id]) return
  
  try {
    await ElMessageBox.confirm(
      `确定要取消收藏 "${favorite.word}" 吗？`,
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
  
  removeLoading[favorite.id] = true
  try {
    await favoriteApi.removeFavorite(favorite.wordId)
    favoriteList.value = favoriteList.value.filter(item => item.id !== favorite.id)
    pagination.total--
    ElMessage.success(`已取消收藏 "${favorite.word}"`)
    
    if (favoriteList.value.length === 0 && pagination.page > 1) {
      pagination.page--
      fetchFavorites()
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('取消收藏失败')
  } finally {
    removeLoading[favorite.id] = false
  }
}

const handlePageChange = (page: number) => {
  pagination.page = page
  fetchFavorites()
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  fetchFavorites()
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
  fetchFavorites()
})
</script>

<style scoped>
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
