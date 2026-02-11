package org.jeecg.modules.airag.cs.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.PasswordUtil;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.cs.entity.CsAgent;
import org.jeecg.modules.airag.cs.entity.CsAgentLoginLog;
import org.jeecg.modules.airag.cs.entity.CsGlobalConfig;
import org.jeecg.modules.airag.cs.mapper.CsAgentLoginLogMapper;
import org.jeecg.modules.airag.cs.mapper.CsGlobalConfigMapper;
import org.jeecg.modules.airag.cs.mapper.CsSubAgentMapper;
import org.jeecg.modules.airag.cs.service.ICsAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 客服管理Controller
 * 
 * @author jeecg
 * @date 2026-01-07
 */
@Slf4j
@Tag(name = "客服管理")
@RestController
@RequestMapping("/cs/agent")
public class CsAgentController extends JeecgController<CsAgent, ICsAgentService> {

    /** 访客AI应用全局配置的Redis Key */
    private static final String VISITOR_APP_REDIS_KEY = "cs:global:visitor_app_id";
    private static final String VISITOR_APP_CONFIG_KEY = "visitor_app_id";
    private static final String VISITOR_ACCESS_REDIS_KEY = "cs:global:visitor_access";
    private static final String VISITOR_ACCESS_CONFIG_KEY = "visitor_access";

    /** AI开关配置 */
    private static final String AI_ENABLED_REDIS_KEY = "cs:global:ai_enabled";
    private static final String AI_ENABLED_CONFIG_KEY = "ai_enabled";

    /** 对话分配配置 */
    private static final String CONVERSATION_ASSIGN_REDIS_KEY = "cs:global:conversation_assign";
    private static final String CONVERSATION_ASSIGN_CONFIG_KEY = "conversation_assign";

    /** 留言板设置配置 */
    private static final String MESSAGE_BOARD_REDIS_KEY = "cs:global:message_board";
    private static final String MESSAGE_BOARD_CONFIG_KEY = "message_board";

    /** 自动消息配置 */
    private static final String AUTO_MESSAGES_REDIS_KEY = "cs:global:auto_messages";
    private static final String AUTO_MESSAGES_CONFIG_KEY = "auto_messages";

    /** 管理员客服角色编码 */
    private static final String ADMIN_AGENT_ROLE_CODE = "cs_admin_agent";
    /** 子客服角色编码 */
    private static final String SUB_AGENT_ROLE_CODE = "cs_sub_agent";

    @Autowired
    private ICsAgentService csAgentService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private CsGlobalConfigMapper csGlobalConfigMapper;

    @Autowired
    private CsSubAgentMapper csSubAgentMapper;

    @Autowired
    private CsAgentLoginLogMapper csAgentLoginLogMapper;

    /**
     * 分页列表查询
     */
    @Operation(summary = "分页列表查询")
    @GetMapping("/list")
    public Result<IPage<CsAgent>> queryPageList(CsAgent csAgent,
                                                 @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                 @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                 HttpServletRequest req) {
        QueryWrapper<CsAgent> queryWrapper = QueryGenerator.initQueryWrapper(csAgent, req.getParameterMap());
        Page<CsAgent> page = new Page<>(pageNo, pageSize);
        IPage<CsAgent> pageList = csAgentService.page(page, queryWrapper);
        // 填充登录账号（从sys_user获取）
        for (CsAgent agent : pageList.getRecords()) {
            if (oConvertUtils.isNotEmpty(agent.getUserId())) {
                String username = csSubAgentMapper.getUsernameByUserId(agent.getUserId());
                if (username != null) {
                    agent.setUsername(username);
                }
            }
        }
        return Result.OK(pageList);
    }

    /**
     * 添加管理员客服
     * 自动创建 sys_user 并分配 cs_admin_agent 角色
     */
    @AutoLog(value = "客服管理-添加")
    @Operation(summary = "添加管理员客服")
    @PostMapping("/add")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> add(@RequestBody Map<String, Object> params) {
        String username = (String) params.get("username");
        String password = (String) params.get("password");
        String nickname = (String) params.get("nickname");
        String avatar = (String) params.get("avatar");
        String welcomeMessage = (String) params.get("welcomeMessage");
        Integer maxSessions = params.get("maxSessions") != null ? Integer.parseInt(params.get("maxSessions").toString()) : 10;

        if (oConvertUtils.isEmpty(username) || oConvertUtils.isEmpty(password)) {
            return Result.error("用户名和密码不能为空");
        }
        if (oConvertUtils.isEmpty(nickname)) {
            return Result.error("客服昵称不能为空");
        }

        // 1. 检查用户名是否已存在
        int existCount = csSubAgentMapper.countByUsername(username);
        if (existCount > 0) {
            return Result.error("用户名已存在: " + username);
        }

        // 2. 创建 sys_user
        String sysUserId = UUID.randomUUID().toString().replace("-", "");
        String salt = oConvertUtils.randomGen(8);
        String passwordEncode = PasswordUtil.encrypt(username, password, salt);
        csSubAgentMapper.insertSysUser(sysUserId, username, nickname, passwordEncode, salt, null, null);

        // 3. 分配管理员客服角色
        String roleId = csSubAgentMapper.getRoleIdByCode(ADMIN_AGENT_ROLE_CODE);
        if (oConvertUtils.isNotEmpty(roleId)) {
            String userRoleId = UUID.randomUUID().toString().replace("-", "");
            csSubAgentMapper.insertSysUserRole(userRoleId, sysUserId, roleId);
        } else {
            log.warn("[CS-Agent] 未找到管理员客服角色: {}", ADMIN_AGENT_ROLE_CODE);
        }

        // 4. 创建 cs_agent
        CsAgent csAgent = new CsAgent();
        csAgent.setUserId(sysUserId);
        csAgent.setNickname(nickname);
        csAgent.setAvatar(avatar);
        csAgent.setMaxSessions(maxSessions);
        csAgent.setWelcomeMessage(welcomeMessage);
        csAgent.setStatus(CsAgent.STATUS_OFFLINE);
        csAgent.setCurrentSessions(0);
        csAgent.setTotalServed(0);
        csAgent.setRole(CsAgent.ROLE_SUPERVISOR); // 管理员客服固定 role=1
        csAgent.setCreateBy(((LoginUser) SecurityUtils.getSubject().getPrincipal()).getUsername());
        csAgent.setCreateTime(new Date());
        csAgentService.save(csAgent);

        return Result.OK("添加成功！");
    }

    /**
     * 编辑客服
     * 支持角色变更联动（子客服提升为管理员时清空 parent_agent_id 和 allowed_menus，并更换 sys_role）
     */
    @AutoLog(value = "客服管理-编辑")
    @Operation(summary = "编辑客服")
    @PutMapping("/edit")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> edit(@RequestBody CsAgent csAgent) {
        if (oConvertUtils.isEmpty(csAgent.getId())) {
            return Result.error("ID不能为空");
        }
        CsAgent existing = csAgentService.getById(csAgent.getId());
        if (existing == null) {
            return Result.error("客服不存在");
        }

        // 检测是否有角色变更
        if (csAgent.getRole() != null && !csAgent.getRole().equals(existing.getRole())) {
            int newRole = csAgent.getRole();
            String userId = existing.getUserId();
            if (oConvertUtils.isNotEmpty(userId)) {
                // 删除旧角色
                csSubAgentMapper.deleteSysUserRoleByUserId(userId);
                // 分配新角色
                String newRoleCode = (newRole == CsAgent.ROLE_SUPERVISOR) ? ADMIN_AGENT_ROLE_CODE : SUB_AGENT_ROLE_CODE;
                String roleId = csSubAgentMapper.getRoleIdByCode(newRoleCode);
                if (oConvertUtils.isNotEmpty(roleId)) {
                    String userRoleId = UUID.randomUUID().toString().replace("-", "");
                    csSubAgentMapper.insertSysUserRole(userRoleId, userId, roleId);
                }
            }

            // 子客服提升为管理员时，清空子客服相关字段
            if (newRole == CsAgent.ROLE_SUPERVISOR) {
                csAgent.setParentAgentId(null);
                csAgent.setAllowedMenus(null);
            }
        }

        // 同步更新 sys_user 的 realname
        if (oConvertUtils.isNotEmpty(csAgent.getNickname()) && oConvertUtils.isNotEmpty(existing.getUserId())) {
            csSubAgentMapper.updateSysUserRealname(existing.getUserId(), csAgent.getNickname());
        }

        csAgent.setUpdateBy(((LoginUser) SecurityUtils.getSubject().getPrincipal()).getUsername());
        csAgent.setUpdateTime(new Date());
        csAgentService.updateById(csAgent);
        return Result.OK("编辑成功!");
    }

    /**
     * 删除客服（同时清理 sys_user_role 和逻辑删除 sys_user）
     * 不允许删除自己
     */
    @AutoLog(value = "客服管理-删除")
    @Operation(summary = "删除客服")
    @DeleteMapping("/delete")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> delete(@RequestParam(name = "id", required = false) String id,
                                 @RequestBody(required = false) java.util.Map<String, Object> body) {
        if (id == null || id.isEmpty()) {
            Object idObj = body != null ? body.get("id") : null;
            id = idObj != null ? String.valueOf(idObj) : null;
        }
        if (id == null || id.isEmpty()) {
            return Result.error("id不能为空");
        }
        CsAgent agent = csAgentService.getById(id);
        if (agent == null) {
            return Result.error("客服不存在");
        }
        // 防止删除自己
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        if (loginUser != null && oConvertUtils.isNotEmpty(agent.getUserId())
                && agent.getUserId().equals(loginUser.getId())) {
            return Result.error("不能删除自己的账号");
        }
        if (oConvertUtils.isNotEmpty(agent.getUserId())) {
            csSubAgentMapper.deleteSysUserRoleByUserId(agent.getUserId());
            csSubAgentMapper.logicDeleteSysUser(agent.getUserId());
        }
        csAgentService.removeById(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     */
    @AutoLog(value = "客服管理-批量删除")
    @Operation(summary = "批量删除")
    @DeleteMapping("/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids") String ids) {
        csAgentService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功!");
    }

    /**
     * 客服上线
     */
    @AutoLog(value = "客服管理-上线")
    @Operation(summary = "客服上线")
    @PostMapping("/online/{id}")
    public Result<String> goOnline(@PathVariable String id) {
        csAgentService.goOnline(id);
        return Result.OK("上线成功!");
    }

    /**
     * 客服下线
     */
    @AutoLog(value = "客服管理-下线")
    @Operation(summary = "客服下线")
    @PostMapping("/offline/{id}")
    public Result<String> goOffline(@PathVariable String id, HttpServletRequest request) {
        // 记录退出日志
        try {
            CsAgent agent = csAgentService.getById(id);
            if (agent != null && oConvertUtils.isNotEmpty(agent.getUserId())) {
                String username = csSubAgentMapper.getUsernameByUserId(agent.getUserId());
                if (oConvertUtils.isNotEmpty(username)) {
                    CsAgentLoginLog logRecord = new CsAgentLoginLog();
                    logRecord.setLoginDate(new Date());
                    logRecord.setUsername(username);
                    logRecord.setEvent(CsAgentLoginLog.EVENT_LOGOUT);
                    logRecord.setIp(getClientIp(request));
                    logRecord.setCreateTime(new Date());
                    csAgentLoginLogMapper.insert(logRecord);
                }
            }
        } catch (Exception e) {
            log.warn("[CS-Security] 记录退出日志失败: {}", e.getMessage());
        }
        csAgentService.goOffline(id);
        return Result.OK("下线成功!");
    }

    /**
     * 设置忙碌
     */
    @AutoLog(value = "客服管理-设置忙碌")
    @Operation(summary = "设置忙碌")
    @PostMapping("/busy/{id}")
    public Result<String> setBusy(@PathVariable String id) {
        csAgentService.setBusy(id);
        return Result.OK("设置成功!");
    }

    /**
     * 获取可用客服列表
     */
    @Operation(summary = "获取可用客服列表")
    @GetMapping("/available")
    public Result<List<CsAgent>> getAvailableAgents() {
        List<CsAgent> agents = csAgentService.getAvailableAgents();
        return Result.OK(agents);
    }

    /**
     * 根据用户ID获取客服信息
     */
    @Operation(summary = "根据用户ID获取客服信息")
    @GetMapping("/byUserId/{userId}")
    public Result<CsAgent> getByUserId(@PathVariable String userId) {
        CsAgent agent = csAgentService.getByUserId(userId);
        return Result.OK(agent);
    }

    /**
     * 根据当前登录用户获取客服信息
     */
    @Operation(summary = "获取当前用户的客服信息")
    @GetMapping("/current")
    public Result<CsAgent> getCurrentAgent() {
        CsAgent agent = csAgentService.getCurrentAgent();
        return Result.OK(agent);
    }

    /**
     * 设置客服AI建议应用（用于AI辅助模式）
     */
    @Operation(summary = "设置客服AI建议应用")
    @PutMapping("/{id}/default-app")
    public Result<String> setDefaultApp(@PathVariable String id, @RequestBody java.util.Map<String, String> params) {
        String appId = params.get("appId");
        CsAgent agent = csAgentService.getById(id);
        if (agent == null) {
            return Result.error("客服不存在");
        }
        agent.setDefaultAppId(appId);
        csAgentService.updateById(agent);
        return Result.OK("设置成功");
    }

    /**
     * 设置访客AI应用（全局配置，用于AI自动回复模式）
     * 注意：这是全局设置，任何客服修改都会影响所有会话
     */
    @Operation(summary = "设置访客AI应用（全局）")
    @PutMapping("/global/visitor-app")
    public Result<String> setGlobalVisitorApp(@RequestBody java.util.Map<String, String> params) {
        String appId = params.get("appId");
        if (appId == null || appId.isEmpty()) {
            csGlobalConfigMapper.deleteById(VISITOR_APP_CONFIG_KEY);
            redisTemplate.delete(VISITOR_APP_REDIS_KEY);
        } else {
            saveGlobalConfigValue(VISITOR_APP_CONFIG_KEY, appId);
            redisTemplate.opsForValue().set(VISITOR_APP_REDIS_KEY, appId);
        }
        log.info("[CS-Agent] 全局访客AI应用已更新: appId={}", appId);
        return Result.OK("设置成功");
    }

    /**
     * 获取访客AI应用（全局配置）
     */
    @Operation(summary = "获取访客AI应用（全局）")
    @org.jeecg.config.shiro.IgnoreAuth
    @GetMapping("/global/visitor-app")
    public Result<java.util.Map<String, String>> getGlobalVisitorApp() {
        String appId = redisTemplate.opsForValue().get(VISITOR_APP_REDIS_KEY);
        if (appId == null || appId.isEmpty()) {
            appId = getGlobalConfigValue(VISITOR_APP_CONFIG_KEY);
            if (appId != null && !appId.isEmpty()) {
                redisTemplate.opsForValue().set(VISITOR_APP_REDIS_KEY, appId);
            }
        }
        java.util.Map<String, String> result = new java.util.HashMap<>();
        result.put("appId", appId);
        return Result.OK(result);
    }

    /**
     * 获取访客接入配置（全局）
     */
    @Operation(summary = "获取访客接入配置（全局）")
    @org.jeecg.config.shiro.IgnoreAuth
    @GetMapping("/global/visitor-access")
    public Result<java.util.Map<String, String>> getGlobalVisitorAccess() {
        String json = redisTemplate.opsForValue().get(VISITOR_ACCESS_REDIS_KEY);
        if (json == null || json.isEmpty()) {
            json = getGlobalConfigValue(VISITOR_ACCESS_CONFIG_KEY);
            if (json != null && !json.isEmpty()) {
                redisTemplate.opsForValue().set(VISITOR_ACCESS_REDIS_KEY, json);
            }
        }
        java.util.Map<String, String> result = new java.util.HashMap<>();
        if (json != null && !json.isEmpty()) {
            try {
                JSONObject obj = JSONObject.parseObject(json);
                result.put("secretKey", obj.getString("secretKey"));
                result.put("whitelist", obj.getString("whitelist"));
                // tokenRequired 默认 true（兼容旧数据）
                result.put("tokenRequired", obj.containsKey("tokenRequired") ? obj.getString("tokenRequired") : "true");
            } catch (Exception e) {
                log.warn("[CS-Agent] 解析访客接入配置失败", e);
            }
        } else {
            result.put("tokenRequired", "true");
        }
        return Result.OK(result);
    }

    /**
     * 设置访客接入配置（全局）
     */
    @Operation(summary = "设置访客接入配置（全局）")
    @PutMapping("/global/visitor-access")
    public Result<String> setGlobalVisitorAccess(@RequestBody java.util.Map<String, String> params) {
        String secretKey = params.getOrDefault("secretKey", "");
        String whitelist = params.getOrDefault("whitelist", "");
        String tokenRequired = params.getOrDefault("tokenRequired", "true");
        JSONObject obj = new JSONObject();
        obj.put("secretKey", secretKey);
        obj.put("whitelist", whitelist);
        obj.put("tokenRequired", "true".equals(tokenRequired));
        String json = obj.toJSONString();
        saveGlobalConfigValue(VISITOR_ACCESS_CONFIG_KEY, json);
        redisTemplate.opsForValue().set(VISITOR_ACCESS_REDIS_KEY, json);
        log.info("[CS-Agent] 全局访客接入配置已更新, tokenRequired={}", tokenRequired);
        return Result.OK("设置成功");
    }

    /**
     * 生成访客接入密钥（全局）
     */
    @Operation(summary = "生成访客接入密钥（全局）")
    @GetMapping("/global/visitor-access/generate-key")
    public Result<String> generateVisitorAccessKey() {
        String key = UUID.randomUUID().toString().replace("-", "");
        return Result.OK(key);
    }

    // ==================== 聊天窗口设置 ====================

    private static final String CHAT_WINDOW_REDIS_KEY = "cs:global:chat_window_settings";
    private static final String CHAT_WINDOW_CONFIG_KEY = "chat_window_settings";
    private static final String SENSITIVE_WORDS_REDIS_KEY = "cs:global:sensitive_words";
    private static final String SENSITIVE_WORDS_CONFIG_KEY = "sensitive_words";

    /**
     * 获取聊天窗口设置（全局，访客端也需调用）
     */
    @Operation(summary = "获取聊天窗口设置（全局）")
    @org.jeecg.config.shiro.IgnoreAuth
    @GetMapping("/global/chat-window-settings")
    public Result<String> getChatWindowSettings() {
        String json = redisTemplate.opsForValue().get(CHAT_WINDOW_REDIS_KEY);
        if (json == null || json.isEmpty()) {
            json = getGlobalConfigValue(CHAT_WINDOW_CONFIG_KEY);
            if (json != null && !json.isEmpty()) {
                redisTemplate.opsForValue().set(CHAT_WINDOW_REDIS_KEY, json);
            }
        }
        return Result.OK(json != null ? json : "{}");
    }

    /**
     * 保存聊天窗口设置（全局）
     */
    @Operation(summary = "保存聊天窗口设置（全局）")
    @PutMapping("/global/chat-window-settings")
    public Result<String> saveChatWindowSettings(@RequestBody String body) {
        saveGlobalConfigValue(CHAT_WINDOW_CONFIG_KEY, body);
        redisTemplate.opsForValue().set(CHAT_WINDOW_REDIS_KEY, body);
        log.info("[CS-Agent] 聊天窗口设置已更新");
        return Result.OK("保存成功");
    }

    /**
     * 获取敏感词配置（全局，访客端也需调用来做前端校验）
     */
    @Operation(summary = "获取敏感词配置（全局）")
    @org.jeecg.config.shiro.IgnoreAuth
    @GetMapping("/global/sensitive-words")
    public Result<String> getSensitiveWords() {
        String json = redisTemplate.opsForValue().get(SENSITIVE_WORDS_REDIS_KEY);
        if (json == null || json.isEmpty()) {
            json = getGlobalConfigValue(SENSITIVE_WORDS_CONFIG_KEY);
            if (json != null && !json.isEmpty()) {
                redisTemplate.opsForValue().set(SENSITIVE_WORDS_REDIS_KEY, json);
            }
        }
        return Result.OK(json != null ? json : "{\"enabled\":false,\"words\":[]}");
    }

    /**
     * 保存敏感词配置（全局）
     */
    @Operation(summary = "保存敏感词配置（全局）")
    @PutMapping("/global/sensitive-words")
    public Result<String> saveSensitiveWords(@RequestBody String body) {
        saveGlobalConfigValue(SENSITIVE_WORDS_CONFIG_KEY, body);
        redisTemplate.opsForValue().set(SENSITIVE_WORDS_REDIS_KEY, body);
        log.info("[CS-Agent] 敏感词配置已更新");
        return Result.OK("保存成功");
    }

    // ==================== AI开关 ====================

    /**
     * 获取AI开关状态（全局）
     */
    @Operation(summary = "获取AI开关状态（全局）")
    @org.jeecg.config.shiro.IgnoreAuth
    @GetMapping("/global/ai-enabled")
    public Result<java.util.Map<String, Object>> getAiEnabled() {
        String value = redisTemplate.opsForValue().get(AI_ENABLED_REDIS_KEY);
        if (value == null) {
            value = getGlobalConfigValue(AI_ENABLED_CONFIG_KEY);
            if (value != null) {
                redisTemplate.opsForValue().set(AI_ENABLED_REDIS_KEY, value);
            }
        }
        boolean enabled = value == null || "true".equalsIgnoreCase(value);
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("enabled", enabled);
        return Result.OK(result);
    }

    /**
     * 设置AI开关（全局）
     */
    @Operation(summary = "设置AI开关（全局）")
    @PutMapping("/global/ai-enabled")
    public Result<String> setAiEnabled(@RequestBody java.util.Map<String, Object> params) {
        Boolean enabled = (Boolean) params.get("enabled");
        if (enabled == null) {
            enabled = true;
        }
        String value = enabled.toString();
        saveGlobalConfigValue(AI_ENABLED_CONFIG_KEY, value);
        redisTemplate.opsForValue().set(AI_ENABLED_REDIS_KEY, value);
        log.info("[CS-Agent] AI开关已更新: enabled={}", enabled);
        return Result.OK("设置成功");
    }

    // ==================== 对话分配配置 ====================

    /**
     * 获取对话分配配置（全局）
     */
    @Operation(summary = "获取对话分配配置（全局）")
    @GetMapping("/global/conversation-assign")
    public Result<JSONObject> getConversationAssignConfig() {
        String json = redisTemplate.opsForValue().get(CONVERSATION_ASSIGN_REDIS_KEY);
        if (json == null || json.isEmpty()) {
            json = getGlobalConfigValue(CONVERSATION_ASSIGN_CONFIG_KEY);
            if (json != null && !json.isEmpty()) {
                redisTemplate.opsForValue().set(CONVERSATION_ASSIGN_REDIS_KEY, json);
            }
        }
        JSONObject result;
        if (json != null && !json.isEmpty()) {
            try {
                result = JSONObject.parseObject(json);
            } catch (Exception e) {
                log.warn("[CS-Agent] 解析对话分配配置失败", e);
                result = getDefaultConversationAssignConfig();
            }
        } else {
            result = getDefaultConversationAssignConfig();
        }
        return Result.OK(result);
    }

    /**
     * 保存对话分配配置（全局）
     */
    @Operation(summary = "保存对话分配配置（全局）")
    @PutMapping("/global/conversation-assign")
    public Result<String> setConversationAssignConfig(@RequestBody JSONObject config) {
        String json = config.toJSONString();
        saveGlobalConfigValue(CONVERSATION_ASSIGN_CONFIG_KEY, json);
        redisTemplate.opsForValue().set(CONVERSATION_ASSIGN_REDIS_KEY, json);
        log.info("[CS-Agent] 对话分配配置已更新");
        return Result.OK("设置成功");
    }

    private JSONObject getDefaultConversationAssignConfig() {
        JSONObject config = new JSONObject();
        config.put("assignMode", "saturation");
        JSONObject inherit = new JSONObject();
        inherit.put("enabled", true);
        inherit.put("expireMinutes", 60);
        config.put("inheritLastAgent", inherit);
        JSONObject hold = new JSONObject();
        hold.put("minutes", 10);
        config.put("conversationHold", hold);
        JSONObject timeout = new JSONObject();
        timeout.put("enabled", false);
        timeout.put("seconds", 20);
        config.put("agentTimeoutReminder", timeout);
        return config;
    }

    // ==================== 留言板设置 ====================

    /**
     * 获取留言板设置（全局）
     */
    @Operation(summary = "获取留言板设置（全局）")
    @org.jeecg.config.shiro.IgnoreAuth
    @GetMapping("/global/message-board")
    public Result<JSONObject> getMessageBoardConfig() {
        String json = redisTemplate.opsForValue().get(MESSAGE_BOARD_REDIS_KEY);
        if (json == null || json.isEmpty()) {
            json = getGlobalConfigValue(MESSAGE_BOARD_CONFIG_KEY);
            if (json != null && !json.isEmpty()) {
                redisTemplate.opsForValue().set(MESSAGE_BOARD_REDIS_KEY, json);
            }
        }
        JSONObject result;
        if (json != null && !json.isEmpty()) {
            try {
                result = JSONObject.parseObject(json);
            } catch (Exception e) {
                log.warn("[CS-Agent] 解析留言板设置失败", e);
                result = getDefaultMessageBoardConfig();
            }
        } else {
            result = getDefaultMessageBoardConfig();
        }
        return Result.OK(result);
    }

    /**
     * 保存留言板设置（全局）
     */
    @Operation(summary = "保存留言板设置（全局）")
    @PutMapping("/global/message-board")
    public Result<String> setMessageBoardConfig(@RequestBody JSONObject config) {
        String json = config.toJSONString();
        saveGlobalConfigValue(MESSAGE_BOARD_CONFIG_KEY, json);
        redisTemplate.opsForValue().set(MESSAGE_BOARD_REDIS_KEY, json);
        log.info("[CS-Agent] 留言板设置已更新");
        return Result.OK("设置成功");
    }

    /**
     * 获取自动消息配置（全局）
     */
    @Operation(summary = "获取自动消息配置（全局）")
    @GetMapping("/global/auto-messages")
    public Result<JSONObject> getAutoMessagesConfig() {
        String json = redisTemplate.opsForValue().get(AUTO_MESSAGES_REDIS_KEY);
        if (json == null || json.isEmpty()) {
            json = getGlobalConfigValue(AUTO_MESSAGES_CONFIG_KEY);
            if (json != null && !json.isEmpty()) {
                redisTemplate.opsForValue().set(AUTO_MESSAGES_REDIS_KEY, json);
            }
        }
        JSONObject result;
        if (json != null && !json.isEmpty()) {
            try {
                result = JSONObject.parseObject(json);
            } catch (Exception e) {
                result = getDefaultAutoMessagesConfig();
            }
        } else {
            result = getDefaultAutoMessagesConfig();
        }
        return Result.OK(result);
    }

    /**
     * 保存自动消息配置（全局）
     */
    @Operation(summary = "保存自动消息配置（全局）")
    @PutMapping("/global/auto-messages")
    public Result<String> setAutoMessagesConfig(@RequestBody JSONObject config) {
        String json = config.toJSONString();
        saveGlobalConfigValue(AUTO_MESSAGES_CONFIG_KEY, json);
        redisTemplate.opsForValue().set(AUTO_MESSAGES_REDIS_KEY, json);
        log.info("[CS-Agent] 自动消息配置已更新");
        return Result.OK("设置成功");
    }

    private JSONObject getDefaultAutoMessagesConfig() {
        JSONObject config = new JSONObject();
        config.put("defaultLang", "zh-CN");
        JSONObject languages = new JSONObject();

        JSONObject zhCN = new JSONObject();
        zhCN.put("label", "中文简体");
        zhCN.put("messages", new com.alibaba.fastjson.JSONArray());
        languages.put("zh-CN", zhCN);

        JSONObject zhTW = new JSONObject();
        zhTW.put("label", "中文繁體");
        zhTW.put("messages", new com.alibaba.fastjson.JSONArray());
        languages.put("zh-TW", zhTW);

        JSONObject en = new JSONObject();
        en.put("label", "English");
        en.put("messages", new com.alibaba.fastjson.JSONArray());
        languages.put("en", en);

        config.put("languages", languages);
        return config;
    }

    private JSONObject getDefaultMessageBoardConfig() {
        JSONObject config = new JSONObject();
        config.put("subtitle", "客服不在线，请留言");
        JSONObject fields = new JSONObject();
        String[] fieldNames = {"name", "phone", "email", "qq", "wechat", "image"};
        for (String name : fieldNames) {
            JSONObject field = new JSONObject();
            field.put("show", "name".equals(name));
            field.put("required", "name".equals(name));
            fields.put(name, field);
        }
        config.put("fields", fields);
        return config;
    }

    private String getGlobalConfigValue(String configKey) {
        CsGlobalConfig config = csGlobalConfigMapper.selectById(configKey);
        return config != null ? config.getConfigValue() : null;
    }

    /**
     * 获取当前登录用户（子客服）可见菜单ID列表
     * 前端 permissionGuard 调用此接口来过滤菜单
     */
    @Operation(summary = "获取当前客服可见菜单")
    @GetMapping("/current-menus")
    public Result<JSONObject> getCurrentMenus() {
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        if (loginUser == null) {
            return Result.error("未登录");
        }
        CsAgent agent = csAgentService.getByUserId(loginUser.getId());
        JSONObject data = new JSONObject();
        if (agent == null) {
            // 非客服用户，不做菜单限制
            data.put("isSubAgent", false);
            data.put("allowedMenus", new JSONArray());
            return Result.OK(data);
        }
        if (oConvertUtils.isNotEmpty(agent.getParentAgentId())) {
            // 是子客服，返回允许的菜单列表
            data.put("isSubAgent", true);
            if (oConvertUtils.isNotEmpty(agent.getAllowedMenus())) {
                data.put("allowedMenus", JSON.parseArray(agent.getAllowedMenus()));
            } else {
                data.put("allowedMenus", new JSONArray());
            }
        } else {
            data.put("isSubAgent", false);
            data.put("allowedMenus", new JSONArray());
        }
        return Result.OK(data);
    }

    private void saveGlobalConfigValue(String configKey, String configValue) {
        CsGlobalConfig existing = csGlobalConfigMapper.selectById(configKey);
        Date now = new Date();
        if (existing == null) {
            CsGlobalConfig config = new CsGlobalConfig();
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            config.setCreateTime(now);
            config.setUpdateTime(now);
            csGlobalConfigMapper.insert(config);
        } else {
            existing.setConfigValue(configValue);
            existing.setUpdateTime(now);
            csGlobalConfigMapper.updateById(existing);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
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

}
