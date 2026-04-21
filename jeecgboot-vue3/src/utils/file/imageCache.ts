/**
 * 全站统一图片缓存（兼容 cse:// 端到端加密）+ 视频/媒体独立通道
 *
 * 职责：
 *  - 普通 http(s) 图片：与 csImageCache 行为一致，两级缓存（内存 LRU + IndexedDB）
 *  - cse://{fid} 图片：调 SecureFileController 取密文 + DEK，前端 WebCrypto 解密为 Blob
 *  - cse://{fid} 视频/媒体：独立通道（withMediaCache），不入 LRU/IDB，引用计数管理
 *  - 缓存 key = `cache:{userId}:{fid|url}`，避免多账号在同一终端互相污染
 *  - 退出登录时调用 `clearImageCache()` 清理（含 DEK 缓存）
 *
 * 关键设计（t0b / t0f）：
 *  - persistent 标记集合：长驻图标（logo/favicon/avatar）永不被 LRU 淘汰
 *  - 非 persistent LRU 淘汰时延迟 60s 才 revoke blob URL，给 reactiveMap 时间通知组件切到占位
 *  - withMediaCache 与 withImageCache 同步签名（命中即返回 blob URL，未命中触发解密 + reactive）
 *  - withMediaCache 使用引用计数（调用 +1 / releaseMedia -1），引用归零即 revoke，禁止 LRU 干预
 */
import { shallowReactive } from 'vue';
import { getToken } from '/@/utils/auth';
import { useUserStoreWithOut } from '/@/store/modules/user';
import { isCseUrl, parseCseFid } from '/@/utils/cse/cseUrl';
import { clearDekCache, decryptFileById } from '/@/utils/cse/cseDecrypt';

const DB_NAME = 'jeecg_image_cache';
const STORE_NAME = 'images';
const DB_VERSION = 1;
const MAX_IDB_ENTRIES = 300;
const MAX_MEM_ENTRIES = 150;
const MAX_SINGLE_SIZE = 5 * 1024 * 1024;
const TTL_MS = 7 * 24 * 60 * 60 * 1000;
const REVOKE_DELAY_MS = 60 * 1000; // 【t0b】LRU 淘汰后延迟 60s 才 revoke，给 reactiveMap 时间通知组件

const memoryCache = new Map<string, string>();
const memoryAccessOrder: string[] = [];
const pendingRequests = new Map<string, Promise<string>>();
/**
 * 响应式 cse:// blob URL 映射：key 为缓存 key，value 为 blob URL。
 * 同步函数 withImageCache 在未命中时会在此 map 上读取（让 Vue 收集依赖），
 * 异步解密成功后写入 → 触发组件自动重渲染。
 *
 * 【sentinel 协议】
 *  - value 为 blob URL（非空字符串）→ 命中，模板正常渲染
 *  - value 为 ''（空串）→ "已尝试且失败" sentinel，让骨架消失但不锁定重试
 *    → withImageCache / withImageThumbCache 的 `if (reactiveHit)` truthy 检查
 *      把空串视为未命中，仍走 pendingRequests 重新解密
 *    → isImageReady 用 .get() !== undefined 检查，空串命中 true 让骨架消失
 *  - 60s TTL 后自动 delete，下次访问会触发新一轮解密
 */
const cseReactiveMap = shallowReactive(new Map<string, string>());

/**
 * 解密失败 sentinel TTL：60s 后自动清除空串占位，允许后续重试。
 * 设计目的：避免"骨架永久转圈"，同时不会因为永久写入 sentinel 而锁死重试。
 */
const FAILED_TTL_MS = 60 * 1000;
function markDecryptFailed(key: string): void {
  cseReactiveMap.set(key, '');
  setTimeout(() => {
    if (cseReactiveMap.get(key) === '') cseReactiveMap.delete(key);
  }, FAILED_TTL_MS);
}

/** 【t0b】长驻图标 key 集合，标记后永不被 LRU 淘汰 */
const persistentKeys = new Set<string>();

/**
 * 【S-P0-4】缓存代际计数：每次 clearImageCache / releaseAllMedia / clearAllCseCache 调用 bumpGeneration() +1。
 *
 * 异步解密任务（loadAndCache / decryptFileByIdToBlobUrl）启动时记录当前 generation，
 * 完成时若 generation 已变化（说明期间发生了 logout / 401 / 切租户），则
 * 立即 URL.revokeObjectURL 释放新生成的 blob，并跳过写回缓存，防止"幽灵 blob"。
 *
 * 三个唯一入口：clearImageCache() / releaseAllMedia() / clearAllCseCache()，
 * 其他模块禁止直接调用 URL.revokeObjectURL 绕过。
 */
let cacheGeneration = 0;
function bumpGeneration(): void {
  cacheGeneration++;
}
export function getCacheGeneration(): number {
  return cacheGeneration;
}

let _db: IDBDatabase | null = null;
let _dbFailed = false;

function openDB(): Promise<IDBDatabase> {
  if (_db) return Promise.resolve(_db);
  if (_dbFailed) return Promise.reject(new Error('IndexedDB unavailable'));
  return new Promise((resolve, reject) => {
    try {
      const req = indexedDB.open(DB_NAME, DB_VERSION);
      req.onupgradeneeded = () => {
        const db = req.result;
        if (!db.objectStoreNames.contains(STORE_NAME)) {
          db.createObjectStore(STORE_NAME, { keyPath: 'key' });
        }
      };
      req.onsuccess = () => {
        _db = req.result;
        resolve(_db);
      };
      req.onerror = () => {
        _dbFailed = true;
        reject(req.error);
      };
    } catch (e) {
      _dbFailed = true;
      reject(e);
    }
  });
}

interface CacheEntry {
  key: string;
  blob: Blob;
  timestamp: number;
}

function idbGet(key: string): Promise<CacheEntry | undefined> {
  return openDB().then(
    (db) =>
      new Promise((resolve) => {
        const tx = db.transaction(STORE_NAME, 'readonly');
        const req = tx.objectStore(STORE_NAME).get(key);
        req.onsuccess = () => resolve(req.result as CacheEntry | undefined);
        req.onerror = () => resolve(undefined);
      }),
    () => undefined,
  );
}

function idbPut(entry: CacheEntry): Promise<void> {
  return openDB().then(
    (db) =>
      new Promise((resolve) => {
        const tx = db.transaction(STORE_NAME, 'readwrite');
        tx.objectStore(STORE_NAME).put(entry);
        tx.oncomplete = () => resolve();
        tx.onerror = () => resolve();
      }),
    () => {},
  );
}

function idbDelete(key: string): Promise<void> {
  return openDB().then(
    (db) =>
      new Promise((resolve) => {
        const tx = db.transaction(STORE_NAME, 'readwrite');
        tx.objectStore(STORE_NAME).delete(key);
        tx.oncomplete = () => resolve();
        tx.onerror = () => resolve();
      }),
    () => {},
  );
}

function idbGetAll(): Promise<CacheEntry[]> {
  return openDB().then(
    (db) =>
      new Promise((resolve) => {
        const tx = db.transaction(STORE_NAME, 'readonly');
        const req = tx.objectStore(STORE_NAME).getAll();
        req.onsuccess = () => resolve(req.result as CacheEntry[]);
        req.onerror = () => resolve([]);
      }),
    () => [],
  );
}

function idbGetAllKeys(): Promise<string[]> {
  return openDB().then(
    (db) =>
      new Promise((resolve) => {
        const tx = db.transaction(STORE_NAME, 'readonly');
        const req = tx.objectStore(STORE_NAME).getAllKeys();
        req.onsuccess = () => resolve(req.result as string[]);
        req.onerror = () => resolve([]);
      }),
    () => [],
  );
}

function touchMemory(key: string) {
  const idx = memoryAccessOrder.indexOf(key);
  if (idx > -1) memoryAccessOrder.splice(idx, 1);
  memoryAccessOrder.push(key);
}

/**
 * 【t0b】延迟 revoke blob URL：LRU 淘汰后给 reactiveMap 60s 时间通知组件切换占位，
 * 避免 DOM 还在引用就立即释放导致破图。
 */
function scheduleRevoke(blobUrl: string) {
  setTimeout(() => {
    try {
      URL.revokeObjectURL(blobUrl);
    } catch {}
  }, REVOKE_DELAY_MS);
}

function setMemoryCache(key: string, blobUrl: string) {
  // 【t0b】LRU 淘汰时跳过 persistent，并延迟 revoke
  while (memoryCache.size >= MAX_MEM_ENTRIES && memoryAccessOrder.length > 0) {
    // 找一个非 persistent 的最老 key 淘汰
    let evictIdx = -1;
    for (let i = 0; i < memoryAccessOrder.length; i++) {
      if (!persistentKeys.has(memoryAccessOrder[i])) {
        evictIdx = i;
        break;
      }
    }
    if (evictIdx < 0) {
      // 所有 key 都是 persistent → 不淘汰，直接退出（避免死循环）
      break;
    }
    const oldest = memoryAccessOrder.splice(evictIdx, 1)[0];
    const old = memoryCache.get(oldest);
    if (old) scheduleRevoke(old);
    memoryCache.delete(oldest);
    cseReactiveMap.delete(oldest);
  }
  memoryCache.set(key, blobUrl);
  cseReactiveMap.set(key, blobUrl);
  touchMemory(key);
}

function currentUserId(): string {
  try {
    const u = useUserStoreWithOut();
    const info = (u as any).getUserInfo;
    return info?.id || info?.username || (getToken() ? 'guest' : 'anon');
  } catch {
    return 'anon';
  }
}

function buildKey(originalUrl: string): string {
  return `cache:${currentUserId()}:${originalUrl}`;
}

function buildThumbKey(originalUrl: string): string {
  return `cache:thumb:${currentUserId()}:${originalUrl}`;
}

function isCacheableUrl(url: string): boolean {
  if (!url) return false;
  if (url.startsWith('data:') || url.startsWith('blob:')) return false;
  if (isCseUrl(url)) return true;
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('/')) return true;
  return false;
}

/** withImageCache 选项 */
export interface WithImageCacheOptions {
  /** 标记为长驻图标（logo / favicon / 顶栏 avatar），永不被 LRU 淘汰 */
  persistent?: boolean;
}

/**
 * 同步取缓存图片 URL：
 *  - 命中返回 blob URL；
 *  - cse:// 未命中：返回占位 URL（''），并触发后台解密；
 *  - 普通 URL 未命中：返回原 URL（直链可用），同时触发后台缓存。
 *
 * @param opts.persistent 标记为长驻图标，永不淘汰（用于 logo/favicon 等）
 */
export function withImageCache(url: string, opts?: WithImageCacheOptions): string {
  if (!isCacheableUrl(url)) return url;
  const key = buildKey(url);
  if (opts?.persistent) persistentKeys.add(key);
  const cached = memoryCache.get(key);
  if (cached) {
    touchMemory(key);
    return cached;
  }
  // 让 Vue 在 reactiveMap 上建立依赖：异步解密完成后写入 reactiveMap，触发组件重渲染
  const reactiveHit = cseReactiveMap.get(key);
  if (reactiveHit) {
    return reactiveHit;
  }
  if (!pendingRequests.has(key)) {
    pendingRequests.set(
      key,
      loadAndCache(url, key).finally(() => pendingRequests.delete(key)),
    );
  }
  // cse:// 不能直接给 <img>，所以未命中时返回 1x1 透明占位
  return isCseUrl(url) ? TRANSPARENT_PNG : url;
}

/** 异步版本：等待缓存就绪再返回 blob URL */
export async function withImageCacheAsync(url: string, opts?: WithImageCacheOptions): Promise<string> {
  if (!isCacheableUrl(url)) return url;
  const key = buildKey(url);
  if (opts?.persistent) persistentKeys.add(key);
  const cached = memoryCache.get(key);
  if (cached) {
    touchMemory(key);
    return cached;
  }
  let pending = pendingRequests.get(key);
  if (!pending) {
    pending = loadAndCache(url, key).finally(() => pendingRequests.delete(key));
    pendingRequests.set(key, pending);
  }
  try {
    return await pending;
  } catch {
    return isCseUrl(url) ? TRANSPARENT_PNG : url;
  }
}

/**
 * 同步取缓存图片 URL（缩略图通道，仅用于 cse:// 图片附件）：
 *  - 只对 image 类型有意义；非图片调用方必须自己判断后再调用
 *  - 命中返回 blob URL；未命中返回 TRANSPARENT_PNG 占位，触发后台解密
 *  - 解密走 decryptFileById(fid, { thumb: true })，命中后端 ?thumb=1 通道
 *  - 任何 thumb 解密错误（AAD 不匹配 / 网络 / 后端无 thumbObjectKey 静默回退原图导致 tag 校验失败）
 *    自动一次性重试原图（thumb=false），原图也失败才返回 TRANSPARENT_PNG
 *  - 与 withImageCache 共用 LRU + IDB + cacheGeneration，只是 cache key 加 `thumb:` 前缀避免互相覆盖
 *  - 非 cse:// URL 透传到 withImageCache
 */
export function withImageThumbCache(url: string, opts?: WithImageCacheOptions): string {
  if (!isCacheableUrl(url)) return url;
  if (!isCseUrl(url)) return withImageCache(url, opts);

  const key = buildThumbKey(url);
  if (opts?.persistent) persistentKeys.add(key);
  const cached = memoryCache.get(key);
  if (cached) {
    touchMemory(key);
    return cached;
  }
  const reactiveHit = cseReactiveMap.get(key);
  if (reactiveHit) return reactiveHit;
  if (!pendingRequests.has(key)) {
    pendingRequests.set(
      key,
      loadAndCache(url, key, { thumb: true }).finally(() => pendingRequests.delete(key)),
    );
  }
  return TRANSPARENT_PNG;
}

/**
 * F3 修正：判定指定 URL 的缓存是否就绪（用于模板骨架屏判定）。
 *
 * 关键点：必须用 `cseReactiveMap.get()` 而不是 `.has()`。
 *  - shallowReactive(Map) 在 Vue 3 reactivity 里 `.has()` 的 track 行为版本间不稳定
 *  - 只有 `.get()` 是 collection handler 里明确 track 的操作
 * 模板调用时会自动建立响应式订阅，cseReactiveMap.set 后 Vue 会自动重渲染。
 *
 * 普通 http(s) URL 直接返回 true（本来就不需要解密，<img> 直接拿原 URL 渲染）。
 */
export function isImageReady(url: string): boolean {
  if (!url) return false;
  if (!isCseUrl(url)) return true;
  const thumbKey = buildThumbKey(url);
  if (cseReactiveMap.get(thumbKey) !== undefined) return true;
  const origKey = buildKey(url);
  if (cseReactiveMap.get(origKey) !== undefined) return true;
  return false;
}

const TRANSPARENT_PNG =
  'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=';

export function preloadImages(urls: (string | null | undefined)[]): void {
  for (const u of urls) {
    if (u && isCacheableUrl(u)) withImageCache(u);
  }
}

/**
 * Electron 等长会话客户端的常驻头像预热入口。
 * 业务侧（如客服工作台 / 通讯录）在 onActivated / visibilitychange 时调用一次：
 *
 *   import { warmupAvatars } from '/@/utils/file/imageCache';
 *   onActivated(() => warmupAvatars(myContacts.map(c => c.avatar)));
 *
 * 内部按时间窗 5 分钟去重，防止反复打 server。
 */
const _warmupCooldown = new Map<string, number>();
export function warmupAvatars(urls: (string | null | undefined)[]): void {
  const now = Date.now();
  for (const u of urls) {
    if (!u || !isCacheableUrl(u)) continue;
    const last = _warmupCooldown.get(u) || 0;
    if (now - last < 5 * 60 * 1000) continue;
    _warmupCooldown.set(u, now);
    withImageCacheAsync(u).catch(() => {});
  }
}

export function onImageError(e: Event, fallback?: string) {
  const img = e.target as HTMLImageElement;
  if (!img || img.dataset.fallbackApplied) return;
  img.dataset.fallbackApplied = 'true';
  img.src = fallback || '/logo.svg';
}

export async function initImageCache(): Promise<void> {
  try {
    const entries = await idbGetAll();
    const now = Date.now();
    for (const entry of entries) {
      if (now - entry.timestamp > TTL_MS) {
        idbDelete(entry.key).catch(() => {});
        continue;
      }
      try {
        const blobUrl = URL.createObjectURL(entry.blob);
        setMemoryCache(entry.key, blobUrl);
      } catch {}
    }
    await checkStorageQuota();
  } catch {}
}

/**
 * 启动 / 周期性检查 IndexedDB 配额。
 * 当 usage / quota > 0.85 时主动 LRU 淘汰，避免后续 idbPut 写入失败。
 */
export async function checkStorageQuota(): Promise<void> {
  try {
    if (!navigator?.storage?.estimate) return;
    const est = await navigator.storage.estimate();
    const quota = est.quota || 0;
    const usage = est.usage || 0;
    if (!quota) return;
    const ratio = usage / quota;
    if (ratio < 0.85) return;
    console.warn(`[ImageCache] 存储使用率 ${(ratio * 100).toFixed(1)}%，触发 LRU 淘汰`);
    const all = await idbGetAll();
    all.sort((a, b) => a.timestamp - b.timestamp);
    const toRemove = Math.ceil(all.length * 0.3); // 一次淘汰 30%
    for (let i = 0; i < toRemove; i++) {
      await idbDelete(all[i].key);
    }
  } catch {}
}

/** 退出登录 / 401 时调用 */
export function clearImageCache(): void {
  // 【S-P0-4】先 bump generation：让进行中的解密任务完成时被丢弃，防止幽灵 blob 写回
  bumpGeneration();
  for (const blobUrl of memoryCache.values()) {
    try {
      URL.revokeObjectURL(blobUrl);
    } catch {}
  }
  memoryCache.clear();
  memoryAccessOrder.length = 0;
  pendingRequests.clear();
  cseReactiveMap.clear();
  persistentKeys.clear();
  // 媒体通道也一并清理
  for (const blobUrl of mediaReactive.values()) {
    try {
      URL.revokeObjectURL(blobUrl);
    } catch {}
  }
  mediaReactive.clear();
  mediaRefCount.clear();
  pendingMedia.clear();
  clearDekCache();
}

async function loadAndCache(url: string, key: string, opts: { thumb?: boolean } = {}): Promise<string> {
  // 【S-P0-4】记录启动时的 generation，完成后校验，防止清理期间的写回竞态
  const myGen = cacheGeneration;
  const isThumb = !!opts.thumb;
  // 内存先 try IDB
  try {
    const e = await idbGet(key);
    if (e && Date.now() - e.timestamp < TTL_MS) {
      const blobUrl = URL.createObjectURL(e.blob);
      if (myGen !== cacheGeneration) {
        try { URL.revokeObjectURL(blobUrl); } catch {}
        return TRANSPARENT_PNG;
      }
      setMemoryCache(key, blobUrl);
      return blobUrl;
    }
  } catch {}

  let blob: Blob | null = null;
  if (isCseUrl(url)) {
    const fid = parseCseFid(url);
    if (!fid) {
      // F2: fid 解析失败属真实失败 → 写 sentinel 让骨架消失
      markDecryptFailed(key);
      return TRANSPARENT_PNG;
    }
    try {
      blob = await decryptFileById(fid, { mime: 'image/*', thumb: isThumb });
    } catch (e) {
      // F2: thumb 解密任何错误（AAD 不匹配 / 网络 / 后端无 thumbObjectKey 静默回退原图导致 tag 校验失败）
      // 都自动一次性重试原图（thumb=false）。结果会写入当前 thumb key，让 isImageReady 正常感知。
      if (isThumb) {
        console.warn('[CSE] thumb decrypt fail, fallback to original', fid, e);
        try {
          blob = await decryptFileById(fid, { mime: 'image/*', thumb: false });
        } catch (e2) {
          console.warn('[CSE] original decrypt also fail', fid, e2);
          // F2: thumb + 原图都失败（核心场景）→ 写 sentinel + 60s TTL
          markDecryptFailed(key);
          return TRANSPARENT_PNG;
        }
      } else {
        console.warn('[CSE] decrypt fail', fid, e);
        // F2: 非 thumb 路径解密失败 → 写 sentinel
        markDecryptFailed(key);
        return TRANSPARENT_PNG;
      }
    }
  } else {
    try {
      const r = await fetch(url, { credentials: 'omit' });
      if (!r.ok) return url;
      blob = await r.blob();
    } catch {
      return url;
    }
  }

  if (!blob || blob.size === 0 || blob.size > MAX_SINGLE_SIZE) {
    // F2: blob 大小异常 / 空 blob，仅 cse:// 分支写 sentinel；普通 URL 直接 return 原 url
    if (isCseUrl(url)) {
      markDecryptFailed(key);
      return TRANSPARENT_PNG;
    }
    return url;
  }
  const blobUrl = URL.createObjectURL(blob);
  // 【S-P0-4】完成后校验 generation：清理期间的解密结果直接丢弃，避免写回幽灵 blob
  if (myGen !== cacheGeneration) {
    try { URL.revokeObjectURL(blobUrl); } catch {}
    return isCseUrl(url) ? TRANSPARENT_PNG : url;
  }
  setMemoryCache(key, blobUrl);
  idbPut({ key, blob, timestamp: Date.now() }).then(() => evictIdbIfNeeded()).catch(() => {});
  return blobUrl;
}

async function evictIdbIfNeeded(): Promise<void> {
  try {
    const keys = await idbGetAllKeys();
    if (keys.length <= MAX_IDB_ENTRIES) return;
    const all = await idbGetAll();
    all.sort((a, b) => a.timestamp - b.timestamp);
    const toRemove = all.length - MAX_IDB_ENTRIES;
    for (let i = 0; i < toRemove; i++) {
      await idbDelete(all[i].key);
    }
  } catch {}
}

// ─── 【t0f 修订】视频/媒体独立通道 ───────────────────────────────
//
// 设计要点（修订理由：原引用计数方案在 Vue 模板每次重渲染都会调用 +1，
// 但 onUnmounted 只 -1 一次，引用永远归不了零，blob 永久泄漏）：
//
//  - 与 withImageCache 同步签名，模板可直接 :src="withMediaCache(url)"
//  - 同 url 解密后写入 mediaReactive（reactive 触发重渲染）
//  - 不入 LRU、不入 IndexedDB（视频体积大，缓存成本远超收益）
//  - 生命周期管理：按「会话级」清理，由业务侧在切会话 / 离开页 / logout 时主动调
//    releaseAllMedia() 或 releaseMedia(specificUrl)
//  - clearImageCache 已包含所有媒体 blob 释放（logout 钩子统一兜底）
//
// 业务侧调用约定：
//  ```
//  // 模板
//  <video v-if="getAttachmentUrl(item)" :src="getAttachmentUrl(item)" controls />
//
//  // 切会话时
//  watch(currentConversation, () => releaseAllMedia());
//  ```

const mediaReactive = shallowReactive(new Map<string, string>());
const mediaRefCount = new Map<string, number>(); // 保留供失败统计/调试
const pendingMedia = new Map<string, Promise<string>>();

/**
 * 视频/媒体同步包装：未命中返回空串，业务模板需用 v-if 套条件渲染。
 *
 * 【F1+实施修订】放弃引用计数（模板重渲染会爆增），改用会话级清理。
 *
 * 【F5 警告】未命中返回 '' 时，<video src=""> 在 Firefox/Safari 显示破图标，
 * 业务模板必须用：
 *     <video v-if="getAttachmentUrl(file)" :src="getAttachmentUrl(file)" controls />
 *
 * @param url cse:// 完整 URL（非 cse:// 直接返回原 URL）
 * @param mime 推荐 MIME（如 'video/mp4'）
 */
export function withMediaCache(url: string, mime?: string): string {
  if (!url) return '';
  if (!isCseUrl(url)) return url;

  const hit = mediaReactive.get(url);
  if (hit) return hit;

  if (!pendingMedia.has(url)) {
    const fid = parseCseFid(url);
    if (!fid) return '';
    // 【S-P0-4】记录启动时的 generation，完成后校验
    const myGen = cacheGeneration;
    const p = decryptFileByIdToBlobUrl(fid, mime || 'application/octet-stream')
      .then((blobUrl) => {
        if (myGen !== cacheGeneration) {
          try { URL.revokeObjectURL(blobUrl); } catch {}
          return '';
        }
        mediaReactive.set(url, blobUrl); // 写入 reactive map → 触发模板重渲染
        return blobUrl;
      })
      .catch((e) => {
        console.warn('[withMediaCache] decrypt fail', fid, e?.message || e);
        return '';
      })
      .finally(() => {
        pendingMedia.delete(url);
      });
    pendingMedia.set(url, p);
  }

  return ''; // 未命中：业务侧 v-if 保护
}

/**
 * 释放单个媒体 blob URL（按 url 直接释放，不计数）。
 * 业务侧在「确定不再使用此视频」时调用（如关闭播放器组件、关闭弹窗）。
 * 注意：如果同 url 仍被其他组件渲染中，调用此函数会让那些组件破图，
 *       通常更安全的做法是切会话/离开页时统一调 releaseAllMedia()。
 */
export function releaseMedia(url: string): void {
  if (!url || !isCseUrl(url)) return;
  const blob = mediaReactive.get(url);
  if (blob) {
    try {
      URL.revokeObjectURL(blob);
    } catch {}
  }
  mediaReactive.delete(url);
  mediaRefCount.delete(url);
}

/**
 * 释放所有媒体 blob URL（会话级清理）。
 * 业务侧在切会话 / 离开聊天页 / logout 前调用。
 * logout 路径已通过 clearImageCache 自动清理，业务无需重复。
 */
export function releaseAllMedia(): void {
  // 【S-P0-4】bump generation：进行中的视频解密任务完成时会自动 revoke + 不写回
  bumpGeneration();
  for (const blobUrl of mediaReactive.values()) {
    try {
      URL.revokeObjectURL(blobUrl);
    } catch {}
  }
  mediaReactive.clear();
  mediaRefCount.clear();
  pendingMedia.clear();
}

/** 内部：解密为 blob URL（不复用 cseDecrypt 的 decryptFileToObjectUrl 是为了能传自定义 mime） */
async function decryptFileByIdToBlobUrl(fid: string, mime: string): Promise<string> {
  const blob = await decryptFileById(fid, { mime });
  return URL.createObjectURL(blob);
}
