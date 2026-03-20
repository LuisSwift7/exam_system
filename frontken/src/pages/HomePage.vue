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
import ClassManage from './teacher/ClassManage.vue'
import TeacherManage from './admin/TeacherManage.vue'
import NotificationManage from './teacher/NotificationManage.vue'
import ReviewManage from './teacher/ReviewManage.vue'
import UserManage from './admin/UserManage.vue'
import LogManage from './admin/LogManage.vue'
import NotificationBell from '../components/NotificationBell.vue'

const auth = useAuthStore()
const router = useRouter()

const loading = ref(false)
const exams = ref<any[]>([])
const activeTab = ref('student') // 'student' | 'question' | 'exam'
const adminTab = ref('teacher') // 'teacher'

// 教师控制台数据
const dashboardLoading = ref(false)
const dashboardOverview = ref({
  studentCount: 0,
  classCount: 0,
  examCount: 0,
  feedbackCount: 0,
  newStudentsThisWeek: 0,
  newClassesThisMonth: 0,
  newExamsThisWeek: 0,
  resolvedFeedbacksThisWeek: 0
})
const recentActivities = ref<any[]>([])

// 处理活动按钮点击
function handleActivityAction(activity: any) {
  switch (activity.type) {
    case 'class_apply':
      // 处理班级申请
      router.push('/teacher/class-manage');
      break;
    case 'exam_complete':
      // 查看考试结果
      router.push('/teacher/exam-manage');
      break;
    case 'exam_publish':
      // 查看试卷
      router.push('/teacher/exam-manage');
      break;
    case 'feedback_submit':
      // 回复反馈
      router.push('/teacher/feedback');
      break;
    default:
      break;
  }
}

// 查看全部活动
function viewAllActivities() {
  // 这里可以跳转到活动记录页面或显示更多活动
  ElMessage.info('查看全部活动功能开发中');
}

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

function hasResultAvailable(item: any) {
  return item.studentStatus === 1 || (item.studentStatus === 2 && !!item.recordId)
}

function getStatusTag(item: any) {
  if (hasResultAvailable(item)) return { type: 'success', text: '已完成' }
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
  if (hasResultAvailable(item)) {
    router.push(`/student/exam-result/${item.recordId}`)
  } else if (item.studentStatus === 0) {
    router.push(`/student/exam/${item.id}/taking`)
  } else {
    router.push(`/student/exam/${item.id}`)
  }
}

const getBtnText = (item: any) => {
  if (hasResultAvailable(item)) return '查看结果'
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
  if (hasResultAvailable(item)) return false
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

function formatFeedbackTime(value?: string) {
  return value ? value.replace('T', ' ') : '-'
}

function formatFeedbackQuestionType(type?: number) {
  const typeMap: Record<number, string> = {
    1: '单选题',
    2: '多选题',
    3: '判断题'
  }
  return type ? typeMap[type] || '题目' : '题目'
}

function normalizeFeedbackOptions(options: any) {
  if (Array.isArray(options)) {
    return options.map((item: any) => ({
      key: item?.key || '',
      value: item?.value || '',
      imageUrl: item?.imageUrl || ''
    }))
  }

  if (typeof options === 'string') {
    try {
      const parsed = JSON.parse(options)
      if (Array.isArray(parsed)) {
        return parsed.map((item: any) => ({
          key: item?.key || '',
          value: item?.value || '',
          imageUrl: item?.imageUrl || ''
        }))
      }
    } catch {
      return []
    }
  }

  return []
}

async function fetchDashboardOverview() {
  if (auth.me?.roleCode !== 'TEACHER') return
  dashboardLoading.value = true
  try {
    const res = await http.get('/api/teacher/dashboard/overview')
    dashboardOverview.value = res.data.data
  } catch (e: any) {
    ElMessage.error(e?.message || '获取数据概览失败')
  } finally {
    dashboardLoading.value = false
  }
}

async function fetchRecentActivities() {
  if (auth.me?.roleCode !== 'TEACHER') return
  try {
    const res = await http.get('/api/teacher/dashboard/activities')
    recentActivities.value = res.data.data || []
  } catch (e: any) {
    ElMessage.error(e?.message || '获取最近活动失败')
    recentActivities.value = []
  }
}

onMounted(() => {
  fetchExams()
  if (auth.me?.roleCode === 'TEACHER') {
    fetchDashboardOverview()
    fetchRecentActivities()
  }
})
</script>

<template>
  <div class="page">
    <div class="page__bg">
      <div class="bg-shape shape-1"></div>
      <div class="bg-shape shape-2"></div>
      <div class="bg-shape shape-3"></div>
      <div class="bg-shape shape-4"></div>
    </div>
    
    <!-- 装饰元素 -->
    <div class="decorative-elements">
      <div class="decorative-circle circle-1"></div>
      <div class="decorative-circle circle-2"></div>
      <div class="decorative-circle circle-3"></div>
    </div>
    
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
        <div class="head__right">
          <NotificationBell />
          <el-button class="head__logout" link @click="logout">
            <Icon icon="iconoir:log-out" />
            退出
          </el-button>
        </div>
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
            <button class="btn-wrong-book" @click="$router.push('/student/classes')">
              <Icon icon="iconoir:group" class="btn-icon" />
              <span>我的班级</span>
            </button>
            <button class="btn-wrong-book" style="margin-left: 12px; background: #fff; color: #10d4a6; border: 1px solid #10d4a6" @click="openMyFeedbacks">
              <Icon icon="iconoir:chat-bubble" class="btn-icon" />
              <span>我的反馈</span>
            </button>
            <button class="btn-wrong-book" style="margin-left: 12px; background: #fffaf2; color: #c26b18; border: 1px solid #f1cfaa" @click="$router.push('/student/reviews')">
              <Icon icon="iconoir:page-search" class="btn-icon" />
              <span>我的讲评</span>
            </button>
            <img src="https://api.iconify.design/iconoir:graduation-cap.svg?color=%2310d4a6" class="welcome-icon" />
          </div>
        </div>

        <div class="grid" v-loading="loading">
          <div v-if="!loading && exams.length === 0" class="empty-container">
            <el-empty description="暂无考试，去休息一下吧 ~" />
          </div>
          
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
        <el-dialog v-model="feedbackDialogVisible" title="我的反馈记录" width="680px">
          <div v-loading="feedbackLoading" class="feedback-list">
            <el-empty v-if="myFeedbacks.length === 0" description="暂无反馈记录" />
            <div v-else v-for="item in myFeedbacks" :key="item.id" class="feedback-item">
              <div class="fb-head">
                <span class="fb-time">{{ formatFeedbackTime(item.createTime) }}</span>
                <el-tag size="small" :type="item.status === 1 ? 'success' : 'info'">{{ item.status === 1 ? '已回复' : '待处理' }}</el-tag>
              </div>
              <div v-if="item.question" class="fb-question">
                <div class="fb-question-head">
                  <span>{{ item.question.category || '未分类' }}</span>
                  <span>{{ formatFeedbackQuestionType(item.question.type) }}</span>
                </div>
                <div class="fb-question-content">{{ item.question.content }}</div>
                <div
                  v-for="option in normalizeFeedbackOptions(item.question.options)"
                  :key="`${item.id}-${option.key}`"
                  class="fb-option"
                >
                  <span class="fb-option-key">{{ option.key }}</span>
                  <div class="fb-option-body">
                    <span>{{ option.value }}</span>
                    <img v-if="option.imageUrl" :src="option.imageUrl" alt="选项配图" class="fb-option-image" />
                  </div>
                </div>
              </div>
              <div class="fb-content">
                <strong>我的提问：</strong> {{ item.content }}
              </div>
              <div v-if="item.status === 1" class="fb-reply">
                <strong>教师回复：</strong> {{ item.replyContent }}
                <div class="reply-time">{{ formatFeedbackTime(item.replyTime) }}</div>
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
            <div class="stat-item" @click="activeTab = 'student'">
              <div class="stat-icon bg-blue">
                <Icon icon="iconoir:group" />
              </div>
              <div class="stat-info">
                <span class="stat-label">学生管理</span>
                <span class="stat-action">查看列表</span>
              </div>
            </div>
            <div class="stat-item" @click="activeTab = 'question'">
              <div class="stat-icon bg-green">
                <Icon icon="iconoir:book-stack" />
              </div>
              <div class="stat-info">
                <span class="stat-label">题库资源</span>
                <span class="stat-action">管理试题</span>
              </div>
            </div>
            <div class="stat-item" @click="activeTab = 'exam'">
              <div class="stat-icon bg-purple">
                <Icon icon="iconoir:page" />
              </div>
              <div class="stat-info">
                <span class="stat-label">试卷管理</span>
                <span class="stat-action">创建试卷</span>
              </div>
            </div>
            <div class="stat-item" @click="activeTab = 'feedback'">
              <div class="stat-icon bg-orange">
                <Icon icon="iconoir:chat-bubble" />
              </div>
              <div class="stat-info">
                <span class="stat-label">学生反馈</span>
                <span class="stat-action">查看回复</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 数据概览 -->
        <div class="dashboard-overview fade-in" style="animation-delay: 0.2s" v-loading="dashboardLoading">
          <div class="overview-card">
            <div class="overview-icon bg-blue">
              <Icon icon="iconoir:user" />
            </div>
            <div class="overview-content">
              <h3>学生总数</h3>
              <p class="overview-value">{{ dashboardOverview.studentCount }}</p>
              <p class="overview-change">+{{ dashboardOverview.newStudentsThisWeek }} 本周</p>
            </div>
          </div>
          <div class="overview-card">
            <div class="overview-icon bg-green">
              <Icon icon="iconoir:group" />
            </div>
            <div class="overview-content">
              <h3>班级数量</h3>
              <p class="overview-value">{{ dashboardOverview.classCount }}</p>
              <p class="overview-change">+{{ dashboardOverview.newClassesThisMonth }} 本月</p>
            </div>
          </div>
          <div class="overview-card">
            <div class="overview-icon bg-purple">
              <Icon icon="iconoir:page" />
            </div>
            <div class="overview-content">
              <h3>已发布试卷</h3>
              <p class="overview-value">{{ dashboardOverview.examCount }}</p>
              <p class="overview-change">+{{ dashboardOverview.newExamsThisWeek }} 本周</p>
            </div>
          </div>
        </div>

        <!-- 最近活动 -->
        <div class="recent-activities fade-in" style="animation-delay: 0.3s">
          <div class="activity-header">
            <h2>最近活动</h2>
          </div>
          <div class="activity-list">
            <div v-if="recentActivities.length === 0" class="empty-activities">
              <p>暂无最近活动</p>
            </div>
            <div v-for="(activity, index) in recentActivities" :key="index" class="activity-item">
              <div :class="['activity-icon', activity.color]">
                <Icon :icon="activity.icon" />
              </div>
              <div class="activity-content">
                <p class="activity-text" v-html="activity.text"></p>
                <p class="activity-time">{{ activity.time }}</p>
              </div>
            </div>
          </div>
        </div>

        <div class="tabs-container fade-in" style="animation-delay: 0.4s">
          <div class="tabs">
            <div class="tab" :class="{ active: activeTab === 'student' }" @click="activeTab = 'student'">
              <Icon icon="iconoir:group" />
              <span>学生管理</span>
            </div>
            <div class="tab" :class="{ active: activeTab === 'class' }" @click="activeTab = 'class'">
              <Icon icon="iconoir:user" />
              <span>班级管理</span>
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
            <div class="tab" :class="{ active: activeTab === 'notification' }" @click="activeTab = 'notification'">
              <Icon icon="iconoir:bell" />
              <span>通知管理</span>
            </div>
            <div class="tab" :class="{ active: activeTab === 'review' }" @click="activeTab = 'review'">
              <Icon icon="iconoir:edit" />
              <span>讲评管理</span>
            </div>
          </div>
        </div>

        <div class="tab-content fade-in" style="animation-delay: 0.5s">
          <StudentManage v-if="activeTab === 'student'" />
          <ClassManage v-if="activeTab === 'class'" />
          <QuestionManage v-if="activeTab === 'question'" />
          <ExamManage v-if="activeTab === 'exam'" />
          <FeedbackManage v-if="activeTab === 'feedback'" />
          <NotificationManage v-if="activeTab === 'notification'" />
          <ReviewManage v-if="activeTab === 'review'" />
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
            <div class="tab" :class="{ active: adminTab === 'user' }" @click="adminTab = 'user'">
              <Icon icon="iconoir:user" />
              <span>用户管理</span>
            </div>
            <div class="tab" :class="{ active: adminTab === 'log' }" @click="adminTab = 'log'">
              <Icon icon="iconoir:journal" />
              <span>日志管理</span>
            </div>
            <div class="tab" :class="{ active: adminTab === 'notification' }" @click="adminTab = 'notification'">
              <Icon icon="iconoir:bell" />
              <span>通知管理</span>
            </div>
          </div>
        </div>

        <div class="tab-content fade-in" style="animation-delay: 0.3s">
          <TeacherManage v-if="adminTab === 'teacher'" />
          <UserManage v-if="adminTab === 'user'" />
          <LogManage v-if="adminTab === 'log'" />
          <NotificationManage v-if="adminTab === 'notification'" />
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  position: relative;
  background: #f8fafc; /* Lighter, cleaner base */
  overflow-x: hidden;
}

.page__bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
  z-index: 0;
}

/* Golden Ratio Split Background */
.bg-shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.6;
}

.shape-1 {
  /* Top-left main accent (Golden ratio focus) */
  width: 60vw;
  height: 60vw;
  top: -20vw;
  left: -10vw;
  background: radial-gradient(circle, rgba(16, 212, 166, 0.15), rgba(16, 212, 166, 0.05) 60%, transparent 80%);
  animation: float 15s ease-in-out infinite;
}

.shape-2 {
  /* Bottom-right balance */
  width: 50vw;
  height: 50vw;
  bottom: -10vw;
  right: -10vw;
  background: radial-gradient(circle, rgba(255, 161, 22, 0.1), rgba(255, 161, 22, 0.05) 60%, transparent 80%);
  animation: float 20s ease-in-out infinite reverse;
}

.shape-3 {
  /* Top-right subtle highlight */
  width: 30vw;
  height: 30vw;
  top: 10vh;
  right: 10vw;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.08), transparent 70%);
  animation: pulse 10s ease-in-out infinite;
}

.shape-4 {
  /* Left sidebar implication strip */
  position: absolute;
  top: 0;
  left: 0;
  width: 280px; /* Sidebar width implication */
  height: 100%;
  background: linear-gradient(90deg, rgba(255,255,255,0.8), rgba(255,255,255,0));
  backdrop-filter: blur(10px);
  z-index: 0;
  display: none; /* Hidden by default, enable if we want a strong sidebar look */
}

/* Static Decorative Elements (Geometric) */
.decorative-elements {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.decorative-circle {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(16, 212, 166, 0.1);
}

.circle-1 {
  width: 400px;
  height: 400px;
  top: -50px;
  right: 10%;
  animation: rotate 60s linear infinite;
}

.circle-2 {
  width: 200px;
  height: 200px;
  bottom: 20%;
  left: 5%;
  border-color: rgba(255, 161, 22, 0.15);
  animation: float 12s ease-in-out infinite;
}

.circle-3 {
  width: 80px;
  height: 80px;
  top: 20%;
  left: 15%;
  background: rgba(16, 212, 166, 0.1);
  border: none;
  animation: pulse 4s ease-in-out infinite;
}

.empty-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  grid-column: 1 / -1;
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

.fb-question {
  margin-bottom: 12px;
  padding: 12px;
  border-radius: 10px;
  background: #fffef8;
  border: 1px solid #ece5d4;
}

.fb-question-head {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
  font-size: 12px;
  color: #8b6d3b;
}

.fb-question-content {
  color: #1f2937;
  line-height: 1.6;
  margin-bottom: 10px;
}

.fb-option {
  display: flex;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #f0eadc;
}

.fb-option + .fb-option {
  margin-top: 8px;
}

.fb-option-key {
  width: 18px;
  font-weight: 700;
  color: #7a5a16;
}

.fb-option-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
  color: #475569;
  line-height: 1.5;
}

.fb-option-image {
  max-width: 100%;
  max-height: 160px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  object-fit: contain;
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

.shell {
  position: relative;
  z-index: 1;
  max-width: 1400px; /* Increased max-width for better use of space */
  margin: 0 auto;
  padding: 0 40px;
}

.head {
  height: 80px; /* Taller header */
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(0,0,0,0.03);
  margin-bottom: 20px;
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

.head__right {
  display: flex;
  align-items: center;
  gap: 16px;
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
  background: rgba(255, 255, 255, 0.85); /* Glass effect */
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.04);
  border: 1px solid rgba(255,255,255,0.6);
  backdrop-filter: blur(12px);
  transition: all 0.3s ease;
  position: relative;
  display: flex;
  flex-direction: column;
}

.card:hover {
  transform: translateY(-5px);
  box-shadow: 0 20px 40px rgba(0,0,0,0.08);
  background: rgba(255, 255, 255, 0.95);
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
  background: rgba(255, 255, 255, 0.8);
  padding: 8px;
  border-radius: 20px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  backdrop-filter: blur(8px);
  width: fit-content;
  border: 1px solid rgba(255,255,255,0.5);
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
  background: linear-gradient(120deg, rgba(255,255,255,0.9) 0%, rgba(240, 253, 249, 0.9) 100%);
  border-radius: 24px;
  padding: 48px;
  margin-bottom: 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 10px 30px rgba(16, 212, 166, 0.08);
  border: 1px solid rgba(255,255,255,0.8);
  backdrop-filter: blur(10px);
  position: relative;
  overflow: hidden;
}

.welcome-text h1 {
  font-size: 32px;
  font-weight: 800;
  color: #1a1e23;
  margin: 0 0 16px;
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
  gap: 20px;
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
  display: none;
}

.bg-blue { background: rgba(64, 158, 255, 0.1); color: #409eff; }
.bg-green { background: rgba(103, 194, 58, 0.1); color: #67c23a; }

/* 数据概览 */
.dashboard-overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.overview-card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.04);
  display: flex;
  align-items: center;
  gap: 20px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(255,255,255,0.6);
}

.overview-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.06);
}

.overview-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(180deg, #10d4a6, #409eff);
}

.overview-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.overview-content h3 {
  font-size: 14px;
  font-weight: 600;
  color: #666;
  margin: 0 0 8px;
}

.overview-value {
  font-size: 28px;
  font-weight: 800;
  color: #1a1e23;
  margin: 0 0 4px;
}

.overview-change {
  font-size: 12px;
  color: #10d4a6;
  margin: 0;
}

/* 最近活动 */
.recent-activities {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 24px;
  padding: 32px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.04);
  margin-bottom: 32px;
  border: 1px solid rgba(255,255,255,0.6);
}

.activity-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.activity-header h2 {
  font-size: 18px;
  font-weight: 700;
  color: #1a1e23;
  margin: 0;
}

.view-all-btn {
  background: #f5f7fa;
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.view-all-btn:hover {
  background: #e4e7ed;
  color: #1a1e23;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.activity-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  transition: all 0.2s;
  border: 1px solid #f0f0f0;
  justify-content: space-between;
}

.activity-content {
  flex: 1;
}

.activity-item:hover {
  background: #f9fafb;
  border-color: #e5e7eb;
}

.activity-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  margin-top: 2px;
}

.activity-content {
  flex: 1;
}

.activity-text {
  font-size: 14px;
  color: #333;
  margin: 0 0 4px;
  line-height: 1.4;
}

.activity-text strong {
  color: #1a1e23;
  font-weight: 600;
}

.activity-time {
  font-size: 12px;
  color: #999;
  margin: 0;
}

.activity-action {
  background: #10d4a6;
  color: #fff;
  border: none;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
  margin-top: 2px;
}

.activity-action:hover {
  background: #0cc096;
  transform: translateY(-1px);
}

.empty-activities {
  text-align: center;
  padding: 40px;
  color: #999;
  font-size: 14px;
}

/* 颜色类 */
.bg-purple { background: rgba(230, 183, 120, 0.18); color: #b7791f; }
.bg-orange { background: rgba(255, 161, 22, 0.1); color: #ff9f1c; }

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

/* 装饰元素 */
.decorative-elements {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

.decorative-circle {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(16, 212, 166, 0.1), transparent 70%);
  animation: float 6s ease-in-out infinite;
}

.circle-1 {
  width: 120px;
  height: 120px;
  top: 20%;
  right: 10%;
  animation-delay: 0s;
}

.circle-2 {
  width: 80px;
  height: 80px;
  top: 60%;
  left: 5%;
  animation-delay: 2s;
}

.circle-3 {
  width: 100px;
  height: 100px;
  bottom: 10%;
  right: 20%;
  animation-delay: 4s;
}

.decorative-shape {
  position: absolute;
  background: linear-gradient(45deg, rgba(255, 161, 22, 0.08), transparent);
  animation: rotate 12s linear infinite;
}

.shape-1 {
  width: 60px;
  height: 60px;
  top: 30%;
  left: 15%;
  clip-path: polygon(50% 0%, 100% 50%, 50% 100%, 0% 50%);
  animation-delay: 0s;
}

.shape-2 {
  width: 40px;
  height: 40px;
  bottom: 20%;
  left: 30%;
  clip-path: polygon(0% 0%, 100% 0%, 75% 100%, 25% 100%);
  animation-delay: 3s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-20px) scale(1.05);
  }
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 卡片装饰效果 */
.card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(90deg, #10d4a6, #409eff);
  border-radius: 16px 16px 0 0;
  opacity: 0;
  transition: opacity 0.3s;
}

.card:hover::before {
  opacity: 1;
}

/* 欢迎区域装饰 */
.welcome-section::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(16, 212, 166, 0.1), transparent 70%);
  border-radius: 50%;
  animation: pulse 4s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 0.5;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.8;
  }
}

/* 教师控制台装饰 */
.dashboard-header::after {
  content: '';
  position: absolute;
  bottom: -20px;
  right: -20px;
  width: 150px;
  height: 150px;
  background: radial-gradient(circle, rgba(64, 158, 255, 0.1), transparent 70%);
  border-radius: 50%;
  animation: float 5s ease-in-out infinite;
  animation-delay: 1s;
}

.dashboard-header {
  position: relative;
  overflow: hidden;
}

@media (max-width: 960px) {
  .dashboard-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;
  }
}
</style>
