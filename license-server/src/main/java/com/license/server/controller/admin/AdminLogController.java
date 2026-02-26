package com.license.server.controller.admin;

import com.license.server.dto.PageResult;
import com.license.server.entity.LicenseLog;
import com.license.server.repository.LicenseLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/admin/log")
@RequiredArgsConstructor
public class AdminLogController {

    private final LicenseLogRepository logRepository;

    @GetMapping("/list")
    public PageResult<LicenseLog> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long appPk,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String result) {
        Page<LicenseLog> pageResult = logRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (appPk != null) predicates.add(cb.equal(root.get("appPk"), appPk));
            if (action != null && !action.isBlank()) predicates.add(cb.equal(root.get("action"), action));
            if (result != null && !result.isBlank()) predicates.add(cb.equal(root.get("result"), result));
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createTime")));
        return PageResult.of(pageResult.getContent(), pageResult.getTotalElements(), page, size);
    }
}
