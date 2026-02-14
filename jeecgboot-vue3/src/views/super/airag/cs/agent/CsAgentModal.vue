<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    :title="isUpdate ? '编辑管理员客服' : '新增管理员客服'"
    @ok="handleSubmit"
    width="600px"
  >
    <BasicForm @register="registerForm">
      <template #avatar="{ model, field }">
        <CropperAvatar
          :uploadApi="uploadImg"
          :value="getAvatarFullUrl(model[field])"
          @change="(src, data) => { model[field] = data }"
          width="100"
        />
      </template>
    </BasicForm>
  </BasicModal>
</template>

<script lang="ts" setup>
import { ref, computed, unref } from 'vue';
import { BasicModal, useModalInner } from '/@/components/Modal';
import { BasicForm, useForm } from '/@/components/Form';
import { CropperAvatar } from '/@/components/Cropper';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';
import { uploadImg } from '/@/api/sys/upload';
import { getFileAccessHttpUrl } from '/@/utils/common/compUtils';

const emit = defineEmits(['success', 'register']);
const { createMessage } = useMessage();

const isUpdate = ref(false);
const recordId = ref('');

function getAvatarFullUrl(path: string) {
  return path ? getFileAccessHttpUrl(path) : '';
}

const [registerForm, { setFieldsValue, resetFields, validate, updateSchema }] = useForm({
  labelWidth: 100,
  schemas: [
    {
      field: 'username',
      label: '登录用户名',
      component: 'Input',
      required: true,
      componentProps: { placeholder: '请输入登录用户名' },
      ifShow: () => !unref(isUpdate),
    },
    {
      field: 'password',
      label: '登录密码',
      component: 'InputPassword',
      required: true,
      componentProps: { placeholder: '请输入登录密码' },
      ifShow: () => !unref(isUpdate),
    },
    { field: 'nickname', label: '客服昵称', component: 'Input', required: true,
      componentProps: { placeholder: '请输入客服昵称' }
    },
    { field: 'avatar', label: '头像', component: 'Input', slot: 'avatar' },
    { field: 'maxSessions', label: '最大接待数', component: 'InputNumber', defaultValue: 10,
      componentProps: { min: 1, max: 50 }
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
    setFieldsValue({
      nickname: data.record.nickname,
      avatar: data.record.avatar,
      maxSessions: data.record.maxSessions,
      welcomeMessage: data.record.welcomeMessage,
    });
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
