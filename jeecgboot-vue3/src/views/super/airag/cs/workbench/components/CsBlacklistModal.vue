<template>
  <a-modal
    v-model:open="modalOpen"
    :title="type === 'user' ? '拉黑访客' : '拉黑IP'"
    :okButtonProps="{ disabled: !reason.trim() }"
    @ok="handleConfirm"
    @cancel="modalOpen = false"
  >
    <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
      <a-form-item v-if="type === 'ip'" label="IP地址">
        <a-input v-model:value="ipValue" placeholder="IP或IP段（如192.168.1.0/24）" />
      </a-form-item>
      <a-form-item v-if="type === 'user'" label="访客信息">
        <span>{{ visitorLabel || '-' }}</span>
      </a-form-item>
      <a-form-item label="拉黑原因" required>
        <a-textarea v-model:value="reason" :rows="3" placeholder="请输入拉黑原因（必填）" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';

defineOptions({ name: 'CsBlacklistModal' });

const props = defineProps<{
  open: boolean;
  type: 'user' | 'ip';
  defaultIp?: string;
  visitorLabel?: string;
}>();

const emit = defineEmits<{
  (e: 'update:open', v: boolean): void;
  (e: 'confirm', payload: { type: 'user' | 'ip'; reason: string; ip?: string }): void;
}>();

const modalOpen = computed({
  get: () => props.open,
  set: (v: boolean) => emit('update:open', v),
});

const reason = ref('');
const ipValue = ref('');

watch(() => props.open, (val) => {
  if (val) {
    reason.value = '';
    ipValue.value = props.defaultIp || '';
  }
});

function handleConfirm() {
  const payload: { type: 'user' | 'ip'; reason: string; ip?: string } = {
    type: props.type,
    reason: reason.value.trim(),
  };
  if (props.type === 'ip') {
    payload.ip = ipValue.value.trim();
  }
  emit('confirm', payload);
}
</script>
