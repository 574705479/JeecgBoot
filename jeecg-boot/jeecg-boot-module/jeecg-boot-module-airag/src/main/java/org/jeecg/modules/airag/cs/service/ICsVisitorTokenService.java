package org.jeecg.modules.airag.cs.service;

import jakarta.servlet.http.HttpServletRequest;
import org.jeecg.modules.airag.cs.vo.CsVisitorTokenPayload;

/**
 * 访客短时token服务
 */
public interface ICsVisitorTokenService {

    CsVisitorTokenPayload issueToken(String externalUserId, String userName, String source);

    CsVisitorTokenPayload issueSessionToken(String externalUserId, String userName, String source);

    CsVisitorTokenPayload parseToken(String token);

    CsVisitorTokenPayload parseSessionToken(String token);

    String extractToken(HttpServletRequest request);

    String extractSessionToken(HttpServletRequest request);

    boolean isBlacklisted(String externalUserId);

    void blacklist(String externalUserId);

    /** 拉黑访客（含原因、名称、操作人，同步写入数据库） */
    void blacklistWithReason(String externalUserId, String visitorName, String reason, String operator);

    void unblacklist(String externalUserId);

    boolean isIpBlacklisted(String clientIp);

    void blacklistIp(String clientIp);

    /** 拉黑IP（含原因、操作人，同步写入数据库，支持CIDR段） */
    void blacklistIpWithReason(String ip, String reason, String operator);

    void unblacklistIp(String clientIp);

    boolean isAdminRequest(HttpServletRequest request);

    String getGlobalVisitorAppId();

    boolean checkRateLimit(String externalUserId, String clientIp);
}
