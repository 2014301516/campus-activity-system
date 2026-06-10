<script setup>
import { ref, onMounted } from 'vue'
import { activityApi, registrationApi, signInApi, categoryApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

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
    const res = await activityApi.getList({ size: 100 })
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
        ElMessage.success('更新成功')
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

// 删除
async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定要删除该活动吗？', '确认删除', { type: 'warning' })
    await activityApi.delete(id)
    ElMessage.success('删除成功')
    fetchActivities()
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
  const map = { draft: '草稿', pending: '待审核', approved: '已通过', ongoing: '进行中', ended: '已结束', cancelled: '已取消' }
  return map[s] || s
}

function formatTime(time) {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

onMounted(() => {
  fetchActivities()
  fetchCategories()
})
</script>

<template>
  <div class="page-card">
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
      <h2>📝 活动管理</h2>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 发布活动
      </el-button>
    </div>

    <div v-loading="loading">
      <el-empty v-if="!loading && activities.length === 0" description="暂无活动" />
      <el-table v-else :data="activities" stripe>
        <el-table-column label="标题" prop="title" min-width="180" />
        <el-table-column label="分类" prop="categoryName" width="100" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'approved' ? 'success' : row.status === 'pending' ? 'warning' : 'info'" size="small">
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
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)" :disabled="row.status === 'ended'">编辑</el-button>
            <el-button size="small" @click="viewRegistrations(row)">报名名单</el-button>
            <el-button size="small" @click="viewSignIns(row)">签到</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
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
      <el-table :data="registrations" stripe max-height="400">
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
    <el-dialog v-model="showSignDialog" :title="'签到记录 - ' + viewActivityTitle" width="560px">
      <el-table :data="signIns" stripe max-height="400">
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
