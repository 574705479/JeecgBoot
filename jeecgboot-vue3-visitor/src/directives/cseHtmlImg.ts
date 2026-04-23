/**
 * 全局指令 v-cse-html：富文本 / v-html 内 cse:// 图片异步解密渲染
 *
 * 设计要点：
 *  - 缓存层完全委托给 /@/utils/file/imageCache（withImageCacheAsync）：
 *    自动获得内存 LRU + IndexedDB 持久化（7 天 TTL）+ 失败重试退避 + cseReactive 触发
 *  - 与附件图片共享缓存，同一张图既出现在富文本又出现在 image grid 时只解密一次
 *  - 本地 failCount 仅做日志降噪，不重复实现失败抑制
 *  - pending 去重已在 imageCache 内部实现，本文件不再二次维护
 */
import type { App, Directive } from 'vue';
import { isCseUrl } from '/@/utils/cse/cseUrl';
import {
  withImageCacheAsync,
  getImageBlobIfReady,
  getImageDimsIfReady,
  whenImageCacheReady,
  clearImageCache,
} from '/@/utils/file/imageCache';

const TRANSPARENT_PNG =
  'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=';

const failCount = new Map<string, number>();
const MAX_FAIL = 2;

export function getCseBlobIfReady(cseUrl: string): string | null {
  return getImageBlobIfReady(cseUrl);
}

export function resolveCseBlob(cseUrl: string): Promise<string> {
  if (!isCseUrl(cseUrl)) return Promise.resolve(TRANSPARENT_PNG);
  if ((failCount.get(cseUrl) || 0) >= MAX_FAIL) {
    return Promise.resolve(TRANSPARENT_PNG);
  }
  // 委托给 imageCache：内存 hit → IDB hit → decrypt 三级链路全自动
  return withImageCacheAsync(cseUrl).then((u) => {
    if (u && u.startsWith('blob:')) {
      failCount.delete(cseUrl);
      return u;
    }
    // imageCache 失败兜底返回 TRANSPARENT_PNG（cse 场景），此时计数避免无限重试日志
    const n = (failCount.get(cseUrl) || 0) + 1;
    failCount.set(cseUrl, n);
    if (n <= 1) {
      console.warn('[cseHtmlImg] decrypt fail', cseUrl);
    }
    return TRANSPARENT_PNG;
  });
}

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

/**
 * 用已知尺寸给 img 元素预占位，消除 unsized image 加载后的 CLS（layout shift）。
 * 仅在用户没在 HTML 里写 width/height 也没写 style.aspect-ratio 时设置。
 * 用 inline style + CSS aspect-ratio 而非 attribute width/height，避免和富文本里
 * 既有的 max-width/height 冲突，让浏览器自动计算最终尺寸。
 */
function applyImgDimsHint(img: HTMLImageElement, dims: { w: number; h: number } | null): void {
  if (!dims || dims.w <= 0 || dims.h <= 0) return;
  const hasUserSize =
    img.hasAttribute('width') ||
    img.hasAttribute('height') ||
    /(?:^|;)\s*(width|height|aspect-ratio)\s*:/i.test(img.getAttribute('style') || '');
  if (hasUserSize) return;
  if (img.getAttribute('data-cse-dims-applied') === '1') return;
  img.setAttribute('data-cse-dims-applied', '1');
  // aspect-ratio 让浏览器在已知容器宽度时按比例预留高度，比硬编码 width/height 更适合响应式
  const prevStyle = img.getAttribute('style') || '';
  const sep = prevStyle && !prevStyle.endsWith(';') ? ';' : '';
  img.setAttribute('style', `${prevStyle}${sep}aspect-ratio:${dims.w}/${dims.h};`);
}

function processImg(img: HTMLImageElement): void {
  let cseUrl = img.getAttribute('data-cse-src') || '';
  const currentSrc = img.getAttribute('src') || '';
  if (!cseUrl && isCseUrl(currentSrc)) {
    cseUrl = currentSrc;
    img.setAttribute('data-cse-src', cseUrl);
  }

  const dataSrc = img.getAttribute('data-src') || '';
  if (!cseUrl && isCseUrl(dataSrc)) {
    cseUrl = dataSrc;
    img.setAttribute('data-cse-src', cseUrl);
  }

  if (cseUrl) {
    // 优先 dims 预占位（来自 IDB 的真实图片宽高），消除富文本 unsized image 引起的 CLS
    const dimsNow = getImageDimsIfReady(cseUrl);
    if (dimsNow) {
      applyImgDimsHint(img, dimsNow);
    } else {
      // imageCache init 还没跑完时 dimsCache 是空的；init 完成后回头再补一次。
      // 此时图片可能还没解码完成，setAttribute aspect-ratio 仍能让浏览器在最终绘制时按比例占位。
      whenImageCacheReady().then(() => {
        if (!img.isConnected) return;
        applyImgDimsHint(img, getImageDimsIfReady(cseUrl));
      });
    }

    const ready = getImageBlobIfReady(cseUrl);
    if (ready) {
      if (img.getAttribute('src') !== ready) img.setAttribute('src', ready);
    } else {
      if (img.getAttribute('src') !== TRANSPARENT_PNG && currentSrc.indexOf('blob:') !== 0) {
        img.setAttribute('src', TRANSPARENT_PNG);
      }
      const observer = getLazyObserver();
      if (observer) {
        observer.observe(img);
      } else {
        resolveCseBlob(cseUrl).then((u) => {
          if (img.isConnected) img.setAttribute('src', u);
        });
      }
    }
  }

  const srcset = img.getAttribute('srcset') || '';
  if (srcset && srcset.indexOf('cse://') >= 0 && !img.getAttribute('data-cse-srcset-processing')) {
    img.setAttribute('data-cse-srcset-processing', '1');
    rewriteSrcset(img, 'srcset', srcset);
  }
}

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

export function rewriteCseImages(root: HTMLElement | null | undefined): void {
  if (!root) return;
  root.querySelectorAll('img').forEach((img) => processImg(img as HTMLImageElement));
  root.querySelectorAll('source').forEach((src) => processSource(src as HTMLSourceElement));
}

/**
 * 字符串级 dims 预注入：v-html 写入 DOM 之前给 cse:// img 标签直接补 inline aspect-ratio。
 * 解决 v-cse-html.mounted 钩子永远晚于 Vue 插入 DOM 引发的"先 unsized 渲染再补尺寸"那一帧 CLS。
 *
 * 仅在 dimsCache 已 hydrate 命中时生效（IDB 之前缓存过的图）；首次出现的新图保持原行为，
 * 由后续 loadAndCache 异步探测 + 下次刷新起消除 CLS。
 *
 * 不会破坏已有 width/height/aspect-ratio：检测到任意一个就跳过该 img，
 * 让用户在富文本编辑器里手写的尺寸优先。
 */
const CSE_IMG_RE = /<img\b([^>]*)\bsrc=(["'])(cse:\/\/[a-zA-Z0-9_-]+)\2([^>]*)>/gi;
export function preinjectCseImageDimsInHtml(html: string): string {
  if (!html || html.indexOf('cse://') < 0) return html;
  return html.replace(CSE_IMG_RE, (full, pre: string, q: string, url: string, post: string) => {
    // 用户已在 HTML 里写了尺寸/比例 → 不覆盖
    const merged = `${pre} ${post}`;
    if (/\b(?:width|height)\s*=/i.test(merged)) return full;
    if (/aspect-ratio\s*:/i.test(merged)) return full;
    const dims = getImageDimsIfReady(url);
    if (!dims || dims.w <= 0 || dims.h <= 0) return full;
    const ar = `aspect-ratio:${dims.w}/${dims.h};`;
    const styleMatch = post.match(/(\bstyle=)(["'])([^"']*)\2/i);
    let newPost: string;
    if (styleMatch) {
      const oldStyle = styleMatch[3] || '';
      const sep = oldStyle && !oldStyle.endsWith(';') ? ';' : '';
      const newStyle = `${oldStyle}${sep}${ar}`;
      newPost = post.replace(styleMatch[0], `${styleMatch[1]}${q}${newStyle}${q}`);
    } else {
      newPost = ` style="${ar}"${post}`;
    }
    return `<img${pre}src=${q}${url}${q}${newPost}>`;
  });
}

export const vCseHtml: Directive<HTMLElement> = {
  mounted(el) {
    rewriteCseImages(el);
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

/**
 * 兼容旧 API：内部委托给 imageCache.clearImageCache（统一清空内存 + IDB + DEK）。
 * 一般在 logout / 切租户时调用。
 */
export function clearCseHtmlImgCache(): void {
  failCount.clear();
  clearImageCache();
}

export function setupCseHtmlDirective(app: App): void {
  app.directive('cse-html', vCseHtml);
}
