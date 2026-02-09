<template>
  <div class="cs-ip-blacklist-page">
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button type="primary" preIcon="ant-design:plus-outlined" @click="handleAdd">添加IP黑名单</a-button>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getActions(record)" />
      </template>
      <template #statusSlot="{ record }">
        <a-tag v-if="unbannedIds.has(record.id)" color="green">已解封</a-tag>
        <a-tag v-else color="red">封禁中</a-tag>
      </template>
    </BasicTable>

    <!-- 添加弹窗 -->
    <BasicModal @register="registerModal" title="添加IP黑名单" @ok="handleSubmit" :minHeight="60">
      <a-form :model="formState" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="IP/IP段" required>
          <a-input v-model:value="formState.ip" placeholder="如 192.168.1.1 或 192.168.1.0/24" />
        </a-form-item>
        <a-form-item label="拉黑原因" required>
          <a-textarea v-model:value="formState.reason" :rows="3" placeholder="请输入拉黑原因" />
        </a-form-item>
      </a-form>
    </BasicModal>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue';
import { BasicTable, useTable, TableAction } from '/@/components/Table';
import { BasicModal, useModal } from '/@/components/Modal';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';

const { createMessage } = useMessage();

const [registerModal, { openModal, closeModal }] = useModal();

const formState = reactive({
  ip: '',
  reason: '',
});

const unbannedIds = ref(new Set<string>());

const columns = [
  { title: 'IP/IP段', dataIndex: 'ip', width: 200 },
  { title: '拉黑原因', dataIndex: 'reason', width: 250 },
  { title: '拉黑人', dataIndex: 'operator', width: 120 },
  { title: '拉黑日期', dataIndex: 'banDate', width: 180 },
  { title: '状态', dataIndex: 'status', width: 100, slots: { customRender: 'statusSlot' } },
];

const [registerTable, { reload }] = useTable({
  title: '访客IP黑名单',
  api: async (params) => {
    unbannedIds.value.clear();
    const res = await defHttp.get({ url: '/cs/security/ip-blacklist/list', params });
    return res;
  },
  columns,
  formConfig: {
    schemas: [
      { field: 'ip', label: 'IP', component: 'Input', colProps: { span: 6 } },
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

function getActions(record: any) {
  if (unbannedIds.value.has(record.id)) {
    return [];
  }
  return [
    {
      label: '解封',
      color: 'error' as const,
      icon: 'ant-design:unlock-outlined',
      popConfirm: {
        title: '确定解封该IP吗？解封后该IP将不再被拦截。',
        confirm: () => handleUnban(record),
      },
    },
  ];
}

function handleAdd() {
  formState.ip = '';
  formState.reason = '';
  openModal(true);
}

async function handleSubmit() {
  if (!formState.ip) {
    createMessage.warning('请输入IP/IP段');
    return;
  }
  if (!formState.reason) {
    createMessage.warning('请输入拉黑原因');
    return;
  }
  await defHttp.post({ url: '/cs/security/ip-blacklist/add', params: formState });
  createMessage.success('添加成功');
  closeModal();
  reload();
}

async function handleUnban(record: any) {
  await defHttp.delete({ url: `/cs/security/ip-blacklist/delete/${record.id}` });
  createMessage.success('解封成功');
  unbannedIds.value.add(record.id);
}
</script>

<style lang="less" scoped>
.cs-ip-blacklist-page {
  padding: 16px;
}
</style>
