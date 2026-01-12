<template>
  <div class="cs-conversation-page">
    <BasicTable @register="registerTable">
      <template #action="{ record }">
        <TableAction :actions="getActions(record)" />
      </template>
      <template #status="{ record }">
        <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
      </template>
    </BasicTable>

    <!-- 会话详情弹窗 -->
    <CsConversationDetailModal @register="registerDetailModal" />
  </div>
</template>

<script lang="ts" setup>
import { BasicTable, useTable, TableAction } from '/@/components/Table';
import { useModal } from '/@/components/Modal';
import { defHttp } from '/@/utils/http/axios';
import CsConversationDetailModal from './CsConversationDetailModal.vue';

const [registerDetailModal, { openModal: openDetailModal }] = useModal();

const columns = [
  { title: '会话ID', dataIndex: 'id', width: 180 },
  { title: '用户', dataIndex: 'externalUserName', width: 120, customRender: ({ text, record }) => text || record.userId || '匿名' },
  { title: '客服', dataIndex: 'agentId', width: 120 },
  { title: '状态', dataIndex: 'status', width: 100, slots: { customRender: 'status' } },
  { title: '来源', dataIndex: 'source', width: 100 },
  { title: '用户消息', dataIndex: 'userMessageCount', width: 100 },
  { title: 'AI消息', dataIndex: 'aiMessageCount', width: 100 },
  { title: '客服消息', dataIndex: 'agentMessageCount', width: 100 },
  { title: '满意度', dataIndex: 'satisfaction', width: 80, customRender: ({ text }) => text ? `${text}星` : '-' },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '结束时间', dataIndex: 'endTime', width: 160 },
];

const [registerTable, { reload }] = useTable({
  title: '会话记录',
  api: async (params) => {
    const res = await defHttp.get({ url: '/cs/conversation/list', params });
    return res;
  },
  columns,
  formConfig: {
    schemas: [
      { field: 'externalUserName', label: '用户名', component: 'Input', colProps: { span: 6 } },
      { field: 'status', label: '状态', component: 'Select', colProps: { span: 6 },
        componentProps: {
          options: [
            { label: 'AI接待', value: 0 },
            { label: '排队中', value: 1 },
            { label: '人工接待', value: 2 },
            { label: '已结束', value: 3 },
          ]
        }
      },
      { field: 'createTime', label: '创建时间', component: 'RangePicker', colProps: { span: 8 } },
    ],
  },
  useSearchForm: true,
  showTableSetting: true,
  bordered: true,
  actionColumn: {
    width: 120,
    title: '操作',
    dataIndex: 'action',
    slots: { customRender: 'action' },
  },
});

function getStatusColor(status: number) {
  switch (status) {
    case 0: return 'blue';
    case 1: return 'orange';
    case 2: return 'green';
    case 3: return 'default';
    default: return 'default';
  }
}

function getStatusText(status: number) {
  switch (status) {
    case 0: return 'AI接待';
    case 1: return '排队中';
    case 2: return '人工接待';
    case 3: return '已结束';
    default: return '未知';
  }
}

function getActions(record: any) {
  return [
    { label: '查看详情', onClick: () => openDetailModal(true, { record }) },
  ];
}
</script>

<style lang="less" scoped>
.cs-conversation-page {
  padding: 16px;
}
</style>
