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
const stems = ref<Record<number, any>>({})
const currentCategory = ref('')
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
const captureTimer = ref<number | null>(null)

// Feedback
const feedbackDialogVisible = ref(false)
const feedbackContent = ref('')

// Local cache
const cachedAnswers = ref<Record<number, string | string[]>>({})

// WebSocket
const ws = ref<any | null>(null)

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

// 分组题目，将具有相同stemId的题目放在一起
const groupedQuestionsByStem = computed(() => {
  const groups: any[] = []
  const stemMap: Record<number, any[]> = {}
  
  // 首先按stemId分组
  questions.value.forEach((q: any) => {
    if (q.stemId) {
      if (!stemMap[q.stemId]) {
        stemMap[q.stemId] = []
      }
      stemMap[q.stemId].push(q)
    } else {
      // 没有stemId的题目单独一组
      groups.push([q])
    }
  })
  
  // 将stemId分组添加到groups中
  Object.values(stemMap).forEach(group => {
    groups.push(group)
  })
  
  return groups
})

const currentGroup = computed(() => groupedQuestionsByStem.value[currentIdx.value])
const currentQ = computed(() => {
  // 为了保持兼容性，返回当前组的第一个题目
  return currentGroup.value ? currentGroup.value[0] : null
})

const groupedQuestions = computed(() => {
  const groups: Record<string, any[]> = {}
  questions.value.forEach((q: any) => {
    const cat = q.category || '未分类'
    if (!groups[cat]) groups[cat] = []
    groups[cat].push(q)
  })
  return groups
})

const categoryList = computed(() => Object.keys(groupedQuestions.value))

function goToCategory(cat: string) {
  const qList = groupedQuestions.value[cat]
  if (qList && qList.length > 0) {
    // 找到包含该分类题目的第一个组
    const groupIndex = groupedQuestionsByStem.value.findIndex((group: any[]) => {
      return group.some((q: any) => q.category === cat)
    })
    if (groupIndex >= 0) currentIdx.value = groupIndex
  }
}

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
    startCaptureTimer()
  } catch (err) {
    console.error('Camera Error:', err)
    ElMessage.warning('无法访问摄像头，请检查设备权限')
  }
}

function captureImage() {
  if (!videoRef.value) return
  
  const canvas = document.createElement('canvas')
  canvas.width = 320
  canvas.height = 240
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  
  ctx.drawImage(videoRef.value, 0, 0, canvas.width, canvas.height)
  
  // 直接通过HTTP POST发送图片
  canvas.toBlob((blob) => {
    if (blob) {
      const formData = new FormData()
      formData.append('image', blob, `capture_${Date.now()}.png`)
      formData.append('recordId', recordId.value?.toString() || '')
      
      http.post('/api/student/exam-taking/capture', formData)
        .then(() => {
          console.log('Capture uploaded successfully')
        })
        .catch(err => {
          console.error('Capture upload failed:', err)
        })
    }
  })
}

function startCaptureTimer() {
  // 每3分钟抓拍一次
  captureTimer.value = window.setInterval(() => {
    captureImage()
  }, 3 * 60 * 1000)
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
    const record = startRes.data.data
    recordId.value = record.id
    
    // 初始化WebSocket连接
    initWebSocket()
    
    // Check if completed
    if (record.status === 1) {
      ElMessage.info('您已完成该考试')
      router.replace(`/student/exam-result/${recordId.value}`)
      return
    }
    
    // 2. Get Questions
    const qRes = await http.get(`/api/student/exam-taking/${examId}/questions`)
    questions.value = qRes.data.data.map((q: any) => {
      let opts = []
      if (Array.isArray(q.options)) {
        // 处理选项数据，支持包含图片的选项
        opts = q.options.map((opt: any) => {
          if (typeof opt === 'object' && opt !== null) {
            return opt
          } else if (typeof opt === 'string') {
            // 兼容旧格式
            const [key, ...rest] = opt.split('.')
            return { key: key.trim(), value: rest.join('.').trim() }
          }
          return opt
        })
      } else if (typeof q.options === 'string') {
        try {
          const parsed = JSON.parse(q.options)
          if (Array.isArray(parsed)) {
            opts = parsed.map((opt: any) => {
              if (typeof opt === 'object' && opt !== null) {
                return opt
              } else if (typeof opt === 'string') {
                const [key, ...rest] = opt.split('.')
                return { key: key.trim(), value: rest.join('.').trim() }
              }
              return opt
            })
          } else {
            opts = []
          }
        } catch {
          opts = []
        }
      }
      return { ...q, options: opts }
    })

    // 3. Get Stems for Data Analysis Questions
    const stemIds = [...new Set(questions.value.map((q: any) => q.stemId).filter((id: any) => id))]
    if (stemIds.length > 0) {
      try {
        const stemsRes = await http.get(`/api/stems/batch`, { params: { ids: stemIds } })
        stems.value = stemsRes.data
      } catch (e) {
        console.error('Failed to fetch stems:', e)
      }
    }

    // 4. Set Time
    if (record.remainingSeconds !== undefined) {
      timeLeft.value = record.remainingSeconds
    } else {
      const detailRes = await http.get(`/api/student/exam-taking/${examId}/detail`)
      timeLeft.value = detailRes.data.data.duration * 60
    }
    
    // 5. Load cached answers
    loadCachedAnswers()
    
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

async function selectAnswer(val: string, questionId?: number, questionType?: number) {
  const qId = questionId || currentQ.value?.id
  const qType = questionType || currentQ.value?.type
  
  if (!qId || !qType) return
  
  let finalAns: string | string[] = val
  if (qType === 2) {
    // Multi choice logic
    const current = (answers.value[qId] as string[]) || []
    if (current.includes(val)) {
      finalAns = current.filter(v => v !== val)
    } else {
      finalAns = [...current, val].sort()
    }
  }
  
  answers.value[qId] = finalAns
  
  // Auto save
  try {
    await http.post('/api/student/exam-taking/submit-answer', {
      recordId: recordId.value,
      questionId: qId,
      answer: Array.isArray(finalAns) ? finalAns.join('') : finalAns,
      isMarked: marks.value[qId] ? 1 : 0
    })
    // 上传成功后，从缓存中移除
    if (cachedAnswers.value[qId]) {
      delete cachedAnswers.value[qId]
      saveCachedAnswers()
    }
  } catch {
    // 网络失败，缓存到localStorage
    cachedAnswers.value[qId] = finalAns
    saveCachedAnswers()
  }
}

function saveCachedAnswers() {
  if (recordId.value) {
    localStorage.setItem(`exam_answers_${recordId.value}`, JSON.stringify(cachedAnswers.value))
  }
}

function loadCachedAnswers() {
  if (recordId.value) {
    // 加载答案缓存
    const cachedAnswersData = localStorage.getItem(`exam_answers_${recordId.value}`)
    if (cachedAnswersData) {
      try {
        cachedAnswers.value = JSON.parse(cachedAnswersData)
        // 合并缓存的答案到当前答案
        Object.assign(answers.value, cachedAnswers.value)
        // 尝试上传缓存的答案
        uploadCachedAnswers()
      } catch (e) {
        console.error('Failed to load cached answers:', e)
      }
    }
    
    // 加载标记状态缓存
    const cachedMarksData = localStorage.getItem(`exam_marks_${recordId.value}`)
    if (cachedMarksData) {
      try {
        const cachedMarks = JSON.parse(cachedMarksData)
        // 合并缓存的标记状态到当前标记
        Object.assign(marks.value, cachedMarks)
      } catch (e) {
        console.error('Failed to load cached marks:', e)
      }
    }
  }
}

async function uploadCachedAnswers() {
  for (const [qId, answer] of Object.entries(cachedAnswers.value)) {
    try {
      await http.post('/api/student/exam-taking/submit-answer', {
        recordId: recordId.value,
        questionId: Number(qId),
        answer: Array.isArray(answer) ? answer.join('') : answer,
        isMarked: marks.value[Number(qId)] ? 1 : 0
      })
      delete cachedAnswers.value[Number(qId)]
      saveCachedAnswers()
    } catch (e) {
      console.error('Failed to upload cached answer:', e)
    }
  }
}

function toggleMark() {
  if (!currentQ.value) return
  const qId = currentQ.value.id
  marks.value[qId] = !marks.value[qId]
  // Also sync mark status
  if (answers.value[qId]) selectAnswer(answers.value[qId])
}

function manualSave() {
  if (recordId.value) {
    // 保存当前所有答案到localStorage
    localStorage.setItem(`exam_answers_${recordId.value}`, JSON.stringify(answers.value))
    // 保存标记状态
    localStorage.setItem(`exam_marks_${recordId.value}`, JSON.stringify(marks.value))
    ElMessage.success('答案已临时保存')
    console.log('Manual save completed')
  }
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

function initWebSocket() {
  if (!recordId.value) {
    console.log('WebSocket initialization skipped: recordId is null')
    return
  }
  
  // 建立原生WebSocket连接
  const wsUrl = `ws://localhost:8080/api/student/exam-taking/${recordId.value}/ws`
  console.log('WebSocket connecting to:', wsUrl)
  
  try {
    // 创建原生WebSocket实例
    const sock = new WebSocket(wsUrl)
    
    sock.onopen = () => {
      console.log('WebSocket connected successfully')
      // 发送心跳
      setInterval(() => {
        if (sock.readyState === 1) { // 1 = OPEN
          console.log('Sending heartbeat')
          sock.send(JSON.stringify({ type: 'heartbeat' }))
        }
      }, 30000)
    }
    
    sock.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data)
        console.log('WebSocket message received:', message)
        
        // 处理服务器消息
        switch (message.type) {
          case 'serverMessage':
            ElMessage.info(message.content)
            break
          case 'captureRequest':
            captureImage()
            break
          case 'heartbeatResponse':
            console.log('Heartbeat response received')
            break
          case 'connectionSuccess':
            console.log('Connection success message:', message)
            break
          case 'testResponse':
            console.log('Test response received:', message)
            break
          default:
            break
        }
      } catch (e) {
        console.error('Failed to parse WebSocket message:', e)
        console.error('Raw message:', event.data)
      }
    }
    
    sock.onclose = (event) => {
      console.log('WebSocket disconnected:', event.code, event.reason)
    }
    
    sock.onerror = (error) => {
      console.error('WebSocket error:', error)
    }
    
    // 保存连接实例
    ws.value = sock
  } catch (e) {
    console.error('Failed to connect with WebSocket:', e)
  }
}

onUnmounted(() => {
  if (timer.value) clearInterval(timer.value)
  if (captureTimer.value) clearInterval(captureTimer.value)
  if (mediaStream.value) {
    mediaStream.value.getTracks().forEach(track => track.stop())
  }
  if (ws.value) {
    try {
      ws.value.close()
    } catch (e) {
      console.error('Failed to close WebSocket:', e)
    }
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
          
          <div class="category-tabs" v-if="categoryList.length > 1">
            <div 
              v-for="cat in categoryList" 
              :key="cat"
              class="category-tab"
              :class="{ active: currentQ?.category === cat || (!currentQ?.category && cat === '未分类') }"
              @click="goToCategory(cat)"
            >
              {{ cat }}
            </div>
          </div>
          
          <div class="grid">
            <div 
              v-for="(group, idx) in groupedQuestionsByStem" 
              :key="idx"
              class="dot"
              :class="{ 
                active: idx === currentIdx,
                filled: group.some(q => answers[q.id]),
                marked: group.some(q => marks[q.id])
              }"
              @click="currentIdx = idx"
            >
              {{ idx + 1 }}
              <div class="dot__mark" v-if="group.some(q => marks[q.id])" />
            </div>
          </div>

          <div class="side__foot">
            <div class="legend">
              <span class="dot filled small"></span> 已答
              <span class="dot marked small"></span> 标记
            </div>
            <div class="button-group">
            <el-button type="info" class="save-btn" @click="manualSave">临时保存</el-button>
            <el-button type="primary" class="submit-btn" @click="submitExam(false)">交卷</el-button>
          </div>
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
            <el-tag size="small" effect="dark" type="primary">{{ currentQ.category || '未分类' }}</el-tag>
            <el-tag size="small" effect="dark">{{ currentQ.type === 2 ? '多选题' : '单选题' }}</el-tag>
            <span class="q-idx">第 {{ currentIdx + 1 }} / {{ groupedQuestionsByStem.length }} 组</span>
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
            <!-- Shared Stem for Data Analysis Questions -->
            <div v-if="currentQ.stemId && stems[currentQ.stemId]" class="q-stem">
              <div class="stem-content">{{ stems[currentQ.stemId].content }}</div>
              <img v-if="stems[currentQ.stemId].contentImageUrl" :src="stems[currentQ.stemId].contentImageUrl" class="stem-image" alt="Stem Image" />
            </div>
            
            <!-- 显示当前组中的所有题目 -->
            <div v-for="(q, index) in currentGroup" :key="q.id" class="grouped-question">
              <div class="question-number">第 {{ questions.indexOf(q) + 1 }} 题</div>
              <div class="q-content">{{ q.content }}</div>
              <img v-if="q.contentImageUrl" :src="q.contentImageUrl" class="q-image" alt="Question Image" />
              
              <div class="options">
                <div 
                  v-for="opt in q.options" 
                  :key="opt.key || opt"
                  class="option"
                  :class="{ 
                    selected: q.type === 2 
                      ? (answers[q.id] as string[])?.includes(opt.key || opt.split('.')[0])
                      : answers[q.id] === (opt.key || opt.split('.')[0]) 
                  }"
                  @click="selectAnswer(opt.key || opt.split('.')[0], q.id, q.type)"
                >
                  <div class="opt-key">{{ opt.key || opt.split('.')[0] }}</div>
                  <div class="opt-val">
                    <span v-if="opt.value">{{ opt.value }}</span>
                    <span v-else-if="typeof opt === 'string'">
                      {{ opt.substring(opt.indexOf('.') + 1) }}
                    </span>
                    <img v-if="opt.imageUrl" :src="opt.imageUrl" class="option-image" />
                  </div>
                </div>
              </div>
              <div class="question-divider" v-if="index < currentGroup.length - 1"></div>
            </div>
          </div>

          <div class="q-foot">
            <el-button :disabled="currentIdx === 0" @click="currentIdx--">上一题</el-button>
            <el-button type="primary" :disabled="currentIdx === groupedQuestionsByStem.length - 1" @click="currentIdx++">下一题</el-button>
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

.category-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
}

.category-tab {
  padding: 6px 12px;
  font-size: 12px;
  border-radius: 16px;
  background: #f0f2f5;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
}

.category-tab:hover {
  background: #e6f4fe;
  color: #409eff;
}

.category-tab.active {
  background: #409eff;
  color: #fff;
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

.button-group {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.save-btn,
.submit-btn {
  flex: 1;
  box-sizing: border-box;
  margin: 0;
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
  margin-bottom: 16px;
  font-weight: 500;
}

.q-image {
  max-width: 100%;
  max-height: 300px;
  object-fit: contain;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  margin-bottom: 32px;
}

.q-stem {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
}

.grouped-question {
  margin-bottom: 24px;
  padding-bottom: 24px;
}

.question-number {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #1e293b;
}

.question-divider {
  height: 1px;
  background: #e2e8f0;
  margin: 24px 0;
}

.stem-content {
  font-size: 16px;
  line-height: 1.6;
  color: #475569;
  margin-bottom: 16px;
}

.stem-image {
  max-width: 100%;
  max-height: 250px;
  object-fit: contain;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  margin-bottom: 0;
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

.option-image {
  max-width: 100%;
  max-height: 200px;
  margin-top: 8px;
  border-radius: 8px;
  border: 1px solid #eee;
  object-fit: contain;
}

.q-foot {
  margin-top: auto;
  padding-top: 40px;
  display: flex;
  justify-content: space-between;
}

/* Button Group Styles */
.button-group {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.save-btn,
.submit-btn {
  flex: 1;
  box-sizing: border-box;
  margin: 0;
}
</style>
