<template>
  <div class="cs-login-log-page">
    <BasicTable @register="registerTable" />
  </div>
</template>

<script lang="ts" setup>
import { BasicTable, useTable } from '/@/components/Table';
import { defHttp } from '/@/utils/http/axios';

const columns = [
  { title: '日期', dataIndex: 'loginDate', width: 180 },
  { title: '账号', dataIndex: 'username', width: 150 },
  { title: '事件', dataIndex: 'event', width: 120 },
  { title: 'IP', dataIndex: 'ip', width: 160 },
];

const [registerTable] = useTable({
  title: '客服登录日志',
  api: async (params) => {
    const res = await defHttp.get({ url: '/cs/security/login-log/list', params });
    return res;
  },
  columns,
  formConfig: {
    schemas: [
      { field: 'username', label: '账号', component: 'Input', colProps: { span: 6 } },
      {
        field: 'event',
        label: '事件',
        component: 'Select',
        colProps: { span: 6 },
        componentProps: {
          options: [
            { label: '登录成功', value: '登录成功' },
            { label: '登录失败', value: '登录失败' },
            { label: 'IP拦截', value: 'IP拦截' },
            { label: '退出', value: '退出' },
          ],
        },
      },
      {
        field: 'startDate',
        label: '开始日期',
        component: 'DatePicker',
        colProps: { span: 6 },
        componentProps: {
          valueFormat: 'YYYY-MM-DD',
        },
      },
      {
        field: 'endDate',
        label: '结束日期',
        component: 'DatePicker',
        colProps: { span: 6 },
        componentProps: {
          valueFormat: 'YYYY-MM-DD',
        },
      },
    ],
  },
  useSearchForm: true,
  showTableSetting: true,
  bordered: true,
});
</script>

<style lang="less" scoped>
.cs-login-log-page {
  padding: 16px;
}
</style>
