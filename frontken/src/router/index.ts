import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import HomePage from '../pages/HomePage.vue'
import LoginPage from '../pages/LoginPage.vue'
import ExamDetailPage from '../pages/student/ExamDetailPage.vue'
import ExamTakingPage from '../pages/student/ExamTakingPage.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginPage },
    { path: '/', name: 'home', component: HomePage },
    { path: '/student/exam/:id', name: 'exam-detail', component: ExamDetailPage },
    { path: '/student/exam/:id/taking', name: 'exam-taking', component: ExamTakingPage },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.path === '/login') return true
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
