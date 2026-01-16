<template>
  <div class="cs-conversation-page">
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #tableTitle>
        <a-space>
          <a-button danger :disabled="!selectedIds.length" @click="batchDelete">
            <DeleteOutlined /> 批量删除
          </a-button>
          <a-button @click="exportData">
            <ExportOutlined /> 导出
          </a-button>
        </a-space>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getActions(record)" :dropDownActions="getDropDownActions(record)" />
      </template>
      <template #status="{ record }">
        <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
      </template>
      <template #deleted="{ record }">
        <a-tag v-if="record.deleted === 1" color="red">已删除</a-tag>
        <span v-else>-</span>
      </template>
      <template #replyMode="{ record }">
        <a-tag :color="getModeColor(record.replyMode)">{{ getModeText(record.replyMode) }}</a-tag>
      </template>
    </BasicTable>

    <!-- 会话详情弹窗 -->
    <CsConversationDetailModal @register="registerDetailModal" />
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue';
import { BasicTable, useTable, TableAction } from '/@/components/Table';
import { useModal } from '/@/components/Modal';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';
import { DeleteOutlined, ExportOutlined } from '@ant-design/icons-vue';
import CsConversationDetailModal from './CsConversationDetailModal.vue';

const { createMessage, createConfirm } = useMessage();
const [registerDetailModal, { openModal: openDetailModal }] = useModal();

// 客服列表（用于筛选）
const agentOptions = ref<{ label: string; value: string }[]>([]);

// 加载客服列表
async function loadAgentList() {
  try {
    const res = await defHttp.get({ url: '/cs/agent/list', params: { pageSize: 1000 } });
    const records = res.records || res || [];
    agentOptions.value = records.map((agent: any) => ({
      label: agent.nickname || agent.username || agent.id,
      value: agent.id,
    }));
  } catch (e) {
    console.error('加载客服列表失败', e);
  }
}

onMounted(() => {
  loadAgentList();
});

// 选中的行
const selectedRowKeys = ref<string[]>([]);
const selectedIds = computed(() => selectedRowKeys.value);

const rowSelection = {
  type: 'checkbox' as const,
  selectedRowKeys: selectedRowKeys,
  onChange: (keys: string[]) => {
    selectedRowKeys.value = keys;
  },
};

const columns = [
  { title: '访客', dataIndex: 'userName', width: 120, 
    customRender: ({ text, record }: any) => text || record.userId?.substring(0, 12) || '匿名访客' 
  },
  { title: '负责客服', dataIndex: 'ownerAgentName', width: 100,
    customRender: ({ text }: any) => text || '-'
  },
  { title: '状态', dataIndex: 'status', width: 90, slots: { customRender: 'status' } },
  { title: '删除状态', dataIndex: 'deleted', width: 90, slots: { customRender: 'deleted' } },
  { title: '回复模式', dataIndex: 'replyMode', width: 100, slots: { customRender: 'replyMode' } },
  { title: '来源', dataIndex: 'source', width: 80,
    customRender: ({ text }: any) => text || '直接访问'
  },
  { title: '消息数', dataIndex: 'messageCount', width: 80, align: 'center' as const },
  { title: '满意度', dataIndex: 'satisfaction', width: 80, align: 'center' as const,
    customRender: ({ text }: any) => text ? `${text}⭐` : '-' 
  },
  { title: '创建时间', dataIndex: 'createTime', width: 160, sorter: true },
  { title: '结束时间', dataIndex: 'endTime', width: 160,
    customRender: ({ text }: any) => text || '-'
  },
];

const [registerTable, { reload, getSelectRows }] = useTable({
  title: '会话记录',
  api: async (params) => {
    // 处理时间范围参数
    if (params.createTime && Array.isArray(params.createTime)) {
      params.createTimeBegin = params.createTime[0];
      params.createTimeEnd = params.createTime[1];
      delete params.createTime;
    }
    // ★ 会话记录默认使用 history 筛选模式，包含所有会话
    params.filter = 'history';
    // ★ 默认包含已删除的记录
    params.includeDeleted = true;
    const res = await defHttp.get({ url: '/cs/conversation/list', params });
    return res;
  },
  columns,
  formConfig: {
    schemas: [
      { 
        field: 'userName', 
        label: '访客', 
        component: 'Input', 
        colProps: { span: 5 },
        componentProps: { placeholder: '访客名称/ID' }
      },
      { 
        field: 'filterAgentId', 
        label: '负责客服', 
        component: 'Select', 
        colProps: { span: 5 },
        componentProps: {
          options: agentOptions,
          placeholder: '选择客服',
          allowClear: true,
          showSearch: true,
          filterOption: (input: string, option: any) => {
            return option.label?.toLowerCase().includes(input.toLowerCase());
          },
        }
      },
      { 
        field: 'status', 
        label: '状态', 
        component: 'Select', 
        colProps: { span: 4 },
        componentProps: {
          options: [
            { label: '全部', value: '' },
            { label: '待接入', value: 0 },
            { label: '服务中', value: 1 },
            { label: '已结束', value: 2 },
          ],
          allowClear: true,
        }
      },
      { 
        field: 'replyMode', 
        label: '回复模式', 
        component: 'Select', 
        colProps: { span: 4 },
        componentProps: {
          options: [
            { label: '全部', value: '' },
            { label: 'AI自动', value: 0 },
            { label: '手动', value: 1 },
            { label: 'AI辅助', value: 2 },
          ],
          allowClear: true,
        }
      },
      { 
        field: 'createTime', 
        label: '创建时间', 
        component: 'RangePicker', 
        colProps: { span: 6 },
        componentProps: {
          format: 'YYYY-MM-DD',
          valueFormat: 'YYYY-MM-DD',
        }
      },
    ],
  },
  useSearchForm: true,
  showTableSetting: true,
  bordered: true,
  showIndexColumn: false,
  rowKey: 'id',
  actionColumn: {
    width: 140,
    title: '操作',
    dataIndex: 'action',
    fixed: 'right' as const,
    slots: { customRender: 'action' },
  },
});

function getStatusColor(status: number) {
  switch (status) {
    case 0: return 'blue';
    case 1: return 'green';
    case 2: return 'default';
    default: return 'default';
  }
}

function getStatusText(status: number) {
  switch (status) {
    case 0: return '待接入';
    case 1: return '服务中';
    case 2: return '已结束';
    default: return '未知';
  }
}

function getModeColor(mode: number) {
  switch (mode) {
    case 0: return 'purple';
    case 1: return 'orange';
    case 2: return 'cyan';
    default: return 'default';
  }
}

function getModeText(mode: number) {
  switch (mode) {
    case 0: return 'AI自动';
    case 1: return '手动';
    case 2: return 'AI辅助';
    default: return '未知';
  }
}

function getActions(record: any) {
  return [
    { label: '查看详情', onClick: () => openDetailModal(true, { record }) },
  ];
}

function getDropDownActions(record: any) {
  const actions: any[] = [];
  
  // 只有已结束的会话才能删除
  if (record.status === 2) {
    actions.push({
      label: '删除',
      color: 'error',
      popConfirm: {
        title: '确定删除该会话记录吗？',
        confirm: () => handleDelete(record.id),
      },
    });
  }
  
  return actions;
}

// 删除单个会话
async function handleDelete(id: string) {
  try {
    await defHttp.delete({ url: `/cs/conversation/${id}` });
    createMessage.success('删除成功');
    reload();
  } catch (e) {
    createMessage.error('删除失败');
  }
}

// 批量删除
async function batchDelete() {
  if (!selectedIds.value.length) {
    createMessage.warning('请选择要删除的会话');
    return;
  }
  
  // 检查是否都是已结束的会话
  const rows = getSelectRows();
  const hasActive = rows.some((r: any) => r.status !== 2);
  if (hasActive) {
    createMessage.warning('只能删除已结束的会话');
    return;
  }
  
  createConfirm({
    iconType: 'warning',
    title: '确认删除',
    content: `确定删除选中的 ${selectedIds.value.length} 条会话记录吗？`,
    onOk: async () => {
      try {
        // 逐个删除
        for (const id of selectedIds.value) {
          await defHttp.delete({ url: `/cs/conversation/${id}` });
        }
        createMessage.success('批量删除成功');
        selectedRowKeys.value = [];
        reload();
      } catch (e) {
        createMessage.error('删除失败');
      }
    },
  });
}

// 导出数据
async function exportData() {
  createMessage.info('导出功能开发中...');
}
</script>

<style lang="less" scoped>
.cs-conversation-page {
  padding: 16px;
}
</style>
