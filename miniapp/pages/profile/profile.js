const { getUserInfo } = require('../../utils/request')
const app = getApp()

Page({
  data: {
    userInfo: null,
    detail: {},
    token: '',
    roleLabel: ''
  },

  onShow() {
    const token = app.globalData.token
    const userInfo = app.globalData.userInfo
    this.setData({ token, userInfo })
    if (userInfo) {
      const map = { student: '学生', organizer: '活动组织者', admin: '管理员' }
      this.setData({ roleLabel: map[userInfo.role] || userInfo.role })
    }
    if (token) {
      this.fetchDetail()
    }
  },

  async fetchDetail() {
    try {
      const data = await getUserInfo()
      this.setData({ detail: data })
    } catch (e) {}
  },

  handleLogout() {
    wx.removeStorageSync('token')
    wx.removeStorageSync('userInfo')
    app.globalData.token = ''
    app.globalData.userInfo = null
    this.setData({ token: '', userInfo: null, detail: {} })
    wx.showToast({ title: '已退出' })
  }
})
