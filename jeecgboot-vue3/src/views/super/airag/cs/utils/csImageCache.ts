/**
 * 图片缓存工具 —— 两级缓存（内存 Map + IndexedDB）
 *
 * 设计原则：
 *  - 任何阶段异常均降级返回原始 URL，最坏情况 = 无缓存（当前行为）
 *  - 非响应式 Map：不触发 Vue 重渲染，避免图片闪烁
 *  - 仅缓存配置类图片（头像/Logo/广告），不缓存消息附件
 */

const DB_NAME = 'cs_image_cache';
const STORE_NAME = 'images';
const DB_VERSION = 1;
const MAX_IDB_ENTRIES = 200;
const MAX_MEM_ENTRIES = 100;
const MAX_SINGLE_SIZE = 5 * 1024 * 1024; // 5MB
const TTL_MS = 7 * 24 * 60 * 60 * 1000; // 7 天

// ─── 内存缓存 ───────────────────────────────────────
const memoryCache = new Map<string, string>(); // url -> blobURL
const memoryAccessOrder: string[] = []; // LRU 顺序

// ─── 请求去重 ────────────────────────────────────────
const pendingRequests = new Map<string, Promise<string>>();

// ─── IndexedDB 操作 ──────────────────────────────────

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
          db.createObjectStore(STORE_NAME, { keyPath: 'url' });
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
  url: string;
  blob: Blob;
  timestamp: number;
}

function idbGet(url: string): Promise<CacheEntry | undefined> {
  return openDB().then(
    (db) =>
      new Promise((resolve) => {
        const tx = db.transaction(STORE_NAME, 'readonly');
        const store = tx.objectStore(STORE_NAME);
        const req = store.get(url);
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
        const store = tx.objectStore(STORE_NAME);
        store.put(entry);
        tx.oncomplete = () => resolve();
        tx.onerror = () => resolve();
      }),
    () => {},
  );
}

function idbGetAllKeys(): Promise<string[]> {
  return openDB().then(
    (db) =>
      new Promise((resolve) => {
        const tx = db.transaction(STORE_NAME, 'readonly');
        const store = tx.objectStore(STORE_NAME);
        const req = store.getAllKeys();
        req.onsuccess = () => resolve(req.result as string[]);
        req.onerror = () => resolve([]);
      }),
    () => [],
  );
}

function idbDelete(url: string): Promise<void> {
  return openDB().then(
    (db) =>
      new Promise((resolve) => {
        const tx = db.transaction(STORE_NAME, 'readwrite');
        const store = tx.objectStore(STORE_NAME);
        store.delete(url);
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
        const store = tx.objectStore(STORE_NAME);
        const req = store.getAll();
        req.onsuccess = () => resolve(req.result as CacheEntry[]);
        req.onerror = () => resolve([]);
      }),
    () => [],
  );
}

// ─── 内存缓存管理 ────────────────────────────────────

function touchMemory(url: string) {
  const idx = memoryAccessOrder.indexOf(url);
  if (idx > -1) memoryAccessOrder.splice(idx, 1);
  memoryAccessOrder.push(url);
}

function setMemoryCache(url: string, blobUrl: string) {
  // LRU 淘汰
  while (memoryCache.size >= MAX_MEM_ENTRIES && memoryAccessOrder.length > 0) {
    const oldest = memoryAccessOrder.shift()!;
    const oldBlob = memoryCache.get(oldest);
    if (oldBlob) {
      try { URL.revokeObjectURL(oldBlob); } catch {}
    }
    memoryCache.delete(oldest);
  }
  memoryCache.set(url, blobUrl);
  touchMemory(url);
}

// ─── URL 校验 ────────────────────────────────────────

function isCacheableUrl(url: string): boolean {
  if (!url) return false;
  if (url.startsWith('data:') || url.startsWith('blob:')) return false;
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('/')) return true;
  return false;
}

// ─── 核心 API ────────────────────────────────────────

/**
 * 初始化：从 IndexedDB 加载所有未过期条目到内存 Map。
 * 应在 main.ts 中与 loadBrandConfig 并行调用。
 */
export async function initImageCache(): Promise<void> {
  try {
    const entries = await idbGetAll();
    const now = Date.now();
    for (const entry of entries) {
      if (now - entry.timestamp > TTL_MS) {
        idbDelete(entry.url).catch(() => {});
        continue;
      }
      try {
        const blobUrl = URL.createObjectURL(entry.blob);
        setMemoryCache(entry.url, blobUrl);
      } catch {}
    }
  } catch {
    // IndexedDB 不可用，静默降级
  }
}

/**
 * 同步获取缓存图片 URL。
 * - 命中内存：返回 blobURL
 * - 未命中：返回原始 URL，并触发后台加载（下次调用时命中）
 */
export function withImageCache(url: string): string {
  if (!isCacheableUrl(url)) return url;

  const cached = memoryCache.get(url);
  if (cached) {
    touchMemory(url);
    return cached;
  }

  // 后台异步加载（不阻塞当前渲染）
  if (!pendingRequests.has(url)) {
    const p = loadAndCache(url).finally(() => pendingRequests.delete(url));
    pendingRequests.set(url, p);
  }

  return url;
}

/**
 * 预热：批量预加载图片到缓存。不阻塞调用方。
 */
export function preloadImages(urls: (string | undefined | null)[]): void {
  for (const url of urls) {
    if (url && isCacheableUrl(url)) {
      withImageCache(url);
    }
  }
}

/**
 * 图片加载错误降级处理。
 * 在 <img @error="onImageError"> 中使用。
 */
export function onImageError(e: Event, fallbackUrl?: string) {
  const img = e.target as HTMLImageElement;
  if (!img || img.dataset.fallbackApplied) return;
  img.dataset.fallbackApplied = 'true';
  img.src = fallbackUrl || '/logo.svg';
}

/**
 * 清理：释放所有 blob URL。
 * 在 beforeunload 时调用（可选，进程退出时也会自动释放）。
 */
export function cleanupImageCache(): void {
  for (const blobUrl of memoryCache.values()) {
    try { URL.revokeObjectURL(blobUrl); } catch {}
  }
  memoryCache.clear();
  memoryAccessOrder.length = 0;
  pendingRequests.clear();
}

// ─── 内部：加载并写入两级缓存 ────────────────────────

async function loadAndCache(url: string): Promise<string> {
  // 先查 IndexedDB
  try {
    const entry = await idbGet(url);
    if (entry && Date.now() - entry.timestamp < TTL_MS) {
      const blobUrl = URL.createObjectURL(entry.blob);
      setMemoryCache(url, blobUrl);
      return blobUrl;
    }
  } catch {}

  // 网络请求
  try {
    const resp = await fetch(url, { credentials: 'omit' });
    if (!resp.ok) return url;

    const blob = await resp.blob();
    if (blob.size > MAX_SINGLE_SIZE || blob.size === 0) return url;

    const blobUrl = URL.createObjectURL(blob);
    setMemoryCache(url, blobUrl);

    // 异步写入 IndexedDB（不阻塞返回）
    idbPut({ url, blob, timestamp: Date.now() })
      .then(() => evictIdbIfNeeded())
      .catch(() => {});

    return blobUrl;
  } catch {
    return url;
  }
}

async function evictIdbIfNeeded(): Promise<void> {
  try {
    const keys = await idbGetAllKeys();
    if (keys.length <= MAX_IDB_ENTRIES) return;

    const entries = await idbGetAll();
    entries.sort((a, b) => a.timestamp - b.timestamp);

    const toRemove = entries.length - MAX_IDB_ENTRIES;
    for (let i = 0; i < toRemove; i++) {
      await idbDelete(entries[i].url);
    }
  } catch {}
}

// ─── 聊天窗口配置 localStorage 缓存 ─────────────────

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
