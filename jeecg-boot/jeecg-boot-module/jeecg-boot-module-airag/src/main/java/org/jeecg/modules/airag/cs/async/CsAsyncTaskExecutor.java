package org.jeecg.modules.airag.cs.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 客服模块异步任务执行器。
 *
 * <p>按用途拆分三个独立线程池，避免相互饿死：</p>
 * <ul>
 *   <li><b>ws</b>：WebSocket 推送；追求低延迟，默认 8 线程</li>
 *   <li><b>mongo</b>：消息落库；追求吞吐 + 隔离 DB 抖动，默认 4 线程</li>
 *   <li><b>conv</b>：会话状态更新 / FAQ / AI 触发链路，默认 4 线程</li>
 * </ul>
 *
 * <p>所有线程池规格都支持 {@code jeecg.cs.async.*} 配置覆盖，方便不同负载场景调参。</p>
 */
@Slf4j
@Component
public class CsAsyncTaskExecutor {

    @Value("${jeecg.cs.async.ws-threads:8}")
    private int wsThreads;

    @Value("${jeecg.cs.async.mongo-threads:4}")
    private int mongoThreads;

    @Value("${jeecg.cs.async.conversation-threads:4}")
    private int conversationThreads;

    @Value("${jeecg.cs.async.queue-size:2000}")
    private int queueSize;

    private ThreadPoolExecutor mongoExecutor;
    private ThreadPoolExecutor conversationExecutor;
    private ThreadPoolExecutor wsExecutor;

    @PostConstruct
    public void init() {
        this.mongoExecutor = buildExecutor("cs-mongo", mongoThreads, queueSize);
        this.conversationExecutor = buildExecutor("cs-conv", conversationThreads, queueSize);
        this.wsExecutor = buildExecutor("cs-ws", wsThreads, queueSize);
        log.info("[CS-Async] executors initialized: ws={}, mongo={}, conv={}, queue={}",
                wsThreads, mongoThreads, conversationThreads, queueSize);
    }

    public void submitMongo(Runnable task) {
        execute(mongoExecutor, task, "mongo");
    }

    public void submitConversation(Runnable task) {
        execute(conversationExecutor, task, "conversation");
    }

    public void submitWs(Runnable task) {
        execute(wsExecutor, task, "ws");
    }

    private void execute(ThreadPoolExecutor executor, Runnable task, String type) {
        if (executor == null) {
            // 极端情况下 Spring 尚未完成初始化，fall back 到 caller thread
            task.run();
            return;
        }
        try {
            executor.execute(task);
        } catch (RejectedExecutionException e) {
            log.warn("[CS-Async] {} queue full, running in caller thread", type);
            task.run();
        }
    }

    private ThreadPoolExecutor buildExecutor(String name, int size, int queueCap) {
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(queueCap);
        ThreadFactory factory = new ThreadFactory() {
            private final AtomicInteger index = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName(name + "-" + index.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        };
        return new ThreadPoolExecutor(
                size,
                size,
                60L,
                TimeUnit.SECONDS,
                queue,
                factory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @PreDestroy
    public void shutdown() {
        shutdownExecutor(mongoExecutor, "mongo");
        shutdownExecutor(conversationExecutor, "conversation");
        shutdownExecutor(wsExecutor, "ws");
    }

    private void shutdownExecutor(ExecutorService executor, String name) {
        if (executor == null) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("[CS-Async] {} executor shutdown timeout, forcing", name);
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}
