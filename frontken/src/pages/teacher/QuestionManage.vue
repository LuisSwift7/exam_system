<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { http } from '../../api/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Icon } from '@iconify/vue'

const loading = ref(false)
const list = ref<any[]>([])
const dialogVisible = ref(false)
const formRef = ref<any>(null)
const form = ref<any>({
  id: undefined,
  content: '',
  type: 1,
  options: [{ key: 'A', value: '' }, { key: 'B', value: '' }, { key: 'C', value: '' }, { key: 'D', value: '' }],
  answer: [],
  analysis: '',
  difficulty: 3
})

const rules = {
  content: [{ required: true, message: '请输入题目内容', trigger: 'blur' }],
  answer: [{ required: true, message: '请选择正确答案', trigger: 'change' }]
}

const importDialogVisible = ref(false)
const parsedQuestions = ref<any[]>([])
const importLoading = ref(false)

async function handleUpload(options: any) {
  const formData = new FormData()
  formData.append('file', options.file)
  importLoading.value = true
  try {
    const res = await http.post('/api/teacher/question/import/pdf/parse', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    parsedQuestions.value = res.data.data
    ElMessage.success(`成功解析 ${parsedQuestions.value.length} 道题目`)
  } catch (e: any) {
    ElMessage.error(e?.message || '解析失败')
  } finally {
    importLoading.value = false
  }
}

async function confirmImport() {
  if (!parsedQuestions.value.length) return
  try {
    await http.post('/api/teacher/question/import/batch', parsedQuestions.value)
    ElMessage.success('导入成功')
    importDialogVisible.value = false
    parsedQuestions.value = []
    fetchList()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await http.get('/api/teacher/questions')
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
    content: '',
    type: 1,
    options: [{ key: 'A', value: '' }, { key: 'B', value: '' }, { key: 'C', value: '' }, { key: 'D', value: '' }],
    answer: [],
    analysis: '',
    difficulty: 3
  }
  dialogVisible.value = true
}

function handleEdit(row: any) {
  let opts = []
  if (Array.isArray(row.options)) {
    opts = row.options.map((o: string) => {
      const [key, ...rest] = o.split('.')
      return { key, value: rest.join('.').trim() }
    })
  } else if (typeof row.options === 'string') {
    try {
      opts = JSON.parse(row.options).map((o: string) => {
        const [key, ...rest] = o.split('.')
        return { key, value: rest.join('.').trim() }
      })
    } catch {
      // Fallback
      opts = [{ key: 'A', value: '' }, { key: 'B', value: '' }, { key: 'C', value: '' }, { key: 'D', value: '' }]
    }
  }
  
  let ans = row.answer
  if (row.type === 2 && typeof ans === 'string') {
    // If multiple choice, split if string
    ans = ans.split('')
  } else if (row.type === 1 && Array.isArray(ans)) {
    // If single, take first
    ans = ans[0] || ''
  } else if (row.type === 2 && !ans) {
    ans = []
  }

  form.value = { ...row, options: opts, answer: ans }
  dialogVisible.value = true
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该试题？', '警告', { type: 'warning' })
    await http.delete(`/api/teacher/questions/${id}`)
    ElMessage.success('删除成功')
    fetchList()
  } catch {
    // Cancelled
  }
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        let ans = form.value.answer
        if (form.value.type === 2 && Array.isArray(ans)) {
          ans = ans.sort().join('')
        }

        const payload = {
          ...form.value,
          answer: ans,
          options: form.value.options.map((o: any) => `${o.key}. ${o.value}`)
        }
        
        if (form.value.id) {
          await http.put(`/api/teacher/questions/${form.value.id}`, payload)
        } else {
          await http.post('/api/teacher/questions', payload)
        }
        ElMessage.success('操作成功')
        dialogVisible.value = false
        fetchList()
      } catch (e: any) {
        ElMessage.error(e?.message || '操作失败')
      }
    }
  })
}

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div class="panel">
    <div class="panel__head">
      <h3>试题管理</h3>
      <div class="actions">
        <el-button @click="importDialogVisible = true">
          <Icon icon="iconoir:page" />
          PDF智能导入
        </el-button>
        <el-button type="primary" @click="handleAdd">
          <Icon icon="iconoir:plus" />
          添加试题
        </el-button>
      </div>
    </div>

    <el-table :data="list" v-loading="loading" style="width: 100%">
      <el-table-column prop="content" label="题目内容" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag>{{ row.type === 1 ? '单选' : '多选' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="difficulty" label="难度" width="180">
        <template #default="{ row }">
          <el-rate v-model="row.difficulty" disabled />
        </template>
      </el-table-column>
      <el-table-column prop="answer" label="答案" width="100" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑试题' : '添加试题'"
      width="600px"
      append-to-body
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type" @change="form.answer = form.type === 1 ? '' : []">
            <el-radio :label="1">单选题</el-radio>
            <el-radio :label="2">多选题</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题干" prop="content">
          <el-input type="textarea" v-model="form.content" :rows="3" />
        </el-form-item>
        <el-form-item label="难度" prop="difficulty">
          <el-rate v-model="form.difficulty" />
        </el-form-item>
        
        <el-divider content-position="left">选项设置</el-divider>
        <div v-for="(opt, idx) in form.options" :key="idx" class="opt-row">
          <span class="opt-key">{{ opt.key }}</span>
          <el-input v-model="opt.value" placeholder="请输入选项内容" />
        </div>

        <el-divider />
        <el-form-item label="正确答案" prop="answer">
          <el-select v-model="form.answer" placeholder="请选择" :multiple="form.type === 2">
            <el-option v-for="opt in form.options" :key="opt.key" :label="opt.key" :value="opt.key" />
          </el-select>
        </el-form-item>
        <el-form-item label="解析" prop="analysis">
          <el-input type="textarea" v-model="form.analysis" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submit">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- PDF Import Dialog -->
    <el-dialog
      v-model="importDialogVisible"
      title="智能导入试题"
      width="900px"
      append-to-body
    >
      <div class="import-container" v-loading="importLoading">
        <el-upload
          class="upload-demo"
          drag
          action=""
          :http-request="handleUpload"
          :show-file-list="false"
          accept=".pdf"
        >
          <Icon icon="iconoir:cloud-upload" style="font-size: 48px; color: #999" />
          <div class="el-upload__text">拖拽PDF文件到此处或 <em>点击上传</em></div>
          <div class="el-upload__tip">支持自动识别标准格式的题目：1. 题干 A. 选项 答案：A 解析：...</div>
        </el-upload>

        <div v-if="parsedQuestions.length > 0" class="parsed-list">
          <div class="list-head">
            <span>已识别 {{ parsedQuestions.length }} 道题目</span>
            <el-button type="primary" size="small" @click="parsedQuestions = []">清空</el-button>
          </div>
          <el-table :data="parsedQuestions" height="400" border>
            <el-table-column type="index" width="50" />
            <el-table-column prop="content" label="题干" show-overflow-tooltip />
            <el-table-column label="选项" width="200">
              <template #default="{ row }">
                <div v-for="(opt, i) in row.options" :key="i" class="text-xs text-gray-500 truncate">
                  {{ opt }}
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="answer" label="答案" width="60" />
            <el-table-column prop="analysis" label="解析" show-overflow-tooltip />
          </el-table>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="importDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmImport" :disabled="!parsedQuestions.length">
            确认导入
          </el-button>
        </span>
      </template>
    </el-dialog>
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

.opt-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.opt-key {
  font-weight: 700;
  width: 20px;
}

.actions {
  display: flex;
  gap: 12px;
}

.parsed-list {
  margin-top: 24px;
  border-top: 1px dashed #eee;
  padding-top: 24px;
}

.list-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
</style>
