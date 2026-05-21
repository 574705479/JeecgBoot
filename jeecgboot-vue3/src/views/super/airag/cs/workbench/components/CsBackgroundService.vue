<template>
  <span class="cs-bg-service" style="display: none" aria-hidden="true">cs-bg</span>
</template>

<script lang="ts">
import { defineComponent, onMounted, onUnmounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { defHttp } from '/@/utils/http/axios';
import { getToken } from '/@/utils/auth';
import { useGlobSetting } from '/@/hooks/setting';
import { ElectronEnum } from '/@/enums/jeecgEnum';
import { useUserStoreWithOut } from '/@/store/modules/user';
import { useCsStore } from '/@/store/modules/cs';
import { CsWsClient } from '/@/views/super/airag/cs/services/CsWsClient';
import { decryptMessage, decryptTransport } from '/@/views/super/airag/cs/utils/csEncrypt';
import { playCsNotificationSound, CS_NOTIFY_MAX_GAIN } from '/@/views/super/airag/cs/utils/csNotificationSound';
import {
  stripHtmlTags,
  buildMessagePreview,
} from '/@/views/super/airag/cs/workbench/render/csMessageRender';
import { useCsContinuousRing } from '/@/views/super/airag/cs/workbench/composables/useCsContinuousRing';

const httpGet = (config: any) => defHttp.get(config, { successMessageMode: 'none' });

function getWsBaseUrl() {
  const globSetting = useGlobSetting();
  const { apiUrl, domainUrl, urlPrefix } = globSetting;
  let base = /^https?:\/\//.test(apiUrl) ? apiUrl : '';
  if (!base && /^https?:\/\//.test(domainUrl)) {
    base = domainUrl;
  }
  if (!base) {
    base = globSetting.apiUrl || window.location.origin;
  }
  let parsed: URL;
  try {
    parsed = new URL(base);
  } catch {
    parsed = new URL(globSetting.apiUrl || window.location.origin);
  }
  const wsProtocol = parsed.protocol === 'https:' ? 'wss:' : 'ws:';
  let prefix = urlPrefix || parsed.pathname || '';
  if (prefix && !prefix.startsWith('/')) {
    prefix = `/${prefix}`;
  }
  prefix = prefix.replace(/\/$/, '');
  return `${wsProtocol}//${parsed.host}${prefix}`;
}

const SOUND_THROTTLE_MS = 1500;
const NOTIFY_THROTTLE_MS = 2000;
const TITLE_FLASH_INTERVAL_MS = 1000;

function getDisplayName(conv: any): string {
  if (!conv) return '新消息';
  return conv.visitorNickname || conv.userName || '访客';
}

function getMessageAttachments(msg: any): any[] {
  if (!msg) return [];
  const extra = msg.extra;
  if (!extra) return [];
  let parsed: any = extra;
  if (typeof extra === 'string') {
    try {
      parsed = JSON.parse(extra);
    } catch {
      return [];
    }
  }
  if (Array.isArray(parsed?.attachments)) return parsed.attachments;
  if (Array.isArray(parsed)) return parsed;
  return [];
}

export default defineComponent({
  name: 'CsBackgroundService',
  setup() {
    const csStore = useCsStore();
    const userStore = useUserStoreWithOut();
    const route = useRoute();
    const globSetting = useGlobSetting();

    let wsClient: CsWsClient | null = null;
    let bootstrapped = false;
    let disposed = false;

    let audioCtx: AudioContext | null = null;
    let lastSoundTime = 0;
    const lastNotifyMap = new Map<string, number>();
    const activeNotifications = new Map<string, Notification>();

    let titleFlashTimer: number | null = null;
    let titleFlashing = false;
    let titleFlashAlt = '【新消息】';

    function isWorkbenchVisible(): boolean {
      if (route.path !== '/cs/workbench') return false;
      if (typeof document === 'undefined') return false;
      if (document.visibilityState !== 'visible') return false;
      if (typeof document.hasFocus === 'function' && !document.hasFocus()) return false;
      return true;
    }

    function getOriginalTitle(): string {
      const t = document.title || '';
      if (t.startsWith(titleFlashAlt + ' ')) {
        return t.slice(titleFlashAlt.length + 1);
      }
      return t;
    }

    function startTitleFlash() {
      if (titleFlashTimer) return;
      let toggle = false;
      const tick = () => {
        if (disposed) {
          stopTitleFlash();
          return;
        }
        if (isWorkbenchVisible()) {
          stopTitleFlash();
          return;
        }
        toggle = !toggle;
        const base = getOriginalTitle();
        if (toggle) {
          document.title = `${titleFlashAlt} ${base}`;
        } else {
          document.title = base;
        }
      };
      titleFlashTimer = window.setInterval(tick, TITLE_FLASH_INTERVAL_MS);
      titleFlashing = true;
      tick();
    }

    function stopTitleFlash() {
      if (titleFlashTimer) {
        clearInterval(titleFlashTimer);
        titleFlashTimer = null;
      }
      if (titleFlashing) {
        document.title = getOriginalTitle();
      }
      titleFlashing = false;
    }

    function tryStopTitleFlash() {
      if (isWorkbenchVisible()) stopTitleFlash();
    }

    function playSoundOnce() {
      if (!csStore.soundEnabled) return;
      const now = Date.now();
      if (now - lastSoundTime < SOUND_THROTTLE_MS) return;
      lastSoundTime = now;
      try {
        if (!audioCtx) audioCtx = new AudioContext();
        if (audioCtx.state === 'suspended') audioCtx.resume();
        const mult = Math.max(0, Math.min(CS_NOTIFY_MAX_GAIN, csStore.soundVolumePercent / 100));
        playCsNotificationSound(audioCtx, mult);
      } catch (_) {
        // 忽略音频异常
      }
    }

    function ensureNotificationPermission(): boolean {
      if (typeof Notification === 'undefined') return false;
      if (Notification.permission === 'granted') return true;
      if (Notification.permission === 'default') {
        try {
          Notification.requestPermission().catch(() => {});
        } catch (_) {
          /* ignore */
        }
      }
      return false;
    }

    function notifyNewMessage(conv: any, data: any) {
      if (isWorkbenchVisible()) return;

      const conversationId = data?.conversationId;
      if (!conversationId) return;
      const now = Date.now();
      const lastNotify = lastNotifyMap.get(conversationId) || 0;
      if (now - lastNotify < NOTIFY_THROTTLE_MS) return;
      lastNotifyMap.set(conversationId, now);

      const title = conv ? getDisplayName(conv) : '新消息';
      const attachments = getMessageAttachments({ extra: data?.extra });
      const body =
        buildMessagePreview(data?.content || '', attachments) || '收到一条新消息';

      if (globSetting.isElectronPlatform && (window as any)[ElectronEnum.ELECTRON_API]) {
        const api = (window as any)[ElectronEnum.ELECTRON_API];
        try {
          api.sendNotifyFlash?.();
          api.trayFlash?.();
          api.sendNotification?.(title, body, '/cs/workbench?conversationId=' + conversationId);
        } catch (e) {
          console.warn('[CsBackgroundService] electron notify failed', e);
        }
        startTitleFlash();
        return;
      }

      startTitleFlash();

      if (!ensureNotificationPermission()) return;

      try {
        const prev = activeNotifications.get(conversationId);
        if (prev) {
          try {
            prev.close();
          } catch (_) {
            /* ignore */
          }
        }
        const notification = new Notification(title, { body, tag: conversationId });
        activeNotifications.set(conversationId, notification);
        notification.onclick = () => {
          window.focus();
          try {
            csStore.events.emit('notification_click', { conversationId });
          } catch (_) {
            /* ignore */
          }
          notification.close();
        };
        notification.onclose = () => activeNotifications.delete(conversationId);
      } catch (e) {
        console.warn('[CsBackgroundService] new Notification failed', e);
      }
    }

    function closeAllNotifications() {
      activeNotifications.forEach((n) => {
        try {
          n.close();
        } catch (_) {
          /* ignore */
        }
      });
      activeNotifications.clear();
    }

    // ============ 持续响铃 ============
    /**
     * 持续响铃专用播放：不走 playSoundOnce 的 1.5s 节流（节流由 ringIntervalSeconds 保证），
     * 但仍受总开关 soundEnabled 控制；复用 background 自有 audioCtx + playCsNotificationSound。
     */
    function ringPlayOnce() {
      if (!csStore.soundEnabled) return;
      try {
        if (!audioCtx) audioCtx = new AudioContext();
        if (audioCtx.state === 'suspended') audioCtx.resume();
        const mult = Math.max(0, Math.min(CS_NOTIFY_MAX_GAIN, csStore.soundVolumePercent / 100));
        playCsNotificationSound(audioCtx, mult);
      } catch (_) {
        /* 忽略音频异常 */
      }
    }

    /** all_visitors 模式判定源：是否仍有"我的进行中且访客留言未回复"的会话 */
    function hasAnyMyVisitorWaiting(): boolean {
      if (csStore.visitorLastMsgTime.size === 0) return false;
      const me = csStore.agentId;
      let found = false;
      csStore.visitorLastMsgTime.forEach((_ts, cid) => {
        if (found) return;
        const c = csStore.conversations.find((x: any) => x.id === cid);
        if (!c) return;
        if (c.status === 2) return;
        if (!c.ownerAgentId || c.ownerAgentId === me) found = true;
      });
      return found;
    }

    /**
     * 持续响铃 isInForeground：window 聚焦 AND 标签可见 AND 路由在工作台。
     * 三者全满足才算"前台聚焦"，on_blur 模式下任一不满足即响铃（含跨菜单）。
     */
    function isInForegroundForRing(): boolean {
      if (typeof document === 'undefined') return false;
      if (typeof document.hasFocus === 'function' && !document.hasFocus()) return false;
      if (document.visibilityState !== 'visible') return false;
      if (route.path !== '/cs/workbench') return false;
      return true;
    }

    const ring = useCsContinuousRing({
      isSoundEnabled: () => csStore.soundEnabled,
      isInForeground: isInForegroundForRing,
      playOnce: ringPlayOnce,
      hasAnyMyVisitorWaiting,
    });

    async function loadAgentInfo() {
      try {
        const res = await httpGet({ url: '/cs/agent/current' });
        if (res?.id) {
          csStore.setAgentId(String(res.id));
          csStore.setAgentInfo({
            id: res.id,
            nickname: res.nickname || '客服',
            avatar: res.avatar || '',
            status: res.status || 0,
            role: res.role || 0,
            defaultAppId: res.defaultAppId,
          });
          return true;
        }
      } catch (e) {
        console.warn('[CsBackgroundService] loadAgentInfo failed', e);
      }
      return false;
    }

    async function loadConversationsList() {
      if (!csStore.agentId) return;
      try {
        const res = await httpGet({
          url: '/cs/conversation/list',
          params: {
            agentId: csStore.agentId,
            filter: 'mine',
            pageNo: 1,
            pageSize: 50,
          },
        });
        const list = res?.records || [];
        list.forEach((conv: any) => {
          if (conv.lastMessage) conv.lastMessage = decryptMessage(conv.lastMessage);
          if (conv.satisfactionComment) conv.satisfactionComment = decryptMessage(conv.satisfactionComment);
        });
        csStore.mergeRemoteList(list);
        csStore.sortConversations();

        // 初始化访客等待状态：用后端 visitorLastMsgTime 字段填充 csStore.visitorLastMsgTime，
        // 然后基于此重建持续响铃队列（mode!=off 时自动开响）
        const me = csStore.agentId;
        list.forEach((conv: any) => {
          if (conv.status !== 1) return;
          if (!conv.ownerAgentId || conv.ownerAgentId !== me) return;
          if (!conv.visitorLastMsgTime) return;
          const ts = new Date(conv.visitorLastMsgTime).getTime();
          if (!Number.isFinite(ts) || ts <= 0) return;
          csStore.markVisitorWaiting(conv.id, ts);
        });
        if (csStore.continuousRingMode !== 'off') {
          csStore.rebuildPendingFromVisitorWaiting();
        }

        try {
          csStore.events.emit('conversations_reloaded', { list });
        } catch (_) {
          /* ignore */
        }
      } catch (e) {
        console.warn('[CsBackgroundService] loadConversationsList failed', e);
      }
    }

    function buildWsUrl(): string {
      const wsBase = getWsBaseUrl();
      const token = getToken();
      const aid = csStore.agentId;
      if (!aid) return '';
      return `${wsBase}/ws/cs/agent?userId=${aid}&token=${encodeURIComponent(token || '')}`;
    }

    /**
     * 处理 WS 消息
     *
     * 职责拆分：
     *   - background：维护 csStore.conversations（"我的进行中"集合，用于菜单角标 + 跨菜单通知判定）
     *   - workbench（订阅 ws_message）：维护本地 conversations.value（按当前 filter）+ messages / aiSuggestion / visitorInfo 等当前会话 UI
     *
     * background 的 conversations 写入只关注 ownerAgentId === me（或 unassigned 等需要分配给我的边界），
     * 状态为非 closed（status !== 2）。其它客服的会话不进 csStore.conversations，避免菜单角标误计。
     */
    async function handleWsMessage(data: any) {
      if (!data || typeof data !== 'object') return;
      const myAgentId = csStore.agentId;

      switch (data.type) {
        case 'message': {
          const decryptedContent = decryptMessage(data.content);
          data.content = decryptedContent;
          const conv = csStore.conversations.find((c: any) => c.id === data.conversationId);
          if (conv) {
            const previewText = buildMessagePreview(
              decryptedContent || '',
              getMessageAttachments({ extra: data.extra }),
            );
            const patch: any = {
              lastMessage: previewText || stripHtmlTags(decryptedContent) || decryptedContent,
              lastMessageTime: new Date().toISOString(),
            };
            if (data.senderType === 0) patch.userOnline = true;
            if (data.senderType === 2 && data.senderName && conv.status === 1) {
              patch.lastTalkingAgent = data.senderName;
            }
            if (!isWorkbenchVisible()) {
              // 工作台不可见时无法标记"已读"，未读自增
              patch.unreadCount = Math.max(0, (conv.unreadCount || 0) + 1);
            } else if (data.senderType === 0) {
              // 工作台可见时，若访客发消息且未在该会话页签上，仍计未读（具体清除由 workbench scheduleClearUnread 完成）
              patch.unreadCount = Math.max(0, (conv.unreadCount || 0) + 1);
            }
            csStore.updateConversation(conv.id, patch);
          }
          csStore.sortConversations();

          // 访客发消息：跨菜单通知 + 持续响铃 enqueue
          if (data.senderType === 0 && conv) {
            const isMyConv =
              !conv.ownerAgentId ||
              conv.ownerAgentId === myAgentId ||
              (Array.isArray(conv.collaborators) &&
                conv.collaborators.some((cc: any) => cc.agentId === myAgentId));
            if (isMyConv) {
              if (!isWorkbenchVisible()) playSoundOnce();
              notifyNewMessage(conv, data);
              csStore.markVisitorWaiting(conv.id, Date.now());
              csStore.enqueueContinuousRing(conv.id);
            }
          }
          // 客服回复（含 senderType=2 自己 / 其他客服 / AI 自动回复）：dequeue + 清访客等待
          // ownerAgentId === me 才执行（避免他人会话的回复污染我的状态机）
          if (data.senderType === 2 && conv) {
            const isMyConv = !conv.ownerAgentId || conv.ownerAgentId === myAgentId;
            if (isMyConv) {
              csStore.clearVisitorWaiting(conv.id);
              csStore.dequeueContinuousRing(conv.id);
            }
          }
          break;
        }

        case 'delivery_failed': {
          csStore.updateConversation(data.conversationId, { userOnline: false });
          break;
        }

        case 'conversation_assigned': {
          const extraData = data.extra || data;
          const assignedAgentId = extraData.agentId;
          const assignedAgentName = extraData.agentName || '';
          const assignedAgentAvatar = extraData.agentAvatar || '';
          const targetId = extraData.conversationId;
          const existing = csStore.conversations.find((c: any) => c.id === targetId);

          if (existing) {
            const patch: any = {
              status: 1,
              ownerAgentId: assignedAgentId,
              ownerAgentName: assignedAgentName,
              ownerAgentAvatar: assignedAgentAvatar,
              assignTime: new Date().toISOString(),
            };
            if (extraData.customFields !== undefined) patch.customFields = extraData.customFields;
            csStore.updateConversation(targetId, patch);

            // 若分配给了别人 → 从 csStore（"我的进行中"）移除
            if (assignedAgentId && assignedAgentId !== myAgentId) {
              csStore.removeConversation(targetId);
            }
          } else if (assignedAgentId === myAgentId) {
            // 分配给我但 store 里没有 → 触发一次刷新（fallback poll 兜底）
            loadConversationsList();
          }

          if (assignedAgentId === myAgentId) {
            if (!isWorkbenchVisible()) playSoundOnce();
            const targetConv = csStore.conversations.find((c: any) => c.id === targetId);
            notifyNewMessage(targetConv, {
              ...data,
              content: '访客已请求人工接入',
              conversationId: targetId,
            });
            // 接入时若访客在分配前已有未读 → 立即开响（保留访客等待真实时间戳）
            const lastMsgTime =
              targetConv?.visitorLastMsgTime || extraData.visitorLastMsgTime;
            if (lastMsgTime) {
              const ts = new Date(lastMsgTime).getTime();
              if (Number.isFinite(ts) && ts > 0) {
                csStore.markVisitorWaiting(targetId, ts);
                csStore.enqueueContinuousRing(targetId);
              }
            }
          }
          break;
        }

        case 'new_conversation': {
          const exists = csStore.conversations.find((c: any) => c.id === data.conversationId);
          if (exists) break;
          const convOwnerAgentId = data.extra?.ownerAgentId;
          const convStatus = data.extra?.status;

          if (!data.extra || convStatus === undefined) {
            // extra 缺失 → 兜底刷新
            loadConversationsList();
            break;
          }
          if (convStatus === 2) break; // 已结束会话不入列表

          // 仅当被分配给我时入 csStore（菜单角标语义为"我的进行中"）
          if (convOwnerAgentId === myAgentId) {
            const newConv: any = {
              id: data.conversationId,
              userId: data.senderId,
              userName: data.senderName || '访客',
              appId: data.extra?.appId,
              status: convStatus ?? 0,
              replyMode: data.extra?.replyMode || 0,
              ownerAgentId: convOwnerAgentId,
              createTime: data.extra?.createTime || new Date().toISOString(),
              lastMessageTime: data.extra?.createTime || new Date().toISOString(),
              lastMessage: decryptTransport(data.content) || '',
              unreadCount: 0,
              messageCount: 0,
              userIp: data.extra?.userIp,
              userOs: data.extra?.userOs,
              userOsVersion: data.extra?.userOsVersion,
              userBrowser: data.extra?.userBrowser,
              userBrowserVersion: data.extra?.userBrowserVersion,
              userDeviceId: data.extra?.userDeviceId,
              userCountry: data.extra?.userCountry,
              userProvince: data.extra?.userProvince,
              userCity: data.extra?.userCity,
              userLang: data.extra?.userLang,
              visitorNickname: data.extra?.visitorNickname,
              visitorStar: data.extra?.visitorStar,
              visitorStarTime: data.extra?.visitorStarTime,
            };
            csStore.upsertConversation(newConv);
            csStore.sortConversations();

            if (!isWorkbenchVisible()) playSoundOnce();
            notifyNewMessage(newConv, {
              ...data,
              content: '新访客接入',
              conversationId: data.conversationId,
            });
          }
          break;
        }

        case 'conversation_closed': {
          const extraData = data.extra || data;
          const conversationId = extraData.conversationId || data.conversationId;
          // 关闭即从"我的进行中"集合移除 + 清持续响铃
          csStore.removeConversation(conversationId);
          csStore.clearVisitorWaiting(conversationId);
          csStore.dequeueContinuousRing(conversationId);
          break;
        }

        case 'conversation_transferred': {
          const extraData = data.extra || data;
          const conversationId = extraData.conversationId || data.conversationId;
          const fromAgentId = extraData.fromAgentId;
          const toAgentId = extraData.toAgentId;
          const toAgentName = extraData.toAgentName;
          const toAgentAvatar =
            extraData.toAgentAvatar || extraData.conversation?.ownerAgentAvatar || '';

          if (toAgentId === myAgentId) {
            // 转给我了 → 触发一次列表刷新（保险）
            loadConversationsList();
            const fromName = extraData.fromAgentName || '其他客服';
            // notifyNewMessage 在异步刷新前先用最小占位，避免重复通知
            const targetConv = csStore.conversations.find((c: any) => c.id === conversationId);
            if (!isWorkbenchVisible()) playSoundOnce();
            notifyNewMessage(targetConv, {
              ...data,
              content: `${fromName} 转接了一个会话`,
              conversationId,
            });
            // 转入：若访客在转入前已留未读 → 立即开响（visitorLastMsgTime 由 conv 字段或 ws 后续 'message' 同步）
            const lastMsgTime =
              targetConv?.visitorLastMsgTime ||
              extraData.conversation?.visitorLastMsgTime;
            if (lastMsgTime) {
              const ts = new Date(lastMsgTime).getTime();
              if (Number.isFinite(ts) && ts > 0) {
                csStore.markVisitorWaiting(conversationId, ts);
                csStore.enqueueContinuousRing(conversationId);
              }
            }
          } else if (fromAgentId === myAgentId) {
            // 转给别人 → 从我的列表移除 + 清响铃
            csStore.removeConversation(conversationId);
            csStore.clearVisitorWaiting(conversationId);
            csStore.dequeueContinuousRing(conversationId);
          } else {
            // 旁观者 → 如果列表里有（理论上不会有，因为不是我的），更新 owner
            const exist = csStore.conversations.find((c: any) => c.id === conversationId);
            if (exist) {
              csStore.updateConversation(conversationId, {
                ownerAgentId: toAgentId,
                ownerAgentName: toAgentName,
                ownerAgentAvatar: toAgentAvatar,
              });
            }
          }
          break;
        }

        case 'mode_changed': {
          const extraData = data.extra || data;
          if (extraData.conversationId !== undefined && extraData.newMode !== undefined) {
            csStore.updateConversation(extraData.conversationId, {
              replyMode: extraData.newMode,
            });
          }
          break;
        }

        case 'agent_status_changed':
          // 不影响 csStore.conversations，留给 workbench 处理监控视图
          break;

        case 'ai_stream_complete': {
          const decrypted = decryptMessage(data.content);
          data.content = decrypted;
          const conv = csStore.conversations.find((c: any) => c.id === data.conversationId);
          if (conv) {
            csStore.updateConversation(conv.id, {
              lastMessage: decrypted || conv.lastMessage,
              lastMessageTime: data.timestamp || new Date().toISOString(),
            });
            csStore.sortConversations();
          }
          break;
        }

        case 'user_offline': {
          csStore.updateConversation(data.conversationId, { userOnline: false });
          break;
        }
        case 'user_online': {
          csStore.updateConversation(data.conversationId, { userOnline: true });
          break;
        }

        case 'unread_cleared': {
          csStore.updateConversation(data.conversationId, { unreadCount: 0 });
          // 多端互通：另一端把未读清零（一般是自己在另一端打开会话） → 也视为"已回复访客"
          csStore.clearVisitorWaiting(data.conversationId);
          csStore.dequeueContinuousRing(data.conversationId);
          break;
        }

        case 'visitor_updated': {
          const extraData = data.extra || data;
          const visitor = extraData.visitor || extraData;
          if (!visitor?.userId) break;
          let touched = false;
          csStore.conversations.forEach((conv: any) => {
            if (conv.userId !== visitor.userId) return;
            if (visitor.appId && conv.appId && conv.appId !== visitor.appId) return;
            const patch: any = {};
            if (visitor.nickname) patch.visitorNickname = visitor.nickname;
            if (visitor.star !== undefined) {
              patch.visitorStar = visitor.star;
              patch.visitorStarTime = visitor.starTime || null;
            }
            if (Object.keys(patch).length > 0) {
              csStore.updateConversation(conv.id, patch);
              touched = true;
            }
          });
          if (touched) csStore.sortConversations();
          break;
        }

        case 'agent_timeout_reminder': {
          // 客服超时未回复（后端定时任务推送）：不在工作台时给提示音
          if (!isWorkbenchVisible()) playSoundOnce();
          // 兜底信号：ws 抖动时 'message' 可能丢，超时提醒来了说明真有未回复 → 强制 enqueue
          const cid = data.conversationId || data.extra?.conversationId;
          if (cid) {
            const conv = csStore.conversations.find((c: any) => c.id === cid);
            const isMyConv = conv && (!conv.ownerAgentId || conv.ownerAgentId === myAgentId);
            if (isMyConv) {
              const lastMsgTime =
                conv.visitorLastMsgTime || data.extra?.visitorLastMsgTime;
              const ts = lastMsgTime ? new Date(lastMsgTime).getTime() : Date.now();
              csStore.markVisitorWaiting(cid, Number.isFinite(ts) && ts > 0 ? ts : Date.now());
              csStore.enqueueContinuousRing(cid);
            }
          }
          break;
        }

        case 'quota_kick': {
          // 客服坐席已满，强制下线
          csStore.clearAllRing();
          if (wsClient) {
            wsClient.dispose();
            wsClient = null;
          }
          stopTitleFlash();
          closeAllNotifications();
          csStore.events.emit('ws_quota_exceeded', {
            reason: decryptTransport(data.content) || '客服坐席已满，您已被强制下线',
          });
          userStore.logout(true);
          break;
        }

        // 以下事件不影响 csStore.conversations 列表层，仅由 workbench 处理：
        case 'message_recall':
        case 'ai_suggestion':
        case 'ai_suggestion_stream':
        case 'ai_suggestion_complete':
        case 'ai_suggestion_error':
        case 'ai_typing':
        case 'ai_stream':
        case 'blacklist_changed':
        default:
          break;
      }

      // 转发给 workbench / 其他订阅者做 UI 副作用
      try {
        csStore.events.emit('ws_message', data);
      } catch (e) {
        console.error('[CsBackgroundService] emit ws_message threw', e);
      }
    }

    function handleAppLogout() {
      // 退出登录是终态：先清持续响铃 → dispose ws → 关通知 → 停 title 闪烁 → reset store
      // 注：reset() 内部也会调 clearAllRing，但提前一拍避免回调瞬间还在响
      csStore.clearAllRing();
      stopTitleFlash();
      closeAllNotifications();
      lastNotifyMap.clear();
      if (wsClient) {
        wsClient.dispose();
        wsClient = null;
      }
      csStore.reset();
      bootstrapped = false;
      // 移除事件订阅，避免重复 bind
      try {
        csStore.events.clear('cs_command');
        csStore.events.clear('cs_request_reload');
        csStore.events.clear('cs_force_reconnect');
      } catch (_) {
        /* ignore */
      }
    }

    async function bootstrap() {
      if (bootstrapped) return;
      const ok = await loadAgentInfo();
      if (!ok || !csStore.agentId) {
        // 非客服身份 → graceful 退出，不创建 ws / 不消耗资源
        return;
      }
      bootstrapped = true;

      wsClient = new CsWsClient({
        getUrl: () => buildWsUrl(),
        agentIdProvider: () => csStore.agentId,
        onMessage: (d) => {
          handleWsMessage(d).catch((e) =>
            console.error('[CsBackgroundService] handleWsMessage threw', e),
          );
        },
        onStatusChange: (s) => {
          csStore.setWsStatus(s);
        },
        onSsoKickout: (reason) => {
          console.warn('[CsBackgroundService] SSO 互踢 (4001)', reason);
          csStore.clearAllRing();
          stopTitleFlash();
          closeAllNotifications();
          csStore.events.emit('ws_sso_kickout', { reason });
          userStore.logout(true);
        },
        onSessionReplaced: (reason) => {
          console.warn('[CsBackgroundService] 会话被替换 (4002)', reason);
          csStore.events.emit('ws_session_replaced', { reason });
        },
        onQuotaExceeded: (reason) => {
          console.warn('[CsBackgroundService] 坐席已满 (4005)', reason);
          csStore.clearAllRing();
          stopTitleFlash();
          closeAllNotifications();
          csStore.events.emit('ws_quota_exceeded', { reason });
          userStore.logout(true);
        },
        onTokenExpired: () => {
          console.warn('[CsBackgroundService] token 过期 (401)');
          csStore.events.emit('ws_token_expired');
        },
        onFallbackPoll: async () => {
          await loadConversationsList();
        },
        onReconnectSuccess: async () => {
          // 并行加载，避免 await 串行阻塞
          await Promise.all([loadAgentInfo(), loadConversationsList()]);
          csStore.events.emit('ws_reconnected');
        },
        onReconnectCountdown: (n) => {
          csStore.setWsReconnectCountdown(n);
        },
        onBannerShow: (show) => {
          csStore.setWsBanner(show);
        },
      });

      wsClient.connect();
      wsClient.startFallbackPoll();
      // 并行加载首批数据
      loadConversationsList();

      // 转发 workbench 的 cs_command（如 stop_ai_suggestion）到 ws
      csStore.events.on('cs_command', (cmd: any) => {
        if (wsClient) wsClient.send(cmd);
      });
      // workbench 主动请求拉取一次会话列表
      csStore.events.on('cs_request_reload', () => {
        loadConversationsList();
      });
      // workbench 的"立即重连"按钮
      csStore.events.on('cs_force_reconnect', () => {
        if (wsClient) {
          wsClient.connect();
        } else if (csStore.agentId) {
          // 极端情况：bootstrap 未完成 / 已 dispose，重新初始化
          bootstrap().catch((e) =>
            console.error('[CsBackgroundService] re-bootstrap threw', e),
          );
        }
      });
    }

    function handleVisibilityWatch() {
      if (typeof document === 'undefined') return;
      const onChange = () => {
        if (document.visibilityState === 'visible') tryStopTitleFlash();
      };
      const onFocus = () => tryStopTitleFlash();
      document.addEventListener('visibilitychange', onChange);
      window.addEventListener('focus', onFocus);
      onUnmounted(() => {
        document.removeEventListener('visibilitychange', onChange);
        window.removeEventListener('focus', onFocus);
      });
    }

    onMounted(() => {
      handleVisibilityWatch();
      window.addEventListener('app-logout', handleAppLogout);
      bootstrap().catch((e) =>
        console.error('[CsBackgroundService] bootstrap threw', e),
      );
    });

    watch(
      () => route.path,
      (p) => {
        if (p === '/cs/workbench') tryStopTitleFlash();
        // 在已登出后再切到任意页面时，若 token 与 store 都已清空但 layout 仍 mount，
        // 重新登录回到 default layout 时按守护策略再尝试一次 bootstrap
        if (!bootstrapped && !disposed) {
          const token = getToken();
          if (token) {
            bootstrap().catch((e) =>
              console.error('[CsBackgroundService] re-bootstrap on route change threw', e),
            );
          }
        }
      },
    );

    onUnmounted(() => {
      disposed = true;
      stopTitleFlash();
      closeAllNotifications();
      lastNotifyMap.clear();
      window.removeEventListener('app-logout', handleAppLogout);
      // dispose 持续响铃：停 timer + pauseTick + Electron flash（store 状态由 reset / clearAllRing 单独清）
      try {
        ring.dispose();
      } catch (e) {
        console.warn('[CsBackgroundService] ring.dispose threw', e);
      }
      if (wsClient) {
        wsClient.dispose();
        wsClient = null;
      }
      if (audioCtx) {
        try {
          audioCtx.close();
        } catch (_) {
          /* ignore */
        }
        audioCtx = null;
      }
      bootstrapped = false;
      // 不调 csStore.reset()，因为可能是组件 hot reload 而非真退出
    });

    return {};
  },
});
</script>
