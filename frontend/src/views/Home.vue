<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { activityApi, categoryApi, noticeApi, dashboardApi } from '@/api'

const router = useRouter()
const loading = ref(false)
const activityList = ref([])
const categories = ref([])
const notices = ref([])
const featuredActivities = ref([])
const upcomingActivities = ref([])
const freshActivities = ref([])
const total = ref(0)
const activitySquareRef = ref(null)
const stats = ref({
  totalActivities: 0,
  ongoingActivities: 0,
  totalRegistrations: 0
})

const keyword = ref('')
const categoryId = ref(null)
const sort = ref('newest')
const page = ref(1)
const pageSize = ref(8)

async function fetchCategories() {
  try {
    const res = await categoryApi.getList()
    categories.value = res.data
  } catch (e) { /* ignore */ }
}

async function fetchNotices() {
  try {
    const res = await noticeApi.getList({ page: 1, size: 4 })
    notices.value = res.data.records || []
  } catch (e) { /* ignore */ }
}

async function fetchStats() {
  try {
    const res = await dashboardApi.getStats()
    stats.value = res.data || stats.value
  } catch (e) { /* ignore */ }
}

async function fetchActivities() {
  loading.value = true
  try {
    const res = await activityApi.getList({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value || undefined,
      categoryId: categoryId.value || undefined,
      sort: sort.value
    })
    activityList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    activityList.value = []
  } finally {
    loading.value = false
  }
}

async function fetchFeaturedActivities() {
  try {
    const res = await activityApi.getList({ page: 1, size: 3, sort: 'newest' })
    featuredActivities.value = res.data.records || []
  } catch (e) { /* ignore */ }
}

async function fetchUpcomingActivities() {
  try {
    const res = await activityApi.getList({ page: 1, size: 20, sort: 'startTimeAsc' })
    const now = new Date()
    upcomingActivities.value = (res.data.records || [])
      .filter(item => parseActivityTime(item.startTime) > now)
      .slice(0, 4)
  } catch (e) { /* ignore */ }
}

async function fetchFreshActivities() {
  try {
    const res = await activityApi.getList({ page: 1, size: 12, sort: 'newest' })
    const records = res.data.records || []
    const featuredIds = new Set(featuredActivities.value.map(item => item.id))
    const excludedIds = new Set([
      ...featuredActivities.value.map(item => item.id),
      ...upcomingActivities.value.map(item => item.id)
    ])

    const deduplicated = records.filter(item => !excludedIds.has(item.id))
    const fallback = records.filter(item => !featuredIds.has(item.id))
    freshActivities.value = (deduplicated.length > 0 ? deduplicated : fallback).slice(0, 4)
  } catch (e) { /* ignore */ }
}

function handleSearch() {
  page.value = 1
  fetchActivities()
}

async function handleBrowseAll() {
  keyword.value = ''
  categoryId.value = null
  sort.value = 'newest'
  page.value = 1
  await fetchActivities()
  await nextTick()
  activitySquareRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function handleViewPopular() {
  sort.value = 'popular'
  page.value = 1
  await fetchActivities()
  await nextTick()
  activitySquareRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function handleCategoryChange(catId) {
  categoryId.value = catId
  page.value = 1
  fetchActivities()
}

function handlePageChange(p) {
  page.value = p
  fetchActivities()
}

function goDetail(id) {
  router.push(`/activity/${id}`)
}

function parseActivityTime(time) {
  if (!time) return null
  return new Date(time.replace(' ', 'T'))
}

function formatTime(time) {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

function formatDate(time) {
  if (!time) return ''
  return formatTime(time).slice(0, 10)
}

function statusTag(status) {
  const map = {
    draft: { type: 'info', text: '草稿' },
    pending: { type: 'warning', text: '待审核' },
    approved: { type: 'success', text: '报名中' },
    rejected: { type: 'danger', text: '已驳回' },
    ongoing: { type: 'primary', text: '进行中' },
    ended: { type: 'info', text: '已结束' },
    cancelled: { type: 'danger', text: '已取消' }
  }
  return map[status] || { type: 'info', text: status }
}

function countdownText(time) {
  if (!time) return '待定'
  const target = parseActivityTime(time)
  const now = new Date()
  const diff = target - now
  if (diff <= 0) return '即将开始'
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(hours / 24)
  if (days > 0) return `${days} 天后开始`
  return `${Math.max(hours, 1)} 小时后开始`
}

function categoryCountLabel() {
  return `${categories.value.length} 个分类`
}

async function initHomePage() {
  await Promise.all([
    fetchCategories(),
    fetchNotices(),
    fetchStats(),
    fetchFeaturedActivities(),
    fetchUpcomingActivities(),
    fetchActivities()
  ])
  await fetchFreshActivities()
}

onMounted(() => {
  initHomePage()
})
</script>

<template>
  <div class="home-page">
    <section class="hero-section">
      <div class="hero-main">
        <div class="hero-copy">
          <span class="hero-label">校园活动一站式平台</span>
          <h1>发现值得参加的校园活动</h1>
          <p>
            在这里快速查看讲座、比赛、志愿服务和社团活动，及时完成报名、签到和评价。
          </p>
          <div class="hero-actions">
            <el-button type="primary" size="large" @click="handleBrowseAll">浏览全部活动</el-button>
            <el-button size="large" @click="handleViewPopular">查看热门活动</el-button>
          </div>
        </div>
        <div class="hero-highlight" v-if="featuredActivities.length > 0" @click="goDetail(featuredActivities[0].id)">
          <div class="hero-highlight-cover">
            <img v-if="featuredActivities[0].coverImage" :src="featuredActivities[0].coverImage" alt="" />
            <div v-else class="cover-placeholder">
              <el-icon :size="44"><Picture /></el-icon>
            </div>
            <span class="card-badge" :class="'badge-' + featuredActivities[0].status">
              {{ statusTag(featuredActivities[0].status).text }}
            </span>
          </div>
          <div class="hero-highlight-body">
            <div class="hero-highlight-title">{{ featuredActivities[0].title }}</div>
            <div class="hero-highlight-desc">
              {{ featuredActivities[0].description?.substring(0, 70) }}{{ featuredActivities[0].description?.length > 70 ? '...' : '' }}
            </div>
            <div class="hero-highlight-meta">
              <span><el-icon><Clock /></el-icon>{{ formatTime(featuredActivities[0].startTime) }}</span>
              <span><el-icon><Location /></el-icon>{{ featuredActivities[0].location }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="hero-side" v-if="featuredActivities.length > 1">
        <div
          v-for="activity in featuredActivities.slice(1)"
          :key="activity.id"
          class="hero-mini-card"
          @click="goDetail(activity.id)"
        >
          <div class="hero-mini-cover">
            <img v-if="activity.coverImage" :src="activity.coverImage" alt="" />
            <div v-else class="cover-placeholder">
              <el-icon :size="28"><Picture /></el-icon>
            </div>
            <span class="card-badge" :class="'badge-' + activity.status">
              {{ statusTag(activity.status).text }}
            </span>
          </div>
          <div class="hero-mini-body">
            <div class="hero-mini-title">{{ activity.title }}</div>
            <div class="hero-mini-meta">{{ formatDate(activity.startTime) }} · {{ activity.categoryName }}</div>
          </div>
        </div>
      </div>
    </section>

    <section class="stats-grid">
      <div class="stat-card stat-blue">
        <div class="stat-value">{{ stats.totalActivities || 0 }}</div>
        <div class="stat-label">活动总数</div>
        <div class="stat-sub">{{ categoryCountLabel() }}</div>
      </div>
      <div class="stat-card stat-green">
        <div class="stat-value">{{ stats.ongoingActivities || 0 }}</div>
        <div class="stat-label">进行中活动</div>
        <div class="stat-sub">自动按时间流转</div>
      </div>
      <div class="stat-card stat-orange">
        <div class="stat-value">{{ stats.totalRegistrations || 0 }}</div>
        <div class="stat-label">累计报名</div>
        <div class="stat-sub">实时统计报名情况</div>
      </div>
      <div class="stat-card stat-purple">
        <div class="stat-value">{{ notices.length }}</div>
        <div class="stat-label">最新公告</div>
        <div class="stat-sub">及时了解活动通知</div>
      </div>
    </section>

    <section v-if="notices.length > 0" class="notice-panel">
      <div class="section-head">
        <div>
          <h3>最新公告</h3>
          <p>报名提醒、系统通知和活动信息一目了然</p>
        </div>
      </div>
      <div class="notice-grid">
        <div v-for="notice in notices" :key="notice.id" class="notice-card">
          <div class="notice-card-title">📢 {{ notice.title }}</div>
          <div class="notice-card-content">{{ notice.content }}</div>
        </div>
      </div>
    </section>

    <section class="highlight-grid">
      <div class="highlight-column">
        <div class="section-head">
          <div>
            <h3>即将开始</h3>
            <p>优先关注最近要开始的活动</p>
          </div>
        </div>
        <div class="compact-list">
          <div v-for="activity in upcomingActivities" :key="activity.id" class="compact-card" @click="goDetail(activity.id)">
            <div>
              <div class="compact-title">{{ activity.title }}</div>
              <div class="compact-meta">{{ formatTime(activity.startTime) }} · {{ activity.location }}</div>
            </div>
            <el-tag type="warning" effect="plain">{{ countdownText(activity.startTime) }}</el-tag>
          </div>
          <el-empty v-if="upcomingActivities.length === 0" description="暂无即将开始的活动" />
        </div>
      </div>

      <div class="highlight-column">
        <div class="section-head">
          <div>
            <h3>新上架活动</h3>
            <p>优先展示最近发布且未在上方重复出现的活动</p>
          </div>
        </div>
        <div class="compact-list">
          <div v-for="activity in freshActivities" :key="activity.id" class="compact-card" @click="goDetail(activity.id)">
            <div>
              <div class="compact-title">{{ activity.title }}</div>
              <div class="compact-meta">{{ formatDate(activity.createdAt || activity.startTime) }} 发布 · {{ activity.categoryName }}</div>
            </div>
            <div class="hot-badge">
              <el-icon><Clock /></el-icon>
              {{ formatDate(activity.startTime) }}
            </div>
          </div>
          <el-empty v-if="freshActivities.length === 0" description="暂无新上架活动" />
        </div>
      </div>
    </section>

    <section ref="activitySquareRef" class="filter-panel">
      <div class="section-head">
        <div>
          <h3>活动广场</h3>
          <p>按分类、关键词和热度快速筛选所有可报名活动</p>
        </div>
      </div>

      <div class="category-bar category-inline">
        <el-button
          :type="categoryId === null ? 'primary' : ''"
          size="default"
          @click="handleCategoryChange(null)"
        >
          全部
        </el-button>
        <el-button
          v-for="cat in categories"
          :key="cat.id"
          :type="categoryId === cat.id ? 'primary' : ''"
          size="default"
          @click="handleCategoryChange(cat.id)"
        >
          {{ cat.name }}
        </el-button>
      </div>

      <div class="filter-bar">
        <div class="search-box">
          <el-input
            v-model="keyword"
            placeholder="搜索活动名称..."
            clearable
            size="large"
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
            <template #append>
              <el-button type="primary" @click="handleSearch">
                <el-icon><Search /></el-icon>
                搜索
              </el-button>
            </template>
          </el-input>
        </div>

        <el-select v-model="sort" size="large" style="width:140px" @change="fetchActivities">
          <el-option label="最新发布" value="newest" />
          <el-option label="最受欢迎" value="popular" />
        </el-select>
      </div>
    </section>

    <div v-loading="loading" class="activity-grid-wrap">
      <el-empty v-if="!loading && activityList.length === 0" description="暂无活动" />

      <div v-else class="activity-grid">
        <div
          v-for="activity in activityList"
          :key="activity.id"
          class="activity-card"
          @click="goDetail(activity.id)"
        >
          <div class="card-cover">
            <img
              v-if="activity.coverImage"
              :src="activity.coverImage"
              alt=""
            />
            <div v-else class="cover-placeholder">
              <el-icon :size="40"><Picture /></el-icon>
            </div>
            <span class="card-badge" :class="'badge-' + activity.status">
              {{ statusTag(activity.status).text }}
            </span>
          </div>

          <div class="card-info">
            <h3 class="card-title">{{ activity.title }}</h3>
            <p class="card-desc">{{ activity.description?.substring(0, 60) }}{{ activity.description?.length > 60 ? '...' : '' }}</p>
            <div class="card-meta">
              <span><el-icon><Location /></el-icon>{{ activity.location }}</span>
              <span><el-icon><Clock /></el-icon>{{ formatTime(activity.startTime) }}</span>
            </div>
            <div class="card-bottom">
              <el-tag size="small">{{ activity.categoryName }}</el-tag>
              <span class="participant-count">
                <el-icon><User /></el-icon> {{ activity.currentParticipants }}/{{ activity.maxParticipants }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div class="pagination-wrap" v-if="total > pageSize">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="page"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  max-width: 1280px;
  margin: 0 auto;
}

.hero-section {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.hero-main {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 20px;
  background: linear-gradient(135deg, #eef5ff 0%, #f8fbff 100%);
  border-radius: 18px;
  padding: 28px;
  border: 1px solid #e4efff;
}

.hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.hero-label {
  display: inline-block;
  width: fit-content;
  background: #eaf3ff;
  color: #409eff;
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 12px;
  margin-bottom: 12px;
}

.hero-copy h1 {
  font-size: 34px;
  line-height: 1.2;
  color: #1f2d3d;
  margin-bottom: 14px;
}

.hero-copy p {
  color: #5c6b77;
  line-height: 1.8;
  margin-bottom: 22px;
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-highlight {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.08);
}

.hero-highlight-cover {
  position: relative;
  height: 190px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.hero-highlight-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-highlight-body {
  padding: 18px;
}

.hero-highlight-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.hero-highlight-desc {
  color: #606266;
  line-height: 1.7;
  font-size: 13px;
  margin-bottom: 12px;
}

.hero-highlight-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #909399;
  font-size: 13px;
}

.hero-highlight-meta span {
  display: flex;
  align-items: center;
  gap: 6px;
}

.hero-side {
  display: grid;
  gap: 16px;
}

.hero-mini-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #ebeef5;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
}

.hero-mini-cover {
  position: relative;
  height: 124px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  overflow: hidden;
}

.hero-mini-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-mini-body {
  padding: 16px 18px;
}

.hero-mini-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.hero-mini-meta {
  font-size: 13px;
  color: #909399;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  border-radius: 16px;
  padding: 22px;
  color: #fff;
}

.stat-value {
  font-size: 34px;
  font-weight: 700;
}

.stat-label {
  margin-top: 6px;
  font-size: 15px;
}

.stat-sub {
  margin-top: 8px;
  font-size: 12px;
  opacity: 0.9;
}

.stat-blue { background: linear-gradient(135deg, #409eff, #337ecc); }
.stat-green { background: linear-gradient(135deg, #67c23a, #529b2e); }
.stat-orange { background: linear-gradient(135deg, #e6a23c, #cf9236); }
.stat-purple { background: linear-gradient(135deg, #9b6bff, #7a4ee0); }

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: end;
  margin-bottom: 14px;
}

.section-head h3 {
  font-size: 22px;
  color: #303133;
  margin-bottom: 4px;
}

.section-head p {
  color: #909399;
  font-size: 13px;
}

.notice-panel,
.filter-panel {
  background: #fff;
  border-radius: 16px;
  padding: 22px;
  margin-bottom: 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
}

.notice-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.notice-card {
  background: #f8fbff;
  border: 1px solid #e4efff;
  border-radius: 12px;
  padding: 16px;
}

.notice-card-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.notice-card-content {
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
}

.category-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.category-inline {
  margin-bottom: 16px;
}

.highlight-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.highlight-column {
  background: #fff;
  border-radius: 16px;
  padding: 22px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
}

.compact-list {
  display: grid;
  gap: 12px;
}

.compact-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 12px;
  background: #f8fafc;
  cursor: pointer;
}

.compact-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.compact-meta {
  font-size: 13px;
  color: #909399;
}

.hot-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #409eff;
  font-weight: 600;
  white-space: nowrap;
}

.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-box {
  flex: 1;
}

.activity-grid-wrap {
  min-height: 200px;
}

.activity-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.activity-card {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.activity-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 28px rgba(0,0,0,0.12);
}

.card-cover {
  height: 150px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  color: rgba(255,255,255,0.6);
}

.card-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  color: #fff;
}

.badge-approved  { background: #67c23a; }
.badge-ongoing   { background: #409eff; }
.badge-pending   { background: #e6a23c; }
.badge-rejected  { background: #f56c6c; }
.badge-draft     { background: #909399; }
.badge-ended     { background: #909399; }
.badge-cancelled { background: #f56c6c; }

.card-info {
  padding: 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-desc {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-bottom: 12px;
  min-height: 36px;
}

.card-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 12px;
}

.card-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.card-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.participant-count {
  font-size: 13px;
  color: #409eff;
  display: flex;
  align-items: center;
  gap: 4px;
}

.pagination-wrap {
  text-align: center;
  margin-top: 32px;
}

@media (max-width: 1200px) {
  .hero-section,
  .hero-main,
  .stats-grid,
  .notice-grid,
  .highlight-grid,
  .activity-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 900px) {
  .hero-section,
  .hero-main,
  .stats-grid,
  .notice-grid,
  .highlight-grid,
  .activity-grid {
    grid-template-columns: 1fr;
  }

  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
