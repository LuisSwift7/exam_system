<template>
  <div class="notification-bell">
    <el-dropdown @visible-change="handleDropdownVisibleChange">
      <div class="bell-wrapper">
        <Icon icon="iconoir:bell" class="bell-icon" />
        <el-badge v-if="unreadCount > 0" :value="unreadCount" class="notification-badge" />
      </div>
      <template #dropdown>
        <div class="notification-dropdown">
          <div class="dropdown-header">
            <h3>通知中心</h3>
            <el-button v-if="unreadCount > 0" type="text" size="small" @click="markAllAsRead">
              全部标记已读
            </el-button>
          </div>
          <div class="dropdown-body">
            <div v-if="notifications.length === 0" class="empty-notifications">
              <Icon icon="iconoir:bell-off" class="empty-icon" />
              <p>暂无通知</p>
            </div>
            <div v-else class="notification-list">
              <div
                v-for="notification in notifications"
                :key="notification.id"
                :class="['notification-item', { 'unread': !notification.isRead }]"
                @click="handleNotificationClick"
              >
                <div class="notification-content">
                  <h4 class="notification-title">{{ notification.title }}</h4>
                  <p class="notification-message" v-if="notification.type === 'exam_published'" v-html="formatExamNotification(notification)"></p>
                  <p class="notification-message" v-else>{{ notification.content }}</p>
                  <span class="notification-time">{{ formatTime(notification.createdAt) }}</span>
                </div>
                <el-button
                  type="text"
                  size="small"
                  class="mark-read-btn"
                  @click.stop="markAsRead(notification.id)"
                >
                  {{ notification.isRead ? '已读' : '标记已读' }}
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { http } from '../api/http'
import { ElMessage } from 'element-plus'

const router = useRouter()
const notifications = ref<any[]>([])
const unreadCount = ref(0)

async function fetchNotifications() {
  try {
    const res = await http.get('/api/notifications')
    notifications.value = res.data.data
    updateUnreadCount()
  } catch (e: any) {
    console.error('获取通知失败:', e)
  }
}

async function fetchUnreadCount() {
  try {
    const res = await http.get('/api/notifications/unread/count')
    unreadCount.value = res.data.data
  } catch (e: any) {
    console.error('获取未读通知数失败:', e)
  }
}

async function markAsRead(id: number) {
  try {
    await http.put(`/api/notifications/${id}/read`)
    const notification = notifications.value.find(n => n.id === id)
    if (notification) {
      notification.isRead = true
      updateUnreadCount()
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '标记已读失败')
  }
}

async function markAllAsRead() {
  try {
    await http.put('/api/notifications/read-all')
    notifications.value.forEach(n => {
      n.isRead = true
    })
    updateUnreadCount()
    ElMessage.success('全部标记已读成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '标记已读失败')
  }
}

function updateUnreadCount() {
  unreadCount.value = notifications.value.filter(n => !n.isRead).length
}

function handleDropdownVisibleChange(visible: boolean) {
  if (visible) {
    fetchNotifications()
  }
}

function navigateToExam(examId: number) {
  router.push(`/student/exam/${examId}`)
}

function formatExamNotification(notification: any) {
  // 提取通知内容中的试卷名称和班级名称
  const content = notification.content
  // 简单处理，为试卷名称添加链接
  return content.replace(/(.+的)(.+)(已发布，请及时查看)/, '$1<a href="javascript:void(0)" class="exam-link" data-exam-id="' + notification.relatedId + '">$2</a>$3')
}

function formatTime(time: string) {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 监听点击事件，处理试卷链接的点击
function handleNotificationClick(event: MouseEvent) {
  const target = event.target as HTMLElement
  if (target.classList.contains('exam-link')) {
    const examId = target.getAttribute('data-exam-id')
    if (examId) {
      router.push(`/student/exam/${examId}`)
    }
  }
}

onMounted(() => {
  fetchUnreadCount()
  // 每30秒刷新一次未读通知数
  setInterval(fetchUnreadCount, 30000)
})
</script>

<style scoped>
.notification-bell {
  position: relative;
}

.bell-wrapper {
  position: relative;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.bell-wrapper:hover {
  background: #f5f7fa;
}

.bell-icon {
  font-size: 20px;
  color: #666;
}

.notification-badge {
  position: absolute;
  top: 0;
  right: 0;
  transform: translate(50%, -50%);
}

.notification-dropdown {
  width: 360px;
  max-height: 400px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.dropdown-header h3 {
  font-size: 16px;
  font-weight: 700;
  color: #1a1e23;
  margin: 0;
}

.dropdown-body {
  flex: 1;
  overflow-y: auto;
  max-height: 320px;
}

.empty-notifications {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #999;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.notification-list {
  padding: 8px 0;
}

.notification-item {
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  border-bottom: 1px solid #f0f2f5;
}

.notification-item:hover {
  background: #f5f7fa;
}

.notification-item.unread {
  background: #f0fdf4;
}

.notification-content {
  flex: 1;
  margin-right: 12px;
}

.notification-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1e23;
  margin: 0 0 4px 0;
}

.notification-message {
  font-size: 13px;
  color: #666;
  margin: 0 0 8px 0;
  line-height: 1.4;
}

.notification-time {
  font-size: 12px;
  color: #999;
}

.mark-read-btn {
  font-size: 12px;
  color: #10d4a6;
  padding: 0;
  margin-left: 8px;
}

.exam-link {
  color: #409eff;
  text-decoration: underline;
  cursor: pointer;
}

.exam-link:hover {
  color: #66b1ff;
}

/* 滚动条样式 */
.dropdown-body::-webkit-scrollbar {
  width: 6px;
}

.dropdown-body::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.dropdown-body::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.dropdown-body::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>