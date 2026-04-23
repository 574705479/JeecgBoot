/**
 * 访客端 user store stub
 *
 * 主项目 imageCache 用 useUserStoreWithOut().getUserInfo.id 拼图片缓存 key
 * （多账号在同终端隔离）。访客端只有一个虚拟「访客」身份，
 * 这里返回固定 id='visitor'，让全部 cse 解密结果落在同一命名空间下。
 *
 * 退出登录 / 切租户 → 直接刷新整个访客页面即可，不需要复杂状态。
 */
export interface VisitorUserInfo {
  id: string;
  username: string;
}

export function useUserStoreWithOut() {
  return {
    getUserInfo: { id: 'visitor', username: 'visitor' } as VisitorUserInfo,
  };
}
