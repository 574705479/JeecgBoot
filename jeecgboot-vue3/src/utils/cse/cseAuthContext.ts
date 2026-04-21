/**
 * CSE 访问凭证上下文（Phase 3.2a 终审）
 *
 * 背景：
 * 1) 客服访客通道（/views/super/airag/cs/userChat 入口）使用 X-Visitor-Session / X-Visitor-Token
 *    访问 /sys/secure/file/{fid}/key 与 /sys/secure/file/{fid}，前端不会有 X-Access-Token；
 * 2) 但通用 CSE 解密 utils（cseDecrypt.ts、imageCache.ts 等）原本只读取 getToken()，
 *    在访客模式下 token 为空 → HKDF IKM 错误 → AES-GCM AuthError → 头像/聊天图无法解密；
 * 3) 同时后端 SecureFileController 已优先级化为：登录 token > 访客 session > 旧版访客 token，
 *    前端必须保证「派生 IKM 的 token」与「请求里实际生效的鉴权头」一致，否则同样会 AuthError。
 *
 * 用法（必须由进入访客 SDK / userChat 入口的页面调用 setVisitorCredential）：
 *   setVisitorCredential(sessionToken, rawVisitorToken)；离开/失效时 clearVisitorCredential()。
 *
 * 公共 CSE 工具内部应：
 *   - HKDF 派生密钥：getCseAuthToken() —— 登录优先，无则访客
 *   - HTTP 调用 /sys/secure/file 时：getCseExtraHeaders() 注入访客头
 */
import { getToken } from '/@/utils/auth';

interface VisitorCredential {
  sessionToken: string;
  visitorToken?: string;
}

interface DeviceCredential {
  deviceId: string;
  appSecret: string;
}

let visitorCredential: VisitorCredential | null = null;
// Phase 3.2f：免 Token 模式访客凭证，承载 X-Device-Id + X-App-Secret，
// 与后端 SecureFileController.resolveVisitorCredential 第 4 优先级严格对齐。
// sealToken 用 deviceId（每访客不同 → DEK 不会跨用户泄露）。
let deviceCredential: DeviceCredential | null = null;

/**
 * 设置访客凭证。建议在 userChat 入口拿到 sessionToken 后立即调用。
 * @param sessionToken 后端 /airag/cs/visitor/token/exchange 返回的会话 token
 * @param visitorToken 原始 URL ?token= 透传过来的访客 token（可选，用于兜底兼容老接口）
 */
export function setVisitorCredential(sessionToken: string, visitorToken?: string): void {
  if (!sessionToken) {
    visitorCredential = null;
    return;
  }
  visitorCredential = { sessionToken, visitorToken };
}

/** 清除访客凭证（fatalError / token 失效 / 离开访客页时调用） */
export function clearVisitorCredential(): void {
  visitorCredential = null;
}

/**
 * 设置免 Token 模式的设备凭证。
 * 触发时机：userChat onMounted 进入「免 Token 模式（tokenRequired=false）」分支，
 *          完成 checkAppKey 之后。
 * @param deviceId  访客设备码（前端 localStorage cs_user_id_* 持久化）
 * @param appSecret 接入密钥（URL ?key= 或租户后台配置的 secretKey）
 */
export function setDeviceCredential(deviceId: string, appSecret: string): void {
  if (!deviceId || !appSecret) {
    deviceCredential = null;
    return;
  }
  deviceCredential = { deviceId, appSecret };
}

/** 清除设备凭证（离开访客页 / fatalError 时调用） */
export function clearDeviceCredential(): void {
  deviceCredential = null;
}

/** 当前是否登录用户（顺带回退到访客模式判定） */
export function hasLoginToken(): boolean {
  return !!getToken();
}

/** 当前是否处于访客模式（无登录 token，但有访客凭证；含 sessionToken 与 deviceId 两路） */
export function isVisitorMode(): boolean {
  if (getToken()) return false;
  return !!visitorCredential?.sessionToken || !!deviceCredential?.deviceId;
}

/**
 * 用于 HKDF 派生 DEK 的 token。后端逻辑：
 *   sealToken = X-Access-Token ?? X-Visitor-Session ?? X-Visitor-Token ?? X-Device-Id (免 Token 模式)
 * 前后端必须严格对齐，否则 AES-GCM 一定 AuthError。
 */
export function getCseAuthToken(): string {
  const t = getToken();
  if (t) return t;
  if (visitorCredential?.sessionToken) return visitorCredential.sessionToken;
  return deviceCredential?.deviceId || '';
}

/**
 * 调用 /sys/secure/file/* 时附加的访客头。
 * 登录态返回空对象（避免冗余头干扰）。
 * 优先级：X-Visitor-Session/X-Visitor-Token > X-Device-Id+X-App-Secret
 */
export function getCseExtraHeaders(): Record<string, string> {
  if (getToken()) return {};
  if (visitorCredential) {
    const h: Record<string, string> = {};
    if (visitorCredential.sessionToken) h['X-Visitor-Session'] = visitorCredential.sessionToken;
    if (visitorCredential.visitorToken) h['X-Visitor-Token'] = visitorCredential.visitorToken;
    return h;
  }
  if (deviceCredential) {
    return {
      'X-Device-Id': deviceCredential.deviceId,
      'X-App-Secret': deviceCredential.appSecret,
    };
  }
  return {};
}
