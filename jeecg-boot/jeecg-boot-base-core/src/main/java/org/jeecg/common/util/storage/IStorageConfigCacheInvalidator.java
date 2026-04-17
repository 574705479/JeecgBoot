package org.jeecg.common.util.storage;

/**
 * 存储桶配置缓存失效接口
 * 在 SysStorageConfig 增删改时调用 invalidate()，强制下一次上传重新读 DB。
 */
public interface IStorageConfigCacheInvalidator {
    /** 立即清空已缓存的存储配置和已构建的客户端 */
    void invalidate();
}
