import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { noAuth: true }
  },
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/activity/:id',
    name: 'ActivityDetail',
    component: () => import('@/views/ActivityDetail.vue')
  },
  {
    path: '/my-activities',
    name: 'MyActivities',
    component: () => import('@/views/MyActivities.vue')
  },
  {
    path: '/manage',
    name: 'ActivityManage',
    component: () => import('@/views/ActivityManage.vue'),
    meta: { roles: ['organizer', 'admin'] }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/views/Admin.vue'),
    meta: { roles: ['admin'] }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue')
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
  const role = userInfo?.role

  // 不需要登录的页面直接通过
  if (to.meta.noAuth) {
    return next()
  }

  // 未登录跳转到登录页
  if (!token) {
    return next('/login')
  }

  if (to.meta.roles && !to.meta.roles.includes(role)) {
    return next('/home')
  }

  next()
})

export default router
