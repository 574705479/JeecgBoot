package org.jeecg.modules.airag.cs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.cs.constant.CsRedisKeys;
import org.jeecg.modules.airag.cs.entity.CsConversation;
import org.jeecg.modules.airag.cs.entity.CsMessage;
import org.jeecg.modules.airag.cs.service.ICsConversationService;
import org.jeecg.modules.airag.cs.service.ICsMessageService;
import org.jeecg.modules.airag.cs.service.ICsVisitorTokenService;
import org.jeecg.modules.airag.cs.util.CsCryptoUtil;
import org.jeecg.modules.airag.cs.vo.CsVisitorTokenPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.airag.cs.entity.CsAgent;
import org.jeecg.modules.airag.cs.service.ICsAgentService;
import org.jeecg.common.util.CommonUtils;
import org.jeecg.common.util.storage.IStorageUploadService;
import org.jeecg.modules.airag.cs.entity.CsFileHash;
import org.jeecg.modules.airag.cs.service.ICsFileHashService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;


/**
 * 消息管理 (重构版)
 * 
 * @author jeecg
 * @date 2026-01-12
 */
@Slf4j
@Tag(name = "客服消息管理")
@RestController
@RequestMapping("/cs/message")
public class CsMessageController {

    @Autowired
    private ICsMessageService messageService;

    @Autowired
    private ICsConversationService conversationService;

    @Autowired
    private ICsVisitorTokenService visitorTokenService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ICsFileHashService fileHashService;

    @Autowired
    private ICsAgentService agentService;

    @Autowired
    private CsCryptoUtil csCryptoUtil;

    @Value(value = "${jeecg.path.upload}")
    private String uploadpath;

    @Autowired
    private IStorageUploadService storageUploadService;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg",
            "mp4", "webm", "ogg", "mov", "avi", "mkv", "flv", "3gp", "wmv",
            "pdf"
    );

    /** 默认文件大小限制 10MB，可在聊天窗口设置中配置（最大50MB） */
    private static final long DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final long ABSOLUTE_MAX_FILE_SIZE = 50 * 1024 * 1024;

    /**
     * 获取配置的最大文件大小（MB → bytes）
     */
    private long getConfiguredMaxFileSize() {
        try {
            String json = redisTemplate.opsForValue().get(CsRedisKeys.REDIS_CHAT_WINDOW);
            if (json != null && !json.isEmpty()) {
                JSONObject config = JSON.parseObject(json);
                Integer maxMb = config.getInteger("maxFileSize");
                if (maxMb != null && maxMb > 0) {
                    long maxBytes = (long) maxMb * 1024 * 1024;
                    return Math.min(maxBytes, ABSOLUTE_MAX_FILE_SIZE);
                }
            }
        } catch (Exception e) {
            log.warn("[CS-Message] 读取聊天窗口文件大小配置失败", e);
        }
        return DEFAULT_MAX_FILE_SIZE;
    }

    /**
     * 访客秒传哈希检测接口
     */
    @Operation(summary = "访客文件秒传检测")
    @org.jeecg.config.shiro.IgnoreAuth
    @PostMapping("/visitor/checkHash")
    public Result<?> visitorCheckHash(@RequestParam String md5, @RequestParam Long fileSize, HttpServletRequest request) {
        boolean isAdmin = visitorTokenService.isAdminRequest(request);
        if (!isAdmin) {
            CsVisitorTokenPayload tokenPayload = resolveVisitorPayload(request);
            if (tokenPayload == null) {
                if (visitorTokenService.isTokenRequired()) {
                    return Result.error("访客凭证无效或已过期");
                }
                if (!visitorTokenService.validateAppKey(request)) {
                    return Result.error("接入密钥无效");
                }
                String devId = visitorTokenService.extractDeviceId(request);
                if (oConvertUtils.isEmpty(devId)) {
                    return Result.error("缺少设备码");
                }
                if (visitorTokenService.isBlacklisted(devId)) {
                    return Result.error("访客已被拉黑");
                }
            } else {
                if (visitorTokenService.isBlacklisted(tokenPayload.getExternalUserId())) {
                    return Result.error("访客已被拉黑");
                }
            }
        }

        CsFileHash record = fileHashService.findByMd5AndSize(md5, fileSize);
        Map<String, Object> data = new HashMap<>();
        if (record != null) {
            boolean fileExists = verifyFileExists(record.getFilePath());
            if (fileExists) {
                data.put("exists", true);
                data.put("url", record.getFilePath());
                return Result.OK(data);
            } else {
                fileHashService.removeById(record.getId());
            }
        }
        data.put("exists", false);
        return Result.OK(data);
    }

    /**
     * 访客文件上传接口
     */
    @Operation(summary = "访客文件上传")
    @org.jeecg.config.shiro.IgnoreAuth
    @PostMapping("/visitor/upload")
    public Result<?> visitorUpload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        // 校验访客身份
        boolean isAdmin = visitorTokenService.isAdminRequest(request);
        if (!isAdmin) {
            CsVisitorTokenPayload tokenPayload = resolveVisitorPayload(request);
            if (tokenPayload == null) {
                if (visitorTokenService.isTokenRequired()) {
                    return Result.error("访客凭证无效或已过期");
                }
                if (!visitorTokenService.validateAppKey(request)) {
                    return Result.error("接入密钥无效");
                }
                String devId = visitorTokenService.extractDeviceId(request);
                if (oConvertUtils.isEmpty(devId)) {
                    return Result.error("缺少设备码");
                }
                if (visitorTokenService.isBlacklisted(devId)) {
                    return Result.error("访客已被拉黑");
                }
            } else {
                if (visitorTokenService.isBlacklisted(tokenPayload.getExternalUserId())) {
                    return Result.error("访客已被拉黑");
                }
            }
        }

        try {
            if (file == null || file.isEmpty()) {
                return Result.error("请选择文件");
            }
            long maxFileSize = getConfiguredMaxFileSize();
            if (file.getSize() > maxFileSize) {
                long maxMb = maxFileSize / (1024 * 1024);
                return Result.error("文件大小不能超过" + maxMb + "MB");
            }
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return Result.error("文件名无效");
            }
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                return Result.error("不支持的文件类型，仅支持图片/视频/PDF");
            }

            String bizPath = "cs-visitor";
            String md5 = request.getParameter("md5");
            String savePath = storageUploadService.upload(file, bizPath);

            try {
                if (oConvertUtils.isEmpty(md5)) {
                    md5 = CommonUtils.computeMd5(file);
                }
                fileHashService.saveFileHashIgnoreDuplicate(md5, savePath, file.getSize(), file.getOriginalFilename(), bizPath);
            } catch (Exception e) {
                log.warn("保存文件哈希失败，不影响上传: {}", e.getMessage());
            }

            Result<String> result = new Result<>();
            result.setMessage(savePath);
            result.setSuccess(true);
            return result;
        } catch (Exception e) {
            log.error("[CS-Message] 访客文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    private boolean verifyFileExists(String filePath) {
        if (storageUploadService.isEffectiveLocal()) {
            return new File(uploadpath + File.separator + filePath).exists();
        }
        return true;
    }

    /**
     * 发送消息 (通用)
     */
    @Operation(summary = "发送消息")
    @org.jeecg.config.shiro.IgnoreAuth
    @PostMapping("/send")
    public Result<CsMessage> send(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        String conversationId = (String) params.get("conversationId");
        String content = csCryptoUtil.decryptTransport((String) params.get("content"));
        String senderId = (String) params.get("senderId");
        String senderName = (String) params.get("senderName");
        Integer msgType = params.get("msgType") instanceof Number ? ((Number) params.get("msgType")).intValue() : null;
        String extra = params.get("extra") != null ? String.valueOf(params.get("extra")) : null;
        
        // 兼容处理 senderType，可能是字符串或数字
        Object senderTypeObj = params.get("senderType");
        String senderType;
        if (senderTypeObj instanceof Integer) {
            // 1=用户, 2=客服
            senderType = ((Integer) senderTypeObj) == 1 ? "user" : "agent";
        } else if (senderTypeObj instanceof String) {
            String typeStr = (String) senderTypeObj;
            // 支持 "1", "user" 等格式
            senderType = "1".equals(typeStr) || "user".equals(typeStr) ? "user" : "agent";
        } else {
            senderType = "user"; // 默认用户
        }
        
        boolean isAdmin = visitorTokenService.isAdminRequest(request);
        if (!isAdmin) {
            CsVisitorTokenPayload tokenPayload = resolveVisitorPayload(request);
            if (tokenPayload != null) {
                if (visitorTokenService.isBlacklisted(tokenPayload.getExternalUserId())) {
                    return Result.error("访客已被拉黑");
                }
                if ("agent".equals(senderType)) {
                    return Result.error("访客无权限发送客服消息");
                }
                senderId = tokenPayload.getExternalUserId();
                if (oConvertUtils.isEmpty(senderName)) {
                    senderName = tokenPayload.getUserName();
                }
            } else if (!visitorTokenService.isTokenRequired()) {
                // 免Token模式：先校验接入密钥
                if (!visitorTokenService.validateAppKey(request)) {
                    return Result.error("接入密钥无效");
                }
                String devId = visitorTokenService.extractDeviceId(request);
                if (oConvertUtils.isEmpty(devId)) {
                    return Result.error("缺少设备码");
                }
                if (visitorTokenService.isBlacklisted(devId)) {
                    return Result.error("访客已被拉黑");
                }
                if ("agent".equals(senderType)) {
                    return Result.error("访客无权限发送客服消息");
                }
                senderId = devId;
            } else {
                return Result.error("访客凭证无效或已过期");
            }
            if (oConvertUtils.isNotEmpty(conversationId) && !isConversationOwner(conversationId, senderId)) {
                return Result.error("无权访问该会话");
            }
        }

        // 访客消息敏感词校验
        if ("user".equals(senderType) && oConvertUtils.isNotEmpty(content)) {
            String hitWord = checkSensitiveWords(content);
            if (hitWord != null) {
                return Result.error("消息包含敏感内容，请修改后重试");
            }
        }

        CsMessage message;
        if ("user".equals(senderType)) {
            message = messageService.sendUserMessage(conversationId, senderId, senderName, content, msgType, extra);
        } else {
            message = messageService.sendAgentMessage(conversationId, senderId, senderName, content, msgType, extra);
        }
        
        return Result.OK(encryptMessageForResponse(message));
    }

    /**
     * FAQ自动回复接口（访客触发，以系统客服身份回复预设答案）
     * 同时发送访客问题（不触发AI回复）+ 预设答案
     */
    @Operation(summary = "FAQ自动回复")
    @org.jeecg.config.shiro.IgnoreAuth
    @PostMapping("/faq/answer")
    public Result<CsMessage> faqAnswer(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        String conversationId = (String) params.get("conversationId");
        String question = csCryptoUtil.decryptTransport((String) params.get("question"));
        String answer = csCryptoUtil.decryptTransport((String) params.get("answer"));

        if (oConvertUtils.isEmpty(conversationId) || oConvertUtils.isEmpty(question)) {
            return Result.error("参数不完整");
        }

        // 校验访客身份
        boolean isAdmin = visitorTokenService.isAdminRequest(request);
        if (!isAdmin) {
            CsVisitorTokenPayload tokenPayload = resolveVisitorPayload(request);
            if (tokenPayload != null) {
                if (visitorTokenService.isBlacklisted(tokenPayload.getExternalUserId())) {
                    return Result.error("访客已被拉黑");
                }
                if (!isConversationOwner(conversationId, tokenPayload.getExternalUserId())) {
                    return Result.error("无权访问该会话");
                }
            } else if (!visitorTokenService.isTokenRequired()) {
                if (!visitorTokenService.validateAppKey(request)) {
                    return Result.error("接入密钥无效");
                }
                String devId = visitorTokenService.extractDeviceId(request);
                if (oConvertUtils.isEmpty(devId)) {
                    return Result.error("缺少设备码");
                }
                if (visitorTokenService.isBlacklisted(devId)) {
                    return Result.error("访客已被拉黑");
                }
                if (!isConversationOwner(conversationId, devId)) {
                    return Result.error("无权访问该会话");
                }
            } else {
                return Result.error("访客凭证无效或已过期");
            }
        }

        // 校验答案是否为已配置的FAQ答案（防伪造）
        try {
            String settingsJson = redisTemplate.opsForValue().get(CsRedisKeys.REDIS_CHAT_WINDOW);
            if (oConvertUtils.isNotEmpty(settingsJson)) {
                JSONObject settings = JSON.parseObject(settingsJson);
                Boolean faqEnabled = settings.getBoolean("faqEnabled");
                if (faqEnabled == null || !faqEnabled) {
                    return Result.error("常见问题功能未启用");
                }
                JSONArray faqList = settings.getJSONArray("faqList");
                boolean found = false;
                String storedAnswer = null;
                if (faqList != null) {
                    for (int i = 0; i < faqList.size(); i++) {
                        JSONObject faq = faqList.getJSONObject(i);
                        if (faq == null) continue;
                        if (question.equals(faq.getString("question"))) {
                            found = true;
                            storedAnswer = faq.getString("answer");
                            break;
                        }
                        JSONArray children = faq.getJSONArray("children");
                        if (children != null) {
                            for (int j = 0; j < children.size(); j++) {
                                JSONObject child = children.getJSONObject(j);
                                if (child != null && question.equals(child.getString("question"))) {
                                    found = true;
                                    storedAnswer = child.getString("answer");
                                    break;
                                }
                            }
                        }
                        if (found) break;
                    }
                }
                if (!found || oConvertUtils.isEmpty(storedAnswer)) {
                    return Result.error("无效的FAQ问题");
                }
                answer = storedAnswer;
            } else {
                return Result.error("常见问题功能未配置");
            }
        } catch (Exception e) {
            log.error("[CS-Message] FAQ答案校验失败", e);
            return Result.error("FAQ校验失败");
        }

        try {
            // 1. 先发送访客问题（不触发AI回复），从会话中获取访客信息
            if (oConvertUtils.isNotEmpty(question)) {
                CsConversation conversation = conversationService.getConversation(conversationId);
                if (conversation != null) {
                    String userId = conversation.getUserId();
                    String userName = conversation.getUserName();
                    messageService.sendUserMessageRaw(conversationId, userId, userName, question);
                }
            }

            // 2. 以智能助手身份发送预设答案
            CsMessage message = messageService.sendAgentMessage(
                    conversationId, "faq_system", "智能助手", answer, 0, null);
            return Result.OK(encryptMessageForResponse(message));
        } catch (Exception e) {
            log.error("[CS-Message] FAQ回复发送失败", e);
            return Result.error("FAQ回复发送失败: " + e.getMessage());
        }
    }

    /**
     * FAQ交互端点（智能助手消息模式）
     * 访客点击FAQ链接/返回操作时调用，后端生成智能助手消息并通过WebSocket推送
     */
    @Operation(summary = "FAQ交互")
    @org.jeecg.config.shiro.IgnoreAuth
    @PostMapping("/faq/interact")
    public Result<?> faqInteract(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        String conversationId = (String) params.get("conversationId");
        String action = (String) params.get("action");
        Integer faqIndex = params.get("faqIndex") instanceof Number ? ((Number) params.get("faqIndex")).intValue() : null;
        java.util.List<Integer> parentPath = new java.util.ArrayList<>();
        Object pathObj = params.get("parentPath");
        if (pathObj instanceof java.util.List) {
            for (Object item : (java.util.List<?>) pathObj) {
                if (item instanceof Number) {
                    parentPath.add(((Number) item).intValue());
                }
            }
        }

        if (oConvertUtils.isEmpty(conversationId) || oConvertUtils.isEmpty(action)) {
            return Result.error("参数不完整");
        }

        boolean isAdmin = visitorTokenService.isAdminRequest(request);
        if (!isAdmin) {
            CsVisitorTokenPayload tokenPayload = resolveVisitorPayload(request);
            if (tokenPayload != null) {
                if (visitorTokenService.isBlacklisted(tokenPayload.getExternalUserId())) {
                    return Result.error("访客已被拉黑");
                }
                if (!isConversationOwner(conversationId, tokenPayload.getExternalUserId())) {
                    return Result.error("无权访问该会话");
                }
            } else if (!visitorTokenService.isTokenRequired()) {
                if (!visitorTokenService.validateAppKey(request)) {
                    return Result.error("接入密钥无效");
                }
                String devId = visitorTokenService.extractDeviceId(request);
                if (oConvertUtils.isEmpty(devId)) {
                    return Result.error("缺少设备码");
                }
                if (visitorTokenService.isBlacklisted(devId)) {
                    return Result.error("访客已被拉黑");
                }
                if (!isConversationOwner(conversationId, devId)) {
                    return Result.error("无权访问该会话");
                }
            } else {
                return Result.error("访客凭证无效或已过期");
            }
        }

        try {
            messageService.handleFaqInteract(conversationId, action, faqIndex, parentPath);
            return Result.OK("success");
        } catch (Exception e) {
            log.error("[CS-Message] FAQ交互处理失败", e);
            return Result.error("FAQ交互处理失败: " + e.getMessage());
        }
    }

    /**
     * 用户发送消息
     */
    @Operation(summary = "用户发送消息")
    @org.jeecg.config.shiro.IgnoreAuth
    @PostMapping("/user/send")
    public Result<CsMessage> sendUserMessage(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        String conversationId = params.get("conversationId") != null ? String.valueOf(params.get("conversationId")) : null;
        String userId = params.get("userId") != null ? String.valueOf(params.get("userId")) : null;
        String userName = params.get("userName") != null ? String.valueOf(params.get("userName")) : null;
        String content = csCryptoUtil.decryptTransport(params.get("content") != null ? String.valueOf(params.get("content")) : null);
        Integer msgType = params.get("msgType") instanceof Number ? ((Number) params.get("msgType")).intValue() : null;
        String extra = params.get("extra") != null ? String.valueOf(params.get("extra")) : null;
        
        boolean isAdmin = visitorTokenService.isAdminRequest(request);
        if (!isAdmin) {
            CsVisitorTokenPayload tokenPayload = resolveVisitorPayload(request);
            if (tokenPayload != null) {
                if (visitorTokenService.isBlacklisted(tokenPayload.getExternalUserId())) {
                    return Result.error("访客已被拉黑");
                }
                userId = tokenPayload.getExternalUserId();
                if (oConvertUtils.isEmpty(userName)) {
                    userName = tokenPayload.getUserName();
                }
            } else if (!visitorTokenService.isTokenRequired()) {
                if (!visitorTokenService.validateAppKey(request)) {
                    return Result.error("接入密钥无效");
                }
                String devId = visitorTokenService.extractDeviceId(request);
                if (oConvertUtils.isEmpty(devId)) {
                    return Result.error("缺少设备码");
                }
                if (visitorTokenService.isBlacklisted(devId)) {
                    return Result.error("访客已被拉黑");
                }
                userId = devId;
            } else {
                return Result.error("访客凭证无效或已过期");
            }
            if (oConvertUtils.isNotEmpty(conversationId) && !isConversationOwner(conversationId, userId)) {
                return Result.error("无权访问该会话");
            }
        }

        // 访客消息敏感词校验
        if (oConvertUtils.isNotEmpty(content)) {
            String hitWord = checkSensitiveWords(content);
            if (hitWord != null) {
                return Result.error("消息包含敏感内容，请修改后重试");
            }
        }

        CsMessage message = messageService.sendUserMessage(conversationId, userId, userName, content, msgType, extra);
        return Result.OK(encryptMessageForResponse(message));
    }

    /**
     * 客服发送消息
     */
    @Operation(summary = "客服发送消息")
    @PostMapping("/agent/send")
    public Result<CsMessage> sendAgentMessage(@RequestBody Map<String, Object> params) {
        // 从登录用户获取真实客服信息，防止身份伪造
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        if (loginUser == null) {
            return Result.error("未登录");
        }
        CsAgent agent = agentService.getByUserId(loginUser.getId());
        if (agent == null) {
            agent = agentService.getByUserId(loginUser.getUsername());
        }
        if (agent == null) {
            return Result.error("未找到客服信息");
        }
        String agentId = agent.getId();
        String agentName = agent.getNickname();
        
        String conversationId = params.get("conversationId") != null ? String.valueOf(params.get("conversationId")) : null;
        String content = csCryptoUtil.decryptTransport(params.get("content") != null ? String.valueOf(params.get("content")) : null);
        Integer msgType = params.get("msgType") instanceof Integer ? (Integer) params.get("msgType") : null;
        String extra = params.get("extra") != null ? String.valueOf(params.get("extra")) : null;
        
        CsMessage message = messageService.sendAgentMessage(conversationId, agentId, agentName, content, msgType, extra);
        return Result.OK(encryptMessageForResponse(message));
    }

    /**
     * 获取会话消息
     */
    @Operation(summary = "获取会话消息")
    @org.jeecg.config.shiro.IgnoreAuth
    @GetMapping("/{conversationId}")
    public Result<List<CsMessage>> getMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "50") Integer limit,
            HttpServletRequest request) {
        if (!validateVisitorAccess(conversationId, request)) {
            return Result.error("访客凭证无效或已过期");
        }
        try {
            List<CsMessage> messages = messageService.getMessages(conversationId, limit);
            encryptMessagesForTransport(messages);
            return Result.OK(messages);
        } catch (Exception e) {
            log.error("[CS-Message] 获取消息失败: conversationId={}", conversationId, e);
            return Result.error("消息加载失败，请稍后重试");
        }
    }

    /**
     * 获取会话消息（通过参数）
     */
    @Operation(summary = "获取会话消息列表")
    @org.jeecg.config.shiro.IgnoreAuth
    @GetMapping("/list")
    public Result<List<CsMessage>> getMessageList(
            @RequestParam String conversationId,
            @RequestParam(defaultValue = "100") Integer limit,
            HttpServletRequest request) {
        if (!validateVisitorAccess(conversationId, request)) {
            return Result.error("访客凭证无效或已过期");
        }
        try {
            List<CsMessage> messages = messageService.getMessages(conversationId, limit);
            encryptMessagesForTransport(messages);
            return Result.OK(messages);
        } catch (Exception e) {
            log.error("[CS-Message] 获取消息列表失败: conversationId={}", conversationId, e);
            return Result.error("消息加载失败，请稍后重试");
        }
    }

    /**
     * 获取会话消息（分页）
     */
    @Operation(summary = "获取会话消息(分页)")
    @org.jeecg.config.shiro.IgnoreAuth
    @GetMapping("/{conversationId}/page")
    public Result<List<CsMessage>> getMessagesPage(
            @PathVariable String conversationId,
            @RequestParam(required = false) String beforeId,
            @RequestParam(defaultValue = "20") Integer limit,
            HttpServletRequest request) {
        if (!validateVisitorAccess(conversationId, request)) {
            return Result.error("访客凭证无效或已过期");
        }
        try {
            List<CsMessage> messages = messageService.getMessages(conversationId, beforeId, limit);
            encryptMessagesForTransport(messages);
            return Result.OK(messages);
        } catch (Exception e) {
            log.error("[CS-Message] 分页获取消息失败: conversationId={}, beforeId={}", conversationId, beforeId, e);
            return Result.error("消息加载失败，请稍后重试");
        }
    }

    // ==================== 消息撤回 ====================

    /**
     * 撤回消息
     */
    @Operation(summary = "撤回消息")
    @PutMapping("/{messageId}/recall")
    public Result<String> recallMessage(@PathVariable String messageId) {
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        if (loginUser == null) {
            return Result.error("未登录");
        }
        String agentId = null;
        CsAgent agent = agentService.getByUserId(loginUser.getId());
        if (agent == null) {
            agent = agentService.getByUserId(loginUser.getUsername());
        }
        if (agent != null) {
            agentId = agent.getId();
        }
        if (agentId == null) {
            return Result.error("未找到客服信息");
        }
        boolean success = messageService.recallMessage(messageId, agentId);
        if (success) {
            return Result.OK("撤回成功");
        }
        return Result.error("撤回失败");
    }

    // ==================== AI相关 ====================

    /**
     * 获取AI建议
     */
    @Operation(summary = "获取AI建议")
    @GetMapping("/ai-suggestion/{conversationId}")
    public Result<Map<String, Object>> getAiSuggestion(@PathVariable String conversationId) {
        String suggestion = messageService.getCurrentAiSuggestion(conversationId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("suggestion", suggestion != null ? csCryptoUtil.encryptTransport(suggestion) : null);
        result.put("hasSuggestion", suggestion != null);
        
        return Result.OK(result);
    }

    /**
     * 确认AI建议
     */
    @Operation(summary = "确认AI建议")
    @PostMapping("/ai-confirm/{conversationId}")
    public Result<CsMessage> confirmAiSuggestion(
            @PathVariable String conversationId,
            @RequestBody Map<String, String> params) {
        
        String suggestionId = params.get("suggestionId");
        String agentId = params.get("agentId");
        String agentName = params.get("agentName");
        String editedContent = csCryptoUtil.decryptTransport(params.get("editedContent"));
        
        CsMessage message = messageService.confirmAiSuggestion(
                conversationId, suggestionId, agentId, agentName, editedContent);
        
        if (message == null) {
            return Result.error("AI建议已过期或不存在");
        }
        
        return Result.OK(encryptMessageForResponse(message));
    }

    /**
     * 生成AI建议（流式）
     * 建议内容通过WebSocket推送，这里只返回状态
     */
    @Operation(summary = "生成AI建议")
    @PostMapping("/ai-generate/{conversationId}")
    public Result<Map<String, Object>> generateAiSuggestion(
            @PathVariable String conversationId,
            @RequestBody Map<String, String> params) {
        
        String userMessage = csCryptoUtil.decryptTransport(params.get("userMessage"));
        String agentId = params.get("agentId");
        String result = messageService.generateAiSuggestion(conversationId, userMessage, agentId);
        
        Map<String, Object> response = new HashMap<>();
        if ("__STREAMING__".equals(result)) {
            response.put("streaming", true);
            response.put("success", true);
            response.put("message", "AI建议正在生成，请通过WebSocket接收");
        } else if (result != null) {
            response.put("suggestion", csCryptoUtil.encryptTransport(result));
            response.put("success", true);
        } else {
            response.put("success", false);
            response.put("message", "AI建议生成失败");
        }
        
        return Result.OK(response);
    }

    // ==================== 已读状态 ====================

    /**
     * 标记已读
     */
    @Operation(summary = "标记已读")
    @PostMapping("/{conversationId}/read")
    public Result<String> markAsRead(
            @PathVariable String conversationId,
            @RequestParam String userId) {
        messageService.markAsRead(conversationId, userId);
        return Result.OK("已标记已读");
    }

    /**
     * 获取未读数
     */
    @Operation(summary = "获取未读数")
    @GetMapping("/{conversationId}/unread")
    public Result<Map<String, Object>> getUnreadCount(@PathVariable String conversationId) {
        int count = messageService.getUnreadCount(conversationId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("unreadCount", count);
        
        return Result.OK(result);
    }

    private boolean validateVisitorAccess(String conversationId, HttpServletRequest request) {
        if (visitorTokenService.isAdminRequest(request)) {
            return true;
        }
        CsVisitorTokenPayload payload = resolveVisitorPayload(request);
        if (payload != null) {
            if (visitorTokenService.isBlacklisted(payload.getExternalUserId())) {
                return false;
            }
            return isConversationOwner(conversationId, payload.getExternalUserId());
        }
        // 免Token模式：先校验接入密钥，再通过设备码校验
        if (!visitorTokenService.isTokenRequired()) {
            if (!visitorTokenService.validateAppKey(request)) {
                return false;
            }
            String devId = visitorTokenService.extractDeviceId(request);
            if (oConvertUtils.isEmpty(devId)) {
                return false;
            }
            if (visitorTokenService.isBlacklisted(devId)) {
                return false;
            }
            return isConversationOwner(conversationId, devId);
        }
        return false;
    }

    private boolean isConversationOwner(String conversationId, String userId) {
        if (oConvertUtils.isEmpty(conversationId) || oConvertUtils.isEmpty(userId)) {
            return false;
        }
        CsConversation conversation = conversationService.getById(conversationId);
        return conversation != null && userId.equals(conversation.getUserId());
    }

    private String checkSensitiveWords(String content) {
        return messageService.checkSensitiveWords(content);
    }

    /**
     * 创建消息副本并做双层加密（用于HTTP响应，不修改原始对象）
     */
    private CsMessage encryptMessageForResponse(CsMessage original) {
        if (original == null) return null;
        CsMessage copy = new CsMessage();
        copy.setId(original.getId());
        copy.setConversationId(original.getConversationId());
        copy.setContent(csCryptoUtil.encryptTransport(csCryptoUtil.encryptStorage(original.getContent())));
        copy.setSenderId(original.getSenderId());
        copy.setSenderName(original.getSenderName());
        copy.setSenderType(original.getSenderType());
        copy.setSenderAvatar(original.getSenderAvatar());
        copy.setCreateTime(original.getCreateTime());
        copy.setMsgType(original.getMsgType());
        copy.setExtra(original.getExtra());
        copy.setIsAiGenerated(original.getIsAiGenerated());
        copy.setAiConfirmed(original.getAiConfirmed());
        copy.setAiSuggestionId(original.getAiSuggestionId());
        return copy;
    }

    /**
     * 对已存储加密的消息列表做传输层加密
     */
    private void encryptMessagesForTransport(List<CsMessage> messages) {
        if (messages == null) return;
        for (CsMessage msg : messages) {
            msg.setContent(csCryptoUtil.encryptTransport(msg.getContent()));
        }
    }

    private CsVisitorTokenPayload resolveVisitorPayload(HttpServletRequest request) {
        String sessionToken = visitorTokenService.extractSessionToken(request);
        if (oConvertUtils.isNotEmpty(sessionToken)) {
            CsVisitorTokenPayload payload = visitorTokenService.parseSessionToken(sessionToken);
            if (payload != null) {
                return payload;
            }
        }
        String shortToken = visitorTokenService.extractToken(request);
        if (oConvertUtils.isNotEmpty(shortToken)) {
            return visitorTokenService.parseToken(shortToken);
        }
        return null;
    }
}
