package org.jeecg.modules.system.security.cse.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.PasswordUtil;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.mapper.SysUserMapper;
import org.jeecg.modules.system.security.cse.entity.CseKek;
import org.jeecg.modules.system.security.cse.entity.CseKekAuditLog;
import org.jeecg.modules.system.security.cse.mapper.CseKekAuditLogMapper;
import org.jeecg.modules.system.security.cse.mapper.CseKekMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKeyFactory;

/**
 * CSE KEK 管理服务（list/generate/activate/archive/export/import + 二次密码 + 审计日志）
 */
@Slf4j
@Service
public class CseKekService {

    @Autowired
    private CseKekMapper cseKekMapper;

    @Autowired
    private CseKekAuditLogMapper auditLogMapper;

    @Autowired
    private KekProvider kekProvider;

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 列出所有 KEK 元数据（剔除 kek_b64 明文）
     */
    public List<CseKek> listAll() {
        List<CseKek> list = kekProvider.listAllMeta();
        for (CseKek k : list) {
            k.setKekB64(null);
        }
        return list;
    }

    /**
     * 生成新 KEK（STAGED 状态）
     */
    @Transactional
    public CseKek generate(LoginUser operator, String operatorIp, String secondaryPassword, String remark) {
        verifyPassword(operator, secondaryPassword);
        CseKek kek = kekProvider.generateNew(operator.getUsername());
        if (remark != null && !remark.isEmpty()) {
            kek.setRemark(remark);
            cseKekMapper.updateById(kek);
        }
        kekProvider.writeAudit(kek.getKid(), CseKekAuditLog.ACTION_GENERATE,
                operator.getId(), operator.getUsername(), operatorIp, "生成新 KEK STAGED");
        kekProvider.refresh();
        return kek;
    }

    /**
     * 激活某 KEK：原 ACTIVE 转 DEPRECATED
     */
    @Transactional
    public void activate(LoginUser operator, String operatorIp, String secondaryPassword, String kid) {
        verifyPassword(operator, secondaryPassword);
        CseKek target = cseKekMapper.selectById(kid);
        if (target == null) {
            throw new JeecgBootException("KEK 不存在: " + kid);
        }
        if (CseKek.STATUS_ARCHIVED.equals(target.getStatus())) {
            throw new JeecgBootException("已归档 KEK 不可再激活");
        }
        // 当前 ACTIVE 转 DEPRECATED
        LambdaQueryWrapper<CseKek> qw = new LambdaQueryWrapper<>();
        qw.eq(CseKek::getStatus, CseKek.STATUS_ACTIVE);
        List<CseKek> actives = cseKekMapper.selectList(qw);
        for (CseKek a : actives) {
            if (!a.getKid().equals(kid)) {
                a.setStatus(CseKek.STATUS_DEPRECATED);
                a.setDeprecatedTime(LocalDateTime.now());
                cseKekMapper.updateById(a);
                kekProvider.writeAudit(a.getKid(), CseKekAuditLog.ACTION_DEPRECATE,
                        operator.getId(), operator.getUsername(), operatorIp, "因 " + kid + " 激活而自动转 DEPRECATED");
            }
        }
        target.setStatus(CseKek.STATUS_ACTIVE);
        target.setActivatedTime(LocalDateTime.now());
        cseKekMapper.updateById(target);
        kekProvider.writeAudit(kid, CseKekAuditLog.ACTION_ACTIVATE,
                operator.getId(), operator.getUsername(), operatorIp, "激活为 ACTIVE");
        kekProvider.refresh();
    }

    /**
     * 归档（仍可解密旧文件，但 UI 上隐藏）
     */
    @Transactional
    public void archive(LoginUser operator, String operatorIp, String secondaryPassword, String kid) {
        verifyPassword(operator, secondaryPassword);
        CseKek k = cseKekMapper.selectById(kid);
        if (k == null) {
            throw new JeecgBootException("KEK 不存在: " + kid);
        }
        if (CseKek.STATUS_ACTIVE.equals(k.getStatus())) {
            throw new JeecgBootException("活跃 KEK 不能直接归档，请先激活其它 KEK");
        }
        k.setStatus(CseKek.STATUS_ARCHIVED);
        cseKekMapper.updateById(k);
        kekProvider.writeAudit(kid, CseKekAuditLog.ACTION_ARCHIVE,
                operator.getId(), operator.getUsername(), operatorIp, "归档");
        kekProvider.refresh();
    }

    /**
     * 导出加密 zip 备份。zip 用 PBKDF2 派生的 AES-GCM 加密整个 JSON 主体。
     * 返回字节数组（前端下载）。
     */
    public byte[] exportEncryptedZip(LoginUser operator, String operatorIp, String secondaryPassword, String zipPassword) {
        verifyPassword(operator, secondaryPassword);
        if (zipPassword == null || zipPassword.length() < 8) {
            throw new JeecgBootException("zip 密码必须至少 8 位");
        }
        List<CseKek> all = cseKekMapper.selectList(null);
        // 使用 JSONArray 序列化避免特殊字符（双引号/反斜杠）破坏导出 JSON
        JSONArray arr = new JSONArray();
        for (CseKek k : all) {
            JSONObject o = new JSONObject();
            o.put("kid", k.getKid());
            o.put("kekB64", k.getKekB64());
            o.put("status", k.getStatus());
            o.put("createdBy", k.getCreatedBy());
            arr.add(o);
        }
        byte[] body = arr.toJSONString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] key = f.generateSecret(new PBEKeySpec(zipPassword.toCharArray(), salt, 100_000, 256)).getEncoded();
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] cipher = c.doFinal(body);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(bos)) {
                zos.putNextEntry(new ZipEntry("cse-kek.enc"));
                zos.write(salt);
                zos.write(iv);
                zos.write(cipher);
                zos.closeEntry();
            }
            kekProvider.writeAudit(null, CseKekAuditLog.ACTION_EXPORT,
                    operator.getId(), operator.getUsername(), operatorIp,
                    "导出 " + all.size() + " 个 KEK 备份");
            return bos.toByteArray();
        } catch (Exception e) {
            throw new JeecgBootException("导出失败: " + e.getMessage());
        }
    }

    /**
     * 从加密 zip 恢复 KEK
     */
    @Transactional
    public int importEncryptedZip(LoginUser operator, String operatorIp, String secondaryPassword,
                                   String zipPassword, byte[] zipBytes) {
        verifyPassword(operator, secondaryPassword);
        try {
            byte[] all = null;
            try (ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
                ZipEntry e;
                while ((e = zis.getNextEntry()) != null) {
                    if ("cse-kek.enc".equals(e.getName())) {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = zis.read(buf)) > 0) {
                            bos.write(buf, 0, n);
                        }
                        all = bos.toByteArray();
                        break;
                    }
                }
            }
            if (all == null || all.length < 16 + 12 + 16) {
                throw new JeecgBootException("zip 内容无效");
            }
            byte[] salt = java.util.Arrays.copyOfRange(all, 0, 16);
            byte[] iv = java.util.Arrays.copyOfRange(all, 16, 28);
            byte[] cipherBody = java.util.Arrays.copyOfRange(all, 28, all.length);
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] key = f.generateSecret(new PBEKeySpec(zipPassword.toCharArray(), salt, 100_000, 256)).getEncoded();
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] plainBody = c.doFinal(cipherBody);

            String json = new String(plainBody, java.nio.charset.StandardCharsets.UTF_8);
            com.alibaba.fastjson.JSONArray arr = com.alibaba.fastjson.JSON.parseArray(json);
            int count = 0;
            for (int i = 0; i < arr.size(); i++) {
                com.alibaba.fastjson.JSONObject o = arr.getJSONObject(i);
                String kid = o.getString("kid");
                if (cseKekMapper.selectById(kid) != null) {
                    log.warn("[CSE] 跳过已存在的 KEK: {}", kid);
                    continue;
                }
                CseKek kek = new CseKek();
                kek.setKid(kid);
                kek.setKekB64(o.getString("kekB64"));
                kek.setStatus(o.getString("status"));
                kek.setCreatedBy(o.getString("createdBy"));
                kek.setCreatedTime(LocalDateTime.now());
                kek.setRemark("从备份导入");
                kek.setFileCount(0L);
                cseKekMapper.insert(kek);
                count++;
            }
            kekProvider.writeAudit(null, CseKekAuditLog.ACTION_IMPORT,
                    operator.getId(), operator.getUsername(), operatorIp,
                    "导入 " + count + " 个 KEK");
            kekProvider.refresh();
            return count;
        } catch (JeecgBootException e) {
            throw e;
        } catch (Exception e) {
            throw new JeecgBootException("导入失败: " + e.getMessage());
        }
    }

    public List<CseKekAuditLog> listAuditLog(int limit) {
        LambdaQueryWrapper<CseKekAuditLog> qw = new LambdaQueryWrapper<>();
        qw.orderByDesc(CseKekAuditLog::getOperateTime).last("LIMIT " + Math.max(1, Math.min(limit, 500)));
        return auditLogMapper.selectList(qw);
    }

    /**
     * 二次密码校验：超管输入自己的登录密码
     *
     * 改为 public 以便 CseConfigService 等其他敏感操作复用，集中维护一份校验逻辑。
     */
    public void verifyPassword(LoginUser operator, String secondaryPassword) {
        if (operator == null) {
            throw new JeecgBootException("未登录");
        }
        if (secondaryPassword == null || secondaryPassword.isEmpty()) {
            throw new JeecgBootException("请输入二次密码");
        }
        SysUser u = sysUserMapper.selectById(operator.getId());
        if (u == null) {
            throw new JeecgBootException("用户不存在");
        }
        String enc = PasswordUtil.encrypt(u.getUsername(), secondaryPassword, u.getSalt());
        if (!enc.equals(u.getPassword())) {
            throw new JeecgBootException("二次密码错误");
        }
    }

    public static byte[] decodeBase64(String s) {
        return Base64.getDecoder().decode(s);
    }
}
