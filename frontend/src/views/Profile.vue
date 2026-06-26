<script setup>
import { computed, ref, onMounted } from 'vue'
import { userApi, activityApi, registrationApi, dashboardApi, uploadApi } from '@/api'
import { useAuthStore } from '@/store/auth'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()
const loading = ref(false)
const editing = ref(false)
const statsLoading = ref(false)
const profileStats = ref([])

const user = ref({
  realName: '',
  phone: '',
  email: '',
  avatar: ''
})

const formRef = ref(null)

const rules = {
  realName: [{ required: true, message: '姓名不能为空', trigger: 'blur' }],
  phone: [
    { required: true, message: '手机号不能为空', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

async function fetchUserInfo() {
  loading.value = true
  try {
    const res = await userApi.getUserInfo()
    user.value = {
      realName: res.data.realName,
      phone: res.data.phone,
      email: res.data.email || '',
      avatar: res.data.avatar || ''
    }
    // 同步更新 store
    if (res.data) {
      authStore.userInfo = {
        ...authStore.userInfo,
        realName: res.data.realName
      }
    }
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

const avatarUploading = ref(false)
const avatarFileInput = ref(null)

async function handleAvatarUpload(e) {
  const file = e.target.files[0]
  if (!file || file.size > 2 * 1024 * 1024) { ElMessage.warning('图片不能超过2MB'); return }
  avatarUploading.value = true
  const reader = new FileReader()
  reader.onload = async () => {
    try {
      const res = await uploadApi.uploadBase64(reader.result)
      user.value.avatar = res.data.url
      await userApi.updateProfile({ realName: user.value.realName, phone: user.value.phone, email: user.value.email, avatar: res.data.url })
      authStore.userInfo = { ...authStore.userInfo, avatar: res.data.url }
      ElMessage.success('头像更新成功')
    } catch (e) { /* ignore */ }
    finally { avatarUploading.value = false }
  }
  reader.readAsDataURL(file)
}

async function handleSave() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await userApi.updateProfile(user.value)
      ElMessage.success('保存成功')
      editing.value = false
    } catch (e) { /* ignore */ }
  })
}

const roleLabel = {
  student: '学生',
  organizer: '活动组织者',
  admin: '管理员'
}

const roleTagType = computed(() => {
  return authStore.role === 'admin' ? 'danger' : authStore.role === 'organizer' ? 'warning' : 'info'
})

async function fetchProfileStats() {
  statsLoading.value = true
  try {
    if (authStore.role === 'student') {
      const res = await registrationApi.getMyRegistrations()
      const registrations = res.data || []
      profileStats.value = [
        { label: '全部报名记录', value: registrations.length, sub: '当前账号产生的全部报名记录' },
        { label: '当前有效报名', value: registrations.filter(item => item.status === 'registered').length, sub: '尚未取消的报名记录' },
        { label: '已结束活动', value: registrations.filter(item => item.activityStatus === 'ended').length, sub: '已经结束、可回顾的活动' }
      ]
      return
    }

    if (authStore.role === 'organizer') {
      const res = await dashboardApi.getOrganizerStats()
      const stats = res.data || {}
      profileStats.value = [
        { label: '已发布活动', value: stats.totalActivities || 0, sub: '你创建的活动总数' },
        { label: '待审核', value: stats.statusStats?.pending || 0, sub: '等待管理员审核的活动' },
        { label: '进行中', value: stats.statusStats?.ongoing || 0, sub: '当前处于进行中的活动' },
        { label: '总报名人次', value: stats.totalRegistrations || 0, sub: '你的活动累计报名人次' }
      ]
      return
    }

    const res = await dashboardApi.getStats()
    const stats = res.data || {}
    profileStats.value = [
      { label: '平台活动', value: stats.totalActivities || 0, sub: '系统中的活动总数' },
      { label: '进行中', value: stats.ongoingActivities || 0, sub: '当前处于进行中的活动' },
      { label: '累计报名', value: stats.totalRegistrations || 0, sub: '平台已产生的报名记录' }
    ]
  } catch (e) {
    profileStats.value = []
  } finally {
    statsLoading.value = false
  }
}

onMounted(() => {
  fetchUserInfo()
  fetchProfileStats()
})
</script>

<template>
  <div class="page-card profile-page">
    <div class="profile-header">
      <div style="display:flex;align-items:center;gap:16px">
        <div class="avatar-section">
          <img v-if="user.avatar" :src="user.avatar" class="profile-avatar-img" />
          <div v-else class="profile-avatar-placeholder">{{ (authStore.userInfo?.realName || '?')[0] }}</div>
          <input type="file" accept="image/*" @change="handleAvatarUpload" style="display:none" ref="avatarFileInput" />
          <el-button size="small" :loading="avatarUploading" @click="avatarFileInput.click()" style="margin-top:8px">更换头像</el-button>
        </div>
        <div>
          <h2>👤 个人中心</h2>
        </div>
      </div>
      <el-tag :type="roleTagType" size="large">
        {{ roleLabel[authStore.role] || authStore.role }}
      </el-tag>
    </div>

    <div v-if="profileStats.length > 0" v-loading="statsLoading" class="stats-grid">
      <div v-for="(card, idx) in profileStats" :key="card.label" :class="['stat-card', ['stat-blue','stat-green','stat-orange','stat-purple'][idx]]">
        <div class="stat-value">{{ card.value }}</div>
        <div class="stat-label">{{ card.label }}</div>
        <div class="stat-sub">{{ card.sub }}</div>
      </div>
    </div>

    <div v-loading="loading" style="margin-top:24px">
      <!-- 基本信息展示 -->
      <el-descriptions :column="1" border v-if="!editing">
        <el-descriptions-item label="用户名">{{ authStore.userInfo?.username }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag :type="roleTagType">
            {{ roleLabel[authStore.role] || authStore.role }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="姓名">{{ user.realName }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ user.phone }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ user.email || '未填写' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 编辑模式 -->
      <el-form v-else ref="formRef" :model="user" :rules="rules" label-width="80px">
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="user.realName" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="user.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="user.email" placeholder="选填" />
        </el-form-item>
      </el-form>

      <div style="margin-top:20px">
    <el-button v-if="!editing" type="primary" @click="editing = true">编辑资料</el-button>
        <template v-else>
          <el-button type="primary" @click="handleSave">保存</el-button>
          <el-button @click="editing = false">取消</el-button>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 980px;
}

.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: start;
  gap: 16px;
}

.profile-header p {
  margin-top: 8px;
  color: #909399;
  line-height: 1.7;
}

@media (max-width: 900px) {
  .profile-stats {
    grid-template-columns: 1fr;
  }

  .profile-header {
    flex-direction: column;
    align-items: stretch;
  }
}

.avatar-section { display: flex; flex-direction: column; align-items: center; }
.profile-avatar-img { width: 72px; height: 72px; border-radius: 50%; object-fit: cover; border: 3px solid #e4e7ed; }
.profile-avatar-placeholder {
  width: 72px; height: 72px; border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff; display: flex; align-items: center; justify-content: center;
  font-size: 28px; font-weight: 700;
}

.notif-section { background:#fff; border-radius:8px; padding:16px; }
.notif-item { padding:12px 0; border-bottom:1px solid #ebeef5; }
.notif-item:last-child { border-bottom:none; }
.notif-item.unread { background:#f0f7ff; margin:0 -16px; padding:12px 16px; border-radius:6px; }
.notif-header { display:flex; justify-content:space-between; margin-bottom:4px; }
.notif-title { font-size:14px; font-weight:600; color:#303133; }
.notif-time { font-size:12px; color:#c0c4cc; }
.notif-content { font-size:13px; color:#606266; line-height:1.5; }
</style>
