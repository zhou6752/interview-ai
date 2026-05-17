<template>
  <div class="page-container">
    <h2 class="text-h1">简历诊断</h2>

    <div class="surface-card upload-card">
      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        :limit="1"
        accept=".pdf,.docx,.doc,.txt"
        :on-change="handleFileChange"
        :before-upload="beforeUpload"
        :disabled="analyzing || polling"
      >
        <el-icon class="upload-icon" :size="48"><UploadFilled /></el-icon>
        <div class="upload-text">
          <p class="upload-hint">将简历文件拖到此处，或 <em>点击上传</em></p>
          <p class="upload-limit">支持 PDF / DOCX / DOC / TXT 格式，文件不超过 5MB</p>
        </div>
      </el-upload>

      <div class="upload-actions">
        <el-button
          type="primary"
          :loading="analyzing || polling"
          :disabled="!selectedFile"
          @click="submitAnalyze"
        >
          {{ polling ? '分析进度中...' : analyzing ? '同步分析中...' : '开始分析' }}
        </el-button>
        <el-button :disabled="analyzing || polling" @click="resetForm">清空</el-button>
      </div>
      <div v-if="polling && !result" class="analyzing-hint">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>AI 正在深度分析你的简历...{{ pollingText }}</span>
        <el-progress :percentage="pollingProgress" :show-text="false" style="width:200px;margin-top:8px" />
      </div>
      <div v-if="analyzing" class="analyzing-hint">
        <el-icon class="is-loading"><Loading /></el-icon>
        AI 正在深度分析你的简历，提取技术栈、评估技能水平并生成定向面试题，预计需要 30-60 秒，请耐心等待...
      </div>
    </div>

    <div v-if="result" class="result-section">
      <div class="surface-card score-summary-card">
        <div class="score-summary-header">
          <el-progress
            type="circle"
            :percentage="result.skillScore"
            :color="scoreColor"
            :width="120"
          >
            <span class="score-number">{{ result.skillScore }}</span>
            <span class="score-unit">分</span>
          </el-progress>
          <div class="score-summary-body">
            <span class="text-h2" style="margin-bottom: 8px;">综合评估</span>
            <p class="score-summary-text">{{ result.summary }}</p>
          </div>
        </div>
      </div>

      <div class="surface-card section-card" v-if="result.strengths && result.strengths.length">
        <h3 class="text-h2" style="margin-bottom: 16px;">亮点</h3>
        <div
          v-for="(item, idx) in result.strengths"
          :key="'s' + idx"
          class="color-bar-card"
        >
          <div class="color-bar-card__bar color-bar-card__bar--success"></div>
          <div class="color-bar-card__content text-body">{{ item }}</div>
        </div>
      </div>

      <div class="surface-card section-card" v-if="result.weaknesses && result.weaknesses.length">
        <h3 class="text-h2" style="margin-bottom: 16px;">不足</h3>
        <div
          v-for="(item, idx) in result.weaknesses"
          :key="'w' + idx"
          class="color-bar-card"
        >
          <div class="color-bar-card__bar color-bar-card__bar--warning"></div>
          <div class="color-bar-card__content text-body">{{ item }}</div>
        </div>
      </div>

      <div class="surface-card section-card" v-if="result.suggestions && result.suggestions.length">
        <h3 class="text-h2" style="margin-bottom: 16px;">改进建议</h3>
        <div
          v-for="(item, idx) in result.suggestions"
          :key="'g' + idx"
          class="improvement-item"
        >
          <span class="number-badge">{{ idx + 1 }}</span>
          <span class="text-body">{{ item }}</span>
        </div>
      </div>

      <el-card v-if="result.interviewQuestions && result.interviewQuestions.length" class="result-card">
        <template #header>
          <span class="text-h2">AI 生成的定向面试题</span>
        </template>
        <el-collapse accordion>
          <el-collapse-item
            v-for="(q, idx) in result.interviewQuestions"
            :key="idx"
            :title="'Q' + (idx + 1) + ': ' + q.question"
          >
            <p><el-tag size="small">知识点</el-tag> {{ q.expectedKnowledge }}</p>
            <p style="margin-top: 8px">
              <el-tag size="small" :type="q.difficulty === 'hard' ? 'danger' : q.difficulty === 'medium' ? 'warning' : 'success'">
                {{ q.difficulty === 'hard' ? '困难' : q.difficulty === 'medium' ? '中等' : '简单' }}
              </el-tag>
            </p>
          </el-collapse-item>
        </el-collapse>
      </el-card>

      <div class="start-interview-section" v-if="result">
        <el-button type="primary" size="large" @click="startInterviewWithResult">
          用此诊断开始面试
        </el-button>
      </div>
    </div>

    <el-card v-if="historyList.length > 0" class="history-section">
      <template #header><span class="text-h2">分析历史（{{ historyList.length }}）</span></template>
      <div v-for="item in historyList" :key="item.id" class="history-item">
        <span class="history-filename">{{ item.fileName }}</span>
        <el-tag size="small">{{ item.skillScore }} 分</el-tag>
        <span class="text-caption">{{ item.createTime?.replace('T',' ') }}</span>
        <el-button size="small" type="primary" link @click="viewHistory(item)">查看</el-button>
        <el-popconfirm title="确定删除？" @confirm="deleteHistoryItem(item.id)">
          <template #reference><el-button size="small" type="danger" link>删除</el-button></template>
        </el-popconfirm>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled, Loading } from '@element-plus/icons-vue'
import { analyzeResume, analyzeResumeAsync, getResumeTaskStatus, getResumeHistory, deleteResumeHistory, saveResumeHistory } from '../api/index.js'

const router = useRouter()

const uploadRef = ref(null)
const selectedFile = ref(null)
const analyzing = ref(false)
const polling = ref(false)
const pollingText = ref('')
const pollingProgress = ref(0)
const result = ref(null)
const historyList = ref([])

const scoreColor = computed(() => {
  if (!result.value) return '#409eff'
  const s = result.value.skillScore
  if (s >= 80) return '#67c23a'
  if (s >= 60) return '#e6a23c'
  return '#f56c6c'
})

function beforeUpload(file) {
  const validExts = ['.pdf', '.docx', '.doc', '.txt']
  const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase()
  if (!validExts.includes(ext)) {
    ElMessage.error('仅支持 PDF / DOCX / DOC / TXT 格式文件')
    return false
  }
  const isUnder5MB = file.size / 1024 / 1024 < 5
  if (!isUnder5MB) {
    ElMessage.error('文件大小不能超过 5MB')
    return false
  }
  return true
}

function handleFileChange(file) {
  selectedFile.value = file.raw
}

async function submitAnalyze() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择简历文件')
    return
  }
  result.value = null
  const ext = selectedFile.value.name.substring(selectedFile.value.name.lastIndexOf('.')).toLowerCase()
  if (ext === '.txt') {
    analyzing.value = true
    try {
      const data = await analyzeResume(selectedFile.value)
      result.value = data
      ElMessage.success('简历分析完成')
      loadHistory()
    } catch (err) {
      const msg = err.response?.data?.message || err.message || '分析失败，请重试'
      ElMessage.error(msg)
    } finally {
      analyzing.value = false
    }
    return
  }

  polling.value = true
  pollingProgress.value = 0
  pollingText.value = ''
  try {
    const { taskId } = await analyzeResumeAsync(selectedFile.value)
    await pollTaskStatus(taskId)
  } catch (err) {
    const msg = err.response?.data?.message || err.message || '分析失败，请重试'
    ElMessage.error(msg)
  } finally {
    polling.value = false
  }
}

async function pollTaskStatus(taskId) {
  const maxAttempts = 60
  for (let i = 0; i < maxAttempts; i++) {
    const status = await getResumeTaskStatus(taskId)
    pollingProgress.value = Math.min(90, (i / maxAttempts) * 100)
    pollingText.value = status.status === 'ANALYZING'
      ? ` (第 ${status.attempt} 次尝试...)`
      : status.status === 'PENDING'
        ? ' (排队中...)'
        : ''
    if (status.status === 'COMPLETED') {
      pollingProgress.value = 100
      try {
        result.value = JSON.parse(status.resultJson)
        ElMessage.success('简历分析完成')
        saveResumeHistory({
          fileName: selectedFile.value.name,
          resultJson: status.resultJson,
          skillScore: result.value.skillScore || 0
        })
        loadHistory()
      } catch {
        ElMessage.error('结果解析失败')
      }
      return
    }
    if (status.status === 'FAILED') {
      ElMessage.error(status.errorMsg || 'AI 分析失败，请重试')
      return
    }
    if (status.status === 'NOT_FOUND') {
      ElMessage.error('任务已过期，请重新上传')
      return
    }
    await new Promise(r => setTimeout(r, 2000))
  }
  ElMessage.warning('分析超时，请重试')
}

function resetForm() {
  selectedFile.value = null
  result.value = null
  uploadRef.value?.clearFiles()
}

function startInterviewWithResult() {
  router.push({
    path: '/interview',
    query: { resumeResult: JSON.stringify(result.value) }
  })
}

function viewHistory(item) {
  if (item.resultJson) {
    try {
      const parsed = JSON.parse(item.resultJson)
      if (!parsed.skillScore && !parsed.interviewQuestions && !parsed.strengths) {
        ElMessage.warning('该记录数据不完整（可能是旧版记录），建议删除后重新分析')
        return
      }
      result.value = parsed
      window.scrollTo({ top: document.querySelector('.result-section')?.offsetTop || 400, behavior: 'smooth' })
    } catch {
      ElMessage.error('历史数据解析失败，请删除后重新分析')
    }
  }
}

async function deleteHistoryItem(id) {
  try {
    await deleteResumeHistory(id)
    historyList.value = historyList.value.filter(item => item.id !== id)
    ElMessage.success('已删除')
  } catch (err) {
    const msg = err.response?.data?.message || err.message || '删除失败'
    ElMessage.error(msg)
  }
}

onMounted(() => {
  loadHistory()
})

async function loadHistory() {
  try {
    historyList.value = await getResumeHistory()
  } catch {
  }
}
</script>

<style scoped>
.upload-card :deep(.el-upload-dragger) {
  border: 2px dashed #D1D5DB;
  border-radius: 12px;
  background-color: #FFFFFF;
  padding: 40px 24px;
  transition: all 0.2s ease-out;
}
.upload-card :deep(.el-upload-dragger:hover) {
  border-color: #6366F1;
  background-color: rgba(99, 102, 241, 0.04);
}
.upload-card :deep(.el-upload-dragger.is-dragover) {
  border-color: #6366F1;
  background-color: rgba(99, 102, 241, 0.04);
}

.upload-icon {
  color: #9CA3AF;
}

.upload-text {
  margin-top: 12px;
}

.upload-hint {
  font-size: 14px;
  color: #6B7280;
}

.upload-hint em {
  color: #6366F1;
  font-style: normal;
}

.upload-limit {
  font-size: 12px;
  color: #9CA3AF;
  margin-top: 6px;
}

.upload-actions {
  margin-top: 24px;
  display: flex;
  gap: 8px;
  justify-content: center;
}

.analyzing-hint {
  color: #6B7280;
  font-size: 12px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
}

.result-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 24px;
}

.score-summary-card {
  margin-bottom: 0;
}

.score-summary-header {
  display: flex;
  align-items: center;
  gap: 24px;
}

.score-number {
  font-size: 28px;
  font-weight: 700;
  color: #1F2937;
}

.score-unit {
  font-size: 12px;
  color: #9CA3AF;
}

.score-summary-body {
  flex: 1;
}

.score-summary-text {
  font-size: 14px;
  color: #4B5563;
  line-height: 1.75;
  margin-top: 4px;
}

.section-card {
  margin-bottom: 0;
}

.color-bar-card {
  display: flex;
  align-items: stretch;
  border: 1px solid #E5E7EB;
  border-radius: 12px;
  overflow: hidden;
  background-color: #FFFFFF;
  margin-bottom: 8px;
}

.color-bar-card__bar {
  width: 4px;
  flex-shrink: 0;
}

.color-bar-card__bar--success {
  background-color: #15803D;
}

.color-bar-card__bar--warning {
  background-color: #B45309;
}

.color-bar-card__content {
  flex: 1;
  padding: 12px 16px;
}

.improvement-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 8px 0;
}

.improvement-item:last-child {
  padding-bottom: 0;
}

.number-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background-color: #EEF2FF;
  color: #6366F1;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
  margin-top: 2px;
}

.start-interview-section {
  text-align: center;
  padding: 24px 0;
}

.history-section {
  margin-top: 24px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 0;
  border-bottom: 1px solid #F3F4F6;
}

.history-item:last-child {
  border-bottom: none;
}

.history-filename {
  flex: 1;
  font-weight: 500;
  color: #1F2937;
  font-size: 14px;
}
</style>
