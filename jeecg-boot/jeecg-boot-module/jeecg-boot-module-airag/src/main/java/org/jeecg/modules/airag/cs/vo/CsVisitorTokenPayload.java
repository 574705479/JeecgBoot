package org.jeecg.modules.airag.cs.vo;

import lombok.Data;

/**
 * 访客短时token载荷
 */
@Data
public class CsVisitorTokenPayload {
    private String token;
    private String externalUserId;
    private String userName;
    private String appId;
    private Long expireAt;
    private String source;
}
