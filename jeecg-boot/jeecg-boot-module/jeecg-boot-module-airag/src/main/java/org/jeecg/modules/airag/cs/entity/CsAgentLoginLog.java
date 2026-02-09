package org.jeecg.modules.airag.cs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 客服登录日志
 */
@Data
@TableName("cs_agent_login_log")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "客服登录日志")
public class CsAgentLoginLog implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String EVENT_LOGIN_SUCCESS = "登录成功";
    public static final String EVENT_LOGIN_FAILED = "登录失败";
    public static final String EVENT_IP_BLOCKED = "IP拦截";
    public static final String EVENT_LOGOUT = "退出";

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private String id;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "日期")
    private Date loginDate;

    @Schema(description = "账号")
    private String username;

    @Schema(description = "事件")
    private String event;

    @Schema(description = "IP地址")
    private String ip;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;
}
