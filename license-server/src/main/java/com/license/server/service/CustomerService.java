package com.license.server.service;

import com.license.server.entity.Customer;
import com.license.server.repository.CustomerRepository;
import com.license.server.repository.LicenseRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final LicenseRepository licenseRepository;

    public Page<Customer> list(int page, int size, String keyword) {
        return customerRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("delFlag"), 0));
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("customerName"), pattern),
                        cb.like(root.get("contactName"), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime")));
    }

    public Customer getById(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("客户不存在"));
    }

    @Transactional
    public Customer create(Customer customer) {
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer update(Long id, Customer updated) {
        Customer customer = getById(id);
        customer.setCustomerName(updated.getCustomerName());
        customer.setContactName(updated.getContactName());
        customer.setContactPhone(updated.getContactPhone());
        customer.setContactEmail(updated.getContactEmail());
        customer.setRemark(updated.getRemark());
        return customerRepository.save(customer);
    }

    @Transactional
    public void softDelete(Long id) {
        long activeLicenses = licenseRepository.countByCustomerIdAndStatusInAndDelFlag(
                id, Arrays.asList("ACTIVE", "INACTIVE"), 0);
        if (activeLicenses > 0) {
            throw new IllegalArgumentException("该客户有活跃许可证，无法删除");
        }
        Customer customer = getById(id);
        customer.setDelFlag(1);
        customerRepository.save(customer);
    }
}
