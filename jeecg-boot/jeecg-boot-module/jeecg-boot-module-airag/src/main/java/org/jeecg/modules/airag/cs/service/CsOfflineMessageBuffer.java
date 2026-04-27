package org.jeecg.modules.airag.cs.service;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.cs.constant.CsRedisKeys;
import org.jeecg.modules.airag.cs.websocket.CsWebSocketMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基于 Redis Stream 的离线消息缓冲。
 *
 * <p>使用场景：某次 WebSocket 推送因目标离线而丢失时，调用方把 payload enqueue 进来；
 * 目标重新上线（WS 握手）时，调用 drain 把积压消息按 FIFO 顺序串行补推。</p>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>每个 conversationId / agentId 对应一个独立 Stream key，互不干扰</li>
 *   <li>XADD 用 Lua 脚本带 {@code MAXLEN ~ N} 做近似裁剪 + {@code EXPIRE} 24h 兜底，防止极端情况无限堆积</li>
 *   <li>只持久化消息型 payload（见 {@link #PERSISTENT_TYPES}），控制帧/心跳/typing 不入队</li>
 *   <li>所有 Redis 操作都包了 try-catch，确保离线缓冲失败不会影响主消息路径</li>
 * </ul>
 *
 * @author jeecg
 * @date 2026-04-24
 */
@Slf4j
@Component
public class CsOfflineMessageBuffer {

    /** 仅对这些"真正的业务消息"进行离线缓冲，控制帧/心跳/输入中等不持久化 */
    private static final Set<String> PERSISTENT_TYPES = Set.of(
            CsWebSocketMessage.TYPE_MESSAGE,
            CsWebSocketMessage.TYPE_SYSTEM,
            CsWebSocketMessage.TYPE_AI_STREAM_COMPLETE,
            CsWebSocketMessage.TYPE_DELIVERY_FAILED,
            CsWebSocketMessage.TYPE_MESSAGE_RECALL
    );

    /**
     * XADD + EXPIRE 原子脚本。
     * <pre>
     * KEYS[1]  Stream key
     * ARGV[1]  MAXLEN 近似条数上限（如 500）
     * ARGV[2]  payload JSON
     * ARGV[3]  TTL 秒
     * </pre>
     */
    private static final DefaultRedisScript<Long> XADD_SCRIPT;

    static {
        XADD_SCRIPT = new DefaultRedisScript<>();
        XADD_SCRIPT.setScriptText(
                "redis.call('XADD', KEYS[1], 'MAXLEN', '~', ARGV[1], '*', 'p', ARGV[2])\n" +
                "redis.call('EXPIRE', KEYS[1], ARGV[3])\n" +
                "return 1"
        );
        XADD_SCRIPT.setResultType(Long.class);
    }

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // ==================== enqueue ====================

    /**
     * 向访客会话离线 Stream 写入一条消息（目标 userId 不在线时调用）。
     *
     * @param conversationId 会话 ID
     * @param payload        已构造好的 WS 消息实体（一次加密/序列化好）
     */
    public void enqueueForUser(String conversationId, CsWebSocketMessage payload) {
        if (oConvertUtils.isEmpty(conversationId) || payload == null) {
            return;
        }
        if (!isPersistent(payload)) {
            return;
        }
        enqueue(CsRedisKeys.OFFLINE_USER_STREAM_PREFIX + conversationId, payload, "user:" + conversationId);
    }

    /**
     * 向客服离线 Stream 写入一条消息（目标 agentId 不在线时调用）。
     */
    public void enqueueForAgent(String agentId, CsWebSocketMessage payload) {
        if (oConvertUtils.isEmpty(agentId) || payload == null) {
            return;
        }
        if (!isPersistent(payload)) {
            return;
        }
        enqueue(CsRedisKeys.OFFLINE_AGENT_STREAM_PREFIX + agentId, payload, "agent:" + agentId);
    }

    private void enqueue(String key, CsWebSocketMessage payload, String scope) {
        try {
            String json = JSON.toJSONString(payload);
            stringRedisTemplate.execute(
                    XADD_SCRIPT,
                    Collections.singletonList(key),
                    String.valueOf(CsRedisKeys.OFFLINE_MESSAGE_MAX_LEN),
                    json,
                    String.valueOf(CsRedisKeys.OFFLINE_MESSAGE_TTL_SECONDS)
            );
            if (log.isDebugEnabled()) {
                log.debug("[CS-Offline] enqueue {} type={}, bytes={}", scope, payload.getType(), json.length());
            }
        } catch (Exception e) {
            // 离线缓冲是"最佳努力"，失败不应影响主消息路径
            log.warn("[CS-Offline] enqueue 失败: scope={}, type={}, err={}",
                    scope, payload.getType(), e.getMessage());
        }
    }

    // ==================== drain ====================

    /**
     * 读出并清空访客会话的离线消息（重连时调用）。
     *
     * @return FIFO 顺序的消息列表，从未入队时返回空列表
     */
    public List<CsWebSocketMessage> drainForUser(String conversationId) {
        if (oConvertUtils.isEmpty(conversationId)) {
            return Collections.emptyList();
        }
        return drain(CsRedisKeys.OFFLINE_USER_STREAM_PREFIX + conversationId, "user:" + conversationId);
    }

    /**
     * 读出并清空某个客服的离线消息（客服重连时调用）。
     */
    public List<CsWebSocketMessage> drainForAgent(String agentId) {
        if (oConvertUtils.isEmpty(agentId)) {
            return Collections.emptyList();
        }
        return drain(CsRedisKeys.OFFLINE_AGENT_STREAM_PREFIX + agentId, "agent:" + agentId);
    }

    private List<CsWebSocketMessage> drain(String key, String scope) {
        try {
            List<MapRecord<String, Object, Object>> records =
                    stringRedisTemplate.opsForStream().range(key, Range.unbounded());
            if (records == null || records.isEmpty()) {
                return Collections.emptyList();
            }
            List<CsWebSocketMessage> result = new ArrayList<>(records.size());
            for (MapRecord<String, Object, Object> record : records) {
                Map<Object, Object> value = record.getValue();
                if (value == null) {
                    continue;
                }
                Object json = value.get("p");
                if (json == null) {
                    continue;
                }
                try {
                    CsWebSocketMessage msg = JSON.parseObject(String.valueOf(json), CsWebSocketMessage.class);
                    if (msg != null) {
                        result.add(msg);
                    }
                } catch (Exception parseEx) {
                    log.warn("[CS-Offline] drain parse 失败: scope={}, err={}", scope, parseEx.getMessage());
                }
            }
            // 消费后立即删除 Stream，避免重复下推
            stringRedisTemplate.delete(key);
            if (!result.isEmpty()) {
                log.info("[CS-Offline] drain {} 条离线消息 scope={}", result.size(), scope);
            }
            return result;
        } catch (Exception e) {
            log.warn("[CS-Offline] drain 失败: scope={}, err={}", scope, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== 辅助 ====================

    private boolean isPersistent(CsWebSocketMessage payload) {
        String type = payload.getType();
        if (oConvertUtils.isEmpty(type)) {
            return false;
        }
        return PERSISTENT_TYPES.contains(type);
    }
}
