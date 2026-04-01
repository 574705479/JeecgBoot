<template>
  <div class="cs-conversation-page">
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-space>
          <a-button @click="exportData">
            <ExportOutlined /> 导出
          </a-button>
        </a-space>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getActions(record)" />
      </template>
    </BasicTable>

    <!-- 会话详情弹窗 -->
    <CsConversationDetailModal @register="registerDetailModal" />
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicTable, useTable, TableAction } from '/@/components/Table';
import { useModal } from '/@/components/Modal';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';
import { ExportOutlined } from '@ant-design/icons-vue';
import CsConversationDetailModal from './CsConversationDetailModal.vue';

const { createMessage } = useMessage();
const [registerDetailModal, { openModal: openDetailModal }] = useModal();

// 客服列表（用于筛选）
const agentOptions = ref<{ label: string; value: string }[]>([]);

// 聊天窗口配置（用于判断展示位置开关）
const chatWindowSettings = ref<any>({});
async function loadChatWindowSettings() {
  try {
    const res = await defHttp.get({ url: '/cs/agent/global/chat-window-settings' });
    let parsed: any = {};
    if (typeof res === 'string') { try { parsed = JSON.parse(res); } catch {} }
    else if (res && typeof res === 'object') { parsed = res; }
    chatWindowSettings.value = parsed;
  } catch {}
}

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
  loadChatWindowSettings();
});

const columns = [
  { title: '会话ID', dataIndex: 'id', width: 220 },
  { title: '客服', dataIndex: 'ownerAgentName', width: 100,
    customRender: ({ text }: any) => text || '-'
  },
  { title: '接入信息', dataIndex: 'customFields', width: 220,
    customRender: ({ text }: any) => {
      if (!text) return '-';
      try {
        const fields = typeof text === 'string' ? JSON.parse(text) : text;
        if (typeof fields === 'object' && fields !== null) {
          const fieldDefs = chatWindowSettings.value?.humanAgentFields;
          const entries = Object.entries(fields).filter(([k]) => {
            if (!Array.isArray(fieldDefs) || !fieldDefs.length) return true;
            const def = fieldDefs.find((d: any) => d.label === k);
            return !def || def.showInHistory !== false;
          });
          if (!entries.length) return '-';
          const tags = entries.map(([k, v]) =>
            h(Tag, { color: 'red', size: 'small', style: 'margin: 2px' }, () => `${k}: ${v}`)
          );
          return h('div', { style: 'display: flex; flex-wrap: wrap; gap: 2px;' }, tags);
        }
      } catch {}
      return '-';
    }
  },
  { title: '访客昵称', dataIndex: 'visitorNickname', width: 120,
    customRender: ({ text, record }: any) => text || record.userName || '-'
  },
  { title: '客服消息数', dataIndex: 'agentMessageCount', width: 90, align: 'center' as const,
    customRender: ({ text }: any) => text ?? 0
  },
  { title: '访客消息数', dataIndex: 'visitorMessageCount', width: 90, align: 'center' as const,
    customRender: ({ text }: any) => text ?? 0
  },
  { title: '对话时长', dataIndex: 'createTime', width: 100,
    customRender: ({ record }: any) => {
      if (!record.endTime || !record.createTime) return '-';
      const ms = new Date(record.endTime).getTime() - new Date(record.createTime).getTime();
      if (ms < 0) return '-';
      const totalSeconds = Math.floor(ms / 1000);
      const hours = Math.floor(totalSeconds / 3600);
      const minutes = Math.floor((totalSeconds % 3600) / 60);
      const seconds = totalSeconds % 60;
      if (hours > 0) return `${hours}时${minutes}分`;
      if (minutes > 0) return `${minutes}分${seconds}秒`;
      return `${seconds}秒`;
    }
  },
  { title: '开始时间', dataIndex: 'createTime', width: 160, sorter: true,
    key: 'startTime',
  },
  { title: '结束时间', dataIndex: 'endTime', width: 160,
    customRender: ({ text }: any) => text || '-'
  },
  { title: '访客ID', dataIndex: 'userId', width: 220 },
  { title: '系统环境', width: 120,
    customRender: ({ record }: any) => {
      const os = record.userOs || '';
      const browser = record.userBrowser || '';
      return os && browser ? `${os}/${browser}` : os || browser || '-';
    }
  },
  { title: '结束方式', dataIndex: 'endType', width: 90,
    customRender: ({ text }: any) => {
      const map: Record<number, string> = { 0: '客服结束', 1: '超时结束', 2: '访客结束', 3: '系统清理' };
      return text != null ? (map[text] || '-') : '-';
    }
  },
  { title: '首次响应', dataIndex: 'firstResponseSeconds', width: 100,
    customRender: ({ text }: any) => {
      if (text == null) return '-';
      if (text < 60) return `${text}秒`;
      return `${Math.floor(text / 60)}分${text % 60}秒`;
    }
  },
  { title: '对话评价', dataIndex: 'satisfaction', width: 90, align: 'center' as const,
    customRender: ({ text }: any) => text ? `${text}⭐` : '-'
  },
  { title: '着陆页', dataIndex: 'landingPage', width: 250,
    customRender: ({ text }: any) => text || '-'
  },
  { title: '来源页', dataIndex: 'referrerPage', width: 250,
    customRender: ({ text }: any) => text || '-'
  },
];

const [registerTable, { reload }] = useTable({
  title: '会话记录',
  api: async (params) => {
    // 处理时间范围参数
    if (params.createTime && Array.isArray(params.createTime)) {
      params.createTimeBegin = params.createTime[0];
      params.createTimeEnd = params.createTime[1];
      delete params.createTime;
    }
    if (params.endTime && Array.isArray(params.endTime)) {
      params.endTimeBegin = params.endTime[0];
      params.endTimeEnd = params.endTime[1];
      delete params.endTime;
    }
    // 清理空字符串参数
    Object.keys(params).forEach((key) => {
      if (params[key] === '' || params[key] === undefined) {
        delete params[key];
      }
    });
    // ★ 会话记录默认使用 history 筛选模式，包含所有会话
    params.filter = 'history';
    // ★ 默认包含已删除的记录
    params.includeDeleted = true;
    const res = await defHttp.get({ url: '/cs/conversation/list', params });
    return res;
  },
  columns,
  formConfig: {
    labelWidth: 80,
    schemas: [
      {
        field: 'id',
        label: '会话ID',
        component: 'Input',
        colProps: { span: 5 },
        componentProps: { placeholder: '会话ID' },
      },
      {
        field: 'filterAgentId',
        label: '客服',
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
        },
      },
      {
        field: 'userId',
        label: '访客ID',
        component: 'Input',
        colProps: { span: 5 },
        componentProps: { placeholder: '访客ID' },
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
        },
      },
      {
        field: 'endType',
        label: '结束方式',
        component: 'Select',
        colProps: { span: 5 },
        componentProps: {
          options: [
            { label: '全部', value: '' },
            { label: '客服结束', value: 0 },
            { label: '超时结束', value: 1 },
            { label: '访客结束', value: 2 },
            { label: '系统清理', value: 3 },
          ],
          allowClear: true,
        },
      },
      {
        field: 'satisfaction',
        label: '对话评价',
        component: 'Select',
        colProps: { span: 5 },
        componentProps: {
          options: [
            { label: '全部', value: '' },
            { label: '1星', value: 1 },
            { label: '2星', value: 2 },
            { label: '3星', value: 3 },
            { label: '4星', value: 4 },
            { label: '5星', value: 5 },
          ],
          allowClear: true,
        },
      },
      {
        field: 'createTime',
        label: '开始时间',
        component: 'RangePicker',
        colProps: { span: 6 },
        componentProps: {
          format: 'YYYY-MM-DD',
          valueFormat: 'YYYY-MM-DD',
        },
      },
      {
        field: 'endTime',
        label: '结束时间',
        component: 'RangePicker',
        colProps: { span: 6 },
        componentProps: {
          format: 'YYYY-MM-DD',
          valueFormat: 'YYYY-MM-DD',
        },
      },
      {
        field: 'source',
        label: '来源渠道',
        component: 'Input',
        colProps: { span: 5 },
        componentProps: { placeholder: '来源渠道' },
      },
      {
        field: 'landingPage',
        label: '着陆页',
        component: 'Input',
        colProps: { span: 5 },
        componentProps: { placeholder: '着陆页URL关键词' },
      },
      {
        field: 'referrerPage',
        label: '来源页',
        component: 'Input',
        colProps: { span: 5 },
        componentProps: { placeholder: '来源页URL关键词' },
      },
    ],
  },
  useSearchForm: true,
  showTableSetting: true,
  bordered: true,
  scroll: { x: 3160 },
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

function getActions(record: any) {
  return [
    { label: '查看详情', onClick: () => openDetailModal(true, { record }) },
  ];
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
