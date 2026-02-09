<template>
  <div class="cs-visitor-blacklist-page">
    <BasicTable @register="registerTable">
      <template #action="{ record }">
        <TableAction :actions="getActions(record)" />
      </template>
      <template #statusSlot="{ record }">
        <a-tag v-if="unbannedIds.has(record.id)" color="green">已解封</a-tag>
        <a-tag v-else color="red">封禁中</a-tag>
      </template>
    </BasicTable>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import { BasicTable, useTable, TableAction } from '/@/components/Table';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';

const { createMessage } = useMessage();

const unbannedIds = ref(new Set<string>());

const columns = [
  { title: '访客名称', dataIndex: 'visitorName', width: 150 },
  { title: '访客ID', dataIndex: 'visitorId', width: 200 },
  { title: '拉黑原因', dataIndex: 'reason', width: 200 },
  { title: '操作人', dataIndex: 'operator', width: 120 },
  { title: '拉黑日期', dataIndex: 'banDate', width: 180 },
  { title: '最近访问时间', dataIndex: 'lastVisitTime', width: 180 },
  { title: '状态', dataIndex: 'status', width: 100, slots: { customRender: 'statusSlot' } },
];

const [registerTable, { reload }] = useTable({
  title: '访客黑名单',
  api: async (params) => {
    unbannedIds.value.clear();
    const res = await defHttp.get({ url: '/cs/security/visitor-blacklist/list', params });
    return res;
  },
  columns,
  formConfig: {
    schemas: [
      { field: 'visitorName', label: '访客名称', component: 'Input', colProps: { span: 6 } },
      { field: 'visitorId', label: '访客ID', component: 'Input', colProps: { span: 6 } },
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
        title: '确定解封该访客吗？解封后该访客将恢复访问。',
        confirm: () => handleUnban(record),
      },
    },
  ];
}

async function handleUnban(record: any) {
  await defHttp.delete({ url: `/cs/security/visitor-blacklist/delete/${record.id}` });
  createMessage.success('解封成功');
  unbannedIds.value.add(record.id);
}
</script>

<style lang="less" scoped>
.cs-visitor-blacklist-page {
  padding: 16px;
}
</style>
