<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { http } from '../../api/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Icon } from '@iconify/vue'

const loading = ref(false)
const list = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const isDataLoading = ref(false)

async function fetchList() {
  isDataLoading.value = true
  loading.value = true
  try {
    const res = await http.get('/api/admin/users', {
      params: {
        page: currentPage.value,
        size: pageSize.value
      }
    })
    list.value = res.data.data.records
    total.value = res.data.data.total
  } catch (e: any) {
    ElMessage.error(e?.message || '获取用户列表失败')
  } finally {
    loading.value = false
    setTimeout(() => {
      isDataLoading.value = false
    }, 100)
  }
}

async function handleStatusChange(row: any) {
  if (isDataLoading.value) return
  
  const oldStatus = row.status
  const newStatus = oldStatus === 1 ? 0 : 1
  
  try {
    await http.put(`/api/admin/users/${row.id}/status`, {}, {
      params: {
        status: newStatus
      }
    })
    ElMessage.success(`用户已${newStatus === 1 ? '启用' : '禁用'}`)
  } catch (e: any) {
    row.status = oldStatus
    ElMessage.error(e?.message || '操作失败')
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

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div class="panel">
    <div class="panel__head">
      <h3>用户管理</h3>
    </div>

    <el-table :data="list" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="realName" label="真实姓名" width="120" />
      <el-table-column prop="roleCode" label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="row.roleCode === 'ADMIN' ? 'danger' : row.roleCode === 'TEACHER' ? 'warning' : 'info'">
            {{ row.roleCode === 'ADMIN' ? '管理员' : row.roleCode === 'TEACHER' ? '教师' : '学生' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-switch 
            v-model="row.status" 
            :active-value="1"
            :inactive-value="0"
            @change="handleStatusChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="lastLoginTime" label="最后登录时间" width="180">
        <template #default="{ row }">
          {{ row.lastLoginTime ? new Date(row.lastLoginTime).toLocaleString() : '从未登录' }}
        </template>
      </el-table-column>
      <el-table-column prop="createdTime" label="创建时间" width="180">
        <template #default="{ row }">
          {{ new Date(row.createdTime).toLocaleString() }}
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