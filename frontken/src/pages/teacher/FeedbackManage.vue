<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { http } from '../../api/http'
import { ElMessage } from 'element-plus'
import { Icon } from '@iconify/vue'

const loading = ref(false)
const list = ref<any[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const status = ref<number | null>(null) // null: all, 0: pending, 1: handled

// Reply Dialog
const replyDialogVisible = ref(false)
const currentFeedback = ref<any>(null)
const replyContent = ref('')
const publishAsReview = ref(false)
const reviewTitle = ref('')

async function fetchList() {
  loading.value = true
  try {
    const res = await http.get('/api/teacher/feedback/list', {
      params: {
        page: page.value,
        size: size.value,
        status: status.value
      }
    })
    list.value = res.data.data.records
    total.value = res.data.data.total
  } catch (e: any) {
    ElMessage.error(e?.message || '获取反馈列表失败')
  } finally {
    loading.value = false
  }
}

async function handleReply(item: any) {
  currentFeedback.value = item
  replyContent.value = ''
  publishAsReview.value = false
  reviewTitle.value = `关于题目 #${item.questionId} 的讲评`
  
  // Check if review exists
  try {
    const res = await http.get(`/api/teacher/feedback/review/${item.questionId}`)
    if (res.data.data) {
      reviewTitle.value = res.data.data.title
      // If review exists, maybe default to checking it? Or just let them decide.
      // publishAsReview.value = true 
    }
  } catch (e) {
    // ignore
  }

  replyDialogVisible.value = true
}

async function submitReply() {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  
  try {
    // 1. Reply to feedback
    await http.post('/api/teacher/feedback/reply', {
      id: currentFeedback.value.id,
      replyContent: replyContent.value
    })

    // 2. Publish review if checked
    if (publishAsReview.value) {
      if (!reviewTitle.value.trim()) {
        ElMessage.warning('请输入讲评标题')
        return
      }
      await http.post('/api/teacher/feedback/review/save', {
        questionId: currentFeedback.value.questionId,
        title: reviewTitle.value,
        content: replyContent.value,
        publishStatus: 1
      })
      ElMessage.success('回复并发布讲评成功')
    } else {
      ElMessage.success('回复成功')
    }
    
    replyDialogVisible.value = false
    fetchList()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

function handlePageChange(p: number) {
  page.value = p
  fetchList()
}

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div class="feedback-manage">
    <div class="filter-bar">
      <el-radio-group v-model="status" @change="fetchList">
        <el-radio-button :label="null">全部</el-radio-button>
        <el-radio-button :label="0">待处理</el-radio-button>
        <el-radio-button :label="1">已处理</el-radio-button>
      </el-radio-group>
      <el-button @click="fetchList">
        <Icon icon="iconoir:refresh" />
        刷新
      </el-button>
    </div>

    <div class="list-container" v-loading="loading">
      <el-table :data="list" style="width: 100%">
        <el-table-column prop="studentId" label="学生ID" width="100" />
        <el-table-column prop="questionId" label="题目ID" width="100" />
        <el-table-column prop="content" label="反馈内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="提交时间" width="180">
          <template #default="{ row }">
            {{ row.createTime ? row.createTime.replace('T', ' ') : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'">
              {{ row.status === 1 ? '已回复' : '待处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button 
              v-if="row.status === 0"
              type="primary" 
              size="small" 
              link 
              @click="handleReply(row)"
            >
              回复
            </el-button>
            <el-button 
              v-else
              type="info" 
              size="small" 
              link 
              @click="handleReply(row)"
            >
              查看回复
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="size"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <el-dialog
      v-model="replyDialogVisible"
      :title="currentFeedback?.status === 1 ? '查看回复' : '回复反馈'"
      width="500px"
      append-to-body
    >
      <div class="dialog-content">
        <div class="feedback-item">
          <label>学生反馈：</label>
          <div class="text">{{ currentFeedback?.content }}</div>
        </div>
        <div class="reply-item">
          <label>教师回复：</label>
          <el-input
            v-if="currentFeedback?.status === 0"
            v-model="replyContent"
            type="textarea"
            :rows="4"
            placeholder="请输入详细的解答和讲评..."
          />
          <div v-if="currentFeedback?.status === 0" class="publish-option">
            <el-checkbox v-model="publishAsReview">同时发布为题目讲评</el-checkbox>
            <el-input 
              v-if="publishAsReview" 
              v-model="reviewTitle" 
              placeholder="请输入讲评标题" 
              style="margin-top: 8px"
            />
          </div>
          <div v-else class="text reply-text">
            {{ currentFeedback?.replyContent }}
          </div>
        </div>
      </div>
      <template #footer v-if="currentFeedback?.status === 0">
        <span class="dialog-footer">
          <el-button @click="replyDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitReply">提交回复</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.feedback-manage {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 24px;
}

.pagination {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.dialog-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.feedback-item label, .reply-item label {
  display: block;
  font-weight: bold;
  margin-bottom: 8px;
  color: #1a1e23;
}

.publish-option {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e5e7eb;
}

.text {
  font-size: 14px;
  color: #334155;
  line-height: 1.6;
  white-space: pre-wrap;
  background: #f8fafc;
  padding: 12px;
  border-radius: 8px;
}

.reply-text {
  background: #f0f9eb;
  color: #67c23a;
}
</style>
