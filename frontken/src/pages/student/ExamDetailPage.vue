<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { http } from '../../api/http'
import { ElMessage } from 'element-plus'
import { Icon } from '@iconify/vue'

const route = useRoute()
const router = useRouter()
const examId = route.params.id

const loading = ref(false)
const exam = ref<any>(null)
const review = ref<any>(null)
const reviewLoading = ref(false)

async function fetchDetail() {
  loading.value = true
  try {
    const res = await http.get(`/api/student/exam-taking/${examId}/detail`)
    exam.value = res.data.data
  } catch (e: any) {
    ElMessage.error(e?.message || '获取考试详情失败')
  } finally {
    loading.value = false
  }
}

async function fetchReview() {
  reviewLoading.value = true
  try {
    const res = await http.get(`/api/student/reviews/exam/${examId}`)
    review.value = res.data.data
  } catch (e: any) {
    // 没有讲评时不显示错误
  } finally {
    reviewLoading.value = false
  }
}

async function startExam() {
  try {
    await http.post(`/api/student/exam-taking/${examId}/start`)
    router.replace(`/student/exam/${examId}/taking`)
  } catch (e: any) {
    ElMessage.error(e?.message || '开始考试失败')
  }
}

onMounted(async () => {
  await fetchDetail()
  await fetchReview()
})
</script>

<template>
  <div class="page">
    <div class="page__bg" />
    <div class="shell" v-loading="loading">
      <el-button link class="back-btn" @click="router.back()">
        <Icon icon="iconoir:arrow-left" />
        返回
      </el-button>

      <div class="card" v-if="exam">
        <div class="head">
          <h1 class="title">{{ exam.title }}</h1>
          <div class="meta">
            <span class="tag">
              <Icon icon="iconoir:timer" />
              {{ exam.duration }} 分钟
            </span>
            <span class="tag">
              <Icon icon="iconoir:calendar" />
              {{ exam.startTime?.replace('T', ' ').slice(0, 16) }} 开始
            </span>
          </div>
        </div>

        <div class="body">
          <div class="section">
            <h3>考试说明</h3>
            <p>{{ exam.description || '暂无说明' }}</p>
          </div>
          
          <div class="section">
            <h3>考前须知</h3>
            <ul class="rules">
              <li>请检查网络环境是否稳定。</li>
              <li>考试期间禁止切换页面，否则可能被记录异常。</li>
              <li>请在规定时间内完成作答，倒计时结束后将自动提交。</li>
            </ul>
          </div>
          
          <!-- 讲评部分 -->
          <div class="section" v-if="review" v-loading="reviewLoading">
            <h3>考试讲评</h3>
            <div class="review-card">
              <h4 class="review-title">{{ review.title }}</h4>
              <div class="review-content">{{ review.content }}</div>
              <div class="review-time">发布时间：{{ new Date(review.createdAt).toLocaleString() }}</div>
            </div>
          </div>
        </div>

        <div class="foot">
          <el-button class="start-btn" type="primary" size="large" round @click="startExam">
            我已阅读，开始考试
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  position: relative;
  background: #f6f8fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.page__bg {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 50% 50%, rgba(16, 212, 166, 0.05), transparent 60%);
  pointer-events: none;
}

.shell {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 600px;
  padding: 24px;
}

.back-btn {
  margin-bottom: 16px;
  color: #666;
}

.card {
  background: #fff;
  border-radius: 24px;
  padding: 40px;
  box-shadow: 0 20px 40px rgba(0,0,0,0.04);
  text-align: center;
}

.title {
  font-size: 24px;
  font-weight: 800;
  color: #1a1e23;
  margin: 0 0 16px;
}

.meta {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-bottom: 32px;
}

.tag {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #666;
  background: #f5f7fa;
  padding: 6px 12px;
  border-radius: 8px;
}

.body {
  text-align: left;
  margin-bottom: 40px;
}

.section {
  margin-bottom: 24px;
}

.section h3 {
  font-size: 15px;
  font-weight: 700;
  color: #1a1e23;
  margin: 0 0 12px;
}

.section p {
  font-size: 14px;
  color: #555;
  line-height: 1.6;
  margin: 0;
}

.rules {
  margin: 0;
  padding-left: 20px;
  font-size: 14px;
  color: #555;
  line-height: 1.8;
}

/* 讲评样式 */
.review-card {
  background: #f9fafb;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #e5e7eb;
}

.review-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1e23;
  margin: 0 0 12px;
}

.review-content {
  font-size: 14px;
  color: #555;
  line-height: 1.6;
  margin: 0 0 12px;
  white-space: pre-wrap;
}

.review-time {
  font-size: 12px;
  color: #999;
  text-align: right;
}

.start-btn {
  width: 100%;
  font-weight: 700;
  height: 48px;
  font-size: 16px;
}
</style>
