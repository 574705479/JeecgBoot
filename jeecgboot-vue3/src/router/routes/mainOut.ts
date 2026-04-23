/**
The routing of this file will not show the layout.
It is an independent new page.
the contents of the file still need to log in to access
 */
import type { AppRouteModule } from '/@/router/types';

// test
// http:ip:port/main-out
//
// 注意：原 /cs/userChat（含 alias /cs/chat）路由已下线。
// 现由独立子项目 jeecgboot-vue3-visitor 接管，build:with-visitor 会把访客端产物
// 输出到 dist/cs/userChat/index.html，由 nginx 优先匹配真实文件后直接返回。
// 旧组件 /@/views/super/airag/cs/userChat/index.vue 暂时保留，作为紧急回滚备份；
// 一段时间稳定后可彻底删除并清理 register.ts、permissionGuard、checkStatus 中相关引用。
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
