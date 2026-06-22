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
  chatLoading.value = true
  try {
    const res = await aiChatApi.ask(q)
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
    <section ref="heroSectionRef" class="hero-section">
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

      <div v-loading="aiLoading">
        <el-empty v-if="!aiLoading && aiRecommendations.length === 0" description="当前暂无可推荐活动" />

        <div v-else class="ai-grid">
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
          <el-button type="primary" :loading="chatLoading" @click="sendChat()" :disabled="!chatInput.trim()">发送</el-button>
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
  </div>
</template>

<style scoped>
.home-page {
  max-width: 1280px;
  margin: 0 auto;
  position: relative;
  z-index: 0;
}

.home-page::before {
  content: '';
  position: fixed;
  top: 60px;
  right: 0;
  bottom: 0;
  left: 0;
  background:
    linear-gradient(rgba(245, 248, 255, 0.78), rgba(245, 248, 255, 0.78)),
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
  .hero-section,
  .hero-main,
  .stats-grid,
  .notice-grid,
  .highlight-grid,
  .ai-grid,
  .activity-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .floating-nav {
    right: 14px;
  }
}

@media (max-width: 900px) {
  .hero-section,
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

  .recommendation-mode-switch {
    width: fit-content;
  }
}
</style>
