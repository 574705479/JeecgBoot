package org.jeecg.common.license.endpoint;

import org.jeecg.common.api.vo.Result;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

/**
 * license 模块关闭时的回退端点，避免前端请求 /license/status 时出现 404 / NoResourceFoundException
 */
@RestController
@RequestMapping("/license")
@ConditionalOnProperty(name = "license.enabled", havingValue = "false", matchIfMissing = true)
public class LicenseDisabledController {

    @GetMapping("/status")
    public Result<?> status() {
        return Result.OK(Collections.singletonMap("licensed", false));
    }

    @GetMapping("/plans")
    public Result<?> plans() {
        return Result.OK(Collections.emptyList());
    }
}
