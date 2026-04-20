package org.jeecg.modules.airag.cs.security;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * airag 子模块独立的头像入库前置 Guard
 *
 * 与 jeecg-system-biz 的 OssFileAvatarGuard 语义一致；因为 airag 模块不直接依赖
 * system-biz，无法引用其 OssFileMapper / 实体类，这里独立用 JdbcTemplate 读 oss_file。
 *
 * 安全设计要点详见 docs/cse-runbook.md "Phase 2 入库 Guard" 章节。
 */
@Slf4j
@Component
public class AvatarGuard {

    private static final String AVATAR_PREFIX = "avatar/";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 校验头像字段值是否合法。失败抛 {@link JeecgBootException}。
     *
     * @param avatarValue 用户即将写入的 avatar 字段值
     */
    public void validateAvatar(String avatarValue) {
        if (avatarValue == null) {
            return;
        }
        String v = avatarValue.trim();
        if (v.isEmpty()) {
            return;
        }
        if (!v.startsWith("cse://") && !v.contains("cse://")) {
            return;
        }
        String fid = extractFid(v);
        if (fid == null || fid.isEmpty()) {
            throw new JeecgBootException("头像地址格式非法，请重新选择头像");
        }
        String bizPath;
        try {
            bizPath = jdbcTemplate.queryForObject(
                    "SELECT biz_path FROM oss_file WHERE file_id = ? LIMIT 1",
                    String.class, fid);
        } catch (EmptyResultDataAccessException e) {
            log.warn("[AvatarGuard] 拒绝写入：fid={} 对应 oss_file 不存在", fid);
            throw new JeecgBootException("头像文件不存在，请重新上传");
        } catch (Exception e) {
            log.error("[AvatarGuard] 查询 oss_file 异常 fid=" + fid, e);
            throw new JeecgBootException("头像校验失败，请稍后再试");
        }
        if (bizPath == null || !bizPath.startsWith(AVATAR_PREFIX)) {
            log.warn("[AvatarGuard] 拒绝写入：fid={} biz_path={} 非 avatar 业务", fid, bizPath);
            throw new JeecgBootException("非法头像 fid，请使用头像上传通道重新上传或提交迁移申请");
        }
    }

    private String extractFid(String v) {
        int idx = v.indexOf("cse://");
        if (idx < 0) {
            return null;
        }
        String tail = v.substring(idx + "cse://".length());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tail.length(); i++) {
            char c = tail.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
                sb.append(c);
            } else {
                break;
            }
        }
        return sb.toString();
    }
}
