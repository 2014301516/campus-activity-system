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

function formatTime(time) {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

onMounted(fetchData)
</script>

<template>
  <div class="page-card">
    <h2>📋 我的活动</h2>

    <div v-loading="loading">
      <el-empty v-if="!loading && registrations.length === 0" description="暂无报名记录" />

      <el-table v-else :data="registrations" stripe style="margin-top:20px">
        <el-table-column label="活动名称" prop="activityTitle" min-width="200">
          <template #default="{ row }">
            <el-link type="primary" @click="goDetail(row.activityId)">{{ row.activityTitle }}</el-link>
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
            <template v-if="row.status === 'registered'">
              <el-button size="small" type="danger" @click="handleCancel(row.activityId)">取消报名</el-button>
              <el-button size="small" type="success" :loading="signingActivityId === row.activityId"
                         @click="handleSignIn(row.activityId)">签到</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>
