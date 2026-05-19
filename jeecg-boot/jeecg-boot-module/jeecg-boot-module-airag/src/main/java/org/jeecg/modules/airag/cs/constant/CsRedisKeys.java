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

    /**
     * 品牌配置缓存 key（cs_brand_config 表当前生效的 status=1 条目，整体 JSON 缓存）。
     *
     * <p>Phase 3：访客端首屏 bootstrap 重度依赖品牌配置（logo/标题/主题色），原 controller 每次
     * 全表查 + ORDER BY，QPS 上来后 DB 压力明显。改为先读 Redis、写操作时 del，TTL 兜底防漂移。</p>
     */
    public static final String REDIS_BRAND_CONFIG = REDIS_PREFIX + "brand_config";

    /** 品牌配置 Redis 缓存 TTL（秒）。5 分钟与人工修改的容忍窗口对齐 */
    public static final int BRAND_CONFIG_TTL_SECONDS = 300;

    // ==================== 运行期状态 Key（仅 Redis，不入库） ====================

    /** 客服轮询分配游标 */
    public static final String REDIS_ROUND_ROBIN_INDEX = REDIS_PREFIX + "round_robin_index";

    /**
     * 客服在线状态 ZSET key（score = 心跳/上线时间戳ms，member = agentId）。
     *
     * <p>Phase 3：原 {@code getOnlineAgents()} 走 cs_agent 全表 LIKE/IN 查询，访客端首屏
     * 高频调用会拖慢 DB。引入 Redis ZSET 维护"30s 内有过心跳/上线"的 agentId 集合，
     * {@code ZRANGEBYSCORE > now-30s} 可在 O(logN) 内拿到在线列表。</p>
     *
     * <p>由 goOnline / setBusy / agent 心跳时 ZADD，goOffline 时 ZREM。Redis 不可用时回落 DB 查。</p>
     */
    public static final String REDIS_AGENT_ONLINE_ZSET = "cs:agent:online";

    /** 在线状态 ZSET 心跳过期窗口（毫秒）。客服心跳间隔 ~10s，30s 兜底比心跳更宽松，避免误下线 */
    public static final long AGENT_ONLINE_TTL_MS = 30_000L;

    /**
     * 客服"最近一次登录"标记 key 前缀（完整 key 形如 cs:agent:recent_login:{agentId}）。
     * 用于在框架 SSO 挤下线场景中，让 WebSocket handleAgentDisconnect
     * 跳过宽限期 goOffline，避免新登录刚 goOnline 又被旧 ws 关闭误置为 OFFLINE。
     * 一次性消费：被命中后立即 del，30s TTL 兜底。
     */
    public static final String REDIS_AGENT_RECENT_LOGIN_PREFIX = "cs:agent:recent_login:";
    public static final int RECENT_LOGIN_TTL_SECONDS = 30;

    /**
     * 客服"升级前状态快照"key 前缀（完整 key 形如 cs:agent:preshutdown:{agentId}），
     * value 为 String 形式的 status（"1"/"2"/"3"）。
     *
     * <p>启动 PostConstruct 在 reset 之前写入；ws 重连时一次性消费。30 分钟 TTL 兜底，
     * 避免镜像升级窗口里所有非 OFFLINE 客服被强制重置为 OFFLINE 后无法恢复，
     * 进而导致访客新会话堆积在「未分配」直到客服手动开关一次。</p>
     */
    public static final String REDIS_AGENT_PRESHUTDOWN_PREFIX = "cs:agent:preshutdown:";
    public static final long PRESHUTDOWN_TTL_MINUTES = 30L;

    // ==================== 离线消息缓冲（Redis Stream） ====================

    /**
     * 访客侧离线消息缓冲 Stream key 前缀（完整 key 形如 cs:offline:user:{conversationId}）。
     *
     * <p>当 WebSocket 推送因用户不在线而失败时，把 {@code CsWebSocketMessage} JSON 序列化后 XADD 进该 Stream；
     * 用户重连（WS 握手）时 XRANGE 全量读取并串行下推，然后 DEL 清理，用 Stream 替代 Mongo 查最近消息的热路径。</p>
     *
     * <p>仅保留消息型 payload（TYPE_MESSAGE / TYPE_AI_STREAM_COMPLETE / TYPE_SYSTEM），
     * 类似 typing / pong 之类的控制帧不做持久化。</p>
     */
    public static final String OFFLINE_USER_STREAM_PREFIX = "cs:offline:user:";

    /**
     * 客服侧离线消息缓冲 Stream key 前缀（完整 key 形如 cs:offline:agent:{agentId}）。
     *
     * <p>主要覆盖"客服侧 DELIVERY_FAILED 通知 / 其他客服发的同事会话消息"等定向场景。</p>
     */
    public static final String OFFLINE_AGENT_STREAM_PREFIX = "cs:offline:agent:";

    /** 离线 Stream 单 key 的 TTL（秒）。默认 24h：超过该窗口的消息不再补齐，避免长期积压占内存/磁盘 */
    public static final long OFFLINE_MESSAGE_TTL_SECONDS = 24 * 60 * 60L;

    /**
     * 离线 Stream 单 key 保留的最近条数上限（XADD MAXLEN ~）。
     * 超过该长度 XADD 时自动裁掉最老条目，避免极端用户积压过多消息导致 Redis 内存膨胀。
     */
    public static final long OFFLINE_MESSAGE_MAX_LEN = 500L;
}
