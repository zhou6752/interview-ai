<template>
  <div class="page-container">
    <h1 class="text-h1">我的面试历史</h1>

    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <p>加载中...</p>
    </div>

    <div v-else-if="historyList.length === 0" class="empty-container">
      <el-empty description="暂无面试记录，开始你的第一场模拟面试吧" />
      <el-button type="primary" @click="$router.push('/')">去首页</el-button>
    </div>

    <div v-else class="grid-3">
      <el-card
        v-for="item in historyList"
        :key="item.sessionToken"
        class="history-card surface-card"
        :class="{ 'history-card-clickable': item.status === 'COMPLETED' && item.reportJson }"
        @click="item.status === 'COMPLETED' && item.reportJson && showHistoryReport(item)"
      >
        <div class="history-header flex-between">
          <el-tag :type="statusTagType(item.status)" size="small">
            {{ statusLabel(item.status) }}
          </el-tag>
          <div v-if="item.totalScore" class="history-score">
            <span class="score-num">{{ item.totalScore }}</span>
            <span class="score-label">分</span>
          </div>
        </div>

        <div class="text-h2 history-position">{{ item.position }}</div>
        <div class="text-caption history-time">{{ formatTime(item.startTime) }}</div>

        <div class="history-footer">
          <div class="history-footer-info">
            <span class="question-count">共 {{ item.questionCount }} 题</span>
            <span v-if="item.status === 'IN_PROGRESS' && item.remainingSeconds > 0" class="remaining-time">
              ⏱ 剩余 {{ formatRemaining(item.remainingSeconds) }}
            </span>
          </div>
          <div class="history-actions">
            <el-button
              v-if="item.status === 'IN_PROGRESS'"
              size="small"
              @click.stop="continueInterview(item)"
            >
              继续面试
            </el-button>
            <el-button
              size="small"
              type="danger"
              plain
              @click.stop="deleteHistory(item)"
            >
              删除
            </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <ReportDialog v-model:visible="historyReportVisible" :report="historyReportData" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { getInterviewHistory, deleteInterviewHistory } from '../api/index.js'
import ReportDialog from '../components/ReportDialog.vue'

const router = useRouter()
const loading = ref(true)
const historyList = ref([])
const historyReportVisible = ref(false)
const historyReportData = ref(null)

function formatTime(time) {
  if (!time) return ''
  return time.replace('T', ' ')
}

function statusTagType(status) {
  if (status === 'COMPLETED') return 'success'
  if (status === 'EXPIRED') return 'danger'
  return 'warning'
}

function statusLabel(status) {
  if (status === 'COMPLETED') return '已完成'
  if (status === 'EXPIRED') return '已过期'
  return '进行中'
}

function formatRemaining(seconds) {
  if (seconds <= 0) return '0 分钟'
  const mins = Math.floor(seconds / 60)
  if (mins >= 60) {
    const hours = Math.floor(mins / 60)
    const remainMins = mins % 60
    return remainMins > 0 ? `${hours} 小时 ${remainMins} 分钟` : `${hours} 小时`
  }
  return `${mins} 分钟`
}

function showHistoryReport(item) {
  if (item.reportJson) {
    try {
      historyReportData.value = JSON.parse(item.reportJson)
    } catch {
      historyReportData.value = { overallComment: '报告数据解析失败' }
    }
  }
  historyReportVisible.value = true
}

function continueInterview(item) {
  router.push({ path: '/interview', query: { sessionToken: item.sessionToken, position: item.position } })
}

async function deleteHistory(item) {
  try {
    await ElMessageBox.confirm('确定要删除这条面试记录吗？删除后无法恢复。', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteInterviewHistory(item.sessionToken)
    ElMessage.success('已删除')
    historyList.value = historyList.value.filter(h => h.sessionToken !== item.sessionToken)
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err.response?.data?.message || err.message || '删除失败')
    }
  }
}

onMounted(async () => {
  try {
    historyList.value = await getInterviewHistory()
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.loading-container {
  text-align: center;
  padding: 60px 0;
  color: #9CA3AF;
}

.empty-container {
  text-align: center;
  padding: 40px 0;
}

.history-card {
  border: 1px solid #E5E7EB;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-card-clickable {
  cursor: pointer;
}

.history-header {
  align-items: center;
}

.history-position {
  margin-top: 8px;
}

.history-time {
  margin-bottom: 8px;
}

.history-score .score-num {
  font-size: 28px;
  font-weight: 700;
  color: #6366F1;
}

.history-score .score-label {
  font-size: 12px;
  color: #9CA3AF;
  margin-left: 2px;
}

.history-footer {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.history-footer-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.question-count {
  color: #9CA3AF;
  font-size: 13px;
}

.remaining-time {
  color: #6B7280;
  font-size: 13px;
}

.history-actions {
  display: flex;
  gap: 8px;
}
</style>
