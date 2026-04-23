/**
 * 访客端环境变量适配层（替代主项目 utils/env.ts）
 *
 * 主项目 getAppEnvConfig 读取 _APP.{key}（dev 用 import.meta.env、prod 用 _app.config.js 覆盖）。
 * 访客端不再需要运行时配置注入，统一从 import.meta.env 读取。
 */
export interface VisitorEnvConfig {
  VITE_GLOB_API_URL: string;
  VITE_GLOB_DOMAIN_URL: string;
  VITE_GLOB_API_URL_PREFIX: string;
}

export function getAppEnvConfig(): VisitorEnvConfig {
  return {
    VITE_GLOB_API_URL: import.meta.env.VITE_GLOB_API_URL || '',
    VITE_GLOB_DOMAIN_URL: import.meta.env.VITE_GLOB_DOMAIN_URL || '',
    VITE_GLOB_API_URL_PREFIX: import.meta.env.VITE_GLOB_API_URL_PREFIX || '',
  };
}

export const isProdMode = (): boolean => import.meta.env.PROD;
export const isDevMode = (): boolean => import.meta.env.DEV;
