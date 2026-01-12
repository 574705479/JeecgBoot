package org.jeecg.modules.airag.app.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.airag.app.entity.AiragAppSecret;
import org.jeecg.modules.airag.app.service.IAiragAppSecretService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * @Description: AI应用接入密钥管理
 * @Author: jeecg-boot
 * @Date: 2025-01-07
 * @Version: V1.0
 */
@Slf4j
@Tag(name = "AI应用接入密钥管理")
@RestController
@RequestMapping("/airag/appSecret")
public class AiragAppSecretController {

    @Autowired
    private IAiragAppSecretService airagAppSecretService;

    /**
     * 分页列表查询
     *
     * @param airagAppSecret 查询参数
     * @param pageNo         页码
     * @param pageSize       每页数量
     * @param req            请求
     * @return 分页结果
     */
    @Operation(summary = "分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<AiragAppSecret>> queryPageList(AiragAppSecret airagAppSecret,
                                                        @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                        @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                        HttpServletRequest req) {
        QueryWrapper<AiragAppSecret> queryWrapper = QueryGenerator.initQueryWrapper(airagAppSecret, req.getParameterMap());
        Page<AiragAppSecret> page = new Page<>(pageNo, pageSize);
        IPage<AiragAppSecret> pageList = airagAppSecretService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 添加
     *
     * @param airagAppSecret 密钥配置
     * @return 结果
     */
    @Operation(summary = "添加")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody AiragAppSecret airagAppSecret) {
        // 自动生成密钥
        if (airagAppSecret.getSecretKey() == null || airagAppSecret.getSecretKey().isEmpty()) {
            airagAppSecret.setSecretKey(airagAppSecretService.generateSecretKey());
        }
        // 默认启用
        if (airagAppSecret.getEnabled() == null) {
            airagAppSecret.setEnabled(1);
        }
        // 默认5分钟有效期
        if (airagAppSecret.getTokenExpireMinutes() == null) {
            airagAppSecret.setTokenExpireMinutes(5);
        }
        airagAppSecretService.save(airagAppSecret);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     *
     * @param airagAppSecret 密钥配置
     * @return 结果
     */
    @Operation(summary = "编辑")
    @PutMapping(value = "/edit")
    public Result<String> edit(@RequestBody AiragAppSecret airagAppSecret) {
        airagAppSecretService.updateById(airagAppSecret);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     *
     * @param id ID
     * @return 结果
     */
    @Operation(summary = "通过id删除")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id") String id) {
        airagAppSecretService.removeById(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     *
     * @param ids ID列表
     * @return 结果
     */
    @Operation(summary = "批量删除")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids") String ids) {
        airagAppSecretService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     *
     * @param id ID
     * @return 密钥配置
     */
    @Operation(summary = "通过id查询")
    @GetMapping(value = "/queryById")
    public Result<AiragAppSecret> queryById(@RequestParam(name = "id") String id) {
        AiragAppSecret airagAppSecret = airagAppSecretService.getById(id);
        if (airagAppSecret == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(airagAppSecret);
    }

    /**
     * 根据应用ID查询密钥配置
     *
     * @param appId 应用ID
     * @return 密钥配置
     */
    @Operation(summary = "根据应用ID查询密钥配置")
    @GetMapping(value = "/queryByAppId")
    public Result<AiragAppSecret> queryByAppId(@RequestParam(name = "appId") String appId) {
        AiragAppSecret airagAppSecret = airagAppSecretService.getEnabledByAppId(appId);
        return Result.OK(airagAppSecret);
    }

    /**
     * 生成新密钥
     *
     * @return 新密钥
     */
    @Operation(summary = "生成新密钥")
    @GetMapping(value = "/generateKey")
    public Result<String> generateKey() {
        String secretKey = airagAppSecretService.generateSecretKey();
        return Result.OK(secretKey);
    }

    /**
     * 重新生成密钥
     *
     * @param id 配置ID
     * @return 新密钥
     */
    @Operation(summary = "重新生成密钥")
    @PostMapping(value = "/regenerateKey")
    public Result<String> regenerateKey(@RequestParam(name = "id") String id) {
        AiragAppSecret secret = airagAppSecretService.getById(id);
        if (secret == null) {
            return Result.error("未找到对应数据");
        }
        String newKey = airagAppSecretService.generateSecretKey();
        secret.setSecretKey(newKey);
        airagAppSecretService.updateById(secret);
        return Result.OK(newKey);
    }
}
