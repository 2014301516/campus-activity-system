<script setup>
import { ref, onMounted } from 'vue'
import { adminApi, activityApi, categoryApi, noticeApi, dashboardApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

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

// ==================== 用户管理 ====================
const users = ref([])
const userTotal = ref(0)
const userPage = ref(1)
const userKeyword = ref('')
const userLoading = ref(false)

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
    const res = await activityApi.getList({ status: 'pending', size: 50 })
    pendingActivities.value = res.data.records || []
  } catch (e) { /* ignore */ }
  finally { auditLoading.value = false }
}

async function handleAudit(id, status) {
  try {
    await adminApi.auditActivity(id, status)
    ElMessage.success(status === 'approved' ? '已通过审核' : '已驳回')
    fetchPendingActivities()
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
    await ElMessageBox.confirm('确定删除该分类吗？', '确认', { type: 'warning' })
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

onMounted(() => {
  fetchStats()
  fetchUsers()
  fetchPendingActivities()
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
                <div class="stat-value">{{ stats?.statusStats?.pending || 0 }}</div>
                <div class="stat-label">待审核活动</div>
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
          <div style="display:flex;gap:24px">
            <div v-for="(count, status) in (stats?.statusStats || {})" :key="status" style="flex:1">
              <div style="display:flex;align-items:center;gap:8px">
                <span style="color:#606266;min-width:60px">{{ { draft:'草稿', pending:'待审核', approved:'已通过', ongoing:'进行中', ended:'已结束' }[status] }}</span>
                <el-progress :percentage="stats?.totalActivities ? Math.round(count / stats.totalActivities * 100) : 0" :color="'#409eff'" style="flex:1" />
                <span style="color:#909399;min-width:30px">{{ count }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 用户管理 -->
      <el-tab-pane label="用户管理" name="users">
        <div style="margin-bottom:12px;display:flex;gap:8px">
          <el-input v-model="userKeyword" placeholder="搜索用户名/姓名/学号" style="width:280px" clearable @clear="fetchUsers" @keyup.enter="fetchUsers" />
          <el-button type="primary" @click="fetchUsers">搜索</el-button>
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
          <el-empty v-if="pendingActivities.length === 0" description="暂无待审核活动" />
          <el-table v-else :data="pendingActivities" stripe>
            <el-table-column label="标题" prop="title" min-width="180" />
            <el-table-column label="分类" prop="categoryName" width="100" />
            <el-table-column label="组织者" prop="organizerName" width="100" />
            <el-table-column label="时间" min-width="200">
              <template #default="{ row }">{{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button size="small" type="success" @click="handleAudit(row.id, 'approved')">通过</el-button>
                <el-button size="small" type="danger" @click="handleAudit(row.id, 'pending')">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
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
  </div>
</template>

<style scoped>
.stat-card {
  border-radius: 10px;
  padding: 24px;
  color: #fff;
  text-align: center;
}
.stat-card .stat-value {
  font-size: 36px;
  font-weight: bold;
}
.stat-card .stat-label {
  font-size: 14px;
  margin-top: 4px;
  opacity: 0.9;
}
.stat-blue  { background: linear-gradient(135deg, #409eff, #337ecc); }
.stat-green { background: linear-gradient(135deg, #67c23a, #529b2e); }
.stat-orange { background: linear-gradient(135deg, #e6a23c, #cf9236); }
.stat-purple { background: linear-gradient(135deg, #a855f7, #9333ea); }
</style>
