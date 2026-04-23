/**
 * 访客端首屏 bootstrap 客户端封装。
 *
 * <p>对应后端 {@code POST /airag/cs/visitor/bootstrap}，把原本 9 个串行 HTTP
 * 合并为 1 个 POST。本模块只负责"取数 + 缓存"，不做反应式状态写入；
 * 状态写入由调用方（{@code ChatMain.vue}）的 {@code applyBootstrap} 处理。</p>
 *
 * <h3>三段式调用</h3>
 * <ol>
 *   <li>同步：{@link readBootstrapCache} 读 localStorage 拿上次快照</li>
 *   <li>同步：调用方立即用 cached 渲染（消除白屏感）</li>
 *   <li>异步：{@link fetchBootstrap} 真正打到服务端，返回 fresh，调用方 diff 更新</li>
 * </ol>
 *
 * <h3>缓存策略</h3>
 * <ul>
 *   <li>key: {@code cs_bootstrap_v1_${visitorKey}}（不同 key/agent 隔离）</li>
 *   <li>过期: 24h，超期视作无缓存（避免老 schema 残留）</li>
 *   <li>失效场景: schema 变更时 bump 版本号到 v2</li>
 *   <li>快照内容仍是密文/半密文形式（recentMessages.content = storage ENC:）</li>
 * </ul>
 *
 * @author jeecg
 */
import { defHttp } from '/@/utils/http/axios';
import { decryptTransport } from '/@/utils/cs/csEncrypt';

const CACHE_VERSION = 'v1';
const CACHE_TTL_MS = 24 * 3600 * 1000;

/** 后端 VisitorBootstrapVO 的前端镜像（字段对应 Java VO） */
export interface BootstrapResponse {
  // 鉴权 / 黑名单
  tokenRequired?: boolean;
  ipBlocked?: boolean;
  userBlocked?: boolean;
  keyInvalid?: boolean;
  clientIp?: string;
  authStatus?: string;

  // 全局配置（明文 JSON 字符串）
  chatWindowConfigJson?: string;
  sensitiveWordsJson?: string;
  brandConfigJson?: string;
  messageBoardConfigJson?: string;
  aiEnabled?: boolean;
  visitorAppId?: string;

  // 客服在线状态
  agentOnline?: boolean;
  agentOnlineCount?: number;

  // 会话 + 消息（conversation.lastMessage 已 transport 加密；
  // recentMessages[].content 仍是 storage ENC: 形态）
  conversation?: any | null;
  recentMessages?: any[];
  hasMoreMessages?: boolean;

  // 未读留言（content/reply 已 transport 加密，phone/email/qq/wechat 已脱敏）
  unreadReplies?: any[];

  // 元数据
  serverTime?: number;
  etag?: string;
}

export interface BootstrapPayload {
  /** 客户端缓存的 conversationId，命中则复用 */
  conversationId?: string;
  /** true 时只查 active 不主动创建会话 */
  skipCreate?: boolean;
  /** 首屏拉历史消息条数（1-100，默认 20） */
  recentLimit?: number;
}

interface CacheEntry {
  _cachedAt: number;
  _version: string;
  data: BootstrapResponse;
}

function cacheStorageKey(visitorKey: string): string {
  return `cs_bootstrap_${CACHE_VERSION}_${visitorKey}`;
}

/**
 * 同步读 localStorage 里的 bootstrap 快照；过期或损坏返回 null。
 */
export function readBootstrapCache(visitorKey: string): BootstrapResponse | null {
  if (!visitorKey) return null;
  try {
    const raw = localStorage.getItem(cacheStorageKey(visitorKey));
    if (!raw) return null;
    const parsed = JSON.parse(raw) as CacheEntry;
    if (!parsed || parsed._version !== CACHE_VERSION) return null;
    if (typeof parsed._cachedAt !== 'number') return null;
    if (Date.now() - parsed._cachedAt > CACHE_TTL_MS) return null;
    return parsed.data || null;
  } catch {
    return null;
  }
}

/**
 * 写 bootstrap 快照到 localStorage。失败静默吞（隐私模式 / 配额满）。
 */
export function writeBootstrapCache(visitorKey: string, data: BootstrapResponse): void {
  if (!visitorKey || !data) return;
  try {
    const entry: CacheEntry = {
      _cachedAt: Date.now(),
      _version: CACHE_VERSION,
      data,
    };
    localStorage.setItem(cacheStorageKey(visitorKey), JSON.stringify(entry));
  } catch {
    // 配额满 / 隐私模式禁用，忽略
  }
}

/**
 * 主动清掉某个 key 的 bootstrap 缓存。schema 不兼容、登出时调用。
 */
export function clearBootstrapCache(visitorKey: string): void {
  if (!visitorKey) return;
  try {
    localStorage.removeItem(cacheStorageKey(visitorKey));
  } catch {
    // ignore
  }
}

/**
 * 鉴权透传：bootstrap 在 onMounted 早期调用，axios 拦截器通常还没拿到 cseAuthContext，
 * 所以必须显式把 URL 上读到的 ?key= / ?token= / ?sessionToken= 透传给后端，
 * 否则后端走 validateAppKey(request) 拿不到 secretKey 会直接返回 keyInvalid:true。
 */
export interface BootstrapAuthHints {
  /** URL 上的 ?key=（免 Token 模式必传，对应 X-App-Secret / request.getParameter("key")） */
  key?: string;
  /** URL 上的 ?sessionToken=（设备绑定模式） */
  sessionToken?: string;
  /** URL 上的 ?token= 或 ?visitorToken=（短期 Token 模式） */
  visitorToken?: string;
  /**
   * 浏览器侧持久化设备码，免 Token 模式下后端用它确定 visitorUserId。
   * 不传的话 bootstrap 拿不到 visitorUserId，conversation 段会跳过创建，
   * 前端就无法连 WebSocket（缺 conversationId/userId）。
   */
  deviceId?: string;
}

function buildBootstrapUrl(auth?: BootstrapAuthHints): string {
  const base = '/airag/cs/visitor/bootstrap';
  if (!auth) return base;
  const qs: string[] = [];
  if (auth.key) qs.push('key=' + encodeURIComponent(auth.key));
  if (auth.sessionToken) qs.push('sessionToken=' + encodeURIComponent(auth.sessionToken));
  if (auth.visitorToken) qs.push('token=' + encodeURIComponent(auth.visitorToken));
  if (auth.deviceId) qs.push('deviceId=' + encodeURIComponent(auth.deviceId));
  return qs.length ? base + '?' + qs.join('&') : base;
}

function buildBootstrapHeaders(auth?: BootstrapAuthHints): Record<string, string> {
  const headers: Record<string, string> = {};
  if (!auth) return headers;
  // 同步双通道：query 给老 Filter / shiro 兜底，header 给新链路（CsRequestUtil 优先读 header）
  if (auth.key) headers['X-App-Secret'] = auth.key;
  if (auth.visitorToken) headers['X-Visitor-Token'] = auth.visitorToken;
  if (auth.deviceId) headers['X-Device-Id'] = auth.deviceId;
  return headers;
}

/**
 * 真正打 POST /airag/cs/visitor/bootstrap，返回解外壳后的 VO。
 *
 * <p>失败抛异常，调用方需要捕获后回退到原有 9 个接口路径。</p>
 */
export async function fetchBootstrap(
  payload: BootstrapPayload = {},
  auth?: BootstrapAuthHints,
): Promise<BootstrapResponse> {
  const res = await defHttp.post(
    {
      url: buildBootstrapUrl(auth),
      data: payload || {},
      headers: buildBootstrapHeaders(auth),
    },
    { isTransformResponse: false },
  );
  if (!res || res.success !== true) {
    throw new Error('bootstrap response not success: ' + JSON.stringify(res));
  }
  const cipher = res.result;
  if (typeof cipher !== 'string' || !cipher) {
    throw new Error('bootstrap result is empty');
  }
  const json = decryptTransport(cipher);
  if (!json || json === cipher) {
    throw new Error('bootstrap transport decrypt failed');
  }
  try {
    return JSON.parse(json) as BootstrapResponse;
  } catch (e: any) {
    throw new Error('bootstrap json parse failed: ' + (e?.message || e));
  }
}

export interface BootstrapResult {
  /** 同步可用：上次访问写入的快照（可能比服务端旧） */
  cached: BootstrapResponse | null;
  /** 异步：真正打到服务端的 fresh 数据（成功后自动写缓存） */
  fresh: Promise<BootstrapResponse>;
}

/**
 * 三段式调用入口。
 *
 * @param visitorKey 用作缓存隔离的 key（一般是 URL 上的 ?key= 参数）
 * @param payload    bootstrap 请求体
 * @param auth       透传给后端鉴权的 URL 参数（key / sessionToken / visitorToken）；
 *                   未传时默认把 visitorKey 当作 ?key= 传上去（最常见的免 Token 场景）
 */
export function bootstrapVisitor(
  visitorKey: string,
  payload: BootstrapPayload = {},
  auth?: BootstrapAuthHints,
): BootstrapResult {
  const cached = readBootstrapCache(visitorKey);
  const effectiveAuth: BootstrapAuthHints = {
    key: auth?.key ?? visitorKey,
    sessionToken: auth?.sessionToken,
    visitorToken: auth?.visitorToken,
    deviceId: auth?.deviceId,
  };
  const fresh = fetchBootstrap(payload, effectiveAuth).then((data) => {
    writeBootstrapCache(visitorKey, data);
    return data;
  });
  return { cached, fresh };
}
