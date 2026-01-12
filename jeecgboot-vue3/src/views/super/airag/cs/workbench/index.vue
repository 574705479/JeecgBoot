<template>
  <div class="cs-workbench">
    <!-- 左侧会话列表 -->
    <div class="cs-sidebar">
      <!-- 顶部客服信息 -->
      <div class="cs-agent-info">
        <a-avatar :src="agentAvatar" :size="40">
          <template #icon><UserOutlined /></template>
        </a-avatar>
        <div class="agent-details">
          <span class="agent-name">{{ agentName }}</span>
          <a-tag :color="agentStatus === 1 ? 'green' : 'default'">
            {{ agentStatus === 1 ? '在线' : '离线' }}
          </a-tag>
        </div>
      </div>

      <!-- 筛选器 -->
      <div class="cs-filter">
        <a-radio-group v-model:value="filter" button-style="solid" size="small">
          <a-radio-button value="mine">我负责</a-radio-button>
          <a-radio-button value="unassigned">未分配</a-radio-button>
          <a-radio-button value="collab">协作中</a-radio-button>
        </a-radio-group>
      </div>

      <!-- 会话列表 -->
      <div class="cs-conversation-list">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="cs-conversation-item"
          :class="{ 
            active: currentConversation?.id === conv.id,
            unread: conv.unreadCount > 0,
            closed: conv.status === 2
          }"
          @click="selectConversation(conv)"
        >
          <a-avatar :size="40" class="conv-avatar">
            {{ conv.userName?.charAt(0) || '访' }}
          </a-avatar>
          <div class="conv-info">
            <div class="conv-header">
              <span class="conv-name">{{ conv.userName || '访客' }}</span>
              <span class="conv-time">{{ formatTime(conv.lastMessageTime) }}</span>
            </div>
            <div class="conv-message">
              {{ conv.lastMessage || '暂无消息' }}
            </div>
            <div class="conv-meta">
              <a-tag v-if="conv.status === 0" color="blue" size="small">未分配</a-tag>
              <a-tag v-else-if="conv.status === 2" color="default" size="small">已结束</a-tag>
              <a-tag v-else :color="getModeColor(conv.replyMode)" size="small">
                {{ getModeName(conv.replyMode) }}
              </a-tag>
              <a-badge v-if="conv.unreadCount > 0" :count="conv.unreadCount" />
            </div>
          </div>
          <a-button 
            v-if="conv.status === 0" 
            type="primary" 
            size="small"
            @click.stop="assignConversation(conv.id)"
          >
            接入
          </a-button>
        </div>
        
        <a-empty v-if="conversations.length === 0" description="暂无会话" />
      </div>
    </div>

    <!-- 中间聊天区域 -->
    <div class="cs-chat-panel" v-if="currentConversation">
      <!-- 聊天头部 -->
      <div class="chat-header">
        <div class="chat-user-info">
          <a-avatar :size="36" class="user-avatar" @click="showVisitorDetail">
            {{ currentConversation.userName?.charAt(0) || '访' }}
          </a-avatar>
          <div class="user-details" @click="showVisitorDetail" style="cursor: pointer;">
            <span class="user-name">
              {{ getVisitorDisplayName(currentConversation) }}
              <StarFilled v-if="currentVisitorStar" class="visitor-star" />
              <a-tag v-if="currentVisitorLevel === 3" color="gold" size="small">VIP</a-tag>
              <a-tag v-else-if="currentVisitorLevel === 2" color="blue" size="small">重要</a-tag>
            </span>
            <a-tag :color="userOnline ? 'green' : 'default'" size="small">
              {{ userOnline ? '在线' : '离线' }}
            </a-tag>
          </div>
          <a-button type="link" size="small" @click="showVisitorDetail">
            <InfoCircleOutlined /> 详情
          </a-button>
        </div>
        
        <!-- 回复模式切换 -->
        <div class="chat-mode" v-if="currentConversation.status === 1">
          <span>回复模式：</span>
          <a-select 
            v-model:value="currentReplyMode" 
            size="small" 
            style="width: 120px"
            @change="changeMode"
          >
            <a-select-option :value="0">AI自动</a-select-option>
            <a-select-option :value="1">手动</a-select-option>
            <a-select-option :value="2">AI辅助</a-select-option>
          </a-select>
        </div>

        <div class="chat-actions">
          <a-button v-if="currentConversation.status !== 2" @click="showTransferModal = true">
            移交
          </a-button>
          <a-button v-if="currentConversation.status !== 2" danger @click="closeConversation">
            结束
          </a-button>
        </div>
      </div>

      <!-- 协作者栏 -->
      <div class="collaborator-bar" v-if="collaborators.length > 0">
        <span class="collab-label">协作中：</span>
        <a-avatar-group :max-count="5" size="small">
          <a-tooltip v-for="collab in collaborators" :key="collab.agentId" :title="collab.agentName">
            <a-avatar :style="{ backgroundColor: collab.role === 0 ? '#1890ff' : '#87d068' }">
              {{ collab.agentName?.charAt(0) || 'A' }}
            </a-avatar>
          </a-tooltip>
        </a-avatar-group>
        <a-button type="link" size="small" @click="showInviteModal = true">
          <PlusOutlined /> 邀请
        </a-button>
      </div>

      <!-- 消息区域 -->
      <div class="chat-messages" ref="messagesRef">
        <div v-for="msg in messages" :key="msg.id" class="message-item" :class="getMessageClass(msg)">
          <div v-if="msg.senderType === 3" class="system-message">
            {{ msg.content }}
          </div>
          <template v-else>
            <a-avatar v-if="msg.senderType === 0" :size="32" class="msg-avatar">
              {{ msg.senderName?.charAt(0) || '访' }}
            </a-avatar>
            <div class="msg-content">
              <div class="msg-header" v-if="msg.senderType !== 0">
                <span class="msg-sender">{{ msg.senderName }}</span>
                <a-tag v-if="msg.isAiGenerated" color="purple" size="small">AI</a-tag>
              </div>
              <div class="msg-bubble" :class="{ 'ai-bubble': msg.isAiGenerated }">
                <div class="msg-text" v-html="renderMarkdown(msg.content)"></div>
              </div>
              <div class="msg-time">{{ formatMessageTime(msg.createTime) }}</div>
            </div>
            <a-avatar v-if="msg.senderType !== 0" :size="32" class="msg-avatar agent-avatar">
              {{ msg.actualSenderName?.charAt(0) || msg.senderName?.charAt(0) || 'A' }}
            </a-avatar>
          </template>
        </div>
      </div>

      <!-- AI建议卡片 -->
      <div class="ai-suggestion-card" v-if="aiSuggestion && currentReplyMode === 2">
        <div class="suggestion-header">
          <RobotOutlined /> AI建议回复
        </div>
        <div class="suggestion-content">{{ aiSuggestion }}</div>
        <div class="suggestion-actions">
          <a-button type="primary" size="small" @click="confirmAiSuggestion(false)">
            直接发送
          </a-button>
          <a-button size="small" @click="editAiSuggestion">
            编辑后发送
          </a-button>
          <a-button size="small" @click="aiSuggestion = ''">
            忽略
          </a-button>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="chat-input" v-if="currentConversation.status !== 2">
        <a-textarea
          v-model:value="inputMessage"
          :placeholder="inputPlaceholder"
          :auto-size="{ minRows: 2, maxRows: 6 }"
          @keydown.enter.exact.prevent="sendMessage"
        />
        <div class="input-actions">
          <a-button type="primary" @click="sendMessage" :disabled="!inputMessage.trim()">
            发送
          </a-button>
        </div>
      </div>
      <div class="chat-closed" v-else>
        <span>会话已结束</span>
      </div>
    </div>

    <!-- 空状态 -->
    <div class="cs-empty" v-else>
      <a-empty description="选择一个会话开始聊天" />
    </div>

    <!-- 移交弹窗 -->
    <a-modal v-model:open="showTransferModal" title="移交会话" @ok="transferConversation">
      <a-select
        v-model:value="transferToAgent"
        placeholder="选择目标客服"
        style="width: 100%"
        :options="availableAgents"
      />
    </a-modal>

    <!-- 邀请协作弹窗 -->
    <a-modal v-model:open="showInviteModal" title="邀请协作" @ok="inviteCollaborator">
      <a-select
        v-model:value="inviteAgent"
        placeholder="选择要邀请的客服"
        style="width: 100%"
        :options="availableAgents"
      />
    </a-modal>

    <!-- 访客详情弹窗 -->
    <CsVisitorModal
      v-model:open="showVisitorModal"
      :appId="currentConversation?.appId || ''"
      :userId="currentConversation?.userId || ''"
      @saved="onVisitorSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue';
import { message } from 'ant-design-vue';
import { UserOutlined, PlusOutlined, RobotOutlined, StarFilled, InfoCircleOutlined } from '@ant-design/icons-vue';
import { defHttp } from '/@/utils/http/axios';
import { useUserStore } from '/@/store/modules/user';
import MarkdownIt from 'markdown-it';
import hljs from 'highlight.js';
import CsVisitorModal from '../visitor/CsVisitorModal.vue';

// Markdown渲染器
const md = new MarkdownIt({
  highlight: function (str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(str, { language: lang }).value;
      } catch (__) {}
    }
    return '';
  }
});

// 客服信息
const userStore = useUserStore();
const agentId = ref('');
const agentName = ref('');
const agentAvatar = ref('');
const agentStatus = ref(0);

// 会话列表
const filter = ref('mine');
const conversations = ref<any[]>([]);
const currentConversation = ref<any>(null);
const currentReplyMode = ref(0);

// 协作者
const collaborators = ref<any[]>([]);

// 消息
const messages = ref<any[]>([]);
const inputMessage = ref('');
const messagesRef = ref<HTMLElement | null>(null);

// AI建议
const aiSuggestion = ref('');

// 用户状态
const userOnline = ref(false);

// 弹窗
const showTransferModal = ref(false);
const showInviteModal = ref(false);
const showVisitorModal = ref(false);
const transferToAgent = ref('');
const inviteAgent = ref('');
const availableAgents = ref<any[]>([]);

// 访客信息缓存
const visitorCache = ref<Map<string, any>>(new Map());
const currentVisitorLevel = ref(1);
const currentVisitorStar = ref(false);

// WebSocket
let ws: WebSocket | null = null;
let refreshTimer: number | null = null;

// 计算属性
const inputPlaceholder = computed(() => {
  if (currentReplyMode.value === 0) {
    return 'AI自动回复中，输入消息将切换为手动模式...';
  }
  return '输入消息...（Enter发送）';
});

// 初始化
onMounted(async () => {
  await loadAgentInfo();
  await loadConversations();
  connectWebSocket();
  
  // 定时刷新会话列表
  refreshTimer = window.setInterval(() => {
    loadConversations();
  }, 10000);
});

onUnmounted(() => {
  if (ws) {
    ws.close();
  }
  if (refreshTimer) {
    clearInterval(refreshTimer);
  }
});

// 监听筛选变化
watch(filter, () => {
  loadConversations();
});

// 加载客服信息
async function loadAgentInfo() {
  try {
    const res = await defHttp.get({ url: '/cs/agent/current' });
    if (res?.id) {
      agentId.value = res.id;
      agentName.value = res.nickname || '客服';
      agentAvatar.value = res.avatar;
      agentStatus.value = res.status || 0;
      
      if (agentStatus.value === 0) {
        await defHttp.post({ url: `/cs/agent/online/${agentId.value}` });
        agentStatus.value = 1;
      }
    }
  } catch (e) {
    console.error('加载客服信息失败', e);
  }
}

// 加载会话列表
async function loadConversations() {
  try {
    const res = await defHttp.get({
      url: '/cs/conversation/list',
      params: {
        agentId: agentId.value,
        filter: filter.value,
        pageNo: 1,
        pageSize: 50
      }
    });
    conversations.value = res?.records || [];
  } catch (e) {
    console.error('加载会话列表失败', e);
  }
}

// 选择会话
async function selectConversation(conv: any) {
  currentConversation.value = conv;
  currentReplyMode.value = conv.replyMode || 0;
  
  // 加载消息
  await loadMessages(conv.id);
  
  // 加载协作者
  await loadCollaborators(conv.id);
  
  // 加载访客信息
  await loadVisitorInfo(conv.appId, conv.userId);
  
  // 清除未读
  if (conv.unreadCount > 0) {
    await defHttp.post({ url: `/cs/conversation/${conv.id}/clear-unread` });
    conv.unreadCount = 0;
  }
  
  // 检查用户在线状态
  checkUserOnline();
}

// 加载访客信息
async function loadVisitorInfo(appId: string, userId: string) {
  const cacheKey = `${appId}_${userId}`;
  
  // 尝试从缓存获取
  if (visitorCache.value.has(cacheKey)) {
    const visitor = visitorCache.value.get(cacheKey);
    currentVisitorLevel.value = visitor.level || 1;
    currentVisitorStar.value = visitor.star === 1;
    return;
  }
  
  try {
    const res = await defHttp.get({
      url: '/airag/cs/visitor/getByUser',
      params: { appId, userId }
    });
    
    if (res) {
      visitorCache.value.set(cacheKey, res);
      currentVisitorLevel.value = res.level || 1;
      currentVisitorStar.value = res.star === 1;
    } else {
      currentVisitorLevel.value = 1;
      currentVisitorStar.value = false;
    }
  } catch {
    currentVisitorLevel.value = 1;
    currentVisitorStar.value = false;
  }
}

// 获取访客显示名称
function getVisitorDisplayName(conv: any): string {
  if (!conv) return '访客';
  
  const cacheKey = `${conv.appId}_${conv.userId}`;
  const visitor = visitorCache.value.get(cacheKey);
  
  if (visitor) {
    if (visitor.nickname) return visitor.nickname;
    if (visitor.realName) return visitor.realName;
  }
  
  return conv.userName || '访客';
}

// 显示访客详情弹窗
function showVisitorDetail() {
  if (!currentConversation.value) return;
  showVisitorModal.value = true;
}

// 访客信息保存后回调
async function onVisitorSaved() {
  if (!currentConversation.value) return;
  
  // 清除缓存，重新加载
  const cacheKey = `${currentConversation.value.appId}_${currentConversation.value.userId}`;
  visitorCache.value.delete(cacheKey);
  
  await loadVisitorInfo(currentConversation.value.appId, currentConversation.value.userId);
}

// 加载消息
async function loadMessages(conversationId: string) {
  try {
    const res = await defHttp.get({
      url: `/cs/message/${conversationId}`,
      params: { limit: 100 }
    });
    messages.value = res || [];
    scrollToBottom();
  } catch (e) {
    console.error('加载消息失败', e);
  }
}

// 加载协作者
async function loadCollaborators(conversationId: string) {
  try {
    const res = await defHttp.get({ url: `/cs/collaborator/${conversationId}` });
    collaborators.value = res || [];
  } catch (e) {
    console.error('加载协作者失败', e);
  }
}

// 发送消息
async function sendMessage() {
  const content = inputMessage.value.trim();
  if (!content || !currentConversation.value) return;
  
  try {
    // 如果是AI自动模式，切换为手动模式
    if (currentReplyMode.value === 0) {
      await changeMode(1);
    }
    
    await defHttp.post({
      url: '/cs/message/agent/send',
      params: {
        conversationId: currentConversation.value.id,
        agentId: agentId.value,
        agentName: agentName.value,
        content: content
      }
    });
    
    inputMessage.value = '';
    await loadMessages(currentConversation.value.id);
  } catch (e) {
    message.error('发送失败');
  }
}

// 接入会话
async function assignConversation(conversationId: string) {
  try {
    const res = await defHttp.post({
      url: `/cs/conversation/${conversationId}/assign`,
      params: { agentId: agentId.value }
    }, { isTransformResponse: false });
    
    const result = res.result || res;
    if (result.success) {
      message.success('接入成功');
      await loadConversations();
      if (result.conversation) {
        await selectConversation(result.conversation);
      }
    } else {
      message.error(result.message || '接入失败');
    }
  } catch (e) {
    message.error('接入失败');
  }
}

// 切换回复模式
async function changeMode(mode: number) {
  if (!currentConversation.value) return;
  
  try {
    await defHttp.put({
      url: `/cs/conversation/${currentConversation.value.id}/mode`,
      params: { mode }
    });
    currentReplyMode.value = mode;
    currentConversation.value.replyMode = mode;
  } catch (e) {
    message.error('切换失败');
  }
}

// 结束会话
async function closeConversation() {
  if (!currentConversation.value) return;
  
  try {
    await defHttp.post({ url: `/cs/conversation/${currentConversation.value.id}/close` });
    message.success('会话已结束');
    currentConversation.value.status = 2;
    await loadConversations();
  } catch (e) {
    message.error('操作失败');
  }
}

// 移交会话
async function transferConversation() {
  if (!currentConversation.value || !transferToAgent.value) return;
  
  try {
    await defHttp.post({
      url: `/cs/conversation/${currentConversation.value.id}/transfer`,
      params: {
        toAgentId: transferToAgent.value,
        fromAgentId: agentId.value
      }
    });
    message.success('移交成功');
    showTransferModal.value = false;
    await loadConversations();
  } catch (e) {
    message.error('移交失败');
  }
}

// 邀请协作
async function inviteCollaborator() {
  if (!currentConversation.value || !inviteAgent.value) return;
  
  try {
    await defHttp.post({
      url: '/cs/collaborator/invite',
      params: {
        conversationId: currentConversation.value.id,
        agentId: inviteAgent.value,
        inviteBy: agentId.value
      }
    });
    message.success('邀请成功');
    showInviteModal.value = false;
    await loadCollaborators(currentConversation.value.id);
  } catch (e) {
    message.error('邀请失败');
  }
}

// 确认AI建议
async function confirmAiSuggestion(edited: boolean) {
  if (!currentConversation.value || !aiSuggestion.value) return;
  
  try {
    await defHttp.post({
      url: `/cs/message/ai-confirm/${currentConversation.value.id}`,
      params: {
        agentId: agentId.value,
        agentName: agentName.value,
        editedContent: edited ? inputMessage.value : null
      }
    });
    
    aiSuggestion.value = '';
    inputMessage.value = '';
    await loadMessages(currentConversation.value.id);
  } catch (e) {
    message.error('发送失败');
  }
}

// 编辑AI建议
function editAiSuggestion() {
  inputMessage.value = aiSuggestion.value;
  aiSuggestion.value = '';
}

// 检查用户在线
function checkUserOnline() {
  // 简化实现，通过WebSocket状态判断
  userOnline.value = true;
}

// WebSocket连接
function connectWebSocket() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const host = window.location.host;
  const wsUrl = `${protocol}//${host}/jeecg-boot/ws/cs?userId=${agentId.value}&userType=agent`;
  
  ws = new WebSocket(wsUrl);
  
  ws.onopen = () => {
    console.log('[CS-WS] 连接成功');
  };
  
  ws.onmessage = (event) => {
    handleWsMessage(JSON.parse(event.data));
  };
  
  ws.onclose = () => {
    console.log('[CS-WS] 连接关闭');
    // 5秒后重连
    setTimeout(connectWebSocket, 5000);
  };
}

// 处理WebSocket消息
function handleWsMessage(data: any) {
  switch (data.type) {
    case 'message':
      if (currentConversation.value?.id === data.conversationId) {
        loadMessages(data.conversationId);
      }
      loadConversations();
      break;
      
    case 'new_conversation':
      loadConversations();
      break;
      
    case 'ai_suggestion':
      if (currentConversation.value?.id === data.conversationId) {
        aiSuggestion.value = data.content;
      }
      break;
      
    case 'user_offline':
      if (currentConversation.value?.id === data.conversationId) {
        userOnline.value = false;
      }
      break;
      
    case 'conversation_closed':
      if (currentConversation.value?.id === data.conversationId) {
        currentConversation.value.status = 2;
      }
      loadConversations();
      break;
  }
}

// 工具函数
function formatTime(time: string) {
  if (!time) return '';
  const date = new Date(time);
  const now = new Date();
  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  }
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' });
}

function formatMessageTime(time: string) {
  if (!time) return '';
  return new Date(time).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
}

function getModeColor(mode: number) {
  return mode === 0 ? 'green' : (mode === 1 ? 'orange' : 'purple');
}

function getModeName(mode: number) {
  return mode === 0 ? 'AI自动' : (mode === 1 ? '手动' : 'AI辅助');
}

function getMessageClass(msg: any) {
  if (msg.senderType === 3) return 'system';
  return msg.senderType === 0 ? 'user' : 'agent';
}

function renderMarkdown(content: string) {
  if (!content) return '';
  return md.render(content);
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight;
    }
  });
}
</script>

<style lang="less" scoped>
.cs-workbench {
  display: flex;
  height: 100%;
  background: #f5f7fa;
}

.cs-sidebar {
  width: 320px;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
}

.cs-agent-info {
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #f0f0f0;
  
  .agent-details {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
  
  .agent-name {
    font-weight: 500;
  }
}

.cs-filter {
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.cs-conversation-list {
  flex: 1;
  overflow-y: auto;
}

.cs-conversation-item {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
  transition: background 0.2s;
  
  &:hover {
    background: #f9f9f9;
  }
  
  &.active {
    background: #e6f7ff;
  }
  
  &.unread {
    background: #fff7e6;
  }
  
  &.closed {
    opacity: 0.6;
  }
  
  .conv-avatar {
    background: #1890ff;
    color: #fff;
  }
  
  .conv-info {
    flex: 1;
    min-width: 0;
  }
  
  .conv-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 4px;
  }
  
  .conv-name {
    font-weight: 500;
  }
  
  .conv-time {
    font-size: 12px;
    color: #999;
  }
  
  .conv-message {
    font-size: 13px;
    color: #666;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  
  .conv-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 4px;
  }
}

.cs-chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.chat-header {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  
  .chat-user-info {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  
  .user-details {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  
  .user-name {
    font-weight: 500;
    font-size: 16px;
    display: flex;
    align-items: center;
    gap: 6px;
    
    .visitor-star {
      color: #faad14;
      font-size: 14px;
    }
  }
  
  .user-avatar {
    cursor: pointer;
    transition: transform 0.2s;
    
    &:hover {
      transform: scale(1.05);
    }
  }
  
  .chat-mode {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  
  .chat-actions {
    display: flex;
    gap: 8px;
  }
}

.collaborator-bar {
  padding: 8px 16px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  gap: 8px;
  
  .collab-label {
    font-size: 12px;
    color: #666;
  }
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.message-item {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
  
  &.user {
    .msg-bubble {
      background: #e6f7ff;
    }
  }
  
  &.agent {
    flex-direction: row-reverse;
    
    .msg-content {
      align-items: flex-end;
    }
    
    .msg-bubble {
      background: #f6ffed;
      
      &.ai-bubble {
        background: #f9f0ff;
      }
    }
  }
  
  &.system {
    justify-content: center;
  }
}

.system-message {
  font-size: 12px;
  color: #999;
  background: #f5f5f5;
  padding: 4px 12px;
  border-radius: 12px;
}

.msg-content {
  display: flex;
  flex-direction: column;
  max-width: 70%;
}

.msg-header {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 4px;
  
  .msg-sender {
    font-size: 12px;
    color: #666;
  }
}

.msg-bubble {
  padding: 8px 12px;
  border-radius: 8px;
  
  .msg-text {
    word-break: break-word;
    
    :deep(p) {
      margin: 0;
    }
    
    :deep(pre) {
      background: #f5f5f5;
      padding: 8px;
      border-radius: 4px;
      overflow-x: auto;
    }
  }
}

.msg-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.msg-avatar {
  background: #1890ff;
  color: #fff;
  flex-shrink: 0;
  
  &.agent-avatar {
    background: #52c41a;
  }
}

.ai-suggestion-card {
  margin: 8px 16px;
  padding: 12px;
  background: #f9f0ff;
  border: 1px solid #d3adf7;
  border-radius: 8px;
  
  .suggestion-header {
    font-weight: 500;
    color: #722ed1;
    margin-bottom: 8px;
    display: flex;
    align-items: center;
    gap: 4px;
  }
  
  .suggestion-content {
    padding: 8px;
    background: #fff;
    border-radius: 4px;
    margin-bottom: 8px;
  }
  
  .suggestion-actions {
    display: flex;
    gap: 8px;
  }
}

.chat-input {
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
  
  .input-actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 8px;
  }
}

.chat-closed {
  padding: 16px;
  text-align: center;
  color: #999;
  border-top: 1px solid #f0f0f0;
}

.cs-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
