import { defHttp } from '/@/utils/http/axios';
import { BRAND_STORAGE_KEY } from '/@/settings/brandSetting';
import { getFileAccessHttpUrl } from '/@/utils/common/compUtils';
import { useGlobSetting } from '/@/hooks/setting';
import { isCseUrl, parseCseFid } from '/@/utils/cse/cseUrl';
// 仅 csImageCache 仍保留预热（已转发到 utils/file/imageCache）；本文件不再使用 withImageCacheAsync
import { preloadImages } from '/@/utils/file/imageCache';

type BrandConfig = {
  appTitle?: string;
  appShortTitle?: string;
  appSubtitle?: string;
  logoUrl?: string;
  faviconUrl?: string;
  loginBgUrl?: string;
  loadingTitle?: string;
};

function normalizeBrand(raw: any): Record<string, string> {
  if (!raw) return {};
  return {
    title: raw.appTitle || raw.title,
    shortTitle: raw.appShortTitle || raw.shortTitle,
    subtitle: raw.appSubtitle || raw.subtitle,
    logoUrl: raw.logoUrl,
    faviconUrl: raw.faviconUrl,
    loginBgUrl: raw.loginBgUrl,
    loadingTitle: raw.loadingTitle,
  };
}

export async function loadBrandConfig(): Promise<BrandConfig | null> {
  try {
    const res = await defHttp.get(
      { url: '/cs/brand/get' },
      { isTransformResponse: false, errorMessageMode: 'none' }
    );
    let rawData = res?.result || res;
    if (typeof rawData === 'string') {
      try {
        const { decryptTransport } = await import('/@/views/super/airag/cs/utils/csEncrypt');
        rawData = JSON.parse(decryptTransport(rawData));
      } catch { /* fallback to raw */ }
    }
    const data = rawData;
    const normalized = normalizeBrand(data);
    if (Object.keys(normalized).length) {
      // 预解析资源URL并存入localStorage，供 index.html 加载页直接使用
      if (normalized.logoUrl) {
        normalized._resolvedLogoUrl = resolveBrandPublicUrl(normalized.logoUrl);
      }
      if (normalized.faviconUrl) {
        normalized._resolvedFaviconUrl = resolveBrandPublicUrl(normalized.faviconUrl);
      }
      window.__APP_BRAND__ = Object.assign({}, window.__APP_BRAND__ || {}, normalized);
      window.localStorage.setItem(BRAND_STORAGE_KEY, JSON.stringify(normalized));
      // 预热到统一缓存（http(s) 的话顺便走 fetch 加载；cse:// 已经通过公开端点走 HTTP，不需 imageCache 解密）
      preloadImages([normalized._resolvedLogoUrl, normalized._resolvedFaviconUrl]);
      applyBrandToDom(normalized);
    }
    return data || null;
  } catch (e) {
    return null;
  }
}

/**
 * 把品牌字段（可能是 cse://fid / http(s):// / 相对路径）解析为浏览器可直接消费的公开 URL。
 *
 * 关键差异 vs resolveBrandUrl：
 *  - cse://fid → /jeecg-boot/cs/brand/file/{fid}（匿名代理端点，无 token 也能访问）
 *  - http(s) / data: / blob: → 原样返回
 *  - 相对路径 → 走 getFileAccessHttpUrl 拼接
 *
 * 用途：所有需要让浏览器/邮件/favicon 直接加载品牌图的地方都用这个函数，
 * 避免依赖前端 cse 解密链路（解密链路需要 token，登录前不可用）。
 */
export function resolveBrandPublicUrl(input?: string): string {
  if (!input) return '';
  if (isCseUrl(input)) {
    const fid = parseCseFid(input);
    if (!fid) return '';
    const { domainUrl } = useGlobSetting();
    // domainUrl 形如 http://host:port/jeecg-boot
    // 拼接出来就是绝对 URL，避免任何环境下的相对路径歧义
    return `${domainUrl || ''}/cs/brand/file/${fid}`;
  }
  if (/^https?:\/\//i.test(input) || input.startsWith('data:') || input.startsWith('blob:')) {
    return input;
  }
  return resolveBrandUrl(input);
}

export function resolveBrandUrl(url?: string) {
  if (!url) return '';
  if (/^https?:\/\//i.test(url)) return url;
  if (url.startsWith('/')) {
    const { isElectronPlatform, domainUrl } = useGlobSetting();
    if (isElectronPlatform) {
      // Electron file:// 下根路径会解析到磁盘根目录
      // 简单文件名（如 /logo.svg）转为相对路径，API 路径拼接后端地址
      const segments = url.split('/').filter(Boolean);
      if (segments.length === 1) {
        return '.' + url;
      }
      return domainUrl + url;
    }
    return url;
  }
  return getFileAccessHttpUrl(url);
}

function simpleHash(str: string): string {
  let hash = 5381;
  for (let i = 0; i < str.length; i++) {
    hash = ((hash << 5) + hash + str.charCodeAt(i)) & 0x7fffffff;
  }
  return hash.toString(36);
}

let _lastFaviconPath = '';

/**
 * 应用品牌配置到 DOM：title / favicon / loading-title / loading-logo。
 *
 * 同步化：之前用 withImageCacheAsync（依赖前端 cse 解密链路 + token），
 * 现改用同步 resolveBrandPublicUrl 直接生成 HTTP URL（cse:// 走匿名代理端点）。
 * 不依赖登录态，不阻塞首屏。
 *
 * 注意 #app-loading-logo：vue mount 后 #app 容器被替换 → loading-logo 元素已销毁。
 * 这里赋值会 silent fail；客户清缓存首次访问 loading 阶段必然显示默认 logo（约 200-500ms）。
 * 这是预期行为，参见 plan F12。
 */
export function applyBrandToDom(brand: Record<string, string>) {
  if (!brand) return;
  const title = brand.title;
  if (title) {
    document.title = title;
  }

  // ─── favicon ────────────────────────────────────────
  const faviconUrl = brand.faviconUrl || brand.logoUrl;
  if (faviconUrl) {
    const finalUrl = resolveBrandPublicUrl(faviconUrl);
    if (finalUrl) {
      let href = finalUrl;
      if (faviconUrl !== _lastFaviconPath) {
        _lastFaviconPath = faviconUrl;
        // 我们的匿名代理端点已带 1h 缓存，用 hash 强制刷新覆盖；blob:/data: 不加版本
        if (!finalUrl.startsWith('blob:') && !finalUrl.startsWith('data:')) {
          href = `${finalUrl}${finalUrl.includes('?') ? '&' : '?'}v=${simpleHash(faviconUrl)}`;
        }
      }
      const iconLinks = document.querySelectorAll("link[rel='icon'], link[rel='shortcut icon']");
      if (iconLinks.length) {
        iconLinks.forEach((node) => {
          (node as HTMLLinkElement).href = href;
        });
      } else {
        const link = document.createElement('link');
        link.rel = 'icon';
        link.href = href;
        document.head.appendChild(link);
      }
    }
  }

  // ─── 首屏 loading 标题 ─────────────────────────────
  const loadingEl = document.getElementById('app-loading-title');
  if (loadingEl) {
    loadingEl.textContent = brand.loadingTitle || title || loadingEl.textContent || '';
  }

  // ─── 首屏 loading logo（注意：vue mount 后此元素已销毁，赋值是 no-op） ──
  const logoUrl = brand.logoUrl;
  if (logoUrl) {
    const logoEl = document.getElementById('app-loading-logo') as HTMLImageElement | null;
    if (logoEl) {
      const finalUrl = resolveBrandPublicUrl(logoUrl);
      if (finalUrl) logoEl.src = finalUrl;
    }
  }
}
