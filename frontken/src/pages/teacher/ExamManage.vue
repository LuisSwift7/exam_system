<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { http } from '../../api/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Icon } from '@iconify/vue'

const loading = ref(false)
const list = ref<any[]>([])
const dialogVisible = ref(false)
const composeVisible = ref(false)
const currentExamId = ref<number | null>(null)
const form = ref<any>({
  id: undefined,
  title: '',
  description: '',
  startTime: '',
  endTime: '',
  duration: 60
})

// Preview
const previewVisible = ref(false)
const previewData = ref<any>(null)

// Compose
const activeComposeTab = ref('manual') // 'manual' | 'auto'
const questions = ref<any[]>([])
const selectedQIds = ref<number[]>([])
const autoStrategy = ref({
  type: 1,
  count: 10,
  score: 2,
  difficulty: 3
})

async function fetchList() {
  loading.value = true
  try {
    const res = await http.get('/api/teacher/exams')
    list.value = res.data.data.list
  } catch (e: any) {
    ElMessage.error(e?.message || '获取列表失败')
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  form.value = {
    id: undefined,
    title: '',
    description: '',
    startTime: '',
    endTime: '',
    duration: 60
  }
  dialogVisible.value = true
}

function handleEdit(row: any) {
  form.value = { ...row }
  dialogVisible.value = true
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该试卷？', '警告', { type: 'warning' })
    await http.delete(`/api/teacher/exams/${id}`)
    ElMessage.success('删除成功')
    fetchList()
  } catch {
    // Cancelled
  }
}

async function handlePublish(row: any) {
  try {
    await ElMessageBox.confirm(`确认发布试卷 "${row.title}" 吗？发布后学生可见。`, '发布确认', {
      type: 'warning',
      confirmButtonText: '确认发布'
    })
    await http.post(`/api/teacher/exams/${row.id}/publish`)
    ElMessage.success('发布成功')
    fetchList()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '发布失败')
    }
  }
}

async function handlePreview(row: any) {
  try {
    const res = await http.get(`/api/teacher/exams/${row.id}/preview`)
    previewData.value = res.data.data
    previewVisible.value = true
  } catch (e: any) {
    ElMessage.error(e?.message || '加载预览失败')
  }
}

async function submit() {
  try {
    // Format dates
    const payload = { ...form.value }
    if (payload.startTime) payload.startTime = payload.startTime.replace(' ', 'T')
    if (payload.endTime) payload.endTime = payload.endTime.replace(' ', 'T')

    if (form.value.id) {
      await http.put(`/api/teacher/exams/${form.value.id}`, payload)
    } else {
      await http.post('/api/teacher/exams', payload)
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    fetchList()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

async function openCompose(examId: number) {
  currentExamId.value = examId
  composeVisible.value = true
  activeComposeTab.value = 'manual'
  // Load questions and current selection
  const [qRes, sRes] = await Promise.all([
    http.get('/api/teacher/questions?size=1000'), // Load all for now
    http.get(`/api/teacher/exams/${examId}/question-ids`)
  ])
  questions.value = qRes.data.data.list
  selectedQIds.value = sRes.data.data
}

async function submitManual() {
  if (!currentExamId.value) return
  try {
    await http.post(`/api/teacher/exams/${currentExamId.value}/manual-compose`, {
      questionIds: selectedQIds.value,
      score: 2 // Default score per question
    })
    ElMessage.success('手动组卷成功')
    composeVisible.value = false
  } catch (e: any) {
    ElMessage.error(e?.message || '组卷失败')
  }
}

async function submitAuto() {
  if (!currentExamId.value) return
  try {
    await http.post(`/api/teacher/exams/${currentExamId.value}/auto-compose`, autoStrategy.value)
    ElMessage.success('自动组卷成功')
    composeVisible.value = false
  } catch (e: any) {
    ElMessage.error(e?.message || '组卷失败')
  }
}

function getStatusTag(status: number) {
  if (status === 1) return { type: 'success', text: '已发布' }
  if (status === 2) return { type: 'danger', text: '已结束' }
  return { type: 'info', text: '未发布' }
}

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div class="panel">
    <div class="panel__head">
      <h3>试卷管理</h3>
      <el-button type="primary" @click="handleAdd">
        <Icon icon="iconoir:plus" />
        创建试卷
      </el-button>
    </div>

    <el-table :data="list" v-loading="loading" style="width: 100%">
      <el-table-column prop="title" label="试卷名称" show-overflow-tooltip />
      <el-table-column prop="duration" label="时长(分)" width="100" />
      <el-table-column prop="startTime" label="开始时间" width="180">
        <template #default="{ row }">
          {{ row.startTime?.replace('T', ' ').slice(0, 16) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusTag(row.status).type as any">{{ getStatusTag(row.status).text }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button 
            link 
            type="success" 
            size="small" 
            v-if="row.status === 0"
            @click="handlePublish(row)"
          >
            发布
          </el-button>
          <el-button link type="info" size="small" @click="handlePreview(row)">预览</el-button>
          <el-button link type="primary" size="small" @click="openCompose(row.id)">组卷</el-button>
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑试卷' : '创建试卷'"
      width="500px"
      append-to-body
    >
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input type="textarea" v-model="form.description" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="时长(分)">
          <el-input-number v-model="form.duration" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submit">确认</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- Compose Dialog -->
    <el-dialog
      v-model="composeVisible"
      title="组卷管理"
      width="800px"
      append-to-body
    >
      <el-tabs v-model="activeComposeTab">
        <el-tab-pane label="手动组卷" name="manual">
          <div class="manual-box">
            <el-transfer
              v-model="selectedQIds"
              :data="questions"
              :props="{ key: 'id', label: 'content' }"
              :titles="['题库', '已选']"
              filterable
            />
            <div class="actions">
              <el-button type="primary" @click="submitManual">保存手动组卷</el-button>
            </div>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="自动组卷" name="auto">
          <el-form :model="autoStrategy" label-width="100px" style="max-width: 400px; margin: 0 auto;">
            <el-form-item label="题型">
              <el-select v-model="autoStrategy.type">
                <el-radio :label="1">单选题</el-radio>
              </el-select>
            </el-form-item>
            <el-form-item label="题目数量">
              <el-input-number v-model="autoStrategy.count" :min="1" />
            </el-form-item>
            <el-form-item label="单题分值">
              <el-input-number v-model="autoStrategy.score" :min="1" />
            </el-form-item>
            <el-form-item label="难度偏好">
              <el-rate v-model="autoStrategy.difficulty" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="submitAuto">生成试卷</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- Preview Drawer -->
    <el-drawer v-model="previewVisible" title="试卷预览" size="600px" append-to-body>
      <div v-if="previewData" class="preview-content">
        <div class="p-head">
          <h2>{{ previewData.exam.title }}</h2>
          <div class="p-meta">
            <el-tag effect="dark">{{ getStatusTag(previewData.exam.status).text }}</el-tag>
            <span>总分: {{ previewData.questions.reduce((sum: number, q: any) => sum + q.score, 0) }}</span>
            <span>题目数: {{ previewData.questions.length }}</span>
            <span>时长: {{ previewData.exam.duration }}分钟</span>
          </div>
          <p class="p-desc">{{ previewData.exam.description }}</p>
        </div>
        
        <div class="p-list">
          <div v-for="(q, idx) in previewData.questions" :key="q.id" class="p-item">
            <div class="p-item-head">
              <span class="idx">{{ idx + 1 }}.</span>
              <el-tag size="small" effect="plain">{{ q.type === 2 ? '多选' : '单选' }}</el-tag>
              <el-tag size="small" type="warning" effect="plain">{{ q.score }}分</el-tag>
              <span class="p-q-diff">难度: {{ q.difficulty }}</span>
            </div>
            <div class="p-item-body">
              <div class="q-content">{{ q.content }}</div>
              <div class="q-opts">
                 <div v-for="opt in (typeof q.options === 'string' ? JSON.parse(q.options) : q.options)" :key="opt" class="q-opt">
                   {{ opt }}
                 </div>
              </div>
              <div class="q-ans-box">
                <div class="q-ans">
                  <span class="label">正确答案:</span> 
                  <span class="val">{{ q.answer }}</span>
                </div>
                 <div class="q-analysis" v-if="q.analysis">
                  <span class="label">解析:</span> 
                  <span class="val">{{ q.analysis }}</span>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-if="previewData.questions.length === 0" description="暂无题目，请先进行组卷" />
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.panel {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  width: 100%;
}

.panel__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.panel__head h3 {
  font-size: 18px;
  font-weight: 700;
  color: #1a1e23;
  margin: 0;
}

.manual-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.preview-content {
  padding: 0 20px 20px;
}

.p-head {
  border-bottom: 1px solid #eee;
  padding-bottom: 20px;
  margin-bottom: 20px;
}

.p-head h2 {
  margin: 0 0 12px 0;
  color: #1a1e23;
}

.p-meta {
  display: flex;
  gap: 16px;
  color: #666;
  font-size: 14px;
  align-items: center;
}

.p-desc {
  margin: 12px 0 0;
  color: #888;
  font-size: 13px;
}

.p-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.p-item {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 16px;
}

.p-item-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.idx {
  font-weight: 700;
  font-size: 16px;
  color: #1a1e23;
}

.p-q-diff {
  font-size: 12px;
  color: #999;
  margin-left: auto;
}

.q-content {
  font-size: 15px;
  color: #333;
  margin-bottom: 12px;
  line-height: 1.5;
}

.q-opts {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.q-opt {
  font-size: 14px;
  color: #555;
  background: #f8f9fa;
  padding: 8px 12px;
  border-radius: 6px;
}

.q-ans-box {
  background: #fdf6ec;
  padding: 12px;
  border-radius: 6px;
  font-size: 13px;
}

.q-ans {
  margin-bottom: 4px;
  color: #e6a23c;
  font-weight: 500;
}

.q-analysis {
  color: #888;
  margin-top: 8px;
  border-top: 1px dashed #e6a23c;
  padding-top: 8px;
}

.label {
  margin-right: 8px;
}
</style>
