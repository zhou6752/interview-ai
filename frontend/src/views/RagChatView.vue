<template>
  <div class="page-container">
    <h1 class="text-h1" style="margin-bottom: 24px">知识库问答</h1>
    <div class="rag-layout">
      <div class="session-sidebar">
        <el-button type="primary" size="small" @click="openCreateDialog" style="width:100%;margin-bottom:12px">
          + 新建对话
        </el-button>
        <div v-if="sessions.length === 0" class="empty-hint">暂无对话</div>
        <div
          v-for="s in sessions"
          :key="s.id"
          class="session-item"
          :class="{ active: activeSession?.id === s.id }"
          @click="selectSession(s)"
        >
          <span class="session-title">{{ s.title || '新对话' }}</span>
          <el-button text size="small" type="danger" @click.stop="deleteSession(s.id)">×</el-button>
        </div>
      </div>

      <div class="chat-area" v-if="activeSession">
        <div class="chat-messages" ref="msgContainer">
          <div v-if="messages.length === 0 && !loading" class="empty-hint">开始提问吧</div>
          <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role">
            <div v-if="m.role === 'ai'" class="chat-bubble--ai md-content" v-html="renderMarkdown(m.content)"></div>
            <div v-else class="chat-bubble--user">{{ m.content }}</div>
          </div>
          <div v-if="loading" class="msg ai">
            <div class="chat-bubble--ai" style="color: #4B5563">思考中...</div>
          </div>
        </div>
        <div class="chat-input">
          <el-input v-model="input" placeholder="输入问题..." @keyup.enter="send" :disabled="loading" />
          <el-button type="primary" :disabled="!input.trim() || loading" @click="send">发送</el-button>
        </div>
      </div>
      <div v-else class="chat-area empty-chat">
        <span>选择一个对话或新建一个开始提问</span>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="选择知识库" width="500px">
      <div v-if="knowledgeList.length === 0" class="empty-hint">
        暂无可用的知识库文档，请先在「知识库管理」中上传文档
      </div>
      <el-checkbox-group v-model="selectedNames">
        <div v-for="item in knowledgeList" :key="item.name" class="kb-checkbox-item">
          <el-checkbox :value="item.name">{{ item.name }}</el-checkbox>
        </div>
      </el-checkbox-group>
      <div v-if="selectedNames.length === 0 && knowledgeList.length > 0" class="select-warning">
        请至少选择一个知识库
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCreate" :disabled="selectedNames.length === 0">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { apiClient } from '../api/index.js'
import { marked } from 'marked'

marked.setOptions({ breaks: true, gfm: true });

const sessions = ref([])
const activeSession = ref(null)
const messages = ref([])
const input = ref('')
const loading = ref(false)
const msgContainer = ref(null)
const dialogVisible = ref(false)
const knowledgeList = ref([])
const selectedNames = ref([])

apiClient.get('/rag-chat/sessions').then(res => {
  sessions.value = res.data || []
}).catch(() => sessions.value = [])

async function createSession(knowledgeBaseNames) {
  try {
    const res = await apiClient.post('/rag-chat/sessions', { title: '新对话', knowledgeBaseNames: knowledgeBaseNames || [] })
    sessions.value.unshift(res.data)
    selectSession(res.data)
  } catch { ElMessage.error('创建失败') }
}

async function openCreateDialog() {
  dialogVisible.value = true
  selectedNames.value = []
  try {
    const res = await apiClient.get('/knowledge/list')
    knowledgeList.value = res.data || []
  } catch {
    knowledgeList.value = []
  }
}

async function confirmCreate() {
  dialogVisible.value = false
  await createSession(selectedNames.value)
}

async function selectSession(s) {
  activeSession.value = s
  messages.value = []
  try {
    const res = await apiClient.get(`/rag-chat/sessions/${s.id}/messages`)
    messages.value = res.data || []
    await nextTick()
    scrollBottom()
  } catch { /* 新会话无消息 */ }
}

async function deleteSession(id) {
  try {
    await apiClient.delete(`/rag-chat/sessions/${id}`)
    sessions.value = sessions.value.filter(s => s.id !== id)
    if (activeSession.value?.id === id) activeSession.value = null
  } catch { ElMessage.error('删除失败') }
}

async function send() {
  if (!input.value.trim() || loading.value) return
  const q = input.value.trim()
  input.value = ''
  messages.value.push({ role: 'user', content: q })
  loading.value = true

  const token = localStorage.getItem('token')
  try {
    const res = await fetch(`/api/rag-chat/sessions/${activeSession.value.id}/messages/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify({ question: q })
    })

    const reader = res.body.getReader()
    const decoder = new TextDecoder()
    const aiMsg = { role: 'ai', content: '' }
    messages.value.push(aiMsg)
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      const events = buffer.split('\n\n')
      buffer = events.pop()

      for (const event of events) {
        for (const line of event.split('\n')) {
          if (line.startsWith('data:')) {
            aiMsg.content += line.substring(5).trimStart()
          }
        }
      }
      await nextTick()
      scrollBottom()
    }

    for (const line of buffer.split('\n')) {
      if (line.startsWith('data:')) {
        aiMsg.content += line.substring(5).trimStart()
      }
    }
  } catch (e) {
    messages.value.push({ role: 'ai', content: '【错误】请求失败: ' + e.message })
  } finally {
    loading.value = false
  }
}

function scrollBottom() {
  const el = msgContainer.value
  if (el) el.scrollTop = el.scrollHeight
}

function renderMarkdown(text) {
  if (!text) return ''
  return marked.parse(text)
}
</script>

<style scoped>
.rag-layout {
  display: flex;
  gap: 16px;
  height: calc(100vh - 140px);
}

.session-sidebar {
  width: 220px;
  padding-right: 12px;
  overflow-y: auto;
  flex-shrink: 0;
  background: #FFFFFF;
  border-right: 1px solid #E5E7EB;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 8px;
  cursor: pointer;
  border-radius: 12px;
  margin-bottom: 4px;
  color: #6B7280;
}

.session-item:hover {
  background: #F9FAFB;
}

.session-item.active {
  background: #EEF2FF;
}

.session-item.active .session-title {
  color: #6366F1;
}

.session-title {
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  color: #6B7280;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.msg {
  margin-bottom: 12px;
}

.msg.user {
  text-align: right;
}

.msg.ai {
  text-align: left;
}

.msg .chat-bubble--ai,
.msg .chat-bubble--user {
  display: inline-block;
  max-width: 80%;
}

.chat-input {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid #E5E7EB;
}

.empty-hint {
  color: #9CA3AF;
  text-align: center;
  padding: 24px;
}

.empty-chat {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9CA3AF;
}

.kb-checkbox-item {
  margin-bottom: 8px;
}

.select-warning {
  color: #6B7280;
  font-size: 13px;
  margin-top: 8px;
}
</style>
