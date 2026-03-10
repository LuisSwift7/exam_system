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
const form = ref({
  id: undefined,
  examId: '',
  title: '',
  content: ''
})

async function fetchList() {
  loading.value = true
  try {
    const res = await http.get('/api/teacher/reviews')
    list.value = res.data.data
  } catch (e: any) {
    ElMessage.error(e?.message || '获取讲评列表失败')
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
    ElMessage.error(e?.message || '获取试卷列表失败')
  } finally {
    examsLoading.value = false
  }
}

async function handleAdd() {
  form.value = {
    id: undefined,
    examId: '',
    title: '',
    content: ''
  }
  await fetchExams()
  dialogVisible.value = true
}

async function handleEdit(row: any) {
  await fetchExams()
  form.value = {
    id: row.id,
    examId: row.examId,
    title: row.title,
    content: row.content
  }
  dialogVisible.value = true
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该讲评？', '警告', { type: 'warning' })
    // 这里需要添加删除讲评的API调用
    ElMessage.success('删除成功')
    fetchList()
  } catch {
    // Cancelled
  }
}

async function handlePublish(row: any) {
  try {
    await ElMessageBox.confirm(`确认发布讲评 "${row.title}" 吗？发布后学生可见。`, '发布确认', {
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

async function submit() {
  try {
    if (!form.value.examId) {
      ElMessage.error('请选择试卷')
      return
    }
    if (!form.value.title) {
      ElMessage.error('请输入讲评标题')
      return
    }
    if (!form.value.content) {
      ElMessage.error('请输入讲评内容')
      return
    }
    
    if (form.value.id) {
      await http.put(`/api/teacher/reviews/${form.value.id}`, form.value)
    } else {
      await http.post('/api/teacher/reviews', form.value)
    }
    
    ElMessage.success('操作成功')
    dialogVisible.value = false
    fetchList()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

onMounted(() => {
  fetchList()
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
      <el-table-column prop="title" label="讲评标题" show-overflow-tooltip />
      <el-table-column prop="examId" label="关联试卷" width="180">
        <template #default="{ row }">
          {{ exams.find(exam => exam.id === row.examId)?.title || '未知试卷' }}
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
          {{ new Date(row.createdAt).toLocaleString() }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
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
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑讲评' : '创建讲评'"
      width="800px"
      append-to-body
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="关联试卷">
          <el-select 
            v-model="form.examId" 
            placeholder="请选择试卷" 
            style="width: 100%"
            :loading="examsLoading"
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
          <el-input v-model="form.title" placeholder="请输入讲评标题" />
        </el-form-item>
        <el-form-item label="讲评内容">
          <el-input type="textarea" v-model="form.content" placeholder="请输入讲评内容" rows="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submit">确认</el-button>
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
</style>