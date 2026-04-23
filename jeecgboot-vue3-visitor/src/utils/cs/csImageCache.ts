/**
 * 转发壳：兼容主项目 ../utils/csImageCache 的 import 路径。
 *
 * 真正实现已统一到 /@/utils/file/imageCache。
 * 业务侧只需要：withImageCache / withImageCacheAsync / preloadImages / onImageError
 * + 聊天窗口配置 localStorage 缓存（业务工具）
 */
export {
  withImageCache,
  withImageCacheAsync,
  preloadImages,
  onImageError,
} from '/@/utils/file/imageCache';

import { initImageCache as _initGlobalImageCache } from '/@/utils/file/imageCache';

export async function initCsAvatarCache(): Promise<void> {
  await _initGlobalImageCache();
}

export function cleanupImageCache(): void {
  /* no-op */
}

const CHAT_WINDOW_CONFIG_CACHE_KEY = 'CS_CHAT_WINDOW_CONFIG';

export function getCachedChatWindowConfig(): Record<string, any> | null {
  try {
    const raw = localStorage.getItem(CHAT_WINDOW_CONFIG_CACHE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function setCachedChatWindowConfig(config: Record<string, any>): void {
  try {
    localStorage.setItem(CHAT_WINDOW_CONFIG_CACHE_KEY, JSON.stringify(config));
  } catch {}
}
