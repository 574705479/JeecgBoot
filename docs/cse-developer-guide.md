# CSE 开发者指南

> 面向需要在 JeecgBoot 上接入端到端文件加密的开发同学。

## 1. 我什么时候需要用 CSE？

| 场景 | 是否走 CSE | 怎么做 |
| --- | --- | --- |
| 用户头像 / 聊天图片 / 富文本图片 | ✅ | 把上传 bizPath 设到 `encrypted-paths` 名单中即可 |
| 公开 Logo / 验证码 / 静态资源 | ❌ | 加到 `public-paths` 黑名单 |
| Excel / PDF / 任意二进制 | ✅（默认） | 走 `IStorageUploadService.upload(...)` 即可，自动加密 |
| 系统配置图标 / 安装包下载 | ❌ | 仍走 `/sys/common/static/**`（明文，老入口） |

## 2. 上传

任意 Spring Controller 内：

```java
@Autowired
private IStorageUploadService storageUploadService;

String url = storageUploadService.upload(file, "rich/" + userId);
// 命中加密链路时返回 cse://{fid}，否则返回 http(s) URL
```

字节流：

```java
String url = storageUploadService.uploadOnlineImage(bytes, basePath, "rich/" + userId);
```

> 不要再直接用 `MinioUtil` / `OssBootUtil`，必须经 `IStorageUploadService`，否则跳过加密。

## 3. 前端展示

### 3.1 头像 / 普通 `<img>`

直接用 `<CseImage>`：

```vue
<template>
  <CseImage :src="user.avatar" :width="48" :preview="true" />
</template>

<script setup lang="ts">
import CseImage from '/@/components/CseImage/index.vue';
</script>
```

或者在 setup 里用 composable：

```ts
import { withImageCacheAsync } from '/@/utils/file/imageCache';

const avatar = ref('');
watchEffect(async () => {
  avatar.value = await withImageCacheAsync(user.avatar);
});
```

### 3.2 富文本

`MarkdownViewer` 与 `Tinymce/Editor` 已自动支持 `cse://` 图片：
- MarkdownViewer：内容里的 `<img src="cse://...">` 会被替换为占位图，进入视口后异步解密
- TinyMCE：编辑时 `cse://` 解密为 blob URL 显示，保存时再转换回 `cse://{fid}` 持久化

### 3.3 下载 / 预览

```ts
import { downloadByUrl } from '/@/utils/file/downloadCse';

await downloadByUrl(record.url, record.fileName); // 自动识别 cse:// 与 http
```

## 4. 配置项

`application.yml`：

```yaml
jeecg:
  cse:
    enabled: true
    public-paths: [public/, brand/, captcha/]   # 黑名单：公开资源不加密
    encrypted-paths: []                         # 白名单：留空则按 enabled 全量加密
    thumb-width: 256                            # 缩略图最大边像素
    dek-cache-seconds: 10                       # 服务端 DEK 短缓存秒数
```

## 5. URL / 协议约定

- `cse://{fid}` — 8/16 字节随机 fid（`OssFileMetaService.genFileId()`），存于 `oss_file.url`
- `oss_file.object_key` — 形如 `avatar/cse/{fid}.cse`，桶里固定以 `.cse` 结尾
- 缩略图 `object_key` — `avatar/cse/{fid}.thumb.cse`
- 下行接口 — `/sys/secure/file/{fid}` 与 `/sys/secure/file/{fid}/key`

## 6. 加解密原理

### 6.1 加密（服务端，CseUploader）

```
plaintext ──AES-256-GCM(DEK, IV)──▶ ciphertext  → 写入 OSS
            │                                         ↑
            └─ DEK ──AES-256-GCM(KEK, IV')──▶ wrappedDek → 写入 oss_file.dek_wrapped_b64
```

### 6.2 解密（前端，cseDecrypt.ts）

```
GET /sys/secure/file/{fid}/key
  → server: dek = unwrap(dek_wrapped_b64, KEK)
            sealedDek = AES-GCM(HKDF(token, "cse:" + fid, kid+iv), dek)
  → client: dek = AES-GCM-decrypt(sealedDek, HKDF(token, ...))

GET /sys/secure/file/{fid}
  → ciphertext blob
  → client: plaintext = AES-GCM-decrypt(ciphertext, dek, IV)
  → URL.createObjectURL(plaintext blob) → <img src="blob:..."/>
```

HKDF 二次包装确保：即使第三方 CDN 缓存了 `/key` 响应，也无法在缺少用户登录 token 的情况下还原 DEK。

## 7. 缓存层

`/@/utils/file/imageCache`：

| 函数 | 说明 |
| --- | --- |
| `withImageCache(url)` | 同步：命中返回 blob URL，未命中返回占位 + 后台拉取 |
| `withImageCacheAsync(url)` | 异步：等待解密完成再返回 blob URL |
| `clearImageCache()` | 退出登录 / 401 触发，清空内存 + IndexedDB + DEK 缓存 |
| `checkStorageQuota()` | 配额超 85% 自动 LRU 淘汰 30% 旧条目 |

缓存键：`cache:{userId}:{fid|url}`，避免多账号共用终端时互相污染。

## 8. 测试

参考 `cse-test-report-phase1.md` ~ `cse-test-report-phase2.md`：
- 单测：`FileEncryptionServiceTest`、`SecureFileControllerTest`、`CseKekServiceTest`
- 前端：`cseDecrypt.spec.ts`、`imageCache.spec.ts`
- E2E：`tests/e2e/cse/*.spec.ts`（vitest + playwright）

## 9. 常见坑

1. **新加上传入口没走加密** — 必须经 `IStorageUploadService`，不要直连 OSS SDK
2. **直接 `:src="record.url"`** — cse:// 浏览器无法识别，必须经 `<CseImage>` / `withImageCacheAsync`
3. **`<a href="cse://...">` 直链下载** — 用 `downloadByUrl`
4. **跨租户读到别人的图** — `OssFileMetaService.canRead` 已加租户校验，但写入时务必落 `tenant_id`
5. **AutoPoi 导出 Excel 头像列空白** — 需经 `CseAwareImageReader`（见 backlog）
