import { defHttp } from '/@/utils/http/axios';

export type BizCategory = 'ENCRYPT' | 'PUBLIC';

export interface CseBizDef {
  path: string;
  name: string;
  description: string;
  category: BizCategory;
  forceLocked: boolean;
}

export interface CseConfigVO {
  enabled: boolean;
  encryptedPaths: string[];
  publicPaths: string[];
  dictionary: CseBizDef[];
  customEncrypted: string[];
  customPublic: string[];
}

export interface CseDryRunReq {
  bizPath: string;
  mode: 'current' | 'preview';
  previewEncrypted?: string[];
  previewPublic?: string[];
  previewEnabled?: boolean;
}

export interface CseDryRunVO {
  bizPath: string;
  mode: string;
  enabled: boolean;
  shouldEncrypt: boolean;
  matchedRule?: string;
  reason?: string;
}

export interface CseSaveReq {
  enabled: boolean;
  encryptedPaths: string[];
  publicPaths: string[];
  password: string;
}

/** 读取当前配置 + 字典 + 自定义部分 */
export function getCseConfig() {
  return defHttp.get<CseConfigVO>({ url: '/sys/cse/config' });
}

/** 保存配置（需二次密码） */
export function saveCseConfig(data: CseSaveReq) {
  return defHttp.put<string>({ url: '/sys/cse/config', data });
}

/** 命中测试：可用 current 或 preview 模式 */
export function dryRunPath(req: CseDryRunReq) {
  return defHttp.post<CseDryRunVO>({ url: '/sys/cse/config/dryRun', data: req });
}
