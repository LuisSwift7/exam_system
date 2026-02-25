<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { http } from '../../api/http'
import { useAuthStore } from '../../stores/auth'
import { ElMessage } from 'element-plus'
import { User, Check } from '@element-plus/icons-vue'
import { Icon } from '@iconify/vue'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const recordId = route.params.recordId
const result = ref<any>(null)
const loading = ref(true)
const reviews = ref<Record<number, any>>({})

// Feedback
const feedbackDialogVisible = ref(false)
const feedbackContent = ref('')
const currentQuestionId = ref<number | null>(null)

function openFeedbackDialog(qId: number) {
  currentQuestionId.value = qId
  feedbackContent.value = ''
  feedbackDialogVisible.value = true
}

async function submitFeedback() {
  if (!feedbackContent.value.trim()) {
    ElMessage.warning('请输入反馈内容')
    return
  }
  
  try {
    await http.post('/api/student/feedback/create', {
      questionId: currentQuestionId.value,
      examId: result.value.exam.id,
      content: feedbackContent.value
    })
    ElMessage.success('反馈提交成功')
    feedbackDialogVisible.value = false
  } catch (e: any) {
    ElMessage.error(e?.message || '提交失败')
  }
}

const fetchResult = async () => {
  try {
    const res = await http.get(`/api/student/exam-taking/${recordId}/result`)
    if (res.data.code === 0) {
      result.value = res.data.data
      // Fetch reviews
      if (result.value.questions) {
        result.value.questions.forEach((q: any) => {
          fetchReview(q.id)
        })
      }
    } else {
      ElMessage.error(res.data.message)
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

async function fetchReview(qId: number) {
  try {
    const res = await http.get(`/api/student/feedback/review/${qId}`)
    if (res.data.data) {
      reviews.value[qId] = res.data.data
    }
  } catch (e) {
    // ignore
  }
}

const formatOption = (opt: any) => {
  if (Array.isArray(opt)) {
    return opt.map((o: string) => {
      // If backend sends "A. Value", parse it
      if (o.includes('.')) {
        const firstDot = o.indexOf('.')
        const key = o.substring(0, firstDot).trim()
        const value = o.substring(firstDot + 1).trim()
        return { key, value }
      }
      // Fallback if structure is different
      return { key: '', value: o }
    })
  }
  // Fallback for string
  try {
    return JSON.parse(opt)
  } catch {
    return []
  }
}

async function logout() {
  auth.logout()
  await router.replace('/login')
}

onMounted(() => {
  fetchResult()
})
</script>

<template>
  <div class="page" v-loading="loading">
    <div class="page__bg" />
    
    <div class="shell">
      <header class="head">
        <div class="head__left">
          <div class="head__brand">
            <Icon icon="iconoir:graduation-cap" class="head__logo" />
            <span class="head__title">考试结果</span>
          </div>
          <div class="head__user">
            <span class="head__name">{{ auth.me?.realName || auth.me?.username }}</span>
            <span class="head__role">{{ auth.me?.roleCode }}</span>
          </div>
        </div>
        <el-button class="head__logout" link @click="logout">
          <Icon icon="iconoir:log-out" />
          退出
        </el-button>
      </header>

      <div class="main-content fade-in" v-if="result">
        <div class="result-header">
          <div class="result-info">
            <h1>{{ result.exam.title }}</h1>
            <p class="exam-desc">{{ result.exam.description || '暂无描述' }}</p>
          </div>
          <el-button type="primary" round @click="router.push('/dashboard')">
            <Icon icon="iconoir:home" class="btn-icon" />
            返回首页
          </el-button>
        </div>

        <div class="score-overview">
          <div class="score-card">
            <div class="score-value">{{ result.record.score }}</div>
            <div class="score-label">最终得分</div>
          </div>
          <div class="info-card">
            <div class="info-item">
              <span class="label">提交时间</span>
              <span class="value">{{ result.record.submitTime ? result.record.submitTime.replace('T', ' ') : '-' }}</span>
            </div>
            <div class="info-item">
              <span class="label">试卷总分</span>
              <span class="value">{{ result.totalScore }}</span>
            </div>
          </div>
        </div>

        <div class="category-stats" v-if="result.categoryStats && result.categoryStats.length > 0">
          <h3 class="stats-title">题型得分统计</h3>
          <div class="stats-grid">
            <div v-for="stat in result.categoryStats" :key="stat.category" class="stat-card">
              <div class="stat-category">{{ stat.category }}</div>
              <div class="stat-score">{{ stat.score }}分</div>
              <div class="stat-info">
                <span>正确 {{ stat.correctCount }}/{{ stat.totalCount }}</span>
                <span class="stat-rate">正确率 {{ (stat.accuracyRate * 100).toFixed(0) }}%</span>
              </div>
              <el-progress 
                :percentage="Math.round(stat.accuracyRate * 100)" 
                :color="stat.accuracyRate >= 0.6 ? '#67c23a' : '#f56c6c'"
                :show-text="false"
              />
            </div>
          </div>
        </div>

        <div class="questions-list">
          <div v-for="(q, index) in result.questions" :key="q.id" class="q-card" :class="{ correct: q.isCorrect, wrong: !q.isCorrect }">
            <div class="q-header">
              <div class="q-status">
                <Icon :icon="q.isCorrect ? 'iconoir:check-circle' : 'iconoir:cancel-circle'" />
                {{ q.isCorrect ? '正确' : '错误' }}
              </div>
              <div style="display: flex; align-items: center; gap: 12px">
                <el-button link type="primary" size="small" @click="openFeedbackDialog(q.id)">
                  <Icon icon="iconoir:chat-bubble" style="margin-right: 4px" />
                  题目反馈
                </el-button>
                <span class="q-meta">第 {{ index + 1 }} 题 · {{ q.category || '未分类' }} · {{ q.type === 2 ? '多选题' : '单选题' }} · {{ q.score }}分</span>
              </div>
            </div>
            
            <div class="q-content">
              <div class="q-text">{{ q.content }}</div>
              <div class="q-options">
                <div v-for="opt in formatOption(q.options)" :key="opt.key" class="option-item"
                  :class="{
                    'is-student-answer': q.studentAnswer?.includes(opt.key),
                    'is-correct-answer': q.answer?.includes(opt.key)
                  }"
                >
                  <span class="opt-key">{{ opt.key }}</span>
                  <span class="opt-val">{{ opt.value }}</span>
                  <Icon icon="iconoir:user" v-if="q.studentAnswer?.includes(opt.key)" class="my-ans-icon" />
                  <Icon icon="iconoir:check" v-if="q.answer?.includes(opt.key)" class="correct-ans-icon" />
                </div>
              </div>
              
              <div class="q-analysis">
                <div class="analysis-label">
                  <Icon icon="iconoir:light-bulb" />
                  解析
                </div>
                <div class="analysis-content">{{ q.analysis || '暂无解析' }}</div>
                <div class="analysis-ans">正确答案：{{ q.answer }}</div>
                
                <div class="review-box" v-if="reviews[q.id]">
                  <div class="review-head">
                    <Icon icon="iconoir:teacher" />
                    教师讲评
                  </div>
                  <div class="review-title">{{ reviews[q.id].title }}</div>
                  <div class="review-content">{{ reviews[q.id].content }}</div>
                  <div class="review-time">发布于 {{ reviews[q.id].createTime?.replace('T', ' ') }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else-if="!loading" description="未找到结果或加载失败" />

      <!-- Feedback Dialog -->
      <el-dialog
        v-model="feedbackDialogVisible"
        title="题目反馈"
        width="500px"
        append-to-body
      >
        <el-input
          v-model="feedbackContent"
          type="textarea"
          :rows="4"
          placeholder="请输入您对该题目的疑问或反馈..."
        />
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="feedbackDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitFeedback">提交</el-button>
          </span>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  position: relative;
  background: #f6f8fa;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.page__bg {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 80% 10%, rgba(16, 212, 166, 0.08), transparent 40%),
    radial-gradient(circle at 10% 90%, rgba(255, 161, 22, 0.08), transparent 40%);
  pointer-events: none;
}

.page__bg::after {
  content: '';
  position: absolute;
  inset: 0;
  opacity: 0.4;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='140' height='140'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.9' numOctaves='2' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='140' height='140' filter='url(%23n)' opacity='.35'/%3E%3C/svg%3E");
  mix-blend-mode: overlay;
}

.shell {
  position: relative;
  z-index: 1;
  max-width: 800px;
  margin: 0 auto;
  padding: 0 24px 40px;
}

.head {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.head__left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.head__brand {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1a1e23;
}

.head__logo {
  font-size: 24px;
  color: #10d4a6;
}

.head__title {
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.5px;
}

.head__user {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  padding: 4px 10px;
  background: rgba(0,0,0,0.04);
  border-radius: 99px;
}

.main-content {
  animation: slideUp 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.result-info h1 {
  font-size: 24px;
  font-weight: 800;
  color: #1a1e23;
  margin: 0 0 8px 0;
  letter-spacing: -0.5px;
}

.exam-desc {
  color: #64748b;
  font-size: 14px;
  margin: 0;
}

.btn-icon {
  margin-right: 6px;
  font-size: 16px;
}

.score-overview {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 32px;
}

.category-stats {
  margin-bottom: 32px;
}

.stats-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1e23;
  margin: 0 0 16px 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}

.stat-category {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.stat-score {
  font-size: 24px;
  font-weight: 700;
  color: #10d4a6;
  margin-bottom: 8px;
}

.stat-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-rate {
  font-weight: 500;
}

.score-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.score-value {
  font-size: 48px;
  font-weight: 800;
  color: #10d4a6;
  line-height: 1;
  margin-bottom: 8px;
  letter-spacing: -2px;
}

.score-label {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.info-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-item .label {
  color: #64748b;
  font-size: 14px;
}

.info-item .value {
  font-weight: 600;
  color: #1a1e23;
}

.q-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  border: 1px solid transparent;
  transition: all 0.3s ease;
}

.q-card.correct {
  border-color: rgba(16, 212, 166, 0.2);
}

.q-card.wrong {
  border-color: rgba(245, 108, 108, 0.2);
}

.q-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.q-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 700;
  font-size: 14px;
}

.correct .q-status { color: #10d4a6; }
.wrong .q-status { color: #f56c6c; }

.q-meta {
  font-size: 13px;
  color: #94a3b8;
  font-weight: 500;
}

.q-text {
  font-size: 16px;
  font-weight: 600;
  color: #1a1e23;
  line-height: 1.6;
  margin-bottom: 20px;
}

.option-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  margin-bottom: 10px;
  background: #fff;
  transition: all 0.2s;
}

.opt-key {
  font-weight: 700;
  width: 24px;
  color: #64748b;
}

.opt-val {
  flex: 1;
  color: #334155;
}

.is-student-answer {
  background-color: #fef2f2;
  border-color: #fecaca;
}

.is-student-answer .opt-key,
.is-student-answer .opt-val {
  color: #b91c1c;
}

.is-correct-answer {
  background-color: #ecfdf5 !important;
  border-color: #a7f3d0 !important;
}

.is-correct-answer .opt-key,
.is-correct-answer .opt-val {
  color: #047857;
}

.my-ans-icon {
  color: #ef4444;
  font-size: 18px;
}

.correct-ans-icon {
  color: #10d4a6;
  font-size: 18px;
  margin-left: 8px;
}

.q-analysis {
  margin-top: 24px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px solid #f1f5f9;
}

.analysis-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
  color: #64748b;
  margin-bottom: 8px;
}

.analysis-content {
  font-size: 14px;
  color: #334155;
  line-height: 1.6;
  margin-bottom: 12px;
}

.analysis-ans {
  font-size: 13px;
  font-weight: 700;
  color: #10d4a6;
  padding-top: 12px;
  border-top: 1px solid #e2e8f0;
}

.review-box {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #d1d5db;
  color: #1a1e23;
}

.review-head {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
  color: #10d4a6;
  margin-bottom: 8px;
}

.review-title {
  font-weight: 600;
  margin: 4px 0 8px;
  color: #1a1e23;
  font-size: 14px;
}

.review-content {
  font-size: 14px;
  color: #334155;
  line-height: 1.6;
}

.review-time {
  font-size: 12px;
  color: #6b7280;
  margin-top: 8px;
  text-align: right;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.fade-in {
  animation: fadeIn 0.8s ease-out forwards;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>
