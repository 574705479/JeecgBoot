import type { GlobConfig } from '/#/config';

import { getAppEnvConfig } from '/@/utils/env';
import { getBrandSetting } from '/@/settings/brandSetting';

export const useGlobSetting = (): Readonly<GlobConfig> => {
  const {
    VITE_GLOB_APP_TITLE,
    VITE_GLOB_API_URL,
    VITE_GLOB_APP_SHORT_NAME,
    VITE_GLOB_API_URL_PREFIX,
    VITE_GLOB_APP_CAS_BASE_URL,
    VITE_GLOB_APP_OPEN_SSO,
    VITE_GLOB_APP_OPEN_QIANKUN,
    VITE_GLOB_DOMAIN_URL,
    VITE_GLOB_ONLINE_VIEW_URL,
    VITE_GLOB_RUN_PLATFORM,

    // 【JEECG作为乾坤子应用】
    VITE_GLOB_QIANKUN_MICRO_APP_NAME,
    VITE_GLOB_QIANKUN_MICRO_APP_ENTRY,
  } = getAppEnvConfig();

  // if (!/[a-zA-Z\_]*/.test(VITE_GLOB_APP_SHORT_NAME)) {
  //   warn(
  //     `VITE_GLOB_APP_SHORT_NAME Variables can only be characters/underscores, please modify in the environment variables and re-running.`
  //   );
  // }

  const brand = getBrandSetting();
  const appTitle = brand.appTitle || VITE_GLOB_APP_TITLE;
  const appShortTitle = brand.appShortTitle || VITE_GLOB_APP_SHORT_NAME;
  const shortTitle = appShortTitle.replace(/_/g, ' ');

  // 【QQYUN-10956】配置了自定义前缀，外部连接打不开，需要兼容处理
  // 将相对路径的 domainURL 拼接当前页面域名，确保 WebSocket/上传等场景获得完整 URL
  let domainURL = VITE_GLOB_DOMAIN_URL;
  if (!/^http(s)?/.test(domainURL) && !/^(\/\/)?(.*\.)?.+\..+/.test(domainURL)) {
    if (!domainURL.startsWith('/')) {
      domainURL = '/' + domainURL;
    }
    domainURL = window.location.origin + domainURL;
  }

  const glob: Readonly<GlobConfig> = {
    title: appTitle,
    domainUrl: domainURL,
    apiUrl: VITE_GLOB_API_URL,
    shortName: appShortTitle,
    shortTitle: shortTitle,
    openSso: VITE_GLOB_APP_OPEN_SSO,
    openQianKun: VITE_GLOB_APP_OPEN_QIANKUN,
    casBaseUrl: VITE_GLOB_APP_CAS_BASE_URL,
    urlPrefix: VITE_GLOB_API_URL_PREFIX,
    uploadUrl: domainURL,
    viewUrl: VITE_GLOB_ONLINE_VIEW_URL,
    useNewTaskModal: true,
    isElectronPlatform: VITE_GLOB_RUN_PLATFORM === 'electron',
    isQiankunMicro: VITE_GLOB_QIANKUN_MICRO_APP_NAME != null && VITE_GLOB_QIANKUN_MICRO_APP_NAME !== '',
    qiankunMicroAppEntry: VITE_GLOB_QIANKUN_MICRO_APP_ENTRY,
  };

  if (!window['_CONFIG']) {
    window['_CONFIG'] = {}
  }
  // @ts-ignore
  window._CONFIG['domianURL'] = domainURL;

  return glob as Readonly<GlobConfig>;
};
