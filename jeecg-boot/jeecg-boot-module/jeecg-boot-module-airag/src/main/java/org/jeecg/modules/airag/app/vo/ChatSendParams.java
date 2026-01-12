package org.jeecg.modules.airag.app.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @Description: 发送消息的入参
 * @Author: chenrui
 * @Date: 2025/2/25 11:47
 */
@NoArgsConstructor
@Data
public class ChatSendParams {

    public ChatSendParams(String content, String conversationId, String topicId, String appId) {
        this.content = content;
        this.conversationId = conversationId;
        this.topicId = topicId;
        this.appId = appId;
    }

    /**
     * 用户输入的聊天内容
     */
    private String content;

    /**
     * 对话会话ID
     */
    private String conversationId;

    /**
     * 对话主题ID（用于关联历史记录）
     */
    private String topicId;

    /**
     * 应用id
     */
    private String appId;

    /**
     * 图片列表
     */
    private List<String> images;

    /**
     * 工作流额外入参配置
     * key: 参数field, value: 参数值
     * for [issues/8545]新建AI应用的时候只能选择没有自定义参数的AI流程
     */
    private Map<String, Object> flowInputs;

    /**
     * 是否开启网络搜索（仅千问模型支持）
     */
    private Boolean enableSearch;

    // ==================== 第三方接入参数 ====================

    /**
     * 第三方用户ID（用于绑定外部系统用户）
     */
    private String externalUserId;

    /**
     * 第三方用户名称
     */
    private String externalUserName;

    /**
     * 会话模式: temp=临时会话(3小时过期), persist=持久会话(绑定用户)
     */
    private String sessionMode;

    /**
     * 接入签名 token = MD5(appId + secretKey + timestamp)
     */
    private String token;

    /**
     * 时间戳（用于签名验证）
     */
    private Long timestamp;

}
