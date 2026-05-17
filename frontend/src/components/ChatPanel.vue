<template>
  <div class="chat-panel" :class="modeClass">
    <div class="chat-header" v-if="mode !== 'practice'">
      <div class="timer-bar">
        <el-progress
          :percentage="timerPercent"
          :stroke-width="6"
          :color="timerColor"
          :show-text="false"
        />
        <span class="timer-pill" :class="{ 'timer-danger': timerPercent <= 20 }">
          {{ formatTime(remainingTime) }}
        </span>
      </div>
    </div>

    <div class="chat-messages" ref="messagesContainer">
      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        class="chat-message"
        :class="[msg.role, msg.type]"
      >
        <div class="chat-bubble__avatar" v-if="msg.role === 'ai'">
          <el-icon :size="20"><Cpu /></el-icon>
        </div>
        <div
          class="chat-bubble md-content"
          :class="msg.role === 'ai' ? 'chat-bubble--ai' : msg.role === 'user' ? 'chat-bubble--user' : ''"
          v-html="renderMarkdown(msg.content)"
        />
        <div class="chat-bubble__avatar chat-bubble__avatar--user" v-if="msg.role === 'user'">
          <el-icon :size="20"><UserFilled /></el-icon>
        </div>
      </div>

      <div v-if="loading" class="chat-message ai">
        <div class="chat-bubble__avatar">
          <el-icon :size="20"><Cpu /></el-icon>
        </div>
        <div class="chat-bubble chat-bubble--ai">
          正在思考
          <span class="dot-flashing">
            <span></span><span></span><span></span>
          </span>
        </div>
      </div>

      <div ref="scrollAnchor" />
    </div>

    <div class="chat-input-area">
      <div class="chat-stats">
        <el-tag size="small" type="info">已答 {{ answerCount }} 题</el-tag>
        <span class="mode-badge" :class="mode">
          {{ mode === 'practice' ? '练习' : mode === 'strict' ? '严苛' : '模拟' }}
        </span>
      </div>
      <div class="chat-input-row">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="2"
          placeholder="输入你的回答..."
          :disabled="loading || finished"
          resize="none"
          @keydown.enter.exact.prevent="handleSend"
        />
        <div class="chat-buttons">
          <el-button
            type="primary"
            :disabled="!inputText.trim() || loading || finished"
            @click="handleSend"
          >
            发送
          </el-button>
          <el-button
            type="warning"
            :disabled="loading || finished"
            @click="$emit('finish')"
          >
            结束面试
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onUnmounted, onMounted } from 'vue'
import { Cpu, UserFilled } from '@element-plus/icons-vue'
import { marked } from 'marked'

const props = defineProps({
  messages: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  finished: { type: Boolean, default: false },
  answerCount: { type: Number, default: 0 },
  mode: { type: String, default: 'simulation' }
})

const emit = defineEmits(['send', 'finish'])

const inputText = ref('')
const messagesContainer = ref(null)
const scrollAnchor = ref(null)
const remainingTime = ref(0)
let timerInterval = null

const timeLimit = computed(() => props.mode === 'strict' ? 60 : 120)
const timerPercent = computed(() => {
  if (timeLimit.value <= 0) return 100
  return Math.round((remainingTime.value / timeLimit.value) * 100)
})
const timerColor = computed(() => {
  if (timerPercent.value <= 20) return '#f56c6c'
  if (timerPercent.value <= 50) return '#e6a23c'
  return '#67c23a'
})
const modeClass = computed(() => 'mode-' + props.mode)

function formatTime(seconds) {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}

function startTimer() {
  remainingTime.value = timeLimit.value
  clearInterval(timerInterval)
  timerInterval = setInterval(() => {
    if (remainingTime.value > 0) {
      remainingTime.value--
    } else {
      clearInterval(timerInterval)
    }
  }, 1000)
}

function resumeTimer() {
  clearInterval(timerInterval)
  timerInterval = setInterval(() => {
    if (remainingTime.value > 0) {
      remainingTime.value--
    } else {
      clearInterval(timerInterval)
    }
  }, 1000)
}

watch(() => props.answerCount, () => {
  if (props.mode !== 'practice') {
    startTimer()
  }
})

watch(() => props.loading, (val) => {
  if (val) {
    clearInterval(timerInterval)
    timerInterval = null
  } else if (props.mode !== 'practice' && remainingTime.value > 0) {
    resumeTimer()
  }
})

onUnmounted(() => {
  clearInterval(timerInterval)
})

onMounted(() => {
  if (props.mode !== 'practice') {
    startTimer()
  }
})

marked.setOptions({
  breaks: true,
  gfm: true
})

function renderMarkdown(text) {
  if (!text) return ''
  return marked.parse(text)
}

function handleSend() {
  if (!inputText.value.trim() || props.loading || props.finished) return
  emit('send', inputText.value.trim())
  inputText.value = ''
}

function scrollToBottom() {
  nextTick(() => {
    scrollAnchor.value?.scrollIntoView({ behavior: 'smooth' })
  })
}

watch(() => props.messages.length, scrollToBottom)
watch(() => {
  const msgs = props.messages
  if (msgs.length && msgs[msgs.length - 1]) return msgs[msgs.length - 1].content
  return ''
}, scrollToBottom)
</script>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 280px);
  min-height: 500px;
  border: 1px solid #E5E7EB;
  border-radius: 12px;
  background-color: #FFFFFF;
  overflow: hidden;
}

.chat-panel.mode-practice {
  border-color: #15803D;
}

.chat-panel.mode-strict {
  border-color: #DC2626;
}

.chat-header {
  padding: 8px 16px;
  border-bottom: 1px solid #E5E7EB;
}

.timer-bar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.timer-bar :deep(.el-progress) {
  flex: 1;
}

.timer-pill {
  background-color: #111827;
  color: #FFFFFF;
  border-radius: 9999px;
  padding: 2px 10px;
  font-size: 12px;
  font-weight: 600;
  min-width: 36px;
  text-align: center;
}

.timer-pill.timer-danger {
  background-color: #DC2626;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background-color: #FFFFFF;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-message {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.chat-message.user {
  justify-content: flex-end;
}

.chat-bubble__avatar--user {
  background-color: #F3F4F6;
  color: #6B7280;
}

.chat-stats {
  padding: 4px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.mode-badge {
  font-size: 12px;
  padding: 1px 8px;
  border-radius: 9999px;
  font-weight: 500;
}

.mode-badge.practice {
  background-color: rgba(21, 128, 61, 0.1);
  color: #15803D;
}

.mode-badge.simulation {
  background-color: rgba(99, 102, 241, 0.1);
  color: #6366F1;
}

.mode-badge.strict {
  background-color: rgba(220, 38, 38, 0.1);
  color: #DC2626;
}

.chat-input-area {
  border-top: 1px solid #E5E7EB;
  padding: 12px 16px;
  background-color: #FFFFFF;
}

.chat-input-row {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.chat-input-row :deep(.el-textarea) {
  flex: 1;
}

.chat-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
}
</style>
