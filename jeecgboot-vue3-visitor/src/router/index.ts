/**
 * 访客端极简 router —— 单路由 + 兜底 404 重定向
 *
 * 主项目 router 含 100+ 路由 + 鉴权守卫 + i18n 标题 + 进度条 + 动态加载，
 * 访客端只需渲染 ChatMain.vue 一个组件，连权限都不需要。
 */
import { createRouter, createWebHashHistory, RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'ChatRoot',
    component: () => import('/@/views/ChatMain.vue'),
    meta: { title: '在线客服' },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
];

export const router = createRouter({
  history: createWebHashHistory(),
  routes,
});

router.afterEach((to) => {
  const t = (to.meta as any)?.title;
  if (t) document.title = t as string;
});

export default router;
