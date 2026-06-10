<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { registrationApi, signInApi } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const registrations = ref([])
const signingActivityId = ref(null)

async function fetchData() {
  loading.value = true
  try {
    const res = await registrationApi.getMyRegistrations()
    registrations.value = res.data || []
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

async function handleCancel(activityId) {
  try {
    await registrationApi.cancel(activityId)
    ElMessage.success('已取消报名')
    fetchData()
  } catch (e) { /* ignore */ }
}

async function handleSignIn(activityId) {
  signingActivityId.value = activityId
  try {
    await signInApi.signIn(activityId)
    ElMessage.success('签到成功！')
    fetchData()
  } catch (e) { /* ignore */ }
  finally { signingActivityId.value = null }
}

function goDetail(id) {
  router.push(`/activity/${id}`)
}

function displayActivityTitle(row) {
  return row.activityTitle && row.activityTitle !== '未知'
    ? row.activityTitle
    : '活动已删除'
}

function isDeletedActivity(row) {
  return displayActivityTitle(row) === '活动已删除'
}

function formatTime(time) {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

function parseTime(time) {
  return time ? new Date(time.replace(' ', 'T')) : null
}

function canSignIn(row) {
  if (row.status !== 'registered') return false
  if (row.activityStatus !== 'approved' && row.activityStatus !== 'ongoing') return false

  const now = new Date()
  const startTime = parseTime(row.activityStartTime)
  const endTime = parseTime(row.activityEndTime)
  if (!startTime || !endTime) return false

  const signInStartTime = new Date(startTime.getTime() - 60 * 60 * 1000)
  return now >= signInStartTime && now <= endTime
}

function signInButtonText(row) {
  if (row.activityStatus !== 'approved' && row.activityStatus !== 'ongoing') {
    return '当前不可签到'
  }

  const startTime = parseTime(row.activityStartTime)
  const endTime = parseTime(row.activityEndTime)
  if (!startTime || !endTime) return '签到'

  const now = new Date()
  const signInStartTime = new Date(startTime.getTime() - 60 * 60 * 1000)
  if (now < signInStartTime) return '未到签到时间'
  if (now > endTime) return '活动已结束'
  return '签到'
}

onMounted(fetchData)
</script>

<template>
  <div class="page-card">
    <h2>📋 我的报名</h2>

    <div v-loading="loading">
      <el-empty v-if="!loading && registrations.length === 0" description="暂无报名记录" />

      <el-table v-else :data="registrations" stripe style="margin-top:20px">
        <el-table-column label="活动名称" prop="activityTitle" min-width="200">
          <template #default="{ row }">
            <el-text v-if="isDeletedActivity(row)" type="info">{{ displayActivityTitle(row) }}</el-text>
            <el-link v-else type="primary" @click="goDetail(row.activityId)">{{ displayActivityTitle(row) }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="报名时间" prop="registeredAt" width="160">
          <template #default="{ row }">{{ formatTime(row.registeredAt) }}</template>
        </el-table-column>
        <el-table-column label="报名状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'registered' ? 'success' : 'info'">
              {{ row.status === 'registered' ? '已报名' : '已取消' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" v-if="registrations.some(r => r.status === 'registered')">
          <template #default="{ row }">
            <template v-if="row.status === 'registered' && !isDeletedActivity(row)">
              <el-button size="small" type="danger" @click="handleCancel(row.activityId)">取消报名</el-button>
              <el-button size="small" type="success" :loading="signingActivityId === row.activityId"
                         :disabled="!canSignIn(row)"
                         @click="handleSignIn(row.activityId)">{{ signInButtonText(row) }}</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>
