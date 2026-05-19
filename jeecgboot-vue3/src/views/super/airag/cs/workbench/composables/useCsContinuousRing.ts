/**
 * 客服工作台 - 持续响铃 composable
 *
 * 在「新消息提示音」总开关之上提供两层细粒度控制：
 *   1) 持续响铃模式（off / on_blur / always）：决定在什么焦点状态下持续响
 *   2) 停止条件（any_one / all_visitors）：决定单条会话回复够不够停响
 *
 * 入队是无条件的（仅看 isMyConv + 模式 ≠ off），是否实际播放完全由 tick 内
 * 的 focusMatchesMode() 实时判定 —— 这样 "模式 = on_blur 时用户当前聚焦，
 * 5 秒后切走" 等场景才能正确响铃。
 *
 * Electron 端注意：tray.ts 的 startBlink 是自递归 setTimeout 循环，
 * 不能每个 tick 调用 trayFlash，否则递归叠加导致 CPU 飙升、图标乱闪。
 * 因此用 isElectronFlashing 状态机去重，只在「首次开始响 → 显式停响」
 * 两个边界各调一次。
 */
import { ref, computed, watch, type ComputedRef, type Ref } from 'vue';
import { useGlobSetting } from '/@/hooks/setting';
import { ElectronEnum } from '/@/enums/jeecgEnum';

export type ContinuousRingMode = 'off' | 'on_blur' | 'always';
/**
 * 停止条件：
 *   - any_one        客服回复任一未读会话即停响（响铃仅为提醒"有事来了"）
 *   - all_visitors   必须回复完所有未回复访客才停响（响铃是任务清单，必须清空）
 */
export type RingStopCondition = 'any_one' | 'all_visitors';

const MODE_KEY = 'cs_workbench_continuous_ring_mode';
const STOP_KEY = 'cs_workbench_ring_stop_condition';
const INTERVAL_KEY = 'cs_workbench_ring_interval';

const VALID_MODES: ContinuousRingMode[] = ['off', 'on_blur', 'always'];
const VALID_STOPS: RingStopCondition[] = ['any_one', 'all_visitors'];
const VALID_INTERVALS = [3, 5, 10, 15];

function readMode(): ContinuousRingMode {
  const raw = localStorage.getItem(MODE_KEY);
  return VALID_MODES.includes(raw as ContinuousRingMode) ? (raw as ContinuousRingMode) : 'off';
}

function readStopCondition(): RingStopCondition {
  const raw = localStorage.getItem(STOP_KEY);
  // 旧值兼容：this_conversation 旧实现实际等价于"全部回复才停"，但新语义下默认更友好的"回复任一即停"
  if (raw === 'this_conversation') return 'any_one';
  return VALID_STOPS.includes(raw as RingStopCondition) ? (raw as RingStopCondition) : 'any_one';
}

function readInterval(): number {
  const raw = localStorage.getItem(INTERVAL_KEY);
  const n = raw != null ? parseInt(raw, 10) : NaN;
  return VALID_INTERVALS.includes(n) ? n : 5;
}

export interface CsContinuousRingDeps {
  /** 路由是否仍在工作台 */
  isOnWorkbench: () => boolean;
  /** 总开关（soundEnabled）是否打开 */
  isSoundEnabled: () => boolean;
  /** 当前焦点是否在工作台（visibilityState=visible && document.hasFocus()） */
  isInForeground: () => boolean;
  /** 播一次声音（复用 index.vue 已有 audioCtx + playCsNotificationSound） */
  playOnce: () => void;
  /** all_visitors 模式时使用，遍历 visitorLastMsgTime + conversations 判定 */
  hasAnyMyVisitorWaiting: () => boolean;
  /** 用于 rebuildPendingFromVisitorWaiting：返回所有"属于本客服且未回复"的 convId 列表 */
  collectMyWaitingConvIds: () => string[];
}

export interface CsContinuousRingApi {
  // 持久化字段
  continuousRingMode: Ref<ContinuousRingMode>;
  ringStopCondition: Ref<RingStopCondition>;
  ringIntervalSeconds: Ref<number>;

  // 内存状态
  ringPausedUntil: Ref<number>;
  pendingRingConvs: Ref<Set<string>>;
  continuousRingActive: ComputedRef<boolean>;
  isRingPaused: ComputedRef<boolean>;
  pauseRemainSeconds: Ref<number>;

  // 操作
  enqueueContinuousRing: (convId: string) => void;
  dequeueContinuousRing: (convId: string) => void;
  rebuildPendingFromVisitorWaiting: () => void;
  clearAllRing: () => void;
  pauseRing: (minutes: number) => void;
  resumeRing: () => void;
  ensureTimerStarted: () => void;
  onUnmount: () => void;

  // 设置回调（直接改 ref 也可，封装回调便于设置抽屉直接绑定）
  onContinuousRingModeChange: (v: ContinuousRingMode) => void;
  onRingStopConditionChange: (v: RingStopCondition) => void;
  onRingIntervalChange: (v: number) => void;
  onPauseRing: (minutes: number) => void;
  onResumeRing: () => void;
}

export function useCsContinuousRing(deps: CsContinuousRingDeps): CsContinuousRingApi {
  const globSetting = useGlobSetting();

  const continuousRingMode = ref<ContinuousRingMode>(readMode());
  const ringStopCondition = ref<RingStopCondition>(readStopCondition());
  const ringIntervalSeconds = ref<number>(readInterval());

  const ringPausedUntil = ref<number>(0);
  const pendingRingConvs = ref<Set<string>>(new Set<string>());
  const pauseRemainSeconds = ref<number>(0);

  /**
   * any_one 模式专用状态机：客服在本轮响铃中是否已回复过至少一个会话。
   *   - dequeue 时置 true → tick 内立即停响（即使队列还有未回复）
   *   - 新的访客消息 enqueue 进来 → 重置 false（"新一轮提醒"）
   *   - 队列空 / 模式切换 / 暂停恢复 / 路由离开 / 重建队列 → 重置 false
   * 不影响 all_visitors 模式（该模式只看 visitorLastMsgTime 是否还有未回复）
   */
  const replyAcknowledged = ref(false);

  const continuousRingActive = computed(() => continuousRingMode.value !== 'off');
  const isRingPaused = computed(() => Date.now() < ringPausedUntil.value);

  // 持久化 watch
  watch(continuousRingMode, (v) => localStorage.setItem(MODE_KEY, v));
  watch(ringStopCondition, (v) => localStorage.setItem(STOP_KEY, v));
  watch(ringIntervalSeconds, (v) => localStorage.setItem(INTERVAL_KEY, String(v)));

  // ============ Electron 闪烁状态机 ============
  let isElectronFlashing = false;

  function getElectronApi(): any {
    if (!globSetting.isElectronPlatform) return null;
    return (window as any)[ElectronEnum.ELECTRON_API] ?? null;
  }

  function startElectronFlashIfNeeded() {
    if (isElectronFlashing) return;
    const api = getElectronApi();
    if (!api) return;
    try {
      api.sendNotifyFlash?.();
      api.trayFlash?.();
      isElectronFlashing = true;
    } catch {
      /* 忽略 Electron API 异常 */
    }
  }

  function tryStopElectronFlash() {
    if (!isElectronFlashing) return;
    const api = getElectronApi();
    try { api?.trayFlashStop?.(); } catch { /* ignore */ }
    isElectronFlashing = false;
  }

  // ============ 全局定时器 ============
  let ringTimer: ReturnType<typeof setInterval> | null = null;
  let pauseTickTimer: ReturnType<typeof setInterval> | null = null;

  function focusMatchesMode(): boolean {
    const mode = continuousRingMode.value;
    if (mode === 'off') return false;
    if (mode === 'always') return true;
    /* on_blur */
    return !deps.isInForeground();
  }

  function tick() {
    // 总开关或路由不在工作台 → 同步停闪并跳过
    if (!deps.isSoundEnabled() || !deps.isOnWorkbench()) {
      tryStopElectronFlash();
      return;
    }
    // 暂停期内
    if (Date.now() < ringPausedUntil.value) return;

    const mode = continuousRingMode.value;
    if (mode === 'off') {
      tryStopElectronFlash();
      return;
    }
    const inFg = deps.isInForeground();
    const focusOk = mode === 'always' || (mode === 'on_blur' && !inFg);
    if (!focusOk) {
      // 焦点恢复时 win.on('focus') 已自动 stopBlink（tray.ts:118），这里仍主动同步状态
      tryStopElectronFlash();
      return;
    }

    // any_one：客服已回复任一会话 → 立即停响（核心差异点 vs all_visitors）
    if (ringStopCondition.value === 'any_one' && replyAcknowledged.value) {
      tryStopElectronFlash();
      return;
    }

    const stillNeed = ringStopCondition.value === 'all_visitors'
      ? deps.hasAnyMyVisitorWaiting()
      : pendingRingConvs.value.size > 0;
    if (!stillNeed) {
      tryStopElectronFlash();
      return;
    }

    deps.playOnce();
    if (!inFg) startElectronFlashIfNeeded();
  }

  function ensureTimerStarted() {
    if (ringTimer != null) return;
    if (continuousRingMode.value === 'off') return;
    const ms = Math.max(1, ringIntervalSeconds.value) * 1000;
    ringTimer = setInterval(tick, ms);
  }

  function stopTimer() {
    if (ringTimer != null) {
      clearInterval(ringTimer);
      ringTimer = null;
    }
  }

  function restartTimer() {
    stopTimer();
    ensureTimerStarted();
  }

  // ============ 暂停剩余时间刷新（仅在暂停期运行） ============
  function startPauseTick() {
    if (pauseTickTimer != null) return;
    pauseTickTimer = setInterval(() => {
      const remainMs = ringPausedUntil.value - Date.now();
      if (remainMs <= 0) {
        pauseRemainSeconds.value = 0;
        stopPauseTick();
        ringPausedUntil.value = 0;
      } else {
        pauseRemainSeconds.value = Math.ceil(remainMs / 1000);
      }
    }, 1000);
  }

  function stopPauseTick() {
    if (pauseTickTimer != null) {
      clearInterval(pauseTickTimer);
      pauseTickTimer = null;
    }
  }

  // ============ 入队 / 出队 / 重建 ============
  function enqueueContinuousRing(convId: string) {
    if (!convId) return;
    if (continuousRingMode.value === 'off') return;

    // 任何新的访客消息进来 → 重置 acknowledged（"新一轮提醒"，重新开响）
    // 这样客服回复了 A 之后 B 又来新消息，仍然会被提醒到
    replyAcknowledged.value = false;

    if (ringStopCondition.value === 'all_visitors') {
      // 全局源模式只看 visitorLastMsgTime，不维护 pendingRingConvs
      ensureTimerStarted();
      return;
    }
    // any_one 模式也维护 pendingRingConvs，作为"队列空了→自然停响"的判定源
    if (!pendingRingConvs.value.has(convId)) {
      const next = new Set(pendingRingConvs.value);
      next.add(convId);
      pendingRingConvs.value = next;
    }
    ensureTimerStarted();
  }

  function dequeueContinuousRing(convId: string) {
    if (!convId) return;
    if (pendingRingConvs.value.has(convId)) {
      const next = new Set(pendingRingConvs.value);
      next.delete(convId);
      pendingRingConvs.value = next;
    }
    // any_one 模式：客服回复了任一会话 → 立即标记，tick 内将停响
    // all_visitors 模式：不设置该标记，仍需所有 visitorLastMsgTime 清零才停
    if (ringStopCondition.value === 'any_one') {
      replyAcknowledged.value = true;
      tryStopElectronFlash();
    }
    // 队列清空 + 全局源也无未回复 → 主动停闪（tick 也会处理，但提前一拍更稳）
    if (pendingRingConvs.value.size === 0 && !deps.hasAnyMyVisitorWaiting()) {
      tryStopElectronFlash();
    }
  }

  function rebuildPendingFromVisitorWaiting() {
    const ids = deps.collectMyWaitingConvIds();
    pendingRingConvs.value = new Set(ids);
    // 重建队列 = 页面刷新/初次加载，应从干净状态开始（让存量未回复仍能响）
    replyAcknowledged.value = false;
    if (ids.length > 0) ensureTimerStarted();
  }

  function clearAllRing() {
    pendingRingConvs.value = new Set();
    replyAcknowledged.value = false;
    stopTimer();
    tryStopElectronFlash();
  }

  function pauseRing(minutes: number) {
    const ms = Math.max(0, minutes) * 60 * 1000;
    if (ms <= 0) return;
    ringPausedUntil.value = Date.now() + ms;
    pauseRemainSeconds.value = Math.ceil(ms / 1000);
    tryStopElectronFlash();
    startPauseTick();
  }

  function resumeRing() {
    ringPausedUntil.value = 0;
    pauseRemainSeconds.value = 0;
    // 暂停结束 → 给客服一次"重新被提醒"的机会（避免恢复后立刻被旧的 acknowledged 静默掉）
    replyAcknowledged.value = false;
    stopPauseTick();
  }

  // ============ 模式 / 停止条件 / 间隔的 watch ============
  watch(continuousRingMode, (newMode, oldMode) => {
    if (newMode === 'off') {
      clearAllRing();
    } else if (oldMode === 'off') {
      // 已有未回复存量时立刻响
      ensureTimerStarted();
    } else {
      // on_blur ↔ always：tick 内 focusMatchesMode 自动衔接，无需重启 timer
    }
  });

  watch(ringStopCondition, (newCond, oldCond) => {
    if (newCond === oldCond) return;
    // 切换停止条件 → 重置 acknowledged，让新模式从干净状态开始
    replyAcknowledged.value = false;
    if (newCond === 'any_one' && oldCond === 'all_visitors') {
      // all_visitors → any_one：从 visitorLastMsgTime 重建队列，作为"队列空"的判定源
      rebuildPendingFromVisitorWaiting();
    } else if (newCond === 'all_visitors' && oldCond === 'any_one') {
      // any_one → all_visitors：清空 pendingRingConvs，tick 改读 visitorLastMsgTime 自动衔接
      pendingRingConvs.value = new Set();
      if (continuousRingMode.value !== 'off') ensureTimerStarted();
    }
  });

  watch(ringIntervalSeconds, () => {
    if (ringTimer != null) restartTimer();
  });

  // ============ 卸载 ============
  function onUnmount() {
    clearAllRing();
    stopPauseTick();
  }

  // ============ 设置抽屉的回调封装 ============
  function onContinuousRingModeChange(v: ContinuousRingMode) {
    if (VALID_MODES.includes(v)) continuousRingMode.value = v;
  }
  function onRingStopConditionChange(v: RingStopCondition) {
    if (VALID_STOPS.includes(v)) ringStopCondition.value = v;
  }
  function onRingIntervalChange(v: number) {
    if (VALID_INTERVALS.includes(v)) ringIntervalSeconds.value = v;
  }
  function onPauseRing(minutes: number) {
    pauseRing(minutes);
  }
  function onResumeRing() {
    resumeRing();
  }

  return {
    continuousRingMode,
    ringStopCondition,
    ringIntervalSeconds,
    ringPausedUntil,
    pendingRingConvs,
    continuousRingActive,
    isRingPaused,
    pauseRemainSeconds,
    enqueueContinuousRing,
    dequeueContinuousRing,
    rebuildPendingFromVisitorWaiting,
    clearAllRing,
    pauseRing,
    resumeRing,
    ensureTimerStarted,
    onUnmount,
    onContinuousRingModeChange,
    onRingStopConditionChange,
    onRingIntervalChange,
    onPauseRing,
    onResumeRing,
  };
}
