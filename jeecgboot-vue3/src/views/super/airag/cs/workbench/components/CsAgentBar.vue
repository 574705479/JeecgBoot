<template>
  <div class="agent-bar">
    <div class="agent-info">
      <a-avatar :size="32" class="self-agent-avatar" :src="agentAvatarUrl">
        {{ (ctx.agentName.value || '客').charAt(0) }}
      </a-avatar>
      <span class="agent-name">{{ ctx.agentName.value }}</span>
    </div>
    <div class="agent-actions">
      <a-switch
        v-model:checked="onlineModel"
        checked-children="在线"
        un-checked-children="隐身"
        @change="handleSwitchChange"
      />
      <a-tooltip title="设置">
        <SettingOutlined class="setting-icon" @click="openSettings" />
      </a-tooltip>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { SettingOutlined } from '@ant-design/icons-vue';
import { useCsWorkbenchContext } from '../context';
import { withImageCache } from '../../utils/csImageCache';
import { getFileAccessHttpUrl } from '/@/utils/common/compUtils';

defineOptions({ name: 'CsAgentBar' });

const ctx = useCsWorkbenchContext();

const agentAvatarUrl = computed(() => {
  const v = ctx.agentAvatar.value;
  return v ? withImageCache(getFileAccessHttpUrl(v)) : '';
});

const onlineModel = computed({
  get: () => ctx.isOnline.value,
  set: (v: boolean) => { ctx.isOnline.value = v; },
});

function handleSwitchChange(checked: boolean | string | number) {
  ctx.toggleOnline(Boolean(checked));
}

function openSettings() {
  ctx.showSettingsDrawer.value = true;
}
</script>

<style lang="less" scoped>
@ease-smooth: cubic-bezier(0.4, 0, 0.2, 1);

.agent-bar {
  padding: 14px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, var(--cs-bar-start), var(--cs-bar-end));
  border-bottom: 1px solid var(--cs-border);

  .agent-info {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .self-agent-avatar {
    flex-shrink: 0;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  }

  .agent-name {
    font-weight: 600;
    color: var(--cs-brand-text);
    font-size: 14px;
  }

  .agent-actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  :deep(.ant-switch) {
    background: #ff4d4f;
    &.ant-switch-checked {
      background: #52c41a;
    }
  }

  .setting-icon {
    font-size: 18px;
    color: var(--cs-brand-text);
    cursor: pointer;
    transition: opacity 0.2s @ease-smooth;
    &:hover {
      opacity: 0.7;
    }
  }
}
</style>
