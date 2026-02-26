package com.license.server.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "license_log")
public class LicenseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "license_id")
    private Long licenseId;

    @Column(name = "app_pk")
    private Long appPk;

    @Column(name = "action", length = 50)
    private String action;

    @Column(name = "client_ip", length = 50)
    private String clientIp;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "result", length = 20)
    private String result;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
