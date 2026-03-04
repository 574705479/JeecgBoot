import { defHttp } from '/@/utils/http/axios';
import { BRAND_STORAGE_KEY } from '/@/settings/brandSetting';
import { getFileAccessHttpUrl } from '/@/utils/common/compUtils';
import { useGlobSetting } from '/@/hooks/setting';

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
      // 预解析资源URL并存入localStorage，供 index.html 加载页直接使用
      if (normalized.logoUrl) {
        normalized._resolvedLogoUrl = resolveBrandUrl(normalized.logoUrl);
      }
      if (normalized.faviconUrl) {
        normalized._resolvedFaviconUrl = resolveBrandUrl(normalized.faviconUrl);
      }
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
  // 更新 loading logo
  const logoUrl = brand.logoUrl;
  if (logoUrl) {
    const logoEl = document.getElementById('app-loading-logo') as HTMLImageElement | null;
    if (logoEl) {
      logoEl.src = resolveBrandUrl(logoUrl);
    }
  }
}
