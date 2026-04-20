package org.jeecg.modules.system.security.cse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.oss.entity.OssFile;
import org.jeecg.modules.oss.mapper.OssFileMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * oss_file 加密元数据服务
 */
@Slf4j
@Service
public class OssFileMetaService {

    @Autowired
    private OssFileMapper ossFileMapper;

    public OssFile getByFileId(String fileId) {
        if (fileId == null || fileId.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<OssFile> qw = new LambdaQueryWrapper<>();
        qw.eq(OssFile::getFileId, fileId).last("LIMIT 1");
        return ossFileMapper.selectOne(qw);
    }

    public void save(OssFile file) {
        ossFileMapper.insert(file);
    }

    public static String genFileId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    /**
     * 权限判断（第一道闸 + 业务前缀映射）
     * 简化版：
     * - public_flag=1 → 放行
     * - 跨租户 → 拒绝
     * - 上传者本人 → 放行
     * - 默认放行（业务侧可在网关层增加细粒度规则）
     */
    public boolean canRead(OssFile file, LoginUser user, String visitorToken) {
        if (file == null) {
            return false;
        }
        Integer pf = file.getPublicFlag();
        if (pf != null && pf == 1) {
            return true;
        }
        // 品牌资源公开放行：cs-brand/* 已通过 /cs/brand/file/{fid} 匿名代理对外开放，
        // 私有解密通道（已登录用户）也应同步放行，避免品牌配置面板等场景因租户不匹配而空白
        if (file.getBizPath() != null && file.getBizPath().startsWith("cs-brand")) {
            return true;
        }
        // 头像类资源（avatar/）：客服 / 用户头像在客服列表、消息气泡、对话头里被同事和访客都需要展示，
        // 不应受租户隔离，但要求至少持有可信通道（已登录或持访客 token），不允许匿名读取
        if (file.getBizPath() != null && file.getBizPath().startsWith("avatar/")) {
            if (user != null) {
                return true;
            }
            if (visitorToken != null && !visitorToken.isEmpty()) {
                return true;
            }
        }
        if (user == null && (visitorToken == null || visitorToken.isEmpty())) {
            return false;
        }
        if (user != null) {
            // 跨租户隔离：tenantId="0" 视为「无租户/默认租户」，对所有登录用户放行
            // 仅当 fileTenant 为非默认租户 (>0) 且与用户绑定的租户列表不交集时拒绝
            String fileTenant = file.getTenantId();
            if (fileTenant != null && !fileTenant.isEmpty() && !"0".equals(fileTenant)) {
                String userTenant = String.valueOf(user.getRelTenantIds() == null ? "" : user.getRelTenantIds());
                // 用 "," 切分严格匹配，避免 "10".contains("1") 误判
                boolean hit = false;
                if (!userTenant.isEmpty()) {
                    for (String t : userTenant.split(",")) {
                        if (fileTenant.equals(t.trim())) { hit = true; break; }
                    }
                }
                if (!hit) {
                    log.warn("[CSE] denied fid={} user={} reason=tenant-mismatch fileTenant={} userTenant={}",
                            file.getFileId(), user.getUsername(), fileTenant, userTenant);
                    return false;
                }
            }
            // 上传者本人放行：createBy 字段写入的是 username（参见 CseUploader#setCreateBy），
            // 旧代码用 user.getId() 比较，永远不匹配；此处更正为 username 比对
            if (user.getUsername() != null && user.getUsername().equals(file.getCreateBy())) {
                return true;
            }
            // 业务前缀映射（默认放行 - 后续可基于 bizPath 校验更细权限码）
            // TODO[F-1] 跟进项：盘点 bizPath 白名单 + 加配置项 cse.acl.default-deny（默认 false）灰度切默认拒绝
            return true;
        }
        // 访客 token：仅限客服访客上传的文件且 bizPath 命中 cs-visitor
        if (file.getBizPath() != null && file.getBizPath().startsWith("cs-visitor")) {
            return true;
        }
        return false;
    }

    public LoginUser getCurrentUser() {
        try {
            Object p = SecurityUtils.getSubject().getPrincipal();
            if (p instanceof LoginUser) {
                return (LoginUser) p;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
