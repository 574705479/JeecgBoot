/**
 * 1:1 复制自 jeecgboot-vue3/src/directives/cseHtmlImg.ts
 *
 * 全局指令 v-cse-html：富文本 / v-html 内 cse:// 图片异步解密渲染
 *
 * 设计要点：
 *  - 进程内 blobMap 长生命周期（持有，不参与 LRU），由 clearAllCseCache 统一清理
 *  - pending 去重并发请求
 *  - failCount 抑制反复重试与日志刷屏
 */
import type { App, Directive } from 'vue';
import { isCseUrl, parseCseFid } from '/@/utils/cse/cseUrl';
import { decryptFileToObjectUrl } from '/@/utils/cse/cseDecrypt';

const TRANSPARENT_PNG =
  'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=';

const cseBlobMap = new Map<string, string>();
const pending = new Map<string, Promise<string>>();
const failCount = new Map<string, number>();
const MAX_FAIL = 2;

export function getCseBlobIfReady(cseUrl: string): string | null {
  return cseBlobMap.get(cseUrl) || null;
}

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
    const ready = cseBlobMap.get(cseUrl);
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

export function setupCseHtmlDirective(app: App): void {
  app.directive('cse-html', vCseHtml);
}
