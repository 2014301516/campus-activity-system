<script setup>
import { useAuthStore } from './store/auth'
import { useRouter, useRoute } from 'vue-router'
import { computed } from 'vue'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const showLayout = computed(() => route.path !== '/login')

function handleLogout() {
  authStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<template>
  <div id="app-container">
    <!-- 非登录页显示导航栏 -->
    <el-container v-if="showLayout">
      <el-header class="app-header">
        <div class="header-left">
          <h2 @click="$router.push('/home')" style="cursor:pointer">🎪 校园活动管理</h2>
        </div>
        <div class="header-right">
          <el-menu mode="horizontal" :ellipsis="false" :router="true" class="header-menu">
            <el-menu-item index="/home">首页</el-menu-item>
            <el-menu-item v-if="authStore.role !== 'admin'" index="/my-activities">我的活动</el-menu-item>
            <el-menu-item v-if="authStore.role === 'organizer' || authStore.role === 'admin'" index="/manage">
              活动管理
            </el-menu-item>
            <el-menu-item v-if="authStore.role === 'admin'" index="/admin">
              后台管理
            </el-menu-item>
          </el-menu>
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
      <el-main>
        <router-view />
      </el-main>
    </el-container>

    <!-- 登录页不显示导航栏 -->
    <router-view v-else />
  </div>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
  background-color: #f5f7fa;
}

#app-container {
  min-height: 100vh;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
  padding: 0 40px;
  height: 60px !important;
}

.header-left h2 {
  color: #409eff;
  font-size: 18px;
  white-space: nowrap;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-menu {
  border-bottom: none !important;
}

.header-menu .el-menu-item {
  height: 60px;
  line-height: 60px;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.el-main {
  padding: 24px 40px;
  max-width: 1400px;
  margin: 0 auto;
}

/* 页面通用卡片样式 */
.page-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
</style>
