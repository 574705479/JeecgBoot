package org.jeecg.modules.system.security.cse.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * CSE 端到端文件加密配置（jeecg.cse.*）
 */
@Data
@Component
@ConfigurationProperties(prefix = "jeecg.cse")
public class CseProperties {

    /** 总开关：false 时所有上传走老明文链路，下行接口仍可服务历史 cse 文件 */
    private boolean enabled = true;

    /** 白名单：bizPath 命中其中任意前缀则不加密（公开资源） */
    private List<String> publicPaths = new ArrayList<>();

    /** 白名单：bizPath 命中其中任意前缀才加密。空则按 enabled 全量加密 */
    private List<String> encryptedPaths = new ArrayList<>();

    /** 缩略图最大边像素 */
    private int thumbWidth = 256;

    /** DEK 内存缓存秒数（高并发优化，0 表示不缓存） */
    private int dekCacheSeconds = 10;

    /** 用户自助迁移老 fid 到 avatar 业务的接口（POST /sys/user/migrateAvatar）配置 */
    private MigrateAvatar migrateAvatar = new MigrateAvatar();

    @Data
    public static class MigrateAvatar {
        /**
         * 业务表反查白名单：{table}.{column}，命中即拒绝迁移（防 B2 把消息附件/聊天图当头像）
         * 例：cs_message.message_content、cs_message_attachment.attachment_url
         */
        private List<String> scanTables = new ArrayList<>();
        /** 阈值天数：超过该天数仅做 WARN 审计日志，不阻塞迁移 */
        private int warnAfterDays = 30;
    }
}
