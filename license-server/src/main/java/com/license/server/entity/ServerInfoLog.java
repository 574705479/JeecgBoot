package com.license.server.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "server_info_log")
public class ServerInfoLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "server_id")
    private Long serverId;

    @Column(name = "server", length = 255)
    private String server;

    @Column(name = "task", length = 120)
    private String task;

    @Column(name = "log", columnDefinition = "MEDIUMTEXT")
    private String log;

    @Column(name = "status", columnDefinition = "tinyint")
    private Integer status = 1;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration", length = 60)
    private String duration;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
