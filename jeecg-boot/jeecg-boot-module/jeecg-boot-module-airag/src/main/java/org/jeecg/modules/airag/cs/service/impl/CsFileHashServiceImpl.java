package org.jeecg.modules.airag.cs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.cs.entity.CsFileHash;
import org.jeecg.modules.airag.cs.mapper.CsFileHashMapper;
import org.jeecg.modules.airag.cs.service.ICsFileHashService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 文件哈希秒传记录 Service实现
 *
 * @author jeecg
 * @date 2026-03-03
 */
@Slf4j
@Service
public class CsFileHashServiceImpl extends ServiceImpl<CsFileHashMapper, CsFileHash> implements ICsFileHashService {

    @Override
    public CsFileHash findByMd5AndSize(String md5Hash, long fileSize) {
        LambdaQueryWrapper<CsFileHash> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CsFileHash::getMd5Hash, md5Hash)
               .eq(CsFileHash::getFileSize, fileSize)
               .last("LIMIT 1");
        return this.getOne(wrapper);
    }

    @Override
    public void saveFileHashIgnoreDuplicate(String md5Hash, String filePath, long fileSize, String fileName, String bizPath) {
        try {
            CsFileHash entity = new CsFileHash();
            entity.setMd5Hash(md5Hash);
            entity.setFilePath(filePath);
            entity.setFileSize(fileSize);
            entity.setFileName(fileName);
            entity.setBizPath(bizPath);
            entity.setCreateTime(new Date());
            this.save(entity);
        } catch (DuplicateKeyException e) {
            log.debug("文件哈希记录已存在，跳过: md5={}, size={}", md5Hash, fileSize);
        }
    }
}
