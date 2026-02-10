<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { http } from '../../api/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Icon } from '@iconify/vue'

const loading = ref(false)
const list = ref<any[]>([])
const dialogVisible = ref(false)
const formRef = ref<any>(null)
const form = ref({
  id: undefined,
  username: '',
  realName: '',
  password: '',
  status: 1
})
const rules = {
  username: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [{ required: false, message: '留空则不修改', trigger: 'blur' }]
}

async function fetchList() {
  loading.value = true
  try {
    const res = await http.get('/api/teacher/students')
    list.value = res.data.data.list
  } catch (e: any) {
    ElMessage.error(e?.message || '获取列表失败')
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  form.value = { id: undefined, username: '', realName: '', password: '', status: 1 }
  rules.password[0].required = true
  rules.password[0].message = '初始密码必填'
  dialogVisible.value = true
}

function handleEdit(row: any) {
  form.value = { ...row, password: '' }
  rules.password[0].required = false
  rules.password[0].message = '留空则不修改'
  dialogVisible.value = true
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该学生？', '警告', { type: 'warning' })
    await http.delete(`/api/teacher/students/${id}`)
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
        if (form.value.id) {
          await http.put(`/api/teacher/students/${form.value.id}`, form.value)
        } else {
          await http.post('/api/teacher/students', form.value)
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
      <h3>学生管理</h3>
      <el-button type="primary" @click="handleAdd">
        <Icon icon="iconoir:plus" />
        添加学生
      </el-button>
    </div>

    <el-table :data="list" v-loading="loading" style="width: 100%">
      <el-table-column prop="username" label="学号" width="180" />
      <el-table-column prop="realName" label="姓名" width="180" />
      <el-table-column prop="createdTime" label="注册时间" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑学生' : '添加学生'"
      width="500px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="学号" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" placeholder="不修改请留空" show-password />
        </el-form-item>
        <el-form-item label="状态" v-if="form.id">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
          />
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
