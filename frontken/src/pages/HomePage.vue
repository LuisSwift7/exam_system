<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { Icon } from '@iconify/vue'
import { http } from '../api/http'
import { ElMessage } from 'element-plus'
import StudentManage from './teacher/StudentManage.vue'
import QuestionManage from './teacher/QuestionManage.vue'
import ExamManage from './teacher/ExamManage.vue'
import FeedbackManage from './teacher/FeedbackManage.vue'
import TeacherManage from './admin/TeacherManage.vue'

const auth = useAuthStore()
const router = useRouter()

const loading = ref(false)
const exams = ref<any[]>([])
const activeTab = ref('student') // 'student' | 'question' | 'exam'
const adminTab = ref('teacher') // 'teacher'

const title = computed(() => {
  if (auth.me?.roleCode === 'TEACHER') return '教师控制台'
  if (auth.me?.roleCode === 'ADMIN') return '系统管理'
  return '学生考场'
})

async function fetchExams() {
  if (auth.me?.roleCode !== 'STUDENT') return
  loading.value = true
  try {
    const res = await http.get('/api/student/exams')
    exams.value = res.data.data.list
  } catch (e: any) {
    ElMessage.error(e?.message || '获取考试列表失败')
  } finally {
    loading.value = false
  }
}

async function logout() {
  auth.logout()
  await router.replace('/login')
}

function getStatusTag(item: any) {
  if (item.studentStatus === 1) return { type: 'success', text: '已完成' }
  if (item.studentStatus === 0) return { type: 'warning', text: '答题中' }
  if (item.studentStatus === 2) return { type: 'danger', text: '已结束' }
  
  // Check exam time for not started exams
  const now = new Date()
  const startTime = new Date(item.startTime)
  const endTime = new Date(item.endTime)
  
  if (now < startTime) return { type: 'info', text: '未开始' }
  if (now > endTime) return { type: 'danger', text: '已结束' }
  return { type: 'primary', text: '进行中' }
}

const handleExamClick = (item: any) => {
  if (item.studentStatus === 1) {
    router.push(`/student/exam-result/${item.recordId}`)
  } else if (item.studentStatus === 0) {
    router.push(`/student/exam/${item.id}/taking`)
  } else {
    router.push(`/student/exam/${item.id}`)
  }
}

const getBtnText = (item: any) => {
  if (item.studentStatus === 1) return '查看结果'
  if (item.studentStatus === 0) return '继续考试'
  if (item.studentStatus === 2) return '已结束'
  
  // Check exam time for not started exams
  const now = new Date()
  const startTime = new Date(item.startTime)
  const endTime = new Date(item.endTime)
  
  if (now < startTime) return '等待开始'
  if (now > endTime) return '已结束'
  return '进入考试'
}

function isExamDisabled(item: any) {
  if (item.studentStatus === 1 || item.studentStatus === 0) return false
  if (item.studentStatus === 2) return true
  
  // Check exam time
  const now = new Date()
  const startTime = new Date(item.startTime)
  const endTime = new Date(item.endTime)
  
  if (now < startTime) return true // Not started yet
  if (now > endTime) return true // Already ended
  return false // In progress
}

// Student Feedback
const feedbackDialogVisible = ref(false)
const myFeedbacks = ref<any[]>([])
const feedbackLoading = ref(false)

async function openMyFeedbacks() {
  feedbackDialogVisible.value = true
  feedbackLoading.value = true
  try {
    const res = await http.get('/api/student/feedback/list', {
      params: { page: 1, size: 100 }
    })
    myFeedbacks.value = res.data.data.records
  } catch (e: any) {
    ElMessage.error(e?.message || '获取反馈列表失败')
  } finally {
    feedbackLoading.value = false
  }
}

onMounted(() => {
  fetchExams()
})
</script>

<template>
  <div class="page">
    <div class="page__bg" />
    
    <div class="shell">
      <header class="head">
        <div class="head__left">
          <div class="head__brand">
            <Icon icon="iconoir:graduation-cap" class="head__logo" />
            <span class="head__title">{{ title }}</span>
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

      <!-- Student View -->
      <main class="main" v-if="auth.me?.roleCode === 'STUDENT'">
        <div class="welcome-section fade-in">
          <div class="welcome-text">
            <h1>欢迎回来，{{ auth.me?.realName || '同学' }} 👋</h1>
            <p>准备好迎接新的挑战了吗？这里是你待参加的考试。</p>
          </div>
          <div class="welcome-actions">
            <button class="btn-wrong-book" @click="$router.push('/student/wrong-book')">
              <Icon icon="carbon:notebook" class="btn-icon" />
              <span>错题本</span>
            </button>
            <button class="btn-wrong-book" style="margin-left: 12px; background: #fff; color: #10d4a6; border: 1px solid #10d4a6" @click="openMyFeedbacks">
              <Icon icon="iconoir:chat-bubble" class="btn-icon" />
              <span>我的反馈</span>
            </button>
            <img src="https://api.iconify.design/iconoir:graduation-cap.svg?color=%2310d4a6" class="welcome-icon" />
          </div>
        </div>

        <div class="grid" v-loading="loading">
          <el-empty v-if="!loading && exams.length === 0" description="暂无考试，去休息一下吧 ~" />
          
          <div v-for="(item, index) in exams" :key="item.id" class="card slide-up" :style="{ animationDelay: `${index * 0.1}s` }">
            <div class="card__status">
              <el-tag :type="getStatusTag(item).type as any" effect="dark" size="small" round>
                {{ getStatusTag(item).text }}
              </el-tag>
            </div>
            <div class="card__body">
              <h3 class="card__title">{{ item.title }}</h3>
              <p class="card__desc">{{ item.description || '暂无描述' }}</p>
              
              <div class="card__meta">
                <div class="meta__item">
                  <Icon icon="iconoir:timer" />
                  <span>{{ item.duration }} 分钟</span>
                </div>
                <div class="meta__item">
                  <Icon icon="iconoir:calendar" />
                  <span>{{ item.startTime?.replace('T', ' ').slice(0, 16) }}</span>
                </div>
              </div>
            </div>
            
            <div class="card__foot">
              <el-button 
                class="card__btn" 
                type="primary" 
                :disabled="isExamDisabled(item)" 
                round
                @click="handleExamClick(item)"
              >
                {{ getBtnText(item) }}
                <Icon icon="iconoir:arrow-right" class="btn-icon" v-if="!isExamDisabled(item)" />
              </el-button>
            </div>
          </div>
        </div>

        <!-- Student Feedback Dialog -->
        <el-dialog v-model="feedbackDialogVisible" title="我的反馈记录" width="600px">
          <div v-loading="feedbackLoading" class="feedback-list">
            <el-empty v-if="myFeedbacks.length === 0" description="暂无反馈记录" />
            <div v-else v-for="item in myFeedbacks" :key="item.id" class="feedback-item">
              <div class="fb-head">
                <span class="fb-time">{{ item.createTime?.replace('T', ' ') }}</span>
                <el-tag size="small" :type="item.status === 1 ? 'success' : 'info'">{{ item.status === 1 ? '已回复' : '待处理' }}</el-tag>
              </div>
              <div class="fb-content">
                <strong>我的提问：</strong> {{ item.content }}
              </div>
              <div v-if="item.status === 1" class="fb-reply">
                <strong>教师回复：</strong> {{ item.replyContent }}
                <div class="reply-time">{{ item.replyTime?.replace('T', ' ') }}</div>
              </div>
            </div>
          </div>
        </el-dialog>
      </main>

      <!-- Teacher View -->
      <main class="main teacher-view" v-else-if="auth.me?.roleCode === 'TEACHER'">
        <div class="dashboard-header fade-in">
          <div class="dash-welcome">
            <h1>教师控制台</h1>
            <p>高效管理您的课程与考试。</p>
          </div>
          <div class="quick-stats">
            <div class="stat-item">
              <div class="stat-icon bg-blue">
                <Icon icon="iconoir:group" />
              </div>
              <div class="stat-info">
                <span class="stat-label">学生管理</span>
                <span class="stat-action">查看列表</span>
              </div>
            </div>
            <div class="stat-item">
              <div class="stat-icon bg-green">
                <Icon icon="iconoir:book-stack" />
              </div>
              <div class="stat-info">
                <span class="stat-label">题库资源</span>
                <span class="stat-action">管理试题</span>
              </div>
            </div>
          </div>
        </div>

        <div class="tabs-container fade-in" style="animation-delay: 0.2s">
          <div class="tabs">
            <div class="tab" :class="{ active: activeTab === 'student' }" @click="activeTab = 'student'">
              <Icon icon="iconoir:group" />
              <span>学生管理</span>
            </div>
            <div class="tab" :class="{ active: activeTab === 'question' }" @click="activeTab = 'question'">
              <Icon icon="iconoir:book-stack" />
              <span>试题管理</span>
            </div>
            <div class="tab" :class="{ active: activeTab === 'exam' }" @click="activeTab = 'exam'">
              <Icon icon="iconoir:page" />
              <span>试卷管理</span>
            </div>
            <div class="tab" :class="{ active: activeTab === 'feedback' }" @click="activeTab = 'feedback'">
              <Icon icon="iconoir:chat-bubble" />
              <span>反馈管理</span>
            </div>
          </div>
        </div>

        <div class="tab-content fade-in" style="animation-delay: 0.3s">
          <StudentManage v-if="activeTab === 'student'" />
          <QuestionManage v-if="activeTab === 'question'" />
          <ExamManage v-if="activeTab === 'exam'" />
          <FeedbackManage v-if="activeTab === 'feedback'" />
        </div>
      </main>

      <!-- Admin View -->
      <main class="main teacher-view" v-else-if="auth.me?.roleCode === 'ADMIN'">
        <div class="dashboard-header fade-in">
          <div class="dash-welcome">
            <h1>系统管理中心</h1>
            <p>维护系统安全与用户权限。</p>
          </div>
        </div>

        <div class="tabs-container fade-in" style="animation-delay: 0.2s">
          <div class="tabs">
            <div class="tab" :class="{ active: adminTab === 'teacher' }" @click="adminTab = 'teacher'">
              <Icon icon="iconoir:group" />
              <span>教师管理</span>
            </div>
          </div>
        </div>

        <div class="tab-content fade-in" style="animation-delay: 0.3s">
          <TeacherManage v-if="adminTab === 'teacher'" />
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  position: relative;
  background: #f6f8fa;
}

.page__bg {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 80% 10%, rgba(16, 212, 166, 0.08), transparent 40%),
    radial-gradient(circle at 10% 90%, rgba(255, 161, 22, 0.08), transparent 40%);
  pointer-events: none;
}

.feedback-list {
  max-height: 500px;
  overflow-y: auto;
  padding: 10px;
}

.feedback-item {
  background: #f9fafb;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  border: 1px solid #e5e7eb;
}

.fb-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 12px;
  color: #6b7280;
}

.fb-content {
  color: #1a1e23;
  margin-bottom: 12px;
  line-height: 1.5;
}

.fb-reply {
  background: #f0fdf4;
  padding: 12px;
  border-radius: 6px;
  color: #166534;
  font-size: 14px;
}

.reply-time {
  font-size: 12px;
  color: #86efac;
  margin-top: 4px;
  text-align: right;
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
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

.head {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
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
  color: #555;
}

.head__role {
  font-weight: 700;
  font-size: 11px;
  opacity: 0.6;
}

.head__logout {
  color: #666;
  font-size: 13px;
}

.head__logout:hover {
  color: #f56c6c;
}

.main {
  padding: 40px 0 60px;
}

.main__title {
  margin-bottom: 24px;
}

.main__title h2 {
  font-size: 24px;
  font-weight: 800;
  color: #1a1e23;
  margin: 0;
}

.main__sub {
  font-size: 13px;
  color: #888;
  margin-top: 4px;
  display: block;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
}

.card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03), 0 1px 2px rgba(0,0,0,0.02);
  border: 1px solid rgba(0,0,0,0.04);
  transition: all 0.2s ease;
  position: relative;
  display: flex;
  flex-direction: column;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(0,0,0,0.06);
}

.card__status {
  position: absolute;
  top: 16px;
  right: 16px;
}

.card__body {
  flex: 1;
}

.card__title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1e23;
  margin: 0 0 8px;
  padding-right: 60px;
}

.card__desc {
  font-size: 13px;
  color: #666;
  margin: 0 0 16px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card__meta {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.meta__item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #888;
}

.card__foot {
  margin-top: auto;
}

.card__btn {
  width: 100%;
  font-weight: 600;
}

/* Teacher View Styles */
.teacher-view {
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: 100%;
}

.tab-content {
  width: 100%;
}

.tabs {
  display: flex;
  gap: 8px;
  background: #fff;
  padding: 8px;
  border-radius: 16px;
  width: fit-content;
}

.tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.tab:hover {
  background: #f5f7fa;
  color: #1a1e23;
}

.tab.active {
  background: #1a1e23;
  color: #fff;
  box-shadow: 0 4px 12px rgba(26, 30, 35, 0.2);
}

.placeholder {
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Animations */
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.fade-in {
  animation: fadeIn 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  opacity: 0; /* Init hidden */
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.slide-up {
  animation: slideUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  opacity: 0;
}

/* Welcome Section */
.welcome-section {
  background: linear-gradient(135deg, #fff 0%, #f0fdf9 100%);
  border-radius: 24px;
  padding: 40px;
  margin-bottom: 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 4px 20px rgba(16, 212, 166, 0.05);
  border: 1px solid rgba(16, 212, 166, 0.1);
  position: relative;
  overflow: hidden;
}

.welcome-text h1 {
  font-size: 28px;
  font-weight: 800;
  color: #1a1e23;
  margin: 0 0 12px;
  background: linear-gradient(90deg, #1a1e23, #444);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.welcome-text p {
  font-size: 16px;
  color: #666;
  margin: 0;
}

.welcome-icon {
  width: 120px;
  height: 120px;
  opacity: 0.8;
  transform: rotate(-10deg);
}

.welcome-actions {
  display: flex;
  align-items: center;
  gap: 32px;
}

.btn-wrong-book {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border: 3px solid #1a1e23;
  padding: 12px 24px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 700;
  color: #1a1e23;
  cursor: pointer;
  box-shadow: 4px 4px 0 #1a1e23;
  transition: all 0.2s;
}

.btn-wrong-book:hover {
  transform: translate(-2px, -2px);
  box-shadow: 6px 6px 0 #1a1e23;
  background: #f0fdf9;
}

.btn-wrong-book:active {
  transform: translate(2px, 2px);
  box-shadow: 0 0 0 #1a1e23;
}

.btn-wrong-book .btn-icon {
  font-size: 24px;
  color: #10d4a6;
}

/* Dashboard Header */
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.dash-welcome h1 {
  font-size: 26px;
  font-weight: 800;
  margin: 0 0 8px;
  color: #1a1e23;
}

.dash-welcome p {
  color: #888;
  font-size: 14px;
  margin: 0;
}

.quick-stats {
  display: flex;
  gap: 20px;
}

.stat-item {
  background: #fff;
  padding: 12px 20px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
  transition: all 0.2s;
}

.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0,0,0,0.06);
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.bg-blue { background: rgba(64, 158, 255, 0.1); color: #409eff; }
.bg-green { background: rgba(103, 194, 58, 0.1); color: #67c23a; }

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 14px;
  font-weight: 700;
  color: #333;
}

.stat-action {
  font-size: 12px;
  color: #999;
}

/* Tabs */
.tabs-container {
  margin-bottom: 24px;
}

.tabs {
  background: #fff;
  padding: 6px;
  border-radius: 14px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.03);
}

.tab {
  padding: 10px 24px;
  border-radius: 10px;
  font-weight: 600;
}

.tab:hover {
  background: rgba(0,0,0,0.02);
}

.tab.active {
  background: #1a1e23;
  color: #fff;
  box-shadow: 0 4px 12px rgba(26, 30, 35, 0.15);
}

.btn-icon {
  margin-left: 4px;
  transition: transform 0.2s;
}

.card__btn:hover .btn-icon {
  transform: translateX(4px);
}
</style>
