/**
 * 1:1 复制自 jeecgboot-vue3/src/utils/file/imageCache.ts
 *
 * 全站统一图片缓存（兼容 cse:// 端到端加密）+ 视频/媒体独立通道
 *
 * 访客端注意：
 *  - getToken() 永远返回 ''（详见 /@/utils/auth stub）
 *  - useUserStoreWithOut() 返回固定 'visitor' id（详见 /@/store/modules/user stub）
 *  - 因此所有缓存 key 都落在同一命名空间下，访客端不需要多账号隔离
 *  - 退出登录时调用 clearImageCache() 清理
 */
import { shallowReactive } from 'vue';
import { getToken } from '/@/utils/auth';
import { useUserStoreWithOut } from '/@/store/modules/user';
import { isCseUrl, parseCseFid } from '/@/utils/cse/cseUrl';
import { clearDekCache, decryptFileById } from '/@/utils/cse/cseDecrypt';
import { DEFAULT_LOGO_URL } from '/@/utils/asset';

const DB_NAME = 'jeecg_image_cache';
const STORE_NAME = 'images';
// v2 新增 'media' store 用于视频/音频持久化（与图片隔离配额，避免视频撑爆图片 LRU）
const STORE_NAME_MEDIA = 'media';
const DB_VERSION = 2;
const MAX_IDB_ENTRIES = 300;
const MAX_MEM_ENTRIES = 150;
const MAX_SINGLE_SIZE = 5 * 1024 * 1024;
// 视频/音频允许更大单文件（典型聊天短视频 5~30MB）。超过此上限直接放弃 IDB（仍走内存 + 实时解密）
const MAX_MEDIA_SIZE = 30 * 1024 * 1024;
// 视频 IDB 最多 50 条；按 30MB 上限算最坏情况 1.5GB，仍在常见浏览器配额内（80%~95% 之前 LRU 兜底）
const MAX_MEDIA_IDB_ENTRIES = 50;
const TTL_MS = 7 * 24 * 60 * 60 * 1000;
const REVOKE_DELAY_MS = 60 * 1000;

const memoryCache = new Map<string, string>();
const memoryAccessOrder: string[] = [];
const pendingRequests = new Map<string, Promise<string>>();

const cseReactiveMap = shallowReactive(new Map<string, string>());

function markDecryptFailed(key: string): void {
  cseReactiveMap.set(key, '');
}

interface FailureEntry {
  attempts: number;
  nextRetryAt: number;
  permanent: boolean;
  lastError?: string;
}
const failureRegistry = new Map<string, FailureEntry>();
const RETRY_BACKOFF_MS = [30_000, 120_000];
const MAX_AUTO_RETRY = RETRY_BACKOFF_MS.length;

function classifyError(err: any): 'transient' | 'permanent' {
  const status = err?.response?.status;
  if (status === 401 || status === 403) return 'permanent';
  if (typeof DOMException !== 'undefined' && err instanceof DOMException) return 'permanent';
  return 'transient';
}

function recordFailure(key: string, err: any): void {
  const prev = failureRegistry.get(key);
  const kind = classifyError(err);
  const lastError = (err && (err.message || String(err))) || 'unknown';
  if (kind === 'permanent') {
    failureRegistry.set(key, {
      attempts: MAX_AUTO_RETRY,
      nextRetryAt: Number.MAX_SAFE_INTEGER,
      permanent: true,
      lastError,
    });
    return;
  }
  const attempts = (prev?.attempts || 0) + 1;
  if (attempts >= MAX_AUTO_RETRY) {
    failureRegistry.set(key, {
      attempts,
      nextRetryAt: Number.MAX_SAFE_INTEGER,
      permanent: false,
      lastError,
    });
    return;
  }
  const backoff = RETRY_BACKOFF_MS[Math.min(attempts - 1, RETRY_BACKOFF_MS.length - 1)];
  failureRegistry.set(key, {
    attempts,
    nextRetryAt: Date.now() + backoff,
    permanent: false,
    lastError,
  });
}

function canAttempt(key: string): boolean {
  const e = failureRegistry.get(key);
  if (!e) return true;
  if (e.permanent) return false;
  return Date.now() >= e.nextRetryAt;
}

function clearFailure(key: string): void {
  failureRegistry.delete(key);
}

function getFailureState(key: string): { failed: boolean; exhausted: boolean } {
  const e = failureRegistry.get(key);
  if (!e) return { failed: false, exhausted: false };
  return { failed: e.attempts > 0, exhausted: e.attempts >= MAX_AUTO_RETRY };
}

const persistentKeys = new Set<string>();

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
        // v1 → v2：新增 media store。已有用户的图片缓存 (images) 不动。
        if (!db.objectStoreNames.contains(STORE_NAME_MEDIA)) {
          db.createObjectStore(STORE_NAME_MEDIA, { keyPath: 'key' });
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
  // 已知图片尺寸（仅 image，video/audio 留空）
  // 用于"无感刷新"：v-cse-html 渲染前 setAttribute width/height，
  // 让占位框就是最终尺寸，消除富文本 unsized image 引起的 CLS。
  // 老缓存可能没有这两个字段（向后兼容），首次访问时缺尺寸属正常，下次访问有。
  w?: number;
  h?: number;
}

const dimsCache = new Map<string, { w: number; h: number }>();

function probeImageDims(blobUrl: string): Promise<{ w: number; h: number } | null> {
  return new Promise((resolve) => {
    if (typeof Image === 'undefined') return resolve(null);
    const img = new Image();
    let done = false;
    const finish = (r: { w: number; h: number } | null) => {
      if (done) return;
      done = true;
      img.onload = null;
      img.onerror = null;
      resolve(r);
    };
    img.onload = () => finish({ w: img.naturalWidth, h: img.naturalHeight });
    img.onerror = () => finish(null);
    try {
      img.src = blobUrl;
    } catch {
      finish(null);
    }
    setTimeout(() => finish(null), 5000);
  });
}

function idbGet(key: string, store: string = STORE_NAME): Promise<CacheEntry | undefined> {
  return openDB().then(
    (db) =>
      new Promise((resolve) => {
        const tx = db.transaction(store, 'readonly');
        const req = tx.objectStore(store).get(key);
        req.onsuccess = () => resolve(req.result as CacheEntry | undefined);
        req.onerror = () => resolve(undefined);
      }),
    () => undefined,
  );
}

function idbPut(entry: CacheEntry, store: string = STORE_NAME): Promise<void> {
  return openDB().then(
    (db) =>
      new Promise((resolve) => {
        const tx = db.transaction(store, 'readwrite');
        tx.objectStore(store).put(entry);
        tx.oncomplete = () => resolve();
        tx.onerror = () => resolve();
      }),
    () => {},
  );
}

function idbDelete(key: string, store: string = STORE_NAME): Promise<void> {
  return openDB().then(
    (db) =>
      new Promise((resolve) => {
        const tx = db.transaction(store, 'readwrite');
        tx.objectStore(store).delete(key);
        tx.oncomplete = () => resolve();
        tx.onerror = () => resolve();
      }),
    () => {},
  );
}

function idbGetAll(store: string = STORE_NAME): Promise<CacheEntry[]> {
  return openDB().then(
    (db) =>
      new Promise((resolve) => {
        const tx = db.transaction(store, 'readonly');
        const req = tx.objectStore(store).getAll();
        req.onsuccess = () => resolve(req.result as CacheEntry[]);
        req.onerror = () => resolve([]);
      }),
    () => [],
  );
}

function idbGetAllKeys(store: string = STORE_NAME): Promise<string[]> {
  return openDB().then(
    (db) =>
      new Promise((resolve) => {
        const tx = db.transaction(store, 'readonly');
        const req = tx.objectStore(store).getAllKeys();
        req.onsuccess = () => resolve(req.result as string[]);
        req.onerror = () => resolve([]);
      }),
    () => [],
  );
}

function idbClearStore(store: string): Promise<void> {
  return openDB().then(
    (db) =>
      new Promise((resolve) => {
        const tx = db.transaction(store, 'readwrite');
        tx.objectStore(store).clear();
        tx.oncomplete = () => resolve();
        tx.onerror = () => resolve();
      }),
    () => {},
  );
}

function touchMemory(key: string) {
  const idx = memoryAccessOrder.indexOf(key);
  if (idx > -1) memoryAccessOrder.splice(idx, 1);
  memoryAccessOrder.push(key);
}

function scheduleRevoke(blobUrl: string) {
  setTimeout(() => {
    try {
      URL.revokeObjectURL(blobUrl);
    } catch {}
  }, REVOKE_DELAY_MS);
}

function setMemoryCache(key: string, blobUrl: string) {
  while (memoryCache.size >= MAX_MEM_ENTRIES && memoryAccessOrder.length > 0) {
    let evictIdx = -1;
    for (let i = 0; i < memoryAccessOrder.length; i++) {
      if (!persistentKeys.has(memoryAccessOrder[i])) {
        evictIdx = i;
        break;
      }
    }
    if (evictIdx < 0) {
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

export interface WithImageCacheOptions {
  persistent?: boolean;
}

export function withImageCache(url: string, opts?: WithImageCacheOptions): string {
  if (!isCacheableUrl(url)) return url;
  const key = buildKey(url);
  if (opts?.persistent) persistentKeys.add(key);
  const cached = memoryCache.get(key);
  if (cached) {
    touchMemory(key);
    return cached;
  }
  const reactiveHit = cseReactiveMap.get(key);
  if (reactiveHit) {
    return reactiveHit;
  }
  if (reactiveHit === '' && !canAttempt(key)) {
    return TRANSPARENT_PNG;
  }
  if (!pendingRequests.has(key)) {
    pendingRequests.set(
      key,
      loadAndCache(url, key).finally(() => pendingRequests.delete(key)),
    );
  }
  return isCseUrl(url) ? TRANSPARENT_PNG : url;
}

export async function withImageCacheAsync(url: string, opts?: WithImageCacheOptions): Promise<string> {
  if (!isCacheableUrl(url)) return url;
  const key = buildKey(url);
  if (opts?.persistent) persistentKeys.add(key);
  const cached = memoryCache.get(key);
  if (cached) {
    touchMemory(key);
    return cached;
  }
  const reactiveHit = cseReactiveMap.get(key);
  if (reactiveHit === '' && !canAttempt(key)) {
    return isCseUrl(url) ? TRANSPARENT_PNG : url;
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
  if (reactiveHit === '' && !canAttempt(key)) {
    return TRANSPARENT_PNG;
  }
  if (!pendingRequests.has(key)) {
    pendingRequests.set(
      key,
      loadAndCache(url, key, { thumb: true }).finally(() => pendingRequests.delete(key)),
    );
  }
  return TRANSPARENT_PNG;
}

export function isImageReady(url: string): boolean {
  if (!url) return false;
  if (!isCseUrl(url)) return true;
  const thumbKey = buildThumbKey(url);
  const tv = cseReactiveMap.get(thumbKey);
  if (tv) return true;
  const origKey = buildKey(url);
  const ov = cseReactiveMap.get(origKey);
  if (ov) return true;
  return false;
}

/**
 * 同步获取已缓存的原图 blob URL（不触发任何加载）。
 * 富文本指令 v-cse-html 用它做 quick check，避免重复异步等待。
 * 仅查 memoryCache（非 cseReactiveMap），保证返回的 blob URL 还在 LRU 内未被 revoke。
 */
export function getImageBlobIfReady(url: string): string | null {
  if (!url || !isCseUrl(url)) return null;
  const key = buildKey(url);
  const cached = memoryCache.get(key);
  if (cached) {
    touchMemory(key);
    return cached;
  }
  return null;
}

/**
 * 首屏阻塞预热（无感刷新关键）：批量从 IDB 把已有缓存 hydrate 到 memoryCache + cseReactiveMap，
 * 这样 v-for 第一帧渲染时 withImageCache(url) 同步命中内存返回 blob URL，
 * 避免"空 src / TRANSPARENT_PNG → 真图"的肉眼可见闪烁。
 *
 * 性能：一次 IDB readonly transaction 并行 get N 个 key，常见 < 30ms（取决于 blob 大小）。
 *
 * @param urls 需要预热的图片 URL 列表（cse:// 或 http(s)，自动跳过不可缓存的、已在内存的）
 * @returns 命中数量
 */
export async function warmupImagesFromIdb(urls: string[]): Promise<number> {
  // 兜底：先确保 init 完成（initImageCache 已 hydrate memory + dimsCache + cseReactiveMap），
  // 否则下面 has(k) 早返回会跳过 dims hydrate，导致 v-cse-html 同步读不到尺寸。
  await whenImageCacheReady();
  const seen = new Set<string>();
  const targets: { key: string }[] = [];
  for (const u of urls) {
    if (!u || !isCacheableUrl(u)) continue;
    const k = buildKey(u);
    if (memoryCache.has(k) || cseReactiveMap.has(k)) continue;
    if (seen.has(k)) continue;
    seen.add(k);
    targets.push({ key: k });
    if (isCseUrl(u)) {
      const tk = buildThumbKey(u);
      if (!memoryCache.has(tk) && !cseReactiveMap.has(tk) && !seen.has(tk)) {
        seen.add(tk);
        targets.push({ key: tk });
      }
    }
  }
  if (targets.length === 0) return 0;

  let hits = 0;
  const upgradeBacklog: { key: string; blob: Blob; blobUrl: string }[] = [];
  try {
    const db = await openDB();
    const tx = db.transaction(STORE_NAME, 'readonly');
    const store = tx.objectStore(STORE_NAME);
    await Promise.all(
      targets.map(
        (t) =>
          new Promise<void>((resolve) => {
            const req = store.get(t.key);
            req.onsuccess = () => {
              const v = req.result as CacheEntry | undefined;
              if (v && Date.now() - v.timestamp <= TTL_MS) {
                try {
                  const blobUrl = URL.createObjectURL(v.blob);
                  setMemoryCache(t.key, blobUrl);
                  cseReactiveMap.set(t.key, blobUrl);
                  if (typeof v.w === 'number' && typeof v.h === 'number' && v.w > 0 && v.h > 0) {
                    dimsCache.set(t.key, { w: v.w, h: v.h });
                  } else {
                    // 老 entry：排队补探测
                    upgradeBacklog.push({ key: t.key, blob: v.blob, blobUrl });
                  }
                  hits++;
                } catch {}
              }
              resolve();
            };
            req.onerror = () => resolve();
          }),
      ),
    );
  } catch {}

  if (upgradeBacklog.length > 0) {
    Promise.resolve().then(async () => {
      for (const item of upgradeBacklog) {
        try {
          const dims = await probeImageDims(item.blobUrl);
          if (dims && dims.w > 0 && dims.h > 0) {
            dimsCache.set(item.key, dims);
            await idbPut({ key: item.key, blob: item.blob, timestamp: Date.now(), w: dims.w, h: dims.h });
          }
        } catch {}
      }
    });
  }
  return hits;
}

/**
 * 同步获取已预热的图片真实尺寸（仅供 v-cse-html 富文本图片使用）。
 * 命中 → 渲染时 setAttribute width/height 占位，消除 layout shift。
 * 未命中 → 老缓存或首次访问，本帧 unsized，下次访问就有。
 */
export function getImageDimsIfReady(url: string): { w: number; h: number } | null {
  if (!url || !isCseUrl(url)) return null;
  const key = buildKey(url);
  return dimsCache.get(key) || null;
}

/**
 * 视频/音频版预热：与 warmupImagesFromIdb 同构，但走 STORE_NAME_MEDIA + mediaReactive。
 * v-for 首帧 withMediaCache(url) 同步命中 mediaReactive，<video src> 直接就绪不闪。
 */
export async function warmupMediaFromIdb(urls: string[]): Promise<number> {
  await whenImageCacheReady();
  const seen = new Set<string>();
  const targets: { key: string; url: string }[] = [];
  for (const u of urls) {
    if (!u || !isCseUrl(u)) continue;
    if (mediaReactive.has(u) || seen.has(u)) continue;
    seen.add(u);
    targets.push({ key: buildMediaKey(u), url: u });
  }
  if (targets.length === 0) return 0;

  let hits = 0;
  try {
    const db = await openDB();
    const tx = db.transaction(STORE_NAME_MEDIA, 'readonly');
    const store = tx.objectStore(STORE_NAME_MEDIA);
    await Promise.all(
      targets.map(
        (t) =>
          new Promise<void>((resolve) => {
            const req = store.get(t.key);
            req.onsuccess = () => {
              const v = req.result as CacheEntry | undefined;
              if (v && Date.now() - v.timestamp <= TTL_MS) {
                try {
                  const blobUrl = URL.createObjectURL(v.blob);
                  mediaReactive.set(t.url, blobUrl);
                  hits++;
                } catch {}
              }
              resolve();
            };
            req.onerror = () => resolve();
          }),
      ),
    );
  } catch {}
  return hits;
}

const TRANSPARENT_PNG =
  'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=';

export function preloadImages(urls: (string | null | undefined)[]): void {
  for (const u of urls) {
    if (u && isCacheableUrl(u)) withImageCache(u);
  }
}

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
  img.src = fallback || DEFAULT_LOGO_URL;
}

export function retryImage(canonicalUrl: string): void {
  if (!canonicalUrl) return;
  const tk = buildThumbKey(canonicalUrl);
  const ok = buildKey(canonicalUrl);
  for (const k of [tk, ok]) {
    failureRegistry.delete(k);
    if (cseReactiveMap.get(k) === '') cseReactiveMap.delete(k);
    pendingRequests.delete(k);
  }
}

export function getImageFailureState(canonicalUrl: string): { failed: boolean; exhausted: boolean } {
  if (!canonicalUrl) return { failed: false, exhausted: false };
  const tk = buildThumbKey(canonicalUrl);
  const ok = buildKey(canonicalUrl);
  const t = getFailureState(tk);
  const o = getFailureState(ok);
  return { failed: t.failed || o.failed, exhausted: t.exhausted || o.exhausted };
}

// 全局 init promise：保证图片缓存模块加载时即开始 hydrate IDB → memory（含 dims），
// 后续 warmup / v-cse-html / withImageCache 都基于已 hydrate 的状态运行，不再出现
// "ChatMain 数据回来时 init 还没跑完导致 dimsCache 空" 的竞态。
let _imageCacheInitPromise: Promise<void> | null = null;
export function whenImageCacheReady(): Promise<void> {
  return _imageCacheInitPromise || Promise.resolve();
}

async function _doInitImageCache(): Promise<void> {
  try {
    const entries = await idbGetAll();
    const now = Date.now();
    const upgradeBacklog: { key: string; blob: Blob; blobUrl: string }[] = [];
    for (const entry of entries) {
      if (now - entry.timestamp > TTL_MS) {
        idbDelete(entry.key).catch(() => {});
        continue;
      }
      try {
        const blobUrl = URL.createObjectURL(entry.blob);
        setMemoryCache(entry.key, blobUrl);
        // 富文本图片需要 cseReactiveMap 命中才能被 v-cse-html 同步识别
        cseReactiveMap.set(entry.key, blobUrl);
        // dims hydrate：v-cse-html 渲染时同步 setAttribute aspect-ratio，
        // 消除富文本 unsized image 加载后 0→自然高度撑开造成的 CLS。
        if (typeof entry.w === 'number' && typeof entry.h === 'number' && entry.w > 0 && entry.h > 0) {
          dimsCache.set(entry.key, { w: entry.w, h: entry.h });
        } else {
          // 老 entry 没 w/h：排队后台探测 + 回写 IDB（下次起就有 dims）
          upgradeBacklog.push({ key: entry.key, blob: entry.blob, blobUrl });
        }
      } catch {}
    }
    await checkStorageQuota();
    // 后台升级老 entry 的 dims，不阻塞 init 返回。当前帧 unsized image 仍可能引起一次 CLS，下次刷新起即消除。
    if (upgradeBacklog.length > 0) {
      Promise.resolve().then(async () => {
        for (const item of upgradeBacklog) {
          try {
            const dims = await probeImageDims(item.blobUrl);
            if (dims && dims.w > 0 && dims.h > 0) {
              dimsCache.set(item.key, dims);
              await idbPut({ key: item.key, blob: item.blob, timestamp: Date.now(), w: dims.w, h: dims.h });
            }
          } catch {}
        }
      });
    }
  } catch {}
}

export function initImageCache(): Promise<void> {
  if (!_imageCacheInitPromise) {
    _imageCacheInitPromise = _doInitImageCache();
  }
  return _imageCacheInitPromise;
}

// 模块加载时立即触发一次 init（幂等、同步发起）：
// 越早发起 IDB readonly tx，dimsCache / cseReactiveMap 越早 hydrate 完，
// v-cse-html 第一帧 / withImageCache 同步命中率越高，CLS / 闪烁越接近 0。
try {
  if (typeof window !== 'undefined') {
    initImageCache().catch(() => {});
  }
} catch {}

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
    const toRemove = Math.ceil(all.length * 0.3);
    for (let i = 0; i < toRemove; i++) {
      await idbDelete(all[i].key);
    }
  } catch {}
}

export function clearImageCache(): void {
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
  for (const blobUrl of mediaReactive.values()) {
    try {
      URL.revokeObjectURL(blobUrl);
    } catch {}
  }
  mediaReactive.clear();
  mediaRefCount.clear();
  pendingMedia.clear();
  failureRegistry.clear();
  // logout 场景：图片 + 视频 IDB 都要清。fire-and-forget，不阻塞 UI。
  idbClearStore(STORE_NAME).catch(() => {});
  idbClearStore(STORE_NAME_MEDIA).catch(() => {});
  clearDekCache();
}

async function loadAndCache(url: string, key: string, opts: { thumb?: boolean } = {}): Promise<string> {
  const myGen = cacheGeneration;
  const isThumb = !!opts.thumb;
  try {
    const e = await idbGet(key);
    if (e && Date.now() - e.timestamp < TTL_MS) {
      const blobUrl = URL.createObjectURL(e.blob);
      if (myGen !== cacheGeneration) {
        try { URL.revokeObjectURL(blobUrl); } catch {}
        return TRANSPARENT_PNG;
      }
      setMemoryCache(key, blobUrl);
      // 老 entry 已有 w/h 时直接 hydrate 到 dimsCache（v-cse-html 同步可读）
      if (typeof e.w === 'number' && typeof e.h === 'number' && e.w > 0 && e.h > 0) {
        dimsCache.set(key, { w: e.w, h: e.h });
      }
      return blobUrl;
    }
  } catch {}

  let blob: Blob | null = null;
  if (isCseUrl(url)) {
    const fid = parseCseFid(url);
    if (!fid) {
      if (myGen === cacheGeneration) {
        recordFailure(key, new Error('parse-fail'));
        markDecryptFailed(key);
      }
      return TRANSPARENT_PNG;
    }
    try {
      blob = await decryptFileById(fid, { mime: 'image/*', thumb: isThumb });
    } catch (e) {
      if (isThumb) {
        console.warn('[CSE] thumb decrypt fail, fallback to original', fid, e);
        try {
          blob = await decryptFileById(fid, { mime: 'image/*', thumb: false });
        } catch (e2) {
          console.warn('[CSE] original decrypt also fail', fid, e2);
          if (myGen === cacheGeneration) {
            recordFailure(key, e2);
            markDecryptFailed(key);
          }
          return TRANSPARENT_PNG;
        }
      } else {
        console.warn('[CSE] decrypt fail', fid, e);
        if (myGen === cacheGeneration) {
          recordFailure(key, e);
          markDecryptFailed(key);
        }
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
    if (isCseUrl(url)) {
      if (myGen === cacheGeneration) {
        recordFailure(key, new Error('blob-size-invalid'));
        markDecryptFailed(key);
      }
      return TRANSPARENT_PNG;
    }
    return url;
  }
  const blobUrl = URL.createObjectURL(blob);
  if (myGen !== cacheGeneration) {
    try { URL.revokeObjectURL(blobUrl); } catch {}
    return isCseUrl(url) ? TRANSPARENT_PNG : url;
  }
  clearFailure(key);
  setMemoryCache(key, blobUrl);
  // 异步探测尺寸 → 写入 dims 内存 + IDB（带 w/h）。
  // 不阻塞 blob URL 的返回（图片立即显示），下次刷新 v-cse-html 就能预占尺寸消除 CLS。
  const blobRef = blob;
  probeImageDims(blobUrl)
    .then((dims) => {
      if (dims && dims.w > 0 && dims.h > 0) {
        dimsCache.set(key, dims);
        idbPut({ key, blob: blobRef, timestamp: Date.now(), w: dims.w, h: dims.h })
          .then(() => evictIdbIfNeeded())
          .catch(() => {});
      } else {
        idbPut({ key, blob: blobRef, timestamp: Date.now() })
          .then(() => evictIdbIfNeeded())
          .catch(() => {});
      }
    })
    .catch(() => {
      idbPut({ key, blob: blobRef, timestamp: Date.now() })
        .then(() => evictIdbIfNeeded())
        .catch(() => {});
    });
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

const mediaReactive = shallowReactive(new Map<string, string>());
const mediaRefCount = new Map<string, number>();
const pendingMedia = new Map<string, Promise<string>>();

function buildMediaKey(url: string): string {
  return `media:${currentUserId()}:${url}`;
}

/**
 * 视频/音频缓存：与图片独立 IDB store + 30MB 单文件上限
 * 
 * 流程：
 *   1. 内存 hit → 直接返回 blob URL（同步）
 *   2. 内存 miss + IDB hit → 异步 hydrate（createObjectURL + 写内存 + 触发 reactive 刷新），
 *      省掉了一次 /sys/secure/file/{fid}/key 取密 + /sys/secure/file/{fid} 拉密文 + AES 解密
 *   3. 全 miss → decryptFileById（含 inFlight 去重）→ 写 IDB（异步，不阻塞 UI）→ 写内存
 */
export function withMediaCache(url: string, mime?: string): string {
  if (!url) return '';
  if (!isCseUrl(url)) return url;

  const hit = mediaReactive.get(url);
  if (hit) return hit;

  if (!pendingMedia.has(url) && canAttempt(url)) {
    const fid = parseCseFid(url);
    if (!fid) {
      recordFailure(url, new Error('parse-fail'));
      return '';
    }
    const myGen = cacheGeneration;
    const p = loadMediaFromCacheOrDecrypt(url, fid, mime || 'application/octet-stream', myGen)
      .then((blobUrl) => {
        if (myGen !== cacheGeneration) {
          if (blobUrl) {
            try { URL.revokeObjectURL(blobUrl); } catch {}
          }
          return '';
        }
        if (blobUrl) {
          clearFailure(url);
          mediaReactive.set(url, blobUrl);
        }
        return blobUrl;
      })
      .catch((e) => {
        console.warn('[withMediaCache] load fail', fid, e?.message || e);
        if (myGen !== cacheGeneration) return '';
        recordFailure(url, e);
        return '';
      })
      .finally(() => {
        pendingMedia.delete(url);
      });
    pendingMedia.set(url, p);
  }

  return '';
}

/**
 * 视频/音频加载主流程：
 *   IDB hit + TTL 内 → 直接 createObjectURL 返回（省网络 + 省解密）
 *   IDB miss / 过期 → decryptFileById → 回写 IDB（≤30MB 才存）
 */
async function loadMediaFromCacheOrDecrypt(
  url: string,
  fid: string,
  mime: string,
  myGen: number,
): Promise<string> {
  const cacheKey = buildMediaKey(url);

  // 第一步：先查 IDB
  try {
    const e = await idbGet(cacheKey, STORE_NAME_MEDIA);
    if (e && Date.now() - e.timestamp < TTL_MS) {
      // 检测 generation 是否已变（用户切账号 / clearImageCache）
      if (myGen !== cacheGeneration) return '';
      const blobUrl = URL.createObjectURL(e.blob);
      return blobUrl;
    }
  } catch {}

  // 第二步：IDB miss → 实际解密
  const blob = await decryptFileById(fid, { mime });
  if (myGen !== cacheGeneration) {
    return '';
  }
  const blobUrl = URL.createObjectURL(blob);

  // 第三步：异步回写 IDB（不阻塞返回，单文件 ≤30MB 才入库）
  if (blob.size > 0 && blob.size <= MAX_MEDIA_SIZE) {
    idbPut({ key: cacheKey, blob, timestamp: Date.now() }, STORE_NAME_MEDIA)
      .then(() => evictMediaIdbIfNeeded())
      .catch(() => {});
  }
  return blobUrl;
}

async function evictMediaIdbIfNeeded(): Promise<void> {
  try {
    const keys = await idbGetAllKeys(STORE_NAME_MEDIA);
    if (keys.length <= MAX_MEDIA_IDB_ENTRIES) return;
    const all = await idbGetAll(STORE_NAME_MEDIA);
    all.sort((a, b) => a.timestamp - b.timestamp);
    const toRemove = all.length - MAX_MEDIA_IDB_ENTRIES;
    for (let i = 0; i < toRemove; i++) {
      await idbDelete(all[i].key, STORE_NAME_MEDIA);
    }
  } catch {}
}

export function retryMedia(canonicalUrl: string): void {
  if (!canonicalUrl) return;
  failureRegistry.delete(canonicalUrl);
  pendingMedia.delete(canonicalUrl);
  const old = mediaReactive.get(canonicalUrl);
  if (old) {
    try { URL.revokeObjectURL(old); } catch {}
  }
  mediaReactive.delete(canonicalUrl);
}

export function getMediaFailureState(canonicalUrl: string): { failed: boolean; exhausted: boolean } {
  if (!canonicalUrl) return { failed: false, exhausted: false };
  return getFailureState(canonicalUrl);
}

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
 * 清空内存中的视频 blob URL（onUnmounted / restartConversation 调用）。
 * IDB 持久化数据保留，下次进页面 withMediaCache 命中 IDB 即秒开。
 * logout 场景请改用 clearImageCache（会一并清 IDB）。
 */
export function releaseAllMedia(): void {
  bumpGeneration();
  for (const blobUrl of mediaReactive.values()) {
    try {
      URL.revokeObjectURL(blobUrl);
    } catch {}
  }
  mediaReactive.clear();
  mediaRefCount.clear();
  pendingMedia.clear();
  failureRegistry.clear();
}

