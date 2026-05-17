<template>
  <div class="page-container page-container--wide">
    <div class="flex-between" style="margin-bottom: 24px">
      <div>
        <h1 class="text-h1">知识库管理</h1>
        <p class="text-caption">管理您的知识库文件，查看使用统计</p>
      </div>
      <div class="gap-md" style="display:flex">
        <el-button type="primary" @click="showUploadDialog = true">
          <el-icon><Upload /></el-icon>上传知识库
        </el-button>
        <el-button @click="$router.push('/rag-chat')" plain>
          <el-icon><ChatDotSquare /></el-icon>问答助手
        </el-button>
      </div>
    </div>

    <div class="grid-3" style="margin-bottom: 24px">
      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--primary">
          <el-icon :size="22"><Document /></el-icon>
        </div>
        <div>
          <div class="stat-card__value">{{ stats.documentCount }}</div>
          <div class="stat-card__label">知识库总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--info">
          <el-icon :size="22"><Collection /></el-icon>
        </div>
        <div>
          <div class="stat-card__value">{{ stats.totalChunks }}</div>
          <div class="stat-card__label">向量总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--success">
          <el-icon :size="22"><ChatLineSquare /></el-icon>
        </div>
        <div>
          <div class="stat-card__value">{{ stats.totalQueries }}</div>
          <div class="stat-card__label">总提问次数</div>
        </div>
      </div>
    </div>

    <div class="flex-between gap-md" style="margin-bottom: 16px">
      <el-input v-model="searchKeyword" placeholder="搜索知识库名称..." clearable :prefix-icon="Search" style="width:280px" />
      <div class="gap-md" style="display:flex">
        <el-select v-model="sortBy" placeholder="按时间排序" style="width:140px" size="default">
          <el-option label="按时间降序" value="time-desc" />
          <el-option label="按时间升序" value="time-asc" />
          <el-option label="按大小降序" value="size-desc" />
          <el-option label="按提问数降序" value="query-desc" />
        </el-select>
        <el-select v-model="filterCategory" placeholder="全部分类" style="width:130px" size="default">
          <el-option label="全部分类" value="" />
          <el-option label="面试" value="面试" />
          <el-option label="技术" value="技术" />
          <el-option label="其他" value="其他" />
        </el-select>
      </div>
    </div>

    <div class="surface-card">
      <el-table :data="filteredList" stripe v-loading="loading" empty-text="暂无知识库文档，请上传">
        <el-table-column prop="name" label="名称" min-width="260">
          <template #default="{ row }">
            <div class="doc-name">
              <el-icon :size="18" style="color: #6366F1"><Document /></el-icon>
              <span class="doc-title">{{ row.name }}</span>
            </div>
            <div class="doc-filename">{{ row.fileName || row.name }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag :type="catType(row.category)" size="small">{{ row.category || '其他' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="大小" width="100">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="vectorStatus" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.vectorStatus)" size="small">{{ statusText(row.vectorStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="分块" width="75" />
        <el-table-column prop="queryCount" label="提问数" width="85" />
        <el-table-column prop="createTime" label="上传时间" min-width="140">
          <template #default="{ row }">{{ fmtDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="70">
          <template #default="{ row }">
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="showUploadDialog" title="上传知识库文档" width="520px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="选择文件">
          <el-upload ref="uploadRef" :auto-upload="false" :limit="1" :on-change="onFileChange"
            accept=".pdf,.txt,.md,.doc,.docx" drag>
            <el-icon :size="40"><UploadFilled /></el-icon>
            <div class="upload-text">拖拽文件到此处或点击上传</div>
            <template #tip>
              <div class="upload-tip">支持 PDF、TXT、Markdown、Word 文档</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="文档名称（选填）">
          <el-input v-model="uploadName" placeholder="默认使用文件名" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="uploadCategory" placeholder="选择分类">
            <el-option label="面试" value="面试" />
            <el-option label="技术" value="技术" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpload" :loading="uploading">开始上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Upload, UploadFilled, Document, Collection, ChatLineSquare, ChatDotSquare, Delete } from '@element-plus/icons-vue'
import { apiClient } from '../api/index.js'

const loading = ref(false)
const uploading = ref(false)
const searchKeyword = ref('')
const sortBy = ref('time-desc')
const filterCategory = ref('')
const showUploadDialog = ref(false)
const uploadFile = ref(null)
const uploadName = ref('')
const uploadCategory = ref('其他')
const uploadRef = ref(null)

const stats = ref({ documentCount: 0, totalChunks: 0, totalQueries: 0 })
const docList = ref([])

onMounted(() => {
  loadStats()
  loadList()
})

async function loadStats() {
  try {
    const res = await apiClient.get('/knowledge/stats')
    stats.value = res.data
  } catch (e) { /* ignore */ }
}

async function loadList() {
  loading.value = true
  try {
    const res = await apiClient.get('/knowledge/list')
    docList.value = res.data
  } catch (e) {
    ElMessage.error('加载文档列表失败')
  } finally {
    loading.value = false
  }
}

const filteredList = computed(() => {
  let arr = [...docList.value]
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    arr = arr.filter(d => (d.name || '').toLowerCase().includes(kw)
      || (d.fileName || '').toLowerCase().includes(kw))
  }
  if (filterCategory.value) {
    arr = arr.filter(d => d.category === filterCategory.value)
  }
  if (sortBy.value === 'time-asc') {
    arr.sort((a, b) => new Date(a.createTime) - new Date(b.createTime))
  } else if (sortBy.value === 'size-desc') {
    arr.sort((a, b) => (b.fileSize || 0) - (a.fileSize || 0))
  } else if (sortBy.value === 'query-desc') {
    arr.sort((a, b) => (b.queryCount || 0) - (a.queryCount || 0))
  } else {
    arr.sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
  }
  return arr
})

function formatSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / (1024 * 1024)).toFixed(1) + 'MB'
}

function fmtDate(d) {
  if (!d) return '-'
  return new Date(d).toLocaleDateString('zh-CN')
}

function catType(c) {
  if (c === '面试') return ''
  if (c === '技术') return 'success'
  return 'info'
}

function statusType(s) {
  if (s === 'COMPLETED') return 'success'
  if (s === 'PG_UNAVAILABLE') return 'danger'
  if (s === 'FAILED') return 'warning'
  return 'info'
}

function statusText(s) {
  if (s === 'COMPLETED') return '已完成'
  if (s === 'PG_UNAVAILABLE') return 'PG 未连接'
  if (s === 'FAILED') return '向量化失败'
  return s || '未知'
}

function onFileChange(file) {
  uploadFile.value = file.raw
  if (!uploadName.value) {
    uploadName.value = file.name
  }
}

async function handleUpload() {
  if (!uploadFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  uploading.value = true
  try {
    const form = new FormData()
    form.append('file', uploadFile.value)
    form.append('name', uploadName.value || uploadFile.value.name)
    form.append('category', uploadCategory.value)
    const res = await apiClient.post('/knowledge/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 180000
    })
    ElMessage.success('上传成功')
    const result = res.data
    if (result?.hint) ElMessage.warning(result.hint)
    showUploadDialog.value = false
    uploadFile.value = null
    uploadName.value = ''
    uploadRef.value?.clearFiles()
    loadStats()
    loadList()
  } catch (e) {
    ElMessage.error('上传失败: ' + (e.response?.data?.message || e.message))
  } finally {
    uploading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.name}」吗？`, '删除确认', { type: 'warning' })
    await apiClient.delete(`/knowledge/${encodeURIComponent(row.name)}`)
    ElMessage.success('已删除')
    loadStats()
    loadList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}
</script>

<style scoped>
.doc-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.doc-title {
  font-weight: 500;
  color: #1F2937;
}

.doc-filename {
  margin-top: 2px;
  font-size: 12px;
  color: #9CA3AF;
  padding-left: 26px;
}

.upload-text {
  margin-top: 10px;
  font-size: 14px;
  color: #4B5563;
}

.upload-tip {
  font-size: 12px;
  color: #9CA3AF;
}
</style>
