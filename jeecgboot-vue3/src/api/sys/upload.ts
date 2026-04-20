import { UploadApiResult } from './model/uploadModel';
import { defHttp } from '/@/utils/http/axios';
import { UploadFileParams } from '/#/axios';
import { useGlobSetting } from '/@/hooks/setting';

const { uploadUrl = '' } = useGlobSetting();

/**
 * @description: Upload interface
 */
export function uploadApi(params: UploadFileParams, onUploadProgress: (progressEvent: ProgressEvent) => void) {
  return defHttp.uploadFile<UploadApiResult>(
    {
      url: uploadUrl,
      onUploadProgress,
    },
    params
  );
}
/**
 * @description: Upload interface
 */
export function uploadImg(params: UploadFileParams, onUploadProgress: (progressEvent: ProgressEvent) => void) {
  return defHttp.uploadFile<UploadApiResult>(
    {
      url: `${uploadUrl}/sys/common/upload`,
      onUploadProgress,
    },
    params,
    { isReturnResponse: true }
  );
}

/**
 * 品牌图专用上传：强制 biz=cs-brand。
 *
 * 配套 CsBrandFileController.bizPath 严格校验（"cs-brand" / "cs-brand/" 前缀），
 * 防止越权管理员把任意他人 fid 塞入 brand 字段后通过匿名代理端点泄漏。
 *
 * 仅供 cs/brand 配置页使用，不影响其他 CropperUpload 调用方（继续用 uploadImg）。
 */
export function uploadBrandImg(params: UploadFileParams, onUploadProgress: (progressEvent: ProgressEvent) => void) {
  const merged: UploadFileParams = {
    ...params,
    data: { ...(params.data || {}), biz: 'cs-brand' },
  };
  return defHttp.uploadFile<UploadApiResult>(
    {
      url: `${uploadUrl}/sys/common/upload`,
      onUploadProgress,
    },
    merged,
    { isReturnResponse: true }
  );
}

/**
 * 头像专用上传：强制 biz=avatar/cs-agent。
 *
 * 配套 OssFileMetaService.canRead 对 avatar/ 前缀放行（已登录用户 + 持访客 token 均放行，
 * 跳过租户隔离），解决 admin 跨租户给客服换头像后客服自己看不到的问题。
 *
 * prod yml 已预置 cse.encrypted-paths 含 avatar/，路径自动走 CSE 加密链路。
 */
export function uploadAvatarImg(params: UploadFileParams, onUploadProgress: (progressEvent: ProgressEvent) => void) {
  const merged: UploadFileParams = {
    ...params,
    data: { ...(params.data || {}), biz: 'avatar/cs-agent' },
  };
  return defHttp.uploadFile<UploadApiResult>(
    {
      url: `${uploadUrl}/sys/common/upload`,
      onUploadProgress,
    },
    merged,
    { isReturnResponse: true }
  );
}
