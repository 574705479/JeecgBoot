<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    title="会话详情"
    :footer="null"
    width="1200px"
  >
    <div class="conversation-detail">
      <!-- 基本信息 -->
      <a-descriptions :column="3" bordered size="small" class="top-info">
        <a-descriptions-item label="会话ID" :span="2">
          <ATypographyText copyable>{{ record?.id }}</ATypographyText>
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getStatusColor(record?.status)">{{ getStatusText(record?.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="访客">
          <span class="user-info">
            <a-avatar size="small">{{ (record?.visitorNickname || record?.userName || '访').charAt(0) }}</a-avatar>
            {{ record?.visitorNickname || record?.userName || record?.userId || '匿名访客' }}
            <template v-if="parsedCustomFields.length">
              <a-tag v-for="cf in parsedCustomFields" :key="cf.label" color="red" size="small" style="margin-left: 4px;">
                {{ cf.label }}: {{ cf.value }}
              </a-tag>
            </template>
          </span>
        </a-descriptions-item>
        <a-descriptions-item label="负责客服">
          {{ record?.ownerAgentName || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="回复模式">
          <a-tag :color="getModeColor(record?.replyMode)">{{ getModeText(record?.replyMode) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="来源渠道">{{ record?.source || '直接访问' }}</a-descriptions-item>
        <a-descriptions-item label="满意度">
          <a-rate :value="record?.satisfaction || 0" disabled allow-half :count="5" />
          <span v-if="record?.satisfactionComment" class="satisfaction-comment">
            "{{ record.satisfactionComment }}"
          </span>
        </a-descriptions-item>
        <a-descriptions-item label="结束方式">
          {{ endTypeText }}
        </a-descriptions-item>
        <a-descriptions-item label="消息统计">
          共{{ record?.messageCount || 0 }}条 (客服{{ record?.agentMessageCount || 0 }} / 访客{{ record?.visitorMessageCount || 0 }})
        </a-descriptions-item>
        <a-descriptions-item label="首次响应">{{ formatResponseTime(record?.firstResponseSeconds) }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ record?.createTime }}</a-descriptions-item>
        <a-descriptions-item label="接入时间">{{ record?.assignTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="结束时间">{{ record?.endTime || '-' }}</a-descriptions-item>
      </a-descriptions>

      <!-- 左右分栏 -->
      <div class="split-layout">
        <!-- 左侧：消息记录 -->
        <div class="split-left">
          <div class="message-section">
            <div class="section-header">
              <span class="section-title">
                <MessageOutlined /> 消息记录
              </span>
              <span class="message-count">共 {{ messages.length }} 条</span>
            </div>

            <a-spin :spinning="loading">
              <div class="message-list" ref="messageListRef" @scroll.passive="handleMessageScroll">
                <template v-if="messages.length > 0">
                  <div v-for="msg in displayMessages" :key="msg.id" :class="['message-item', getMsgClass(msg)]">
                    <template v-if="msg.senderType === 3">
                      <div class="system-message">
                        <span class="system-text">{{ msg.content }}</span>
                        <span class="system-time">{{ formatTime(msg.createTime) }}</span>
                      </div>
                    </template>

                    <template v-else-if="msg.senderType === 0">
                      <div class="user-message">
                        <a-avatar :size="32" class="msg-avatar user-avatar">
                          {{ (msg.senderName || '访').charAt(0) }}
                        </a-avatar>
                        <div class="msg-content">
                          <div class="msg-header">
                            <span class="sender-name">{{ msg.senderName || '访客' }}</span>
                            <span class="msg-time">{{ formatTime(msg.createTime) }}</span>
                          </div>
                          <div class="msg-bubble user-bubble" v-html="renderMessage(msg.content)"></div>
                          <div v-if="getMediaAttachments(msg).length" class="msg-media-grid" :class="`media-grid--${Math.min(getMediaGridData(msg).total, 4)}`">
                            <div class="media-item" v-for="(item, idx) in getMediaGridData(msg).items" :key="idx">
                              <img v-if="item.type === 'image'" :src="getAttachmentUrl(item)" @click="openImagePreview(msg, item)" />
                              <video v-else :src="getAttachmentUrl(item)" controls playsinline />
                              <div v-if="idx === getMediaGridData(msg).items.length - 1 && getMediaGridData(msg).extraCount > 0" class="media-more" @click.stop="openMediaViewer(msg)">+{{ getMediaGridData(msg).extraCount }}</div>
                            </div>
                          </div>
                          <div v-if="getFileAttachments(msg).length" class="msg-file-list">
                            <div class="file-item" v-for="(item, idx) in getFileAttachments(msg)" :key="idx" @click="openFilePreview(item)">
                              <span class="file-icon">📄</span>
                              <span class="file-name">{{ item.name || item.url }}</span>
                            </div>
                          </div>
                        </div>
                      </div>
                    </template>

                    <template v-else>
                      <div class="agent-message">
                        <div class="msg-content">
                          <div class="msg-header">
                            <span class="msg-time">{{ formatTime(msg.createTime) }}</span>
                            <span class="sender-name">
                              {{ msg.senderName || (isAiMessage(msg) ? 'AI客服' : '客服') }}
                            </span>
                            <a-tag v-if="isAiMessage(msg)" color="purple" size="small">AI</a-tag>
                            <a-tag v-else-if="!isAiMessage(msg)" color="green" size="small">客服</a-tag>
                            <a-tag v-if="msg.status === 3" color="red" size="small">已撤回</a-tag>
                          </div>
                          <div class="msg-bubble agent-bubble" :class="{ 'ai-bubble': isAiMessage(msg), 'revoked-bubble': msg.status === 3 }" v-html="renderMessage(msg.content)"></div>
                          <div v-if="getMediaAttachments(msg).length" class="msg-media-grid" :class="`media-grid--${Math.min(getMediaGridData(msg).total, 4)}`">
                            <div class="media-item" v-for="(item, idx) in getMediaGridData(msg).items" :key="idx">
                              <img v-if="item.type === 'image'" :src="getAttachmentUrl(item)" @click="openImagePreview(msg, item)" />
                              <video v-else :src="getAttachmentUrl(item)" controls playsinline />
                              <div v-if="idx === getMediaGridData(msg).items.length - 1 && getMediaGridData(msg).extraCount > 0" class="media-more" @click.stop="openMediaViewer(msg)">+{{ getMediaGridData(msg).extraCount }}</div>
                            </div>
                          </div>
                          <div v-if="getFileAttachments(msg).length" class="msg-file-list">
                            <div class="file-item" v-for="(item, idx) in getFileAttachments(msg)" :key="idx" @click="openFilePreview(item)">
                              <span class="file-icon">📄</span>
                              <span class="file-name">{{ item.name || item.url }}</span>
                            </div>
                          </div>
                        </div>
                        <a-avatar :size="32" class="msg-avatar agent-avatar" :src="isAiMessage(msg) ? getAiAvatarUrl() : ''">
                          {{ isAiMessage(msg) ? 'AI' : (msg.senderName || '客').charAt(0) }}
                        </a-avatar>
                      </div>
                    </template>
                  </div>
                </template>
                <a-empty v-else description="暂无消息记录" />
              </div>
            </a-spin>
          </div>
        </div>

        <!-- 右侧：访客信息 -->
        <div class="split-right">
          <div class="panel-header">访客信息</div>
          <div class="panel-body">
            <a-spin :spinning="visitorLoading">
              <template v-if="visitorInfo">
                <div class="info-section">
                  <div class="section-title">基本信息</div>
                  <div class="info-item">
                    <label>访客ID</label>
                    <span class="info-value">{{ record?.userId || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label>备注昵称</label>
                    <span class="info-value">{{ visitorInfo.nickname || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label>真实姓名</label>
                    <span class="info-value">{{ visitorInfo.realName || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label>手机号</label>
                    <span class="info-value">{{ visitorInfo.phone || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label>客户等级</label>
                    <a-rate :value="visitorInfo.level || 0" disabled :count="3" />
                  </div>
                  <div class="info-item">
                    <label>星标</label>
                    <span class="info-value">{{ visitorInfo.star ? '★' : '-' }}</span>
                  </div>
                </div>

                <div class="info-section">
                  <div class="section-title">访问信息</div>
                  <div class="info-item">
                    <label>IP地址</label>
                    <span class="info-value">{{ record?.userIp || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label>IP归属地</label>
                    <span class="info-value">{{ formatGeoLocation() }}</span>
                  </div>
                  <div class="info-item">
                    <label>操作系统</label>
                    <span class="info-value">{{ record?.userOs || '-' }}{{ record?.userOsVersion ? ' ' + record.userOsVersion : '' }}</span>
                  </div>
                  <div class="info-item">
                    <label>浏览器</label>
                    <span class="info-value">{{ record?.userBrowser || '-' }}{{ record?.userBrowserVersion ? ' ' + record.userBrowserVersion : '' }}</span>
                  </div>
                  <div class="info-item" v-if="record?.userDeviceId">
                    <label>设备码</label>
                    <span class="info-value" style="font-family: monospace; font-size: 12px;">{{ record.userDeviceId }}</span>
                  </div>
                  <div class="info-item">
                    <label>浏览器语言</label>
                    <span class="info-value">{{ record?.userLang || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label>来源</label>
                    <span class="info-value">{{ record?.source || '直接访问' }}</span>
                  </div>
                  <div class="info-item">
                    <label>首次访问</label>
                    <span class="info-value">{{ visitorInfo.firstVisitTime || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label>访问次数</label>
                    <span class="info-value">{{ visitorInfo.visitCount || 1 }} 次</span>
                  </div>
                </div>

                <div v-if="parsedCustomFields.length" class="info-section">
                  <div class="section-title">转人工填写信息</div>
                  <div v-for="cf in parsedCustomFields" :key="cf.label" class="info-item">
                    <label>{{ cf.label }}</label>
                    <span class="info-value" style="color: #ff4d4f; font-weight: 500;">{{ cf.value }}</span>
                  </div>
                </div>

                <div class="info-section">
                  <div class="section-title">标签</div>
                  <div class="tags-wrapper">
                    <template v-if="visitorTags.length">
                      <a-tag v-for="tag in visitorTags" :key="tag" color="blue">{{ tag }}</a-tag>
                    </template>
                    <span v-else class="no-data">暂无标签</span>
                  </div>
                </div>

                <div class="info-section">
                  <div class="section-title">备注</div>
                  <div class="notes-content">{{ visitorInfo.notes || '暂无备注' }}</div>
                </div>
              </template>
              <a-empty v-else description="暂无访客信息" />
            </a-spin>
          </div>
        </div>
      </div>
    </div>

    <a-modal v-model:open="mediaViewerVisible" :footer="null" width="820px" title="媒体预览">
      <div class="media-viewer-header">
        <span>共 {{ mediaViewerList.length }} 项</span>
        <span class="media-viewer-tip">点击图片可放大，视频可播放</span>
      </div>
      <div class="media-viewer-grid">
        <div class="media-viewer-item" v-for="(item, index) in mediaViewerList" :key="`${item.url}_${index}`">
          <img v-if="item.type === 'image'" :src="getAttachmentUrl(item)" @click="openImagePreviewFromList(mediaViewerList, item)" />
          <video v-else :src="getAttachmentUrl(item)" controls />
        </div>
      </div>
    </a-modal>
  </BasicModal>
</template>

<script lang="ts" setup>
import { ref, nextTick, computed } from 'vue';
import { BasicModal, useModalInner } from '/@/components/Modal';
import { defHttp } from '/@/utils/http/axios';
import { MessageOutlined } from '@ant-design/icons-vue';
import { Typography } from 'ant-design-vue';
import { getFileAccessHttpUrl } from '/@/utils/common/compUtils';
import { getBrandSetting } from '/@/settings/brandSetting';
import { resolveBrandUrl } from '/@/utils/brand';
import { createImgPreview } from '/@/components/Preview';
import { useGlobSetting } from '/@/hooks/setting';
import { decryptTransport, decryptMessage } from '../utils/csEncrypt';
import MarkdownIt from 'markdown-it';
import hljs from 'highlight.js';

const { Text: ATypographyText } = Typography;
const globSetting = useGlobSetting();

function isAiMessage(msg: any): boolean {
  const st = Number(msg?.senderType);
  return st === 1 || msg?.isAiGenerated || (st === 2 && !msg?.senderId);
}

const chatWindowLogo = ref('');
const chatWindowSettings = ref<any>({});
async function loadChatWindowSettings() {
  try {
    const rawRes = await defHttp.get({ url: '/cs/agent/global/chat-window-settings' });
    const res = typeof rawRes === 'string' ? decryptTransport(rawRes) : rawRes;
    let parsed: any = {};
    if (typeof res === 'string') { try { parsed = JSON.parse(res); } catch {} }
    else if (res && typeof res === 'object') { parsed = res; }
    chatWindowSettings.value = parsed;
    if (parsed.logo) chatWindowLogo.value = parsed.logo;
  } catch {}
}

function getAiAvatarUrl(): string {
  if (chatWindowLogo.value) return getFileAccessHttpUrl(chatWindowLogo.value);
  const brandLogo = getBrandSetting().logoUrl;
  if (brandLogo) return resolveBrandUrl(brandLogo);
  return '';
}

const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: function (str: string, lang: string) {
    if (lang && hljs.getLanguage(lang)) {
      try { return hljs.highlight(str, { language: lang }).value; } catch (__) {}
    }
    return '';
  },
});

function normalizeImgUrls(html: string): string {
  try {
    const origin = new URL(globSetting.domainUrl).origin;
    return html.replace(
      /(<img[^>]*?\ssrc=["'])(\/[^"']+)(["'])/gi,
      (_match, pre, path, suf) => `${pre}${origin}${path}${suf}`,
    );
  } catch { return html; }
}

/** 纯文本 URL 自动识别：在 HTML 转义前检测 URL，转为可点击的 <a> 标签 */
function linkifyPlainText(text: string): string {
  const urlPattern = /(https?:\/\/[^\s<>]*[^\s<>.,;:!?。，；：！？)\]】]|www\.[^\s<>]*[^\s<>.,;:!?。，；：！？)\]】])/gi;
  let lastIndex = 0;
  let result = '';
  const esc = (s: string) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  let match: RegExpExecArray | null;
  urlPattern.lastIndex = 0;
  while ((match = urlPattern.exec(text)) !== null) {
    result += esc(text.slice(lastIndex, match.index));
    const url = match[0];
    const href = url.startsWith('www.') ? 'https://' + url : url;
    result += `<a class="auto-link" href="${esc(href)}" target="_blank" rel="noopener noreferrer">${esc(url)}</a>`;
    lastIndex = match.index + url.length;
  }
  result += esc(text.slice(lastIndex));
  return result.replace(/\n/g, '<br>');
}

function renderMessage(content: string) {
  if (!content) return '';
  content = content.replace(/#\s*\{\s*domainURL\s*\}/g, globSetting.domainUrl);
  content = normalizeImgUrls(content);
  const isRichHtml = /^\s*<(?:p|div|ul|ol|h[1-6]|table|blockquote)\b/i.test(content.trim());
  if (isRichHtml) return content;
  const hasMarkdown = /!\[[^\]]*]\([^)]*\)|\*\*[^*]+\*\*|```|^\s*#/m.test(content);
  if (hasMarkdown) return md.render(content);
  const hasInlineHtml = /<([a-z][\s\S]*?)>/i.test(content);
  if (hasInlineHtml) return md.render(content);
  return linkifyPlainText(content);
}

// ==================== 附件解析辅助函数 ====================

function parseExtra(extra: any) {
  if (!extra) return null;
  if (typeof extra === 'string') {
    try {
      return JSON.parse(extra);
    } catch {
      return null;
    }
  }
  return extra;
}

function getMessageAttachments(msg: any): any[] {
  return parseExtra(msg?.extra)?.attachments || [];
}

function getMediaAttachments(msg: any): any[] {
  return getMessageAttachments(msg).filter(item => item.type === 'image' || item.type === 'video');
}

function getFileAttachments(msg: any): any[] {
  return getMessageAttachments(msg).filter(item => item.type === 'file');
}

function getMediaGridData(msg: any) {
  const media = getMediaAttachments(msg);
  const maxItems = 4;
  const items = media.slice(0, maxItems);
  const extraCount = Math.max(0, media.length - maxItems);
  return { items, extraCount, total: media.length };
}

function getAttachmentUrl(attachment: any) {
  return getFileAccessHttpUrl(attachment?.url);
}

function openImagePreview(msg: any, item: any) {
  const images = getMessageAttachments(msg).filter(att => att.type === 'image');
  const imageList = images.map(att => getAttachmentUrl(att));
  if (!imageList.length) return;
  const targetUrl = getAttachmentUrl(item);
  const index = imageList.findIndex(url => url === targetUrl);
  createImgPreview({ imageList, index: index >= 0 ? index : 0, defaultWidth: 700, rememberState: true });
}

function openFilePreview(item: any) {
  const url = getAttachmentUrl(item);
  if (url) window.open(url, '_blank');
}

const mediaViewerVisible = ref(false);
const mediaViewerList = ref<any[]>([]);

function openMediaViewer(msg: any) {
  mediaViewerList.value = getMediaAttachments(msg);
  mediaViewerVisible.value = true;
}

function openImagePreviewFromList(list: any[], item: any) {
  const images = (list || []).filter(att => att.type === 'image');
  const imageList = images.map(att => getAttachmentUrl(att));
  if (!imageList.length) return;
  const targetUrl = getAttachmentUrl(item);
  const index = imageList.findIndex(url => url === targetUrl);
  createImgPreview({ imageList, index: index >= 0 ? index : 0, defaultWidth: 700, rememberState: true });
}

const record = ref<any>(null);
const messages = ref<any[]>([]);
const loading = ref(false);
const messageListRef = ref<HTMLElement | null>(null);
const visitorInfo = ref<any>(null);
const visitorLoading = ref(false);

const endTypeText = computed(() => {
  const map: Record<number, string> = { 0: '客服结束', 1: '超时结束', 2: '访客结束', 3: '系统清理' };
  return record.value?.endType != null ? (map[record.value.endType] || '-') : '-';
});

const visitorTags = computed(() => {
  if (!visitorInfo.value?.tags) return [];
  try {
    const tags = typeof visitorInfo.value.tags === 'string'
      ? JSON.parse(visitorInfo.value.tags)
      : visitorInfo.value.tags;
    return Array.isArray(tags) ? tags : [];
  } catch {
    return [];
  }
});

const parsedCustomFields = computed(() => {
  const raw = record.value?.customFields;
  if (!raw) return [];
  try {
    const fields = typeof raw === 'string' ? JSON.parse(raw) : raw;
    if (typeof fields === 'object' && fields !== null) {
      const all = Object.entries(fields).map(([label, value]) => ({ label, value: String(value) }));
      const fieldDefs = chatWindowSettings.value?.humanAgentFields;
      if (!Array.isArray(fieldDefs) || !fieldDefs.length) return all;
      return all.filter((f) => {
        const def = fieldDefs.find((d: any) => d.label === f.label);
        return !def || def.showInHistory !== false;
      });
    }
  } catch {}
  return [];
});

function formatResponseTime(seconds: number | null | undefined) {
  if (seconds == null) return '-';
  if (seconds === 0) return '立即响应';
  if (seconds < 60) return `${seconds}秒`;
  return `${Math.floor(seconds / 60)}分${seconds % 60}秒`;
}

function formatGeoLocation() {
  const parts = [record.value?.userCountry, record.value?.userProvince, record.value?.userCity].filter(Boolean);
  return parts.length ? parts.join('/') : '-';
}
const historyPageSize = 100;
const loadingHistory = ref(false);
const hasMoreHistory = ref(true);
const historyBeforeId = ref<string | null>(null);
const displayMessages = computed(() => {
  const list: any[] = [];
  let lastDateKey = '';
  for (const msg of messages.value) {
    const dateKey = getDateKey(msg?.createTime);
    if (dateKey && dateKey !== lastDateKey) {
      list.push({
        id: `date-${dateKey}`,
        senderType: 3,
        content: formatDateSeparator(msg.createTime),
        isDateSeparator: true,
      });
      lastDateKey = dateKey;
    }
    list.push(msg);
  }
  return list;
});

const [registerModal] = useModalInner(async (data) => {
  record.value = data?.record ? { ...data.record } : null;
  if (record.value?.satisfactionComment) {
    record.value.satisfactionComment = decryptMessage(record.value.satisfactionComment);
  }
  if (record.value?.lastMessage) {
    record.value.lastMessage = decryptMessage(record.value.lastMessage);
  }
  messages.value = [];
  historyBeforeId.value = null;
  hasMoreHistory.value = true;
  visitorInfo.value = null;
  
  if (record.value?.id) {
    await Promise.all([loadMessages(record.value.id), loadChatWindowSettings()]);
    loadVisitorInfo();
  }
});

async function loadVisitorInfo() {
  if (!record.value?.userId) return;
  visitorLoading.value = true;
  try {
    const params: any = { userId: record.value.userId };
    if (record.value.appId) {
      params.appId = record.value.appId;
    }
    const res = await defHttp.get({
      url: '/airag/cs/visitor/getByUser',
      params,
    });
    visitorInfo.value = res || null;
  } catch {
    visitorInfo.value = null;
  } finally {
    visitorLoading.value = false;
  }
}

async function loadMessages(conversationId: string) {
  loading.value = true;
  try {
    const res = await defHttp.get({ 
      url: `/cs/message/${conversationId}`, 
      params: { limit: historyPageSize } 
    });
    const rawList = Array.isArray(res) ? res : (res?.result || res?.records || []);
    messages.value = (rawList || []).map((m: any) => ({ ...m, content: decryptMessage(m.content) }));
    historyBeforeId.value = messages.value[0]?.id || null;
    hasMoreHistory.value = messages.value.length >= historyPageSize;
    
    // 滚动到底部
    nextTick(() => {
      if (messageListRef.value) {
        messageListRef.value.scrollTop = messageListRef.value.scrollHeight;
      }
    });
  } catch (e) {
    console.error('加载消息失败', e);
    messages.value = [];
  } finally {
    loading.value = false;
  }
}

async function loadMoreMessages() {
  if (!record.value?.id) return;
  if (loadingHistory.value || !hasMoreHistory.value) return;
  const beforeId = historyBeforeId.value;
  if (!beforeId) {
    hasMoreHistory.value = false;
    return;
  }

  const el = messageListRef.value;
  const prevScrollHeight = el?.scrollHeight || 0;
  const prevScrollTop = el?.scrollTop || 0;

  loadingHistory.value = true;
  try {
    const res = await defHttp.get({
      url: `/cs/message/${record.value.id}/page`,
      params: { beforeId, limit: historyPageSize },
    });
    const rawOlder = Array.isArray(res) ? res : (res?.result || res?.records || []);
    const olderMessages = rawOlder.map((m: any) => ({ ...m, content: decryptMessage(m.content) }));
    if (!olderMessages.length) {
      hasMoreHistory.value = false;
      return;
    }
    const existingIds = new Set(messages.value.map(m => m.id));
    const filtered = olderMessages.filter((m: any) => !existingIds.has(m.id));
    if (!filtered.length) {
      hasMoreHistory.value = false;
      return;
    }
    messages.value = [...filtered, ...messages.value];
    historyBeforeId.value = filtered[0]?.id || historyBeforeId.value;
    if (olderMessages.length < historyPageSize) {
      hasMoreHistory.value = false;
    }
    nextTick(() => {
      const nextEl = messageListRef.value;
      if (!nextEl) return;
      const nextScrollHeight = nextEl.scrollHeight;
      nextEl.scrollTop = nextScrollHeight - prevScrollHeight + prevScrollTop;
    });
  } catch (e) {
    console.error('加载历史消息失败', e);
  } finally {
    loadingHistory.value = false;
  }
}

function handleMessageScroll(event?: Event) {
  const el = (event?.target as HTMLElement) || messageListRef.value;
  if (!el) return;
  if (loadingHistory.value || !hasMoreHistory.value) return;
  if (el.scrollTop <= 20) {
    loadMoreMessages();
  }
}

function getStatusColor(status: number) {
  switch (status) {
    case 0: return 'blue';
    case 1: return 'green';
    case 2: return 'default';
    default: return 'default';
  }
}

function getStatusText(status: number) {
  switch (status) {
    case 0: return '待接入';
    case 1: return '服务中';
    case 2: return '已结束';
    default: return '未知';
  }
}

function getModeColor(mode: number) {
  switch (mode) {
    case 0: return 'purple';
    case 1: return 'orange';
    case 2: return 'cyan';
    default: return 'default';
  }
}

function getModeText(mode: number) {
  switch (mode) {
    case 0: return 'AI自动';
    case 1: return '手动';
    case 2: return 'AI辅助';
    default: return '未知';
  }
}

function getMsgClass(msg: any) {
  if (isAiMessage(msg)) return 'msg-ai';
  switch (msg.senderType) {
    case 0: return 'msg-user';
    case 3: return 'msg-system';
    default: return 'msg-agent';
  }
}

function formatTime(time: string) {
  if (!time) return '';
  const date = new Date(time);
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
}

function getDateKey(time: string | Date) {
  if (!time) return '';
  const date = new Date(time);
  if (Number.isNaN(date.getTime())) return '';
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function formatDateSeparator(time: string | Date) {
  if (!time) return '';
  const date = new Date(time);
  if (Number.isNaN(date.getTime())) return '';
  const today = new Date();
  const dateKey = getDateKey(date);
  const todayKey = getDateKey(today);
  if (dateKey === todayKey) return '今天';
  const yesterday = new Date();
  yesterday.setDate(yesterday.getDate() - 1);
  if (dateKey === getDateKey(yesterday)) return '昨天';
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' });
}
</script>

<style lang="less" scoped>
.conversation-detail {
  .top-info {
    margin-bottom: 20px;

    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .satisfaction-comment {
      margin-left: 8px;
      color: #999;
      font-size: 12px;
      font-style: italic;
    }
  }

  .split-layout {
    display: flex;
    gap: 16px;
    margin-top: 16px;
    min-height: 500px;
  }

  .split-left {
    flex: 1;
    min-width: 0;
  }

  .split-right {
    width: 320px;
    flex-shrink: 0;
    border: 1px solid #f0f0f0;
    border-radius: 8px;
    overflow: hidden;
    display: flex;
    flex-direction: column;

    .panel-header {
      padding: 12px 16px;
      font-size: 14px;
      font-weight: 600;
      background: #fafafa;
      border-bottom: 1px solid #f0f0f0;
    }

    .panel-body {
      flex: 1;
      overflow-y: auto;
      max-height: 530px;
      padding: 0 16px 16px;
    }

    .info-section {
      padding-top: 14px;

      &:not(:last-child) {
        padding-bottom: 10px;
        border-bottom: 1px solid #f0f0f0;
      }

      .section-title {
        font-size: 13px;
        font-weight: 600;
        color: #333;
        margin-bottom: 10px;
        padding-left: 8px;
        border-left: 3px solid #1890ff;
      }
    }

    .info-item {
      display: flex;
      align-items: baseline;
      padding: 4px 0;
      font-size: 13px;
      line-height: 1.6;

      label {
        width: 80px;
        flex-shrink: 0;
        color: #999;
      }

      .info-value {
        flex: 1;
        color: #333;
        word-break: break-all;
      }
    }

    .tags-wrapper {
      padding: 4px 0;

      .no-data {
        color: #999;
        font-size: 13px;
      }
    }

    .notes-content {
      padding: 8px 10px;
      background: #fafafa;
      border-radius: 4px;
      color: #555;
      font-size: 13px;
      min-height: 36px;
      white-space: pre-wrap;
    }
  }

  .message-section {
    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid #f0f0f0;
      margin-bottom: 12px;

      .section-title {
        font-size: 15px;
        font-weight: 500;
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .message-count {
        color: #999;
        font-size: 13px;
      }
    }
  }

  .message-list {
    max-height: 480px;
    overflow-y: auto;
    padding: 16px;
    background: linear-gradient(180deg, #f8f9fa 0%, #f0f2f5 100%);
    border-radius: 8px;
  }

  .message-item {
    margin-bottom: 16px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .system-message {
    text-align: center;
    padding: 8px 0;

    .system-text {
      display: inline-block;
      padding: 4px 16px;
      background: rgba(0, 0, 0, 0.04);
      border-radius: 12px;
      color: #999;
      font-size: 12px;
    }

    .system-time {
      display: block;
      margin-top: 4px;
      font-size: 11px;
      color: #bbb;
    }
  }

  .user-message {
    display: flex;
    align-items: flex-start;
    gap: 10px;

    .user-avatar {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #fff;
      flex-shrink: 0;
    }

    .msg-content {
      max-width: 70%;
    }

    .msg-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 4px;

      .sender-name {
        font-size: 12px;
        color: #666;
        font-weight: 500;
      }

      .msg-time {
        font-size: 11px;
        color: #bbb;
      }
    }

    .user-bubble {
      background: #fff;
      border: 1px solid #e8e8e8;
      border-radius: 0 12px 12px 12px;
    }
  }

  .agent-message {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    justify-content: flex-end;

    .agent-avatar {
      background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
      color: #fff;
      flex-shrink: 0;
    }

    .msg-content {
      max-width: 70%;
    }

    .msg-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 4px;
      justify-content: flex-end;

      .sender-name {
        font-size: 12px;
        color: #666;
        font-weight: 500;
      }

      .msg-time {
        font-size: 11px;
        color: #bbb;
      }
    }

    .agent-bubble {
      background: #e6f7ff;
      border-radius: 12px 0 12px 12px;

      &.ai-bubble {
        background: #f9f0ff;
      }

      &.revoked-bubble {
        opacity: 0.6;
      }
    }
  }

  .msg-bubble {
    padding: 10px 14px;
    font-size: 14px;
    line-height: 1.6;
    word-break: break-word;

    :deep(p) {
      margin: 0 0 0.5em;
      &:last-child { margin-bottom: 0; }
    }

    :deep(img) {
      max-width: 100%;
      border-radius: 6px;
      cursor: pointer;
    }

    :deep(pre) {
      background: #f5f5f5;
      border-radius: 6px;
      padding: 10px;
      overflow-x: auto;
      margin: 6px 0;
      font-size: 13px;
    }

    :deep(code) {
      font-family: 'Consolas', 'Monaco', monospace;
      font-size: 13px;
    }

    :deep(a) {
      color: #1890ff;
      text-decoration: underline;
    }
  }

  .msg-media-grid {
    display: grid;
    gap: 4px;
    margin-top: 6px;
    max-width: 360px;

    .media-item {
      position: relative;
      border-radius: 8px;
      overflow: hidden;
      background: #f0f0f0;
      cursor: pointer;
      transition: transform 0.15s ease;

      img, video {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }

      &:hover { transform: scale(1.02); }

      .media-more {
        position: absolute;
        inset: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        background: rgba(0, 0, 0, 0.55);
        color: #fff;
        font-size: 18px;
        font-weight: 600;
      }
    }
  }

  .media-grid--1 {
    grid-template-columns: 1fr;
    .media-item { aspect-ratio: 3 / 2; }
  }
  .media-grid--2 {
    grid-template-columns: repeat(2, 1fr);
    .media-item { aspect-ratio: 1 / 1; }
  }
  .media-grid--3 {
    grid-template-columns: repeat(2, 1fr);
    grid-template-rows: repeat(2, 1fr);
    .media-item { aspect-ratio: 1 / 1; }
    .media-item:nth-child(1) { grid-row: span 2; aspect-ratio: auto; }
  }
  .media-grid--4 {
    grid-template-columns: repeat(2, 1fr);
    .media-item { aspect-ratio: 1 / 1; }
  }

  .msg-file-list {
    margin-top: 6px;
    display: flex;
    flex-direction: column;
    gap: 6px;

    .file-item {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 8px 10px;
      background: rgba(24, 144, 255, 0.04);
      border-left: 3px solid #1890ff;
      border-radius: 0 8px 8px 0;
      cursor: pointer;
      font-size: 12px;
      transition: all 0.15s ease;

      &:hover {
        background: rgba(24, 144, 255, 0.08);
        box-shadow: 0 1px 4px rgba(24, 144, 255, 0.15);
      }

      .file-icon {
        flex-shrink: 0;
      }

      .file-name {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }
}

.media-viewer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 13px;
  color: #666;

  .media-viewer-tip {
    color: #999;
    font-size: 12px;
  }
}

.media-viewer-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 10px;
}

.media-viewer-item {
  border-radius: 10px;
  overflow: hidden;
  background: #f5f5f5;
  border: 1px solid #e8e8e8;
  aspect-ratio: 16 / 9;
  transition: transform 0.15s ease, box-shadow 0.15s ease;

  img, video {
    width: 100%;
    height: 100%;
    object-fit: cover;
    cursor: pointer;
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
  }
}
</style>
