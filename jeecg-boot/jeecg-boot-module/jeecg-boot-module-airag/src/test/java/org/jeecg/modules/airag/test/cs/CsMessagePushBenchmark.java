package org.jeecg.modules.airag.test.cs;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * 客服消息推送极速化优化 —— 端到端压测脚本。
 *
 * <p>本类是独立可运行 main（非 @SpringBootTest），用 JDK 17 自带 HttpClient/WebSocket
 * 直接对已启动的 JeecgBoot 后端发压，测量"发送 → 对端 WS 收到"的端到端延迟分布。</p>
 *
 * <h3>前置条件</h3>
 * <ol>
 *   <li>后端已正常启动（默认 {@code http://localhost:8080/jeecg-boot}）。</li>
 *   <li>有一个 sys_user 账号并已在「系统-客服管理」挂接客服身份（默认压测账号 {@code jeecg/123456}）。</li>
 *   <li>为了跳过登录验证码，任选其一：
 *     <ul>
 *       <li>在 {@code application-dev.yml} 追加 {@code jeecg.firewall.enable-login-captcha: false}；</li>
 *       <li>或启动前手动登录拿到 JWT，并通过 {@code -Dbench.agentToken=<JWT>} 透传。</li>
 *     </ul>
 *   </li>
 *   <li>a2v / offline 两个场景需要访客 WebSocket 可建连；若后台「访客端访问控制」开启了 Token 模式，
 *       这两个场景会自动跳过并输出提示（v2a / fanout 不受影响）。</li>
 * </ol>
 *
 * <h3>运行方式</h3>
 * <pre>
 * # IDE：直接右键 Run main
 *
 * # 命令行（maven 3 + jdk 17）：
 * mvn -pl jeecg-boot/jeecg-boot-module/jeecg-boot-module-airag \
 *     test-compile exec:java \
 *     -Dexec.classpathScope=test \
 *     -Dexec.mainClass=org.jeecg.modules.airag.test.cs.CsMessagePushBenchmark \
 *     -Dbench.iterations=300 -Dbench.scenarios=v2a,a2v,fanout,offline
 * </pre>
 *
 * <h3>输出示例</h3>
 * <pre>
 * [STATS] V2A    count=200 avg=8.34ms min=2.1ms p50=6.8ms p95=18.1ms p99=31.5ms max=43.2ms
 * [STATS] A2V    count=200 avg=9.10ms min=2.4ms p50=7.2ms p95=19.4ms p99=34.0ms max=51.1ms
 * [STATS] Fanout count=200 avg=11.5ms ...
 * [OFFLINE] enqueued=20 delivered=20 ordered=true welcome.offlineDelivered=20
 * </pre>
 *
 * <h3>可调参数（-D 系统属性）</h3>
 * <pre>
 * bench.baseUrl           后端 URL，默认 http://localhost:8080/jeecg-boot
 * bench.wsBaseUrl         WS URL，默认 ws://localhost:8080/jeecg-boot
 * bench.agentUser         客服账号，默认 jeecg
 * bench.agentPass         客服密码，默认 123456
 * bench.agentToken        已有 JWT，非空则跳过登录
 * bench.scenarios         要跑的场景，逗号分隔：v2a,a2v,fanout,offline
 * bench.warmup            预热轮数，默认 20
 * bench.iterations        每场景正式采样数，默认 200
 * bench.visitorCount      扇出场景的访客数，默认 10
 * bench.fanoutPerVisitor  每个访客发送条数，默认 20
 * bench.offlineCount      离线缓冲发送条数，默认 20
 * bench.timeoutMs         单条消息等待超时，默认 5000ms
 * bench.transportKey      与后端 jeecg.cs.crypto.transport-key 一致，默认 dev key
 * bench.transportIv       同上，默认 dev iv
 * </pre>
 *
 * @author jeecg
 * @since 2026-04
 */
public final class CsMessagePushBenchmark {

    // ==================== Config ====================

    private static final String BASE_URL = sp("bench.baseUrl", "http://localhost:8080/jeecg-boot");
    private static final String WS_BASE_URL = sp("bench.wsBaseUrl", "ws://localhost:8080/jeecg-boot");
    private static final String AGENT_USER = sp("bench.agentUser", "jeecg");
    private static final String AGENT_PASS = sp("bench.agentPass", "123456");
    private static final String PRESET_AGENT_TOKEN = sp("bench.agentToken", "");
    private static final String SCENARIOS = sp("bench.scenarios", "v2a,a2v,fanout,offline");
    private static final int WARMUP = Integer.parseInt(sp("bench.warmup", "20"));
    private static final int ITER = Integer.parseInt(sp("bench.iterations", "200"));
    private static final int FANOUT_VISITORS = Integer.parseInt(sp("bench.visitorCount", "10"));
    private static final int FANOUT_PER_VISITOR = Integer.parseInt(sp("bench.fanoutPerVisitor", "20"));
    private static final int OFFLINE_COUNT = Integer.parseInt(sp("bench.offlineCount", "20"));
    private static final long MSG_TIMEOUT_MS = Long.parseLong(sp("bench.timeoutMs", "5000"));
    private static final String TRANSPORT_KEY = sp("bench.transportKey", "Cdg9VObOpE3yEQzz");
    private static final String TRANSPORT_IV = sp("bench.transportIv", "frYwJYzoqXvv5ePy");

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .executor(Executors.newFixedThreadPool(32, r -> {
                Thread t = new Thread(r, "bench-http");
                t.setDaemon(true);
                return t;
            }))
            .build();

    private static final HttpClient WS_HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .executor(Executors.newFixedThreadPool(16, r -> {
                Thread t = new Thread(r, "bench-ws");
                t.setDaemon(true);
                return t;
            }))
            .build();

    private CsMessagePushBenchmark() {
    }

    // ==================== Entry ====================

    public static void main(String[] args) throws Exception {
        String scenarios = args.length > 0 ? args[0] : SCENARIOS;
        log("==== CsMessagePushBenchmark ====");
        log("baseUrl=" + BASE_URL);
        log("wsBaseUrl=" + WS_BASE_URL);
        log("scenarios=" + scenarios + " warmup=" + WARMUP + " iter=" + ITER);

        String token = PRESET_AGENT_TOKEN;
        if (isEmpty(token)) {
            log("[BOOT] 登录 " + AGENT_USER);
            token = login(AGENT_USER, AGENT_PASS);
            if (isEmpty(token)) {
                die("登录失败。请确认账号密码，或在 application-dev.yml 添加 jeecg.firewall.enable-login-captcha: false，或通过 -Dbench.agentToken=<JWT> 跳过登录。");
            }
        }

        JSONObject agent = requireResult(getJson("/cs/agent/current", Map.of("X-Access-Token", token)));
        if (agent == null || isEmpty(agent.getString("id"))) {
            die("当前账号不是客服。请先在「系统-客服管理」里为该账号创建客服身份。");
        }
        String agentId = agent.getString("id");
        String agentName = agent.getString("nickname");
        log("[BOOT] Agent=" + agentName + " id=" + agentId);

        for (String s : scenarios.split(",")) {
            s = s.trim();
            if (s.isEmpty()) continue;
            long t0 = System.nanoTime();
            try {
                switch (s) {
                    case "v2a":
                        scenarioVisitorToAgent(token);
                        break;
                    case "a2v":
                        scenarioAgentToVisitor(token);
                        break;
                    case "fanout":
                        scenarioFanout(token);
                        break;
                    case "offline":
                        scenarioOfflineRecovery(token);
                        break;
                    default:
                        log("[WARN] 未知场景: " + s);
                }
            } catch (Throwable t) {
                log("[ERR] scenario " + s + " failed: " + t);
                t.printStackTrace();
            }
            log(String.format("[DONE] %s in %.2fs", s, (System.nanoTime() - t0) / 1e9));
        }
        log("==== Benchmark finished ====");
        System.exit(0);
    }

    // ==================== Scenarios ====================

    /**
     * 场景 V2A：访客 HTTP 发消息 → 客服 WS 收到。
     * 采用管理员模式绕过访客 Token，因此不需要开放免 Token 模式。
     */
    private static void scenarioVisitorToAgent(String agentToken) throws Exception {
        log("---- Scenario V2A ----");
        String visitorId = "bench_v2a_" + shortUuid();
        String convId = createAdminConversation(agentToken, visitorId, "压测访客V2A");
        log("[V2A] visitorId=" + visitorId + " conv=" + convId);

        try (WsClient agentWs = connectAgent(agentToken)) {
            for (int i = 0; i < WARMUP; i++) {
                String id = fakeVisitorSend(agentToken, convId, visitorId, "warmup-" + i);
                agentWs.awaitMessage(id, MSG_TIMEOUT_MS);
            }
            LatencyStats stats = new LatencyStats("V2A");
            int missed = 0;
            for (int i = 0; i < ITER; i++) {
                long t0 = System.nanoTime();
                String id = fakeVisitorSend(agentToken, convId, visitorId, "v2a-" + i);
                long recv = agentWs.awaitMessage(id, MSG_TIMEOUT_MS);
                if (recv < 0) {
                    missed++;
                } else {
                    stats.add(recv - t0);
                }
            }
            stats.print();
            if (missed > 0) log("[V2A] 丢失 " + missed + " 条（超过 " + MSG_TIMEOUT_MS + "ms 未到达）");
        }
    }

    /**
     * 场景 A2V：客服 HTTP 发消息 → 访客 WS 收到。
     * 需要访客 WS 能建连（后台「访客访问控制」需关闭 Token 模式）。
     */
    private static void scenarioAgentToVisitor(String agentToken) throws Exception {
        log("---- Scenario A2V ----");
        String visitorId = "bench_a2v_" + shortUuid();
        String convId = createAdminConversation(agentToken, visitorId, "压测访客A2V");
        log("[A2V] visitorId=" + visitorId + " conv=" + convId);

        WsClient visitorWs;
        try {
            visitorWs = connectVisitor(visitorId, convId);
            if (!visitorWs.awaitConnected(5_000)) {
                log("[A2V][SKIP] 访客 WS 连接超时（可能后台开启 Token 模式）。跳过。");
                return;
            }
        } catch (Exception e) {
            log("[A2V][SKIP] 访客 WS 连接失败: " + e + "。可能后台开启 Token 模式。跳过。");
            return;
        }

        try (visitorWs) {
            for (int i = 0; i < WARMUP; i++) {
                String id = agentSend(agentToken, convId, "warmup-" + i);
                visitorWs.awaitMessage(id, MSG_TIMEOUT_MS);
            }
            LatencyStats stats = new LatencyStats("A2V");
            int missed = 0;
            for (int i = 0; i < ITER; i++) {
                long t0 = System.nanoTime();
                String id = agentSend(agentToken, convId, "a2v-" + i);
                long recv = visitorWs.awaitMessage(id, MSG_TIMEOUT_MS);
                if (recv < 0) {
                    missed++;
                } else {
                    stats.add(recv - t0);
                }
            }
            stats.print();
            if (missed > 0) log("[A2V] 丢失 " + missed + " 条");
        }
    }

    /**
     * 场景 Fanout：N 个伪访客并发向同一客服发消息（扇入）。
     * 统计每条消息从发送到客服 WS 收到的端到端延迟。
     */
    private static void scenarioFanout(String agentToken) throws Exception {
        log("---- Scenario Fanout (" + FANOUT_VISITORS + " visitors x " + FANOUT_PER_VISITOR + " msgs) ----");

        // 为每个伪访客预创建一个会话
        List<String[]> visitors = new ArrayList<>();
        for (int i = 0; i < FANOUT_VISITORS; i++) {
            String vid = "bench_fan_" + i + "_" + shortUuid();
            String cid = createAdminConversation(agentToken, vid, "压测访客F" + i);
            visitors.add(new String[]{vid, cid});
        }

        try (WsClient agentWs = connectAgent(agentToken)) {
            // 预热：每个访客发 1 条
            for (String[] v : visitors) {
                String id = fakeVisitorSend(agentToken, v[1], v[0], "warmup");
                agentWs.awaitMessage(id, MSG_TIMEOUT_MS);
            }

            LatencyStats stats = new LatencyStats("Fanout");
            AtomicInteger missed = new AtomicInteger();
            CountDownLatch done = new CountDownLatch(FANOUT_VISITORS);

            long fanoutStart = System.nanoTime();
            for (String[] v : visitors) {
                final String vid = v[0];
                final String cid = v[1];
                final int visitorIdx = visitors.indexOf(v);
                Thread t = new Thread(() -> {
                    // 每个 visitor 线程独立的 HttpClient，解决 JDK HttpClient HTTP/1.1 单连接
                    // 被顺序复用导致扇入并发被客户端侧串行化的问题
                    HttpClient threadHttp = newThreadHttp();
                    try {
                        for (int i = 0; i < FANOUT_PER_VISITOR; i++) {
                            long t0 = System.nanoTime();
                            // 消息内容保持干净文本，避开后端敏感词库（dev 环境里 "f"、"as" 等单字符会误杀）
                            String id = fakeVisitorSend(threadHttp, agentToken, cid, vid, "ping v" + visitorIdx + " n" + i);
                            long recv = agentWs.awaitMessage(id, MSG_TIMEOUT_MS);
                            if (recv < 0) {
                                missed.incrementAndGet();
                            } else {
                                stats.add(recv - t0);
                            }
                        }
                    } catch (Exception e) {
                        log("[FAN] visitor " + vid + " err: " + e);
                    } finally {
                        done.countDown();
                    }
                }, "bench-fan-" + vid);
                t.setDaemon(true);
                t.start();
            }
            if (!done.await(MSG_TIMEOUT_MS * 2L * FANOUT_PER_VISITOR, TimeUnit.MILLISECONDS)) {
                log("[FAN] 部分访客线程超时未完成");
            }
            double elapsedSec = (System.nanoTime() - fanoutStart) / 1e9;
            int total = FANOUT_VISITORS * FANOUT_PER_VISITOR;
            log(String.format("[FAN] %d 条消息耗时 %.2fs, 吞吐 %.1f msg/s", total, elapsedSec, total / elapsedSec));
            stats.print();
            if (missed.get() > 0) log("[FAN] 丢失 " + missed.get() + " 条");
        }
    }

    /**
     * 场景 Offline：访客离线期间客服连发 N 条，访客重连后应通过 Redis Stream 一次性补齐。
     * 验证要点：
     *   1) 重连欢迎帧的 extra.offlineDelivered == N
     *   2) 收到的消息顺序与发送一致（FIFO）
     *   3) 全部 N 条都能收到
     */
    private static void scenarioOfflineRecovery(String agentToken) throws Exception {
        log("---- Scenario Offline Recovery ----");
        String visitorId = "bench_off_" + shortUuid();
        String convId = createAdminConversation(agentToken, visitorId, "压测访客OFF");
        log("[OFF] visitorId=" + visitorId + " conv=" + convId);

        // 先建一次连接确认访客 WS 通道可用，然后立即断开
        WsClient probe;
        try {
            probe = connectVisitor(visitorId, convId);
            if (!probe.awaitConnected(5_000)) {
                log("[OFF][SKIP] 访客 WS 连接超时（可能后台开启 Token 模式）。跳过。");
                return;
            }
            probe.close();
        } catch (Exception e) {
            log("[OFF][SKIP] 访客 WS 连接失败: " + e + "。跳过。");
            return;
        }

        // 等 200ms 让服务端清理 session
        Thread.sleep(200);

        // 客服连发 OFFLINE_COUNT 条
        List<String> sentIds = new ArrayList<>(OFFLINE_COUNT);
        for (int i = 0; i < OFFLINE_COUNT; i++) {
            sentIds.add(agentSend(agentToken, convId, "offline-" + i));
        }
        log("[OFF] 已向离线访客发送 " + sentIds.size() + " 条");

        // 访客重连
        try (WsClient reconnect = connectVisitor(visitorId, convId)) {
            if (!reconnect.awaitConnected(5_000)) {
                log("[OFF] 访客重连超时");
                return;
            }

            // 等待全部补齐或超时
            long deadline = System.nanoTime() + MSG_TIMEOUT_MS * 1_000_000L * 2;
            List<String> receivedInOrder = reconnect.getOrderedMessageIds();
            while (receivedInOrder.size() < sentIds.size() && System.nanoTime() < deadline) {
                LockSupport.parkNanos(2_000_000L);
                receivedInOrder = reconnect.getOrderedMessageIds();
            }

            int delivered = receivedInOrder.size();
            int offlineDelivered = reconnect.getWelcomeOfflineDelivered();
            boolean ordered = true;
            int check = Math.min(delivered, sentIds.size());
            for (int i = 0; i < check; i++) {
                if (!sentIds.get(i).equals(receivedInOrder.get(i))) {
                    ordered = false;
                    break;
                }
            }
            log(String.format("[OFFLINE] enqueued=%d delivered=%d ordered=%s welcome.offlineDelivered=%d",
                    sentIds.size(), delivered, ordered, offlineDelivered));
            if (delivered < sentIds.size()) {
                log("[OFFLINE] 未全部补齐。请检查：1) Redis Stream 是否可写；2) CsOfflineMessageBuffer 是否生效");
            }
        }
    }

    // ==================== Bootstrap ====================

    /** 调 /sys/login 拿 JWT。失败返回 null。 */
    private static String login(String user, String pass) {
        try {
            JSONObject body = new JSONObject();
            body.put("username", user);
            body.put("password", pass);
            body.put("captcha", "");
            body.put("checkKey", "");
            JSONObject resp = postJson("/sys/login", body.toJSONString(), Map.of());
            if (!Boolean.TRUE.equals(resp.getBoolean("success"))) {
                log("[LOGIN] fail: " + resp.getString("message"));
                return null;
            }
            JSONObject res = resp.getJSONObject("result");
            return res == null ? null : res.getString("token");
        } catch (Exception e) {
            log("[LOGIN] exception: " + e);
            return null;
        }
    }

    /** 以客服管理员身份为伪访客创建或复用会话。 */
    private static String createAdminConversation(String agentToken, String visitorId, String visitorName) throws Exception {
        JSONObject body = new JSONObject();
        body.put("userId", visitorId);
        body.put("userName", visitorName);
        JSONObject resp = postJson("/cs/conversation/get-or-create", body.toJSONString(),
                Map.of("X-Access-Token", agentToken));
        JSONObject conv = requireResult(resp);
        if (conv == null || isEmpty(conv.getString("id"))) {
            throw new RuntimeException("createAdminConversation failed: " + resp);
        }
        return conv.getString("id");
    }

    /** 以管理员身份伪装访客发消息，返回 messageId。 */
    private static String fakeVisitorSend(String agentToken, String conversationId, String visitorId, String content) throws Exception {
        return fakeVisitorSend(HTTP, agentToken, conversationId, visitorId, content);
    }

    /** Fanout 等扇入场景下让每个 visitor 线程传入独立 HttpClient，避免客户端侧 HTTP/1.1 单连接串行。 */
    private static String fakeVisitorSend(HttpClient client, String agentToken, String conversationId, String visitorId, String content) throws Exception {
        JSONObject body = new JSONObject();
        body.put("conversationId", conversationId);
        body.put("senderId", visitorId);
        body.put("senderType", "user");
        body.put("content", encryptTransport(content));
        body.put("msgType", 0);
        JSONObject resp = postJson(client, "/cs/message/send", body.toJSONString(),
                Map.of("X-Access-Token", agentToken));
        JSONObject msg = requireResult(resp);
        if (msg == null || isEmpty(msg.getString("id"))) {
            throw new RuntimeException("fakeVisitorSend failed: " + resp);
        }
        return msg.getString("id");
    }

    /** 客服发消息，返回 messageId。 */
    private static String agentSend(String agentToken, String conversationId, String content) throws Exception {
        JSONObject body = new JSONObject();
        body.put("conversationId", conversationId);
        body.put("content", encryptTransport(content));
        body.put("msgType", 0);
        JSONObject resp = postJson("/cs/message/agent/send", body.toJSONString(),
                Map.of("X-Access-Token", agentToken));
        JSONObject msg = requireResult(resp);
        if (msg == null || isEmpty(msg.getString("id"))) {
            throw new RuntimeException("agentSend failed: " + resp);
        }
        return msg.getString("id");
    }

    // ==================== HTTP ====================

    private static JSONObject postJson(String path, String body, Map<String, String> headers) throws Exception {
        return postJson(HTTP, path, body, headers);
    }

    /**
     * 扇入类并发场景下，每个 visitor 线程建议持一个独立 HttpClient，
     * 以绕过 JDK HttpClient HTTP/1.1 单连接被顺序复用的现象，得到真实后端吞吐。
     */
    private static JSONObject postJson(HttpClient client, String path, String body, Map<String, String> headers) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        headers.forEach(b::header);
        HttpResponse<String> r = client.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (r.statusCode() != 200) {
            throw new RuntimeException("POST " + path + " -> " + r.statusCode() + " body=" + r.body());
        }
        return JSON.parseObject(r.body());
    }

    private static JSONObject getJson(String path, Map<String, String> headers) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(10))
                .GET();
        headers.forEach(b::header);
        HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (r.statusCode() != 200) {
            throw new RuntimeException("GET " + path + " -> " + r.statusCode() + " body=" + r.body());
        }
        return JSON.parseObject(r.body());
    }

    /** 每线程独立的轻量 HttpClient，用于并发扇入场景。 */
    private static HttpClient newThreadHttp() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /** 取 Result.result 字段；若 success=false 或 result 不是 JSONObject 返回 null。 */
    private static JSONObject requireResult(JSONObject resp) {
        if (resp == null) return null;
        if (!Boolean.TRUE.equals(resp.getBoolean("success"))) {
            log("[WARN] resp not success: " + resp.getString("message"));
            return null;
        }
        Object res = resp.get("result");
        return (res instanceof JSONObject) ? (JSONObject) res : null;
    }

    // ==================== WebSocket ====================

    private static WsClient connectAgent(String token) throws Exception {
        String url = WS_BASE_URL + "/ws/cs/agent?userType=agent&token=" + urlEncode(token);
        return WsClient.open(url, "agent");
    }

    private static WsClient connectVisitor(String visitorId, String conversationId) throws Exception {
        String url = WS_BASE_URL + "/ws/cs/user"
                + "?userType=user"
                + "&userId=" + urlEncode(visitorId)
                + "&deviceId=" + urlEncode(visitorId)
                + "&conversationId=" + urlEncode(conversationId);
        return WsClient.open(url, "visitor:" + visitorId);
    }

    /**
     * 共享 ping 调度器：所有 WsClient 共用，每 15 秒发一次业务层 {@code {"type":"ping"}}。
     *
     * <p>后端 {@code AGENT_PING_TIMEOUT_MS = 60_000}，长时间运行的压测如果不主动发 ping，
     * 客服 session 会被服务端以 4003 ping_timeout 踢出，污染所有 warmup/iteration 较大的测量。</p>
     */
    private static final ScheduledExecutorService PING_SCHEDULER = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "bench-ping-" + System.identityHashCode(r));
        t.setDaemon(true);
        return t;
    });

    private static final long PING_INTERVAL_MS = 15_000L;

    /** WebSocket 客户端封装，按 messageId 做延迟匹配 + 维护接收顺序。 */
    private static final class WsClient implements AutoCloseable, WebSocket.Listener {
        private final String tag;
        private final ConcurrentHashMap<String, Long> ackByMessageId = new ConcurrentHashMap<>();
        private final List<String> orderedMessageIds = new ArrayList<>();
        private final AtomicLong welcomeOfflineDelivered = new AtomicLong(-1);
        private final CountDownLatch connected = new CountDownLatch(1);
        private volatile WebSocket socket;
        private StringBuilder buffer = new StringBuilder(1024);
        private volatile ScheduledFuture<?> pingTask;

        private WsClient(String tag) {
            this.tag = tag;
        }

        static WsClient open(String url, String tag) throws Exception {
            WsClient c = new WsClient(tag);
            try {
                c.socket = WS_HTTP.newWebSocketBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .buildAsync(URI.create(url), c)
                        .get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException("WS connect failed [" + tag + "] url=" + url + " err=" + e, e);
            }
            c.startPing();
            return c;
        }

        private void startPing() {
            pingTask = PING_SCHEDULER.scheduleAtFixedRate(() -> {
                try {
                    WebSocket ws = socket;
                    if (ws != null && !ws.isOutputClosed()) {
                        ws.sendText("{\"type\":\"ping\"}", true);
                    }
                } catch (Throwable t) {
                    // 静默：ping 失败说明 session 已经挂了，让正常流程处理
                }
            }, PING_INTERVAL_MS, PING_INTERVAL_MS, TimeUnit.MILLISECONDS);
        }

        boolean awaitConnected(long timeoutMs) throws InterruptedException {
            return connected.await(timeoutMs, TimeUnit.MILLISECONDS);
        }

        /**
         * 等待某个 messageId 的 WS 到达。返回到达时 nanoTime，-1 表示超时。
         * 命中后会消耗记录。
         */
        long awaitMessage(String messageId, long timeoutMs) {
            long deadline = System.nanoTime() + timeoutMs * 1_000_000L;
            while (System.nanoTime() < deadline) {
                Long ns = ackByMessageId.remove(messageId);
                if (ns != null) return ns;
                LockSupport.parkNanos(100_000L);
            }
            return -1L;
        }

        List<String> getOrderedMessageIds() {
            synchronized (orderedMessageIds) {
                return new ArrayList<>(orderedMessageIds);
            }
        }

        int getWelcomeOfflineDelivered() {
            long v = welcomeOfflineDelivered.get();
            return v < 0 ? 0 : (int) v;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String frame = buffer.toString();
                buffer.setLength(0);
                handleFrame(frame);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log("[WS:" + tag + "] error: " + error);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            log("[WS:" + tag + "] close " + statusCode + " " + reason);
            return null;
        }

        private void handleFrame(String json) {
            long recvNanos = System.nanoTime();
            try {
                JSONObject obj = JSON.parseObject(json);
                String type = obj.getString("type");
                if ("connected".equals(type)) {
                    JSONObject extra = obj.getJSONObject("extra");
                    if (extra != null && extra.containsKey("offlineDelivered")) {
                        welcomeOfflineDelivered.compareAndSet(-1, extra.getLongValue("offlineDelivered"));
                    }
                    connected.countDown();
                    return;
                }
                if (!"message".equals(type) && !"ai_stream_complete".equals(type)) {
                    return;
                }
                String mid = obj.getString("messageId");
                if (isEmpty(mid)) return;
                ackByMessageId.putIfAbsent(mid, recvNanos);
                synchronized (orderedMessageIds) {
                    orderedMessageIds.add(mid);
                }
            } catch (Exception e) {
                // 非 JSON 或字段缺失，忽略
            }
        }

        @Override
        public void close() {
            try {
                if (pingTask != null) {
                    pingTask.cancel(false);
                }
            } catch (Exception ignore) {
            }
            try {
                if (socket != null) {
                    socket.sendClose(WebSocket.NORMAL_CLOSURE, "bye").get(2, TimeUnit.SECONDS);
                }
            } catch (Exception ignore) {
            }
        }
    }

    // ==================== Crypto ====================

    private static String encryptTransport(String data) {
        if (data == null || data.isEmpty()) return data;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec key = new SecretKeySpec(TRANSPORT_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec iv = new IvParameterSpec(TRANSPORT_IV.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            byte[] enc = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(enc);
        } catch (Exception e) {
            throw new RuntimeException("encryptTransport failed", e);
        }
    }

    // ==================== Stats ====================

    private static final class LatencyStats {
        final String name;
        final List<Long> samplesNanos = new ArrayList<>();

        LatencyStats(String name) {
            this.name = name;
        }

        synchronized void add(long ns) {
            if (ns > 0) samplesNanos.add(ns);
        }

        void print() {
            long[] arr;
            synchronized (this) {
                arr = samplesNanos.stream().mapToLong(Long::longValue).sorted().toArray();
            }
            if (arr.length == 0) {
                log("[STATS] " + name + " no samples");
                return;
            }
            double avg = Arrays.stream(arr).average().orElse(0);
            log(String.format(
                    "[STATS] %-8s count=%d avg=%.2fms min=%.2fms p50=%.2fms p90=%.2fms p95=%.2fms p99=%.2fms p999=%.2fms max=%.2fms",
                    name, arr.length, avg / 1e6,
                    arr[0] / 1e6, pct(arr, 0.50) / 1e6, pct(arr, 0.90) / 1e6, pct(arr, 0.95) / 1e6,
                    pct(arr, 0.99) / 1e6, pct(arr, 0.999) / 1e6, arr[arr.length - 1] / 1e6));
        }

        private static long pct(long[] sorted, double p) {
            int idx = (int) Math.ceil(p * sorted.length) - 1;
            if (idx < 0) idx = 0;
            if (idx >= sorted.length) idx = sorted.length - 1;
            return sorted[idx];
        }
    }

    // ==================== Util ====================

    private static String sp(String key, String def) {
        String v = System.getProperty(key);
        return (v == null || v.isEmpty()) ? def : v;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private static String shortUuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private static String urlEncode(String v) {
        try {
            return java.net.URLEncoder.encode(v, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return v;
        }
    }

    private static void log(String s) {
        System.out.println("[" + System.currentTimeMillis() + "] " + s);
    }

    private static void die(String msg) {
        log("[FATAL] " + msg);
        System.exit(1);
    }
}
