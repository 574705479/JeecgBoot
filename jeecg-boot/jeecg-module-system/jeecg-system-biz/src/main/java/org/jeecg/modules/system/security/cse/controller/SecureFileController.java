package org.jeecg.modules.system.security.cse.controller;

import com.alibaba.fastjson.JSONObject;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.CommonAPI;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.TokenUtils;
import org.jeecg.config.shiro.IgnoreAuth;
import org.jeecg.modules.airag.cs.service.ICsVisitorTokenService;
import org.jeecg.modules.oss.entity.OssFile;
import org.jeecg.modules.system.security.cse.service.CseFileStorage;
import org.jeecg.modules.system.security.cse.service.FileEncryptionService;
import org.jeecg.modules.system.security.cse.service.OssFileMetaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * CSE 加密文件下行接口
 * <ul>
 *   <li>GET /sys/secure/file/{fid} 返回密文流（octet-stream）</li>
 *   <li>GET /sys/secure/file/{fid}/key 返回 HKDF 二次包装后的 DEK</li>
 * </ul>
 *
 * Phase 3.1a：方法级 {@link IgnoreAuth} —— 让访客（仅持 X-Visitor-Session/X-Visitor-Token）
 * 也能进入 controller，由 controller 内部根据凭证类型自行做权限判定，
 * 而不是被 Shiro 全局拒绝。已登录用户由 {@link #tryResolveLoginUser} 走 JwtUtil 手动解析。
 *
 * Phase 3.1b：抽出 {@link #resolveVisitorCredential}，统一访问凭证（登录 token / 访客 session / 旧版访客 token）
 * 的派生密钥与 canRead 入参，避免 download/getKey 双入口出现头部读取顺序不一致。
 */
@Slf4j
@RestController
@RequestMapping("/sys/secure/file")
public class SecureFileController {

    @Autowired
    private OssFileMetaService metaService;

    @Autowired
    private CseFileStorage cseFileStorage;

    @Autowired
    private FileEncryptionService fileEncryptionService;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Autowired
    private CommonAPI commonApi;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * Phase 3.2f：免 Token 模式下复用 CS 访客模块的 appKey 校验。
     * 通过 setter 注入避免循环依赖（airag → system-local-api → system-biz）。
     */
    @Autowired(required = false)
    private ICsVisitorTokenService csVisitorTokenService;

    private void incr(String name, String... tags) {
        try {
            if (meterRegistry != null) meterRegistry.counter(name, tags).increment();
        } catch (Exception ignored) {}
    }

    /** 已 unwrap 的 DEK 10 秒短期缓存（懒过期，减小 GCM 操作压力） */
    private final ConcurrentMap<String, DekCacheEntry> dekCache = new ConcurrentHashMap<>();
    private static final long DEK_CACHE_TTL_MS = 10_000L;

    private static class DekCacheEntry {
        final byte[] dek;
        final long expireAt;
        DekCacheEntry(byte[] dek, long expireAt) {
            this.dek = dek;
            this.expireAt = expireAt;
        }
    }

    private byte[] getOrUnwrapDek(OssFile file) {
        String fid = file.getFileId();
        long now = System.currentTimeMillis();
        DekCacheEntry e = dekCache.get(fid);
        if (e != null && e.expireAt > now) {
            return e.dek;
        }
        byte[] wrapped = Base64.getDecoder().decode(file.getDekWrappedB64());
        byte[] dek = fileEncryptionService.unwrapDek(wrapped, file.getKekKid());
        dekCache.put(fid, new DekCacheEntry(dek, now + DEK_CACHE_TTL_MS));
        return dek;
    }

    /**
     * 统一访问凭证（Phase 3.1b T3 终审）。
     * 优先级：
     *   1) X-Access-Token（登录用户，且非空）
     *   2) X-Visitor-Session（访客 SDK / 客服面板新版头）
     *   3) X-Visitor-Token（兼容老接口）
     *
     * - {@code sealToken} 用于 HKDF 二次包装 DEK，必须与前端 cseDecrypt.ts 解包时使用的 token 一致；
     * - {@code visitorTokenForCanRead} 仅在「非登录」场景下传给 {@link OssFileMetaService#canRead} 用作可信通道判定；
     *   登录场景下应传 null，避免 canRead 走访客分支误放行。
     */
    private static class AccessCredential {
        final String sealToken;
        final String visitorTokenForCanRead;
        AccessCredential(String sealToken, String visitorTokenForCanRead) {
            this.sealToken = sealToken;
            this.visitorTokenForCanRead = visitorTokenForCanRead;
        }
    }

    private AccessCredential resolveVisitorCredential(HttpServletRequest req) {
        String access = req.getHeader("X-Access-Token");
        if (access != null && !access.isEmpty()) {
            return new AccessCredential(access, null);
        }
        String session = req.getHeader("X-Visitor-Session");
        if (session != null && !session.isEmpty()) {
            return new AccessCredential(session, session);
        }
        String visitor = req.getHeader("X-Visitor-Token");
        if (visitor != null && !visitor.isEmpty()) {
            return new AccessCredential(visitor, visitor);
        }
        // Phase 3.2f：免 Token 模式访客通道（X-Device-Id + X-App-Secret）
        // 触发条件（必须全部满足）：
        //   1) 全局 tokenRequired=false（租户主动选择免 Token 模式）
        //   2) X-Device-Id 非空（前端 SDK 持久化的访客设备码）
        //   3) X-App-Secret / ?key= 与租户配置的 secretKey 一致（validateAppKey 自带校验）
        // sealToken = deviceId（每访客不同，DEK 不会跨用户泄露）
        // 与前端 cseAuthContext.getCseAuthToken() 在 device 模式下返回 deviceId 严格对齐。
        if (csVisitorTokenService != null) {
            try {
                if (!csVisitorTokenService.isTokenRequired()) {
                    String deviceId = csVisitorTokenService.extractDeviceId(req);
                    if (deviceId != null && !deviceId.isEmpty()
                            && csVisitorTokenService.validateAppKey(req)) {
                        return new AccessCredential(deviceId, deviceId);
                    }
                }
            } catch (Exception e) {
                log.warn("[CSE] device credential resolve failed: {}", e.getMessage());
            }
        }
        return new AccessCredential(null, null);
    }

    /**
     * 由于 controller 标了 @IgnoreAuth，Shiro Subject.principal 始终为 null。
     * 此处手工通过 X-Access-Token 解析登录用户（与 TokenUtils.getLoginUser 完全一致）。
     * 解析失败一律返回 null，由后续 canRead + 访客分支处理。
     */
    private LoginUser tryResolveLoginUser(HttpServletRequest req) {
        String token = req.getHeader("X-Access-Token");
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            String username = JwtUtil.getUsername(token);
            if (username == null || username.isEmpty()) {
                return null;
            }
            return TokenUtils.getLoginUser(username, commonApi, redisUtil);
        } catch (Exception e) {
            log.warn("[CSE] tryResolveLoginUser failed: {}", e.getMessage());
            return null;
        }
    }

    @IgnoreAuth
    @GetMapping("/{fid}")
    public void download(@PathVariable("fid") String fid,
                         @RequestParam(value = "thumb", required = false) Integer thumb,
                         HttpServletRequest request, HttpServletResponse response) throws IOException {
        OssFile file = metaService.getByFileId(fid);
        if (file == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        LoginUser user = tryResolveLoginUser(request);
        AccessCredential cred = resolveVisitorCredential(request);
        if (!metaService.canRead(file, user, cred.visitorTokenForCanRead)) {
            int code = user == null ? HttpServletResponse.SC_UNAUTHORIZED : HttpServletResponse.SC_FORBIDDEN;
            incr("cse.download.deny", "code", String.valueOf(code));
            response.setStatus(code);
            return;
        }
        boolean isThumb = thumb != null && thumb == 1 && file.getThumbObjectKey() != null && !file.getThumbObjectKey().isEmpty();
        String objectKey = isThumb ? file.getThumbObjectKey() : file.getObjectKey();
        if (objectKey == null || objectKey.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        // 304 短路：密文按 fid（thumb 加后缀）做强 ETag。客户端命中即可省掉密文体传输。
        String etag = "\"" + fid + (isThumb ? "-t" : "") + "\"";
        String ifNoneMatch = request.getHeader("If-None-Match");
        if (etag.equals(ifNoneMatch)) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            response.setHeader("ETag", etag);
            response.setHeader("Cache-Control", "private, max-age=300, must-revalidate");
            response.setHeader("Vary", "Authorization, X-Visitor-Session");
            return;
        }
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        // 关键安全调整：
        // 1. max-age 从 86400 收紧到 300，避免 token 撤销 / 会话切换的 24h 窗口里旧密文从 disk cache 直出
        // 2. must-revalidate 强制过期后回源校验权限
        // 3. ETag 让回源时走 304 不重传密文体
        // 4. Vary: Authorization, X-Visitor-Session 防止同一浏览器多账号缓存交叉污染
        // 5. 不用 immutable，与 must-revalidate 矛盾、且 token 撤销窗口有泄密风险
        response.setHeader("Cache-Control", "private, max-age=300, must-revalidate");
        response.setHeader("ETag", etag);
        response.setHeader("Vary", "Authorization, X-Visitor-Session");
        try (InputStream in = cseFileStorage.openCipher(file.getStorageType(), file.getBucket(), objectKey);
             OutputStream out = response.getOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            out.flush();
        } catch (Exception e) {
            log.error("[CSE] download fid={} error", fid, e);
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    @IgnoreAuth
    @GetMapping("/{fid}/key")
    public Result<JSONObject> getKey(@PathVariable("fid") String fid, HttpServletRequest request, HttpServletResponse response) {
        // F1 安全兜底：sealedDek 用一次性 sealToken 签出，绝不可被任何缓存层留存。
        // 即使前端将来误传 joinTime: false 关闭 _t=，这里也要拒绝缓存。
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        OssFile file = metaService.getByFileId(fid);
        if (file == null) {
            return Result.error(404, "文件不存在");
        }
        LoginUser user = tryResolveLoginUser(request);
        AccessCredential cred = resolveVisitorCredential(request);
        if (!metaService.canRead(file, user, cred.visitorTokenForCanRead)) {
            int code = user == null ? 401 : 403;
            incr("cse.dek.deny", "code", String.valueOf(code));
            return Result.error(code, "无权访问");
        }
        if (cred.sealToken == null || cred.sealToken.isEmpty()) {
            return Result.error(401, "缺少 token");
        }
        long t0 = System.nanoTime();
        byte[] dekClear = getOrUnwrapDek(file);
        byte[] iv = Base64.getDecoder().decode(file.getIvB64());
        byte[] sealedDek = fileEncryptionService.sealDekForClient(dekClear, cred.sealToken, fid, file.getKekKid(), iv);

        JSONObject body = new JSONObject();
        body.put("algo", file.getAlgo());
        body.put("ivB64", file.getIvB64());
        body.put("kid", file.getKekKid());
        body.put("dekSealedB64", Base64.getEncoder().encodeToString(sealedDek));
        try {
            if (meterRegistry != null) {
                Timer.builder("cse.dek.seal.duration")
                        .register(meterRegistry)
                        .record(System.nanoTime() - t0, TimeUnit.NANOSECONDS);
                meterRegistry.counter("cse.dek.success").increment();
            }
        } catch (Exception ignored) {}
        return Result.OK(body);
    }
}
