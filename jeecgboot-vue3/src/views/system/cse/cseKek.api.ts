import { defHttp } from '/@/utils/http/axios';

export interface CseKekVO {
  kid: string;
  status: 'ACTIVE' | 'STAGED' | 'DEPRECATED' | 'ARCHIVED';
  createdBy?: string;
  createdTime?: string;
  activatedTime?: string;
  deprecatedTime?: string;
  lastUsedTime?: string;
  fileCount?: number;
  remark?: string;
}

export interface CseKekAuditLogVO {
  id: number;
  kid?: string;
  action: string;
  operatorId?: string;
  operatorName?: string;
  operatorIp?: string;
  operateTime: string;
  remark?: string;
}

export function listKek() {
  return defHttp.get<CseKekVO[]>({ url: '/sys/cse/kek/list' });
}

export function listKekAudit(limit = 100) {
  return defHttp.get<CseKekAuditLogVO[]>({ url: '/sys/cse/kek/audit', params: { limit } });
}

export function generateKek(password: string, remark?: string) {
  return defHttp.post<CseKekVO>({ url: '/sys/cse/kek/generate', data: { password, remark } });
}

export function activateKek(password: string, kid: string) {
  return defHttp.post<string>({ url: '/sys/cse/kek/activate', data: { password, kid } });
}

export function archiveKek(password: string, kid: string) {
  return defHttp.post<string>({ url: '/sys/cse/kek/archive', data: { password, kid } });
}

/** 导出加密 zip：直接 fetch 到 blob */
export async function exportKekZip(password: string, zipPassword: string): Promise<Blob> {
  const res = await defHttp.post(
    {
      url: '/sys/cse/kek/export',
      data: { password, zipPassword },
      responseType: 'blob',
    },
    { isReturnNativeResponse: true },
  );
  return res.data as Blob;
}

export function importKekZip(formData: FormData) {
  return defHttp.post<number>({
    url: '/sys/cse/kek/import',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}
