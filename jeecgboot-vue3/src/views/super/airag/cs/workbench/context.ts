/**
 * 客服工作台共享上下文
 *
 * 设计原则：
 *   1. 仅 provide 子组件确实需要的最小集合（只读 ref / computed + 必要 callback）。
 *   2. 不暴露 WebSocket 实例、消息发送、内部缓冲区，这些核心业务保留在父级 index.vue。
 *   3. 字段允许在后续 PR 追加，但不应在已发布字段上做破坏性修改。
 */
import type { InjectionKey, Ref, ComputedRef } from 'vue';
import { inject } from 'vue';
import type { ContinuousRingMode, RingStopCondition } from './composables/useCsContinuousRing';

export type { ContinuousRingMode, RingStopCondition };

/** 当前会话最小契约，子组件仅按需读取其中字段 */
export interface CsConversation {
  id: string;
  status?: number;
  ownerAgentId?: string;
  ownerAgentName?: string;
  customFields?: any;
  visitorNickname?: string;
  userName?: string;
  userId?: string;
  unreadCount?: number;
  visitorStar?: number;
  lastMessage?: string;
  lastMessageTime?: string;
  userOnline?: boolean;
  lastTalkingAgent?: string;
  [key: string]: any;
}

/** 访客信息（详情面板用） */
export interface CsVisitorInfo {
  star?: number;
  level?: number;
  nickname?: string;
  realName?: string;
  phone?: string;
  notes?: string;
  [key: string]: any;
}

/** 解析后的自定义字段 */
export interface CsCustomField {
  label: string;
  value: string;
  [key: string]: any;
}

/** WebSocket 连接状态 */
export type CsWsStatus = 'connected' | 'connecting' | 'reconnecting' | 'disconnected';

/** 主题 CSS 变量集合 */
export type CsThemeVars = Record<string, string>;

/** 设置抽屉里使用的 AI / 声音相关配置（保留在父级、按需暴露给 Drawer） */
export interface CsWorkbenchSettings {
  selectedAppId: Ref<string | undefined>;
  visitorAppId: Ref<string | undefined>;
  aiEnabled: Ref<boolean>;
  aiPrologueEnabled: Ref<boolean>;
  soundEnabled: Ref<boolean>;
  soundVolumePercent: Ref<number>;
  aiAppList: Ref<any[]>;

  // 持续响铃配置
  continuousRingMode: Ref<ContinuousRingMode>;
  ringStopCondition: Ref<RingStopCondition>;
  ringIntervalSeconds: Ref<number>;
  continuousRingActive: ComputedRef<boolean>;
  isRingPaused: ComputedRef<boolean>;
  pauseRemainSeconds: Ref<number>;

  onAppChange: (id: string | undefined) => Promise<void> | void;
  onVisitorAppChange: (id: string | undefined) => Promise<void> | void;
  onAiEnabledChange: (checked: boolean) => Promise<void> | void;
  onAiPrologueEnabledChange: (checked: boolean) => Promise<void> | void;
  onSoundEnabledChange: (val: boolean) => void;

  onContinuousRingModeChange: (v: ContinuousRingMode) => void;
  onRingStopConditionChange: (v: RingStopCondition) => void;
  onRingIntervalChange: (v: number) => void;
  onPauseRing: (minutes: number) => void;
  onResumeRing: () => void;
}

/**
 * 工作台共享上下文：父级 provide / 子组件 inject。
 */
export interface CsWorkbenchContext {
  agentId: Ref<string>;
  agentName: Ref<string>;
  agentAvatar: Ref<string>;
  isOnline: Ref<boolean>;
  isColleagueReadonly: ComputedRef<boolean>;

  currentConversation: Ref<CsConversation | null>;
  visitorInfo: Ref<CsVisitorInfo>;
  userOnline: Ref<boolean>;
  parsedCustomFields: ComputedRef<CsCustomField[]>;
  currentReplyMode: Ref<number>;
  satisfactionPushing: Ref<boolean>;
  satisfactionPushed: Ref<boolean>;

  themeVars: ComputedRef<CsThemeVars>;

  wsStatus: Ref<CsWsStatus>;
  wsShowBanner: Ref<boolean>;
  wsReconnectCountdown: Ref<number>;

  showDetailPanel: Ref<boolean>;
  showSettingsDrawer: Ref<boolean>;

  settings: CsWorkbenchSettings;

  getDisplayName: (conv: any) => string;
  getModeName: (mode: number) => string;
  formatTime: (time: string) => string;

  toggleOnline: (checked: boolean) => Promise<void> | void;
  changeMode: (mode: number) => Promise<void> | void;
  pushSatisfaction: () => Promise<void> | void;
  openTransferModal: () => Promise<void> | void;
  closeConversation: () => Promise<void> | void;
  connectWebSocket: () => void;
}

export const CS_WORKBENCH_CONTEXT_KEY: InjectionKey<CsWorkbenchContext> = Symbol('CsWorkbenchContext');

export function useCsWorkbenchContext(): CsWorkbenchContext {
  const ctx = inject(CS_WORKBENCH_CONTEXT_KEY);
  if (!ctx) {
    throw new Error('[CsWorkbench] useCsWorkbenchContext() 必须在 <CsWorkbench> 组件树内调用');
  }
  return ctx;
}
