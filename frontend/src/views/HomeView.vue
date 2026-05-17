<template>
  <div class="page-container page-container--wide">
    <section class="hero">
      <p class="text-eyebrow">智能面试平台</p>
      <h1 class="text-display hero__title">让每一次面试都成为成长的阶梯</h1>
      <p class="text-body hero__desc">
        上传简历，AI 深度分析技术栈。模拟真实面试场景，实时评分，精准能力画像。
      </p>
      <div class="hero__actions">
        <router-link to="/resume">
          <el-button type="primary" size="large">开始使用</el-button>
        </router-link>
        <router-link to="/interview">
          <el-button size="large">了解更多</el-button>
        </router-link>
      </div>
    </section>

    <section class="features grid-3">
      <div class="feature-card" @click="$router.push('/resume')">
        <div class="feature-card__icon">
          <el-icon :size="20"><Document /></el-icon>
        </div>
        <h3 class="text-h3" style="margin-bottom: 8px;">简历诊断</h3>
        <p class="text-body">
          上传 PDF，AI 分析技术栈，生成精准评分与定向面试题
        </p>
        <span class="feature-card__link">立即体验 →</span>
      </div>

      <div class="feature-card" @click="$router.push('/interview')">
        <div class="feature-card__icon">
          <el-icon :size="20"><ChatDotRound /></el-icon>
        </div>
        <h3 class="text-h3" style="margin-bottom: 8px;">模拟面试</h3>
        <p class="text-body">
          练习/模拟/严苛三种模式，SSE 流式对话，实时能力画像
        </p>
        <span class="feature-card__link">立即体验 →</span>
      </div>

      <div class="feature-card" @click="$router.push('/knowledge')">
        <div class="feature-card__icon">
          <el-icon :size="20"><Collection /></el-icon>
        </div>
        <h3 class="text-h3" style="margin-bottom: 8px;">知识库</h3>
        <p class="text-body">
          上传文档自动向量化，智能检索与分类管理
        </p>
        <span class="feature-card__link">立即体验 →</span>
      </div>

      <div class="feature-card" @click="$router.push('/rag-chat')">
        <div class="feature-card__icon">
          <el-icon :size="20"><ChatLineSquare /></el-icon>
        </div>
        <h3 class="text-h3" style="margin-bottom: 8px;">知识库问答</h3>
        <p class="text-body">
          RAG 多轮对话，基于知识库上下文精准回答
        </p>
        <span class="feature-card__link">立即体验 →</span>
      </div>
    </section>

    <section class="tech-stack">
      <p class="text-eyebrow">技术栈</p>
      <div class="tech-stack__tags">
        <span class="tag-pill">Java 17</span>
        <span class="tag-pill">Spring Boot 3</span>
        <span class="tag-pill">Vue 3</span>
        <span class="tag-pill">PostgreSQL</span>
        <span class="tag-pill">Redis</span>
        <span class="tag-pill">ONNX Runtime</span>
        <span class="tag-pill">pgvector</span>
        <span class="tag-pill">SSE Streaming</span>
      </div>
    </section>
  </div>
</template>

<script setup>
import { Document, ChatDotRound, Collection, ChatLineSquare } from '@element-plus/icons-vue'
</script>

<style scoped>
.text-eyebrow {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: #6366F1;
  background-color: #EEF2FF;
  padding: 4px 12px;
  border-radius: 9999px;
}

.hero {
  text-align: center;
  max-width: 720px;
  margin: 56px auto 64px;
}

.hero__title {
  margin: 16px 0;
}

.text-body.hero__desc {
  font-size: 18px;
  color: #6B7280;
  max-width: 560px;
  margin: 0 auto 32px;
}

.hero__actions {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.features {
  max-width: 1080px;
  margin: 0 auto;
}

.feature-card h3 {
  margin-bottom: 8px;
}

.feature-card p {
  margin-bottom: 16px;
}

.feature-card__link {
  font-size: 14px;
  color: #6366F1;
  font-weight: 500;
}

.tech-stack {
  text-align: center;
  margin-top: 64px;
  padding-bottom: 64px;
}

.tech-stack__tags {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

@media (max-width: 768px) {
  .hero {
    margin: 32px auto;
  }
}
</style>
