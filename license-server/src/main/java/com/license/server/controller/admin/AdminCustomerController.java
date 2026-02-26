package com.license.server.controller.admin;

import com.license.server.dto.PageResult;
import com.license.server.dto.Result;
import com.license.server.entity.Customer;
import com.license.server.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/customer")
@RequiredArgsConstructor
public class AdminCustomerController {

    private final CustomerService customerService;

    @GetMapping("/list")
    public PageResult<Customer> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        Page<Customer> result = customerService.list(page, Math.min(size, 100), keyword);
        return PageResult.of(result.getContent(), result.getTotalElements(), page, size);
    }

    @GetMapping("/{id}")
    public Result<Customer> getById(@PathVariable Long id) {
        return Result.ok(customerService.getById(id));
    }

    @PostMapping
    public Result<Customer> create(@RequestBody Customer customer) {
        return Result.ok(customerService.create(customer));
    }

    @PutMapping("/{id}")
    public Result<Customer> update(@PathVariable Long id, @RequestBody Customer customer) {
        try {
            return Result.ok(customerService.update(id, customer));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            customerService.softDelete(id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }
}
