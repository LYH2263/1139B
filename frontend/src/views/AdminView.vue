<template>
  <div class="admin-page page-container">
    <PageHeader 
      :title="activeTab === 'words' ? '词库管理' : '系统公告'" 
      :subtitle="activeTab === 'words' ? '管理系统中的单词数据，支持批量导入。' : '向所有用户发布系统公告通知。'"
    >
      <template #actions>
        <template v-if="activeTab === 'words'">
          <el-button type="success" @click="showImportDialog = true">
            <el-icon class="mr-1"><Upload /></el-icon> 批量导入
          </el-button>
          <el-button type="primary" @click="openAddDialog">
            <el-icon class="mr-1"><Plus /></el-icon> 新增单词
          </el-button>
        </template>
        <template v-else>
          <el-button type="primary" @click="showBroadcastDialog = true">
            <el-icon class="mr-1"><Promotion /></el-icon> 发布公告
          </el-button>
        </template>
      </template>
    </PageHeader>
    
    <div class="admin-content">
      <el-tabs v-model="activeTab" class="admin-tabs">
        <el-tab-pane label="词库管理" name="words">
          <SectionCard :body-style="{ padding: '0' }">
            <el-table 
              :data="wordList" 
              v-loading="loading" 
              stripe
              style="width: 100%"
            >
              <el-table-column prop="id" label="ID" width="80" align="center" />
              
              <el-table-column prop="word" label="单词" width="160">
                <template #default="{ row }">
                  <span class="word-text">{{ row.word }}</span>
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
              
              <el-table-column label="操作" width="180" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="handleEdit(row)">
                    <el-icon class="mr-1"><Edit /></el-icon> 编辑
                  </el-button>
                  <el-button link type="danger" size="small" @click="handleDelete(row)">
                    <el-icon class="mr-1"><Delete /></el-icon> 删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            
            <div class="pagination-wrapper">
              <el-pagination
                v-model:current-page="pagination.page"
                v-model:page-size="pagination.size"
                :total="pagination.total"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next"
                background
                @size-change="handleSizeChange"
                @current-change="handlePageChange"
              />
            </div>
          </SectionCard>
        </el-tab-pane>
        
        <el-tab-pane label="系统公告" name="notifications">
          <SectionCard>
            <el-alert
              title="发布系统公告"
              type="info"
              :closable="false"
              show-icon
              class="alert-tip"
            >
              <template #title>
                <span>系统公告将发送给所有注册用户，请注意公告内容的准确性。</span>
              </template>
            </el-alert>
            
            <div class="broadcast-section">
              <el-button type="primary" size="large" @click="showBroadcastDialog = true">
                <el-icon class="mr-1"><Promotion /></el-icon> 发布新公告
              </el-button>
            </div>
          </SectionCard>
        </el-tab-pane>
      </el-tabs>
    </div>
    
    <!-- Word Edit Dialog -->
    <el-dialog 
      v-model="showAddDialog" 
      :title="isEdit ? '编辑单词' : '新增单词'"
      width="600px"
      class="custom-dialog"
      destroy-on-close
    >
      <el-form 
        :model="wordForm" 
        :rules="wordRules" 
        ref="wordFormRef" 
        label-position="top"
        class="word-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="单词" prop="word">
              <el-input v-model="wordForm.word" placeholder="Example: apple" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="音标">
              <el-input v-model="wordForm.phonetic" placeholder="Example: [ˈæpl]" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="词性">
          <el-select v-model="wordForm.pos" placeholder="选择词性" style="width: 100%">
            <el-option label="名词 (Noun)" value="noun" />
            <el-option label="动词 (Verb)" value="verb" />
            <el-option label="形容词 (Adjective)" value="adjective" />
            <el-option label="副词 (Adverb)" value="adverb" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="释义" prop="meaning">
          <el-input 
            v-model="wordForm.meaning" 
            type="textarea" 
            :rows="2"
            placeholder="请输入中文释义"
          />
        </el-form-item>
        
        <el-form-item label="例句">
          <el-input 
            v-model="wordForm.example" 
            type="textarea" 
            :rows="3"
            placeholder="请输入英文例句"
          />
        </el-form-item>
        
        <el-form-item label="记忆提示">
          <el-input 
            v-model="wordForm.memoryTip" 
            type="textarea" 
            :rows="2"
            placeholder="助记技巧或联想提示"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="submitWord" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>
    
    <!-- Import Dialog -->
    <el-dialog v-model="showImportDialog" title="批量导入单词" width="500px" class="custom-dialog">
      <el-upload
        class="upload-area"
        drag
        action=""
        :auto-upload="false"
        :on-change="handleFileChange"
        accept=".csv"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          拖拽 CSV 文件到此处，或 <em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 UTF-8 编码的 CSV 文件，需包含表头: word, meaning, example (可选)
          </div>
        </template>
      </el-upload>
      
      <template #footer>
        <el-button @click="showImportDialog = false">取消</el-button>
        <el-button type="primary" @click="submitImport" :loading="importing">开始导入</el-button>
      </template>
    </el-dialog>
    
    <!-- Broadcast Dialog -->
    <el-dialog 
      v-model="showBroadcastDialog" 
      title="发布系统公告" 
      width="600px" 
      class="custom-dialog"
      destroy-on-close
    >
      <el-form 
        :model="broadcastForm" 
        :rules="broadcastRules" 
        ref="broadcastFormRef" 
        label-position="top"
      >
        <el-form-item label="公告标题" prop="title">
          <el-input 
            v-model="broadcastForm.title" 
            placeholder="请输入公告标题"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input 
            v-model="broadcastForm.content" 
            type="textarea" 
            :rows="6"
            placeholder="请输入公告内容"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showBroadcastDialog = false">取消</el-button>
        <el-button type="primary" @click="submitBroadcast" :loading="broadcasting">发布公告</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Edit, Delete, UploadFilled, Promotion } from '@element-plus/icons-vue'
import type { Word } from '@/types'
import { wordApi } from '@/api/word'
import { notificationApi } from '@/api/notification'
import PageHeader from '@/components/ui/PageHeader.vue'
import SectionCard from '@/components/ui/SectionCard.vue'

const activeTab = ref('words')
const loading = ref(false)
const submitting = ref(false)
const importing = ref(false)
const broadcasting = ref(false)
const showAddDialog = ref(false)
const showImportDialog = ref(false)
const showBroadcastDialog = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const wordFormRef = ref()
const broadcastFormRef = ref()
const wordList = ref<Word[]>([])
const importFile = ref<File | null>(null)

const broadcastForm = reactive({
  title: '',
  content: ''
})

const broadcastRules = {
  title: [
    { required: true, message: '请输入公告标题', trigger: 'blur' },
    { max: 100, message: '标题长度不能超过100字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入公告内容', trigger: 'blur' },
    { max: 500, message: '内容长度不能超过500字符', trigger: 'blur' }
  ]
}

const wordForm = reactive({
  word: '',
  phonetic: '',
  pos: '',
  meaning: '',
  example: '',
  memoryTip: ''
})

const wordRules = {
  word: [
    { required: true, message: '请输入单词', trigger: 'blur' }
  ],
  meaning: [
    { required: true, message: '请输入释义', trigger: 'blur' }
  ]
}

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const fetchWords = async () => {
  loading.value = true
  try {
    const res = await wordApi.getWords({
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

const openAddDialog = () => {
  resetForm()
  showAddDialog.value = true
}

const handleEdit = (row: Word) => {
  isEdit.value = true
  editId.value = row.id
  wordForm.word = row.word
  wordForm.phonetic = row.phonetic || ''
  wordForm.pos = row.pos || ''
  wordForm.meaning = row.meaning
  wordForm.example = row.example || ''
  wordForm.memoryTip = row.memoryTip || ''
  showAddDialog.value = true
}

const handleDelete = async (row: Word) => {
  try {
    await ElMessageBox.confirm(`确定要删除单词 "${row.word}" 吗？`, '警告', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await wordApi.deleteWord(row.id)
    ElMessage.success('删除成功')
    fetchWords()
  } catch (error) {
    console.error(error)
  }
}

const submitWord = async () => {
  const valid = await wordFormRef.value?.validate().catch(() => false)
  if (!valid) return
  
  submitting.value = true
  try {
    if (isEdit.value && editId.value) {
      await wordApi.updateWord(editId.value, wordForm)
      ElMessage.success('更新成功')
    } else {
      await wordApi.createWord(wordForm)
      ElMessage.success('添加成功')
    }
    showAddDialog.value = false
    fetchWords()
  } catch (error) {
    console.error(error)
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  isEdit.value = false
  editId.value = null
  wordForm.word = ''
  wordForm.phonetic = ''
  wordForm.pos = ''
  wordForm.meaning = ''
  wordForm.example = ''
  wordForm.memoryTip = ''
  // Clear validation after next tick
  nextTick(() => {
    wordFormRef.value?.clearValidate()
  })
}

const handleFileChange = (file: any) => {
  importFile.value = file.raw
}

const submitImport = async () => {
  if (!importFile.value) {
    ElMessage.warning('请选择文件')
    return
  }
  
  importing.value = true
  try {
    const res = await wordApi.importWords(importFile.value)
    ElMessage.success(`成功导入 ${res.importedCount} 个单词`)
    if (res.failedRows.length > 0) {
      ElMessage.warning(`${res.failedRows.length} 行数据格式错误已跳过`)
    }
    showImportDialog.value = false
    importFile.value = null
    fetchWords()
  } catch (error) {
    console.error(error)
  } finally {
    importing.value = false
  }
}

// Pagination Handlers
const handlePageChange = (page: number) => {
  pagination.page = page
  fetchWords()
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  fetchWords()
}

// Formatters
const formatPos = (pos: string) => {
  const map: Record<string, string> = {
    noun: '名词',
    verb: '动词',
    adjective: '形容词',
    adverb: '副词'
  }
  return map[pos] || pos
}

const submitBroadcast = async () => {
  const valid = await broadcastFormRef.value?.validate().catch(() => false)
  if (!valid) return
  
  try {
    await ElMessageBox.confirm(
      '确定要发布此系统公告吗？公告将发送给所有注册用户。',
      '确认发布',
      {
        confirmButtonText: '确认发布',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    broadcasting.value = true
    const result = await notificationApi.broadcastNotification(broadcastForm)
    ElMessage.success(`公告发布成功，已发送给 ${result.sentCount} 位用户`)
    showBroadcastDialog.value = false
    broadcastForm.title = ''
    broadcastForm.content = ''
    nextTick(() => {
      broadcastFormRef.value?.clearValidate()
    })
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('发布失败，请重试')
    }
  } finally {
    broadcasting.value = false
  }
}

onMounted(() => {
  fetchWords()
})
</script>

<style scoped>
.word-text {
  font-weight: 600;
  color: var(--c-text-primary);
}

.phonetic-text {
  font-family: serif;
  color: var(--c-text-secondary);
}

.pagination-wrapper {
  padding: var(--space-md);
  border-top: 1px solid var(--c-border-light);
  display: flex;
  justify-content: flex-end;
}

.mr-1 { margin-right: 4px; }

.admin-tabs :deep(.el-tabs__header) {
  margin-bottom: var(--space-lg);
}

.alert-tip {
  margin-bottom: var(--space-lg);
}

.broadcast-section {
  text-align: center;
  padding: var(--space-xl) 0;
}
</style>
