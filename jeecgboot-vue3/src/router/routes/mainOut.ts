/**
The routing of this file will not show the layout.
It is an independent new page.
the contents of the file still need to log in to access
 */
import type { AppRouteModule } from '/@/router/types';

// test
// http:ip:port/main-out
//
// 访客端入口（/cs/userChat、/cs/chat）已彻底下线，独立子项目 jeecgboot-vue3-visitor 独占。
// build:with-visitor 会把访客端产物输出到 dist/cs/userChat/index.html，
// 由 nginx 优先匹配真实文件后直接返回访客端 SPA，主项目 vue-router 不再参与。
// 旧组件 /@/views/super/airag/cs/userChat/index.vue 已删除；相关死引用
// （register.ts 的 /cs/chat 路由、permissionGuard 的 CsUserChat name）已清理完毕。
export const mainOutRoutes: AppRouteModule[] = [
  {
    path: '/cs/widget-preview',
    name: 'CsWidgetPreview',
    component: () => import('/@/views/super/airag/cs/widgetPreview/index.vue'),
    meta: {
      title: '客服挂件预览',
      ignoreAuth: true,
    },
  },
];

export const mainOutRouteNames = mainOutRoutes.map((item) => item.name);
