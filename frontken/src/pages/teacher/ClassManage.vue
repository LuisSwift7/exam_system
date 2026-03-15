<template>
  <div class="panel">
    <div class="panel__head">
      <div class="panel__title">
        <Icon icon="iconoir:group" class="title-icon" />
        <h3>班级管理</h3>
      </div>
      <p class="panel__desc">创建和管理您的班级，添加学生并发布考试</p>
      <el-button type="primary" @click="openCreateDialog">
        <Icon icon="iconoir:plus" />
        创建班级
      </el-button>
    </div>

    <div class="grid-container">
      <el-card v-for="item in classes" :key="item.id" class="class-card">
        <template #header>
          <div class="card-header">
            <h3>{{ item.name }}</h3>
            <div class="header-actions">
              <el-tag size="small" class="class-code">
                {{ item.code }}
              </el-tag>
              <el-dropdown>
                <el-button link size="small">
                  <Icon icon="iconoir:settings" />
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="openEditDialog(item)">
                      <Icon icon="iconoir:edit" />
                      编辑班级
                    </el-dropdown-item>
                    <el-dropdown-item @click="openStudentManage(item)" divided>
                      <Icon icon="iconoir:group" />
                      管理学生
                    </el-dropdown-item>
                    <el-dropdown-item @click="deleteClass(item)" divided danger>
                      <Icon icon="iconoir:trash" />
                      删除班级
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </template>
        
        <div class="card-body">
          <div class="class-info">
            <div class="info-item">
              <Icon icon="iconoir:group" />
              <span>学生人数: {{ item.studentCount || 0 }}</span>
            </div>
            <div class="info-item">
              <Icon icon="iconoir:calendar" />
              <span>创建时间: {{ formatTime(item.createdTime) }}</span>
            </div>
          </div>
          
          <div class="card-actions">
            <el-button type="primary" size="small" @click="openStudentManage(item)">
              <Icon icon="iconoir:group" />
              管理学生
            </el-button>
            <el-button type="info" size="small" @click="openApplications(item)">
              <Icon icon="iconoir:chat-bubble" />
              申请管理
              <el-badge v-if="item.applicationCount" :value="item.applicationCount" class="application-badge" />
            </el-button>
          </div>
        </div>
      </el-card>
      
      <div v-if="!loading && classes.length === 0" class="empty-container">
        <el-empty description="暂无班级，点击上方按钮创建" />
      </div>
    </div>

    <!-- 创建/编辑班级对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="400px" append-to-body>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="班级名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入班级名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveClass">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 学生管理对话框 -->
    <el-dialog v-model="studentDialogVisible" title="班级学生管理" width="700px" append-to-body>
      <div class="student-manage">
        <div class="add-student-section">
          <h4>添加学生</h4>
          <el-form :model="addStudentForm" ref="addStudentFormRef" inline>
            <el-form-item label="学生账号">
              <el-autocomplete
                v-model="addStudentForm.studentId"
                :fetch-suggestions="searchStudents"
                placeholder="请输入学生账号搜索"
                :trigger-on-focus="false"
                @select="handleStudentSelect"
                style="width: 240px"
              >
                <template #prefix>
                  <Icon icon="iconoir:search" class="search-icon" />
                </template>
              </el-autocomplete>
            </el-form-item>
            <el-button type="primary" @click="addStudent" :disabled="!selectedStudent">添加</el-button>
          </el-form>
        </div>
        
        <div class="student-list-section">
          <h4>班级学生</h4>
          <el-table :data="classStudents" style="width: 100%">
            <el-table-column prop="studentUsername" label="学生账号" width="150" />
            <el-table-column prop="studentRealName" label="学生姓名" />
            <el-table-column prop="joinTime" label="加入时间" width="180" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <el-button type="danger" size="small" @click="removeStudent(scope.row)">
                  移除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-dialog>

    <!-- 申请管理对话框 -->
    <el-dialog v-model="applicationDialogVisible" title="班级申请管理" width="700px" append-to-body>
      <el-table :data="classApplications" style="width: 100%">
        <el-table-column prop="studentUsername" label="学生账号" width="150" />
        <el-table-column prop="studentRealName" label="学生姓名" />
        <el-table-column prop="applyTime" label="申请时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" @click="approveApplication(scope.row, 1)">
              批准
            </el-button>
            <el-button type="danger" size="small" @click="approveApplication(scope.row, 2)">
              拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Icon } from '@iconify/vue'
import { http } from '../../api/http'
import { ElMessage, ElMessageBox } from 'element-plus'

const classes = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('创建班级')
const form = ref({
  name: ''
})
const formRef = ref<any>()
const rules = {
  name: [{ required: true, message: '请输入班级名称', trigger: 'blur' }]
}

const studentDialogVisible = ref(false)
const currentClass = ref<any>(null)
const classStudents = ref<any[]>([])
const addStudentForm = ref({
  studentId: ''
})
const addStudentFormRef = ref<any>()
const selectedStudent = ref<any>(null)

const applicationDialogVisible = ref(false)
const classApplications = ref<any[]>()

async function fetchClasses() {
  loading.value = true
  try {
    const res = await http.get('/api/teacher/classes')
    console.log('班级列表接口返回:', res.data.data.list)
    const classList = res.data.data.list
    // 获取每个班级的学生人数和申请数
    for (const item of classList) {
      await fetchClassStats(item)
    }
    classes.value = classList
  } catch (e: any) {
    ElMessage.error(e?.message || '获取班级列表失败')
  } finally {
    loading.value = false
  }
}

async function fetchClassStats(classItem: any) {
  try {
    console.log(`获取班级 ${classItem.id} 的统计信息`)
    // 获取学生人数
    const studentsRes = await http.get(`/api/teacher/classes/${classItem.id}/students`)
    console.log(`学生人数返回:`, studentsRes.data.data)
    // 检查返回数据结构，可能total字段不存在或在不同层级
    if (studentsRes.data.data && studentsRes.data.data.list) {
      classItem.studentCount = studentsRes.data.data.list.length
    } else if (studentsRes.data.data && typeof studentsRes.data.data === 'number') {
      classItem.studentCount = studentsRes.data.data
    } else {
      classItem.studentCount = 0
    }
    
    // 获取申请数
    const appsRes = await http.get(`/api/teacher/classes/${classItem.id}/applications`)
    console.log(`申请数返回:`, appsRes.data.data)
    if (appsRes.data.data && appsRes.data.data.list) {
      classItem.applicationCount = appsRes.data.data.list.length
    } else if (appsRes.data.data && typeof appsRes.data.data === 'number') {
      classItem.applicationCount = appsRes.data.data
    } else {
      classItem.applicationCount = 0
    }
    console.log(`更新后的班级信息:`, classItem)
  } catch (e) {
    console.error('获取班级统计失败', e)
    // 确保即使接口失败也有默认值
    classItem.studentCount = 0
    classItem.applicationCount = 0
  }
}

function openCreateDialog() {
  dialogTitle.value = '创建班级'
  form.value = { name: '' }
  dialogVisible.value = true
}

function openEditDialog(item: any) {
  dialogTitle.value = '编辑班级'
  form.value = { name: item.name }
  currentClass.value = item
  dialogVisible.value = true
}

async function saveClass() {
  await formRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      try {
        if (currentClass.value) {
          // 编辑班级
          await http.put(`/api/teacher/classes/${currentClass.value.id}`, {
            name: form.value.name
          })
          ElMessage.success('班级更新成功')
          await fetchClasses()
        } else {
          // 创建班级
          await http.post('/api/teacher/classes', {
            name: form.value.name
          })
          ElMessage.success('班级创建成功')
          await fetchClasses()
        }
        dialogVisible.value = false
        currentClass.value = null
      } catch (e: any) {
        ElMessage.error(e?.message || '保存失败')
      }
    }
  })
}

function openStudentManage(item: any) {
  currentClass.value = item
  studentDialogVisible.value = true
  fetchClassStudents(item.id)
}

async function fetchClassStudents(classId: number) {
  try {
    const res = await http.get(`/api/teacher/classes/${classId}/students`)
    classStudents.value = res.data.data.list
  } catch (e: any) {
    ElMessage.error(e?.message || '获取学生列表失败')
  }
}

async function addStudent() {
  if (!selectedStudent.value) {
    ElMessage.warning('请先选择学生')
    return
  }
  
  try {
    await http.post(`/api/teacher/classes/${currentClass.value.id}/students`, {
      studentId: selectedStudent.value.id
    })
    ElMessage.success('学生添加成功')
    await fetchClassStudents(currentClass.value.id)
    // 更新班级统计信息
    await fetchClassStats(currentClass.value)
    // 重新获取班级列表以更新显示
    await fetchClasses()
    addStudentForm.value.studentId = ''
    selectedStudent.value = null
  } catch (e: any) {
    ElMessage.error(e?.message || '添加失败')
  }
}

async function searchStudents(query: string, callback: any) {
  if (!query) {
    callback([])
    return
  }
  
  try {
    const res = await http.get(`/api/teacher/students/search`, {
      params: { keyword: query }
    })
    const students = res.data.data.list.map((item: any) => ({
      value: item.username,
      label: `${item.username} - ${item.realName}`,
      id: item.id
    }))
    callback(students)
  } catch (e) {
    console.error('搜索学生失败', e)
    callback([])
  }
}

function handleStudentSelect(item: any) {
  selectedStudent.value = {
    id: item.id,
    username: item.value,
    realName: item.label.split(' - ')[1]
  }
}

async function removeStudent(student: any) {
  try {
    await http.delete(`/api/teacher/classes/${currentClass.value.id}/students/${student.studentId}`)
    ElMessage.success('学生移除成功')
    await fetchClassStudents(currentClass.value.id)
  } catch (e: any) {
    ElMessage.error(e?.message || '移除失败')
  }
}

function openApplications(item: any) {
  currentClass.value = item
  applicationDialogVisible.value = true
  fetchClassApplications(item.id)
}

async function fetchClassApplications(classId: number) {
  try {
    const res = await http.get(`/api/teacher/classes/${classId}/applications`)
    classApplications.value = res.data.data.list
  } catch (e: any) {
    ElMessage.error(e?.message || '获取申请列表失败')
  }
}

async function approveApplication(application: any, status: number) {
  try {
    await http.put(`/api/teacher/classes/applications/${application.id}/approve`, {
      status
    })
    ElMessage.success(status === 1 ? '批准成功' : '拒绝成功')
    await fetchClassApplications(currentClass.value.id)
    await fetchClassStats(currentClass.value)
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

async function deleteClass(item: any) {
  try {
    await ElMessageBox.confirm(
      `确定要删除班级「${item.name}」吗？删除后将无法恢复。`,
      '删除班级',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await http.delete(`/api/teacher/classes/${item.id}`)
    ElMessage.success('班级删除成功')
    await fetchClasses()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '删除失败')
    }
  }
}

function formatTime(time: string) {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  fetchClasses()
})
</script>

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
  flex-wrap: wrap;
  gap: 12px;
}

.panel__title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.title-icon {
  font-size: 20px;
  color: #10d4a6;
  display: flex;
  align-items: center;
  justify-content: center;
}

.panel__title h3 {
  font-size: 18px;
  font-weight: 700;
  color: #1a1e23;
  margin: 0;
}

.panel__desc {
  font-size: 14px;
  color: #666;
  margin: 0;
  flex: 1;
  min-width: 200px;
}

.grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 24px;
}

.class-card {
  border-radius: 16px;
  overflow: hidden;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.class-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.card-header h3 {
  font-size: 18px;
  font-weight: 700;
  color: #1a1e23;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.class-code {
  background: #f0fdf4;
  color: #166534;
  border-color: #86efac;
}

.card-body {
  padding-top: 16px;
}

.class-info {
  margin-bottom: 24px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 14px;
  color: #666;
}

.card-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.student-manage {
  padding: 16px 0;
}

.add-student-section {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #e5e7eb;
}

.add-student-section h4,
.student-list-section h4 {
  font-size: 16px;
  font-weight: 700;
  color: #1a1e23;
  margin: 0 0 16px;
}

.application-badge {
  margin-left: 4px;
}

.empty-container {
  grid-column: 1 / -1;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

@media (max-width: 768px) {
  .grid-container {
    grid-template-columns: 1fr;
  }
  
  .panel__head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>