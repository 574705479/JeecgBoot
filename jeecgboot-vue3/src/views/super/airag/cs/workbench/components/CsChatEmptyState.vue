<template>
  <div class="chat-empty">
    <MessageOutlined class="empty-icon large" />
    <p>选择一个会话开始聊天</p>
  </div>
</template>

<script setup lang="ts">
import { MessageOutlined } from '@ant-design/icons-vue';

defineOptions({ name: 'CsChatEmptyState' });
</script>

<style lang="less" scoped>
.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--cs-text-muted);
  background: var(--cs-bg-chat);
  height: 100%;

  :deep(.anticon) {
    font-size: 72px !important;
    background: linear-gradient(135deg, var(--cs-brand-start), var(--cs-brand-end));
    background-clip: text;
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    opacity: 0.3;
  }

  p {
    margin-top: 16px;
    font-size: 15px;
    color: var(--cs-text-muted);
    font-weight: 500;
  }

  .empty-icon {
    font-size: 48px;
    color: var(--cs-text-muted);
    &.large { font-size: 64px; }
  }
}
</style>
