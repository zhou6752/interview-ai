# Vue 3 前端学习理解文档

> 本文档帮助你理解 InterView AI 前端项目的核心概念。按照你的学习策略组织：
> 1. 理解组件结构（template / script setup / style）
> 2. 理解数据如何从后端流向前端
> 3. 理解组件之间的跳转
> 4. 不追求手写 CSS，用 Element Plus 组件库

---

## 一、项目结构总览

```
frontend/
├── index.html              # Vite 的 HTML 入口，只有一个 <div id="app">
├── package.json            # 项目依赖清单（vue、element-plus、axios 等）
├── vite.config.js          # Vite 配置（开发服务器端口 + 代理）
├── src/
│   ├── main.js             # Vue 应用入口：创建 app → 注册 Router → 注册 Element Plus → 挂载
│   ├── App.vue             # 根组件：顶部导航栏 + <router-view />（页面切换区域）
│   ├── api/
│   │   └── index.js        # 所有后端 API 调用函数的封装（Axios + SSE）
│   ├── router/
│   │   └── index.js        # 路由配置：URL 路径 → 页面组件的映射
│   ├── views/              # 页面级组件（每个路由对应一个）
│   │   ├── HomeView.vue        # 首页
│   │   ├── ResumeAnalyzer.vue  # 简历诊断
│   │   ├── InterviewView.vue   # 面试对话
│   │   └── InterviewReport.vue # 面试报告
│   ├── components/
│   │   └── ChatPanel.vue   # 可复用的聊天组件（被 InterviewView 使用）
│   └── styles/
│       └── main.css        # 全局样式 + 自定义 class
```

**关键概念区分：**
- `views/` — 页面组件，每个路由对应一个，代表一个完整的页面
- `components/` — 可复用组件，可以在多个页面中被引用，如 ChatPanel 被 InterviewView 使用

---

## 二、数据流全景图（最重要的理解）

以"面试对话"流程为例，数据是如何流转的：

```
【用户操作】
    用户点击"发送"
        ↓
【InterviewView.vue — 脚本层（script setup）】
    handleSend(text) 函数被调用：
    1. 把用户消息 push 到 messages 数组 → 界面立即显示用户气泡
    2. 调用 answerQuestion(sessionToken, text, onChunk, onDone, onError)
        ↓
【api/index.js — API 层】
    answerQuestion() 函数：
    1. fetch POST /api/interview/answer → 后端 SSE 流式返回
    2. response.body.getReader() → 逐块读取 AI 回复
    3. 每个 chunk 调用 onChunk(chunk)
        ↓
【InterviewView.vue — 回调更新数据】
    onChunk(chunk) 回调：
    aiMsg.content += chunk  ← 追加到消息对象
        ↓
【Vue 响应式系统 — 自动刷新界面】
    messages 是 ref() 定义的响应式数据
    当 aiMsg.content 变了 → Vue 自动重新渲染 <template> 中用到它的位置
        ↓
【ChatPanel.vue — 模板层（template）】
    <div v-html="renderMarkdown(msg.content)" />
    ↑ v-html 绑定到 msg.content，内容变了自动更新
```

**一句话总结：**
> 用户操作 → 调 API 函数 → 拿到数据 → 更新 ref 变量 → Vue 自动更新页面

---

## 三、Vue 3 组合式 API (`<script setup>`)

### 3.1 什么是 `<script setup>`？

Vue 3 有两种写法：
- **Options API**（Vue 2 的方式）：`data()`、`methods`、`mounted()` 等分散在各选项里
- **组合式 API (Composition API)** + `<script setup>`：所有逻辑写在一个 setup 函数里，更灵活

```vue
<!-- Options API (Vue 2 风格，本项目不用) -->
<script>
export default {
  data() { return { count: 0 } },
  methods: { increment() { this.count++ } },
  mounted() { console.log('组件加载了') }
}
</script>

<!-- 组合式 API (本项目实际使用) -->
<script setup>
import { ref, onMounted } from 'vue'

const count = ref(0)           // 响应式数据
function increment() {         // 普通函数
  count.value++
}
onMounted(() => {              // 生命周期钩子
  console.log('组件加载了')
})
</script>
```

### 3.2 `ref` vs `reactive`

| 特性 | `ref` | `reactive` |
|------|-------|------------|
| 包装类型 | 任何类型（数字、字符串、对象都行） | 只能包装对象/数组 |
| 访问方式 | `.value` 访问（模板里自动解包） | 直接访问属性 |
| 本项目使用 | **大量使用** | 较少（ref 更灵活） |

```js
const count = ref(0)           // 数字用 ref
count.value++                   // 修改要靠 .value

const messages = ref([])        // 数组也用 ref
messages.value.push({ role: 'ai', content: '你好' })

const result = ref(null)        // 初始为空的对象也用 ref
result.value = { skillScore: 85 }  // 赋新值要 .value
```

### 3.3 常用生命周期钩子

```js
import { onMounted, onUnmounted, watch } from 'vue'

// 组件挂载到页面后执行（类似 Java 的 @PostConstruct）
onMounted(() => {
  fetchData()  // 组件加载完 → 调后端 API 获取数据
})

// 组件卸载前执行（类似 Java 的 @PreDestroy）
onUnmounted(() => {
  clearInterval(timer)
})

// 监听数据变化
watch(() => messages.value.length, () => {
  scrollToBottom()  // 消息数量变了 → 自动滚动到底部
})
```

### 3.4 `computed` — 计算属性

当某个值依赖其他数据计算得出时使用，有缓存机制：

```js
const scoreColor = computed(() => {
  const s = result.value?.skillScore
  if (s >= 80) return '#67c23a'   // 绿色
  if (s >= 60) return '#e6a23c'   // 黄色
  return '#f56c6c'                 // 红色
})
// scoreColor 会自动根据 skillScore 变化而变化
```

---

## 四、组件通信（数据如何在不同组件间传递）

### 4.1 Props — 父组件传数据给子组件

**父组件（InterviewView.vue）传值：**
```vue
<ChatPanel
  :messages="messages"          <!-- 冒号表示动态绑定变量 -->
  :loading="waiting"
  :answer-count="answerCount"
/>
```

**子组件（ChatPanel.vue）接收：**
```vue
<script setup>
const props = defineProps({
  messages: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  answerCount: { type: Number, default: 0 }
})
// 在模板中直接用 messages、loading、answerCount
</script>
```

### 4.2 Emit — 子组件通知父组件

**子组件触发事件：**
```vue
<script setup>
const emit = defineEmits(['send', 'finish'])

function handleSend() {
  emit('send', inputText.value)  // 告诉父组件：用户发送了这条消息
}
</script>
```

**父组件监听：**
```vue
<ChatPanel
  @send="handleSend"     <!-- 子组件 emit('send') → 调父组件 handleSend -->
  @finish="handleFinish"
/>
```

### 4.3 Router Query — 跨页面传参

页面间跳转时，通过 URL 参数传递数据：

```js
// 跳转方（ResumeAnalyzer.vue）
router.push({
  path: '/interview',
  query: {
    position: 'JavaBackend',
    resumeResult: JSON.stringify(result.value)  // 对象要序列化
  }
})

// 接收方（InterviewView.vue）
import { useRoute } from 'vue-router'
const route = useRoute()

const position = route.query.position        // 'JavaBackend'
const resumeData = JSON.parse(route.query.resumeResult)  // 反序列化回来
```

**query vs params 区别：**

| | query | params |
|------|-------|--------|
| URL 样子 | `/interview?token=abc` | `/interview/abc` |
| 刷新会丢失吗 | **不会** | 取决于路由配置 |
| 传对象 | 需要 JSON.stringify | 一般只传 ID |
| 本项目使用 | ✅ **全部用 query** | 不用 |

---

## 五、Vue Router — 路由原理

### 5.1 路由配置

```js
// src/router/index.js
const routes = [
  { path: '/',         name: 'home',      component: HomeView },
  { path: '/resume',   name: 'resume',    component: ResumeAnalyzer },
  { path: '/interview', name: 'interview', component: InterviewView },
  { path: '/report',   name: 'report',    component: InterviewReport }
]
```

**原理：** 当浏览器地址栏变成 `/resume` 时，Vue Router 找到对应的 `ResumeAnalyzer` 组件，渲染到 `<router-view />` 的位置。

### 5.2 两种跳转方式

```vue
<!-- 声明式（模板中） -->
<router-link to="/resume">去简历页</router-link>

<!-- 编程式（JS 中，本项目主要用这种方式） -->
<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()

router.push('/resume')                          // 字符串路径
router.push({ path: '/report', query: { sessionToken: 'xxx' } })
router.push({ name: 'interview' })              // 通过路由名称跳转
</script>
```

### 5.3 `router-link` vs 手动 `el-menu` 跳转

本项目使用 Element Plus 的 `el-menu` + `router` 属性：

```vue
<el-menu router>           <!-- router 属性让 el-menu-item 自动跳转 -->
  <el-menu-item index="/resume">简历诊断</el-menu-item>
  <!-- 点击后自动 router.push('/resume') -->
</el-menu>
```

---

## 六、SSE 流式处理原理

### 6.1 为什么不用普通 Axios？

普通 Axios 请求是：发请求 → 等后端全部返回 → 一次性处理。
SSE（Server-Sent Events）是：发请求 → 后端逐字推数据 → 前端收到一个字就显示一个字。

效果：用户看到 AI 一个字一个字"打"出来，像真人聊天。

### 6.2 fetch + ReadableStream 实现

```js
async function answerQuestion(sessionToken, userAnswer, onChunk, onDone, onError) {
  const response = await fetch('/api/interview/answer', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sessionToken, userAnswer })
  })

  const reader = response.body.getReader()   // 拿到流的"读取器"
  const decoder = new TextDecoder()           // 把字节解码成文字

  while (true) {
    const { done, value } = await reader.read()  // 每次读一块
    if (done) break                              // 读完了
    const text = decoder.decode(value)           // 解码
    onChunk(text)                                 // 每块都回调通知
  }
  onDone()  // 全部读完
}
```

**工作流程：**
```
后端 SSE 流： "你" → "的回答" → "很好" → "【评价】很好【下一题】请解释..."
              ↓          ↓           ↓
reader.read() 第1次     第2次       第3次
              ↓          ↓           ↓
onChunk回调    "你"     "的回答"    "很好"
              ↓
aiMsg.content 拼接： "你" → "你的回答" → "你的回答很好"
              ↓
Vue 自动更新界面，用户看到字一个个出现
```

---

## 七、Element Plus 组件速查

本项目用到的 Element Plus 组件及用法：

| 组件 | 用途 | 关键属性 |
|------|------|----------|
| `el-menu` | 顶部导航栏 | `router` — 自动跳转，`mode="horizontal"` |
| `el-card` | 卡片容器 | `shadow="hover"` — 悬停阴影 |
| `el-button` | 按钮 | `type` — 颜色，`loading` — 加载态，`round` — 圆角 |
| `el-upload` | 文件上传 | `drag` — 拖拽上传，`accept=".pdf"`，`before-upload` — 校验钩子 |
| `el-progress` | 进度条/环形图 | `type="circle"` — 环形，`percentage` — 百分比 |
| `el-tag` | 标签 | `type` — 颜色，`effect="plain"` — 朴素风格 |
| `el-collapse` | 折叠面板 | `accordion` — 手风琴模式（只能开一个） |
| `el-alert` | 提示条 | `title` — 内容，`type` — 颜色 |
| `el-steps` | 步骤条 | `direction="vertical"` — 竖向排列 |
| `el-timeline` | 时间线 | 搭配 `el-timeline-item` 使用 |
| `el-select` | 下拉选择 | `v-model` — 绑定选中值 |
| `el-input` | 输入框 | `type="textarea"` — 多行文本，`rows` — 行数 |
| `el-result` | 结果页 | 用于错误提示等场景 |
| `el-loading` | 加载遮罩 | `v-loading="true"` 指令方式使用 |
| `el-message` | 消息通知 | `ElMessage.success('成功')` JS 调用 |
| `el-row / el-col` | 栅格布局 | `:gutter="16"` — 间距，`:xs="24" :sm="8"` — 响应式列宽 |

**引入方式：**
```js
// main.js 中全局注册
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
app.use(ElementPlus)

// 图标需要单独导入
import { UploadFilled, Cpu, UserFilled } from '@element-plus/icons-vue'
```

---

## 八、Vite 代理原理

### 为什么需要代理？

- 前端开发服务器跑在 `localhost:5173`
- 后端 Spring Boot 跑在 `localhost:8080`
- 浏览器有"同源策略"：5173 的页面不能直接请求 8080 的 API（跨域阻止）

### Vite 代理解决方案

```js
// vite.config.js
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',  // 把 /api 开头的请求转发到 8080
      changeOrigin: true
    }
  }
}
```

**工作流程：**

```
前端代码：fetch('/api/interview/start')
    ↓
浏览器发请求到：http://localhost:5173/api/interview/start
    ↓
Vite 开发服务器拦截到 /api/* 请求
    ↓
自动转发到：http://localhost:8080/api/interview/start（后端）
    ↓
后端处理完返回数据
    ↓
Vite 把响应传回前端
```

**好处：** 浏览器看来，请求是发给 5173 的（同源），不触发跨域问题。但实际上 Vite 在后台偷偷转发给了 8080。

---

## 九、核心页面数据流详解

### 简历诊断页 (ResumeAnalyzer.vue)

```
用户拖入 PDF → el-upload 的 before-upload 校验
     ↓
用户点击"开始分析" → submitAnalyze()
     ↓
调用 analyzeResume(file) → POST /api/resume/analyze (FormData)
     ↓
后端返回 ResumeAnalysisResult JSON
     ↓
result.value = data → Vue 响应式更新
     ↓
模板显示：环形评分图 + 标签列表 + 折叠面板面试题
```

### 面试对话页 (InterviewView.vue)

```
【阶段1：选择岗位】
用户选岗位 + 点"开始面试"
     ↓
handleStart() → startInterview(position, resumeResult)
     ↓
POST /api/interview/start → 拿到 sessionToken + firstQuestion
     ↓
messages.push({ role: 'ai', content: firstQuestion })
     ↓
界面显示第一道面试题

【阶段2：循环对话】
用户输入回答 + 点"发送"
     ↓
handleSend(text) → 添加用户消息到 messages
     ↓
answerQuestion(sessionToken, text, onChunk, onDone, onError)
     ↓
SSE 流式读取 → 逐字追加到 aiMsg.content
     ↓
用户看到 AI 评价 + 下一题逐字出现

【阶段3：结束面试】
用户点"结束面试"
     ↓
router.push('/report?sessionToken=xxx')
```

### 面试报告页 (InterviewReport.vue)

```
页面加载 → onMounted()
     ↓
fetchReport() → finishInterview(sessionToken)
     ↓
POST /api/interview/finish → InterviewReport JSON
     ↓
report.value = data → 渲染评分卡片 + 折叠详情 + 时间线
```

---

## 十、学习建议

按照你列的学习策略，建议这样读代码：

1. **先看 App.vue** — 理解 `<router-view />` 是怎么实现页面切换的
2. **再看 router/index.js** — 理解 URL 和组件的对应关系
3. **看 HomeView.vue** — 最简单的页面，两个卡片 + 跳转
4. **看 api/index.js** — 理解 Axios 如何封装后端接口
5. **看 ChatPanel.vue** — 核心聊天组件，理解 Props/Emits 父子通信
6. **看 InterviewView.vue** — 最复杂的页面，把前面学的串起来
7. **看 ResumeAnalyzer.vue** — 含文件上传的特殊处理
8. **看 InterviewReport.vue** — 了解 Element Plus 各种组件的组合使用

**实践建议：** 打开 `npm run dev`，边看页面边对照源码，理解"界面上这个区域是哪个组件的哪段代码渲染的"。