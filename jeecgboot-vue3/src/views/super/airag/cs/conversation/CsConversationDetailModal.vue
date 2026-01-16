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
          <div class="message-list" ref="messageListRef">
            <template v-if="messages.length > 0">
              <div v-for="msg in messages" :key="msg.id" :class="['message-item', getMsgClass(msg)]">
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
  </BasicModal>
</template>

<script lang="ts" setup>
import { ref, nextTick } from 'vue';
import { BasicModal, useModalInner } from '/@/components/Modal';
import { defHttp } from '/@/utils/http/axios';
import { MessageOutlined } from '@ant-design/icons-vue';
import { Typography } from 'ant-design-vue';

const { Text: ATypographyText } = Typography;

const record = ref<any>(null);
const messages = ref<any[]>([]);
const loading = ref(false);
const messageListRef = ref<HTMLElement | null>(null);

const [registerModal] = useModalInner(async (data) => {
  record.value = data?.record;
  messages.value = [];
  
  if (record.value?.id) {
    await loadMessages(record.value.id);
  }
});

async function loadMessages(conversationId: string) {
  loading.value = true;
  try {
    const res = await defHttp.get({ 
      url: `/cs/message/${conversationId}`, 
      params: { limit: 500 } 
    });
    messages.value = res || [];
    
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
}
</style>
