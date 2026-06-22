<script setup>
import { useAuthStore } from './store/auth'
import { useRouter, useRoute } from 'vue-router'
import { computed, ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'
import { notificationApi } from './api'

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

onMounted(() => { fetchUnreadCount() })

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
            <el-button :icon="Bell" circle @click="$router.push('/notifications')" />
          </el-badge>
          <el-dropdown class="user-dropdown">
            <span class="user-info">
              <el-avatar :size="32" icon="UserFilled" />
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
</style>
