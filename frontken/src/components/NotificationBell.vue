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
                :class="['notification-item', { unread: !notification.isRead }]"
                @click="openNotificationDetail(notification)"
              >
                <div class="notification-content">
                  <h4 class="notification-title">{{ notification.title }}</h4>
                  <p class="notification-message">{{ notification.content }}</p>
                  <span class="notification-time">{{ formatTime(notification.createdAt) }}</span>
                </div>

                <div class="notification-actions">
                  <el-button type="text" size="small" class="detail-btn" @click.stop="openNotificationDetail(notification)">
                    查看详情
                  </el-button>
                  <el-button type="text" size="small" class="mark-read-btn" @click.stop="markAsRead(notification.id)">
                    {{ notification.isRead ? '已读' : '标记已读' }}
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </el-dropdown>

    <el-dialog v-model="detailVisible" title="通知详情" width="520px" append-to-body>
      <div v-loading="detailLoading" class="detail-dialog">
        <template v-if="currentNotification">
          <div class="detail-title">{{ currentNotification.title }}</div>
          <div class="detail-meta">
            <span>{{ currentNotification.isRead ? '已读' : '未读' }}</span>
            <span>{{ formatTime(currentNotification.createdAt) }}</span>
          </div>
          <div class="detail-content">{{ currentNotification.content }}</div>
        </template>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button
            v-if="canNavigate(currentNotification)"
            type="primary"
            @click="navigateByNotification(currentNotification)"
          >
            {{ currentNotification?.type === 'review_published' ? '查看讲评' : '查看考试' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { ElMessage } from 'element-plus'
import { http } from '../api/http'

const router = useRouter()
const notifications = ref<any[]>([])
const unreadCount = ref(0)
const detailVisible = ref(false)
const detailLoading = ref(false)
const currentNotification = ref<any | null>(null)
let refreshTimer: ReturnType<typeof setInterval> | null = null

async function fetchNotifications() {
  try {
    const res = await http.get('/api/notifications')
    notifications.value = res.data.data || []
    updateUnreadCount()
  } catch (e: any) {
    console.error('获取通知失败:', e)
  }
}

async function fetchUnreadCount() {
  try {
    const res = await http.get('/api/notifications/unread/count')
    unreadCount.value = res.data.data || 0
  } catch (e: any) {
    console.error('获取未读通知数失败:', e)
  }
}

async function markAsRead(id: number) {
  try {
    await http.put(`/api/notifications/${id}/read`)
    const notification = notifications.value.find((item) => item.id === id)
    if (notification) {
      notification.isRead = true
      updateUnreadCount()
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '标记已读失败')
  }
}

async function openNotificationDetail(notification: any) {
  detailVisible.value = true
  detailLoading.value = true

  try {
    const res = await http.get(`/api/notifications/${notification.id}`)
    currentNotification.value = res.data.data || notification

    if (!notification.isRead) {
      await markAsRead(notification.id)
      notification.isRead = true
      if (currentNotification.value) {
        currentNotification.value.isRead = true
      }
    }
  } catch (e: any) {
    currentNotification.value = notification
    ElMessage.error(e?.message || '获取通知详情失败')
  } finally {
    detailLoading.value = false
  }
}

async function markAllAsRead() {
  try {
    await http.put('/api/notifications/read-all')
    notifications.value.forEach((item) => {
      item.isRead = true
    })
    updateUnreadCount()
    ElMessage.success('全部标记已读成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '标记已读失败')
  }
}

function updateUnreadCount() {
  unreadCount.value = notifications.value.filter((item) => !item.isRead).length
}

function handleDropdownVisibleChange(visible: boolean) {
  if (visible) {
    fetchNotifications()
  }
}

function canNavigate(notification: any) {
  if (!notification) return false
  if (notification.type === 'review_published') return true
  return notification.type === 'exam_published' && !!notification.relatedId
}

function navigateByNotification(notification: any) {
  if (!notification) return

  detailVisible.value = false

  if (notification.type === 'review_published') {
    router.push('/student/reviews')
    return
  }

  if (notification.type === 'exam_published' && notification.relatedId) {
    router.push(`/student/exam/${notification.relatedId}`)
  }
}

function formatTime(time?: string) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  fetchUnreadCount()
  refreshTimer = setInterval(fetchUnreadCount, 30000)
})

onBeforeUnmount(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
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
  border-radius: 10px;
  transition: background 0.2s ease;
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
  max-height: 420px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.dropdown-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.dropdown-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
}

.dropdown-body {
  flex: 1;
  overflow-y: auto;
  max-height: 340px;
}

.empty-notifications {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #94a3b8;
}

.empty-icon {
  font-size: 44px;
  margin-bottom: 12px;
}

.notification-list {
  padding: 8px 0;
}

.notification-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.notification-item:hover {
  background: #f8fafc;
}

.notification-item.unread {
  background: #fffaf2;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-title {
  margin: 0 0 6px;
  color: #1f2937;
  font-size: 14px;
  font-weight: 700;
}

.notification-message {
  margin: 0 0 8px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.notification-time {
  color: #94a3b8;
  font-size: 12px;
}

.notification-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.detail-dialog {
  min-height: 120px;
}

.detail-title {
  color: #1f2937;
  font-size: 18px;
  font-weight: 700;
}

.detail-meta {
  display: flex;
  gap: 12px;
  margin-top: 10px;
  color: #94a3b8;
  font-size: 12px;
}

.detail-content {
  margin-top: 16px;
  color: #475569;
  line-height: 1.8;
  white-space: pre-wrap;
}

.dialog-footer {
  display: inline-flex;
  gap: 10px;
}
</style>
