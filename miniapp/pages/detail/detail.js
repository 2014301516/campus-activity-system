const { getActivityDetail, register, cancelRegister, signIn, signOut,
        getSignInStatus, submitReview, getActivityReviews, wxLogin } = require('../../utils/request')
const app = getApp()

Page({
  data: {
    activity: null,
    reviews: [],
    token: '',
    canRegister: false, canSignIn: false, canSignOut: false, canReview: false,
    signInStatus: '',       // '' / 'signed-in' / 'signed-out'
    signInId: null,
    showReviewDialog: false,
    reviewRating: 5,
    reviewComment: ''
  },

  onLoad(options) {
    const id = options.id
    this.activityId = id
    this.setData({ token: app.globalData.token })
    this.fetchDetail()
    this.fetchReviews()
    if (app.globalData.token) {
      this.fetchSignInStatus()
    }
  },

  // 登录
  doLoginAndRefresh() {
    wx.login({
      success: (res) => {
        wxLogin(res.code).then(data => {
          app.globalData.token = data.token
          app.globalData.userInfo = data
          wx.setStorageSync('token', data.token)
          wx.setStorageSync('userInfo', data)
          this.setData({ token: data.token })
          this.fetchSignInStatus()
          this.refreshButtons()
        })
      }
    })
  },

  async fetchDetail() {
    try {
      const data = await getActivityDetail(this.activityId)
      this.setData({ activity: data })
      this.refreshButtons()
    } catch (e) {}
  },

  async fetchReviews() {
    try {
      const data = await getActivityReviews(this.activityId)
      this.setData({ reviews: data || [] })
    } catch (e) {}
  },

  async fetchSignInStatus() {
    try {
      const data = await getSignInStatus(this.activityId)
      if (data) {
        if (data.signOutTime) {
          this.setData({ signInStatus: 'signed-out', signInId: data.id })
        } else {
          this.setData({ signInStatus: 'signed-in', signInId: data.id })
        }
      }
      this.refreshButtons()
    } catch (e) {}
  },

  // 根据活动状态和签到状态决定按钮显隐
  refreshButtons() {
    const { activity, signInStatus } = this.data
    if (!activity) return
    const s = activity.status

    this.setData({
      canRegister: (s === 'approved' || s === 'ongoing') && !!this.data.token,
      canSignIn: s === 'ongoing' && !signInStatus && !!this.data.token,
      canSignOut: s === 'ongoing' && signInStatus === 'signed-in',
      canReview: (s === 'ongoing' || s === 'ended') && !!this.data.token
    })
  },

  // 报名
  async handleRegister() {
    try { await register(this.activityId); wx.showToast({ title: '报名成功' }); this.fetchDetail() } catch (e) {}
  },
  async handleCancel() {
    try { await cancelRegister(this.activityId); wx.showToast({ title: '已取消' }); this.fetchDetail() } catch (e) {}
  },
  async handleSignIn() {
    try { await signIn(this.activityId); wx.showToast({ title: '签到成功' }); this.fetchSignInStatus() } catch (e) {}
  },
  async handleSignOut() {
    try { await signOut(this.data.signInId); wx.showToast({ title: '签退成功' }); this.fetchSignInStatus() } catch (e) {}
  },

  // 评价
  showReview() { this.setData({ showReviewDialog: true }) },
  hideReview() { this.setData({ showReviewDialog: false }) },
  setRating(e) { this.setData({ reviewRating: parseInt(e.currentTarget.dataset.r) }) },
  onReviewInput(e) { this.setData({ reviewComment: e.detail.value }) },
  async submitReview() {
    try {
      await submitReview(this.activityId, this.data.reviewRating, this.data.reviewComment)
      wx.showToast({ title: '评价成功' })
      this.hideReview()
      this.fetchReviews()
    } catch (e) {}
  },

  // 状态标签
  get statusTag() {
    const s = this.data.activity?.status
    const map = {
      approved: { type: 'success', text: '报名中' },
      ongoing:  { type: 'info',    text: '进行中' },
      ended:    { type: 'default', text: '已结束' },
      pending:  { type: 'warning', text: '待审核' },
      cancelled:{ type: 'danger',  text: '已取消' }
    }
    return map[s] || { type: 'default', text: s }
  }
})
