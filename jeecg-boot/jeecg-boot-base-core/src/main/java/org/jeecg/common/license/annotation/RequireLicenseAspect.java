package org.jeecg.common.license.annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.license.constant.LicenseCode;
import org.jeecg.common.license.core.LicenseClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class RequireLicenseAspect {

    private static final Logger log = LoggerFactory.getLogger(RequireLicenseAspect.class);

    private final LicenseClientService licenseClientService;

    public RequireLicenseAspect(LicenseClientService licenseClientService) {
        this.licenseClientService = licenseClientService;
    }

    @Around("@annotation(requireLicense)")
    public Object checkFeature(ProceedingJoinPoint joinPoint, RequireLicense requireLicense) throws Throwable {
        String featureCode = requireLicense.value();
        if (!licenseClientService.isLicensed()) {
            log.debug("[License] Feature check blocked - unlicensed: {}", featureCode);
            return Result.error(LicenseCode.UNLICENSED, "系统未授权");
        }
        if (!licenseClientService.isFeatureEnabled(featureCode)) {
            log.debug("[License] Feature unauthorized: {}", featureCode);
            return Result.error(LicenseCode.FEATURE_UNAUTHORIZED, "此功能需要升级授权套餐");
        }
        return joinPoint.proceed();
    }
}
