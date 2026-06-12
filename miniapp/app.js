App({
  globalData: {
    token: '',
    userInfo: null,
    baseURL: 'https://your-ngrok-url.ngrok-free.app/api'  // 替换为 ngrok URL
  },

  onLaunch() {
    // 启动时尝试恢复登录状态
    const token = wx.getStorageSync('token')
    const userInfo = wx.getStorageSync('userInfo')
    if (token) {
      this.globalData.token = token
      this.globalData.userInfo = userInfo
    }
  }
})
