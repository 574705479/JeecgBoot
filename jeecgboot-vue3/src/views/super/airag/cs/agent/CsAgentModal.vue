<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    :title="isUpdate ? '编辑客服' : '新增客服'"
    @ok="handleSubmit"
    width="600px"
  >
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue';
import { BasicModal, useModalInner } from '/@/components/Modal';
import { BasicForm, useForm } from '/@/components/Form';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';

const emit = defineEmits(['success', 'register']);
const { createMessage } = useMessage();

const isUpdate = ref(false);
const recordId = ref('');

const [registerForm, { setFieldsValue, resetFields, validate }] = useForm({
  labelWidth: 100,
  schemas: [
    { field: 'userId', label: '关联用户', component: 'JSelectUser', required: true },
    { field: 'nickname', label: '客服昵称', component: 'Input', required: true },
    { field: 'avatar', label: '头像', component: 'JImageUpload' },
    { field: 'maxSessions', label: '最大接待数', component: 'InputNumber', defaultValue: 5,
      componentProps: { min: 1, max: 20 }
    },
    { field: 'role', label: '角色', component: 'Select', defaultValue: 0,
      componentProps: {
        options: [
          { label: '普通客服', value: 0 },
          { label: '管理者', value: 1 },
        ],
        placeholder: '请选择角色'
      },
      helpMessage: '管理者可监控所有会话'
    },
    { field: 'welcomeMessage', label: '欢迎语', component: 'InputTextArea',
      componentProps: { rows: 3, placeholder: '用户接入时发送的欢迎语' }
    },
  ],
  showActionButtonGroup: false,
});

const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
  resetFields();
  isUpdate.value = !!data?.isUpdate;
  
  if (data?.record) {
    recordId.value = data.record.id;
    setFieldsValue(data.record);
  }
});

async function handleSubmit() {
  try {
    const values = await validate();
    setModalProps({ confirmLoading: true });
    
    if (isUpdate.value) {
      await defHttp.put({ url: '/cs/agent/edit', data: { ...values, id: recordId.value } });
    } else {
      await defHttp.post({ url: '/cs/agent/add', data: values });
    }
    
    createMessage.success(isUpdate.value ? '编辑成功' : '新增成功');
    closeModal();
    emit('success');
  } finally {
    setModalProps({ confirmLoading: false });
  }
}
</script>
