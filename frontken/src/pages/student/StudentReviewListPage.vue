<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { ElMessage } from 'element-plus'
import { http } from '../../api/http'

const router = useRouter()
const loading = ref(false)
const reviews = ref<any[]>([])
const detailVisible = ref(false)
const currentReview = ref<any | null>(null)

const publishedCount = computed(() => reviews.value.length)
const questionReviewCount = computed(() =>
  reviews.value.reduce((total, item) => total + (item.questionReviews?.length || 0), 0)
)

async function fetchReviews() {
  loading.value = true
  try {
    const res = await http.get('/api/student/reviews')
    reviews.value = res.data.data || []
  } catch (e: any) {
    ElMessage.error(e?.message || '获取讲评列表失败')
  } finally {
    loading.value = false
  }
}

function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString() : '-'
}

function getSummary(item: any) {
  const summary = item.summary || item.content || ''
  if (!summary) return '这份讲评以逐题分析为主，可以点开弹窗查看完整内容。'
  return summary.length > 96 ? `${summary.slice(0, 96)}...` : summary
}

function openReview(item: any) {
  currentReview.value = item
  detailVisible.value = true
}

onMounted(() => {
  fetchReviews()
})
</script>

<template>
  <div class="page">
    <div class="page__bg" />

    <div class="shell">
      <div class="topbar">
        <el-button link class="back-btn" @click="router.push('/dashboard')">
          <Icon icon="iconoir:arrow-left" />
          返回首页
        </el-button>
      </div>

      <section class="hero">
        <div class="hero-copy">
          <p class="hero-eyebrow">Student Reviews</p>
          <h1>我的讲评</h1>
          <p class="hero-text">这里集中查看老师已经发布的考试讲评。</p>
        </div>

        <div class="hero-stats">
          <div class="hero-stat">
            <span>已发布讲评</span>
            <strong>{{ publishedCount }}</strong>
          </div>
          <div class="hero-stat">
            <span>逐题讲评</span>
            <strong>{{ questionReviewCount }}</strong>
          </div>
        </div>
      </section>

      <div class="review-list" v-loading="loading">
        <el-empty v-if="!loading && !reviews.length" description="暂时还没有可查看的讲评" />

        <article v-for="item in reviews" :key="item.id" class="review-card">
          <div class="review-head">
            <div>
              <div class="review-meta">
                <span class="review-tag">{{ item.examTitle || '未命名试卷' }}</span>
                <span class="review-time">{{ formatTime(item.createdAt) }}</span>
              </div>
              <h3>{{ item.title }}</h3>
            </div>

            <el-button type="primary" round @click="openReview(item)">
              <Icon icon="iconoir:open-in-window" class="btn-icon" />
              查看完整讲评
            </el-button>
          </div>

          <p class="review-summary">{{ getSummary(item) }}</p>

          <div class="review-foot">
            <span>总评 {{ item.summary ? '1' : '0' }} 段</span>
            <span>逐题讲评 {{ item.questionReviews?.length || 0 }} 条</span>
          </div>
        </article>
      </div>
    </div>

    <el-dialog
      v-model="detailVisible"
      width="920px"
      append-to-body
      destroy-on-close
      class="review-detail-dialog"
    >
      <template #header>
        <div v-if="currentReview" class="dialog-head">
          <div>
            <p class="dialog-eyebrow">考试讲评</p>
            <h3>{{ currentReview.title }}</h3>
          </div>
          <div class="dialog-meta">
            <span>{{ currentReview.examTitle || '未命名试卷' }}</span>
            <span>{{ formatTime(currentReview.createdAt) }}</span>
          </div>
        </div>
      </template>

      <div v-if="currentReview" class="dialog-body">
        <section class="detail-section">
          <div class="section-head">
            <h4>总评</h4>
          </div>
          <div class="detail-summary">
            {{ currentReview.summary || currentReview.content || '这份讲评暂时没有填写总评。' }}
          </div>
        </section>

        <section class="detail-section">
          <div class="section-head">
            <h4>逐题讲评</h4>
            <span>{{ currentReview.questionReviews?.length || 0 }} 条</span>
          </div>

          <el-empty
            v-if="!currentReview.questionReviews?.length"
            description="这份讲评暂时没有逐题内容"
          />

          <div v-else class="question-review-list">
            <article
              v-for="item in currentReview.questionReviews"
              :key="item.questionId"
              class="question-review-card"
            >
              <div class="question-review-head">
                <span class="question-chip">第 {{ item.questionNo || '?' }} 题</span>
              </div>
              <div class="question-stem">{{ item.questionContent || '题干暂缺' }}</div>
              <div class="question-comment">{{ item.content }}</div>
            </article>
          </div>
        </section>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
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
  background:
    radial-gradient(circle at 10% 14%, rgba(255, 255, 255, 0.92), transparent 28%),
    radial-gradient(circle at 92% 10%, rgba(16, 212, 166, 0.05), transparent 24%),
    linear-gradient(180deg, #fbfcfd 0%, #f6f8fa 100%);
  opacity: 0.95;
}

.shell {
  position: relative;
  z-index: 1;
  max-width: 1100px;
  margin: 0 auto;
  padding: 28px 24px 56px;
}

.topbar {
  margin-bottom: 18px;
}

.back-btn {
  color: #6b7280;
}

.hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  padding: 30px;
  border: 1px solid #e5e7eb;
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(250, 251, 252, 0.98));
  box-shadow: 0 18px 48px rgba(148, 163, 184, 0.08);
}

.hero-eyebrow,
.dialog-eyebrow {
  margin: 0 0 8px;
  color: #b45309;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero h1,
.dialog-head h3 {
  margin: 0;
  color: #1f2937;
  font-size: 34px;
  font-weight: 800;
}

.hero-text {
  margin: 10px 0 0;
  color: #5b6472;
  font-size: 15px;
}

.hero-stats {
  display: flex;
  gap: 14px;
}

.hero-stat {
  min-width: 140px;
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.96);
}

.hero-stat span {
  display: block;
  margin-bottom: 8px;
  color: #8b6b45;
  font-size: 12px;
}

.hero-stat strong {
  color: #1f2937;
  font-size: 30px;
  font-weight: 800;
}

.review-list {
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-card {
  padding: 24px;
  border: 1px solid #ece6db;
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 12px 36px rgba(148, 163, 184, 0.08);
}

.review-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.review-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.review-tag,
.question-chip {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: #fff3e2;
  color: #9a5a16;
  font-size: 12px;
  font-weight: 700;
}

.review-time {
  color: #94a3b8;
  font-size: 12px;
}

.review-head h3 {
  margin: 0;
  color: #1f2937;
  font-size: 22px;
  font-weight: 700;
}

.btn-icon {
  margin-right: 6px;
}

.review-summary {
  margin: 16px 0 0;
  color: #475569;
  font-size: 15px;
  line-height: 1.8;
}

.review-foot {
  display: flex;
  gap: 16px;
  margin-top: 18px;
  color: #8a94a6;
  font-size: 13px;
}

.dialog-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding-right: 20px;
}

.dialog-head h3 {
  font-size: 28px;
}

.dialog-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #94a3b8;
  font-size: 13px;
  text-align: right;
}

.dialog-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.detail-section {
  padding: 22px;
  border: 1px solid #e5e7eb;
  border-radius: 22px;
  background: #fff;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.section-head h4 {
  margin: 0;
  color: #1f2937;
  font-size: 18px;
  font-weight: 700;
}

.section-head span {
  color: #94a3b8;
  font-size: 13px;
}

.detail-summary,
.question-stem,
.question-comment {
  color: #475569;
  font-size: 15px;
  line-height: 1.8;
  white-space: pre-wrap;
}

.question-review-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.question-review-card {
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: #fff;
}

.question-review-head {
  margin-bottom: 12px;
}

.question-stem {
  color: #1f2937;
}

.question-comment {
  margin-top: 12px;
}

@media (max-width: 760px) {
  .hero,
  .review-head,
  .dialog-head {
    flex-direction: column;
  }

  .hero-stats {
    width: 100%;
  }

  .hero-stat {
    flex: 1;
    min-width: 0;
  }

  .dialog-meta {
    text-align: left;
  }
}
</style>
