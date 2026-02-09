package org.jeecg.modules.airag.cs.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.modules.airag.cs.entity.CsVisitor;
import org.jeecg.modules.airag.cs.entity.CsGlobalConfig;
import org.jeecg.modules.airag.cs.mapper.CsGlobalConfigMapper;
import org.jeecg.modules.airag.cs.service.ICsVisitorService;
import org.jeecg.modules.airag.cs.service.ICsVisitorTokenService;
import org.jeecg.modules.airag.cs.vo.CsVisitorTokenPayload;
import org.jeecg.common.util.oConvertUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.regex.Pattern;
import java.util.Date;
import java.util.Map;

/**
 * 访客信息Controller
 * 
 * @author jeecg
 * @date 2026-01-12
 */
@Slf4j
@Tag(name = "客服-访客管理")
@RestController
@RequestMapping("/airag/cs/visitor")
public class CsVisitorController extends JeecgController<CsVisitor, ICsVisitorService> {

    @Autowired
    private ICsVisitorService visitorService;

    @Autowired
    private ICsVisitorTokenService visitorTokenService;

    private static final String VISITOR_ACCESS_REDIS_KEY = "cs:global:visitor_access";
    private static final String VISITOR_ACCESS_CONFIG_KEY = "visitor_access";

    @Autowired
    private CsGlobalConfigMapper csGlobalConfigMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 分页查询访客列表
     */
    @Operation(summary = "分页查询访客列表")
    @GetMapping("/list")
    public Result<IPage<CsVisitor>> list(
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) Integer star,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        Page<CsVisitor> page = new Page<>(pageNo, pageSize);
        IPage<CsVisitor> result = visitorService.pageVisitors(page, appId, keyword, level, star);
        if (result != null && result.getRecords() != null) {
            for (CsVisitor visitor : result.getRecords()) {
                visitor.setBlacklisted(visitorTokenService.isBlacklisted(visitor.getUserId()));
            }
        }
        return Result.OK(result);
    }

    /**
     * 根据ID查询访客详情
     */
    @Operation(summary = "查询访客详情")
    @GetMapping("/detail")
    public Result<CsVisitor> detail(@RequestParam String id) {
        CsVisitor visitor = visitorService.getById(id);
        if (visitor == null) {
            return Result.error("访客不存在");
        }
        return Result.OK(visitor);
    }

    /**
     * 根据appId和userId查询访客
     * 如果访客不存在，返回空对象（不报错，因为新访客可能还没有创建记录）
     * 注：新版客服系统不再强制要求appId，可以只通过userId查询
     */
    @Operation(summary = "根据appId和userId查询访客")
    @GetMapping("/getByUser")
    public Result<CsVisitor> getByUser(
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String userId) {
        if (userId == null || userId.isEmpty()) {
            // userId是必须的
            return Result.OK(null);
        }
        
        CsVisitor visitor;
        if (appId != null && !appId.isEmpty()) {
            // 有appId时，按appId+userId精确查询
            visitor = visitorService.getByAppAndUser(appId, userId);
        } else {
            // 无appId时，只按userId查询（取最新的一条）
            visitor = visitorService.getByUserId(userId);
        }
        // 不管是否存在都返回OK，前端根据是否有数据判断
        return Result.OK(visitor);
    }

    /**
     * 生成访客短时token（第三方接入）
     */
    @Operation(summary = "生成访客短时token")
    @org.jeecg.config.shiro.IgnoreAuth
    @PostMapping("/token")
    public Result<CsVisitorTokenPayload> issueToken(@RequestBody Map<String, String> params, HttpServletRequest request) {
        String externalUserId = params.get("externalUserId");
        String userName = params.get("userName");
        String source = params.get("source");
        String clientIp = getClientIp(request);

        if (oConvertUtils.isEmpty(externalUserId)) {
            return Result.error("externalUserId不能为空");
        }
        if (oConvertUtils.isEmpty(source)) {
            return Result.error("source不能为空");
        }
        if (visitorTokenService.isIpBlacklisted(clientIp)) {
            return Result.error("IP已被拉黑");
        }

        String normalizedUserId = source + ":" + externalUserId;
        if (visitorTokenService.isBlacklisted(normalizedUserId)) {
            return Result.error("访客已被拉黑");
        }

        String secretKey = request.getHeader("X-App-Secret");
        if (oConvertUtils.isEmpty(secretKey)) {
            secretKey = params.get("secretKey");
        }

        String appId = visitorTokenService.getGlobalVisitorAppId();
        if (oConvertUtils.isEmpty(appId)) {
            return Result.error("未配置全局访客AI应用");
        }
        VisitorAccessConfig accessConfig = getVisitorAccessConfig();
        if (accessConfig != null && oConvertUtils.isNotEmpty(accessConfig.secretKey)) {
            if (oConvertUtils.isEmpty(secretKey) || !accessConfig.secretKey.equals(secretKey)) {
                return Result.error("密钥无效");
            }
        }
        if (accessConfig != null && oConvertUtils.isNotEmpty(accessConfig.whitelist)) {
            String referer = request.getHeader("Referer");
            WhitelistCheckResult check = validateSourceWhitelist(accessConfig.whitelist, referer, clientIp);
            if (!check.allowed) {
                return Result.error(check.reason);
            }
        }

        if (!visitorTokenService.checkRateLimit(normalizedUserId, clientIp)) {
            return Result.error("获取token过于频繁");
        }

        CsVisitorTokenPayload payload = visitorTokenService.issueToken(externalUserId, userName, source);
        if (payload == null) {
            return Result.error("生成token失败");
        }
        return Result.OK(payload);
    }

    /**
     * 校验短时token是否有效
     */
    @Operation(summary = "校验短时token")
    @org.jeecg.config.shiro.IgnoreAuth
    @GetMapping("/token/validate")
    public Result<CsVisitorTokenPayload> validateToken(HttpServletRequest request) {
        String shortToken = request.getHeader("X-Visitor-Token");
        if (oConvertUtils.isEmpty(shortToken)) {
            shortToken = request.getParameter("token");
        }
        if (oConvertUtils.isEmpty(shortToken)) {
            return Result.error("短时token不能为空");
        }
        CsVisitorTokenPayload payload = visitorTokenService.parseToken(shortToken);
        if (payload == null) {
            return Result.error("短时token无效或已过期");
        }
        if (visitorTokenService.isBlacklisted(payload.getExternalUserId())) {
            return Result.error("访客已被拉黑");
        }
        return Result.OK(payload);
    }

    /**
     * 用短时token换取会话凭证
     */
    @Operation(summary = "换取会话凭证")
    @org.jeecg.config.shiro.IgnoreAuth
    @PostMapping("/session/exchange")
    public Result<CsVisitorTokenPayload> exchangeSession(@RequestBody(required = false) Map<String, String> params,
                                                         HttpServletRequest request) {
        String clientIp = getClientIp(request);
        if (visitorTokenService.isIpBlacklisted(clientIp)) {
            return Result.error("IP已被拉黑");
        }
        String shortToken = request.getHeader("X-Visitor-Token");
        if (oConvertUtils.isEmpty(shortToken) && params != null) {
            shortToken = params.get("token");
        }
        if (oConvertUtils.isEmpty(shortToken)) {
            shortToken = request.getParameter("token");
        }
        if (oConvertUtils.isEmpty(shortToken)) {
            return Result.error("短时token不能为空");
        }

        CsVisitorTokenPayload payload = visitorTokenService.parseToken(shortToken);
        if (payload == null) {
            return Result.error("短时token无效或已过期");
        }
        if (visitorTokenService.isBlacklisted(payload.getExternalUserId())) {
            return Result.error("访客已被拉黑");
        }

        String source = payload.getSource();
        String normalized = payload.getExternalUserId();
        String rawId = normalized;
        if (oConvertUtils.isNotEmpty(source) && normalized != null && normalized.startsWith(source + ":")) {
            rawId = normalized.substring(source.length() + 1);
        }

        CsVisitorTokenPayload session = visitorTokenService.issueSessionToken(rawId, payload.getUserName(), source);
        if (session == null) {
            return Result.error("生成会话凭证失败");
        }
        return Result.OK(session);
    }

    /**
     * 校验会话凭证是否有效
     */
    @Operation(summary = "校验会话凭证")
    @org.jeecg.config.shiro.IgnoreAuth
    @GetMapping("/session/validate")
    public Result<CsVisitorTokenPayload> validateSession(HttpServletRequest request) {
        String sessionToken = request.getHeader("X-Visitor-Session");
        if (oConvertUtils.isEmpty(sessionToken)) {
            sessionToken = request.getParameter("sessionToken");
        }
        if (oConvertUtils.isEmpty(sessionToken)) {
            return Result.error("会话凭证不能为空");
        }
        CsVisitorTokenPayload payload = visitorTokenService.parseSessionToken(sessionToken);
        if (payload == null) {
            return Result.error("会话凭证无效或已过期");
        }
        if (visitorTokenService.isBlacklisted(payload.getExternalUserId())) {
            return Result.error("访客已被拉黑");
        }
        return Result.OK(payload);
    }

    /**
     * 拉黑访客
     */
    @Operation(summary = "拉黑访客")
    @PostMapping("/blacklist/add")
    public Result<String> addBlacklist(@RequestParam(required = false) String userId,
                                       @RequestBody(required = false) Map<String, String> body) {
        String target = userId != null ? userId : (body != null ? body.get("userId") : null);
        if (oConvertUtils.isEmpty(target)) {
            return Result.error("userId不能为空");
        }
        String reason = body != null ? body.get("reason") : null;
        String visitorName = body != null ? body.get("visitorName") : null;
        org.jeecg.common.system.vo.LoginUser loginUser = (org.jeecg.common.system.vo.LoginUser) org.apache.shiro.SecurityUtils.getSubject().getPrincipal();
        String operator = loginUser != null ? loginUser.getUsername() : "system";
        visitorTokenService.blacklistWithReason(target, visitorName, reason, operator);
        return Result.OK("已拉黑");
    }

    /**
     * 取消拉黑
     */
    @Operation(summary = "取消拉黑")
    @PostMapping("/blacklist/remove")
    public Result<String> removeBlacklist(@RequestParam(required = false) String userId,
                                          @RequestBody(required = false) Map<String, String> body) {
        String target = userId != null ? userId : (body != null ? body.get("userId") : null);
        if (oConvertUtils.isEmpty(target)) {
            return Result.error("userId不能为空");
        }
        visitorTokenService.unblacklist(target);
        return Result.OK("已取消拉黑");
    }

    /**
     * 检查是否拉黑
     */
    @Operation(summary = "检查是否拉黑")
    @GetMapping("/blacklist/check")
    public Result<Map<String, Object>> checkBlacklist(@RequestParam String userId) {
        boolean blocked = visitorTokenService.isBlacklisted(userId);
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("blacklisted", blocked);
        return Result.OK(result);
    }

    /**
     * 访客端自检拉黑（通过访客token解析用户）
     */
    @Operation(summary = "访客端自检拉黑")
    @org.jeecg.config.shiro.IgnoreAuth
    @GetMapping("/blacklist/check-self")
    public Result<Map<String, Object>> checkSelfBlacklist(HttpServletRequest request) {
        String shortToken = visitorTokenService.extractToken(request);
        String sessionToken = visitorTokenService.extractSessionToken(request);
        CsVisitorTokenPayload payload = null;
        if (oConvertUtils.isNotEmpty(sessionToken)) {
            payload = visitorTokenService.parseSessionToken(sessionToken);
        }
        if (payload == null && oConvertUtils.isNotEmpty(shortToken)) {
            payload = visitorTokenService.parseToken(shortToken);
        }
        if (payload == null) {
            return Result.error("访客凭证无效或已过期");
        }
        boolean blocked = visitorTokenService.isBlacklisted(payload.getExternalUserId());
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("blacklisted", blocked);
        return Result.OK(result);
    }

    /**
     * 拉黑IP（支持IP段）
     */
    @Operation(summary = "拉黑IP")
    @PostMapping("/blacklist/ip/add")
    public Result<String> addIpBlacklist(@RequestParam(required = false) String ip,
                                         @RequestBody(required = false) Map<String, String> body) {
        String target = ip != null ? ip : (body != null ? body.get("ip") : null);
        if (oConvertUtils.isEmpty(target)) {
            return Result.error("ip不能为空");
        }
        if (!org.jeecg.modules.airag.cs.util.CsIpMatchUtil.isValidIpOrCidr(target)) {
            return Result.error("IP格式无效，支持单个IP或CIDR段（如192.168.1.0/24）");
        }
        String reason = body != null ? body.get("reason") : null;
        org.jeecg.common.system.vo.LoginUser loginUser = (org.jeecg.common.system.vo.LoginUser) org.apache.shiro.SecurityUtils.getSubject().getPrincipal();
        String operator = loginUser != null ? loginUser.getUsername() : "system";
        visitorTokenService.blacklistIpWithReason(target.trim(), reason, operator);
        return Result.OK("已拉黑");
    }

    /**
     * 取消拉黑IP
     */
    @Operation(summary = "取消拉黑IP")
    @PostMapping("/blacklist/ip/remove")
    public Result<String> removeIpBlacklist(@RequestParam(required = false) String ip,
                                            @RequestBody(required = false) Map<String, String> body) {
        String target = ip != null ? ip : (body != null ? body.get("ip") : null);
        if (oConvertUtils.isEmpty(target)) {
            return Result.error("ip不能为空");
        }
        visitorTokenService.unblacklistIp(target);
        return Result.OK("已取消拉黑");
    }

    /**
     * 检查IP是否拉黑
     */
    @Operation(summary = "检查IP是否拉黑")
    @GetMapping("/blacklist/ip/check")
    public Result<Map<String, Object>> checkIpBlacklist(@RequestParam String ip) {
        boolean blocked = visitorTokenService.isIpBlacklisted(ip);
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("blacklisted", blocked);
        return Result.OK(result);
    }

    /**
     * 检查当前访问IP是否拉黑
     */
    @Operation(summary = "检查当前访问IP是否拉黑")
    @org.jeecg.config.shiro.IgnoreAuth
    @GetMapping("/blacklist/ip/check-current")
    public Result<Map<String, Object>> checkCurrentIpBlacklist(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        boolean blocked = visitorTokenService.isIpBlacklisted(clientIp);
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("blacklisted", blocked);
        result.put("ip", clientIp);
        return Result.OK(result);
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (oConvertUtils.isNotEmpty(ip)) {
            int idx = ip.indexOf(',');
            return idx > -1 ? ip.substring(0, idx).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (oConvertUtils.isNotEmpty(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    private static final Pattern IP_PATTERN = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");

    private static class VisitorAccessConfig {
        private final String secretKey;
        private final String whitelist;

        private VisitorAccessConfig(String secretKey, String whitelist) {
            this.secretKey = secretKey;
            this.whitelist = whitelist;
        }
    }

    private static class WhitelistCheckResult {
        private final boolean allowed;
        private final String reason;

        private WhitelistCheckResult(boolean allowed, String reason) {
            this.allowed = allowed;
            this.reason = reason;
        }
    }

    private WhitelistCheckResult validateSourceWhitelist(String rawList, String referer, String clientIp) {
        if (oConvertUtils.isEmpty(rawList)) {
            return new WhitelistCheckResult(true, "");
        }
        String host = extractHost(referer);
        String[] rules = rawList.split(",");
        boolean hasAllow = false;
        boolean allowMatched = false;

        for (String rule : rules) {
            if (rule == null) {
                continue;
            }
            String item = rule.trim();
            if (item.isEmpty()) {
                continue;
            }
            boolean deny = item.startsWith("!");
            if (deny) {
                item = item.substring(1).trim();
            }
            if (item.isEmpty()) {
                continue;
            }
            boolean matched = matchRule(item, host, clientIp);
            if (deny && matched) {
                return new WhitelistCheckResult(false, "来源在黑名单");
            }
            if (!deny) {
                hasAllow = true;
                if (matched) {
                    allowMatched = true;
                }
            }
        }
        if (hasAllow && !allowMatched) {
            return new WhitelistCheckResult(false, "来源不在白名单");
        }
        return new WhitelistCheckResult(true, "");
    }

    private boolean matchRule(String rule, String host, String clientIp) {
        if (oConvertUtils.isEmpty(rule)) {
            return false;
        }
        String normalized = rule.trim().toLowerCase();
        if (IP_PATTERN.matcher(normalized).matches()) {
            return oConvertUtils.isNotEmpty(clientIp) && clientIp.equals(normalized);
        }
        String domain = normalized;
        if (normalized.contains("://")) {
            try {
                URI uri = new URI(normalized);
                if (uri.getHost() != null) {
                    domain = uri.getHost().toLowerCase();
                }
            } catch (Exception e) {
                return false;
            }
        }
        if (oConvertUtils.isEmpty(host)) {
            return false;
        }
        String hostLower = host.toLowerCase();
        if (domain.startsWith("*.")) {
            String suffix = domain.substring(1);
            return hostLower.endsWith(suffix) || hostLower.equals(domain.substring(2));
        }
        return hostLower.equals(domain);
    }

    private String extractHost(String referer) {
        if (oConvertUtils.isEmpty(referer)) {
            return null;
        }
        try {
            URI uri = new URI(referer);
            return uri.getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private VisitorAccessConfig getVisitorAccessConfig() {
        String json = redisTemplate.opsForValue().get(VISITOR_ACCESS_REDIS_KEY);
        if (oConvertUtils.isEmpty(json)) {
            CsGlobalConfig config = csGlobalConfigMapper.selectById(VISITOR_ACCESS_CONFIG_KEY);
            json = config != null ? config.getConfigValue() : null;
            if (oConvertUtils.isNotEmpty(json)) {
                redisTemplate.opsForValue().set(VISITOR_ACCESS_REDIS_KEY, json);
            }
        }
        if (oConvertUtils.isEmpty(json)) {
            return new VisitorAccessConfig("", "");
        }
        try {
            JSONObject obj = JSONObject.parseObject(json);
            String secretKey = obj != null ? obj.getString("secretKey") : "";
            String whitelist = obj != null ? obj.getString("whitelist") : "";
            return new VisitorAccessConfig(secretKey, whitelist);
        } catch (Exception e) {
            log.warn("[CS-Visitor] 解析访客接入配置失败", e);
            return new VisitorAccessConfig("", "");
        }
    }

    /**
     * 更新访客信息（如果不存在则自动创建）
     * 注：新版客服系统不再强制要求appId，可以只通过userId创建/更新访客
     */
    @Operation(summary = "更新访客信息")
    @PostMapping("/update")
    public Result<CsVisitor> update(@RequestBody CsVisitor visitor) {
        // 如果有ID，直接更新
        if (visitor.getId() != null && !visitor.getId().isEmpty()) {
            visitor.setUpdateTime(new Date());
            boolean success = visitorService.updateById(visitor);
            if (success) {
                CsVisitor updated = visitorService.getById(visitor.getId());
                visitorService.notifyVisitorUpdated(updated);
                return Result.OK(updated);
            }
            return Result.error("更新失败");
        }
        
        // 没有ID时，通过userId查找或创建（appId可选）
        if (visitor.getUserId() == null || visitor.getUserId().isEmpty()) {
            return Result.error("userId不能为空");
        }
        
        // 先查找是否已存在
        CsVisitor existing;
        if (visitor.getAppId() != null && !visitor.getAppId().isEmpty()) {
            // 有appId时，精确匹配
            existing = visitorService.getByAppAndUser(visitor.getAppId(), visitor.getUserId());
        } else {
            // 无appId时，只按userId查询
            existing = visitorService.getByUserId(visitor.getUserId());
        }
        
        if (existing != null) {
            // 存在则更新
            visitor.setId(existing.getId());
            visitor.setUpdateTime(new Date());
            visitorService.updateById(visitor);
            CsVisitor updated = visitorService.getById(existing.getId());
            visitorService.notifyVisitorUpdated(updated);
            return Result.OK(updated);
        } else {
            // 不存在则创建
            visitor.setCreateTime(new Date());
            visitor.setUpdateTime(new Date());
            visitor.setVisitCount(1);
            visitor.setConversationCount(1);
            visitor.setFirstVisitTime(new Date());
            visitor.setLastVisitTime(new Date());
            visitorService.save(visitor);
            visitorService.notifyVisitorUpdated(visitor);
            return Result.OK(visitor);
        }
    }

    /**
     * 切换星标
     */
    @Operation(summary = "切换星标")
    @PostMapping("/toggleStar")
    public Result<String> toggleStar(@RequestParam(required = false) String id,
                                     @RequestBody(required = false) Map<String, String> body) {
        String visitorId = id != null ? id : (body != null ? body.get("id") : null);
        if (visitorId == null || visitorId.isEmpty()) {
            return Result.error("id不能为空");
        }
        boolean success = visitorService.toggleStar(visitorId);
        if (success) {
            CsVisitor updated = visitorService.getById(visitorId);
            visitorService.notifyVisitorUpdated(updated);
        }
        return success ? Result.OK("操作成功") : Result.error("操作失败");
    }

    /**
     * 更新客户等级
     */
    @Operation(summary = "更新客户等级")
    @PostMapping("/updateLevel")
    public Result<String> updateLevel(@RequestBody Map<String, Object> params) {
        String id = (String) params.get("id");
        Integer level = params.get("level") instanceof Number ? ((Number) params.get("level")).intValue() : null;
        
        if (id == null || id.isEmpty()) {
            return Result.error("id不能为空");
        }
        if (level == null || level < 1 || level > 3) {
            return Result.error("等级值无效");
        }
        boolean success = visitorService.updateLevel(id, level);
        if (success) {
            CsVisitor updated = visitorService.getById(id);
            visitorService.notifyVisitorUpdated(updated);
        }
        return success ? Result.OK("更新成功") : Result.error("更新失败");
    }

    /**
     * 更新标签
     */
    @Operation(summary = "更新标签")
    @PostMapping("/updateTags")
    public Result<String> updateTags(@RequestBody Map<String, String> params) {
        String id = params.get("id");
        String tags = params.get("tags");
        
        if (id == null || id.isEmpty()) {
            return Result.error("id不能为空");
        }
        boolean success = visitorService.updateTags(id, tags);
        if (success) {
            CsVisitor updated = visitorService.getById(id);
            visitorService.notifyVisitorUpdated(updated);
        }
        return success ? Result.OK("更新成功") : Result.error("更新失败");
    }

    /**
     * 快速备注(只更新备注昵称)
     */
    @Operation(summary = "快速备注")
    @PostMapping("/quickRemark")
    public Result<String> quickRemark(@RequestBody Map<String, String> params) {
        String id = params.get("id");
        String nickname = params.get("nickname");
        
        if (id == null || id.isEmpty()) {
            return Result.error("id不能为空");
        }
        CsVisitor visitor = new CsVisitor();
        visitor.setId(id);
        visitor.setNickname(nickname);
        visitor.setUpdateTime(new Date());
        boolean success = visitorService.updateById(visitor);
        if (success) {
            CsVisitor updated = visitorService.getById(id);
            visitorService.notifyVisitorUpdated(updated);
        }
        return success ? Result.OK("备注成功") : Result.error("备注失败");
    }

    /**
     * 删除访客
     */
    @Operation(summary = "删除访客")
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam String id) {
        boolean success = visitorService.removeById(id);
        return success ? Result.OK("删除成功") : Result.error("删除失败");
    }

    /**
     * 批量删除访客
     */
    @Operation(summary = "批量删除访客")
    @DeleteMapping("/deleteBatch")
    public Result<String> deleteBatch(@RequestParam String ids) {
        String[] idArray = ids.split(",");
        for (String id : idArray) {
            visitorService.removeById(id.trim());
        }
        return Result.OK("删除成功");
    }
}
