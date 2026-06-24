<script setup>
import { computed, ref, onMounted } from 'vue'
import { activityApi, registrationApi, signInApi, categoryApi, dashboardApi, aiChatApi, uploadApi } from '@/api'
import { useAuthStore } from '@/store/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
use([CanvasRenderer, BarChart, GridComponent, TooltipComponent])

const authStore = useAuthStore()
const loading = ref(false)
const activities = ref([])
const registrations = ref([])
const signIns = ref([])
const categories = ref([])

const showDialog = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const form = ref({
  id: null,
  title: '',
  description: '',
  categoryId: null,
  location: '',
  startTime: '',
  endTime: '',
  maxParticipants: 50,
  coverImage: ''
})

const formRules = {
  title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }],
  description: [{ required: true, message: '请输入活动描述', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  location: [{ required: true, message: '请输入活动地点', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  maxParticipants: [{ required: true, message: '请输入最大报名人数', trigger: 'blur' }]
}

// 当前查看的活动ID（用于报名和签到详情）
const viewActivityId = ref(null)
const viewActivityTitle = ref('')
const showRegDialog = ref(false)
const showSignDialog = ref(false)

async function fetchActivities() {
  loading.value = true
  try {
    const res = await activityApi.getList({
      size: 100,
      organizerId: authStore.userInfo?.userId
    })
    activities.value = res.data.records || []
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

async function fetchCategories() {
  try {
    const res = await categoryApi.getList()
    categories.value = res.data
  } catch (e) { /* ignore */ }
}

// 打开创建对话框
function openCreate() {
  isEdit.value = false
  form.value = { id: null, title: '', description: '', categoryId: null, location: '', startTime: '', endTime: '', maxParticipants: 50, coverImage: '' }
  showDialog.value = true
}

// 打开编辑对话框
function openEdit(activity) {
  isEdit.value = true
  form.value = {
    id: activity.id,
    title: activity.title,
    description: activity.description,
    categoryId: activity.categoryId,
    location: activity.location,
    startTime: activity.startTime,
    endTime: activity.endTime,
    maxParticipants: activity.maxParticipants,
    coverImage: activity.coverImage || ''
  }
  showDialog.value = true
}

// 提交
async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) {
        await activityApi.update(form.value.id, form.value)
        ElMessage.success('已提交修改，等待管理员重新审核')
      } else {
        await activityApi.create(form.value)
        ElMessage.success('创建成功')
      }
      showDialog.value = false
      fetchActivities()
    } catch (e) {
      // 拦截器已显示错误消息
    }
  })
}

function canRequestCancel(activity) {
  return activity.status === 'approved' || activity.status === 'ongoing'
}

// 申请取消活动
async function handleRequestCancel(activity) {
  try {
    const { value } = await ElMessageBox.prompt(
      `请填写活动“${activity.title}”的取消申请理由，提交后需要管理员审核。`,
      '申请取消活动',
      {
        type: 'warning',
        inputType: 'textarea',
        inputPlaceholder: '例如：场地临时不可用、活动时间冲突、组织安排调整等',
        inputValidator: (inputValue) => {
          if (!inputValue || !inputValue.trim()) {
            return '请填写取消申请理由'
          }
          if (inputValue.trim().length < 4) {
            return '申请理由至少填写 4 个字'
          }
          return true
        }
      }
    )
    await activityApi.requestCancel(activity.id, value.trim())
    ElMessage.success('已提交取消申请，等待管理员审核')
    fetchActivities()
    fetchOrganizerStats()
  } catch (e) { /* ignore */ }
}

// 查看报名名单
async function viewRegistrations(activity) {
  viewActivityId.value = activity.id
  viewActivityTitle.value = activity.title
  try {
    const res = await registrationApi.getActivityRegistrations(activity.id)
    registrations.value = res.data || []
  } catch (e) { registrations.value = [] }
  showRegDialog.value = true
}

// 查看签到记录
async function viewSignIns(activity) {
  viewActivityId.value = activity.id
  viewActivityTitle.value = activity.title
  try {
    const res = await signInApi.getActivitySignIns(activity.id)
    signIns.value = res.data || []
  } catch (e) { signIns.value = [] }
  showSignDialog.value = true
}

// 状态标签
function statusLabel(s) {
  const map = { draft: '草稿', pending: '待审核', approved: '已通过', rejected: '已驳回', ongoing: '进行中', ended: '已结束', cancel_pending: '取消待审核', cancelled: '已取消' }
  return map[s] || s
}

function statusTagType(status) {
  const map = {
    approved: 'success',
    pending: 'warning',
    rejected: 'danger',
    ongoing: 'primary',
    ended: 'info',
    cancel_pending: 'warning',
    cancelled: 'info',
    draft: 'info'
  }
  return map[status] || 'info'
}

function exportExcel(activityId) {
  window.open('/api/activity/' + activityId + '/registrations/export')
}

const coverUploading = ref(false)
const coverFileInput = ref(null)
function handleCoverUpload(res) {
  if (res.code === 200) form.value.coverImage = res.data.url
}
async function handleCoverFile(e) {
  const file = e.target.files[0]
  if (!file || file.size > 5 * 1024 * 1024) { ElMessage.warning('图片不能超过5MB'); return }
  coverUploading.value = true
  const reader = new FileReader()
  reader.onload = async () => {
    try {
      const res = await uploadApi.uploadBase64(reader.result)
      form.value.coverImage = res.data.url
      ElMessage.success('上传成功')
    } catch (err) { /* ignore */ }
    finally { coverUploading.value = false }
  }
  reader.readAsDataURL(file)
}

const aiGenLoading = ref(false)
async function generateDescription() {
  if (!form.value.title || !form.value.categoryId) return
  aiGenLoading.value = true
  try {
    const res = await aiChatApi.generateDescription(form.value.title, form.value.categoryId)
    form.value.description = res.data.description
    formRef.value?.clearValidate('description')
  } catch (e) { /* ignore */ }
  finally { aiGenLoading.value = false }
}

function formatTime(time) {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

function latestReason(activity) {
  if (activity.status === 'cancel_pending' && activity.cancelRequestReason) {
    return `申请理由：${activity.cancelRequestReason}`
  }
  if (activity.rejectReason) {
    return `驳回理由：${activity.rejectReason}`
  }
  return ''
}

// ===== 组织者统计 =====
const organizerStats = ref(null)

async function fetchOrganizerStats() {
  try { const res = await dashboardApi.getOrganizerStats(); organizerStats.value = res.data } catch (e) {}
}

const topActivityChartOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: 100, right: 20, top: 10, bottom: 20 },
  xAxis: { type: 'value', minInterval: 1 },
  yAxis: { type: 'category', data: (organizerStats.value?.topActivities || []).map(a => a.title).reverse(),
           axisLabel: { fontSize: 11 } },
  series: [{
    data: (organizerStats.value?.topActivities || []).map(a => a.count).reverse(),
    type: 'bar',
    barWidth: '60%',
    itemStyle: { color: '#409eff', borderRadius: [0, 6, 6, 0] }
  }]
}))

onMounted(() => {
  fetchActivities()
  fetchCategories()
  fetchOrganizerStats()
})
</script>

<template>
  <div class="page-card">
    <div class="manage-header">
      <div>
        <h2>📝 活动管理</h2>
        <p>在这里集中维护你发布的活动，查看报名和签到情况，并及时调整活动安排。</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 发布活动
      </el-button>
    </div>

    <!-- 组织者数据概览 -->
    <div class="org-stats-cards" v-if="organizerStats">
      <div class="org-stat blue">
        <div class="org-stat-value">{{ organizerStats.totalActivities }}</div>
        <div class="org-stat-label">已发布活动</div>
      </div>
      <div class="org-stat orange">
        <div class="org-stat-value">{{ (organizerStats.statusStats?.pending || 0) + (organizerStats.statusStats?.cancel_pending || 0) }}</div>
        <div class="org-stat-label">待审核</div>
      </div>
      <div class="org-stat green">
        <div class="org-stat-value">{{ organizerStats.statusStats?.ongoing || 0 }}</div>
        <div class="org-stat-label">进行中</div>
      </div>
      <div class="org-stat purple">
        <div class="org-stat-value">{{ organizerStats.totalRegistrations }}</div>
        <div class="org-stat-label">总报名人次</div>
      </div>
    </div>

    <!-- 报名排行 -->
    <div class="org-chart-wrap" v-if="organizerStats && organizerStats.topActivities?.length">
      <h4 style="margin-bottom:8px;font-size:14px;color:#606266">活动报名排行</h4>
      <v-chart :option="topActivityChartOption" :autoresize="true" style="height:260px" />
    </div>

    <div v-loading="loading">
      <el-empty v-if="!loading && activities.length === 0" description="你还没有创建活动，点击右上角“发布活动”开始准备演示数据。" />
        <el-table v-else :data="activities" stripe>
          <el-table-column label="标题" min-width="220">
            <template #default="{ row }">
              <div class="activity-title-cell">
                <div class="activity-title-text">{{ row.title }}</div>
                <div v-if="latestReason(row)" class="activity-reason-text">
                  {{ latestReason(row) }}
                </div>
              </div>
            </template>
          </el-table-column>
        <el-table-column label="分类" prop="categoryName" width="100" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="报名" width="80">
          <template #default="{ row }">{{ row.currentParticipants }}/{{ row.maxParticipants }}</template>
        </el-table-column>
        <el-table-column label="开始时间" width="140">
          <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="390">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" @click="openEdit(row)" :disabled="row.status === 'ended' || row.status === 'cancelled' || row.status === 'cancel_pending'">编辑</el-button>
              <el-button size="small" @click="viewRegistrations(row)">报名名单</el-button>
              <el-button size="small" @click="viewSignIns(row)">签到签退记录</el-button>
              <el-button
                size="small"
                type="danger"
                plain
                :disabled="row.status === 'cancel_pending' || !canRequestCancel(row)"
                @click="handleRequestCancel(row)"
              >
                {{ row.status === 'cancel_pending' ? '取消审核中' : '申请取消' }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 创建/编辑对话框 -->
    <el-dialog v-model="showDialog" :title="isEdit ? '编辑活动' : '发布活动'" width="640px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="活动标题" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width:100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="活动详细描述" />
          <el-button size="small" style="margin-top:6px" :disabled="!form.title || !form.categoryId" :loading="aiGenLoading" @click="generateDescription">
            ✨ AI 生成描述
          </el-button>
        </el-form-item>
        <el-form-item label="封面图">
          <el-input v-model="form.coverImage" placeholder="输入URL或点击上传" clearable />
          <input type="file" accept="image/*" @change="handleCoverFile" style="display:none" ref="coverFileInput" />
          <div style="display:flex;align-items:center;gap:8px;margin-top:6px">
            <el-button size="small" :loading="coverUploading" @click="coverFileInput.click()">📷 本地上传</el-button>
          </div>
          <div v-if="form.coverImage" style="margin-top:12px">
            <el-image :src="form.coverImage" fit="cover" style="width:260px;height:140px;border-radius:8px;border:1px solid #ebeef5" />
          </div>
        </el-form-item>
        <el-form-item label="地点" prop="location">
          <el-input v-model="form.location" placeholder="活动地点" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择结束时间" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="最大人数" prop="maxParticipants">
          <el-input-number v-model="form.maxParticipants" :min="1" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">{{ isEdit ? '保存修改' : '提交审核' }}</el-button>
      </template>
    </el-dialog>

    <!-- 报名名单对话框 -->
    <el-dialog v-model="showRegDialog" :title="'报名名单 - ' + viewActivityTitle" width="560px">
      <div v-if="registrations.length > 0" style="margin-bottom:12px;text-align:right">
        <el-button size="small" type="success" @click="exportExcel(viewActivityId)">📥 导出Excel</el-button>
      </div>
      <el-empty v-if="registrations.length === 0" description="当前还没有学生报名这场活动。" />
      <el-table v-else :data="registrations" stripe max-height="400">
        <el-table-column label="姓名" prop="userName" />
        <el-table-column label="报名时间" width="160">
          <template #default="{ row }">{{ formatTime(row.registeredAt) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'registered' ? 'success' : 'info'" size="small">
              {{ row.status === 'registered' ? '已报名' : '已取消' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 签到记录对话框 -->
    <el-dialog v-model="showSignDialog" :title="'签到签退记录 - ' + viewActivityTitle" width="560px">
      <el-empty v-if="signIns.length === 0" description="当前还没有签到记录，活动开始后再回来查看。" />
      <el-table v-else :data="signIns" stripe max-height="400">
        <el-table-column label="姓名" prop="userName" />
        <el-table-column label="签到时间" width="160">
          <template #default="{ row }">{{ formatTime(row.signInTime) }}</template>
        </el-table-column>
        <el-table-column label="签退时间" width="160">
          <template #default="{ row }">{{ formatTime(row.signOutTime) || '未签退' }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<style scoped>
.manage-header {
  display: flex;
  justify-content: space-between;
  align-items: start;
  gap: 16px;
  margin-bottom: 20px;
}

.manage-header p {
  margin-top: 8px;
  color: #909399;
  line-height: 1.7;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.activity-title-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.activity-title-text {
  color: #303133;
}

.activity-reason-text {
  font-size: 12px;
  line-height: 1.5;
  color: #909399;
}

@media (max-width: 768px) {
  .manage-header {
    flex-direction: column;
    align-items: stretch;
  }
}

/* 组织者统计 */
.org-stats-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}
.org-stat {
  flex: 1;
  border-radius: 10px;
  padding: 20px 16px;
  color: #fff;
  text-align: center;
}
.org-stat .org-stat-value { font-size: 28px; font-weight: 700; }
.org-stat .org-stat-label { font-size: 13px; opacity: 0.85; margin-top: 4px; }
.org-stat.blue   { background: linear-gradient(135deg, #409eff, #337ecc); }
.org-stat.orange { background: linear-gradient(135deg, #e6a23c, #cf9236); }
.org-stat.green  { background: linear-gradient(135deg, #67c23a, #529b2e); }
.org-stat.purple { background: linear-gradient(135deg, #a855f7, #9333ea); }
.org-chart-wrap {
  background: #fafafa;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
}
</style>
