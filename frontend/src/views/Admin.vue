<script setup>
import { computed, ref, reactive, onMounted } from 'vue'
import { adminApi, activityApi, categoryApi, noticeApi, dashboardApi, aiChatApi } from '@/api'
import { useAuthStore } from '@/store/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, BarChart, LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'

use([CanvasRenderer, PieChart, BarChart, LineChart, GridComponent, LegendComponent, TooltipComponent])

const authStore = useAuthStore()
const activeTab = ref('dashboard')

// ==================== 数据统计 ====================
const stats = ref(null)
const statsLoading = ref(false)

async function fetchStats() {
  statsLoading.value = true
  try {
    const res = await dashboardApi.getStats()
    stats.value = res.data
  } catch (e) { /* ignore */ }
  finally { statsLoading.value = false }
}

// ==================== 报名趋势 + 月度对比 ====================
const trendData = ref([])
const monthlyData = ref([])

async function fetchTrendData() {
  try { const res = await dashboardApi.getRegistrationTrend(); trendData.value = res.data || [] } catch (e) {}
}
async function fetchMonthlyData() {
  try { const res = await dashboardApi.getMonthlyActivities(); monthlyData.value = res.data || [] } catch (e) {}
}

const trendChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, top: 20, bottom: 30 },
  xAxis: {
    type: 'category',
    data: trendData.value.map(d => d.date),
    axisLabel: { fontSize: 11, rotate: 45 }
  },
  yAxis: { type: 'value', minInterval: 1 },
  series: [{
    data: trendData.value.map(d => d.count),
    type: 'line',
    smooth: true,
    areaStyle: { color: 'rgba(64,158,255,0.15)' },
    lineStyle: { color: '#409eff', width: 2 },
    itemStyle: { color: '#409eff' }
  }]
}))

const monthlyChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, top: 20, bottom: 30 },
  xAxis: {
    type: 'category',
    data: monthlyData.value.map(d => d.month),
    axisLabel: { fontSize: 11 }
  },
  yAxis: { type: 'value', minInterval: 1 },
  series: [{
    data: monthlyData.value.map(d => d.count),
    type: 'bar',
    barWidth: '50%',
    itemStyle: {
      color: '#409eff',
      borderRadius: [6, 6, 0, 0]
    }
  }]
}))

// ==================== 用户管理 ====================
const users = ref([])
const userTotal = ref(0)
const userPage = ref(1)
const userKeyword = ref('')
const userLoading = ref(false)

const showCreateUser = ref(false)
const createUserFormRef = ref(null)
const createUserForm = reactive({
  username: '', password: '', realName: '', studentId: '', phone: '', role: 'student'
})
const createUserRules = {
  username: [{ required: true, message: '请输入用户名' }],
  password: [{ required: true, min: 6, message: '密码至少6位' }],
  realName: [{ required: true, message: '请输入姓名' }],
  studentId: [{ required: true, message: '请输入学号' }],
  phone: [{ required: true, pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }],
  role: [{ required: true, message: '请选择角色' }]
}

async function handleCreateUser() {
  if (!createUserFormRef.value) return
  await createUserFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await adminApi.createUser({ ...createUserForm, email: '' })
      ElMessage.success('用户创建成功')
      showCreateUser.value = false
      createUserForm.username = ''; createUserForm.password = ''; createUserForm.realName = ''
      createUserForm.studentId = ''; createUserForm.phone = ''; createUserForm.role = 'student'
      fetchUsers()
    } catch (e) { /* ignore */ }
  })
}

async function fetchUsers() {
  userLoading.value = true
  try {
    const res = await adminApi.getUsers({ page: userPage.value, size: 10, keyword: userKeyword.value || undefined })
    users.value = res.data.records || []
    userTotal.value = res.data.total || 0
  } catch (e) { /* ignore */ }
  finally { userLoading.value = false }
}

async function toggleUserStatus(user) {
  const newStatus = user.status === 1 ? 0 : 1
  try {
    await adminApi.updateUserStatus(user.id, newStatus)
    ElMessage.success(`已${newStatus === 1 ? '启用' : '禁用'}用户`)
    fetchUsers()
  } catch (e) { /* ignore */ }
}

// ==================== 活动审核 ====================
const pendingActivities = ref([])
const auditLoading = ref(false)

async function fetchPendingActivities() {
  auditLoading.value = true
  try {
    const res = await activityApi.getList({ includeAll: true, size: 100 })
    pendingActivities.value = (res.data.records || []).filter(item =>
      item.status === 'pending' || item.status === 'cancel_pending'
    )
  } catch (e) { /* ignore */ }
  finally { auditLoading.value = false }
}

const aiSuggestionLoading = ref(null)

async function showAiSuggestion(row) {
  aiSuggestionLoading.value = row.id
  try {
    const res = await aiChatApi.getAuditSuggestion(row.id)
    ElMessageBox.alert(res.data.suggestion, 'AI 审核建议 - ' + row.title, { confirmButtonText: '知道了' })
  } catch (e) { /* ignore */ }
  finally { aiSuggestionLoading.value = null }
}

async function handleAudit(row, status) {
  try {
    let rejectReason = ''
    if (status === 'rejected') {
      const { value } = await ElMessageBox.prompt(
        row.status === 'cancel_pending'
          ? `请填写驳回“${row.title}”取消申请的理由`
          : `请填写驳回活动“${row.title}”的理由`,
        row.status === 'cancel_pending' ? '驳回取消申请' : '驳回活动',
        {
          type: 'warning',
          inputType: 'textarea',
          inputPlaceholder: '请填写清晰的驳回理由，组织者会看到这段说明',
          inputValidator: (inputValue) => {
            if (!inputValue || !inputValue.trim()) {
              return '请填写驳回理由'
            }
            if (inputValue.trim().length < 4) {
              return '驳回理由至少填写 4 个字'
            }
            return true
          }
        }
      )
      rejectReason = value.trim()
    }
    await adminApi.auditActivity(row.id, status, rejectReason)
    if (row.status === 'cancel_pending') {
      ElMessage.success(status === 'approved' ? '已通过取消申请' : '已驳回取消申请')
    } else {
      ElMessage.success(status === 'approved' ? '已通过审核' : '已驳回')
    }
    fetchStats()
    fetchPendingActivities()
    fetchAllActivities()
  } catch (e) { /* ignore */ }
}

// ==================== 全部活动 ====================
const allActivities = ref([])
const allActivityLoading = ref(false)
const allActivityStatus = ref('')
const allActivityPage = ref(1)
const allActivityPageSize = ref(10)
const allActivityTotal = ref(0)

async function fetchAllActivities() {
  allActivityLoading.value = true
  try {
    const params = {
      page: allActivityPage.value,
      size: allActivityPageSize.value,
      includeAll: true
    }
    if (allActivityStatus.value) params.status = allActivityStatus.value
    const res = await activityApi.getList(params)
    allActivities.value = res.data.records || []
    allActivityTotal.value = res.data.total || 0
  } catch (e) { /* ignore */ }
  finally { allActivityLoading.value = false }
}

function handleAllActivityStatusChange() {
  allActivityPage.value = 1
  fetchAllActivities()
}

function handleAllActivityPageChange(page) {
  allActivityPage.value = page
  fetchAllActivities()
}

function isModifyApply(row) {
  if (!row.createdAt || !row.updatedAt) return false
  return new Date(row.updatedAt).getTime() - new Date(row.createdAt).getTime() > 10000
}

function canCancelActivity(row) {
  return row.status !== 'ended' && row.status !== 'cancelled' && row.status !== 'cancel_pending'
}

async function handleCancelActivity(row) {
  try {
    await ElMessageBox.confirm(`确定将活动“${row.title}”标记为已取消吗？`, '确认', { type: 'warning' })
    await adminApi.cancelActivity(row.id)
    ElMessage.success('活动已取消')
    fetchStats()
    fetchPendingActivities()
    fetchAllActivities()
  } catch (e) { /* ignore */ }
}

// ==================== 分类管理 ====================
const categories = ref([])
const newCategoryName = ref('')
const newCategoryDesc = ref('')

async function fetchCategories() {
  try {
    const res = await categoryApi.getList()
    categories.value = res.data
  } catch (e) { /* ignore */ }
}

async function addCategory() {
  if (!newCategoryName.value.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  try {
    await adminApi.addCategory({ name: newCategoryName.value, description: newCategoryDesc.value })
    ElMessage.success('分类添加成功')
    newCategoryName.value = ''
    newCategoryDesc.value = ''
    fetchCategories()
  } catch (e) { /* ignore */ }
}

async function deleteCategory(id) {
  try {
    await ElMessageBox.confirm('确定删除该分类吗？如果已有活动正在使用该分类，将无法删除。', '确认', { type: 'warning' })
    await adminApi.deleteCategory(id)
    ElMessage.success('删除成功')
    fetchCategories()
  } catch (e) { /* ignore */ }
}

// ==================== 公告管理 ====================
const notices = ref([])
const noticeForm = ref({ title: '', content: '' })
const showNoticeDialog = ref(false)

async function fetchNotices() {
  try {
    const res = await noticeApi.getList({ page: 1, size: 20 })
    notices.value = res.data.records || []
  } catch (e) { /* ignore */ }
}

async function publishNotice() {
  if (!noticeForm.value.title.trim() || !noticeForm.value.content.trim()) {
    ElMessage.warning('请填写公告标题和内容')
    return
  }
  try {
    await adminApi.publishNotice(noticeForm.value)
    ElMessage.success('公告发布成功')
    showNoticeDialog.value = false
    noticeForm.value = { title: '', content: '' }
    fetchNotices()
  } catch (e) { /* ignore */ }
}

async function deleteNotice(id) {
  try {
    await ElMessageBox.confirm('确定删除该公告吗？', '确认', { type: 'warning' })
    await adminApi.deleteNotice(id)
    ElMessage.success('删除成功')
    fetchNotices()
  } catch (e) { /* ignore */ }
}

function formatTime(time) {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

function statusLabel(status) {
  const map = {
    draft: '草稿',
    pending: '待审核',
    approved: '已通过',
    rejected: '已驳回',
    ongoing: '进行中',
    ended: '已结束',
    cancel_pending: '取消待审核',
    cancelled: '已取消'
  }
  return map[status] || status
}

const statusColorMap = {
  draft: '#909399',
  pending: '#e6a23c',
  approved: '#67c23a',
  rejected: '#f56c6c',
  ongoing: '#409eff',
  ended: '#c0c4cc',
  cancel_pending: '#ebb563',
  cancelled: '#a8abb2'
}

const statusChartOption = computed(() => {
  const statusStats = stats.value?.statusStats || {}
  const data = Object.entries(statusStats)
    .filter(([, count]) => Number(count) > 0)
    .map(([status, count]) => ({
      value: count,
      name: statusLabel(status),
      itemStyle: { color: statusColorMap[status] || '#409eff' }
    }))

  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} 个 ({d}%)'
    },
    legend: {
      bottom: 0,
      left: 'center',
      itemWidth: 10,
      itemHeight: 10
    },
    series: [
      {
        type: 'pie',
        radius: ['45%', '72%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        label: {
          formatter: '{b}\n{c} 个'
        },
        labelLine: {
          length: 12,
          length2: 10
        },
        data
      }
    ]
  }
})

function statusTagType(status) {
  const map = {
    approved: 'success',
    pending: 'warning',
    rejected: 'danger',
    draft: 'info',
    ongoing: 'primary',
    ended: 'info',
    cancel_pending: 'warning',
    cancelled: 'info'
  }
  return map[status] || 'info'
}

function auditReasonText(row) {
  if (row.status === 'cancel_pending') {
    return row.cancelRequestReason || '未填写'
  }
  if (row.status === 'rejected') {
    return row.rejectReason || '未填写'
  }
  return '-'
}

onMounted(() => {
  fetchStats()
  fetchTrendData()
  fetchMonthlyData()
  fetchUsers()
  fetchPendingActivities()
  fetchAllActivities()
  fetchCategories()
  fetchNotices()
})
</script>

<template>
  <div class="page-card">
    <h2>⚙️ 后台管理</h2>

    <el-tabs v-model="activeTab" style="margin-top:16px">
      <!-- 数据统计 -->
      <el-tab-pane label="数据概览" name="dashboard">
        <div v-loading="statsLoading">
          <el-row :gutter="20" style="margin-bottom:24px">
            <el-col :span="6">
              <div class="stat-card stat-blue">
                <div class="stat-value">{{ stats?.totalActivities || 0 }}</div>
                <div class="stat-label">活动总数</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card stat-green">
                <div class="stat-value">{{ stats?.ongoingActivities || 0 }}</div>
                <div class="stat-label">进行中活动</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card stat-orange">
                <div class="stat-value">{{ stats?.totalUsers || 0 }}</div>
                <div class="stat-label">用户总数</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card stat-purple">
                <div class="stat-value">{{ stats?.totalRegistrations || 0 }}</div>
                <div class="stat-label">报名总数</div>
              </div>
            </el-col>
          </el-row>

          <!-- 分类统计 -->
          <h4 style="margin-bottom:12px">各分类活动数量</h4>
          <el-row :gutter="12">
            <el-col :span="4" v-for="cat in (stats?.categoryStats || [])" :key="cat.name" style="margin-bottom:12px">
              <div style="background:#f5f7fa;border-radius:8px;padding:16px;text-align:center">
                <div style="font-size:24px;font-weight:bold;color:#409eff">{{ cat.count }}</div>
                <div style="font-size:13px;color:#909399;margin-top:4px">{{ cat.name }}</div>
              </div>
            </el-col>
          </el-row>

          <!-- 状态分布 -->
          <h4 style="margin:20px 0 12px">活动状态分布</h4>
          <div class="status-chart-card">
            <v-chart
              :option="statusChartOption"
              :autoresize="true"
              class="status-chart"
            />
            <div class="status-summary">
              <div v-for="(count, status) in (stats?.statusStats || {})" :key="status" class="status-summary-item">
                <span class="status-dot" :style="{ backgroundColor: statusColorMap[status] || '#409eff' }"></span>
                <span class="status-name">{{ statusLabel(status) }}</span>
                <span class="status-count">{{ count }}</span>
              </div>
            </div>
          </div>

          <!-- 报名趋势 -->
          <h4 style="margin:24px 0 12px">近30天报名趋势</h4>
          <div class="chart-container">
            <v-chart :option="trendChartOption" :autoresize="true" style="height:260px" />
          </div>

          <!-- 月度活动对比 -->
          <h4 style="margin:24px 0 12px">月度活动数量</h4>
          <div class="chart-container">
            <v-chart :option="monthlyChartOption" :autoresize="true" style="height:220px" />
          </div>
        </div>
      </el-tab-pane>

      <!-- 用户管理 -->
      <el-tab-pane label="用户管理" name="users">
        <div style="margin-bottom:12px;display:flex;gap:8px">
          <el-input v-model="userKeyword" placeholder="搜索用户名/姓名/学号" style="width:280px" clearable @clear="fetchUsers" @keyup.enter="fetchUsers" />
          <el-button type="primary" @click="fetchUsers">搜索</el-button>
          <el-button type="success" @click="showCreateUser = true">添加用户</el-button>
        </div>
        <el-table :data="users" stripe v-loading="userLoading">
          <el-table-column label="用户名" prop="username" width="120" />
          <el-table-column label="姓名" prop="realName" width="100" />
          <el-table-column label="学号" prop="studentId" width="120" />
          <el-table-column label="手机号" prop="phone" width="130" />
          <el-table-column label="角色" width="100">
            <template #default="{ row }">
              <el-tag :type="row.role === 'admin' ? 'danger' : row.role === 'organizer' ? 'warning' : 'info'" size="small">
                {{ row.role === 'admin' ? '管理员' : row.role === 'organizer' ? '组织者' : '学生' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '正常' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="注册时间" width="160">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button size="small" :type="row.status === 1 ? 'danger' : 'success'"
                         :disabled="row.status === 1 && row.id === authStore.userInfo?.userId"
                         @click="toggleUserStatus(row)">
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination v-if="userTotal > 10" :total="userTotal" :page-size="10"
                       :current-page="userPage" @current-change="(p) => { userPage = p; fetchUsers() }"
                       layout="prev, pager, next" style="margin-top:16px;text-align:center" />
      </el-tab-pane>

      <!-- 活动审核 -->
      <el-tab-pane label="活动审核" name="audit">
        <div v-loading="auditLoading">
          <el-empty v-if="pendingActivities.length === 0" description="暂无待审核活动或取消申请" />
          <el-table v-else :data="pendingActivities" stripe>
            <el-table-column label="标题" min-width="180">
              <template #default="{ row }">
                <el-link type="primary" @click="$router.push('/activity/' + row.id)">{{ row.title }}</el-link>
              </template>
            </el-table-column>
            <el-table-column label="分类" prop="categoryName" width="100" />
            <el-table-column label="组织者" prop="organizerName" width="100" />
            <el-table-column label="审核类型" width="110">
              <template #default="{ row }">
                <el-tag :type="row.status === 'cancel_pending' ? 'warning' : isModifyApply(row) ? 'info' : 'info'" size="small">
                  {{ row.status === 'cancel_pending' ? '取消申请' : isModifyApply(row) ? '修改申请' : '发布申请' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="申请/驳回说明" min-width="220">
              <template #default="{ row }">
                <div class="audit-reason-cell">
                  {{ auditReasonText(row) }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="活动时间" min-width="200">
              <template #default="{ row }">{{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}</template>
            </el-table-column>
            <el-table-column label="最近更新" width="140">
              <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="300">
              <template #default="{ row }">
                <el-button size="small" type="success" @click="handleAudit(row, 'approved')">
                  {{ row.status === 'cancel_pending' ? '同意取消' : '通过' }}
                </el-button>
                <el-button size="small" type="danger" @click="handleAudit(row, 'rejected')">
                  {{ row.status === 'cancel_pending' ? '驳回申请' : '驳回' }}
                </el-button>
                <el-button size="small" type="warning" :loading="aiSuggestionLoading === row.id" @click="showAiSuggestion(row)">🤖 AI建议</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 全部活动 -->
      <el-tab-pane label="全部活动" name="allActivities">
        <div style="margin-bottom:12px">
          <el-select v-model="allActivityStatus" placeholder="按状态筛选" clearable style="width:180px" @change="handleAllActivityStatusChange" @clear="handleAllActivityStatusChange">
            <el-option label="草稿" value="draft" />
            <el-option label="待审核" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已驳回" value="rejected" />
            <el-option label="进行中" value="ongoing" />
            <el-option label="已结束" value="ended" />
            <el-option label="取消待审核" value="cancel_pending" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
        </div>
        <el-table :data="allActivities" stripe v-loading="allActivityLoading">
          <el-table-column label="ID" prop="id" width="60" />
          <el-table-column label="标题" prop="title" min-width="180" />
          <el-table-column label="分类" prop="categoryName" width="100" />
          <el-table-column label="组织者" prop="organizerName" width="100" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="报名" width="80">
            <template #default="{ row }">{{ row.currentParticipants }}/{{ row.maxParticipants }}</template>
          </el-table-column>
          <el-table-column label="时间" width="140">
            <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button
                v-if="canCancelActivity(row)"
                size="small"
                type="danger"
                plain
                @click="handleCancelActivity(row)"
              >
                取消活动
              </el-button>
              <span v-else style="color:#c0c4cc">-</span>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-if="allActivityTotal > allActivityPageSize"
          :total="allActivityTotal"
          :page-size="allActivityPageSize"
          :current-page="allActivityPage"
          @current-change="handleAllActivityPageChange"
          layout="prev, pager, next"
          style="margin-top:16px;text-align:center"
        />
      </el-tab-pane>

      <!-- 分类管理 -->
      <el-tab-pane label="分类管理" name="categories">
        <div style="margin-bottom:16px;display:flex;gap:8px">
          <el-input v-model="newCategoryName" placeholder="分类名称" style="width:180px" />
          <el-input v-model="newCategoryDesc" placeholder="分类描述" style="width:240px" />
          <el-button type="primary" @click="addCategory">添加分类</el-button>
        </div>
        <el-table :data="categories" stripe>
          <el-table-column label="ID" prop="id" width="80" />
          <el-table-column label="名称" prop="name" width="160" />
          <el-table-column label="描述" prop="description" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button size="small" type="danger" @click="deleteCategory(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 公告管理 -->
      <el-tab-pane label="公告管理" name="notices">
        <div style="margin-bottom:16px">
          <el-button type="primary" @click="showNoticeDialog = true">
            <el-icon><Plus /></el-icon> 发布公告
          </el-button>
        </div>
        <el-table :data="notices" stripe>
          <el-table-column label="标题" prop="title" min-width="200" />
          <el-table-column label="内容" prop="content" min-width="300">
            <template #default="{ row }">{{ row.content?.length > 100 ? row.content.substring(0, 100) + '...' : row.content }}</template>
          </el-table-column>
          <el-table-column label="发布时间" width="160">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button size="small" type="danger" @click="deleteNotice(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 发布公告对话框 -->
    <el-dialog v-model="showNoticeDialog" title="发布公告" width="500px">
      <el-form :model="noticeForm">
        <el-form-item label="标题">
          <el-input v-model="noticeForm.title" placeholder="公告标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="noticeForm.content" type="textarea" :rows="6" placeholder="公告内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showNoticeDialog = false">取消</el-button>
        <el-button type="primary" @click="publishNotice">发布</el-button>
      </template>
    </el-dialog>

    <!-- 添加用户对话框 -->
    <el-dialog v-model="showCreateUser" title="添加用户" width="460px">
      <el-form :model="createUserForm" :rules="createUserRules" ref="createUserFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createUserForm.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createUserForm.password" type="password" placeholder="至少6位" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="createUserForm.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="学号" prop="studentId">
          <el-input v-model="createUserForm.studentId" placeholder="学号" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="createUserForm.phone" placeholder="11位手机号" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="createUserForm.role" style="width:100%">
            <el-option label="学生" value="student" />
            <el-option label="组织者" value="organizer" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateUser = false">取消</el-button>
        <el-button type="primary" @click="handleCreateUser">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.status-chart-card {
  display: grid;
  grid-template-columns: minmax(320px, 1fr) minmax(220px, 280px);
  gap: 24px;
  align-items: center;
  padding: 20px 24px;
  background: #f8fafc;
  border: 1px solid #ebeef5;
  border-radius: 14px;
}

.status-chart {
  height: 340px;
}

.status-summary {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.status-summary-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #fff;
  border-radius: 10px;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-name {
  flex: 1;
  color: #606266;
}

.status-count {
  font-weight: 600;
  color: #303133;
}

.audit-reason-cell {
  font-size: 12px;
  line-height: 1.6;
  color: #606266;
}

.chart-container {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}


@media (max-width: 900px) {
  .status-chart-card {
    grid-template-columns: 1fr;
  }
}
</style>
