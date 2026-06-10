<script setup>
import { ref, onMounted } from 'vue'
import { userApi } from '@/api'
import { useAuthStore } from '@/store/auth'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()
const loading = ref(false)
const editing = ref(false)

const user = ref({
  realName: '',
  phone: '',
  email: ''
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
      email: res.data.email || ''
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

onMounted(fetchUserInfo)
</script>

<template>
  <div class="page-card" style="max-width:640px">
    <h2>👤 个人中心</h2>

    <div v-loading="loading" style="margin-top:24px">
      <!-- 基本信息展示 -->
      <el-descriptions :column="1" border v-if="!editing">
        <el-descriptions-item label="用户名">{{ authStore.userInfo?.username }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag :type="authStore.role === 'admin' ? 'danger' : authStore.role === 'organizer' ? 'warning' : 'info'">
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
