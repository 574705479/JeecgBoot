import { defineStore } from 'pinia';
import { ref, computed, watch } from 'vue';
import { store } from '/@/store';

export type WsStatus = 'connected' | 'connecting' | 'reconnecting' | 'disconnected';

/**
 * 持续响铃模式：
 *   - off       不响
 *   - on_blur   窗口失焦 / 浏览器最小化 / 路由不在工作台 时响（前台聚焦在工作台时停）
 *   - always    任意场景持续响（直到回执条件满足）
 */
export type ContinuousRingMode = 'off' | 'on_blur' | 'always';

/**
 * 持续响铃停止条件：
 *   - any_one        客服回复任一未读会话即停响
 *   - all_visitors   必须回复完所有未回复访客才停响
 */
export type RingStopCondition = 'any_one' | 'all_visitors';

type Listener = (payload: any) => void;

class MiniEmitter {
  private map = new Map<string, Set<Listener>>();

  on(event: string, fn: Listener) {
    if (!this.map.has(event)) this.map.set(event, new Set());
    this.map.get(event)!.add(fn);
  }

  off(event: string, fn: Listener) {
    this.map.get(event)?.delete(fn);
  }

  emit(event: string, payload?: any) {
    const subs = this.map.get(event);
    if (!subs) return;
    for (const fn of Array.from(subs)) {
      try {
        fn(payload);
      } catch (e) {
        console.error(`[CsStore] listener of "${event}" threw:`, e);
      }
    }
  }

  clear(event?: string) {
    if (event) this.map.delete(event);
    else this.map.clear();
  }
}

const SOUND_ENABLED_KEY = 'CS_SOUND_ENABLED';
const SOUND_VOLUME_KEY = 'CS_SOUND_VOLUME';

const RING_MODE_KEY = 'cs_workbench_continuous_ring_mode';
const RING_STOP_KEY = 'cs_workbench_ring_stop_condition';
const RING_INTERVAL_KEY = 'cs_workbench_ring_interval';

const VALID_RING_MODES: ContinuousRingMode[] = ['off', 'on_blur', 'always'];
const VALID_RING_STOPS: RingStopCondition[] = ['any_one', 'all_visitors'];
const VALID_RING_INTERVALS = [3, 5, 10, 15];

function readRingMode(): ContinuousRingMode {
  try {
    const raw = localStorage.getItem(RING_MODE_KEY);
    return VALID_RING_MODES.includes(raw as ContinuousRingMode) ? (raw as ContinuousRingMode) : 'off';
  } catch (_) {
    return 'off';
  }
}

function readRingStopCondition(): RingStopCondition {
  try {
    const raw = localStorage.getItem(RING_STOP_KEY);
    // 旧值兼容：this_conversation 旧实现实际等价于"全部回复才停"，但新语义下默认更友好的"回复任一即停"
    if (raw === 'this_conversation') return 'any_one';
    return VALID_RING_STOPS.includes(raw as RingStopCondition) ? (raw as RingStopCondition) : 'any_one';
  } catch (_) {
    return 'any_one';
  }
}

function readRingInterval(): number {
  try {
    const raw = localStorage.getItem(RING_INTERVAL_KEY);
    const n = raw != null ? parseInt(raw, 10) : NaN;
    return VALID_RING_INTERVALS.includes(n) ? n : 5;
  } catch (_) {
    return 5;
  }
}

export const useCsStore = defineStore('cs', () => {
  const conversations = ref<any[]>([]);
  const agentId = ref<string>('');
  const agentInfo = ref<any>(null);
  const wsStatus = ref<WsStatus>('disconnected');
  const wsShowBanner = ref(false);
  const wsReconnectCountdown = ref(0);
  const soundEnabled = ref(true);
  const soundVolumePercent = ref(100);

  try {
    const e = localStorage.getItem(SOUND_ENABLED_KEY);
    if (e !== null) soundEnabled.value = e === '1';
    const v = localStorage.getItem(SOUND_VOLUME_KEY);
    if (v !== null) {
      const n = parseInt(v, 10);
      if (Number.isFinite(n)) soundVolumePercent.value = Math.max(0, Math.min(200, n));
    }
  } catch (_) {
    // 忽略 localStorage 异常（隐私模式 / SSR）
  }

  // ============ 持续响铃状态（迁自 useCsContinuousRing.ts，状态归 store 契约） ============
  const continuousRingMode = ref<ContinuousRingMode>(readRingMode());
  const ringStopCondition = ref<RingStopCondition>(readRingStopCondition());
  const ringIntervalSeconds = ref<number>(readRingInterval());
  const ringPausedUntil = ref<number>(0);
  const pauseRemainSeconds = ref<number>(0);
  const pendingRingConvs = ref<Set<string>>(new Set<string>());
  /**
   * any_one 模式专用状态机：客服在本轮响铃中是否已回复过至少一个会话。
   *   - dequeue 时置 true → tick 内立即停响
   *   - 新的访客消息 enqueue 进来 → 重置 false（新一轮提醒）
   *   - 队列空 / 模式切换 / 暂停恢复 / 重建队列 → 重置 false
   * 不影响 all_visitors 模式
   */
  const replyAcknowledged = ref(false);
  /**
   * 访客最后发消息时间戳（conversationId -> timestamp ms），客服回复后清除。
   * 用普通 Map：每秒主动 tick 不依赖响应性，避免 Pinia 对 Map mutations 的响应性陷阱。
   */
  const visitorLastMsgTime = new Map<string, number>();

  // localStorage 持久化（flush: 'sync' 避免 Pinia 初始化阶段触发额外写）
  watch(continuousRingMode, (v) => {
    try {
      localStorage.setItem(RING_MODE_KEY, v);
    } catch (_) { /* ignore */ }
  }, { flush: 'sync' });
  watch(ringStopCondition, (v) => {
    try {
      localStorage.setItem(RING_STOP_KEY, v);
    } catch (_) { /* ignore */ }
  }, { flush: 'sync' });
  watch(ringIntervalSeconds, (v) => {
    try {
      localStorage.setItem(RING_INTERVAL_KEY, String(v));
    } catch (_) { /* ignore */ }
  }, { flush: 'sync' });

  /**
   * 停止条件切换副作用：用 watch 响应 ref 变化（兼容 v-model setter 直接写 ref + setter action 两条路径）。
   * 不能放在 setRingStopCondition action 内做 oldVal/newVal 对比，因为抽屉 v-model:value=computed setter
   * 会先写 ref，@change 再调 action，此时 oldVal 已被覆盖。
   */
  watch(ringStopCondition, (newCond, oldCond) => {
    if (newCond === oldCond) return;
    replyAcknowledged.value = false;
    if (newCond === 'any_one' && oldCond === 'all_visitors') {
      // all_visitors → any_one：从 visitorLastMsgTime 重建队列（响铃源切到 pendingRingConvs）
      const me = agentId.value;
      const next = new Set<string>();
      visitorLastMsgTime.forEach((_ts, cid) => {
        const c = conversations.value.find((x: any) => x.id === cid);
        if (!c) return;
        if (!c.ownerAgentId || c.ownerAgentId === me) next.add(cid);
      });
      pendingRingConvs.value = next;
    } else if (newCond === 'all_visitors' && oldCond === 'any_one') {
      // any_one → all_visitors：清空 pendingRingConvs，tick 改读 visitorLastMsgTime
      pendingRingConvs.value = new Set<string>();
    }
  });

  const myUnreadTotal = computed(() => {
    if (!agentId.value) return 0;
    let total = 0;
    for (const c of conversations.value) {
      if (c.ownerAgentId !== agentId.value) continue;
      if (c.status === 2) continue;
      const n = c.unreadCount || 0;
      if (n > 0) total += n;
    }
    return total;
  });

  const continuousRingActive = computed(() => continuousRingMode.value !== 'off');
  const isRingPaused = computed(() => Date.now() < ringPausedUntil.value);

  const events = new MiniEmitter();

  function upsertConversation(conv: any) {
    if (!conv?.id) return;
    const i = conversations.value.findIndex((c) => c.id === conv.id);
    if (i >= 0) {
      Object.assign(conversations.value[i], conv);
    } else {
      conversations.value.unshift(conv);
    }
  }

  function removeConversation(id: string) {
    if (!id) return;
    const i = conversations.value.findIndex((c) => c.id === id);
    if (i >= 0) conversations.value.splice(i, 1);
  }

  function updateConversation(id: string, patch: Record<string, any>) {
    if (!id || !patch) return;
    const c = conversations.value.find((x) => x.id === id);
    if (c) Object.assign(c, patch);
  }

  function mergeRemoteList(remote: any[]) {
    if (!Array.isArray(remote)) return;
    const remoteIds = new Set<string>(remote.map((c: any) => c.id));
    const localIndex = new Map<string, any>();
    conversations.value.forEach((c: any) => localIndex.set(c.id, c));

    for (let i = conversations.value.length - 1; i >= 0; i--) {
      const c = conversations.value[i];
      if (!remoteIds.has(c.id)) {
        conversations.value.splice(i, 1);
      }
    }

    remote.forEach((rc: any) => {
      const local = localIndex.get(rc.id);
      if (!local) {
        conversations.value.push(rc);
      } else {
        Object.keys(rc).forEach((k) => {
          if (local[k] !== rc[k]) {
            local[k] = rc[k];
          }
        });
      }
    });
  }

  function sortConversations() {
    conversations.value.sort((a, b) => {
      const aStar = a.visitorStar || 0;
      const bStar = b.visitorStar || 0;
      if (aStar !== bStar) return bStar - aStar;
      if (aStar === 1 && bStar === 1) {
        const aStarTime = a.visitorStarTime ? new Date(a.visitorStarTime).getTime() : 0;
        const bStarTime = b.visitorStarTime ? new Date(b.visitorStarTime).getTime() : 0;
        if (aStarTime !== bStarTime) return bStarTime - aStarTime;
      }

      const aUnread = a.unreadCount || 0;
      const bUnread = b.unreadCount || 0;
      if (aUnread > 0 && bUnread === 0) return -1;
      if (aUnread === 0 && bUnread > 0) return 1;

      const aTime = a.lastMessageTime ? new Date(a.lastMessageTime).getTime() : 0;
      const bTime = b.lastMessageTime ? new Date(b.lastMessageTime).getTime() : 0;
      return bTime - aTime;
    });
  }

  function clearConversations() {
    conversations.value = [];
  }

  function setAgentId(id: string) {
    agentId.value = id || '';
  }

  function setAgentInfo(info: any) {
    agentInfo.value = info || null;
  }

  function setWsStatus(s: WsStatus) {
    wsStatus.value = s;
    events.emit('ws_status_changed', s);
  }

  function setWsBanner(show: boolean) {
    wsShowBanner.value = !!show;
  }

  function setWsReconnectCountdown(n: number) {
    wsReconnectCountdown.value = Math.max(0, n | 0);
  }

  function setSoundEnabled(v: boolean) {
    soundEnabled.value = !!v;
    try {
      localStorage.setItem(SOUND_ENABLED_KEY, v ? '1' : '0');
    } catch (_) {
      // ignore
    }
  }

  function setSoundVolume(n: number) {
    const v = Math.max(0, Math.min(200, n | 0));
    soundVolumePercent.value = v;
    try {
      localStorage.setItem(SOUND_VOLUME_KEY, String(v));
    } catch (_) {
      // ignore
    }
  }

  // ============ 访客等待（visitorLastMsgTime） ============
  function markVisitorWaiting(id: string, ts?: number) {
    if (!id) return;
    visitorLastMsgTime.set(id, ts || Date.now());
  }

  function clearVisitorWaiting(id: string) {
    if (!id) return;
    if (visitorLastMsgTime.delete(id)) {
      // 通知 workbench 私有 UI 状态（timeoutNotifiedSet / visitorWaitingSeconds）同步清理
      events.emit('visitor_waiting_cleared', id);
    }
  }

  function clearAllVisitorWaiting() {
    if (visitorLastMsgTime.size === 0) return;
    visitorLastMsgTime.clear();
    events.emit('visitor_waiting_cleared_all');
  }

  // ============ 持续响铃 actions（仅写状态；timer/Electron 副作用由 composable 内 watch 触发） ============
  function setContinuousRingMode(v: ContinuousRingMode) {
    if (!VALID_RING_MODES.includes(v)) return;
    continuousRingMode.value = v;
  }

  function setRingStopCondition(v: RingStopCondition) {
    if (!VALID_RING_STOPS.includes(v)) return;
    // 仅写 ref，副作用由 watch(ringStopCondition) 统一触发（兼容 v-model 直接写 ref 路径）
    ringStopCondition.value = v;
  }

  function setRingIntervalSeconds(n: number) {
    const num = Number(n);
    if (!VALID_RING_INTERVALS.includes(num)) return;
    ringIntervalSeconds.value = num;
  }

  function enqueueContinuousRing(id: string) {
    if (!id) return;
    if (continuousRingMode.value === 'off') return;
    // 任何新的访客消息 → 重置 acknowledged（"新一轮提醒"，重新开响）
    replyAcknowledged.value = false;
    if (ringStopCondition.value === 'all_visitors') {
      // 全局源模式只看 visitorLastMsgTime，不维护 pendingRingConvs
      return;
    }
    if (!pendingRingConvs.value.has(id)) {
      const next = new Set(pendingRingConvs.value);
      next.add(id);
      pendingRingConvs.value = next;
    }
  }

  function dequeueContinuousRing(id: string) {
    if (!id) return;
    if (pendingRingConvs.value.has(id)) {
      const next = new Set(pendingRingConvs.value);
      next.delete(id);
      pendingRingConvs.value = next;
    }
    // any_one 模式：客服回复了任一会话 → 立即标记，tick 内将停响
    if (ringStopCondition.value === 'any_one') {
      replyAcknowledged.value = true;
    }
  }

  /**
   * 基于 visitorLastMsgTime + conversations 重建 pendingRingConvs。
   * 用于 mode off→on / loadConversationsList 完成 / 停止条件切换。
   */
  function rebuildPendingFromVisitorWaiting() {
    const me = agentId.value;
    const next = new Set<string>();
    visitorLastMsgTime.forEach((_ts, cid) => {
      const c = conversations.value.find((x: any) => x.id === cid);
      if (!c) return;
      if (!c.ownerAgentId || c.ownerAgentId === me) next.add(cid);
    });
    pendingRingConvs.value = next;
    // 重建队列 = 页面刷新 / 初次加载 / 模式切换，应从干净状态开始
    replyAcknowledged.value = false;
  }

  function clearAllRing() {
    pendingRingConvs.value = new Set<string>();
    replyAcknowledged.value = false;
    ringPausedUntil.value = 0;
    pauseRemainSeconds.value = 0;
  }

  function pauseRing(minutes: number) {
    const ms = Math.max(0, Number(minutes) || 0) * 60 * 1000;
    if (ms <= 0) return;
    ringPausedUntil.value = Date.now() + ms;
    pauseRemainSeconds.value = Math.ceil(ms / 1000);
  }

  function resumeRing() {
    ringPausedUntil.value = 0;
    pauseRemainSeconds.value = 0;
    // 暂停结束 → 给客服一次"重新被提醒"的机会
    replyAcknowledged.value = false;
  }

  function reset() {
    conversations.value = [];
    agentId.value = '';
    agentInfo.value = null;
    wsStatus.value = 'disconnected';
    wsShowBanner.value = false;
    wsReconnectCountdown.value = 0;
    // 持续响铃 + 访客等待终态清理（登出 / SSO 互踢 / quota 踢出）
    clearAllRing();
    clearAllVisitorWaiting();
    // soundEnabled / soundVolumePercent / 持续响铃三档配置 保留，跨会话保留用户偏好
  }

  return {
    conversations,
    agentId,
    agentInfo,
    wsStatus,
    wsShowBanner,
    wsReconnectCountdown,
    soundEnabled,
    soundVolumePercent,
    myUnreadTotal,

    // 持续响铃状态
    continuousRingMode,
    ringStopCondition,
    ringIntervalSeconds,
    ringPausedUntil,
    pauseRemainSeconds,
    pendingRingConvs,
    replyAcknowledged,
    visitorLastMsgTime,
    continuousRingActive,
    isRingPaused,

    events,
    upsertConversation,
    removeConversation,
    updateConversation,
    mergeRemoteList,
    sortConversations,
    clearConversations,
    setAgentId,
    setAgentInfo,
    setWsStatus,
    setWsBanner,
    setWsReconnectCountdown,
    setSoundEnabled,
    setSoundVolume,

    // 访客等待 actions
    markVisitorWaiting,
    clearVisitorWaiting,
    clearAllVisitorWaiting,

    // 持续响铃 actions
    setContinuousRingMode,
    setRingStopCondition,
    setRingIntervalSeconds,
    enqueueContinuousRing,
    dequeueContinuousRing,
    rebuildPendingFromVisitorWaiting,
    clearAllRing,
    pauseRing,
    resumeRing,

    reset,
  };
});

export function useCsStoreWithOut() {
  return useCsStore(store);
}
