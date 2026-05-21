/**
 * 客服 WebSocket 客户端
 *
 * 业务零耦合：仅负责协议层（连接/心跳/健康检查/fallback poll/断线重连/错误码处理），
 * 业务行为通过 callback 注入。
 *
 * 行为细节逐字复刻自原 workbench/index.vue 1390-4316 行代码：
 *   - heartbeatMs: 15s 心跳
 *   - fallbackHealthyMs: 30s 健康降级阈值
 *   - fallbackDegradedMs: 5s 降级 poll 间隔
 *   - 指数退避重连（1s → 2s → 4s → 8s → ... 上限 30s + jitter）
 *   - 4002 会话替换：1.5s 自愈重连
 *   - 4001 SSO 互踢：不重连（plan 新增）
 *   - 4005 坐席已满：onQuotaExceeded 不重连
 *   - 401 token 过期（onmessage data.code === 401）：onTokenExpired 不重连
 *   - visibility 变可见时主动 ping，5s 内无 pong 强制 close 触发重连
 *   - network online 时尝试重连
 *   - beforeunload 主动 close 1000
 */

export type WsStatus = 'connected' | 'connecting' | 'reconnecting' | 'disconnected';

export interface CsWsClientOptions {
  /** 每次 connect 调用时返回最新 URL（含最新 token + agentId） */
  getUrl(): string;
  /** 仅用于日志与 reconnect 内部校验是否仍有 agentId */
  agentIdProvider(): string;

  /** 心跳间隔 ms，默认 15000 */
  heartbeatMs?: number;
  /** WS 健康状态下的 fallback poll 间隔 ms，默认 30000 */
  fallbackHealthyMs?: number;
  /** WS 不健康状态下的 fallback poll 间隔 ms，默认 5000 */
  fallbackDegradedMs?: number;
  /** WS 在线但 lastMessageAt 在该窗口内时跳过 fallback poll（避免重复拉），默认 5000 */
  fallbackTriggeredAfterWsMs?: number;
  /** 4002 会话替换后的自愈重连延迟 ms，默认 1500 */
  sessionReplacedReconnectMs?: number;
  /** 重连退避上限 ms，默认 30000 */
  maxReconnectDelayMs?: number;
  /** visibility 变可见后主动 ping 的超时 ms，默认 5000 */
  visibilityPingTimeoutMs?: number;

  /** 收到任意 WS 消息（已 JSON.parse） */
  onMessage(data: any): void;
  /** WS 状态变化（每次都回调，调用方自己 dedupe） */
  onStatusChange(status: WsStatus): void;
  /** 4001 SSO 互踢：不重连，调用方决定 logout */
  onSsoKickout(reason?: string): void;
  /** 4002 会话被替换：类内已自动调度 1.5s 自愈重连，回调仅供 banner 提示 */
  onSessionReplaced(reason?: string): void;
  /** 4005 坐席已满：不重连，调用方决定 logout */
  onQuotaExceeded(reason?: string): void;
  /** 401 token 过期（onmessage data.code === 401 时触发）：不重连，调用方刷 token 后显式 connect() */
  onTokenExpired(): void;
  /** fallback poll tick：业务侧拉取最新会话列表 */
  onFallbackPoll(): Promise<void>;
  /** 重连成功（非首次连接的 onopen）：业务侧并行恢复 loadAgentInfo + loadConversations 等 */
  onReconnectSuccess(): Promise<void>;

  /** 可选：重连倒计时（每秒回调一次直至 0） */
  onReconnectCountdown?(seconds: number): void;
  /** 可选：banner 显示状态变化 */
  onBannerShow?(show: boolean): void;
  /** 可选：手动控制 fallback poll 是否参考 currentConversationId 判断刷新 */
  shouldRefreshCurrentMessages?(): boolean;
  /** 可选：reconnect / visibility ping 时业务侧需要的预钩子（默认无） */
  onBeforeReconnect?(): void;
}

const DEFAULT_HEARTBEAT_MS = 15000;
const DEFAULT_FALLBACK_HEALTHY_MS = 30000;
const DEFAULT_FALLBACK_DEGRADED_MS = 5000;
const DEFAULT_FALLBACK_TRIGGERED_AFTER_WS_MS = 5000;
const DEFAULT_SESSION_REPLACED_RECONNECT_MS = 1500;
const DEFAULT_MAX_RECONNECT_DELAY_MS = 30000;
const DEFAULT_VISIBILITY_PING_TIMEOUT_MS = 5000;

export class CsWsClient {
  private opts: CsWsClientOptions;
  private heartbeatMs: number;
  private fallbackHealthyMs: number;
  private fallbackDegradedMs: number;
  private fallbackTriggeredAfterWsMs: number;
  private sessionReplacedReconnectMs: number;
  private maxReconnectDelayMs: number;
  private visibilityPingTimeoutMs: number;

  private ws: WebSocket | null = null;
  private status: WsStatus = 'disconnected';
  private lastWsMessageAt = 0;

  private heartbeatTimer: number | null = null;
  private healthTimer: number | null = null;
  private reconnectTimer: number | null = null;
  private fallbackPollTimer: number | null = null;
  private countdownTimer: number | null = null;
  private connectedBannerTimer: number | null = null;
  private sessionReplacedTimer: number | null = null;

  private wsManuallyClosed = false;
  private hasConnectedOnce = false;
  private reconnectAttempts = 0;
  private disposed = false;

  private boundVisibility: () => void;
  private boundOnline: () => void;
  private boundBeforeUnload: () => void;

  constructor(opts: CsWsClientOptions) {
    this.opts = opts;
    this.heartbeatMs = opts.heartbeatMs ?? DEFAULT_HEARTBEAT_MS;
    this.fallbackHealthyMs = opts.fallbackHealthyMs ?? DEFAULT_FALLBACK_HEALTHY_MS;
    this.fallbackDegradedMs = opts.fallbackDegradedMs ?? DEFAULT_FALLBACK_DEGRADED_MS;
    this.fallbackTriggeredAfterWsMs =
      opts.fallbackTriggeredAfterWsMs ?? DEFAULT_FALLBACK_TRIGGERED_AFTER_WS_MS;
    this.sessionReplacedReconnectMs =
      opts.sessionReplacedReconnectMs ?? DEFAULT_SESSION_REPLACED_RECONNECT_MS;
    this.maxReconnectDelayMs = opts.maxReconnectDelayMs ?? DEFAULT_MAX_RECONNECT_DELAY_MS;
    this.visibilityPingTimeoutMs =
      opts.visibilityPingTimeoutMs ?? DEFAULT_VISIBILITY_PING_TIMEOUT_MS;

    this.boundVisibility = this.handleVisibilityChange.bind(this);
    this.boundOnline = this.handleNetworkOnline.bind(this);
    this.boundBeforeUnload = this.handleBeforeUnload.bind(this);

    if (typeof document !== 'undefined') {
      document.addEventListener('visibilitychange', this.boundVisibility);
    }
    if (typeof window !== 'undefined') {
      window.addEventListener('online', this.boundOnline);
      window.addEventListener('beforeunload', this.boundBeforeUnload);
    }
  }

  /** 连接 WS。已连接 / 正在连接时直接 return（idempotent） */
  connect(): void {
    if (this.disposed) return;
    if (this.ws && (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING)) {
      return;
    }
    if (!this.opts.agentIdProvider()) {
      // 没有 agentId 时不连接（业务侧自行决定何时调 connect）
      return;
    }

    this.wsManuallyClosed = false;

    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    this.stopWsCountdown();

    if (this.hasConnectedOnce) {
      const next: WsStatus = this.reconnectAttempts > 0 ? 'reconnecting' : 'connecting';
      this.setStatus(next);
      this.opts.onBannerShow?.(true);
    } else {
      this.setStatus('connecting');
    }

    let url: string;
    try {
      url = this.opts.getUrl();
    } catch (e) {
      console.error('[CsWsClient] getUrl threw', e);
      this.scheduleReconnect();
      return;
    }
    if (!url) {
      this.scheduleReconnect();
      return;
    }

    let socket: WebSocket;
    try {
      socket = new WebSocket(url);
    } catch (e) {
      console.error('[CsWsClient] new WebSocket failed', e);
      this.scheduleReconnect();
      return;
    }
    this.ws = socket;
    const thisWs = socket;

    socket.onopen = async () => {
      if (this.ws !== thisWs) return;
      const isReconnect = this.reconnectAttempts > 0;
      this.reconnectAttempts = 0;
      this.lastWsMessageAt = Date.now();
      this.startHeartbeat();
      this.startHealthCheck();

      if (isReconnect) {
        try {
          await this.opts.onReconnectSuccess();
        } catch (e) {
          console.warn('[CsWsClient] onReconnectSuccess threw', e);
        }
      }

      if (this.ws !== thisWs) return;

      if (this.hasConnectedOnce) {
        this.setStatus('connected');
        if (this.connectedBannerTimer) {
          clearTimeout(this.connectedBannerTimer);
        }
        this.connectedBannerTimer = window.setTimeout(() => {
          this.opts.onBannerShow?.(false);
          this.connectedBannerTimer = null;
        }, 1500);
      } else {
        this.setStatus('connected');
      }
      this.hasConnectedOnce = true;
    };

    socket.onmessage = (event) => {
      this.lastWsMessageAt = Date.now();
      let data: any;
      try {
        data = JSON.parse(event.data);
      } catch (e) {
        console.error('[CsWsClient] parse message failed', e);
        return;
      }
      if (data && data.code === 401) {
        try {
          this.opts.onTokenExpired();
        } catch (e) {
          console.error('[CsWsClient] onTokenExpired threw', e);
        }
        this.wsManuallyClosed = true;
        this.stopHeartbeat();
        this.stopHealthCheck();
        this.stopFallbackPoll();
        try {
          socket.close();
        } catch (_) {
          /* ignore */
        }
        return;
      }
      try {
        this.opts.onMessage(data);
      } catch (e) {
        console.error('[CsWsClient] onMessage threw', e);
      }
    };

    socket.onerror = () => {
      if (!this.wsManuallyClosed && this.ws === thisWs) {
        try {
          socket.close();
        } catch (_) {
          /* ignore */
        }
      }
    };

    socket.onclose = (event) => {
      if (this.ws !== thisWs) return;
      this.ws = null;
      this.stopHeartbeat();
      this.stopHealthCheck();

      if (event.code === 4001) {
        if (this.reconnectTimer) {
          clearTimeout(this.reconnectTimer);
          this.reconnectTimer = null;
        }
        this.stopFallbackPoll();
        this.wsManuallyClosed = true;
        this.setStatus('disconnected');
        this.opts.onBannerShow?.(false);
        try {
          this.opts.onSsoKickout(event.reason);
        } catch (e) {
          console.error('[CsWsClient] onSsoKickout threw', e);
        }
        return;
      }

      if (event.code === 4002) {
        if (this.reconnectTimer) {
          clearTimeout(this.reconnectTimer);
          this.reconnectTimer = null;
        }
        if (this.wsManuallyClosed) {
          this.stopFallbackPoll();
          return;
        }
        this.setStatus('reconnecting');
        this.opts.onBannerShow?.(true);
        try {
          this.opts.onSessionReplaced(event.reason);
        } catch (e) {
          console.error('[CsWsClient] onSessionReplaced threw', e);
        }
        if (this.sessionReplacedTimer) {
          clearTimeout(this.sessionReplacedTimer);
        }
        this.sessionReplacedTimer = window.setTimeout(() => {
          this.sessionReplacedTimer = null;
          if (
            !this.disposed &&
            !this.wsManuallyClosed &&
            !this.ws &&
            this.opts.agentIdProvider()
          ) {
            this.connect();
          }
        }, this.sessionReplacedReconnectMs);
        return;
      }

      if (event.code === 4005) {
        this.stopFallbackPoll();
        if (this.reconnectTimer) {
          clearTimeout(this.reconnectTimer);
          this.reconnectTimer = null;
        }
        this.wsManuallyClosed = true;
        this.setStatus('disconnected');
        this.opts.onBannerShow?.(false);
        try {
          this.opts.onQuotaExceeded(event.reason);
        } catch (e) {
          console.error('[CsWsClient] onQuotaExceeded threw', e);
        }
        return;
      }

      if (!this.wsManuallyClosed) {
        this.scheduleReconnect();
      } else {
        this.setStatus('disconnected');
      }
    };
  }

  /** 主动关闭 WS，不再自动重连。可重新调 connect() 重新建连 */
  close(_reason?: string): void {
    this.wsManuallyClosed = true;
    this.stopHeartbeat();
    this.stopHealthCheck();
    this.stopFallbackPoll();
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.sessionReplacedTimer) {
      clearTimeout(this.sessionReplacedTimer);
      this.sessionReplacedTimer = null;
    }
    if (this.ws) {
      try {
        this.ws.close();
      } catch (_) {
        /* ignore */
      }
    }
    this.ws = null;
    this.setStatus('disconnected');
    this.opts.onBannerShow?.(false);
    this.stopWsCountdown();
    if (this.connectedBannerTimer) {
      clearTimeout(this.connectedBannerTimer);
      this.connectedBannerTimer = null;
    }
  }

  /** 销毁实例：close + 移除全局监听 */
  dispose(): void {
    if (this.disposed) return;
    this.disposed = true;
    this.close('dispose');
    if (typeof document !== 'undefined') {
      document.removeEventListener('visibilitychange', this.boundVisibility);
    }
    if (typeof window !== 'undefined') {
      window.removeEventListener('online', this.boundOnline);
      window.removeEventListener('beforeunload', this.boundBeforeUnload);
    }
  }

  send(data: any): boolean {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return false;
    try {
      this.ws.send(typeof data === 'string' ? data : JSON.stringify(data));
      return true;
    } catch (_) {
      return false;
    }
  }

  isHealthy(): boolean {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return false;
    if (!this.lastWsMessageAt) return true;
    return Date.now() - this.lastWsMessageAt < this.fallbackHealthyMs;
  }

  getStatus(): WsStatus {
    return this.status;
  }

  getLastMessageAt(): number {
    return this.lastWsMessageAt;
  }

  /** 启动 fallback poll：在 onMounted / connect 之外的入口（业务侧调用） */
  startFallbackPoll(): void {
    this.stopFallbackPoll();
    const tick = async () => {
      this.fallbackPollTimer = null;
      try {
        if (typeof document !== 'undefined' && document.hidden) return;
        if (this.disposed) return;
        if (!this.opts.agentIdProvider()) return;
        if (this.ws && this.ws.readyState === WebSocket.OPEN && this.lastWsMessageAt) {
          if (Date.now() - this.lastWsMessageAt < this.fallbackTriggeredAfterWsMs) {
            return;
          }
        }
        await this.opts.onFallbackPoll();
      } catch (_) {
        /* ignore */
      } finally {
        if (this.disposed) return;
        const nextDelay =
          this.ws && this.ws.readyState === WebSocket.OPEN
            ? this.fallbackHealthyMs
            : this.fallbackDegradedMs;
        this.fallbackPollTimer = window.setTimeout(tick, nextDelay);
      }
    };
    this.fallbackPollTimer = window.setTimeout(tick, this.fallbackDegradedMs);
  }

  stopFallbackPoll(): void {
    if (this.fallbackPollTimer) {
      clearTimeout(this.fallbackPollTimer);
      this.fallbackPollTimer = null;
    }
  }

  // === 内部 ===

  private setStatus(s: WsStatus) {
    if (this.status === s) {
      try {
        this.opts.onStatusChange(s);
      } catch (e) {
        console.error('[CsWsClient] onStatusChange threw', e);
      }
      return;
    }
    this.status = s;
    try {
      this.opts.onStatusChange(s);
    } catch (e) {
      console.error('[CsWsClient] onStatusChange threw', e);
    }
  }

  private startHeartbeat() {
    this.stopHeartbeat();
    this.heartbeatTimer = window.setInterval(() => {
      if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return;
      try {
        this.ws.send(JSON.stringify({ type: 'ping', ts: Date.now() }));
      } catch (_) {
        try {
          this.ws?.close();
        } catch (__) {
          /* ignore */
        }
      }
    }, this.heartbeatMs);
  }

  private stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  private startHealthCheck() {
    this.stopHealthCheck();
    this.healthTimer = window.setInterval(() => {
      if (!this.ws) return;
      if (this.ws.readyState !== WebSocket.OPEN) {
        this.scheduleReconnect();
      }
    }, this.heartbeatMs);
  }

  private stopHealthCheck() {
    if (this.healthTimer) {
      clearInterval(this.healthTimer);
      this.healthTimer = null;
    }
  }

  private stopWsCountdown() {
    if (this.countdownTimer) {
      clearInterval(this.countdownTimer);
      this.countdownTimer = null;
    }
    this.opts.onReconnectCountdown?.(0);
  }

  private scheduleReconnect() {
    if (this.disposed) return;
    if (this.wsManuallyClosed) return;
    if (this.reconnectTimer) return;

    const jitter = Math.floor(Math.random() * 1000);
    const delay =
      Math.min(this.maxReconnectDelayMs, 1000 * Math.pow(2, this.reconnectAttempts)) + jitter;
    this.reconnectAttempts += 1;

    if (this.hasConnectedOnce) {
      this.setStatus('disconnected');
      this.opts.onBannerShow?.(true);
      if (this.countdownTimer) {
        clearInterval(this.countdownTimer);
        this.countdownTimer = null;
      }
      let remain = Math.ceil(delay / 1000);
      this.opts.onReconnectCountdown?.(remain);
      this.countdownTimer = window.setInterval(() => {
        remain -= 1;
        this.opts.onReconnectCountdown?.(remain);
        if (remain <= 0) {
          if (this.countdownTimer) {
            clearInterval(this.countdownTimer);
            this.countdownTimer = null;
          }
        }
      }, 1000);
    }

    this.reconnectTimer = window.setTimeout(() => {
      this.reconnectTimer = null;
      this.stopWsCountdown();
      this.opts.onBeforeReconnect?.();
      this.connect();
    }, delay);
  }

  /**
   * visibility change → visible 时主动 ping 探活
   * 5s 内 lastMessageAt 没更新就强制 close 触发重连（B1 修复，PC 休眠唤醒后僵尸 ws 检测）
   */
  private handleVisibilityChange() {
    if (this.disposed) return;
    if (typeof document === 'undefined' || document.hidden) return;
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      this.connect();
      return;
    }
    const beforePing = this.lastWsMessageAt;
    try {
      this.ws.send(JSON.stringify({ type: 'ping', ts: Date.now() }));
    } catch (_) {
      try {
        this.ws?.close();
      } catch (__) {
        /* ignore */
      }
      return;
    }
    setTimeout(() => {
      if (
        this.lastWsMessageAt === beforePing &&
        this.ws &&
        this.ws.readyState === WebSocket.OPEN
      ) {
        console.warn('[CsWsClient] visibility ping 5s 内无 pong，判定 ws 僵尸，强制重连');
        try {
          this.ws.close();
        } catch (_) {
          /* ignore */
        }
      }
    }, this.visibilityPingTimeoutMs);
  }

  private handleNetworkOnline() {
    if (this.disposed) return;
    this.connect();
  }

  private handleBeforeUnload() {
    if (this.disposed) return;
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.wsManuallyClosed = true;
      try {
        this.ws.close(1000, 'page_refresh');
      } catch (_) {
        /* ignore */
      }
    }
  }
}
