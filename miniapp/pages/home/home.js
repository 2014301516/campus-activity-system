const { getActivityList, getCategories } = require('../../utils/request')
const app = getApp()

Page({
  data: {
    activities: [],
    categories: [],
    keyword: '',
    categoryId: null,
    page: 1,
    hasMore: true,
    loading: false
  },

  onLoad() {
    this.fetchCategories()
    this.fetchActivities()
  },

  onShow() {
    // 每次回到首页刷新（可能从详情页报名回来）
    this.setData({ page: 1, hasMore: true })
    this.fetchActivities()
  },

  onPullDownRefresh() {
    this.setData({ page: 1, hasMore: true })
    this.fetchActivities().then(() => wx.stopPullDownRefresh())
  },

  // 登录
  doLogin(callback) {
    if (app.globalData.token) {
      callback && callback()
      return
    }
    wx.login({
      success: (res) => {
        if (res.code) {
          const { wxLogin } = require('../../utils/request')
          wxLogin(res.code).then(data => {
            app.globalData.token = data.token
            app.globalData.userInfo = data
            wx.setStorageSync('token', data.token)
            wx.setStorageSync('userInfo', data)
            callback && callback()
          }).catch(() => {
            wx.showToast({ title: '登录失败', icon: 'none' })
          })
        }
      }
    })
  },

  // 获取分类
  async fetchCategories() {
    try {
      const data = await getCategories()
      this.setData({ categories: data })
    } catch (e) {}
  },

  // 获取活动列表
  async fetchActivities() {
    this.setData({ loading: true })
    try {
      const data = await getActivityList({
        page: this.data.page,
        size: 8,
        keyword: this.data.keyword,
        categoryId: this.data.categoryId
      })
      const list = this.data.page === 1 ? data.records : [...this.data.activities, ...data.records]
      this.setData({
        activities: list,
        hasMore: list.length < data.total
      })
    } catch (e) {
      this.setData({ activities: [] })
    } finally {
      this.setData({ loading: false })
    }
  },

  // 搜索
  onSearchInput(e) {
    this.setData({ keyword: e.detail.value })
  },
  onSearch() {
    this.setData({ page: 1, hasMore: true })
    this.fetchActivities()
  },

  // 分类切换
  onCategoryChange(e) {
    const id = e.currentTarget.dataset.id || null
    this.setData({ categoryId: id, page: 1, hasMore: true })
    this.fetchActivities()
  },

  // 加载更多
  loadMore() {
    this.setData({ page: this.data.page + 1 })
    this.fetchActivities()
  },

  // 跳详情
  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/detail/detail?id=' + id })
  }
})
