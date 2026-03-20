<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { http } from '../../api/http'
import { ElMessage } from 'element-plus'
import { Icon } from '@iconify/vue'

const loading = ref(false)
const list = ref<any[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const status = ref<number | null>(null)

const replyDialogVisible = ref(false)
const currentFeedback = ref<any>(null)
const replyContent = ref('')
const publishAsReview = ref(false)
const reviewTitle = ref('')

const questionDialogVisible = ref(false)
const questionFormRef = ref<any>(null)
const stems = ref<any[]>([])
const stemDialogVisible = ref(false)
const stemForm = ref<any>({
  id: undefined,
  content: '',
  contentImageUrl: ''
})

const categories = ['常识判断', '言语理解', '数量关系', '判断推理', '资料分析']

const questionForm = ref<any>(createDefaultQuestionForm())

const questionRules = {
  category: [{ required: true, message: '请选择题库', trigger: 'change' }],
  content: [{ required: true, message: '请输入题干', trigger: 'blur' }],
  answer: [{ required: true, message: '请选择答案', trigger: 'change' }]
}

function createDefaultQuestionForm() {
  return {
    id: undefined,
    content: '',
    contentImageUrl: '',
    type: 1,
    category: '常识判断',
    options: [
      { key: 'A', value: '', imageUrl: '' },
      { key: 'B', value: '', imageUrl: '' },
      { key: 'C', value: '', imageUrl: '' },
      { key: 'D', value: '', imageUrl: '' }
    ],
    answer: '',
    analysis: '',
    difficulty: 3,
    stemId: undefined
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await http.get('/api/teacher/feedback/list', {
      params: {
        page: page.value,
        size: size.value,
        status: status.value
      }
    })
    list.value = res.data.data.records
    total.value = res.data.data.total
  } catch (e: any) {
    ElMessage.error(e?.message || '获取反馈失败')
  } finally {
    loading.value = false
  }
}

async function fetchStems() {
  try {
    const res = await http.get('/api/stems/batch', { params: { ids: [] } })
    stems.value = Object.values(res.data || {})
  } catch (e) {
    console.error('fetch stems failed', e)
  }
}

async function handleReply(item: any) {
  currentFeedback.value = item
  replyContent.value = item.replyContent || ''
  publishAsReview.value = false
  reviewTitle.value = `关于题目 #${item.questionId} 的讲评`

  try {
    const res = await http.get(`/api/teacher/feedback/review/${item.questionId}`)
    if (res.data.data?.title) {
      reviewTitle.value = res.data.data.title
    }
  } catch {
    // ignore
  }

  replyDialogVisible.value = true
}

async function submitReply() {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  try {
    await http.post('/api/teacher/feedback/reply', {
      id: currentFeedback.value.id,
      replyContent: replyContent.value
    })

    if (publishAsReview.value) {
      if (!reviewTitle.value.trim()) {
        ElMessage.warning('请输入讲评标题')
        return
      }

      await http.post('/api/teacher/feedback/review/save', {
        questionId: currentFeedback.value.questionId,
        title: reviewTitle.value,
        content: replyContent.value,
        publishStatus: 1
      })
      ElMessage.success('回复并发布成功')
    } else {
      ElMessage.success('回复成功')
    }

    replyDialogVisible.value = false
    fetchList()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

function openQuestionEditor(item: any) {
  if (!item?.question) {
    ElMessage.warning('当前题目不存在')
    return
  }

  questionForm.value = normalizeQuestionForEdit(item.question)
  questionDialogVisible.value = true
}

function normalizeQuestionForEdit(question: any) {
  const options = normalizeOptions(question.options)
  let answer: string | string[] = question.answer || ''

  if (question.type === 2) {
    answer = typeof answer === 'string' ? answer.split('') : Array.isArray(answer) ? answer : []
  } else if (Array.isArray(answer)) {
    answer = answer[0] || ''
  }

  return {
    id: question.id,
    content: question.content || '',
    contentImageUrl: question.contentImageUrl || '',
    type: question.type || 1,
    category: question.category || '常识判断',
    options: options.length
      ? options
      : [
          { key: 'A', value: '', imageUrl: '' },
          { key: 'B', value: '', imageUrl: '' },
          { key: 'C', value: '', imageUrl: '' },
          { key: 'D', value: '', imageUrl: '' }
        ],
    answer,
    analysis: question.analysis || '',
    difficulty: question.difficulty || 3,
    stemId: question.stemId || undefined
  }
}

async function submitQuestion() {
  if (!questionFormRef.value) return

  await questionFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return

    try {
      let answer = questionForm.value.answer
      if (questionForm.value.type === 2 && Array.isArray(answer)) {
        answer = answer.sort().join('')
      } else if (questionForm.value.type === 1 && Array.isArray(answer)) {
        answer = answer[0] || ''
      }

      const payload = {
        ...questionForm.value,
        answer,
        options: questionForm.value.options.map((option: any) => ({
          key: option.key,
          value: option.value,
          imageUrl: option.imageUrl
        })),
        stemId: questionForm.value.stemId || undefined
      }

      await http.put(`/api/teacher/questions/${questionForm.value.id}`, payload)
      syncQuestionToFeedbacks(payload)
      questionDialogVisible.value = false
      ElMessage.success('题目已更新')
    } catch (e: any) {
      ElMessage.error(e?.message || '题目更新失败')
    }
  })
}

function syncQuestionToFeedbacks(question: any) {
  const normalizedQuestion = {
    ...question,
    answer: Array.isArray(question.answer) ? question.answer.join('') : question.answer,
    options: normalizeOptions(question.options)
  }

  list.value = list.value.map((item) =>
    item.questionId === normalizedQuestion.id
      ? {
          ...item,
          question: { ...item.question, ...normalizedQuestion }
        }
      : item
  )

  if (currentFeedback.value?.questionId === normalizedQuestion.id) {
    currentFeedback.value = {
      ...currentFeedback.value,
      question: {
        ...currentFeedback.value.question,
        ...normalizedQuestion
      }
    }
  }
}

async function uploadImage(file: File, type: 'question' | 'option' | 'stem', optionIndex?: number) {
  const compressedFile = await compressImage(file)
  const formData = new FormData()
  formData.append('file', compressedFile)

  try {
    const res = await http.post('/api/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    const url = res.data.data.url

    if (type === 'question') {
      questionForm.value.contentImageUrl = url
    }
    if (type === 'option' && typeof optionIndex === 'number') {
      questionForm.value.options[optionIndex].imageUrl = url
    }
    if (type === 'stem') {
      stemForm.value.contentImageUrl = url
    }

    ElMessage.success('图片上传成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '图片上传失败')
  }
}

function compressImage(file: File): Promise<File> {
  return new Promise((resolve) => {
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')
    const img = new Image()

    img.onload = () => {
      let { width, height } = img
      const maxWidth = 800
      const maxHeight = 800

      if (width > maxWidth) {
        height = (height * maxWidth) / width
        width = maxWidth
      }

      if (height > maxHeight) {
        width = (width * maxHeight) / height
        height = maxHeight
      }

      canvas.width = width
      canvas.height = height
      ctx?.drawImage(img, 0, 0, width, height)

      canvas.toBlob(
        (blob) => {
          if (!blob) {
            resolve(file)
            return
          }

          if (blob.size > 200 * 1024) {
            canvas.toBlob(
              (smallerBlob) => {
                resolve(smallerBlob ? new File([smallerBlob], file.name, { type: file.type }) : file)
              },
              file.type,
              0.6
            )
            return
          }

          resolve(new File([blob], file.name, { type: file.type }))
        },
        file.type,
        0.8
      )
    }

    img.src = URL.createObjectURL(file)
  })
}

function handleAddStem() {
  stemForm.value = {
    id: undefined,
    content: '',
    contentImageUrl: ''
  }
  stemDialogVisible.value = true
}

function handleEditStem(row: any) {
  if (!row) return
  stemForm.value = {
    id: row.id,
    content: row.content || '',
    contentImageUrl: row.contentImageUrl || ''
  }
  stemDialogVisible.value = true
}

async function submitStem() {
  try {
    const payload = {
      ...stemForm.value,
      category: '资料分析'
    }

    if (stemForm.value.id) {
      await http.put(`/api/stems/${stemForm.value.id}`, payload)
      ElMessage.success('共享题干已更新')
    } else {
      const res = await http.post('/api/stems', payload)
      questionForm.value.stemId = res.data?.id
      ElMessage.success('共享题干已创建')
    }

    stemDialogVisible.value = false
    fetchStems()
  } catch (e: any) {
    ElMessage.error(e?.message || '共享题干保存失败')
  }
}

function handlePageChange(value: number) {
  page.value = value
  fetchList()
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ') : '-'
}

function formatQuestionType(type?: number) {
  const typeMap: Record<number, string> = {
    1: '单选题',
    2: '多选题',
    3: '判断题'
  }
  return type ? typeMap[type] || '题目' : '题目'
}

function normalizeOptions(options: any) {
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

onMounted(() => {
  fetchList()
  fetchStems()
})
</script>

<template>
  <div class="feedback-manage">
    <div class="panel-head">
      <div>
        <h3>题目反馈</h3>
        <p>看题。答疑。顺手改题。</p>
      </div>
      <div class="filter-bar">
        <el-radio-group v-model="status" @change="fetchList">
          <el-radio-button :label="null">全部</el-radio-button>
          <el-radio-button :label="0">待处理</el-radio-button>
          <el-radio-button :label="1">已回复</el-radio-button>
        </el-radio-group>
        <el-button class="refresh-btn" @click="fetchList">
          <Icon icon="iconoir:refresh" />
          刷新
        </el-button>
      </div>
    </div>

    <div class="table-wrap" v-loading="loading">
      <el-table :data="list" style="width: 100%">
        <el-table-column prop="studentId" label="学生" width="92" />
        <el-table-column prop="questionId" label="题号" width="92" />
        <el-table-column label="关联题目" min-width="320">
          <template #default="{ row }">
            <div v-if="row.question" class="question-brief">
              <div class="brief-meta">
                <span>{{ row.question.category || '未分类' }}</span>
                <span>{{ formatQuestionType(row.question.type) }}</span>
              </div>
              <div class="brief-content">{{ row.question.content || '题干缺失' }}</div>
            </div>
            <span v-else class="question-empty">题目不存在</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="反馈内容" min-width="220" show-overflow-tooltip />
        <el-table-column prop="createTime" label="提交时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'">
              {{ row.status === 1 ? '已回复' : '待处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              :type="row.status === 0 ? 'primary' : 'info'"
              size="small"
              link
              @click="handleReply(row)"
            >
              {{ row.status === 0 ? '回复' : '查看' }}
            </el-button>
            <el-button
              v-if="row.question"
              type="warning"
              size="small"
              link
              @click="openQuestionEditor(row)"
            >
              编辑题目
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="size"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <el-dialog
      v-model="replyDialogVisible"
      :title="currentFeedback?.status === 1 ? '查看反馈' : '回复反馈'"
      width="920px"
      append-to-body
    >
      <div class="dialog-content" v-if="currentFeedback">
        <section v-if="currentFeedback.question" class="question-card">
          <div class="card-top">
            <div class="card-title">
              <span class="question-no">题目 #{{ currentFeedback.questionId }}</span>
              <h4>{{ currentFeedback.question.content }}</h4>
            </div>
            <div class="card-actions">
              <span class="mini-tag">{{ currentFeedback.question.category || '未分类' }}</span>
              <span class="mini-tag">{{ formatQuestionType(currentFeedback.question.type) }}</span>
              <el-button size="small" type="warning" plain @click="openQuestionEditor(currentFeedback)">
                编辑题目
              </el-button>
            </div>
          </div>

          <img
            v-if="currentFeedback.question.contentImageUrl"
            :src="currentFeedback.question.contentImageUrl"
            alt="题目配图"
            class="question-image"
          />

          <div
            v-for="option in normalizeOptions(currentFeedback.question.options)"
            :key="option.key"
            class="option-row"
          >
            <span class="option-key">{{ option.key }}</span>
            <div class="option-body">
              <span>{{ option.value }}</span>
              <img v-if="option.imageUrl" :src="option.imageUrl" alt="选项配图" class="option-image" />
            </div>
          </div>

          <div class="analysis-box">
            <div class="analysis-line">
              <strong>正确答案</strong>
              <span>{{ currentFeedback.question.answer || '暂无' }}</span>
            </div>
            <div class="analysis-text">{{ currentFeedback.question.analysis || '暂无解析' }}</div>
          </div>
        </section>

        <section class="message-card">
          <label>学生反馈</label>
          <div class="message-text">{{ currentFeedback.content }}</div>
        </section>

        <section class="message-card">
          <label>教师回复</label>
          <el-input
            v-if="currentFeedback.status === 0"
            v-model="replyContent"
            type="textarea"
            :rows="5"
            placeholder="把思路讲清。把误区点透。"
          />
          <div v-else class="message-text reply-text">
            {{ currentFeedback.replyContent }}
          </div>

          <div v-if="currentFeedback.status === 0" class="publish-box">
            <el-checkbox v-model="publishAsReview">同步发布讲评</el-checkbox>
            <el-input
              v-if="publishAsReview"
              v-model="reviewTitle"
              placeholder="讲评标题"
            />
          </div>
        </section>
      </div>

      <template #footer v-if="currentFeedback?.status === 0">
        <span class="dialog-footer">
          <el-button @click="replyDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitReply">提交回复</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="questionDialogVisible"
      title="编辑关联题目"
      width="760px"
      append-to-body
    >
      <el-form ref="questionFormRef" :model="questionForm" :rules="questionRules" label-width="88px">
        <div class="edit-grid">
          <el-form-item label="题库" prop="category">
            <el-select v-model="questionForm.category" placeholder="请选择题库">
              <el-option v-for="category in categories" :key="category" :label="category" :value="category" />
            </el-select>
          </el-form-item>

          <el-form-item label="类型" prop="type">
            <el-radio-group v-model="questionForm.type" @change="questionForm.answer = questionForm.type === 2 ? [] : ''">
              <el-radio :label="1">单选题</el-radio>
              <el-radio :label="2">多选题</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>

        <el-form-item v-if="questionForm.category === '资料分析'" label="共享题干">
          <div class="stem-row">
            <el-select v-model="questionForm.stemId" placeholder="请选择共享题干" style="width: 100%">
              <el-option label="不关联共享题干" :value="undefined" />
              <el-option
                v-for="stem in stems"
                :key="stem.id"
                :label="stem.content?.slice(0, 30) + (stem.content?.length > 30 ? '...' : '')"
                :value="stem.id"
              />
            </el-select>
            <div class="stem-actions">
              <el-button size="small" type="primary" @click="handleAddStem">新建题干</el-button>
              <el-button
                v-if="questionForm.stemId"
                size="small"
                @click="handleEditStem(stems.find((item: any) => item.id === questionForm.stemId))"
              >
                编辑题干
              </el-button>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="题干" prop="content">
          <el-input v-model="questionForm.content" type="textarea" :rows="4" placeholder="修改题干" />
          <div class="image-toolbox">
            <el-upload
              action=""
              :http-request="(file) => uploadImage(file.file, 'question')"
              :show-file-list="false"
              accept="image/*"
            >
              <el-button size="small" type="primary">上传题干图</el-button>
            </el-upload>
            <el-button v-if="questionForm.contentImageUrl" size="small" type="danger" @click="questionForm.contentImageUrl = ''">
              删除图片
            </el-button>
          </div>
          <img v-if="questionForm.contentImageUrl" :src="questionForm.contentImageUrl" alt="题干配图" class="preview-image" />
        </el-form-item>

        <el-form-item label="难度" prop="difficulty">
          <el-rate v-model="questionForm.difficulty" />
        </el-form-item>

        <el-divider content-position="left">选项设置</el-divider>

        <div v-for="(option, index) in questionForm.options" :key="option.key" class="option-edit-row">
          <span class="option-label">{{ option.key }}</span>
          <el-input v-model="option.value" placeholder="请输入选项内容" class="option-input" />
          <div class="option-actions">
            <el-upload
              action=""
              :http-request="(file) => uploadImage(file.file, 'option', index)"
              :show-file-list="false"
              accept="image/*"
            >
              <el-button size="small" type="primary">选项图</el-button>
            </el-upload>
            <el-button v-if="option.imageUrl" size="small" type="danger" @click="option.imageUrl = ''">删图</el-button>
          </div>
        </div>

        <div class="option-preview-list">
          <img
            v-for="option in questionForm.options.filter((item: any) => item.imageUrl)"
            :key="option.key"
            :src="option.imageUrl"
            :alt="`${option.key} 选项图`"
            class="option-preview-image"
          />
        </div>

        <el-form-item label="答案" prop="answer">
          <el-select v-model="questionForm.answer" :multiple="questionForm.type === 2" placeholder="请选择答案">
            <el-option v-for="option in questionForm.options" :key="option.key" :label="option.key" :value="option.key" />
          </el-select>
        </el-form-item>

        <el-form-item label="解析" prop="analysis">
          <el-input v-model="questionForm.analysis" type="textarea" :rows="4" placeholder="补充解析" />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="questionDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitQuestion">保存题目</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="stemDialogVisible"
      :title="stemForm.id ? '编辑共享题干' : '新建共享题干'"
      width="640px"
      append-to-body
    >
      <el-form :model="stemForm" label-width="88px">
        <el-form-item label="题干内容">
          <el-input v-model="stemForm.content" type="textarea" :rows="5" placeholder="请输入共享题干" />
        </el-form-item>
        <el-form-item label="题干图片">
          <div class="image-toolbox">
            <el-upload
              action=""
              :http-request="(file) => uploadImage(file.file, 'stem')"
              :show-file-list="false"
              accept="image/*"
            >
              <el-button size="small" type="primary">上传图片</el-button>
            </el-upload>
            <el-button v-if="stemForm.contentImageUrl" size="small" type="danger" @click="stemForm.contentImageUrl = ''">
              删除图片
            </el-button>
          </div>
          <img v-if="stemForm.contentImageUrl" :src="stemForm.contentImageUrl" alt="共享题干图片" class="preview-image" />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="stemDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitStem">保存题干</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.feedback-manage {
  position: relative;
  padding: 24px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}

.panel-head,
.table-wrap {
  position: relative;
  z-index: 1;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.panel-head h3 {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  color: #2d2416;
}

.panel-head p {
  margin: 8px 0 0;
  color: #7a6b56;
  font-size: 13px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.refresh-btn {
  border-color: rgba(181, 137, 41, 0.28);
  color: #6b4f16;
  background: rgba(255, 248, 228, 0.78);
}

.table-wrap {
  padding: 16px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(210, 194, 155, 0.5);
  backdrop-filter: blur(10px);
}

.question-brief {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.brief-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #8b6d3b;
}

.brief-content {
  color: #2f2a22;
  line-height: 1.6;
}

.question-empty {
  color: #9ca3af;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.dialog-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.question-card,
.message-card {
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(180deg, #fffef9, #fff);
  border: 1px solid rgba(222, 214, 194, 0.9);
}

.card-top {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.card-title {
  flex: 1;
}

.question-no {
  display: inline-block;
  margin-bottom: 8px;
  color: #8f6a22;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.card-title h4 {
  margin: 0;
  color: #251f17;
  line-height: 1.7;
  font-size: 16px;
}

.card-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: flex-start;
}

.mini-tag {
  height: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  background: #f7f0d9;
  color: #7a5a16;
  font-size: 12px;
  font-weight: 700;
}

.question-image,
.option-image,
.preview-image,
.option-preview-image {
  display: block;
  max-width: 100%;
  border-radius: 14px;
  border: 1px solid #e7dfcf;
}

.question-image {
  margin-bottom: 14px;
  max-height: 300px;
  object-fit: contain;
}

.option-row {
  display: flex;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #fbf8f0;
  border: 1px solid #efe8d8;
}

.option-row + .option-row {
  margin-top: 10px;
}

.option-key {
  min-width: 24px;
  font-weight: 800;
  color: #7b5f28;
}

.option-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 10px;
  color: #3b3328;
  line-height: 1.6;
}

.option-image {
  max-height: 220px;
  object-fit: contain;
}

.analysis-box {
  margin-top: 14px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #f4f7ee;
  border: 1px solid #d9e1cb;
}

.analysis-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  color: #406146;
}

.analysis-text {
  color: #475569;
  line-height: 1.7;
}

.message-card label {
  display: block;
  margin-bottom: 10px;
  color: #4a3420;
  font-size: 13px;
  font-weight: 800;
}

.message-text {
  white-space: pre-wrap;
  line-height: 1.7;
  color: #344054;
  padding: 14px 16px;
  border-radius: 14px;
  background: #faf7f1;
}

.reply-text {
  background: #f1f8eb;
  color: #365314;
}

.publish-box {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed #ddd2bc;
}

.edit-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.stem-row {
  width: 100%;
}

.stem-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.image-toolbox {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.preview-image {
  margin-top: 12px;
  max-height: 260px;
  object-fit: contain;
}

.option-edit-row {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}

.option-label {
  font-weight: 800;
  color: #7b5f28;
}

.option-input {
  min-width: 0;
}

.option-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  white-space: nowrap;
}

.option-preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 4px 0 18px 88px;
}

.option-preview-image {
  width: 96px;
  height: 96px;
  object-fit: cover;
}

@media (max-width: 900px) {
  .panel-head,
  .card-top,
  .edit-grid {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .feedback-manage {
    padding: 18px;
  }

  .option-edit-row {
    grid-template-columns: 1fr;
  }

  .option-actions {
    justify-content: flex-start;
  }

  .option-preview-list {
    margin-left: 0;
  }
}
</style>
