<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { activityApi, categoryApi, noticeApi } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const activityList = ref([])
const categories = ref([])
const notices = ref([])
const total = ref(0)

// 查询条件
const keyword = ref('')
const categoryId = ref(null)
const sort = ref('newest')
const page = ref(1)
const pageSize = ref(8)

// 获取分类
async function fetchCategories() {
  try {
    const res = await categoryApi.getList()
    categories.value = res.data
  } catch (e) { /* ignore */ }
}

// 获取公告
async function fetchNotices() {
  try {
    const res = await noticeApi.getList({ page: 1, size: 3 })
    notices.value = res.data.records || []
  } catch (e) { /* ignore */ }
}

// 获取活动列表
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

// 搜索
function handleSearch() {
  page.value = 1
  fetchActivities()
}

// 分类切换
function handleCategoryChange(catId) {
  categoryId.value = catId
  page.value = 1
  fetchActivities()
}

// 分页
function handlePageChange(p) {
  page.value = p
  fetchActivities()
}

// 去详情页
function goDetail(id) {
  router.push(`/activity/${id}`)
}

// 格式化时间
function formatTime(time) {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

// 活动状态标签
function statusTag(status) {
  const map = {
    draft: { type: 'info', text: '草稿' },
    pending: { type: 'warning', text: '待审核' },
    approved: { type: 'success', text: '已通过' },
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
    <!-- 公告栏 -->
    <el-alert
      v-for="notice in notices" :key="notice.id"
      :title="notice.title"
      type="info"
      :description="notice.content?.length > 80 ? notice.content.substring(0, 80) + '...' : notice.content"
      show-icon
      :closable="false"
      style="margin-bottom:8px"
    />

    <!-- 搜索栏 -->
    <div class="page-card" style="margin-bottom:20px">
      <el-row :gutter="12" align="middle">
        <el-col :span="8">
          <el-input v-model="keyword" placeholder="搜索活动名称..." clearable @clear="handleSearch" @keyup.enter="handleSearch">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </el-col>
        <el-col :span="2">
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 搜索
          </el-button>
        </el-col>
        <el-col :span="3">
          <el-select v-model="sort" placeholder="排序方式" @change="fetchActivities">
            <el-option label="最新发布" value="newest" />
            <el-option label="最受欢迎" value="popular" />
          </el-select>
        </el-col>
      </el-row>
    </div>

    <!-- 分类筛选 -->
    <div style="margin-bottom:20px">
      <el-radio-group v-model="categoryId" @change="handleCategoryChange" size="default">
        <el-radio-button :value="null">全部</el-radio-button>
        <el-radio-button v-for="cat in categories" :key="cat.id" :value="cat.id">
          {{ cat.name }}
        </el-radio-button>
      </el-radio-group>
    </div>

    <!-- 活动列表 -->
    <div v-loading="loading">
      <el-empty v-if="!loading && activityList.length === 0" description="暂无活动" />

      <el-row :gutter="20" v-else>
        <el-col :span="6" v-for="activity in activityList" :key="activity.id" style="margin-bottom:20px">
          <el-card shadow="hover" class="activity-card" @click="goDetail(activity.id)">
            <!-- 封面图 -->
            <div class="card-cover">
              <img v-if="activity.coverImage" :src="activity.coverImage" alt="" />
              <div v-else class="cover-placeholder">
                <el-icon :size="48"><Picture /></el-icon>
              </div>
              <el-tag :type="statusTag(activity.status).type" size="small" class="status-badge">
                {{ statusTag(activity.status).text }}
              </el-tag>
            </div>

            <div class="card-body">
              <h3 class="card-title">{{ activity.title }}</h3>
              <div class="card-meta">
                <div><el-icon><Location /></el-icon> {{ activity.location }}</div>
                <div><el-icon><Clock /></el-icon> {{ formatTime(activity.startTime) }}</div>
              </div>
              <div class="card-footer">
                <el-tag size="small" type="info">{{ activity.categoryName }}</el-tag>
                <span class="participant-count">
                  <el-icon><User /></el-icon> {{ activity.currentParticipants }}/{{ activity.maxParticipants }}
                </span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 分页 -->
      <div style="text-align:center;margin-top:20px" v-if="total > pageSize">
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
.activity-card {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  border-radius: 8px;
  overflow: hidden;
}

.activity-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.1);
}

.card-cover {
  height: 140px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  margin: -20px -20px 16px;
  overflow: hidden;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  color: #c0c4cc;
}

.status-badge {
  position: absolute;
  top: 8px;
  right: 8px;
}

.card-title {
  font-size: 15px;
  margin-bottom: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-meta {
  font-size: 12px;
  color: #909399;
  line-height: 22px;
  margin-bottom: 12px;
}

.card-meta .el-icon {
  vertical-align: middle;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.participant-count {
  font-size: 13px;
  color: #409eff;
}
</style>
