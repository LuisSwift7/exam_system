<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { Icon } from '@iconify/vue'
import { ElMessage } from 'element-plus'
import { wrongBookApi, type WrongBookItem, type WrongBookStats } from '../../api/wrongBook'
import { http } from '../../api/http'
import { useAuthStore } from '../../stores/auth'
import { useRouter } from 'vue-router'

const router = useRouter()
const auth = useAuthStore()

// State
const loading = ref(false)
const list = ref<WrongBookItem[]>([])
const stats = ref<WrongBookStats>({
  totalCount: 0,
  practiceCount: 0,
  practiceAccuracy: 0,
  typeDistribution: {}
})
const query = ref({
  page: 1,
  size: 12,
  keyword: '',
  type: undefined as number | undefined
})
const total = ref(0)

// Practice State
const practiceVisible = ref(false)
const currentQ = ref<WrongBookItem | null>(null)
const userAnswers = ref<string[]>([])
const showResult = ref(false)
const isCorrect = ref(false)
const teacherReview = ref<any>(null)

// Feedback
const feedbackDialogVisible = ref(false)
const feedbackContent = ref('')

async function fetchReview(questionId: number) {
  try {
    const res = await http.get(`/api/student/feedback/review/${questionId}`)
    teacherReview.value = res.data.data
  } catch (e) {
    console.error('Fetch review failed', e)
  }
}

function openFeedbackDialog() {
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
      questionId: currentQ.value?.questionId,
      examId: currentQ.value?.examId,
      content: feedbackContent.value
    })
    ElMessage.success('反馈提交成功')
    feedbackDialogVisible.value = false
  } catch (e: any) {
    ElMessage.error(e?.message || '提交失败')
  }
}

// Fetch Data
async function fetchData() {
  loading.value = true
  try {
    const res = await wrongBookApi.list(query.value)
    const data = res.data.data
    list.value = data.records
    total.value = data.total
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function fetchStats() {
  try {
    const res = await wrongBookApi.getStats()
    stats.value = res.data.data
  } catch (e) {
    console.error(e)
  }
}

function handleSearch() {
  query.value.page = 1
  fetchData()
}

function formatType(type: number) {
  const map: Record<number, string> = { 1: '单选', 2: '多选', 3: '判断' }
  return map[type] || '未知'
}

// Practice Logic
function startPractice(item: WrongBookItem) {
  currentQ.value = item
  userAnswers.value = []
  showResult.value = false
  practiceVisible.value = true
}

async function submitPractice() {
  if (!currentQ.value) return
  if (userAnswers.value.length === 0) {
    ElMessage.warning('请选择答案')
    return
  }
  
  // Check Answer
  const correctAns = currentQ.value.questionAnswer
  let correct = false
  
  if (currentQ.value.questionType === 2) {
    // Multiple choice: join and compare
    // Answers usually stored as "A,B" sorted
    const myAns = userAnswers.value.sort().join(',')
    correct = myAns === correctAns
  } else {
    correct = userAnswers.value[0] === correctAns
  }
  
  isCorrect.value = correct
  showResult.value = true
  
  if (currentQ.value?.questionId) {
    fetchReview(currentQ.value.questionId)
  }

  // Submit to backend
  try {
    await wrongBookApi.practice(currentQ.value.id, correct)
    // Update local stats
    currentQ.value.practiceCount++
    if (correct) currentQ.value.practiceCorrectCount++
    await fetchStats() // Refresh global stats
  } catch (e) {
    console.error(e)
  }
}

function handleClosePractice() {
  practiceVisible.value = false
  currentQ.value = null
}

function getOptionKey(opt: string) {
  return opt.split('.')[0]
}

function getOptionContent(opt: string) {
  return opt.substring(2)
}

onMounted(() => {
  fetchData()
  fetchStats()
})
</script>

<template>
  <div class="page">
    <div class="page__bg"></div>
    
    <div class="shell">
      <!-- Header -->
      <header class="head">
        <div class="head__left">
          <div class="head__brand">
            <Icon icon="carbon:notebook" class="head__logo" />
            <span class="head__title">错题本</span>
          </div>
          <div class="head__user">
            <span class="head__name">{{ auth.me?.realName || auth.me?.username }}</span>
          </div>
        </div>
        <el-button class="head__back" link @click="router.push('/dashboard')">
          <Icon icon="iconoir:arrow-left" />
          返回首页
        </el-button>
      </header>

      <main class="main">
        <!-- Stats Grid -->
        <div class="stats-row">
          <div class="stat-card">
            <div class="stat-icon bg-red">
              <Icon icon="carbon:warning-filled" />
            </div>
            <div class="stat-info">
              <span class="stat-label">错题总数</span>
              <strong class="stat-num">{{ stats.totalCount }}</strong>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon bg-blue">
              <Icon icon="carbon:development" />
            </div>
            <div class="stat-info">
              <span class="stat-label">已练习</span>
              <strong class="stat-num">{{ stats.practiceCount }}</strong>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon bg-green">
              <Icon icon="carbon:checkmark-filled" />
            </div>
            <div class="stat-info">
              <span class="stat-label">正确率</span>
              <strong class="stat-num">{{ (stats.practiceAccuracy * 100).toFixed(1) }}%</strong>
            </div>
          </div>
        </div>

        <!-- Filter -->
        <div class="filter-bar">
          <div class="search-box">
            <Icon icon="iconoir:search" class="search-icon" />
            <input 
              v-model="query.keyword" 
              type="text" 
              placeholder="搜索题目关键词..." 
              @keyup.enter="handleSearch"
            >
          </div>
          <el-select v-model="query.type" placeholder="全部题型" clearable @change="handleSearch" style="width: 140px">
            <el-option label="单选题" :value="1" />
            <el-option label="多选题" :value="2" />
            <el-option label="判断题" :value="3" />
          </el-select>
        </div>

        <!-- Question List -->
        <div v-loading="loading" class="list-container">
          <el-empty v-if="!loading && list.length === 0" description="暂无错题，继续保持！" />

          <div v-else class="grid">
            <div v-for="item in list" :key="item.id" class="card">
              <div class="card__head">
                <el-tag size="small" :type="item.questionType === 1 ? '' : (item.questionType === 2 ? 'warning' : 'info')">
                  {{ formatType(item.questionType) }}
                </el-tag>
                <span class="card__time">{{ new Date(item.updateTime).toLocaleDateString() }}</span>
              </div>
              <div class="card__body">
                <p class="card__content">{{ item.questionContent }}</p>
              </div>
              <div class="card__foot">
                <div class="card__stats">
                  <span><Icon icon="iconoir:cancel" /> {{ item.wrongCount }}</span>
                  <span><Icon icon="iconoir:refresh" /> {{ item.practiceCount }}</span>
                </div>
                <el-button type="primary" size="small" round @click="startPractice(item)">
                  练习
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <div class="pagination-wrapper" v-if="total > 0">
          <el-pagination
            v-model:current-page="query.page"
            :page-size="query.size"
            :total="total"
            layout="prev, pager, next"
            background
            @current-change="fetchData"
          />
        </div>
      </main>
    </div>

    <!-- Practice Overlay (Modal) -->
    <div v-if="practiceVisible" class="practice-overlay">
      <div class="practice-modal slide-up">
        <div class="modal-head">
          <h2>错题练习</h2>
          <div class="modal-actions">
            <el-button link @click="openFeedbackDialog">
              <Icon icon="iconoir:chat-bubble" />
              题目反馈
            </el-button>
            <button class="btn-close" @click="handleClosePractice">
              <Icon icon="iconoir:cancel" />
            </button>
          </div>
        </div>
        
        <div v-if="currentQ" class="modal-body">
          <div class="p-question">
            <el-tag class="p-tag">{{ formatType(currentQ.questionType) }}</el-tag>
            <p>{{ currentQ.questionContent }}</p>
          </div>

          <div class="p-options">
            <template v-for="opt in currentQ.questionOptions" :key="opt">
              <!-- Single Choice -->
              <div 
                v-if="currentQ.questionType === 1 || currentQ.questionType === 3"
                class="option-item"
                :class="{ 
                  selected: userAnswers.includes(getOptionKey(opt)),
                  correct: showResult && getOptionKey(opt) === currentQ.questionAnswer,
                  wrong: showResult && userAnswers.includes(getOptionKey(opt)) && getOptionKey(opt) !== currentQ.questionAnswer
                }"
                @click="!showResult && (userAnswers = [getOptionKey(opt)])"
              >
                <span class="opt-key">{{ getOptionKey(opt) }}</span>
                <span class="opt-val">{{ getOptionContent(opt) }}</span>
                <Icon v-if="showResult && getOptionKey(opt) === currentQ.questionAnswer" icon="carbon:checkmark" class="result-icon success" />
                <Icon v-if="showResult && userAnswers.includes(getOptionKey(opt)) && getOptionKey(opt) !== currentQ.questionAnswer" icon="carbon:close" class="result-icon error" />
              </div>

              <!-- Multiple Choice -->
              <div 
                v-if="currentQ.questionType === 2"
                class="option-item"
                :class="{ 
                  selected: userAnswers.includes(getOptionKey(opt)),
                  correct: showResult && currentQ.questionAnswer.includes(getOptionKey(opt)), 
                  wrong: showResult && userAnswers.includes(getOptionKey(opt)) && !currentQ.questionAnswer.includes(getOptionKey(opt))
                }"
                @click="!showResult && (userAnswers.includes(getOptionKey(opt)) ? userAnswers = userAnswers.filter(x => x !== getOptionKey(opt)) : userAnswers.push(getOptionKey(opt)))"
              >
                 <span class="opt-key">{{ getOptionKey(opt) }}</span>
                 <span class="opt-val">{{ getOptionContent(opt) }}</span>
              </div>
            </template>
          </div>

          <div v-if="showResult" class="result-box" :class="{ success: isCorrect, error: !isCorrect }">
            <div class="result-title">
              <Icon :icon="isCorrect ? 'carbon:checkmark-filled' : 'carbon:warning-filled'" />
              {{ isCorrect ? '回答正确！' : '回答错误' }}
            </div>
            <div class="result-analysis">
              <strong>解析：</strong>
              <p>{{ currentQ.questionAnalysis || '暂无解析' }}</p>
            </div>
            <div class="result-analysis review-box" v-if="teacherReview">
              <strong>教师讲评：</strong>
              <div class="review-title">{{ teacherReview.title }}</div>
              <p>{{ teacherReview.content }}</p>
              <div class="review-time">发布于 {{ teacherReview.createTime?.replace('T', ' ') }}</div>
            </div>
          </div>
        </div>

        <div class="modal-foot">
          <el-button v-if="!showResult" type="primary" size="large" @click="submitPractice">提交答案</el-button>
          <el-button v-else size="large" @click="handleClosePractice">完成练习</el-button>
        </div>
      </div>
    </div>

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
</template>

<style scoped>
.page {
  min-height: 100vh;
  background-color: #f4f5f7;
  position: relative;
}

.page__bg {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)' opacity='0.4'/%3E%3C/svg%3E");
  opacity: 0.4;
  pointer-events: none;
  z-index: 0;
  mix-blend-mode: overlay;
}

.shell {
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 24px;
  position: relative;
  z-index: 1;
}

/* Header */
.head {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32px;
}

.head__left { display: flex; align-items: center; gap: 24px; }
.head__brand { display: flex; align-items: center; gap: 8px; color: #1a1e23; }
.head__logo { font-size: 24px; color: #10d4a6; }
.head__title { font-size: 18px; font-weight: 800; letter-spacing: -0.5px; }
.head__user { 
  display: flex; align-items: center; gap: 8px; font-size: 13px; 
  padding: 4px 10px; background: rgba(0,0,0,0.04); border-radius: 99px; color: #555; 
}
.head__back { color: #666; font-size: 14px; }
.head__back:hover { color: #10d4a6; }

/* Main */
.main { padding-bottom: 60px; }

/* Stats */
.stats-row {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-bottom: 40px;
}

.stat-card {
  flex: 1;
  max-width: 280px;
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.02);
  transition: transform 0.2s;
}

.stat-card:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(0,0,0,0.05); }

.stat-icon {
  width: 48px; height: 48px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px;
}
.bg-red { background: #fff0f0; color: #ff4757; }
.bg-blue { background: #f0f7ff; color: #2e86de; }
.bg-green { background: #f0fff4; color: #10d4a6; }

.stat-info { display: flex; flex-direction: column; }
.stat-label { font-size: 12px; color: #888; margin-bottom: 4px; }
.stat-num { font-size: 24px; font-weight: 800; color: #1a1e23; }

/* Filter */
.filter-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.search-box {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
}

.search-icon { color: #999; font-size: 20px; margin-right: 12px; }

.search-box input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 14px;
  padding: 12px 0;
  color: #1a1e23;
  background: transparent;
}

/* Grid */
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
  padding-bottom: 40px;
}

.card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
  border: 1px solid transparent;
}

.card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0,0,0,0.06);
  border-color: rgba(16, 212, 166, 0.2);
}

.card__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card__time { font-size: 12px; color: #999; }

.card__body { flex: 1; margin-bottom: 16px; }
.card__content {
  font-size: 15px;
  color: #1a1e23;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card__foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid #f5f5f5;
}

.card__stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #888;
}
.card__stats span { display: flex; align-items: center; gap: 4px; }

/* Modal */
.practice-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.4);
  backdrop-filter: blur(4px);
  z-index: 100;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px;
}

.practice-modal {
  background: #fff;
  width: 100%;
  max-width: 700px;
  max-height: 85vh;
  border-radius: 24px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 24px 48px rgba(0,0,0,0.15);
  overflow: hidden;
}

.slide-up { animation: slideUp 0.3s ease-out; }
@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.modal-head {
  padding: 20px 24px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.modal-head h2 { margin: 0; font-size: 18px; font-weight: 700; }
.btn-close { background: none; border: none; font-size: 20px; cursor: pointer; color: #999; }
.btn-close:hover { color: #333; }

.modal-body { padding: 32px; overflow-y: auto; flex: 1; }

.p-question { margin-bottom: 32px; }
.p-tag { margin-right: 8px; vertical-align: middle; }
.p-question p { display: inline; font-size: 18px; font-weight: 600; line-height: 1.6; color: #1a1e23; }

.p-options { display: flex; flex-direction: column; gap: 12px; }

.option-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border: 2px solid #f4f5f7;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.option-item:hover { background: #fafafa; border-color: #eee; }
.option-item.selected { border-color: #10d4a6; background: #f0fdf9; }
.option-item.correct { border-color: #10d4a6; background: #f0fdf9; }
.option-item.wrong { border-color: #ff4757; background: #fff0f1; }

.opt-key { font-weight: 700; width: 32px; font-size: 16px; color: #1a1e23; }
.opt-val { font-size: 16px; color: #333; }

.result-icon { position: absolute; right: 16px; font-size: 20px; }
.result-icon.success { color: #10d4a6; }
.result-icon.error { color: #ff4757; }

.result-box {
  margin-top: 32px;
  padding: 20px;
  border-radius: 12px;
}
.result-box.success { background: #f0fdf9; border: 1px solid #10d4a6; }
.result-box.error { background: #fff0f1; border: 1px solid #ff4757; }

.result-title {
  font-size: 16px; font-weight: 700; margin-bottom: 12px; display: flex; align-items: center; gap: 8px;
}
.result-box.success .result-title { color: #10d4a6; }
.result-box.error .result-title { color: #ff4757; }

.result-analysis {
  color: #1a1e23;
  font-size: 15px;
  line-height: 1.6;
}
.result-analysis strong {
  font-weight: 700;
  color: #000;
}

.review-box {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #d1d5db;
  color: #1a1e23;
}

.review-title {
  font-weight: 600;
  margin: 4px 0 8px;
  color: #1a1e23;
}

.review-time {
  font-size: 12px;
  color: #6b7280;
  margin-top: 8px;
  text-align: right;
}

.modal-foot {
  padding: 20px 24px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  background: #fafafa;
}

.pagination-wrapper {
  margin-top: 40px;
  display: flex;
  justify-content: center;
}
</style>
