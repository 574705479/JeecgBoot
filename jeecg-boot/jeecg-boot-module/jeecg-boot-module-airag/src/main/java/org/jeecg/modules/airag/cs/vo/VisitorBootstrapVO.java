package org.jeecg.modules.airag.cs.vo;

import lombok.Data;
import org.jeecg.modules.airag.cs.entity.CsConversation;
import org.jeecg.modules.airag.cs.entity.CsLeaveMessage;
import org.jeecg.modules.airag.cs.entity.CsMessage;

import java.util.List;

/**
 * 访客端首屏 bootstrap 合包返回 VO。
 *
 * <p>用于一次性返回原本需要 9 个 HTTP 串行请求才能拿到的全部首屏所需数据。
 * 整体 JSON 由 {@code CsVisitorBootstrapController#bootstrap} 外壳 transport 加密后返回。</p>
 *
 * <p>注意：嵌套对象内部已经做了"按字段 transport 加密"的字段（例如
 * {@link CsConversation#getLastMessage()}、{@link CsMessage#getContent()}）保持其原有加密形态，
 * 前端解外壳后按现有逻辑继续处理即可，最大化复用前端解码代码路径。</p>
 *
 * @author jeecg
 */
@Data
public class VisitorBootstrapVO {

    // ==================== 鉴权与黑名单 ====================

    /** 服务端是否要求 token 才能访问（true 表示必须带 token） */
    private Boolean tokenRequired;

    /** 当前 IP 是否在黑名单（true 时其余字段可能为 null） */
    private Boolean ipBlocked;

    /** 当前访客 userId 是否在黑名单 */
    private Boolean userBlocked;

    /** 接入密钥是否无效（仅在免 token 模式下有意义） */
    private Boolean keyInvalid;

    /** 解析到的客户端 IP（仅用于前端展示/调试） */
    private String clientIp;

    /** 鉴权状态简要说明：valid / no_session / no_token / token_expired 等 */
    private String authStatus;

    // ==================== 全局配置 ====================

    /**
     * 聊天窗口配置 JSON 字符串（明文）。
     * 替代 GET /airag/cs/agent/global/chat-window-settings。
     */
    private String chatWindowConfigJson;

    /**
     * 敏感词配置 JSON 字符串（明文）。
     * 替代 GET /airag/cs/agent/global/sensitive-words。
     */
    private String sensitiveWordsJson;

    /**
     * 品牌配置 JSON 字符串（明文，从 cs_brand_config 表序列化）。
     * 替代 GET /cs/brand/get。
     */
    private String brandConfigJson;

    /**
     * 留言板配置 JSON 字符串（明文）。
     * 替代 GET /airag/cs/agent/global/message-board。
     */
    private String messageBoardConfigJson;

    /** 全局 AI 总开关（true 表示打开） */
    private Boolean aiEnabled;

    /** 全局访客 AI 应用 ID（appId） */
    private String visitorAppId;

    // ==================== 客服在线状态 ====================

    /** 是否有任何客服在线（用于决定是否进入留言板模式） */
    private Boolean agentOnline;

    /** 在线客服数量（含忙碌/隐身） */
    private Integer agentOnlineCount;

    // ==================== 会话 + 消息 ====================

    /**
     * 当前活跃会话（已对 lastMessage / satisfactionComment 做 transport 加密，
     * 与 /cs/conversation/get-or-create 返回格式一致）。
     */
    private CsConversation conversation;

    /**
     * 最近 N 条消息（默认 20）。content 字段保留 storage ENC: 状态，
     * 前端走 cseDecrypt 解 storage 层即可。
     */
    private List<CsMessage> recentMessages;

    /** 是否还有更多历史消息（用于前端控制"加载更多"按钮显示） */
    private Boolean hasMoreMessages;

    // ==================== 留言回复 ====================

    /**
     * 未读留言回复（content/reply 已 transport 加密，phone/email/qq/wechat 已脱敏）。
     * 替代 GET /cs/leaveMessage/byUser。
     */
    private List<CsLeaveMessage> unreadReplies;

    // ==================== 元数据 ====================

    /** 服务端时间戳（毫秒），用于前端时钟漂移补偿 */
    private Long serverTime;

    /**
     * 当前快照的 etag（基于关键字段 hash 计算），客户端二次进入时
     * 可在请求体中带上 lastEtag，命中时服务端可短路返回 304。
     * 当前版本未启用，预留给后续优化。
     */
    private String etag;
}
