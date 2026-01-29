/**
The routing of this file will not show the layout.
It is an independent new page.
the contents of the file still need to log in to access
 */
import type { AppRouteModule } from '/@/router/types';

// test
// http:ip:port/main-out
export const mainOutRoutes: AppRouteModule[] = [
  {
    path: '/cs/userChat',
    name: 'CsUserChat',
    component: () => import('/@/views/super/airag/cs/userChat/index.vue'),
    meta: {
      title: '在线客服',
      ignoreAuth: true,
    },
    alias: '/cs/chat',
  },
  {
    path: '/cs/widget-preview',
    name: 'CsWidgetPreview',
    component: () => import('/@/views/super/airag/cs/widgetPreview/index.vue'),
    meta: {
      title: '客服挂件预览',
      ignoreAuth: true,
    },
  },
  {
    path: '/cs/access-example',
    name: 'CsAccessExample',
    component: () => import('/@/views/super/airag/cs/accessExample/index.vue'),
    meta: {
      title: '第三方接入示例',
      ignoreAuth: true,
    },
  },
  {
    path: '/main-out',
    name: 'MainOut',
    component: () => import('/@/views/demo/main-out/index.vue'),
    meta: {
      title: 'MainOut',
      ignoreAuth: true,
    },
  },
];

export const mainOutRouteNames = mainOutRoutes.map((item) => item.name);
