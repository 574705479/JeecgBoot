package com.license.server.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Entity
@Table(name = "license")
public class License {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "license_key", unique = true, nullable = false, length = 64)
    private String licenseKey;

    @Column(name = "app_pk", nullable = false)
    private Long appPk;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "plan_id")
    private Long planId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_ips", columnDefinition = "JSON")
    private List<String> allowedIps;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "quotas", columnDefinition = "JSON")
    private Map<String, Object> quotas;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features", columnDefinition = "JSON")
    private List<String> features;

    @Column(name = "issue_date")
    private LocalDateTime issueDate;

    @Column(name = "expire_date")
    private LocalDateTime expireDate;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "activated_ip", length = 50)
    private String activatedIp;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @Column(name = "callback_url", length = 500)
    private String callbackUrl;

    @Column(name = "status", length = 20)
    private String status = "INACTIVE";

    @Column(name = "del_flag", columnDefinition = "tinyint")
    private Integer delFlag = 0;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "domain_config", columnDefinition = "JSON")
    private Map<String, Object> domainConfig;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
