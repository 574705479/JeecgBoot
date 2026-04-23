/**
 * 访客端静态资源路径工具：所有指向 public/ 的本地兜底资源（logo、默认头像等）
 * 必须经过 assetUrl() 拼接 import.meta.env.BASE_URL，
 * 否则在 prod base path（/visitor/）下会请求到错误的根路径并 404。
 */
export function assetUrl(path: string): string {
  if (!path) return path;
  if (/^(https?:|data:|blob:|cse:)/i.test(path)) return path;
  const base = (import.meta.env.BASE_URL || '/').replace(/\/+$/, '/');
  const trimmed = path.replace(/^\/+/, '');
  return base.endsWith('/') ? base + trimmed : `${base}/${trimmed}`;
}

export const DEFAULT_LOGO_URL = assetUrl('logo.svg');
export const DEFAULT_USER_AVATAR_URL = assetUrl('default-user-avatar.png');
