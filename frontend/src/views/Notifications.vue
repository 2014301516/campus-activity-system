<script setup>
import { ref, onMounted } from 'vue'
import { notificationApi } from '@/api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const notifications = ref([])

async function fetchNotifications() {
  loading.value = true
  try { const res = await notificationApi.getMyNotifications(); notifications.value = res.data || [] } catch (e) {}
  finally { loading.value = false }
}

async function markAllRead() {
  try { await notificationApi.markAllRead(); notifications.value.forEach(n => n.isRead = 1); ElMessage.success('已全部标为已读') } catch (e) {}
}

onMounted(fetchNotifications)
</script>

<template>
  <div class="page-card">
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
      <h2>🔔 消息通知</h2>
      <el-button v-if="notifications.some(n=>n.isRead===0)" type="primary" size="small" @click="markAllRead">全部已读</el-button>
    </div>

    <div v-loading="loading">
      <el-empty v-if="!loading && notifications.length === 0" description="暂无消息" />

      <div v-else>
        <div v-for="n in notifications" :key="n.id"
             class="notif-card" :class="{ unread: n.isRead === 0 }">
          <div class="notif-top">
            <span class="notif-title">{{ n.title }}</span>
            <el-tag v-if="n.isRead === 0" type="danger" size="small">未读</el-tag>
            <el-tag v-else type="info" size="small">已读</el-tag>
          </div>
          <div class="notif-body">{{ n.content }}</div>
          <div class="notif-footer">
            <span class="notif-type">{{ typeLabel(n.type) }}</span>
            <span class="notif-time">{{ n.createdAt }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  methods: {
    typeLabel(type) {
      const map = { audit_reject: '审核驳回', cancel_reject: '取消驳回', system: '系统通知' }
      return map[type] || type
    }
  }
}
</script>

<style scoped>
.notif-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 16px 20px;
  margin-bottom: 12px;
  transition: background 0.2s;
}
.notif-card.unread {
  border-left: 3px solid #409eff;
  background: #f0f7ff;
}
.notif-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.notif-title {
  flex: 1;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.notif-body {
  font-size: 14px;
  color: #606266;
  line-height: 1.7;
  margin-bottom: 10px;
}
.notif-footer {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #c0c4cc;
}
.notif-type {
  color: #909399;
}
</style>
