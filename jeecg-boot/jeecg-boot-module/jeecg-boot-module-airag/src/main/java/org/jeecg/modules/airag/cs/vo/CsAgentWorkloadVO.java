package org.jeecg.modules.airag.cs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 客服工作量统计
 *
 * @author jeecg
 * @date 2026-01-29
 */
@Data
@Schema(description = "客服工作量统计")
public class CsAgentWorkloadVO {

    @Schema(description = "客服ID")
    private String agentId;

    @Schema(description = "客服名称")
    private String agentName;

    @Schema(description = "会话数")
    private Long conversationCount;

    @Schema(description = "消息数")
    private Long messageCount;

    @Schema(description = "平均满意度")
    private Double avgSatisfaction;

    @Schema(description = "平均首次响应耗时(秒)")
    private Long avgFirstResponseSeconds;
}
