package org.jeecg.modules.system.security.cse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.system.security.cse.entity.CseKek;
import org.jeecg.modules.system.security.cse.entity.CseKekAuditLog;
import org.jeecg.modules.system.security.cse.mapper.CseKekAuditLogMapper;
import org.jeecg.modules.system.security.cse.mapper.CseKekMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KEK 提供者：启动时从 cse_kek 表加载到内存，首次启动表空时自动生成 k1。
 * 仅本类可访问明文 KEK 字节，对外只暴露 wrap/unwrap 用的字节获取接口。
 */
@Slf4j
@Component
public class KekProvider {

    @Autowired
    private CseKekMapper cseKekMapper;

    @Autowired
    private CseKekAuditLogMapper auditLogMapper;

    private final Map<String, byte[]> keyMap = new ConcurrentHashMap<>();
    private volatile String activeKid;

    @PostConstruct
    public synchronized void init() {
        refresh();
        if (keyMap.isEmpty()) {
            String kid = "k1";
            byte[] kekBytes = new byte[32];
            new SecureRandom().nextBytes(kekBytes);
            CseKek kek = new CseKek();
            kek.setKid(kid);
            kek.setKekB64(Base64.getEncoder().encodeToString(kekBytes));
            kek.setStatus(CseKek.STATUS_ACTIVE);
            kek.setCreatedBy("SYSTEM");
            kek.setCreatedTime(LocalDateTime.now());
            kek.setActivatedTime(LocalDateTime.now());
            kek.setFileCount(0L);
            kek.setRemark("首次启动自动生成");
            cseKekMapper.insert(kek);
            writeAudit(kid, CseKekAuditLog.ACTION_INIT, "SYSTEM", "SYSTEM", "127.0.0.1", "首次启动自动初始化");
            log.info("[CSE] 首次启动检测到无 KEK，已自动生成 kid={}", kid);
            refresh();
        }
        log.info("[CSE] 已加载 {} 个 KEK，活跃 kid={}", keyMap.size(), activeKid);
    }

    /**
     * 重新加载 KEK 表（KEK 轮换/导入后调用）
     */
    public synchronized void refresh() {
        Map<String, byte[]> newMap = new ConcurrentHashMap<>();
        String newActive = null;
        List<CseKek> all = cseKekMapper.selectList(null);
        for (CseKek k : all) {
            try {
                newMap.put(k.getKid(), Base64.getDecoder().decode(k.getKekB64()));
                if (CseKek.STATUS_ACTIVE.equals(k.getStatus())) {
                    newActive = k.getKid();
                }
            } catch (Exception e) {
                log.error("[CSE] KEK 行解析失败 kid={}", k.getKid(), e);
            }
        }
        this.keyMap.clear();
        this.keyMap.putAll(newMap);
        this.activeKid = newActive;
    }

    /**
     * 取活跃 KEK 字节（加密新文件用）
     */
    public byte[] getActiveKek() {
        if (activeKid == null) {
            throw new JeecgBootException("[CSE] 当前没有活跃 KEK");
        }
        byte[] k = keyMap.get(activeKid);
        if (k == null) {
            throw new JeecgBootException("[CSE] 活跃 KEK 已被卸载: " + activeKid);
        }
        return k;
    }

    public String getActiveKid() {
        return activeKid;
    }

    /**
     * 按 kid 取 KEK 字节（解密历史文件用）
     */
    public byte[] getKek(String kid) {
        byte[] k = keyMap.get(kid);
        if (k == null) {
            throw new JeecgBootException("[CSE] 未找到 KEK: " + kid);
        }
        return k;
    }

    public boolean hasKek(String kid) {
        return keyMap.containsKey(kid);
    }

    public List<CseKek> listAllMeta() {
        LambdaQueryWrapper<CseKek> qw = new LambdaQueryWrapper<>();
        qw.orderByAsc(CseKek::getCreatedTime);
        return cseKekMapper.selectList(qw);
    }

    /**
     * 仅由 CseKekService 调用：创建新 KEK（STAGED 状态）
     */
    public synchronized CseKek generateNew(String creator) {
        String kid = nextKid();
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        CseKek kek = new CseKek();
        kek.setKid(kid);
        kek.setKekB64(Base64.getEncoder().encodeToString(bytes));
        kek.setStatus(CseKek.STATUS_STAGED);
        kek.setCreatedBy(creator);
        kek.setCreatedTime(LocalDateTime.now());
        kek.setFileCount(0L);
        cseKekMapper.insert(kek);
        refresh();
        return kek;
    }

    private String nextKid() {
        List<CseKek> list = cseKekMapper.selectList(null);
        int max = 0;
        for (CseKek k : list) {
            String kid = k.getKid();
            if (kid != null && kid.startsWith("k")) {
                try {
                    int n = Integer.parseInt(kid.substring(1));
                    if (n > max) {
                        max = n;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return "k" + (max + 1);
    }

    public void writeAudit(String kid, String action, String operatorId, String operatorName,
                           String operatorIp, String remark) {
        CseKekAuditLog log = new CseKekAuditLog();
        log.setKid(kid);
        log.setAction(action);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setOperatorIp(operatorIp);
        log.setOperateTime(LocalDateTime.now());
        log.setRemark(remark);
        auditLogMapper.insert(log);
    }
}
