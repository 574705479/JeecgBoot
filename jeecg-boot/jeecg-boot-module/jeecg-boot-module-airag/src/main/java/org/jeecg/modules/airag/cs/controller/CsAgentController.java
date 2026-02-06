package org.jeecg.modules.airag.cs.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.airag.cs.entity.CsAgent;
import org.jeecg.modules.airag.cs.entity.CsGlobalConfig;
import org.jeecg.modules.airag.cs.mapper.CsGlobalConfigMapper;
import org.jeecg.modules.airag.cs.service.ICsAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
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

    @Autowired
    private ICsAgentService csAgentService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private CsGlobalConfigMapper csGlobalConfigMapper;

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
        return Result.OK(pageList);
    }

    /**
     * 添加
     */
    @AutoLog(value = "客服管理-添加")
    @Operation(summary = "添加")
    @PostMapping("/add")
    public Result<String> add(@RequestBody CsAgent csAgent) {
        csAgentService.save(csAgent);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     */
    @AutoLog(value = "客服管理-编辑")
    @Operation(summary = "编辑")
    @PutMapping("/edit")
    public Result<String> edit(@RequestBody CsAgent csAgent) {
        csAgentService.updateById(csAgent);
        return Result.OK("编辑成功!");
    }

    /**
     * 删除
     */
    @AutoLog(value = "客服管理-删除")
    @Operation(summary = "删除")
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam(name = "id", required = false) String id,
                                 @RequestBody(required = false) java.util.Map<String, Object> body) {
        if (id == null || id.isEmpty()) {
            Object idObj = body != null ? body.get("id") : null;
            id = idObj != null ? String.valueOf(idObj) : null;
        }
        if (id == null || id.isEmpty()) {
            return Result.error("id不能为空");
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
    public Result<String> goOffline(@PathVariable String id) {
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
            } catch (Exception e) {
                log.warn("[CS-Agent] 解析访客接入配置失败", e);
            }
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
        JSONObject obj = new JSONObject();
        obj.put("secretKey", secretKey);
        obj.put("whitelist", whitelist);
        String json = obj.toJSONString();
        saveGlobalConfigValue(VISITOR_ACCESS_CONFIG_KEY, json);
        redisTemplate.opsForValue().set(VISITOR_ACCESS_REDIS_KEY, json);
        log.info("[CS-Agent] 全局访客接入配置已更新");
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

}
