import type { AppRouteRecordRaw } from '/@/router/types';
import { LAYOUT } from '/@/router/constant';

export const DASHBOARD_ROUTE: AppRouteRecordRaw = {
  path: '/dashboard',
  name: 'Dashboard',
  component: LAYOUT,
  redirect: '/dashboard/analysis',
  meta: {
    title: '首页',
    icon: 'ant-design:dashboard-outlined',
    hideChildrenInMenu: true,
    orderNo: 0,
  },
  children: [
    {
      path: 'analysis',
      name: 'Analysis',
      component: () => import('/@/views/dashboard/Analysis/index.vue'),
      meta: { title: '首页', hideMenu: true },
    },
  ],
};

export const AI_ROUTE: AppRouteRecordRaw = {
  path: '',
  name: 'ai-parent',
  component: LAYOUT,
  meta: {
    title: 'ai',
  },
  children: [
    {
      path: '/ai',
      name: 'ai',
      component: () => import('/@/views/dashboard/ai/index.vue'),
      meta: {
        title: 'AI助手',
      },
    },
  ],
};

export const staticRoutesList = [DASHBOARD_ROUTE, AI_ROUTE];
