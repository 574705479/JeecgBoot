package org.jeecg.modules.airag.cs.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-session 异步 WebSocket 发送器。
 *
 * <p>核心目的：避免 HTTP 请求线程被 {@code synchronized(session)} + 底层
 * {@link WebSocketSession#sendMessage} 的 I/O 阻塞。特别是多访客扇入到同一客服的场景，
 * 原先每条消息都要在业务主路径里串行等 WS 帧写完才能返回 HTTP 响应。</p>
 *
 * <h3>保证</h3>
 * <ul>
 *   <li>同一 session 严格 FIFO：队列 + 单 drainer，保持消息顺序</li>
 *   <li>同一 session 任一时刻最多 1 个 drainer 在跑：通过 {@link AtomicBoolean} CAS 保证</li>
 *   <li>生产者（HTTP 线程）入队后立即返回：只做 offer + 可能 submit 一次 drain 任务</li>
 *   <li>过载保护：队列超过 capacity 则丢弃并告警，避免慢消费者拖垮内存</li>
 * </ul>
 *
 * <h3>drain 线程复用</h3>
 * <p>drain 任务跑在共享的 {@code wsExecutor} 线程池里。单个 session 的 drainer 退出时
 * 会先 {@code drainScheduled.set(false)} 再重新检查队列，若有残留则再次 CAS 抢 drain 权，
 * 消除"set false 和生产者 CAS 之间的遗漏窗口"。</p>
 */
@Slf4j
public final class CsSessionSender {

    /** 默认每 session 积压上限，超出将丢弃最早未消费的消息并告警。 */
    public static final int DEFAULT_CAPACITY = 1024;

    private final WebSocketSession session;
    private final Executor drainExecutor;
    private final int capacity;

    private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger pending = new AtomicInteger();
    private final AtomicBoolean drainScheduled = new AtomicBoolean(false);

    public CsSessionSender(WebSocketSession session, Executor drainExecutor) {
        this(session, drainExecutor, DEFAULT_CAPACITY);
    }

    public CsSessionSender(WebSocketSession session, Executor drainExecutor, int capacity) {
        this.session = session;
        this.drainExecutor = drainExecutor;
        this.capacity = Math.max(64, capacity);
    }

    /**
     * 入队一条已序列化好的 JSON 文本消息。
     *
     * <p>带 fast path：当前既无 drainer 在跑、队列又为空时，CAS 抢 drain 权后
     * 直接在调用者线程同步发送一条，避免线程切换开销（典型单发场景 V2A 因此不回退）。</p>
     *
     * @return true 表示已送达或已入队；false 表示 session 不可用或队列溢出丢弃
     */
    public boolean enqueue(String json) {
        if (json == null) {
            return false;
        }
        if (!session.isOpen()) {
            return false;
        }
        // Fast path：队列空 + 无 drainer，直接同步发送省一次线程切换
        if (pending.get() == 0 && drainScheduled.compareAndSet(false, true)) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage(json));
                        }
                    }
                }
            } catch (Throwable e) {
                log.error("[CS-WebSocket] fast send failed: sessionId={}, error={}",
                        session.getId(), e.getMessage(), e);
            } finally {
                drainScheduled.set(false);
            }
            // 这段窗口（fast path 持锁期间）可能有其他生产者入过队，需要二次检查并重启 drainer
            if (!queue.isEmpty() && drainScheduled.compareAndSet(false, true)) {
                try {
                    drainExecutor.execute(this::drain);
                } catch (RejectedExecutionException e) {
                    drainScheduled.set(false);
                    log.warn("[CS-WebSocket] drain reschedule rejected: sessionId={}, error={}",
                            session.getId(), e.getMessage());
                }
            }
            return true;
        }

        // Slow path：有并发，走异步队列
        if (pending.get() >= capacity) {
            log.warn("[CS-WebSocket] session buffer overflow, drop message: sessionId={}, pending={}, cap={}",
                    session.getId(), pending.get(), capacity);
            return false;
        }
        pending.incrementAndGet();
        queue.offer(json);
        scheduleDrain();
        return true;
    }

    /** 当前待发送条数（近似值，用于监控）。 */
    public int pendingSize() {
        return pending.get();
    }

    /** 会话关闭时调用，清空残留消息。 */
    public void dispose() {
        queue.clear();
        pending.set(0);
    }

    private void scheduleDrain() {
        if (drainScheduled.compareAndSet(false, true)) {
            try {
                drainExecutor.execute(this::drain);
            } catch (RejectedExecutionException e) {
                // 线程池已关闭；回退 drainScheduled 以便后续 producer 重试
                drainScheduled.set(false);
                log.warn("[CS-WebSocket] drain rejected: sessionId={}, error={}",
                        session.getId(), e.getMessage());
            } catch (Throwable t) {
                drainScheduled.set(false);
                log.error("[CS-WebSocket] drain schedule failed: sessionId={}",
                        session.getId(), t);
            }
        }
    }

    /**
     * 在 wsExecutor 线程上串行消费队列中的消息。
     *
     * <p>循环结束后的"set false + 再次检查"模式用于关闭与生产者的 race 窗口：
     * 生产者 offer 完可能恰巧 CAS 失败（drainScheduled 此时还为 true），
     * 此时需要退出的 drainer 再次看一眼队列并在非空时重新抢 drain 权。</p>
     */
    private void drain() {
        while (true) {
            String msg;
            while ((msg = queue.poll()) != null) {
                pending.decrementAndGet();
                if (!session.isOpen()) {
                    continue;
                }
                try {
                    // sendMessage 线程不安全，需串行；同 session 在此只有一个 drainer，
                    // 加 synchronized 仅作为兜底（防止极端情况下有旧代码路径仍同步发送）。
                    synchronized (session) {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage(msg));
                        }
                    }
                } catch (Throwable e) {
                    log.error("[CS-WebSocket] async send failed: sessionId={}, error={}",
                            session.getId(), e.getMessage(), e);
                }
            }
            drainScheduled.set(false);
            if (queue.isEmpty()) {
                return;
            }
            if (!drainScheduled.compareAndSet(false, true)) {
                return;
            }
        }
    }
}
