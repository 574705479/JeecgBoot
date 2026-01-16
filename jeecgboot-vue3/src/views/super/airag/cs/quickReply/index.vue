<template>
  <div class="cs-quick-reply-page">
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #tableTitle>
        <a-button type="primary" preIcon="ant-design:plus-outlined" @click="handleAdd">新增快捷回复</a-button>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getActions(record)" />
      </template>
      <template #status="{ record }">
        <a-tag :color="record.status === 1 ? 'green' : 'default'">
          {{ record.status === 1 ? '启用' : '禁用' }}
        </a-tag>
      </template>
    </BasicTable>

    <!-- 编辑弹窗 -->
    <CsQuickReplyModal @register="registerModal" @success="reload" />
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import { BasicTable, useTable, TableAction } from '/@/components/Table';
import { useModal } from '/@/components/Modal';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';
import CsQuickReplyModal from './CsQuickReplyModal.vue';

const { createMessage } = useMessage();

const [registerModal, { openModal }] = useModal();

const columns = [
  { title: '标题/关键词', dataIndex: 'title', width: 150 },
  { title: '回复内容', dataIndex: 'content', width: 300, ellipsis: true },
  { title: '消息类型', dataIndex: 'msgType', width: 100, 
    customRender: ({ text }) => {
      const types = { 0: '文本', 1: '图片', 2: '文件', 5: '富文本' };
      return types[text] || '未知';
    }
  },
  { title: '所属客服', dataIndex: 'agentId', width: 120, customRender: ({ text }) => text || '公共' },
  { title: '使用次数', dataIndex: 'useCount', width: 80 },
  { title: '状态', dataIndex: 'status', width: 80, slots: { customRender: 'status' } },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
];

const [registerTable, { reload }] = useTable({
  title: '快捷回复管理',
  api: async (params) => {
    const res = await defHttp.get({ url: '/cs/quickReply/list', params });
    return res;
  },
  columns,
  formConfig: {
    schemas: [
      { field: 'title', label: '标题', component: 'Input', colProps: { span: 6 } },
      { field: 'content', label: '内容', component: 'Input', colProps: { span: 6 } },
    ],
  },
  useSearchForm: true,
  showTableSetting: true,
  bordered: true,
  actionColumn: {
    width: 150,
    title: '操作',
    dataIndex: 'action',
    slots: { customRender: 'action' },
  },
});

const rowSelection = ref({
  type: 'checkbox',
});

function getActions(record: any) {
  return [
    { label: '编辑', onClick: () => handleEdit(record) },
    { label: '删除', color: 'error', popConfirm: { title: '确定删除吗?', confirm: () => handleDelete(record) } },
  ];
}

function handleAdd() {
  openModal(true, { isUpdate: false });
}

function handleEdit(record: any) {
  openModal(true, { isUpdate: true, record });
}

async function handleDelete(record: any) {
  if (!record?.id) {
    createMessage.error('缺少快捷回复ID');
    return;
  }
  await defHttp.delete({ url: `/cs/quickReply/delete?id=${record.id}` });
  createMessage.success('删除成功');
  reload();
}
</script>

<style lang="less" scoped>
.cs-quick-reply-page {
  padding: 16px;
}
</style>
