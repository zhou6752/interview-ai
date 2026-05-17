<template>
  <div class="page-container">
    <h2 class="text-h1" style="margin-bottom: 24px;">模拟面试</h2>

    <el-card v-if="!started" class="setup-card">
      <div class="form-section">
        <label class="setup-label">面试岗位</label>
        <el-select v-model="position" placeholder="请选择面试岗位" style="width: 100%">
          <el-option
            v-for="skill in skills"
            :key="skill.id"
            :label="skill.name"
            :value="skill.id"
          >
            <span>{{ skill.name }}</span>
            <span class="option-desc">{{ skill.description }}</span>
          </el-option>
        </el-select>
      </div>

      <div class="form-section">
        <label class="setup-label">面试模式</label>
        <div class="mode-selector grid-3">
          <div
            :class="['mode-card', { 'is-active': interviewMode === 'practice' }]"
            @click="interviewMode = 'practice'"
          >
            <div :class="['mode-card__icon', interviewMode === 'practice' ? 'mode-card__icon--is-active' : 'mode-card__icon--default']">
              <el-icon :size="20"><EditPen /></el-icon>
            </div>
            <h3 class="text-h3" style="margin-bottom: 4px;">练习模式</h3>
            <p class="text-caption">无时间限制，AI 引导式对话</p>
          </div>
          <div
            :class="['mode-card', { 'is-active': interviewMode === 'simulation' }]"
            @click="interviewMode = 'simulation'"
          >
            <div :class="['mode-card__icon', interviewMode === 'simulation' ? 'mode-card__icon--is-active' : 'mode-card__icon--default']">
              <el-icon :size="20"><Monitor /></el-icon>
            </div>
            <h3 class="text-h3" style="margin-bottom: 4px;">模拟面试</h3>
            <p class="text-caption">每题限时 2 分钟，真实场景</p>
          </div>
          <div
            :class="['mode-card', { 'is-active': interviewMode === 'strict' }]"
            @click="interviewMode = 'strict'"
          >
            <div :class="['mode-card__icon', interviewMode === 'strict' ? 'mode-card__icon--is-active' : 'mode-card__icon--default']">
              <el-icon :size="20"><WarningFilled /></el-icon>
            </div>
            <h3 class="text-h3" style="margin-bottom: 4px;">严苛挑战</h3>
            <p class="text-caption">每题限时 1 分钟，压力追问</p>
          </div>
        </div>
        <div class="mode-desc">
          <span v-if="interviewMode === 'practice'">无时间限制，AI 像导师一样引导，答不上来给提示</span>
          <span v-else-if="interviewMode === 'strict'">每题限时 1 分钟，AI 压力追问到底</span>
          <span v-else>每题限时 2 分钟，模拟真实面试场景</span>
        </div>
      </div>

      <div v-if="resumeData" class="form-section">
        <el-tag type="success" effect="plain">已关联简历诊断结果，面试题将优先从你的简历技术栈抽取</el-tag>
      </div>

      <el-button type="primary" size="large" :loading="starting" @click="handleStart" style="width: 100%">
        开始面试
      </el-button>
      <span v-if="!resumeData" class="text-caption" style="display: block; margin-top: 8px; text-align: center;">
        直接开始将使用题库随机抽题
      </span>
    </el-card>

    <div v-else class="interview-layout">
      <div class="interview-main">
        <ChatPanel
          :messages="messages"
          :loading="waiting"
          :finished="finished"
          :answer-count="answerCount"
          :mode="interviewMode"
          @send="handleSend"
          @finish="handleFinish"
        />
      </div>
    </div>

    <ReportDialog v-model:visible="reportDialogVisible" :report="reportData" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { EditPen, Monitor, WarningFilled } from '@element-plus/icons-vue'
import ChatPanel from '../components/ChatPanel.vue'
import ReportDialog from '../components/ReportDialog.vue'
import { startInterview, answerQuestion, finishInterview, getSkills } from '../api/index.js'

const route = useRoute()

const position = ref('java-backend')
const skills = ref([])
const interviewMode = ref('simulation')
const resumeData = ref(null)
const started = ref(false)
const starting = ref(false)
const waiting = ref(false)
const finished = ref(false)
const sessionToken = ref('')
const messages = ref([])
const answerCount = ref(0)
const totalQuestions = ref(0)
const currentIndex = ref(0)
const showFinishButton = ref(false)
const reportData = ref(null)
const reportDialogVisible = ref(false)

onMounted(async () => {
  try {
    const data = await getSkills()
    skills.value = data
    if (data.length > 0 && !data.find(s => s.id === position.value)) {
      position.value = data[0].id
    }
  } catch {
    skills.value = [
      { id: 'java-backend', name: 'Java 后端开发', description: 'Java/Spring/数据库/分布式' },
      { id: 'general', name: '通用技术面试', description: '数据结构/网络/操作系统' }
    ]
  }
  if (route.query.position) {
    position.value = route.query.position
  }
  if (route.query.resumeResult) {
    try {
      resumeData.value = JSON.parse(route.query.resumeResult)
    } catch {
      resumeData.value = null
    }
  }
  if (route.query.sessionToken) {
    sessionToken.value = route.query.sessionToken
    await handleStart()
  }
})

async function handleStart() {
  if (!position.value) {
    ElMessage.warning('请选择面试岗位')
    return
  }
  starting.value = true
  try {
    const data = await startInterview(position.value, resumeData.value, interviewMode.value, sessionToken.value || null)
    sessionToken.value = data.sessionToken
    totalQuestions.value = data.totalQuestions || 0
    currentIndex.value = data.currentQuestionIndex || 0
    messages.value.push({ role: 'ai', content: data.firstQuestion })
    started.value = true
    answerCount.value = 0
  } catch (err) {
    const msg = err.response?.data?.message || err.message || '启动面试失败'
    ElMessage.error(msg)
  } finally {
    starting.value = false
  }
}

async function handleSend(text) {
  messages.value.push({ role: 'user', content: text })
  waiting.value = true
  answerCount.value++

  await answerQuestion(
    sessionToken.value,
    text,
    (event) => {
      switch (event.type) {
        case 'next_question':
          currentIndex.value = event.questionIndex
          totalQuestions.value = event.totalQuestions
          const prefix = event.isFollowUp ? '[追问] ' : ''
          messages.value.push({
            role: 'ai',
            content: prefix + event.question,
            difficulty: event.difficulty,
            category: event.category
          })
          break
        case 'all_answered':
          messages.value.push({
            role: 'system',
            content: event.message
          })
          showFinishButton.value = true
          break
        case 'error':
          ElMessage.error(event.data || '出错了')
          break
      }
    },
    () => {
      waiting.value = false
    },
    (err) => {
      waiting.value = false
      const msg = err.response?.data?.message || err.message || '请求失败'
      ElMessage.error(msg)
    }
  )
}

async function handleFinish() {
  finished.value = true
  try {
    const data = await finishInterview(sessionToken.value)
    reportData.value = data
    reportDialogVisible.value = true
  } catch (err) {
    const msg = err.response?.data?.message || err.message || '报告生成失败'
    ElMessage.error(msg)
    finished.value = false
  }
}
</script>

<style scoped>
.setup-card {
  max-width: 640px;
  margin: 0 auto;
}

.form-section {
  margin-bottom: 24px;
}

.setup-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

.mode-selector {
  margin-bottom: 12px;
}

.mode-desc {
  font-size: 13px;
  color: #6B7280;
  line-height: 1.5;
}

.option-desc {
  float: right;
  color: #9CA3AF;
  font-size: 12px;
}

.interview-layout {
  display: flex;
  gap: 16px;
  max-width: 1000px;
  margin: 0 auto;
}

.interview-main {
  flex: 1;
  min-width: 0;
}
</style>
