<template>
  <div class="class-join">
    <div class="page-header">
      <div style="display: flex; align-items: center; gap: 8px;">
        <Icon icon="iconoir:users" style="font-size: 20px; color: #10d4a6;" />
        <h1>我的班级</h1>
      </div>
      <p>查看您加入的班级，或通过班级代码申请加入新班级</p>
      <el-button type="primary" @click="openJoinDialog" class="join-btn">
        <Icon icon="iconoir:plus" />
        加入班级
      </el-button>
    </div>

    <div v-if="auth.me?.roleCode === 'STUDENT'">
      <div class="grid-container" v-if="myClasses.length > 0">
        <el-card v-for="item in myClasses" :key="item.id" class="class-card">
          <template #header>
            <div class="card-header">
              <h3>{{ item.name }}</h3>
              <el-tag size="small" class="class-code">
                {{ item.code }}
              </el-tag>
            </div>
          </template>
          <div class="card-body">
            <div class="class-info">
              <div class="info-item">
                <Icon icon="iconoir:user" />
                <span>班级代码: {{ item.code }}</span>
              </div>
              <div class="info-item">
                <Icon icon="iconoir:calendar" />
                <span>加入时间: {{ formatTime(item.joinTime) }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <el-empty v-else description="您还没有加入任何班级" class="empty-state" />
    </div>

    <!-- 加入班级对话框 -->
    <el-dialog v-model="joinDialogVisible" title="加入班级" width="400px">
      <el-form :model="joinForm" :rules="joinRules" ref="joinFormRef" label-width="100px">
        <el-form-item label="班级代码" prop="code">
          <el-input 
            v-model="joinForm.code" 
            placeholder="请输入班级代码"
            @keyup.enter="handleJoin"
          />
        </el-form-item>
        <el-form-item v-if="selectedClass" label="班级信息" class="class-info-item">
          <div class="class-detail">
            <h4>{{ selectedClass.name }}</h4>
            <p>班级代码: {{ selectedClass.code }}</p>
            <el-button 
              type="primary" 
              size="small" 
              @click="handleJoin"
              style="margin-top: 8px"
            >
              确认加入
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="joinDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="checkCode" 
            :loading="checkingCode"
            v-if="!selectedClass"
          >
            检查代码
          </el-button>
          <el-button 
            type="primary" 
            @click="handleJoin"
            v-else
          >
            确认加入
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import { http } from '../../api/http'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const myClasses = ref<any[]>([])
const loading = ref(false)

const joinDialogVisible = ref(false)
const joinForm = ref({
  code: ''
})
const joinFormRef = ref<any>()
const joinRules = {
  code: [{ required: true, message: '请输入班级代码', trigger: 'blur' }]
}

const selectedClass = ref<any>(null)
const checkingCode = ref(false)

async function fetchMyClasses() {
  loading.value = true
  try {
    const res = await http.get('/api/student/classes')
    myClasses.value = res.data.data
  } catch (e: any) {
    ElMessage.error(e?.message || '获取班级列表失败')
  } finally {
    loading.value = false
  }
}

function openJoinDialog() {
  joinForm.value = { code: '' }
  selectedClass.value = null
  joinDialogVisible.value = true
}

async function checkCode() {
  await joinFormRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      checkingCode.value = true
      try {
        const res = await http.get(`/api/student/classes/check-code/${joinForm.value.code}`)
        if (res.data.code === 0 && res.data.data) {
          selectedClass.value = res.data.data
          ElMessage.success('班级代码正确')
        } else {
          ElMessage.error('班级不存在')
          selectedClass.value = null
        }
      } catch (e: any) {
        ElMessage.error(e?.message || '检查班级代码失败')
        selectedClass.value = null
      } finally {
        checkingCode.value = false
      }
    }
  })
}

async function handleJoin() {
  if (!selectedClass.value) {
    await checkCode()
    if (!selectedClass.value) return
  }

  try {
    await http.post('/api/student/classes/apply', {
      code: joinForm.value.code
    })
    ElMessage.success('申请成功，请等待教师批准')
    joinDialogVisible.value = false
    await fetchMyClasses()
  } catch (e: any) {
    ElMessage.error(e?.message || '申请失败')
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
  if (auth.me?.roleCode === 'STUDENT') {
    fetchMyClasses()
  }
})
</script>

<style scoped>
.class-join {
  min-height: 100vh;
  background: #f6f8fa;
  padding: 24px;
}

.page-header {
  margin-bottom: 40px;
  background: linear-gradient(135deg, #fff 0%, #f0fdf9 100%);
  padding: 40px;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(16, 212, 166, 0.05);
  border: 1px solid rgba(16, 212, 166, 0.1);
  position: relative;
  overflow: hidden;
}

.page-header h1 {
  font-size: 28px;
  font-weight: 800;
  color: #1a1e23;
  margin: 0 0 8px;
  background: linear-gradient(90deg, #1a1e23, #444);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.page-header p {
  font-size: 16px;
  color: #666;
  margin: 0 0 24px;
}

.join-btn {
  position: absolute;
  top: 40px;
  right: 40px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
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
  align-items: center;
}

.card-header h3 {
  font-size: 18px;
  font-weight: 700;
  color: #1a1e23;
  margin: 0;
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
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 14px;
  color: #666;
}

.empty-state {
  margin-top: 80px;
}

.class-info-item {
  margin-top: 8px;
}

.class-detail {
  background: #f9fafb;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.class-detail h4 {
  font-size: 16px;
  font-weight: 700;
  color: #1a1e23;
  margin: 0 0 8px;
}

.class-detail p {
  font-size: 14px;
  color: #666;
  margin: 0 0 12px;
}

@media (max-width: 768px) {
  .grid-container {
    grid-template-columns: 1fr;
  }
  
  .join-btn {
    position: static;
    margin-top: 16px;
  }
}
</style>