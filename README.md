在线客服系统（KeFu）
===============

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](./LICENSE)
[![SpringBoot](https://img.shields.io/badge/SpringBoot-3.5.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.x-42b883.svg)](https://vuejs.org/)
[![Vite](https://img.shields.io/badge/Vite-6-blueviolet.svg)](https://vitejs.dev/)
[![JDK](https://img.shields.io/badge/JDK-17%2B-red.svg)](https://openjdk.org/)


项目介绍
-----------------------------------

本项目是一套基于 **JeecgBoot 3.9** 二次开发的、可商用的 **AI 在线客服系统（KeFu）**，定位于"全渠道接入 + 人工/AI 协同 + 私有化部署"。系统在 JeecgBoot 低代码框架基础上，新增了完整的客服模块（`org.jeecg.modules.airag.cs`），支持网页挂件、独立链接、二维码、Electron 桌面端等多种接入方式，并对接主流大模型（DeepSeek、ChatGPT、千问、Ollama）实现 AI 自动回复、AI 辅助回复、智能 FAQ 等能力。

适用场景：电商售前售后、SaaS 在线咨询、企业官网客服、私域流量沉淀、内部 IT/HR 服务台等。


核心特性
-----------------------------------

- **多端接入**：访客端 H5（响应式 + 移动适配）、独立挂件 SDK、二维码扫码、网页 iframe 嵌入、Electron 桌面客户端（多窗口/托盘/系统通知）。
- **人机协同**：会话支持 `AI 自动 / 人工 / AI 辅助` 三种模式动态切换；AI 给出建议草稿，客服一键确认即可发送，兼顾效率与准确性。
- **会话调度**：多客服自动分配、负载均衡、转接、邀请协作（多人同会话）、坐席数量配额、客服超时未回复自动通知访客。
- **FAQ 知识库**：支持无限层级嵌套问答、关键词触发、点击导航、富文本答案，可作为入口引导减轻人工压力。
- **快捷回复**：支持文本、图片、文件、富文本四类，按客服私有 / 团队公共两类管理，支持快捷键秒发。
- **客户管理（CRM 轻量版）**：访客画像、星标客户、自定义字段、留言管理、历史对话回溯、IP 归属地与设备识别。
- **数据安全**：消息端到端混合加密（RSA-2048 密钥交换 + AES-256 数据加密 + HMAC-SHA256 完整性校验）、敏感词拦截、访客/IP 黑名单、客服 IP 白名单、登录日志审计。
- **统计分析**：客服对话量、平均响应时长、首响时长、及时回复率、出勤记录、访客地域分布、对话效率排行等多维度报表。
- **品牌定制**：聊天窗口主题、Logo、欢迎语、FAQ 引导文案、问候语、自动消息均可在线配置；支持多品牌/多租户。
- **授权与配额**：内置授权客户端（License Client），与独立 License Server 联动，支持坐席、知识库、应用等多维配额校验，适合 SaaS 化销售。


技术架构
-----------------------------------

### 后端

| 项 | 选型 |
|---|---|
| 语言 / JDK | Java 17（兼容 JDK 21/24） |
| 框架 | Spring Boot 3.5.5 + Spring Cloud Alibaba 2023.0.3.3（单体/微服务双模式） |
| ORM | MyBatis-Plus 3.5.12 + Druid 1.2.24 |
| 安全 | Apache Shiro 2.0.4 + JWT 4.5.0 |
| 实时通信 | Spring WebSocket（端点 `/ws/cs`） |
| 大模型 | LangChain4j，支持 DeepSeek / ChatGPT / 千问 / Ollama 等 |
| 任务调度 | Quartz / XXL-Job |
| 微服务 | Nacos、Gateway、Sentinel、Skywalking |

### 前端

| 项 | 选型 |
|---|---|
| 框架 | Vue 3.x + TypeScript + Vite 6 |
| UI | Ant Design Vue 4 |
| 状态管理 | Pinia |
| 桌面端 | Electron（多窗口隔离 + 自动更新） |
| 构建特性 | MPA 双入口（管理端 `index.html` + 访客端 `visitor.html`，访客端首屏 < 5KB） |

### 存储与中间件

| 用途 | 选型 |
|---|---|
| 业务主库 | MySQL 8.4（默认端口 13306，库名 `jeecg-boot`） |
| 聊天消息 | MongoDB 8.2（端口 27017，库名 `jeecg`，集合 `chat_messages`） |
| 缓存 / 分布式锁 | Redis 8.0（端口 6379） + Redisson |
| 向量库（AI RAG） | PostgreSQL + pgvector（端口 5432） |
| 对象存储 | MinIO / 阿里 OSS / 本地存储（可切换） |


功能模块清单
-----------------------------------

```
├─ 客服工作台（workbench）
│  ├─ 实时会话列表（未分配 / 我的 / 同事 / 已结束）
│  ├─ 多窗口聊天 + 富文本 + 图片 + 文件 + 表情
│  ├─ AI 建议消息（确认即发）+ 引用 + 撤回
│  ├─ 邀请协作 / 转接 / 结束会话
│  ├─ 桌面通知（Web Audio + Notification + Electron 托盘闪烁）
│  └─ 断线自动重连（带倒计时 Banner）
├─ 访客端（userChat / 独立 H5）
│  ├─ 移动端自适应（dvh + safe-area）
│  ├─ FAQ 智能导航
│  ├─ 留言（离线时自动转留言）
│  ├─ 接入「请求人工客服」流程
│  └─ 端到端加密通信
├─ 客服管理
│  ├─ 客服账号（绑定 sys_user，独立头像/昵称/欢迎语）
│  ├─ 子客服（团队管理者）+ 菜单授权
│  ├─ 客服 IP 白名单、登录日志、状态日志
│  └─ 同账号多端登录互踢、强制下线
├─ 会话与消息
│  ├─ 会话记录、回放、撤回
│  ├─ 消息类型：文本 / 图片 / 文件 / 富文本 / 系统消息 / FAQ
│  └─ 三层消息模型：CsMessage(DTO) → WS 传输 → MongoDB 持久化
├─ 客户与营销
│  ├─ 访客画像、自定义字段、标签、星标
│  ├─ 留言箱（含回复/撤回/记录）
│  └─ 自动消息 / 主动邀请对话
├─ AI 能力
│  ├─ AI 应用（基于 airag-app 模块）
│  ├─ 知识库 RAG（基于 airag-llm 模块 + pgvector）
│  ├─ FAQ 关键词触发
│  └─ AI 辅助回复 / AI 自动回复
├─ 安全防护
│  ├─ 敏感词拦截（实时双向校验）
│  ├─ IP 黑名单 + WebSocket 实时踢人
│  ├─ 访客黑名单
│  └─ 接入域名白名单
├─ 统计分析
│  ├─ 客服对话统计
│  ├─ 客服对话效率（响应时长、及时回复率）
│  ├─ 出勤记录（在线/隐身/忙碌时长）
│  └─ 访客区域分布
├─ 品牌与配置
│  ├─ 品牌 Logo / 主题色
│  ├─ 聊天窗口配置（欢迎语、FAQ 头文案、占位符等）
│  ├─ 接入域名管理
│  └─ 接入示例代码（支持 Electron 域名自适应）
├─ 数据治理
│  ├─ 定时数据清理任务（对话/日志/缓存分别配置保留天数）
│  └─ 清理日志审计
└─ 系统集成
   ├─ License 授权客户端（与独立 License Server 联动）
   ├─ 多租户 SaaS
   └─ 单点登录（CAS）
```


数据库
-----------------------------------

业务表全部以 `cs_` 为前缀，核心表如下：

| 表名 | 说明 |
|---|---|
| `cs_agent` | 客服账号（关联 `sys_user`），含头像/昵称/最大会话数/角色 |
| `cs_conversation` | 会话主表，含状态、回复模式、统计字段（首响秒数、消息数等） |
| `cs_visitor` | 访客主表，含画像、星标、地理位置、设备 |
| `cs_collaborator` | 多人协作（主/协/临三种角色） |
| `cs_quick_reply` / `cs_quick_reply_category` | 快捷回复及分类 |
| `cs_brand_config` | 品牌配置 |
| `cs_domain_config` | 接入域名配置 |
| `cs_global_config` | 全局配置（敏感词、超时通知、客服分配策略等） |
| `cs_leave_message` | 留言箱 |
| `cs_ip_blacklist` / `cs_visitor_blacklist` / `cs_agent_ip_whitelist` | 安全相关 |
| `cs_agent_login_log` / `cs_agent_status_log` | 审计日志 |
| `cs_ip_geo_cache` / `cs_file_hash` / `cs_cleanup_log` | 辅助缓存与日志 |

聊天消息存储在 **MongoDB** 集合 `chat_messages` 中（实体 `ChatMessage`）。

数据库初始化脚本：`jeecg-boot/db/jeecgboot.sql`，增量脚本通过 **Flyway** 自动执行（位于 `jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/`）。


快速启动
-----------------------------------

### 方式一：Docker Compose 一键启动（推荐）

```bash
# 在项目根目录
docker compose up -d
```

启动后服务清单：

| 容器 | 端口 | 用途 |
|---|---|---|
| `kefu-mysql` | 13306 | MySQL 8.4 |
| `kefu-redis` | 6379 | Redis 8.0 |
| `kefu-pgvector` | 5432 | PostgreSQL + pgvector |
| `kefu-mongodb` | 27017 | MongoDB 8.2 |
| `kefu-system` | 8080 | 后端服务 |
| `kefu-vue` | 3111 | 前端 Nginx |

访问 [http://localhost:3111](http://localhost:3111)，默认账号 `admin / 123456`。

### 方式二：本地开发模式

#### 1. 启动依赖中间件

只启动 MySQL / Redis / MongoDB / pgvector 四个数据相关容器：

```bash
docker compose up -d kefu-mysql kefu-redis kefu-mongodb kefu-pgvector
```

#### 2. 启动后端

```bash
# 在 jeecg-boot 根目录使用 Reactor 一次拉起，避免子模块用到 ~/.m2 旧 JAR
cd jeecg-boot
mvn clean spring-boot:run -pl jeecg-module-system/jeecg-system-start -am -T 1C
```

后端启动后：
- 端口：`8080`，上下文路径：`/jeecg-boot`
- 启动类：`org.jeecg.JeecgSystemApplication`
- 日志目录：`jeecg-boot/jeecg-module-system/logs/`

#### 3. 启动前端

```bash
cd jeecgboot-vue3
pnpm install            # 仅首次或依赖变更
npm run dev
```

前端启动后：
- 端口：`3100`
- 代理：`/jeecgboot` → `http://localhost:8080/jeecg-boot`
- 管理端：[http://localhost:3100](http://localhost:3100)
- 访客端：[http://localhost:3100/cs/userChat?agentId=xxx](http://localhost:3100/cs/userChat)

#### 4. （可选）启动 Electron 桌面客户端

```bash
cd jeecgboot-vue3
npm run electron:dev
```


接入访客端
-----------------------------------

### 1. 链接接入

```
http://your-domain/cs/userChat?agentId=xxx
```

可在「客服系统 → 接入设置」页面动态选择客服并复制链接 / 二维码。

### 2. 网页挂件

将以下脚本嵌入到任意网页：

```html
<script src="http://your-domain/widget.js" data-agent-id="xxx"></script>
```

详细文档见管理后台「客服系统 → 接入设置」。

### 3. Electron 桌面端

桌面端首次启动需输入授权码（License Key），由 License Server 颁发；之后会自动竞速选择最优 API 域名，2 小时刷新一次授权状态。


环境与版本要求
-----------------------------------

| 项 | 最低版本 | 推荐版本 |
|---|---|---|
| JDK | 17 | 17 / 21 |
| Maven | 3.6 | 3.9+ |
| Node.js | 20.19 | 22.12+ |
| pnpm | 9 | 10+ |
| MySQL | 5.7 | 8.4 |
| Redis | 6 | 8.0 |
| MongoDB | 6 | 8.2 |
| PostgreSQL | 14（带 pgvector） | 16 |


目录结构
-----------------------------------

```
JeecgBoot/
├── jeecg-boot/                                       # 后端
│   ├── jeecg-boot-base-core/                         # 基础核心 + License 客户端
│   ├── jeecg-module-system/                          # 系统管理（用户/角色/菜单）
│   │   └── jeecg-system-start/                       # 主启动入口
│   ├── jeecg-boot-module/
│   │   └── jeecg-boot-module-airag/                  # AI + 客服核心模块
│   │       └── src/main/java/org/jeecg/modules/airag/
│   │           ├── app/                              # AI 应用
│   │           ├── chat/                             # AI 聊天 + MongoDB
│   │           ├── llm/                              # 知识库/模型/MCP
│   │           ├── ocr/                              # OCR
│   │           └── cs/                               # 在线客服 ★
│   │               ├── controller/                   # CsAgent / CsConversation / CsMessage 等
│   │               ├── service/ + service/impl/      # 业务实现
│   │               ├── websocket/                    # CsWebSocketHandler / 拦截器
│   │               ├── task/                         # 定时任务（超时检查 / 数据清理）
│   │               ├── entity/ mapper/ vo/           # 数据层
│   │               └── config/ constant/ util/       # 加密、Redis Key、IP 等共享工具
│   └── db/                                           # 初始化 SQL
├── jeecgboot-vue3/                                   # 前端
│   ├── src/views/super/airag/cs/                     # 客服功能页面 ★
│   │   ├── workbench/                                # 客服工作台（5000+ 行）
│   │   ├── userChat/                                 # 访客端
│   │   ├── agent/ subAgent/                          # 客服管理
│   │   ├── conversation/ messageBoard/               # 会话与留言
│   │   ├── statistics/                               # 统计分析（4 个子页面）
│   │   ├── quickReply/ brand/ chatWindowSettings/    # 快捷回复 / 品牌 / 聊天窗口
│   │   ├── security/                                 # IP 黑/白名单、登录日志
│   │   └── domainConfig/ accessExample/ dataCleanup/ # 域名 / 接入示例 / 数据清理
│   └── electron/                                     # 桌面端主进程
├── docker-compose.yml                                # 本地一键编排
└── LICENSE                                           # Apache License 2.0
```


调试与运维
-----------------------------------

### 后端日志

- 控制台日志：终端启动输出
- 文件日志：`jeecg-boot/jeecg-module-system/logs/jeecgboot-{yyyy-MM-dd}.{i}.log`
- 错误日志：`error-log.html`

### 浏览器端调试

- WebSocket 调试：浏览器 DevTools → Network → WS，订阅事件如 `new_conversation` / `message` / `agent_connected` / `force_offline` 等。
- 加密消息：管理端开发者工具中可见 `$ENC$v1$...` 形式的加密消息体，会自动通过 `csEncrypt.ts` 解密渲染。

### 常见配置

- WebSocket 跨域：`jeecg.cs.ws.allowed-origins=https://a,https://b`
- 数据保留策略：管理后台「数据清理」页配置对话/日志/缓存的保留天数
- 客服分配策略：管理后台「会话分配」页配置（最大会话数、超时通知、人工接入按钮等）


开源协议
-----------------------------------

本项目基于 [Apache License 2.0](./LICENSE) 协议开源，二次分发请保留 LICENSE 文件与版权声明。

底层框架 [JeecgBoot](https://github.com/jeecgboot/JeecgBoot) 同样基于 Apache License 2.0 协议，特此致谢。

