package org.jeecg.modules.airag.cs.task;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.chat.entity.ChatMessage;
import org.jeecg.modules.airag.chat.service.IChatMessageService;
import org.jeecg.modules.airag.cs.entity.*;
import org.jeecg.modules.airag.cs.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CsDataCleanupTask {

    private static final String LOCK_KEY = "cs:lock:data_cleanup";
    private static final String CONFIG_KEY = "data_cleanup";
    private static final String CONFIG_REDIS_KEY = "cs:global:data_cleanup";
    private static final int BATCH_SIZE = 500;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private CsGlobalConfigMapper csGlobalConfigMapper;

    @Autowired
    private CsConversationMapper conversationMapper;

    @Autowired
    private CsCollaboratorMapper collaboratorMapper;

    @Autowired
    private CsAgentLoginLogMapper loginLogMapper;

    @Autowired
    private CsAgentStatusLogMapper statusLogMapper;

    @Autowired
    private CsVisitorMapper visitorMapper;

    @Autowired
    private CsIpGeoCacheMapper ipGeoCacheMapper;

    @Autowired
    private CsFileHashMapper fileHashMapper;

    @Autowired
    private CsLeaveMessageMapper leaveMessageMapper;

    @Autowired
    private CsCleanupLogMapper cleanupLogMapper;

    @Autowired
    @Lazy
    private IChatMessageService chatMessageService;

    @Scheduled(cron = "0 0 3 * * ?")
    public void dailyCleanup() {
        executeCleanup("auto", null);
    }

    /**
     * 手动触发入口
     */
    public Map<String, Integer> triggerCleanup(String operator) {
        return executeCleanup("manual", operator);
    }

    private Map<String, Integer> executeCleanup(String triggerType, String operator) {
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "1", 30, TimeUnit.MINUTES);
        if (locked == null || !locked) {
            log.info("[CS-Cleanup] 未获取到锁，跳过本次清理");
            return Collections.emptyMap();
        }

        Date startTime = new Date();
        Map<String, Integer> results = new LinkedHashMap<>();

        try {
            JSONObject config = getCleanupConfig();
            if (config == null || !config.getBooleanValue("enabled")) {
                log.info("[CS-Cleanup] 清理功能未启用，跳过");
                return results;
            }

            int conversationDays = config.getIntValue("conversationDays");
            int logAndVisitorDays = config.getIntValue("logAndVisitorDays");
            int cacheDays = config.getIntValue("cacheDays");

            if (conversationDays < 1) conversationDays = 1;
            if (logAndVisitorDays < 1) logAndVisitorDays = 1;
            if (cacheDays < 1) cacheDays = 1;

            Date convDeadline = daysAgo(conversationDays);
            Date logDeadline = daysAgo(logAndVisitorDays);
            Date cacheDeadline = daysAgo(cacheDays);

            // === 第1组: 对话记录 ===
            cleanConversations(convDeadline, results);
            cleanAiConversations(convDeadline, results);
            cleanSoftDeletedMessages(convDeadline, results);

            // === 第2组: 日志与访客 ===
            cleanLoginLogs(logDeadline, results);
            cleanStatusLogs(logDeadline, results);
            cleanInactiveVisitors(logDeadline, results);

            // === 第3组: 缓存与辅助 ===
            cleanIpGeoCache(cacheDeadline, results);
            cleanFileHash(cacheDeadline, results);
            cleanLeaveMessages(cacheDeadline, results);

            log.info("[CS-Cleanup] 清理完成: {}", results);
        } catch (Exception e) {
            log.error("[CS-Cleanup] 清理过程出现异常", e);
        } finally {
            redisTemplate.delete(LOCK_KEY);
            saveCleanupLog(triggerType, operator, startTime, results);
        }

        return results;
    }

    // ==================== 第1组: 对话记录 ====================

    private void cleanConversations(Date deadline, Map<String, Integer> results) {
        int totalConv = 0;
        long totalMsg = 0;
        int totalCollab = 0;

        try {
            while (true) {
                List<String> expiredIds = conversationMapper.selectExpiredClosedIds(deadline, BATCH_SIZE);
                if (expiredIds.isEmpty()) break;

                if (!expiredIds.isEmpty()) {
                    long msgDeleted = chatMessageService.physicalDeleteByConversationIds(expiredIds, ChatMessage.CONV_TYPE_AGENT);
                    totalMsg += msgDeleted;

                    int collabDeleted = collaboratorMapper.deleteByConversationIds(expiredIds);
                    totalCollab += collabDeleted;
                }

                int deleted = conversationMapper.physicalDeleteExpired(deadline, BATCH_SIZE);
                totalConv += deleted;

                if (deleted < BATCH_SIZE) break;
                sleep();
            }
        } catch (Exception e) {
            log.error("[CS-Cleanup] 清理会话失败", e);
        }

        results.put("conversation", totalConv);
        results.put("csMessage", (int) totalMsg);
        results.put("collaborator", totalCollab);
    }

    private void cleanAiConversations(Date deadline, Map<String, Integer> results) {
        int keyCount = 0;
        long msgCount = 0;
        try {
            List<String> expiredConvIds = new ArrayList<>();
            ScanOptions options = ScanOptions.scanOptions().match("airag:chat:*").count(200).build();
            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    String key = cursor.next();
                    try {
                        String json = redisTemplate.opsForValue().get(key);
                        if (json == null) continue;
                        JSONObject conv = JSON.parseObject(json);
                        Date createTime = parseDate(conv.get("createTime"));
                        if (createTime != null && createTime.before(deadline)) {
                            String convId = conv.getString("id");
                            if (convId != null) {
                                expiredConvIds.add(convId);
                            }
                            redisTemplate.delete(key);
                            keyCount++;

                            if (expiredConvIds.size() >= BATCH_SIZE) {
                                msgCount += chatMessageService.physicalDeleteByConversationIds(expiredConvIds, ChatMessage.CONV_TYPE_AI);
                                expiredConvIds.clear();
                                sleep();
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            if (!expiredConvIds.isEmpty()) {
                msgCount += chatMessageService.physicalDeleteByConversationIds(expiredConvIds, ChatMessage.CONV_TYPE_AI);
            }
        } catch (Exception e) {
            log.error("[CS-Cleanup] 清理AI会话失败", e);
        }
        results.put("aiRedisKey", keyCount);
        results.put("aiMessage", (int) msgCount);
    }

    private void cleanSoftDeletedMessages(Date deadline, Map<String, Integer> results) {
        try {
            long deleted = chatMessageService.physicalDeleteSoftDeleted(deadline);
            results.put("softDeletedMessage", (int) deleted);
        } catch (Exception e) {
            log.error("[CS-Cleanup] 清理软删除消息失败", e);
            results.put("softDeletedMessage", 0);
        }
    }

    // ==================== 第2组: 日志与访客 ====================

    private void cleanLoginLogs(Date deadline, Map<String, Integer> results) {
        int total = 0;
        try {
            while (true) {
                LambdaQueryWrapper<CsAgentLoginLog> wrapper = new LambdaQueryWrapper<>();
                wrapper.lt(CsAgentLoginLog::getLoginDate, deadline).last("LIMIT " + BATCH_SIZE);
                List<CsAgentLoginLog> batch = loginLogMapper.selectList(wrapper);
                if (batch.isEmpty()) break;

                List<String> ids = new ArrayList<>();
                for (CsAgentLoginLog item : batch) ids.add(item.getId());
                loginLogMapper.deleteByIds(ids);
                total += batch.size();

                if (batch.size() < BATCH_SIZE) break;
                sleep();
            }
        } catch (Exception e) {
            log.error("[CS-Cleanup] 清理登录日志失败", e);
        }
        results.put("loginLog", total);
    }

    private void cleanStatusLogs(Date deadline, Map<String, Integer> results) {
        int total = 0;
        try {
            while (true) {
                LambdaQueryWrapper<CsAgentStatusLog> wrapper = new LambdaQueryWrapper<>();
                wrapper.lt(CsAgentStatusLog::getStartTime, deadline).last("LIMIT " + BATCH_SIZE);
                List<CsAgentStatusLog> batch = statusLogMapper.selectList(wrapper);
                if (batch.isEmpty()) break;

                List<Long> ids = new ArrayList<>();
                for (CsAgentStatusLog item : batch) ids.add(item.getId());
                statusLogMapper.deleteByIds(ids);
                total += batch.size();

                if (batch.size() < BATCH_SIZE) break;
                sleep();
            }
        } catch (Exception e) {
            log.error("[CS-Cleanup] 清理状态日志失败", e);
        }
        results.put("statusLog", total);
    }

    private void cleanInactiveVisitors(Date deadline, Map<String, Integer> results) {
        int total = 0;
        try {
            while (true) {
                LambdaQueryWrapper<CsVisitor> wrapper = new LambdaQueryWrapper<>();
                wrapper.lt(CsVisitor::getLastVisitTime, deadline)
                        .and(w -> w.isNull(CsVisitor::getStar).or().eq(CsVisitor::getStar, 0))
                        .last("LIMIT " + BATCH_SIZE);
                List<CsVisitor> batch = visitorMapper.selectList(wrapper);
                if (batch.isEmpty()) break;

                List<String> ids = new ArrayList<>();
                for (CsVisitor item : batch) ids.add(item.getId());
                visitorMapper.deleteByIds(ids);
                total += batch.size();

                if (batch.size() < BATCH_SIZE) break;
                sleep();
            }
        } catch (Exception e) {
            log.error("[CS-Cleanup] 清理不活跃访客失败", e);
        }
        results.put("visitor", total);
    }

    // ==================== 第3组: 缓存与辅助 ====================

    private void cleanIpGeoCache(Date deadline, Map<String, Integer> results) {
        int total = 0;
        try {
            while (true) {
                LambdaQueryWrapper<CsIpGeoCache> wrapper = new LambdaQueryWrapper<>();
                wrapper.lt(CsIpGeoCache::getCreateTime, deadline).last("LIMIT " + BATCH_SIZE);
                List<CsIpGeoCache> batch = ipGeoCacheMapper.selectList(wrapper);
                if (batch.isEmpty()) break;

                List<String> ids = new ArrayList<>();
                for (CsIpGeoCache item : batch) ids.add(item.getId());
                ipGeoCacheMapper.deleteByIds(ids);
                total += batch.size();

                if (batch.size() < BATCH_SIZE) break;
                sleep();
            }
        } catch (Exception e) {
            log.error("[CS-Cleanup] 清理IP缓存失败", e);
        }
        results.put("ipGeoCache", total);
    }

    private void cleanFileHash(Date deadline, Map<String, Integer> results) {
        int total = 0;
        try {
            while (true) {
                LambdaQueryWrapper<CsFileHash> wrapper = new LambdaQueryWrapper<>();
                wrapper.lt(CsFileHash::getCreateTime, deadline).last("LIMIT " + BATCH_SIZE);
                List<CsFileHash> batch = fileHashMapper.selectList(wrapper);
                if (batch.isEmpty()) break;

                List<String> ids = new ArrayList<>();
                for (CsFileHash item : batch) ids.add(item.getId());
                fileHashMapper.deleteByIds(ids);
                total += batch.size();

                if (batch.size() < BATCH_SIZE) break;
                sleep();
            }
        } catch (Exception e) {
            log.error("[CS-Cleanup] 清理文件哈希失败", e);
        }
        results.put("fileHash", total);
    }

    private void cleanLeaveMessages(Date deadline, Map<String, Integer> results) {
        int total = 0;
        try {
            while (true) {
                LambdaQueryWrapper<CsLeaveMessage> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(CsLeaveMessage::getStatus, CsLeaveMessage.STATUS_REPLIED)
                        .lt(CsLeaveMessage::getCreateTime, deadline)
                        .last("LIMIT " + BATCH_SIZE);
                List<CsLeaveMessage> batch = leaveMessageMapper.selectList(wrapper);
                if (batch.isEmpty()) break;

                List<String> ids = new ArrayList<>();
                for (CsLeaveMessage item : batch) ids.add(item.getId());
                leaveMessageMapper.deleteByIds(ids);
                total += batch.size();

                if (batch.size() < BATCH_SIZE) break;
                sleep();
            }
        } catch (Exception e) {
            log.error("[CS-Cleanup] 清理留言失败", e);
        }
        results.put("leaveMessage", total);
    }

    // ==================== 工具方法 ====================

    private JSONObject getCleanupConfig() {
        try {
            String json = redisTemplate.opsForValue().get(CONFIG_REDIS_KEY);
            if (json == null || json.isEmpty()) {
                CsGlobalConfig config = csGlobalConfigMapper.selectById(CONFIG_KEY);
                json = config != null ? config.getConfigValue() : null;
                if (json != null && !json.isEmpty()) {
                    redisTemplate.opsForValue().set(CONFIG_REDIS_KEY, json);
                }
            }
            return json != null ? JSON.parseObject(json) : null;
        } catch (Exception e) {
            log.warn("[CS-Cleanup] 读取清理配置失败", e);
            return null;
        }
    }

    private void saveCleanupLog(String triggerType, String operator, Date startTime, Map<String, Integer> results) {
        try {
            Date endTime = new Date();
            CsCleanupLog logEntry = new CsCleanupLog();
            logEntry.setTriggerType(triggerType);
            logEntry.setStartTime(startTime);
            logEntry.setEndTime(endTime);
            logEntry.setDurationMs((int) (endTime.getTime() - startTime.getTime()));
            logEntry.setResultJson(JSON.toJSONString(results));
            logEntry.setCreateBy(operator);
            cleanupLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.error("[CS-Cleanup] 保存清理日志失败", e);
        }
    }

    private Date daysAgo(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -days);
        return cal.getTime();
    }

    private Date parseDate(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Date) return (Date) obj;
        if (obj instanceof Long) return new Date((Long) obj);
        if (obj instanceof Number) return new Date(((Number) obj).longValue());
        try {
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(String.valueOf(obj));
        } catch (Exception e) {
            return null;
        }
    }

    private void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
