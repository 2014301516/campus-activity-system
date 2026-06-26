<script setup>
import { useAuthStore } from './store/auth'
import { useRouter, useRoute } from 'vue-router'
import { computed, ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'
import { notificationApi, aiChatApi } from './api'
import { marked } from 'marked'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const showLayout = computed(() => route.path !== '/login')
const unreadCount = ref(0)

// ===== 页面标签 =====
const titleMap = {
  Home: '首页', ActivityDetail: '活动详情', MyActivities: '我的报名',
  ActivityManage: '活动管理', Admin: '后台管理', Notifications: '消息通知', Profile: '个人中心'
}
const tabs = ref([{ path: '/home', title: '首页', closable: false }])
const activeTab = ref('/home')

watch(() => route.path, (path) => {
  fetchUnreadCount()
  if (path === '/login') return
  activeTab.value = path
  const name = route.name || 'Home'
  const title = titleMap[name] || name
  if (!tabs.value.find(t => t.path === path)) {
    tabs.value.push({ path, title, closable: true })
  }
})

function handleTabClick(tab) {
  router.push(tab.props.name)
}
function handleTabRemove(path) {
  const idx = tabs.value.findIndex(t => t.path === path)
  tabs.value.splice(idx, 1)
  if (activeTab.value === path && tabs.value.length > 0) {
    const last = tabs.value[Math.min(idx, tabs.value.length - 1)]
    router.push(last.path)
  }
}

async function fetchUnreadCount() {
  if (!authStore.isLoggedIn) return
  try { const res = await notificationApi.getUnreadCount(); unreadCount.value = res.data.count } catch (e) {}
}

// ===== AI 聊天（全局） =====
const chatVisible = ref(false)
const chatInput = ref('')
const chatMessages = ref([])
const chatLoading = ref(false)
const quickQuestions = computed(() => {
  const r = authStore.role
  const p = route.path
  if (r === 'student') {
    if (p.includes('/activity/')) return ['这个活动适合我吗？', '还有类似的活动吗？', '怎么报名？']
    if (p === '/my-activities') return ['我的签到状态？', '怎么取消报名？', '还有什么推荐？']
    return ['最近有什么活动？', '哪个活动最热门？', '帮我推荐活动', '周末有什么？']
  }
  if (r === 'organizer') {
    if (p === '/manage') return ['怎么提高报名人数？', '我的活动审核状态？', '怎么发布活动？']
    return ['怎么发布活动？', '如何管理报名？', '怎么看签到记录？']
  }
  if (r === 'admin') {
    if (p === '/admin') return ['有待审核的活动吗？', '系统运行情况怎么样？', '最近报名趋势如何？']
    return ['系统概况怎么样？', '有待审核的吗？', '用户活跃度如何？']
  }
  return ['最近有什么活动？', '哪个活动最热门？']
})

function renderMd(text) {
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
    const page = route.name ? route.name.charAt(0).toLowerCase() + route.name.slice(1) : 'home'
    const res = await aiChatApi.ask(q, null, history)
    chatMessages.value.push({ role: 'ai', content: res.data.answer, source: res.data.source })
  } catch (e) {
    chatMessages.value.push({ role: 'ai', content: '抱歉，AI 暂时无法回复。', source: 'error' })
  } finally {
    chatLoading.value = false
    if (chatMessages.value.length > 10) chatMessages.value = chatMessages.value.slice(-10)
  }
}

onMounted(() => {
  fetchUnreadCount()
  window.addEventListener('notification-read', fetchUnreadCount)
})

function handleLogout() {
  authStore.logout()
  tabs.value = [{ path: '/home', title: '首页', closable: false }]
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<template>
  <div id="app-container">
    <el-container v-if="showLayout">
      <el-header class="app-header">
        <div class="header-left">
          <h2 @click="$router.push('/home')" style="cursor:pointer">🎪 校园活动管理</h2>
        </div>
        <div class="header-right">
          <el-menu mode="horizontal" :ellipsis="false" :router="true" :default-active="route.path" class="header-menu">
            <el-menu-item index="/home">首页</el-menu-item>
            <el-menu-item v-if="authStore.role === 'student'" index="/my-activities">我的报名</el-menu-item>
            <el-menu-item v-if="authStore.role === 'organizer'" index="/manage">活动管理</el-menu-item>
            <el-menu-item v-if="authStore.role === 'admin'" index="/admin">后台管理</el-menu-item>
          </el-menu>
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="notify-badge">
            <el-button :icon="Bell" circle :type="route.path === '/notifications' ? 'primary' : 'default'" @click="$router.push('/notifications')" />
          </el-badge>
          <el-dropdown class="user-dropdown">
            <span class="user-info">
              <el-avatar :size="32" :src="authStore.userInfo?.avatar" icon="UserFilled" />
              <span style="margin-left:8px">{{ authStore.userInfo?.realName || '用户' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/profile')">个人中心</el-dropdown-item>
                <el-dropdown-item @click="handleLogout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 页面标签栏 -->
      <div class="tab-bar">
        <el-tabs v-model="activeTab" type="card" class="page-tabs" @tab-click="handleTabClick" @tab-remove="handleTabRemove">
          <el-tab-pane v-for="t in tabs" :key="t.path" :label="t.title" :name="t.path" :closable="t.closable" />
        </el-tabs>
      </div>

      <el-main>
        <router-view />
      </el-main>
    </el-container>

    <router-view v-else />

    <!-- AI 悬浮聊天（全局） -->
    <template v-if="showLayout">
      <div class="ai-fab" @click="chatVisible = !chatVisible">
        <span v-if="!chatVisible">🤖</span>
        <span v-else>✕</span>
      </div>
      <div class="ai-chat-dialog" v-if="chatVisible">
        <div class="ai-chat-header">
          <span>🤖 AI 活动助手</span>
          <span class="ai-chat-close" @click="chatVisible = false">✕</span>
        </div>
        <div class="ai-chat-body">
          <div v-if="chatMessages.length === 0" class="ai-chat-hint">
            <p>👋 你好！我是校园活动 AI 助手</p>
            <div class="quick-qs" style="display:flex;flex-wrap:wrap;gap:6px;margin-top:8px">
              <button v-for="q in quickQuestions" :key="q" class="qq-btn" @click="sendChat(q)">{{ q }}</button>
            </div>
          </div>
          <div v-for="(m, i) in chatMessages" :key="i" class="ai-chat-msg" :class="m.role">
            <div class="ai-chat-bubble" v-if="m.role === 'user'">{{ m.content }}</div>
            <div class="ai-chat-bubble" v-else v-html="renderMd(m.content)"></div>
          </div>
          <div v-if="chatLoading" class="ai-chat-msg ai"><div class="ai-chat-bubble">思考中...</div></div>
        </div>
        <div class="ai-chat-input">
          <input v-model="chatInput" placeholder="问 AI..." @keyup.enter="sendChat()" :disabled="chatLoading" />
          <button @click="sendChat()" :disabled="chatLoading">发送</button>
        </div>
      </div>
    </template>
  </div>
</template>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
  background-color: #f5f7fa;
}
#app-container { min-height: 100vh; }

.app-header {
  display: flex; align-items: center; justify-content: space-between;
  background: #fff; box-shadow: 0 1px 4px rgba(0,0,0,0.08);
  padding: 0 40px; height: 60px !important;
}
.header-left h2 { color: #409eff; font-size: 18px; white-space: nowrap; }
.header-right { display: flex; align-items: center; gap: 16px; }
.header-menu { border-bottom: none !important; }
.header-menu .el-menu-item { height: 60px; line-height: 60px; }
.user-info { display: flex; align-items: center; cursor: pointer; }
.notify-badge { margin-right: 8px; }

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
  width: 380px; height: 520px; z-index: 998;
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
.ai-chat-body { flex: 1; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 10px; }
.ai-chat-hint { text-align: center; padding: 20px 16px; color: #606266; }
.qq-btn { background: #f0f2f5; border: none; border-radius: 16px; padding: 6px 14px; font-size: 12px; color: #409eff; cursor: pointer; }
.ai-chat-msg { display: flex; }
.ai-chat-msg.user { justify-content: flex-end; }
.ai-chat-msg.user .ai-chat-bubble { background: #409eff; color: #fff; border-radius: 14px 14px 4px 14px; }
.ai-chat-msg.ai .ai-chat-bubble { background: #f0f2f5; color: #303133; border-radius: 14px 14px 14px 4px; }
.ai-chat-bubble { max-width: 280px; padding: 10px 14px; font-size: 13px; line-height: 1.6; word-break: break-word; }
.ai-chat-input { display: flex; gap: 8px; padding: 12px 16px; border-top: 1px solid #ebeef5; }
.ai-chat-input input { flex: 1; border: 1px solid #dcdfe6; border-radius: 20px; padding: 8px 16px; font-size: 13px; outline: none; }
.ai-chat-input input:focus { border-color: #409eff; }
.ai-chat-input button { background: #409eff; color: #fff; border: none; border-radius: 20px; padding: 8px 18px; font-size: 13px; cursor: pointer; }

/* 页面标签 */
.tab-bar {
  background: #fff; border-bottom: 1px solid #e4e7ed;
  padding: 0 24px;
}
.page-tabs { --el-tabs-header-height: 36px; }
.page-tabs .el-tabs__header { margin-bottom: 0; }
.page-tabs .el-tabs__nav { border: none; }
.page-tabs .el-tabs__item { height: 34px; line-height: 34px; font-size: 13px; border-radius: 6px 6px 0 0; }

.el-main { padding: 24px 40px; max-width: 1400px; margin: 0 auto; }
.page-card { background: #fff; border-radius: 8px; padding: 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }

/* 全局彩色统计卡片 */
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px; margin-bottom: 20px; }
.stat-card { border-radius: 22px; padding: 28px 24px; color: #fff; position: relative; overflow: hidden; box-shadow: 0 20px 40px rgba(44,72,110,0.14); }
.stat-card::after { content: ''; position: absolute; top: -24px; right: -24px; width: 96px; height: 96px; border-radius: 50%; background: rgba(255,255,255,0.12); }
.stat-value { font-size: 38px; font-weight: 700; letter-spacing: -0.5px; }
.stat-label { font-size: 15px; opacity: 0.9; margin-top: 6px; font-weight: 500; }
.stat-sub { font-size: 12px; opacity: 0.85; margin-top: 8px; }
.stat-blue  { background: linear-gradient(135deg, #3e8dff, #5aa9ff); }
.stat-green { background: linear-gradient(135deg, #32b67a, #57c68e); }
.stat-orange{ background: linear-gradient(135deg, #f0a43b, #f4ba5e); }
.stat-purple{ background: linear-gradient(135deg, #7d63ff, #9b7dff); }
</style>
