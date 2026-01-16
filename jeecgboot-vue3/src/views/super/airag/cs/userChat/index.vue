<template>
  <div class="user-chat-container">
    <!-- 头部 -->
    <div class="chat-header">
      <div class="header-info">
        <img class="app-avatar" :src="appInfo.avatar || defaultAvatar" />
        <div class="app-info">
          <span class="app-name">{{ appInfo.name || '在线客服' }}</span>
          <span class="status-text">
            <span :class="['status-dot', connectionStatus]"></span>
            {{ connectionStatusText }}
            <a-tag v-if="hasAgent && replyMode === 1" color="green" size="small" style="margin-left: 6px;">人工服务</a-tag>
            <a-tag v-else-if="replyMode === 0" color="blue" size="small" style="margin-left: 6px;">AI客服</a-tag>
          </span>
        </div>
      </div>
      <div class="header-actions">
        <a-tooltip title="清空聊天记录">
          <DeleteOutlined @click="clearMessages" />
        </a-tooltip>
      </div>
    </div>

    <!-- 消息区域 -->
    <div class="chat-messages" ref="messagesRef">
      <div v-if="loading" class="loading-wrapper">
        <a-spin />
      </div>
      <template v-else>
        <div v-if="messages.length === 0" class="empty-messages">
          <MessageOutlined style="font-size: 48px; color: #bfbfbf" />
          <p>开始您的咨询吧~</p>
        </div>
        <div v-for="msg in messages" :key="msg.id" 
             :class="['message-item', getMessageClass(msg)]">
          <!-- 系统消息 -->
          <div v-if="msg.senderType === 3" class="system-message">
            {{ msg.content }}
          </div>
          <!-- 用户消息 (senderType === 0 表示用户) -->
          <div v-else-if="isUserMessage(msg)" class="user-message">
            <div class="message-content">
              <div class="message-text">{{ msg.content }}</div>
              <div class="message-time">{{ formatTime(msg.createTime) }}</div>
            </div>
            <img class="avatar" :src="defaultUserAvatar" />
          </div>
          <!-- 客服/AI消息 (senderType === 1 AI, 2 客服) -->
          <div v-else class="agent-message">
            <img class="avatar" :src="appInfo.avatar || defaultAvatar" />
            <div class="message-content">
              <div class="sender-info">
                <span class="sender-name">{{ msg.senderName || '客服' }}</span>
                <a-tag v-if="msg.senderType === 1" color="blue" size="small">AI</a-tag>
              </div>
              <div class="message-text" v-html="renderMessage(msg.content)"></div>
              <div class="message-time">{{ formatTime(msg.createTime) }}</div>
            </div>
          </div>
        </div>
        <!-- 客服正在输入提示 -->
        <div v-if="agentTyping" class="typing-indicator">
          <img class="avatar" :src="appInfo.avatar || defaultAvatar" />
          <div class="typing-dots">
            <span></span><span></span><span></span>
          </div>
        </div>
      </template>
    </div>

    <!-- 预设问题 -->
    <div v-if="presetQuestions.length > 0" class="preset-questions">
      <div class="preset-title">
        <BulbOutlined />
        <span>常见问题</span>
      </div>
      <div class="preset-list">
        <a-button 
          v-for="(question, index) in presetQuestions" 
          :key="index"
          size="small"
          @click="selectPresetQuestion(question)"
        >
          {{ question }}
        </a-button>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input" v-if="!conversationClosed">
      <a-textarea
        v-model:value="inputMessage"
        :placeholder="aiResponding ? 'AI正在回复中，请稍候...' : '请输入您要咨询的问题...'"
        :auto-size="{ minRows: 1, maxRows: 4 }"
        :disabled="aiResponding"
        @keydown="handleKeydown"
      />
      <a-button 
        type="primary" 
        @click="sendMessage" 
        :loading="sending || aiResponding" 
        :disabled="!inputMessage.trim() || aiResponding"
      >
        <SendOutlined />
        {{ aiResponding ? 'AI回复中...' : '发送' }}
      </a-button>
    </div>
    <!-- 会话已结束时显示重新开始按钮 -->
    <div class="chat-closed" v-else>
      <span>会话已结束</span>
      <a-button type="primary" @click="restartConversation">
        重新开始对话
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts" name="UserChatPage">
import { ref, onMounted, onUnmounted, nextTick, computed, watch } from 'vue';
import { message } from 'ant-design-vue';
import { DeleteOutlined, MessageOutlined, SendOutlined, BulbOutlined } from '@ant-design/icons-vue';
import { defHttp } from '/@/utils/http/axios';

const silentRequestOptions = { successMessageMode: 'none' as const };
function httpGet<T = any>(config: any, options: any = {}) {
  return defHttp.get<T>(config, { ...silentRequestOptions, ...options });
}
function httpPost<T = any>(config: any, options: any = {}) {
  return defHttp.post<T>(config, { ...silentRequestOptions, ...options });
}

// 应用信息
const appInfo = ref({
  id: '',
  name: '在线客服',
  avatar: '',
  prologue: '', // 开场白
  presetQuestion: '', // 预设问题（逗号或换行分隔）
});
const defaultAvatar = 'https://gw.alipayobjects.com/zos/rmsportal/KDpgvguMpGfqaHPjicRK.svg';
const defaultUserAvatar = 'https://gw.alipayobjects.com/zos/antfincdn/XAosXuNZyF/BiazfanxmamNRoxxVxka.png';

// 预设问题列表
const presetQuestions = computed(() => {
  if (!appInfo.value.presetQuestion) return [];
  
  const rawQuestion = appInfo.value.presetQuestion;
  
  // 尝试解析JSON格式 (如: [{"key":1,"descr":"问题1"},{"key":2,"descr":"问题2"}])
  try {
    if (rawQuestion.trim().startsWith('[')) {
      const parsed = JSON.parse(rawQuestion);
      if (Array.isArray(parsed)) {
        return parsed
          .map((item: any) => item.descr || item.question || item.content || '')
          .filter((q: string) => q.length > 0);
      }
    }
  } catch {
    // 解析失败，尝试其他格式
  }
  
  // 支持逗号或换行分隔的纯文本格式
  return rawQuestion
    .split(/[,，\n]/)
    .map((q: string) => q.trim())
    .filter((q: string) => q.length > 0);
});

// 消息列表
const messages = ref<any[]>([]);
const loading = ref(false);
const sending = ref(false);
const inputMessage = ref('');
const messagesRef = ref<HTMLElement | null>(null);

// WebSocket
let ws: WebSocket | null = null;
const wsConnected = ref(false);
const agentTyping = ref(false);
let wsReconnectTimer: number | null = null;
let wsManuallyClosed = false;

// AI回复中状态（用于限制用户快速发送）
const aiResponding = ref(false);
let aiResponseTimeoutTimer: number | null = null;

function stopAiResponding(reason?: string) {
  if (aiResponseTimeoutTimer) {
    clearTimeout(aiResponseTimeoutTimer);
    aiResponseTimeoutTimer = null;
  }
  if (aiResponding.value) {
    aiResponding.value = false;
  }
  if (reason) {
    messages.value.push({
      id: Date.now().toString(),
      content: reason,
      senderType: 3,
      createTime: new Date().toISOString(),
    });
    scrollToBottom();
  }
}

// 流式AI消息临时存储 (messageId -> 累积内容)
const streamingMessages = ref<Map<string, string>>(new Map());

// 用户信息
const userId = ref('');
const userName = ref('访客');
const conversationId = ref('');
const conversationClosed = ref(false);  // 会话是否已结束
const replyMode = ref(0);  // 回复模式: 0=AI自动, 1=手动
const hasAgent = ref(false);  // 是否有客服接入

// 连接状态
const connectionStatus = computed(() => {
  if (!wsConnected.value) return 'offline';
  return 'online';
});

const connectionStatusText = computed(() => {
  if (!wsConnected.value) return '连接中...';
  return '在线';
});

// 初始化
onMounted(async () => {
  // 生成或获取用户ID
  initUserId();

  // 获取或创建会话
  await initConversation();

  // 加载历史消息
  await loadMessages();

  // 连接WebSocket
  connectWebSocket();

  // 滚动到底部
  scrollToBottom();
});

onUnmounted(() => {
  disconnectWebSocket();
});

// 初始化用户ID
function initUserId() {
  // 从localStorage获取或生成新的用户ID
  let storedUserId = localStorage.getItem('cs_user_id');
  if (!storedUserId) {
    storedUserId = 'user_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    localStorage.setItem('cs_user_id', storedUserId);
  }
  userId.value = storedUserId;

  // 获取用户名
  const storedUserName = localStorage.getItem('cs_user_name');
  if (storedUserName) {
    userName.value = storedUserName;
  }
}


// 选择预设问题
function selectPresetQuestion(question: string) {
  inputMessage.value = question;
  // 直接发送
  sendMessage();
}

// 初始化会话
async function initConversation() {
  try {
    // 从localStorage获取已有的会话ID（现在不需要appId）
    const storedConvId = localStorage.getItem(`cs_conversation_${userId.value}`);
    if (storedConvId) {
      // 检查会话是否已结束
      try {
        const convRes = await httpGet({ url: `/cs/conversation/${storedConvId}` });
        if (convRes && convRes.status === 2) {
          // 会话已结束，清除存储并创建新会话
          localStorage.removeItem(`cs_conversation_${userId.value}`);
        } else {
          conversationId.value = storedConvId;
          return;
        }
      } catch {
        // 获取会话失败，创建新会话
        localStorage.removeItem(`cs_conversation_${userId.value}`);
      }
    }

    // 创建新会话（不再传appId，AI应用由客服工作台选择）
    const res = await httpPost({
      url: '/cs/conversation/create',
      data: {
        userId: userId.value,
        userName: userName.value,
      },
    });
    if (res) {
      // 判断是否为 Result 包装
      if (res.id) {
        conversationId.value = res.id;
        localStorage.setItem(`cs_conversation_${userId.value}`, res.id);
      } else if (res.result && res.result.id) {
        conversationId.value = res.result.id;
        localStorage.setItem(`cs_conversation_${userId.value}`, res.result.id);
      }
    }
  } catch (e) {
    console.error('初始化会话失败', e);
    // 使用临时会话ID
    conversationId.value = `temp_${userId.value}_${Date.now()}`;
  }
}

// 加载历史消息
async function loadMessages() {
  if (!conversationId.value || conversationId.value.startsWith('temp_')) return;

  loading.value = true;
  try {
    const res = await httpGet({
      url: '/cs/message/list',
      params: {
        conversationId: conversationId.value,
        limit: 100,
      },
    });
    if (res) {
      messages.value = Array.isArray(res) ? res : (res.result || []);
    }
  } catch {
    // 忽略错误
  } finally {
    loading.value = false;
  }
}

// 连接WebSocket
function connectWebSocket() {
  if (!conversationId.value || !userId.value) {
    console.warn('缺少conversationId或userId，无法连接WebSocket');
    return;
  }
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
    return;
  }
  if (wsReconnectTimer) {
    clearTimeout(wsReconnectTimer);
    wsReconnectTimer = null;
  }
  wsManuallyClosed = false;

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const host = window.location.host; // 使用当前页面的host（包含端口），通过代理连接
  // 通过前端代理连接后端WebSocket，路径前缀 /jeecgboot（不再需要appId）
  const wsUrl = `${protocol}//${host}/jeecgboot/ws/cs/user?userId=${userId.value}&conversationId=${conversationId.value}`;

  console.log('[UserChat] 连接WebSocket:', wsUrl);

  ws = new WebSocket(wsUrl);

  ws.onopen = () => {
    console.log('[UserChat] WebSocket已连接');
    wsConnected.value = true;
    startHeartbeat();
  };

  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      handleWsMessage(data);
    } catch (e) {
      console.error('[UserChat] 解析WebSocket消息失败', e);
    }
  };

  ws.onclose = (event) => {
    console.log('[UserChat] WebSocket已断开:', event.code, event.reason);
    wsConnected.value = false;
    stopHeartbeat();
    if (aiResponding.value && replyMode.value === 0) {
      stopAiResponding('网络中断，AI回复可能未完成，请稍后重试');
    }
    ws = null;
    // 自动重连
    if (!wsManuallyClosed) {
      wsReconnectTimer = window.setTimeout(() => {
        if (!wsConnected.value) {
          connectWebSocket();
        }
      }, 3000);
    }
  };

  ws.onerror = (error) => {
    console.error('[UserChat] WebSocket错误:', error);
    if (aiResponding.value && replyMode.value === 0) {
      stopAiResponding('网络异常，AI回复可能未完成，请稍后重试');
    }
  };
}

// 断开WebSocket
function disconnectWebSocket() {
  stopHeartbeat();
  wsManuallyClosed = true;
  if (wsReconnectTimer) {
    clearTimeout(wsReconnectTimer);
    wsReconnectTimer = null;
  }
  if (ws) {
    ws.close();
    ws = null;
  }
  wsConnected.value = false;
  stopAiResponding();
}

// 心跳
let heartbeatTimer: any = null;
function startHeartbeat() {
  heartbeatTimer = setInterval(() => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'ping' }));
    }
  }, 30000);
}

function stopHeartbeat() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer);
    heartbeatTimer = null;
  }
}

// 处理WebSocket消息
function handleWsMessage(data: any) {
  console.log('[UserChat] 收到消息:', data);

  switch (data.type) {
    case 'connected':
      console.log('[UserChat] 连接成功确认');
      // 获取会话的replyMode
      if (data.extra) {
        replyMode.value = data.extra.replyMode ?? 0;
        hasAgent.value = data.extra.hasAgent ?? false;
      }
      break;

    case 'message':
      // 收到新消息（来自客服或AI）
      const msgSenderType = Number(data.senderType);
      const newMsg = {
        id: data.messageId || Date.now().toString(),
        conversationId: data.conversationId,
        content: data.content,
        senderType: msgSenderType,
        senderId: data.senderId,
        senderName: data.senderName,
        createTime: data.timestamp || new Date().toISOString(),
      };
      // 避免重复添加
      if (!messages.value.find(m => m.id === newMsg.id)) {
        messages.value.push(newMsg);
        scrollToBottom();
      }
      // 收到非用户消息（AI/客服）后，解除等待状态
      // senderType: 0=用户, 1=客服, 2=AI, 3=系统
      if (msgSenderType !== 0) {
        console.log('[UserChat] 收到AI/客服回复，解除等待状态, senderType:', msgSenderType);
        stopAiResponding();
      }
      break;

    case 'system':
      // 系统消息
      messages.value.push({
        id: Date.now().toString(),
        content: data.content,
        senderType: 3,
        createTime: new Date().toISOString(),
      });
      scrollToBottom();
      break;

    case 'typing':
      // 客服正在输入
      agentTyping.value = true;
      setTimeout(() => {
        agentTyping.value = false;
      }, 3000);
      break;

    case 'ai_typing':
      // AI正在输入状态
      if (data.data?.isTyping) {
        agentTyping.value = true;
      } else {
        agentTyping.value = false;
      }
      break;

    case 'ai_stream':
      // AI流式消息 - 逐步显示
      handleAiStreamToken(data);
      break;

    case 'ai_stream_complete':
      // AI流式消息完成
      handleAiStreamComplete(data);
      break;

    case 'agent_connected':
      // 客服已接入，自动切换为手动模式
      hasAgent.value = true;
      if (data.extra?.replyMode !== undefined) {
        replyMode.value = data.extra.replyMode;
      } else {
        replyMode.value = 1; // 默认切换为手动模式
      }
      // ★ 切换为手动模式后，解除AI回复中状态
      stopAiResponding();
      messages.value.push({
        id: Date.now().toString(),
        content: data.content || `客服 ${data.extra?.agentName || data.senderName || ''} 已为您服务`,
        senderType: 3,
        createTime: new Date().toISOString(),
      });
      scrollToBottom();
      break;

    case 'mode_changed':
      // 模式切换通知
      if (data.extra?.replyMode !== undefined) {
        replyMode.value = data.extra.replyMode;
      }
      // ★ 切换为手动模式后，解除AI回复中状态
      if (replyMode.value === 1) {
        stopAiResponding();
      }
      console.log('[UserChat] 回复模式已切换为', replyMode.value === 1 ? '人工服务' : 'AI自动回复');
      break;

    case 'conversation_closed':
      // 会话已结束
      conversationClosed.value = true;
      messages.value.push({
        id: Date.now().toString(),
        content: data.content || '会话已结束，感谢您的咨询',
        senderType: 3,
        createTime: new Date().toISOString(),
      });
      scrollToBottom();
      break;

    case 'pong':
      // 心跳响应
      break;

    default:
      console.log('[UserChat] 未处理的消息类型:', data.type);
  }
}

// 发送消息
async function sendMessage() {
  const content = inputMessage.value.trim();
  if (!content) return;

  // AI自动模式下，AI回复中时不允许发送新消息
  if (replyMode.value === 0 && aiResponding.value) {
    message.warning('请等待AI回复完成');
    return;
  }

  if (!conversationId.value) {
    message.error('会话未初始化');
    return;
  }

  // 先添加到本地显示
  const localMsg = {
    id: 'local_' + Date.now(),
    conversationId: conversationId.value,
    content: content,
    senderType: 0, // 用户消息用0
    senderId: userId.value,
    senderName: userName.value,
    createTime: new Date().toISOString(),
  };
  messages.value.push(localMsg);
  
  // 清空输入框并等待 DOM 更新
  inputMessage.value = '';
  await nextTick();
  
  scrollToBottom();

  sending.value = true;
  
  // 只有AI自动模式才设置AI正在回复状态
  const isAiMode = replyMode.value === 0;
  if (isAiMode) {
    aiResponding.value = true;
  }
  
  // 设置超时自动解除AI回复状态（防止异常情况下一直锁定）
  if (aiResponseTimeoutTimer) {
    clearTimeout(aiResponseTimeoutTimer);
  }
  aiResponseTimeoutTimer = window.setTimeout(() => {
    if (isAiMode) {
      stopAiResponding('AI回复超时，请稍后重试');
    }
  }, 60000); // 60秒超时
  
  try {
    // 通过WebSocket发送
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({
        type: 'message',
        conversationId: conversationId.value,
        content: content,
        userName: userName.value,
      }));
    } else {
      // 降级：通过HTTP发送
      await httpPost({
        url: '/cs/message/send',
        data: {
          conversationId: conversationId.value,
          content: content,
          senderId: userId.value,
          senderName: userName.value,
          senderType: 'user',
        },
      });
    }
  } catch (e) {
    console.error('发送消息失败', e);
    message.error('发送失败，请重试');
    if (isAiMode) {
      stopAiResponding();
    }
  } finally {
    sending.value = false;
  }
}

// 处理键盘事件
function handleKeydown(e: KeyboardEvent) {
  // 只在按下 Enter 且不带 Shift 时发送
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    sendMessage();
  }
  // Shift+Enter 允许默认换行行为
}

// 重新开始对话
async function restartConversation() {
  try {
    // 断开当前WebSocket
    disconnectWebSocket();
    
    // 清除localStorage中的会话ID，以便创建新会话
    localStorage.removeItem(`cs_conversation_${userId.value}`);
    
    // 清空消息和状态
    messages.value = [];
    conversationClosed.value = false;
    conversationId.value = '';
    
    // 创建新会话
    await initConversation();
    
    // 重新连接WebSocket
    connectWebSocket();
    
    console.log('[UserChat] 已开始新对话');
  } catch (e) {
    console.error('[UserChat] 重新开始对话失败', e);
    message.error('重新开始失败，请刷新页面');
  }
}

// 清空消息
function clearMessages() {
  messages.value = [];
}

// 处理AI流式token
function handleAiStreamToken(data: any) {
  const messageId = data.messageId;
  const token = data.content;
  
  if (!messageId || !token) return;
  
  // 累积token
  const currentContent = streamingMessages.value.get(messageId) || '';
  const newContent = currentContent + token;
  streamingMessages.value.set(messageId, newContent);
  
  // 查找或创建消息
  let existingMsg = messages.value.find(m => m.id === messageId);
  if (!existingMsg) {
    // 创建新的流式消息
    existingMsg = {
      id: messageId,
      conversationId: data.conversationId,
      content: newContent,
      senderType: 1, // AI消息
      senderId: 'ai',
      senderName: '智能客服',
      createTime: new Date().toISOString(),
      isStreaming: true, // 标记为流式消息
    };
    messages.value.push(existingMsg);
  } else {
    // 更新现有消息内容
    existingMsg.content = newContent;
  }
  
  scrollToBottom();
}

// 处理AI流式消息完成
function handleAiStreamComplete(data: any) {
  const messageId = data.messageId;
  const fullContent = data.content;
  
  // 清除流式缓存
  streamingMessages.value.delete(messageId);
  
  // 更新消息为最终内容
  const existingMsg = messages.value.find(m => m.id === messageId);
  if (existingMsg) {
    existingMsg.content = fullContent;
    existingMsg.isStreaming = false;
  }
  
  // 解除AI回复中状态
  stopAiResponding();
  agentTyping.value = false;
  
  scrollToBottom();
}

// 滚动到底部
function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight;
    }
  });
}

// 格式化时间
function formatTime(time: string | Date) {
  if (!time) return '';
  const date = new Date(time);
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
}

// 判断是否是用户消息
// senderType: 0-用户, 1-AI, 2-客服, 3-系统
function isUserMessage(msg: any): boolean {
  // 用户消息 senderType === 0
  // 或者 senderId 等于当前用户ID
  if (msg.senderType === 0) return true;
  if (msg.senderId === userId.value) return true;
  return false;
}

// 获取消息样式类
function getMessageClass(msg: any) {
  if (msg.senderType === 3) return 'is-system';
  if (isUserMessage(msg)) return 'is-user';
  return msg.senderType === 1 ? 'is-ai' : 'is-agent';
}

// 渲染消息内容（简单HTML转换）
function renderMessage(content: string) {
  if (!content) return '';
  // 简单的换行转换
  return content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>');
}

// 监听消息变化，自动滚动
watch(messages, () => {
  scrollToBottom();
}, { deep: true });
</script>

<style lang="less" scoped>
.user-chat-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  max-width: 800px;
  margin: 0 auto;
  background: #f5f5f5;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);

  .header-info {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .app-avatar {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    border: 2px solid rgba(255, 255, 255, 0.3);
    object-fit: cover;
  }

  .app-info {
    display: flex;
    flex-direction: column;
  }

  .app-name {
    font-size: 18px;
    font-weight: 600;
  }

  .status-text {
    font-size: 12px;
    opacity: 0.9;
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    
    &.online {
      background: #52c41a;
      box-shadow: 0 0 4px #52c41a;
    }
    
    &.offline {
      background: #faad14;
    }
  }

  .header-actions {
    font-size: 18px;
    cursor: pointer;
    opacity: 0.8;
    
    &:hover {
      opacity: 1;
    }
  }
}

.chat-messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: #fff;

  .loading-wrapper, .empty-messages {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: #999;
    
    p {
      margin-top: 16px;
      font-size: 14px;
    }
  }
}

.message-item {
  margin-bottom: 16px;

  &.is-system {
    display: flex;
    justify-content: center;
  }
}

.system-message {
  padding: 6px 16px;
  background: #f0f0f0;
  border-radius: 16px;
  font-size: 12px;
  color: #666;
}

.user-message {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 12px;

  .message-content {
    max-width: 70%;
  }

  .message-text {
    padding: 12px 16px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    border-radius: 20px 20px 4px 20px;
    font-size: 14px;
    line-height: 1.6;
    word-break: break-word;
  }

  .message-time {
    text-align: right;
    font-size: 11px;
    color: #999;
    margin-top: 4px;
  }

  .avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    object-fit: cover;
  }
}

.agent-message {
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 12px;

  .avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    object-fit: cover;
  }

  .message-content {
    max-width: 70%;
  }

  .sender-info {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 4px;
  }

  .sender-name {
    font-size: 12px;
    color: #666;
  }

  .message-text {
    padding: 12px 16px;
    background: #f5f5f5;
    border-radius: 20px 20px 20px 4px;
    font-size: 14px;
    line-height: 1.6;
    word-break: break-word;

    :deep(p) {
      margin: 0 0 8px;
      &:last-child {
        margin-bottom: 0;
      }
    }

    :deep(pre) {
      background: #f0f0f0;
      padding: 8px 12px;
      border-radius: 8px;
      overflow-x: auto;
    }

    :deep(code) {
      background: #e8e8e8;
      padding: 2px 6px;
      border-radius: 4px;
      font-size: 13px;
    }

    :deep(ul), :deep(ol) {
      padding-left: 20px;
      margin: 8px 0;
    }
  }

  .message-time {
    font-size: 11px;
    color: #999;
    margin-top: 4px;
  }
}

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;

  .avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
  }

  .typing-dots {
    display: flex;
    gap: 4px;
    padding: 12px 16px;
    background: #f5f5f5;
    border-radius: 20px;

    span {
      width: 8px;
      height: 8px;
      background: #999;
      border-radius: 50%;
      animation: typing 1.4s infinite ease-in-out;

      &:nth-child(1) { animation-delay: 0s; }
      &:nth-child(2) { animation-delay: 0.2s; }
      &:nth-child(3) { animation-delay: 0.4s; }
    }
  }
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.preset-questions {
  padding: 12px 20px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;

  .preset-title {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: #666;
    margin-bottom: 10px;

    .anticon {
      color: #faad14;
    }
  }

  .preset-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;

    :deep(.ant-btn) {
      background: #fff;
      border: 1px solid #e8e8e8;
      border-radius: 16px;
      font-size: 12px;
      color: #666;
      transition: all 0.3s;

      &:hover {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: #fff;
        border-color: transparent;
      }
    }
  }
}

.chat-input {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  padding: 16px 20px;
  background: #fff;
  border-top: 1px solid #f0f0f0;

  :deep(.ant-input) {
    flex: 1;
    border-radius: 20px;
    padding: 10px 16px;
    resize: none;
    border-color: #d9d9d9;
    
    &:focus {
      border-color: #667eea;
      box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
    }
  }

  :deep(.ant-btn) {
    border-radius: 20px;
    height: 40px;
    padding: 0 24px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;
    
    &:hover {
      background: linear-gradient(135deg, #5a6fd6 0%, #6a4190 100%);
    }
    
    &:disabled {
      background: #d9d9d9;
    }
  }
}

.chat-closed {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 24px 20px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
  
  span {
    color: #999;
    font-size: 14px;
  }
  
  :deep(.ant-btn) {
    border-radius: 20px;
    height: 40px;
    padding: 0 32px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;
  }
}
</style>
