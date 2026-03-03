package org.jeecg.modules.airag.cs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.airag.cs.entity.CsFileHash;

/**
 * 文件哈希秒传记录 Service
 *
 * @author jeecg
 * @date 2026-03-03
 */
public interface ICsFileHashService extends IService<CsFileHash> {

    /**
     * 根据 MD5 和文件大小查询已有记录
     */
    CsFileHash findByMd5AndSize(String md5Hash, long fileSize);

    /**
     * 保存文件哈希记录（并发安全，重复插入时静默跳过）
     */
    void saveFileHashIgnoreDuplicate(String md5Hash, String filePath, long fileSize, String fileName, String bizPath);
}
