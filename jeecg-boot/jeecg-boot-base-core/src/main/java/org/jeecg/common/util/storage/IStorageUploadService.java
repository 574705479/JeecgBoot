package org.jeecg.common.util.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 全站统一上传（读取 DB 存储配置或回退 jeecg.uploadType yml）
 */
public interface IStorageUploadService {

    String upload(MultipartFile file, String bizPath);

    /**
     * 在线图片字节上传（替代 CommonUtils.uploadOnlineImage 中随 uploadType 分支）
     */
    String uploadOnlineImage(byte[] data, String uploadRootPath, String bizPath);

    /**
     * 当前是否为本地磁盘（秒传校验等）
     */
    boolean isEffectiveLocal();

}
