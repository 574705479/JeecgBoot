# CSE 运维 Runbook

> 客户端加密 + 服务端 KMS 模型（CSE + KMS）端到端图片加密。本文档面向运维 / SRE / 安全 owner。

## 1. 架构速览

| 层 | 内容 |
| --- | --- |
| **KEK** | AES-256，存储在 `cse_kek` 表，首次启动自动生成 `k1` |
| **DEK** | 每文件唯一，AES-256-GCM 加密文件，KEK 包装后存 `oss_file.dek_wrapped_b64` |
| **下行** | `/sys/secure/file/{fid}` 返回密文，`/sys/secure/file/{fid}/key` 返回 HKDF 二次包装的 DEK |
| **前端** | WebCrypto AES-GCM 解密；Electron / 旧浏览器自动 fallback `@noble/ciphers` |

## 2. 启动检查清单

```bash
# 后端启动后
curl -H "X-Access-Token: $TOKEN" http://localhost:8080/jeecg-boot/sys/cse/kek/list

# 验证 KEK 状态：必须有且仅有一个 active
# 默认 kid=k1（首次启动自动生成）

# 前端登录后控制台应看到：
# [CSE] SubtleCrypto + noble fallback 自检通过
```

## 3. 灰度配置

`application-{env}.yml`：

```yaml
jeecg:
  cse:
    enabled: true                 # 总开关；false 则上传走老明文链路
    public-paths: [public/, brand/, captcha/]
    encrypted-paths: [avatar/, chat/, rich/, leave-msg/]
    thumb-width: 256
    dek-cache-seconds: 10
```

灰度顺序建议：
1. `enabled=true` + `encrypted-paths=[chat/]` （仅聊天）
2. `+ rich/` （富文本）
3. `+ avatar/` （头像）
4. 移除 `encrypted-paths`，按 `public-paths` 黑名单全量加密

## 4. KEK 轮换

后端管理菜单：`系统设置 → CSE 密钥管理`。

操作流程：
1. 登录管理员账号，进入 `CSE 密钥管理` 页面
2. 点击 `生成新 KEK` → 输入二次密码 → 后台创建 `pending` 状态的 `k2`
3. `k2` 不会自动接管加密，确认无问题后点击 `激活`，旧 `k1` 自动转为 `archived`
4. **不要删除已归档 KEK**：仍有历史文件依赖它解密
5. 必要时点击 `导出 ZIP` 备份；密钥包以 PBKDF2 + AES-GCM 加密

## 5. 应急回滚

| 场景 | 操作 |
| --- | --- |
| 加密链路异常上传失败 | `jeecg.cse.enabled=false` 重启，所有上传立即回退老明文链路；下行接口仍服务历史 cse 文件 |
| KEK 被误删除 / 损坏 | 从 `导出 ZIP` 备份恢复（KEK 管理页 → 导入），输入二次密码与备份口令 |
| 单个 cse 文件无法解密 | 查 `cse_kek_audit_log` 确认 KEK 未变；若需脱密落盘，跑 `scripts/decrypt-and-republish.js`（见 6 节） |

## 6. decrypt-and-republish 脚本

详见 `scripts/decrypt-and-republish.js`：

```bash
node scripts/decrypt-and-republish.js \
  --base http://localhost:8080/jeecg-boot \
  --token $ADMIN_TOKEN \
  --where "biz_path LIKE 'avatar/%'" \
  --out ./decrypted
```

会按 `oss_file` 表批量拉密文 → 解密 → 落盘到 `--out` 目录。**仅在合规许可下使用**。

## 7. 监控指标 (Micrometer)

| Metric | 含义 |
| --- | --- |
| `cse.encrypt.duration` | 加密耗时 Timer |
| `cse.encrypt.fail{stage=upload|write}` | 加密失败计数 |
| `cse.dek.deny{code=401|403}` | DEK 接口鉴权拒绝 |
| `cse.dek.success` | DEK 派发成功 |
| `cse.dek.seal.duration` | DEK 二次包装耗时 |
| `cse.download.deny{code=401|403}` | 密文下载鉴权拒绝 |

接入 Grafana 仪表板：建议告警 `rate(cse.dek.deny{code="403"}[1m]) > 5`（疑似越权）。

## 8. 常见故障排查

| 现象 | 排查 |
| --- | --- |
| 前端图片显示透明占位 | 浏览器 Network 看 `/sys/secure/file/{fid}` 是否 200；`/key` 是否返回 dekSealedB64 |
| Electron 桌面端解密失败 | 控制台搜 `[CSE] SubtleCrypto`，未通过则确认 `crypto.subtle` 是否在 `file://` 上不可用，已自动 fallback 到 noble |
| 跨租户能看到对方图片 | 立即查 `oss_file.tenant_id` 是否为空，`OssFileMetaService.canRead` 必须命中租户校验 |
| 缩略图打不开 | `thumb_object_key` 字段非空但密文丢失：脚本重跑 `regenerate-thumbnails`（待补） |

## 9. 安全约束

- 禁止上传 SVG（CseUploader 已拒绝）
- 上传图片自动剥离 EXIF / GPS / IPTC
- 401 / 退出登录立即清空前端 IndexedDB 与 DEK 缓存（避免多账号共享终端的残留）
- KEK 操作必须二次密码 + 全量审计日志（`cse_kek_audit_log`）
