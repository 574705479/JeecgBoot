<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    title="会话详情"
    :footer="null"
    width="800px"
  >
    <div class="conversation-detail">
      <a-descriptions :column="2" bordered size="small">
        <a-descriptions-item label="会话ID">{{ record?.id }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getStatusColor(record?.status)">{{ getStatusText(record?.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="用户">{{ record?.externalUserName || record?.userId || '匿名' }}</a-descriptions-item>
        <a-descriptions-item label="客服">{{ record?.agentId || '-' }}</a-descriptions-item>
        <a-descriptions-item label="来源">{{ record?.source || '-' }}</a-descriptions-item>
        <a-descriptions-item label="满意度">{{ record?.satisfaction ? `${record.satisfaction}星` : '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ record?.createTime }}</a-descriptions-item>
        <a-descriptions-item label="结束时间">{{ record?.endTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="用户消息数">{{ record?.userMessageCount || 0 }}</a-descriptions-item>
        <a-descriptions-item label="AI消息数">{{ record?.aiMessageCount || 0 }}</a-descriptions-item>
        <a-descriptions-item label="客服消息数">{{ record?.agentMessageCount || 0 }}</a-descriptions-item>
        <a-descriptions-item label="转人工原因">{{ record?.transferReason || '-' }}</a-descriptions-item>
      </a-descriptions>

      <a-divider>消息记录</a-divider>
      
      <div class="message-list">
        <div v-for="msg in messages" :key="msg.id" :class="['message-item', getMsgClass(msg)]">
          <div class="message-sender">{{ msg.senderName }} ({{ getSenderType(msg.senderType) }})</div>
          <div class="message-content">{{ msg.content }}</div>
          <div class="message-time">{{ msg.createTime }}</div>
        </div>
        <a-empty v-if="messages.length === 0" description="暂无消息" />
      </div>
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import { BasicModal, useModalInner } from '/@/components/Modal';
import { defHttp } from '/@/utils/http/axios';

const record = ref<any>(null);
const messages = ref<any[]>([]);

const [registerModal] = useModalInner(async (data) => {
  record.value = data?.record;
  if (record.value?.id) {
    await loadMessages(record.value.id);
  }
});

async function loadMessages(conversationId: string) {
  try {
    const res = await defHttp.get({ url: `/cs/conversation/${conversationId}/messages`, params: { limit: 200 } });
    messages.value = res || [];
  } catch (e) {
    console.error('加载消息失败', e);
    messages.value = [];
  }
}

function getStatusColor(status: number) {
  switch (status) {
    case 0: return 'blue';
    case 1: return 'orange';
    case 2: return 'green';
    case 3: return 'default';
    default: return 'default';
  }
}

function getStatusText(status: number) {
  switch (status) {
    case 0: return 'AI接待';
    case 1: return '排队中';
    case 2: return '人工接待';
    case 3: return '已结束';
    default: return '未知';
  }
}

function getSenderType(type: number) {
  switch (type) {
    case 0: return '用户';
    case 1: return 'AI';
    case 2: return '客服';
    case 3: return '系统';
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
</script>

<style lang="less" scoped>
.conversation-detail {
  .message-list {
    max-height: 400px;
    overflow-y: auto;
    padding: 12px;
    background: #f5f5f5;
    border-radius: 8px;
  }

  .message-item {
    padding: 8px 12px;
    margin-bottom: 8px;
    background: #fff;
    border-radius: 8px;
    
    &.msg-user {
      border-left: 3px solid #1890ff;
    }
    &.msg-ai {
      border-left: 3px solid #52c41a;
    }
    &.msg-agent {
      border-left: 3px solid #faad14;
    }
    &.msg-system {
      border-left: 3px solid #999;
      background: #fafafa;
    }
  }

  .message-sender {
    font-size: 12px;
    color: #666;
    margin-bottom: 4px;
  }

  .message-content {
    font-size: 14px;
    word-break: break-word;
  }

  .message-time {
    font-size: 11px;
    color: #999;
    margin-top: 4px;
    text-align: right;
  }
}
</style>
