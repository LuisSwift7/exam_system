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
  contentImageUrl: '',
  type: 1,
  category: '常识判断',
  options: [{ key: 'A', value: '', imageUrl: '' }, { key: 'B', value: '', imageUrl: '' }, { key: 'C', value: '', imageUrl: '' }, { key: 'D', value: '', imageUrl: '' }],
  answer: [],
  analysis: '',
  difficulty: 3,
  stemId: undefined
})

const stems = ref<any[]>([])
const stemDialogVisible = ref(false)
const stemForm = ref<any>({
  id: undefined,
  content: '',
  contentImageUrl: ''
})

const categories = ['常识判断', '言语理解', '数量关系', '判断推理', '资料分析']
const filterCategory = ref('')

const rules = {
  content: [{ required: true, message: '请输入题目内容', trigger: 'blur' }],
  answer: [{ required: true, message: '请选择正确答案', trigger: 'change' }],
  category: [{ required: true, message: '请选择题目分类', trigger: 'change' }]
}

const importDialogVisible = ref(false)
const parsedQuestions = ref<any[]>([])
const importLoading = ref(false)
const importCategory = ref('常识判断')
const selectedIds = ref<number[]>([])

async function uploadImage(file: any, optionIndex: number) {
  // 前端压缩图片
  const compressedFile = await compressImage(file)
  
  const formData = new FormData()
  formData.append('file', compressedFile)
  try {
    const res = await http.post('/api/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    // 构建完整的图片 URL
    const baseUrl = 'http://localhost:8080'
    form.value.options[optionIndex].imageUrl = baseUrl + res.data.data.url
    ElMessage.success('图片上传成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '图片上传失败')
  }
}

async function uploadContentImage(file: any) {
  // 前端压缩图片
  const compressedFile = await compressImage(file)
  
  const formData = new FormData()
  formData.append('file', compressedFile)
  try {
    const res = await http.post('/api/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    // 构建完整的图片 URL
    const baseUrl = 'http://localhost:8080'
    form.value.contentImageUrl = baseUrl + res.data.data.url
    ElMessage.success('题干图片上传成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '题干图片上传失败')
  }
}

async function uploadStemImage(file: any) {
  // 前端压缩图片
  const compressedFile = await compressImage(file)
  
  const formData = new FormData()
  formData.append('file', compressedFile)
  try {
    const res = await http.post('/api/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    // 构建完整的图片 URL
    const baseUrl = 'http://localhost:8080'
    stemForm.value.contentImageUrl = baseUrl + res.data.data.url
    ElMessage.success('共享题干图片上传成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '共享题干图片上传失败')
  }
}

async function fetchStems() {
  try {
    const res = await http.get('/api/stems/batch', { params: { ids: [] } })
    stems.value = Object.values(res.data || {})
  } catch (e) {
    console.error('Failed to fetch stems:', e)
  }
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
  stemForm.value = { ...row }
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
    } else {
      await http.post('/api/stems', payload)
    }
    ElMessage.success('操作成功')
    stemDialogVisible.value = false
    fetchStems()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

// 压缩图片至200k以下
function compressImage(file: File): Promise<File> {
  return new Promise((resolve) => {
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')
    const img = new Image()
    
    img.onload = () => {
      // 计算压缩后的尺寸
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
      
      // 绘制图片
      ctx?.drawImage(img, 0, 0, width, height)
      
      // 压缩图片
      canvas.toBlob(
        (blob) => {
          if (blob) {
            // 检查大小，如果还是超过200k，继续压缩
            if (blob.size > 200 * 1024) {
              // 降低质量再次压缩
              canvas.toBlob(
                (smallerBlob) => {
                  if (smallerBlob) {
                    resolve(new File([smallerBlob], file.name, { type: file.type }))
                  } else {
                    resolve(file)
                  }
                },
                file.type,
                0.6
              )
            } else {
              resolve(new File([blob], file.name, { type: file.type }))
            }
          } else {
            resolve(file)
          }
        },
        file.type,
        0.8
      )
    }
    
    img.src = URL.createObjectURL(file)
  })
}

async function handleUpload(options: any) {
  const formData = new FormData()
  formData.append('file', options.file)
  importLoading.value = true
  try {
    const res = await http.post('/api/teacher/question/import/pdf/parse', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    // Initialize category for each parsed question
    parsedQuestions.value = res.data.data.map((q: any) => ({
      ...q,
      category: importCategory.value
    }))
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
    // Add category
    const list = parsedQuestions.value.map(q => ({
      ...q,
      category: q.category || importCategory.value
    }))
    
    await http.post('/api/teacher/question/import/batch', list)
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
    const res = await http.get('/api/teacher/questions', {
      params: {
        category: filterCategory.value || undefined
      }
    })
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
    contentImageUrl: '',
    type: 1,
    category: '常识判断',
    options: [{ key: 'A', value: '', imageUrl: '' }, { key: 'B', value: '', imageUrl: '' }, { key: 'C', value: '', imageUrl: '' }, { key: 'D', value: '', imageUrl: '' }],
    answer: [],
    analysis: '',
    difficulty: 3,
    stemId: undefined
  }
  dialogVisible.value = true
}

function handleEdit(row: any) {
  let opts = []
  if (Array.isArray(row.options)) {
    // 检查第一个元素是否为对象（Option 对象）
    if (row.options.length > 0 && typeof row.options[0] === 'object') {
      // 已经是 Option 对象数组
      opts = row.options.map((o: any) => ({
        key: o.key,
        value: o.value || '',
        imageUrl: o.imageUrl || ''
      }))
    } else {
      // 是字符串数组，需要转换
      opts = row.options.map((o: string) => {
        const [key, ...rest] = o.split('.')
        return { key, value: rest.join('.').trim(), imageUrl: '' }
      })
    }
  } else if (typeof row.options === 'string') {
    try {
      const parsedOptions = JSON.parse(row.options)
      if (Array.isArray(parsedOptions)) {
        // 检查第一个元素是否为对象
        if (parsedOptions.length > 0 && typeof parsedOptions[0] === 'object') {
          opts = parsedOptions.map((o: any) => ({
            key: o.key,
            value: o.value || '',
            imageUrl: o.imageUrl || ''
          }))
        } else {
          // 是字符串数组
          opts = parsedOptions.map((o: string) => {
            const [key, ...rest] = o.split('.')
            return { key, value: rest.join('.').trim(), imageUrl: '' }
          })
        }
      }
    } catch {
      // Fallback
      opts = [{ key: 'A', value: '', imageUrl: '' }, { key: 'B', value: '', imageUrl: '' }, { key: 'C', value: '', imageUrl: '' }, { key: 'D', value: '', imageUrl: '' }]
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

  form.value = { ...row, options: opts, answer: ans, contentImageUrl: row.contentImageUrl || '', stemId: row.stemId || undefined }
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

async function handleBatchDelete() {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先选择要删除的试题')
    return
  }
  try {
    await ElMessageBox.confirm(`确认批量删除选中的 ${selectedIds.value.length} 道试题？`, '警告', { type: 'warning' })
    await http.delete('/api/teacher/questions/batch', { data: selectedIds.value })
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    fetchList()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '批量删除失败')
    }
  }
}

function handleSelectionChange(selection: any[]) {
  selectedIds.value = selection.map(item => item.id)
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        let ans = form.value.answer
        if (form.value.type === 2 && Array.isArray(ans)) {
          ans = ans.sort().join('')
        } else if (form.value.type === 1 && Array.isArray(ans)) {
          // 单选类型，取第一个答案
          ans = ans[0] || ''
        }

        const payload = {
          ...form.value,
          answer: ans,
          options: form.value.options.map((o: any) => ({
            key: o.key,
            value: o.value,
            imageUrl: o.imageUrl
          })),
          stemId: form.value.stemId || undefined
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
  fetchStems()
})
</script>

<template>
  <div class="panel">
    <div class="panel__head">
      <h3>试题管理</h3>
      <div class="actions">
        <el-button 
          type="danger" 
          plain 
          :disabled="!selectedIds.length"
          @click="handleBatchDelete"
        >
          批量删除
        </el-button>
        <el-select v-model="filterCategory" placeholder="按题库筛选" clearable style="width: 150px" @change="fetchList">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
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

    <el-table :data="list" v-loading="loading" style="width: 100%" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" />
      <el-table-column prop="content" label="题目内容" show-overflow-tooltip />
      <el-table-column prop="category" label="题库" width="120">
        <template #default="{ row }">
          <el-tag type="info">{{ row.category || '未分类' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag>
            {{ row.type === 1 ? '单选' : '多选' }}
          </el-tag>
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
        <el-form-item label="题库" prop="category">
          <el-select v-model="form.category" placeholder="请选择">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        
        <!-- 资料分析题的共享题干选择 -->
        <el-form-item v-if="form.category === '资料分析'" label="共享题干">
          <el-select v-model="form.stemId" placeholder="请选择共享题干">
            <el-option label="无共享题干" :value="undefined" />
            <el-option v-for="stem in stems" :key="stem.id" :label="stem.content.substring(0, 30) + (stem.content.length > 30 ? '...' : '')" :value="stem.id" />
          </el-select>
          <div style="margin-top: 8px; display: flex; gap: 8px;">
            <el-button size="small" type="primary" @click="handleAddStem">添加共享题干</el-button>
            <el-button v-if="form.stemId" size="small" @click="handleEditStem(stems.find((s: any) => s.id === form.stemId))">编辑共享题干</el-button>
          </div>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type" @change="form.answer = form.type === 1 ? '' : []">
            <el-radio :label="1">单选题</el-radio>
            <el-radio :label="2">多选题</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题干" prop="content">
          <el-input type="textarea" v-model="form.content" :rows="3" />
          <div class="content-image-upload" style="margin-top: 10px;">
            <el-upload
              class="avatar-uploader"
              action=""
              :http-request="(file) => uploadContentImage(file.file)"
              :show-file-list="false"
              accept="image/*"
            >
              <img v-if="form.contentImageUrl" :src="form.contentImageUrl" class="content-image" />
              <el-button v-else size="small" type="primary">上传题干图片</el-button>
            </el-upload>
            <el-button v-if="form.contentImageUrl" size="small" type="danger" @click="form.contentImageUrl = ''">删除</el-button>
          </div>
        </el-form-item>
        <el-form-item label="难度" prop="difficulty">
          <el-rate v-model="form.difficulty" />
        </el-form-item>
        
        <el-divider content-position="left">选项设置</el-divider>
        <div v-for="(opt, idx) in form.options" :key="idx" class="opt-row">
          <span class="opt-key">{{ opt.key }}</span>
          <el-input v-model="opt.value" placeholder="请输入选项内容" style="flex: 1" />
          <div class="opt-image-upload">
            <el-upload
              class="avatar-uploader"
              action=""
              :http-request="(file) => uploadImage(file.file, idx)"
              :show-file-list="false"
              accept="image/*"
            >
              <img v-if="opt.imageUrl" :src="opt.imageUrl" class="opt-image" />
              <el-button v-else size="small" type="primary">上传图片</el-button>
            </el-upload>
            <el-button v-if="opt.imageUrl" size="small" type="danger" @click="opt.imageUrl = ''">删除</el-button>
          </div>
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
        <div style="margin-bottom: 20px; display: flex; align-items: center; gap: 12px;">
          <span>默认题库：</span>
          <el-select v-model="importCategory" style="width: 200px">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
          <span style="color: #999; font-size: 13px;">(解析结果将默认使用此分类，也可在下方列表中单独修改)</span>
        </div>

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
            <el-table-column label="题库" width="140">
              <template #default="{ row }">
                <el-select v-model="row.category" placeholder="请选择" size="small">
                  <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
                </el-select>
              </template>
            </el-table-column>
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

    <!-- 共享题干编辑对话框 -->
    <el-dialog
      v-model="stemDialogVisible"
      :title="stemForm.id ? '编辑共享题干' : '添加共享题干'"
      width="600px"
      append-to-body
    >
      <el-form :model="stemForm" label-width="80px">
        <el-form-item label="题干内容">
          <el-input type="textarea" v-model="stemForm.content" :rows="4" placeholder="请输入共享题干内容" />
          <div class="content-image-upload" style="margin-top: 10px;">
            <el-upload
              class="avatar-uploader"
              action=""
              :http-request="(file) => uploadStemImage(file.file)"
              :show-file-list="false"
              accept="image/*"
            >
              <img v-if="stemForm.contentImageUrl" :src="stemForm.contentImageUrl" class="content-image" />
              <el-button v-else size="small" type="primary">上传题干图片</el-button>
            </el-upload>
            <el-button v-if="stemForm.contentImageUrl" size="small" type="danger" @click="stemForm.contentImageUrl = ''">删除</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="stemDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitStem">确定</el-button>
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

.opt-image-upload {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 12px;
}

.opt-image {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #e5e7eb;
}

.avatar-uploader :deep(.el-upload) {
  width: 60px;
  height: 60px;
}

.avatar-uploader :deep(.el-upload__text) {
  display: none;
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

.content-image {
  max-width: 100%;
  max-height: 300px;
  object-fit: contain;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  margin-top: 10px;
}
</style>
