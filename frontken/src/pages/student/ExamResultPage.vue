<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { http } from '../../api/http'
import { useAuthStore } from '../../stores/auth'
import { ElMessage } from 'element-plus'
import { Icon } from '@iconify/vue'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const recordId = route.params.recordId
const result = ref<any>(null)
const loading = ref(true)
const reviews = ref<Record<number, any>>({})
const examReview = ref<any>(null)
const examReviewLoading = ref(false)

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
      if (result.value?.exam?.id) {
        fetchExamReview(result.value.exam.id)
      }
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
    return opt.map((o: any) => {
      // Check if o is an object (Option object)
      if (typeof o === 'object' && o !== null) {
        return {
          key: o.key || '',
          value: o.value || '',
          imageUrl: o.imageUrl || ''
        }
      }
      // If o is a string, parse it
      if (typeof o === 'string') {
        // If backend sends "A. Value", parse it
        if (o.includes('.')) {
          const firstDot = o.indexOf('.')
          const key = o.substring(0, firstDot).trim()
          const value = o.substring(firstDot + 1).trim()
          return { key, value, imageUrl: '' }
        }
        // Fallback if structure is different
        return { key: '', value: o, imageUrl: '' }
      }
      // Fallback for other types
      return { key: '', value: '', imageUrl: '' }
    })
  }
  // Fallback for string
  try {
    const parsed = JSON.parse(opt)
    if (Array.isArray(parsed)) {
      return parsed.map((o: any) => {
        if (typeof o === 'object' && o !== null) {
          return {
            key: o.key || '',
            value: o.value || '',
            imageUrl: o.imageUrl || ''
          }
        }
        return { key: '', value: o || '', imageUrl: '' }
      })
    }
    return []
  } catch {
    return []
  }
}

async function fetchExamReview(examId: number) {
  examReviewLoading.value = true
  try {
    const res = await http.get(`/api/student/reviews/exam/${examId}`)
    examReview.value = res.data.data
  } catch (e) {
    examReview.value = null
  } finally {
    examReviewLoading.value = false
  }
}

function formatDisplayTime(time?: string) {
  return time ? time.replace('T', ' ') : '-'
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

        <div class="exam-review-section" v-if="examReview" v-loading="examReviewLoading">
          <div class="exam-review-card">
            <div class="exam-review-head">
              <div>
                <div class="exam-review-eyebrow">考试讲评</div>
                <h3>{{ examReview.title }}</h3>
              </div>
              <el-button plain round @click="router.push(`/student/exam/${result.exam.id}`)">
                <Icon icon="iconoir:open-in-window" class="btn-icon" />
                打开讲评页
              </el-button>
            </div>

            <div v-if="examReview.summary || examReview.content" class="exam-review-summary">
              {{ examReview.summary || examReview.content }}
            </div>

            <div v-if="examReview.questionReviews?.length" class="exam-review-list">
              <div
                v-for="item in examReview.questionReviews"
                :key="item.questionId"
                class="exam-review-item"
              >
                <div class="exam-review-item-head">第 {{ item.questionNo || '?' }} 题</div>
                <div class="exam-review-item-question">{{ item.questionContent || '题干暂缺' }}</div>
                <div class="exam-review-item-content">{{ item.content }}</div>
              </div>
            </div>

            <div class="exam-review-time">发布时间 {{ formatDisplayTime(examReview.createdAt) }}</div>
          </div>
        </div>

        <div class="ranking-section" v-if="result.ranking">
          <div class="ranking-card">
            <div class="ranking-head">
              <div>
                <div class="ranking-title">班级排名</div>
                <div class="ranking-subtitle">{{ result.ranking.className }}</div>
              </div>
              <div class="ranking-badge">第 {{ result.ranking.myRank || '-' }} 名</div>
            </div>
            <div class="ranking-summary">
              <div class="ranking-summary-item">
                <span class="summary-label">参与排名人数</span>
                <span class="summary-value">{{ result.ranking.participantCount }}</span>
              </div>
            </div>
            <div class="ranking-list" v-if="result.ranking.leaderboard?.length">
              <div
                v-for="item in result.ranking.leaderboard"
                :key="item.studentId"
                class="ranking-item"
                :class="{ current: item.currentStudent }"
              >
                <div class="ranking-left">
                  <span class="ranking-rank">#{{ item.rank }}</span>
                  <span class="ranking-name">{{ item.studentName }}</span>
                </div>
                <div class="ranking-right">
                  <span class="ranking-score">{{ item.score ?? 0 }}分</span>
                  <span class="ranking-time">{{ formatDisplayTime(item.submitTime) }}</span>
                </div>
              </div>
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
              <img v-if="q.contentImageUrl" :src="q.contentImageUrl" class="q-image" alt="Question Image" />
              <div class="q-options">
                <div v-for="opt in formatOption(q.options)" :key="opt.key" class="option-item"
                  :class="{
                    'is-student-answer': q.studentAnswer?.includes(opt.key),
                    'is-correct-answer': q.answer?.includes(opt.key)
                  }"
                >
                  <span class="opt-key">{{ opt.key }}</span>
                  <div class="opt-content">
                    <span class="opt-val">{{ opt.value }}</span>
                    <img v-if="opt.imageUrl" :src="opt.imageUrl" class="opt-image" alt="Option Image" />
                  </div>
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

.exam-review-section {
  margin-bottom: 32px;
}

.exam-review-card {
  padding: 24px;
  border: 1px solid #eadfce;
  border-radius: 20px;
  background:
    radial-gradient(circle at top left, rgba(255, 225, 188, 0.35), transparent 34%),
    linear-gradient(180deg, #fffaf4 0%, #ffffff 100%);
  box-shadow: 0 12px 36px rgba(148, 163, 184, 0.08);
}

.exam-review-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.exam-review-eyebrow {
  margin-bottom: 8px;
  color: #b45309;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.exam-review-head h3 {
  margin: 0;
  color: #1f2937;
  font-size: 22px;
  font-weight: 700;
}

.exam-review-summary {
  color: #475569;
  font-size: 15px;
  line-height: 1.8;
  white-space: pre-wrap;
}

.exam-review-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 18px;
}

.exam-review-item {
  padding: 16px;
  border: 1px solid #ece6db;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
}

.exam-review-item-head {
  color: #9a5a16;
  font-size: 13px;
  font-weight: 700;
}

.exam-review-item-question {
  margin-top: 8px;
  color: #1f2937;
  font-size: 14px;
  line-height: 1.7;
}

.exam-review-item-content {
  margin-top: 10px;
  color: #475569;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.exam-review-time {
  margin-top: 16px;
  color: #94a3b8;
  font-size: 12px;
  text-align: right;
}

.ranking-section {
  margin-bottom: 32px;
}

.ranking-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}

.ranking-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}

.ranking-title {
  font-size: 18px;
  font-weight: 800;
  color: #1a1e23;
}

.ranking-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: #64748b;
}

.ranking-badge {
  padding: 10px 16px;
  border-radius: 999px;
  background: rgba(16, 212, 166, 0.12);
  color: #0f766e;
  font-size: 14px;
  font-weight: 700;
}

.ranking-summary {
  display: flex;
  gap: 16px;
  margin-bottom: 18px;
}

.ranking-summary-item {
  flex: 1;
  padding: 14px 16px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
}

.summary-label {
  display: block;
  font-size: 12px;
  color: #64748b;
  margin-bottom: 6px;
}

.summary-value {
  font-size: 22px;
  font-weight: 800;
  color: #1a1e23;
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ranking-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 14px;
  background: #fbfbf9;
  border: 1px solid #ececec;
}

.ranking-item.current {
  border-color: rgba(16, 212, 166, 0.35);
  background: #f0fdf9;
}

.ranking-left,
.ranking-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ranking-rank {
  font-weight: 800;
  color: #0f766e;
}

.ranking-name {
  color: #1f2937;
  font-weight: 600;
}

.ranking-score {
  color: #111827;
  font-weight: 700;
}

.ranking-time {
  font-size: 12px;
  color: #64748b;
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
  margin-bottom: 12px;
}

.q-image {
  max-width: 100%;
  max-height: 300px;
  object-fit: contain;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
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

.opt-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.opt-val {
  color: #334155;
}

.opt-image {
  max-width: 100%;
  max-height: 200px;
  object-fit: contain;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  margin-top: 8px;
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

@media (max-width: 760px) {
  .result-header,
  .exam-review-head,
  .q-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .score-overview {
    grid-template-columns: 1fr;
  }
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
