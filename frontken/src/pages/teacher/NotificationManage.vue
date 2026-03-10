<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { http } from '../../api/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Icon } from '@iconify/vue'

const loading = ref(false)
const list = ref<any[]>([])
const dialogVisible = ref(false)
const classes = ref<any[]>([])
const classesLoading = ref(false)
const form = ref({
  title: '',
  content: '',
  classIds: [],
  type: 'system'
})

// 分页参数
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

async function fetchList() {
  loading.value = true
  try {
    const res = await http.get('/api/notifications/all', {
      params: {
        page: currentPage.value,
        size: pageSize.value
      }
    })
    list.value = res.data.data.list
    total.value = res.data.data.total
  } catch (e: any) {
    ElMessage.error(e?.message || '获取通知列表失败')
  } finally {
    loading.value = false
  }
}

function handleCurrentChange(val: number) {
  currentPage.value = val
  fetchList()
}

function handleSizeChange(val: number) {
  pageSize.value = val
  currentPage.value = 1
  fetchList()
}

async function fetchClasses() {
  classesLoading.value = true
  try {
    const res = await http.get('/api/teacher/classes?size=1000')
    classes.value = res.data.data.list || []
  } catch (e: any) {
    ElMessage.error(e?.message || '获取班级列表失败')
  } finally {
    classesLoading.value = false
  }
}

async function handleAdd() {
  form.value = {
    title: '',
    content: '',
    classIds: [],
    type: 'system'
  }
  await fetchClasses()
  dialogVisible.value = true
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该通知？', '警告', { type: 'warning' })
    await http.delete(`/api/notifications/${id}`)
    ElMessage.success('删除成功')
    fetchList()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '删除失败')
    }
  }
}

async function submit() {
  try {
    // 表单验证
    if (!form.value.title) {
      ElMessage.error('请输入通知标题')
      return
    }
    if (!form.value.content) {
      ElMessage.error('请输入通知内容')
      return
    }
    if (!form.value.classIds || form.value.classIds.length === 0) {
      ElMessage.error('请选择接收班级')
      return
    }
    
    // 获取选中班级的所有学生ID
    const studentIds: number[] = []
    for (const classId of form.value.classIds) {
      const res = await http.get(`/api/teacher/classes/${classId}/students`)
      const students = res.data.data.list || []
      students.forEach((student: any) => {
        if (student.studentId) {
          studentIds.push(student.studentId)
        }
      })
    }
    
    // 检查是否有学生
    if (studentIds.length === 0) {
      ElMessage.error('所选班级中没有学生')
      return
    }
    
    // 发送通知
    await http.post('/api/notifications/create', {
      userIds: studentIds,
      type: form.value.type,
      title: form.value.title,
      content: form.value.content
    })
    
    ElMessage.success('通知发布成功')
    dialogVisible.value = false
    fetchList()
  } catch (e: any) {
    ElMessage.error(e?.message || '发布通知失败')
  }
}

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div class="panel">
    <div class="panel__head">
      <h3>通知管理</h3>
      <el-button type="primary" @click="handleAdd">
        <Icon icon="iconoir:plus" />
        发布通知
      </el-button>
    </div>

    <el-table :data="list" v-loading="loading" style="width: 100%">
      <el-table-column prop="title" label="通知标题" show-overflow-tooltip />
      <el-table-column prop="content" label="通知内容" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.type === 'system' ? 'info' : 'success'">
            {{ row.type === 'system' ? '系统' : '考试' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ new Date(row.createdAt).toLocaleString() }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页组件 -->
    <div class="pagination-container">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        class="pagination"
      />
    </div>

    <!-- Create Dialog -->
    <el-dialog
      v-model="dialogVisible"
      title="发布通知"
      width="600px"
      append-to-body
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="通知标题">
          <el-input v-model="form.title" placeholder="请输入通知标题" />
        </el-form-item>
        <el-form-item label="通知内容">
          <el-input type="textarea" v-model="form.content" placeholder="请输入通知内容" rows="4" />
        </el-form-item>
        <el-form-item label="通知类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="系统通知" value="system" />
            <el-option label="考试通知" value="exam" />
          </el-select>
        </el-form-item>
        <el-form-item label="接收班级">
          <el-select 
            v-model="form.classIds" 
            multiple 
            placeholder="请选择班级（可多选）"
            style="width: 100%"
            :loading="classesLoading"
          >
            <el-option
              v-for="cls in classes"
              :key="cls.id"
              :label="`${cls.name} (${cls.code})`"
              :value="cls.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submit">发布</el-button>
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

/* 分页样式 */
.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination :deep(.el-pagination__total) {
  color: #666;
  font-size: 14px;
}

.pagination :deep(.el-pagination__sizes) {
  font-size: 14px;
}

.pagination :deep(.el-select .el-input__inner) {
  border-radius: 6px;
  border: 1px solid #e4e7ed;
  font-size: 14px;
  height: 32px;
  padding: 0 24px 0 12px;
}

.pagination :deep(.el-pagination__jump) {
  font-size: 14px;
  color: #666;
}

.pagination :deep(.el-input__inner) {
  border-radius: 6px;
  border: 1px solid #e4e7ed;
  font-size: 14px;
  height: 32px;
  width: 60px;
  text-align: center;
}

.pagination :deep(.el-pagination__btn) {
  border-radius: 6px;
  border: 1px solid #e4e7ed;
  background-color: #fff;
  color: #666;
  min-width: 32px;
  height: 32px;
  line-height: 30px;
  font-size: 14px;
  transition: all 0.2s ease;
}

.pagination :deep(.el-pagination__btn:hover) {
  border-color: #10d4a6;
  color: #10d4a6;
}

.pagination :deep(.el-pagination__btn:focus) {
  outline: none;
  box-shadow: 0 0 0 2px rgba(16, 212, 166, 0.2);
}

.pagination :deep(.el-pagination__active .el-pagination__page-btn) {
  background-color: #10d4a6;
  border-color: #10d4a6;
  color: #fff;
  font-weight: 600;
}

.pagination :deep(.el-pagination__active .el-pagination__page-btn:hover) {
  background-color: #0cc096;
  border-color: #0cc096;
  color: #fff;
}

.pagination :deep(.el-pagination__page-btn) {
  border-radius: 6px;
  border: 1px solid #e4e7ed;
  background-color: #fff;
  color: #666;
  min-width: 32px;
  height: 32px;
  line-height: 30px;
  font-size: 14px;
  margin: 0 4px;
  transition: all 0.2s ease;
}

.pagination :deep(.el-pagination__page-btn:hover) {
  border-color: #10d4a6;
  color: #10d4a6;
}

.pagination :deep(.el-pagination__page-btn:focus) {
  outline: none;
  box-shadow: 0 0 0 2px rgba(16, 212, 166, 0.2);
}
</style>