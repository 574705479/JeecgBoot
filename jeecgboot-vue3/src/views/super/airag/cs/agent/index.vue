<template>
  <div class="cs-agent-page">
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #tableTitle>
        <a-button type="primary" preIcon="ant-design:plus-outlined" @click="handleAdd">新增管理员客服</a-button>
      </template>
      <template #avatar="{ record }">
        <a-avatar :src="getAvatarUrl(record.avatar)">{{ (record.nickname || '客').charAt(0) }}</a-avatar>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getActions(record)" />
      </template>
      <template #status="{ record }">
        <a-badge :status="getStatusBadge(record.status)" :text="getStatusText(record.status)" />
      </template>
    </BasicTable>

    <!-- 编辑弹窗 -->
    <CsAgentModal @register="registerModal" @success="reload" />
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import { BasicTable, useTable, TableAction } from '/@/components/Table';
import { useModal } from '/@/components/Modal';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';
import { getFileAccessHttpUrl } from '/@/utils/common/compUtils';
import { withImageCache } from '../utils/csImageCache';
// Phase 4.2 (M4)：管理员客服列表加载后批量预热头像（5 分钟去重），加快后续渲染命中
import { warmupAvatars } from '/@/utils/file/imageCache';
import CsAgentModal from './CsAgentModal.vue';

const { createConfirm, createMessage } = useMessage();

const [registerModal, { openModal }] = useModal();

const columns = [
  { title: '头像', dataIndex: 'avatar', width: 80, slots: { customRender: 'avatar' } },
  { title: '客服昵称', dataIndex: 'nickname', width: 120 },
  { title: '登录账号', dataIndex: 'username', width: 120 },
  { title: '最大接待数', dataIndex: 'maxSessions', width: 100 },
  { title: '当前接待数', dataIndex: 'currentSessions', width: 100 },
  { title: '状态', dataIndex: 'status', width: 100, slots: { customRender: 'status' } },
  { title: '累计服务', dataIndex: 'totalServed', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
];

const [registerTable, { reload }] = useTable({
  title: '客服管理',
  api: async (params) => {
    const res = await defHttp.get({ url: '/cs/agent/list', params: { ...params, role: 1 } });
    try {
      const records = (res?.records || res?.list || (Array.isArray(res) ? res : [])) as any[];
      const urls = records
        .map((r) => r?.avatar)
        .filter((u) => !!u)
        .map((u: string) => getFileAccessHttpUrl(u));
      if (urls.length) warmupAvatars(urls);
    } catch {
      // 预热失败不阻塞业务
    }
    return res;
  },
  columns,
  formConfig: {
    schemas: [
      { field: 'nickname', label: '昵称', component: 'Input', colProps: { span: 6 } },
      { field: 'status', label: '状态', component: 'Select', colProps: { span: 6 },
        componentProps: {
          options: [
            { label: '离线', value: 0 },
            { label: '在线', value: 1 },
            { label: '忙碌', value: 2 },
            { label: '隐身', value: 3 },
          ]
        }
      },
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

function getAvatarUrl(avatar?: string) {
  return avatar ? withImageCache(getFileAccessHttpUrl(avatar)) : '';
}

function getStatusBadge(status: number) {
  switch (status) {
    case 1: return 'success';
    case 2: return 'warning';
    case 0: return 'error';
    default: return 'default';
  }
}

function getStatusText(status: number) {
  switch (status) {
    case 1: return '在线';
    case 2: return '忙碌';
    case 3: return '隐身';
    default: return '离线';
  }
}

function getActions(record: any) {
  return [
    { label: '编辑', onClick: () => handleEdit(record) },
    { label: '删除', color: 'error', popConfirm: { title: '确定删除该管理员客服吗?', confirm: () => handleDelete(record) } },
  ];
}

function handleAdd() {
  openModal(true, { isUpdate: false });
}

function handleEdit(record: any) {
  openModal(true, { isUpdate: true, record });
}

async function handleDelete(record: any) {
  await defHttp.delete({ url: '/cs/agent/delete', params: { id: record.id } });
  createMessage.success('删除成功');
  reload();
}
</script>

<style lang="less" scoped>
.cs-agent-page {
  padding: 16px;
}
</style>
