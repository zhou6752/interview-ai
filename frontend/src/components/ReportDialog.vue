<template>
  <el-dialog
    :model-value="visible"
    title="面试报告"
    width="800px"
    destroy-on-close
    @update:model-value="$emit('update:visible', $event)"
  >
    <template v-if="report">
      <el-row :gutter="16" class="score-row">
        <el-col :xs="12" :sm="6">
          <div class="score-card score-tech">
            <div class="score-label">技术深度<br><small>权重 40%</small></div>
            <div class="score-value">{{ report.technicalScore }}</div>
            <el-progress
              :percentage="report.technicalScore"
              :color="'#fff'"
              :stroke-width="6"
              :show-text="false"
            />
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="score-card score-logic">
            <div class="score-label">逻辑表达<br><small>权重 25%</small></div>
            <div class="score-value">{{ report.logicScore }}</div>
            <el-progress
              :percentage="report.logicScore"
              :color="'#fff'"
              :stroke-width="6"
              :show-text="false"
            />
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="score-card score-knowledge">
            <div class="score-label">知识广度<br><small>权重 20%</small></div>
            <div class="score-value">{{ report.knowledgeBreadth }}</div>
            <el-progress
              :percentage="report.knowledgeBreadth"
              :color="'#fff'"
              :stroke-width="6"
              :show-text="false"
            />
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="score-card score-practice">
            <div class="score-label">实践经验<br><small>权重 15%</small></div>
            <div class="score-value">{{ report.practiceScore || 0 }}</div>
            <el-progress
              :percentage="report.practiceScore || 0"
              :color="'#fff'"
              :stroke-width="6"
              :show-text="false"
            />
          </div>
        </el-col>
      </el-row>

      <el-card v-if="report.overallScore || report.technicalScore" class="report-card overall-score-card">
        <template #header>
          <span class="text-h2">综合得分</span>
        </template>
        <div class="overall-score-value">
          {{ computedOverall }}
          <small>/100</small>
        </div>
        <el-progress
          type="circle"
          :percentage="computedOverall"
          :stroke-width="8"
          :color="overallColor"
          :width="120"
        />
      </el-card>

      <el-card v-if="report.overallComment" class="report-card">
        <template #header>
          <span class="text-h2">综合评语</span>
        </template>
        <el-alert :title="report.overallComment" type="info" :closable="false" show-icon />
      </el-card>

      <el-card v-if="report.questionDetails && report.questionDetails.length" class="report-card">
        <template #header>
          <span class="text-h2">逐题详情</span>
        </template>
        <el-collapse accordion>
          <el-collapse-item
            v-for="(detail, idx) in report.questionDetails"
            :key="idx"
            :title="'Q' + (idx + 1) + ': ' + detail.question"
          >
            <div class="detail-section answer-box">
              <h4>你的回答</h4>
              <p class="detail-text">{{ detail.userAnswer }}</p>
            </div>
            <div class="detail-section reference-box">
              <h4>参考答案</h4>
              <p class="detail-text">{{ detail.referenceAnswer }}</p>
            </div>
            <div class="detail-section">
              <h4>AI 点评</h4>
              <el-tag effect="plain" type="warning">{{ detail.comment }}</el-tag>
            </div>
          </el-collapse-item>
        </el-collapse>
      </el-card>

      <el-card v-if="report.learningPath && report.learningPath.length" class="report-card">
        <template #header>
          <span class="text-h2">学习路径建议</span>
        </template>
        <el-timeline>
          <el-timeline-item
            v-for="(item, idx) in report.learningPath"
            :key="idx"
            :timestamp="'建议 ' + (idx + 1)"
            placement="top"
          >
            <el-card shadow="never" class="timeline-card">{{ item }}</el-card>
          </el-timeline-item>
        </el-timeline>
      </el-card>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  report: { type: Object, default: null },
  visible: { type: Boolean, default: false }
})

defineEmits(['update:visible'])

const computedOverall = computed(() => {
  if (!props.report) return 0
  const t = props.report.technicalScore || 0
  const l = props.report.logicScore || 0
  const k = props.report.knowledgeBreadth || 0
  const p = props.report.practiceScore || 0
  return Math.round(t * 0.4 + l * 0.25 + k * 0.2 + p * 0.15)
})

const overallColor = computed(() => {
  const s = computedOverall.value
  if (s >= 85) return '#67c23a'
  if (s >= 70) return '#409eff'
  if (s >= 50) return '#e6a23c'
  return '#f56c6c'
})
</script>

<style scoped>
.score-row {
  margin-bottom: 16px;
}

.score-row .el-col {
  margin-bottom: 8px;
}

.report-card {
  margin-bottom: 16px;
}

.detail-section {
  margin-bottom: 12px;
}

.detail-section h4 {
  font-size: 14px;
  color: #6B7280;
  margin-bottom: 6px;
}

.detail-text {
  color: #4B5563;
  line-height: 1.6;
  white-space: pre-wrap;
}

.answer-box {
  background-color: #F9FAFB;
  border-radius: 8px;
  padding: 16px;
}

.reference-box {
  background-color: #FFFFFF;
  border-left: 4px solid #6366F1;
  padding: 16px;
}

.timeline-card {
  font-size: 14px;
  color: #4B5563;
}

.overall-score-card {
  text-align: center;
  margin-bottom: 16px;
}

.overall-score-value {
  font-size: 56px;
  font-weight: 700;
  color: #1F2937;
  margin-bottom: 8px;
}

.overall-score-value small {
  font-size: 24px;
  color: #9CA3AF;
}
</style>
