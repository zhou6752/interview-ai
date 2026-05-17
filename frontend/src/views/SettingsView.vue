<template>
  <div class="page-container">
    <h1 class="text-h1 settings-title">API 密钥管理</h1>

    <div class="surface-card status-card" :class="{ 'status-warning': !serviceStatus.chatConfigured }">
      <div class="status-row">
        <div class="status-item">
          <span :class="serviceStatus.chatConfigured ? 'status-dot--success' : 'status-dot--warning'"></span>
          <span class="status-label">聊天</span>
          <span class="status-value text-body">{{ serviceStatus.chatConfigured ? (serviceStatus.chatConfigName || serviceStatus.chatModel) : '未配置' }}</span>
          <span class="status-badge" :class="serviceStatus.chatConfigured ? 'badge-success' : 'badge-warning'">{{ serviceStatus.chatConfigured ? '已配置' : '未配置' }}</span>
        </div>
        <span class="status-divider"></span>
        <div class="status-item">
          <span :class="serviceStatus.embConfigured ? 'status-dot--success' : 'status-dot--warning'"></span>
          <span class="status-label">向量化</span>
          <span class="status-value text-body">{{ serviceStatus.embConfigured ? (serviceStatus.embConfigName || serviceStatus.embModel) : '未配置（知识库问答将不可用）' }}</span>
          <span class="status-badge" :class="serviceStatus.embConfigured ? 'badge-success' : 'badge-warning'">{{ serviceStatus.embConfigured ? '已配置' : '未配置' }}</span>
        </div>
      </div>
    </div>

    <div class="surface-card">
      <div class="card-header">
        <span class="text-h2">🗣 聊天服务</span>
        <span class="text-body card-subtitle">模拟面试、简历分析、知识库问答等功能共用此配置</span>
      </div>
      <el-form label-width="120px">
        <el-form-item label="配置名称">
          <el-input v-model="chatForm.configName" placeholder="如：我的 DeepSeek" />
        </el-form-item>
        <el-form-item label="预设平台">
          <el-select v-model="chatForm.selectedProvider" placeholder="选择预设平台（自动填充地址和模型）" style="width: 100%" @change="onProviderChange($event, 'chat')">
            <el-option label="不使用预设" value="" />
            <el-option v-for="p in providers" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型">
          <el-select v-model="chatForm.selectedModel" placeholder="选择模型" style="width: 100%" @change="onModelChange">
            <el-option v-for="m in modelOptions" :key="m.model" :label="m.name" :value="m.model" />
            <el-option label="自定义" value="__custom__" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型名称" v-if="chatForm.selectedModel === '__custom__'">
          <el-input v-model="chatForm.customModelName" placeholder="如：deepseek-chat、qwen-plus" />
          <div class="form-hint">请填写大模型服务商提供的模型标识符</div>
        </el-form-item>
        <el-form-item label="Base URL" v-if="chatForm.selectedModel === '__custom__'">
          <el-input v-model="chatForm.baseUrl" placeholder="如：https://api.deepseek.com" />
          <div class="form-hint">
            常见：DeepSeek 填 <code>https://api.deepseek.com</code>，通义千问兼容模式填 <code>https://dashscope.aliyuncs.com/compatible-mode</code>
          </div>
        </el-form-item>
        <el-form-item label="API 密钥">
          <el-input v-model="chatForm.apiKey" :type="showChatKey ? 'text' : 'password'" placeholder="sk-your-api-key-here">
            <template #suffix>
              <el-button text @click="showChatKey = !showChatKey">{{ showChatKey ? '隐藏' : '显示' }}</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button :loading="testingChat" @click="handleTestChat">测试连接</el-button>
          <el-button type="primary" :loading="savingChat" @click="handleSaveChat">保存</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="surface-card">
      <div class="card-header">
        <span class="text-h2">🔍 向量化服务</span>
        <span class="text-body card-subtitle" v-if="serviceStatus.chatSupportsEmbedding">聊天服务已支持向量化，可不单独配置</span>
        <span class="text-body card-subtitle" v-else>AI 将知识库文档转为向量以实现语义搜索</span>
      </div>
      <div class="embedding-toggle">
        <el-radio-group v-model="embDedicated" size="default">
          <el-radio :value="true">单独配置向量化服务</el-radio>
          <el-radio :value="false">不单独配置，复用聊天服务的 Embedding</el-radio>
        </el-radio-group>
      </div>
      <el-form label-width="120px" :disabled="!embDedicated" class="embedding-form">
        <el-form-item label="预设平台">
          <el-select v-model="embForm.selectedProvider" placeholder="推荐阿里云百炼" style="width: 100%" @change="onEmbProviderChange">
            <el-option v-for="p in embeddingProviders" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
          <div class="form-hint">推荐阿里云百炼（DashScope），唯一完备支持 chat+vector。DeepSeek / Kimi 不支持向量化</div>
        </el-form-item>
        <el-form-item label="Embedding 模型">
          <el-select v-model="embForm.embeddingModel" placeholder="选择模型" style="width: 100%">
            <el-option v-for="m in embModelOptions" :key="m.model" :label="m.label" :value="m.model" />
            <el-option label="自定义" value="__custom__" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型名称" v-if="embForm.embeddingModel === '__custom__'">
          <el-input v-model="embForm.customEmbModel" placeholder="如：text-embedding-v3" />
        </el-form-item>
        <el-form-item label="向量维度">
          <el-input-number v-model="embForm.embeddingDimensions" :min="128" :max="4096" />
          <div class="form-hint">常用 1024/1536，默认推荐 1024</div>
        </el-form-item>
        <el-form-item label="API 密钥">
          <el-input ref="embKeyInput" v-model="embForm.apiKey" :type="showEmbKey ? 'text' : 'password'" placeholder="sk-your-api-key-here">
            <template #suffix>
              <el-button text @click="showEmbKey = !showEmbKey">{{ showEmbKey ? '隐藏' : '显示' }}</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button :loading="testingEmbedding" @click="handleTestEmbedding">测试向量化</el-button>
          <el-button type="primary" :loading="savingEmbedding" @click="handleSaveEmbedding">保存</el-button>
        </el-form-item>
      </el-form>
      <el-alert
        v-if="!embDedicated && serviceStatus.chatConfigured && !serviceStatus.chatSupportsEmbedding"
        title="当前聊天模型不支持向量化"
        type="warning"
        :closable="false"
        show-icon
        style="margin-top: 12px"
      >
        如需启用知识库问答，请开启"单独配置"，使用阿里云百炼（DashScope）
      </el-alert>
    </div>

    <div class="surface-card" v-if="configs.length">
      <div class="card-header">
        <span class="text-h2">🔧 已保存的配置 ({{ configs.length }})</span>
      </div>
      <el-table :data="configs" style="width: 100%">
        <el-table-column label="角色" width="70">
          <template #default="{ row: cfg }">
            <span class="config-role">{{ getConfigRole(cfg) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="名称" min-width="140">
          <template #default="{ row: cfg }">
            <span class="config-name">{{ cfg.configName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="模型" min-width="160">
          <template #default="{ row: cfg }">
            <div class="config-model-cell">
              <span class="config-model">{{ cfg.modelName }}</span>
              <span v-if="cfg.embeddingDimensions" class="emb-dim">({{ cfg.embeddingDimensions }}维)</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="密钥" width="160">
          <template #default="{ row: cfg }">
            <span class="masked-key">{{ cfg.apiKeyMasked }}</span>
          </template>
        </el-table-column>
        <el-table-column label="标签" width="140">
          <template #default="{ row: cfg }">
            <div class="config-tags">
              <el-tag v-if="cfg.isActive" type="success" size="small">活跃</el-tag>
              <el-tag v-if="cfg.embeddingModel" size="small" type="info">Embedding</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260">
          <template #default="{ row: cfg }">
            <div class="config-actions">
              <el-button v-if="!cfg.isActive" type="primary" size="small" @click="handleSetActive(cfg.id)">设为活跃</el-button>
              <el-button v-if="cfg.supportsEmbedding && !cfg.useForEmbedding" type="success" size="small" @click="handleSetEmbedding(cfg.id)">设为Embedding</el-button>
              <el-button type="danger" size="small" text @click="handleDelete(cfg.id)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getSupportedModels, getApiConfigs, saveApiConfig,
  setActiveConfig, deleteApiConfig, testApiConnection, getActiveConfig, isLoggedIn,
  getProviders, getServiceStatus
} from '../api/index.js'
import { apiClient } from '../api/index.js'
import { useRouter } from 'vue-router'

const router = useRouter()
const configs = ref([])
const modelOptions = ref([])
const providers = ref([])
const savingChat = ref(false)
const savingEmbedding = ref(false)
const testingChat = ref(false)
const testingEmbedding = ref(false)
const showChatKey = ref(false)
const showEmbKey = ref(false)
const embDedicated = ref(false)
const embKeyInput = ref(null)

const serviceStatus = ref({
  chatConfigured: false,
  chatModel: null,
  chatConfigName: null,
  chatSupportsEmbedding: false,
  embConfigured: false,
  embModel: null,
  embConfigName: null,
  embDedicated: false,
  configured: false
})

const chatForm = reactive({
  configName: '',
  selectedProvider: '',
  selectedModel: '',
  customModelName: '',
  baseUrl: '',
  apiKey: ''
})

const embForm = reactive({
  selectedProvider: '',
  embeddingModel: '',
  customEmbModel: '',
  embeddingDimensions: 1024,
  apiKey: ''
})

const embeddingProviders = ref([])
const embModelOptions = ref([])

onMounted(async () => {
  if (!isLoggedIn()) {
    router.push('/login')
    return
  }
  try { modelOptions.value = await getSupportedModels() } catch { }
  try {
    providers.value = await getProviders()
    embeddingProviders.value = providers.value.filter(p => p.supportsEmbedding)
  } catch { }
  await loadData()
})

watch(embDedicated, async (val) => {
  if (val) {
    await nextTick()
    embKeyInput.value?.focus()
  }
})

function onProviderChange(providerId, formType) {
  if (!providerId) return
  const provider = providers.value.find(p => p.id === providerId)
  if (!provider) return
  const form = formType === 'chat' ? chatForm : embForm
  form.baseUrl = provider.baseUrl
  if (formType === 'chat') {
    form.selectedModel = provider.defaultModel
    form.customModelName = ''
  }
}

function onEmbProviderChange(providerId) {
  if (!providerId) return
  const provider = providers.value.find(p => p.id === providerId)
  if (!provider) return
  embForm.baseUrl = provider.baseUrl
  if (provider.embModels && provider.embModels.length) {
    embModelOptions.value = provider.embModels
    embForm.embeddingModel = provider.embModels[0].model
    embForm.embeddingDimensions = provider.embModels[0].dimensions || 1024
  }
}

async function loadData() {
  try {
    const [list, statusData] = await Promise.all([
      getApiConfigs(),
      getServiceStatus().catch(() => null)
    ])
    configs.value = list || []
    if (statusData) {
      serviceStatus.value = statusData
    }
  } catch { }
}

function onModelChange(val) {
  if (val !== '__custom__') {
    const found = modelOptions.value.find(m => m.model === val)
    chatForm.baseUrl = found ? found.baseUrl : ''
    chatForm.customModelName = ''
  } else {
    chatForm.baseUrl = ''
  }
}

function resolveChatModelName() {
  if (chatForm.selectedModel === '__custom__') return chatForm.customModelName.trim()
  return chatForm.selectedModel
}

function resolveEmbModelName() {
  if (embForm.embeddingModel === '__custom__') return embForm.customEmbModel.trim()
  return embForm.embeddingModel
}

async function handleTestChat() {
  if (!chatForm.apiKey?.trim()) { ElMessage.warning('请先输入 API 密钥'); return }
  const modelName = resolveChatModelName()
  if (!modelName) { ElMessage.warning('请选择或输入模型名称'); return }
  testingChat.value = true
  try {
    const res = await testApiConnection({ apiKey: chatForm.apiKey.trim(), modelName, baseUrl: chatForm.baseUrl })
    res.success ? ElMessage.success(res.message) : ElMessage.error(res.message)
  } catch (err) {
    ElMessage.error(err.response?.data?.message || err.message || '测试失败')
  } finally { testingChat.value = false }
}

async function handleTestEmbedding() {
  if (!embForm.apiKey?.trim()) { ElMessage.warning('请先输入 API 密钥'); return }
  const modelName = resolveEmbModelName() || embForm.embeddingModel
  if (!modelName) { ElMessage.warning('请选择 Embedding 模型'); return }
  testingEmbedding.value = true
  try {
    const res = await testApiConnection({ apiKey: embForm.apiKey.trim(), modelName, baseUrl: embForm.baseUrl })
    res.success ? ElMessage.success(res.message) : ElMessage.error(res.message)
  } catch (err) {
    ElMessage.error(err.response?.data?.message || err.message || '测试失败')
  } finally { testingEmbedding.value = false }
}

async function handleSaveChat() {
  if (!chatForm.configName?.trim()) { ElMessage.warning('请输入配置名称'); return }
  if (!chatForm.apiKey?.trim()) { ElMessage.warning('请输入 API 密钥'); return }
  if (!chatForm.selectedModel) { ElMessage.warning('请选择模型'); return }
  const modelName = resolveChatModelName()
  if (!modelName) { ElMessage.warning('请输入模型名称'); return }
  savingChat.value = true
  try {
    await saveApiConfig({
      configName: chatForm.configName.trim(),
      modelName,
      supportsEmbedding: false,
      baseUrl: chatForm.baseUrl,
      embeddingModel: null,
      apiKey: chatForm.apiKey.trim()
    })
    ElMessage.success('聊天配置已保存')
    if (!serviceStatus.value.chatConfigured) {
      await loadData()
    } else {
      await loadData()
    }
    chatForm.configName = ''
    chatForm.apiKey = ''
    chatForm.customModelName = ''
  } catch (err) {
    ElMessage.error(err.response?.data?.message || err.message || '保存失败')
  } finally { savingChat.value = false }
}

async function handleSaveEmbedding() {
  if (!embDedicated.value) { ElMessage.warning('请先开启单独配置'); return }
  if (!embForm.apiKey?.trim()) { ElMessage.warning('请先输入 API 密钥'); return }
  if (!embForm.embeddingModel) { ElMessage.warning('请选择 Embedding 模型'); return }
  const modelName = resolveEmbModelName() || embForm.embeddingModel
  savingEmbedding.value = true
  try {
    const saved = await saveApiConfig({
      configName: '🔍 ' + (embForm.embeddingModel || 'Embedding'),
      modelName: modelName,
      supportsEmbedding: !!embForm.embeddingModel,
      baseUrl: embForm.baseUrl,
      embeddingModel: embForm.embeddingModel,
      embeddingDimensions: embForm.embeddingDimensions || 1024,
      apiKey: embForm.apiKey.trim()
    })
    if (saved && saved.id) {
      await apiClient.post(`/config/set-embedding/${saved.id}`)
    }
    ElMessage.success('向量化配置已保存')
    embForm.apiKey = ''
    await loadData()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || err.message || '保存失败')
  } finally { savingEmbedding.value = false }
}

async function handleSetActive(id) {
  try {
    await setActiveConfig(id)
    ElMessage.success('已切换配置')
    await loadData()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || err.message || '切换失败')
  }
}

async function handleSetEmbedding(id) {
  try {
    await apiClient.post(`/config/set-embedding/${id}`)
    ElMessage.success('已设为 Embedding 专用服务商')
    await loadData()
  } catch { ElMessage.error('设置失败') }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除此配置？', '提示', { type: 'warning' })
    await deleteApiConfig(id)
    ElMessage.success('已删除')
    await loadData()
  } catch { }
}

function getConfigRole(cfg) {
  if (cfg.isActive && cfg.supportsEmbedding) return '🗣+🔍'
  if (cfg.isActive) return '🗣'
  if (cfg.supportsEmbedding) return '🔍'
  return ''
}
</script>

<style scoped>
.settings-title {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #E5E7EB;
}

.card-subtitle {
  color: #9CA3AF;
}

.status-card {
  margin-bottom: 16px;
  border-left: 4px solid #15803D;
}

.status-card.status-warning {
  border-left-color: #6366F1;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6B7280;
}

.status-label {
  font-weight: 500;
  color: #4B5563;
}

.status-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 9999px;
}

.badge-success {
  background-color: rgba(21, 128, 61, 0.12);
  color: #15803D;
}

.badge-warning {
  background-color: rgba(99, 102, 241, 0.12);
  color: #6366F1;
}

.status-divider {
  width: 1px;
  height: 20px;
  background-color: #E5E7EB;
}

.embedding-toggle {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #E5E7EB;
}

.embedding-form {
  margin-top: 8px;
}

.config-name {
  font-weight: 500;
  color: #1F2937;
}

.config-model {
  color: #9CA3AF;
}

.config-model-cell {
  display: flex;
  align-items: center;
  gap: 4px;
}

.emb-dim {
  font-size: 12px;
  color: #9CA3AF;
}

.config-role {
  font-size: 16px;
}

.config-tags {
  display: flex;
  align-items: center;
  gap: 6px;
}

.masked-key {
  font-size: 12px;
  color: #9CA3AF;
  font-family: monospace;
}

.config-actions {
  display: flex;
  gap: 8px;
  flex-wrap: nowrap;
}

.form-hint {
  font-size: 12px;
  color: #9CA3AF;
  margin-top: 4px;
  line-height: 1.5;
}

.form-hint code {
  padding: 1px 4px;
  background: #F3F4F6;
  color: #4B5563;
  border-radius: 4px;
  font-size: 11px;
}
</style>
