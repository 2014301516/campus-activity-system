<script setup>
import { computed, ref, onMounted } from 'vue'
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
const roleIntroMap = {
  student: '学生可在这里完成报名、签到和评价。',
  organizer: '组织者可在活动管理中维护自己发布的活动。',
  admin: '管理员主要负责审核活动和平台治理。',
  guest: '登录后可参与报名、签到和评价。'
}

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
  if (!authStore.isLoggedIn || authStore.role !== 'student') return
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

const statusTagTypeMap = {
  draft: 'info',
  pending: 'warning',
  approved: 'success',
  rejected: 'danger',
  ongoing: 'primary',
  ended: 'info',
  cancelled: 'danger'
}

const statusDescMap = {
  draft: '活动仍在编辑中，暂未进入审核流程。',
  pending: '活动已提交审核，等待管理员处理。',
  approved: '活动已通过审核，符合条件的学生可以报名。',
  rejected: '活动审核未通过，组织者修改后可重新提交。',
  ongoing: '活动正在进行中，已报名学生可按规则签到。',
  ended: '活动已经结束，可查看评价与历史信息。',
  cancelled: '活动已取消，请关注其他活动安排。'
}

const progressPercent = computed(() => {
  if (!activity.value || !activity.value.maxParticipants) return 0
  return Math.min(
    100,
    Math.round((activity.value.currentParticipants / activity.value.maxParticipants) * 100)
  )
})

const summaryCards = computed(() => {
  if (!activity.value) return []
  return [
    {
      label: '报名进度',
      value: `${activity.value.currentParticipants}/${activity.value.maxParticipants}`,
      sub: `${progressPercent.value}% 已报名`,
      icon: 'User'
    },
    {
      label: '活动分类',
      value: activity.value.categoryName || '未分类',
      sub: activity.value.organizerName || '未知组织者',
      icon: 'CollectionTag'
    },
    {
      label: '活动地点',
      value: activity.value.location || '待定',
      sub: formatTime(activity.value.startTime),
      icon: 'Location'
    }
  ]
})

const detailTips = computed(() => {
  if (!activity.value) return []
  const tips = []
  if (activity.value.status === 'approved') {
    tips.push('当前处于可报名状态，建议尽早报名以免人数满额。')
  }
  if (activity.value.status === 'ongoing') {
    tips.push('活动进行中，已报名同学可按照时间规则完成签到。')
  }
  if (canCancel()) {
    tips.push('你已报名该活动，如临时无法参加可先取消报名。')
  }
  const hint = signInHint()
  if (hint) {
    tips.push(hint)
  }
  if (tips.length === 0) {
    tips.push('请根据活动时间、地点和状态合理安排参与计划。')
  }
  return tips
})

const identityHint = computed(() => {
  if (!authStore.isLoggedIn) return roleIntroMap.guest
  return roleIntroMap[authStore.role] || roleIntroMap.guest
})

const canReview = computed(() => {
  return !!activity.value &&
    authStore.isLoggedIn &&
    authStore.role === 'student' &&
    (activity.value.status === 'ongoing' || activity.value.status === 'ended')
})

const reviewEmptyText = computed(() => {
  if (!authStore.isLoggedIn) return '暂时还没有评价，登录后参与活动可留下你的体验。'
  if (authStore.role !== 'student') return '暂时还没有评价，评价功能仅对学生开放。'
  return '暂时还没有评价，参加活动后可以回来分享感受。'
})

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
      <section class="hero-panel">
        <div class="hero-cover">
          <img v-if="activity.coverImage" :src="activity.coverImage" alt="" />
          <div v-else class="detail-cover-placeholder">
            <el-icon :size="56"><Picture /></el-icon>
            <span>暂无封面图</span>
          </div>
        </div>
        <div class="hero-main">
          <div class="detail-header">
            <div>
              <div class="detail-kicker">活动详情</div>
              <h1>{{ activity.title }}</h1>
            </div>
            <el-tag :type="statusTagTypeMap[activity.status] || 'info'" size="large">
              {{ statusLabelMap[activity.status] || activity.status }}
            </el-tag>
          </div>
          <p class="hero-subtitle">
            {{ statusDescMap[activity.status] || '请查看活动时间、地点和参与要求。' }}
          </p>
          <div class="hero-meta">
            <span><el-icon><Clock /></el-icon>{{ formatTime(activity.startTime) }} - {{ formatTime(activity.endTime) }}</span>
            <span><el-icon><Location /></el-icon>{{ activity.location }}</span>
            <span><el-icon><User /></el-icon>{{ activity.organizerName }}</span>
          </div>
          <div class="hero-note">
            <el-icon><InfoFilled /></el-icon>
            <span>{{ identityHint }}</span>
          </div>
        </div>
      </section>

      <section class="summary-grid">
        <div v-for="card in summaryCards" :key="card.label" class="summary-card">
          <div class="summary-icon">
            <el-icon v-if="card.icon === 'User'"><User /></el-icon>
            <el-icon v-else-if="card.icon === 'CollectionTag'"><CollectionTag /></el-icon>
            <el-icon v-else><Location /></el-icon>
          </div>
          <div class="summary-content">
            <div class="summary-label">{{ card.label }}</div>
            <div class="summary-value">{{ card.value }}</div>
            <div class="summary-sub">{{ card.sub }}</div>
          </div>
        </div>
      </section>

      <div class="progress-panel">
        <div class="progress-head">
          <span>报名情况</span>
          <span>{{ progressPercent }}%</span>
        </div>
        <el-progress :percentage="progressPercent" :stroke-width="10" />
        <div class="progress-sub">当前已报名 {{ activity.currentParticipants }} 人，最多可容纳 {{ activity.maxParticipants }} 人。</div>
      </div>

      <div class="detail-layout">
        <div class="detail-main">
          <div class="detail-section">
            <h3>活动详情</h3>
            <div class="description-content">{{ activity.description }}</div>
          </div>

          <div class="detail-section">
            <h3>活动信息</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="活动分类">{{ activity.categoryName }}</el-descriptions-item>
              <el-descriptions-item label="组织者">{{ activity.organizerName }}</el-descriptions-item>
              <el-descriptions-item label="活动地点">{{ activity.location }}</el-descriptions-item>
              <el-descriptions-item label="报名人数">{{ activity.currentParticipants }} / {{ activity.maxParticipants }}</el-descriptions-item>
              <el-descriptions-item label="开始时间">{{ formatTime(activity.startTime) }}</el-descriptions-item>
              <el-descriptions-item label="结束时间">{{ formatTime(activity.endTime) }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </div>

        <aside class="detail-side">
          <div class="side-card">
            <h3>参与提醒</h3>
            <div class="tip-list">
              <div v-for="tip in detailTips" :key="tip" class="tip-item">
                <el-icon><Opportunity /></el-icon>
                <span>{{ tip }}</span>
              </div>
            </div>
          </div>
        </aside>
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
        <el-button v-if="canReview"
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
      <div class="review-title">
        <h3>活动评价 ({{ reviews.length }})</h3>
        <span>看看其他同学对这场活动的真实反馈</span>
      </div>
      <el-empty v-if="reviews.length === 0" :description="reviewEmptyText" />
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
.hero-panel {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.hero-cover {
  min-height: 280px;
  border-radius: 18px;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.hero-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.detail-cover-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.85);
}

.hero-main {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 16px;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.detail-kicker {
  font-size: 13px;
  color: #409eff;
  margin-bottom: 8px;
}

.detail-header h1 {
  font-size: 30px;
}

.hero-subtitle {
  color: #606266;
  line-height: 1.8;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  color: #606266;
}

.hero-meta span,
.hero-note {
  display: flex;
  align-items: center;
  gap: 6px;
}

.hero-note {
  width: fit-content;
  padding: 10px 14px;
  border-radius: 12px;
  background: #f5f9ff;
  color: #409eff;
  font-size: 14px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.summary-card {
  display: flex;
  gap: 14px;
  padding: 18px;
  border-radius: 16px;
  background: #f8fbff;
  border: 1px solid #e8f1ff;
}

.summary-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eaf3ff;
  color: #409eff;
  font-size: 20px;
}

.summary-label {
  font-size: 13px;
  color: #909399;
}

.summary-value {
  margin-top: 4px;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.summary-sub {
  margin-top: 4px;
  color: #909399;
  font-size: 13px;
}

.progress-panel {
  margin-bottom: 24px;
  padding: 18px 20px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #ebeef5;
}

.progress-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-weight: 600;
  color: #303133;
}

.progress-sub {
  margin-top: 10px;
  color: #909399;
  font-size: 13px;
}

.detail-layout {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
}

.detail-main,
.detail-side {
  display: grid;
  gap: 20px;
}

.detail-section {
  padding: 22px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #ebeef5;
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

.side-card {
  padding: 22px;
  border-radius: 16px;
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
  border: 1px solid #e8f1ff;
}

.side-card h3 {
  margin-bottom: 14px;
  color: #303133;
}

.tip-list {
  display: grid;
  gap: 12px;
}

.tip-item {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  color: #606266;
  line-height: 1.7;
}

.action-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding: 20px 0;
  border-top: 1px solid #ebeef5;
}

.review-title {
  display: flex;
  justify-content: space-between;
  align-items: end;
  margin-bottom: 12px;
}

.review-title span {
  font-size: 13px;
  color: #909399;
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

@media (max-width: 1000px) {
  .hero-panel,
  .detail-layout,
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
