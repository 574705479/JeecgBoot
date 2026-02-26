import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import BasicLayout from '../layouts/BasicLayout.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/index.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    component: BasicLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/dashboard/index.vue'),
        meta: { title: '仪表盘' },
      },
      {
        path: 'app',
        name: 'AppList',
        component: () => import('../views/app/AppList.vue'),
        meta: { title: '应用管理' },
      },
      {
        path: 'plan',
        name: 'PlanList',
        component: () => import('../views/plan/PlanList.vue'),
        meta: { title: '套餐管理' },
      },
      {
        path: 'license',
        name: 'LicenseList',
        component: () => import('../views/license/LicenseList.vue'),
        meta: { title: '许可证列表' },
      },
      {
        path: 'license/create',
        name: 'LicenseCreate',
        component: () => import('../views/license/LicenseCreate.vue'),
        meta: { title: '创建许可证' },
      },
      {
        path: 'license/:id',
        name: 'LicenseDetail',
        component: () => import('../views/license/LicenseDetail.vue'),
        meta: { title: '许可证详情' },
      },
      {
        path: 'customer',
        name: 'CustomerList',
        component: () => import('../views/customer/CustomerList.vue'),
        meta: { title: '客户管理' },
      },
      {
        path: 'log',
        name: 'LogList',
        component: () => import('../views/log/LogList.vue'),
        meta: { title: '操作日志' },
      },
      {
        path: 'server/info',
        name: 'ServerInfoList',
        component: () => import('../views/server/ServerInfoList.vue'),
        meta: { title: '服务器管理' },
      },
      {
        path: 'server/docker',
        name: 'DockerServiceList',
        component: () => import('../views/server/DockerServiceList.vue'),
        meta: { title: 'Docker服务管理' },
      },
      {
        path: 'server/info/log',
        name: 'ServerInfoLogList',
        component: () => import('../views/server/ServerInfoLogList.vue'),
        meta: { title: '服务器日志' },
      },
      {
        path: 'settings/password',
        name: 'ChangePassword',
        component: () => import('../views/settings/ChangePassword.vue'),
        meta: { title: '修改密码' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('accessToken')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
