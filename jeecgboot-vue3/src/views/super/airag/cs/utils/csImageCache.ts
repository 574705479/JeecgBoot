/**
 * 客服业务图片缓存 —— 已统一到全站 `utils/file/imageCache`
 *
 * 历史背景：
 *  - 早期客服模块独立实现了一套图片缓存（含独立 IndexedDB `cs_image_cache`）
 *  - 与 `utils/file/imageCache` 并行存在导致：
 *    a) 同一张 cse:// 被解密两次、同时占两份 blob URL（内存翻倍）
 *    b) 切账号时此处的 reactiveMap 残留前一用户头像（敏感数据残留）
 *    c) cseReactiveMap 不互通，AppLogo 改不到客服模块
 *
 * 当前（已重构）：
 *  - 所有公共缓存 API 转发到 `utils/file/imageCache`
 *  - 旧独立 IndexedDB 不再创建（业务回归后旧数据由浏览器自然 GC）
 *  - `cleanupImageCache` 退化为 no-op，保留导出用作兼容性 import（禁止删除）
 *  - 仅自有的「聊天窗口配置 localStorage 缓存」工具仍在本文件
 */
export {
  withImageCache,
  withImageCacheAsync,
  preloadImages,
  onImageError,
} from '/@/utils/file/imageCache';

// Phase 4.3 (T1) 终审重命名：早期 csImageCache 也导出过同名 initImageCache，
// 与 /@/utils/file/imageCache 的 initImageCache 在 main.ts 看起来像被双重调用，
// 实际上是同一个函数被重新导出。这里改名为 initCsAvatarCache 强调
// 它是「客服模块上下文中的头像缓存初始化」语义入口（内部仍委托全局实现），
// main.ts 也只需要调用一次全局 initImageCache，避免误以为存在重复初始化逻辑。
import { initImageCache as _initGlobalImageCache } from '/@/utils/file/imageCache';

/**
 * 客服模块语义化的头像缓存初始化（仅供客服业务页 onMounted 显式调用）。
 * 与 {@link _initGlobalImageCache}（main.ts 启动时全局调用）完全等价，
 * 保留命名是为了语义清晰：业务侧只关心「客服头像」，无需知道底层是统一缓存。
 */
export async function initCsAvatarCache(): Promise<void> {
  await _initGlobalImageCache();
}

/**
 * 历史 API：曾用于 main.ts beforeunload 释放本地 blob URL。
 * 转发壳重构后已无独立资源需释放，此函数保留为 no-op 仅作兼容性 import。
 * 全站统一清理走 `clearAllCseCache`（详见 t0a）。
 */
export function cleanupImageCache(): void {
  /* no-op: 已统一到 utils/file/imageCache.clearImageCache */
}

// ─── 聊天窗口配置 localStorage 缓存（业务工具，与图片缓存无关） ─────

const CHAT_WINDOW_CONFIG_CACHE_KEY = 'CS_CHAT_WINDOW_CONFIG';

/**
 * 聊天窗口配置（来自后端 `/cs/agent/global/chat-window-settings`）
 *
 * 已知字段先按需声明，后续如果定义稳定可以收紧索引签名。
 */
export interface ChatWindowConfig {
  logo?: string;
  [key: string]: any;
}

export function getCachedChatWindowConfig(): ChatWindowConfig | null {
  try {
    const raw = localStorage.getItem(CHAT_WINDOW_CONFIG_CACHE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function setCachedChatWindowConfig(config: ChatWindowConfig): void {
  try {
    localStorage.setItem(CHAT_WINDOW_CONFIG_CACHE_KEY, JSON.stringify(config));
  } catch {}
}
