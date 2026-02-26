package com.license.server.service;

import com.license.server.entity.App;
import com.license.server.entity.LicensePlan;
import com.license.server.repository.AppRepository;
import com.license.server.repository.LicensePlanRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LicensePlanService {

    private final LicensePlanRepository planRepository;
    private final AppRepository appRepository;

    public Page<LicensePlan> list(int page, int size, Long appPk) {
        return planRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("delFlag"), 0));
            if (appPk != null) predicates.add(cb.equal(root.get("appPk"), appPk));
            return cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, size, Sort.by("sortOrder")));
    }

    public List<LicensePlan> listByApp(Long appPk) {
        return planRepository.findByAppPkAndDelFlag(appPk, 0);
    }

    public LicensePlan getById(Long id) {
        return planRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("套餐不存在"));
    }

    @Transactional
    public LicensePlan create(LicensePlan plan) {
        if (planRepository.existsByAppPkAndPlanCodeAndDelFlag(plan.getAppPk(), plan.getPlanCode(), 0)) {
            throw new IllegalArgumentException("同一应用下套餐标识不能重复");
        }
        filterByDefinitions(plan);
        return planRepository.save(plan);
    }

    @Transactional
    public LicensePlan update(Long id, LicensePlan updated) {
        LicensePlan plan = getById(id);
        plan.setPlanName(updated.getPlanName());
        plan.setQuotas(updated.getQuotas());
        plan.setFeatures(updated.getFeatures());
        plan.setSortOrder(updated.getSortOrder());
        plan.setStatus(updated.getStatus());
        filterByDefinitions(plan);
        return planRepository.save(plan);
    }

    @Transactional
    public void softDelete(Long id) {
        LicensePlan plan = getById(id);
        plan.setDelFlag(1);
        planRepository.save(plan);
    }

    private void filterByDefinitions(LicensePlan plan) {
        App app = appRepository.findById(plan.getAppPk()).orElse(null);
        if (app == null) return;
        Set<String> validQuotaCodes = (app.getQuotasDef() != null)
                ? app.getQuotasDef().stream().map(d -> (String) d.get("code")).collect(Collectors.toSet())
                : Collections.emptySet();
        Set<String> validFeatureCodes = (app.getFeaturesDef() != null)
                ? app.getFeaturesDef().stream().map(d -> (String) d.get("code")).collect(Collectors.toSet())
                : Collections.emptySet();
        if (plan.getQuotas() != null) {
            plan.getQuotas().keySet().retainAll(validQuotaCodes);
        }
        if (plan.getFeatures() != null) {
            plan.getFeatures().removeIf(f -> !validFeatureCodes.contains(f));
        }
    }
}
