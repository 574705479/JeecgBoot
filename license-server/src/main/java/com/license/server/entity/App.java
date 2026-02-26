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
@Table(name = "app")
public class App {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "app_name", nullable = false, length = 100)
    private String appName;

    @Column(name = "app_id", unique = true, nullable = false, length = 64)
    private String appId;

    @Column(name = "app_secret", nullable = false, length = 128)
    private String appSecret;

    @Column(name = "app_secret_old", length = 128)
    private String appSecretOld;

    @Column(name = "secret_rotate_at")
    private LocalDateTime secretRotateAt;

    @Column(name = "public_key", columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "private_key", columnDefinition = "TEXT")
    private String privateKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "quotas_def", columnDefinition = "JSON")
    private List<Map<String, Object>> quotasDef;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features_def", columnDefinition = "JSON")
    private List<Map<String, Object>> featuresDef;

    @Column(name = "status", columnDefinition = "tinyint")
    private Integer status = 1;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

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
