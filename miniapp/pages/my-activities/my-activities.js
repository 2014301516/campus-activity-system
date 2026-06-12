const { getMyRegistrations, cancelRegister, signIn, signOut, getSignInStatus } = require('../../utils/request')
const app = getApp()

Page({
  data: {
    registrations: [],
    signInMap: {},    // { activityId: true, 'activityId-out': true, 'activityId-id': signInId }
    token: '',
    loading: false
  },

  onShow() {
    this.setData({ token: app.globalData.token })
    if (app.globalData.token) {
      this.fetchData()
    }
  },

  async fetchData() {
    this.setData({ loading: true })
    try {
      const data = await getMyRegistrations()
      this.setData({ registrations: data || [] })
      // 批量查签到状态
      const map = {}
      for (const r of data) {
        if (r.status !== 'registered' || !r.activityStartTime) continue
        try {
          const signInData = await getSignInStatus(r.activityId)
          if (signInData) {
            map[r.activityId] = true
            map[r.activityId + '-id'] = signInData.id
            if (signInData.signOutTime) {
              map[r.activityId + '-out'] = true
            }
          }
        } catch (e) {}
      }
      this.setData({ signInMap: map })
    } catch (e) {
      this.setData({ registrations: [] })
    } finally {
      this.setData({ loading: false })
    }
  },

  async handleCancel(e) {
    const id = e.currentTarget.dataset.id
    try { await cancelRegister(id); wx.showToast({ title: '已取消' }); this.fetchData() } catch (e) {}
  },
  async handleSignIn(e) {
    const id = e.currentTarget.dataset.id
    try { await signIn(id); wx.showToast({ title: '签到成功' }); this.fetchData() } catch (e) {}
  },
  async handleSignOut(e) {
    const signInId = e.currentTarget.dataset.id
    try { await signOut(signInId); wx.showToast({ title: '签退成功' }); this.fetchData() } catch (e) {}
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/detail/detail?id=' + id })
  }
})
