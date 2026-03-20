<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { http } from '../../api/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Icon } from '@iconify/vue'

const loading = ref(false)
const list = ref<any[]>([])
const dialogVisible = ref(false)
const exams = ref<any[]>([])
const examsLoading = ref(false)
const examQuestions = ref<any[]>([])
const questionLoading = ref(false)

const form = ref<any>(createDefaultForm())

function createDefaultForm() {
  return {
    id: undefined,
    examId: undefined,
    title: '',
    summary: '',
    questionReviews: [] as Array<{
      questionId?: number
      content: string
    }>
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await http.get('/api/teacher/reviews')
    list.value = res.data.data || []
  } catch (e: any) {
    ElMessage.error(e?.message || '获取讲评失败')
  } finally {
    loading.value = false
  }
}

async function fetchExams() {
  examsLoading.value = true
  try {
    const res = await http.get('/api/teacher/exams')
    exams.value = res.data.data.list || []
  } catch (e: any) {
    ElMessage.error(e?.message || '获取试卷失败')
  } finally {
    examsLoading.value = false
  }
}

async function fetchExamQuestions(examId?: number | string) {
  examQuestions.value = []
  if (!examId) return

  questionLoading.value = true
  try {
    const res = await http.get(`/api/teacher/exams/${examId}/preview`)
    examQuestions.value = (res.data.data?.questions || []).map((question: any, index: number) => ({
      ...question,
      questionNo: index + 1
    }))
  } catch (e: any) {
    ElMessage.error(e?.message || '获取试卷题目失败')
  } finally {
    questionLoading.value = false
  }
}

async function handleAdd() {
  form.value = createDefaultForm()
  await fetchExams()
  dialogVisible.value = true
}

async function handleEdit(row: any) {
  await fetchExams()
  form.value = {
    id: row.id,
    examId: row.examId,
    title: row.title || '',
    summary: row.summary || row.content || '',
    questionReviews: (row.questionReviews || []).map((item: any) => ({
      questionId: item.questionId,
      content: item.content || ''
    }))
  }
  await fetchExamQuestions(row.examId)
  dialogVisible.value = true
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确认删除这条讲评吗？', '删除确认', {
      type: 'warning',
      confirmButtonText: '删除'
    })
    await http.delete(`/api/teacher/reviews/${id}`)
    ElMessage.success('删除成功')
    fetchList()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '删除失败')
    }
  }
}

async function handlePublish(row: any) {
  try {
    await ElMessageBox.confirm(`确认发布讲评《${row.title}》吗？发布后学生可见。`, '发布确认', {
      type: 'warning',
      confirmButtonText: '确认发布'
    })
    await http.post(`/api/teacher/reviews/${row.id}/publish`)
    ElMessage.success('发布成功')
    fetchList()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '发布失败')
    }
  }
}

function addQuestionReview() {
  form.value.questionReviews.push({
    questionId: undefined,
    content: ''
  })
}

function removeQuestionReview(index: number) {
  form.value.questionReviews.splice(index, 1)
}

async function handleExamChange(value: number | string) {
  form.value.questionReviews = form.value.questionReviews.map((item: any) => ({
    questionId: undefined,
    content: item.content
  }))
  await fetchExamQuestions(value)
}

function getQuestionLabel(question: any) {
  const content = question.content || '未命名题目'
  const shortContent = content.length > 42 ? `${content.slice(0, 42)}...` : content
  return `第 ${question.questionNo} 题 · ${shortContent}`
}

function getSelectedExamTitle(examId?: number | string) {
  return exams.value.find((exam) => exam.id === examId)?.title || '尚未选择试卷'
}

function getSelectedQuestion(questionId?: number) {
  return examQuestions.value.find((question) => question.id === questionId) || null
}

function canSubmit() {
  const hasSummary = !!form.value.summary?.trim()
  const hasQuestionReview = form.value.questionReviews.some((item: any) => item.questionId && item.content?.trim())
  return hasSummary || hasQuestionReview
}

async function submit() {
  if (!form.value.examId) {
    ElMessage.error('请选择试卷')
    return
  }
  if (!form.value.title?.trim()) {
    ElMessage.error('请输入讲评标题')
    return
  }
  if (!canSubmit()) {
    ElMessage.error('请填写总评或至少一条题目讲评')
    return
  }

  const questionReviews = form.value.questionReviews
    .filter((item: any) => item.questionId && item.content?.trim())
    .map((item: any) => ({
      questionId: item.questionId,
      content: item.content.trim()
    }))

  const payload = {
    examId: form.value.examId,
    title: form.value.title.trim(),
    summary: form.value.summary?.trim() || '',
    questionReviews
  }

  try {
    if (form.value.id) {
      await http.put(`/api/teacher/reviews/${form.value.id}`, payload)
    } else {
      await http.post('/api/teacher/reviews', payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchList()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  }
}

onMounted(async () => {
  await Promise.all([fetchList(), fetchExams()])
})
</script>

<template>
  <div class="panel">
    <div class="panel__head">
      <h3>讲评管理</h3>
      <el-button type="primary" @click="handleAdd">
        <Icon icon="iconoir:plus" />
        创建讲评
      </el-button>
    </div>

    <el-table :data="list" v-loading="loading" style="width: 100%">
      <el-table-column prop="title" label="讲评标题" min-width="220" show-overflow-tooltip />
      <el-table-column label="关联试卷" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.examTitle || exams.find((exam) => exam.id === row.examId)?.title || '未知试卷' }}
        </template>
      </el-table-column>
      <el-table-column label="讲评结构" width="180">
        <template #default="{ row }">
          <span class="meta-text">
            总评 {{ row.summary ? '1' : '0' }} 段 / 题目 {{ row.questionReviews?.length || 0 }} 条
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '已发布' : '未发布' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ row.createdAt ? new Date(row.createdAt).toLocaleString() : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 0"
            link
            type="success"
            size="small"
            @click="handlePublish(row)"
          >
            发布
          </el-button>
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      width="1080px"
      append-to-body
      class="review-dialog"
      destroy-on-close
    >
      <div class="review-editor">
        <div class="editor-hero">
          <div>
            <p class="hero-eyebrow">{{ form.id ? '编辑讲评' : '创建讲评' }}</p>
            <h4>{{ form.id ? '调整这份讲评' : '新建一份讲评' }}</h4>
            <p class="hero-text">先选试卷，再写总评，最后给题目补充逐题讲评。</p>
          </div>
          <div class="editor-hero-badge">
            <span>当前试卷</span>
            <strong>{{ getSelectedExamTitle(form.examId) }}</strong>
          </div>
        </div>

        <el-form :model="form" label-position="top" class="editor-form">
          <div class="editor-meta-grid">
            <section class="editor-block">
              <div class="block-head">
                <p class="block-eyebrow">Step 1</p>
                <h5>基础信息</h5>
              </div>

              <el-form-item label="关联试卷">
                <el-select
                  v-model="form.examId"
                  placeholder="请选择试卷"
                  style="width: 100%"
                  :loading="examsLoading"
                  @change="handleExamChange"
                >
                  <el-option
                    v-for="exam in exams"
                    :key="exam.id"
                    :label="exam.title"
                    :value="exam.id"
                  />
                </el-select>
              </el-form-item>

              <el-form-item label="讲评标题">
                <el-input v-model="form.title" placeholder="例如：三月阶段测验讲评" />
              </el-form-item>
            </section>

            <section class="editor-block summary-block">
              <div class="block-head">
                <p class="block-eyebrow">Step 2</p>
                <h5>整体讲评</h5>
              </div>

              <el-form-item label="总评">
                <el-input
                  v-model="form.summary"
                  type="textarea"
                  :rows="8"
                  resize="none"
                  placeholder="写这张试卷的共性问题、失分原因和复习建议。"
                />
              </el-form-item>
            </section>
          </div>

          <section class="editor-block question-block">
            <div class="section-head">
              <div class="block-head">
                <p class="block-eyebrow">Step 3</p>
                <h5>逐题讲评</h5>
                <p class="block-note">一条讲评里，可以补充同一张试卷的多道题。</p>
              </div>
              <el-button type="primary" plain @click="addQuestionReview" :disabled="!form.examId">
                <Icon icon="iconoir:plus" />
                添加题目讲评
              </el-button>
            </div>

            <div class="question-reviews" v-loading="questionLoading">
              <div v-if="!form.examId" class="inline-tip">
                先选择试卷，才能添加具体题目的讲评内容。
              </div>

              <el-empty
                v-else-if="!form.questionReviews.length"
                description="还没有添加题目讲评"
              />

              <div
                v-for="(item, index) in form.questionReviews"
                :key="index"
                class="question-review-card"
              >
                <div class="question-review-head">
                  <div class="question-review-title">
                    <span class="question-index">题目讲评 {{ index + 1 }}</span>
                    <span v-if="getSelectedQuestion(item.questionId)" class="question-chip">
                      第 {{ getSelectedQuestion(item.questionId)?.questionNo }} 题
                    </span>
                  </div>
                  <el-button link type="danger" size="small" @click="removeQuestionReview(index)">
                    删除
                  </el-button>
                </div>

                <div class="question-review-body">
                  <div class="question-review-select">
                    <label>选择题目</label>
                    <el-select
                      v-model="item.questionId"
                      placeholder="选择试卷中的一道题"
                      style="width: 100%"
                      filterable
                    >
                      <el-option
                        v-for="question in examQuestions"
                        :key="question.id"
                        :label="getQuestionLabel(question)"
                        :value="question.id"
                      />
                    </el-select>

                    <div class="question-preview">
                      {{
                        item.questionId
                          ? getSelectedQuestion(item.questionId)?.content || '未找到题目内容'
                          : '选中题目后，这里会展示题干内容，方便你对照写讲评。'
                      }}
                    </div>
                  </div>

                  <div class="question-review-editor">
                    <label>讲评内容</label>
                    <el-input
                      v-model="item.content"
                      type="textarea"
                      :rows="7"
                      resize="none"
                      placeholder="说明这道题为什么易错、正确思路是什么、该怎么复盘。"
                    />
                  </div>
                </div>
              </div>
            </div>
          </section>
        </el-form>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submit">保存讲评</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.panel {
  width: 100%;
  padding: 24px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.03);
}

.panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.panel__head h3 {
  margin: 0;
  color: #1f2937;
  font-size: 18px;
  font-weight: 700;
}

.meta-text {
  color: #64748b;
  font-size: 13px;
}

.review-editor {
  padding: 4px 6px 0;
}

.editor-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
  padding: 24px;
  border: 1px solid #eadfce;
  border-radius: 24px;
  background:
    radial-gradient(circle at top left, rgba(248, 226, 201, 0.75), transparent 42%),
    linear-gradient(135deg, #fffaf3 0%, #fff 58%, #f8f5ef 100%);
}

.hero-eyebrow,
.block-eyebrow {
  margin: 0 0 8px;
  color: #a16207;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.editor-hero h4,
.block-head h5 {
  margin: 0;
  color: #1f2937;
  font-size: 24px;
  font-weight: 700;
}

.hero-text {
  margin: 10px 0 0;
  color: #5b6472;
  line-height: 1.7;
}

.editor-hero-badge {
  min-width: 220px;
  padding: 16px 18px;
  border: 1px solid rgba(191, 124, 63, 0.2);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.editor-hero-badge span {
  display: block;
  margin-bottom: 6px;
  color: #9a6a34;
  font-size: 12px;
}

.editor-hero-badge strong {
  color: #1f2937;
  font-size: 16px;
  line-height: 1.5;
}

.editor-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.editor-meta-grid {
  display: grid;
  grid-template-columns: minmax(320px, 1.05fr) minmax(360px, 1fr);
  gap: 18px;
}

.editor-block {
  padding: 22px;
  background: #fff;
  border: 1px solid #ece6db;
  border-radius: 22px;
  box-shadow: 0 14px 40px rgba(148, 163, 184, 0.08);
}

.block-head {
  margin-bottom: 16px;
}

.block-head h5 {
  font-size: 18px;
}

.block-note {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 13px;
}

.summary-block :deep(.el-textarea__inner) {
  min-height: 210px;
}

.question-block {
  padding-bottom: 18px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.question-reviews {
  min-height: 180px;
}

.inline-tip {
  padding: 16px 18px;
  border: 1px dashed #d8c7af;
  border-radius: 16px;
  background: #fffaf2;
  color: #8b6b45;
  font-size: 13px;
}

.question-review-card {
  padding: 18px;
  border: 1px solid #e7dfd3;
  border-radius: 20px;
  background:
    linear-gradient(180deg, rgba(255, 250, 245, 0.95), rgba(255, 255, 255, 0.98));
}

.question-review-card + .question-review-card {
  margin-top: 14px;
}

.question-review-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.question-review-title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.question-index {
  color: #1f2937;
  font-size: 15px;
  font-weight: 700;
}

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

.question-review-body {
  display: grid;
  grid-template-columns: minmax(280px, 0.95fr) minmax(320px, 1.05fr);
  gap: 16px;
}

.question-review-select,
.question-review-editor {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.question-review-select label,
.question-review-editor label {
  color: #475569;
  font-size: 13px;
  font-weight: 600;
}

.question-preview {
  min-height: 120px;
  padding: 14px 16px;
  border: 1px solid #ede5d8;
  border-radius: 16px;
  background: #fff;
  color: #4b5563;
  line-height: 1.7;
  white-space: pre-wrap;
}

.dialog-footer {
  display: inline-flex;
  gap: 10px;
}

@media (max-width: 960px) {
  .editor-hero,
  .section-head,
  .question-review-head {
    flex-direction: column;
    align-items: stretch;
  }

  .editor-hero-badge {
    min-width: auto;
  }

  .editor-meta-grid,
  .question-review-body {
    grid-template-columns: 1fr;
  }
}
</style>
