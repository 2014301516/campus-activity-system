import axios from 'axios'
import { ElMessage } from 'element-plus'

let isHandlingAuthFailure = false

const request = axios.create({
  baseURL: '/api',  // Vite proxy → localhost:8080
  timeout: 15000
})

// 请求拦截器 — 添加 Token
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器 — 统一错误处理
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      if (res.code === 401 || res.code === 403) {
        if (!isHandlingAuthFailure) {
          isHandlingAuthFailure = true
          ElMessage.error(res.message || '登录状态已失效，请重新登录')
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          if (window.location.hash !== '#/login') {
            window.location.hash = '#/login'
          }
          setTimeout(() => {
            isHandlingAuthFailure = false
          }, 800)
        }
        return Promise.reject(new Error(res.message || '登录状态已失效'))
      }
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  error => {
    const status = error.response?.status
    if (status === 401 || status === 403) {
      if (!isHandlingAuthFailure) {
        isHandlingAuthFailure = true
        ElMessage.error('登录状态已失效，请重新登录')
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        if (window.location.hash !== '#/login') {
          window.location.hash = '#/login'
        }
        setTimeout(() => {
          isHandlingAuthFailure = false
        }, 800)
      }
      return Promise.reject(error)
    }
    ElMessage.error('网络错误，请稍后重试')
    return Promise.reject(error)
  }
)

export default request
