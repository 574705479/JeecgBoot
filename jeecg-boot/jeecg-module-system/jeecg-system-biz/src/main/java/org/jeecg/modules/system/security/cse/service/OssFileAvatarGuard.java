package org.jeecg.modules.system.security.cse.service;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.oss.entity.OssFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 头像入库前置 Guard
 *
 * 安全设计要点：
 * - 二期 B2 风险根因：旧 healer 模式自动把 oss_file.biz_path 升格为 avatar/...
 *   导致攻击者可以把别人上传的私密 fid（聊天附件等）写进自己的 avatar，让消息附件全员可见。
 * - 修复策略：放弃 healer，改为前置校验 —— 写库前若 cse:// 后的 fid 对应记录的 biz_path
 *   不以 "avatar/" 开头则直接拒绝，不做任何写操作（零副作用）。
 * - 用户若想沿用历史非 avatar 的 fid，必须通过独立 /sys/user/migrateAvatar 接口
 *   走 createBy + 业务表反查的强校验流程。
 *
 * 入参可能的形态：
 * - null / 空字符串：直接放行（清空头像）
 * - HTTP(S) URL：第三方头像，不走 CSE，直接放行
 * - cse://{fid} 或包含 fid 的字符串：解析 fid 后查 oss_file 校验 biz_path 前缀
 */
@Slf4j
@Component
public class OssFileAvatarGuard {

    /** 合法头像 biz_path 前缀（与一期 SQL / SecureFileController 兼容） */
    private static final String AVATAR_PREFIX = "avatar/";

    @Autowired
    private OssFileMetaService ossFileMetaService;

    /**
     * 校验头像字段值是否合法。校验失败抛 {@link JeecgBootException}，业务层 ResponseAdvice 会转 500。
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
        // 非 cse:// 协议（http/https/相对路径）→ 放行（OAuth、第三方头像或历史明文 URL）
        if (!containsCseFid(v)) {
            return;
        }
        String fid = extractFid(v);
        if (fid == null || fid.isEmpty()) {
            // 含 cse:// 但解析不出 fid，视为脏数据
            throw new JeecgBootException("头像地址格式非法，请重新选择头像");
        }
        OssFile file = ossFileMetaService.getByFileId(fid);
        if (file == null) {
            log.warn("[AvatarGuard] 拒绝写入：fid={} 对应 oss_file 不存在", fid);
            throw new JeecgBootException("头像文件不存在，请重新上传");
        }
        String bizPath = file.getBizPath();
        if (bizPath == null || !bizPath.startsWith(AVATAR_PREFIX)) {
            log.warn("[AvatarGuard] 拒绝写入：fid={} biz_path={} 非 avatar 业务，create_by={}",
                    fid, bizPath, file.getCreateBy());
            throw new JeecgBootException("非法头像 fid，请使用头像上传通道重新上传或提交迁移申请");
        }
        // 通过：不做任何写操作（零副作用，杜绝 healer 类越权升权）
    }

    /** 字符串包含 cse:// 协议或裸 fid 模式 */
    private boolean containsCseFid(String v) {
        return v.startsWith("cse://") || v.contains("cse://");
    }

    /**
     * 从 cse://{fid} 字符串中提取 fid。
     * 兼容形态：
     *   cse://abcdef
     *   /sys/secure/file/cse://abcdef/...
     *   带后缀: cse://abcdef.png
     */
    private String extractFid(String v) {
        int idx = v.indexOf("cse://");
        if (idx < 0) {
            return null;
        }
        String tail = v.substring(idx + "cse://".length());
        // 截到第一个非 fid 字符（保留 hex 与 dash）
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
