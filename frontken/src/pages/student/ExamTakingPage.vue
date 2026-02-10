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

// Anti-cheat
const switchCount = ref(0)
const MAX_SWITCH_COUNT = 3
let lastViolationTime = 0

// Camera
const videoRef = ref<HTMLVideoElement | null>(null)
const mediaStream = ref<MediaStream | null>(null)

// Feedback
const feedbackDialogVisible = ref(false)
const feedbackContent = ref('')

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
      questionId: currentQ.value.id,
      examId: parseInt(examId as string),
      content: feedbackContent.value
    })
    ElMessage.success('反馈提交成功')
    feedbackDialogVisible.value = false
  } catch (e: any) {
    ElMessage.error(e?.message || '提交失败')
  }
}

const currentQ = computed(() => questions.value[currentIdx.value])

async function initCamera() {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ 
      video: { 
        width: 320,
        height: 240,
        facingMode: 'user'
      }, 
      audio: false 
    })
    mediaStream.value = stream
    if (videoRef.value) {
      videoRef.value.srcObject = stream
    }
  } catch (err) {
    console.error('Camera Error:', err)
    ElMessage.warning('无法访问摄像头，请检查设备权限')
  }
}

function handleViolation() {
  if (loading.value) return // Avoid re-trigger during submit
  
  const now = Date.now()
  if (now - lastViolationTime < 1000) return // Debounce 1s
  lastViolationTime = now
  
  switchCount.value++
  
  if (switchCount.value > MAX_SWITCH_COUNT) {
    // Exceeded limit
    ElMessage.error('您已超过切屏限制次数，系统将自动交卷！')
    submitExam(true)
  } else {
    // Warning
    ElMessageBox.alert(
      `检测到您进行了切屏操作！当前已切屏 ${switchCount.value} 次，累计 ${MAX_SWITCH_COUNT + 1} 次将被强制交卷。`,
      '切屏警告',
      {
        confirmButtonText: '我知道了',
        type: 'warning',
        closeOnClickModal: false,
        closeOnPressEscape: false,
        showClose: false
      }
    )
  }
}

function onVisibilityChange() {
  if (document.hidden) {
    handleViolation()
  }
}

function onBlur() {
  // Only trigger if document is also hidden or just rely on blur?
  // Blur is strict (Alt+Tab triggers it). 
  // To avoid double counting with visibilityChange (which might happen depending on browser), 
  // we can debounce or just accept that they capture slightly different things.
  // Usually visibilityChange is enough for "Tab Switch", Blur is for "App Switch".
  // Let's use handleViolation directly.
  handleViolation()
}

async function init() {
  try {
    // 1. Start/Get Record
    const startRes = await http.post(`/api/student/exam-taking/${examId}/start`)
    recordId.value = startRes.data.data.id
    
    // Check if completed
    if (startRes.data.data.status === 1) {
      ElMessage.info('您已完成该考试')
      router.replace(`/student/exam-result/${recordId.value}`)
      return
    }
    
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
    router.replace(`/student/exam-result/${recordId.value}`)
  } catch (e: any) {
    ElMessage.error(e?.message || '交卷失败')
    loading.value = false
  }
}

onMounted(() => {
  init()
  initCamera()
  document.addEventListener('visibilitychange', onVisibilityChange)
  window.addEventListener('blur', onBlur)
})

onUnmounted(() => {
  if (timer.value) clearInterval(timer.value)
  if (mediaStream.value) {
    mediaStream.value.getTracks().forEach(track => track.stop())
  }
  document.removeEventListener('visibilitychange', onVisibilityChange)
  window.removeEventListener('blur', onBlur)
})
</script>

<template>
  <div class="page" v-loading="loading">
    <div class="page__bg" />
    
    <div class="layout" v-if="!loading">
      <!-- Left: Navigation -->
      <aside class="side">
        <!-- Camera View -->
        <div class="side__card camera-card">
          <div class="camera-box">
            <video ref="videoRef" autoplay playsinline muted></video>
            <div class="rec-badge">
              <span class="dot-blink"></span> REC
            </div>
          </div>
        </div>

        <div class="side__card">
          <div class="side__head">
            <h3>答题卡</h3>
            <div class="timer">
              <Icon icon="iconoir:timer" />
              <span>{{ formatTime }}</span>
            </div>
          </div>

          <div class="monitor-info">
            <Icon icon="iconoir:eye-alt" />
            <span>切屏次数: {{ switchCount }} / {{ MAX_SWITCH_COUNT + 1 }}</span>
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

        <!-- Instructions -->
        <div class="side__card">
          <div class="side__head">
            <h3>考试注意事项</h3>
          </div>
          <ul class="exam-tips">
            <li><Icon icon="iconoir:wifi" /> 保持网络畅通，避免中断</li>
            <li><Icon icon="iconoir:cancel" /> 禁止切屏，超过限制自动交卷</li>
            <li><Icon icon="iconoir:camera" /> 全程开启摄像头监控</li>
            <li><Icon icon="iconoir:timer" /> 倒计时结束将自动交卷</li>
            <li><Icon icon="iconoir:warning-circle" /> 诚信考试，严禁作弊</li>
          </ul>
        </div>
      </aside>

      <!-- Center: Question -->
      <main class="main">
        <el-empty v-if="!currentQ" description="暂无题目数据" />
        <div class="question-card" v-else>
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
            <el-button 
              size="small" 
              type="default" 
              plain 
              @click="openFeedbackDialog"
            >
              <Icon icon="iconoir:chat-bubble" />
              题目反馈
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
  align-items: stretch;
}

.side {
  width: 280px;
  display: flex;
  flex-direction: column;
}

.side__card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  margin-bottom: 24px;
}

.side__card:last-child {
  margin-bottom: 0;
}

.camera-card {
  padding: 0;
  overflow: hidden;
  background: #000;
  display: flex;
  justify-content: center;
  align-items: center;
}

.camera-box {
  width: 100%;
  aspect-ratio: 4/3;
  position: relative;
}

.camera-box video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scaleX(-1); /* Mirror effect */
}

.rec-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(255, 0, 0, 0.6);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  padding: 4px 8px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.dot-blink {
  width: 8px;
  height: 8px;
  background: #fff;
  border-radius: 50%;
  animation: blink 1s infinite;
}

@keyframes blink {
  0% { opacity: 1; }
  50% { opacity: 0.3; }
  100% { opacity: 1; }
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
  color: #1a1e23;
  font-weight: 700;
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

.monitor-info {
  margin-bottom: 20px;
  font-size: 13px;
  color: #e6a23c;
  background: #fdf6ec;
  padding: 8px 12px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
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

.exam-tips {
  margin: 0;
  padding: 0;
  list-style: none;
}

.exam-tips li {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #666;
  margin-bottom: 12px;
  line-height: 1.4;
}

.exam-tips li:last-child {
  margin-bottom: 0;
}

.exam-tips li svg {
  color: #10d4a6;
  font-size: 16px;
  flex-shrink: 0;
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.question-card {
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  min-height: 600px;
  display: flex;
  flex-direction: column;
  flex: 1;
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
