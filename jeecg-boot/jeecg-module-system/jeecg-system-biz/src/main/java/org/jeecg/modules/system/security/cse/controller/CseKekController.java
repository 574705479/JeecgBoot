package org.jeecg.modules.system.security.cse.controller;

import com.alibaba.fastjson.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.IpUtils;
import org.jeecg.modules.system.security.cse.entity.CseKek;
import org.jeecg.modules.system.security.cse.entity.CseKekAuditLog;
import org.jeecg.modules.system.security.cse.service.CseKekService;
import org.jeecg.modules.system.security.cse.service.OssFileMetaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * CSE KEK 后台管理接口（仅超管角色 + 二次密码）
 * 路径: /sys/cse/kek/**
 */
@Slf4j
@RestController
@RequestMapping("/sys/cse/kek")
public class CseKekController {

    @Autowired
    private CseKekService cseKekService;

    @Autowired
    private OssFileMetaService metaService;

    @GetMapping("/list")
    @RequiresPermissions("cse:kek:view")
    public Result<List<CseKek>> list() {
        return Result.OK(cseKekService.listAll());
    }

    @GetMapping("/audit")
    @RequiresPermissions("cse:kek:view-log")
    public Result<List<CseKekAuditLog>> audit(@RequestParam(value = "limit", defaultValue = "100") int limit) {
        return Result.OK(cseKekService.listAuditLog(limit));
    }

    @PostMapping("/generate")
    @RequiresPermissions("cse:kek:generate")
    public Result<CseKek> generate(@RequestBody JSONObject body, HttpServletRequest request) {
        LoginUser user = currentUser();
        String pwd = body.getString("password");
        String remark = body.getString("remark");
        CseKek kek = cseKekService.generate(user, IpUtils.getIpAddr(request), pwd, remark);
        kek.setKekB64(null);
        return Result.OK("已生成 STAGED 状态 KEK", kek);
    }

    @PostMapping("/activate")
    @RequiresPermissions("cse:kek:activate")
    public Result<?> activate(@RequestBody JSONObject body, HttpServletRequest request) {
        LoginUser user = currentUser();
        String pwd = body.getString("password");
        String kid = body.getString("kid");
        cseKekService.activate(user, IpUtils.getIpAddr(request), pwd, kid);
        return Result.OK("已激活: " + kid);
    }

    @PostMapping("/archive")
    @RequiresPermissions("cse:kek:archive")
    public Result<?> archive(@RequestBody JSONObject body, HttpServletRequest request) {
        LoginUser user = currentUser();
        String pwd = body.getString("password");
        String kid = body.getString("kid");
        cseKekService.archive(user, IpUtils.getIpAddr(request), pwd, kid);
        return Result.OK("已归档: " + kid);
    }

    /**
     * 导出加密 zip 备份
     * Body: { password, zipPassword }
     */
    @PostMapping("/export")
    @RequiresPermissions("cse:kek:export")
    public void exportZip(@RequestBody JSONObject body, HttpServletRequest request, HttpServletResponse response) throws IOException {
        LoginUser user = currentUser();
        String pwd = body.getString("password");
        String zipPwd = body.getString("zipPassword");
        byte[] data = cseKekService.exportEncryptedZip(user, IpUtils.getIpAddr(request), pwd, zipPwd);
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader("Content-Disposition",
                "attachment; filename=cse-kek-backup-" + System.currentTimeMillis() + ".zip");
        try (OutputStream out = response.getOutputStream()) {
            out.write(data);
            out.flush();
        }
    }

    /**
     * 从加密 zip 恢复
     */
    @PostMapping("/import")
    @RequiresPermissions("cse:kek:import")
    public Result<Integer> importZip(@RequestParam("file") MultipartFile file,
                                     @RequestParam("password") String password,
                                     @RequestParam("zipPassword") String zipPassword,
                                     HttpServletRequest request) throws IOException {
        LoginUser user = currentUser();
        int count = cseKekService.importEncryptedZip(user, IpUtils.getIpAddr(request), password, zipPassword, file.getBytes());
        return Result.OK("已导入 " + count + " 个 KEK", count);
    }

    private LoginUser currentUser() {
        return metaService.getCurrentUser();
    }
}
