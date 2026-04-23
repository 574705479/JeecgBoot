/**
 * 极简 compUtils（访客端用）—— 主项目 utils/common/compUtils.ts 含 30+ 业务工具，
 * 访客端只需要 getFileAccessHttpUrl + getHeaders（FileChip 不用，不放入）。
 */
import { useGlobSetting } from '/@/hooks/setting';

const globSetting = useGlobSetting();
const baseApiUrl = globSetting.domainUrl;

/**
 * 获取文件服务访问路径
 * @param fileUrl 文件路径
 * @param prefix(默认 http) 文件路径前缀 http/https
 */
export const getFileAccessHttpUrl = (fileUrl: string, prefix: string = 'http'): string => {
  let result = fileUrl;
  try {
    // CSE 端到端加密资源原样返回，由上层 withImageCache 解密
    if (typeof fileUrl === 'string' && fileUrl.indexOf('cse://') === 0) {
      return fileUrl;
    }
    if (fileUrl && fileUrl.length > 0 && !fileUrl.startsWith(prefix)) {
      const isArray = fileUrl.indexOf('[') != -1;
      if (!isArray) {
        const fullPrefix = `${baseApiUrl}/sys/common/static/`;
        if (!fileUrl.startsWith(fullPrefix)) {
          result = `${fullPrefix}${fileUrl}`;
        }
      }
    }
  } catch (err) {}
  return result;
};
