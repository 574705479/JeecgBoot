<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    title="会话详情"
    :footer="null"
    width="900px"
  >
    <div class="conversation-detail">
      <!-- 基本信息 -->
      <a-descriptions :column="3" bordered size="small" class="info-section">
        <a-descriptions-item label="会话ID" :span="2">
          <ATypographyText copyable>{{ record?.id }}</ATypographyText>
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getStatusColor(record?.status)">{{ getStatusText(record?.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="访客">
          <span class="user-info">
            <a-avatar size="small">{{ (record?.userName || '访').charAt(0) }}</a-avatar>
            {{ record?.userName || record?.userId || '匿名访客' }}
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
        <a-descriptions-item label="消息总数">{{ record?.messageCount || 0 }} 条</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ record?.createTime }}</a-descriptions-item>
        <a-descriptions-item label="接入时间">{{ record?.assignTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="结束时间">{{ record?.endTime || '-' }}</a-descriptions-item>
      </a-descriptions>

      <!-- 消息记录 -->
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
                <!-- 系统消息 -->
                <template v-if="msg.senderType === 3">
                  <div class="system-message">
                    <span class="system-text">{{ msg.content }}</span>
                    <span class="system-time">{{ formatTime(msg.createTime) }}</span>
                  </div>
                </template>
                
                <!-- 用户消息 -->
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
                      <div class="msg-bubble user-bubble">{{ msg.content }}</div>
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
                
                <!-- AI/客服消息 -->
                <template v-else>
                  <div class="agent-message">
                    <div class="msg-content">
                      <div class="msg-header">
                        <span class="msg-time">{{ formatTime(msg.createTime) }}</span>
                        <span class="sender-name">
                          {{ msg.senderName || (msg.senderType === 1 ? 'AI客服' : '客服') }}
                        </span>
                        <a-tag v-if="msg.senderType === 1" color="purple" size="small">AI</a-tag>
                        <a-tag v-else color="green" size="small">客服</a-tag>
                      </div>
                      <div class="msg-bubble agent-bubble" :class="{ 'ai-bubble': msg.senderType === 1 }">
                        {{ msg.content }}
                      </div>
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
                    <a-avatar :size="32" class="msg-avatar agent-avatar">
                      {{ msg.senderType === 1 ? 'AI' : (msg.senderName || '客').charAt(0) }}
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
import { createImgPreview } from '/@/components/Preview';

const { Text: ATypographyText } = Typography;

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
  record.value = data?.record;
  messages.value = [];
  historyBeforeId.value = null;
  hasMoreHistory.value = true;
  
  if (record.value?.id) {
    await loadMessages(record.value.id);
  }
});

async function loadMessages(conversationId: string) {
  loading.value = true;
  try {
    const res = await defHttp.get({ 
      url: `/cs/message/${conversationId}`, 
      params: { limit: historyPageSize } 
    });
    const list = Array.isArray(res) ? res : (res?.result || res?.records || []);
    messages.value = list || [];
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
    const olderMessages = Array.isArray(res) ? res : (res?.result || res?.records || []);
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
  switch (msg.senderType) {
    case 0: return 'msg-user';
    case 1: return 'msg-ai';
    case 2: return 'msg-agent';
    case 3: return 'msg-system';
    default: return '';
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
  .info-section {
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
    max-height: 450px;
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
  
  // 系统消息
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
  
  // 用户消息（左侧）
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
  
  // 客服/AI消息（右侧）
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
    }
  }
  
  .msg-bubble {
    padding: 10px 14px;
    font-size: 14px;
    line-height: 1.6;
    word-break: break-word;
    white-space: pre-wrap;
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
