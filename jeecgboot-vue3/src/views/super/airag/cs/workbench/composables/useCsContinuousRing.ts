/**
 * 客服持续响铃 - 行为副作用层（迁移后）
 *
 * 职责（迁移后）：
 *   - 仅处理"状态变化 → 启停 timer / Electron flash"等副作用
 *   - 所有持久化字段、运行时状态、行为方法均归 csStore（参见 src/store/modules/cs.ts）
 *   - background 通过 csStore actions 写状态，watch 自动驱动 timer 启停
 *
 * 关键不变量：
 *   - 入队是无条件的（仅看模式 ≠ off + ring 出队由停止条件决定）
 *   - 焦点状态判定保留在 deps.isInForeground 内（含路由判定，承载 on_blur 跨菜单语义）
 *   - tick 内不再硬路由 guard（删除原 isOnWorkbench 判断）
 *
 * Electron 端注意：tray.ts 的 startBlink 是自递归 setTimeout 循环，
 * 不能每个 tick 调用 trayFlash，否则递归叠加导致 CPU 飙升、图标乱闪。
 * 因此用 isElectronFlashing 状态机去重，只在「首次开始响 → 显式停响」
 * 两个边界各调一次。
 */
import { watch } from 'vue';
import { useGlobSetting } from '/@/hooks/setting';
import { ElectronEnum } from '/@/enums/jeecgEnum';
import { useCsStore, type ContinuousRingMode, type RingStopCondition } from '/@/store/modules/cs';

export type { ContinuousRingMode, RingStopCondition };

export interface CsContinuousRingDeps {
  /** 总开关（soundEnabled）是否打开 */
  isSoundEnabled: () => boolean;
  /**
   * 当前是否处于"前台聚焦在工作台"状态。
   * 实现方应同时判定：
   *   document.hasFocus() && document.visibilityState === 'visible' && route.path === '/cs/workbench'
   * on_blur 模式：!isInForeground() 即响（窗口失焦 / 标签切走 / 路由不在工作台）。
   */
  isInForeground: () => boolean;
  /** 播一次声音（复用 background 自有 audioCtx + playCsNotificationSound） */
  playOnce: () => void;
  /** all_visitors 模式时使用，遍历 visitorLastMsgTime + conversations 判定 */
  hasAnyMyVisitorWaiting: () => boolean;
}

export interface CsContinuousRingHandle {
  /** background 卸载时调用：停 timer / pauseTick / Electron flash（store 状态由 csStore.reset 单独清） */
  dispose: () => void;
}

export function useCsContinuousRing(deps: CsContinuousRingDeps): CsContinuousRingHandle {
  const csStore = useCsStore();
  const globSetting = useGlobSetting();

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

  function tick() {
    // 总开关关闭 → 同步停闪并跳过
    if (!deps.isSoundEnabled()) {
      tryStopElectronFlash();
      return;
    }
    // 暂停期内
    if (Date.now() < csStore.ringPausedUntil) return;

    const mode = csStore.continuousRingMode;
    if (mode === 'off') {
      tryStopElectronFlash();
      return;
    }
    const inFg = deps.isInForeground();
    const focusOk = mode === 'always' || (mode === 'on_blur' && !inFg);
    if (!focusOk) {
      // 前台聚焦在工作台时（on_blur）/ off 模式：停闪
      // Electron 焦点恢复时 win.on('focus') 也会自动 stopBlink（tray.ts:118），这里仍主动同步
      tryStopElectronFlash();
      return;
    }

    // any_one：客服已回复任一会话 → 立即停响
    if (csStore.ringStopCondition === 'any_one' && csStore.replyAcknowledged) {
      tryStopElectronFlash();
      return;
    }

    const stillNeed = csStore.ringStopCondition === 'all_visitors'
      ? deps.hasAnyMyVisitorWaiting()
      : csStore.pendingRingConvs.size > 0;
    if (!stillNeed) {
      tryStopElectronFlash();
      return;
    }

    deps.playOnce();
    if (!inFg) startElectronFlashIfNeeded();
  }

  function ensureTimerStarted() {
    if (ringTimer != null) return;
    if (csStore.continuousRingMode === 'off') return;
    const ms = Math.max(1, csStore.ringIntervalSeconds) * 1000;
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
      const remainMs = csStore.ringPausedUntil - Date.now();
      if (remainMs <= 0) {
        // 暂停结束 → 通过 store action 清理（避免直接写 ref）
        csStore.resumeRing();
      } else {
        csStore.pauseRemainSeconds = Math.ceil(remainMs / 1000);
      }
    }, 1000);
  }

  function stopPauseTick() {
    if (pauseTickTimer != null) {
      clearInterval(pauseTickTimer);
      pauseTickTimer = null;
    }
  }

  // ============ watch 驱动副作用 ============
  // 模式切换：off↔on 时启停 timer，off→on 显式重建 pending 让存量未回复立即响
  watch(
    () => csStore.continuousRingMode,
    (newMode, oldMode) => {
      if (newMode === 'off') {
        stopTimer();
        tryStopElectronFlash();
      } else if (oldMode === 'off') {
        // off → on：重建队列让存量未回复立即响（避免开开关时正好没新消息再也不响）
        csStore.rebuildPendingFromVisitorWaiting();
        ensureTimerStarted();
      }
      // on_blur ↔ always：tick 内焦点判定自动衔接，无需重启 timer
    },
  );

  // 间隔变化：重启 timer 让新间隔即时生效
  watch(
    () => csStore.ringIntervalSeconds,
    () => {
      if (ringTimer != null) restartTimer();
    },
  );

  // 暂停时间变化：>0 启 pauseTick / 同步停闪；==0 停 pauseTick
  watch(
    () => csStore.ringPausedUntil,
    (newVal) => {
      if (newVal > 0) {
        tryStopElectronFlash();
        startPauseTick();
      } else {
        stopPauseTick();
      }
    },
  );

  // pendingRingConvs 数量变化：队列首次非空时启 timer，让首条入队立刻响
  watch(
    () => csStore.pendingRingConvs.size,
    (size) => {
      if (size > 0 && csStore.continuousRingMode !== 'off') {
        ensureTimerStarted();
      }
    },
  );

  // ============ dispose（background 卸载时调用） ============
  function dispose() {
    stopTimer();
    stopPauseTick();
    tryStopElectronFlash();
  }

  return { dispose };
}
