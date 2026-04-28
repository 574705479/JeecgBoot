<template>
  <a-modal
    v-model:open="modalOpen"
    :title="modalTitle"
    width="400px"
    @ok="handleSave"
  >
    <a-textarea
      v-if="field === 'notes'"
      v-model:value="modalValue"
      :rows="4"
      placeholder="请输入备注内容"
    />
    <a-input
      v-else
      v-model:value="modalValue"
      :placeholder="'请输入' + modalTitle"
    />
  </a-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue';

defineOptions({ name: 'CsVisitorFieldEditModal' });

const props = defineProps<{
  open: boolean;
  value: string;
  field: string;
}>();

const emit = defineEmits<{
  (e: 'update:open', v: boolean): void;
  (e: 'update:value', v: string): void;
  (e: 'save'): void;
}>();

const FIELD_TITLES: Record<string, string> = {
  nickname: '备注昵称',
  realName: '真实姓名',
  phone: '手机号',
  notes: '备注',
};

const modalOpen = computed({
  get: () => props.open,
  set: (v: boolean) => emit('update:open', v),
});

const modalValue = computed({
  get: () => props.value,
  set: (v: string) => emit('update:value', v),
});

const modalTitle = computed(() => FIELD_TITLES[props.field] || '');

function handleSave() {
  emit('save');
}
</script>
