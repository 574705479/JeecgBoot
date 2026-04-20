package org.jeecg.modules.system.security.cse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * CSE 动态配置（单行表）
 *
 * 与 SysStorageConfig 一致：单行配置，主键 id="1"，dev/prod 都用同一行。
 * 历史 yml 配置在表为空时作为兜底回退（CseConfigService 内实现）。
 */
@Data
@TableName("sys_cse_config")
public class SysCseConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 单行主键，与 SysStorageConfig.ID_SINGLETON 一致 */
    public static final String ID_SINGLETON = "1";

    @TableId(type = IdType.INPUT)
    private String id;

    /** 总开关：1=启用 0=关闭 */
    private Integer enabled;

    /** 黑名单 JSON 数组字符串：命中即明文上传 */
    private String publicPaths;

    /** 白名单 JSON 数组字符串：命中才加密 */
    private String encryptedPaths;

    private LocalDateTime updateTime;

    private String updateBy;
}
