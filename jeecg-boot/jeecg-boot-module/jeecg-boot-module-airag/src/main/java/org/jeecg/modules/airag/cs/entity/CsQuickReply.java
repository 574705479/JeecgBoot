package org.jeecg.modules.airag.cs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 快捷回复表
 * 
 * @author jeecg
 * @date 2026-01-07
 */
@Data
@TableName("cs_quick_reply")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "快捷回复表")
public class CsQuickReply implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Excel(name = "分类ID", width = 15)
    @Schema(description = "分类ID")
    private String categoryId;

    @Excel(name = "标题/关键词", width = 20)
    @Schema(description = "标题/关键词")
    private String title;

    @Excel(name = "回复内容", width = 40)
    @Schema(description = "回复内容")
    private String content;

    @Excel(name = "消息类型", width = 10)
    @Schema(description = "消息类型: 0-文本 1-图片 2-文件 5-富文本")
    private Integer msgType;

    @Schema(description = "扩展信息")
    private String extra;

    @Excel(name = "使用次数", width = 10)
    @Schema(description = "使用次数")
    private Integer useCount;

    @Schema(description = "所属客服(NULL表示公共)")
    private String agentId;

    @Excel(name = "状态", width = 10)
    @Schema(description = "状态: 0-禁用 1-启用")
    private Integer status;

    @Excel(name = "排序", width = 10)
    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "创建人")
    private String createBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新人")
    private String updateBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private Date updateTime;
}
