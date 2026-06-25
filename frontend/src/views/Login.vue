<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { userApi } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()

const isLogin = ref(true)
const loading = ref(false)

// 登录表单
const loginForm = reactive({
  username: '',
  password: ''
})

// 注册表单
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  studentId: '',
  phone: '',
  email: ''
})

const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: (rule, value, callback) => {
      if (value !== registerForm.password) { callback(new Error('两次密码不一致')) }
      else { callback() }
    }, trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  studentId: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

const loginFormRef = ref(null)
const registerFormRef = ref(null)

// 登录
async function handleLogin() {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await userApi.login(loginForm)
      authStore.setAuth(res.data)
      ElMessage.success('登录成功！')
      router.push('/home')
    } catch (e) {
      // 错误已在拦截器中处理
    } finally {
      loading.value = false
    }
  })
}

// 注册
async function handleRegister() {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userApi.register({
        username: registerForm.username,
        password: registerForm.password,
        realName: registerForm.realName,
        studentId: registerForm.studentId,
        phone: registerForm.phone,
        email: registerForm.email,
        role: 'student'
      })
      ElMessage.success('注册成功！请登录')
      isLogin.value = true
    } catch (e) {
      // 错误已在拦截器中处理
    } finally {
      loading.value = false
    }
  })
}

function toggleMode() {
  isLogin.value = !isLogin.value
}
</script>

<template>
  <div class="login-page">
    <!-- 背景装饰 -->
    <div class="bg-circles">
      <div class="circle c1"></div>
      <div class="circle c2"></div>
      <div class="circle c3"></div>
      <div class="circle c4"></div>
    </div>

    <!-- 登录卡片 -->
    <div class="login-card" v-if="isLogin">
      <div class="card-left">
        <div class="brand-area">
          <div class="brand-icon">🎪</div>
          <h2>校园活动管理</h2>
          <p>发现精彩校园生活</p>
        </div>
        <div class="brand-features">
          <div class="feat"><span>📋</span>活动发布与审核</div>
          <div class="feat"><span>✅</span>报名签到评价</div>
          <div class="feat"><span>📊</span>数据统计仪表盘</div>
          <div class="feat"><span>🤖</span>AI 智能助手</div>
        </div>
      </div>
      <div class="card-right">
        <h2 class="form-title">欢迎登录</h2>
        <p class="form-sub">使用账号密码登录系统</p>
        <el-form ref="loginFormRef" :model="loginForm" :rules="{ username: [{ required: true, message: '请输入用户名' }], password: [{ required: true, message: '请输入密码' }] }"
                 size="large" class="login-form">
          <el-form-item prop="username">
            <el-input v-model="loginForm.username" placeholder="用户名 / 学号" prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="Lock" @keyup.enter="handleLogin" show-password />
          </el-form-item>
          <el-button type="primary" :loading="loading" size="large" class="submit-btn" @click="handleLogin">登 录</el-button>
        </el-form>
        <div class="toggle-link">
          还没有账号？<el-link type="primary" @click="toggleMode">立即注册</el-link>
        </div>
      </div>
    </div>

    <!-- 注册卡片 -->
    <div class="login-card register-card" v-else>
      <div class="card-left">
        <div class="brand-area">
          <div class="brand-icon">🎪</div>
          <h2>加入我们</h2>
          <p>开启校园活动之旅</p>
        </div>
      </div>
      <div class="card-right">
        <h2 class="form-title">创建账号</h2>
        <p class="form-sub">填写信息完成注册</p>
        <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" size="large" class="login-form">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item prop="username"><el-input v-model="registerForm.username" placeholder="用户名" /></el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item prop="studentId"><el-input v-model="registerForm.studentId" placeholder="学号" /></el-form-item>
            </el-col>
          </el-row>
          <el-form-item prop="realName"><el-input v-model="registerForm.realName" placeholder="真实姓名" /></el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item prop="password"><el-input v-model="registerForm.password" type="password" placeholder="密码（至少6位）" show-password /></el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item prop="confirmPassword"><el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" show-password /></el-form-item>
            </el-col>
          </el-row>
          <el-form-item prop="phone"><el-input v-model="registerForm.phone" placeholder="手机号" /></el-form-item>
          <el-form-item prop="email"><el-input v-model="registerForm.email" placeholder="邮箱（选填）" /></el-form-item>
          <el-button type="primary" :loading="loading" size="large" class="submit-btn" @click="handleRegister">注 册</el-button>
        </el-form>
        <div class="toggle-link">
          已有账号？<el-link type="primary" @click="toggleMode">去登录</el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f1729 0%, #1a2744 50%, #1e3a5f 100%);
  position: relative;
  overflow: hidden;
}

/* 背景装饰圆 */
.bg-circles { position: absolute; inset: 0; pointer-events: none; }
.circle {
  position: absolute; border-radius: 50%;
  background: rgba(64,158,255,0.06);
  animation: float 20s infinite ease-in-out;
}
.c1 { width: 600px; height: 600px; top: -200px; right: -150px; animation-delay: 0s; }
.c2 { width: 400px; height: 400px; bottom: -100px; left: -100px; animation-delay: -7s; }
.c3 { width: 300px; height: 300px; top: 40%; right: 15%; animation-delay: -14s; background: rgba(103,194,58,0.05); }
.c4 { width: 200px; height: 200px; bottom: 20%; left: 30%; animation-delay: -3s; background: rgba(168,85,247,0.05); }
@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -30px) scale(1.05); }
  66% { transform: translate(-20px, 20px) scale(0.95); }
}

.login-card {
  position: relative; z-index: 1;
  width: 860px; min-height: 520px;
  background: rgba(255,255,255,0.95);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  box-shadow: 0 24px 80px rgba(0,0,0,0.3);
  display: flex; overflow: hidden;
}
.register-card { min-height: 560px; }

.card-left {
  width: 380px; flex-shrink: 0;
  background: linear-gradient(160deg, #1d4ed8 0%, #3b82f6 30%, #6366f1 70%, #8b5cf6 100%);
  color: #fff; padding: 48px 36px;
  display: flex; flex-direction: column; justify-content: space-between;
}
.brand-icon { font-size: 48px; margin-bottom: 16px; }
.brand-area h2 { font-size: 24px; font-weight: 700; margin-bottom: 8px; }
.brand-area p { font-size: 14px; opacity: 0.8; }
.brand-features { display: flex; flex-direction: column; gap: 14px; }
.feat { font-size: 14px; opacity: 0.85; display: flex; align-items: center; gap: 10px; }
.feat span { font-size: 18px; }

.card-right {
  flex: 1; padding: 48px 40px;
  display: flex; flex-direction: column; justify-content: center;
}
.form-title { font-size: 22px; font-weight: 700; color: #1e293b; margin-bottom: 4px; }
.form-sub { font-size: 13px; color: #94a3b8; margin-bottom: 24px; }
.submit-btn { width: 100%; height: 44px; font-size: 16px; letter-spacing: 2px; border-radius: 8px; margin-top: 4px; }
.toggle-link { text-align: center; margin-top: 16px; font-size: 13px; color: #94a3b8; }

.login-card .el-input__wrapper { border-radius: 8px; }
.login-card .el-form-item { margin-bottom: 18px; }
</style>
