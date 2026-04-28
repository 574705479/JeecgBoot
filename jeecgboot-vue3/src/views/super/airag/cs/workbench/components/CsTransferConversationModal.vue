<template>
  <a-modal
    v-model:open="modalOpen"
    title="转接会话"
    width="480px"
    :footer="null"
  >
    <div class="transfer-content">
      <div v-if="loading" class="transfer-loading">
        <a-spin />
        <span>加载客服列表...</span>
      </div>
      <div v-else-if="agents.length === 0" class="transfer-empty">
        <InboxOutlined class="empty-icon" />
        <p>暂无其他在线客服</p>
      </div>
      <div v-else class="agent-list">
        <div
          v-for="agent in agents"
          :key="agent.id"
          class="agent-card"
          @click="handleTransfer(agent.id)"
        >
          <a-avatar :size="48" :src="getAgentItemAvatarUrl(agent)" class="agent-avatar">
            {{ agent.nickname?.charAt(0) || '客' }}
          </a-avatar>
          <div class="agent-info">
            <div class="agent-name">{{ agent.nickname || '客服' }}</div>
            <div class="agent-stats">
              <span>
                <a-badge :status="agent.status === 1 ? 'success' : agent.status === 2 ? 'warning' : 'default'" />
                {{ getAgentStatusText(agent.status) }}
              </span>
              <span>当前接待: {{ agent.currentSessions || 0 }}/{{ agent.maxSessions || 10 }}</span>
            </div>
          </div>
          <a-button type="primary" size="small" class="transfer-btn">
            转接
          </a-button>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { InboxOutlined } from '@ant-design/icons-vue';
import { withImageCache } from '/@/utils/file/imageCache';
import { getFileAccessHttpUrl } from '/@/utils/common/compUtils';

defineOptions({ name: 'CsTransferConversationModal' });

const props = defineProps<{
  open: boolean;
  loading: boolean;
  agents: any[];
}>();

const emit = defineEmits<{
  (e: 'update:open', v: boolean): void;
  (e: 'transfer', toAgentId: string): void;
}>();

const modalOpen = computed({
  get: () => props.open,
  set: (v: boolean) => emit('update:open', v),
});

function handleTransfer(toAgentId: string) {
  emit('transfer', toAgentId);
}

function getAgentItemAvatarUrl(agent: any): string {
  const avatar = agent?.avatar;
  return avatar ? withImageCache(getFileAccessHttpUrl(avatar)) : '';
}

function getAgentStatusText(status: number): string {
  switch (status) {
    case 1: return '在线';
    case 2: return '忙碌';
    case 3: return '隐身';
    default: return '离线';
  }
}
</script>

<style lang="less" scoped>
@ease-smooth: cubic-bezier(0.4, 0, 0.2, 1);

.transfer-content {
  min-height: 200px;
  max-height: 400px;
  overflow-y: auto;
}

.transfer-loading, .transfer-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: var(--cs-text-muted);
  gap: 12px;
}

.empty-icon {
  font-size: 48px;
  color: var(--cs-text-muted);
}

.agent-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.agent-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--cs-bg-card);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s @ease-smooth;
  border: 1px solid transparent;

  &:hover {
    background: rgba(var(--cs-brand-rgb), 0.05);
    border-color: var(--cs-brand-start);
    box-shadow: 0 4px 12px rgba(var(--cs-brand-rgb), 0.1);
    .transfer-btn { opacity: 1; }
  }

  .agent-avatar {
    background: linear-gradient(135deg, var(--cs-brand-start), var(--cs-brand-end));
    color: #fff;
    flex-shrink: 0;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  }

  .agent-info {
    flex: 1;
    min-width: 0;
    .agent-name {
      font-size: 15px;
      font-weight: 500;
      color: var(--cs-text-primary);
      margin-bottom: 4px;
    }
    .agent-stats {
      display: flex;
      gap: 16px;
      font-size: 12px;
      color: var(--cs-text-muted);
    }
  }

  .transfer-btn {
    opacity: 0;
    transition: opacity 0.2s @ease-smooth;
  }
}
</style>
