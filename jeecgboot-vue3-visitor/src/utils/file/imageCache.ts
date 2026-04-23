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

