/**
 * 全局指令 v-cse-html：富文本 / v-html 内 cse:// 图片异步解密渲染
 *
 * 原本在 `views/super/airag/cs/utils/cseHtmlImg.ts`，本次（t0d）迁到全局：
 *  - 注册为全局 directive，所有 <div v-html v-cse-html> 节点直接可用，无需 import
 *  - 扩展支持：<img src> / <img srcset> / <img data-src> / <source src> / <source srcset>
 *  - 扩展支持：MutationObserver 监听容器内动态新增的 <img>（如 Vditor 编辑器输入图片）
 *  - 解密失败：保留透明占位，不抛错，不无限重试
 *  - 旧路径 `cs/utils/cseHtmlImg.ts` 仍以 re-export 形式存在，保证现有 import 兼容
 *
 * 设计要点（t0b 一致）：
 *  - 进程内 blobMap 长生命周期（持有，不参与 LRU），由 clearAllCseCache 统一清理
 *  - pending 去重并发请求
 *  - failCount 抑制反复重试与日志刷屏
 */
import type { App, Directive } from 'vue';
import { isCseUrl, parseCseFid } from '/@/utils/cse/cseUrl';
import { decryptFileToObjectUrl } from '/@/utils/cse/cseDecrypt';

const TRANSPARENT_PNG =
  'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=';

/** cse:// URL → blob URL（解密成功后写入，进程内长生命周期） */
const cseBlobMap = new Map<string, string>();
/** 解密中的 Promise，去重并发请求 */
const pending = new Map<string, Promise<string>>();
/** 解密失败次数（短期内连续失败超过阈值则跳过，避免日志刷屏） */
const failCount = new Map<string, number>();
const MAX_FAIL = 2;

export function getCseBlobIfReady(cseUrl: string): string | null {
  return cseBlobMap.get(cseUrl) || null;
}

/** 异步解密 cse:// URL，命中缓存直接返回；失败返回透明占位 */
export function resolveCseBlob(cseUrl: string): Promise<string> {
  const cached = cseBlobMap.get(cseUrl);
  if (cached) return Promise.resolve(cached);

  const ex = pending.get(cseUrl);
  if (ex) return ex;

  if ((failCount.get(cseUrl) || 0) >= MAX_FAIL) {
    return Promise.resolve(TRANSPARENT_PNG);
  }

  const fid = parseCseFid(cseUrl);
  if (!fid) return Promise.resolve(TRANSPARENT_PNG);

  const p = decryptFileToObjectUrl(fid, { mime: 'image/*' })
    .then((blobUrl) => {
      cseBlobMap.set(cseUrl, blobUrl);
      failCount.delete(cseUrl);
      return blobUrl;
    })
    .catch((e) => {
      const n = (failCount.get(cseUrl) || 0) + 1;
      failCount.set(cseUrl, n);
      if (n <= 1) {
        console.warn('[cseHtmlImg] decrypt fail', fid, e?.message || e);
      }
      return TRANSPARENT_PNG;
    })
    .finally(() => {
      pending.delete(cseUrl);
    });

  pending.set(cseUrl, p);
  return p;
}

/**
 * 【t28】视口懒解密：进入视口附近 ±200px 才触发解密，长聊天历史不打爆 server
 * 全局共享一个 IntersectionObserver，所有挂指令的容器内 img 都注册到它
 */
const VIEWPORT_OBSERVER_MARGIN = '200px';
let lazyObserver: IntersectionObserver | null = null;
function getLazyObserver(): IntersectionObserver | null {
  if (typeof IntersectionObserver === 'undefined') return null;
  if (lazyObserver) return lazyObserver;
  lazyObserver = new IntersectionObserver(
    (entries) => {
      for (const ent of entries) {
        if (!ent.isIntersecting) continue;
        const img = ent.target as HTMLImageElement;
        const cseUrl = img.getAttribute('data-cse-src');
        if (!cseUrl) continue;
        // 触发解密 + 写回 src
        resolveCseBlob(cseUrl).then((u) => {
          if (img.isConnected) img.setAttribute('src', u);
        });
        lazyObserver?.unobserve(img);
      }
    },
    { rootMargin: VIEWPORT_OBSERVER_MARGIN, threshold: 0.01 },
  );
  return lazyObserver;
}

/** 处理单个 <img>：备份 cse:// 原值到 data-cse-src，src 占位，进入视口后异步解密回写 */
function processImg(img: HTMLImageElement): void {
  // 1. src 处理
  let cseUrl = img.getAttribute('data-cse-src') || '';
  const currentSrc = img.getAttribute('src') || '';
  if (!cseUrl && isCseUrl(currentSrc)) {
    cseUrl = currentSrc;
    img.setAttribute('data-cse-src', cseUrl);
  }

  // 2. data-src（懒加载库）处理
  const dataSrc = img.getAttribute('data-src') || '';
  if (!cseUrl && isCseUrl(dataSrc)) {
    cseUrl = dataSrc;
    img.setAttribute('data-cse-src', cseUrl);
  }

  if (cseUrl) {
    const ready = cseBlobMap.get(cseUrl);
    if (ready) {
      if (img.getAttribute('src') !== ready) img.setAttribute('src', ready);
    } else {
      if (img.getAttribute('src') !== TRANSPARENT_PNG && currentSrc.indexOf('blob:') !== 0) {
        img.setAttribute('src', TRANSPARENT_PNG);
      }
      // 【t28】视口懒解密：注册到 IntersectionObserver，进入视口后才解密
      const observer = getLazyObserver();
      if (observer) {
        observer.observe(img);
      } else {
        // Fallback：浏览器不支持 IntersectionObserver，立即解密（与改造前一致）
        resolveCseBlob(cseUrl).then((u) => {
          if (img.isConnected) img.setAttribute('src', u);
        });
      }
    }
  }

  // 3. srcset 处理（多源描述符）—— 不走视口懒加载（srcset 通常用于响应式图，性能影响小）
  const srcset = img.getAttribute('srcset') || '';
  if (srcset && srcset.indexOf('cse://') >= 0 && !img.getAttribute('data-cse-srcset-processing')) {
    img.setAttribute('data-cse-srcset-processing', '1');
    rewriteSrcset(img, 'srcset', srcset);
  }
}

/** 处理单个 <source>：与 <img> 类似，但 source 没有 src 显示行为，主要是 srcset */
function processSource(src: HTMLSourceElement): void {
  const srcAttr = src.getAttribute('src') || '';
  if (isCseUrl(srcAttr)) {
    src.setAttribute('data-cse-src', srcAttr);
    src.setAttribute('src', TRANSPARENT_PNG);
    resolveCseBlob(srcAttr).then((u) => {
      if (src.isConnected) src.setAttribute('src', u);
    });
  }
  const srcset = src.getAttribute('srcset') || '';
  if (srcset && srcset.indexOf('cse://') >= 0 && !src.getAttribute('data-cse-srcset-processing')) {
    src.setAttribute('data-cse-srcset-processing', '1');
    rewriteSrcset(src, 'srcset', srcset);
  }
}

/** srcset 解析与异步回写：'cse://a 1x, cse://b 2x' → 解密后回写 */
function rewriteSrcset(el: Element, attr: string, srcset: string): void {
  const items = srcset.split(',').map((s) => s.trim());
  const parts: Array<{ url: string; descriptor: string; isCse: boolean }> = items
    .map((s) => {
      const sp = s.split(/\s+/);
      const url = sp[0] || '';
      const descriptor = sp.slice(1).join(' ');
      return { url, descriptor, isCse: isCseUrl(url) };
    })
    .filter((p) => !!p.url);

  Promise.all(
    parts.map((p) => (p.isCse ? resolveCseBlob(p.url) : Promise.resolve(p.url))),
  ).then((resolved) => {
    if (!el.isConnected) return;
    const newSrcset = parts
      .map((p, i) => (p.descriptor ? `${resolved[i]} ${p.descriptor}` : resolved[i]))
      .join(', ');
    el.setAttribute(attr, newSrcset);
    el.removeAttribute('data-cse-srcset-processing');
  });
}

/** 扫描容器内所有 <img> / <source>，处理 cse:// 源 */
export function rewriteCseImages(root: HTMLElement | null | undefined): void {
  if (!root) return;
  root.querySelectorAll('img').forEach((img) => processImg(img as HTMLImageElement));
  root.querySelectorAll('source').forEach((src) => processSource(src as HTMLSourceElement));
}

/** 全局 directive：v-cse-html */
export const vCseHtml: Directive<HTMLElement> = {
  mounted(el) {
    rewriteCseImages(el);
    // 监听容器内动态新增的 <img>（如 Vditor 编辑期粘贴新图、富文本服务端流式追加）
    try {
      const observer = new MutationObserver((mutations) => {
        for (const m of mutations) {
          for (const node of Array.from(m.addedNodes)) {
            if (node.nodeType !== 1) continue;
            const elNode = node as HTMLElement;
            if (elNode.tagName === 'IMG') {
              processImg(elNode as unknown as HTMLImageElement);
            } else if (elNode.tagName === 'SOURCE') {
              processSource(elNode as unknown as HTMLSourceElement);
            } else {
              // 容器节点：递归处理子树
              elNode.querySelectorAll?.('img').forEach((img) => processImg(img as HTMLImageElement));
              elNode.querySelectorAll?.('source').forEach((src) => processSource(src as HTMLSourceElement));
            }
          }
        }
      });
      observer.observe(el, { childList: true, subtree: true });
      (el as any).__cseHtmlObserver = observer;
    } catch {}
  },
  updated(el) {
    rewriteCseImages(el);
  },
  unmounted(el) {
    try {
      (el as any).__cseHtmlObserver?.disconnect();
      (el as any).__cseHtmlObserver = null;
    } catch {}
  },
};

/** 全局清理（退出登录 / 401 时调用，由 clearAllCseCache 统一调度） */
export function clearCseHtmlImgCache(): void {
  for (const u of cseBlobMap.values()) {
    try {
      URL.revokeObjectURL(u);
    } catch {}
  }
  cseBlobMap.clear();
  pending.clear();
  failCount.clear();
}

/** 注册为全局指令：app.directive('cse-html', vCseHtml) */
export function setupCseHtmlDirective(app: App): void {
  app.directive('cse-html', vCseHtml);
}
