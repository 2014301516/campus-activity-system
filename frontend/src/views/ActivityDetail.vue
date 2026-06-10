<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { activityApi, registrationApi, signInApi, reviewApi } from '@/api'
import { useAuthStore } from '@/store/auth'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activity = ref(null)
const reviews = ref([])
const mySignInStatus = ref(null)
const myRegistration = ref(null)
const loading = ref(false)
const registering = ref(false)
const cancelling = ref(false)
const signingIn = ref(false)

// 评价
const showReviewDialog = ref(false)
const reviewForm = ref({ rating: 5, comment: '' })

const activityId = Number(route.params.id)

async function fetchDetail() {
  loading.value = true
  try {
    const res = await activityApi.getDetail(activityId)
    activity.value = res.data
  } catch (e) {
    ElMessage.error('活动不存在')
    router.push('/home')
  } finally {
    loading.value = false
  }
}

async function fetchReviews() {
  try {
    const res = await reviewApi.getActivityReviews(activityId)
    reviews.value = res.data || []
  } catch (e) { /* ignore */ }
}

async function fetchSignInStatus() {
  if (!authStore.isLoggedIn) return
  try {
    const res = await signInApi.getStatus(activityId)
    mySignInStatus.value = res.data
  } catch (e) { /* ignore */ }
}

async function fetchMyRegistration() {
  if (!authStore.isLoggedIn || authStore.role !== 'student') return
  try {
    const res = await registrationApi.getMyRegistrations()
    myRegistration.value = (res.data || []).find(item => item.activityId === activityId) || null
  } catch (e) { /* ignore */ }
}

// 报名
async function handleRegister() {
  registering.value = true
  try {
    await registrationApi.register(activityId)
    ElMessage.success('报名成功！')
    fetchDetail()
    fetchMyRegistration()
  } catch (e) { /* ignore */ }
  finally { registering.value = false }
}

// 取消报名
async function handleCancel() {
  cancelling.value = true
  try {
    await registrationApi.cancel(activityId)
    ElMessage.success('已取消报名')
    fetchDetail()
    fetchMyRegistration()
  } catch (e) { /* ignore */ }
  finally { cancelling.value = false }
}

// 签到
async function handleSignIn() {
  signingIn.value = true
  try {
    await signInApi.signIn(activityId)
    ElMessage.success('签到成功！')
    fetchSignInStatus()
  } catch (e) { /* ignore */ }
  finally { signingIn.value = false }
}

// 签退
async function handleSignOut() {
  signingIn.value = true
  try {
    await signInApi.signOut(mySignInStatus.value.id)
    ElMessage.success('签退成功！')
    fetchSignInStatus()
  } catch (e) { /* ignore */ }
  finally { signingIn.value = false }
}

// 提交评价
async function submitReview() {
  try {
    await reviewApi.submit({
      activityId: activityId,
      rating: reviewForm.value.rating,
      comment: reviewForm.value.comment
    })
    ElMessage.success('评价成功！')
    showReviewDialog.value = false
    reviewForm.value = { rating: 5, comment: '' }
    fetchReviews()
  } catch (e) { /* ignore */ }
}

function formatTime(time) {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

function parseTime(time) {
  return time ? new Date(time.replace(' ', 'T')) : null
}

function canRegister() {
  return !!activity.value &&
    (activity.value.status === 'approved' || activity.value.status === 'ongoing') &&
    myRegistration.value?.status !== 'registered'
}

function canCancel() {
  return myRegistration.value?.status === 'registered'
}

function isSignInWindowOpen() {
  if (!activity.value) return false
  if (activity.value.status !== 'approved' && activity.value.status !== 'ongoing') return false

  const startTime = parseTime(activity.value.startTime)
  const endTime = parseTime(activity.value.endTime)
  if (!startTime || !endTime) return false

  const now = new Date()
  const signInStartTime = new Date(startTime.getTime() - 60 * 60 * 1000)
  return now >= signInStartTime && now <= endTime
}

function signInHint() {
  if (!activity.value || !canCancel()) return ''
  const startTime = parseTime(activity.value.startTime)
  const endTime = parseTime(activity.value.endTime)
  if (!startTime || !endTime) return ''

  const now = new Date()
  const signInStartTime = new Date(startTime.getTime() - 60 * 60 * 1000)
  if (now < signInStartTime) return '活动开始前 1 小时开放签到'
  if (now > endTime) return '活动已结束，不能再签到'
  return ''
}

const statusLabelMap = {
  draft: '草稿', pending: '待审核', approved: '已通过', rejected: '已驳回',
  ongoing: '进行中', ended: '已结束', cancelled: '已取消'
}

onMounted(() => {
  fetchDetail()
  fetchReviews()
  fetchSignInStatus()
  fetchMyRegistration()
})
</script>

<template>
  <div v-loading="loading">
    <div v-if="activity" class="page-card">
      <!-- 活动标题 -->
      <div class="detail-header">
        <h1>{{ activity.title }}</h1>
        <el-tag :type="activity.status === 'approved' || activity.status === 'ongoing' ? 'success' : 'info'" size="large">
          {{ statusLabelMap[activity.status] || activity.status }}
        </el-tag>
      </div>

      <!-- 活动信息 -->
      <el-descriptions :column="2" border style="margin:24px 0">
        <el-descriptions-item label="活动分类">{{ activity.categoryName }}</el-descriptions-item>
        <el-descriptions-item label="组织者">{{ activity.organizerName }}</el-descriptions-item>
        <el-descriptions-item label="活动地点">{{ activity.location }}</el-descriptions-item>
        <el-descriptions-item label="报名人数">{{ activity.currentParticipants }} / {{ activity.maxParticipants }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ formatTime(activity.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ formatTime(activity.endTime) }}</el-descriptions-item>
      </el-descriptions>

      <!-- 活动描述 -->
      <div class="detail-section">
        <h3>活动详情</h3>
        <div class="description-content">{{ activity.description }}</div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-bar" v-if="authStore.isLoggedIn && authStore.role === 'student'">
        <el-button v-if="canRegister()"
                   type="primary" size="large" :loading="registering" @click="handleRegister">
          📝 立即报名
        </el-button>
        <el-button v-if="canCancel()"
                   type="warning" size="large" :loading="cancelling" @click="handleCancel">
          取消报名
        </el-button>

        <!-- 签到区域 -->
        <template v-if="canCancel()">
          <el-button v-if="!mySignInStatus" type="success" size="large" :loading="signingIn"
                     :disabled="!isSignInWindowOpen()" @click="handleSignIn">
            ✅ 签到
          </el-button>
          <template v-else>
            <span style="color:#67c23a;font-size:14px">已签到: {{ formatTime(mySignInStatus.signInTime) }}</span>
            <el-button v-if="!mySignInStatus.signOutTime" size="large" :loading="signingIn" @click="handleSignOut">
              签退
            </el-button>
            <span v-else style="color:#909399;font-size:14px">已签退: {{ formatTime(mySignInStatus.signOutTime) }}</span>
          </template>
          <span v-if="!mySignInStatus && signInHint()" style="color:#909399;font-size:14px">{{ signInHint() }}</span>
        </template>

        <!-- 评价按钮 -->
        <el-button v-if="activity.status === 'ongoing' || activity.status === 'ended'"
                   type="info" size="large" @click="showReviewDialog = true">
          ⭐ 评价
        </el-button>
      </div>

      <!-- 未登录提示 -->
      <div v-if="!authStore.isLoggedIn" class="action-bar">
        <el-button type="primary" size="large" @click="$router.push('/login')">
          登录后参与活动
        </el-button>
      </div>
    </div>

    <!-- 评价列表 -->
    <div class="page-card" style="margin-top:20px" v-if="activity">
      <h3>活动评价 ({{ reviews.length }})</h3>
      <el-empty v-if="reviews.length === 0" description="暂无评价" />
      <div v-else>
        <div v-for="review in reviews" :key="review.id" class="review-item">
          <div class="review-header">
            <span class="review-user">{{ review.userName }}</span>
            <el-rate v-model="review.rating" disabled show-score size="small" />
          </div>
          <p class="review-comment">{{ review.comment }}</p>
          <span class="review-time">{{ formatTime(review.createdAt) }}</span>
        </div>
      </div>
    </div>

    <!-- 评价对话框 -->
    <el-dialog v-model="showReviewDialog" title="评价活动" width="480px">
      <el-form :model="reviewForm">
        <el-form-item label="评分">
          <el-rate v-model="reviewForm.rating" show-score />
        </el-form-item>
        <el-form-item label="评论">
          <el-input v-model="reviewForm.comment" type="textarea" :rows="4" placeholder="说说你的感受..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReviewDialog = false">取消</el-button>
        <el-button type="primary" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.detail-header h1 {
  font-size: 24px;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section h3 {
  margin-bottom: 12px;
  color: #303133;
}

.description-content {
  white-space: pre-line;
  color: #606266;
  line-height: 1.8;
}

.action-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 0;
  border-top: 1px solid #ebeef5;
}

.review-item {
  border-bottom: 1px solid #ebeef5;
  padding: 16px 0;
}

.review-item:last-child {
  border-bottom: none;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.review-user {
  font-weight: bold;
  color: #303133;
}

.review-comment {
  color: #606266;
  line-height: 1.6;
  margin-bottom: 4px;
}

.review-time {
  font-size: 12px;
  color: #c0c4cc;
}
</style>
