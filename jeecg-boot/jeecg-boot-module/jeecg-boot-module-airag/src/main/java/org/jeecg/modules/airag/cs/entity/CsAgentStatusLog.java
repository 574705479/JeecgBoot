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
 * 客服状态变更日志
 *
 * @author jeecg
 */
@Data
@TableName("cs_agent_status_log")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "客服状态变更日志")
public class CsAgentStatusLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "客服ID")
    private String agentId;

    @Schema(description = "状态: 0-离线 1-在线 2-忙碌 3-隐身")
    private Integer status;

    @Schema(description = "触发来源: manual-手动, websocket_disconnect-断连, system-系统自动")
    private String triggerSource;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "状态开始时间")
    private Date startTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "状态结束时间(NULL表示当前状态)")
    private Date endTime;

    @Schema(description = "持续时长(秒)")
    private Integer durationSeconds;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    public static final int STATUS_OFFLINE = 0;
    public static final int STATUS_ONLINE = 1;
    public static final int STATUS_BUSY = 2;
    public static final int STATUS_INVISIBLE = 3;

    public static final String TRIGGER_MANUAL = "manual";
    public static final String TRIGGER_WEBSOCKET_DISCONNECT = "websocket_disconnect";
    public static final String TRIGGER_SYSTEM = "system";
}
