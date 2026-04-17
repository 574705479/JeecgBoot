package org.jeecg.modules.system.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.common.util.storage.IStorageConfigCacheInvalidator;
import org.jeecg.config.JeecgBaseConfig;
import org.jeecg.modules.system.entity.SysStorageConfig;
import org.jeecg.modules.system.mapper.SysStorageConfigMapper;
import org.jeecg.modules.system.model.StorageConfigSaveDTO;
import org.jeecg.modules.system.service.impl.StorageConfigConnectionTestService;
import org.jeecg.modules.system.storage.StorageCredentialCrypto;
import org.jeecg.modules.system.vo.StorageConfigVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 全站存储桶配置（仅 admin）
 */
@Slf4j
@RestController
@RequestMapping("/sys/storage")
public class SysStorageConfigController {

    @Autowired
    private SysStorageConfigMapper storageConfigMapper;
    @Autowired
    private JeecgBaseConfig jeecgBaseConfig;
    @Autowired
    private StorageConfigConnectionTestService storageConfigConnectionTestService;
    @Autowired
    private IStorageConfigCacheInvalidator storageConfigCacheInvalidator;

    @RequiresRoles("admin")
    @GetMapping(value = "/config")
    public Result<StorageConfigVO> getConfig() {
        StorageConfigVO vo = new StorageConfigVO();
        vo.setYmlUploadType(jeecgBaseConfig.getUploadType());
        if (jeecgBaseConfig.getPath() != null) {
            vo.setYmlUploadPath(jeecgBaseConfig.getPath().getUpload());
        }
        SysStorageConfig row = storageConfigMapper.selectById(SysStorageConfig.ID_SINGLETON);
        if (row == null) {
            vo.setEffectiveSource("yml");
            return Result.OK(vo);
        }
        vo.setEffectiveSource("database");
        vo.setId(row.getId());
        vo.setRemark(row.getRemark());
        vo.setUpdateBy(row.getUpdateBy());
        vo.setUpdateTime(row.getUpdateTime());
        String st = SysStorageConfig.normalizeStorageType(row.getStorageType());
        if (st == null) {
            vo.setStorageType(row.getStorageType());
            vo.setAliyunEndpoint(row.getAliyunEndpoint());
            vo.setAliyunBucket(row.getAliyunBucket());
            vo.setAliyunAccessKeyId(row.getAliyunAccessKeyId());
            vo.setAliyunSecretConfigured(oConvertUtils.isNotEmpty(row.getAliyunSecretCipher()));
            vo.setAliyunStaticDomain(row.getAliyunStaticDomain());
            vo.setAliyunTransferAccel(Boolean.TRUE.equals(row.getAliyunTransferAccel()));
            vo.setAliyunRoleArn(row.getAliyunRoleArn());
            vo.setTencentRegion(row.getTencentRegion());
            vo.setTencentBucket(row.getTencentBucket());
            vo.setTencentSecretId(row.getTencentSecretId());
            vo.setTencentSecretKeyConfigured(oConvertUtils.isNotEmpty(row.getTencentSecretKeyCipher()));
            vo.setTencentDomain(row.getTencentDomain());
            vo.setTencentGlobalAccel(Boolean.TRUE.equals(row.getTencentGlobalAccel()));
            return Result.OK(vo);
        }
        vo.setStorageType(st);
        if (SysStorageConfig.TYPE_SYSTEM.equals(st)) {
            vo.setAliyunSecretConfigured(false);
            vo.setTencentSecretKeyConfigured(false);
        } else if (SysStorageConfig.TYPE_ALIYUN.equals(st)) {
            vo.setAliyunEndpoint(row.getAliyunEndpoint());
            vo.setAliyunBucket(row.getAliyunBucket());
            vo.setAliyunAccessKeyId(row.getAliyunAccessKeyId());
            vo.setAliyunSecretConfigured(oConvertUtils.isNotEmpty(row.getAliyunSecretCipher()));
            vo.setAliyunStaticDomain(row.getAliyunStaticDomain());
            vo.setAliyunTransferAccel(Boolean.TRUE.equals(row.getAliyunTransferAccel()));
            vo.setAliyunRoleArn(row.getAliyunRoleArn());
            vo.setTencentSecretKeyConfigured(false);
        } else if (SysStorageConfig.TYPE_TENCENT.equals(st)) {
            vo.setAliyunSecretConfigured(false);
            vo.setTencentRegion(row.getTencentRegion());
            vo.setTencentBucket(row.getTencentBucket());
            vo.setTencentSecretId(row.getTencentSecretId());
            vo.setTencentSecretKeyConfigured(oConvertUtils.isNotEmpty(row.getTencentSecretKeyCipher()));
            vo.setTencentDomain(row.getTencentDomain());
            vo.setTencentGlobalAccel(Boolean.TRUE.equals(row.getTencentGlobalAccel()));
        } else {
            vo.setAliyunEndpoint(row.getAliyunEndpoint());
            vo.setAliyunBucket(row.getAliyunBucket());
            vo.setAliyunAccessKeyId(row.getAliyunAccessKeyId());
            vo.setAliyunSecretConfigured(oConvertUtils.isNotEmpty(row.getAliyunSecretCipher()));
            vo.setAliyunStaticDomain(row.getAliyunStaticDomain());
            vo.setAliyunTransferAccel(Boolean.TRUE.equals(row.getAliyunTransferAccel()));
            vo.setAliyunRoleArn(row.getAliyunRoleArn());
            vo.setTencentRegion(row.getTencentRegion());
            vo.setTencentBucket(row.getTencentBucket());
            vo.setTencentSecretId(row.getTencentSecretId());
            vo.setTencentSecretKeyConfigured(oConvertUtils.isNotEmpty(row.getTencentSecretKeyCipher()));
            vo.setTencentDomain(row.getTencentDomain());
            vo.setTencentGlobalAccel(Boolean.TRUE.equals(row.getTencentGlobalAccel()));
        }
        return Result.OK(vo);
    }

    /**
     * 保存前连通性检测（与 save 使用相同 DTO，密钥留空时读库中密文）
     */
    @RequiresRoles("admin")
    @PostMapping(value = "/config/test")
    public Result<String> testConfig(@RequestBody StorageConfigSaveDTO dto) {
        try {
            SysStorageConfig existing = storageConfigMapper.selectById(SysStorageConfig.ID_SINGLETON);
            storageConfigConnectionTestService.assertConnectionOk(dto, existing);
            return Result.OK("连接正常");
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("[SysStorage] test connection failed", e);
            return Result.error("存储连接检测失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    @RequiresRoles("admin")
    @PutMapping(value = "/config")
    public Result<String> saveConfig(@RequestBody StorageConfigSaveDTO dto) {
        if (dto == null || oConvertUtils.isEmpty(dto.getStorageType())) {
            return Result.error("storageType 不能为空");
        }
        String st = dto.getStorageType().trim().toUpperCase();
        if (!SysStorageConfig.TYPE_SYSTEM.equals(st) && !SysStorageConfig.TYPE_ALIYUN.equals(st) && !SysStorageConfig.TYPE_TENCENT.equals(st)) {
            return Result.error("不支持的存储类型");
        }
        SysStorageConfig existing = storageConfigMapper.selectById(SysStorageConfig.ID_SINGLETON);
        SysStorageConfig entity = existing != null ? existing : new SysStorageConfig();
        entity.setId(SysStorageConfig.ID_SINGLETON);
        entity.setStorageType(st);
        entity.setRemark(trimOrNull(dto.getRemark()));

        if (SysStorageConfig.TYPE_SYSTEM.equals(st)) {
            entity.setAliyunEndpoint(null);
            entity.setAliyunBucket(null);
            entity.setAliyunAccessKeyId(null);
            entity.setAliyunSecretCipher(null);
            entity.setAliyunStaticDomain(null);
            entity.setAliyunTransferAccel(false);
            entity.setAliyunRoleArn(null);
            entity.setTencentRegion(null);
            entity.setTencentBucket(null);
            entity.setTencentSecretId(null);
            entity.setTencentSecretKeyCipher(null);
            entity.setTencentDomain(null);
            entity.setTencentGlobalAccel(false);
        } else if (SysStorageConfig.TYPE_ALIYUN.equals(st)) {
            entity.setAliyunEndpoint(trimOrNull(dto.getAliyunEndpoint()));
            entity.setAliyunBucket(trimOrNull(dto.getAliyunBucket()));
            entity.setAliyunAccessKeyId(trimOrNull(dto.getAliyunAccessKeyId()));
            entity.setAliyunStaticDomain(trimOrNull(dto.getAliyunStaticDomain()));
            entity.setAliyunTransferAccel(dto.getAliyunTransferAccel() != null && dto.getAliyunTransferAccel());
            entity.setAliyunRoleArn(trimOrNull(dto.getAliyunRoleArn()));
            entity.setTencentRegion(null);
            entity.setTencentBucket(null);
            entity.setTencentSecretId(null);
            entity.setTencentSecretKeyCipher(null);
            entity.setTencentDomain(null);
            entity.setTencentGlobalAccel(false);
            if (dto.getAliyunAccessKeySecret() != null && !dto.getAliyunAccessKeySecret().isEmpty()) {
                entity.setAliyunSecretCipher(StorageCredentialCrypto.encryptPlain(dto.getAliyunAccessKeySecret()));
            } else if (existing != null && oConvertUtils.isNotEmpty(existing.getAliyunSecretCipher())) {
                entity.setAliyunSecretCipher(existing.getAliyunSecretCipher());
            } else {
                return Result.error("请填写阿里云 AccessKey Secret");
            }
        } else if (SysStorageConfig.TYPE_TENCENT.equals(st)) {
            entity.setTencentRegion(trimOrNull(dto.getTencentRegion()));
            entity.setTencentBucket(trimOrNull(dto.getTencentBucket()));
            entity.setTencentSecretId(trimOrNull(dto.getTencentSecretId()));
            entity.setTencentDomain(trimOrNull(dto.getTencentDomain()));
            entity.setTencentGlobalAccel(dto.getTencentGlobalAccel() != null && dto.getTencentGlobalAccel());
            entity.setAliyunEndpoint(null);
            entity.setAliyunBucket(null);
            entity.setAliyunAccessKeyId(null);
            entity.setAliyunSecretCipher(null);
            entity.setAliyunStaticDomain(null);
            entity.setAliyunTransferAccel(false);
            entity.setAliyunRoleArn(null);
            if (dto.getTencentSecretKey() != null && !dto.getTencentSecretKey().isEmpty()) {
                entity.setTencentSecretKeyCipher(StorageCredentialCrypto.encryptPlain(dto.getTencentSecretKey()));
            } else if (existing != null && oConvertUtils.isNotEmpty(existing.getTencentSecretKeyCipher())) {
                entity.setTencentSecretKeyCipher(existing.getTencentSecretKeyCipher());
            } else {
                return Result.error("请填写腾讯云 SecretKey");
            }
        }

        LoginUser u = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String userName = u != null ? u.getUsername() : "system";
        java.util.Date now = new java.util.Date();
        if (existing == null) {
            entity.setCreateBy(userName);
            entity.setCreateTime(now);
        }
        entity.setUpdateBy(userName);
        entity.setUpdateTime(now);

        if (existing == null) {
            storageConfigMapper.insert(entity);
        } else {
            storageConfigMapper.updateById(entity);
        }
        storageConfigCacheInvalidator.invalidate();
        return Result.OK("保存成功");
    }

    private static String trimOrNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
