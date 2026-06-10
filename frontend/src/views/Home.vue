<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { activityApi, categoryApi, noticeApi } from '@/api'

const router = useRouter()
const loading = ref(false)
const activityList = ref([])
const categories = ref([])
const notices = ref([])
const total = ref(0)

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
    const res = await noticeApi.getList({ page: 1, size: 3 })
    notices.value = res.data.records || []
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

function handleSearch() {
  page.value = 1
  fetchActivities()
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

function formatTime(time) {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

function statusTag(status) {
  const map = {
    draft: { type: 'info', text: '草稿' },
    pending: { type: 'warning', text: '待审核' },
    approved: { type: 'success', text: '报名中' },
    ongoing: { type: '', text: '进行中' },
    ended: { type: 'info', text: '已结束' },
    cancelled: { type: 'danger', text: '已取消' }
  }
  return map[status] || { type: 'info', text: status }
}

onMounted(() => {
  fetchCategories()
  fetchNotices()
  fetchActivities()
})
</script>

<template>
  <div class="home-page">

    <!-- 搜索与筛选区 -->
    <div class="filter-bar">
      <!-- 搜索框 -->
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

      <!-- 排序 -->
      <el-select v-model="sort" size="large" style="width:140px" @change="fetchActivities">
        <el-option label="最新发布" value="newest" />
        <el-option label="最受欢迎" value="popular" />
      </el-select>
    </div>

    <!-- 分类标签 -->
    <div class="category-bar">
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

    <!-- 公告通知 -->
    <div v-if="notices.length > 0" class="notice-bar">
      <el-icon class="notice-icon"><Bell /></el-icon>
      <div class="notice-scroll">
        <span v-for="notice in notices" :key="notice.id" class="notice-item">
          📢 {{ notice.title }}
          <el-divider direction="vertical" />
        </span>
      </div>
    </div>

    <!-- 活动列表 -->
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

      <!-- 分页 -->
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
  max-width: 1200px;
  margin: 0 auto;
}

/* ======== 搜索与筛选 ======== */
.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.search-box {
  flex: 1;
  max-width: 520px;
}

/* ======== 分类标签 ======== */
.category-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

/* ======== 公告通知 ======== */
.notice-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 6px;
  padding: 10px 16px;
  margin-bottom: 20px;
  overflow: hidden;
}

.notice-icon {
  font-size: 18px;
  color: #409eff;
  flex-shrink: 0;
}

.notice-scroll {
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
}

.notice-item {
  font-size: 13px;
  color: #606266;
  margin-right: 8px;
}

/* ======== 活动卡片网格 ======== */
.activity-grid-wrap {
  min-height: 200px;
}

.activity-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

@media (max-width: 1200px) {
  .activity-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 900px) {
  .activity-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .activity-grid {
    grid-template-columns: 1fr;
  }
}

/* ======== 单个卡片 ======== */
.activity-card {
  background: #fff;
  border-radius: 10px;
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

/* ======== 分页 ======== */
.pagination-wrap {
  text-align: center;
  margin-top: 32px;
}
</style>
