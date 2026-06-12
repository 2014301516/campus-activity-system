const app = getApp()

// 统一请求方法
function request(url, method = 'GET', data = {}) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: app.globalData.baseURL + url,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        'Authorization': app.globalData.token ? 'Bearer ' + app.globalData.token : ''
      },
      success(res) {
        if (res.data.code === 200) {
          resolve(res.data.data)
        } else {
          wx.showToast({ title: res.data.message || '请求失败', icon: 'none' })
          if (res.data.code === 401) {
            // Token 过期，重新登录
            wx.removeStorageSync('token')
            wx.removeStorageSync('userInfo')
            app.globalData.token = ''
            app.globalData.userInfo = null
            wx.reLaunch({ url: '/pages/home/home' })
          }
          reject(new Error(res.data.message))
        }
      },
      fail(err) {
        wx.showToast({ title: '网络错误，请稍后重试', icon: 'none' })
        reject(err)
      }
    })
  })
}

// ==================== API 函数 ====================

// 微信登录
function wxLogin(code) {
  return request('/user/wx-login', 'POST', { code })
}

// 获取用户信息
function getUserInfo() {
  return request('/user/info')
}

// 活动
function getActivityList(params) {
  return request('/activity/list?' + toQuery(params))
}
function getActivityDetail(id) {
  return request('/activity/' + id)
}

// 报名
function register(activityId) {
  return request('/activity/' + activityId + '/register', 'POST')
}
function cancelRegister(activityId) {
  return request('/activity/' + activityId + '/register', 'DELETE')
}
function getMyRegistrations() {
  return request('/my-registrations')
}

// 签到
function signIn(activityId) {
  return request('/sign-in', 'POST', { activityId })
}
function signOut(signInId) {
  return request('/sign-in/' + signInId, 'PUT')
}
function getSignInStatus(activityId) {
  return request('/sign-in/status?activityId=' + activityId)
}

// 评价
function submitReview(activityId, rating, comment) {
  return request('/review', 'POST', { activityId, rating, comment })
}
function getActivityReviews(activityId) {
  return request('/review/activity/' + activityId)
}

// 分类 + 公告
function getCategories() {
  return request('/category/list')
}
function getNotices(page = 1, size = 3) {
  return request('/notice/list?page=' + page + '&size=' + size)
}

// 工具：对象转 query string
function toQuery(params) {
  const keys = Object.keys(params).filter(k => params[k] !== undefined && params[k] !== null && params[k] !== '')
  return keys.map(k => encodeURIComponent(k) + '=' + encodeURIComponent(params[k])).join('&')
}

module.exports = {
  request,
  wxLogin, getUserInfo,
  getActivityList, getActivityDetail,
  register, cancelRegister, getMyRegistrations,
  signIn, signOut, getSignInStatus,
  submitReview, getActivityReviews,
  getCategories, getNotices
}
