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
@Table(name = "docker_service")
public class DockerServiceConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "server_id", nullable = false)
    private Long serverId;

    @Column(name = "service_name", nullable = false, length = 120)
    private String serviceName;

    @Column(name = "container_name", length = 120)
    private String containerName;

    @Column(name = "hostname", length = 120)
    private String hostname;

    @Column(name = "image_name", nullable = false, length = 255)
    private String imageName;

    @Column(name = "current_version", length = 100)
    private String currentVersion;

    @Column(name = "target_version", length = 100)
    private String targetVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ports", columnDefinition = "JSON")
    private List<String> ports;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "environment", columnDefinition = "JSON")
    private List<String> environment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "volumes", columnDefinition = "JSON")
    private List<String> volumes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "depends_on", columnDefinition = "JSON")
    private List<String> dependsOn;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "networks", columnDefinition = "JSON")
    private Map<String, Object> networks;

    @Column(name = "restart_policy", length = 50)
    private String restartPolicy;

    @Column(name = "command", columnDefinition = "TEXT")
    private String command;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extra_config", columnDefinition = "JSON")
    private Map<String, Object> extraConfig;

    @Column(name = "use_params_mode", columnDefinition = "tinyint")
    private Integer useParamsMode = 0;

    @Column(name = "status", columnDefinition = "tinyint")
    private Integer status = 0;

    @Column(name = "del_flag", columnDefinition = "tinyint")
    private Integer delFlag = 0;

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
