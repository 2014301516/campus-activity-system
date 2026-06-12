import request from './request'

// ==================== 用户 ====================
export const userApi = {
  login: (data) => request.post('/user/login', data),
  register: (data) => request.post('/user/register', data),
  getUserInfo: () => request.get('/user/info'),
  updateProfile: (data) => request.put('/user/update', data)
}

// ==================== 活动 ====================
export const activityApi = {
  getList: (params) => request.get('/activity/list', { params }),
  getDetail: (id) => request.get(`/activity/${id}`),
  create: (data) => request.post('/activity', data),
  update: (id, data) => request.put(`/activity/${id}`, data),
  delete: (id) => request.delete(`/activity/${id}`)
}

// ==================== 报名 ====================
export const registrationApi = {
  register: (activityId) => request.post(`/activity/${activityId}/register`),
  cancel: (activityId) => request.delete(`/activity/${activityId}/register`),
  getMyRegistrations: () => request.get('/my-registrations'),
  getActivityRegistrations: (activityId) => request.get(`/activity/${activityId}/registrations`)
}

// ==================== 签到 ====================
export const signInApi = {
  signIn: (activityId) => request.post('/sign-in', { activityId }),
  signOut: (id) => request.put(`/sign-in/${id}`),
  getStatus: (activityId) => request.get('/sign-in/status', { params: { activityId } }),
  getActivitySignIns: (activityId) => request.get(`/sign-in/activity/${activityId}`)
}

// ==================== 评价 ====================
export const reviewApi = {
  submit: (data) => request.post('/review', data),
  getActivityReviews: (activityId) => request.get(`/review/activity/${activityId}`)
}

// ==================== 分类 ====================
export const categoryApi = {
  getList: () => request.get('/category/list')
}

// ==================== 公告 ====================
export const noticeApi = {
  getList: (params) => request.get('/notice/list', { params })
}

// ==================== 统计 ====================
export const dashboardApi = {
  getStats: () => request.get('/dashboard/stats')
}

// ==================== 管理员 ====================
export const adminApi = {
  getUsers: (params) => request.get('/admin/users', { params }),
  updateUserStatus: (id, status) => request.put(`/admin/users/${id}/status`, { status }),
  auditActivity: (id, status) => request.put(`/admin/activity/${id}/audit`, { status }),
  cancelActivity: (id) => request.put(`/admin/activity/${id}/cancel`),
  addCategory: (data) => request.post('/admin/category', data),
  deleteCategory: (id) => request.delete(`/admin/category/${id}`),
  publishNotice: (data) => request.post('/admin/notice', data),
  deleteNotice: (id) => request.delete(`/admin/notice/${id}`)
}
