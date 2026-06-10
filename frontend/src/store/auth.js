import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const token = ref(localStorage.getItem('token') || '')

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => userInfo.value?.role || 'student')

  function setAuth(authData) {
    token.value = authData.token
    userInfo.value = {
      userId: authData.userId,
      username: authData.username,
      realName: authData.realName,
      role: authData.role
    }
    localStorage.setItem('token', authData.token)
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return { userInfo, token, isLoggedIn, role, setAuth, logout }
})
