package org.jeecg.modules.system.security.cse.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.IpUtils;
import org.jeecg.modules.system.security.cse.service.CseConfigService;
import org.jeecg.modules.system.security.cse.service.OssFileMetaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * CSE 动态配置管理（基础配置 Tab）
 *
 * 路径：/sys/cse/config
 *
 * 与 CseKekController 同级，共用同一菜单「文件加密管理」。
 */
@Slf4j
@RestController
@RequestMapping("/sys/cse/config")
public class CseConfigController {

    @Autowired
    private CseConfigService cseConfigService;

    @Autowired
    private OssFileMetaService metaService;

    /**
     * 读取当前配置 + 字典 + 自定义部分
     *
     * 返回结构：
     * {
     *   enabled: true,
     *   encryptedPaths: [...],
     *   publicPaths: [...],
     *   dictionary: [{path, name, description, category, forceLocked}, ...],
     *   customEncrypted: [...],   // 不在字典中的自定义白名单
     *   customPublic: [...]
     * }
     */
    @GetMapping
    @RequiresPermissions("cse:config:view")
    public Result<JSONObject> get() {
        return Result.OK(cseConfigService.getCurrentForUi());
    }

    /**
     * 保存配置
     *
     * Body: { enabled, encryptedPaths: [..], publicPaths: [..], password }
     * 由前端把字典勾选 + 高级 tag 兜底合并后整体提交。
     */
    @PutMapping
    @RequiresPermissions("cse:config:edit")
    public Result<?> save(@RequestBody JSONObject body, HttpServletRequest request) {
        LoginUser user = metaService.getCurrentUser();
        String pwd = body.getString("password");

        CseConfigService.SaveRequest req = new CseConfigService.SaveRequest();
        req.enabled = body.getBooleanValue("enabled");
        req.encryptedPaths = readArray(body, "encryptedPaths");
        req.publicPaths = readArray(body, "publicPaths");

        cseConfigService.save(req, user, IpUtils.getIpAddr(request), pwd);
        return Result.OK("配置已保存，立即生效（无需重启）");
    }

    /**
     * 命中测试
     *
     * Body: { bizPath, mode: "current"|"preview", previewEncrypted?: [], previewPublic?: [], previewEnabled?: bool }
     *  - current: 用 DB 当前生效配置
     *  - preview: 用前端待保存的配置（让用户勾完未提交也能即时预览）
     */
    @PostMapping("/dryRun")
    @RequiresPermissions("cse:config:view")
    public Result<JSONObject> dryRun(@RequestBody JSONObject body) {
        String bizPath = body.getString("bizPath");
        String mode = body.getString("mode");
        if (mode == null || mode.isEmpty()) {
            mode = "current";
        }
        List<String> previewEnc = readArray(body, "previewEncrypted");
        List<String> previewPub = readArray(body, "previewPublic");
        Boolean previewEnabled = body.containsKey("previewEnabled") ? body.getBoolean("previewEnabled") : null;
        return Result.OK(cseConfigService.dryRun(bizPath, mode, previewEnc, previewPub, previewEnabled));
    }

    private static List<String> readArray(JSONObject body, String key) {
        JSONArray arr = body.getJSONArray(key);
        if (arr == null) {
            return new ArrayList<>();
        }
        List<String> r = new ArrayList<>(arr.size());
        for (int i = 0; i < arr.size(); i++) {
            String s = arr.getString(i);
            if (s != null) r.add(s);
        }
        return r;
    }
}
