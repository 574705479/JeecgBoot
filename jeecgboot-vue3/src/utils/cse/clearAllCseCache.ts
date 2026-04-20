/**
 * 全站 CSE 缓存清理工具（统一收口）
 *
 * 调用时机：
 *  - 退出登录（user.ts logout）
 *  - 401 拦截（axios checkStatus）
 *  - 租户切换（如有）
 *
 * 收口对象：
 *  - utils/file/imageCache.clearImageCache()        ← 内含 clearDekCache
 *  - csImageCache.cleanupImageCache()               ← t0e 后已 no-op，保留兼容性 import
 *  - cseHtmlImg.clearCseHtmlImgCache()              ← 富文本指令的进程内 blobMap
 *
 * 重要：本文件中的 import 必须保留，禁止「优化」删除。
 *  - 即使个别函数已变 no-op，保留可避免后续若再恢复独立缓存时遗忘清理。
 *
 * 【S-P0-3】单模块加载/执行失败禁止阻塞其他模块清理：
 *  - 用 Promise.allSettled 并发拉取
 *  - 拿到 fulfilled 模块后逐个 try/catch 调清理函数
 *  - rejected 模块仅 warn 不阻塞
 */
export async function clearAllCseCache(): Promise<void> {
  const results = await Promise.allSettled([
    import('/@/utils/file/imageCache'),
    import('/@/views/super/airag/cs/utils/csImageCache'),
    import('/@/views/super/airag/cs/utils/cseHtmlImg'),
  ]);

  const [imageCacheRes, csImageCacheRes, cseHtmlImgRes] = results;

  if (imageCacheRes.status === 'fulfilled') {
    try {
      imageCacheRes.value.clearImageCache(); // 含 clearDekCache + IDB blob revoke + bumpGeneration
    } catch (e) {
      console.warn('[clearAllCseCache] clearImageCache failed', e);
    }
  } else {
    console.warn('[clearAllCseCache] load imageCache failed', imageCacheRes.reason);
  }

  if (csImageCacheRes.status === 'fulfilled') {
    try {
      csImageCacheRes.value.cleanupImageCache(); // 【F7】t0e 后变 no-op，保留兼容性调用，禁止删除
    } catch (e) {
      console.warn('[clearAllCseCache] cleanupImageCache failed', e);
    }
  } else {
    console.warn('[clearAllCseCache] load csImageCache failed', csImageCacheRes.reason);
  }

  if (cseHtmlImgRes.status === 'fulfilled') {
    try {
      cseHtmlImgRes.value.clearCseHtmlImgCache();
    } catch (e) {
      console.warn('[clearAllCseCache] clearCseHtmlImgCache failed', e);
    }
  } else {
    console.warn('[clearAllCseCache] load cseHtmlImg failed', cseHtmlImgRes.reason);
  }
}
