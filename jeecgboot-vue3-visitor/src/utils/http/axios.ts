/**
 * 访客端极简 axios 封装
 *
 * 设计目标：
 *   - 提供与主项目 defHttp 相同的调用签名：
 *       defHttp.get<T>({ url, params, headers, ... }, options)
 *       defHttp.post / put / delete 同理
 *   - 访客端不依赖全局 token 拦截器（业务层显式 buildAuthHeaders 注入 4 种鉴权 header）
 *   - 不依赖 useI18n / errorLog / signMd5 / lodash-es 等主项目重型基础设施
 *
 * options 兼容字段：
 *   - isTransformResponse (默认 true)：true → 直接返回 result；false → 返回 res.data 全量
 *   - isReturnNativeResponse (默认 false)：true → 返回原始 AxiosResponse
 *   - errorMessageMode  ('message' | 'modal' | 'none'，默认 'message')
 *   - successMessageMode ('success' | 'none'，默认 'none')
 *   - joinPrefix (默认 true)
 *   - apiUrl / urlPrefix 显式覆盖
 */
import axios, { AxiosRequestConfig, AxiosResponse, AxiosInstance } from 'axios';
import { message as antdMessage, Modal } from 'ant-design-vue';
import { useGlobSetting } from '/@/hooks/setting';
import { isString } from '/@/utils/is';

export interface RequestOptions {
  joinPrefix?: boolean;
  apiUrl?: string;
  urlPrefix?: string;
  isTransformResponse?: boolean;
  isReturnNativeResponse?: boolean;
  errorMessageMode?: 'none' | 'modal' | 'message';
  successMessageMode?: 'none' | 'success';
  joinTime?: boolean;
  ignoreRepeatRequest?: boolean;
}

export interface RequestConfig extends Omit<AxiosRequestConfig, 'url'> {
  url?: string;
}

const globSetting = useGlobSetting();
const DEFAULT_BASE = globSetting.domainUrl || '';
const DEFAULT_API = globSetting.apiUrl || '';
const DEFAULT_PREFIX = globSetting.urlPrefix || '';
const DEFAULT_TIMEOUT = 60_000;

function joinTimestamp(join: boolean): Record<string, any> {
  return join ? { _t: Date.now() } : {};
}

function showError(mode: NonNullable<RequestOptions['errorMessageMode']>, msg: string) {
  if (!msg || mode === 'none') return;
  if (mode === 'modal') {
    Modal.error({ title: '请求出错', content: msg });
  } else {
    antdMessage.error(msg);
  }
}

function checkStatus(status: number, msg: string): string {
  let m = msg;
  switch (status) {
    case 400:
      m = msg || '请求参数错误';
      break;
    case 401:
      m = msg || '未授权或登录已过期';
      break;
    case 403:
      m = msg || '禁止访问';
      break;
    case 404:
      m = msg || '请求资源不存在';
      break;
    case 405:
      m = msg || '请求方法不被允许';
      break;
    case 408:
      m = msg || '请求超时';
      break;
    case 500:
      m = msg || '服务器内部错误';
      break;
    case 502:
      m = msg || '网关错误';
      break;
    case 503:
      m = msg || '服务不可用';
      break;
    case 504:
      m = msg || '网关超时';
      break;
    default:
      m = msg || `请求失败（${status}）`;
  }
  return m;
}

const instance: AxiosInstance = axios.create({
  baseURL: '',
  timeout: DEFAULT_TIMEOUT,
  withCredentials: false,
  headers: { 'Content-Type': 'application/json;charset=UTF-8' },
});

// 请求拦截器：仅做 query timestamp（防缓存）；token 由业务层显式注入 headers
instance.interceptors.request.use(
  (config) => {
    if (config.method?.toLowerCase() === 'get') {
      config.params = { ...(config.params || {}), ...joinTimestamp(true) };
    }
    return config;
  },
  (err) => Promise.reject(err),
);

// 响应拦截器：仅透传，所有业务转换在 transform 内
instance.interceptors.response.use(
  (res) => res,
  (err) => Promise.reject(err),
);

function transformResponse<T = any>(res: AxiosResponse<any>, options: RequestOptions): T {
  const { isTransformResponse = true, isReturnNativeResponse = false } = options;
  if (isReturnNativeResponse) return res as any;
  if (!isTransformResponse) return res.data;
  const data = res.data;
  if (!data) {
    throw new Error('请求返回数据为空');
  }
  const { code, result, success, message } = data;
  const ok = Reflect.has(data, 'code') && (code === 200 || code === 0);
  if (ok) {
    if (success && message && options.successMessageMode === 'success') {
      antdMessage.success(message);
    }
    return result as T;
  }
  showError(options.errorMessageMode || 'message', message || '请求失败');
  throw new Error(message || '请求失败');
}

function buildUrl(config: RequestConfig, options: RequestOptions): string {
  const apiUrl = options.apiUrl != null ? options.apiUrl : DEFAULT_API;
  const urlPrefix = options.urlPrefix != null ? options.urlPrefix : DEFAULT_PREFIX;
  let url = config.url || '';
  if (!url) return '';
  if (/^https?:\/\//i.test(url)) return url;
  if (options.joinPrefix !== false && urlPrefix && !url.startsWith(urlPrefix)) {
    url = `${urlPrefix}${url}`;
  }
  if (apiUrl && isString(apiUrl)) {
    url = `${DEFAULT_BASE}${url}`;
  }
  return url;
}

async function request<T = any>(config: RequestConfig, options: RequestOptions = {}): Promise<T> {
  const finalUrl = buildUrl(config, options);
  const cfg: AxiosRequestConfig = {
    ...config,
    url: finalUrl,
  };
  // 注入租户头（访客端 fixed '0'）
  cfg.headers = {
    ...(cfg.headers || {}),
    'X-Tenant-Id': '0',
  };
  try {
    const res = await instance.request(cfg);
    return transformResponse<T>(res, options);
  } catch (err: any) {
    if (err?.response) {
      const status = err.response.status;
      const data = err.response.data;
      const msg = data?.message || data?.msg || err.message || '';
      const finalMsg = checkStatus(status, msg);
      showError(options.errorMessageMode || 'message', finalMsg);
      throw new Error(finalMsg);
    }
    if (err?.message?.includes('Network Error')) {
      showError(options.errorMessageMode || 'message', '网络错误，请检查网络连接');
    } else if (err?.message?.includes('timeout')) {
      showError(options.errorMessageMode || 'message', '请求超时，请稍后重试');
    }
    throw err;
  }
}

export const defHttp = {
  get<T = any>(config: RequestConfig, options?: RequestOptions): Promise<T> {
    return request<T>({ ...config, method: 'GET' }, options);
  },
  post<T = any>(config: RequestConfig, options?: RequestOptions): Promise<T> {
    return request<T>({ ...config, method: 'POST' }, options);
  },
  put<T = any>(config: RequestConfig, options?: RequestOptions): Promise<T> {
    return request<T>({ ...config, method: 'PUT' }, options);
  },
  delete<T = any>(config: RequestConfig, options?: RequestOptions): Promise<T> {
    return request<T>({ ...config, method: 'DELETE' }, options);
  },
  request<T = any>(config: RequestConfig, options?: RequestOptions): Promise<T> {
    return request<T>(config, options);
  },
};

export default defHttp;
