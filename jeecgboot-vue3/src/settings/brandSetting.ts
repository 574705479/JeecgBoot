export const BRAND_STORAGE_KEY = 'CS_BRAND_CONFIG';

export const DEFAULT_BRAND = {
  title: '客服系统',
  shortTitle: '客服系统',
  subtitle: '欢迎使用客服系统',
  logoUrl: '/logo.svg',
  faviconUrl: '/logo.svg',
  loginBgUrl: '',
  loadingTitle: '客服系统',
};

function getStoredBrand(): Record<string, string> {
  try {
    const raw = window.localStorage.getItem(BRAND_STORAGE_KEY);
    return raw ? JSON.parse(raw) : {};
  } catch (e) {
    return {};
  }
}

// 运行时可通过 window.__APP_BRAND__ 覆盖（便于不改代码直接替换）
export function getBrandSetting() {
  const runtimeBrand = (window as any).__APP_BRAND__ || {};
  const storedBrand = getStoredBrand();
  const merged = { ...storedBrand, ...runtimeBrand };
  return {
    // 系统名称（浏览器标题、登录页、侧边栏等统一使用）
    appTitle: merged.title || DEFAULT_BRAND.title,
    appShortTitle: merged.shortTitle || DEFAULT_BRAND.shortTitle,
    // 登录页副标题
    appSubtitle: merged.subtitle || DEFAULT_BRAND.subtitle,
    // Logo 资源（public 下路径）
    logoUrl: merged.logoUrl || DEFAULT_BRAND.logoUrl,
    faviconUrl: merged.faviconUrl || DEFAULT_BRAND.faviconUrl,
    loginBgUrl: merged.loginBgUrl || DEFAULT_BRAND.loginBgUrl,
    loadingTitle: merged.loadingTitle || DEFAULT_BRAND.loadingTitle,
  };
}
