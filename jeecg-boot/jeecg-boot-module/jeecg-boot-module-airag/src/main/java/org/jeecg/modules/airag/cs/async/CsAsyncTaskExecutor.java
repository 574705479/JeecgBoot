package org.jeecg.modules.airag.cs.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class CsAsyncTaskExecutor {
    private static final int MONGO_THREADS = 2;
    private static final int CONVERSATION_THREADS = 2;
    private static final int WS_THREADS = 4;
    private static final int MONGO_QUEUE = 2000;
    private static final int CONVERSATION_QUEUE = 2000;
    private static final int WS_QUEUE = 2000;

    private final ThreadPoolExecutor mongoExecutor = buildExecutor("cs-mongo", MONGO_THREADS, MONGO_QUEUE);
    private final ThreadPoolExecutor conversationExecutor = buildExecutor("cs-conv", CONVERSATION_THREADS, CONVERSATION_QUEUE);
    private final ThreadPoolExecutor wsExecutor = buildExecutor("cs-ws", WS_THREADS, WS_QUEUE);

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
        try {
            executor.execute(task);
        } catch (RejectedExecutionException e) {
            log.warn("[CS-Async] {} queue full, running in caller thread", type);
            task.run();
        }
    }

    private ThreadPoolExecutor buildExecutor(String name, int size, int queueSize) {
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(queueSize);
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
