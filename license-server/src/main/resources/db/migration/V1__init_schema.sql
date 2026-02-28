CREATE TABLE app (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    app_name         VARCHAR(100) NOT NULL COMMENT '应用名称',
    app_id           VARCHAR(64) UNIQUE NOT NULL COMMENT '应用标识（客户端配置用）',
    app_secret       VARCHAR(128) NOT NULL COMMENT '应用密钥（HMAC签名用）',
    app_secret_old   VARCHAR(128) COMMENT '轮换中的旧密钥（24小时过渡期内有效）',
    secret_rotate_at DATETIME COMMENT '密钥轮换时间',
    public_key       TEXT COMMENT 'RSA公钥PEM',
    private_key      TEXT COMMENT 'RSA私钥PEM（AES-256-GCM加密存储）',
    quotas_def       JSON COMMENT '配额类型定义 [{code,name,type,defaultValue,description}]',
    features_def     JSON COMMENT '功能模块定义 [{code,name,description}]',
    status           TINYINT DEFAULT 1 COMMENT '1启用 0停用',
    remark           TEXT,
    create_time      DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE license_plan (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    app_pk        BIGINT NOT NULL COMMENT '所属应用(关联app.id)',
    plan_name     VARCHAR(100) NOT NULL COMMENT '套餐名称',
    plan_code     VARCHAR(50) NOT NULL COMMENT '套餐标识',
    quotas        JSON COMMENT '配额值 {"max_seats":50,"max_storage_gb":100}',
    features      JSON COMMENT '功能列表 ["airag","lowcode"]',
    sort_order    INT DEFAULT 0 COMMENT '排序',
    status        TINYINT DEFAULT 1,
    del_flag      TINYINT DEFAULT 0 COMMENT '软删除 0正常 1已删除',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_app_plan (app_pk, plan_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE customer (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(200) NOT NULL COMMENT '客户/公司名称',
    contact_name  VARCHAR(100) COMMENT '联系人',
    contact_phone VARCHAR(50),
    contact_email VARCHAR(100),
    del_flag      TINYINT DEFAULT 0 COMMENT '软删除 0正常 1已删除',
    remark        TEXT,
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE license (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    license_key     VARCHAR(64) UNIQUE NOT NULL COMMENT '许可证密钥 LIC-{appShort}-{random}-{checksum}',
    app_pk          BIGINT NOT NULL COMMENT '所属应用(关联app.id)',
    customer_id     BIGINT COMMENT '所属客户',
    plan_id         BIGINT COMMENT '基于的套餐（可为空=自定义）',
    allowed_ips     JSON COMMENT '允许的IP白名单 ["1.2.3.4","5.6.7.0/24"]，空数组=不限IP',
    quotas          JSON COMMENT '配额值 {"max_seats":50}',
    features        JSON COMMENT '授权功能 ["airag","lowcode"]',
    issue_date      DATETIME COMMENT '颁发日期',
    expire_date     DATETIME COMMENT '到期日期',
    activated_at    DATETIME COMMENT '首次激活时间',
    activated_ip    VARCHAR(50) COMMENT '首次激活来源IP',
    last_heartbeat  DATETIME COMMENT '最后心跳时间',
    status          VARCHAR(20) DEFAULT 'INACTIVE' COMMENT 'INACTIVE/ACTIVE/SUSPENDED/REVOKED/EXPIRED',
    del_flag        TINYINT DEFAULT 0 COMMENT '软删除 0正常 1已删除',
    remark          TEXT,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_app_pk (app_pk),
    INDEX idx_customer_id (customer_id),
    INDEX idx_status (status),
    INDEX idx_expire_date (expire_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE license_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    license_id    BIGINT,
    app_pk        BIGINT COMMENT '所属应用(关联app.id)',
    action        VARCHAR(50) COMMENT 'ACTIVATE/HEARTBEAT/DEACTIVATE/REVOKE/EXTEND/SUSPEND',
    client_ip     VARCHAR(50) COMMENT '请求来源IP',
    operator_id   BIGINT COMMENT '操作人ID(管理员操作时记录，系统操作为空)',
    result        VARCHAR(20) COMMENT 'SUCCESS/FAILED',
    message       TEXT,
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_license_id (license_id),
    INDEX idx_app_pk (app_pk),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE admin_user (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    username         VARCHAR(50) UNIQUE NOT NULL,
    password         VARCHAR(200) NOT NULL COMMENT 'BCrypt',
    real_name        VARCHAR(100),
    status           TINYINT DEFAULT 1,
    last_login_time  DATETIME COMMENT '最后登录时间',
    last_login_ip    VARCHAR(50) COMMENT '最后登录IP',
    login_fail_count INT DEFAULT 0 COMMENT '连续登录失败次数',
    locked_until     DATETIME COMMENT '锁定到期时间(失败5次锁定30分钟)',
    create_time      DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- =============================================
-- 初始数据：JeecgBoot 应用 + 套餐模板
-- 注意：RSA密钥对和appSecret请通过管理后台生成/轮换
-- =============================================

INSERT INTO app (id, app_name, app_id, app_secret, quotas_def, features_def, status, remark) VALUES (
    1,
    'JeecgBoot',
    'jeecg-boot',
    'PLACEHOLDER_SECRET_ROTATE_AFTER_FIRST_LOGIN',
    JSON_ARRAY(
        JSON_OBJECT('code','max_seats',          'name','最大在线用户数',  'type','number', 'defaultValue',10, 'description','允许同时在线的用户数，0=不限'),
        JSON_OBJECT('code','max_users',          'name','最大注册用户数',  'type','number', 'defaultValue',50, 'description','系统注册用户总数上限，0=不限'),
        JSON_OBJECT('code','max_cs_agents',      'name','最大客服坐席数',  'type','number', 'defaultValue',5,  'description','客服坐席数量上限，0=不限'),
        JSON_OBJECT('code','max_ai_apps',        'name','最大AI应用数',   'type','number', 'defaultValue',5,  'description','AI应用数量上限，0=不限'),
        JSON_OBJECT('code','max_knowledge_bases', 'name','最大知识库数',   'type','number', 'defaultValue',5,  'description','AI知识库数量上限，0=不限'),
        JSON_OBJECT('code','max_tenants',        'name','最大租户数',     'type','number', 'defaultValue',0,  'description','租户数量上限，0=不限'),
        JSON_OBJECT('code','max_datasources',    'name','最大数据源数',   'type','number', 'defaultValue',1,  'description','数据源数量上限，0=不限'),
        JSON_OBJECT('code','max_storage_gb',     'name','存储空间(GB)',   'type','number', 'defaultValue',10, 'description','文件存储空间上限GB，0=不限'),
        JSON_OBJECT('code','max_api_count',      'name','OpenAPI接口数',  'type','number', 'defaultValue',10, 'description','OpenAPI接口数量上限，0=不限')
    ),
    JSON_ARRAY(
        JSON_OBJECT('code','system',           'name','系统管理',    'description','基础功能（所有版本包含）'),
        JSON_OBJECT('code','lowcode',          'name','低代码引擎',  'description','Online表单/代码生成器'),
        JSON_OBJECT('code','report',           'name','数据报表',    'description','报表/图表/打印设计器'),
        JSON_OBJECT('code','bigscreen',        'name','大屏设计器',  'description','数据可视化大屏'),
        JSON_OBJECT('code','dashboard_design', 'name','仪表盘设计',  'description','自定义仪表盘'),
        JSON_OBJECT('code','workflow',         'name','工作流引擎',  'description','流程设计/审批/任务'),
        JSON_OBJECT('code','airag',            'name','AI应用平台',  'description','AI模型/应用/聊天/流程'),
        JSON_OBJECT('code','ai_knowledge',     'name','AI知识库',    'description','知识库/向量化/RAG'),
        JSON_OBJECT('code','ai_ocr',           'name','AI OCR',     'description','智能文字识别'),
        JSON_OBJECT('code','cs',               'name','在线客服',    'description','座席/会话/访客/工作台'),
        JSON_OBJECT('code','cs_security',      'name','客服安全',    'description','IP黑名单/登录日志'),
        JSON_OBJECT('code','openapi',          'name','OpenAPI',    'description','接口管理/AK-SK认证'),
        JSON_OBJECT('code','monitor',          'name','系统监控',    'description','性能/日志/SQL监控'),
        JSON_OBJECT('code','tenant',           'name','多租户',      'description','租户管理/隔离')
    ),
    1,
    '初始应用，请通过管理后台生成RSA密钥对并轮换appSecret'
);

-- 体验版
INSERT INTO license_plan (app_pk, plan_name, plan_code, quotas, features, sort_order, status) VALUES (
    1, '体验版', 'trial',
    JSON_OBJECT('max_seats',3, 'max_users',10),
    JSON_ARRAY('system', 'lowcode'),
    1, 1
);

-- 基础版
INSERT INTO license_plan (app_pk, plan_name, plan_code, quotas, features, sort_order, status) VALUES (
    1, '基础版', 'basic',
    JSON_OBJECT('max_seats',20, 'max_users',100),
    JSON_ARRAY('system', 'lowcode', 'report', 'monitor'),
    2, 1
);

-- 专业版
INSERT INTO license_plan (app_pk, plan_name, plan_code, quotas, features, sort_order, status) VALUES (
    1, '专业版', 'professional',
    JSON_OBJECT('max_seats',100, 'max_users',500, 'max_cs_agents',30, 'max_ai_apps',50, 'max_knowledge_bases',20),
    JSON_ARRAY('system', 'lowcode', 'report', 'monitor', 'workflow', 'airag', 'ai_knowledge', 'cs', 'openapi'),
    3, 1
);

-- 企业版（全部配额=0表示不限，全部功能）
INSERT INTO license_plan (app_pk, plan_name, plan_code, quotas, features, sort_order, status) VALUES (
    1, '企业版', 'enterprise',
    JSON_OBJECT('max_seats',0, 'max_users',0, 'max_cs_agents',0, 'max_ai_apps',0, 'max_knowledge_bases',0, 'max_tenants',0, 'max_datasources',0, 'max_storage_gb',0, 'max_api_count',0),
    JSON_ARRAY('system', 'lowcode', 'report', 'bigscreen', 'dashboard_design', 'workflow', 'airag', 'ai_knowledge', 'ai_ocr', 'cs', 'cs_security', 'openapi', 'monitor', 'tenant'),
    4, 1
);
