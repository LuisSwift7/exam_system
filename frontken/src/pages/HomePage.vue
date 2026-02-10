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

function getStatusTag(status: number) {
  if (status === 1) return { type: 'success', text: '进行中' }
  if (status === 2) return { type: 'info', text: '未开始' }
  return { type: 'danger', text: '已结束' }
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
        <div class="main__title">
          <h2>待考列表</h2>
          <span class="main__sub">请按时参加。</span>
        </div>

        <div class="grid" v-loading="loading">
          <el-empty v-if="!loading && exams.length === 0" description="暂无考试" />
          
          <div v-for="item in exams" :key="item.id" class="card">
            <div class="card__status">
              <el-tag :type="getStatusTag(item.status).type as any" effect="dark" size="small" round>
                {{ getStatusTag(item.status).text }}
              </el-tag>
            </div>
            <div class="card__body">
              <h3 class="card__title">{{ item.title }}</h3>
              <p class="card__desc">{{ item.description }}</p>
              
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
                :disabled="item.status !== 1" 
                round
                @click="router.push(`/student/exam/${item.id}`)"
              >
                {{ item.status === 1 ? '进入考试' : (item.status === 2 ? '等待开始' : '已结束') }}
              </el-button>
            </div>
          </div>
        </div>
      </main>

      <!-- Teacher View -->
      <main class="main teacher-view" v-else-if="auth.me?.roleCode === 'TEACHER'">
        <div class="tabs">
          <div class="tab" :class="{ active: activeTab === 'student' }" @click="activeTab = 'student'">
            <Icon icon="iconoir:group" />
            学生管理
          </div>
          <div class="tab" :class="{ active: activeTab === 'question' }" @click="activeTab = 'question'">
            <Icon icon="iconoir:book-stack" />
            试题管理
          </div>
          <div class="tab" :class="{ active: activeTab === 'exam' }" @click="activeTab = 'exam'">
            <Icon icon="iconoir:page" />
            试卷管理
          </div>
        </div>

        <div class="tab-content">
          <StudentManage v-if="activeTab === 'student'" />
          <QuestionManage v-if="activeTab === 'question'" />
          <ExamManage v-if="activeTab === 'exam'" />
        </div>
      </main>

      <!-- Admin View -->
      <main class="main teacher-view" v-else-if="auth.me?.roleCode === 'ADMIN'">
        <div class="tabs">
          <div class="tab" :class="{ active: adminTab === 'teacher' }" @click="adminTab = 'teacher'">
            <Icon icon="iconoir:group" />
            教师管理
          </div>
        </div>

        <div class="tab-content">
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
</style>
