/**
 * 访客端极简版 useGlobSetting
 *
 * 替代主项目 src/hooks/setting/index.ts —— 后者依赖 GlobConfig、qiankun、Electron、CAS 等十多项配置，
 * 访客端一律用不到。这里只提供 imageCache / cseDecrypt / brand 真正消费的字段：
 *   - domainUrl：所有后端请求的绝对前缀
 *   - apiUrl / urlPrefix：与主项目 axios 行为对齐
 *   - isElectronPlatform：访客端不可能在 electron，固定 false
 */
import { getAppEnvConfig } from '/@/utils/env';

export interface VisitorGlobConfig {
  domainUrl: string;
  apiUrl: string;
  urlPrefix: string;
  uploadUrl: string;
  isElectronPlatform: boolean;
}

let _cached: VisitorGlobConfig | null = null;

export function useGlobSetting(): Readonly<VisitorGlobConfig> {
  if (_cached) return _cached;
  const { VITE_GLOB_DOMAIN_URL, VITE_GLOB_API_URL, VITE_GLOB_API_URL_PREFIX } = getAppEnvConfig();
  let domainUrl = VITE_GLOB_DOMAIN_URL || '';
  // 与主项目同样的相对路径补全：'/jeecgboot' → 'http://current-host/jeecgboot'
  if (domainUrl && !/^https?:\/\//.test(domainUrl) && !/^\/\//.test(domainUrl)) {
    if (!domainUrl.startsWith('/')) domainUrl = '/' + domainUrl;
    if (typeof window !== 'undefined') {
      domainUrl = window.location.origin + domainUrl;
    }
  }
  _cached = {
    domainUrl,
    apiUrl: VITE_GLOB_API_URL || '',
    urlPrefix: VITE_GLOB_API_URL_PREFIX || '',
    uploadUrl: domainUrl,
    isElectronPlatform: false,
  };
  if (typeof window !== 'undefined') {
    if (!window._CONFIG) window._CONFIG = {};
    window._CONFIG['domianURL'] = domainUrl;
  }
  return _cached;
}
