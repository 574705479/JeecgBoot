/**
 * 访客端极简 brand 工具
 *
 * 主项目 utils/brand.ts 含 loadBrandConfig（拉接口 + 写 localStorage + 应用 DOM），
 * 访客端只需要 resolveBrandPublicUrl —— 把 cse://fid 转成 /jeecgboot/cs/brand/file/{fid}。
 */
import { useGlobSetting } from '/@/hooks/setting';
import { isCseUrl, parseCseFid } from '/@/utils/cse/cseUrl';
import { defHttp } from '/@/utils/http/axios';
import { BRAND_STORAGE_KEY } from '/@/settings/brandSetting';
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

/**
 * 把品牌字段（cse://fid / http(s):// / 相对路径）解析为浏览器可直接消费的公开 URL。
 */
export function resolveBrandPublicUrl(input?: string): string {
  if (!input) return '';
  if (isCseUrl(input)) {
    const fid = parseCseFid(input);
    if (!fid) return '';
    const { domainUrl } = useGlobSetting();
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
    return url;
  }
  // 相对路径直接拼 domainUrl
  const { domainUrl } = useGlobSetting();
  return (domainUrl || '') + '/sys/common/static/' + url;
}

function simpleHash(str: string): string {
  let hash = 5381;
  for (let i = 0; i < str.length; i++) {
    hash = ((hash << 5) + hash + str.charCodeAt(i)) & 0x7fffffff;
  }
  return hash.toString(36);
}

let _lastFaviconPath = '';

export function applyBrandToDom(brand: Record<string, string>) {
  if (!brand) return;
  const title = brand.title;
  if (title) {
    document.title = title;
  }

  const faviconUrl = brand.faviconUrl || brand.logoUrl;
  if (faviconUrl) {
    const finalUrl = resolveBrandPublicUrl(faviconUrl);
    if (finalUrl) {
      let href = finalUrl;
      if (faviconUrl !== _lastFaviconPath) {
        _lastFaviconPath = faviconUrl;
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
}

/**
 * 访客端品牌加载（可选）：拉一次 /cs/brand/get，写入 localStorage 缓存 + 应用到 DOM。
 * ChatMain 主体也会读 localStorage 拿默认值。
 */
export async function loadBrandConfig(): Promise<BrandConfig | null> {
  try {
    const res: any = await defHttp.get(
      { url: '/cs/brand/get' },
      { isTransformResponse: false, errorMessageMode: 'none' },
    );
    let rawData = res?.result || res;
    if (typeof rawData === 'string') {
      try {
        const { decryptTransport } = await import('/@/utils/cs/csEncrypt');
        rawData = JSON.parse(decryptTransport(rawData));
      } catch { /* fallback */ }
    }
    const data = rawData;
    const normalized = normalizeBrand(data);
    if (Object.keys(normalized).length) {
      if (normalized.logoUrl) {
        normalized._resolvedLogoUrl = resolveBrandPublicUrl(normalized.logoUrl);
      }
      if (normalized.faviconUrl) {
        normalized._resolvedFaviconUrl = resolveBrandPublicUrl(normalized.faviconUrl);
      }
      window.__APP_BRAND__ = Object.assign({}, window.__APP_BRAND__ || {}, normalized);
      window.localStorage.setItem(BRAND_STORAGE_KEY, JSON.stringify(normalized));
      preloadImages([normalized._resolvedLogoUrl, normalized._resolvedFaviconUrl]);
      applyBrandToDom(normalized);
    }
    return data || null;
  } catch (e) {
    return null;
  }
}
