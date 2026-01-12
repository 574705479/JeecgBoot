package org.jeecg.modules.airag.app.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.jeecg.modules.airag.app.entity.AiragApp;
import org.jeecg.modules.airag.common.vo.MessageHistory;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @Description: 聊天会话
 * 
 * 【新架构说明】
 * - messages字段不再序列化到Redis，消息统一存储在MongoDB
 * - Redis只存储会话元信息（id, title, createTime等）
 * - 获取消息请使用IChatMessageService
 * 
 * @Author: chenrui
 * @Date: 2025/2/25 14:56
 */
@Data
public class ChatConversation {

    /**
     * 会话id
     */
    private String id;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 消息记录
     * 
     * 【已废弃】不再存储到Redis，仅作为临时内存使用
     * 消息统一存储在MongoDB的chat_messages集合中
     */
    @JsonIgnore
    private transient List<MessageHistory> messages;

    /**
     * app
     */
    private AiragApp app;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 流程入参配置（工作流的额外参数设置）
     * key: 参数field, value: 参数值
     * for [issues/8545]新建AI应用的时候只能选择没有自定义参数的AI流程
     */
    private Map<String, Object> flowInputs;

    // ==================== 用户信息 ====================
    
    /**
     * 内部用户ID（登录用户）
     */
    private String userId;

    /**
     * 第三方用户ID（外部接入用户）
     */
    private String externalUserId;

    /**
     * 第三方用户名称
     */
    private String externalUserName;

    /**
     * 会话模式: temp=临时会话, persist=持久会话
     */
    private String sessionMode;

    /**
     * 应用ID（用于缓存key构建）
     */
    private String appId;

    /**
     * Redis缓存key（用于SSE回调线程中保存会话）
     */
    private String cacheKey;
}
