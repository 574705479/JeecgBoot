/**
 * 1:1 复制自主项目 src/settings/brandSetting.ts
 * 仅 DEFAULT_BRAND 的 logoUrl/faviconUrl 改为 assetUrl()，
 * 以便在 prod base path（/visitor/）下也能正确解析本地兜底资源。
 */
import { assetUrl } from '/@/utils/asset';

export const BRAND_STORAGE_KEY = 'CS_BRAND_CONFIG';

export const DEFAULT_BRAND = {
  title: '在线客服',
  shortTitle: '在线客服',
  subtitle: '欢迎使用在线客服',
  logoUrl: assetUrl('logo.svg'),
  faviconUrl: assetUrl('logo.svg'),
  loginBgUrl: '',
  loadingTitle: '在线客服',
};

function getStoredBrand(): Record<string, string> {
  try {
    const raw = window.localStorage.getItem(BRAND_STORAGE_KEY);
    return raw ? JSON.parse(raw) : {};
  } catch (e) {
    return {};
  }
}

export function getBrandSetting() {
  const runtimeBrand = (window as any).__APP_BRAND__ || {};
  const storedBrand = getStoredBrand();
  const merged = { ...storedBrand, ...runtimeBrand };
  return {
    appTitle: merged.title || DEFAULT_BRAND.title,
    appShortTitle: merged.shortTitle || DEFAULT_BRAND.shortTitle,
    appSubtitle: merged.subtitle || DEFAULT_BRAND.subtitle,
    logoUrl: merged.logoUrl || DEFAULT_BRAND.logoUrl,
    faviconUrl: merged.faviconUrl || DEFAULT_BRAND.faviconUrl,
    loginBgUrl: merged.loginBgUrl || DEFAULT_BRAND.loginBgUrl,
    loadingTitle: merged.loadingTitle || DEFAULT_BRAND.loadingTitle,
  };
}
