import axios from 'axios'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 120000,
  headers: { 'Content-Type': 'application/json' }
})

apiClient.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  res => {
    // 自动解包 Result<T>: { code, message, data, timestamp } → data
    const body = res.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) {
        res.data = body.data
      } else {
        return Promise.reject(new Error(body.message || '请求失败'))
      }
    }
    return res
  },
  err => {
    if (err.response && (err.response.status === 401 || err.response.status === 403)) {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export function getToken() { return localStorage.getItem('token') }
export function setToken(token) { localStorage.setItem('token', token) }
export function removeToken() { localStorage.removeItem('token') }
export function getUsername() { return localStorage.getItem('username') }
export function setUsername(name) { localStorage.setItem('username', name) }
export function isLoggedIn() { return !!localStorage.getItem('token') }

export function analyzeResume(file) {
  const formData = new FormData()
  formData.append('file', file)
  return apiClient.post('/resume/analyze', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 180000
  }).then(res => res.data)
}

export function analyzeResumeAsync(file) {
  const formData = new FormData()
  formData.append('file', file)
  return apiClient.post('/resume/analyze/async', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 30000
  }).then(res => res.data)
}

export function getResumeTaskStatus(taskId) {
  return apiClient.get(`/resume/analyze/status/${taskId}`).then(res => res.data)
}

export function getResumeHistory() {
  return apiClient.get('/resume/history').then(res => res.data)
}

export function deleteResumeHistory(id) {
  return apiClient.delete(`/resume/history/${id}`).then(res => res.data)
}

export function saveResumeHistory(data) {
  return apiClient.post('/resume/history/save', data).then(res => res.data)
}

export function startInterview(position, resumeAnalysisResult, mode, sessionToken) {
  return apiClient.post('/interview/start', {
    position,
    mode: mode || 'simulation',
    resumeAnalysisResult: resumeAnalysisResult || null,
    sessionToken: sessionToken || null
  }).then(res => res.data)
}

export function finishInterview(sessionToken) {
  return apiClient.post('/interview/finish', { sessionToken }).then(res => res.data)
}

export async function answerQuestion(sessionToken, userAnswer, onEvent, onDone, onError) {
  const token = localStorage.getItem('token')
  const controller = new AbortController()
  try {
    const response = await fetch('/api/interview/answer', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify({ sessionToken, userAnswer }),
      signal: controller.signal
    })

    if (!response.ok) {
      if (response.status === 401 || response.status === 403) {
        localStorage.removeItem('token')
        localStorage.removeItem('username')
        window.location.href = '/login'
        return controller
      }
      const errorText = await response.text()
      throw new Error(errorText || '请求失败')
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        for (const data of parseSseEvents(buffer)) {
          if (data) {
            try {
              const json = JSON.parse(data)
              if (onEvent) onEvent(json)
            } catch {
              if (onEvent) onEvent({ type: 'error', data })
            }
          }
        }
        onDone()
        break
      }
      const text = decoder.decode(value, { stream: true })
      buffer += text

      const lastDoubleNewline = buffer.lastIndexOf('\n\n')
      if (lastDoubleNewline !== -1) {
        const complete = buffer.substring(0, lastDoubleNewline + 2)
        buffer = buffer.substring(lastDoubleNewline + 2)
        for (const data of parseSseEvents(complete)) {
          if (data) {
            try {
              const json = JSON.parse(data)
              if (onEvent) onEvent(json)
            } catch {
              if (onEvent) onEvent({ type: 'error', data })
            }
          }
        }
      }
    }
  } catch (err) {
    if (err.name !== 'AbortError') {
      onError(err)
    }
  }
  return controller
}

function parseSseEvents(text) {
  const events = []
  const rawEvents = text.split('\n\n')
  for (const raw of rawEvents) {
    if (!raw.trim()) continue
    let data = ''
    const lines = raw.split('\n')
    for (const line of lines) {
      const trimmed = line.trim()
      if (!trimmed) continue
      if (trimmed.startsWith(':')) continue
      if (trimmed.startsWith('retry:')) continue
      if (trimmed.startsWith('event:')) {
        if (trimmed.substring(6).trim() === 'error') {
          events.push('__ERROR__')
        }
        continue
      }
      if (trimmed.startsWith('data:')) {
        let content = trimmed.substring(5)
        while (content.trim().startsWith('data:')) {
          content = content.trim().substring(5)
        }
        if (content.trim()) {
          data += content
        }
      }
    }
    if (data.trim()) {
      events.push(data)
    }
  }
  return events
}

export function register(data) {
  return apiClient.post('/auth/register', data).then(res => res.data)
}

export function login(data) {
  return apiClient.post('/auth/login', data).then(res => res.data)
}

export function getSupportedModels() {
  return apiClient.get('/config/models').then(res => res.data)
}

export function getServiceStatus() {
  return apiClient.get('/config/status').then(res => res.data)
}

export function getApiConfigs() {
  return apiClient.get('/config/list').then(res => res.data)
}

export function saveApiConfig(data) {
  return apiClient.post('/config/save', data).then(res => res.data)
}

export function setActiveConfig(id) {
  return apiClient.post(`/config/set-active/${id}`).then(res => res.data)
}

export function deleteApiConfig(id) {
  return apiClient.delete(`/config/${id}`).then(res => res.data)
}

export function testApiConnection(data) {
  return apiClient.post('/config/test', data).then(res => res.data)
}

export function getActiveConfig() {
  return apiClient.get('/config/active').then(res => res.data)
}

export function getSkills() {
  return apiClient.get('/interview/skills').then(res => res.data)
}

export function getInterviewHistory() {
  return apiClient.get('/interview/history').then(res => res.data)
}

export function deleteInterviewHistory(sessionToken) {
  return apiClient.delete(`/interview/history/${sessionToken}`).then(res => res.data)
}

export function getProviders() {
  return apiClient.get('/config/providers').then(res => res.data)
}

export { apiClient }
