package com.license.server.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "server_info")
public class ServerInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "server_name", nullable = false, length = 120)
    private String serverName;

    @Column(name = "server_type", columnDefinition = "tinyint")
    private Integer serverType = 1;

    @Column(name = "connection_type", columnDefinition = "tinyint")
    private Integer connectionType = 1;

    @Column(name = "cloud_vendor", length = 100)
    private String cloudVendor;

    @Column(name = "ip", nullable = false, length = 255)
    private String ip;

    @Column(name = "ssh_port")
    private Integer sshPort = 22;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "private_key", columnDefinition = "TEXT")
    private String privateKey;

    @Column(name = "private_key_path", length = 500)
    private String privateKeyPath;

    @Column(name = "public_key", columnDefinition = "tinyint")
    private Integer publicKey = 0;

    @Column(name = "public_key_value", columnDefinition = "TEXT")
    private String publicKeyValue;

    @Column(name = "ms_user", length = 100)
    private String msUser;

    @Column(name = "ms_pwd", length = 255)
    private String msPwd;

    @Column(name = "ms_port")
    private Integer msPort = 3306;

    @Column(name = "database_name", length = 120)
    private String databaseName = "im_platform";

    @Column(name = "docker_api_port")
    private Integer dockerApiPort;

    @Column(name = "sp_link", columnDefinition = "TEXT")
    private String spLink;

    @Column(name = "status", columnDefinition = "tinyint")
    private Integer status = 0;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

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
