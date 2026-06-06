<template>
  <el-dialog
    v-model="visible"
    title="标签管理"
    width="600px"
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <div v-loading="loading" class="tag-manage-container">
      <div class="tag-create-section">
        <el-form :inline="true" :model="createForm" class="create-form">
          <el-form-item>
            <el-input
              v-model="createForm.name"
              placeholder="输入新标签名"
              maxlength="50"
              clearable
              style="width: 180px"
              @keyup.enter="handleCreate"
            />
          </el-form-item>
          <el-form-item label="颜色">
            <div class="color-picker">
              <div
                v-for="color in presetColors"
                :key="color"
                class="color-option"
                :class="{ active: createForm.color === color }"
                :style="{ backgroundColor: color }"
                @click="createForm.color = color"
              >
                <el-icon v-if="createForm.color === color" class="check-icon"><Check /></el-icon>
              </div>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="creating"
              :disabled="!createForm.name.trim()"
              @click="handleCreate"
            >
              <el-icon class="mr-1"><Plus /></el-icon> 新建标签
            </el-button>
          </el-form-item>
        </el-form>
        <div class="tag-count-hint">
          已创建 {{ tags.length }}/20 个标签
        </div>
      </div>

      <el-divider />

      <div class="tags-list-section">
        <div v-if="tags.length === 0" class="tags-empty">
          <el-icon><CollectionTag /></el-icon>
          <p>暂无标签</p>
          <span class="hint">创建你的第一个标签来分类单词吧</span>
        </div>

        <div v-else class="tags-list">
          <div
            v-for="tag in tags"
            :key="tag.id"
            class="tag-item"
          >
            <div class="tag-info">
              <span
                class="tag-color-dot"
                :style="{ backgroundColor: tag.color }"
              />
              <span v-if="editingId !== tag.id" class="tag-name">
                {{ tag.name }}
              </span>
              <el-input
                v-else
                v-model="editForm.name"
                size="small"
                maxlength="50"
                style="width: 140px"
                @keyup.enter="handleSaveEdit(tag)"
                @keyup.esc="handleCancelEdit"
              />
              <span class="tag-word-count">
                {{ tag.wordCount || 0 }} 个单词
              </span>
            </div>

            <div v-if="editingId === tag.id" class="tag-edit-colors">
              <div
                v-for="color in presetColors"
                :key="color"
                class="color-option small"
                :class="{ active: editForm.color === color }"
                :style="{ backgroundColor: color }"
                @click="editForm.color = color"
              />
            </div>

            <div class="tag-actions">
              <template v-if="editingId !== tag.id">
                <el-button
                  link
                  type="primary"
                  size="small"
                  @click="handleStartEdit(tag)"
                >
                  <el-icon class="mr-1"><Edit /></el-icon> 编辑
                </el-button>
                <el-button
                  link
                  type="danger"
                  size="small"
                  @click="handleDelete(tag)"
                >
                  <el-icon class="mr-1"><Delete /></el-icon> 删除
                </el-button>
              </template>
              <template v-else>
                <el-button
                  link
                  type="primary"
                  size="small"
                  :loading="savingId === tag.id"
                  @click="handleSaveEdit(tag)"
                >
                  保存
                </el-button>
                <el-button
                  link
                  type="info"
                  size="small"
                  @click="handleCancelEdit"
                >
                  取消
                </el-button>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Check, CollectionTag, Edit, Delete
} from '@element-plus/icons-vue'
import { tagApi } from '@/api/tag'
import type { Tag } from '@/types'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'tags-updated'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const PRESET_COLORS = [
  '#409EFF', '#67C23A', '#E6A23C', '#F56C6C',
  '#909399', '#8E44AD', '#16A085', '#D35400'
]

const loading = ref(false)
const creating = ref(false)
const savingId = ref<number | null>(null)
const tags = ref<Tag[]>([])
const presetColors = ref<string[]>(PRESET_COLORS)

const createForm = reactive({
  name: '',
  color: PRESET_COLORS[0]
})

const editingId = ref<number | null>(null)
const editForm = reactive({
  name: '',
  color: PRESET_COLORS[0]
})

const fetchTags = async () => {
  loading.value = true
  try {
    tags.value = await tagApi.getTags()
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleCreate = async () => {
  if (!createForm.name.trim()) return
  if (tags.value.length >= 20) {
    ElMessage.warning('最多只能创建 20 个标签')
    return
  }
  creating.value = true
  try {
    await tagApi.createTag({
      name: createForm.name.trim(),
      color: createForm.color
    })
    ElMessage.success('标签创建成功')
    createForm.name = ''
    createForm.color = PRESET_COLORS[0]
    await fetchTags()
    emit('tags-updated')
  } catch (error: any) {
    console.error(error)
  } finally {
    creating.value = false
  }
}

const handleStartEdit = (tag: Tag) => {
  editingId.value = tag.id
  editForm.name = tag.name
  editForm.color = tag.color
}

const handleCancelEdit = () => {
  editingId.value = null
  editForm.name = ''
  editForm.color = PRESET_COLORS[0]
}

const handleSaveEdit = async (tag: Tag) => {
  if (!editForm.name.trim()) {
    ElMessage.warning('标签名不能为空')
    return
  }
  savingId.value = tag.id
  try {
    await tagApi.updateTag(tag.id, {
      name: editForm.name.trim(),
      color: editForm.color
    })
    ElMessage.success('标签已更新')
    editingId.value = null
    await fetchTags()
    emit('tags-updated')
  } catch (error) {
    console.error(error)
  } finally {
    savingId.value = null
  }
}

const handleDelete = async (tag: Tag) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除标签"${tag.name}"吗？该标签与所有单词的绑定关系也将被解除。`,
      '删除标签',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  try {
    await tagApi.deleteTag(tag.id)
    ElMessage.success('标签已删除')
    await fetchTags()
    emit('tags-updated')
  } catch (error) {
    console.error(error)
  }
}

const handleClosed = () => {
  editingId.value = null
  createForm.name = ''
  createForm.color = PRESET_COLORS[0]
}

watch(visible, (val) => {
  if (val) {
    fetchTags()
  }
})
</script>

<style scoped>
.tag-manage-container {
  min-height: 300px;
}

.tag-create-section {
  margin-bottom: var(--space-sm);
}

.create-form {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
  align-items: flex-start;
}

.color-picker {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.color-option {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-full);
  cursor: pointer;
  border: 2px solid transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}

.color-option:hover {
  transform: scale(1.1);
}

.color-option.active {
  border-color: var(--c-text-primary);
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.5) inset;
}

.color-option.small {
  width: 22px;
  height: 22px;
}

.check-icon {
  color: white;
  font-size: 16px;
  font-weight: bold;
}

.tag-count-hint {
  font-size: var(--font-size-xs);
  color: var(--c-text-tertiary);
  margin-top: var(--space-xs);
}

.tags-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--c-text-tertiary);
  padding: var(--space-2xl) 0;
}

.tags-empty .el-icon {
  font-size: 48px;
  margin-bottom: var(--space-sm);
  opacity: 0.5;
}

.tags-empty p {
  margin: 0 0 var(--space-xs) 0;
  font-size: var(--font-size-base);
}

.hint {
  font-size: var(--font-size-sm);
}

.tags-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  max-height: 400px;
  overflow-y: auto;
}

.tag-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-sm) var(--space-md);
  background-color: var(--c-bg-body);
  border-radius: var(--radius-md);
  border: 1px solid var(--c-border-light);
  transition: all var(--transition-fast);
  flex-wrap: wrap;
  gap: var(--space-sm);
}

.tag-item:hover {
  border-color: var(--c-primary-light);
}

.tag-info {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  flex-wrap: wrap;
}

.tag-color-dot {
  width: 14px;
  height: 14px;
  border-radius: var(--radius-full);
  flex-shrink: 0;
}

.tag-name {
  font-weight: 500;
  color: var(--c-text-primary);
}

.tag-word-count {
  font-size: var(--font-size-xs);
  color: var(--c-text-tertiary);
  background-color: var(--c-bg-hover);
  padding: 2px 8px;
  border-radius: var(--radius-full);
}

.tag-edit-colors {
  display: flex;
  gap: 6px;
}

.tag-actions {
  display: flex;
  gap: var(--space-sm);
  margin-left: auto;
}

.mr-1 { margin-right: 4px; }
</style>
