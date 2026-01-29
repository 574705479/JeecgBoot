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

    void unblacklist(String externalUserId);

    boolean isIpBlacklisted(String clientIp);

    void blacklistIp(String clientIp);

    void unblacklistIp(String clientIp);

    boolean isAdminRequest(HttpServletRequest request);

    String getGlobalVisitorAppId();

    boolean checkRateLimit(String externalUserId, String clientIp);
}
