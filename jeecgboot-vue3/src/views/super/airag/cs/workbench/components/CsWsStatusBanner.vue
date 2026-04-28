<template>
  <transition name="ws-banner">
    <div v-if="ctx.wsShowBanner.value" class="ws-status-banner" :class="'ws-' + ctx.wsStatus.value">
      <template v-if="ctx.wsStatus.value === 'connecting'">
        <LoadingOutlined spin /> 正在连接服务器...
      </template>
      <template v-else-if="ctx.wsStatus.value === 'reconnecting'">
        <LoadingOutlined spin /> 连接断开，正在重连...
      </template>
      <template v-else-if="ctx.wsStatus.value === 'disconnected'">
        <ExclamationCircleOutlined />
        <template v-if="ctx.wsReconnectCountdown.value > 0"> {{ ctx.wsReconnectCountdown.value }}秒后自动重连</template>
        <template v-else> 连接已断开</template>
        <a @click="reconnect" style="margin-left:8px">立即重连</a>
      </template>
      <template v-else-if="ctx.wsStatus.value === 'connected'">
        <CheckCircleOutlined /> 已重新连接
      </template>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { LoadingOutlined, ExclamationCircleOutlined, CheckCircleOutlined } from '@ant-design/icons-vue';
import { useCsWorkbenchContext } from '../context';

defineOptions({ name: 'CsWsStatusBanner' });

const ctx = useCsWorkbenchContext();

function reconnect() {
  ctx.connectWebSocket();
}
</script>

<style lang="less" scoped>
.ws-status-banner {
  padding: 4px 12px;
  font-size: 12px;
  text-align: center;
  overflow: hidden;
}
.ws-banner-enter-active, .ws-banner-leave-active {
  transition: all 0.3s ease;
}
.ws-banner-enter-from, .ws-banner-leave-to {
  opacity: 0;
  max-height: 0;
  padding: 0 12px;
}
.ws-connecting { background: #fffbe6; color: #ad8b00; }
.ws-reconnecting { background: #fff7e6; color: #d46b08; }
.ws-disconnected { background: #fff2f0; color: #cf1322; }
.ws-connected { background: #f6ffed; color: #389e0d; }
</style>
