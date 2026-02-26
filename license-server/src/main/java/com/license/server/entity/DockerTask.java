package com.license.server.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "docker_task")
public class DockerTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_type", nullable = false, length = 50)
    private String taskType;

    @Column(name = "server_id", nullable = false)
    private Long serverId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "service_ids", columnDefinition = "JSON")
    private List<Long> serviceIds;

    @Column(name = "service_names", columnDefinition = "TEXT")
    private String serviceNames;

    @Column(name = "status", columnDefinition = "tinyint")
    private Integer status = 0;

    @Column(name = "total_count")
    private Integer totalCount = 0;

    @Column(name = "success_count")
    private Integer successCount = 0;

    @Column(name = "fail_count")
    private Integer failCount = 0;

    @Column(name = "current_service", length = 120)
    private String currentService;

    @Column(name = "progress_percent")
    private Integer progressPercent = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_detail", columnDefinition = "JSON")
    private List<String> resultDetail;

    @Column(name = "finish_time")
    private LocalDateTime finishTime;

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
