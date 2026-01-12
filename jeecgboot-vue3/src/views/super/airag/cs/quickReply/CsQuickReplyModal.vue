<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    :title="isUpdate ? '编辑快捷回复' : '新增快捷回复'"
    @ok="handleSubmit"
    width="600px"
  >
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
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
    { field: 'title', label: '标题/关键词', component: 'Input', required: true,
      componentProps: { placeholder: '用于搜索的关键词' }
    },
    { field: 'content', label: '回复内容', component: 'InputTextArea', required: true,
      componentProps: { rows: 4, placeholder: '快捷回复的内容' }
    },
    { field: 'msgType', label: '消息类型', component: 'Select', defaultValue: 0,
      componentProps: {
        options: [
          { label: '文本', value: 0 },
          { label: '图片', value: 1 },
          { label: '文件', value: 2 },
          { label: '富文本', value: 5 },
        ]
      }
    },
    { field: 'categoryId', label: '分类', component: 'Select',
      componentProps: { placeholder: '选择分类(可选)' }
    },
    { field: 'sort', label: '排序', component: 'InputNumber', defaultValue: 0 },
    { field: 'status', label: '状态', component: 'RadioGroup', defaultValue: 1,
      componentProps: {
        options: [
          { label: '启用', value: 1 },
          { label: '禁用', value: 0 },
        ]
      }
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
      await defHttp.put({ url: '/cs/quickReply/edit', data: { ...values, id: recordId.value } });
    } else {
      await defHttp.post({ url: '/cs/quickReply/add', data: values });
    }
    
    createMessage.success(isUpdate.value ? '编辑成功' : '新增成功');
    closeModal();
    emit('success');
  } finally {
    setModalProps({ confirmLoading: false });
  }
}
</script>
