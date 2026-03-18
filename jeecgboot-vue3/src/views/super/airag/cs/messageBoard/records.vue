<template>
  <div class="message-board-records">
    <a-card title="留言记录" :bordered="false">
      <!-- 筛选 -->
      <div class="filter-bar">
        <a-input v-model:value="filterUserId" placeholder="访客ID" allow-clear style="width: 200px;" @pressEnter="loadData" />
        <a-select v-model:value="filterStatus" style="width: 120px;" @change="loadData" placeholder="全部状态">
          <a-select-option :value="undefined">全部状态</a-select-option>
          <a-select-option :value="0">待回复</a-select-option>
          <a-select-option :value="1">已回复</a-select-option>
        </a-select>
        <a-button type="primary" @click="loadData">查询</a-button>
      </div>

      <!-- 列表 -->
      <a-table
        :dataSource="dataList"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        @change="onTableChange"
        rowKey="id"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'userId'">
            <span class="user-id-cell">{{ record.userId || '-' }}</span>
          </template>
          <template v-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 0 ? 'orange' : 'green'">
              {{ record.status === 0 ? '待回复' : '已回复' }}
            </a-tag>
          </template>
          <template v-if="column.dataIndex === 'content'">
            <span class="content-ellipsis">{{ record.content || '-' }}</span>
          </template>
          <template v-if="column.dataIndex === 'action'">
            <a @click="showDetail(record)">查看</a>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 详情弹窗 — 聊天式沉浸界面 -->
    <a-modal
      v-model:open="detailVisible"
      :title="'留言详情' + (currentRecord?.name ? ' - ' + currentRecord.name : '')"
      :footer="null"
      width="1100px"
      :bodyStyle="{ padding: 0 }"
      destroyOnClose
    >
      <div class="detail-layout" v-if="currentRecord">
        <!-- 左侧：标签页区域 -->
        <div class="detail-left">
          <a-tabs v-model:activeKey="detailActiveTab" class="detail-tabs">
            <!-- Tab 1: 留言详情 -->
            <a-tab-pane key="leave" tab="留言详情">
              <div class="tab-scroll-area">
                <div class="chat-msg-item">
                  <div class="user-message">
                    <a-avatar :size="32" class="msg-avatar user-avatar">
                      {{ (currentRecord.name || '访').charAt(0) }}
                    </a-avatar>
                    <div class="msg-content">
                      <div class="msg-header">
                        <span class="sender-name">{{ currentRecord.name || '访客' }}</span>
                        <span class="msg-time">{{ formatTime(currentRecord.createTime) }}</span>
                      </div>
                      <div class="msg-bubble user-bubble">{{ currentRecord.content || '-' }}</div>
                      <div v-if="currentRecord.imageUrl" class="leave-msg-image">
                        <a-image :src="currentRecord.imageUrl" :width="200" />
                      </div>
                    </div>
                  </div>
                </div>

                <div v-if="currentRecord.status === 1 && currentRecord.reply" class="chat-msg-item">
                  <div class="agent-message">
                    <div class="msg-content">
                      <div class="msg-header">
                        <span class="msg-time">{{ formatTime(currentRecord.replyTime) }}</span>
                        <span class="sender-name">客服</span>
                        <a-tag color="green" size="small">已回复</a-tag>
                      </div>
                      <div class="msg-bubble agent-bubble">{{ currentRecord.reply }}</div>
                      <div class="recall-action always-visible">
                        <a-popconfirm title="确定撤回该回复？" ok-text="确定" cancel-text="取消" @confirm="recallReply(currentRecord)">
                          <a-button type="link" danger size="small"><UndoOutlined /> 撤回</a-button>
                        </a-popconfirm>
                      </div>
                    </div>
                    <a-avatar :size="32" class="msg-avatar agent-avatar">客</a-avatar>
                  </div>
                </div>
              </div>

              <div v-if="currentRecord.status === 0" class="reply-area">
                <a-textarea
                  v-model:value="replyContent"
                  placeholder="请输入回复内容"
                  :rows="3"
                  :maxlength="1000"
                  showCount
                  style="flex:1"
                />
                <a-button type="primary" class="reply-send-btn" @click="submitReply" :loading="replying" :disabled="!replyContent.trim()">
                  <SendOutlined />
                </a-button>
              </div>
            </a-tab-pane>

            <!-- Tab 2: 聊天记录 -->
            <a-tab-pane key="history" tab="聊天记录" :disabled="!currentRecord.userId">
              <div class="tab-scroll-area" ref="chatAreaRef" @scroll.passive="handleChatScroll">
                <a-spin v-if="historyLoading && chatMessages.length === 0" :spinning="true" style="display:flex;justify-content:center;padding:40px 0" />

                <div v-if="historyLoading && chatMessages.length > 0" class="load-more-hint">
                  <a-spin size="small" /> 加载更多...
                </div>
                <div v-else-if="!hasMoreHistory && chatMessages.length > 0" class="load-more-hint no-more">
                  没有更多记录了
                </div>

                <template v-if="chatMessages.length > 0">
                  <div v-for="msg in displayMessages" :key="msg.id" :class="['chat-msg-item', getMsgClass(msg)]">
                    <template v-if="msg.senderType === 3">
                      <div class="system-message">
                        <span class="system-text">{{ msg.content }}</span>
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
                            <a-tag v-else color="green" size="small">客服</a-tag>
                            <a-tag v-if="msg.status === 3" color="red" size="small">已撤回</a-tag>
                          </div>
                          <div class="msg-bubble agent-bubble" :class="{ 'ai-bubble': isAiMessage(msg), 'revoked-bubble': msg.status === 3 }" v-html="renderMessage(msg.content)"></div>
                          <div v-if="getMediaAttachments(msg).length" class="msg-media-grid" :class="`media-grid--${Math.min(getMediaGridData(msg).total, 4)}`">
                            <div class="media-item" v-for="(item, idx) in getMediaGridData(msg).items" :key="idx">
                              <img v-if="item.type === 'image'" :src="getAttachmentUrl(item)" @click="openImagePreview(msg, item)" />
                              <video v-else :src="getAttachmentUrl(item)" controls playsinline />
                            </div>
                          </div>
                          <div v-if="getFileAttachments(msg).length" class="msg-file-list">
                            <div class="file-item" v-for="(item, idx) in getFileAttachments(msg)" :key="idx" @click="openFilePreview(item)">
                              <span class="file-icon">📄</span>
                              <span class="file-name">{{ item.name || item.url }}</span>
                            </div>
                          </div>
                        </div>
                        <a-avatar :size="32" class="msg-avatar agent-avatar">
                          {{ (msg.senderName || '客').charAt(0) }}
                        </a-avatar>
                      </div>
                    </template>
                  </div>
                </template>
                <div v-else-if="!historyLoading" class="empty-history">
                  <a-empty description="暂无聊天记录" :image="simpleImage" />
                </div>
              </div>
            </a-tab-pane>
          </a-tabs>
        </div>

        <!-- 右侧：访客信息 -->
        <div class="detail-right">
          <div class="panel-header">访客信息</div>
          <div class="panel-body">
            <a-spin :spinning="visitorLoading">
              <template v-if="visitorInfo || currentRecord">
                <div class="info-section">
                  <div class="section-title">基本信息</div>
                  <div class="info-item">
                    <label>访客ID</label>
                    <span class="info-value">{{ currentRecord.userId || '-' }}</span>
                  </div>
                  <div class="info-item" v-if="visitorInfo">
                    <label>备注昵称</label>
                    <span class="info-value">{{ visitorInfo.nickname || '-' }}</span>
                  </div>
                  <div class="info-item" v-if="visitorInfo">
                    <label>真实姓名</label>
                    <span class="info-value">{{ visitorInfo.realName || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label>姓名(留言)</label>
                    <span class="info-value">{{ currentRecord.name || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label>手机</label>
                    <span class="info-value">{{ currentRecord.phone || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label>邮箱</label>
                    <span class="info-value">{{ currentRecord.email || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label>QQ</label>
                    <span class="info-value">{{ currentRecord.qq || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label>微信</label>
                    <span class="info-value">{{ currentRecord.wechat || '-' }}</span>
                  </div>
                  <div class="info-item" v-if="visitorInfo">
                    <label>客户等级</label>
                    <a-rate :value="visitorInfo.level || 0" disabled :count="3" />
                  </div>
                  <div class="info-item" v-if="visitorInfo">
                    <label>星标</label>
                    <span class="info-value">{{ visitorInfo.star ? '★' : '-' }}</span>
                  </div>
                </div>

                <div class="info-section" v-if="visitorInfo">
                  <div class="section-title">访问信息</div>
                  <div class="info-item">
                    <label>首次访问</label>
                    <span class="info-value">{{ visitorInfo.firstVisitTime || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label>访问次数</label>
                    <span class="info-value">{{ visitorInfo.visitCount || 1 }} 次</span>
                  </div>
                </div>

                <div class="info-section" v-if="visitorInfo">
                  <div class="section-title">标签</div>
                  <div class="tags-wrapper">
                    <template v-if="visitorTags.length">
                      <a-tag v-for="tag in visitorTags" :key="tag" color="blue">{{ tag }}</a-tag>
                    </template>
                    <span v-else class="no-data">暂无标签</span>
                  </div>
                </div>

                <div class="info-section" v-if="visitorInfo">
                  <div class="section-title">备注</div>
                  <div class="notes-content">{{ visitorInfo.notes || '暂无备注' }}</div>
                </div>
              </template>
              <a-empty v-if="!visitorInfo && !visitorLoading && !currentRecord.userId" description="暂无访客信息" />
            </a-spin>
          </div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted, computed, nextTick } from 'vue';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';
import { UndoOutlined, SendOutlined } from '@ant-design/icons-vue';
import { getFileAccessHttpUrl } from '/@/utils/common/compUtils';
import { createImgPreview } from '/@/components/Preview';
import { useGlobSetting } from '/@/hooks/setting';
import { Empty } from 'ant-design-vue';
import MarkdownIt from 'markdown-it';
import hljs from 'highlight.js';

const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE;
const { createMessage: message } = useMessage();
const globSetting = useGlobSetting();

// ==================== Markdown 渲染 ====================
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

// ==================== 附件解析 ====================
function parseExtra(extra: any) {
  if (!extra) return null;
  if (typeof extra === 'string') {
    try { return JSON.parse(extra); } catch { return null; }
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

function isAiMessage(msg: any): boolean {
  const st = Number(msg?.senderType);
  return st === 1 || msg?.isAiGenerated || (st === 2 && !msg?.senderId);
}

// ==================== 列表数据 ====================
const loading = ref(false);
const replying = ref(false);
const detailVisible = ref(false);
const filterStatus = ref<number | undefined>(undefined);
const filterUserId = ref('');
const replyContent = ref('');
const currentRecord = ref<any>(null);
const dataList = ref<any[]>([]);

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
});

const columns = [
  { title: '访客ID', dataIndex: 'userId', width: 180, ellipsis: true },
  { title: '姓名', dataIndex: 'name', width: 100 },
  { title: '手机', dataIndex: 'phone', width: 120 },
  { title: '留言内容', dataIndex: 'content', ellipsis: true },
  { title: '状态', dataIndex: 'status', width: 90, align: 'center' as const },
  { title: '留言时间', dataIndex: 'createTime', width: 170 },
  { title: '回复时间', dataIndex: 'replyTime', width: 170 },
  { title: '操作', dataIndex: 'action', width: 80, align: 'center' as const },
];

onMounted(async () => {
  await loadData();
});

async function loadData() {
  loading.value = true;
  try {
    const params: Record<string, any> = {
      pageNo: pagination.current,
      pageSize: pagination.pageSize,
    };
    if (filterStatus.value !== undefined) {
      params.status = filterStatus.value;
    }
    if (filterUserId.value.trim()) {
      params.userId = filterUserId.value.trim();
    }
    const res = await defHttp.get({ url: '/cs/leaveMessage/list', params });
    const data = res?.result || res;
    if (data) {
      dataList.value = data.records || [];
      pagination.total = data.total || 0;
    }
  } catch (e) {
    console.error('加载留言记录失败', e);
  } finally {
    loading.value = false;
  }
}

function onTableChange(pag: any) {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  loadData();
}

// ==================== 详情弹窗 ====================
const detailActiveTab = ref('leave');
const chatAreaRef = ref<HTMLElement | null>(null);
const visitorInfo = ref<any>(null);
const visitorLoading = ref(false);
const chatMessages = ref<any[]>([]);
const historyLoading = ref(false);
const hasMoreHistory = ref(false);
const conversationIds = ref<string[]>([]);
const currentConvIndex = ref(0);
const currentConvBeforeId = ref<string | null>(null);
const currentConvHasMore = ref(true);
const MSG_PAGE_SIZE = 100;

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

const displayMessages = computed(() => {
  const list: any[] = [];
  let lastDateKey = '';
  for (const msg of chatMessages.value) {
    const dateKey = getDateKey(msg?.createTime);
    if (dateKey && dateKey !== lastDateKey) {
      list.push({
        id: `date-${dateKey}-${msg.id}`,
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

function showDetail(record: any) {
  currentRecord.value = { ...record };
  replyContent.value = '';
  chatMessages.value = [];
  conversationIds.value = [];
  currentConvIndex.value = 0;
  currentConvBeforeId.value = null;
  currentConvHasMore.value = true;
  hasMoreHistory.value = false;
  visitorInfo.value = null;
  detailActiveTab.value = 'leave';
  detailVisible.value = true;

  if (record.userId) {
    loadVisitorInfo(record.userId);
    loadConversationHistory(record.userId);
  }
}

async function loadVisitorInfo(userId: string) {
  visitorLoading.value = true;
  try {
    const res = await defHttp.get({
      url: '/airag/cs/visitor/getByUser',
      params: { userId },
    });
    visitorInfo.value = res || null;
  } catch {
    visitorInfo.value = null;
  } finally {
    visitorLoading.value = false;
  }
}

async function loadConversationHistory(userId: string) {
  historyLoading.value = true;
  try {
    const res = await defHttp.get({
      url: '/cs/conversation/visitor-history',
      params: { userId },
    });
    const ids = Array.isArray(res) ? res : (res?.result || []);
    conversationIds.value = ids;
    if (ids.length > 0) {
      hasMoreHistory.value = true;
      currentConvIndex.value = 0;
      await loadConversationMessages(ids[0], true, true);
    } else {
      hasMoreHistory.value = false;
    }
  } catch (e) {
    console.error('加载会话历史失败', e);
    hasMoreHistory.value = false;
  } finally {
    historyLoading.value = false;
  }
}

async function loadConversationMessages(conversationId: string, isInitial: boolean, scrollToBottom = false) {
  historyLoading.value = true;
  const el = chatAreaRef.value;
  const prevScrollHeight = el?.scrollHeight || 0;
  const prevScrollTop = el?.scrollTop || 0;

  try {
    let url: string;
    let params: any;

    if (!isInitial && currentConvBeforeId.value) {
      url = `/cs/message/${conversationId}/page`;
      params = { beforeId: currentConvBeforeId.value, limit: MSG_PAGE_SIZE };
    } else {
      url = `/cs/message/${conversationId}`;
      params = { limit: MSG_PAGE_SIZE };
    }

    const res = await defHttp.get({ url, params });
    const list = Array.isArray(res) ? res : (res?.result || res?.records || []);

    if (list.length === 0) {
      currentConvHasMore.value = false;
      tryLoadNextConversation();
      return;
    }

    const existingIds = new Set(chatMessages.value.map(m => m.id));
    const filtered = list.filter((m: any) => !existingIds.has(m.id));

    if (filtered.length === 0) {
      currentConvHasMore.value = false;
      tryLoadNextConversation();
      return;
    }

    chatMessages.value = [...filtered, ...chatMessages.value];
    currentConvBeforeId.value = filtered[0]?.id || null;
    currentConvHasMore.value = list.length >= MSG_PAGE_SIZE;

    updateHasMoreHistory();

    if (scrollToBottom) {
      nextTick(() => {
        if (chatAreaRef.value) {
          chatAreaRef.value.scrollTop = chatAreaRef.value.scrollHeight;
        }
      });
    } else {
      nextTick(() => {
        if (chatAreaRef.value) {
          const newScrollHeight = chatAreaRef.value.scrollHeight;
          chatAreaRef.value.scrollTop = newScrollHeight - prevScrollHeight + prevScrollTop;
        }
      });
    }
  } catch (e) {
    console.error('加载消息失败', e);
  } finally {
    historyLoading.value = false;
  }
}

function tryLoadNextConversation() {
  const nextIndex = currentConvIndex.value + 1;
  if (nextIndex < conversationIds.value.length) {
    currentConvIndex.value = nextIndex;
    currentConvBeforeId.value = null;
    currentConvHasMore.value = true;
  } else {
    hasMoreHistory.value = false;
  }
}

function updateHasMoreHistory() {
  hasMoreHistory.value = currentConvHasMore.value || (currentConvIndex.value + 1 < conversationIds.value.length);
}

function handleChatScroll(event?: Event) {
  const el = (event?.target as HTMLElement) || chatAreaRef.value;
  if (!el || historyLoading.value || !hasMoreHistory.value) return;

  if (el.scrollTop <= 30) {
    loadMoreHistory();
  }
}

async function loadMoreHistory() {
  if (historyLoading.value || !hasMoreHistory.value) return;

  if (currentConvHasMore.value) {
    const convId = conversationIds.value[currentConvIndex.value];
    if (convId) {
      await loadConversationMessages(convId, false);
    }
  } else {
    const nextIndex = currentConvIndex.value + 1;
    if (nextIndex < conversationIds.value.length) {
      currentConvIndex.value = nextIndex;
      currentConvBeforeId.value = null;
      currentConvHasMore.value = true;
      await loadConversationMessages(conversationIds.value[nextIndex], true);
    } else {
      hasMoreHistory.value = false;
    }
  }
}

// ==================== 回复 / 撤回 ====================
async function recallReply(record: any) {
  try {
    await defHttp.put({ url: `/cs/leaveMessage/${record.id}/recallReply` });
    message.success('撤回成功');
    currentRecord.value = { ...currentRecord.value, status: 0, reply: null, replyTime: null };
    await loadData();
  } catch (e) {
    console.error('撤回失败', e);
    message.error('撤回失败');
  }
}

async function submitReply() {
  if (!replyContent.value.trim()) {
    message.warning('请输入回复内容');
    return;
  }
  if (!currentRecord.value?.id) return;

  replying.value = true;
  try {
    await defHttp.put({
      url: `/cs/leaveMessage/${currentRecord.value.id}/reply`,
      data: { reply: replyContent.value },
    });
    message.success('回复成功');
    currentRecord.value = {
      ...currentRecord.value,
      status: 1,
      reply: replyContent.value,
      replyTime: new Date().toLocaleString('zh-CN'),
    };
    replyContent.value = '';
    await loadData();
  } catch (e) {
    console.error('回复失败', e);
    message.error('回复失败');
  } finally {
    replying.value = false;
  }
}

// ==================== 时间格式化 ====================
function formatTime(time: string) {
  if (!time) return '';
  const date = new Date(time);
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

function getDateKey(time: string | Date) {
  if (!time) return '';
  const date = new Date(time);
  if (Number.isNaN(date.getTime())) return '';
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
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

function getMsgClass(msg: any) {
  if (isAiMessage(msg)) return 'msg-ai';
  switch (msg.senderType) {
    case 0: return 'msg-user';
    case 3: return 'msg-system';
    default: return 'msg-agent';
  }
}
</script>

<style lang="less" scoped>
.message-board-records {
  padding: 16px;

  .filter-bar {
    display: flex;
    gap: 12px;
    margin-bottom: 16px;
  }

  .content-ellipsis {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .user-id-cell {
    font-family: monospace;
    font-size: 12px;
    color: #666;
  }
}

// ==================== 详情弹窗 ====================
.detail-layout {
  display: flex;
  height: 680px;
  overflow: hidden;
}

.detail-left {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #f0f0f0;
  overflow: hidden;
}

.detail-tabs {
  display: flex;
  flex-direction: column;
  height: 100%;

  :deep(.ant-tabs-nav) {
    margin-bottom: 0;
    padding: 0 16px;
    background: #fafafa;
    border-bottom: 1px solid #f0f0f0;
  }

  :deep(.ant-tabs-content-holder) {
    flex: 1;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }

  :deep(.ant-tabs-content) {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  :deep(.ant-tabs-tabpane) {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  :deep(.ant-tabs-tabpane-hidden) {
    display: none !important;
  }
}

.tab-scroll-area {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: linear-gradient(180deg, #f8f9fa 0%, #f0f2f5 100%);
}

.reply-area {
  display: flex;
  align-items: self-start;
  gap: 10px;
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
  flex-shrink: 0;

  .reply-send-btn {
    height: 68px;
    width: 48px;
    font-size: 18px;
  }
}

.load-more-hint {
  text-align: center;
  padding: 8px 0;
  color: #999;
  font-size: 12px;

  &.no-more {
    color: #ccc;
  }
}

.empty-history {
  padding: 20px 0;
}

// ==================== 消息气泡 ====================
.chat-msg-item {
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

.leave-msg-image {
  margin-top: 6px;
}

.recall-action {
  margin-top: 4px;
  text-align: right;
  opacity: 0;
  transition: opacity 0.2s;

  &.always-visible {
    opacity: 1;
  }
}

.chat-msg-item:hover .recall-action {
  opacity: 1;
}

// ==================== 附件 ====================
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

    .file-icon { flex-shrink: 0; }

    .file-name {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

// ==================== 右侧访客信息 ====================
.detail-right {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;

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
</style>
