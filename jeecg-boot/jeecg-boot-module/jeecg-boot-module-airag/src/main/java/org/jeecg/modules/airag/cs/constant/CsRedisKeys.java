package org.jeecg.modules.airag.cs.constant;

/**
 * CS 模块全局配置 Key 常量。
 *
 * 命名约定：
 * <ul>
 *   <li>{@code CONFIG_*}：{@code cs_global_config} 表 {@code id} 字段（DB 主键）</li>
 *   <li>{@code REDIS_*}：Redis 缓存 key，统一前缀 {@code cs:global:}</li>
 * </ul>
 * 两者一般配套使用：先查 Redis，未命中再查 DB 后回填 Redis。
 *
 * @author jeecg
 */
public final class CsRedisKeys {

    private CsRedisKeys() {
    }

    /** Redis Key 统一前缀 */
    public static final String REDIS_PREFIX = "cs:global:";

    // ==================== 访客接入 ====================

    /** 访客应用 ID */
    public static final String CONFIG_VISITOR_APP = "visitor_app_id";
    public static final String REDIS_VISITOR_APP = REDIS_PREFIX + CONFIG_VISITOR_APP;

    /** 访客接入配置（含密钥/Token 模式开关） */
    public static final String CONFIG_VISITOR_ACCESS = "visitor_access";
    public static final String REDIS_VISITOR_ACCESS = REDIS_PREFIX + CONFIG_VISITOR_ACCESS;

    // ==================== AI 开关 ====================

    /** AI 总开关 */
    public static final String CONFIG_AI_ENABLED = "ai_enabled";
    public static final String REDIS_AI_ENABLED = REDIS_PREFIX + CONFIG_AI_ENABLED;

    /** AI 开场白开关 */
    public static final String CONFIG_AI_PROLOGUE_ENABLED = "ai_prologue_enabled";
    public static final String REDIS_AI_PROLOGUE_ENABLED = REDIS_PREFIX + CONFIG_AI_PROLOGUE_ENABLED;

    // ==================== 会话与分配 ====================

    /** 会话分配策略 */
    public static final String CONFIG_CONVERSATION_ASSIGN = "conversation_assign";
    public static final String REDIS_CONVERSATION_ASSIGN = REDIS_PREFIX + CONFIG_CONVERSATION_ASSIGN;

    // ==================== 客服安全 ====================

    /** 客服 IP 白名单开关 */
    public static final String CONFIG_WHITELIST_ENABLED = "agent_ip_whitelist_enabled";
    public static final String REDIS_WHITELIST_ENABLED = REDIS_PREFIX + CONFIG_WHITELIST_ENABLED;

    // ==================== 消息与窗口 ====================

    /** 自动消息（欢迎语/超时提醒等） */
    public static final String CONFIG_AUTO_MESSAGES = "auto_messages";
    public static final String REDIS_AUTO_MESSAGES = REDIS_PREFIX + CONFIG_AUTO_MESSAGES;

    /** 聊天窗口样式（Iframe / 弹窗等） */
    public static final String CONFIG_CHAT_WINDOW = "chat_window_settings";
    public static final String REDIS_CHAT_WINDOW = REDIS_PREFIX + CONFIG_CHAT_WINDOW;

    /** 敏感词配置 */
    public static final String CONFIG_SENSITIVE_WORDS = "sensitive_words";
    public static final String REDIS_SENSITIVE_WORDS = REDIS_PREFIX + CONFIG_SENSITIVE_WORDS;

    /** 留言板配置 */
    public static final String CONFIG_MESSAGE_BOARD = "message_board";
    public static final String REDIS_MESSAGE_BOARD = REDIS_PREFIX + CONFIG_MESSAGE_BOARD;

    /** 数据清理配置 */
    public static final String CONFIG_DATA_CLEANUP = "data_cleanup";
    public static final String REDIS_DATA_CLEANUP = REDIS_PREFIX + CONFIG_DATA_CLEANUP;

    // ==================== 运行期状态 Key（仅 Redis，不入库） ====================

    /** 客服轮询分配游标 */
    public static final String REDIS_ROUND_ROBIN_INDEX = REDIS_PREFIX + "round_robin_index";

    /**
     * 客服"最近一次登录"标记 key 前缀（完整 key 形如 cs:agent:recent_login:{agentId}）。
     * 用于在框架 SSO 挤下线场景中，让 WebSocket handleAgentDisconnect
     * 跳过宽限期 goOffline，避免新登录刚 goOnline 又被旧 ws 关闭误置为 OFFLINE。
     * 一次性消费：被命中后立即 del，30s TTL 兜底。
     */
    public static final String REDIS_AGENT_RECENT_LOGIN_PREFIX = "cs:agent:recent_login:";
    public static final int RECENT_LOGIN_TTL_SECONDS = 30;
}
