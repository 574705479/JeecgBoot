package com.license.server.repository;

import com.license.server.entity.LicensePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface LicensePlanRepository extends JpaRepository<LicensePlan, Long>, JpaSpecificationExecutor<LicensePlan> {

    List<LicensePlan> findByAppPkAndDelFlag(Long appPk, Integer delFlag);

    List<LicensePlan> findByAppPkAndDelFlagAndStatusOrderBySortOrderAsc(Long appPk, Integer delFlag, Integer status);

    boolean existsByAppPkAndPlanCodeAndDelFlag(Long appPk, String planCode, Integer delFlag);
}
