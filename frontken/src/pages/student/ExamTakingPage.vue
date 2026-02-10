<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { http } from '../../api/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Icon } from '@iconify/vue'

const route = useRoute()
const router = useRouter()
const examId = route.params.id

const loading = ref(true)
const questions = ref<any[]>([])
const currentIdx = ref(0)
const answers = ref<Record<number, string | string[]>>({})
const marks = ref<Record<number, boolean>>({})
const recordId = ref<number | null>(null)
const timeLeft = ref(0)
const timer = ref<any>(null)

const currentQ = computed(() => questions.value[currentIdx.value])

async function init() {
  try {
    // 1. Start/Get Record
    const startRes = await http.post(`/api/student/exam-taking/${examId}/start`)
    recordId.value = startRes.data.data.id
    
    // 2. Get Questions
    const qRes = await http.get(`/api/student/exam-taking/${examId}/questions`)
    questions.value = qRes.data.data.map((q: any) => {
      let opts = []
      if (Array.isArray(q.options)) {
        opts = q.options
      } else if (typeof q.options === 'string') {
        try {
          opts = JSON.parse(q.options)
        } catch {
          opts = []
        }
      }
      return { ...q, options: opts }
    })

    // 3. Get Detail for Duration (Simple mock for now, ideally backend returns timeLeft)
    const detailRes = await http.get(`/api/student/exam-taking/${examId}/detail`)
    timeLeft.value = detailRes.data.data.duration * 60
    
    startTimer()
  } catch (e: any) {
    ElMessage.error(e?.message || '初始化考试失败')
    router.back()
  } finally {
    loading.value = false
  }
}

function startTimer() {
  timer.value = setInterval(() => {
    if (timeLeft.value > 0) {
      timeLeft.value--
    } else {
      submitExam(true)
    }
  }, 1000)
}

const formatTime = computed(() => {
  const m = Math.floor(timeLeft.value / 60)
  const s = timeLeft.value % 60
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
})

async function selectAnswer(val: string) {
  if (!currentQ.value) return
  
  let finalAns: string | string[] = val
  if (currentQ.value.type === 2) {
    // Multi choice logic
    const current = (answers.value[currentQ.value.id] as string[]) || []
    if (current.includes(val)) {
      finalAns = current.filter(v => v !== val)
    } else {
      finalAns = [...current, val].sort()
    }
  }
  
  answers.value[currentQ.value.id] = finalAns
  
  // Auto save
  try {
    await http.post('/api/student/exam-taking/submit-answer', {
      recordId: recordId.value,
      questionId: currentQ.value.id,
      answer: Array.isArray(finalAns) ? finalAns.join('') : finalAns,
      isMarked: marks.value[currentQ.value.id] ? 1 : 0
    })
  } catch {
    // Silent fail or retry logic
  }
}

function toggleMark() {
  if (!currentQ.value) return
  const qId = currentQ.value.id
  marks.value[qId] = !marks.value[qId]
  // Also sync mark status
  if (answers.value[qId]) selectAnswer(answers.value[qId])
}

async function submitExam(force = false) {
  if (!force) {
    try {
      await ElMessageBox.confirm('确认提交试卷吗？提交后无法修改。', '交卷确认', {
        confirmButtonText: '确认交卷',
        cancelButtonText: '继续答题',
        type: 'warning'
      })
    } catch {
      return
    }
  }

  loading.value = true
  try {
    await http.post('/api/student/exam-taking/submit-exam', { recordId: recordId.value })
    ElMessage.success('交卷成功')
    router.replace('/')
  } catch (e: any) {
    ElMessage.error(e?.message || '交卷失败')
    loading.value = false
  }
}

onMounted(() => {
  init()
})

onUnmounted(() => {
  if (timer.value) clearInterval(timer.value)
})
</script>

<template>
  <div class="page" v-loading="loading">
    <div class="page__bg" />
    
    <div class="layout" v-if="!loading">
      <!-- Left: Navigation -->
      <aside class="side">
        <div class="side__card">
          <div class="side__head">
            <h3>答题卡</h3>
            <div class="timer">
              <Icon icon="iconoir:timer" />
              <span>{{ formatTime }}</span>
            </div>
          </div>
          
          <div class="grid">
            <div 
              v-for="(q, idx) in questions" 
              :key="q.id"
              class="dot"
              :class="{ 
                active: idx === currentIdx,
                filled: answers[q.id],
                marked: marks[q.id]
              }"
              @click="currentIdx = idx"
            >
              {{ idx + 1 }}
              <div class="dot__mark" v-if="marks[q.id]" />
            </div>
          </div>

          <div class="side__foot">
            <div class="legend">
              <span class="dot filled small"></span> 已答
              <span class="dot marked small"></span> 标记
            </div>
            <el-button type="primary" class="submit-btn" @click="submitExam(false)">交卷</el-button>
          </div>
        </div>
      </aside>

      <!-- Center: Question -->
      <main class="main">
        <div class="question-card">
          <div class="q-head">
            <el-tag size="small" effect="dark">{{ currentQ.type === 2 ? '多选题' : '单选题' }}</el-tag>
            <span class="q-idx">第 {{ currentIdx + 1 }} / {{ questions.length }} 题</span>
            <el-button 
              size="small" 
              :type="marks[currentQ.id] ? 'warning' : 'default'" 
              plain 
              @click="toggleMark"
            >
              <Icon icon="iconoir:bookmark" />
              {{ marks[currentQ.id] ? '已标记' : '标记' }}
            </el-button>
          </div>

          <div class="q-body">
            <div class="q-content">{{ currentQ.content }}</div>
            
            <div class="options">
              <div 
                v-for="opt in currentQ.options" 
                :key="opt"
                class="option"
                :class="{ 
                  selected: currentQ.type === 2 
                    ? (answers[currentQ.id] as string[])?.includes(opt.split('.')[0])
                    : answers[currentQ.id] === opt.split('.')[0] 
                }"
                @click="selectAnswer(opt.split('.')[0])"
              >
                <div class="opt-key">{{ opt.split('.')[0] }}</div>
                <div class="opt-val">{{ opt.substring(2) }}</div>
              </div>
            </div>
          </div>

          <div class="q-foot">
            <el-button :disabled="currentIdx === 0" @click="currentIdx--">上一题</el-button>
            <el-button type="primary" :disabled="currentIdx === questions.length - 1" @click="currentIdx++">下一题</el-button>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f6f8fa;
  position: relative;
}

.page__bg {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 10% 10%, rgba(16, 212, 166, 0.05), transparent 40%);
  pointer-events: none;
}

.layout {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.side {
  width: 280px;
  position: sticky;
  top: 24px;
}

.side__card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}

.side__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.side__head h3 {
  font-size: 16px;
  margin: 0;
}

.timer {
  font-family: monospace;
  font-weight: 700;
  color: #f56c6c;
  background: #fef0f0;
  padding: 4px 8px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  margin-bottom: 24px;
}

.dot {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  position: relative;
  transition: all 0.2s;
}

.dot:hover {
  border-color: #10d4a6;
  color: #10d4a6;
}

.dot.active {
  border-color: #10d4a6;
  background: rgba(16, 212, 166, 0.1);
  color: #10d4a6;
  font-weight: 700;
}

.dot.filled {
  background: #10d4a6;
  border-color: #10d4a6;
  color: #fff;
}

.dot.marked {
  border-color: #e6a23c;
}

.dot__mark {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 8px;
  height: 8px;
  background: #e6a23c;
  border-radius: 50%;
  border: 1px solid #fff;
}

.side__foot {
  border-top: 1px solid #eee;
  padding-top: 16px;
}

.legend {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #888;
  margin-bottom: 16px;
  justify-content: center;
}

.dot.small {
  width: 12px;
  height: 12px;
  border-radius: 4px;
  display: inline-block;
  border: none;
}

.submit-btn {
  width: 100%;
}

.main {
  flex: 1;
}

.question-card {
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  min-height: 600px;
  display: flex;
  flex-direction: column;
}

.q-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f5f5f5;
}

.q-idx {
  font-size: 14px;
  color: #888;
  flex: 1;
}

.q-content {
  font-size: 18px;
  line-height: 1.6;
  color: #1a1e23;
  margin-bottom: 32px;
  font-weight: 500;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.option {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  border: 1px solid #eee;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.option:hover {
  background: #f9f9f9;
}

.option.selected {
  background: rgba(16, 212, 166, 0.08);
  border-color: #10d4a6;
}

.opt-key {
  width: 28px;
  height: 28px;
  background: #eee;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  color: #666;
  transition: all 0.2s;
}

.option.selected .opt-key {
  background: #10d4a6;
  color: #fff;
}

.opt-val {
  flex: 1;
  font-size: 15px;
  color: #333;
  line-height: 1.5;
  padding-top: 2px;
}

.q-foot {
  margin-top: auto;
  padding-top: 40px;
  display: flex;
  justify-content: space-between;
}
</style>
