import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import HomePage from '../pages/HomePage.vue'
import LandingPage from '../pages/LandingPage.vue'
import LoginPage from '../pages/LoginPage.vue'
import ExamDetailPage from '../pages/student/ExamDetailPage.vue'
import ExamTakingPage from '../pages/student/ExamTakingPage.vue'
import ExamResultPage from '../pages/student/ExamResultPage.vue'
import WrongBookPage from '../pages/student/WrongBookPage.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'landing', component: LandingPage },
    { path: '/login', name: 'login', component: LoginPage },
    { path: '/dashboard', name: 'dashboard', component: HomePage },
    // 学生端路由
    { path: '/student/exam/:id', name: 'exam-detail', component: ExamDetailPage },
    { path: '/student/exam/:id/taking', name: 'exam-taking', component: ExamTakingPage },
    { path: '/student/exam-result/:recordId', name: 'exam-result', component: ExamResultPage },
    { path: '/student/wrong-book', name: 'wrong-book', component: WrongBookPage },
    { path: '/student/classes', name: 'student-classes', component: () => import('../pages/student/ClassJoinPage.vue') },
    // 教师端路由
    { path: '/teacher/exams', name: 'teacher-exams', component: () => import('../pages/teacher/ExamManage.vue') },
    { path: '/teacher/classes', name: 'teacher-classes', component: () => import('../pages/teacher/ClassManage.vue') },
    { path: '/teacher/students', name: 'teacher-students', component: () => import('../pages/teacher/StudentManage.vue') },
    { path: '/teacher/questions', name: 'teacher-questions', component: () => import('../pages/teacher/QuestionManage.vue') },
    { path: '/teacher/notifications', name: 'teacher-notifications', component: () => import('../pages/teacher/NotificationManage.vue') },
    { path: '/teacher/reviews', name: 'teacher-reviews', component: () => import('../pages/teacher/ReviewManage.vue') },
    { path: '/teacher/feedback', name: 'teacher-feedback', component: () => import('../pages/teacher/FeedbackManage.vue') },
    // 管理员端路由
    { path: '/admin/users', name: 'admin-users', component: () => import('../pages/admin/UserManage.vue') },
    { path: '/admin/teachers', name: 'admin-teachers', component: () => import('../pages/admin/TeacherManage.vue') },
    { path: '/admin/logs', name: 'admin-logs', component: () => import('../pages/admin/LogManage.vue') },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  
  // Public pages
  if (to.path === '/' || to.path === '/login') {
    // Optional: If logged in and visiting public page, redirect to dashboard?
    // For now, let them visit landing page even if logged in.
    return true
  }

  if (!auth.accessToken) return { path: '/login' }
  if (!auth.me) {
    try {
      await auth.fetchMe()
    } catch {
      auth.logout()
      return { path: '/login' }
    }
  }
  return true
})
