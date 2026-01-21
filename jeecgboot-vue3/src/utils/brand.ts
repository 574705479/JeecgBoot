import { defHttp } from '/@/utils/http/axios';
import { BRAND_STORAGE_KEY } from '/@/settings/brandSetting';
import { getFileAccessHttpUrl } from '/@/utils/common/compUtils';

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
    const data = res?.result || res;
    const normalized = normalizeBrand(data);
    if (Object.keys(normalized).length) {
      window.__APP_BRAND__ = Object.assign({}, window.__APP_BRAND__ || {}, normalized);
      window.localStorage.setItem(BRAND_STORAGE_KEY, JSON.stringify(normalized));
      applyBrandToDom(normalized);
    }
    return data || null;
  } catch (e) {
    return null;
  }
}

export function resolveBrandUrl(url?: string) {
  if (!url) return '';
  if (/^https?:\/\//i.test(url)) return url;
  if (url.startsWith('/')) return url;
  return getFileAccessHttpUrl(url);
}

export function applyBrandToDom(brand: Record<string, string>) {
  if (!brand) return;
  const title = brand.title;
  if (title) {
    document.title = title;
  }
  const faviconUrl = brand.faviconUrl || brand.logoUrl;
  if (faviconUrl) {
    const finalUrl = resolveBrandUrl(faviconUrl);
    const withCache = `${finalUrl}${finalUrl.includes('?') ? '&' : '?'}t=${Date.now()}`;
    const iconLinks = document.querySelectorAll("link[rel='icon'], link[rel='shortcut icon']");
    if (iconLinks.length) {
      iconLinks.forEach((node) => {
        (node as HTMLLinkElement).href = withCache;
      });
    } else {
      const link = document.createElement('link');
      link.rel = 'icon';
      link.href = withCache;
      document.head.appendChild(link);
    }
  }
  const loadingEl = document.getElementById('app-loading-title');
  if (loadingEl) {
    loadingEl.textContent = brand.loadingTitle || title || loadingEl.textContent || '';
  }
}
