<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { activityApi, categoryApi, noticeApi, dashboardApi, recommendationApi, aiChatApi } from '@/api'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const activityList = ref([])
const categories = ref([])
const notices = ref([])
const featuredActivities = ref([])
const carouselActivities = ref([])
const upcomingActivities = ref([])
const freshActivities = ref([])
const aiRecommendations = ref([])
const aiLoading = ref(false)
const RECOMMENDATION_MODE_KEY = 'homeRecommendationMode'
const recommendationMode = ref(loadInitialRecommendationMode())
const heroSectionRef = ref(null)
const highlightSectionRef = ref(null)
const aiPanelRef = ref(null)
const activeNavSection = ref('top')
const total = ref(0)
const activitySquareRef = ref(null)
const floatingNavItems = [
  { id: 'top', label: '顶部概览' },
  { id: 'featured', label: '精选活动' },
  { id: 'recommend', label: '为你推荐' },
  { id: 'square', label: '活动广场' }
]
const stats = ref({
  totalActivities: 0,
  ongoingActivities: 0,
  totalRegistrations: 0
})

const keyword = ref('')
const categoryId = ref(null)
const sort = ref('newest')
const page = ref(1)
const pageSize = ref(16)

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
async function fetchCarouselActivities() {
  try {
    const res = await activityApi.getList({ page: 1, size: 20, sort: 'startTimeAsc' })
    carouselActivities.value = (res.data.records || []).filter(a => a.coverImage).slice(0, 5)
  } catch (e) {}
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

function loadInitialRecommendationMode() {
  if (typeof window === 'undefined') {
    return 'local'
  }
  return localStorage.getItem(RECOMMENDATION_MODE_KEY) === 'ai' ? 'ai' : 'local'
}

async function fetchAiRecommendations(mode = recommendationMode.value) {
  if (!authStore.isLoggedIn || authStore.role !== 'student') {
    aiRecommendations.value = []
    return
  }

  aiLoading.value = true
  try {
    const res = await recommendationApi.getList(mode)
    aiRecommendations.value = res.data || []
  } catch (e) {
    aiRecommendations.value = []
  } finally {
    aiLoading.value = false
  }
}

async function handleRecommendationModeChange(mode) {
  if (recommendationMode.value === mode && aiRecommendations.value.length > 0) {
    return
  }
  recommendationMode.value = mode
  if (typeof window !== 'undefined') {
    localStorage.setItem(RECOMMENDATION_MODE_KEY, mode)
  }
  await fetchAiRecommendations(mode)
}

// ===== AI 问答 =====
const chatInput = ref('')
const chatMessages = ref([])
const chatLoading = ref(false)
const chatVisible = ref(false)

const quickQuestions = ['最近有什么适合我的活动？', '哪个活动最热门？', '周末有什么安排？', '帮我推荐一个学术类活动']

import { marked } from 'marked'

function renderMarkdown(text) {
  if (!text) return ''
  return marked(text, { breaks: true })
}

async function sendChat(question) {
  const q = (question || chatInput.value).trim()
  if (!q || chatLoading.value) return
  chatMessages.value.push({ role: 'user', content: q })
  chatInput.value = ''
  const history = chatMessages.value.length > 1 ? chatMessages.value.slice(0, -1) : []
  chatLoading.value = true
  try {
    const res = await aiChatApi.ask(q, null, history)
    chatMessages.value.push({ role: 'ai', content: res.data.answer, source: res.data.source })
  } catch (e) {
    chatMessages.value.push({ role: 'ai', content: '抱歉，AI 暂时无法回复，请稍后再试。', source: 'error' })
  } finally {
    chatLoading.value = false
    if (chatMessages.value.length > 10) chatMessages.value = chatMessages.value.slice(-10)
  }
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

function isRecommendationVisible() {
  return authStore.isLoggedIn && authStore.role === 'student'
}

function visibleFloatingNavItems() {
  return floatingNavItems.filter(item => item.id !== 'recommend' || isRecommendationVisible())
}

function getSectionElement(id) {
  const sectionMap = {
    top: heroSectionRef.value,
    featured: highlightSectionRef.value,
    recommend: aiPanelRef.value,
    square: activitySquareRef.value
  }
  return sectionMap[id] || null
}

function scrollToSection(id) {
  activeNavSection.value = id
  getSectionElement(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function updateActiveNavSection() {
  if (typeof window === 'undefined') {
    return
  }

  const sections = visibleFloatingNavItems()
    .map(item => ({ id: item.id, element: getSectionElement(item.id) }))
    .filter(item => item.element)

  if (sections.length === 0) {
    return
  }

  if (window.scrollY < 120) {
    activeNavSection.value = 'top'
    return
  }

  let currentSection = sections[0].id
  for (const section of sections) {
    const top = section.element.getBoundingClientRect().top
    if (top <= 180) {
      currentSection = section.id
    }
  }
  activeNavSection.value = currentSection
}

async function initHomePage() {
  await Promise.all([
    fetchCategories(),
    fetchNotices(),
    fetchStats(),
    fetchFeaturedActivities(),
    fetchCarouselActivities(),
    fetchUpcomingActivities(),
    fetchActivities()
  ])
  await fetchFreshActivities()
  await fetchAiRecommendations(recommendationMode.value)
}

onMounted(async () => {
  window.addEventListener('scroll', updateActiveNavSection, { passive: true })
  await initHomePage()
  await nextTick()
  updateActiveNavSection()
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', updateActiveNavSection)
})
</script>

<template>
  <div class="home-page">
    <!-- 轮播图 -->
    <section class="hero-carousel">
      <el-carousel height="420px" :interval="5000" arrow="always">
        <el-carousel-item v-for="item in carouselActivities" :key="item.id">
          <div class="carousel-slide" :style="{ backgroundImage: 'url(' + (item.coverImage || '') + ')' }">
            <div class="carousel-overlay">
              <div class="carousel-content">
                <span class="carousel-category">{{ item.categoryName }}</span>
                <h2 class="carousel-title" @click="goDetail(item.id)">{{ item.title }}</h2>
                <p class="carousel-desc">{{ item.description?.substring(0, 80) }}{{ item.description?.length > 80 ? '...' : '' }}</p>
                <div class="carousel-meta">
                  <span>📅 {{ formatDate(item.startTime) }}</span>
                  <span>📍 {{ item.location }}</span>
                  <span>👤 {{ item.currentParticipants }}/{{ item.maxParticipants }}</span>
                </div>
                <el-button type="primary" size="large" @click="goDetail(item.id)">立即查看</el-button>
              </div>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>

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

    <section ref="highlightSectionRef" class="highlight-grid">
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

    <section v-if="authStore.isLoggedIn && authStore.role === 'student'" ref="aiPanelRef" class="ai-panel">
      <div class="section-head">
        <div>
          <h3>为你推荐</h3>
          <p>
            {{ recommendationMode === 'ai'
              ? '已切换为 AI 推荐，会生成更细致的推荐说明，加载会稍慢一些'
              : '默认使用本地快速推荐，结合你的历史报名、签到和评价记录优先推荐更匹配的活动' }}
          </p>
        </div>
        <div class="recommendation-mode-switch" role="tablist" aria-label="推荐模式切换">
          <button
            type="button"
            class="mode-tab"
            :class="{ 'is-active': recommendationMode === 'local' }"
            @click="handleRecommendationModeChange('local')"
          >
            <span class="mode-tab-title">快速推荐</span>
            <span class="mode-tab-desc">本地秒开</span>
          </button>
          <button
            type="button"
            class="mode-tab"
            :class="{ 'is-active': recommendationMode === 'ai' }"
            @click="handleRecommendationModeChange('ai')"
          >
            <span class="mode-tab-title">AI 推荐</span>
            <span class="mode-tab-desc">文案更细</span>
          </button>
        </div>
      </div>

      <div>
        <!-- 骨架屏加载 -->
        <div v-if="aiLoading" class="ai-grid">
          <div v-for="i in 4" :key="'s'+i" class="ai-card ai-skeleton">
            <div class="skeleton-line skeleton-title"></div>
            <div class="skeleton-line skeleton-meta"></div>
            <div class="skeleton-line skeleton-text"></div>
            <div class="skeleton-line skeleton-text short"></div>
            <div class="skeleton-badge"></div>
          </div>
        </div>

        <el-empty v-if="!aiLoading && aiRecommendations.length === 0" description="当前暂无可推荐活动" />

        <div v-if="!aiLoading && aiRecommendations.length > 0" class="ai-grid">
          <div
            v-for="activity in aiRecommendations"
            :key="activity.id"
            class="ai-card"
            @click="goDetail(activity.id)"
          >
            <div class="ai-card-header">
              <div>
                <div class="ai-card-title">{{ activity.title }}</div>
                <div class="ai-card-meta">
                  {{ activity.categoryName }} · {{ formatTime(activity.startTime) }}
                </div>
              </div>
              <div class="ai-card-badges">
                <el-tag size="small" type="success" effect="plain">
                  {{ activity.tag || (recommendationMode === 'ai' ? 'AI推荐' : '快速推荐') }}
                </el-tag>
                <span class="ai-score">匹配度 {{ activity.score || 0 }}</span>
              </div>
            </div>

            <div class="ai-card-reason">{{ activity.reason }}</div>

            <div v-if="activity.analysis || activity.highlights?.length" class="ai-analysis-box">
              <div class="ai-analysis-header">
                <div class="ai-analysis-title">
                  {{ recommendationMode === 'ai' ? 'AI 分析：为什么推荐你' : '推荐依据' }}
                </div>
                <span
                  v-if="recommendationMode === 'ai' && activity.source"
                  class="ai-source-badge"
                  :class="activity.source === 'deepseek' ? 'source-deepseek' : 'source-fallback'"
                >
                  {{ activity.source === 'deepseek' ? 'DeepSeek生成' : '本地兜底' }}
                </span>
              </div>
              <div v-if="activity.analysis" class="ai-analysis-text">{{ activity.analysis }}</div>
              <div v-if="activity.highlights?.length" class="ai-highlights">
                <span
                  v-for="item in activity.highlights"
                  :key="item"
                  class="ai-highlight-chip"
                >
                  {{ item }}
                </span>
              </div>
            </div>

            <div class="ai-card-footer">
              <span><el-icon><Location /></el-icon>{{ activity.location }}</span>
              <span><el-icon><User /></el-icon>{{ activity.currentParticipants }}/{{ activity.maxParticipants }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- AI 问答 -->
      <div class="ai-chat-section">
        <div class="chat-messages" v-if="chatMessages.length > 0">
          <div v-for="(m, i) in chatMessages" :key="i" class="chat-msg" :class="m.role">
            <div class="chat-bubble" v-if="m.role === 'user'">{{ m.content }}</div>
            <div class="chat-bubble" v-else v-html="renderMarkdown(m.content)"></div>
          </div>
        </div>
        <div class="quick-questions">
          <el-button v-for="q in quickQuestions" :key="q" size="small" text @click="sendChat(q)">{{ q }}</el-button>
        </div>
        <div class="chat-input-row">
          <el-input v-model="chatInput" placeholder="问 AI 关于活动的问题..." @keyup.enter="sendChat()" :disabled="chatLoading" />
          <el-button type="primary" @click="sendChat()" :disabled="!chatInput.trim() || chatLoading">发送</el-button>
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

    <aside class="floating-nav" aria-label="首页分区导航">
      <button
        v-for="item in visibleFloatingNavItems()"
        :key="item.id"
        type="button"
        class="floating-nav-item"
        :class="{ 'is-active': activeNavSection === item.id }"
        @click="scrollToSection(item.id)"
      >
        <span class="floating-nav-dot" />
        <span class="floating-nav-label">{{ item.label }}</span>
      </button>
    </aside>

    <!-- AI 悬浮聊天按钮 -->
    <div class="ai-fab" @click="chatVisible = !chatVisible" v-if="authStore.isLoggedIn">
      <span v-if="!chatVisible">🤖</span>
      <span v-else>✕</span>
    </div>

    <!-- AI 聊天弹窗 -->
    <div class="ai-chat-dialog" v-if="chatVisible && authStore.isLoggedIn">
      <div class="ai-chat-header">
        <span>🤖 AI 活动助手</span>
        <span class="ai-chat-close" @click="chatVisible = false">✕</span>
      </div>
      <div class="ai-chat-body">
        <div v-if="chatMessages.length === 0" class="ai-chat-hint">
          <p>👋 你好！我是校园活动 AI 助手</p>
          <p style="font-size:12px;color:#909399">可以问我：最近有什么活动？哪个适合我？</p>
        </div>
        <div v-for="(m, i) in chatMessages" :key="i" class="ai-chat-msg" :class="m.role">
          <div class="ai-chat-bubble" v-if="m.role === 'user'">{{ m.content }}</div>
          <div class="ai-chat-bubble" v-else v-html="renderMarkdown(m.content)"></div>
        </div>
        <div v-if="chatLoading" class="ai-chat-msg ai"><div class="ai-chat-bubble">思考中...</div></div>
      </div>
      <div class="ai-chat-input">
        <input v-model="chatInput" placeholder="问 AI..." @keyup.enter="sendChat(); chatVisible=true" :disabled="chatLoading" />
        <button @click="sendChat(); chatVisible=true" :disabled="chatLoading" style="opacity:0.6;cursor:not-allowed">发送</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  max-width: 1280px;
  margin: 0 auto;
  position: relative;
  z-index: 0;
  padding: 0 12px 32px;
}

.home-page::before {
  content: '';
  position: fixed;
  top: 60px;
  right: 0;
  bottom: 0;
  left: 0;
  background:
    linear-gradient(rgba(246, 249, 255, 0.9), rgba(246, 249, 255, 0.9)),
    url('/home-background.jpg') center / cover no-repeat;
  z-index: -2;
  pointer-events: none;
}

.home-page::after {
  content: '';
  position: fixed;
  top: 60px;
  right: 0;
  bottom: 0;
  left: 0;
  background: radial-gradient(circle at top left, rgba(255, 255, 255, 0.18), transparent 42%);
  z-index: -1;
  pointer-events: none;
}

/* 轮播图 */
.hero-carousel { margin-bottom: 24px; }
.carousel-slide {
  height: 420px; background-size: cover; background-position: center;
  position: relative;
}
.carousel-overlay {
  position: absolute; inset: 0;
  background: linear-gradient(to right, rgba(0,0,0,0.7) 0%, rgba(0,0,0,0.2) 100%);
  display: flex; align-items: center; padding: 0 60px;
}
.carousel-content { color: #fff; max-width: 560px; }
.carousel-category {
  display: inline-block; background: rgba(255,255,255,0.2); color: #fff;
  padding: 4px 14px; border-radius: 20px; font-size: 13px; margin-bottom: 12px;
}
.carousel-title { font-size: 32px; font-weight: 700; margin: 8px 0; cursor: pointer; }
.carousel-title:hover { text-decoration: underline; }
.carousel-desc { font-size: 15px; opacity: 0.85; line-height: 1.6; margin-bottom: 12px; }
.carousel-meta { display: flex; gap: 20px; font-size: 13px; opacity: 0.8; margin-bottom: 16px; }

.hero-section {
  margin-bottom: 24px;
}

.hero-main {
  display: grid;
  grid-template-columns: minmax(0, 1.02fr) minmax(420px, 1.18fr);
  gap: 28px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.92) 0%, rgba(243, 248, 255, 0.88) 55%, rgba(237, 244, 255, 0.82) 100%);
  backdrop-filter: blur(16px);
  border-radius: 32px;
  padding: 38px;
  border: 1px solid rgba(214, 228, 248, 0.95);
  box-shadow: 0 26px 60px rgba(46, 81, 122, 0.14);
  position: relative;
  overflow: hidden;
}

.hero-main::before {
  content: '';
  position: absolute;
  top: -120px;
  right: -80px;
  width: 280px;
  height: 280px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(64, 158, 255, 0.22) 0%, rgba(64, 158, 255, 0) 72%);
  pointer-events: none;
}

.hero-main::after {
  content: '';
  position: absolute;
  left: 42%;
  bottom: -110px;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(117, 171, 255, 0.16) 0%, rgba(117, 171, 255, 0) 72%);
  pointer-events: none;
}

.hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  z-index: 1;
}

.hero-label {
  display: inline-block;
  width: fit-content;
  background: rgba(64, 158, 255, 0.12);
  color: #2168c9;
  border-radius: 999px;
  padding: 8px 16px;
  font-size: 12px;
  margin-bottom: 14px;
  font-weight: 600;
  box-shadow: 0 10px 24px rgba(64, 158, 255, 0.12);
}

.hero-eyebrow {
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #7f8ea3;
  margin-bottom: 14px;
}

.hero-copy h1 {
  font-size: 48px;
  line-height: 1.08;
  color: #162334;
  margin-bottom: 18px;
  max-width: 560px;
  letter-spacing: -0.02em;
}

.hero-copy p {
  color: #5d6b7d;
  line-height: 1.9;
  font-size: 15px;
  margin-bottom: 20px;
  max-width: 540px;
}

.hero-feature-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 24px;
}

.hero-feature-chip {
  display: inline-flex;
  align-items: center;
  padding: 9px 15px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(214, 228, 248, 0.92);
  color: #526276;
  font-size: 13px;
  box-shadow: 0 10px 22px rgba(47, 85, 131, 0.07);
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 26px;
}

.hero-data-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  max-width: 520px;
}

.hero-data-item {
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(220, 231, 247, 0.96);
  box-shadow: 0 14px 28px rgba(53, 86, 128, 0.08);
}

.hero-data-item strong {
  display: block;
  font-size: 24px;
  line-height: 1;
  color: #1e2d3d;
}

.hero-data-item span {
  display: block;
  margin-top: 8px;
  color: #7a8798;
  font-size: 12px;
}

.hero-showcase {
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 18px;
  position: relative;
  z-index: 1;
}

.hero-showcase-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.hero-showcase-kicker {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: linear-gradient(135deg, #2f7de1, #57a6ff);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  box-shadow: 0 14px 28px rgba(64, 158, 255, 0.22);
}

.hero-showcase-title {
  margin-top: 12px;
  color: #203047;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.25;
}

.hero-showcase-text {
  color: #7d8da2;
  font-size: 13px;
  max-width: 220px;
  text-align: right;
}

.hero-showcase-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.18fr) minmax(240px, 0.82fr);
  gap: 18px;
}

.hero-highlight {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 24px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid rgba(223, 234, 248, 0.95);
  box-shadow: 0 18px 36px rgba(43, 73, 111, 0.12);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.hero-highlight:hover {
  transform: translateY(-4px);
  box-shadow: 0 24px 44px rgba(43, 73, 111, 0.16);
}

.hero-highlight-cover {
  position: relative;
  height: 244px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.hero-highlight-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-highlight-body {
  padding: 22px;
}

.hero-highlight-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.hero-highlight-tag {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: #eef5ff;
  color: #327bdb;
  font-size: 12px;
  font-weight: 600;
}

.hero-highlight-time {
  color: #8b98aa;
  font-size: 12px;
}

.hero-highlight-title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 10px;
}

.hero-highlight-desc {
  color: #5d6c7f;
  line-height: 1.8;
  font-size: 14px;
  margin-bottom: 16px;
}

.hero-highlight-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  color: #7f8c9d;
  font-size: 13px;
}

.hero-highlight-meta span {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  border-radius: 14px;
  background: #f7faff;
}

.hero-side {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
  align-content: start;
}

.hero-showcase-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.hero-showcase-point {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(220, 231, 247, 0.92);
  color: #58697e;
  font-size: 13px;
  box-shadow: 0 10px 20px rgba(44, 72, 110, 0.06);
}

.hero-showcase-point::before {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3d8fff, #75b1ff);
  box-shadow: 0 0 0 5px rgba(64, 158, 255, 0.12);
}

.hero-mini-card {
  background: rgba(255, 255, 255, 0.92);
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid rgba(226, 235, 247, 0.95);
  box-shadow: 0 14px 28px rgba(44, 72, 110, 0.08);
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.hero-mini-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 18px 34px rgba(44, 72, 110, 0.12);
}

.hero-mini-cover {
  position: relative;
  height: 132px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  overflow: hidden;
}

.hero-mini-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-mini-body {
  padding: 16px 18px 18px;
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

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 16px;
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
.filter-panel,
.ai-panel {
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

/* AI骨架屏加载 */
.ai-skeleton {
  pointer-events: none; padding: 20px;
  background: linear-gradient(110deg, #eceff4 30%, #f8f9fc 50%, #eceff4 70%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
.skeleton-line { height: 14px; border-radius: 6px; background: #dce1e8; margin-bottom: 12px; }
.skeleton-title { width: 70%; height: 18px; }
.skeleton-meta { width: 50%; }
.skeleton-text { width: 90%; }
.skeleton-text.short { width: 60%; }
.skeleton-badge { width: 60px; height: 22px; border-radius: 11px; background: #dce1e8; margin-top: 8px; }

.ai-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

/* AI 问答聊天 */
.ai-chat-section { border-top: 1px solid #e4e7ed; padding-top: 20px; margin-top: 20px; }
.chat-messages { display: flex; flex-direction: column; gap: 12px; margin-bottom: 12px; max-height: 300px; overflow-y: auto; }
.chat-msg { display: flex; }
.chat-msg.user { justify-content: flex-end; }
.chat-msg.user .chat-bubble { background: #409eff; color: #fff; border-radius: 14px 14px 4px 14px; }
.chat-msg.ai .chat-bubble { background: #f0f2f5; color: #303133; border-radius: 14px 14px 14px 4px; }
.chat-bubble { max-width: 380px; padding: 10px 16px; font-size: 14px; line-height: 1.6; }
.quick-questions { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 12px; }
.chat-input-row { display: flex; gap: 8px; }
.chat-input-row .el-input { flex: 1; }

.recommendation-mode-switch {
  display: flex;
  gap: 8px;
  padding: 6px;
  border-radius: 16px;
  background: #f5f8ff;
  border: 1px solid #dfe9ff;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.mode-tab {
  min-width: 120px;
  padding: 10px 16px;
  border: 0;
  border-radius: 12px;
  background: transparent;
  color: #6b7a90;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  transition: all 0.2s ease;
}

.mode-tab:hover {
  background: rgba(255, 255, 255, 0.65);
  color: #3f4f67;
}

.mode-tab.is-active {
  background: linear-gradient(135deg, #409eff, #5aa8ff);
  color: #fff;
  box-shadow: 0 10px 22px rgba(64, 158, 255, 0.22);
}

.mode-tab-title {
  font-size: 14px;
  font-weight: 600;
}

.mode-tab-desc {
  font-size: 12px;
  opacity: 0.88;
}

.ai-card {
  border: 1px solid #e6f0ff;
  border-radius: 14px;
  padding: 18px;
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.ai-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 24px rgba(64, 158, 255, 0.08);
}

.ai-card-header {
  display: flex;
  justify-content: space-between;
  align-items: start;
  gap: 12px;
  margin-bottom: 12px;
}

.ai-card-badges {
  display: flex;
  flex-direction: column;
  align-items: end;
  gap: 8px;
  flex-shrink: 0;
}

.ai-card-title {
  font-size: 17px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.ai-card-meta {
  font-size: 13px;
  color: #909399;
}

.ai-card-reason {
  color: #4c5a67;
  line-height: 1.8;
  font-size: 14px;
  min-height: 74px;
  margin-bottom: 12px;
}

.ai-score {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 74px;
  padding: 4px 10px;
  border-radius: 999px;
  background: linear-gradient(135deg, #eef5ff, #f8fbff);
  color: #337ecc;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid #d7e8ff;
}

.ai-analysis-box {
  margin-bottom: 14px;
  padding: 12px 14px;
  border-radius: 14px;
  background: linear-gradient(180deg, #f7fbff, #f1f7ff);
  border: 1px solid #dbeafe;
}

.ai-analysis-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.ai-analysis-title {
  font-size: 13px;
  font-weight: 600;
  color: #3a6db1;
}

.ai-source-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.source-deepseek {
  color: #0f8f61;
  background: #e8fbf2;
  border: 1px solid #b7efd2;
}

.source-fallback {
  color: #8a6d1f;
  background: #fff7e6;
  border: 1px solid #f6df9b;
}

.ai-analysis-text {
  margin-bottom: 10px;
  color: #4f5f73;
  font-size: 13px;
  line-height: 1.75;
}

.ai-highlights {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.ai-highlight-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: #f3f8ff;
  color: #4a6fa5;
  font-size: 12px;
  border: 1px solid #dbeafe;
}

.ai-card-footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #7a8a99;
  font-size: 13px;
}

.ai-card-footer span {
  display: flex;
  align-items: center;
  gap: 4px;
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

/* AI 悬浮按钮 + 聊天弹窗 */
.ai-fab {
  position: fixed; bottom: 32px; right: 32px;
  width: 56px; height: 56px; border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #764ba2);
  color: #fff; font-size: 24px; display: flex; align-items: center; justify-content: center;
  cursor: pointer; z-index: 999;
  box-shadow: 0 4px 20px rgba(64,158,255,0.4);
  transition: transform 0.2s;
}
.ai-fab:hover { transform: scale(1.1); }

.ai-chat-dialog {
  position: fixed; bottom: 100px; right: 32px;
  width: 380px; height: 500px; z-index: 998;
  background: #fff; border-radius: 16px;
  box-shadow: 0 8px 40px rgba(0,0,0,0.15);
  display: flex; flex-direction: column;
}
.ai-chat-header {
  padding: 16px 20px; border-bottom: 1px solid #ebeef5;
  display: flex; justify-content: space-between; align-items: center;
  font-weight: 600; font-size: 15px;
}
.ai-chat-close { cursor: pointer; color: #909399; font-size: 18px; }
.ai-chat-body {
  flex: 1; overflow-y: auto; padding: 16px;
  display: flex; flex-direction: column; gap: 10px;
}
.ai-chat-hint { text-align: center; padding: 40px 16px; color: #606266; }
.ai-chat-msg { display: flex; }
.ai-chat-msg.user { justify-content: flex-end; }
.ai-chat-msg.user .ai-chat-bubble { background: #409eff; color: #fff; border-radius: 14px 14px 4px 14px; }
.ai-chat-msg.ai .ai-chat-bubble { background: #f0f2f5; color: #303133; border-radius: 14px 14px 14px 4px; }
.ai-chat-bubble { max-width: 280px; padding: 10px 14px; font-size: 13px; line-height: 1.6; }
.ai-chat-input {
  display: flex; gap: 8px; padding: 12px 16px; border-top: 1px solid #ebeef5;
}
.ai-chat-input input {
  flex: 1; border: 1px solid #dcdfe6; border-radius: 20px;
  padding: 8px 16px; font-size: 13px; outline: none;
}
.ai-chat-input input:focus { border-color: #409eff; }
.ai-chat-input button {
  background: #409eff; color: #fff; border: none;
  border-radius: 20px; padding: 8px 18px; font-size: 13px; cursor: pointer;
}

.floating-nav {
  position: fixed;
  right: 28px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.84);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(220, 232, 250, 0.9);
  box-shadow: 0 16px 34px rgba(31, 45, 61, 0.08);
  z-index: 20;
}

.floating-nav-item {
  min-width: 118px;
  padding: 10px 12px;
  border: 0;
  border-radius: 12px;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  color: #6f7f95;
  transition: all 0.2s ease;
  text-align: left;
}

.floating-nav-item:hover {
  background: rgba(240, 246, 255, 0.92);
  color: #3d5675;
}

.floating-nav-item.is-active {
  background: linear-gradient(135deg, #409eff, #5caeff);
  color: #fff;
  box-shadow: 0 10px 24px rgba(64, 158, 255, 0.24);
}

.floating-nav-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.72;
  flex-shrink: 0;
}

.floating-nav-item.is-active .floating-nav-dot {
  opacity: 1;
}

.floating-nav-label {
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

@media (max-width: 1200px) {
  .hero-main,
  .stats-grid,
  .notice-grid,
  .highlight-grid,
  .ai-grid,
  .activity-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .hero-main {
    grid-template-columns: 1fr;
  }

  .hero-showcase-grid {
    grid-template-columns: 1fr;
  }

  .hero-copy h1 {
    max-width: none;
  }

  .hero-data-strip {
    max-width: none;
  }

  .floating-nav {
    right: 14px;
  }
}

@media (max-width: 900px) {
  .hero-main,
  .stats-grid,
  .notice-grid,
  .highlight-grid,
  .ai-grid,
  .activity-grid {
    grid-template-columns: 1fr;
  }

  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .floating-nav {
    display: none;
  }

  .section-head {
    align-items: stretch;
    flex-direction: column;
  }

  .hero-showcase-head {
    flex-direction: column;
  }

  .hero-showcase-text {
    max-width: none;
    text-align: left;
  }

  .recommendation-mode-switch {
    width: fit-content;
  }

  .hero-copy h1 {
    font-size: 34px;
  }

  .hero-data-strip,
  .hero-side,
  .hero-highlight-meta {
    grid-template-columns: 1fr;
  }
}

</style>
