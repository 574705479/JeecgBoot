import { defHttp } from '/@/utils/http/axios';

export interface StorageConfigVO {
  effectiveSource?: string;
  ymlUploadType?: string;
  ymlUploadPath?: string;
  storageType?: string;
  aliyunEndpoint?: string;
  aliyunBucket?: string;
  aliyunAccessKeyId?: string;
  aliyunSecretConfigured?: boolean;
  aliyunStaticDomain?: string;
  aliyunTransferAccel?: boolean;
  aliyunRoleArn?: string;
  tencentRegion?: string;
  tencentBucket?: string;
  tencentSecretId?: string;
  tencentSecretKeyConfigured?: boolean;
  tencentDomain?: string;
  tencentGlobalAccel?: boolean;
  remark?: string;
  updateBy?: string;
  updateTime?: string;
}

export interface StorageConfigSaveDTO {
  storageType: string;
  aliyunEndpoint?: string;
  aliyunBucket?: string;
  aliyunAccessKeyId?: string;
  aliyunAccessKeySecret?: string;
  aliyunStaticDomain?: string;
  aliyunTransferAccel?: boolean;
  aliyunRoleArn?: string;
  tencentRegion?: string;
  tencentBucket?: string;
  tencentSecretId?: string;
  tencentSecretKey?: string;
  tencentDomain?: string;
  tencentGlobalAccel?: boolean;
  remark?: string;
}

export function getStorageConfig() {
  return defHttp.get<StorageConfigVO>({ url: '/sys/storage/config' });
}

/** 保存前连通性检测（不持久化） */
export function testStorageConnection(data: StorageConfigSaveDTO) {
  return defHttp.post<string>(
    { url: '/sys/storage/config/test', data },
    { errorMessageMode: 'none' },
  );
}

export function saveStorageConfig(data: StorageConfigSaveDTO) {
  return defHttp.put<string>({ url: '/sys/storage/config', data });
}
