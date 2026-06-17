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


系统组成
-----------------------------------

整套系统由 **4 个子项目 + 1 套共享中间件** 组成，可独立开发、独立部署、独立升级：

| 子项目 | 路径 | 角色 | 默认端口 | 镜像名 |
|---|---|---|---|---|
| 客服后端 | [`jeecg-boot/`](./jeecg-boot/) | 主业务后端：客服模块、AI 引擎、WebSocket 端点（`/ws/cs`）、定时任务 | `8080` | `kefu-system` |
| 客服管理前端 | [`jeecgboot-vue3/`](./jeecgboot-vue3/) | 管理端 + 客服工作台 + Electron 桌面端宿主，主前端 SPA | `3100`（dev）/ `80`（prod） | `kefu-vue` |
| 访客端独立项目 | [`jeecgboot-vue3-visitor/`](./jeecgboot-vue3-visitor/) | 极简快加载访客端 SPA（HTML 内联骨架屏，端到端加密），build 后注入主前端 `dist/cs/userChat/` | 与主前端共用 | 无（产物合并发布） |
| 授权服务 | [`license-server/`](./license-server/) | 独立 License 颁发 / 验证 / 管理后台（含独立 Vue 3 前端），与客服系统通过 HTTP 联动 | 后端 `8090`（dev）/ `8180`（docker），前端 `8181`（docker） | `license-server` + `license-frontend` |
| 共享中间件 | — | MySQL 8.4 / Redis 8.0 / MongoDB 8.2 / pgvector / MinIO | 见快速启动表格 | 公共镜像 |

**项目编排：**

- 客服核心栈：根目录 [`docker-compose.yml`](./docker-compose.yml)（开发用）+ [`docker-compose.kefu-1.0.0.yml`](./docker-compose.kefu-1.0.0.yml)（生产环境模板，挂载持久化卷）
- 授权服务：[`license-server/docker-compose.yml`](./license-server/docker-compose.yml)（开发用）+ [`license-server/docker-compose.prod.yml`](./license-server/docker-compose.prod.yml)（生产用）
- 访客端构建：由 [`jeecgboot-vue3/build/script/buildVisitor.ts`](./jeecgboot-vue3/build/script/buildVisitor.ts) 调起，`pnpm build:nocheck` 生成产物后整体拷贝到主前端 `dist/cs/userChat/`，部署时由 nginx 命中真实文件返回
- 数据持久化：根目录 `/www/docker-data/kefu/{mysql,redis,mongodb,pgvector,upload,webapp,license-cache}`（见生产 compose）


核心特性
-----------------------------------

- **多端接入**：访客端 H5（响应式 + 移动适配）、独立挂件 SDK、二维码扫码、网页 iframe 嵌入、Electron 桌面客户端（多窗口/托盘/系统通知）。
- **人机协同**：会话支持 `AI 自动 / 人工 / AI 辅助` 三种模式动态切换；AI 给出建议草稿，客服一键确认即可发送，兼顾效率与准确性。
- **会话调度**：多客服自动分配、负载均衡、转接、邀请协作（多人同会话）、坐席数量配额、客服超时未回复自动通知访客。
- **FAQ 知识库**：支持无限层级嵌套问答、关键词触发、点击导航、富文本答案，可作为入口引导减轻人工压力。
- **快捷回复**：支持文本、图片、文件、富文本四类，按客服私有 / 团队公共两类管理，支持快捷键秒发。
- **客户管理（CRM 轻量版）**：访客画像、星标客户、自定义字段、留言管理、历史对话回溯、IP 归属地与设备识别。
- **数据安全**：消息传输层 / 存储层双层 AES-256 对称加密（密钥与后端 `jeecg.cs.crypto` 配置一致）；图片 / 文件端到端加密（CSE：按文件 HKDF-SHA256 派生密钥 + AES-256-GCM，浏览器 WebCrypto，`@noble` 兜底）、敏感词拦截、访客 / IP 黑名单、客服 IP 白名单、登录日志审计。
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

### 访客端（独立子项目）

| 项 | 选型 |
|---|---|
| 框架 | Vue 3 + TypeScript + Vite 6（`jeecgboot-vue3-visitor` 子项目） |
| 路由 | `vue-router` Hash 模式，单路由 `/` 渲染 `ChatMain.vue`，无鉴权守卫 |
| 加密 | `@noble/ciphers` + `@noble/hashes`（端到端混合加密，浏览器原生 WebCrypto fallback） |
| 渲染 | `markdown-it` + `dompurify`（消息富文本安全渲染）、`@tanstack/vue-virtual`（消息流虚拟滚动） |
| 启动体感 | HTML 内联骨架屏（首字节即可看到完整聊天界面骨架，零外部依赖） |
| 输出 | build 后产物合并到 `jeecgboot-vue3/dist/cs/userChat/`，与主前端镜像 `kefu-vue` 同包发布 |

### 授权服务（License Server）

独立项目位于 [`license-server/`](./license-server/)，与客服系统解耦，可单独部署、独立销售。

| 项 | 选型 |
|---|---|
| 后端 | Spring Boot + JPA + Hibernate + Flyway，独立 MySQL 库 `license_server` |
| 安全 | JWT（access 2h / refresh 7d）+ HMAC 主密钥（`LICENSE_MASTER_KEY`，HKDF 二次派生） |
| 限流 | per IP / per License Key 速率限制（激活、心跳验证均独立配额） |
| 前端 | Vue 3 + TypeScript + Vite + Pinia，集成 wangEditor 富文本与 xterm 终端模拟器 |
| 业务能力 | License 颁发、激活、续期、吊销；客户档案；套餐计划；Docker 服务下发；服务器信息回传；操作审计日志 |
| 客服侧对接 | `jeecg-boot-base-core/license-client` 模块，2h 心跳一次，支持坐席 / 知识库 / 应用多维配额校验 |


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

数据库结构由 **Flyway** 在应用启动时自动创建并升级，无需手动导入 SQL。迁移脚本位于 `jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/`（基线 `V1_0_0__init_schema.sql` + `V1_0_1`～`V1_1_7` 客服功能增量脚本）。


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

#### 5. （可选）启动授权服务 License Server

授权服务是独立项目，与客服系统通过 HTTP 联动。客服侧默认通过 `LICENSE_SERVER_URL` 环境变量指定授权地址（生产配置见 [`docker-compose.kefu-1.0.0.yml`](./docker-compose.kefu-1.0.0.yml) 第 107 行）。

**方式一：Docker Compose 一键启动（推荐）**

```bash
cd license-server
docker compose up -d
```

启动后服务清单：

| 容器 | 端口 | 用途 |
|---|---|---|
| `license-mysql` | 13308 | 独立 MySQL（库名 `license_server`） |
| `license-server` | 8180 | 授权后端 |
| `license-frontend` | 8181 | 授权管理后台前端 |

**方式二：本地开发模式**

```bash
# 启动后端（端口 8090）
cd license-server
mvn spring-boot:run

# 启动前端（端口由 Vite 默认 5173 / 5174 等自动分配）
cd license-server/frontend
pnpm install
pnpm dev
```

> **首次部署提醒**：必须配置 `LICENSE_MASTER_KEY` 环境变量（HMAC 主密钥），否则授权服务无法启动；生产环境建议使用 64 字符随机十六进制字符串，妥善保管不可泄露。


### 5.（可选）独立调试访客端

主仓库 build 时会自动通过 `jeecgboot-vue3/build/script/buildVisitor.ts` 唤起访客端构建。如果想单独调试访客端 UI：

```bash
cd jeecgboot-vue3-visitor
pnpm install
pnpm dev          # 默认端口 5173，独立调试
pnpm build:nocheck  # 仅产出 dist，跳过 vue-tsc 类型检查
```

> 访客端默认通过 hash 路由渲染单页 `ChatMain.vue`，对接的 WebSocket / API 走主前端 vite 代理或 nginx 反向代理转发到 `kefu-system:8080/jeecg-boot`。


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
├── jeecg-boot/                                       # 客服后端（kefu-system 镜像）
│   ├── jeecg-boot-base-core/                         # 基础核心 + License 客户端
│   ├── jeecg-module-system/                          # 系统管理（用户/角色/菜单）
│   │   └── jeecg-system-start/                       # 主启动入口（Dockerfile 在此）
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
├── jeecgboot-vue3/                                   # 客服管理前端（kefu-vue 镜像）
│   ├── src/views/super/airag/cs/                     # 客服功能页面 ★
│   │   ├── workbench/                                # 客服工作台（拆分后 ~6000 行 + 9 子组件）
│   │   ├── userChat/                                 # 旧版访客端（保留兼容入口）
│   │   ├── agent/ subAgent/                          # 客服管理
│   │   ├── conversation/ messageBoard/               # 会话与留言
│   │   ├── statistics/                               # 统计分析（4 个子页面）
│   │   ├── quickReply/ brand/ chatWindowSettings/    # 快捷回复 / 品牌 / 聊天窗口
│   │   ├── security/                                 # IP 黑/白名单、登录日志
│   │   └── domainConfig/ accessExample/ dataCleanup/ # 域名 / 接入示例 / 数据清理
│   ├── build/script/buildVisitor.ts                  # 访客端构建脚本（产物合并到 dist/cs/userChat）
│   └── electron/                                     # 桌面端主进程（多窗口 + 托盘 + 自动更新）
├── jeecgboot-vue3-visitor/                           # 访客端独立子项目 ★
│   ├── index.html                                    # HTML 内联骨架屏（首字节即可见聊天骨架）
│   ├── src/
│   │   ├── views/ChatMain.vue                        # 唯一路由 /，访客端 SPA 主组件
│   │   ├── router/index.ts                           # 极简路由（hash 模式 + 兜底 redirect）
│   │   └── crypto/                                   # 端到端混合加密（noble-ciphers / noble-hashes）
│   └── package.json                                  # 独立依赖（无 ant-design-vue 等管理端依赖，体积小）
├── license-server/                                   # 授权服务（独立项目）★
│   ├── src/main/java/com/license/server/
│   │   └── controller/
│   │       ├── api/                                  # 对外接口（PlanPublic / LicenseApi）
│   │       └── admin/                                # 管理后台接口（Auth/Customer/License/Plan/App/...）
│   ├── src/main/resources/
│   │   ├── application.yml                           # 端口 8090，独立 MySQL 库
│   │   └── db/migration/                             # Flyway 增量迁移
│   ├── frontend/                                     # 授权管理后台前端（Vue 3 + Pinia + wangEditor）
│   ├── Dockerfile                                    # license-server 镜像
│   ├── docker-compose.yml                            # 本地一键启动（含 mysql + server + frontend）
│   └── docker-compose.prod.yml                       # 生产编排
├── docs/                                             # 系统文档
│   ├── cse-developer-guide.md                        # CSE 客户端加密 - 开发者指南
│   ├── cse-runbook.md                                # CSE - 运维 Runbook
│   ├── cse-tenant-admin-guide.md                     # CSE - 租户管理员使用指南
│   └── cse-compliance-checklist.md                   # CSE - 等保/PIPL/GDPR 合规自查
├── docker-compose.yml                                # 客服核心栈（开发用）
├── docker-compose.kefu-1.0.0.yml                     # 客服核心栈（生产模板，含挂载点 /www/docker-data/kefu/...）
├── DESIGN.md                                         # 系统设计文档
└── LICENSE                                           # Apache License 2.0
```


项目文档
-----------------------------------

除本 README 外，[`docs/`](./docs/) 目录下还提供面向不同角色的专题文档：

| 文档 | 受众 | 说明 |
|---|---|---|
| [`docs/cse-developer-guide.md`](./docs/cse-developer-guide.md) | 开发 | 在 JeecgBoot 上接入端到端文件加密（CSE）的代码集成指南，含 `IStorageUploadService` 用法、加密路径白名单 |
| [`docs/cse-runbook.md`](./docs/cse-runbook.md) | 运维 / SRE / 安全 | CSE + KMS 端到端加密架构速览、KEK / DEK 密钥管理、启动检查清单、故障排查 |
| [`docs/cse-tenant-admin-guide.md`](./docs/cse-tenant-admin-guide.md) | 租户管理员 | 纯 GUI 操作手册：在管理后台开启 / 监控 / 自救加密功能 |
| [`docs/cse-compliance-checklist.md`](./docs/cse-compliance-checklist.md) | 安全 / 法务 | 等保 2.0 三级 / PIPL / GDPR / 商用密码法的关键条款映射表 |
| [`DESIGN.md`](./DESIGN.md) | 架构 / 全员 | 系统总体设计文档 |
| [`jeecgboot-vue3/PWA-README.md`](./jeecgboot-vue3/PWA-README.md) | 前端 | 主前端 PWA 渐进式离线缓存配置说明 |
| [`jeecgboot-vue3/electron.md`](./jeecgboot-vue3/electron.md) | 客户端 | Electron 桌面客户端打包与多窗口架构说明 |
| [`license-server/frontend/README.md`](./license-server/frontend/README.md) | 授权前端 | 授权管理后台前端开发模板说明 |
| [`jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/README.md`](./jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/README.md) | DBA | Flyway 增量 SQL 命名规范与迁移指南 |


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


近期版本
-----------------------------------

| 版本 | 日期 | 类型 | 关键变更 |
|---|---|---|---|
| `kefu-system 1.2.24` + `kefu-vue 1.2.72` | 2026-04-29 | Bug 修复 | 修复访客重新打开已结束会话时，客服侧会突然冒出该会话并弹"新访客接入"提示的问题。后端 WS 不再为 `status=2` 的会话广播 `new_conversation/user_online`；前端兜底过滤 `extra.status===2` 的事件。 |
| `kefu-vue 1.2.71` | 2026-04-28 | 性能 / 体验 | 优化客服工作台刷新机制：客服发消息后不再触发整列表/消息流重载、滚动位置稳定；WS 健康时兜底轮询间隔从 5s 拉长到 30s（断网降级回 5s）；`/cs/conversation/list` 改为按 ID 增量 merge，避免会话列表 DOM 整体重建；`/cs/conversation/stats` 防抖窗口 500ms → 2000ms。预期 list/stats 接口频率下降约 6 倍。仅前端，后端未动。 |
| `kefu-vue 1.2.70` | 2026-04-28 | 重构 | 客服工作台前端适度拆分：`workbench/index.vue` 7705 行 → 5986 行，析出 9 个子组件（`CsAgentBar` / `CsWsStatusBanner` / `CsChatHeader` / `CsChatEmptyState` / `CsWorkbenchSettingsDrawer` / `CsTransferConversationModal` / `CsBlacklistModal` / `CsVisitorFieldEditModal` / `CsMediaPreviewModals`）+ 2 个 composables（`useCsWorkbenchTheme` / `useCsMessageMedia`）+ 2 个工具模块（`theme/presets.ts` / `render/csMessageRender.ts`），父级保留消息流 / 会话列表 / 输入区 / WS 核心。 |

> **部署方式**：在 1Panel 修改对应容器版本号后点「同步状态」即可。  
> **兼容性**：1.2.72/1.2.24 前后端独立升级（旧前端 + 新后端、新前端 + 旧后端均兼容）；1.2.71、1.2.70 仅前端，后端无需重启。  
> **回滚**：把容器版本号改回上一版本即可，无数据库迁移依赖。


开源协议
-----------------------------------

本项目基于 [Apache License 2.0](./LICENSE) 协议开源，二次分发请保留 LICENSE 文件与版权声明。

底层框架 [JeecgBoot](https://github.com/jeecgboot/JeecgBoot) 同样基于 Apache License 2.0 协议，特此致谢。

