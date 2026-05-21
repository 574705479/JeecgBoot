/**
 * CsWsClient 单元测试
 * 覆盖：connect/close/heartbeat/health/4001/4002/4005/401/fallback poll/visibility ping/dispose
 */
import { CsWsClient } from '/@/views/super/airag/cs/services/CsWsClient';

class MockWebSocket {
  static OPEN = 1;
  static CONNECTING = 0;
  static CLOSING = 2;
  static CLOSED = 3;

  static instances: MockWebSocket[] = [];

  readyState = MockWebSocket.CONNECTING;
  url: string;
  onopen: ((e?: any) => void) | null = null;
  onmessage: ((e: { data: string }) => void) | null = null;
  onerror: ((e?: any) => void) | null = null;
  onclose: ((e: { code: number; reason: string }) => void) | null = null;
  sent: string[] = [];

  constructor(url: string) {
    this.url = url;
    MockWebSocket.instances.push(this);
  }

  send(data: string) {
    this.sent.push(data);
  }

  close(code?: number, reason?: string) {
    if (this.readyState === MockWebSocket.CLOSED) return;
    this.readyState = MockWebSocket.CLOSED;
    this.onclose?.({ code: code ?? 1000, reason: reason ?? '' });
  }

  triggerOpen() {
    this.readyState = MockWebSocket.OPEN;
    this.onopen?.();
  }

  triggerMessage(data: any) {
    const payload = typeof data === 'string' ? data : JSON.stringify(data);
    this.onmessage?.({ data: payload });
  }

  triggerClose(code: number, reason = '') {
    this.readyState = MockWebSocket.CLOSED;
    this.onclose?.({ code, reason });
  }
}

(global as any).WebSocket = MockWebSocket as any;

interface CallbackBag {
  url: string;
  agentId: string;
  messages: any[];
  statuses: string[];
  ssoCount: number;
  sessionReplacedCount: number;
  quotaCount: number;
  tokenExpiredCount: number;
  fallbackPolls: number;
  reconnectSuccessCount: number;
  countdowns: number[];
  banner: boolean[];
}

function makeBag(initial: Partial<CallbackBag> = {}): CallbackBag {
  return {
    url: 'ws://test/ws/cs/agent?userId=A&token=T',
    agentId: 'A',
    messages: [],
    statuses: [],
    ssoCount: 0,
    sessionReplacedCount: 0,
    quotaCount: 0,
    tokenExpiredCount: 0,
    fallbackPolls: 0,
    reconnectSuccessCount: 0,
    countdowns: [],
    banner: [],
    ...initial,
  };
}

function newClient(bag: CallbackBag, overrides: Partial<any> = {}) {
  return new CsWsClient({
    getUrl: () => bag.url,
    agentIdProvider: () => bag.agentId,
    onMessage: (d) => bag.messages.push(d),
    onStatusChange: (s) => bag.statuses.push(s),
    onSsoKickout: () => {
      bag.ssoCount += 1;
    },
    onSessionReplaced: () => {
      bag.sessionReplacedCount += 1;
    },
    onQuotaExceeded: () => {
      bag.quotaCount += 1;
    },
    onTokenExpired: () => {
      bag.tokenExpiredCount += 1;
    },
    onFallbackPoll: async () => {
      bag.fallbackPolls += 1;
    },
    onReconnectSuccess: async () => {
      bag.reconnectSuccessCount += 1;
    },
    onReconnectCountdown: (n) => bag.countdowns.push(n),
    onBannerShow: (b) => bag.banner.push(b),
    ...overrides,
  });
}

describe('CsWsClient', () => {
  beforeEach(() => {
    MockWebSocket.instances = [];
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  test('1. 没有 agentId 时 connect 不连', () => {
    const bag = makeBag({ agentId: '' });
    const c = newClient(bag);
    c.connect();
    expect(MockWebSocket.instances.length).toBe(0);
    c.dispose();
  });

  test('2. connect 调用 getUrl 并创建 WebSocket', () => {
    const bag = makeBag();
    const c = newClient(bag);
    c.connect();
    expect(MockWebSocket.instances.length).toBe(1);
    expect(MockWebSocket.instances[0].url).toBe(bag.url);
    c.dispose();
  });

  test('3. onopen 后状态为 connected', () => {
    const bag = makeBag();
    const c = newClient(bag);
    c.connect();
    MockWebSocket.instances[0].triggerOpen();
    expect(c.getStatus()).toBe('connected');
    expect(bag.statuses).toContain('connected');
    c.dispose();
  });

  test('4. heartbeat 周期内发 ping', () => {
    const bag = makeBag();
    const c = newClient(bag, { heartbeatMs: 1000 });
    c.connect();
    MockWebSocket.instances[0].triggerOpen();
    jest.advanceTimersByTime(1000);
    const sent = MockWebSocket.instances[0].sent;
    expect(sent.length).toBeGreaterThanOrEqual(1);
    const parsed = JSON.parse(sent[0]);
    expect(parsed.type).toBe('ping');
    c.dispose();
  });

  test('5. close() 主动关闭 ws 并设 disconnected', () => {
    const bag = makeBag();
    const c = newClient(bag);
    c.connect();
    MockWebSocket.instances[0].triggerOpen();
    c.close();
    expect(c.getStatus()).toBe('disconnected');
    expect(MockWebSocket.instances[0].readyState).toBe(MockWebSocket.CLOSED);
  });

  test('6. 4001 SSO 互踢 → onSsoKickout 不重连', () => {
    const bag = makeBag();
    const c = newClient(bag);
    c.connect();
    MockWebSocket.instances[0].triggerOpen();
    MockWebSocket.instances[0].triggerClose(4001, 'sso');
    expect(bag.ssoCount).toBe(1);
    // 推进 30s 不应再产生新连接
    jest.advanceTimersByTime(30000);
    expect(MockWebSocket.instances.length).toBe(1);
    c.dispose();
  });

  test('7. 4002 会话替换 → 1.5s 自愈重连', () => {
    const bag = makeBag();
    const c = newClient(bag, { sessionReplacedReconnectMs: 1500 });
    c.connect();
    MockWebSocket.instances[0].triggerOpen();
    MockWebSocket.instances[0].triggerClose(4002, 'replaced');
    expect(bag.sessionReplacedCount).toBe(1);
    expect(MockWebSocket.instances.length).toBe(1);
    jest.advanceTimersByTime(1500);
    expect(MockWebSocket.instances.length).toBe(2);
    c.dispose();
  });

  test('8. 4005 坐席已满 → onQuotaExceeded 不重连', () => {
    const bag = makeBag();
    const c = newClient(bag);
    c.connect();
    MockWebSocket.instances[0].triggerOpen();
    MockWebSocket.instances[0].triggerClose(4005, 'quota');
    expect(bag.quotaCount).toBe(1);
    jest.advanceTimersByTime(30000);
    expect(MockWebSocket.instances.length).toBe(1);
    c.dispose();
  });

  test('9. 401 token 过期消息 → onTokenExpired', () => {
    const bag = makeBag();
    const c = newClient(bag);
    c.connect();
    MockWebSocket.instances[0].triggerOpen();
    MockWebSocket.instances[0].triggerMessage({ code: 401, msg: 'expired' });
    expect(bag.tokenExpiredCount).toBe(1);
    // 应不再走 onMessage
    expect(bag.messages.length).toBe(0);
    c.dispose();
  });

  test('10. fallback poll 健康间隔 30s', async () => {
    const bag = makeBag();
    const c = newClient(bag, { fallbackHealthyMs: 30000, fallbackDegradedMs: 5000 });
    c.connect();
    MockWebSocket.instances[0].triggerOpen();
    c.startFallbackPoll();
    // 第一次 tick：5s（首次降级值），tick 是 async，需要异步推进让 await + finally 执行
    await jest.advanceTimersByTimeAsync(5000);
    expect(bag.fallbackPolls).toBe(1);
    // 之后 WS 健康 → 下次 30s
    await jest.advanceTimersByTimeAsync(30000);
    expect(bag.fallbackPolls).toBe(2);
    c.dispose();
  });

  test('11. fallback poll 降级间隔 5s（断线状态）', async () => {
    const bag = makeBag();
    const c = newClient(bag, { fallbackDegradedMs: 5000 });
    c.startFallbackPoll();
    await jest.advanceTimersByTimeAsync(5000);
    expect(bag.fallbackPolls).toBe(1);
    await jest.advanceTimersByTimeAsync(5000);
    expect(bag.fallbackPolls).toBe(2);
    c.dispose();
  });

  test('12. dispose 后 connect 无效', () => {
    const bag = makeBag();
    const c = newClient(bag);
    c.dispose();
    c.connect();
    expect(MockWebSocket.instances.length).toBe(0);
  });

  test('13. visibility ping 5s 内无 pong 强制 close', () => {
    const bag = makeBag();
    const c = newClient(bag, { visibilityPingTimeoutMs: 5000 });
    c.connect();
    const ws = MockWebSocket.instances[0];
    ws.triggerOpen();

    // 模拟 visibility 事件：标 lastWsMessageAt 然后调内部 visibility handler
    // 通过手动触发 document.visibilitychange
    Object.defineProperty(document, 'hidden', { configurable: true, get: () => false });
    Object.defineProperty(document, 'visibilityState', { configurable: true, get: () => 'visible' });
    document.dispatchEvent(new Event('visibilitychange'));

    // 此时 ws 应当被发了一个 ping
    const beforeLen = ws.sent.length;
    expect(beforeLen).toBeGreaterThanOrEqual(1);

    // 5s 内不模拟任何 pong → ws.close 应当被强制
    jest.advanceTimersByTime(5000);
    expect(ws.readyState).toBe(MockWebSocket.CLOSED);
    c.dispose();
  });
});
