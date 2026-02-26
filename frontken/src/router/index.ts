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
    { path: '/student/exam/:id', name: 'exam-detail', component: ExamDetailPage },
    { path: '/student/exam/:id/taking', name: 'exam-taking', component: ExamTakingPage },
    { path: '/student/exam-result/:recordId', name: 'exam-result', component: ExamResultPage },
    { path: '/student/wrong-book', name: 'wrong-book', component: WrongBookPage },
    { path: '/student/classes', name: 'student-classes', component: () => import('../pages/student/ClassJoinPage.vue') },
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

  if (!auth.token) return { path: '/login' }
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
