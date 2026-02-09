package org.jeecg.modules.airag.cs.config;

import org.jeecg.modules.airag.cs.interceptor.CsAgentIpWhitelistInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 客服安全模块 Web 配置
 * 注册客服IP白名单拦截器
 */
@Configuration
public class CsSecurityWebConfig implements WebMvcConfigurer {

    @Autowired
    private CsAgentIpWhitelistInterceptor whitelistInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(whitelistInterceptor)
                .addPathPatterns("/cs/**")
                .addPathPatterns("/airag/cs/**");
    }
}
