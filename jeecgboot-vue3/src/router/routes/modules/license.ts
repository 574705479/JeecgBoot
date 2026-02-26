import type { AppRouteModule } from '/@/router/types';
import { LAYOUT } from '/@/router/constant';

const license: AppRouteModule = {
  path: '/license',
  name: 'License',
  component: LAYOUT,
  redirect: '/license/status',
  meta: {
    orderNo: 9999,
    hideMenu: true,
    title: '授权管理',
  },
  children: [
    {
      path: 'status',
      name: 'LicenseStatus',
      component: () => import('/@/views/system/license/LicenseStatus.vue'),
      meta: {
        title: '授权状态',
        hideMenu: true,
      },
    },
  ],
};

export default license;
