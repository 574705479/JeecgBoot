<template>
  <div class="chat-header" v-if="conversation">
    <div class="chat-user">
      <a-avatar :size="40" class="visitor-avatar">{{ ctx.getDisplayName(conversation).charAt(0) }}</a-avatar>
      <div class="user-info">
        <div class="user-name">
          {{ ctx.getDisplayName(conversation) }}
          <StarFilled v-if="ctx.visitorInfo.value.star === 1" class="star-icon" />
          <a-tag v-if="ctx.visitorInfo.value.level === 3" color="gold" size="small">VIP</a-tag>
          <a-tag v-else-if="ctx.visitorInfo.value.level === 2" color="blue" size="small">重要</a-tag>
        </div>
        <div class="user-status">
          <a-badge :status="ctx.userOnline.value ? 'success' : 'default'" :text="ctx.userOnline.value ? '在线' : '离线'" />
          <span class="status-divider">|</span>
          <span class="status-text">{{ ctx.getModeName(ctx.currentReplyMode.value) }}模式</span>
          <span v-if="conversation.status === 1 && conversation.ownerAgentName" class="status-divider">|</span>
          <span v-if="conversation.status === 1 && conversation.ownerAgentName" class="status-text">
            首次接入: {{ conversation.ownerAgentName }}
          </span>
        </div>
        <div v-if="ctx.parsedCustomFields.value.length" class="custom-fields-header">
          <a-tag
            v-for="cf in ctx.parsedCustomFields.value"
            :key="cf.label"
            color="red"
            size="small"
            style="color: #cf1322; background: #fff1f0; border-color: #ffa39e;"
          >
            {{ cf.label }}: {{ cf.value }}
          </a-tag>
        </div>
      </div>
    </div>
    <div class="chat-tools">
      <a-select
        v-model:value="replyModeModel"
        size="small"
        style="width: 100px"
        @change="handleModeChange"
        v-if="conversation.status === 1"
      >
        <a-select-option :value="0">AI自动</a-select-option>
        <a-select-option :value="1">手动</a-select-option>
      </a-select>
      <a-tooltip title="推送满意度评价">
        <a-button
          size="small"
          @click="ctx.pushSatisfaction()"
          v-if="conversation.status !== 2 && !ctx.isColleagueReadonly.value"
          :loading="ctx.satisfactionPushing.value"
          :disabled="ctx.satisfactionPushed.value"
        >
          <SmileOutlined /> {{ ctx.satisfactionPushed.value ? '已推送' : '评价' }}
        </a-button>
      </a-tooltip>
      <a-button size="small" @click="ctx.openTransferModal()" v-if="conversation.status !== 2 && !ctx.isColleagueReadonly.value">
        <SwapOutlined /> 转接
      </a-button>
      <a-button size="small" danger @click="ctx.closeConversation()" v-if="conversation.status !== 2 && !ctx.isColleagueReadonly.value">
        结束
      </a-button>
      <a-button size="small" type="text" @click="toggleDetailPanel">
        <MenuUnfoldOutlined v-if="!ctx.showDetailPanel.value" />
        <MenuFoldOutlined v-else />
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import {
  StarFilled, SwapOutlined, SmileOutlined,
  MenuUnfoldOutlined, MenuFoldOutlined,
} from '@ant-design/icons-vue';
import { useCsWorkbenchContext } from '../context';

defineOptions({ name: 'CsChatHeader' });

const ctx = useCsWorkbenchContext();

const conversation = computed(() => ctx.currentConversation.value);

const replyModeModel = computed({
  get: () => ctx.currentReplyMode.value,
  set: (v: number) => { ctx.currentReplyMode.value = v; },
});

function handleModeChange(value: any) {
  ctx.changeMode(Number(value));
}

function toggleDetailPanel() {
  ctx.showDetailPanel.value = !ctx.showDetailPanel.value;
}
</script>

<style lang="less" scoped>
.chat-header {
  padding: 14px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  background: var(--cs-bg-surface);
  z-index: 1;

  .chat-user {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .user-info {
    .user-name {
      font-size: 16px;
      font-weight: 600;
      display: flex;
      align-items: center;
      gap: 6px;
      color: #222;
      .star-icon { color: #faad14; }
    }

    .user-status {
      font-size: 12px;
      color: var(--cs-text-muted);
      margin-top: 2px;
      display: flex;
      align-items: center;
      gap: 8px;
      .status-divider { color: #e0e0e0; }
    }
    .custom-fields-header {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
      margin-top: 4px;
    }
  }

  .chat-tools {
    display: flex;
    align-items: center;
    gap: 6px;
  }
}
</style>
